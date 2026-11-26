import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogConfig, MatDialogModule } from '@angular/material/dialog';
import { TranslatePipe } from '@ngx-translate/core';

/** Panel class that makes the dialog surface a borderless full-screen sheet (styles in styles.scss). */
export const FULLSCREEN_DIALOG_CLASS = 'tsas-dialog-fullscreen';

/**
 * Dialog size for the current layout: full width and height on a handset (so the on-screen
 * keyboard cannot hide the fields), 500px wide on desktop. Pass `LayoutService.isHandset()`.
 */
export function responsiveDialogConfig(isHandset: boolean): MatDialogConfig {
  return isHandset
    ? { width: '100vw', maxWidth: '100vw', height: '100dvh', maxHeight: '100dvh', panelClass: FULLSCREEN_DIALOG_CLASS }
    : { width: '500px', maxWidth: '100vw' };
}

/** Handset dialog header: cancel on the left, title, save on the right. Stays put while the content scrolls. */
@Component({
  selector: 'app-dialog-header',
  standalone: true,
  imports: [MatButtonModule, MatDialogModule, TranslatePipe],
  template: `
    <header class="dialog-header">
      <button mat-button type="button" data-testid="dialog-cancel" (click)="cancelled.emit()">
        {{ 'common.cancel' | translate }}
      </button>
      <!-- mat-dialog-title loest den umschliessenden Dialog per DI auf und setzt
           dort aria-labelledby: ohne das bleibt role="dialog" am Handy namenlos. -->
      <h2 mat-dialog-title class="dialog-header-title">{{ title() }}</h2>
      <button mat-flat-button type="button" data-testid="dialog-save" [disabled]="saveDisabled()" (click)="saved.emit()">
        {{ 'common.save' | translate }}
      </button>
    </header>
  `,
  styles: [`
    .dialog-header {
      display: flex; align-items: center; justify-content: space-between; gap: 8px;
      flex: none; padding: 8px 12px; border-bottom: 1px solid var(--text-muted, #ccc);
    }
    /* mat-dialog-title schleppt die Headline-Typografie des Dialog-Containers
       mit: padding 6px 24px 13px, ein ::before von 40px Hoehe und 32px
       Zeilenhoehe. Hier sitzt der Titel aber in einer eigenen flachen
       Kopfzeile — ungebremst waere sie bei 360px ueber 100px hoch und dem
       Titel fehlten 48px Breite. */
    .dialog-header-title {
      margin: 0; padding: 0; line-height: 1.2;
      font-size: 18px; font-weight: 600; text-align: center; flex: 1;
    }
    .dialog-header-title::before { display: none; }
    .dialog-header button {
      min-width: 44px;
      --mat-button-text-container-height: 44px;
      --mat-button-filled-container-height: 44px;
    }
  `]
})
export class DialogHeaderComponent {
  readonly title = input.required<string>();
  readonly saveDisabled = input(false);
  readonly cancelled = output<void>();
  readonly saved = output<void>();
}
