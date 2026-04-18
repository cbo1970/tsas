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

import { ApiService } from '../../core/services/api.service';
import { Player, CreatePlayerRequest } from '../../core/models/player.model';
import { PlayerDialogComponent } from './player-dialog.component';

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
    MatTooltipModule
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>Spieler</h1>
        <mat-form-field appearance="outline" class="search-field">
          <mat-icon matPrefix>search</mat-icon>
          <input matInput placeholder="Suchen..." [ngModel]="searchTerm()" (ngModelChange)="searchTerm.set($event)" />
          @if (searchTerm()) {
            <button matSuffix mat-icon-button (click)="searchTerm.set('')">
              <mat-icon>close</mat-icon>
            </button>
          }
        </mat-form-field>
        <button mat-raised-button color="primary" (click)="openCreateDialog()">
          <mat-icon>add</mat-icon>
          Neuer Spieler
        </button>
      </div>

      <mat-card>
        <mat-card-content>
          <table mat-table [dataSource]="filteredPlayers()" matSort (matSortChange)="onSort($event)" class="full-width">
            <ng-container matColumnDef="firstName">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Vorname</th>
              <td mat-cell *matCellDef="let player">{{ player.firstName }}</td>
            </ng-container>

            <ng-container matColumnDef="lastName">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Name</th>
              <td mat-cell *matCellDef="let player">{{ player.lastName }}</td>
            </ng-container>

            <ng-container matColumnDef="ranking">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Ranking</th>
              <td mat-cell *matCellDef="let player">{{ player.ranking ?? '–' }}</td>
            </ng-container>

            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef class="status-header">Laufende<br>Matches</th>
              <td mat-cell *matCellDef="let player" class="status-cell">
                @if (player.activeMatchId) {
                  <button mat-icon-button
                          matTooltip="Laufendes Match anzeigen"
                          (click)="$event.stopPropagation(); goToMatch(player.activeMatchId)">
                    <span class="tennis-ball">🎾</span>
                  </button>
                }
              </td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef></th>
              <td mat-cell *matCellDef="let player" class="actions-cell">
                @if (player.deletable !== false) {
                  <button mat-icon-button color="warn" title="Spieler löschen"
                          (click)="deletePlayer(player); $event.stopPropagation()">
                    <mat-icon>delete</mat-icon>
                  </button>
                } @else {
                  <button mat-icon-button title="Spieler inaktivieren"
                          [class.inactive-btn]="!player.active"
                          (click)="deactivatePlayer(player); $event.stopPropagation()"
                          [disabled]="player.active === false">
                    <mat-icon>person_off</mat-icon>
                  </button>
                }
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"
                [class.inactive-row]="row.active === false"
                (click)="openEditDialog(row)"></tr>
          </table>

          @if (filteredPlayers().length === 0) {
            <div class="empty-state">
              <p>{{ searchTerm() ? 'Keine Spieler gefunden.' : 'Noch keine Spieler angelegt.' }}</p>
            </div>
          }
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-container { padding: 24px; max-width: 900px; margin: 0 auto; }
    .page-header { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
    .page-header h1 { margin: 0; font-size: 28px; }
    .search-field { flex: 1; margin-bottom: -1.25em; }
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
  `]
})
export class PlayersComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);

  players = signal<Player[]>([]);
  searchTerm = signal('');
  sort = signal<Sort>({ active: 'lastName', direction: 'asc' });
  displayedColumns = ['firstName', 'lastName', 'ranking', 'status', 'actions'];

  filteredPlayers = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    const { active, direction } = this.sort();

    let result = this.players().filter(p =>
      !term ||
      `${p.firstName} ${p.lastName}`.toLowerCase().includes(term) ||
      (p.ranking ?? '').toString().toLowerCase().includes(term)
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
