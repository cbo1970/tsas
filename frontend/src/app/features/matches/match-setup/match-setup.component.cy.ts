import { MatchSetupComponent } from './match-setup.component';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import { Player } from '../../../core/models/player.model';

const FREE: Player = {
  id: '1', firstName: 'Roger', lastName: 'Federer',
  gender: 'MALE', handedness: 'RIGHT', backhandType: 'ONE_HANDED',
};

const BUSY: Player = {
  id: '2', firstName: 'Rafael', lastName: 'Nadal',
  gender: 'MALE', handedness: 'LEFT', backhandType: 'TWO_HANDED',
  activeMatchId: 'match-99',
};

const FREE2: Player = {
  id: '3', firstName: 'Anna', lastName: 'Muster',
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

describe('MatchSetupComponent — player filter', () => {

  beforeEach(() => mountSetup());

  it('shows available players in Spieler 1 dropdown', () => {
    cy.get('mat-select').first().click();
    cy.get('mat-option').contains('Roger Federer').should('exist');
    cy.get('mat-option').contains('Anna Muster').should('exist');
  });

  it('hides players with active match from Spieler 1 dropdown', () => {
    cy.get('mat-select').first().click();
    cy.get('mat-option').contains('Rafael Nadal').should('not.exist');
  });

  it('shows available players in Spieler 2 dropdown', () => {
    cy.get('mat-select').eq(1).click();
    cy.get('mat-option').contains('Roger Federer').should('exist');
    cy.get('mat-option').contains('Anna Muster').should('exist');
  });

  it('hides players with active match from Spieler 2 dropdown', () => {
    cy.get('mat-select').eq(1).click();
    cy.get('mat-option').contains('Rafael Nadal').should('not.exist');
  });

  it('shows all players when none have an active match', () => {
    mountSetup([FREE, FREE2]);
    cy.get('mat-select').first().click();
    cy.get('mat-option').should('have.length', 2);
  });

  it('shows empty dropdowns when all players have active matches', () => {
    mountSetup([BUSY]);
    cy.get('mat-select').first().click();
    cy.get('mat-option').should('not.exist');
  });
});
