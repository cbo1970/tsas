import { StatisticsComponent } from './statistics.component';
import { ActivatedRoute, Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MatchStatistics } from '../../../core/models/statistics.model';

const MOCK_STATS: MatchStatistics = {
  matchId: 'match-1',
  totalPoints: 30,
  player1: {
    pointsWon: 18, winners: 8, unforcedErrors: 5, forcedErrors: 3,
    aces: 3, doubleFaults: 1, firstServePercentage: 0.68,
    secondServePercentage: 0.72, breakPointsWon: 2, breakPointsFaced: 4,
    forehandPercentage: 0.65,
  },
  player2: {
    pointsWon: 12, winners: 5, unforcedErrors: 9, forcedErrors: 2,
    aces: 1, doubleFaults: 2, firstServePercentage: 0.55,
    secondServePercentage: 0.60, breakPointsWon: 1, breakPointsFaced: 3,
    forehandPercentage: 0.58,
  },
};

const activatedRouteStub = {
  snapshot: {
    paramMap: { get: (key: string) => key === 'id' ? 'match-1' : null },
    queryParamMap: {
      get: (key: string) => {
        if (key === 'sets') return '6-4,3-6,7-5';
        if (key === 'p1') return 'Müller';
        if (key === 'p2') return 'Meier';
        return null;
      },
    },
  },
};

function mountStats(extraProviders: any[] = []) {
  cy.intercept('GET', '**/api/matches/match-1/statistics', MOCK_STATS).as('getStats');
  cy.mount(StatisticsComponent, {
    providers: [
      provideRouter([]),
      provideHttpClient(),
      provideAnimationsAsync(),
      { provide: ActivatedRoute, useValue: activatedRouteStub },
      ...extraProviders,
    ],
  });
  cy.wait('@getStats');
}

describe('StatisticsComponent', () => {
  it('renders player names from query params', () => {
    mountStats();
    cy.contains('Müller').should('exist');
    cy.contains('Meier').should('exist');
  });

  it('renders one set score row per set', () => {
    mountStats();
    cy.get('[data-testid="set-row"]').should('have.length', 3);
  });

  it('highlights winning set score badge', () => {
    mountStats();
    cy.get('[data-testid="set-row"]').eq(0)
      .find('[data-testid="badge-p1"]').should('have.class', 'winner');
    cy.get('[data-testid="set-row"]').eq(1)
      .find('[data-testid="badge-p2"]').should('have.class', 'winner');
  });

  it('renders all stat rows', () => {
    mountStats();
    cy.get('[data-testid="stat-row"]').should('have.length.gte', 9);
  });

  it('displays correct values for aces', () => {
    mountStats();
    cy.get('[data-testid="val-p1-aces"]').should('contain', '3');
    cy.get('[data-testid="val-p2-aces"]').should('contain', '1');
  });

  it('displays firstServePercentage as percent', () => {
    mountStats();
    cy.get('[data-testid="val-p1-first-serve"]').should('contain', '68%');
    cy.get('[data-testid="val-p2-first-serve"]').should('contain', '55%');
  });

  it('displays break points as fraction', () => {
    mountStats();
    cy.get('[data-testid="val-p1-break-points"]').should('contain', '2/4');
    cy.get('[data-testid="val-p2-break-points"]').should('contain', '1/3');
  });

  it('navigates to /players when back button clicked', () => {
    const navigateSpy = cy.stub().as('routerNavigate');
    mountStats([{ provide: Router, useValue: { navigate: navigateSpy } }]);
    cy.get('[data-testid="back-btn"]').click();
    cy.get('@routerNavigate').should('have.been.calledWith', ['/players']);
  });

  it('navigates to the analysis page with player names when analysis button clicked', () => {
    const navigateSpy = cy.stub().as('routerNavigate');
    mountStats([{ provide: Router, useValue: { navigate: navigateSpy } }]);
    cy.get('[data-testid="analysis-btn"]').click();
    cy.get('@routerNavigate').should('have.been.calledWith',
      ['/matches', 'match-1', 'analysis'],
      { queryParams: { p1: 'Müller', p2: 'Meier' } });
  });
});
