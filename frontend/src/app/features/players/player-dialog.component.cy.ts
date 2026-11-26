import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import { computed } from '@angular/core';
import { PlayersComponent } from './players.component';
import { AuthService } from '../../core/auth/auth.service';
import { testTranslateProviders } from '../../core/i18n/test-providers';

const authMock = {
  isAdmin: computed(() => false),
  userId: computed(() => null),
  roles: computed(() => ['COACH']),
  initialize: () => Promise.resolve(true),
  userName: () => 'Test User',
  logout: () => {},
  login: () => {},
  isAuthenticated: () => true,
} as unknown as AuthService;

/** Opens the real "new player" dialog through PlayersComponent, so the production dialog.open config is under test. */
function openPlayerDialog(width: number, height: number) {
  cy.viewport(width, height);
  cy.intercept('GET', '**/api/players', []).as('getPlayers');
  cy.mount(PlayersComponent, {
    providers: [
      provideRouter([]),
      provideHttpClient(),
      provideAnimationsAsync(),
      ...testTranslateProviders,
      { provide: AuthService, useValue: authMock },
    ],
  });
  cy.wait('@getPlayers');
  cy.contains('button', 'players.newPlayer').click();
  cy.get('.mat-mdc-dialog-surface').should('be.visible');
}

describe('PlayerDialogComponent responsive', () => {
  it('füllt bei 390x736 die volle Breite und Höhe', () => {
    openPlayerDialog(390, 736);
    cy.get('.mat-mdc-dialog-surface').invoke('outerWidth').should('be.gte', 380);
    cy.get('.mat-mdc-dialog-surface').invoke('outerHeight').should('be.gte', 726);
  });

  it('bleibt bei 1280x900 bei rund 500px', () => {
    openPlayerDialog(1280, 900);
    cy.get('.mat-mdc-dialog-surface').invoke('outerWidth').should('be.within', 480, 520);
    cy.get('.mat-mdc-dialog-surface').invoke('outerHeight').should('be.lt', 880);
  });

  it('hält Abbrechen und Speichern sichtbar, wenn das Formular scrollt', () => {
    openPlayerDialog(360, 560);
    cy.get('mat-dialog-content').scrollTo('bottom');
    cy.get('[data-testid="dialog-save"]').should('be.visible').then(($el) => {
      expect($el[0].getBoundingClientRect().top).to.be.lessThan(100);
    });
    cy.get('[data-testid="dialog-cancel"]').should('be.visible');
  });

  // M1: Am Handy kommt die Kopfzeile aus DialogHeaderComponent. Ohne
  // mat-dialog-title bleibt role="dialog" ohne zugaenglichen Namen.
  it('gibt dem Dialog am Handy einen zugänglichen Namen', () => {
    openPlayerDialog(390, 736);
    cy.get('.mat-mdc-dialog-container')
      .invoke('attr', 'aria-labelledby')
      .should('be.a', 'string')
      .should('not.be.empty')
      .then((id) => {
        cy.get(`#${id}`).should('contain', 'Neuer Spieler');
      });
  });

  // M2: mat-dialog-title schleppt die Headline-Typografie des Dialog-Containers
  // mit (padding 6px 24px 13px, ::before mit 40px Hoehe, 32px Zeilenhoehe).
  // Ungebremst wird die Titelbox statt ~22px rund 91px hoch, die Kopfzeile
  // waechst auf ueber 100px und dem Titel fehlen 48px Breite — und das auf
  // genau dem Bildschirm, fuer den die Kopfzeile verdichtet wurde.
  it('haelt die Kopfzeile bei 360px flach', () => {
    openPlayerDialog(360, 640);
    cy.get('.dialog-header').then(($h) => {
      const header = $h[0] as HTMLElement;
      const title = header.querySelector('.dialog-header-title') as HTMLElement;
      const lineHeight = parseFloat(getComputedStyle(title).lineHeight);
      expect(title.getBoundingClientRect().height, 'Hoehe des Titels')
        .to.be.at.most(2 * lineHeight + 1);
      expect(header.getBoundingClientRect().height, 'Hoehe der Kopfzeile').to.be.at.most(64);
    });
  });

  it('hat am Handy Tap-Targets von mindestens 44x44', () => {
    openPlayerDialog(390, 736);
    for (const id of ['dialog-save', 'dialog-cancel']) {
      cy.get(`[data-testid="${id}"]`).then(($el) => {
        // offset* are layout pixels; getBoundingClientRect would include Cypress' preview scaling
        const el = $el[0] as HTMLElement;
        expect(el.offsetWidth).to.be.gte(44);
        expect(el.offsetHeight).to.be.gte(44);
      });
    }
  });
});
