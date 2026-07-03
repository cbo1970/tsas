import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSelectModule } from '@angular/material/select';
import { MatchScore, SetScoreRequest } from '../../../core/models/match.model';

@Component({
  selector: 'app-score-edit-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCheckboxModule,
    MatSelectModule
  ],
  templateUrl: './score-edit-dialog.component.html',
  styles: [`
    .edit-form { display: flex; flex-direction: column; gap: 8px; min-width: 400px; padding-top: 8px; }
    .row-pair { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
    .full-width { width: 100%; }
    .checkbox-row { display: flex; gap: 24px; padding: 8px 0; }
  `]
})
export class ScoreEditDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<ScoreEditDialogComponent>);
  readonly data: { score: MatchScore } = inject(MAT_DIALOG_DATA);

  form = this.fb.group({
    pointsPlayer1: [this.data.score.pointsPlayer1],
    pointsPlayer2: [this.data.score.pointsPlayer2],
    gamesPlayer1: [this.data.score.gamesPlayer1],
    gamesPlayer2: [this.data.score.gamesPlayer2],
    setsPlayer1: [this.data.score.setsPlayer1],
    setsPlayer2: [this.data.score.setsPlayer2],
    isDeuce: [this.data.score.isDeuce],
    isAdvantagePlayer1: [this.data.score.isAdvantagePlayer1],
    currentSet: [this.data.score.currentSet],
    isDone: [this.data.score.isDone],
    winner: [this.data.score.winner ?? null]
  });

  cancel() {
    this.dialogRef.close();
  }

  save() {
    const v = this.form.value;
    const request: SetScoreRequest = {
      pointsPlayer1: v.pointsPlayer1 ?? 0,
      pointsPlayer2: v.pointsPlayer2 ?? 0,
      gamesPlayer1: v.gamesPlayer1 ?? 0,
      gamesPlayer2: v.gamesPlayer2 ?? 0,
      setsPlayer1: v.setsPlayer1 ?? 0,
      setsPlayer2: v.setsPlayer2 ?? 0,
      isDeuce: v.isDeuce ?? false,
      isAdvantagePlayer1: v.isAdvantagePlayer1 ?? null,
      currentSet: v.currentSet ?? 1,
      isDone: v.isDone ?? false,
      winner: v.winner ?? null
    };
    this.dialogRef.close(request);
  }
}
