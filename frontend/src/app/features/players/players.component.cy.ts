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

const DESKTOP: [number, number] = [1280, 900];
const PHONE: [number, number] = [390, 736];

function mountPlayers(
  players: Player[] = PLAYERS,
  extraProviders: any[] = [],
  authMock: AuthService = makeAuthMock(),
  viewport: [number, number] = DESKTOP,
) {
  // Cypress' default component viewport (500x500) counts as handset, so every mount sets one explicitly.
  cy.viewport(...viewport);
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

  describe('card list on a handset (TEN-75)', () => {
    const WITH_MATCH: Player[] = [
      { ...PLAYERS[0], activeMatchId: 'm-1' },
      PLAYERS[1],
      PLAYERS[2],
    ];

    it('shows cards instead of a table at 390x736', () => {
      mountPlayers(PLAYERS, [], makeAuthMock(), PHONE);
      cy.get('[data-testid="player-card"]').should('have.length', 3);
      cy.get('table').should('not.exist');
    });

    it('shows the table instead of cards at 1280x900', () => {
      mountPlayers();
      cy.get('table').should('be.visible');
      cy.get('[data-testid="player-card"]').should('not.exist');
    });

    it('treats a phone in landscape (800x360) as handset', () => {
      mountPlayers(PLAYERS, [], makeAuthMock(), [800, 360]);
      cy.get('[data-testid="player-card"]').should('have.length', 3);
      cy.get('table').should('not.exist');
    });

    it('shows name, ranking and playing hand on the card', () => {
      mountPlayers(PLAYERS, [], makeAuthMock(), PHONE);
      cy.get('[data-testid="player-card"]').first().within(() => {
        cy.contains('Federer').should('be.visible');
        cy.contains('Roger').should('be.visible');
        cy.contains('1').should('be.visible');
        cy.contains('players.handedness.RIGHT').should('be.visible');
      });
    });

    it('opens the edit dialog when tapping the card', () => {
      mountPlayers(PLAYERS, [], makeAuthMock(), PHONE);
      cy.get('[data-testid="player-card"]').first().click();
      cy.get('[formcontrolname="firstName"]').should('be.visible');
    });

    it('shows the ball only for a running match and navigates to it', () => {
      const navigateSpy = cy.stub().as('navigate');
      mountPlayers(WITH_MATCH, [{ provide: Router, useValue: { navigate: navigateSpy } }], makeAuthMock(), PHONE);
      cy.get('[data-testid="open-match"]').should('have.length', 1);
      cy.get('[data-testid="player-card"]').first().find('[data-testid="open-match"]').click();
      cy.get('@navigate').should('have.been.calledWith', ['/matches', 'm-1', 'score']);
      cy.get('[formcontrolname="firstName"]').should('not.exist');
    });

    it('does not open the edit dialog when pressing Enter on the menu or ball button', () => {
      mountPlayers(WITH_MATCH, [{ provide: Router, useValue: { navigate: cy.stub() } }], makeAuthMock(), PHONE);
      cy.get('[data-testid="open-match"]').type('{enter}');
      cy.get('[formcontrolname="firstName"]').should('not.exist');
      cy.get('[data-testid="player-menu"]').first().type('{enter}');
      cy.get('[data-testid="player-history-btn"]').should('be.visible');
      cy.get('[formcontrolname="firstName"]').should('not.exist');
    });

    it('opens the edit dialog with the keyboard on the card', () => {
      mountPlayers(PLAYERS, [], makeAuthMock(), PHONE);
      cy.get('[data-testid="player-card-main"]').first().focus().type('{enter}');
      cy.get('[formcontrolname="firstName"]').should('be.visible');
    });

    it('offers history and head-to-head behind the overflow menu', () => {
      const navigateSpy = cy.stub().as('navigate');
      mountPlayers(PLAYERS, [{ provide: Router, useValue: { navigate: navigateSpy } }], makeAuthMock(), PHONE);
      cy.get('[data-testid="player-history-btn"]').should('not.exist');
      cy.get('[data-testid="player-card"]').first().find('[data-testid="player-menu"]').click();
      cy.get('[data-testid="player-history-btn"]').should('be.visible');
      cy.get('[data-testid="compare-btn"]').should('be.visible');
      cy.get('[data-testid="compare-btn"]').click();
      cy.get('@navigate').should('have.been.calledWith',
        ['/statistics/head-to-head'], { queryParams: { player1: '1' } });
    });

    it('navigates to the match history from the overflow menu', () => {
      const navigateSpy = cy.stub().as('navigate');
      mountPlayers(PLAYERS, [{ provide: Router, useValue: { navigate: navigateSpy } }], makeAuthMock(), PHONE);
      cy.get('[data-testid="player-card"]').first().find('[data-testid="player-menu"]').click();
      cy.get('[data-testid="player-history-btn"]').click();
      cy.get('@navigate').should('have.been.calledWith', ['/players', '1', 'matches']);
    });

    it('offers delete for a deletable player and deactivate otherwise', () => {
      mountPlayers(PLAYERS, [], makeAuthMock(), PHONE);
      cy.get('[data-testid="player-menu"]').eq(0).click();
      cy.get('[data-testid="delete-btn"]').should('be.visible');
      cy.get('[data-testid="deactivate-btn"]').should('not.exist');
      cy.get('body').type('{esc}');
      cy.get('[data-testid="player-menu"]').eq(1).click();
      cy.get('[data-testid="deactivate-btn"]').should('be.visible');
      cy.get('[data-testid="delete-btn"]').should('not.exist');
    });

    it('disables deactivate for an already inactive player', () => {
      mountPlayers(PLAYERS, [], makeAuthMock(), PHONE);
      cy.get('[data-testid="player-menu"]').eq(1).click(); // sorted by last name: Federer, Muster (inactive), Nadal
      cy.get('[data-testid="deactivate-btn"]').should('be.disabled');
    });

    it('dims inactive players', () => {
      mountPlayers(PLAYERS, [], makeAuthMock(), PHONE);
      cy.get('[data-testid="player-card"]').eq(1).should('have.class', 'inactive-card');
      cy.get('[data-testid="player-card"]').eq(0).should('not.have.class', 'inactive-card');
    });

    it('filters the cards with the search field', () => {
      mountPlayers(PLAYERS, [], makeAuthMock(), PHONE);
      cy.get('input[placeholder="players.search"]').type('Nadal');
      cy.get('[data-testid="player-card"]').should('have.length', 1);
      cy.get('[data-testid="player-card"]').contains('Nadal');
    });

    it('keeps the admin scope switch', () => {
      const mixed: Player[] = [
        { ...PLAYERS[0], ownerId: 'owner-1' },
        { ...PLAYERS[1], ownerId: 'owner-2' },
      ];
      mountPlayers(mixed, [], makeAuthMock({ isAdmin: true, userId: 'owner-1' }), PHONE);
      cy.get('[data-cy="admin-scope"]').should('be.visible');
      cy.get('[data-testid="player-card"]').should('have.length', 1);
      cy.get('[data-cy="scope-all"]').click();
      cy.get('[data-testid="player-card"]').should('have.length', 2);
    });

    it('uses a 16px page margin and stacks the header', () => {
      mountPlayers(PLAYERS, [], makeAuthMock(), PHONE);
      cy.get('.page-container').should('have.css', 'padding-left', '16px');
      cy.get('.page-header').should('have.css', 'flex-direction', 'column');
    });

    it('keeps every tap target at least 44x44', () => {
      mountPlayers(WITH_MATCH, [], makeAuthMock(), PHONE);
      cy.get('[data-testid="player-card"]').first().then($c => {
        expect($c[0].getBoundingClientRect().height).to.be.at.least(44);
      });
      cy.get('[data-testid="open-match"], [data-testid="player-menu"]').each($b => {
        const r = $b[0].getBoundingClientRect();
        expect(r.width).to.be.at.least(44);
        expect(r.height).to.be.at.least(44);
      });
      cy.get('[data-testid="h2h-btn"], [data-testid="new-player-btn"]').each($b => {
        expect($b[0].getBoundingClientRect().height).to.be.at.least(44);
      });
    });

    it('does not overflow horizontally at 360x692', () => {
      mountPlayers(WITH_MATCH, [], makeAuthMock({ isAdmin: true, userId: 'owner-1' }), [360, 692]);
      cy.get('[data-testid="player-card"]').should('exist');
      cy.document().its('body').then(b => expect(b.scrollWidth).to.be.at.most(b.clientWidth));
      cy.document().its('documentElement').then(d => expect(d.scrollWidth).to.be.at.most(d.clientWidth));
    });
  });
});
