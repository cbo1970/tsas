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

