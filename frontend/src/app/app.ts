import { Component, inject, signal, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from './core/auth/auth.service';
import { ApiService } from './core/services/api.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatButtonModule, MatIconModule, MatChipsModule,
    MatMenuModule, MatDividerModule, MatSnackBarModule,
  ],
  templateUrl: './app.html',
  styles: [`
    :host { display: flex; flex-direction: column; min-height: 100vh; }
    .app-title { margin-left: 12px; font-size: 20px; font-weight: 500; }
    .spacer { flex: 1; }
    main { flex: 1; background: #f5f5f5; min-height: calc(100vh - 64px); }
    .active-link { background: rgba(255,255,255,0.15); border-radius: 4px; }
    mat-toolbar mat-icon { font-size: 28px; }
    .user-name { font-size: 14px; margin-right: 4px; opacity: 0.9; }
    .admin-badge {
      --mdc-chip-elevated-container-color: #c62828;
      --mdc-chip-label-text-color: #fff;
      --mdc-chip-with-icon-icon-color: #fff;
      margin-right: 8px;
      font-size: 12px;
      font-weight: 600;
      letter-spacing: 0.5px;
    }
    .danger-item { color: #c62828; }
    .danger-item mat-icon { color: #c62828; }
  `]
})
export class App implements OnInit {
  protected readonly authService = inject(AuthService);
  private readonly api = inject(ApiService);
  private readonly snackBar = inject(MatSnackBar);

  protected userName = signal('');

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
        this.snackBar.open('Datenexport heruntergeladen', 'OK', { duration: 3000 });
      },
      error: () => this.snackBar.open('Datenexport fehlgeschlagen', 'OK', { duration: 5000 }),
    });
  }

  /** DSGVO Art. 17: löscht alle eigenen Aggregate nach Bestätigung (TEN-66). */
  protected deleteMyData() {
    const confirmed = window.confirm(
      'Achtung: Alle deine Daten (Spieler, Matches, Punkte, KI-Analysen) werden unwiderruflich gelöscht.\n\n' +
      'Dein Keycloak-Konto bleibt bestehen — bei erneutem Login startest du mit einer leeren Datenbasis.\n\n' +
      'Fortfahren?'
    );
    if (!confirmed) return;
    this.api.deleteMyData().subscribe({
      next: (summary) => {
        this.snackBar.open(
          `Gelöscht: ${summary.players} Spieler, ${summary.matches} Matches, ${summary.points} Punkte`,
          'OK', { duration: 6000 });
        // Hard reload — UI signals (Cache, Liste) sind sonst noch mit gelöschten Daten gefüllt.
        setTimeout(() => window.location.href = '/', 1500);
      },
      error: () => this.snackBar.open('Löschung fehlgeschlagen', 'OK', { duration: 5000 }),
    });
  }
}
