import { MatchHistoryComponent } from './match-history.component';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

const ENTRIES = [
  { matchId: 'm2', opponentName: 'Tom Gegner', setsWon: 2, setsLost: 1, won: true, completedAt: '2026-06-26T10:00:00Z' },
  { matchId: 'm1', opponentName: 'Ann Other', setsWon: 0, setsLost: 2, won: false, completedAt: '2026-06-20T09:00:00Z' },
];
const routeStub = { snapshot: { paramMap: { get: (k: string) => (k === 'id' ? 'p1' : null) } } };

function mount(extra: any[] = []) {
  cy.intercept('GET', '**/api/players/p1', { id: 'p1', firstName: 'Self', lastName: 'Player' }).as('getPlayer');
  cy.intercept('GET', '**/api/players/p1/matches', ENTRIES).as('getHistory');
  cy.mount(MatchHistoryComponent, {
    providers: [provideRouter([]), provideHttpClient(), provideAnimationsAsync(),
      { provide: ActivatedRoute, useValue: routeStub }, ...extra],
  });
  cy.wait('@getHistory');
}

describe('MatchHistoryComponent', () => {
  it('renders one row per completed match with opponent + result', () => {
    mount();
    cy.get('[data-testid="history-entry"]').should('have.length', 2);
    cy.get('[data-testid="history-entry"]').first().should('contain', 'Tom Gegner').and('contain', 'S 2:1');
  });

  it('navigates to the statistics of the clicked match', () => {
    const nav = cy.stub().as('nav');
    mount([{ provide: Router, useValue: { navigate: nav } }]);
    cy.get('[data-testid="history-entry"]').first().click();
    cy.get('@nav').should('have.been.calledWith', ['/matches', 'm2', 'statistics']);
  });

  it('shows the empty state when there are no matches', () => {
    cy.intercept('GET', '**/api/players/p1', { id: 'p1', firstName: 'Self', lastName: 'Player' });
    cy.intercept('GET', '**/api/players/p1/matches', []).as('empty');
    cy.mount(MatchHistoryComponent, {
      providers: [provideRouter([]), provideHttpClient(), provideAnimationsAsync(),
        { provide: ActivatedRoute, useValue: routeStub }],
    });
    cy.wait('@empty');
    cy.get('[data-testid="history-empty"]').should('exist');
  });
});
