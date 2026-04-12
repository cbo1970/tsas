import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
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
          <div class="set-counter">Satz {{ matchData()!.score.currentSet }}</div>
          <div class="match-status-wrapper">
            <div class="match-status" [class.completed]="matchData()!.status === 'COMPLETED'">
              {{ matchData()!.status === 'COMPLETED' ? 'Beendet' : 'Läuft' }}
            </div>
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
              <button mat-raised-button color="primary" (click)="goToMatches()">
                Zurück zur Übersicht
              </button>
            </mat-card-content>
          </mat-card>
        }

        <!-- Tennis court -->
        <div class="court-wrapper">
          <div class="court">

            <!-- Court lines -->
            <div class="line singles-left"></div>
            <div class="line singles-right"></div>
            <div class="line service-line-top"></div>
            <div class="line service-line-bottom"></div>
            <div class="line center-service"></div>

            <!-- Net -->
            <div class="net-area">
              <div class="net-post"></div>
              <div class="net-mesh"></div>
              <div class="net-post"></div>
            </div>

            <!-- Player 2 half (top) — click to score / set serve -->
            <div class="court-half top-half"
                 [class.inactive]="matchData()!.status === 'COMPLETED'"
                 [class.serve-pending]="servingPlayer() === null && matchData()!.status !== 'COMPLETED'"
                 (click)="handleCourtClick(false)">
              <div class="player-overlay">
                <div class="pname">
                  @if (servingPlayer() === 2) { <span class="serve-indicator">🎾 </span> }{{ player2Name() }}
                </div>
                <div class="score-blocks">
                  <div class="score-block">
                    <div class="slbl">Sets</div>
                    <div class="sets-display">
                      @for (s of getSetsArray(); track $index) {
                        <div class="set-badge" [class.won]="s.p2 > s.p1">{{ s.p2 }}</div>
                      }
                    </div>
                  </div>
                  <div class="score-block">
                    <div class="slbl">Games</div>
                    <div class="sval-lg">{{ matchData()!.score.gamesPlayer2 }}</div>
                  </div>
                  <div class="score-block">
                    <div class="slbl">
                      @if (matchData()!.score.isDeuce) { <span class="deuce-lbl">Deuce</span> }
                      @else { Punkte }
                    </div>
                    <div class="sval-md">{{ formatPoints(matchData()!.score, false) }}</div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Player 1 half (bottom) — click to score / set serve -->
            <div class="court-half bottom-half"
                 [class.inactive]="matchData()!.status === 'COMPLETED'"
                 [class.serve-pending]="servingPlayer() === null && matchData()!.status !== 'COMPLETED'"
                 (click)="handleCourtClick(true)">
              <div class="player-overlay">
                <div class="pname">
                  @if (servingPlayer() === 1) { <span class="serve-indicator">🎾 </span> }{{ player1Name() }}
                </div>
                <div class="score-blocks">
                  <div class="score-block">
                    <div class="slbl">Sets</div>
                    <div class="sets-display">
                      @for (s of getSetsArray(); track $index) {
                        <div class="set-badge" [class.won]="s.p1 > s.p2">{{ s.p1 }}</div>
                      }
                    </div>
                  </div>
                  <div class="score-block">
                    <div class="slbl">Games</div>
                    <div class="sval-lg">{{ matchData()!.score.gamesPlayer1 }}</div>
                  </div>
                  <div class="score-block">
                    <div class="slbl">
                      @if (matchData()!.score.isDeuce) { <span class="deuce-lbl">Deuce</span> }
                      @else { Punkte }
                    </div>
                    <div class="sval-md">{{ formatPoints(matchData()!.score, true) }}</div>
                  </div>
                </div>
              </div>
            </div>

          </div>

          <div class="bottom-area">
            <button class="ace-btn"
                    [disabled]="matchData()!.status === 'COMPLETED' || servingPlayer() !== 1"
                    (click)="recordAce(true)">
              <span class="ace-icon">🎾</span>
              <span class="ace-count">{{ matchData()!.score.acesPlayer1 }}</span>
              <span class="ace-lbl">Asse {{ player1Name() }}</span>
            </button>

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

            <button class="ace-btn"
                    [disabled]="matchData()!.status === 'COMPLETED' || servingPlayer() !== 2"
                    (click)="recordAce(false)">
              <span class="ace-icon">🎾</span>
              <span class="ace-count">{{ matchData()!.score.acesPlayer2 }}</span>
              <span class="ace-lbl">Asse {{ player2Name() }}</span>
            </button>
          </div>
        </div>
      } @else {
        <div class="loading">Lade Match...</div>
      }
    </div>
  `,
  styles: [`
    .score-page { padding: 24px; max-width: 900px; margin: 0 auto; }

    /* ── Header ── */
    .score-header { display: flex; align-items: center; margin-bottom: 20px; }
    .score-header h2 { flex: 1; margin: 0; font-size: 24px; }
    .set-counter { font-size: 18px; font-weight: bold; color: #555; }
    .match-status-wrapper { flex: 1; display: flex; justify-content: flex-end; }
    .match-status { padding: 4px 12px; border-radius: 16px; background: #4caf50; color: white; font-size: 14px; }
    .match-status.completed { background: #9e9e9e; }

    /* ── Winner card ── */
    .winner-card { margin-bottom: 20px; background: linear-gradient(135deg, #1a237e, #283593); color: white; }
    .winner-card mat-card-content { padding: 24px !important; }
    .winner-text { display: flex; align-items: center; gap: 8px; font-size: 22px; font-weight: bold; margin-bottom: 8px; }
    .final-score { font-size: 16px; margin-bottom: 16px; }

    /* ── Court wrapper (dark surround = outside the court) ── */
    .court-wrapper {
      background: rgb(8, 40, 88);
      padding: 28px 20px;
      border-radius: 12px;
      display: flex;
      flex-direction: column;
      align-items: center;
      box-shadow: 0 6px 32px rgba(0,0,0,.5);
    }

    /* ── The court itself — landscape (net vertical) ── */
    .court {
      position: relative;
      background: rgb(32, 90, 160);
      border: 3px solid white;
      width: 100%;
      aspect-ratio: 23.77 / 10.97;
    }

    /* ── White court lines (absolute) ── */
    .line { position: absolute; background: white; }

    /* Singles baselines — 12.49 % from top/bottom */
    .singles-left  { left: 0; right: 0; top: 12.49%;    height: 2px; }
    .singles-right { left: 0; right: 0; bottom: 12.49%; height: 2px; }

    /* Service lines — 23.08 % from each baseline (left/right) */
    .service-line-top    { left: 23.08%;  top: 12.49%; bottom: 12.49%; width: 2px; }
    .service-line-bottom { right: 23.08%; top: 12.49%; bottom: 12.49%; width: 2px; }

    /* Center service line — horizontal, between the two service lines */
    .center-service {
      left: 23.08%; right: 23.08%;
      top: calc(50% - 1px); height: 2px;
    }

    /* ── Net — vertical in the center ── */
    .net-area {
      position: absolute;
      left: calc(50% - 5px);
      top: -6px; bottom: -6px;
      width: 10px;
      display: flex;
      flex-direction: column;
      align-items: center;
      z-index: 2;
    }
    .net-post {
      width: 20px; height: 8px;
      background: #ddd;
      border-radius: 2px;
      flex-shrink: 0;
      box-shadow: 0 2px 4px rgba(0,0,0,.5);
    }
    .net-mesh {
      flex: 1; width: 6px;
      background: repeating-linear-gradient(
        180deg,
        rgba(255,255,255,.85) 0px, rgba(255,255,255,.85) 6px,
        transparent 6px, transparent 10px
      );
    }

    /* ── Clickable court halves ── */
    .court-half {
      position: absolute;
      top: 0; bottom: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      user-select: none;
      transition: background 0.15s;
      z-index: 3;
    }
    /* top-half = Player 2 → rechte Seite */
    .top-half    { right: 0; width: 50%; }
    /* bottom-half = Player 1 → linke Seite */
    .bottom-half { left: 0;  width: 50%; }
    .court-half:hover:not(.inactive)  { background: rgba(255,255,255,.06); }
    .court-half:active:not(.inactive) { background: rgba(255,255,255,.12); }
    .court-half.inactive { cursor: default; }
    .court-half.serve-pending { opacity: 0.45; }
    .court-half.serve-pending:hover { opacity: 0.75; background: rgba(255,255,255,.08); }

    /* ── Score overlay card on each half ── */
    .player-overlay {
      background: rgb(6, 28, 70);
      border: 1px solid rgba(255,255,255,.2);
      border-radius: 10px;
      padding: 10px 18px 12px;
      text-align: center;
      color: white;
      min-width: 200px;
      box-shadow: 0 2px 12px rgba(0,0,0,.4);
    }
    .pname {
      font-size: 16px;
      font-weight: bold;
      margin-bottom: 8px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .score-blocks {
      display: flex;
      gap: 20px;
      justify-content: center;
      align-items: flex-end;
    }
    .score-block {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 2px;
    }
    .slbl {
      font-size: 10px;
      text-transform: uppercase;
      letter-spacing: 1px;
      color: rgba(255,255,255,.55);
    }
    .sets-display { display: flex; gap: 5px; }
    .set-badge {
      width: 26px; height: 26px;
      border-radius: 50%;
      background: rgba(255,255,255,.2);
      display: flex; align-items: center; justify-content: center;
      font-size: 13px; font-weight: bold;
    }
    .set-badge.won { background: rgba(255,255,255,.55); }
    .sval-lg { font-size: 46px; font-weight: bold; line-height: 1; }
    .sval-md { font-size: 28px; font-weight: 500; line-height: 1; }
    .deuce-lbl { color: #ffd54f; font-size: 9px; font-weight: bold; }

    /* ── Bottom area (ace buttons + action buttons) ── */
    .bottom-area {
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      margin-top: 20px;
      gap: 12px;
    }

    .action-buttons {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      flex: 1;
    }
    .action-buttons .mat-mdc-outlined-button {
      color: white;
      --mdc-outlined-button-outline-color: rgba(255,255,255,.6);
    }

    .ace-btn {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 4px;
      background: rgba(255,255,255,.1);
      border: 1px solid rgba(255,255,255,.4);
      border-radius: 10px;
      padding: 10px 16px;
      cursor: pointer;
      color: white;
      min-width: 80px;
      transition: background 0.15s;
    }
    .ace-btn:hover:not([disabled]) { background: rgba(255,255,255,.2); }
    .ace-btn:active:not([disabled]) { background: rgba(255,255,255,.28); }
    .ace-btn[disabled] { opacity: 0.4; cursor: default; }
    .ace-icon { font-size: 20px; line-height: 1; }
    .ace-count { font-size: 28px; font-weight: bold; line-height: 1; }
    .ace-lbl { font-size: 10px; text-transform: uppercase; letter-spacing: 0.5px; color: rgba(255,255,255,.7); text-align: center; }
    .loading { text-align: center; padding: 48px; color: #666; }
    .serve-indicator { font-size: 14px; }
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

  servingPlayer = computed(() => this.matchData()?.score?.servingPlayer ?? null);

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

  recordAce(forPlayer1: boolean) {
    const m = this.matchData();
    if (!m || m.status === 'COMPLETED') return;

    const obs = forPlayer1
      ? this.api.acePlayer1(this.matchId)
      : this.api.acePlayer2(this.matchId);

    obs.subscribe({
      next: (score) => {
        this.matchData.update(md => md ? { ...md, score } : md);
        if (score.isDone) {
          this.loadMatch();
        }
      },
      error: () => this.snackBar.open('Fehler beim Speichern', 'OK', { duration: 3000 })
    });
  }

  setServe(forPlayer1: boolean) {
    const m = this.matchData();
    if (!m || m.status === 'COMPLETED') return;

    const obs = forPlayer1
      ? this.api.setServingPlayer1(this.matchId)
      : this.api.setServingPlayer2(this.matchId);

    obs.subscribe({
      next: (score) => {
        this.matchData.update(md => md ? { ...md, score } : md);
      },
      error: () => this.snackBar.open('Fehler beim Speichern', 'OK', { duration: 3000 })
    });
  }

  handleCourtClick(forPlayer1: boolean) {
    if (this.servingPlayer() === null) {
      this.setServe(forPlayer1);
    } else {
      this.scorePoint(forPlayer1);
    }
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
