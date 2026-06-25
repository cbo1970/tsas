Punkt # Design Spec: KI-gestützte Match-Analyse (Postmortem)

| Feld | Wert |
|------|------|
| **Datum** | 2026-05-17 |
| **Status** | Draft |
| **Kontext** | Erstes von drei KI-Szenarien (Postmortem zuerst, Live-Coaching + Vorbereitung als Folge-Specs) |
| **Ansatz** | A — neues `ai-module` + Inhalt für `statistics-module`, Spring AI mit OpenAI |

---

## Ziel

Nach Abschluss eines Matches generiert der Coach per Klick eine taktische Analyse: Schlüsselmomente, Stärken/Schwächen beider Spieler und 3–5 konkrete Empfehlungen fürs nächste Mal. Die Analyse wird einmal generiert, persistiert und auf weitere Anfragen aus der DB geliefert. Eine Re-Generierung ist möglich (überschreibt).

Out of Scope (eigene Specs):
- Live-Coaching während des Matches.
- Vorbereitung gegen denselben Gegner (Head-to-Head über mehrere Matches).
- Angular-Frontend für die Anzeige der Analyse (Backend liefert nur die Endpunkte).

---

## 1. Modul-Layout

Zwei Module werden geändert/neu erstellt; keine Änderungen an `settings.gradle.kts` nötig (`statistics-module` ist bereits registriert):

```
backend/
├── statistics-module/        (jetzt mit Inhalt)
│   └── src/main/java/com/cas/tsas/statistics/
│       ├── domain/model/                   MatchStatistics, PlayerStatistics, StrokeDistribution, DirectionDistribution
│       ├── application/port/in/            ComputeMatchStatisticsUseCase
│       └── application/service/            MatchStatisticsService
│
└── ai-module/               (NEU – Modul anlegen + in settings.gradle.kts eintragen)
    └── src/main/java/com/cas/tsas/ai/
        ├── domain/
        │   ├── model/                      MatchAnalysis, AnalysisStatus, Recommendation
        │   └── exception/                  AnalysisGenerationException
        ├── application/
        │   ├── port/in/                    GenerateMatchAnalysisUseCase, GetMatchAnalysisUseCase
        │   ├── port/out/                   LlmClientPort, SaveMatchAnalysisPort, LoadMatchAnalysisPort
        │   └── service/                    MatchAnalysisService
        └── infrastructure/
            ├── web/                        MatchAnalysisController + DTOs
            ├── persistence/                MatchAnalysisJpaEntity, …Repository, …Adapter
            └── llm/                        OpenAiLlmAdapter, FakeLlmClientAdapter (test), PromptBuilder
```

**Inter-Module-Dependencies:**

- `ai-module` → `statistics-module`, `match-module`, `player-module`, `common-module`
- `statistics-module` → `match-module`, `common-module`
- `app` → `ai-module` (für Auto-Konfiguration; alle anderen Module sind bereits transitiv erfasst)

**Eintrag in `backend/settings.gradle.kts`:** `include("ai-module")` ergänzen.

**Vorbedingung im match-module:** Ein Out-Port `LoadPointsByMatchPort` (Methode `List<Point> loadByMatchId(UUID)`) wird benötigt. Es existiert aktuell nur `SavePointPort` — der Lese-Port wird in diesem Spec mit eingeführt (kleiner Adapter in `match-module/infrastructure/persistence`).

---

## 2. Domain-Modell

### statistics-module (alle Records, immutable)

```java
public record MatchStatistics(
    UUID matchId,
    PlayerStatistics player1,
    PlayerStatistics player2,
    int totalPoints,
    int breakPointsTotal,
    Instant computedAt
) {}

public record PlayerStatistics(
    int playerNumber,                  // 1 oder 2
    int pointsWon,
    int winners,
    int unforcedErrors,
    int forcedErrors,
    int aces,
    int doubleFaults,
    double firstServePercentage,       // 0..1
    double secondServePercentage,      // 0..1
    int breakPointsWon,
    int breakPointsFaced,
    StrokeDistribution strokeDistribution,
    DirectionDistribution directionDistribution
) {}

public record StrokeDistribution(Map<StrokeType, Integer> counts) {}
public record DirectionDistribution(Map<Direction, Integer> counts) {}
```

### ai-module

```java
public class MatchAnalysis {
    private UUID id;
    private UUID matchId;
    private AnalysisStatus status;          // PENDING, COMPLETED, FAILED
    private String keyMoments;
    private String ownStrengths;
    private String ownWeaknesses;
    private String opponentStrengths;
    private String opponentWeaknesses;
    private List<Recommendation> recommendations;
    private String modelUsed;
    private Instant generatedAt;
    private String errorMessage;            // bei FAILED
    // getters/setters
}

public record Recommendation(
    int priority,           // 1..5
    String title,           // kurz
    String detail           // 1–2 Sätze
) {}

public enum AnalysisStatus { PENDING, COMPLETED, FAILED }
```

Genau eine Analyse pro Match (Unique-Constraint auf `match_id`). Re-Generierung überschreibt.

---

## 3. Statistik-Berechnung

`MatchStatisticsService.compute(UUID matchId)` lädt alle Points (über `LoadPointsByMatchPort`) und aggregiert in einer Iteration:

- **Punktzahlen (`pointsWon`)**: zählt nach `Point.winner`.
- **Winner / UE / FE / Net / Out**: nach `PointType` getrennt für den Spieler, der den Schlag ausgeführt hat (siehe Zuordnungsregel unten).
- **Aces / Double Faults**: werden dem `Point.servingPlayer` zugeordnet.
- **First/Second Serve %**: nutzt das neue Feld `serveAttempt` (siehe §4).
- **Breakpoints**: bei `isBreakPoint=true` erhöht sich `breakPointsFaced` beim Aufschläger; gewinnt der Returner den Punkt, zusätzlich `breakPointsWon` beim Returner.
- **Stroke-/Direction-Distribution**: Frequenzzählung pro Spieler über alle Points mit gesetztem `strokeType`/`direction`.

**Zuordnungsregel (zentral in einem Helper `PointAttribution`):**
| PointType | attribuiert auf |
|---|---|
| WINNER | `winner` |
| UNFORCED_ERROR, FORCED_ERROR, NET, OUT_LONG, OUT_SIDE | Gegner von `winner` (der Verlierer hat den Fehler gemacht) |
| ACE | `servingPlayer` |
| DOUBLE_FAULT | `servingPlayer` |

Keine Persistenz von Statistiken — wird bei jedem Aufruf neu berechnet (bei <500 Points/Match vernachlässigbar). Die Persistenz erfolgt erst auf Analyse-Ebene.

---

## 4. Datenmodell-Erweiterung: `serveAttempt` in Point

Neues Feld `Integer serveAttempt` (1 oder 2, nullable für Nicht-Aufschlag-Punkte und Altdaten) in:

- `Point` (domain) – Konstruktor + Getter/Setter
- `PointJpaEntity` (persistence)
- `PointDto` / `PointController` (REST)
- Flyway-Migration V3 (siehe §6)

**Folgearbeit Frontend** (eigenes Ticket, nicht Teil dieses Specs): Beim Punkterfassen 1st/2nd-Aufschlag-Toggle ergänzen. Backend akzeptiert das Feld ab sofort optional, sodass bestehender Frontend-Code nicht bricht.

---

## 5. LLM-Integration

**Library:** `spring-ai-openai-spring-boot-starter` (Spring AI). Liefert `ChatClient` + `BeanOutputConverter` für strukturierten JSON-Output.

### LlmClientPort (Out-Port)

```java
public interface LlmClientPort {
    MatchAnalysisResult generateAnalysis(MatchStatistics stats, MatchMetadata meta);
}
```

`MatchAnalysisResult` ist ein Record im ai-module (DTO zwischen Adapter und Service); enthält die vier Analyse-Texte + `List<Recommendation>`. `modelUsed` wird vom Service aus der Config gesetzt (nicht aus dem LLM-Output).

`MatchMetadata` bündelt Spielerdaten (Name, Ranking, Handedness, Backhand-Typ aus `player-module`) und Match-Parameter (Best-of, Tie-Break, Short-Set).

### OpenAiLlmAdapter

- Nutzt Spring AIs `ChatClient.create(chatModel).prompt()...call().entity(MatchAnalysisResult.class)`.
- Modell konfigurierbar (Default `gpt-4o-mini`).
- Timeout: 60 s.
- Retry: 1 × bei IOException oder 5xx; kein Retry bei 4xx oder Parse-Fehlern.
- Strukturierter Output via `BeanOutputConverter<MatchAnalysisResult>` — Schema wird im Prompt mit ausgeliefert.

### Prompt-Strategie (PromptBuilder)

- **System-Prompt (deutsch):** „Du bist ein erfahrener Tennis-Coach. Analysiere die Match-Statistiken und liefere eine strukturierte taktische Auswertung. Antworte ausschliesslich in deutscher Sprache. Halte dich strikt an das vorgegebene JSON-Schema."
- **User-Prompt:** strukturierte Darstellung von `MatchStatistics` + `MatchMetadata` + Erläuterung der vier Felder + Vorgabe für 3–5 priorisierte Empfehlungen.
- Keine Few-Shot-Examples im ersten Wurf — bei schwankender Qualität später ergänzen.

### Sync vs. Async

Synchron im POST-Request. Frontend zeigt Spinner; 60-s-Timeout akzeptabel für einmaligen Coach-Workflow. Async (Event + Polling) erst, wenn Live-Coaching dazukommt.

---

## 6. Persistenz – Flyway-Migrationen

### V3__add_serve_attempt_to_points.sql
```sql
ALTER TABLE points ADD COLUMN serve_attempt SMALLINT;
-- bestehende Aufschlagpunkte bleiben NULL (unbekannt); ab jetzt 1 oder 2
ALTER TABLE points ADD CONSTRAINT chk_serve_attempt
    CHECK (serve_attempt IS NULL OR serve_attempt IN (1, 2));
```

### V4__create_match_analysis.sql
```sql
CREATE TABLE match_analysis (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id            UUID NOT NULL UNIQUE REFERENCES matches(id) ON DELETE CASCADE,
    status              VARCHAR(16) NOT NULL,
    key_moments         TEXT,
    own_strengths       TEXT,
    own_weaknesses      TEXT,
    opponent_strengths  TEXT,
    opponent_weaknesses TEXT,
    recommendations     JSONB NOT NULL DEFAULT '[]'::jsonb,
    model_used          VARCHAR(64),
    error_message       TEXT,
    generated_at        TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_match_analysis_match ON match_analysis(match_id);
```

`recommendations` als JSONB — kein Join-Table für 3–5 kurze Einträge (YAGNI).

H2 (Test-Profil) muss die Migrationen ebenfalls verstehen. JSONB ist in H2 nicht nativ; im `application-test.yml` mappen wir den Spaltentyp auf `CLOB` via Flyway-Platzhalter oder einer parallelen `V4` für H2 (Variante wird im Plan festgelegt — Standardweg: `${json_type}`-Platzhalter, `json_type=JSONB` für PostgreSQL, `CLOB` für H2).

---

## 7. REST API

| Methode | Pfad | Zweck |
|---|---|---|
| `POST` | `/api/matches/{matchId}/analysis` | generiert (oder regeneriert) und persistiert |
| `GET`  | `/api/matches/{matchId}/analysis` | liefert die gespeicherte Analyse |

**POST-Request:** kein Body. Idempotent in dem Sinne, dass jeder Aufruf den vorherigen Eintrag überschreibt.

**Response (POST 200 / GET 200):**
```json
{
  "matchId": "…",
  "status": "COMPLETED",
  "keyMoments": "…",
  "ownStrengths": "…",
  "ownWeaknesses": "…",
  "opponentStrengths": "…",
  "opponentWeaknesses": "…",
  "recommendations": [
    { "priority": 1, "title": "…", "detail": "…" }
  ],
  "modelUsed": "gpt-4o-mini",
  "generatedAt": "2026-05-17T14:23:11Z",
  "errorMessage": null
}
```

Bei `status=FAILED` (HTTP 502) sind die Analyse-Felder `null`, `errorMessage` ist gefüllt.

**HTTP-Codes:**
| Code | Bedingung |
|---|---|
| 200 | GET/POST erfolgreich |
| 404 | Match unbekannt; GET ohne Analyse: ebenfalls 404 |
| 409 | Match-Status ≠ `COMPLETED` (Analyse nur für beendete Matches) |
| 422 | Match hat weniger als 10 Points (kein OpenAI-Call, Kostenschutz) |
| 502 | OpenAI-Aufruf scheitert oder Antwort unparsbar → `MatchAnalysis` mit `status=FAILED` wird trotzdem persistiert |
| 503 | API-Key fehlt / `tsas.ai.enabled=false` |

**Auth:** fällt unter die bestehende `SecurityConfig` (JWT in prod/local, permitAll im test-Profil).

---

## 8. Konfiguration

**`application.yml`:**
```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.4
          timeout: 60s

tsas:
  ai:
    enabled: ${TSAS_AI_ENABLED:true}
```

**`application-test.yml`:** `tsas.ai.enabled: false` + Bean-Profil `test` aktiviert `FakeLlmClientAdapter` (deterministisches Stub-Result) statt `OpenAiLlmAdapter`.

**API-Key:** ausschliesslich aus Env-Var `OPENAI_API_KEY`. Niemals in Repo oder `.local`-Files committen.

---

## 9. Fehlerbehandlung

| Situation | Verhalten |
|---|---|
| OpenAI-Netz/5xx/Timeout | 1 Retry; danach `MatchAnalysis` mit `status=FAILED`, `errorMessage` persistieren, HTTP 502 |
| OpenAI-Antwort unparsbar | FAILED + parse-error in `errorMessage`, kein Retry, HTTP 502 |
| Match-Status ≠ COMPLETED | kein OpenAI-Call, HTTP 409 |
| <10 Points im Match | kein OpenAI-Call, HTTP 422 |
| `OPENAI_API_KEY` fehlt oder `tsas.ai.enabled=false` | HTTP 503, klare Log-Meldung |

---

## 10. Testing

| Test | Modul | Ebene | Inhalt |
|---|---|---|---|
| `PointAttributionTest` | statistics | Unit | Zuordnungsregel pro PointType |
| `MatchStatisticsServiceTest` | statistics | Unit | handgeschriebene Point-Listen pro Kennzahl |
| `MatchAnalysisServiceTest` | ai | Unit | Orchestrierung mit Fake `LlmClientPort` |
| `OpenAiLlmAdapterTest` | ai | Integration | WireMock-Fake-OpenAI: Request-Serialisierung + Response-Parsing |
| `MatchAnalysisControllerIT` | ai (`@SpringBootTest`, `@ActiveProfiles("test")`) | E2E | HTTP-Flows inkl. 404/409/422/502, H2, FakeLlmClientAdapter |

Keine echten OpenAI-Calls in CI. Smoke-Test mit echtem Key manuell durch Entwickler.

---

## 11. SAD-Update

In `doc/tsas_sad.md`:
- Neuer Abschnitt **„KI-gestützte Match-Analyse"** unter Architektur: Beschreibung des `ai-module` + Ports/Adapter, Datenfluss (Match → Statistics → LLM → MatchAnalysis), Entscheidungen (Spring AI + OpenAI, strukturierter JSON-Output, manuelles Trigger, Persistenz, Synchron-Call mit 60 s Timeout).
- Roadmap: Live-Coaching → V2.x, Vorbereitung gegen Gegner → V2.
- Quality-Targets: Analyse-Generierung ≤ 60 s; OpenAI-API-Kosten als Betriebsrisiko (neuer Bullet).

In `doc/sad/TSAS.drawio`:
- Neue Box „ai-module" im Backend-Container, Pfeil nach extern „OpenAI API".
- Datenflusspfeile: Match → Statistics → AI → DB.

---

## 12. Out of Scope (Folge-Specs)

- **Live-Coaching** (eigener Spec) — neuer Use Case, nutzt denselben `LlmClientPort`, anderer Prompt, andere Trigger-Heuristik (Seitenwechsel etc.).
- **Vorbereitung gegen Gegner** (eigener Spec) — braucht Head-to-Head-Aggregation über mehrere Matches.
- **Angular-Frontend** für Anzeige der Analyse + 1st/2nd-Aufschlag-Toggle bei Punkterfassung.
- Weitere Provider (Anthropic, Ollama) — der `LlmClientPort` macht den Wechsel später möglich, ohne dass dieser Spec ihn vorsieht.

---

## 13. Build-Reihenfolge (Vorschau für writing-plans)

1. `serveAttempt`-Feld in `Point` + Migration V3 + Repo/DTO/Controller + Tests
2. `LoadPointsByMatchPort` im match-module + Adapter
3. `statistics-module` aufbauen: Domain, `PointAttribution`, `MatchStatisticsService` + Tests
4. `ai-module` anlegen (Gradle, settings.gradle.kts) + Domain + Ports
5. JPA-Persistenz für `MatchAnalysis` + Migration V4
6. `FakeLlmClientAdapter` (test-Profil) + `MatchAnalysisService` + Unit-Tests
7. `OpenAiLlmAdapter` + PromptBuilder + WireMock-Tests
8. REST-Controller + DTOs + `MatchAnalysisControllerIT`
9. SAD-Update (`tsas_sad.md` + `TSAS.drawio`)
10. Manueller Smoke-Test mit echtem `OPENAI_API_KEY`
