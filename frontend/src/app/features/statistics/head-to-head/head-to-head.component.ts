import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { ApiService } from '../../../core/services/api.service';
import { Player } from '../../../core/models/player.model';
import { HeadToHeadStatistics } from '../../../core/models/statistics.model';
import { OpponentPreparation } from '../../../core/models/analysis.model';

@Component({
  selector: 'app-head-to-head',
  standalone: true,
  imports: [MatFormFieldModule, MatSelectModule, MatButtonModule, MatProgressSpinnerModule, FormsModule, TranslatePipe],
  templateUrl: './head-to-head.component.html',
  styles: [`
    :host { display: block; min-height: 100dvh; background: linear-gradient(160deg, #103A6B 0%, #2D72B8 100%); color: #eee; font-family: sans-serif; }
    .page { max-width: 520px; margin: 0 auto; padding: 16px; }
    .pickers { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 16px; }
    /* ausgewählter Spielername + Feld auf dem blauen Hintergrund lesbar (weiss) */
    :host ::ng-deep .pickers .mat-mdc-select-value,
    :host ::ng-deep .pickers .mat-mdc-select-value-text,
    :host ::ng-deep .pickers .mat-mdc-select-min-line,
    :host ::ng-deep .pickers .mat-mdc-select-placeholder { color: #fff; }
    :host ::ng-deep .pickers .mat-mdc-select-arrow { color: rgba(255, 255, 255, 0.85); }
    :host ::ng-deep .pickers .mdc-floating-label { color: #fff; }
    :host ::ng-deep .pickers .mat-mdc-form-field.mat-focused .mdc-floating-label { color: #fff; }
    :host ::ng-deep .pickers .mdc-notched-outline > * { border-color: rgba(255, 255, 255, 0.45); }
    .empty-hint { text-align: center; color: #64748b; padding: 32px; }
    .section-title { font-size: 12px; color: #38bdf8; text-transform: uppercase; letter-spacing: .05em; margin: 16px 0 8px; }
    .divider { border-top: 1px solid #1e293b; margin: 10px 0; }
    .stat-grid { display: grid; grid-template-columns: 64px 1fr 64px; gap: 3px 6px; align-items: center; }
    .val { font-size: 13px; font-weight: 600; }
    .val-left { text-align: right; color: #0ea5e9; }
    .val-right { text-align: left; padding-left: 4px; color: #94a3b8; }
    .val-right.leading { color: #eee; font-weight: 700; }
    .player-row .val { color: #fff; }
    .stat-label { font-size: 10px; color: #64748b; text-align: center; }
    .bar { display: flex; height: 4px; border-radius: 2px; overflow: hidden; margin-top: 1px; }
    .bar-p1 { background: #0ea5e9; }
    .bar-p2 { background: #475569; }
    .prep-actions { margin: 24px 0 8px; text-align: center; }
    .prep-actions button { color: #fff; background: #0ea5e9; }
    .prep-actions button[disabled] { opacity: 0.5; }
    .prep-card { background: rgba(15, 23, 42, 0.55); border: 1px solid rgba(56, 189, 248, 0.35); border-radius: 8px; padding: 14px 16px; margin-top: 12px; }
    .prep-card h3 { font-size: 14px; color: #38bdf8; text-transform: uppercase; letter-spacing: .05em; margin: 12px 0 4px; }
    .prep-card h3:first-child { margin-top: 0; }
    .prep-card p { font-size: 13px; line-height: 1.45; color: #e2e8f0; margin: 0 0 8px; white-space: pre-wrap; }
    .prep-card .recos { list-style: none; padding: 0; margin: 6px 0 0; }
    .prep-card .recos li { padding: 6px 0; border-top: 1px solid rgba(148, 163, 184, 0.18); }
    .prep-card .recos li:first-child { border-top: none; }
    .prep-card .reco-title { font-size: 13px; font-weight: 700; color: #fff; }
    .prep-card .reco-detail { font-size: 12px; color: #cbd5e1; margin-top: 2px; }
    .prep-meta { font-size: 11px; color: #94a3b8; margin-top: 10px; }
    .prep-error { background: rgba(127, 29, 29, 0.55); border: 1px solid #fca5a5; color: #fee2e2; border-radius: 6px; padding: 10px 12px; margin-top: 12px; font-size: 13px; }
  `],
})
export class HeadToHeadComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(ApiService);

  players = signal<Player[]>([]);
  player1Id = signal<string | null>(null);
  player2Id = signal<string | null>(null);
  stats = signal<HeadToHeadStatistics | null>(null);

  // TEN-51 — KI-Vorbereitung gegen den Gegner
  preparation = signal<OpponentPreparation | null>(null);
  preparing = signal<boolean>(false);
  prepError = signal<string | null>(null);

  ngOnInit() {
    this.player1Id.set(this.route.snapshot.queryParamMap.get('player1'));
    this.player2Id.set(this.route.snapshot.queryParamMap.get('player2'));
    this.api.getPlayers().subscribe(ps => {
      this.players.set(ps.filter(p => p.active !== false));
      this.fetchIfReady();
    });
  }

  onSelect() {
    this.stats.set(null);
    this.preparation.set(null);
    this.prepError.set(null);
    this.fetchIfReady();
  }

  /**
   * TEN-51 — fordert eine KI-gestützte Vorbereitung des „eigenen Spielers" (Spieler 1) gegen
   * den Gegner (Spieler 2) an. Beide IDs müssen gesetzt und unterschiedlich sein und das
   * Head-to-Head muss mindestens ein abgeschlossenes Match enthalten (Backend → HTTP 422 sonst).
   */
  requestPreparation() {
    const own = this.player1Id();
    const opp = this.player2Id();
    if (!own || !opp || own === opp) return;
    this.preparation.set(null);
    this.prepError.set(null);
    this.preparing.set(true);
    this.api.generateOpponentPreparation(own, opp).subscribe({
      next: p => {
        this.preparation.set(p);
        this.preparing.set(false);
      },
      error: err => {
        const detail = err?.error?.detail || err?.message || 'KI-Vorbereitung fehlgeschlagen.';
        this.prepError.set(detail);
        this.preparing.set(false);
      }
    });
  }

  private fetchIfReady() {
    const p1 = this.player1Id();
    const p2 = this.player2Id();
    if (p1 && p2 && p1 !== p2) {
      this.api.getHeadToHead(p1, p2).subscribe(s => this.stats.set(s));
    }
  }

  playerName(id: string | null): string {
    const p = this.players().find(x => x.id === id);
    return p ? `${p.firstName} ${p.lastName}` : '';
  }

  pct(value: number): string {
    return Math.round(value * 100) + '%';
  }
}
