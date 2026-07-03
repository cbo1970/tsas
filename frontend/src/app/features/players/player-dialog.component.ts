import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { CreatePlayerRequest, Player } from '../../core/models/player.model';

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
  templateUrl: './player-dialog.component.html',
  styles: [`
    .dialog-form { display: flex; flex-direction: column; gap: 8px; min-width: 400px; padding-top: 8px; }
    .full-width { width: 100%; }
  `]
})
export class PlayerDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<PlayerDialogComponent>);
  readonly player: Player | null = inject(MAT_DIALOG_DATA, { optional: true });

  form = this.fb.group({
    firstName: [this.player?.firstName ?? '', Validators.required],
    lastName:  [this.player?.lastName  ?? '', Validators.required],
    gender:    [this.player?.gender    ?? 'MALE', Validators.required],
    handedness:   [this.player?.handedness   ?? 'RIGHT', Validators.required],
    backhandType: [this.player?.backhandType ?? 'TWO_HANDED', Validators.required],
    ranking:     [this.player?.ranking     ?? null as string | null],
    nationality: [this.player?.nationality ?? null as string | null]
  });

  cancel() {
    this.dialogRef.close();
  }

  save() {
    if (this.form.valid) {
      const v = this.form.value;
      const request: CreatePlayerRequest = {
        firstName:   v.firstName!,
        lastName:    v.lastName!,
        gender:      v.gender as any,
        handedness:  v.handedness as any,
        backhandType: v.backhandType as any,
        ranking:     v.ranking    ?? undefined,
        nationality: v.nationality ?? undefined
      };
      this.dialogRef.close(request);
    }
  }
}
