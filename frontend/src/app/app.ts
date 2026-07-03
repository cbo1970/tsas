import { Component, inject, signal, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AuthService } from './core/auth/auth.service';
import { ApiService } from './core/services/api.service';
import { LanguageService } from './core/i18n/language.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatButtonModule, MatIconModule, MatChipsModule,
    MatMenuModule, MatDividerModule, MatSnackBarModule, TranslatePipe,
  ],
  templateUrl: './app.html',
  styles: [`
    :host { display: flex; flex-direction: column; min-height: 100vh; }
    .app-title { margin-left: 12px; font-size: 20px; font-weight: 800; color: var(--brand-strong); }
    .spacer { flex: 1; }
    mat-toolbar {
      background: var(--surface-card);
      color: var(--text);
      border-bottom: 1px solid var(--border);
    }
    main { flex: 1; background: var(--surface-bg); min-height: calc(100vh - 64px); }
    .active-link { border-bottom: 2px solid var(--brand); border-radius: 0; }
    mat-toolbar mat-icon { font-size: 28px; }
    .user-name { font-size: 14px; margin-right: 4px; opacity: 0.9; }
    .admin-badge {
      --mdc-chip-elevated-container-color: var(--danger);
      --mdc-chip-label-text-color: #fff;
      --mdc-chip-with-icon-icon-color: #fff;
      margin-right: 8px;
      font-size: 12px;
      font-weight: 600;
      letter-spacing: 0.5px;
    }
    .danger-item { color: var(--danger); }
    .danger-item mat-icon { color: var(--danger); }
    .flag { display: inline-block; margin-right: 6px; font-size: 18px; line-height: 1; }
  `]
})
export class App implements OnInit {
  protected readonly authService = inject(AuthService);
  protected readonly language = inject(LanguageService);
  private readonly api = inject(ApiService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);

  protected userName = signal('');
  protected readonly languages: ReadonlyArray<{ code: 'de'|'en'|'it'|'fr', label: string, flag: string }> = [
    { code: 'de', label: 'Deutsch', flag: '🇩🇪' },
    { code: 'en', label: 'English', flag: '🇬🇧' },
    { code: 'it', label: 'Italiano', flag: '🇮🇹' },
    { code: 'fr', label: 'Français', flag: '🇫🇷' },
  ];

  protected currentFlag() {
    return this.languages.find(l => l.code === this.language.current())?.flag ?? '🇩🇪';
  }

  protected switchLanguage(code: 'de'|'en'|'it'|'fr') {
    this.language.setLanguage(code);
  }

  ngOnInit() {
    this.authService.initialize().then(() => {
      this.userName.set(this.authService.userName());
    });
  }

  protected logout() {
    this.authService.logout();
  }

  /** DSGVO Art. 20: lädt den vollständigen Daten-Export als JSON-Datei herunter (TEN-66). */
  protected exportMyData() {
    this.api.exportMyData().subscribe({
      next: (data) => {
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `tsas-export-${new Date().toISOString().slice(0, 10)}.json`;
        a.click();
        URL.revokeObjectURL(url);
        this.snackBar.open(this.translate.instant('app.toast.exportDone'), 'OK', { duration: 3000 });
      },
      error: () => this.snackBar.open(this.translate.instant('app.toast.exportFailed'), 'OK', { duration: 5000 }),
    });
  }

  /** DSGVO Art. 17: löscht alle eigenen Aggregate nach Bestätigung (TEN-66). */
  protected deleteMyData() {
    const confirmed = window.confirm(this.translate.instant('app.confirm.deleteData'));
    if (!confirmed) return;
    this.api.deleteMyData().subscribe({
      next: () => {
        this.snackBar.open(this.translate.instant('app.toast.deleteDone'), 'OK', { duration: 6000 });
        // Hard reload — UI signals (Cache, Liste) sind sonst noch mit gelöschten Daten gefüllt.
        setTimeout(() => window.location.href = '/', 1500);
      },
      error: () => this.snackBar.open(this.translate.instant('app.toast.deleteFailed'), 'OK', { duration: 5000 }),
    });
  }
}
