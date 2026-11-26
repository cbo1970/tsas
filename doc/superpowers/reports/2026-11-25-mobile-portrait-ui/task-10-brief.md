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

