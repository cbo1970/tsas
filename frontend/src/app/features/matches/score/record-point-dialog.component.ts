import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { PointType, StrokeType, Direction, RecordPointRequest } from '../../../core/models/point.model';

export interface RecordPointDialogData {
  winner: 1 | 2;
  winnerName: string;
}

@Component({
  selector: 'app-record-point-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatFormFieldModule,
    MatInputModule
  ],
  template: `
    <h2 mat-dialog-title>Punkt für {{ data.winnerName }}</h2>

    <mat-dialog-content>

      <div class="section">
        <div class="section-label">Punkttyp *</div>
        <mat-button-toggle-group [(ngModel)]="pointType" class="toggle-group wrap">
          <mat-button-toggle value="WINNER">Winner</mat-button-toggle>
          <mat-button-toggle value="UNFORCED_ERROR">Eigenf.</mat-button-toggle>
          <mat-button-toggle value="FORCED_ERROR">Erzw. F.</mat-button-toggle>
          <mat-button-toggle value="ACE">Ass</mat-button-toggle>
          <mat-button-toggle value="DOUBLE_FAULT">DF</mat-button-toggle>
          <mat-button-toggle value="NET">Netz</mat-button-toggle>
          <mat-button-toggle value="OUT_LONG">Aus lang</mat-button-toggle>
          <mat-button-toggle value="OUT_SIDE">Aus Seite</mat-button-toggle>
        </mat-button-toggle-group>
      </div>

      <div class="section">
        <div class="section-label">Schlagart (optional)</div>
        <mat-button-toggle-group [(ngModel)]="strokeType" class="toggle-group">
          <mat-button-toggle value="FOREHAND">FH</mat-button-toggle>
          <mat-button-toggle value="BACKHAND">RH</mat-button-toggle>
          <mat-button-toggle value="SERVE">Aufschlag</mat-button-toggle>
          <mat-button-toggle value="VOLLEY">Volley</mat-button-toggle>
          <mat-button-toggle value="SMASH">Smash</mat-button-toggle>
        </mat-button-toggle-group>
      </div>

      <div class="section">
        <div class="section-label">Richtung (optional)</div>
        <mat-button-toggle-group [(ngModel)]="direction" class="toggle-group">
          <mat-button-toggle value="CROSS_COURT">Cross</mat-button-toggle>
          <mat-button-toggle value="DOWN_THE_LINE">DTL</mat-button-toggle>
          <mat-button-toggle value="MIDDLE">Mitte</mat-button-toggle>
        </mat-button-toggle-group>
      </div>

      <mat-form-field class="full-width">
        <mat-label>Bemerkung (optional)</mat-label>
        <input matInput [(ngModel)]="remark" maxlength="500" />
      </mat-form-field>

    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Abbrechen</button>
      <button mat-raised-button color="primary"
              [disabled]="!pointType"
              (click)="confirm()">Speichern</button>
    </mat-dialog-actions>
  `,
  styles: [`
    mat-dialog-content { display: flex; flex-direction: column; gap: 16px; min-width: 320px; }
    .section { display: flex; flex-direction: column; gap: 6px; }
    .section-label { font-size: 12px; color: rgba(0,0,0,.6); font-weight: 500; }
    .toggle-group { flex-wrap: wrap; }
    .toggle-group.wrap ::ng-deep .mat-button-toggle-group { flex-wrap: wrap; }
    .full-width { width: 100%; }
  `]
})
export class RecordPointDialogComponent {
  readonly data: RecordPointDialogData = inject(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<RecordPointDialogComponent>);

  pointType: PointType | null = null;
  strokeType: StrokeType | null = 'FOREHAND';
  direction: Direction | null = 'CROSS_COURT';
  remark = '';

  cancel(): void {
    this.dialogRef.close(null);
  }

  confirm(): void {
    if (!this.pointType) return;
    const result: RecordPointRequest = {
      winner: this.data.winner,
      pointType: this.pointType,
      ...(this.strokeType && { strokeType: this.strokeType }),
      ...(this.direction && { direction: this.direction }),
      ...(this.remark.trim() && { remark: this.remark.trim() })
    };
    this.dialogRef.close(result);
  }
}
