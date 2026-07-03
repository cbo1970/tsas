# TEN-36 — Statistik-Seite: Satz-für-Satz-Aufschlüsselung

**Linear:** [TEN-36](https://linear.app/tennis-score-and-statistic/issue/TEN-36/statistik-page-satz-fur-satz-aufschlusselung)
**Status:** Design abgenommen
**Datum:** 2026-06-26

## Ziel

Auf der Match-Statistik-Seite (`/matches/:id/statistics`) kann der Benutzer über einen Umschalter zwischen **«Gesamt»** und **«Satz 1», «Satz 2», …** wechseln. Die angezeigten Statistiken (Winners, Errors, Aufschlag %, Break Points, Schlag-/Richtungsverteilung) beziehen sich dann nur auf den gewählten Satz. Gesamt = bisheriges Verhalten.

## Abgenommene Entscheidung

**Bundle-API:** `GET /api/matches/{id}/statistics` liefert die Gesamt-Werte **und** zusätzlich ein `sets`-Array (ein Eintrag je gespieltem Satz) in **einem** Request. Das Umschalten der Tabs passiert rein clientseitig (kein Reload, kein Spinner). Kein neuer Query-Param.

## Architektur-Überblick

```
Frontend statistics.component
  Segmented Control [Gesamt | Satz 1 | Satz 2 | …]  → selectedView (Signal)
  Stat-Grid bindet an activeStats (computed: Gesamt oder gewählter Satz)
        ▲ ein Request
GET /api/matches/{id}/statistics
  MatchStatisticsController → ComputeMatchStatisticsUseCase.computeBreakdown(id)
  MatchStatisticsService: computeFrom(points)  (wiederverwendet)
    total  = computeFrom(alle Points)
    sets[] = je set_number: computeFrom(Points dieses Satzes)
```

**Leitplanke:** Das bestehende `MatchStatistics`-Record bleibt **unverändert** (das ai-module konsumiert es via `compute(matchId)`); die Satz-Aufschlüsselung kommt über neue, additive Typen + eine neue Use-Case-Methode.

## Komponente 1 — Backend (statistics-module)

### Berechnung wiederverwendbar machen
`MatchStatisticsService` (`application/service`): die bestehende Akkumulier-Schleife wird in einen privaten Helfer extrahiert:

```java
private MatchStatistics computeFrom(UUID matchId, List<Point> points) { … bestehende Logik … }
```

- `compute(UUID matchId)` (unverändertes Verhalten): lädt alle Points via `LoadPointsByMatchPort` und ruft `computeFrom(matchId, allPoints)`.
- Neu `computeBreakdown(UUID matchId)`: lädt alle Points einmal, berechnet `total = computeFrom(matchId, allPoints)` und gruppiert die Points nach `point.getSetNumber()`; je **vorhandener** Satznummer (aufsteigend sortiert) `computeFrom(matchId, pointsOfSet)`.

### Neue Domain-Typen
```java
public record SetStatistics(int setNumber, MatchStatistics stats) {}
public record MatchStatisticsBreakdown(MatchStatistics total, List<SetStatistics> sets) {}
```

### Neue In-Port-Methode
`ComputeMatchStatisticsUseCase` bekommt zusätzlich:
```java
MatchStatisticsBreakdown computeBreakdown(UUID matchId);
```
(`compute(UUID)` bleibt — das ai-module nutzt es weiter.)

### REST + DTO
- `MatchStatisticsController.getStatistics` ruft neu `computeBreakdown(id)` (Owner-Check `getMatchUseCase.findById(id)` bleibt → 404).
- `MatchStatisticsDto` behält seine flachen Gesamt-Felder (`matchId`, `player1`, `player2`, `totalPoints`) **rückwärtskompatibel** und bekommt zusätzlich:
  ```java
  List<SetStatisticsDto> sets
  // SetStatisticsDto(int setNumber, PlayerStatisticsDto player1, PlayerStatisticsDto player2, int totalPoints)
  ```
  Die Factory `MatchStatisticsDto.from(MatchStatisticsBreakdown)` füllt die Gesamt-Felder aus `total` und `sets` aus den `SetStatistics`.

## Komponente 2 — Frontend (statistics.component)

### Model
`statistics.model.ts`: `MatchStatistics` bekommt optional:
```ts
sets?: { setNumber: number; player1: PlayerStatistics; player2: PlayerStatistics; totalPoints: number }[];
```

### Komponente
- Signal `selectedView = signal<'total' | number>('total')`.
- `computed sets()` = `stats()?.sets ?? []`.
- `computed activeStats()` = je nach `selectedView`: bei `'total'` die `stats()!.player1/player2`; bei einer Satznummer der passende `sets()`-Eintrag; fällt sicher auf Gesamt zurück, wenn die Nummer nicht (mehr) existiert.
- **Stat-Grid-Zeilen binden an `activeStats()`** statt direkt an `stats().player1/2`. (Reine Bindings-Umstellung, gleiche Felder.)
- **Segmented Control** oberhalb der Tabelle (über dem `divider`/`stat-grid`): Button «Gesamt» (`data-testid="set-tab-total"`) + je Eintrag in `sets()` ein Button «Satz N» (`data-testid="set-tab-{N}"`), aktiver Button hervorgehoben. Klick setzt `selectedView`.
- **Unverändert:** Spielernamen-Kopf und die Satz-Score-Badges (zeigen weiter alle Sätze).

### Stil (Design-System „Daylight · Hard Court")
Schlanke, eigene CSS-Leiste:
- Container: `display:flex; gap:6px;` zentriert, `margin-bottom`.
- Button: weisser Grund, `1px solid var(--text)` (schwarzer Rahmen), `var(--text)`-Schrift, `border-radius: var(--radius-pill)`, kleine Schrift/Padding.
- Aktiv: `background: var(--brand); border-color: var(--brand); color:#fff;` (analog zu den jüngsten Score-/Stat-Anpassungen).

## Komponente 3 — i18n
Keine neuen Backend-Keys. Frontend-Labels: «Gesamt» und «Satz {{n}}». Konsistenz mit der bestehenden Seite (die Statistik-Seite verwendet aktuell deutsche Inline-Texte wie „Satz {{ $index+1 }}"); daher werden «Gesamt»/«Satz N» als Inline-Texte gesetzt (kein ngx-translate auf dieser Seite nötig, da sie bereits ohne `TranslatePipe` arbeitet).

## Umfang / Nicht-Ziele (YAGNI)
- Nur die Match-Statistik-Seite (FA-17). Head-to-Head bleibt unberührt.
- Keine Persistenz — weiterhin Laufzeit-Berechnung aus `points`.
- Keine neuen Metriken — dieselben Werte, nur satzgefiltert.
- Keine API-Query-Parameter; keine Änderung an `MatchStatistics` oder am ai-module.

## Teststrategie
- **Backend Unit** (`MatchStatisticsServiceTest` o. ä.): Punkte über **2 Sätze** → `computeBreakdown` liefert korrekte Gesamt-Werte **und** je Satz die richtigen Teil-Statistiken (Winners/Errors/Break-Points), `sets` aufsteigend sortiert; ein Satz ohne Punkte erscheint nicht. Der bestehende `compute(matchId)`-Test bleibt unverändert grün.
- **Backend API-IT** (`StatisticsApiIT`): `GET …/statistics` liefert das `sets`-Array mit den erwarteten `setNumber`-Einträgen plus unveränderte Gesamt-Felder.
- **Frontend Cypress** (`statistics.component.cy.ts`): Segmented Control rendert «Gesamt» + einen Button je Satz aus der gemockten Antwort; Klick auf «Satz 1»/«Satz 2» ändert die Werte im Grid; «Gesamt» ist Default; bestehende Gesamt-Asserts bleiben gültig.

## DoD
- Backend `./gradlew check` grün (inkl. Coverage-Gate 85 %/70 %).
- Frontend Vitest + Cypress grün.
- Umschalter funktioniert visuell (Sicht-Check): Werte ändern sich pro Satz, Gesamt entspricht dem bisherigen Stand.

## Betroffene/neue Dateien (Überblick)
**Backend**
- Neu: `statistics-module/.../domain/model/SetStatistics.java`, `MatchStatisticsBreakdown.java`
- Neu: `statistics-module/.../infrastructure/web/dto/SetStatisticsDto.java`
- Geändert: `application/port/in/ComputeMatchStatisticsUseCase.java` (+`computeBreakdown`), `application/service/MatchStatisticsService.java` (Helfer + Breakdown), `infrastructure/web/MatchStatisticsController.java`, `infrastructure/web/dto/MatchStatisticsDto.java`

**Frontend**
- Geändert: `core/models/statistics.model.ts`, `features/matches/statistics/statistics.component.ts` (+ `.html` falls Template separat — aktuell inline), `statistics.component.cy.ts`

**Tests**
- Geändert/neu: `statistics-module`-Service-Test (Breakdown), `app/.../statistics/StatisticsApiIT.java`, `statistics.component.cy.ts`
