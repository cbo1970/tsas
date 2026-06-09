import { MatchAnalysisComponent } from './match-analysis.component';
import { ActivatedRoute, Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MatchAnalysis } from '../../../core/models/analysis.model';

const MOCK_ANALYSIS: MatchAnalysis = {
  matchId: 'match-1',
  status: 'COMPLETED',
  keyMoments: 'Der zweite Satz war entscheidend.',
  ownStrengths: 'Starker erster Aufschlag.',
  ownWeaknesses: 'Rückhand unter Druck unsicher.',
  opponentStrengths: 'Sehr gute Beinarbeit.',
  opponentWeaknesses: 'Schwache zweite Aufschläge.',
  recommendations: [
    { priority: 2, title: 'Rückhand stabilisieren', detail: 'Mehr Slice einsetzen.' },
    { priority: 1, title: 'Zweite Aufschläge angreifen', detail: 'Früh am Netz Druck machen.' },
  ],
  modelUsed: 'gpt-4-turbo',
  generatedAt: '2026-06-09T14:32:45.123Z',
  errorMessage: null,
};

const activatedRouteStub = {
  snapshot: {
    paramMap: { get: (key: string) => (key === 'id' ? 'match-1' : null) },
    queryParamMap: {
      get: (key: string) => {
        if (key === 'p1') return 'Müller';
        if (key === 'p2') return 'Meier';
        return null;
      },
    },
  },
};

function mount(extraProviders: any[] = []) {
  cy.mount(MatchAnalysisComponent, {
    providers: [
      provideRouter([]),
      provideHttpClient(),
      provideAnimationsAsync(),
      { provide: ActivatedRoute, useValue: activatedRouteStub },
      ...extraProviders,
    ],
  });
}

describe('MatchAnalysisComponent', () => {
  it('renders player names from query params', () => {
    cy.intercept('GET', '**/api/matches/match-1/analysis', { statusCode: 404, body: {} }).as('getAnalysis');
    mount();
    cy.wait('@getAnalysis');
    cy.contains('Müller').should('exist');
    cy.contains('Meier').should('exist');
  });

  it('shows the empty state with a generate button when no analysis exists (404)', () => {
    cy.intercept('GET', '**/api/matches/match-1/analysis', { statusCode: 404, body: {} }).as('getAnalysis');
    mount();
    cy.wait('@getAnalysis');
    cy.get('[data-testid="empty-state"]').should('exist');
    cy.get('[data-testid="generate-btn"]').should('exist');
    cy.get('[data-testid="error-msg"]').should('not.exist');
  });

  it('auto-loads and renders an existing analysis', () => {
    cy.intercept('GET', '**/api/matches/match-1/analysis', MOCK_ANALYSIS).as('getAnalysis');
    mount();
    cy.wait('@getAnalysis');
    cy.get('[data-testid="analysis-result"]').should('exist');
    cy.get('[data-testid="section-key-moments"]').should('contain', 'entscheidend');
    cy.get('[data-testid="section-own-strengths"]').should('contain', 'Aufschlag');
    cy.get('[data-testid="section-opponent-weaknesses"]').should('contain', 'Aufschläge');
    cy.get('[data-testid="regenerate-btn"]').should('exist');
  });

  it('renders recommendations sorted by priority', () => {
    cy.intercept('GET', '**/api/matches/match-1/analysis', MOCK_ANALYSIS).as('getAnalysis');
    mount();
    cy.wait('@getAnalysis');
    cy.get('[data-testid="recommendation"]').should('have.length', 2);
    cy.get('[data-testid="recommendation"]').eq(0).should('contain', 'Zweite Aufschläge angreifen');
    cy.get('[data-testid="recommendation"]').eq(1).should('contain', 'Rückhand stabilisieren');
  });

  it('generates an analysis on button click and renders the result', () => {
    cy.intercept('GET', '**/api/matches/match-1/analysis', { statusCode: 404, body: {} }).as('getAnalysis');
    cy.intercept('POST', '**/api/matches/match-1/analysis', MOCK_ANALYSIS).as('postAnalysis');
    mount();
    cy.wait('@getAnalysis');
    cy.get('[data-testid="generate-btn"]').click();
    cy.wait('@postAnalysis');
    cy.get('[data-testid="analysis-result"]').should('exist');
    cy.get('[data-testid="section-own-weaknesses"]').should('contain', 'Rückhand');
  });

  it('shows "Match noch nicht beendet" on 409', () => {
    cy.intercept('GET', '**/api/matches/match-1/analysis', { statusCode: 404, body: {} }).as('getAnalysis');
    cy.intercept('POST', '**/api/matches/match-1/analysis', {
      statusCode: 409,
      body: { detail: 'Match match-1 is not COMPLETED' },
    }).as('postAnalysis');
    mount();
    cy.wait('@getAnalysis');
    cy.get('[data-testid="generate-btn"]').click();
    cy.wait('@postAnalysis');
    cy.get('[data-testid="error-msg"]').should('contain', 'Match noch nicht beendet');
  });

  it('shows "Zu wenig Punkte erfasst" on 422', () => {
    cy.intercept('GET', '**/api/matches/match-1/analysis', { statusCode: 404, body: {} }).as('getAnalysis');
    cy.intercept('POST', '**/api/matches/match-1/analysis', {
      statusCode: 422,
      body: { detail: 'Match must have at least 10 points (found 3)' },
    }).as('postAnalysis');
    mount();
    cy.wait('@getAnalysis');
    cy.get('[data-testid="generate-btn"]').click();
    cy.wait('@postAnalysis');
    cy.get('[data-testid="error-msg"]').should('contain', 'mindestens 10');
  });

  it('shows the LLM error and the server detail on 502', () => {
    cy.intercept('GET', '**/api/matches/match-1/analysis', { statusCode: 404, body: {} }).as('getAnalysis');
    cy.intercept('POST', '**/api/matches/match-1/analysis', {
      statusCode: 502,
      body: { detail: 'LLM call failed for match match-1 — upstream timeout' },
    }).as('postAnalysis');
    mount();
    cy.wait('@getAnalysis');
    cy.get('[data-testid="generate-btn"]').click();
    cy.wait('@postAnalysis');
    cy.get('[data-testid="error-msg"]').should('contain', 'LLM-Fehler');
    cy.get('[data-testid="error-msg"]').should('contain', 'upstream timeout');
  });

  it('shows the error message of a persisted FAILED analysis', () => {
    cy.intercept('GET', '**/api/matches/match-1/analysis', {
      ...MOCK_ANALYSIS,
      status: 'FAILED',
      errorMessage: 'LLM lieferte ungültiges JSON.',
    }).as('getAnalysis');
    mount();
    cy.wait('@getAnalysis');
    cy.get('[data-testid="error-msg"]').should('contain', 'ungültiges JSON');
    cy.get('[data-testid="analysis-result"]').should('not.exist');
    cy.get('[data-testid="generate-btn"]').should('exist');
  });

  it('navigates to /players when back button clicked', () => {
    cy.intercept('GET', '**/api/matches/match-1/analysis', { statusCode: 404, body: {} }).as('getAnalysis');
    const navigateSpy = cy.stub().as('routerNavigate');
    mount([{ provide: Router, useValue: { navigate: navigateSpy } }]);
    cy.wait('@getAnalysis');
    cy.get('[data-testid="back-btn"]').click();
    cy.get('@routerNavigate').should('have.been.calledWith', ['/players']);
  });
});
