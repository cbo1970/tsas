import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatRadioModule } from '@angular/material/radio';
import { FormsModule } from '@angular/forms';

export interface EndMatchDialogData {
  player1Name: string;
  player2Name: string;
}

export interface EndMatchDialogResult {
  winner: 'PLAYER1' | 'PLAYER2';
}

@Component({
  selector: 'app-end-match-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatRadioModule
  ],
  templateUrl: './end-match-dialog.component.html',
  styles: [`
    .hint { margin: 0 0 16px; color: #555; }
    .radio-group { display: flex; flex-direction: column; gap: 12px; }
  `]
})
export class EndMatchDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<EndMatchDialogComponent>);
  readonly data: EndMatchDialogData = inject(MAT_DIALOG_DATA);

  selectedWinner: 'PLAYER1' | 'PLAYER2' | null = null;

  cancel() {
    this.dialogRef.close();
  }

  confirm() {
    if (this.selectedWinner) {
      this.dialogRef.close({ winner: this.selectedWinner } satisfies EndMatchDialogResult);
    }
  }
}
