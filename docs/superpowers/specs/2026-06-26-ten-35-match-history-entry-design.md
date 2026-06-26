# TEN-35 — Statistik-Seite: Einstieg aus Match-History / Spielerliste

**Linear:** [TEN-35](https://linear.app/tennis-score-and-statistic/issue/TEN-35/statistik-page-einstieg-aus-match-history-spielerliste)
**Status:** Design abgenommen
**Datum:** 2026-06-26

## Ziel

Aus der Spielerliste soll man die Statistik **abgeschlossener** Matches erreichen — über eine **Match-History-Liste pro Spieler**: Liste vergangener Matches (Gegner, Ergebnis, Datum) → Klick öffnet `/matches/:id/statistics`. Bisher kommt man nur direkt nach Matchende dorthin (mit Query-Params `sets/p1/p2`).

## Abgenommene Entscheidungen
- **Ansatz:** Match-History-Liste pro Spieler (nicht nur «Letztes Match»).
- Nur **abgeschlossene** (`COMPLETED`) Matches, neueste zuerst (nach `matches.updated_at`).
- Die **Satz-Score-Badges** (z. B. „6-4, 3-6") existieren nur als Runtime-`setHistory` der Score-Seite und sind aus dem Backend **nicht rekonstruierbar** → beim History-Einstieg entfallen sie; Namen, Stat-Grid und die Satz-**Stat**-Tabs (TEN-36) sind vollständig vorhanden.
- `MatchResponse`/`Match`-Domain bleiben unverändert (der Zeitstempel lebt nur im neuen History-Read-Model).

## Architektur-Überblick

```
Spielerliste (players.component)
  Zeile: [Compare] [History 🕘] [Delete/Deactivate]
                      └─→ /players/:id/matches
MatchHistoryComponent (neu)
  GET /api/players/:id/matches → List<MatchHistoryEntryDto>
  Zeile: Gegner · Ergebnis (S 2:1 / N 1:2) · Datum  → klick → /matches/:matchId/statistics
StatisticsComponent
  ohne ?p1/?p2: Namen via getMatch + getPlayer nachladen; ohne ?sets: keine Badges
```

## Komponente 1 — Backend (match-module): Match-History-Read-Model + Endpoint

Das match-module besitzt Matches + Scores und nutzt bereits player-module-Ports.

### Read-Model + In-Port
```java
// domain/model
public record MatchHistoryEntry(
        UUID matchId, UUID opponentId, String opponentName,
        int setsWon, int setsLost, boolean won, java.time.Instant completedAt) {}

// application/port/in
public interface GetMatchHistoryUseCase {
    /** Completed matches of the player, newest first. Owner-scoped (404 for foreign/unknown player). */
    List<MatchHistoryEntry> forPlayer(UUID playerId);
}
```

### Out-Port + Persistence
```java
// application/port/out
public interface LoadMatchHistoryPort {
    List<MatchHistoryRow> findCompletedByPlayer(UUID playerId, UUID ownerId);

    record MatchHistoryRow(
            UUID matchId, UUID player1Id, UUID player2Id,
            int setsPlayer1, int setsPlayer2, String winner, java.time.Instant completedAt) {}
}
```
Adapter (`MatchPersistenceAdapter` o. eigener Adapter): JPQL/Query über `MatchJpaEntity` ⋈ `MatchScoreJpaEntity` (über `match_id`), gefiltert `(m.player1Id = :playerId OR m.player2Id = :playerId) AND m.status = COMPLETED AND m.ownerId = :ownerId`, sortiert `m.updatedAt DESC`. `completedAt` = `m.updatedAt`. (Owner-Filter zusätzlich zum Spieler-Owner-Check als Defense-in-Depth.)

### Service
`MatchHistoryService implements GetMatchHistoryUseCase`:
- Owner-/Existenz-Check: `loadPlayerPort.findByIdAndOwner(playerId, currentUserId)` → sonst `PlayerNotFoundException` (→404). (Admin-Pfad analog zu bestehenden Services optional; minimal: owner-scoped wie OpponentPreparationService.)
- `findCompletedByPlayer(playerId, ownerId)` laden; jede `MatchHistoryRow` aus Sicht von `playerId` mappen:
  - `opponentId` = der jeweils andere Slot; `opponentName` via `loadPlayerPort.loadPlayer(opponentId)` (Vorname + Nachname; Fallback leerer String, falls Gegner gelöscht).
  - `setsWon`/`setsLost`: ist `playerId == player1Id` → `setsPlayer1`/`setsPlayer2`, sonst getauscht.
  - `won`: `winner == "PLAYER1"` und Spieler ist Slot 1, bzw. `"PLAYER2"` und Slot 2.

### REST + DTO
- Controller (match-module) `@GetMapping("/api/players/{playerId}/matches")` → `getMatchHistoryUseCase.forPlayer(playerId).stream().map(MatchHistoryEntryDto::from).toList()`.
- `MatchHistoryEntryDto(UUID matchId, String opponentName, int setsWon, int setsLost, boolean won, Instant completedAt)`.

## Komponente 2 — Frontend: Match-History-Ansicht + Einstieg

### Einstieg (Spielerliste)
`players.component.html`: in der `actions`-Spalte jeder Zeile ein Icon-Button (`history`, Tooltip „Match-History", `data-testid="player-history-btn"`) → `goToHistory(player)` → `router.navigate(['/players', player.id, 'matches'])`. Methode in `players.component.ts` ergänzen.

### Route + Komponente
- Route in `app.routes.ts`: `{ path: 'players/:id/matches', loadComponent: () => import('./features/players/match-history/match-history.component').then(m => m.MatchHistoryComponent) }`.
- `MatchHistoryComponent` (standalone):
  - Liest `:id` aus der Route, lädt `api.getPlayerMatchHistory(id)` (neu).
  - Signals: `entries`, `loading`. Header zeigt den Spielernamen (via `getPlayer(id)`) + Zurück-Button (→ `/players`).
  - Rendert je `entry` eine klickbare Zeile: **Gegner** (`opponentName`), **Ergebnis** als Badge — gewonnen „S {{setsWon}}:{{setsLost}}" grün (`--success`), verloren „N {{setsWon}}:{{setsLost}}" rot (`--danger`) —, **Datum** (`completedAt`, 24h-Format). Klick → `router.navigate(['/matches', entry.matchId, 'statistics'])` (`data-testid="history-entry"`).
  - Leerzustand: „Noch keine abgeschlossenen Matches".
  - Stil im „Daylight · Hard Court"-Look (Zeilen mit schwarzem Rahmen, Ergebnis-Badge success/danger).

### Model + ApiService
```ts
export interface MatchHistoryEntry {
  matchId: string; opponentName: string;
  setsWon: number; setsLost: number; won: boolean; completedAt: string;
}
// api.service.ts
getPlayerMatchHistory(playerId: string): Observable<MatchHistoryEntry[]> {
  return this.http.get<MatchHistoryEntry[]>(`${this.base}/players/${playerId}/matches`);
}
```

## Komponente 3 — Statistik-Seite self-sufficient

`statistics.component.ts` `ngOnInit`: das `?sets`-Handling und die `p1`/`p2`-Query-Params bleiben. Ergänzung: **wenn `p1`/`p2` fehlen**, nach dem Setzen der `matchId` zusätzlich `api.getMatch(matchId)` → `player1Id`/`player2Id` → `api.getPlayer(id)` aufrufen und `p1Name`/`p2Name` daraus setzen. Die Satz-Score-Badges hängen weiterhin an `setScores()` (nur befüllt, wenn `?sets` vorhanden) — beim History-Einstieg also leer. Keine Backend-Änderung.

## Umfang / Nicht-Ziele (YAGNI)
- Nur abgeschlossene Matches; keine Paginierung; keine Satz-Spielstand-Rekonstruktion.
- Kein «Letztes Match»-Direkt-Button (die History deckt das ab).
- `MatchResponse`/`Match`-Domain, das statistics-Backend und das ai-module unberührt.

## Teststrategie
- **Backend Unit** (`MatchHistoryServiceTest`, Mockito): Mapping aus `MatchHistoryRow` → `MatchHistoryEntry` für beide Slot-Positionen (Gegner/Sätze/`won` korrekt aus Sicht des Spielers), Reihenfolge durchgereicht; Owner-Check (fremder Spieler → `PlayerNotFoundException`).
- **Backend API-IT** (`MatchHistoryApiIT`, Testcontainers): zwei abgeschlossene Matches eines Spielers (+ ein IN_PROGRESS) → `GET /api/players/{id}/matches` liefert die zwei COMPLETED neueste-zuerst mit `opponentName`/`setsWon`/`won`/`completedAt`, IN_PROGRESS ausgeschlossen; unbekannter/fremder Spieler → 404; Spieler ohne Matches → `[]`.
- **Frontend Cypress:**
  - `match-history.component.cy.ts`: Liste rendert Gegner/Ergebnis/Datum; Klick navigiert zu `/matches/:id/statistics`; Leerzustand.
  - `players.component.cy.ts`: History-Button vorhanden → navigiert zu `/players/:id/matches`.
  - `statistics.component.cy.ts`: ohne `p1/p2` werden Namen via `getMatch`+`getPlayer` (gemockt) nachgeladen; ohne `?sets` keine Badges; bestehende Tests bleiben grün.

## DoD
- Backend `./gradlew check` grün (inkl. Coverage-Gate 85 %/70 %).
- Frontend Vitest + Cypress grün.
- Manueller Sicht-Check: Spielerliste → History-Button → Liste → Klick → Statistik (Namen vorhanden, Satz-Tabs funktionieren).

## Betroffene/neue Dateien (Überblick)
**Backend (match-module)**
- Neu: `domain/model/MatchHistoryEntry.java`, `application/port/in/GetMatchHistoryUseCase.java`, `application/port/out/LoadMatchHistoryPort.java`, `application/service/MatchHistoryService.java`, `infrastructure/web/dto/MatchHistoryEntryDto.java`, ein Controller-Mapping (neuer oder bestehender Controller) für `/api/players/{playerId}/matches`
- Geändert: `infrastructure/persistence/repository/MatchPersistenceAdapter.java` (+ Repository-Query) zur Implementierung von `LoadMatchHistoryPort`
- Test: `match-module` `MatchHistoryServiceTest`, `app/.../MatchHistoryApiIT.java`

**Frontend**
- Neu: `features/players/match-history/match-history.component.{ts,html}`, `match-history.component.cy.ts`
- Geändert: `core/models/match.model.ts` (oder eigenes Model), `core/services/api.service.ts` (+`getPlayerMatchHistory`), `app.routes.ts`, `features/players/players.component.{ts,html}` (+History-Button), `features/matches/statistics/statistics.component.ts` (Namen-Fallback), zugehörige `.cy.ts`
