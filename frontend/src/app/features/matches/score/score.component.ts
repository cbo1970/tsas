import { Component, OnInit, inject, signal, computed, effect } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';

import { TranslatePipe } from '@ngx-translate/core';
import { ApiService } from '../../../core/services/api.service';
import { Player } from '../../../core/models/player.model';
import { MatchWithScore, MatchScore } from '../../../core/models/match.model';
import { ScoreEditDialogComponent } from './score-edit-dialog.component';
import { EndMatchDialogComponent, EndMatchDialogResult } from './end-match-dialog.component';
import { PointType, StrokeType, RecordPointRequest } from '../../../core/models/point.model';
import { PlayerNotesComponent } from '../notes/player-notes.component';
import { ServePickerComponent } from './serve-picker.component';
import { ObservationPanelComponent } from './observation-panel.component';

@Component({
  selector: 'app-score',
  standalone: true,
  imports: [
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatSnackBarModule,
    PlayerNotesComponent,
    ServePickerComponent,
    ObservationPanelComponent,
    TranslatePipe,
  ],
  templateUrl: './score.component.html',
  styles: [`
    :host { display: block; height: 100dvh; }

    .scoring-page {
      display: flex;
      flex-direction: column;
      height: 100dvh;
      background: var(--surface-bg);
      color: var(--text);
      overflow: hidden;
    }

    /* ── Score header (verdichtet, <= 100px) ── */
    .score-header {
      background: var(--surface-card);
      padding: 4px 8px;
      border-bottom: 1px solid var(--border);
      flex-shrink: 0;
    }
    .header-row {
      display: grid;
      grid-template-columns: 44px minmax(0, 1fr) auto minmax(0, 1fr) 44px 44px;
      align-items: center;
      gap: 4px;
    }
    .hdr-icon { width: 44px; height: 44px; padding: 10px; }
    .player-half {
      display: flex;
      flex-direction: column;
      align-items: center;
      min-width: 0;
    }
    .player-name {
      max-width: 100%;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 15px;
      font-weight: 700;
      color: var(--text-muted);
    }
    .player-name.serving { color: var(--brand-strong); }
    .games {
      display: flex;
      flex-direction: column;
      align-items: center;
    }
    .games-label { font-size: 10px; color: var(--text); letter-spacing: .5px; }
    .games-value {
      display: flex;
      gap: 4px;
      font-size: 22px;
      font-weight: 800;
      line-height: 1.2;
      font-variant-numeric: tabular-nums;
    }
    .games-value .winning { color: var(--brand-strong); }
    .sep { opacity: .3; }
    .pts-btn {
      background: none;
      border: none;
      color: var(--text);
      font-size: 30px;
      font-weight: 900;
      line-height: 1;
      font-variant-numeric: tabular-nums;
      white-space: nowrap;
      cursor: pointer;
      border-radius: 6px;
      min-width: 44px;
      min-height: 44px;
      padding: 0 6px;
      transition: background .15s;
    }
    .pts-btn:hover { background: var(--surface-muted); }
    .pts-btn.p1 { color: var(--brand-strong); }
    .set-history {
      text-align: center;
      font-size: 10px;
      line-height: 14px;
      color: var(--text);
      letter-spacing: .5px;
    }

    /* ── Erfassungsfläche: genau ein Panel ── */
    .panels-area {
      flex: 1;
      min-height: 0;
      display: flex;
      flex-direction: column;
      padding: 8px;
      overflow: hidden;
    }

    /* ── Spielerumschalter (zwei Flächen à 56px, in Daumenreichweite) ── */
    .player-switch {
      flex-shrink: 0;
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 8px;
      padding: 0 8px 8px;
    }
    .switch-btn {
      min-height: 56px;
      border: 1px solid var(--border);
      border-radius: 10px;
      background: var(--surface-card);
      color: var(--text-muted);
      font-size: 14px;
      font-weight: 700;
      line-height: 1.2;
      padding: 8px 6px;
      cursor: pointer;
      white-space: normal;
      overflow-wrap: anywhere;
      transition: background .15s;
    }
    .switch-btn.active {
      background: var(--brand-soft);
      border-color: var(--brand);
      color: var(--brand-strong);
    }

    /* Coach-Notizen: volle Breite unter dem Umschalter */
    .coach-notes {
      flex-shrink: 0;
      max-height: 40vh;
      overflow-y: auto;
      background: var(--border);
      border-top: 1px solid var(--text);
      padding: 0 12px;
    }
    .coach-notes summary {
      cursor: pointer;
      font-size: 12px;
      font-weight: 700;
      letter-spacing: .04em;
      color: var(--text-muted);
      padding: 8px 0;
    }

    /* ── Winner overlay ── */
    .winner-overlay {
      position: fixed;
      inset: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 20;
      background: rgba(15,23,42,.45);
    }
    .winner-card {
      background: var(--surface-card);
      color: var(--text);
      border-radius: 12px;
      padding: 24px;
      max-width: 320px;
      width: 90%;
      text-align: center;
      box-shadow: var(--shadow-pop);
    }
    .winner-text { font-size: 22px; font-weight: bold; margin-bottom: 8px; color: var(--brand-strong); }
    .final-score { font-size: 15px; color: var(--text-muted); margin-bottom: 16px; }

    .loading { text-align: center; padding: 48px; color: var(--text-subtle); }
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

  /** Spieler, dessen Panel gerade sichtbar ist. */
  activePlayer = signal<1 | 2>(1);

  player1Name = computed(() => {
    const p = this.player1();
    return p ? `${p.firstName} ${p.lastName}` : 'Spieler 1';
  });

  player2Name = computed(() => {
    const p = this.player2();
    return p ? `${p.firstName} ${p.lastName}` : 'Spieler 2';
  });

  servingPlayer = computed(() => this.matchData()?.score?.servingPlayer ?? null);

  needsServer = computed(() => {
    const m = this.matchData();
    return !!m && m.status !== 'COMPLETED' && this.servingPlayer() === null;
  });

  setHistoryText = computed(() => {
    const history = this.setHistory();
    const m = this.matchData();
    if (!m) return '';
    const past = history.map((s, i) => `Satz ${i + 1}: ${s.p1}:${s.p2}`).join(' · ');
    if (m.score.isDone) return past;
    const current = `Satz ${(history.length + 1)} laufend`;
    return past ? `${past} · ${current}` : current;
  });

  activePlayerInfo = computed(() => this.activePlayer() === 1
    ? { id: this.matchData()?.player1Id ?? '', name: this.player1Name() }
    : { id: this.matchData()?.player2Id ?? '', name: this.player2Name() });

  activeOpponentName = computed(() => this.activePlayer() === 1 ? this.player2Name() : this.player1Name());

  activeServeAttempt = computed(() => this.activePlayer() === 1 ? this.serviceContextP1() : this.serviceContextP2());

  activeStrokeType = computed(() => this.activePlayer() === 1 ? this.strokeTypeP1() : this.strokeTypeP2());

  private matchId = '';

  constructor() {
    // Das Panel folgt dem Aufschlag: jede Score-Antwort (Punkt, Aufschlagwahl,
    // manuelle Korrektur) setzt den aktiven Spieler auf den dann gültigen
    // Aufschläger zurück. Bewusst auf matchData() statt auf servingPlayer(),
    // damit auch eine Antwort mit unverändertem Aufschläger zurückschaltet.
    effect(() => {
      const serving = this.matchData()?.score?.servingPlayer ?? null;
      if (serving === 1 || serving === 2) this.activePlayer.set(serving);
    });
  }

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
    this.api.recordPoint(this.matchId, { winner }).subscribe({
      next: (updated) => this.handlePointResponse(updated),
      error: () => this.snackBar.open('Fehler beim Speichern', 'OK', { duration: 3000 })
    });
  }

  recordObservation(panel: 1 | 2, pointType: PointType): void {
    const m = this.matchData();
    if (!m || m.status === 'COMPLETED') return;
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
        this.resetPointContext();
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

  setStroke(panel: 1 | 2, type: StrokeType | null): void {
    if (panel === 1) {
      this.strokeTypeP1.set(type);
    } else {
      this.strokeTypeP2.set(type);
    }
  }

  /**
   * Der Kontext gehört zum erfassten Punkt, nicht zum Panel: nach einem Punkt
   * wird er auf beiden Panels geräumt. Sonst bliebe eine Auswahl auf dem
   * Aufschläger-Panel stehen, wenn der Punkt vom Returnierenden-Panel erfasst
   * wurde, und reiste stumm mit dem nächsten Punkt mit.
   */
  private resetPointContext(): void {
    this.serviceContextP1.set(null);
    this.serviceContextP2.set(null);
    this.strokeTypeP1.set(null);
    this.strokeTypeP2.set(null);
  }

  private handlePointResponse(updated: MatchWithScore): void {
    const prev = this.matchData();
    let setAdded = false;

    if (prev && updated.score.currentSet > prev.score.currentSet) {
      let p1 = prev.score.gamesPlayer1;
      let p2 = prev.score.gamesPlayer2;
      if (updated.score.setsPlayer1 > prev.score.setsPlayer1) { p1 += 1; } else { p2 += 1; }
      this.setHistory.update(h => [...h, { p1, p2 }]);
      setAdded = true;
    }

    this.matchData.set(updated);

    if (updated.score.isDone) {
      if (!setAdded && prev) {
        const p1Won = updated.score.setsPlayer1 > prev.score.setsPlayer1;
        const p1 = updated.score.gamesPlayer1 + (p1Won ? 1 : 0);
        const p2 = updated.score.gamesPlayer2 + (p1Won ? 0 : 1);
        this.setHistory.update(h => [...h, { p1, p2 }]);
      }
      this.navigateToStatistics();
    }
  }

  private navigateToStatistics(): void {
    const sets = this.setHistory().map(s => `${s.p1}-${s.p2}`).join(',');
    this.router.navigate(
      ['/matches', this.matchId, 'statistics'],
      { queryParams: { sets, p1: this.player1Name(), p2: this.player2Name() } }
    );
  }

  pickServer(player: 1 | 2): void {
    const obs = player === 1
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
