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

