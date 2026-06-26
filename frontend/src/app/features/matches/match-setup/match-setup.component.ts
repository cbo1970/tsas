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
import { TranslatePipe } from '@ngx-translate/core';

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
    MatSnackBarModule,
    TranslatePipe
  ],
  templateUrl: './match-setup.component.html',
  styles: [`
    .page-container { padding: 24px; max-width: 600px; margin: 0 auto; }
    .page-header { margin-bottom: 24px; }
    .page-header h1 { margin: 0; font-size: 28px; font-weight: 700; color: var(--text); }
    .setup-form { display: flex; flex-direction: column; gap: 16px; padding: 8px 0; }
    .full-width { width: 100%; }
    .form-row { display: flex; align-items: center; gap: 16px; }
    .form-label { font-size: 14px; color: var(--text); font-weight: 700; min-width: 120px; }
    .radio-group { display: flex; gap: 16px; }
    .toggle-row { justify-content: space-between; padding: 8px 0; border-bottom: 1px solid var(--text); }
    .toggle-row span { font-weight: 700; }
    .actions { display: flex; justify-content: flex-end; padding-top: 16px; }
    mat-card { border: 1px solid var(--text); }
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
