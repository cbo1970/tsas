import { HeadToHeadComponent } from './head-to-head.component';
import { ActivatedRoute } from '@angular/router';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { Player } from '../../../core/models/player.model';
import { HeadToHeadStatistics } from '../../../core/models/statistics.model';

const PLAYERS: Player[] = [
  { id: 'a', ownerId: 'owner-1', firstName: 'Roger', lastName: 'Federer', gender: 'MALE', handedness: 'RIGHT', backhandType: 'ONE_HANDED', active: true },
  { id: 'b', ownerId: 'owner-1', firstName: 'Rafael', lastName: 'Nadal', gender: 'MALE', handedness: 'LEFT', backhandType: 'TWO_HANDED', active: true },
];

const STATS: HeadToHeadStatistics = {
  player1Id: 'a', player2Id: 'b', matchesPlayed: 3,
  player1: {
    playerId: 'a', firstServePercentage: 0.62, firstServeWonPercentage: 0.7, secondServeWonPercentage: 0.5,
    aces: 12, doubleFaults: 4, returnPointsWonFirstPercentage: 0.3, returnPointsWonSecondPercentage: 0.55,
    breakPointsWon: 6, breakPointsPlayed: 10, breakPointsWonPercentage: 0.6, returnGamesWonPercentage: 0.25,
    winners: 40, unforcedErrors: 22, winnersPercentage: 0.33, unforcedErrorPercentage: 0.18,
    matchesWon: 2, matchesLost: 1, setsWon: 5, setsLost: 3,
  },
  player2: {
    playerId: 'b', firstServePercentage: 0.58, firstServeWonPercentage: 0.65, secondServeWonPercentage: 0.45,
    aces: 8, doubleFaults: 6, returnPointsWonFirstPercentage: 0.28, returnPointsWonSecondPercentage: 0.5,
    breakPointsWon: 4, breakPointsPlayed: 9, breakPointsWonPercentage: 0.44, returnGamesWonPercentage: 0.2,
    winners: 35, unforcedErrors: 28, winnersPercentage: 0.29, unforcedErrorPercentage: 0.23,
    matchesWon: 1, matchesLost: 2, setsWon: 3, setsLost: 5,
  },
};

function routeStub(player1: string | null = null, player2: string | null = null) {
  return {
    snapshot: { queryParamMap: { get: (k: string) => (k === 'player1' ? player1 : k === 'player2' ? player2 : null) } },
  };
}

function mount(player1: string | null = null, player2: string | null = null) {
  cy.intercept('GET', '**/api/players', PLAYERS).as('getPlayers');
  cy.intercept('GET', '**/api/statistics/head-to-head*', STATS).as('getH2H');
  cy.mount(HeadToHeadComponent, {
    providers: [
      provideRouter([]),
      provideHttpClient(),
      provideAnimationsAsync(),
      { provide: ActivatedRoute, useValue: routeStub(player1, player2) },
    ],
  });
  cy.wait('@getPlayers');
}

describe('HeadToHeadComponent', () => {
  it('renders two player selects', () => {
    mount();
    cy.get('[data-testid="select-p1"]').should('exist');
    cy.get('[data-testid="select-p2"]').should('exist');
  });

  it('fetches and renders stats when both players are preselected via query params', () => {
    mount('a', 'b');
    cy.wait('@getH2H');
    cy.get('[data-testid="matches-played"]').should('contain', '3');
    cy.get('[data-testid="val-p1-aces"]').should('contain', '12');
    cy.get('[data-testid="val-p2-aces"]').should('contain', '8');
  });

  it('shows match balance', () => {
    mount('a', 'b');
    cy.wait('@getH2H');
    cy.get('[data-testid="val-p1-matches"]').should('contain', '2');
    cy.get('[data-testid="val-p2-matches"]').should('contain', '1');
  });

  it('does not fetch until both players are chosen', () => {
    mount('a', null);
    cy.get('[data-testid="empty-hint"]').should('be.visible');
    cy.get('@getH2H').should('not.exist');
  });
});
