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

