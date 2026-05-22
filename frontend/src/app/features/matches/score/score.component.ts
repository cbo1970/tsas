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
import { PointType, StrokeType, Direction, RecordPointRequest } from '../../../core/models/point.model';

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
  template: `
    <div class="scoring-page">

      <div class="score-strip">
        <button mat-icon-button (click)="goToMatches()">
          <mat-icon>arrow_back</mat-icon>
        </button>
        <div class="strip-center">
          <div class="strip-player">
            @if (servingPlayer() === 1) { <span class="serve-ball">🎾</span> }
            <span class="strip-name">{{ player1Name() }}</span>
            @if (matchData()) {
              <span class="strip-pts">{{ formatPoints(matchData()!.score, true) }}</span>
            }
          </div>
          <span class="strip-sep">:</span>
          <div class="strip-player">
            @if (matchData()) {
              <span class="strip-pts">{{ formatPoints(matchData()!.score, false) }}</span>
            }
            <span class="strip-name">{{ player2Name() }}</span>
            @if (servingPlayer() === 2) { <span class="serve-ball">🎾</span> }
          </div>
        </div>
        <div class="strip-actions">
          <button mat-icon-button data-testid="edit-score-btn" (click)="openEditDialog()">
            <mat-icon>edit</mat-icon>
          </button>
          @if (matchData() && matchData()!.status !== 'COMPLETED') {
            <button mat-mini-fab color="warn" data-testid="end-match-btn" (click)="endMatch()">
              <mat-icon>stop</mat-icon>
            </button>
          }
        </div>
      </div>

      @if (!matchData()) {
        <div class="loading">Lade Match...</div>
      }

      @if (matchData()) {
        <div class="tennis-court">

          <!-- Court surface and lines -->
          <div class="court-infield"></div>
          <div class="court-line court-net"></div>
          <div class="court-line court-singles-top"></div>
          <div class="court-line court-singles-bottom"></div>
          <div class="court-line court-service-left"></div>
          <div class="court-line court-service-right"></div>
          <div class="court-line court-center-left"></div>
          <div class="court-line court-center-right"></div>

          @if (matchData()!.status === 'COMPLETED') {
            <div class="winner-overlay">
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
            </div>
          }

          @if (matchData()!.status !== 'COMPLETED') {

            <!-- Games at net -->
            <div class="net-games-col net-games-col-left">
              @for (s of setHistory(); track $index) {
                <span class="net-games past-set">{{ s.p1 }}</span>
              }
              <span class="net-games">{{ matchData()!.score.gamesPlayer1 }}</span>
            </div>
            <div class="net-games-col net-games-col-right">
              @for (s of setHistory(); track $index) {
                <span class="net-games past-set">{{ s.p2 }}</span>
              }
              <span class="net-games">{{ matchData()!.score.gamesPlayer2 }}</span>
            </div>

            <!-- Corridor scores -->
            <div class="corridor-score corridor-score-left">
              <span class="cs-pts">{{ formatPoints(matchData()!.score, true) }}</span>
              <span class="cs-sets">{{ matchData()!.score.setsPlayer1 }} {{ matchData()!.score.setsPlayer1 === 1 ? 'Satz' : 'Sätze' }}</span>
            </div>
            <div class="corridor-score corridor-score-right">
              <span class="cs-pts">{{ formatPoints(matchData()!.score, false) }}</span>
              <span class="cs-sets">{{ matchData()!.score.setsPlayer2 }} {{ matchData()!.score.setsPlayer2 === 1 ? 'Satz' : 'Sätze' }}</span>
            </div>

            <!-- Player 1 panel (left half) -->
            <div class="player-panel player-panel-left" [class.serving]="servingPlayer() === 1">
              <div class="panel-header">
                <div class="panel-name-row">
                  <span class="panel-name">{{ player1Name() }}</span>
                  @if (servingPlayer() === 1) { <span class="serve-ball-panel">🎾</span> }
                </div>
                @if (servingPlayer() === null) {
                  <span class="serve-hint">Tippen = Aufschlag setzen</span>
                }
              </div>
              <div class="point-grid">
                @for (pt of pointTypes; track pt.value) {
                  <button class="tile"
                    [class.tile-disabled]="isServeOnly(pt.value) && servingPlayer() !== 1"
                    [disabled]="isServeOnly(pt.value) && servingPlayer() !== 1"
                    (click)="recordPoint(effectiveWinner(1, pt.value), pt.value)">
                    <span class="tile-icon">{{ pt.icon }}</span>
                    <span class="tile-label">{{ pt.label }}</span>
                  </button>
                }
                <div class="tile-empty"></div>
              </div>
              <div class="pill-section">
                <div class="pill-col">
                  <span class="pill-label">Schlagart</span>
                  <div class="pill-buttons">
                    @for (st of strokeTypes; track st.value) {
                      <button class="pill" [class.active]="strokeTypeP1() === st.value"
                              (click)="strokeTypeP1.set(st.value)">{{ st.label }}</button>
                    }
                  </div>
                </div>
                <div class="pill-col">
                  <span class="pill-label">Richtung</span>
                  <div class="pill-buttons">
                    @for (d of directions; track d.value) {
                      <button class="pill" [class.active]="directionP1() === d.value"
                              (click)="directionP1.set(d.value)">{{ d.label }}</button>
                    }
                  </div>
                </div>
              </div>
            </div>

            <!-- Player 2 panel (right half) -->
            <div class="player-panel player-panel-right" [class.serving]="servingPlayer() === 2">
              <div class="panel-header">
                <div class="panel-name-row">
                  <span class="panel-name">{{ player2Name() }}</span>
                  @if (servingPlayer() === 2) { <span class="serve-ball-panel">🎾</span> }
                </div>
                @if (servingPlayer() === null) {
                  <span class="serve-hint">Tippen = Aufschlag setzen</span>
                }
              </div>
              <div class="point-grid">
                @for (pt of pointTypes; track pt.value) {
                  <button class="tile"
                    [class.tile-disabled]="isServeOnly(pt.value) && servingPlayer() !== 2"
                    [disabled]="isServeOnly(pt.value) && servingPlayer() !== 2"
                    (click)="recordPoint(effectiveWinner(2, pt.value), pt.value)">
                    <span class="tile-icon">{{ pt.icon }}</span>
                    <span class="tile-label">{{ pt.label }}</span>
                  </button>
                }
                <div class="tile-empty"></div>
              </div>
              <div class="pill-section">
                <div class="pill-col">
                  <span class="pill-label">Schlagart</span>
                  <div class="pill-buttons">
                    @for (st of strokeTypes; track st.value) {
                      <button class="pill" [class.active]="strokeTypeP2() === st.value"
                              (click)="strokeTypeP2.set(st.value)">{{ st.label }}</button>
                    }
                  </div>
                </div>
                <div class="pill-col">
                  <span class="pill-label">Richtung</span>
                  <div class="pill-buttons">
                    @for (d of directions; track d.value) {
                      <button class="pill" [class.active]="directionP2() === d.value"
                              (click)="directionP2.set(d.value)">{{ d.label }}</button>
                    }
                  </div>
                </div>
              </div>
            </div>

          }
        </div>
      }

    </div>
  `,
  styles: [`
    /* ── Page ── */
    .scoring-page {
      display: flex;
      flex-direction: column;
      height: 100dvh;
      background: #1a5276;
      color: white;
      overflow: hidden;
    }

    /* ── Score strip ── */
    .score-strip {
      display: flex;
      align-items: center;
      background: rgba(0,0,0,.82);
      padding: 6px 12px;
      border-bottom: 1px solid rgba(255,255,255,.15);
      flex-shrink: 0;
      gap: 8px;
      z-index: 10;
    }
    .strip-center {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 16px;
    }
    .strip-player { display: flex; align-items: baseline; gap: 5px; }
    .strip-name   { font-size: 13px; font-weight: 600; }
    .strip-pts    { font-size: 22px; font-weight: 800; line-height: 1; }
    .strip-sub    { font-size: 10px; opacity: .5; }
    .strip-sep    { font-size: 20px; opacity: .3; }
    .serve-ball   { font-size: 13px; }
    .strip-actions { display: flex; align-items: center; gap: 4px; }

    /* ── Tennis Court ── */
    .tennis-court {
      flex: 1;
      position: relative;
      background: #1a3a5c;
      overflow: hidden;
    }

    /* Infield surface */
    .court-infield {
      position: absolute;
      top: 8%;
      left: 8%;
      right: 8%;
      bottom: 8%;
      background: #1565c0;
      border: 3px solid white;
      box-shadow: inset 0 0 40px rgba(0,0,0,0.25);
    }

    /* Court lines */
    .court-line { position: absolute; background: white; }

    /* Net (vertical center line) */
    .court-net {
      top: 8%;
      bottom: 8%;
      left: 50%;
      width: 3px;
      transform: translateX(-50%);
    }

    /* Singles sidelines (horizontal — alleys are ~12.5% of court height) */
    .court-singles-top {
      left: 8%;
      right: 8%;
      top: 18.5%;
      height: 2px;
    }
    .court-singles-bottom {
      left: 8%;
      right: 8%;
      bottom: 18.5%;
      height: 2px;
    }

    /* Service lines (vertical, ~27% from each side = 54% of half-court from baseline) */
    .court-service-left {
      left: 27%;
      top: 18.5%;
      bottom: 18.5%;
      width: 2px;
    }
    .court-service-right {
      right: 27%;
      top: 18.5%;
      bottom: 18.5%;
      width: 2px;
    }

    /* Center service marks (horizontal, from service line to net at 50% height) */
    .court-center-left {
      left: 27%;
      right: 50%;
      top: 50%;
      height: 1px;
      transform: translateY(-0.5px);
    }
    .court-center-right {
      left: 50%;
      right: 27%;
      top: 50%;
      height: 1px;
      transform: translateY(-0.5px);
    }

    /* ── Player panels (inside court halves) ── */
    .player-panel {
      position: absolute;
      top: 50%;
      transform: translateY(-50%);
      padding: 10px 12px;
      display: flex;
      flex-direction: column;
      z-index: 5;
      background: rgba(8, 18, 36, 0.88);
      border-radius: 10px;
      border: 1px solid rgba(255,255,255,0.12);
      box-shadow: 0 6px 24px rgba(0,0,0,0.55);
    }

    /* Left panel: from left infield edge to net */
    .player-panel-left {
      left: calc(8% + 10px);
      right: calc(50% + 8px);
    }

    /* Right panel: from net to right infield edge */
    .player-panel-right {
      left: calc(50% + 8px);
      right: calc(8% + 10px);
    }

    /* Portrait: stack panels vertically */
    @media (orientation: portrait) {
      .court-net {
        top: 50%;
        left: 8%;
        right: 8%;
        bottom: auto;
        width: auto;
        height: 3px;
        transform: translateY(-50%);
      }
      .court-singles-top,
      .court-singles-bottom,
      .court-service-left,
      .court-service-right,
      .court-center-left,
      .court-center-right { display: none; }

      .player-panel-left {
        top: 29%;
        left: calc(8% + 10px);
        right: calc(8% + 10px);
      }
      .player-panel-right {
        top: 71%;
        left: calc(8% + 10px);
        right: calc(8% + 10px);
      }
    }

    /* ── Panel header ── */
    .panel-header {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 4px;
      margin-bottom: 8px;
      flex-shrink: 0;
      text-align: center;
    }
    .panel-name {
      font-size: 20px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: .5px;
      color: rgba(255,255,255,.7);
    }
    /* Aufschläger: Name in Hellblau */
    .player-panel.serving .panel-name { color: #90caf9; }

    .panel-name-row {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }
    .serve-ball-panel { font-size: 20px; line-height: 1; }

    .serve-hint {
      font-size: 16px;
      color: rgba(255,255,255,.4);
      font-style: italic;
    }

    /* ── Point type grid ── */
    .point-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 4px;
      margin-bottom: 8px;
      flex-shrink: 0;
    }
    .tile {
      background: #e67e22;
      border: none;
      border-radius: 7px;
      padding: 8px 6px;
      display: flex;
      flex-direction: row;
      align-items: center;
      justify-content: flex-start;
      gap: 6px;
      cursor: pointer;
      color: white;
      font-weight: 600;
      box-shadow: 0 2px 6px rgba(0,0,0,0.4);
      transition: background .15s, transform .1s;
      min-height: 46px;
    }
    .tile:hover  { background: #f39c12; transform: translateY(-1px); }
    .tile:active { background: #ca6f1e; transform: translateY(0); box-shadow: 0 1px 3px rgba(0,0,0,0.4); }
    .tile.tile-disabled { background: rgba(255,255,255,.08); color: rgba(255,255,255,.25); box-shadow: none; cursor: not-allowed; }
    .tile.tile-disabled:hover { background: rgba(255,255,255,.08); transform: none; }
    .tile.tile-disabled .tile-icon { opacity: .3; }
    .tile-icon  { font-size: 22px; line-height: 1; flex-shrink: 0; }
    .tile-label { font-size: 16px; line-height: 1.2; text-align: left; }
    .tile-empty { /* grid placeholder */ }

    /* ── Pills ── */
    .pill-section {
      display: flex;
      gap: 12px;
      flex-shrink: 0;
      margin-top: 12px;
    }
    .pill-col {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 5px;
      flex: 1;
    }
    .pill-buttons {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .pill-label {
      font-size: 14px;
      text-transform: uppercase;
      letter-spacing: .5px;
      color: rgba(255,255,255,.7);
    }
    .pill {
      background: rgba(255,255,255,.1);
      border: 1px solid rgba(255,255,255,.15);
      border-radius: 20px;
      padding: 4px 16px;
      font-size: 16px;
      color: rgba(255,255,255,.65);
      cursor: pointer;
      transition: background .15s;
    }
    .pill:hover  { background: rgba(255,255,255,.2); }
    .pill.active { background: #e67e22; border-color: #e67e22; color: white; font-weight: 700; }

    /* ── Winner overlay ── */
    .winner-overlay {
      position: absolute;
      inset: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 20;
      background: rgba(0,0,0,.5);
    }
    .winner-card {
      background: linear-gradient(135deg, #1a237e, #283593);
      color: white;
      max-width: 320px;
      width: 90%;
    }
    .winner-text {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 20px;
      font-weight: bold;
      margin-bottom: 8px;
    }
    .final-score { font-size: 15px; margin-bottom: 16px; }

    /* ── Games at net ── */
    .net-games-col {
      position: absolute;
      top: calc(8% + 6px);
      display: flex;
      flex-direction: column;
      gap: 2px;
      z-index: 7;
      pointer-events: none;
    }
    .net-games-col-left  { right: calc(50% + 10px); align-items: flex-end; }
    .net-games-col-right { left:  calc(50% + 10px); align-items: flex-start; }
    .net-games {
      font-size: 32px;
      font-weight: 800;
      color: #ccff00;
      line-height: 1;
    }
    .net-games.past-set { font-size: 24px; opacity: .6; }

    @media (orientation: portrait) {
      .net-games-col-left  { top: calc(8% + 6px);  left: calc(8% + 10px); right: auto; align-items: flex-start; }
      .net-games-col-right { top: calc(50% + 6px); left: calc(8% + 10px); }
    }

    /* ── Corridor score display ── */
    .corridor-score {
      position: absolute;
      top: calc(8% + 6px);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      z-index: 6;
      color: white;
      pointer-events: none;
    }
    .corridor-score-left  { left: calc(8% + 4px);  right: calc(50% + 4px); }
    .corridor-score-right { left: calc(50% + 4px);  right: calc(8% + 4px); }
    .cs-pts  { font-size: 32px; font-weight: 800; line-height: 1; }
    .cs-sets { font-size: 14px; color: rgba(255,255,255,.6); margin-top: 3px; }

    @media (orientation: portrait) {
      .corridor-score-left  { left: calc(8% + 4px); right: calc(8% + 4px); }
      .corridor-score-right { top: calc(50% + 6px); left: calc(8% + 4px); right: calc(8% + 4px); }
    }

    .loading { text-align: center; padding: 48px; color: rgba(255,255,255,.5); }
  `]
})
export class ScoreComponent implements OnInit {
  private readonly api       = inject(ApiService);
  private readonly route     = inject(ActivatedRoute);
  private readonly router    = inject(Router);
  private readonly dialog    = inject(MatDialog);
  private readonly snackBar  = inject(MatSnackBar);

  matchData  = signal<MatchWithScore | null>(null);
  player1    = signal<Player | null>(null);
  player2    = signal<Player | null>(null);
  setHistory = signal<Array<{p1: number; p2: number}>>([]);

  strokeTypeP1 = signal<StrokeType>('FOREHAND');
  strokeTypeP2 = signal<StrokeType>('FOREHAND');
  directionP1  = signal<Direction>('CROSS_COURT');
  directionP2  = signal<Direction>('CROSS_COURT');

  readonly pointTypes: { value: PointType; icon: string; label: string }[] = [
    { value: 'WINNER',         icon: '🏆', label: 'Winner'    },
    { value: 'UNFORCED_ERROR', icon: '😓', label: 'Unforced Err' },
    { value: 'FORCED_ERROR',   icon: '💨', label: 'Forced Err' },
    { value: 'ACE',            icon: '🎯', label: 'Ass'       },
    { value: 'DOUBLE_FAULT',   icon: '❌', label: 'DF'        },
    { value: 'NET',            icon: '🔴', label: 'Netz'      },
    { value: 'OUT_LONG',       icon: '↑',  label: 'Aus lang'  },
    { value: 'OUT_SIDE',       icon: '→',  label: 'Aus Seite' },
  ];

  readonly strokeTypes: { value: StrokeType; label: string }[] = [
    { value: 'FOREHAND', label: 'FH'        },
    { value: 'BACKHAND', label: 'RH'        },
    { value: 'VOLLEY',   label: 'Volley'    },
    { value: 'SMASH',    label: 'Smash'     },
  ];

  readonly directions: { value: Direction; label: string }[] = [
    { value: 'CROSS_COURT',   label: 'Cross' },
    { value: 'DOWN_THE_LINE', label: 'DTL'   },
    { value: 'MIDDLE',        label: 'Mitte' },
  ];

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

  recordPoint(winner: 1 | 2, pointType: PointType): void {
    const m = this.matchData();
    if (!m || m.status === 'COMPLETED') return;

    if (this.servingPlayer() === null) {
      this.setServe(winner === 1);
      return;
    }

    const request: RecordPointRequest = {
      winner,
      pointType,
      strokeType: winner === 1 ? this.strokeTypeP1() : this.strokeTypeP2(),
      direction:  winner === 1 ? this.directionP1()  : this.directionP2(),
    };

    this.api.recordPoint(this.matchId, request).subscribe({
      next: (updated) => {
        const prev = this.matchData();
        if (prev && updated.score.currentSet > prev.score.currentSet) {
          let p1 = prev.score.gamesPlayer1;
          let p2 = prev.score.gamesPlayer2;
          if (updated.score.setsPlayer1 > prev.score.setsPlayer1) { p1 += 1; } else { p2 += 1; }
          this.setHistory.update(h => [...h, { p1, p2 }]);
        }
        this.matchData.set(updated);
        if (updated.score.isDone) this.loadMatch();
      },
      error: () => this.snackBar.open('Fehler beim Speichern', 'OK', { duration: 3000 })
    });
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
        next: () => {
          this.loadMatch();
          this.snackBar.open('Match beendet (w.o.)', 'OK', { duration: 3000 });
        },
        error: () => this.snackBar.open('Fehler beim Beenden', 'OK', { duration: 3000 })
      });
    });
  }

  isServeOnly(pt: PointType): boolean {
    return pt === 'ACE' || pt === 'DOUBLE_FAULT';
  }

  effectiveWinner(panelPlayer: 1 | 2, pt: PointType): 1 | 2 {
    return pt === 'DOUBLE_FAULT' ? (panelPlayer === 1 ? 2 : 1) : panelPlayer;
  }

  goToMatches() {
    this.router.navigate(['/players']);
  }
}
