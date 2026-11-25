import { MatchSetupComponent } from './match-setup.component';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import { Player } from '../../../core/models/player.model';
import { testTranslateProviders } from '../../../core/i18n/test-providers';

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
      ...testTranslateProviders,
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

describe('MatchSetupComponent — Layout', () => {

  beforeEach(() => mountSetup());

  it('stellt bei 360x692 das Label über das Bedienelement', () => {
    cy.viewport(360, 692);
    cy.get('[data-testid="sets-to-win-label"]').then($l => {
      cy.get('[data-testid="sets-to-win-group"]').then($g => {
        expect($l[0].getBoundingClientRect().bottom).to.be.at.most($g[0].getBoundingClientRect().top + 1);
      });
    });
  });

  it('läuft bei 360x692 nicht waagerecht über', () => {
    cy.viewport(360, 692);
    cy.document().its('body').then(b => expect(b.scrollWidth).to.be.at.most(b.clientWidth));
  });

  it('gibt dem Startknopf die volle Breite und mindestens 52px Höhe', () => {
    cy.viewport(360, 692);
    cy.get('[data-testid="start-match"]').invoke('outerWidth').should('be.gte', 320);
    cy.get('[data-testid="start-match"]').invoke('outerHeight').should('be.gte', 52);
  });

  it('hält bei 360x692 alle Bedienelemente mindestens 44px hoch', () => {
    cy.viewport(360, 692);
    cy.get('[data-testid="sets-to-win-group"] mat-radio-button .mdc-radio').each($r => {
      expect($r.outerHeight()).to.be.at.least(44);
      expect($r.outerWidth()).to.be.at.least(44);
    });
    cy.get('.toggle-row').each($r => expect($r.outerHeight()).to.be.at.least(44));
  });

  it('behält bei 1024x768 die Desktop-Darstellung: Label neben der Gruppe, Rand 24px', () => {
    cy.viewport(1024, 768);
    cy.get('[data-testid="sets-to-win-label"]').then($l => {
      cy.get('[data-testid="sets-to-win-group"]').then($g => {
        expect($l[0].getBoundingClientRect().right).to.be.at.most($g[0].getBoundingClientRect().left + 1);
      });
    });
    cy.get('.page-container').should('have.css', 'padding-left', '24px');
    cy.get('h1').should('have.css', 'font-size', '28px');
    cy.get('[data-testid="start-match"]').invoke('outerWidth').should('be.lt', 200);
  });
});
