import { PlayersComponent } from './players.component';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { Player } from '../../core/models/player.model';

const PLAYERS: Player[] = [
  {
    id: '1', firstName: 'Roger', lastName: 'Federer',
    gender: 'MALE', handedness: 'RIGHT', backhandType: 'ONE_HANDED',
    ranking: '1', active: true, deletable: true,
  },
  {
    id: '2', firstName: 'Rafael', lastName: 'Nadal',
    gender: 'MALE', handedness: 'LEFT', backhandType: 'TWO_HANDED',
    ranking: '2', active: true, deletable: false,
  },
  {
    id: '3', firstName: 'Anna', lastName: 'Muster',
    gender: 'FEMALE', handedness: 'RIGHT', backhandType: 'TWO_HANDED',
    active: false, deletable: false,
  },
];

function mountPlayers(players: Player[] = PLAYERS) {
  cy.intercept('GET', '**/api/players', players).as('getPlayers');
  cy.mount(PlayersComponent, {
    providers: [provideHttpClient(), provideAnimationsAsync()],
  });
  cy.wait('@getPlayers');
}

describe('PlayersComponent', () => {

  describe('initial render', () => {
    beforeEach(() => mountPlayers());

    it('renders the page heading', () => {
      cy.contains('h1', 'Spieler').should('be.visible');
    });

    it('shows all players in the table', () => {
      cy.contains('Roger').should('be.visible');
      cy.contains('Federer').should('be.visible');
      cy.contains('Nadal').should('be.visible');
      cy.contains('Muster').should('be.visible');
    });

    it('shows "Neuer Spieler" button', () => {
      cy.contains('button', 'Neuer Spieler').should('be.visible');
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
      cy.get('input[placeholder="Suchen..."]').type('Roger');
      cy.contains('td', 'Roger').should('be.visible');
      cy.contains('td', 'Nadal').should('not.exist');
    });

    it('filters players by last name', () => {
      cy.get('input[placeholder="Suchen..."]').type('Fed');
      cy.contains('td', 'Federer').should('be.visible');
      cy.contains('td', 'Nadal').should('not.exist');
    });

    it('shows "Keine Spieler gefunden." when search has no match', () => {
      cy.get('input[placeholder="Suchen..."]').type('xyzzzz');
      cy.contains('Keine Spieler gefunden.').should('be.visible');
    });

    it('shows clear button while search term is active', () => {
      cy.get('input[placeholder="Suchen..."]').type('Roger');
      cy.get('button[matSuffix]').should('exist');
    });

    it('clears search and restores list after clicking clear', () => {
      cy.get('input[placeholder="Suchen..."]').type('Roger');
      cy.contains('td', 'Nadal').should('not.exist');
      cy.get('button[matSuffix]').click();
      cy.contains('td', 'Nadal').should('be.visible');
    });
  });

  describe('empty state', () => {
    it('shows "Noch keine Spieler angelegt." when list is empty', () => {
      mountPlayers([]);
      cy.contains('Noch keine Spieler angelegt.').should('be.visible');
    });
  });
});
