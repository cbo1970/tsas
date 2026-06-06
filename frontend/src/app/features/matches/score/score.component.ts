import { Component, OnInit, inject, signal, computed } from '@angular/core';
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
import { EndMatchDialogComponent, EndMatchDialogResult } from './end-match-dialog.component';
import { PointType, StrokeType, RecordPointRequest } from '../../../core/models/point.model';

@Component({
  selector: 'app-score',
  standalone: true,
  imports: [
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatSnackBarModule,
  ],
  templateUrl: './score.component.html',
  styles: [`
    :host { display: block; height: 100dvh; }

    .scoring-page {
      display: flex;
      flex-direction: column;
      height: 100dvh;
      background: #0f172a;
      color: white;
      overflow: hidden;
      font-family: sans-serif;
    }

    /* ── Score header ── */
    .score-header {
      background: rgba(0,0,0,.8);
      padding: 10px 16px;
      border-bottom: 1px solid rgba(255,255,255,.1);
      flex-shrink: 0;
    }
    .header-grid {
      display: grid;
      grid-template-columns: 1fr auto 1fr;
      align-items: center;
      gap: 8px;
    }
    .player-half-left  { text-align: center; }
    .player-half-right { text-align: center; }
    .player-name {
      font-size: 14px;
      font-weight: 700;
      color: rgba(255,255,255,.75);
    }
    .player-name.serving { color: #4ade80; }
    .score-center { display: flex; align-items: center; gap: 8px; }
    .sep { opacity: .3; }
    .games-num {
      font-size: 26px;
      font-weight: 800;
      line-height: 1;
    }
    .games-num.winning { color: #4ade80; }
    .pts-btn {
      background: none;
      border: none;
      color: white;
      font-size: 34px;
      font-weight: 900;
      line-height: 1;
      cursor: pointer;
      border-radius: 6px;
      padding: 2px 10px;
      transition: background .15s;
    }
    .pts-btn:hover { background: rgba(255,255,255,.12); }
    .pts-btn.p1 { color: #4ade80; }
    .pts-label {
      font-size: 10px;
      opacity: .4;
      letter-spacing: .5px;
      text-align: center;
    }
    .set-history {
      text-align: center;
      margin-top: 4px;
      font-size: 10px;
      opacity: .35;
      letter-spacing: .5px;
    }
    .header-actions {
      display: flex;
      justify-content: flex-end;
      gap: 4px;
      margin-top: 6px;
    }

    /* ── Observation panels ── */
    .panels-area {
      flex: 1;
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
      padding: 12px;
      overflow-y: auto;
    }
    .obs-panel {
      background: rgba(255,255,255,.05);
      border: 1px solid rgba(255,255,255,.12);
      border-radius: 10px;
      padding: 10px;
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .obs-panel.serving {
      background: rgba(74,222,128,.08);
      border-color: rgba(74,222,128,.3);
    }
    .panel-title {
      font-size: 11px;
      font-weight: 700;
      letter-spacing: .5px;
      text-align: center;
      color: rgba(255,255,255,.7);
    }
    .obs-panel.serving .panel-title { color: #4ade80; }
    .serve-badge { font-size: 9px; opacity: .6; font-weight: 400; }

    .section-label {
      font-size: 9px;
      opacity: .5;
      letter-spacing: .5px;
      margin-top: 4px;
    }
    .section-label.green { color: #4ade80; opacity: .8; }
    .section-label.red   { color: #fca5a5; opacity: .8; }

    /* Context toggle buttons (yellow) */
    .ctx-row { display: grid; grid-template-columns: 1fr 1fr; gap: 4px; }
    .ctx-btn {
      background: rgba(254,240,138,.1);
      border: 1px solid rgba(254,240,138,.2);
      border-radius: 5px;
      padding: 6px 4px;
      text-align: center;
      font-size: 12px;
      color: rgba(254,240,138,.5);
      cursor: pointer;
      transition: background .15s;
    }
    .ctx-btn.active {
      background: #854d0e;
      border-color: #fef08a;
      color: #fef08a;
      font-weight: 600;
    }

    /* Observation buttons */
    .obs-row-2 { display: grid; grid-template-columns: 1fr 1fr;     gap: 4px; }
    .obs-row-1 { display: grid; grid-template-columns: 1fr;         gap: 4px; }
    .obs-row-3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 4px; }

    .obs-btn {
      border: none;
      border-radius: 5px;
      padding: 7px 4px;
      text-align: center;
      font-size: 12px;
      font-weight: 700;
      cursor: pointer;
      transition: opacity .15s;
    }
    .obs-btn:hover  { opacity: .85; }
    .obs-btn:active { opacity: .7; }
    .obs-btn.err-sm { font-size: 11px; padding: 6px 2px; }

    .win-btn {
      background: #166534;
      border: 1px solid #4ade80;
      color: #4ade80;
    }
    .err-btn {
      background: #7f1d1d;
      border: 1px solid #fca5a5;
      color: #fca5a5;
    }

    /* ── Winner overlay ── */
    .winner-overlay {
      position: fixed;
      inset: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 20;
      background: rgba(0,0,0,.6);
    }
    .winner-card {
      background: linear-gradient(135deg,#1a237e,#283593);
      color: white;
      border-radius: 12px;
      padding: 24px;
      max-width: 320px;
      width: 90%;
      text-align: center;
    }
    .winner-text { font-size: 22px; font-weight: bold; margin-bottom: 8px; }
    .final-score { font-size: 15px; color: rgba(255,255,255,.7); margin-bottom: 16px; }

    .loading { text-align: center; padding: 48px; color: rgba(255,255,255,.5); }
  `]
})
export class ScoreComponent implements OnInit {
  private readonly api      = inject(ApiService);
  private readonly route    = inject(ActivatedRoute);
  private readonly router   = inject(Router);
  private readonly dialog   = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  matchData  = signal<MatchWithScore | null>(null);
  player1    = signal<Player | null>(null);
  player2    = signal<Player | null>(null);
  setHistory = signal<Array<{p1: number; p2: number}>>([]);

  serviceContextP1 = signal<1 | 2 | null>(null);
  serviceContextP2 = signal<1 | 2 | null>(null);
  strokeTypeP1     = signal<StrokeType | null>(null);
  strokeTypeP2     = signal<StrokeType | null>(null);

  player1Name = computed(() => {
    const p = this.player1();
    return p ? `${p.firstName} ${p.lastName}` : 'Spieler 1';
  });

  player2Name = computed(() => {
    const p = this.player2();
    return p ? `${p.firstName} ${p.lastName}` : 'Spieler 2';
  });

  servingPlayer = computed(() => this.matchData()?.score?.servingPlayer ?? null);

  setHistoryText = computed(() => {
    const history = this.setHistory();
    const m = this.matchData();
    if (!m) return '';
    const past = history.map((s, i) => `Satz ${i + 1}: ${s.p1}:${s.p2}`).join(' · ');
    if (m.score.isDone) return past;
    const current = `Satz ${(history.length + 1)} laufend`;
    return past ? `${past} · ${current}` : current;
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

  recordQuickPoint(winner: 1 | 2): void {
    const m = this.matchData();
    if (!m || m.status === 'COMPLETED') return;
    if (this.servingPlayer() === null) { this.setServe(winner === 1); return; }
    this.api.recordPoint(this.matchId, { winner }).subscribe({
      next: (updated) => this.handlePointResponse(updated),
      error: () => this.snackBar.open('Fehler beim Speichern', 'OK', { duration: 3000 })
    });
  }

  recordObservation(panel: 1 | 2, pointType: PointType): void {
    const m = this.matchData();
    if (!m || m.status === 'COMPLETED') return;
    if (this.servingPlayer() === null) { this.setServe(panel === 1); return; }

    if (pointType === 'ACE' && this.servingPlayer() !== panel) {
      this.snackBar.open('Ass nur für den Aufschläger', 'OK', { duration: 3000 });
      return;
    }
    if (pointType === 'DOUBLE_FAULT' && this.servingPlayer() !== panel) {
      this.snackBar.open('Doppelfehler nur für den Aufschläger', 'OK', { duration: 3000 });
      return;
    }

    const winner: 1 | 2 = (pointType === 'ACE' || pointType === 'WINNER') ? panel : (panel === 1 ? 2 : 1);
    const serveAttempt = panel === 1 ? this.serviceContextP1() : this.serviceContextP2();
    const strokeType   = panel === 1 ? this.strokeTypeP1()     : this.strokeTypeP2();

    const request: RecordPointRequest = { winner, pointType, serveAttempt, strokeType };

    this.api.recordPoint(this.matchId, request).subscribe({
      next: (updated) => {
        this.handlePointResponse(updated);
        this.resetContextForPanel(panel);
      },
      error: () => this.snackBar.open('Fehler beim Speichern', 'OK', { duration: 3000 })
    });
  }

  toggleService(panel: 1 | 2, attempt: 1 | 2): void {
    if (panel === 1) {
      this.serviceContextP1.update(v => v === attempt ? null : attempt);
    } else {
      this.serviceContextP2.update(v => v === attempt ? null : attempt);
    }
  }

  toggleStroke(panel: 1 | 2, type: StrokeType): void {
    if (panel === 1) {
      this.strokeTypeP1.update(v => v === type ? null : type);
    } else {
      this.strokeTypeP2.update(v => v === type ? null : type);
    }
  }

  private resetContextForPanel(panel: 1 | 2): void {
    if (panel === 1) {
      this.serviceContextP1.set(null);
      this.strokeTypeP1.set(null);
    } else {
      this.serviceContextP2.set(null);
      this.strokeTypeP2.set(null);
    }
  }

  private handlePointResponse(updated: MatchWithScore): void {
    const prev = this.matchData();
    if (prev && updated.score.currentSet > prev.score.currentSet) {
      let p1 = prev.score.gamesPlayer1;
      let p2 = prev.score.gamesPlayer2;
      if (updated.score.setsPlayer1 > prev.score.setsPlayer1) { p1 += 1; } else { p2 += 1; }
      this.setHistory.update(h => [...h, { p1, p2 }]);
    }
    this.matchData.set(updated);
    if (updated.score.isDone) this.loadMatch();
  }

  private setServe(forPlayer1: boolean) {
    const obs = forPlayer1
      ? this.api.setServingPlayer1(this.matchId)
      : this.api.setServingPlayer2(this.matchId);
    obs.subscribe({
      next: (score) => this.matchData.update(md => md ? { ...md, score } : md),
      error: () => this.snackBar.open('Fehler beim Speichern', 'OK', { duration: 3000 })
    });
  }

  formatPoints(score: MatchScore, forPlayer1: boolean): string {
    if (score.isDeuce) {
      if (score.isAdvantagePlayer1 == null) return '40';
      return score.isAdvantagePlayer1 === forPlayer1 ? 'A' : '40';
    }
    const pts = forPlayer1 ? score.pointsPlayer1 : score.pointsPlayer2;
    return (['0', '15', '30', '40'] as const)[pts] ?? pts.toString();
  }

  getWinnerName(): string {
    const m = this.matchData();
    if (!m?.score.winner) return '';
    return m.score.winner === 'PLAYER1' ? this.player1Name() : this.player2Name();
  }

  openEditDialog() {
    const m = this.matchData();
    if (!m) return;
    const ref = this.dialog.open(ScoreEditDialogComponent, { width: '500px', data: { score: m.score } });
    ref.afterClosed().subscribe((result) => {
      if (!result) return;
      this.api.setScore(this.matchId, result).subscribe({
        next: (score) => {
          this.matchData.update(md => md ? { ...md, score } : md);
          this.snackBar.open('Score aktualisiert', 'OK', { duration: 3000 });
          if (score.isDone) this.loadMatch();
        },
        error: () => this.snackBar.open('Fehler beim Speichern', 'OK', { duration: 3000 })
      });
    });
  }

  endMatch() {
    const ref = this.dialog.open(EndMatchDialogComponent, {
      width: '360px',
      data: { player1Name: this.player1Name(), player2Name: this.player2Name() }
    });
    ref.afterClosed().subscribe((result: EndMatchDialogResult | undefined) => {
      if (!result) return;
      this.api.endMatchWalkover(this.matchId, result.winner).subscribe({
        next: () => { this.loadMatch(); this.snackBar.open('Match beendet (w.o.)', 'OK', { duration: 3000 }); },
        error: () => this.snackBar.open('Fehler beim Beenden', 'OK', { duration: 3000 })
      });
    });
  }

  goToMatches() {
    this.router.navigate(['/players']);
  }
}
