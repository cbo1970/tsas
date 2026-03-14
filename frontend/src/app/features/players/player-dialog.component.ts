import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { CreatePlayerRequest } from '../../core/models/player.model';

@Component({
  selector: 'app-player-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule
  ],
  template: `
    <h2 mat-dialog-title>Neuer Spieler</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Vorname</mat-label>
          <input matInput formControlName="firstName" />
          @if (form.get('firstName')?.hasError('required') && form.get('firstName')?.touched) {
            <mat-error>Vorname ist erforderlich</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Nachname</mat-label>
          <input matInput formControlName="lastName" />
          @if (form.get('lastName')?.hasError('required') && form.get('lastName')?.touched) {
            <mat-error>Nachname ist erforderlich</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Geschlecht</mat-label>
          <mat-select formControlName="gender">
            <mat-option value="MALE">Männlich</mat-option>
            <mat-option value="FEMALE">Weiblich</mat-option>
            <mat-option value="OTHER">Divers</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Spielhand</mat-label>
          <mat-select formControlName="handedness">
            <mat-option value="RIGHT">Rechts</mat-option>
            <mat-option value="LEFT">Links</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Backhand</mat-label>
          <mat-select formControlName="backhandType">
            <mat-option value="TWO_HANDED">Zweihändig</mat-option>
            <mat-option value="ONE_HANDED">Einhändig</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Ranking (optional)</mat-label>
          <input matInput type="number" formControlName="ranking" />
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Abbrechen</button>
      <button mat-raised-button color="primary" (click)="save()" [disabled]="form.invalid">
        Speichern
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-form { display: flex; flex-direction: column; gap: 8px; min-width: 400px; padding-top: 8px; }
    .full-width { width: 100%; }
  `]
})
export class PlayerDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<PlayerDialogComponent>);

  form = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    gender: ['MALE', Validators.required],
    handedness: ['RIGHT', Validators.required],
    backhandType: ['TWO_HANDED', Validators.required],
    ranking: [null as number | null]
  });

  cancel() {
    this.dialogRef.close();
  }

  save() {
    if (this.form.valid) {
      const v = this.form.value;
      const request: CreatePlayerRequest = {
        firstName: v.firstName!,
        lastName: v.lastName!,
        gender: v.gender as any,
        handedness: v.handedness as any,
        backhandType: v.backhandType as any,
        ranking: v.ranking ?? undefined
      };
      this.dialogRef.close(request);
    }
  }
}
