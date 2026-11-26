# SDD ledger — plan: /Users/cbo/Projects/cas/tsas/.claude/worktrees/mobile-portrait-ui/doc/superpowers/plans/2026-11-25-mobile-portrait-ui.md

**Spec:** `doc/superpowers/specs/2026-11-25-mobile-portrait-ui-design.md` (im Worktree, Commit 978d918)
**Worktree:** `/Users/cbo/Projects/cas/tsas/.claude/worktrees/mobile-portrait-ui`, Branch `worktree-mobile-portrait-ui`, abgezweigt von `origin/develop` (b2e695d)
**Tickets:** TEN-73 (T1–T2), TEN-71 (T3–T7), TEN-74 (T8–T10), TEN-75 (T11), TEN-76 (T12)

## Baseline (vor Task 1)

- `npx ng test --no-watch` → 3 Dateien, 26 Tests, 0 Fehler
- `npx cypress run --component` → 8 Specs, 83 Tests, 0 Fehler
- Commit 978d918 `docs(superpowers): Design und Umsetzungsplan …` (Spec + Plan in den Branch geholt, waren untracked)

## Pre-flight-Scan

### Paare, die Dateien oder Schnittstellen teilen

| Paar | Produziert / Konsumiert | Befund |
|---|---|---|
| T1 → T2, T8, T11 | `LayoutService.isHandset` | sauber, Signatur überall identisch |
| T3 → T5 | `score.component.html`; T3-Test prüft `[data-testid="panel"]` | **Konflikt** — diese ID entsteht erst in T5, der Test würde in T3 leer bestehen |
| T5 → T6 | Panel-Input `serveAttempt` vs. Ersetzen von `serviceContextP1/P2` | **Konflikt** — T5 deklariert ein einzelnes `serveAttempt`, T6 schafft es erst |
| T4 → T6 | `[data-testid="score-header"]` | sauber, T4 erzeugt, T6 prüft |
| T5 → T7 | `[data-testid="panel"]` | sauber, T7 prüft Abwesenheit bei beendetem Match |
| T7 → T8 | `score.component.ts:431` (`ScoreEditDialogComponent`) | **Konflikt** — Zeilennummer verschiebt sich durch T4–T7 |
| T8 → T10 | `player-dialog.component.ts` | sauber, T8 Breite, T10 Vorbefüllung |
| T9 → T10 | `match-setup.component.html` | **Lücke** — T10-Tests nutzen `[data-testid="player2-input"]`, das kein Vorgänger anlegt |
| T8 → T11 | `players.component.ts` | sauber, T8 vor T11 |
| T11 | `players.component.cy.ts` als „Create" geführt | **Korrektur** — Datei existiert bereits, ist zu ändern |

### Tasks, die mit sich selbst im Reinen sind

| Task | Befund |
|---|---|
| T1 | sauber |
| T2 | **Lücke** — Plan sagt nicht, wie im Komponententest die Route gesetzt wird |
| T3 | sauber bis auf den Paar-Konflikt oben; `setServe` ist heute `private` (Zeile 403) |
| T4 | sauber |
| T5 | sauber — die heutige Fachprüfung in Zeile 320–324 verbietet ACE/DOUBLE_FAULT bereits auf dem falschen Panel, deckt sich mit dem rollenabhängigen Panel |
| T6 | sauber |
| T7 | sauber — `[data-testid="player-notes"]` existiert heute in `player-notes.component.html` |
| T8 | **Risiko** — Selektor `.mat-mdc-dialog-surface` ist eine Material-Interna |
| T9 | sauber |
| T10 | sauber bis auf die Lücke oben |
| T11 | sauber bis auf die Korrektur oben |
| T12 | keine TDD-Aufgabe, absichtlich |

### Rulings

- **Ruling:** T3 prüft statt `[data-testid="panel"]` die heute vorhandenen `[data-testid="panel-p1"]`; T5 zieht die Assertion auf `panel` um — ein Test, der eine noch nicht existierende ID auf Abwesenheit prüft, beweist nichts. Kostet bei Irrtum: eine Assertion in T5 nachziehen.
- **Ruling:** T5 reicht den Service-Zustand des aktiven Spielers in den Panel-Input `serveAttempt`; T6 fasst danach `serviceContextP1/P2` zu einem Signal zusammen, ohne die Panel-Schnittstelle zu ändern. Die Schnittstelle ist über beide Tasks stabil. Kostet bei Irrtum: eine Umbenennung in T6.
- **Ruling:** T8 sucht die `dialog.open`-Aufrufe über den Komponentennamen, nicht über Zeilennummern. Die Angabe `:431` im Plan ist ab T4 veraltet. Kostet bei Irrtum: nichts, der Name ist eindeutig.
- **Ruling:** T10 legt die Testids `player1-input` und `player2-input` selbst an. Kostet bei Irrtum: nichts.
- **Ruling:** T11 ändert `players.component.cy.ts`, statt sie anzulegen. Kostet bei Irrtum: nichts.
- **Ruling:** T2 darf die Route über einen gestubbten `Router`/`ActivatedRoute` setzen statt über echte Navigation — ein Komponententest mountet `App` ohne Router-Outlet-Ziel. Kostet bei Irrtum: der Test prüft die Ableitung aus der URL statt echte Navigation; die echte Navigation deckt der Schlussdurchgang ab.
- **Ruling:** T3 macht `setServe` von aussen aufrufbar oder ruft denselben API-Pfad; der heutige implizite Pfad (Zeile 308/318: erster Tipp setzt den Aufschläger) entfällt ersatzlos, so wie die Spec es verlangt. Kostet bei Irrtum: Rückbau einer Sichtbarkeit.
- **Ruling:** T8 darf den Dialog-Container über den tatsächlich gerenderten Klassennamen ansprechen, falls `.mat-mdc-dialog-surface` in Material 21 abweicht. Kostet bei Irrtum: ein Selektor.
- **Ruling:** Die Spec verlangt „ein PR je Ticket". Die Umsetzung läuft auf **einem** Worktree-Branch; fünf PRs nacheinander aus einem Worktree hiesse, zwischen den Tickets auf Merges zu warten. Entschieden: alle Tasks auf diesem Branch, Aufteilung in PRs wird am Ende mit dem Auftraggeber entschieden (ein PR eröffnen ist ohnehin eine Aktion, die eine Rückfrage braucht). Kostet bei Irrtum: ein Branch muss nachträglich in fünf aufgeteilt werden.

## Fortschritt

- Task 1: complete (commits 978d918..81fb281, review clean)
  - `LayoutService` mit `readonly isHandset: Signal<boolean>`, `providedIn: 'root'`, aus `BreakpointObserver.observe(Breakpoints.Handset)` via `toSignal`, `initialValue: false`. Tests: 29 (Basis 26 + 3).
  - ⚠️ des Prüfers „Commit-Trailer nicht im Diff sichtbar" selbst geprüft: 81fb281 trägt `Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>` — kein Mangel.
  - ⚠️ des Prüfers „TDD-Lauf nicht nachvollziehbar" — der Report belegt RED (Spec kompilierte nicht, Service fehlte) und GREEN. Kein Mangel.
  - Minor nicht behoben: der Doc-Kommentar in `layout.service.ts` ist englisch, der Rest des Projekts gemischt. Kein Fix-Durchgang dafür.
- Task 1: minor (deferred): englischer Doc-Kommentar in `layout.service.ts:62-66`, Projekt sonst gemischt.

- Task 2: Review 1 (commits 81fb281..36033f3) → Spec ❌, 1 Important, 4 Minor. Fix-Durchgang 1 gestartet.
  - Important: `.bottom-nav` steht in `app.html:262` vor `<main>` und hat weder `order`, `margin-top:auto` noch `position`. Die Leiste landet dadurch oben — damit ist auch `env(safe-area-inset-bottom)` wirkungslos. Keiner der Cypress-Tests prüft die Position, nur `be.visible`.
- Task 2: minor (deferred): Export/Löschen/Abmelden sind zwischen Desktop- und Mobile-Menü dupliziert; `data-cy="logout"` existiert nur in der mobilen Kopie (`app.html:289-301`).
- Task 2: minor (deferred): `mat-chip` im `mat-menu` ist kein Menüeintrag und liegt ausserhalb der Tastaturnavigation (`app.html:278-282`).
- Task 2: minor (deferred): `aria-label="Sprache"` überschreibt den sichtbaren Text „Sprache (DE)", die aktuelle Sprache geht für Screenreader verloren (`app.html:283`).
- Task 2: minor (deferred): `<nav>` ohne `aria-label` (`app.html:263`).
- Task 2: fix round 1/5 (2 addressed, 0 open — Leiste unten verankert via `order: 1; position: sticky; bottom: 0`, Positions-Test ergänzt; commits 36033f3..d7b846d)
- Task 2: complete (commits 81fb281..d7b846d, review clean)
  - Cypress 90/90 + 1 neuer Positions-Test, Vitest 30/30. `app.spec.ts` bekam `provideRouter([{ path: '**', children: [] }])`, die zwei bestehenden Tests blieben inhaltlich gleich.
- Task 2: minor (deferred, aus der Nachprüfung): `:host` nutzt `min-height: 100vh/100dvh` — Inhalt in `main` mit eigener 100vh-Dimensionierung kann die Seite höher als den Viewport machen. Genau die alte Scoring-Ursache; für Tasks 3–7 mitgegeben.

- Task 3: Implementer meldete DONE_WITH_CONCERNS (commit ae0a692). Zwei davon vor der Prüfung behandelt:
  - **Plandefekt:** Der Brief nannte `PUT /matches/:id/serving-player-1`. Richtig sind `POST /matches/:id/serve/player1` bzw. `/serve/player2` (`api.service.ts:66,70`), Punkte gehen an `POST /matches/:id/points` (`:62`). Korrektur des Implementers bestätigt; für Tasks 4 und 6 übernommen.
  - **Ruling:** Solange `servingPlayer` leer ist, gehört der Aufschlagwahl der ganze Screen — die Kopfzeile mit `quick-score-p1/p2` wird gar nicht gerendert. Sonst lassen sich Punkte ohne Aufschläger erfassen, schlechter als der eben entfernte implizite Pfad, der den Aufschläger wenigstens setzte; `% 1./2. Service` zählt nur Punkte mit Aufschlagversuch. Grundlage: Design-Dokument „zeigt der Screen **vor** dem Panel eine Auswahl … Nach der Wahl erscheint das Panel". Kostet bei Irrtum: ein zusätzlicher Zwischenschritt, den man wieder entfernen kann.
  - Zur Prüfung mitgegeben: der Implementer hat zwei bestehende Vitest-Tests ersetzt, die den entfernten impliziten Pfad prüften.
- Task 3: complete (commits d7b846d..15d27e9, review clean — Spec ✅, keine Critical/Important)
  - `needsServer()` schaltet die ganze `.score-header` frei; `pickServer()` ersetzt das private `setServe`. Score-Spec 30/30, Vitest 31/31. Volle Cypress-Suite vom Controller nachgefahren: **96/96 grün**.
  - ⚠️ des Prüfers selbst geprüft: `goToMatches()` existiert auf `ScoreComponent` (heutiges Template), und `--brand-border`, `--brand-soft`, `--surface-muted`, `--text-muted`, `--text` sind in `styles.scss` auf `:root` definiert. Kein Mangel.
  - ⚠️ offen für alle Tasks: keine visuelle Kontrolle in echter Handy-Breite. Gehört in die Schlussabnahme am Gerät.
  - **Ruling:** Die Coach-Notizen rendern derzeit noch unter der Aufschlagwahl. Bleibt so — Task 7 entfernt das aufklappbare `<details>` ohnehin ersatzlos und ersetzt es durch die Vollbildansicht. Kostet bei Irrtum: nichts, der Zustand ist zwei Tasks lang sichtbar.
- Task 3: minor (deferred): `score.component.spec.ts:195-200` stubbt `recordPoint`, prüft aber nicht, dass es aufgerufen wurde — der Test bestünde auch, wenn `recordQuickPoint` gar nichts täte. Vorschlag des Prüfers: zusätzlich ein `needsServer()`-Guard in `recordQuickPoint`/`recordObservation` als zweite Sicherung zur Template-Gate.
- Task 3: minor (deferred): kein Test für den Fehlerzweig von `pickServer` (Snackbar) und für das `back`-Event des Zurück-Pfeils.
- Task 3: minor (deferred): kein Schutz gegen Doppeltipp auf eine Aufschlagwahl-Fläche (zwei POSTs; Idempotenz des Endpunkts ungeprüft).
- Task 3: minor (deferred): die neuen `@if`/`@else`-Blöcke in `score.component.html` lassen ihre Innenblöcke uneingerückt — vier weitere Tasks fassen dieses Template noch an.

- Task 4: Review 1 (commits 15d27e9..d418af6) → Spec ✅, 1 Important, 5 Minor. Fix-Durchgang 1 gestartet.
  - Important: der Tiebreak-Test (`score.component.cy.ts:54-60`) kann den Fehler, den er besitzt, nicht erkennen — bei `minmax(0,1fr)` plus `nowrap` führt waagerechtes Überlaufen nicht zu grösserer Höhe, und der Test prüft nur Höhe. Kurze Namen verdecken zusätzlich die Konkurrenz um die Breite.
  - ⚠️ des Prüfers selbst geprüft: `formatPoints` liegt ausserhalb des Diffs; längste Ausgabe sind zwei Zeichen (`15`/`30`/`40`, `A` bei Vorteil, zweistellige Tiebreak-Zahlen). Kein langes Wort wie „Vorteil". Kein Mangel.
  - Vor der Prüfung nachgefordert: der Implementer hatte die TDD-Belege selbst als lückenhaft gemeldet und hat sie ohne Codeänderung nachgetragen.
- Task 4: minor (deferred): 🎾-Marker für Spieler 2 steht als nachlaufender Text in einem ellipsierten `nowrap`-Span und wird bei langen Namen abgeschnitten (`score.component.html:155`); bei Spieler 1 bleibt der führende Marker sichtbar.
- Task 4: minor (deferred): `aria-label="Punkt für X"` ersetzt den Buttontext, Screenreader hören den Punktestand nicht mehr (`score.component.html:138,159`).
- Task 4: minor (deferred): der pointType-Test (`score.component.cy.ts:48-51`) besteht auch gegen die alte Kopfzeile — gültiger Regressionswächter, aber kein RED-zu-GREEN-Test.
- Task 4: minor (deferred): `.set-history` hat festes `line-height: 14px`; ein langer Fünf-Satz-Verlauf mit Tiebreak-Details ergäbe eine zweite Zeile (~99px, unter 100 aber ohne Reserve).
- Task 4: minor (deferred): Spielernamen werden bei 360px auf ~70px gequetscht und abgeschnitten, ohne Möglichkeit sie vollständig zu sehen.
- Task 4: fix round 1/5 (1 addressed, 0 open — Tiebreak-Test prüft jetzt waagerecht mit langen Namen, RED durch absichtlich verengte Spalten bewiesen (-93.69px Überlappung), danach 35/35; commits d418af6..9d9a50d)
- Task 4: complete (commits 15d27e9..9d9a50d, review clean)
  - Kopfzeile ~85px, Punktzahlen bleiben `<button>` mit `quick-score-p1/p2`. Score-Spec 35/35, Vitest 31/31.
- Task 4: minor (deferred, aus der Nachprüfung): die `<= 50`-Assertion auf die Buttonhöhe entfiel; Höhe bleibt über `<= 100` auf der Kopfzeile und den 44×44-Test abgedeckt.

- Task 5: Implementer meldete DONE_WITH_CONCERNS (commit b4774c9). Drei Rulings vor der Prüfung:
  - **Ruling:** Rücksprung auf den Aufschläger nach *jedem* erfassten Punkt ist korrekt — so steht es im Design-Dokument. Keine Änderung.
  - **Ruling:** Kacheln werden nie gestreckt, `min-height: 56px` ist eine Untergrenze. Eine Kachel von ~350px war nicht das Abgenommene; das Returnierenden-Panel ist einfach kürzer, Restfläche bleibt leer. Behoben in 2db8ffe, RED bewiesen (`expected 176.25 to be within 56..96`).
  - **Ruling:** Das Löschen der zwei Cypress-Tests zum Ass/DF-Snackbar ist richtig — die Buttons können auf dem Returnierenden-Panel nicht mehr existieren. Der Guard bleibt über zwei Vitest-Tests gedeckt; der Snackbar-*Text* ist damit nirgends mehr geprüft, bewusst akzeptiert.
- Task 5: Review 1 (commits 9d9a50d..2db8ffe) → Spec ✅, 2 Important, 8 Minor. Fix-Durchgang 1 gestartet.
  - Important 1: `score.component.spec.ts` „should follow the serve when it changes" arrangiert `activePlayer=1` und erwartet 1 — besteht auch ohne `effect()`. Einziger Vitest-Test, der für diesen Pfad benannt ist.
  - Important 2: `resetContextForPanel` leert nur das Panel, auf dem erfasst wurde. Mit einem Panel plus Auto-Umschalten heisst das: `2. Service` beim Aufschläger antippen, zum Returnierenden wechseln, dort erfassen → `serviceContextP1` bleibt `2`, die Ansicht springt zurück und der **nächste Punkt schickt still `serveAttempt: 2`**. Gleiches für `strokeType`.
  - **Ruling:** Important 2 wird in Task 5 behoben, nicht nach Task 6 verschoben. Task 6 beseitigt die Klasse strukturell, aber jeder Task muss für sich korrekt sein, und falsche Aufschlagversuche sind in der Oberfläche unsichtbar und vergiften später `% 1./2. Service`. Kostet bei Irrtum: zwei Zeilen, die Task 6 ohnehin ersetzt.
- Task 5: minor (deferred): `player.id` wird im Panel nie gelesen, nur `player().name` — `activePlayerInfo()` füllt ein Feld, das niemand konsumiert. Für Task 6/7 mitgegeben.
- Task 5: minor (deferred): Toggle-Logik liegt auf beiden Seiten der Grenze (`toggleStroke` im Panel, `toggleService` im Parent) — planbedingt durch die Signaturen, als Entscheidung festgehalten.
- Task 5: minor (deferred): `within(56, 96)` subsumiert die beiden `at.least(56)`-Tests; vier Tests, wo zwei reichen, und 96 ist eine Magic Number mit wenig Reserve für längere Übersetzungen (TEN-77).
- Task 5: minor (deferred): sechs überflüssige `cy.viewport(360, 692)`-Aufrufe in Tests, deren `describe` das schon im `beforeEach` setzt.
- Task 5: minor (deferred): `.panels-area { overflow: hidden }` schneidet still ab; mit geöffneten Coach-Notizen (`max-height: 40vh`) kann die Schlagart-Reihe unerreichbar werden. **Für Task 7 mitgeben** — dort entfällt der Notizen-Streifen ohnehin.
- Task 5: minor (deferred): `TranslatePipe` ist über `{{ 'playerNotes.title' | translate }}` weiter auf dem Screen; die Vorgabe „hart deutsch" ist auf Screen-Ebene noch nicht erfüllt. **Für Task 7 mitgeben.**
- Task 5: minor (deferred): ungetestet bleiben `serve-attempt-2`, die 44×44-Untergrenze für `.ctx-btn`/`serve-attempt-*`, die Fehlerzuordnung auf dem Returnierenden-Panel und die beiden Snackbar-Texte.
- Task 5: fix round 1/5 (2 addressed, 0 open — Vitest-Test arrangiert jetzt `activePlayer=2` und erwartet 1 (mutationsgeprüft); `resetContextForPanel` → `resetPointContext()` räumt beide Panels, RED war der echte Defekt `serveAttempt: expected 2 to be one of [null, undefined]`; commits 2db8ffe..6df737e)
- Task 5: complete (commits 9d9a50d..6df737e, review clean)
  - `ObservationPanelComponent` extrahiert (fünf Inputs, drei Outputs, keine Service-Injektion); `score.component.html` −78 Zeilen, `score.component.ts` −29. Cypress 117/117, Vitest 36/36.
  - Der Prüfer hat bestätigt: die Panel-Schnittstelle trägt Task 6 unverändert, `activeServeAttempt()` ist die Indirektion, die dort auf ein Punkt-Signal umgehängt wird.
- Task 5: minor (deferred, aus der Nachprüfung): die Reihenfolge „Reset nach `handlePointResponse`" ist für den Fehlerpfad ungetestet.

- Task 6: complete (commits 6df737e..2df924d, review clean — Spec ✅, keine Critical/Important)
  - Ein punktbezogenes `serveAttempt`-Signal ersetzt `serviceContextP1/P2`; `toggleService` ohne Panel-Argument; Panel-Schnittstelle unverändert. `score.component.ts` −5 Zeilen. Cypress 119/119, Vitest 37/37.
  - **Ruling vorab:** nur der Aufschlagversuch wird zusammengefasst, `strokeTypeP1/P2` bleiben zwei Signale — ein Diff, der beides macht, ist schlechter prüfbar, und das Stroke-Paar ist durch `resetPointContext()` bereits verhaltensrichtig. Zusammenfassen des Stroke-Paars als Minor zurückgestellt.
  - ⚠️ beide selbst geprüft: `makeMatch` hat `servingPlayer: 1` (`score.component.cy.ts:48`), der neue Test wechselt also tatsächlich auf das Returnierenden-Panel — nicht vakuum. Die Task-5-Wächter stehen unverändert in der Datei (`:289-290` aria-pressed, `:305` request body).
- Task 6: minor (deferred): der Langnamen-Test prüft CSS-Eigenschaften (`overflow`, `text-overflow`) statt Pixel — vom Prüfer als angemessen beurteilt, da Paint-Überlauf über `getBoundingClientRect` nicht sichtbar ist; `white-space: nowrap` bleibt ungeprüft.
- Task 6: minor (deferred): `activeServeAttempt` ist ein trivialer `computed`-Alias; `asReadonly()` oder direkte Bindung wäre schlanker, falls die Indirektion je überarbeitet wird.
- Task 6: minor (deferred): der Doc-Kommentar an `resetPointContext` beschreibt noch das alte Panel-Szenario für den Aufschlagversuch.
- Task 6: minor (deferred): `strokeTypeP1/P2` zu einem punktbezogenen Signal zusammenfassen (bewusst aus Task 6 herausgehalten).

- Task 7: Review 1 (commits 2df924d..58e1e18) → Spec ✅, keine Critical/Important, aber ein als Minor eingestufter Befund höher gewertet.
  - **Ruling:** Der Notizen-Einstieg lag innerhalb des `status !== 'COMPLETED'`-Blocks — auf einem beendeten Match gab es gar keinen Weg zu den Notizen mehr. Das ist kein Minor: `MatchPlayerNoteJpaRepository` beschränkt die Spieler-Notizen-Abfrage auf **COMPLETED**-Matches (TEN-68), das sind genau die Notizen, die die Vorbereitung des nächsten Matches speisen (und TEN-70 ausbaut). Muss erreichbar bleiben.
  - **Ruling (Fix 2):** Der erste Fix gab der Leiste `z-index: 21`, damit sie über dem Sieger-Overlay schwebt. Abgelehnt — ein ungedimmter Knopf über einem abgedunkelten Hintergrund liest sich als Renderfehler. Das Overlay behält den Screen, der Notizen-Knopf wandert in die Siegerkarte neben „Zurück zur Übersicht".
  - **Ruling:** Der Zweig „COMPLETED, aber nicht `isDone`" bleibt samt Test. `MatchService.endMatch()` und `endMatchWalkover()` setzen `COMPLETED` unabhängig von `isDone`; im Gleichschritt laufen die Felder nur auf dem Punkterfassungs-Pfad (`MatchService:262-265`).
  - ⚠️ des Prüfers selbst geprüft: `isDone` ohne `COMPLETED` entsteht nicht, das Backend hält beides synchron. Kein Mangel.
- Task 7: fix rounds 1+2 (2 addressed, 0 open; commits 58e1e18..67a67f1)
- Task 7: complete (commits 2df924d..67a67f1, review clean)
  - Untere Leiste mit letzter Beobachtung und Notiz-Knopf, Notizen als Vollbild innerhalb der Route (`PlayerNotesComponent` unverändert wiederverwendet), `<details>` entfernt, `TranslatePipe` aus `score.component.ts` raus. Cypress 60 im Score-Spec, Vitest 37.
  - **TEN-71 damit vollständig umgesetzt (Tasks 3–7).**
- Task 7: minor (deferred): der Notizen-Knopf in der Siegerkarte zeigt den `notes-dot` nicht — auf `isDone`-Matches fehlt der Hinweis, dass Notizen existieren.
- Task 7: minor (deferred): kein Test, dass der Punkt nach `closeNotes()` aktualisiert; kein Test, dass `hasNotes` reine Leerzeichen ignoriert; der Beendet-Test prüft den Siegernamen nicht; die 44px-Zielgrösse von `notes-back` (`hdr-icon`) ist ungeprüft.

- Task 8: complete (commits 67a67f1..93b4d12, review clean — Spec ✅, keine Critical/Important)
  - Ein `responsiveDialogConfig(isHandset)`-Helper speist alle drei `dialog.open`-Stellen; Handy `100vw`/`100dvh` mit festem Kopf (Abbrechen links, Speichern rechts), Desktop unverändert 500px. Cypress 131/131, Vitest 37/37.
  - **Ruling vorab:** Die ~40px-Tippflächen im `EndMatchDialogComponent` wurden behoben, obwohl der Dialog „nur auf Lesbarkeit zu prüfen" war — 40px verletzt die verbindliche 44px-Untergrenze, und das ist der Dialog, den der Coach am Matchende in Eile antippt. Mit Test abgesichert (`expected 40 to be at least 44`).
  - `min-width: 400px` aus beiden Formularen entfernt (erzwang Überlauf bei 360px); der Prüfer hat bestätigt, dass die Desktop-Darstellung dadurch unverändert bleibt.
  - ⚠️ offen, nicht testbar: ob `100dvh` die Felder sichtbar hält, wenn die Bildschirmtastatur aufgeht — auf iOS Safari schrumpft `dvh` dafür nicht. Gehört in die Abnahme am Gerät.
- Task 8: minor (deferred): der Handy-Kopf verlor `mat-dialog-title`, der Dialog hat dadurch keinen zugänglichen Namen mehr (`responsive-dialog.ts:56`). Eine Direktive, aber eine A11y-Regression dieser Änderung — **für die Schlussprüfung vormerken**.
- Task 8: minor (deferred): gemischte Sprachen am Handy — der Kopf übersetzt `common.save`/`common.cancel`, Formulare und Desktop-Aktionen sind hart deutsch.
- Task 8: minor (deferred): kein eigener Test für die Handy-Variante von `ScoreEditDialogComponent` (Template-Zweig, `cancelled`/`saved`-Verdrahtung, 44px der Checkbox).
- Task 8: minor (deferred): `var(--text-muted)` als Rahmenfarbe im Dialogkopf; Komponente und Config-Funktion teilen sich `responsive-dialog.ts`.

- Task 9: complete (commits 93b4d12..3528391, review clean — Spec ✅, keine Critical/Important)
  - Reine Anordnung über `@media (max-width: 700px)`, kein `LayoutService` — richtiges Werkzeug für einen Layout- statt Strukturwechsel. Basisregeln unangetastet, beide `::ng-deep` mit `:host` und innerhalb der Query. Cypress 136/136, Vitest 37/37.
  - **Ruling:** Der Material-Kartenrahmen entfällt unterhalb 700px, damit der Startknopf volle Breite bekommt. Bestätigt — das abgenommene Mockup zeigt diesen Screen am Handy randlos ohne Karte, und eine eingerückte Karte in 360px ist verschenkte Breite. Am Desktop bleibt der Rahmen.
  - ⚠️ beide selbst geprüft: 700px ist die in der Spec festgelegte gemeinsame Grenze; `.page-container`/`.page-header` haben Basisregeln, die der Media-Block per Quellreihenfolge überschreibt.
- Task 9: minor (deferred): die Handy-Tests prüfen weder den 16px-Seitenrand noch die Überschriftgrösse, und nicht, dass die Umschalter am rechten Rand bleiben; der Desktop-Test prüft nicht, dass die Karte dort Rahmen und Hintergrund behält.
- Task 9: minor (deferred): `:host ::ng-deep mat-card-content` ist vermutlich überflüssig; `.mdc-radio` direkt zu überschreiben ist über Material-Upgrades fragil — ein State-Layer-Token wäre robuster.

- Task 10: Review 1 (commits 3528391..3b94c88) → Spec ✅, 1 Important, 3 Minor. Fix-Durchgang 1.
  - Important: Nach Antippen von „neu anlegen" schrieb der Autocomplete `displayPlayer('typed')` → `''` ins Eingabefeld. Bei Abbruch wirkte das Feld leer, während Control und Query den Text noch hielten — am Platz heisst das Namen neu tippen. **Reproduziert** (`expected "" to equal "Zzz Test"`) und behoben: `displayPlayer` gibt Strings unverändert zurück.
  - **Ruling vorab bestätigt:** Die Abweichung vom Brief — `{ prefill: {firstName, lastName} }` statt `prefillName` — bleibt. Sonst müsste der Dialog `splitName` aus `match-setup` importieren, das den Dialog bereits importiert (Zirkelimport); die Namensaufteilung gehört ohnehin zum Aufrufer. Der Prüfer hat bestätigt, dass der Edit-Modus unverändert funktioniert.
- Task 10: complete (commits 3528391..fa21a60, review clean)
  - „« Name » neu anlegen" erscheint nur bei leerer Trefferliste und nicht leerer Eingabe, öffnet den Dialog vorbefüllt und setzt den Spieler in genau das Feld, aus dem heraus angelegt wurde. Volle Cypress-Suite vom Controller nachgefahren: **144/144 grün**, Vitest 37/37.
  - **TEN-74 damit vollständig umgesetzt (Tasks 8–10).**
- Task 10: minor (deferred): `@empty` ist auch wahr, solange `getPlayers` läuft oder fehlschlug — in dem Fenster wird „neu anlegen" angeboten, obwohl ein Treffer existiert.
- Task 10: minor (deferred): ein durch `availablePlayers` herausgefilterter Spieler (mit laufendem Match) wird ebenfalls zum Anlegen angeboten — mögliche Dublette; ein Dublettencheck wäre später sinnvoll.
- Task 10: minor (deferred): kein Test für fehlgeschlagenes Speichern (Snackbar, Feld bleibt) und keiner, dass ein angelegter Spieler danach erneut auswählbar ist.

- Task 11: Review 1 (commits fa21a60..dce0eb0) → Spec ✅, 1 Important, 4 Minor. Fix-Durchgang 1.
  - **Wichtiger Fund des Umsetzers, bestätigt:** Cypress' Standard-Viewport (500×500) zählt als Handset — ohne Korrektur hätten alle bestehenden Tabellen-Tests still gegen die Kartenansicht gelaufen. `mountPlayers` setzt jetzt immer einen Viewport, Standard 1280×900.
  - Important: Die Karte war `role="button"` mit `(keydown.enter)`, Ball- und Menü-Button lagen darin. `keydown` blubbert, `stopPropagation` hing nur am `click` — Enter auf Menü oder Ball öffnete zusätzlich den Bearbeiten-Dialog. Reproduziert, dann **strukturell gelöst**: die Karte ist ein `div`, die Bearbeiten-Aktion ein eigener Button, Ball und Menü sind Geschwister. Alle `stopPropagation` entfallen.
- Task 11: complete (commits fa21a60..de6c0e9, review clean)
  - Kartenliste unterhalb `Breakpoints.Handset`, Tabelle am Desktop unverändert inkl. Sortierung und Suche. Cypress 162/162, Vitest 37/37.
  - **TEN-75 damit umgesetzt.**
- Task 11: minor (deferred): `openMatch` emittiert einen `Player`, der Parent braucht deshalb `activeMatchId!`; den `string` zu emittieren wäre sauberer.
- Task 11: minor (deferred): schwache Assertions in `players.component.cy.ts:308-309` (`cy.contains('1')`, roher i18n-Key) und :313-326 (nur Klassenprüfung); Löschen/Inaktivieren werden nie geklickt, der Bestätigungsfluss ab Karte ist ungetestet.
- Task 11: minor (deferred): Inaktivität wird nur über Deckkraft vermittelt — Screenreader hören nichts. Gleiche Einschränkung wie in der Tabelle, also keine Regression.
- Task 11: minor (deferred): in der Kartenansicht gibt es keine Sortiersteuerung; Karten folgen der Standardsortierung. **Im PR erwähnen.**
- Task 11: minor (deferred): in `match-setup.component.cy.ts` laufen die ersten beiden describes auf Cypress' Standard-500px, also unter der 700px-Grenze — sie prüfen still die Handy-Darstellung und den Handy-Dialog. Beide Breiten bestehen.
- Task 11: minor (deferred, aus der Nachprüfung): `.player-main` hat kein eigenes `:focus-visible`/Hover-Styling, nur den Browser-Standardumriss.

- Task 12: complete (commits de6c0e9..8c7db56, review clean — reine Inspektion, keine Extras)
  - 14 Befunde erfasst, 3 behoben (alle `font-size: 10px` → `11px` in `statistics.component.ts` und `head-to-head.component.ts`), 11 brauchen Layoutänderungen und bleiben liegen. Cypress 162/162, Vitest 37/37.
  - ⚠️ selbst geprüft: Arbeitsbaum sauber, keine Mess-Überreste (`zz-probe*`), keine vom Umsetzer angelegten Tickets.
  - ⚠️ offen: bei **430px wurde nicht gemessen**, obwohl die Spec drei Breiten nennt. Risiko gering (mehr Platz als 360/390), aber als ungeprüft zu führen, nicht als bestanden.
  - Offene Befunde für Tickets: Statistik-Satz-Tabs 24px statt 44; „VH/RH %"-Spalte 48px zu schmal; Head-to-Head-Überlauf bei langen Namen (378px bei 360, 408px bei 390, +18px bei 800); „140 (33%)" bricht in der 64px-Spalte um; Head-to-Head-Auswahlfeld auf ~116px abgeschnitten; `rec-note-input` in der Analyse 24px statt 44; `PlayerNotesComponent` Speichern-Buttons 22px und `.slot-role` 10px (zwei getrennte Punkte).

## Schlussprüfung (b2e695d..8c7db56, Opus)

Drei Befunde, die durch zwölf Einzelprüfungen gerutscht sind, plus Triage der zurückgestellten Minors. Eine Fix-Welle (commits 8c7db56..e240ea7), danach eine Nachprüfung.

- C1 (Critical): `recordQuickPoint` räumte den Punkt-Kontext nicht — der **nächste** Punkt trug stillschweigend den alten `serveAttempt`. Behoben.
- I1: Scoring-Screen schnitt im Querformat Kacheln unerreichbar ab. Behoben, aber siehe unten.
- I2: „neu anlegen" wurde auch für existierende Spieler mit laufendem Match angeboten → Spieler-Dubletten, die Head-to-Head und Statistik auf zwei IDs aufteilen. Behoben über ein `playersLoaded`-Flag.
- I3: `EndMatchDialogComponent` war nie responsiv (fest 360px, Material deckelt auf 80vw → 288px am Handy). Behoben.
- M1 + `aria-label="Sprache"`: behoben.
- **Ruling:** Die Verbreiterung des Beenden-Dialogs auf dem Desktop (360 → 500px) ist zulässig. Die Vorgabe „am Desktop unverändert" nimmt den Scoring-Screen ausdrücklich aus, und der Dialog ist nur von dort erreichbar; 500px ist zudem die Breite der vier anderen Dialoge. Kostet bei Irrtum: eine Zeile.
- **Ruling:** Der Prüfer hat seinen eigenen vorgeschlagenen Querformat-Test verworfen, weil `.click()` gegen den kaputten Code bestanden hätte (Cypress ruft `scrollIntoView`, das einen `overflow: hidden`-Container scrollt, den kein Nutzer scrollen kann). Der Ersatz `assertNotClipped` misst die Vorfahrenkette ohne zu scrollen. Angenommen. Kostet bei Irrtum: ein Test, der nur die Unterkante prüft.
- **Ruling:** `playersLoaded` statt „volle Liste nicht leer" ist besser als mein Vorschlag — die wörtliche Fassung hätte „neu anlegen" für einen Coach mit leerer Spielerliste dauerhaft stillgelegt, also genau im wichtigsten Fall. Angenommen. Kostet bei Irrtum: nichts.

### Zwei offene Punkte, geparkt statt gefixt (keine zweite Fix-Welle)

- **Parked — Ruling:** `@media (max-height: 560px)` ist die falsche Schwelle für die Querformat-Rettung. Die Schwelle liegt unter der Höhe, ab der das Layout zu schneiden beginnt (~590–690px), und sie misst den CSS-Viewport, während das Layout in `100dvh` rechnet — ein iPhone SE (375×667) rendert ~553dvh, die Media Query sieht aber 667 und schaltet nicht. **Der ursprüngliche Fehler lebt damit auf einem Band echter Portrait-Geräte weiter.** Empfehlung des Prüfers: die Bedingung streichen und den Screen unbedingt auf `height: auto; min-height: 100dvh; overflow: visible` stellen, die Portrait-Assertion auf Dokumentebene ziehen. Geparkt, weil das Verfahren keine zweite Fix-Welle vorsieht — gehört dem Auftraggeber vorgelegt. Kostet bei Irrtum: der Scoring-Screen bleibt auf kleineren Handys im Hochformat teilweise unbedienbar.
- **Parked — Ruling:** `mat-dialog-title` zieht Materials eigenes Boxmodell in den Handy-Dialogkopf (`padding: 6px 24px 13px` plus ein 40px-`::before`), wodurch der Titel von ~22px auf ~59px wächst und bei 360px umbricht — eine Regression an genau dem Kopf, den dieser Branch verschlanken sollte. Einzeiler (`padding: 0` plus `::before { display: none }`). Geparkt aus demselben Grund. Kostet bei Irrtum: ein aufgeblähter Dialogkopf am Handy.

### Geparkte Punkte nachträglich behoben (2026-11-26, nach PR #58)

Auf Wunsch des Auftraggebers doch noch gefixt, commits e240ea7..b7d1e11, nachgeprüft und sauber. Auf den offenen PR geschoben.

- F1: Die `@media (max-height: 560px)`-Bedingung ist gestrichen; die Seite steht unbedingt auf `min-height: 100dvh; overflow: visible`, `.panels-area` auf `flex: 1; min-height: 0; overflow: auto`. `.notes-view` von `height: 100%` auf `flex: 1` — sonst wäre der bisher nur im Querformat sichtbare Nebeneffekt ins Hochformat gewandert.
- F2: `padding: 0`, `line-height: 1.2` und `::before { display: none }` auf `.dialog-header-title`; Kopfzeile von 106,6px auf 59,2px.
- **Drei meiner Zahlen waren falsch, der Umsetzer hat nachgemessen statt sie zu glauben:** das Panel misst ~380px statt ~580 (die 580 waren die Seitenhöhe), die ungerettete Lücke war 561–576 statt 561–692, und **mein vorgeschlagener Testpunkt 375×600 hätte gegen den kaputten Code bestanden** — verschoben auf 375×568, dort RED. Zum zweiten Mal auf diesem Befund hätte ein vorgeschlagener Test nichts bewiesen.
- Zusätzlich gefunden: `.mat-mdc-dialog-title` setzt auch `line-height: 32px`; ohne das wäre die Kopfzeile bei 73,5px statt 59,2px geblieben.
- Offen als Minor: ein irreführender Kommentar zum Scrollverhalten der Erfassungsfläche, und ein fixture-unabhängiger Pin auf `padding`/`::before` des Dialogtitels wäre sturer als die Höhenassertionen.

### Folgetickets aus der Triage (noch anzulegen)

A11y · Scoring-Politur · Test-Schulden · Aufräumen — Gruppierung im Prüfbericht. Dazu: `recordQuickPoint` verwirft den bewusst getippten Aufschlagversuch inzwischen, statt ihn falsch mitzuschicken (Korruption weg, Datum trotzdem verloren); und der 96px-Deckel im Kachel-Test bricht, sobald TEN-77 die Beschriftungen übersetzt — das gehört in TEN-77 vermerkt.
