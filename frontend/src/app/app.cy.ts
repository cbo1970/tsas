import { Component, computed } from '@angular/core';
import { Router, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { App } from './app';
import { AuthService } from './core/auth/auth.service';
import { testTranslateProviders } from './core/i18n/test-providers';

@Component({ standalone: true, template: '<p data-testid="page">page</p>' })
class PageStub {}

const authStub: Partial<AuthService> = {
  initialize: () => Promise.resolve(true),
  userName: () => 'Coach',
  logout: () => {},
  isAuthenticated: () => true,
  isAdmin: computed(() => false),
};

function mountApp(url = '/players') {
  cy.mount(App, {
    providers: [
      provideRouter([
        { path: 'players', component: PageStub },
        { path: 'matches/:id/score', component: PageStub },
      ]),
      provideHttpClient(),
      provideAnimationsAsync(),
      ...testTranslateProviders,
      { provide: AuthService, useValue: authStub },
    ],
  }).then(({ fixture }) => {
    const router = fixture.debugElement.injector.get(Router);
    return router.navigateByUrl(url);
  });
}

describe('App — Navigation', () => {
  it('zeigt bei 390x736 die untere Navigationsleiste statt der Toolbar', () => {
    cy.viewport(390, 736);
    mountApp();
    cy.get('[data-testid="bottom-nav"]').should('be.visible');
    cy.get('mat-toolbar').should('not.exist');
  });

  it('zeigt bei 1280x900 die Toolbar und keine Navigationsleiste', () => {
    cy.viewport(1280, 900);
    mountApp();
    cy.get('mat-toolbar').should('be.visible');
    cy.get('[data-testid="bottom-nav"]').should('not.exist');
  });

  it('zeigt im Querformat 800x360 die Handy-Darstellung', () => {
    cy.viewport(800, 360);
    mountApp();
    cy.get('[data-testid="bottom-nav"]').should('be.visible');
    cy.document().its('body').then(b => {
      expect(b.scrollWidth).to.be.at.most(b.clientWidth);
    });
  });

  it('blendet auf der Scoring-Route jede App-Navigation aus — am Handy', () => {
    cy.viewport(390, 736);
    mountApp('/matches/match-1/score');
    cy.get('[data-testid="page"]').should('exist');
    cy.get('[data-testid="bottom-nav"]').should('not.exist');
    cy.get('mat-toolbar').should('not.exist');
  });

  it('blendet auf der Scoring-Route jede App-Navigation aus — am Desktop', () => {
    cy.viewport(1280, 900);
    mountApp('/matches/match-1/score');
    cy.get('[data-testid="page"]').should('exist');
    cy.get('mat-toolbar').should('not.exist');
    cy.get('[data-testid="bottom-nav"]').should('not.exist');
  });

  it('bietet hinter Mehr Sprache, Export, Löschen und Abmelden an', () => {
    cy.viewport(390, 736);
    mountApp();
    cy.get('[data-testid="nav-more"]').click();
    cy.get('[data-cy="lang-menu-trigger"]').should('exist');
    cy.get('[data-cy="export-my-data"]').should('exist');
    cy.get('[data-cy="delete-my-data"]').should('exist');
    cy.get('[data-cy="logout"]').should('exist');
  });

  it('hält alle Tap-Ziele der Navigationsleiste bei mindestens 44x44 px', () => {
    cy.viewport(390, 736);
    mountApp();
    cy.get('[data-testid="bottom-nav"] a, [data-testid="bottom-nav"] button').each($el => {
      const r = $el[0].getBoundingClientRect();
      expect(r.width).to.be.at.least(44);
      expect(r.height).to.be.at.least(44);
    });
  });
});
