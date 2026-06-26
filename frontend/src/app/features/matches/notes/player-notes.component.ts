import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslatePipe } from '@ngx-translate/core';
import { ApiService } from '../../../core/services/api.service';

interface NoteSlot {
  playerId: string;
  name: string;
  roleKey: 'playerNotes.roleOwn' | 'playerNotes.roleOpponent';
  note: string;
  saving: boolean;
  saved: boolean;
}

/**
 * TEN-68: reusable panel showing one editable free-text note per player of a match.
 * Self-contained — give it the matchId; it resolves the two players and loads/saves notes.
 */
@Component({
  selector: 'app-player-notes',
  standalone: true,
  imports: [FormsModule, MatSnackBarModule, TranslatePipe],
  templateUrl: './player-notes.component.html',
  styles: [`
    .notes { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    @media (max-width: 640px) { .notes { grid-template-columns: 1fr; } }
    .slot { display: flex; flex-direction: column; gap: 4px; }
    .slot-label { font-size: 12px; font-weight: 600; opacity: .8; }
    .slot-role { font-size: 10px; opacity: .55; }
    textarea {
      width: 100%; box-sizing: border-box; min-height: 90px; resize: vertical;
      border-radius: 6px; border: 1px solid rgba(127,127,127,.4);
      padding: 8px; font: inherit; background: rgba(255,255,255,.04); color: inherit;
    }
    .saved-hint { font-size: 10px; color: #4ade80; min-height: 12px; }
  `],
})
export class PlayerNotesComponent implements OnInit {
  @Input({ required: true }) matchId!: string;

  private readonly api = inject(ApiService);
  private readonly snackBar = inject(MatSnackBar);

  slots = signal<NoteSlot[]>([]);

  ngOnInit(): void {
    this.api.getMatch(this.matchId).subscribe({
      next: (m) => {
        const base: NoteSlot[] = [
          { playerId: m.player1Id, name: 'Spieler 1', roleKey: 'playerNotes.roleOwn', note: '', saving: false, saved: false },
          { playerId: m.player2Id, name: 'Spieler 2', roleKey: 'playerNotes.roleOpponent', note: '', saving: false, saved: false },
        ];
        this.slots.set(base);
        this.api.getPlayer(m.player1Id).subscribe(p => this.patch(0, s => s.name = `${p.firstName} ${p.lastName}`));
        this.api.getPlayer(m.player2Id).subscribe(p => this.patch(1, s => s.name = `${p.firstName} ${p.lastName}`));
        this.api.getPlayerNotes(this.matchId).subscribe(notes => {
          for (const n of notes) {
            const idx = this.slots().findIndex(s => s.playerId === n.playerId);
            if (idx >= 0) this.patch(idx, s => s.note = n.note);
          }
        });
      },
      error: () => this.snackBar.open('Match nicht gefunden', 'OK', { duration: 3000 }),
    });
  }

  save(index: number): void {
    const slot = this.slots()[index];
    if (!slot) return;
    const wasBlank = !slot.note || !slot.note.trim();
    this.patch(index, s => { s.saving = true; s.saved = false; });
    this.api.savePlayerNote(this.matchId, slot.playerId, slot.note).subscribe({
      next: () => this.patch(index, s => {
        s.saving = false;
        s.saved = true;
        if (wasBlank) s.note = '';
      }),
      error: () => {
        this.patch(index, s => s.saving = false);
        this.snackBar.open('Fehler beim Speichern', 'OK', { duration: 3000 });
      },
    });
  }

  onModel(index: number, value: string): void {
    this.patch(index, s => { s.note = value; s.saved = false; });
  }

  private patch(index: number, mutate: (s: NoteSlot) => void): void {
    this.slots.update(list => list.map((s, i) => {
      if (i !== index) return s;
      const copy = { ...s };
      mutate(copy);
      return copy;
    }));
  }
}
