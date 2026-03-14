import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';

import { ApiService } from '../../core/services/api.service';
import { Player, CreatePlayerRequest } from '../../core/models/player.model';
import { PlayerDialogComponent } from './player-dialog.component';

@Component({
  selector: 'app-players',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatCardModule,
    MatSnackBarModule
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>Spieler</h1>
        <button mat-raised-button color="primary" (click)="openCreateDialog()">
          <mat-icon>add</mat-icon>
          Neuer Spieler
        </button>
      </div>

      <mat-card>
        <mat-card-content>
          <table mat-table [dataSource]="players()" class="full-width">
            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef>Name</th>
              <td mat-cell *matCellDef="let player">
                {{ player.firstName }} {{ player.lastName }}
              </td>
            </ng-container>

            <ng-container matColumnDef="handedness">
              <th mat-header-cell *matHeaderCellDef>Spielhand</th>
              <td mat-cell *matCellDef="let player">
                {{ player.handedness === 'LEFT' ? 'Links' : 'Rechts' }}
              </td>
            </ng-container>

            <ng-container matColumnDef="backhandType">
              <th mat-header-cell *matHeaderCellDef>Backhand</th>
              <td mat-cell *matCellDef="let player">
                {{ player.backhandType === 'ONE_HANDED' ? 'Einhändig' : 'Zweihändig' }}
              </td>
            </ng-container>

            <ng-container matColumnDef="ranking">
              <th mat-header-cell *matHeaderCellDef>Ranking</th>
              <td mat-cell *matCellDef="let player">
                {{ player.ranking ?? '–' }}
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          </table>

          @if (players().length === 0) {
            <div class="empty-state">
              <p>Noch keine Spieler angelegt.</p>
            </div>
          }
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-container { padding: 24px; max-width: 900px; margin: 0 auto; }
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
    .page-header h1 { margin: 0; font-size: 28px; }
    .full-width { width: 100%; }
    .empty-state { text-align: center; padding: 48px; color: #666; }
    table { border-radius: 8px; overflow: hidden; }
  `]
})
export class PlayersComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  players = signal<Player[]>([]);
  displayedColumns = ['name', 'handedness', 'backhandType', 'ranking'];

  ngOnInit() {
    this.loadPlayers();
  }

  loadPlayers() {
    this.api.getPlayers().subscribe({
      next: (players) => this.players.set(players),
      error: () => this.snackBar.open('Fehler beim Laden der Spieler', 'OK', { duration: 3000 })
    });
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
}
