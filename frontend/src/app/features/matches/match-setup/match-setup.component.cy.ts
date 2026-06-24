import { MatchSetupComponent } from './match-setup.component';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import { Player } from '../../../core/models/player.model';

const FREE: Player = {
  id: '1', ownerId: 'owner-1', firstName: 'Roger', lastName: 'Federer',
  gender: 'MALE', handedness: 'RIGHT', backhandType: 'ONE_HANDED',
};

const BUSY: Player = {
  id: '2', ownerId: 'owner-1', firstName: 'Rafael', lastName: 'Nadal',
  gender: 'MALE', handedness: 'LEFT', backhandType: 'TWO_HANDED',
  activeMatchId: 'match-99',
};

const FREE2: Player = {
  id: '3', ownerId: 'owner-1', firstName: 'Anna', lastName: 'Muster',
  gender: 'FEMALE', handedness: 'RIGHT', backhandType: 'TWO_HANDED',
};

function mountSetup(players: Player[] = [FREE, BUSY, FREE2]) {
  cy.intercept('GET', '**/api/players', players).as('getPlayers');
  cy.mount(MatchSetupComponent, {
    providers: [
      provideHttpClient(),
      provideAnimationsAsync(),
      provideRouter([]),
    ],
  });
  cy.wait('@getPlayers');
}

describe('MatchSetupComponent — player autocomplete filter', () => {

  beforeEach(() => mountSetup());

  it('shows available players when input is focused', () => {
    cy.get('input').first().click();
    cy.get('mat-option').contains('Federer Roger').should('exist');
    cy.get('mat-option').contains('Muster Anna').should('exist');
  });

  it('hides players with active match from Spieler 1', () => {
    cy.get('input').first().click();
    cy.get('mat-option').contains('Nadal Rafael').should('not.exist');
  });

  it('filters Spieler 1 options by typed name', () => {
    cy.get('input').first().type('Fed');
    cy.get('mat-option').contains('Federer Roger').should('exist');
    cy.get('mat-option').contains('Muster Anna').should('not.exist');
  });

  it('shows available players when Spieler 2 input is focused', () => {
    cy.get('input').eq(1).click();
    cy.get('mat-option').contains('Federer Roger').should('exist');
    cy.get('mat-option').contains('Muster Anna').should('exist');
  });

  it('hides players with active match from Spieler 2', () => {
    cy.get('input').eq(1).click();
    cy.get('mat-option').contains('Nadal Rafael').should('not.exist');
  });

  it('filters Spieler 2 options by typed name', () => {
    cy.get('input').eq(1).type('Muster');
    cy.get('mat-option').contains('Muster Anna').should('exist');
    cy.get('mat-option').contains('Federer Roger').should('not.exist');
  });

  it('shows empty dropdown when all players have active matches', () => {
    mountSetup([BUSY]);
    cy.get('input').first().click();
    cy.get('mat-option').should('not.exist');
  });
});
