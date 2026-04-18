import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatRadioModule } from '@angular/material/radio';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';

import { ApiService } from '../../../core/services/api.service';
import { Player } from '../../../core/models/player.model';

@Component({
  selector: 'app-match-setup',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatSlideToggleModule,
    MatRadioModule,
    MatSnackBarModule
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>Neues Match</h1>
      </div>

      <mat-card>
        <mat-card-content>
          <form [formGroup]="form" class="setup-form">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Spieler 1</mat-label>
              <mat-select formControlName="player1Id">
                @for (p of availablePlayers(); track p.id) {
                  <mat-option [value]="p.id">{{ p.firstName }} {{ p.lastName }}</mat-option>
                }
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Spieler 2</mat-label>
              <mat-select formControlName="player2Id">
                @for (p of availablePlayers(); track p.id) {
                  <mat-option [value]="p.id">{{ p.firstName }} {{ p.lastName }}</mat-option>
                }
              </mat-select>
            </mat-form-field>

            <div class="form-row">
              <label class="form-label">Gewinnsätze</label>
              <mat-radio-group formControlName="setsToWin" class="radio-group">
                <mat-radio-button [value]="2">2 Sätze</mat-radio-button>
                <mat-radio-button [value]="3">3 Sätze</mat-radio-button>
              </mat-radio-group>
            </div>

            <div class="form-row toggle-row">
              <span>Match Tiebreak</span>
              <mat-slide-toggle formControlName="matchTiebreak"></mat-slide-toggle>
            </div>

            <div class="form-row toggle-row">
              <span>Short Set</span>
              <mat-slide-toggle formControlName="shortSet"></mat-slide-toggle>
            </div>

            <div class="actions">
              <button mat-raised-button color="primary" (click)="startMatch()"
                      [disabled]="form.invalid || loading()">
                Match starten
              </button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-container { padding: 24px; max-width: 600px; margin: 0 auto; }
    .page-header { margin-bottom: 24px; }
    .page-header h1 { margin: 0; font-size: 28px; }
    .setup-form { display: flex; flex-direction: column; gap: 16px; padding: 8px 0; }
    .full-width { width: 100%; }
    .form-row { display: flex; align-items: center; gap: 16px; }
    .form-label { font-size: 14px; color: rgba(0,0,0,.6); min-width: 120px; }
    .radio-group { display: flex; gap: 16px; }
    .toggle-row { justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #eee; }
    .actions { display: flex; justify-content: flex-end; padding-top: 16px; }
  `]
})
export class MatchSetupComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  players = signal<Player[]>([]);
  availablePlayers = computed(() => this.players().filter(p => !p.activeMatchId));
  loading = signal(false);

  form = this.fb.group({
    player1Id: ['', Validators.required],
    player2Id: ['', Validators.required],
    setsToWin: [2, Validators.required],
    matchTiebreak: [false],
    shortSet: [false]
  });

  ngOnInit() {
    this.api.getPlayers().subscribe({
      next: (players) => this.players.set(players),
      error: () => this.snackBar.open('Fehler beim Laden der Spieler', 'OK', { duration: 3000 })
    });
  }

  startMatch() {
    if (this.form.invalid) return;
    const v = this.form.value;
    this.loading.set(true);

    this.api.createMatch({
      player1Id: v.player1Id!,
      player2Id: v.player2Id!,
      setsToWin: v.setsToWin!,
      matchTiebreak: v.matchTiebreak!,
      shortSet: v.shortSet!
    }).subscribe({
      next: (match) => {
        this.router.navigate(['/matches', match.id, 'score']);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Fehler beim Erstellen des Matches', 'OK', { duration: 3000 });
      }
    });
  }
}
