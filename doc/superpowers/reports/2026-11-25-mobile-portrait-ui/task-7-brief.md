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

