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

