# Design Spec: POINT-Entität und Scoring API Redesign

| Feld | Wert |
|------|------|
| **Datum** | 2026-04-25 |
| **Status** | Approved |
| **Kontext** | TSaS Feature Gap #1 — keine einzelnen Punkte persistiert |
| **Ansatz** | A — Erweiterung `match-module`, keine neuen Gradle-Module |

---

## Ziel

Einzelne Punkte eines Tennismatches persistent speichern, sodass alle im SAD (FA-08) definierten Statistiken berechenbar sind — inkl. Return-, Break-Point- und Net-Point-Statistiken.

---

## 1. Datenmodell

### Neue Tabelle: `points`

```sql
CREATE TABLE points (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id        UUID NOT NULL REFERENCES matches(id),
    set_number      SMALLINT NOT NULL,
    game_number     SMALLINT NOT NULL,
    point_number    SMALLINT NOT NULL,
    winner          SMALLINT NOT NULL,         -- 1 = player1, 2 = player2
    point_type      point_type NOT NULL,
    stroke_type     stroke_type NULL,          -- null bei DOUBLE_FAULT
    direction       direction NULL,
    serving_player  SMALLINT NOT NULL,         -- berechnet vom Server
    is_break_point  BOOLEAN NOT NULL DEFAULT FALSE,  -- berechnet vom Server
    remark          TEXT NULL,                 -- max. 500 Zeichen
    recorded_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_points_match_id ON points(match_id);
```

### Enums

```sql
CREATE TYPE point_type AS ENUM (
    'WINNER', 'UNFORCED_ERROR', 'FORCED_ERROR',
    'ACE', 'DOUBLE_FAULT', 'NET', 'OUT_LONG', 'OUT_SIDE'
);

CREATE TYPE stroke_type AS ENUM (
    'FOREHAND', 'BACKHAND', 'SERVE', 'VOLLEY', 'SMASH'
);

CREATE TYPE direction AS ENUM (
    'CROSS_COURT', 'DOWN_THE_LINE', 'MIDDLE'
);
```

### Bestehende Tabellen

`matches` und `match_scores` bleiben unverändert. `match_scores` bleibt als Read-Modell für die Live-Score-Anzeige.

### Ableitbare Werte (kein eigenes DB-Feld)

| Statistik | Ableitung |
|-----------|-----------|
| `is_net_approach` | `stroke_type IN ('VOLLEY', 'SMASH')` |
| `is_serve_point` | `stroke_type = 'SERVE'` |
| `is_return_point` | `winner != serving_player` (Receiver gewinnt den Punkt) |

---

## 2. API-Vertrag

### Neuer Endpoint

```
POST /api/matches/{id}/points
Authorization: Bearer <token>
Content-Type: application/json
```

**Request:**
```json
{
  "winner": 1,
  "pointType": "WINNER",
  "strokeType": "FOREHAND",
  "direction": "CROSS_COURT",
  "remark": "Passing shot"
}
```

| Feld | Pflicht | Werte |
|------|---------|-------|
| `winner` | ✅ | `1` oder `2` |
| `pointType` | ✅ | `WINNER` `UNFORCED_ERROR` `FORCED_ERROR` `ACE` `DOUBLE_FAULT` `NET` `OUT_LONG` `OUT_SIDE` |
| `strokeType` | ❌ | `FOREHAND` `BACKHAND` `SERVE` `VOLLEY` `SMASH` |
| `direction` | ❌ | `CROSS_COURT` `DOWN_THE_LINE` `MIDDLE` |
| `remark` | ❌ | max. 500 Zeichen |

**Response `201 Created`:** bestehende `MatchWithScoreResponse` (Score-Aggregat unverändert)

**Fehlerfälle:**
- `400` — fehlende Pflichtfelder oder ungültige Enum-Werte
- `404` — Match nicht gefunden
- `409` — Match nicht `IN_PROGRESS`

### Entfallende Endpoints

| Endpoint | Ersatz |
|----------|--------|
| `POST /api/matches/{id}/score/player1` | `POST /api/matches/{id}/points` mit `winner=1` |
| `POST /api/matches/{id}/score/player2` | `POST /api/matches/{id}/points` mit `winner=2` |
| `POST /api/matches/{id}/ace/player1` | `POST /api/matches/{id}/points` mit `pointType=ACE, winner=1` |
| `POST /api/matches/{id}/ace/player2` | `POST /api/matches/{id}/points` mit `pointType=ACE, winner=2` |
| `POST /api/matches/{id}/serve/player1` | bleibt — Aufschläger setzen ist eigenständige Aktion |
| `POST /api/matches/{id}/serve/player2` | bleibt |

---

## 3. Service Layer

### Neue Klassen in `match-module`

```
domain/model/
  Point.java
  PointType.java          (enum)
  StrokeType.java         (enum)
  Direction.java          (enum)

application/port/in/
  RecordPointUseCase.java   (ersetzt RecordPointUseCase + RecordAceUseCase)
  RecordPointCommand.java   (winner, pointType, strokeType, direction, remark)

application/port/out/
  SavePointPort.java
  LoadPointsByMatchPort.java

infrastructure/persistence/
  PointJpaEntity.java
  PointJpaRepository.java
  PointPersistenceAdapter.java    (implements SavePointPort, LoadPointsByMatchPort)
  PointMapper.java
```

### `ScoringService.recordPoint()` — Ablauf

```
1. Match laden + validieren (status = IN_PROGRESS, sonst 409)
2. MatchScore laden
3. servingPlayer aus MatchScore.servingPlayer lesen
4. isBreakPoint berechnen:
     Receiver hat Game-Point wenn:
     - Score: 0-40, 15-40, 30-40
     - oder: Deuce + Advantage Receiver
5. setNumber = MatchScore.currentSet
   gameNumber = MatchScore.gamesPlayer1 + MatchScore.gamesPlayer2 + 1
   pointNumber = COUNT(points WHERE match_id AND set = current AND game = current) + 1
6. Point persistieren (SavePointPort)
7. MatchScore-Aggregat aktualisieren (bestehende Logik bleibt unverändert)
8. MatchWithScoreResponse zurückgeben
```

### Entfällt

- `RecordAceUseCase` (Ace = `pointType`)
- Ace-Sonderpfad in `ScoringService`

---

## 4. Frontend

### `ScoreComponent` — Änderungen

Court-Click öffnet neu einen Dialog (statt direktem API-Call):

**Dialog-Flow:**
1. Coach klickt auf Spielerhälfte → `winner` gesetzt
2. Dialog öffnet sich mit:
   - **Punkttyp** (Pflicht, MatButtonToggleGroup): Winner / UE / FE / Ace / DF / Net / Out Long / Out Side
   - **Schlagart** (optional, vorselektiert: FOREHAND): FH / BH / Serve / Volley / Smash
   - **Richtung** (optional, vorselektiert: CROSS_COURT): Cross / DTL / Middle
   - **Bemerkung** (optional, MatInput)
3. Bestätigen → `POST /api/matches/{id}/points`
4. Score-Anzeige aktualisiert sich

**Bestehend, unverändert:**
- `ScoreEditDialogComponent`
- `EndMatchDialogComponent`
- Serve-Buttons (Aufschläger setzen)

**`ApiService`** — neue Methode:
```typescript
recordPoint(matchId: string, command: RecordPointCommand): Observable<MatchWithScore>
```

---

## 5. Datenbank-Migration

Neues Flyway-Skript: `backend/app/src/main/resources/db/migration/V2__add_points_table.sql`

**H2-Kompatibilität:** PostgreSQL `CREATE TYPE` wird in H2 nicht unterstützt. Im `test`-Profil werden die Enums als `VARCHAR` via Hibernate-Mapping abgebildet. Das Migrations-SQL muss Flyway-Placeholder oder ein separates H2-kompatibles Skript verwenden — analog zur bestehenden Lösung in `V1__baseline.sql`.

---

## 6. Tests

| Test | Änderung |
|------|----------|
| `ScoringServiceTest` | Anpassung auf neues `RecordPointCommand`-Schema |
| `MatchApiIT` | Scoring-Tests ersetzen alte Endpoint-Aufrufe durch `POST /points` |
| `PointPersistenceAdapterIT` | Neuer Integrationstest für `SavePointPort` / `LoadPointsByMatchPort` |

---

## 7. Abgrenzung

**In Scope:**
- `POINT`-Entität, Persistenz, neuer Endpoint
- Frontend-Dialog
- Migration `V2`
- Tests

**Out of Scope:**
- `statistics-module` Implementierung (separate Aufgabe, Feature Gap #3)
- Extraktion `scoring-module` als eigenes Gradle-Modul (Feature Gap mittlere Priorität)
- `MATCH_SET`-Entität
