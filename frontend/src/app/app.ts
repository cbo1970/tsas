import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatToolbarModule, MatButtonModule, MatIconModule],
  template: `
    <mat-toolbar color="primary">
      <mat-icon>sports_tennis</mat-icon>
      <span class="app-title">TSaS – Tennis Score &amp; Statistic</span>
      <span class="spacer"></span>
      <a mat-button routerLink="/players" routerLinkActive="active-link">
        <mat-icon>people</mat-icon> Spieler
      </a>
      <a mat-button routerLink="/matches/new" routerLinkActive="active-link">
        <mat-icon>add_circle</mat-icon> Neues Match
      </a>
    </mat-toolbar>
    <main>
      <router-outlet />
    </main>
  `,
  styles: [`
    :host { display: flex; flex-direction: column; min-height: 100vh; }
    .app-title { margin-left: 12px; font-size: 20px; font-weight: 500; }
    .spacer { flex: 1; }
    main { flex: 1; background: #f5f5f5; min-height: calc(100vh - 64px); }
    .active-link { background: rgba(255,255,255,0.15); border-radius: 4px; }
    mat-toolbar mat-icon { font-size: 28px; }
  `]
})
export class App {}
