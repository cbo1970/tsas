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

