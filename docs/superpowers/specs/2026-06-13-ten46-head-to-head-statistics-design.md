# Head-to-Head-Statistik (FA-08, V1) — Design

**Ticket:** TEN-46
**Datum:** 2026-06-13
**Scope:** Backend-Endpoint + Aggregation, SAD-Aktualisierung, Angular-UI (eigene Route + Einstieg auf der Players-Page)

## Kontext

Use Case FA-08 (SAD §10.1) ist dokumentiert, aber nicht implementiert. Heute existiert nur die
**Match-Statistik** (`ComputeMatchStatisticsUseCase`, `GET /api/matches/{id}/statistics`) — Kennzahlen
für ein einzelnes Match. FA-08 fordert einen **Head-to-Head-Vergleich zweier Spieler über alle
gemeinsamen Matches**.

Endpoint laut SAD §10.1:

```
GET /api/statistics/head-to-head?player1={id}&player2={id}
```

## Architektur-Entscheidung: Aggregation

Per-Match-Statistiken können **nicht** zu Prozentwerten gemittelt werden. Stattdessen werden über
alle gemeinsamen Matches **Roh-Zähler je realer Spieler-UUID** akkumuliert und die Prozentwerte
**einmal am Ende** abgeleitet (Denominator 0 → 0).

Wichtiger Twist: derselbe reale Spieler kann in Match A „Spieler 1" und in Match B „Spieler 2" sein.
Die positionsbasierte 1/2-Sicht eines Points wird daher pro Match über `match.player1Id` /
`match.player2Id` auf die reale UUID gemappt und dort akkumuliert.

Verworfene Alternativen:
- **Per-Match-Service wiederverwenden und summieren** — falsch für Prozentwerte; berechnet zudem
  nicht die FA-08-Kennzahlen (Serve-Won%, Return%, Return-Games%).
- **SQL-Aggregation** — Over-Engineering: die Match-Anzahl *pro Paar* ist klein (die 500 aus QZ-04
  ist die *Gesamt*-DB-Größe), und Tennislogik würde in SQL zerstreut.

## 1. Modul-Verdrahtung & Datenzugriff

- **Heimat:** Erweiterung von `statistics-module` (hängt bereits an `match-module`). Neu:
  `implementation(project(":player-module"))`, um die Existenz beider Spieler über das bestehende
  `LoadPlayerPort` zu prüfen (404).
- **Neuer Output-Port** in `match-module`:
  `LoadMatchesByPlayersPort.loadMatchesBetween(UUID a, UUID b): List<Match>` — alle Matches, deren
  `{player1,player2}` dem ungeordneten Paar entspricht. Implementierung in `MatchPersistenceAdapter`
  über neue `MatchJpaRepository`-Query: `(player1Id=a AND player2Id=b) OR (player1Id=b AND player2Id=a)`.
- **Wiederverwendete Ports:** `LoadPointsByMatchPort` (Points je Match), `LoadMatchScorePort`
  (Endstand für Bilanz), `LoadPlayerPort` (Existenz → 404).

## 2. Backend-Berechnung (`HeadToHeadStatisticsService`)

Neuer Input-Port `ComputeHeadToHeadStatisticsUseCase.compute(UUID player1Id, UUID player2Id)` und
Service-Implementierung (`@Service`, `@Transactional(readOnly = true)`), Vorbild
`MatchStatisticsService`.

Ablauf: Existenz beider Spieler prüfen → Matches des Paares laden → je Match Points + `MatchScore`
laden → Roh-Zähler je realer Spieler-UUID akkumulieren → Prozentwerte ableiten.

Kennzahlen je Spieler (Roh-Zähler, daraus Prozentwerte):

- **Aufschlag:** First Serve% (`firstServesIn / serveAttemptsTotal`), First Serve Won%
  (`serveAttempt==1` & `winner==servingPlayer`), Second Serve Won% (`serveAttempt==2`), Aces (abs.),
  Double Faults (abs.).
- **Return:** Return Points Won% auf 1. Aufschlag (Komplement: Returner gewinnt Punkt auf
  gegnerischem `serveAttempt==1`), Return Points Won% auf 2. Aufschlag, Break Points Won%
  (`isBreakPoint` als Returner: gewonnen/gespielt), Return Games Won%.
- **Rallye:** Winners% (`winners / totalPoints`), Unforced Error% (`unforcedErrors / totalPoints`),
  **Net Points Won% → in V1 ausgelassen** (`null`, siehe unten).
- **Match-Bilanz (nur abgeschlossene Matches, `MatchScore.winner != null`):** Siege/Niederlagen,
  Satzbilanz (`setsPlayer1/2`).

**Return Games Won%** — Ableitung: Points je Match nach `(setNumber, gameNumber)` gruppieren.
Server des Games = `servingPlayer` der Points im Game; Game-Gewinner = `winner` des Points mit dem
höchsten `pointNumber` im Game. Ein Return-Game für Spieler X = Game, in dem X **nicht** Server war;
gewonnen, wenn der Game-Gewinner X ist. Aggregiert zu `returnGamesWon / returnGamesPlayed`.

**Einschluss-Regel:** Punkt-Kennzahlen aggregieren über **alle** gemeinsamen Matches mit
erfassten Points (inkl. laufender). **Match-/Satzbilanz zählt nur abgeschlossene Matches.**

**Net Points Won% — Auslassung (V1):** Das `Point`-Modell hat keine explizite „Netzangriff"-Markierung.
Das einzige Netz-Signal (`StrokeType.VOLLEY/SMASH`) ist als Heuristik nicht belastbar. FA-08-Kennzahl
wird in V1 als `null` zurückgegeben; SAD vermerkt, dass dafür ein explizites Net-Approach-Flag auf
`Point` nötig ist.

## 3. API-Vertrag

Neuer `HeadToHeadController` in `statistics-module` (`infrastructure/web`):

```
GET /api/statistics/head-to-head?player1={id}&player2={id}
```

- **200** → `HeadToHeadStatisticsDto { player1Id, player2Id, matchesPlayed, player1, player2 }`,
  wobei `player1`/`player2` vom Typ `H2HPlayerStatsDto` sind und **sowohl Roh-Zähler als auch
  abgeleitete Prozentwerte** tragen (UI kann „23 (45 %)" zeigen). `netPointsWonPercentage` wird als
  `null` serialisiert.
- **404** wenn ein Spieler nicht existiert (`PlayerNotFoundException` über bestehenden
  `GlobalExceptionHandler`, RFC-7807 ProblemDetail).
- **400** wenn `player1 == player2`.
- **200 mit Null-Statistik / `matchesPlayed=0`** wenn das Paar keine Matches hat (beide Spieler gültig).

`matchesPlayed` = Anzahl abgeschlossener Matches zwischen den Spielern.

## 4. Frontend (Angular)

- **Neue lazy Route** `/statistics/head-to-head` (`authGuard`) → standalone `HeadToHeadComponent`.
  Liest `player1`/`player2` aus Query-Params (direkt verlinkbar).
- **In-Page-Picker:** zwei `mat-select`, befüllt aus `api.getPlayers()` (aktive Spieler). Sind beide
  gewählt, wird abgerufen und gerendert.
- **Darstellung:** Wiederverwendung des bestehenden dunklen, gespiegelten Vergleichsbalken-Stils aus
  `statistics.component` (**kein ngx-charts** — nicht installiert). Abschnitte: Match-Bilanz,
  Aufschlag, Return, Rallye. Net-Points-Zeile bleibt ausgeblendet, solange ausgelassen.
- **Models/Service:** neues `HeadToHeadStatistics`-Interface in `statistics.model.ts`; neue Methode
  `api.getHeadToHead(player1, player2)` → `GET /api/statistics/head-to-head?player1=&player2=`.

**Einstiegspunkte auf der Players-Page (`players.component`):**
- **Toolbar-Button „Head-to-Head"** im `.page-header`, navigiert zu `/statistics/head-to-head`.
- **Zeilen-Aktion „Vergleichen"** (Icon-Button, z. B. `compare_arrows`) in der `actions`-Spalte,
  navigiert mit `?player1={id}` vorbelegt — im Ziel ist dann nur noch der zweite Spieler zu wählen.

## 5. Tests

- **Backend Unit:** `HeadToHeadStatisticsServiceTest` (Mockito, mockt `LoadMatchesByPlayersPort`,
  `LoadPointsByMatchPort`, `LoadMatchScorePort`, `LoadPlayerPort`), Vorbild
  `MatchStatisticsServiceTest` mit Inline-`p()`-Builder. Deckt ab: positions→UUID-Remapping,
  Serve-/Return-/Return-Games-Ableitungen, Bilanz aus Scores, leeres Paar, 404- und 400-Pfade.
- **Backend Integration:** `HeadToHeadApiIT` in `:app` (Testcontainers + MockMvc + `jwt()`), Vorbild
  `StatisticsApiIT`. Seedet zwei Spieler + Matches mit gemischten Positionen, prüft JSON sowie
  404/400.
- **Frontend Cypress Component Test:** `head-to-head.component.cy.ts` (Vorbild
  `statistics.component.cy.ts`); `players.component.cy.ts` wird um Assertion für Toolbar-Button und
  Zeilen-Aktion (Navigation mit vorbelegtem `player1`) erweitert.
- **Coverage-Gate** (85 % Line / 70 % Branch, aggregiert) muss grün bleiben.

## 6. SAD-Aktualisierung

- FA-08-Zeile (§10.1) als implementiert markieren.
- **Net Points Won%-Auslassung** dokumentieren (braucht explizites Net-Approach-Flag auf `Point`).
- **Einschluss-Regel** dokumentieren (Bilanz = nur abgeschlossene Matches; Punkt-Kennzahlen inkl.
  laufender Matches).
- **Return-Games-Ableitung** kurz festhalten.
- Neue Abhängigkeit `statistics-module → player-module` in der Bausteinsicht ergänzen.

## Offene/akzeptierte Annahmen

- Net Points Won% ist in V1 bewusst `null` (kein belastbares Datensignal).
- Bilanz nur über abgeschlossene Matches; Punkt-Kennzahlen über alle Matches mit Points.
- `player1 == player2` → 400.
- Performance: pro Paar wenige Matches → N+1-Laden von Points/Score je Match ist unkritisch und
  klar < 60 s (QZ-04).
