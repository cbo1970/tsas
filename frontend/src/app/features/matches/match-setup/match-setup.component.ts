import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormControl, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
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
    MatInputModule,
    MatAutocompleteModule,
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
              <input matInput [formControl]="player1Input"
                     [matAutocomplete]="auto1"
                     placeholder="Name eintippen…">
              <mat-autocomplete #auto1="matAutocomplete"
                                [displayWith]="displayPlayer.bind(this)"
                                (optionSelected)="onPlayer1Selected($event.option.value)">
                @for (p of filtered1(); track p.id) {
                  <mat-option [value]="p">{{ p.lastName }} {{ p.firstName }}</mat-option>
                }
              </mat-autocomplete>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Spieler 2</mat-label>
              <input matInput [formControl]="player2Input"
                     [matAutocomplete]="auto2"
                     placeholder="Name eintippen…">
              <mat-autocomplete #auto2="matAutocomplete"
                                [displayWith]="displayPlayer.bind(this)"
                                (optionSelected)="onPlayer2Selected($event.option.value)">
                @for (p of filtered2(); track p.id) {
                  <mat-option [value]="p">{{ p.lastName }} {{ p.firstName }}</mat-option>
                }
              </mat-autocomplete>
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

  player1Input = new FormControl<Player | string>('');
  player2Input = new FormControl<Player | string>('');

  filtered1 = computed(() => this.filterPlayers(this.player1Query()));
  filtered2 = computed(() => this.filterPlayers(this.player2Query()));

  private player1Query = signal('');
  private player2Query = signal('');

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

    this.player1Input.valueChanges.subscribe(v => {
      const q = typeof v === 'string' ? v : '';
      this.player1Query.set(q);
      if (typeof v !== 'object' || v === null) {
        this.form.patchValue({ player1Id: '' });
      }
    });

    this.player2Input.valueChanges.subscribe(v => {
      const q = typeof v === 'string' ? v : '';
      this.player2Query.set(q);
      if (typeof v !== 'object' || v === null) {
        this.form.patchValue({ player2Id: '' });
      }
    });
  }

  private filterPlayers(query: string): Player[] {
    const term = query.toLowerCase().trim();
    return this.availablePlayers().filter(p =>
      !term ||
      p.lastName.toLowerCase().includes(term) ||
      p.firstName.toLowerCase().includes(term)
    );
  }

  displayPlayer(player: Player | string | null): string {
    if (!player || typeof player === 'string') return '';
    return `${player.lastName} ${player.firstName}`;
  }

  onPlayer1Selected(player: Player) {
    this.form.patchValue({ player1Id: player.id });
    this.player1Query.set('');
  }

  onPlayer2Selected(player: Player) {
    this.form.patchValue({ player2Id: player.id });
    this.player2Query.set('');
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
