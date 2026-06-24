import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';

import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { TranslatePipe } from '@ngx-translate/core';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/auth/auth.service';
import { Player, CreatePlayerRequest } from '../../core/models/player.model';
import { PlayerDialogComponent } from './player-dialog.component';

export type Scope = 'mine' | 'all';

@Component({
  selector: 'app-players',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatCardModule,
    MatSnackBarModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule,
    MatButtonToggleModule,
    TranslatePipe
  ],
  templateUrl: './players.component.html',
  styles: [`
    .page-container { padding: 24px; max-width: 900px; margin: 0 auto; }
    .page-header { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
    .page-header h1 { margin: 0; font-size: 28px; }
    .search-field { flex: 1; min-width: 150px; margin-bottom: -1.25em; }
    .page-header button { flex-shrink: 0; }
    .full-width { width: 100%; }
    .empty-state { text-align: center; padding: 48px; color: #666; }
    table { border-radius: 8px; overflow: hidden; }
    .status-header { width: 48px; white-space: normal; text-align: center; line-height: 1.2; }
    @keyframes pulse { 0%, 100% { transform: scale(1); opacity: 1; } 50% { transform: scale(1.25); opacity: 0.7; } }
    .tennis-ball { font-size: 20px; line-height: 1; display: inline-block; animation: pulse 2s ease-in-out infinite; }
    .status-cell { width: 48px; }
    .actions-cell { width: 56px; text-align: right; }
    .inactive-row { opacity: 0.45; }
    .inactive-btn { opacity: 0.3; }
    tr.mat-mdc-row { cursor: pointer; }
    .admin-scope {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 16px;
      margin-bottom: 16px;
      background: #fff3e0;
      border: 1px solid #ffcc80;
      border-radius: 8px;
      font-size: 14px;
      color: #5d4037;
    }
    .admin-scope mat-icon { color: #c62828; }
    .admin-scope mat-button-toggle-group { margin-left: auto; }
  `]
})
export class PlayersComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  protected readonly authService = inject(AuthService);

  players = signal<Player[]>([]);
  searchTerm = signal('');
  sort = signal<Sort>({ active: 'lastName', direction: 'asc' });
  /** Admin-only filter: 'mine' shows only the current user's players, 'all' shows everyone. Coaches always see 'mine' (server-enforced). */
  scope = signal<Scope>('mine');
  displayedColumns = ['firstName', 'lastName', 'ranking', 'status', 'actions'];

  filteredPlayers = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    const { active, direction } = this.sort();
    const scope = this.scope();
    const ownId = this.authService.userId();

    let result = this.players().filter(p =>
      (scope === 'all' || !this.authService.isAdmin() || p.ownerId === ownId) &&
      (!term ||
        `${p.firstName} ${p.lastName}`.toLowerCase().includes(term) ||
        (p.ranking ?? '').toString().toLowerCase().includes(term))
    );

    if (direction) {
      result = [...result].sort((a, b) => {
        const valA = (a[active as keyof Player] ?? '') as string;
        const valB = (b[active as keyof Player] ?? '') as string;
        return direction === 'asc'
          ? valA.toString().localeCompare(valB.toString())
          : valB.toString().localeCompare(valA.toString());
      });
    }
    return result;
  });

  onSort(sort: Sort) {
    this.sort.set(sort);
  }

  ngOnInit() {
    this.loadPlayers();
  }

  loadPlayers() {
    this.api.getPlayers().subscribe({
      next: (players) => this.players.set(players),
      error: () => this.snackBar.open('Fehler beim Laden der Spieler', 'OK', { duration: 3000 })
    });
  }

  goToMatch(matchId: string) {
    this.router.navigate(['/matches', matchId, 'score']);
  }

  goToHeadToHead() {
    this.router.navigate(['/statistics/head-to-head']);
  }

  comparePlayer(player: Player) {
    this.router.navigate(['/statistics/head-to-head'], { queryParams: { player1: player.id } });
  }

  openCreateDialog() {
    const ref = this.dialog.open(PlayerDialogComponent, { width: '500px' });
    ref.afterClosed().subscribe((result: CreatePlayerRequest | undefined) => {
      if (result) {
        this.api.createPlayer(result).subscribe({
          next: () => {
            this.loadPlayers();
            this.snackBar.open('Spieler angelegt', 'OK', { duration: 3000 });
          },
          error: () => this.snackBar.open('Fehler beim Anlegen', 'OK', { duration: 3000 })
        });
      }
    });
  }

  openEditDialog(player: Player) {
    const ref = this.dialog.open(PlayerDialogComponent, { width: '500px', data: player });
    ref.afterClosed().subscribe((result: CreatePlayerRequest | undefined) => {
      if (result) {
        this.api.updatePlayer(player.id, result).subscribe({
          next: () => {
            this.loadPlayers();
            this.snackBar.open('Spieler gespeichert', 'OK', { duration: 3000 });
          },
          error: () => this.snackBar.open('Fehler beim Speichern', 'OK', { duration: 3000 })
        });
      }
    });
  }

  deletePlayer(player: Player) {
    this.api.deletePlayer(player.id).subscribe({
      next: () => {
        this.loadPlayers();
        this.snackBar.open(`${player.firstName} ${player.lastName} gelöscht`, 'OK', { duration: 3000 });
      },
      error: () => this.snackBar.open('Fehler beim Löschen', 'OK', { duration: 3000 })
    });
  }

  deactivatePlayer(player: Player) {
    this.api.deactivatePlayer(player.id).subscribe({
      next: () => {
        this.loadPlayers();
        this.snackBar.open(`${player.firstName} ${player.lastName} inaktiviert`, 'OK', { duration: 3000 });
      },
      error: () => this.snackBar.open('Fehler beim Inaktivieren', 'OK', { duration: 3000 })
    });
  }
}
