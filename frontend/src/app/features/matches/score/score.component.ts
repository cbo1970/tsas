import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';

import { ApiService } from '../../../core/services/api.service';
import { Player } from '../../../core/models/player.model';
import { MatchWithScore, MatchScore } from '../../../core/models/match.model';
import { ScoreEditDialogComponent } from './score-edit-dialog.component';

@Component({
  selector: 'app-score',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatSnackBarModule
  ],
  template: `
    <div class="score-page">
      @if (matchData()) {
        <div class="score-header">
          <h2>Match</h2>
          <div class="match-status" [class.completed]="matchData()!.status === 'COMPLETED'">
            {{ matchData()!.status === 'COMPLETED' ? 'Beendet' : 'Läuft' }}
          </div>
        </div>

        @if (matchData()!.status === 'COMPLETED' && matchData()!.score.winner) {
          <mat-card class="winner-card">
            <mat-card-content>
              <div class="winner-text">
                <mat-icon>emoji_events</mat-icon>
                <span>Sieger: {{ getWinnerName() }}</span>
              </div>
              <div class="final-score">
                Satz: {{ matchData()!.score.setsPlayer1 }} : {{ matchData()!.score.setsPlayer2 }}
              </div>
              <button mat-raised-button color="primary" routerLink="/players" (click)="goToMatches()">
                Zurück zur Übersicht
              </button>
            </mat-card-content>
          </mat-card>
        }

        <div class="scoreboard">
          <!-- Player 1 -->
          <div class="player-panel" [class.inactive]="matchData()!.status === 'COMPLETED'"
               (click)="scorePoint(true)">
            <div class="player-name">{{ player1Name() }}</div>
            <div class="sets-display">
              @for (s of getSetsArray(); track $index) {
                <div class="set-badge" [class.won]="s.p1 > s.p2">{{ s.p1 }}</div>
              }
            </div>
            <div class="games-display">{{ matchData()!.score.gamesPlayer1 }}</div>
            <div class="points-display">{{ formatPoints(matchData()!.score, true) }}</div>
          </div>

          <!-- Divider with score labels -->
          <div class="score-divider">
            <div class="score-label">Sets</div>
            <div class="score-label">Games</div>
            <div class="score-label">
              @if (matchData()!.score.isDeuce) {
                <span class="deuce-label">Deuce</span>
              } @else {
                Punkte
              }
            </div>
          </div>

          <!-- Player 2 -->
          <div class="player-panel player2" [class.inactive]="matchData()!.status === 'COMPLETED'"
               (click)="scorePoint(false)">
            <div class="player-name">{{ player2Name() }}</div>
            <div class="sets-display">
              @for (s of getSetsArray(); track $index) {
                <div class="set-badge" [class.won]="s.p2 > s.p1">{{ s.p2 }}</div>
              }
            </div>
            <div class="games-display">{{ matchData()!.score.gamesPlayer2 }}</div>
            <div class="points-display">{{ formatPoints(matchData()!.score, false) }}</div>
          </div>
        </div>

        <div class="set-counter">
          Satz {{ matchData()!.score.currentSet }}
        </div>

        <div class="action-buttons">
          <button mat-stroked-button (click)="openEditDialog()">
            <mat-icon>edit</mat-icon>
            Score korrigieren
          </button>
          @if (matchData()!.status !== 'COMPLETED') {
            <button mat-raised-button color="warn" (click)="endMatch()">
              <mat-icon>stop</mat-icon>
              Match beenden
            </button>
          }
        </div>
      } @else {
        <div class="loading">Lade Match...</div>
      }
    </div>
  `,
  styles: [`
    .score-page {
      padding: 24px;
      max-width: 800px;
      margin: 0 auto;
    }
    .score-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }
    .score-header h2 { margin: 0; font-size: 24px; }
    .match-status {
      padding: 4px 12px;
      border-radius: 16px;
      background: #4caf50;
      color: white;
      font-size: 14px;
    }
    .match-status.completed { background: #9e9e9e; }
    .winner-card {
      margin-bottom: 24px;
      background: linear-gradient(135deg, #1a237e, #283593);
      color: white;
    }
    .winner-card mat-card-content { padding: 24px !important; }
    .winner-text {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 24px;
      font-weight: bold;
      margin-bottom: 8px;
    }
    .final-score { font-size: 18px; margin-bottom: 16px; }
    .scoreboard {
      display: grid;
      grid-template-columns: 1fr auto 1fr;
      gap: 0;
      border-radius: 16px;
      overflow: hidden;
      box-shadow: 0 4px 16px rgba(0,0,0,.2);
      min-height: 300px;
    }
    .player-panel {
      background: linear-gradient(180deg, #1a237e 0%, #283593 100%);
      color: white;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 16px;
      padding: 32px 16px;
      cursor: pointer;
      transition: background 0.2s;
      user-select: none;
    }
    .player-panel:hover:not(.inactive) {
      background: linear-gradient(180deg, #283593 0%, #3949ab 100%);
    }
    .player-panel:active:not(.inactive) {
      background: linear-gradient(180deg, #3949ab 0%, #5c6bc0 100%);
    }
    .player-panel.player2 {
      background: linear-gradient(180deg, #b71c1c 0%, #c62828 100%);
    }
    .player-panel.player2:hover:not(.inactive) {
      background: linear-gradient(180deg, #c62828 0%, #d32f2f 100%);
    }
    .player-panel.player2:active:not(.inactive) {
      background: linear-gradient(180deg, #d32f2f 0%, #e53935 100%);
    }
    .player-panel.inactive { cursor: default; opacity: 0.7; }
    .player-name {
      font-size: 20px;
      font-weight: bold;
      text-align: center;
    }
    .sets-display {
      display: flex;
      gap: 8px;
    }
    .set-badge {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: rgba(255,255,255,0.2);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 16px;
      font-weight: bold;
    }
    .set-badge.won { background: rgba(255,255,255,0.5); }
    .games-display {
      font-size: 56px;
      font-weight: bold;
      line-height: 1;
    }
    .points-display {
      font-size: 32px;
      font-weight: 500;
    }
    .score-divider {
      background: #263238;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 16px;
      padding: 32px 8px;
      min-width: 60px;
    }
    .score-label {
      color: rgba(255,255,255,0.5);
      font-size: 11px;
      text-transform: uppercase;
      writing-mode: vertical-rl;
      transform: rotate(180deg);
      letter-spacing: 1px;
    }
    .deuce-label {
      color: #ffd54f;
      font-size: 9px;
      font-weight: bold;
    }
    .set-counter {
      text-align: center;
      margin-top: 16px;
      color: #666;
      font-size: 14px;
    }
    .action-buttons {
      display: flex;
      gap: 16px;
      justify-content: center;
      margin-top: 24px;
    }
    .loading { text-align: center; padding: 48px; color: #666; }
  `]
})
export class ScoreComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  matchData = signal<MatchWithScore | null>(null);
  player1 = signal<Player | null>(null);
  player2 = signal<Player | null>(null);

  player1Name = computed(() => {
    const p = this.player1();
    return p ? `${p.firstName} ${p.lastName}` : 'Spieler 1';
  });

  player2Name = computed(() => {
    const p = this.player2();
    return p ? `${p.firstName} ${p.lastName}` : 'Spieler 2';
  });

  private matchId = '';

  ngOnInit() {
    this.matchId = this.route.snapshot.paramMap.get('id') ?? '';
    this.loadMatch();
  }

  loadMatch() {
    this.api.getMatch(this.matchId).subscribe({
      next: (match) => {
        this.matchData.set(match);
        this.loadPlayers(match.player1Id, match.player2Id);
      },
      error: () => this.snackBar.open('Match nicht gefunden', 'OK', { duration: 3000 })
    });
  }

  loadPlayers(p1Id: string, p2Id: string) {
    this.api.getPlayer(p1Id).subscribe(p => this.player1.set(p));
    this.api.getPlayer(p2Id).subscribe(p => this.player2.set(p));
  }

  scorePoint(player1Scored: boolean) {
    const m = this.matchData();
    if (!m || m.status === 'COMPLETED') return;

    const obs = player1Scored
      ? this.api.scorePlayer1(this.matchId)
      : this.api.scorePlayer2(this.matchId);

    obs.subscribe({
      next: (score) => {
        this.matchData.update(md => md ? { ...md, score } : md);
        if (score.isDone) {
          // Reload full match to get status
          this.loadMatch();
        }
      },
      error: () => this.snackBar.open('Fehler beim Speichern', 'OK', { duration: 3000 })
    });
  }

  formatPoints(score: MatchScore, forPlayer1: boolean): string {
    if (score.isDeuce) {
      if (score.isAdvantagePlayer1 === null || score.isAdvantagePlayer1 === undefined) {
        return '40';
      }
      return score.isAdvantagePlayer1 === forPlayer1 ? 'A' : '40';
    }
    const pts = forPlayer1 ? score.pointsPlayer1 : score.pointsPlayer2;
    const map = ['0', '15', '30', '40'];
    return map[pts] ?? pts.toString();
  }

  getSetsArray(): { p1: number; p2: number }[] {
    const m = this.matchData();
    if (!m) return [];
    const total = m.setsToWin * 2 - 1;
    const result: { p1: number; p2: number }[] = [];
    // We only show completed sets; current set shown via games
    const completedSets = m.score.currentSet - 1;
    // Can't reconstruct per-set scores from current data structure; show totals
    // Show setsPlayer1/setsPlayer2 as a simple indicator
    for (let i = 0; i < m.setsToWin * 2 - 1; i++) {
      if (i < m.score.setsPlayer1) result.push({ p1: 1, p2: 0 });
      else if (i < m.score.setsPlayer1 + m.score.setsPlayer2) result.push({ p1: 0, p2: 1 });
      else result.push({ p1: 0, p2: 0 });
    }
    return result;
  }

  getWinnerName(): string {
    const m = this.matchData();
    if (!m || !m.score.winner) return '';
    if (m.score.winner === 'PLAYER1') return this.player1Name();
    return this.player2Name();
  }

  openEditDialog() {
    const m = this.matchData();
    if (!m) return;

    const ref = this.dialog.open(ScoreEditDialogComponent, {
      width: '500px',
      data: { score: m.score }
    });

    ref.afterClosed().subscribe((result) => {
      if (result) {
        this.api.setScore(this.matchId, result).subscribe({
          next: (score) => {
            this.matchData.update(md => md ? { ...md, score } : md);
            this.snackBar.open('Score aktualisiert', 'OK', { duration: 3000 });
            if (score.isDone) this.loadMatch();
          },
          error: () => this.snackBar.open('Fehler beim Speichern', 'OK', { duration: 3000 })
        });
      }
    });
  }

  endMatch() {
    this.api.endMatch(this.matchId).subscribe({
      next: () => {
        this.loadMatch();
        this.snackBar.open('Match beendet', 'OK', { duration: 3000 });
      },
      error: () => this.snackBar.open('Fehler beim Beenden', 'OK', { duration: 3000 })
    });
  }

  goToMatches() {
    this.router.navigate(['/players']);
  }
}
