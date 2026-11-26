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

