# Serving Player Feature — Design Spec

**Datum:** 2026-04-12
**Branch:** develop

---

## Zusammenfassung

Im Score-Screen wird einmalig zu Beginn des Matches festgelegt, wer aufschlägt. Danach wechselt das Aufschlagrecht automatisch nach jedem Spiel. Der Ass-Button ist nur für den aufschlagenden Spieler aktiv — für den Returnspieler ist er deaktiviert. Solange kein Aufschlagspieler gewählt wurde, sind beide Ass-Buttons deaktiviert.

---

## Datenmodell

### MatchScore (Domain)
`com.cas.tsas.match.domain.model.MatchScore` bekommt ein neues Feld:
- `Integer servingPlayer` — `null` = nicht gesetzt, `1` = Spieler 1, `2` = Spieler 2

Inklusive Getter/Setter nach bestehendem Muster. Initialwert beim Match-Erstellen: `null`.

### MatchScoreJpaEntity
`com.cas.tsas.match.infrastructure.persistence.entity.MatchScoreJpaEntity` bekommt:
- `@Column(name = "serving_player") private Integer servingPlayer;`

Spalte ist nullable. Migration via `ddl-auto: update`.

### MatchScoreResponse
`com.cas.tsas.match.infrastructure.web.dto.response.MatchScoreResponse` (record) bekommt:
- `Integer servingPlayer`

Die `from(MatchScore)`-Factory-Methode mappt das neue Feld.

### MatchScoreMapper
Mappt `servingPlayer` in beide Richtungen (Entity ↔ Domain).

---

## Backend

### Use Case
Neues Interface `com.cas.tsas.match.application.port.in.SetServingPlayerUseCase`:
```java
MatchScore setServingPlayer(SetServingPlayerCommand command);

record SetServingPlayerCommand(
        UUID matchId,
        boolean forPlayer1
) {}
```

### MatchService implementiert SetServingPlayerUseCase
1. Lädt Match via `LoadMatchPort` — wirft `MatchNotFoundException` wenn nicht gefunden
2. Guard: wirft `IllegalStateException("Match is already completed")` wenn `COMPLETED`
3. Lädt `MatchScore` via `LoadMatchScorePort`
4. Setzt `score.setServingPlayer(forPlayer1 ? 1 : 2)`
5. Speichert via `SaveMatchScorePort` und gibt Score zurück

### ScoringService — automatischer Aufschlagwechsel
In `awardGame()`, nach jeder Spielvergabe (reguläres Spiel und Tiebreak-Ende):
```java
if (score.getServingPlayer() != null) {
    score.setServingPlayer(score.getServingPlayer() == 1 ? 2 : 1);
}
```

### MatchService.recordAce() — Validierung
Vor dem Ass-Eintrag wird geprüft (nach dem COMPLETED-Guard):
- `score.getServingPlayer() == null` → `IllegalStateException("No serving player set")`
- `forPlayer1 && score.getServingPlayer() != 1` → `IllegalStateException("Player 1 is not serving")`
- `!forPlayer1 && score.getServingPlayer() != 2` → `IllegalStateException("Player 2 is not serving")`

### MatchController
Zwei neue Endpoints analog zu den Ace-Endpoints:
```
POST /api/matches/{id}/serve/player1  → setServingPlayer(id, true)
POST /api/matches/{id}/serve/player2  → setServingPlayer(id, false)
```
Beide geben `MatchScoreResponse` zurück.

---

## Frontend

### MatchScore Model
`frontend/src/app/core/models/match.model.ts`: `MatchScore`-Interface bekommt:
```typescript
servingPlayer: number | null;
```

### ApiService
Zwei neue Methoden:
```typescript
setServingPlayer1(matchId: string): Observable<MatchScore>
setServingPlayer2(matchId: string): Observable<MatchScore>
```

### ScoreComponent — Logik

Neues computed signal:
```typescript
servingPlayer = computed(() => this.matchData()?.score?.servingPlayer ?? null);
```

Neue `setServe(forPlayer1: boolean)`-Methode — analog zu `recordAce()`:
- Ruft `setServingPlayer1()` bzw. `setServingPlayer2()` auf
- Aktualisiert `matchData`-Signal
- Fehler: SnackBar "Fehler beim Speichern"

### ScoreComponent — Ass-Button disabled-Bedingung

```typescript
// Player 1 ace button:
[disabled]="matchData()!.status === 'COMPLETED' || servingPlayer() !== 1"

// Player 2 ace button:
[disabled]="matchData()!.status === 'COMPLETED' || servingPlayer() !== 2"
```

Beide Buttons sind deaktiviert wenn `servingPlayer === null` (weil `null !== 1` und `null !== 2`).

### ScoreComponent — UI-Änderungen

**Wenn `servingPlayer === null` und Match `IN_PROGRESS`:**

Ein "Aufschlag festlegen"-Toggle erscheint oberhalb der Action-Buttons in der `bottom-area`:

```
[ Ass Sp.1 ]  [ ● Sp.1 serviert ]  [ ● Sp.2 serviert ]  [ Ass Sp.2 ]
              [ ✏ Score korrigieren ]
              [ ⏹ Match beenden    ]
```

Die zwei Toggle-Buttons sind styled als kleine Ghost-Buttons (outline, weiss), beschriftet mit dem Spielernamen. Klick ruft `setServe(true/false)` auf.

**Sobald Aufschlagspieler gesetzt:**

- Der Toggle verschwindet
- Ein kleiner Indikator (🎾) erscheint in der Player-Overlay-Karte des aufschlagenden Spielers auf dem Court
- Ass-Button des Returnspielers ist deaktiviert (opacity 0.4)

---

## Fehlerbehandlung

- Ass-Endpoint gibt 409 zurück wenn kein Aufschlagspieler gesetzt oder falscher Spieler aufschlägt
- Frontend: SnackBar "Fehler beim Speichern" (identisch zu bestehendem Pattern)

---

## Out of Scope

- Aufschlagwechsel innerhalb eines Tiebreaks (alle 2 Punkte) — V2
- Korrektur des Aufschlagspielers nach dem Setzen
- Anzeige des Aufschlagwechsels in der Statistik
