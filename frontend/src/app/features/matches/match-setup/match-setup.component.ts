import { Component, OnInit, WritableSignal, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormControl, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
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
import { CreatePlayerRequest, Player } from '../../../core/models/player.model';
import { LayoutService } from '../../../core/layout/layout.service';
import { responsiveDialogConfig } from '../../../core/layout/responsive-dialog';
import { PlayerDialogComponent } from '../../players/player-dialog.component';

/** Matches a player against a lower-cased, trimmed search term. */
function matchesName(player: Player, term: string): boolean {
  return player.lastName.toLowerCase().includes(term)
      || player.firstName.toLowerCase().includes(term);
}

/** Splits a typed name at the first space: first word is the first name, the rest the last name. */
export function splitName(input: string): { firstName: string; lastName: string } {
  const trimmed = input.trim();
  const i = trimmed.indexOf(' ');
  return i < 0
    ? { firstName: trimmed, lastName: '' }
    : { firstName: trimmed.slice(0, i), lastName: trimmed.slice(i + 1).trim() };
}

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
    @media (max-width: 700px) {
      .page-container { padding: 16px; }
      .page-header { margin-bottom: 16px; }
      mat-card { border: none; box-shadow: none; background: transparent; }
      :host ::ng-deep mat-card-content { padding: 0; }
      .page-header h1 { font-size: 22px; }
      .form-row:not(.toggle-row) { flex-direction: column; align-items: stretch; gap: 4px; }
      .radio-group { gap: 8px; }
      :host ::ng-deep .radio-group .mdc-radio { width: 44px; height: 44px; padding: 12px; box-sizing: border-box; }
      .toggle-row { min-height: 44px; }
      .actions { display: block; }
      .start-button { width: 100%; min-height: 52px; }
    }
  `]
})
export class MatchSetupComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly layout = inject(LayoutService);

  players = signal<Player[]>([]);
  availablePlayers = computed(() => this.players().filter(p => !p.activeMatchId));

  /**
   * Die Antwort auf getPlayers ist da (auch wenn sie leer war). Solange sie
   * aussteht oder fehlgeschlagen ist, weiss die Maske nichts ueber vorhandene
   * Spieler und darf das Anlegen nicht anbieten.
   */
  private readonly playersLoaded = signal(false);

  player1Input = new FormControl<Player | string>('');
  player2Input = new FormControl<Player | string>('');

  filtered1 = computed(() => this.filterPlayers(this.player1Query()));
  filtered2 = computed(() => this.filterPlayers(this.player2Query()));

  /**
   * Das Anlegen haengt an der *vollen* Spielerliste, nicht an der auswaehlbaren:
   * ein Spieler mit laufendem Match faellt zwar aus der Auswahl, existiert aber.
   * Wuerde die Maske ihn zum Anlegen anbieten, entstuende ein zweiter Datensatz
   * und Head-to-Head wie Statistiken spalteten sich still auf zwei Ids auf.
   */
  showCreatePlayer1 = computed(() => this.canCreatePlayer(this.player1Query()));
  showCreatePlayer2 = computed(() => this.canCreatePlayer(this.player2Query()));

  protected readonly player1Query = signal('');
  protected readonly player2Query = signal('');

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
      next: (players) => { this.players.set(players); this.playersLoaded.set(true); },
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
    return this.availablePlayers().filter(p => !term || matchesName(p, term));
  }

  private canCreatePlayer(query: string): boolean {
    const term = query.toLowerCase().trim();
    if (!term || !this.playersLoaded()) return false;
    return !this.players().some(p => matchesName(p, term));
  }

  displayPlayer(player: Player | string | null): string {
    if (!player) return '';
    // A string is the typed text (the "create player" option writes it back): keep it visible.
    if (typeof player === 'string') return player;
    return `${player.lastName} ${player.firstName}`;
  }

  onPlayer1Selected(selected: Player | string) {
    this.onSelected(selected, this.player1Input, 'player1Id', this.player1Query);
  }

  onPlayer2Selected(selected: Player | string) {
    this.onSelected(selected, this.player2Input, 'player2Id', this.player2Query);
  }

  /** A string selection is the "create player" option, carrying the typed text. */
  private onSelected(selected: Player | string, input: FormControl<Player | string | null>,
                     idField: 'player1Id' | 'player2Id', query: WritableSignal<string>) {
    if (typeof selected === 'string') {
      this.createPlayer(selected, input, idField);
      return;
    }
    this.form.patchValue({ [idField]: selected.id });
    query.set('');
  }

  private createPlayer(typed: string, input: FormControl<Player | string | null>,
                       idField: 'player1Id' | 'player2Id') {
    const ref = this.dialog.open(PlayerDialogComponent, {
      ...responsiveDialogConfig(this.layout.isHandset()),
      data: { prefill: splitName(typed) }
    });
    ref.afterClosed().subscribe((request: CreatePlayerRequest | undefined) => {
      if (!request) return;
      this.api.createPlayer(request).subscribe({
        next: (player) => {
          this.players.update(list => [...list, player]);
          input.setValue(player);
          this.form.patchValue({ [idField]: player.id });
        },
        error: () => this.snackBar.open('Fehler beim Anlegen des Spielers', 'OK', { duration: 3000 })
      });
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
