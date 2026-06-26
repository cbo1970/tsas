import { PlayersComponent } from './players.component';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { computed, signal } from '@angular/core';
import { Player } from '../../core/models/player.model';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { testTranslateProviders } from '../../core/i18n/test-providers';

function makeAuthMock(opts: { isAdmin?: boolean; userId?: string | null } = {}) {
  const isAdminSig = signal(opts.isAdmin ?? false);
  const userIdSig = signal<string | null>(opts.userId ?? null);
  return {
    isAdmin: computed(() => isAdminSig()),
    userId: computed(() => userIdSig()),
    roles: computed(() => (opts.isAdmin ? ['COACH', 'ADMIN'] : ['COACH'])),
    initialize: () => Promise.resolve(true),
    userName: () => 'Test User',
    logout: () => {},
    login: () => {},
    isAuthenticated: () => true,
  } as unknown as AuthService;
}

const PLAYERS: Player[] = [
  {
    id: '1', ownerId: 'owner-1', firstName: 'Roger', lastName: 'Federer',
    gender: 'MALE', handedness: 'RIGHT', backhandType: 'ONE_HANDED',
    ranking: '1', active: true, deletable: true,
  },
  {
    id: '2', ownerId: 'owner-1', firstName: 'Rafael', lastName: 'Nadal',
    gender: 'MALE', handedness: 'LEFT', backhandType: 'TWO_HANDED',
    ranking: '2', active: true, deletable: false,
  },
  {
    id: '3', ownerId: 'owner-1', firstName: 'Anna', lastName: 'Muster',
    gender: 'FEMALE', handedness: 'RIGHT', backhandType: 'TWO_HANDED',
    active: false, deletable: false,
  },
];

function mountPlayers(
  players: Player[] = PLAYERS,
  extraProviders: any[] = [],
  authMock: AuthService = makeAuthMock(),
) {
  cy.intercept('GET', '**/api/players', players).as('getPlayers');
  cy.mount(PlayersComponent, {
    providers: [
      provideRouter([]),
      provideHttpClient(),
      provideAnimationsAsync(),
      ...testTranslateProviders,
      { provide: AuthService, useValue: authMock },
      ...extraProviders,
    ],
  });
  cy.wait('@getPlayers');
}

describe('PlayersComponent', () => {

  describe('initial render', () => {
    beforeEach(() => mountPlayers());

    it('renders the page heading', () => {
      // ngx-translate ohne Loader liefert den Key zurück; in echter Laufzeit „Spieler" / „Players" / …
      cy.contains('h1', 'players.title').should('be.visible');
    });

    it('shows all players in the table', () => {
      cy.contains('Roger').should('be.visible');
      cy.contains('Federer').should('be.visible');
      cy.contains('Nadal').should('be.visible');
      cy.contains('Muster').should('be.visible');
    });

    it('shows "newPlayer" button (i18n key in test runner without loader)', () => {
      cy.contains('button', 'players.newPlayer').should('be.visible');
    });

    it('shows delete button for deletable players', () => {
      cy.get('[title="Spieler löschen"]').should('have.length', 1);
    });

    it('shows deactivate button for non-deletable players', () => {
      cy.get('[title="Spieler inaktivieren"]').should('have.length', 2);
    });
  });

  describe('search filter', () => {
    beforeEach(() => mountPlayers());

    it('filters players by first name', () => {
      cy.get('input[placeholder="players.search"]').type('Roger');
      cy.contains('td', 'Roger').should('be.visible');
      cy.contains('td', 'Nadal').should('not.exist');
    });

    it('filters players by last name', () => {
      cy.get('input[placeholder="players.search"]').type('Fed');
      cy.contains('td', 'Federer').should('be.visible');
      cy.contains('td', 'Nadal').should('not.exist');
    });

    it('shows search-empty message when search has no match', () => {
      cy.get('input[placeholder="players.search"]').type('xyzzzz');
      cy.contains('players.emptySearch').should('be.visible');
    });

    it('shows clear button while search term is active', () => {
      cy.get('input[placeholder="players.search"]').type('Roger');
      cy.get('button[matSuffix]').should('exist');
    });

    it('clears search and restores list after clicking clear', () => {
      cy.get('input[placeholder="players.search"]').type('Roger');
      cy.contains('td', 'Nadal').should('not.exist');
      cy.get('button[matSuffix]').click();
      cy.contains('td', 'Nadal').should('be.visible');
    });
  });

  describe('empty state', () => {
    it('shows empty-state hint when list is empty', () => {
      mountPlayers([]);
      cy.contains('players.empty').should('be.visible');
    });
  });

  describe('head-to-head entry points', () => {
    it('shows the Head-to-Head toolbar button', () => {
      mountPlayers();
      cy.get('[data-testid="h2h-btn"]').should('be.visible');
    });

    it('navigates to the head-to-head route from the toolbar button', () => {
      const navigateSpy = cy.stub().as('navigate');
      mountPlayers(PLAYERS, [{ provide: Router, useValue: { navigate: navigateSpy } }]);
      cy.get('[data-testid="h2h-btn"]').click();
      cy.get('@navigate').should('have.been.calledWith', ['/statistics/head-to-head']);
    });

    it('navigates with a preselected player1 from the row action', () => {
      const navigateSpy = cy.stub().as('navigate');
      mountPlayers(PLAYERS, [{ provide: Router, useValue: { navigate: navigateSpy } }]);
      cy.get('[data-testid="compare-btn"]').first().click();
      cy.get('@navigate').should('have.been.calledWith',
        ['/statistics/head-to-head'], { queryParams: { player1: '1' } });
    });

    it('navigates to match-history route from the history button in the row', () => {
      const navigateSpy = cy.stub().as('navigate');
      mountPlayers(PLAYERS, [{ provide: Router, useValue: { navigate: navigateSpy } }]);
      cy.get('[data-testid="player-history-btn"]').first().click();
      cy.get('@navigate').should('have.been.calledWith', ['/players', '1', 'matches']);
    });
  });

  describe('admin scope toggle (TEN-65)', () => {
    const MIXED_PLAYERS: Player[] = [
      { id: '10', ownerId: 'owner-1', firstName: 'My', lastName: 'Player',
        gender: 'MALE', handedness: 'RIGHT', backhandType: 'ONE_HANDED', active: true, deletable: true },
      { id: '20', ownerId: 'owner-2', firstName: 'Other', lastName: 'Owner',
        gender: 'FEMALE', handedness: 'LEFT', backhandType: 'TWO_HANDED', active: true, deletable: true },
    ];

    it('is hidden for coach role', () => {
      mountPlayers(MIXED_PLAYERS, [], makeAuthMock({ isAdmin: false, userId: 'owner-1' }));
      cy.get('[data-cy="admin-scope"]').should('not.exist');
      // Coach sees what the server returned — both rows present.
      cy.contains('td', 'My Player'.split(' ')[0]).should('be.visible');
      cy.contains('td', 'Other').should('be.visible');
    });

    it('is visible for admin and defaults to "Meine"', () => {
      mountPlayers(MIXED_PLAYERS, [], makeAuthMock({ isAdmin: true, userId: 'owner-1' }));
      cy.get('[data-cy="admin-scope"]').should('be.visible');
      cy.get('[data-cy="scope-mine"]').should('have.class', 'mat-button-toggle-checked');
      cy.contains('td', 'My').should('be.visible');
      cy.contains('td', 'Other').should('not.exist');
    });

    it('shows all owners when admin toggles to "Alle Owner"', () => {
      mountPlayers(MIXED_PLAYERS, [], makeAuthMock({ isAdmin: true, userId: 'owner-1' }));
      cy.get('[data-cy="scope-all"]').click();
      cy.contains('td', 'My').should('be.visible');
      cy.contains('td', 'Other').should('be.visible');
    });
  });
});
