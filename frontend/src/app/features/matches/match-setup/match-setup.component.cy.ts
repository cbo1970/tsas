import { MatchSetupComponent } from './match-setup.component';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import { Player } from '../../../core/models/player.model';
import { inject, provideAppInitializer } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
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

function mountComponent() {
  cy.mount(MatchSetupComponent, {
    providers: [
      provideHttpClient(),
      provideAnimationsAsync(),
      provideRouter([]),
      ...testTranslateProviders,
      // The test providers return bare keys; the create option must show the typed name, so give it its text.
      provideAppInitializer(() => inject(TranslateService).setTranslation('de', {
        matchSetup: { createPlayer: '« {{name}} » neu anlegen' },
      })),
    ],
  });
}

function mountSetup(players: Player[] = [FREE, BUSY, FREE2]) {
  cy.intercept('GET', '**/api/players', players).as('getPlayers');
  mountComponent();
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

describe('MatchSetupComponent — Spieler aus der Namenssuche anlegen', () => {

  beforeEach(() => mountSetup());

  it('bietet bei fehlendem Treffer das Anlegen an', () => {
    cy.get('[data-testid="player2-input"]').type('Zzz');
    cy.get('[data-testid="create-player-option"]').should('contain', 'Zzz');
  });

  it('bietet das Anlegen nicht an, wenn ein Treffer existiert', () => {
    cy.get('[data-testid="player2-input"]').type('Muster');
    cy.get('mat-option').contains('Muster Anna').should('exist');
    cy.get('[data-testid="create-player-option"]').should('not.exist');
  });

  it('bietet das Anlegen bei leerer Eingabe nicht an', () => {
    mountSetup([BUSY]);
    cy.get('[data-testid="player2-input"]').click();
    cy.get('[data-testid="create-player-option"]').should('not.exist');
  });

  // I2: Ein Spieler mit laufendem Match faellt aus der Auswahlliste, existiert
  // aber. Boete die Maske hier das Anlegen an, entstuende ein zweiter Datensatz
  // und Head-to-Head sowie saemtliche Statistiken spalteten sich still auf
  // zwei Spieler-Ids auf.
  it('bietet das Anlegen nicht an, wenn der Spieler ein laufendes Match hat (Spieler 1)', () => {
    cy.get('[data-testid="player1-input"]').type('Nadal');
    cy.get('[data-testid="create-player-option"]').should('not.exist');
  });

  it('bietet das Anlegen nicht an, wenn der Spieler ein laufendes Match hat (Spieler 2)', () => {
    cy.get('[data-testid="player2-input"]').type('Nadal');
    cy.get('[data-testid="create-player-option"]').should('not.exist');
  });

  // Dieselbe Luecke, solange die Spielerliste noch unterwegs ist: dann ist die
  // gefilterte Liste leer, ohne dass irgendetwas ueber vorhandene Spieler
  // bekannt waere.
  it('bietet das Anlegen nicht an, solange die Spielerliste noch laedt', () => {
    cy.intercept('GET', '**/api/players', { body: [FREE, BUSY, FREE2], delay: 6000 }).as('getPlayersSlow');
    mountComponent();

    cy.get('[data-testid="player1-input"]').type('Federer');
    cy.get('[data-testid="create-player-option"]').should('not.exist');

    cy.wait('@getPlayersSlow');
    cy.get('[data-testid="create-player-option"]').should('not.exist');
    cy.get('mat-option').contains('Federer Roger').should('exist');
  });

  it('öffnet den Dialog mit dem getippten Namen vorbefüllt', () => {
    cy.get('[data-testid="player2-input"]').type('Zzz Test Muster');
    cy.get('[data-testid="create-player-option"]').click();
    cy.get('[formcontrolname="firstName"]').should('have.value', 'Zzz');
    cy.get('[formcontrolname="lastName"]').should('have.value', 'Test Muster');
  });

  it('übernimmt den angelegten Spieler in das Feld, aus dem heraus angelegt wurde', () => {
    cy.intercept('POST', '**/api/players', { id: 'p9', firstName: 'Zzz', lastName: 'Test' }).as('create');
    cy.get('[data-testid="player2-input"]').type('Zzz Test');
    cy.get('[data-testid="create-player-option"]').click();
    cy.get('[data-testid="dialog-save"]').click();
    cy.wait('@create');
    cy.get('[data-testid="player2-input"]').should('have.value', 'Test Zzz');
    cy.get('[data-testid="player1-input"]').should('have.value', '');
  });

  it('übernimmt den angelegten Spieler auch in Spieler 1', () => {
    cy.intercept('POST', '**/api/players', { id: 'p9', firstName: 'Zzz', lastName: 'Test' }).as('create');
    cy.get('[data-testid="player1-input"]').type('Zzz Test');
    cy.get('[data-testid="create-player-option"]').click();
    cy.get('[data-testid="dialog-save"]').click();
    cy.wait('@create');
    cy.get('[data-testid="player1-input"]').should('have.value', 'Test Zzz');
    cy.get('[data-testid="player2-input"]').should('have.value', '');
  });

  it('behält den getippten Text, wenn der Dialog abgebrochen wird', () => {
    cy.get('[data-testid="player2-input"]').type('Zzz Test');
    cy.get('[data-testid="create-player-option"]').click();
    cy.get('[data-testid="dialog-cancel"]').click();
    cy.get('[data-testid="player2-input"]').should('have.value', 'Zzz Test');
    cy.get('[data-testid="player2-input"]').focus();
    cy.get('[data-testid="create-player-option"]').should('contain', 'Zzz Test');
  });

  it('hält die Option bei 360x692 mindestens 44px hoch', () => {
    cy.viewport(360, 692);
    mountSetup();
    cy.get('[data-testid="player2-input"]').focus().type('Zzz');
    cy.get('[data-testid="create-player-option"]').invoke('outerHeight').should('be.gte', 44);
    cy.document().its('body').then(b => expect(b.scrollWidth).to.be.at.most(b.clientWidth));
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
