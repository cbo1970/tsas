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
  template: `
    <h2 mat-dialog-title>Score korrigieren</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="edit-form">
        <div class="row-pair">
          <mat-form-field appearance="outline">
            <mat-label>Punkte Spieler 1</mat-label>
            <input matInput type="number" formControlName="pointsPlayer1" min="0" max="3" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Punkte Spieler 2</mat-label>
            <input matInput type="number" formControlName="pointsPlayer2" min="0" max="3" />
          </mat-form-field>
        </div>

        <div class="row-pair">
          <mat-form-field appearance="outline">
            <mat-label>Games Spieler 1</mat-label>
            <input matInput type="number" formControlName="gamesPlayer1" min="0" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Games Spieler 2</mat-label>
            <input matInput type="number" formControlName="gamesPlayer2" min="0" />
          </mat-form-field>
        </div>

        <div class="row-pair">
          <mat-form-field appearance="outline">
            <mat-label>Sätze Spieler 1</mat-label>
            <input matInput type="number" formControlName="setsPlayer1" min="0" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Sätze Spieler 2</mat-label>
            <input matInput type="number" formControlName="setsPlayer2" min="0" />
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Aktueller Satz</mat-label>
          <input matInput type="number" formControlName="currentSet" min="1" />
        </mat-form-field>

        <div class="checkbox-row">
          <mat-checkbox formControlName="isDeuce">Deuce</mat-checkbox>
          <mat-checkbox formControlName="isDone">Match beendet</mat-checkbox>
        </div>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Advantage (leer = kein Advantage)</mat-label>
          <mat-select formControlName="isAdvantagePlayer1">
            <mat-option [value]="null">Kein Advantage</mat-option>
            <mat-option [value]="true">Spieler 1</mat-option>
            <mat-option [value]="false">Spieler 2</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Sieger (falls Match beendet)</mat-label>
          <mat-select formControlName="winner">
            <mat-option [value]="null">Kein Sieger</mat-option>
            <mat-option value="PLAYER1">Spieler 1</mat-option>
            <mat-option value="PLAYER2">Spieler 2</mat-option>
          </mat-select>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Abbrechen</button>
      <button mat-raised-button color="primary" (click)="save()">Speichern</button>
    </mat-dialog-actions>
  `,
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
