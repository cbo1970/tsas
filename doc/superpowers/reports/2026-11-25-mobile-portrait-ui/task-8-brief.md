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

