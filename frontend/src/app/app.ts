import { Component, inject, signal, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from './core/auth/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatToolbarModule, MatButtonModule, MatIconModule],
  templateUrl: './app.html',
  styles: [`
    :host { display: flex; flex-direction: column; min-height: 100vh; }
    .app-title { margin-left: 12px; font-size: 20px; font-weight: 500; }
    .spacer { flex: 1; }
    main { flex: 1; background: #f5f5f5; min-height: calc(100vh - 64px); }
    .active-link { background: rgba(255,255,255,0.15); border-radius: 4px; }
    mat-toolbar mat-icon { font-size: 28px; }
    .user-name { font-size: 14px; margin-right: 4px; opacity: 0.9; }
  `]
})
export class App implements OnInit {
  private readonly authService = inject(AuthService);

  protected userName = signal('');

  ngOnInit() {
    this.authService.initialize().then(() => {
      this.userName.set(this.authService.userName());
    });
  }

  protected logout() {
    this.authService.logout();
  }
}
