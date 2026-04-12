# Ass-Button Feature — Design Spec

**Datum:** 2026-04-12
**Branch:** feature/docker_container

---

## Zusammenfassung

Für jeden Spieler wird im Match-Score-Screen ein "Ass"-Button hinzugefügt. Ein Klick auf den Button zählt gleichzeitig einen Punkt für den Spieler (Ass = direkter Punktgewinn) und erhöht den Ass-Zähler des Spielers. Die Ass-Zähler gehören zur Match-Statistik.

---

## Datenmodell

### MatchScore (Domain)
`com.cas.tsas.match.domain.model.MatchScore` bekommt zwei neue Felder:
- `acesPlayer1: int` (default 0)
- `acesPlayer2: int` (default 0)

Inklusive Getter/Setter nach bestehendem Muster.

### MatchScoreJpaEntity
`com.cas.tsas.match.infrastructure.persistence.entity.MatchScoreJpaEntity` bekommt zwei neue Spalten:
- `@Column(name = "aces_player1") int acesPlayer1`
- `@Column(name = "aces_player2") int acesPlayer2`

Die Datenbank wird via `ddl-auto: update` automatisch migriert (lokales H2 und PostgreSQL).

### MatchScoreResponse
`com.cas.tsas.match.infrastructure.web.dto.response.MatchScoreResponse` (record) bekommt zwei neue Felder:
- `int acesPlayer1`
- `int acesPlayer2`

Die `from(MatchScore)`-Factory-Methode mappt die neuen Felder.

### MatchScoreMapper
`MatchScoreMapper` mappt `acesPlayer1` und `acesPlayer2` in beide Richtungen (Entity ↔ Domain).

---

## Backend

### Use Case
Neues Interface `com.cas.tsas.match.application.port.in.RecordAceUseCase`:
```java
MatchScore recordAce(UUID matchId, boolean forPlayer1);
```

### ScoringService
`ScoringService` implementiert zusätzlich `RecordAceUseCase`:
1. Lädt `MatchScore` via `LoadMatchScorePort`
2. Erhöht `acesPlayer1` bzw. `acesPlayer2` um 1
3. Ruft die bestehende Scoring-Logik auf (identisch zu `scorePlayer1`/`scorePlayer2`)
4. Speichert via `SaveMatchScorePort` und gibt das aktualisierte `MatchScore` zurück

### MatchController
Zwei neue Endpoints analog zu den bestehenden Score-Endpoints:
```
POST /api/matches/{id}/ace/player1  → recordAce(id, true)
POST /api/matches/{id}/ace/player2  → recordAce(id, false)
```
Beide geben `MatchScoreResponse` zurück.

---

## Frontend

### MatchScore Model
`frontend/src/app/core/models/match.model.ts`: `MatchScore`-Interface bekommt:
```typescript
acesPlayer1: number;
acesPlayer2: number;
```

### ApiService
Zwei neue Methoden:
```typescript
acePlayer1(matchId: string): Observable<MatchScore>
acePlayer2(matchId: string): Observable<MatchScore>
```

### ScoreComponent — Layout (Option A)

Die Ass-Buttons erscheinen im dunkelblauen Aussenfeld unterhalb des Courts, je einer pro Spielerseite. Die bestehenden Buttons ("Score korrigieren", "Match beenden") bleiben zentriert.

```
[ Ass Sp.1 ]   [ ✏ Score korr. ]   [ Ass Sp.2 ]
               [ ⏹ Match beenden ]
```

Klick auf Ass-Button:
1. Ruft `acePlayer1()` bzw. `acePlayer2()` auf
2. Aktualisiert `matchData`-Signal (identisch zu `scorePoint()`)
3. Buttons sind deaktiviert wenn `matchData().status === 'COMPLETED'`

---

## Fehlerbehandlung

- Fehler beim Ass-Endpoint: SnackBar "Fehler beim Speichern" (identisch zu bestehendem Pattern)
- Kein separates Error-Handling nötig

---

## Out of Scope

- Korrektur von Assen (kein "Ass rückgängig")
- Anzeige der Ass-Zähler in einer separaten Statistik-Seite (V2)
