import { PlayerNotesComponent } from './player-notes.component';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { testTranslateProviders } from '../../../core/i18n/test-providers';

const MATCH = {
  id: 'match-1', player1Id: 'p1', player2Id: 'p2', status: 'IN_PROGRESS',
  score: {},
};

function mount() {
  cy.mount(PlayerNotesComponent, {
    providers: [provideHttpClient(), provideAnimationsAsync(), ...testTranslateProviders],
    componentProperties: { matchId: 'match-1' },
  });
}

describe('PlayerNotesComponent', () => {
  beforeEach(() => {
    cy.intercept('GET', '**/api/matches/match-1', MATCH).as('getMatch');
  });

  it('renders two note inputs and prefills existing notes', () => {
    cy.intercept('GET', '**/api/matches/match-1/notes', [
      { playerId: 'p1', note: 'RH longline', updatedAt: '2026-06-26T10:00:00Z' },
    ]).as('getNotes');
    mount();
    cy.wait(['@getMatch', '@getNotes']);
    cy.get('[data-testid="note-input-0"]').should('have.value', 'RH longline');
    cy.get('[data-testid="note-input-1"]').should('have.value', '');
    cy.contains('playerNotes.roleOwn').should('exist');
    cy.contains('playerNotes.roleOpponent').should('exist');
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
