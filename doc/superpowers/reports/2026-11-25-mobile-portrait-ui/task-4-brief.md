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

