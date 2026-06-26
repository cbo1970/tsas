import { PlayerNotesComponent } from './player-notes.component';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { testTranslateProviders } from '../../../core/i18n/test-providers';

const MATCH = {
  id: 'match-1', player1Id: 'p1', player2Id: 'p2', status: 'IN_PROGRESS',
  score: {},
};
const PLAYER1 = { id: 'p1', firstName: 'Max', lastName: 'Muster' };
const PLAYER2 = { id: 'p2', firstName: 'Tom', lastName: 'Gegner' };

function mount() {
  cy.mount(PlayerNotesComponent, {
    providers: [provideHttpClient(), provideAnimationsAsync(), ...testTranslateProviders],
    componentProperties: { matchId: 'match-1' },
  });
}

describe('PlayerNotesComponent', () => {
  beforeEach(() => {
    cy.intercept('GET', '**/api/matches/match-1', MATCH).as('getMatch');
    cy.intercept('GET', '**/api/players/p1', PLAYER1).as('getP1');
    cy.intercept('GET', '**/api/players/p2', PLAYER2).as('getP2');
  });

  it('renders two note inputs and prefills existing notes', () => {
    cy.intercept('GET', '**/api/matches/match-1/notes', [
      { playerId: 'p1', note: 'RH longline', updatedAt: '2026-06-26T10:00:00Z' },
    ]).as('getNotes');
    mount();
    cy.wait(['@getMatch', '@getNotes']);
    cy.get('[data-testid="note-input-0"]').should('have.value', 'RH longline');
    cy.get('[data-testid="note-input-1"]').should('have.value', '');
    cy.contains('Max Muster').should('exist');
    cy.contains('Tom Gegner').should('exist');
  });

  it('PUTs the note to the correct player on save', () => {
    cy.intercept('GET', '**/api/matches/match-1/notes', []).as('getNotes');
    cy.intercept('PUT', '**/api/matches/match-1/notes/p2', {
      playerId: 'p2', note: 'Slice schwach', updatedAt: '2026-06-26T11:00:00Z',
    }).as('putNote');
    mount();
    cy.wait(['@getMatch', '@getNotes']);
    cy.get('[data-testid="note-input-1"]').type('Slice schwach');
    cy.get('[data-testid="note-save-1"]').click();
    cy.wait('@putNote').its('request.body').should('deep.equal', { note: 'Slice schwach' });
  });
});
