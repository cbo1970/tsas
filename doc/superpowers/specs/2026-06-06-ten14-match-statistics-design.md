# TEN-14 — Match-Statistik-Seite

## Kontext

Nach einem abgeschlossenen Match soll der Benutzer automatisch auf eine Statistik-Seite weitergeleitet werden. Die Seite zeigt einen Spieler-Vergleich im ATP-Stil mit allen bereits berechneten Match-Statistiken.

Einstieg aus der Spielerliste / Match-History ist separates Ticket TEN-35.

## Routing

Neue Route: `/matches/:id/statistics` → `StatisticsComponent` (lazy-loaded, standalone)

Set-Scores und Spielernamen werden als Query-Parameter übergeben, damit die Statistik-Seite den Score-Header ohne zusätzlichen API-Call rendern kann:

```
/matches/123/statistics?sets=6-4,3-6,7-5&p1=Müller&p2=Meier
```

Navigation nach Matchende: `ScoreComponent` erkennt `score.isDone === true` (via `handlePointResponse`) und navigiert mit `router.navigate(['/matches', matchId, 'statistics'], { queryParams })` statt zu `/players`.

## Backend

### Neuer Endpoint

```
GET /api/matches/{id}/statistics
→ 200 MatchStatisticsDto
→ 404 wenn Match nicht existiert (GlobalExceptionHandler)
```

### MatchStatisticsDto

```java
public record MatchStatisticsDto(
    UUID matchId,
    PlayerStatisticsDto player1,
    PlayerStatisticsDto player2,
    int totalPoints
) {}

public record PlayerStatisticsDto(
    int pointsWon,
    int winners,
    int unforcedErrors,
    int forcedErrors,
    int aces,
    int doubleFaults,
    double firstServePercentage,
    double secondServePercentage,
    int breakPointsWon,
    int breakPointsFaced,
    double forehandPercentage
) {}
```

`forehandPercentage` wird aus `StrokeDistribution` berechnet: `forehand / (forehand + backhand)`, oder `0.0` wenn keine Schlagart-Daten vorliegen. `computedAt`, `breakPointsTotal` und `directionDistribution` werden nicht serialisiert.

### Neuer Controller

`MatchStatisticsController` im `statistics-module` unter `infrastructure/web/`:

```java
@RestController
@RequestMapping("/api/matches/{id}/statistics")
public class MatchStatisticsController {
    @GetMapping
    public ResponseEntity<MatchStatisticsDto> getStatistics(@PathVariable UUID id) { ... }
}
```

Kein Caching — `ComputeMatchStatisticsUseCase.compute()` wird bei jedem Request neu aufgerufen.

## Frontend

### Neue Dateien

- `frontend/src/app/features/matches/statistics/statistics.component.ts`
- `frontend/src/app/features/matches/statistics/statistics.component.html`
- `frontend/src/app/features/matches/statistics/statistics.component.cy.ts`

### Neues Interface

```typescript
// frontend/src/app/core/models/statistics.model.ts
export interface PlayerStatistics {
  pointsWon: number;
  winners: number;
  unforcedErrors: number;
  forcedErrors: number;
  aces: number;
  doubleFaults: number;
  firstServePercentage: number;
  secondServePercentage: number;
  breakPointsWon: number;
  breakPointsFaced: number;
  forehandPercentage: number;
}

export interface MatchStatistics {
  matchId: string;
  player1: PlayerStatistics;
  player2: PlayerStatistics;
  totalPoints: number;
}
```

### StatisticsComponent (Signals)

```typescript
stats   = signal<MatchStatistics | null>(null)
setScores = signal<{ p1: number; p2: number }[]>([])
p1Name  = signal<string>('')
p2Name  = signal<string>('')
```

Query-Parameter `sets` wird als `6-4,3-6,7-5` übergeben und in `{ p1, p2 }[]` geparst.

### Template-Layout (ATP-Stil)

1. **Spielernamen** — zentriert, je Hälfte (3-Spalten-Grid)
2. **Set-Score-Tabelle** — eine Zeile pro Satz; Gewinner-Badge (`background: primary`) hervorgehoben, Verlierer gedimmt
3. **Trennlinie**
4. **Statistik-Grid** (`40px | 1fr | 40px`):
   - Gewonnene Punkte
   - Asse · Winners · Forced Errors · Unforced Errors · Doppelfehler
   - *(Trennlinie)*
   - 1. Aufschlag % · 2. Aufschlag %
   - Break Points (Format `gewonnen/gespielt`)
   - *(Trennlinie)*
   - Vorhand % · Rückhand %
5. **"Zur Spielerübersicht"-Button** → `/players`

**Balken-Farben:** Jeder Balken zeigt P1-Anteil links, P2-Anteil rechts. Bei Gewinn-Stats (Winners, Asse) ist der höhere Wert primär-farbig. Bei Fehler-Stats (Unforced/Forced/DF) ist der *niedrigere* Wert grün — weniger Fehler ist besser.

### ApiService

```typescript
getMatchStatistics(matchId: string): Observable<MatchStatistics> {
  return this.http.get<MatchStatistics>(`/api/matches/${matchId}/statistics`);
}
```

### Änderungen am Score-Screen

In `handlePointResponse`: wenn `updated.score.isDone`, navigiere zu Statistics statt `loadMatch()` aufzurufen. Da `handlePointResponse` das letzte Satzergebnis bereits via `setHistory.update()` eingetragen hat (wenn `currentSet` gestiegen ist), enthält `setHistory()` zum Navigationszeitpunkt alle Sätze. Falls `currentSet` beim finalen Punkt *nicht* steigt (Backend-abhängig), muss der Implementierer prüfen, ob der aktuelle Satz noch in die History eingefügt werden muss.

```typescript
const sets = this.setHistory()
  .map(s => `${s.p1}-${s.p2}`)
  .join(',');
this.router.navigate(
  ['/matches', this.matchId, 'statistics'],
  { queryParams: { sets, p1: this.player1Name(), p2: this.player2Name() } }
);
```

## Tests

### Backend (JUnit / MockMvc)

- `GET /api/matches/{id}/statistics` auf abgeschlossenem Match → 200, alle Felder vorhanden
- `GET /api/matches/{unknown-id}/statistics` → 404

### Frontend (Cypress)

- Alle Statistik-Zeilen werden gerendert
- Set-Score-Zeilen entsprechen den Query-Parametern
- Gewinner-Badge ist hervorgehoben, Verlierer-Badge gedimmt
- "Zur Spielerübersicht"-Button navigiert zu `/players`

## Nicht in Scope

- Einstieg aus Spielerliste / Match-History → TEN-35
- Caching der Statistik-Berechnung
- Satz-für-Satz-Aufschlüsselung der Statistiken
- Tiebreak-Anzeige im Set-Score (Score kommt direkt vom Score-Screen via Query-Param)
