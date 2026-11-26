import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { EndMatchDialogComponent } from './end-match-dialog.component';

describe('EndMatchDialogComponent', () => {
  it('hat bei 360px Breite nur Bedienelemente von mindestens 44px Höhe', () => {
    cy.viewport(360, 640);
    cy.mount(EndMatchDialogComponent, {
      providers: [
        provideAnimationsAsync(),
        { provide: MatDialogRef, useValue: { close: () => {} } },
        { provide: MAT_DIALOG_DATA, useValue: { player1Name: 'Roger Federer', player2Name: 'Rafael Nadal' } },
      ],
    });
    // offsetHeight = layout pixels (getBoundingClientRect is scaled by Cypress' preview)
    cy.get('mat-radio-button, button').should('have.length', 4).each(($el) => {
      expect(($el[0] as HTMLElement).offsetHeight, $el[0].tagName + ' ' + $el[0].textContent?.trim()).to.be.gte(44);
    });
  });
});
