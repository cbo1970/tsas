# TEN-68 — Freitext für Coach (Coach-Notizen pro Spieler)

**Linear:** [TEN-68](https://linear.app/tennis-score-and-statistic/issue/TEN-68/freitext-fur-coach)
**Status:** Design abgenommen
**Datum:** 2026-06-26

## Ziel

Der Coach kann während und nach einem Match je Spieler einen Freitext erfassen — Beobachtungen, die **nicht** mit dem Spielstand verknüpft sind. Diese Notizen dienen zwei Zwecken:

- **a) Eigener Spieler:** Feststellen eigener Schwächen mit Vorschlägen fürs Training.
- **b) Gegner:** Feststellen gegnerischer Schwächen für ein zukünftiges Match samt Strategie zum Ausnutzen — gehört zur Matchvorbereitung.

Beide Zwecke werden durch Anbindung an die bestehenden KI-Flows realisiert: **Match-Analyse (Postmortem)** und **Gegner-Vorbereitung (TEN-51)**.

**DoD (aus Ticket):** Alle Tests wieder grün (`./gradlew check` inkl. Coverage-Gate + Frontend-Tests).

## Abgegrenzter Scope (Entscheidungen aus dem Brainstorming)

| Entscheidung | Wahl |
|---|---|
| Struktur | **Ein** editierbares Notizfeld pro (Match, Spieler) — keine Beobachtungs-Liste/kein Log |
| KI-Anbindung | **Beide** Flows: Postmortem **und** Gegner-Vorbereitung |
| Bearbeitbarkeit | **Während UND nach** dem Match (Live-Scoring + Analyse-/Match-Detailseite) |
| Tabellen-Schlüssel | **Variante A:** `(match_id, player_id)` — macht H2H-Aggregation trivial |
| Speichern | Explizit (Button / `blur`), kein Auto-Save-Debounce (YAGNI) |
| Notiz-Länge | 2000 Zeichen |
| Gegner-Notizen-Limit (Prompt) | letzte **10** Matches, neueste zuerst |

**Nicht im Scope (YAGNI):** Beobachtungs-Historie/Log pro Spieler, Auto-Save, Versionierung der Notizen, Notizen unabhängig von einem Match, Änderung des strukturierten LLM-Outputs, Notizen als eigenständiges KI-Trigger-Kriterium.

## Architektur-Überblick

```
Frontend (player-notes.component)
  ├─ Live-Scoring (score.component)         ── PUT/GET ──┐
  └─ Analyse-Seite (match-analysis.comp.)   ── PUT/GET ──┤
                                                          ▼
match-module  MatchController
  GET  /api/matches/{matchId}/notes
  PUT  /api/matches/{matchId}/notes/{playerId}
        └─ owner-geprüft über Match (findByIdAndOwner)
        └─ MatchPlayerNote-Aggregat  ──>  match_player_notes (V10)

ai-module
  MatchAnalysisService ─────────┐
  OpponentPreparationService ───┤── PlayerNotesProviderPort (Out-Port)
                                │      └─ Adapter liest match-module
  PromptBuilder                 │
    userPrompt(+Notiz-Block)    │
    opponentPreparationUserPrompt(+aggregierte Gegner-Notizen)
```

**Clean-Architecture-Leitplanke:** Das ai-module greift **nicht** direkt auf match-module-JPA zu. Zugriff ausschliesslich über den neuen Out-Port `PlayerNotesProviderPort`, implementiert von einem Adapter, der die Lese-Schnittstelle des match-module nutzt (analog zu bestehenden modulübergreifenden Zugriffen, z. B. `GetMatchUseCase`).

## Komponente 1 — Datenmodell & Persistenz (match-module)

### Flyway-Migration `V10__create_match_player_notes.sql`

`backend/app/src/main/resources/db/migration/V10__create_match_player_notes.sql`

```sql
CREATE TABLE match_player_notes (
    id          UUID PRIMARY KEY,
    match_id    UUID NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    player_id   UUID NOT NULL REFERENCES players (id),
    note        VARCHAR(2000) NOT NULL,
    created_at  TIMESTAMP,
    created_by  VARCHAR(255),
    updated_at  TIMESTAMP,
    updated_by  VARCHAR(255),
    CONSTRAINT uq_match_player_notes UNIQUE (match_id, player_id)
);

CREATE INDEX idx_match_player_notes_player ON match_player_notes (player_id);
```

- `ON DELETE CASCADE` an `matches`: Notizen verschwinden mit dem Match (konsistent mit `points`/`match_scores`).
- Index auf `player_id` für die Gegner-Aggregation (`notesAboutPlayer`).
- Audit-Spalten gemäss bestehendem Pattern (V7), befüllt vom `AuditingEntityListener`.

### Domänen-Modell

`backend/match-module/src/main/java/com/cas/tsas/match/domain/model/MatchPlayerNote.java`

```java
public record MatchPlayerNote(
        UUID id,
        UUID matchId,
        UUID playerId,
        String note,
        Instant updatedAt) {
}
```

Eigenes Aggregat, hängt an einem Match, aber unabhängig von `MatchScore`/`Point`. `updatedAt` für die Frontend-Anzeige und die chronologische Sortierung in der Gegner-Aggregation.

### JPA-Entity + Repository

`MatchPlayerNoteJpaEntity` (Audit-Felder wie `PointJpaEntity`), Spring-Data-Repository mit:

- `Optional<…> findByMatchIdAndPlayerId(UUID matchId, UUID playerId)` — Upsert-Lookup.
- `List<…> findByMatchId(UUID matchId)` — die 0–2 Notizen eines Matches.
- `List<…> findByPlayerIdOrderByUpdatedAtDesc(UUID playerId, Pageable limit)` — Gegner-Aggregation, neueste zuerst, begrenzt.

### Ports & Service (Anwendungsschicht match-module)

**In-Ports**
- `SavePlayerNoteUseCase.save(UUID matchId, UUID playerId, String note, CurrentUser user)` → `MatchPlayerNote` (oder Löschung bei leer)
- `GetPlayerNotesUseCase.forMatch(UUID matchId, CurrentUser user)` → `List<MatchPlayerNote>`

**Service `PlayerNoteService`**
- Lädt Match via `findByIdAndOwner(matchId, user)` → `404` falls nicht vorhanden/fremd.
- Validiert: `playerId ∈ { match.player1Id, match.player2Id }` → sonst fachlicher `400`-Fehler (eigene Exception → problem+json).
- Upsert: vorhandene Notiz überschreiben, sonst anlegen.
- **Leerer/blanker `note` → Notiz löschen** (kein leerer Datensatz). Rückgabe signalisiert „gelöscht".

## Komponente 2 — REST-API (match-module, `MatchController`)

| Methode | Pfad | Body | Antwort |
|---|---|---|---|
| `GET` | `/api/matches/{matchId}/notes` | — | `200` `List<PlayerNoteResponse>` (0–2) |
| `PUT` | `/api/matches/{matchId}/notes/{playerId}` | `SavePlayerNoteRequest` | `200` `PlayerNoteResponse` (bzw. `204` bei Löschung) |

**DTOs** (`match-module/.../web/dto`)

```java
public record SavePlayerNoteRequest(@Size(max = 2000) String note) { }

public record PlayerNoteResponse(UUID playerId, String note, Instant updatedAt) {
    public static PlayerNoteResponse from(MatchPlayerNote n) {
        return new PlayerNoteResponse(n.playerId(), n.note(), n.updatedAt());
    }
}
```

**Fehlerverhalten** (über bestehenden globalen `@ControllerAdvice`, RFC-7807):
- Match nicht vorhanden / nicht im Besitz → `404`.
- `playerId` nicht Teil des Matches → `400`.
- `note` > 2000 Zeichen → `400` (Bean-Validation).
- Leerer `note` → `204` (Notiz gelöscht); `GET` liefert diesen Eintrag danach nicht mehr.

**Warum `PUT`-Upsert:** Pro (Match, Spieler) existiert genau **eine** Notiz (Unique-Constraint). `PUT` ist idempotent und passt exakt zum „ein editierbares Feld"-Modell — kein getrenntes Anlegen/Ändern.

## Komponente 3 — KI-Anbindung (ai-module)

### Neuer Out-Port

`backend/ai-module/src/main/java/com/cas/tsas/ai/application/port/out/PlayerNotesProviderPort.java`

```java
public interface PlayerNotesProviderPort {
    /** Beide (0–2) Notizen eines Matches, keyed nach playerId. */
    Map<UUID, String> notesForMatch(UUID matchId);

    /** Notizen ÜBER einen Spieler über abgeschlossene Matches, neueste zuerst, begrenzt. */
    List<String> notesAboutPlayer(UUID playerId, int limit);
}
```

Adapter im `:app`- oder ai-infrastructure-Layer, der die match-module-Lese-Schnittstelle/Repository nutzt. Das ai-module bleibt frei von match-module-JPA-Typen.

### a) Postmortem — `MatchAnalysisService` + `PromptBuilder.userPrompt`

- `MatchAnalysisService.generate()` lädt zusätzlich `notesForMatch(matchId)`.
- `MatchMetadata` wird um nullable Felder `player1Note`, `player2Note` erweitert (in `buildMetadata` befüllt, gemappt über `player1Id`/`player2Id`).
- `PromptBuilder.userPrompt` hängt **nur wenn mindestens eine Notiz vorhanden** einen Block an:

  ```
  Coach-Beobachtungen (Freitext, nicht aus der Statistik abgeleitet):
  - Spieler 1 (eigener Spieler): <player1Note>
  - Spieler 2 (Gegner): <player2Note>
  ```

  Fehlt eine Seite, wird nur die vorhandene gelistet; fehlen beide, entfällt der Block vollständig (Verhalten unverändert).

### b) Gegner-Vorbereitung — `OpponentPreparationService` + `opponentPreparationUserPrompt`

- Service ruft `notesAboutPlayer(opponentId, 10)` — dank Variante A eine einfache Query auf `player_id`.
- Aggregierte Notizen (neueste zuerst, max. 10) werden als Liste an den Prep-Prompt gehängt:

  ```
  Frühere Coach-Beobachtungen zu diesem Gegner (neueste zuerst):
  - <note 1>
  - <note 2>
  ...
  ```

- Fehlen Notizen, entfällt der Block (Verhalten unverändert).

### Guardrails

- Notizen sind reiner **Zusatzkontext**; ihr Fehlen ändert das Verhalten nicht.
- Strukturierter LLM-Output (`MatchAnalysisResult` / `OpponentPreparationResult`) bleibt unverändert.
- Mindestbedingungen für die Generierung bleiben unverändert (Match abgeschlossen, ≥10 Punkte bzw. ≥1 H2H-Match). Notizen allein triggern keine Analyse.
- `FakeLlmClientAdapter` bleibt deterministisch.

## Komponente 4 — Frontend (Angular)

### Wiederverwendbare Komponente `player-notes.component`

`frontend/src/app/features/matches/notes/player-notes.component.{ts,html}` (Standalone)

- **Inputs:** `matchId`, Liste der beiden Spieler (Id + Name + Rolle „eigener Spieler"/„Gegner").
- Lädt beim Init `GET …/notes`, hält die zwei Texte in Signals.
- Rendert **zwei `textarea`** mit Spielername + Rolle als Label, Platzhalter „Beobachtungen zu diesem Spieler…".
- Speichern explizit per Button **oder** `blur` → `api.savePlayerNote(matchId, playerId, note)` (`PUT`). Sichtbares „gespeichert"-Feedback (Status/Snackbar).
- Leeres Feld speichern → löscht die Notiz serverseitig (`204`), lokaler Zustand bleibt leer.

### Einbindung

- **Live-Scoring** `score.component`: einklappbares Panel „Coach-Notizen", bindet `player-notes.component` ein.
- **Analyse-/Match-Detail** `match-analysis.component`: dasselbe Panel, weiterhin editierbar, direkt vor „Analyse generieren".

DRY: eine Quelle (`player-notes.component`), zwei Einbindungsorte — kein Duplikat.

### API-Service & Modelle

`frontend/src/app/core/services/api.service.ts`

```ts
getPlayerNotes(matchId: string): Observable<PlayerNote[]>
savePlayerNote(matchId: string, playerId: string, note: string): Observable<PlayerNote | void>
```

```ts
export interface PlayerNote { playerId: string; note: string; updatedAt: string; }
```

### i18n

Neue Keys in `frontend/public/i18n/{de,en,it,fr}.json`: Panel-Titel, Textarea-Platzhalter, Speichern-Button, Gespeichert-Status, Rollen-Labels („eigener Spieler"/„Gegner"). Deutsch ist Leitsprache/Default.

## Teststrategie

### Backend — Domäne & Persistenz (match-module)
- Repository/Adapter: Upsert (anlegen → überschreiben, Unique gewahrt), Lösch-bei-leer, `findByMatchId` liefert 0–2, `notesAboutPlayer` liefert chronologisch + Limit 10.
- Owner-Scoping: Zugriff auf Notiz eines fremden Matches scheitert.

### Backend — API-Integration (`:app`, Testcontainers + MockMvc)
- `PUT` legt an / überschreibt → `200` + Persistenz; `GET` liefert beide Notizen.
- `playerId` nicht im Match → `400`; fremdes/unbekanntes Match → `404`; `note` > 2000 → `400`.
- Leerer `note` → `204`; danach `GET` ohne diesen Eintrag.

### Backend — KI-Anbindung (ai-module, FakeLlm)
- `PromptBuilder.userPrompt`: Notiz-Block erscheint bei vorhandenen Notizen; entfällt sauber ohne (Regressions-Guard).
- `opponentPreparationUserPrompt`: aggregiert nur Notizen **über den Gegner**, respektiert Limit 10, neueste zuerst.
- `PlayerNotesProviderPort`-Adapter: liefert korrekte Daten; ai-module bleibt frei von match-module-JPA.

### Frontend
- `player-notes.component` Unit/Cypress (`cy.intercept`): rendert zwei Felder, `PUT` bei Speichern, Laden befüllt Felder, Leeren löscht.
- i18n-Keys in allen vier Sprachen vorhanden.

### DoD
- `JAVA_HOME=/opt/java/jdk-25.0.1 … ./gradlew check` grün (inkl. Coverage-Gate 85 % Line / 70 % Branch).
- Frontend Unit- + Cypress-Tests grün.

## Betroffene/neue Dateien (Überblick)

**Neu (Backend)**
- `backend/app/src/main/resources/db/migration/V10__create_match_player_notes.sql`
- `match-module/.../domain/model/MatchPlayerNote.java`
- `match-module/.../application/port/in/SavePlayerNoteUseCase.java`, `GetPlayerNotesUseCase.java`
- `match-module/.../application/service/PlayerNoteService.java`
- `match-module/.../application/port/out/…` (Notiz-Repository-Port)
- `match-module/.../infrastructure/persistence/entity/MatchPlayerNoteJpaEntity.java` (+ Repository + Adapter)
- `match-module/.../infrastructure/web/dto/{SavePlayerNoteRequest,PlayerNoteResponse}.java`
- `ai-module/.../application/port/out/PlayerNotesProviderPort.java` (+ Adapter-Impl)

**Geändert (Backend)**
- `match-module/.../infrastructure/web/MatchController.java` (2 Endpunkte)
- `ai-module/.../application/dto/MatchMetadata.java` (+player1Note, player2Note)
- `ai-module/.../application/service/MatchAnalysisService.java`, `OpponentPreparationService.java`
- `ai-module/.../infrastructure/llm/PromptBuilder.java`

**Neu/Geändert (Frontend)**
- Neu: `features/matches/notes/player-notes.component.{ts,html}`
- Geändert: `score.component`, `match-analysis.component`, `core/services/api.service.ts`, Modelle, `public/i18n/{de,en,it,fr}.json`
