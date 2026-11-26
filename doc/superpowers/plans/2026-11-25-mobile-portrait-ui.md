# Hochformat- und Einhandbedienung Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Das TSaS-Frontend so umbauen, dass der Coach ein Match am Handy im Hochformat mit einem Daumen erfassen kann und Spieler- und Matchanlage am Gerät ohne Umwege funktionieren.

**Architecture:** Zwei Darstellungen nebeneinander. Reine Anordnung läuft über CSS-Media-Queries, Strukturwechsel (Toolbar gegen Navigationsleiste, Tabelle gegen Kartenliste) über `BreakpointObserver` mit `Breakpoints.Handset`. Einzige Ausnahme ist der Scoring-Screen: er wird auf jeder Bildschirmbreite durch Variante C ersetzt und trägt seine eigene Kopfzeile ohne App-Navigation.

**Tech Stack:** Angular 21 standalone, Angular Material, `@angular/cdk/layout`, `@ngx-translate`, Vitest, Cypress Component Testing.

**Spec:** `doc/superpowers/specs/2026-11-25-mobile-portrait-ui-design.md`

## Global Constraints

Gelten für jede Aufgabe, auch wo sie nicht wiederholt werden.

- Zielgeräte: **360 × 800**, **390 × 844**, **430 × 932**, jeweils minus **108 px** sichtbare Browserleisten.
- Beobachtungs-Kacheln im Scoring-Screen: **`min-height: 56px`**, niemals feste Höhe. Jedes andere Tippziel: **mindestens 44 × 44 px**.
- Kacheln sind **umbruchfähig** zu bauen — die Übersetzung in TEN-77 bringt längere Beschriftungen.
- Strukturwechsel laufen über `Breakpoints.Handset` aus `@angular/cdk/layout`. Eine reine Breitengrenze genügt nicht: ein Handy quer misst rund 800 × 360 px. Für reine Anordnung gilt die gemeinsame Grenze **700 px**.
- Die untere Navigationsleiste berücksichtigt **`env(safe-area-inset-bottom)`**.
- Auf der Route `matches/:id/score` ist **auf jeder Bildschirmbreite** weder Toolbar noch Navigationsleiste sichtbar.
- Ausser dem Scoring-Screen bleibt jede Darstellung **am Desktop unverändert**.
- **Keine Backend-Änderung**, keine neuen Punkt-Attribute, keine Migration.
- Neue Beschriftungen gehören in alle vier Dateien `public/i18n/{de,en,it,fr}.json`. **Ausnahme:** der Scoring-Screen bleibt vorerst hart deutsch — seine Übersetzung ist TEN-77.
- Querformat: nichts abgeschnitten, nichts waagerecht über. Senkrechtes Scrollen ist dort zulässig.
- Ein Pull Request je Ticket gegen `develop`. Linear-Status mitführen: In Progress beim Start, In Review beim Öffnen des PR, Done beim Merge.
- Testbefehle: `cd frontend && npx ng test --no-watch` (Vitest) und `cd frontend && npx cypress run --component --spec <pfad>` (Cypress).

## Review Focus

Fälle, die die Spezifikation voraussetzt, ohne sie zu benennen. Jede Zeile hat einen Test in der Aufgabe, der den Code besitzt.

1. **Beendetes Match** (`score.isDone`): Panel und Umschalter dürfen nicht erscheinen, der Sieger-Überlagerung gehört der Bildschirm. → Task 7.
2. **Aufschlagwechsel nach einem Game**: Die Ansicht muss dem neuen Aufschläger folgen, nicht beim alten stehenbleiben. → Task 5.
3. **Zweistellige Punktzahlen im Tie-Break** (10 : 12 statt 40 : 30): Die verdichtete Kopfzeile darf nicht umbrechen oder überlaufen. → Task 4.
4. **Sehr lange Spielernamen**: Name in Kopfzeile, Panel-Kopf und Umschalter wird abgeschnitten, nicht umgebrochen, und schiebt nichts aus dem Bild. → Task 6.
5. **Querformat auf dem Handy** (800 × 360): `Breakpoints.Handset` greift, die Handy-Darstellung erscheint, nichts läuft waagerecht über. → Task 2.

---

## File Structure

**Neu:**
- `frontend/src/app/core/layout/layout.service.ts` — kapselt `BreakpointObserver`; ein Signal `isHandset`. Einzige Stelle, an der die Grenze definiert ist.
- `frontend/src/app/core/layout/layout.service.spec.ts`
- `frontend/src/app/features/matches/score/serve-picker.component.ts` / `.html` — Auswahl „Wer schlägt auf?".
- `frontend/src/app/features/matches/score/observation-panel.component.ts` / `.html` — ein Beobachtungs-Panel, rollenabhängig.
- `frontend/src/app/features/players/player-card-list.component.ts` / `.html` — Kartenliste für das Handy.

**Geändert:**
- `frontend/src/app/app.ts` / `app.html` — Navigationsleiste, Ausblenden der Navigation auf der Scoring-Route, `main`-Höhe.
- `frontend/src/app/features/matches/score/score.component.ts` / `.html` — Variante C.
- `frontend/src/app/features/matches/match-setup/match-setup.component.ts` / `.html` — gestapeltes Formular, Spieleranlage aus der Suche.
- `frontend/src/app/features/players/players.component.ts` / `.html` — Kartenliste unterhalb der Grenze.
- `frontend/src/app/features/players/player-dialog.component.ts` — Vorbefüllung, keine feste Breite.
- `frontend/src/app/features/matches/score/score-edit-dialog.component.ts` — keine feste Breite.
- `public/i18n/{de,en,it,fr}.json`

Das Scoring-Panel kommt deshalb in eine eigene Komponente, weil `score.component.ts` mit 462 Zeilen und rund 200 Zeilen Styles bereits unhandlich ist und Variante C den Panel-Inhalt rollenabhängig macht.

---

## TEN-73 — App-Shell und Navigationsleiste

### Task 1: LayoutService

**Files:**
- Create: `frontend/src/app/core/layout/layout.service.ts`
- Test: `frontend/src/app/core/layout/layout.service.spec.ts`

**Interfaces:**
- Produces: `LayoutService` mit `readonly isHandset: Signal<boolean>`, gespeist aus `BreakpointObserver.observe(Breakpoints.Handset)`.

- [ ] **Step 1: Write the failing test**

```ts
describe('LayoutService', () => {
  it('meldet isHandset true, wenn Breakpoints.Handset greift', () => { /* BreakpointObserver mit { matches: true } stubben */ });
  it('meldet isHandset false, wenn Breakpoints.Handset nicht greift', () => { /* ... { matches: false } */ });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npx ng test --no-watch`
Expected: FAIL — `layout.service` nicht gefunden.

- [ ] **Step 3: Implement `LayoutService` in `frontend/src/app/core/layout/layout.service.ts`**

`@Injectable({ providedIn: 'root' })`. `isHandset` entsteht via `toSignal` aus `BreakpointObserver.observe(Breakpoints.Handset)`, Startwert `false`.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npx ng test --no-watch`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/core/layout/
git commit -m "feat(layout): LayoutService mit isHandset-Signal"
```

---

### Task 2: Navigationsleiste und Ausblenden auf der Scoring-Route

**Files:**
- Modify: `frontend/src/app/app.ts`, `frontend/src/app/app.html`
- Modify: `frontend/src/app/app.spec.ts`
- Create: `frontend/src/app/app.cy.ts`
- Modify: `public/i18n/{de,en,it,fr}.json`

**Interfaces:**
- Consumes: `LayoutService.isHandset` aus Task 1.
- Produces: `App.showAppNav: Signal<boolean>` — false auf Routen, die mit `/matches/` beginnen und auf `/score` enden.

- [ ] **Step 1: Write the failing tests**

In `app.cy.ts`:

```ts
describe('App — Navigation', () => {
  it('zeigt bei 390x736 die untere Navigationsleiste statt der Toolbar', () => {
    cy.viewport(390, 736);
    cy.get('[data-testid="bottom-nav"]').should('be.visible');
    cy.get('mat-toolbar').should('not.exist');
  });

  it('zeigt bei 1280x900 die Toolbar und keine Navigationsleiste', () => {
    cy.viewport(1280, 900);
    cy.get('mat-toolbar').should('be.visible');
    cy.get('[data-testid="bottom-nav"]').should('not.exist');
  });

  it('zeigt im Querformat 800x360 die Handy-Darstellung', () => {   // Review Focus 5
    cy.viewport(800, 360);
    cy.get('[data-testid="bottom-nav"]').should('be.visible');
    cy.document().its('body').then(b => {
      expect(b.scrollWidth).to.be.at.most(b.clientWidth);
    });
  });

  it('blendet auf der Scoring-Route jede App-Navigation aus — am Handy', () => {
    cy.viewport(390, 736);
    // Router auf matches/match-1/score setzen
    cy.get('[data-testid="bottom-nav"]').should('not.exist');
    cy.get('mat-toolbar').should('not.exist');
  });

  it('blendet auf der Scoring-Route jede App-Navigation aus — am Desktop', () => {
    cy.viewport(1280, 900);
    // Router auf matches/match-1/score setzen
    cy.get('mat-toolbar').should('not.exist');
    cy.get('[data-testid="bottom-nav"]').should('not.exist');
  });

  it('bietet hinter Mehr Sprache, Export, Löschen und Abmelden an', () => {
    cy.viewport(390, 736);
    cy.get('[data-testid="nav-more"]').click();
    cy.get('[data-cy="lang-menu-trigger"]').should('exist');
    cy.get('[data-cy="export-my-data"]').should('exist');
    cy.get('[data-cy="delete-my-data"]').should('exist');
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && npx cypress run --component --spec src/app/app.cy.ts`
Expected: FAIL — `[data-testid="bottom-nav"]` existiert nicht.

- [ ] **Step 3: Implement the navigation switch in `app.ts` / `app.html`**

`showAppNav` als `computed` über die aktuelle Router-URL. Template rendert drei Zweige: Toolbar wenn nicht Handset und `showAppNav`, Navigationsleiste wenn Handset und `showAppNav`, sonst nichts. Die Leiste trägt `data-testid="bottom-nav"` und drei Einträge mit `data-testid="nav-players" | "nav-new-match" | "nav-more"`; hinter „Mehr" liegt ein `mat-menu` mit den heutigen Toolbar-Menüeinträgen, der Sprachwahl und dem Admin-Abzeichen. Beschriftungen über `TranslatePipe`, Schlüssel `app.nav.more` in alle vier i18n-Dateien.

- [ ] **Step 4: Fix the `main` height rule**

`main { min-height: calc(100vh - 64px) }` in `app.ts` entfernen; die Höhe ergibt sich aus dem Flex-Layout von `:host`.

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd frontend && npx cypress run --component --spec src/app/app.cy.ts && npx ng test --no-watch`
Expected: PASS, auch die bestehenden `app.spec.ts`-Tests.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/app.ts frontend/src/app/app.html frontend/src/app/app.cy.ts frontend/public/i18n/
git commit -m "feat(shell): untere Navigationsleiste am Handy, keine Navigation auf der Scoring-Route"
```

---

## TEN-71 — Scoring-Screen Variante C

### Task 3: Aufschlagwahl, wenn kein Aufschläger gesetzt ist

**Files:**
- Create: `frontend/src/app/features/matches/score/serve-picker.component.ts` / `.html`
- Modify: `frontend/src/app/features/matches/score/score.component.ts` / `.html`
- Test: `frontend/src/app/features/matches/score/score.component.cy.ts`

**Interfaces:**
- Produces: `ServePickerComponent` mit `input.required<{ player1: string; player2: string }>()` als `players` und `output<1 | 2>()` als `picked`.

- [ ] **Step 1: Write the failing tests**

```ts
describe('ScoreComponent — Aufschlagwahl', () => {
  it('zeigt die Aufschlagwahl, wenn servingPlayer null ist', () => {
    mountScore(makeMatch({ score: { ...makeMatch().score, servingPlayer: null } }));
    cy.get('[data-testid="serve-picker"]').should('be.visible');
    cy.get('[data-testid="panel"]').should('not.exist');
  });

  it('setzt den Aufschläger und zeigt danach das Panel', () => {
    cy.intercept('PUT', '**/api/matches/match-1/serving-player-1', {}).as('setServe');
    cy.get('[data-testid="serve-pick-1"]').click();
    cy.wait('@setServe');
    cy.get('[data-testid="panel"]').should('be.visible');
  });

  it('zeigt die Aufschlagwahl nicht, wenn servingPlayer gesetzt ist', () => {
    mountScore();
    cy.get('[data-testid="serve-picker"]').should('not.exist');
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts`
Expected: FAIL — `[data-testid="serve-picker"]` existiert nicht.

- [ ] **Step 3: Implement `ServePickerComponent`**

Zwei Flächen à `min-height: 88px` mit `data-testid="serve-pick-1"` und `"serve-pick-2"`, darüber Überschrift und ein erklärender Satz. Gibt den gewählten Spieler über `picked` aus.

- [ ] **Step 4: Wire it into `score.component`**

Das Template zeigt `ServePickerComponent` statt des Panels, solange `matchData().score.servingPlayer == null`. Auf `picked` ruft es den bestehenden `setServe`-Pfad. Der heutige implizite Sonderfall — erster Tipp auf eine Kachel setzt den Aufschläger — wird entfernt.

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd frontend && npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/features/matches/score/
git commit -m "feat(score): explizite Aufschlagwahl statt implizitem erstem Tipp"
```

---

### Task 4: Verdichtete Kopfzeile mit antippbaren Punktzahlen

**Files:**
- Modify: `frontend/src/app/features/matches/score/score.component.ts` / `.html`
- Test: `frontend/src/app/features/matches/score/score.component.cy.ts`

**Interfaces:**
- Consumes: bestehende `formatPoints(score, isPlayer1)` und `recordQuickPoint(player)`.

- [ ] **Step 1: Write the failing tests**

```ts
describe('ScoreComponent — Kopfzeile', () => {
  it('ist höchstens 100px hoch', () => {
    cy.viewport(390, 736);
    cy.get('[data-testid="score-header"]').invoke('outerHeight').should('be.lte', 100);
  });

  it('erfasst beim Tipp auf die Punktzahl einen Punkt ohne Attribut', () => {
    cy.intercept('POST', '**/api/matches/match-1/points', {}).as('point');
    cy.get('[data-testid="quick-score-p1"]').click();
    cy.wait('@point').its('request.body.pointType').should('be.oneOf', [null, undefined]);
  });

  it('bricht bei zweistelligen Tie-Break-Punkten nicht um', () => {   // Review Focus 3
    cy.viewport(360, 692);
    mountScore(makeMatch({ score: { ...makeMatch().score, pointsPlayer1: 10, pointsPlayer2: 12 } }));
    cy.get('[data-testid="score-header"]').invoke('outerHeight').should('be.lte', 100);
    cy.get('[data-testid="quick-score-p1"]').should('be.visible');
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts`
Expected: FAIL — Kopfzeile zu hoch, `[data-testid="score-header"]` fehlt.

- [ ] **Step 3: Implement the compact header**

Eine Zeile mit Zurück-Icon, beiden Spielerhälften (Name 15 px, Punktzahl 30 px als `<button>` mit den bestehenden `data-testid`s `quick-score-p1` / `quick-score-p2`), dazwischen Games mit Label, rechts Bearbeiten- und Match-beenden-Icon. Darunter eine Zeile Satzverlauf mit 10 px. Der aufschlagende Spieler wird mit 🎾 und `--brand-strong` ausgezeichnet. Punktzahlen mit `font-variant-numeric: tabular-nums`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd frontend && npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/features/matches/score/
git commit -m "feat(score): verdichtete Kopfzeile, Punktzahlen bleiben antippbar"
```

---

### Task 5: Ein Panel, rollenabhängig, mit Spielerumschalter

**Files:**
- Create: `frontend/src/app/features/matches/score/observation-panel.component.ts` / `.html`
- Modify: `frontend/src/app/features/matches/score/score.component.ts` / `.html`
- Test: `frontend/src/app/features/matches/score/score.component.cy.ts`

**Interfaces:**
- Produces: `ObservationPanelComponent` mit Inputs `player: { id: string; name: string }`, `opponentName: string`, `isServing: boolean`, `serveAttempt: 1 | 2 | null`, `strokeType: StrokeType | null`; Outputs `observation: output<PointType>()`, `serveAttemptChange: output<1 | 2>()`, `strokeTypeChange: output<StrokeType | null>()`.
- Consumes: `PointType`, `StrokeType` aus `core/models/point.model`.

- [ ] **Step 1: Write the failing tests**

```ts
describe('ScoreComponent — ein Panel', () => {
  it('zeigt genau ein Panel', () => {
    cy.get('[data-testid="panel"]').should('have.length', 1);
  });

  it('zeigt beim Aufschläger Ass, Doppelfehler und die Service-Wahl', () => {
    cy.get('[data-testid="obs-ACE"]').should('be.visible');
    cy.get('[data-testid="obs-DOUBLE_FAULT"]').should('be.visible');
    cy.get('[data-testid="serve-attempt-1"]').should('be.visible');
  });

  it('zeigt beim Returnierenden weder Ass noch Doppelfehler noch Service-Wahl', () => {
    cy.get('[data-testid="switch-player-2"]').click();
    cy.get('[data-testid="obs-ACE"]').should('not.exist');
    cy.get('[data-testid="obs-DOUBLE_FAULT"]').should('not.exist');
    cy.get('[data-testid="serve-attempt-1"]').should('not.exist');
  });

  it('gibt jeder Beobachtungs-Kachel mindestens 56px Höhe', () => {
    cy.viewport(360, 692);
    cy.get('[data-testid^="obs-"]').each($el => {
      expect($el[0].getBoundingClientRect().height).to.be.at.least(56);
    });
  });

  it('scrollt auf 360x692 nicht', () => {
    cy.viewport(360, 692);
    cy.get('[data-testid="panels-area"]').then($el => {
      expect($el[0].scrollHeight).to.be.at.most($el[0].clientHeight + 1);
    });
  });

  it('zeigt auch bei 1280x900 genau ein Panel', () => {
    cy.viewport(1280, 900);
    cy.get('[data-testid="panel"]').should('have.length', 1);
    cy.get('[data-testid="switch-player-1"]').should('be.visible');
  });

  it('folgt dem Aufschlagwechsel nach einem Game', () => {   // Review Focus 2
    // Punkt erfassen, der das Game beendet; Antwort mit servingPlayer: 2
    cy.get('[data-testid="panel"]').should('contain', 'Rafael Nadal');
    cy.get('[data-testid="switch-player-2"]').should('have.attr', 'aria-pressed', 'true');
  });
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts`
Expected: FAIL — es gibt zwei Panels, `[data-testid="switch-player-2"]` fehlt.

- [ ] **Step 3: Implement `ObservationPanelComponent`**

Rollenabhängiger Inhalt. Aufschläger: Service-Kontext, Ass, Winner, Doppelfehler, Forced, Unforced, Netz, Schlagart. Returnierender: Winner, Forced, Unforced, Netz, Schlagart. Jede Beobachtungs-Kachel trägt `data-testid="obs-<PointType>"` und `min-height: 56px`; Beschriftungen umbruchfähig, keine feste Höhe.

- [ ] **Step 4: Replace the two-panel area in `score.component`**

`activePlayer = signal<1 | 2>(...)`, vorbelegt mit `servingPlayer`. Ein `effect` setzt `activePlayer` nach jeder Score-Antwort auf den dann gültigen `servingPlayer`. Unter dem Panel zwei Umschaltflächen à `min-height: 56px` mit `data-testid="switch-player-1" | "switch-player-2"` und `aria-pressed`. Das Panel bekommt `data-testid="panel"`, die Fläche darüber `data-testid="panels-area"`.

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd frontend && npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/features/matches/score/
git commit -m "feat(score): ein rollenabhängiges Panel mit Spielerumschalter"
```

---

### Task 6: Aufschlagversuch gehört zum Punkt

**Files:**
- Modify: `frontend/src/app/features/matches/score/score.component.ts`
- Test: `frontend/src/app/features/matches/score/score.component.cy.ts`, `frontend/src/app/features/matches/score/score.component.spec.ts`

**Interfaces:**
- Ersetzt `serviceContextP1` und `serviceContextP2` durch ein einzelnes `serveAttempt = signal<1 | 2 | null>(null)`.

- [ ] **Step 1: Write the failing tests**

```ts
it('schickt den auf dem Aufschläger-Panel gewählten Aufschlagversuch mit, auch wenn vom Returnierenden-Panel erfasst wird', () => {
  cy.get('[data-testid="serve-attempt-2"]').click();
  cy.get('[data-testid="switch-player-2"]').click();
  cy.intercept('POST', '**/api/matches/match-1/points', {}).as('point');
  cy.get('[data-testid="obs-UNFORCED_ERROR"]').click();
  cy.wait('@point').its('request.body.serveAttempt').should('eq', 2);
});

it('setzt den Aufschlagversuch nach dem Erfassen zurück', () => {
  cy.get('[data-testid="serve-attempt-2"]').should('not.have.attr', 'aria-pressed', 'true');
});

it('setzt die Schlagart nach dem Erfassen zurück', () => { /* wie heute */ });
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts`
Expected: FAIL — `serveAttempt` kommt als `null` an.

- [ ] **Step 3: Replace the per-panel service state**

Ein `serveAttempt`-Signal für den laufenden Punkt. `recordObservation` liest es unabhängig davon, welches Panel aktiv ist, und setzt es nach erfolgreicher Antwort zurück. Die bestehenden Vitest-Erwartungen in `score.component.spec.ts` auf das neue Signal umziehen.

- [ ] **Step 4: Add the long-name test**

```ts
it('schneidet sehr lange Spielernamen ab, statt die Zeile zu sprengen', () => {   // Review Focus 4
  cy.viewport(360, 692);
  // Spieler mit 40-Zeichen-Nachnamen mounten
  cy.get('[data-testid="score-header"]').invoke('outerHeight').should('be.lte', 100);
  cy.document().its('body').then(b => expect(b.scrollWidth).to.be.at.most(b.clientWidth));
});
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd frontend && npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts && npx ng test --no-watch`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/features/matches/score/
git commit -m "fix(score): Aufschlagversuch gehört zum Punkt, nicht zum Panel"
```

---

### Task 7: Untere Leiste, Notizen-Vollbild, beendetes Match

**Files:**
- Modify: `frontend/src/app/features/matches/score/score.component.ts` / `.html`
- Test: `frontend/src/app/features/matches/score/score.component.cy.ts`

**Interfaces:**
- Consumes: `PlayerNotesComponent` unverändert, `[matchId]`.

- [ ] **Step 1: Write the failing tests**

```ts
it('zeigt unten die zuletzt erfasste Beobachtung und den Notiz-Button', () => {
  cy.get('[data-testid="obs-WINNER"]').click();
  cy.get('[data-testid="last-observation"]').should('contain', 'Winner');
  cy.get('[data-testid="notes-btn"]').should('be.visible');
});

it('öffnet die Notizen als Vollbild und kehrt zurück', () => {
  cy.get('[data-testid="notes-btn"]').click();
  cy.get('[data-testid="player-notes"]').should('be.visible');
  cy.get('[data-testid="panel"]').should('not.exist');
  cy.get('[data-testid="notes-back"]').click();
  cy.get('[data-testid="panel"]').should('be.visible');
});

it('zeigt bei beendetem Match weder Panel noch Umschalter', () => {   // Review Focus 1
  mountScore(makeMatch({ status: 'COMPLETED', score: { ...makeMatch().score, isDone: true, winner: 1 } }));
  cy.get('[data-testid="panel"]').should('not.exist');
  cy.get('[data-testid="switch-player-1"]').should('not.exist');
  cy.contains('Sieger').should('be.visible');
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts`
Expected: FAIL — `[data-testid="last-observation"]` fehlt.

- [ ] **Step 3: Implement the bottom bar and notes view**

Untere Leiste mit der letzten Beobachtung links (`data-testid="last-observation"`) und einem Notiz-Button rechts (`data-testid="notes-btn"`, `min-height: 40px`, Punkt-Indikator wenn Notizen vorhanden). `showNotes = signal(false)` schaltet innerhalb der Route auf eine Vollbildansicht mit Zurück-Pfeil (`data-testid="notes-back"`), die `PlayerNotesComponent` einbettet. Das heutige aufklappbare `<details>` entfällt.

- [ ] **Step 4: Run the whole scoring suite**

Run: `cd frontend && npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts && npx ng test --no-watch`
Expected: PASS

- [ ] **Step 5: Commit and open the PR for TEN-71**

```bash
git add frontend/src/app/features/matches/score/
git commit -m "feat(score): untere Leiste mit Notiz-Einstieg, Notizen als Vollbild"
```

---

## TEN-74 — Match anlegen und Dialoge

### Task 8: Dialoge ohne feste Breite

**Files:**
- Modify: `frontend/src/app/features/players/players.component.ts:150,165`
- Modify: `frontend/src/app/features/matches/score/score.component.ts:431`
- Modify: `frontend/src/app/features/players/player-dialog.component.ts`
- Test: `frontend/src/app/features/players/player-dialog.component.cy.ts`

**Interfaces:**
- Consumes: `LayoutService.isHandset` aus Task 1.

- [ ] **Step 1: Write the failing tests**

```ts
it('füllt bei 390x736 die volle Breite', () => {
  cy.viewport(390, 736);
  cy.get('.mat-mdc-dialog-surface').invoke('outerWidth').should('be.gte', 380);
});

it('bleibt bei 1280x900 bei rund 500px', () => {
  cy.viewport(1280, 900);
  cy.get('.mat-mdc-dialog-surface').invoke('outerWidth').should('be.within', 480, 520);
});

it('hält Abbrechen und Speichern sichtbar, wenn das Formular scrollt', () => {
  cy.viewport(360, 560);
  cy.get('[data-testid="dialog-save"]').should('be.visible');
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && npx cypress run --component --spec src/app/features/players/player-dialog.component.cy.ts`
Expected: FAIL — Breite ist fest 500px.

- [ ] **Step 3: Replace the fixed widths**

An allen drei `dialog.open`-Stellen `width: '500px'` ersetzen durch `maxWidth: '100vw'` plus eine Breitenangabe, die am Handy `100vw` und am Desktop `500px` ergibt — abgeleitet aus `LayoutService.isHandset`. Am Handy zusätzlich volle Höhe; Kopfzeile des Dialogs mit Abbrechen links und Speichern rechts (`data-testid="dialog-save"`) bleibt beim Scrollen stehen.

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd frontend && npx cypress run --component --spec src/app/features/players/player-dialog.component.cy.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/features/players/ frontend/src/app/features/matches/score/score.component.ts
git commit -m "feat(dialogs): volle Breite am Handy, 500px am Desktop"
```

---

### Task 9: Match-Setup gestapelt

**Files:**
- Modify: `frontend/src/app/features/matches/match-setup/match-setup.component.ts` / `.html`
- Test: `frontend/src/app/features/matches/match-setup/match-setup.component.cy.ts`

- [ ] **Step 1: Write the failing tests**

```ts
it('stellt bei 360x692 das Label über das Bedienelement', () => {
  cy.viewport(360, 692);
  cy.get('[data-testid="sets-to-win-label"]').then($l => {
    cy.get('[data-testid="sets-to-win-group"]').then($g => {
      expect($l[0].getBoundingClientRect().bottom).to.be.at.most($g[0].getBoundingClientRect().top + 1);
    });
  });
});

it('läuft bei 360x692 nicht waagerecht über', () => {
  cy.viewport(360, 692);
  cy.document().its('body').then(b => expect(b.scrollWidth).to.be.at.most(b.clientWidth));
});

it('gibt dem Startknopf die volle Breite', () => {
  cy.viewport(360, 692);
  cy.get('[data-testid="start-match"]').invoke('outerWidth').should('be.gte', 320);
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && npx cypress run --component --spec src/app/features/matches/match-setup/match-setup.component.cy.ts`
Expected: FAIL — Label steht neben der Gruppe.

- [ ] **Step 3: Restack the form below 700px**

Seitenrand 16 px, Überschrift kleiner als 28 px. `.form-row` wird unterhalb der Grenze zu `flex-direction: column; align-items: stretch`, die Umschalterzeilen bleiben als Zeile mit dem Schalter rechts. Startknopf über die volle Breite, `min-height: 52px`, `data-testid="start-match"`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd frontend && npx cypress run --component --spec src/app/features/matches/match-setup/match-setup.component.cy.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/features/matches/match-setup/
git commit -m "feat(match-setup): gestapeltes Formular am Handy"
```

---

### Task 10: Spieler aus der Namenssuche anlegen

**Files:**
- Modify: `frontend/src/app/features/matches/match-setup/match-setup.component.ts` / `.html`
- Modify: `frontend/src/app/features/players/player-dialog.component.ts`
- Test: `frontend/src/app/features/matches/match-setup/match-setup.component.cy.ts`

**Interfaces:**
- Consumes: `PlayerDialogComponent`, erweitert um `data.prefillName?: string`.
- Produces: `splitName(input: string): { firstName: string; lastName: string }` in `match-setup.component.ts` — erstes Wort als Vorname, Rest als Nachname.

- [ ] **Step 1: Write the failing tests**

```ts
it('bietet bei fehlendem Treffer das Anlegen an', () => {
  cy.get('[data-testid="player2-input"]').type('Zzz');
  cy.get('[data-testid="create-player-option"]').should('contain', 'Zzz');
});

it('öffnet den Dialog mit dem getippten Namen vorbefüllt', () => {
  cy.get('[data-testid="create-player-option"]').click();
  cy.get('[formcontrolname="firstName"]').should('have.value', 'Zzz');
});

it('übernimmt den angelegten Spieler in das Feld, aus dem heraus angelegt wurde', () => {
  cy.intercept('POST', '**/api/players', { id: 'p9', firstName: 'Zzz', lastName: 'Test' }).as('create');
  cy.get('[data-testid="dialog-save"]').click();
  cy.wait('@create');
  cy.get('[data-testid="player2-input"]').should('have.value', 'Test Zzz');
});

it('bietet das Anlegen nicht an, wenn ein Treffer existiert', () => {
  cy.get('[data-testid="player2-input"]').clear().type('Nadal');
  cy.get('[data-testid="create-player-option"]').should('not.exist');
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && npx cypress run --component --spec src/app/features/matches/match-setup/match-setup.component.cy.ts`
Expected: FAIL — `[data-testid="create-player-option"]` existiert nicht.

- [ ] **Step 3: Implement `splitName` and the create option**

`splitName` trennt am ersten Leerzeichen. Beide Autocompletes bekommen als letzte Option „« {Eingabe} » neu anlegen" mit `data-testid="create-player-option"`, sichtbar nur wenn die gefilterte Liste leer ist und die Eingabe nicht leer. Klick öffnet `PlayerDialogComponent` mit `data.prefillName`; der Dialog befüllt Vor- und Nachname daraus vor. Nach dem Speichern wird der zurückgegebene Spieler in genau das Feld gesetzt, aus dem heraus angelegt wurde.

- [ ] **Step 4: Add the i18n keys**

`matchSetup.createPlayer` (mit Platzhalter für den getippten Namen) in alle vier Dateien `public/i18n/{de,en,it,fr}.json`, bezogen über `TranslatePipe`.

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd frontend && npx cypress run --component --spec src/app/features/matches/match-setup/match-setup.component.cy.ts && npx ng test --no-watch`
Expected: PASS

- [ ] **Step 6: Commit and open the PR for TEN-74**

```bash
git add frontend/src/app/features/matches/match-setup/ frontend/src/app/features/players/player-dialog.component.ts
git commit -m "feat(match-setup): Spieler direkt aus der Namenssuche anlegen"
```

---

## TEN-75 — Spielerliste als Kartenliste

### Task 11: Kartenliste unterhalb der Grenze

**Files:**
- Create: `frontend/src/app/features/players/player-card-list.component.ts` / `.html`
- Modify: `frontend/src/app/features/players/players.component.ts` / `.html`
- Test: `frontend/src/app/features/players/players.component.cy.ts`

**Interfaces:**
- Consumes: `LayoutService.isHandset`, `Player` aus `core/models/player.model`.
- Produces: `PlayerCardListComponent` mit `input.required<Player[]>()` als `players` und Outputs `edit`, `history`, `compare`, `remove`, `openMatch` — je `output<Player>()`.

- [ ] **Step 1: Write the failing tests**

```ts
it('zeigt bei 390x736 Karten statt Tabelle', () => {
  cy.viewport(390, 736);
  cy.get('[data-testid="player-card"]').should('have.length.greaterThan', 0);
  cy.get('table').should('not.exist');
});

it('zeigt bei 1280x900 die Tabelle', () => {
  cy.viewport(1280, 900);
  cy.get('table').should('be.visible');
  cy.get('[data-testid="player-card"]').should('not.exist');
});

it('öffnet beim Tipp auf die Karte den Bearbeiten-Dialog', () => {
  cy.get('[data-testid="player-card"]').first().click();
  cy.get('[formcontrolname="firstName"]').should('be.visible');
});

it('zeigt das Ballsymbol nur bei laufendem Match und führt dorthin', () => {
  cy.get('[data-testid="player-card"]').first().find('[data-testid="open-match"]').click();
  // Navigation auf matches/<id>/score prüfen
});

it('bietet die übrigen Aktionen hinter dem Überlaufmenü an', () => {
  cy.get('[data-testid="player-card"]').first().find('[data-testid="player-menu"]').click();
  cy.get('[data-testid="player-history-btn"]').should('be.visible');
  cy.get('[data-testid="compare-btn"]').should('be.visible');
});

it('läuft bei 360x692 nicht waagerecht über', () => {
  cy.viewport(360, 692);
  cy.document().its('body').then(b => expect(b.scrollWidth).to.be.at.most(b.clientWidth));
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd frontend && npx cypress run --component --spec src/app/features/players/players.component.cy.ts`
Expected: FAIL — `[data-testid="player-card"]` existiert nicht.

- [ ] **Step 3: Implement `PlayerCardListComponent`**

Je Karte `data-testid="player-card"`: Name, darunter Ranking und Spielhand, rechts das Ballsymbol (`data-testid="open-match"`, nur bei `activeMatchId`) und ein Überlaufmenü (`data-testid="player-menu"`) mit Historie, Head-to-Head und Löschen beziehungsweise Inaktivieren — die heutigen `data-testid`s bleiben erhalten. Tipp auf die Karte löst `edit` aus. Inaktive Spieler werden gedämpft dargestellt.

- [ ] **Step 4: Switch in `players.component`**

Unterhalb der Grenze die Kartenliste, darüber die Tabelle. Kopfzeile stapelt unterhalb der Grenze, Seitenrand 16 px. Der Admin-Bereichsumschalter bleibt in beiden Darstellungen.

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd frontend && npx cypress run --component --spec src/app/features/players/players.component.cy.ts && npx ng test --no-watch`
Expected: PASS

- [ ] **Step 6: Commit and open the PR for TEN-75**

```bash
git add frontend/src/app/features/players/
git commit -m "feat(players): Kartenliste am Handy, Tabelle am Desktop"
```

---

## TEN-76 — Durchsicht der Leseseiten

### Task 12: Vier Leseseiten am Gerät prüfen

**Files:**
- Review: `features/matches/statistics`, `features/statistics/head-to-head`, `features/matches/analysis`, `features/players/match-history`

Keine TDD-Aufgabe — eine Durchsicht, deren Ergebnis Tickets sind.

- [ ] **Step 1: Durchsicht bei 360 × 692 und 390 × 736**

Je Seite prüfen: Seitenränder, Schriftgrössen unter 11 px, Tippziele unter 44 px, waagerechtes Überlaufen (besonders das Gitter `64px 1fr 64px` im Head-to-Head und lange Spielernamen), Verhalten im Querformat.

- [ ] **Step 2: Kleinstkorrekturen anwenden**

Nur Seitenrand und Schriftgrösse, wo keine Layoutänderung nötig ist. Alles andere wird notiert, nicht behoben.

- [ ] **Step 3: Befunde erfassen**

Kommentar an TEN-76 mit der Liste des Gefundenen, dazu je ein Ticket für alles, was eine Layoutänderung braucht.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/features/
git commit -m "fix(ui): Seitenränder und Schriftgrössen der Leseseiten am Handy"
```

---

## Abschluss

- [ ] Alle Tests grün: `cd frontend && npx ng test --no-watch && npx cypress run --component`
- [ ] Jede der drei Zielbreiten (360 / 390 / 430) am Gerät oder in der Emulation durchgeklickt
- [ ] Linear-Status aller fünf Tickets auf Done
