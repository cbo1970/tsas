import { Component, OnInit, inject, signal, computed, effect } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';

import { ApiService } from '../../../core/services/api.service';
import { Player } from '../../../core/models/player.model';
import { MatchWithScore, MatchScore } from '../../../core/models/match.model';
import { LayoutService } from '../../../core/layout/layout.service';
import { responsiveDialogConfig } from '../../../core/layout/responsive-dialog';
import { ScoreEditDialogComponent } from './score-edit-dialog.component';
import { EndMatchDialogComponent, EndMatchDialogResult } from './end-match-dialog.component';
import { PointType, StrokeType, RecordPointRequest } from '../../../core/models/point.model';
import { PlayerNotesComponent } from '../notes/player-notes.component';
import { ServePickerComponent } from './serve-picker.component';
import { ObservationPanelComponent } from './observation-panel.component';

const OBSERVATION_LABELS: Record<PointType, string> = {
  ACE: 'Ass',
  WINNER: 'Winner',
  DOUBLE_FAULT: 'Doppelfehler',
  FORCED_ERROR: 'Forced Error',
  UNFORCED_ERROR: 'Unforced Error',
  NET: 'Netz',
};

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

    /* ── Untere Leiste ── */
    .bottom-bar {
      flex-shrink: 0;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      padding: 0 8px 8px;
    }
    .last-observation {
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 13px;
      color: var(--text-muted);
    }
    .notes-btn {
      position: relative;
      margin-left: auto;
      flex-shrink: 0;
      min-height: 40px;
      min-width: 44px;
      padding: 0 16px;
      border: 1px solid var(--border);
      border-radius: 999px;
      background: var(--surface-card);
      color: var(--text);
      font-size: 14px;
      font-weight: 700;
      cursor: pointer;
    }
    .notes-dot {
      position: absolute;
      top: 6px;
      right: 8px;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--brand);
    }

    /* ── Notizen-Vollbild ── */
    .notes-view { display: flex; flex-direction: column; height: 100%; min-height: 0; }
    .notes-head {
      flex-shrink: 0;
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 4px 8px;
      background: var(--surface-card);
      border-bottom: 1px solid var(--border);
    }
    .notes-title { font-size: 16px; font-weight: 700; }
    .notes-body { flex: 1; min-height: 0; overflow-y: auto; padding: 12px; }

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
    .winner-actions { display: flex; flex-direction: column; gap: 8px; }
    .winner-actions button { min-height: 44px; }
    .final-score { font-size: 15px; color: var(--text-muted); margin-bottom: 16px; }

    .loading { text-align: center; padding: 48px; color: var(--text-subtle); }

    /* ── Flache Viewports (Querformat, ~360px hoch) ──
       Das Aufschlaeger-Panel braucht rund 580px Hoehe; es passt dort nicht.
       Ohne Ausweg schneidet overflow:hidden die unteren Zeilen (Forced /
       Unforced / Netz und die Schlagart) ab und nichts kann dorthin scrollen.
       Im Querformat ist senkrechtes Scrollen ausdruecklich erlaubt, also
       geben wir der Seite ihre natuerliche Hoehe zurueck. */
    @media (max-height: 560px) {
      :host { height: auto; min-height: 100dvh; }
      .scoring-page { height: auto; min-height: 100dvh; overflow: visible; }
      .panels-area { overflow: visible; }
    }
  `]
})
export class ScoreComponent implements OnInit {
  private readonly api      = inject(ApiService);
  private readonly route    = inject(ActivatedRoute);
  private readonly router   = inject(Router);
  private readonly dialog   = inject(MatDialog);
  private readonly layout   = inject(LayoutService);
  private readonly snackBar = inject(MatSnackBar);

  matchData  = signal<MatchWithScore | null>(null);
  player1    = signal<Player | null>(null);
  player2    = signal<Player | null>(null);
  setHistory = signal<Array<{p1: number; p2: number}>>([]);

  /** Aufschlagversuch des laufenden Punkts, unabhängig vom aktiven Panel. */
  serveAttempt     = signal<1 | 2 | null>(null);
  strokeTypeP1     = signal<StrokeType | null>(null);
  strokeTypeP2     = signal<StrokeType | null>(null);

  /** Notizen-Vollbild innerhalb der Scoring-Route. */
  showNotes = signal(false);
  hasNotes  = signal(false);
  /**
   * Zuletzt erfolgreich erfasster Punkt (Anzeige in der unteren Leiste).
   * 'QUICK' = Schnellpunkt ohne Beobachtung — die Leiste darf dann nicht
   * weiter die Beobachtung des vorherigen Punktes behaupten.
   */
  lastObservation = signal<PointType | 'QUICK' | null>(null);

  lastObservationText = computed(() => {
    const o = this.lastObservation();
    if (!o) return 'Noch nichts erfasst';
    return o === 'QUICK' ? 'Zuletzt: Punkt ohne Zuordnung' : `Zuletzt: ${OBSERVATION_LABELS[o]}`;
  });

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

  activeServeAttempt = computed(() => this.serveAttempt());

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
        this.loadNotesIndicator();
      },
      error: () => this.snackBar.open('Match nicht gefunden', 'OK', { duration: 3000 })
    });
  }

  private loadNotesIndicator() {
    this.api.getPlayerNotes(this.matchId).subscribe({
      next: (notes) => this.hasNotes.set(notes.some(n => n.note.trim().length > 0)),
      error: () => this.hasNotes.set(false),
    });
  }

  closeNotes() {
    this.showNotes.set(false);
    this.loadNotesIndicator();
  }

  loadPlayers(p1Id: string, p2Id: string) {
    this.api.getPlayer(p1Id).subscribe(p => this.player1.set(p));
    this.api.getPlayer(p2Id).subscribe(p => this.player2.set(p));
  }

  recordQuickPoint(winner: 1 | 2): void {
    const m = this.matchData();
    if (!m || m.status === 'COMPLETED') return;
    this.api.recordPoint(this.matchId, { winner }).subscribe({
      next: (updated) => {
        this.handlePointResponse(updated);
        // Auch der Schnellpunkt ist ein erfasster Punkt: Aufschlagversuch und
        // Schlagart gehoeren zu ihm und duerfen nicht am naechsten mitreisen.
        this.lastObservation.set('QUICK');
        this.resetPointContext();
      },
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
    const serveAttempt = this.serveAttempt();
    const strokeType   = panel === 1 ? this.strokeTypeP1()     : this.strokeTypeP2();

    const request: RecordPointRequest = { winner, pointType, serveAttempt, strokeType };

    this.api.recordPoint(this.matchId, request).subscribe({
      next: (updated) => {
        this.handlePointResponse(updated);
        this.lastObservation.set(pointType);
        this.resetPointContext();
      },
      error: () => this.snackBar.open('Fehler beim Speichern', 'OK', { duration: 3000 })
    });
  }

  toggleService(attempt: 1 | 2): void {
    this.serveAttempt.update(v => v === attempt ? null : attempt);
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
    this.serveAttempt.set(null);
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
    const ref = this.dialog.open(ScoreEditDialogComponent, { ...responsiveDialogConfig(this.layout.isHandset()), data: { score: m.score } });
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
