import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { TranslatePipe } from '@ngx-translate/core';
import { Player } from '../../core/models/player.model';

/** Handset presentation of the player list: one tappable card per player. */
@Component({
  selector: 'app-player-card-list',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatMenuModule, TranslatePipe],
  templateUrl: './player-card-list.component.html',
  styles: [`
    :host { display: block; }
    .player-card {
      display: flex; align-items: center; gap: 4px;
      min-height: 64px; padding: 8px 4px 8px 16px;
      border-bottom: 1px solid var(--brand-border);
      cursor: pointer;
    }
    .player-card:last-child { border-bottom: 0; }
    .player-card.inactive-card { opacity: 0.45; }
    .player-info { flex: 1; min-width: 0; }
    .player-name { font-weight: 600; color: var(--text); overflow-wrap: anywhere; }
    .player-meta { font-size: 14px; color: var(--text-muted); }
    @keyframes pulse { 0%, 100% { transform: scale(1); opacity: 1; } 50% { transform: scale(1.25); opacity: 0.7; } }
    .tennis-ball { font-size: 20px; line-height: 1; display: inline-block; animation: pulse 2s ease-in-out infinite; }
    .player-card button { flex-shrink: 0; width: 48px; height: 48px; }
  `],
})
export class PlayerCardListComponent {
  readonly players = input.required<Player[]>();

  readonly edit = output<Player>();
  readonly history = output<Player>();
  readonly compare = output<Player>();
  readonly remove = output<Player>();
  readonly deactivate = output<Player>();
  readonly openMatch = output<Player>();
}
