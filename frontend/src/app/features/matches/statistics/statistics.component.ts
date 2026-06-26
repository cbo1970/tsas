import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { NgClass } from '@angular/common';
import { ApiService } from '../../../core/services/api.service';
import { MatchStatistics, PlayerStatistics } from '../../../core/models/statistics.model';

@Component({
  selector: 'app-statistics',
  standalone: true,
  imports: [MatButtonModule, NgClass],
  templateUrl: './statistics.component.html',
  styles: [`
    :host { display: block; min-height: 100dvh; background: var(--surface-bg); color: var(--text); }
    .page { max-width: 480px; margin: 0 auto; padding: 16px; }
    .player-row { display: grid; grid-template-columns: 1fr 48px 1fr; align-items: end; margin-bottom: 8px; }
    .player-name { text-align: center; font-weight: 700; font-size: 15px; color: var(--text); }
    .set-scores { margin-bottom: 12px; }
    .set-row { display: grid; grid-template-columns: 1fr 48px 1fr; align-items: center; gap: 4px; margin-bottom: 4px; }
    .set-label { text-align: center; font-size: 10px; color: var(--text); font-weight: 600; }
    .badge { text-align: center; }
    .badge span { display: inline-block; border-radius: 4px; padding: 3px 14px; font-size: 14px; font-weight: 600; background: var(--border); color: var(--text); }
    .badge.winner span { background: var(--brand); color: #fff; }
    .divider { border-top: 1px solid var(--text); margin: 10px 0; }
    .stat-grid { display: grid; grid-template-columns: 48px 1fr 48px; gap: 3px 6px; align-items: center; }
    .val { font-size: 13px; font-weight: 600; }
    .val-left { text-align: right; color: var(--brand-strong); }
    .val-right { text-align: left; padding-left: 4px; color: var(--text-subtle); }
    .val-right.leading { color: var(--text); font-weight: 700; }
    .stat-label { font-size: 10px; color: var(--text); font-weight: 700; text-align: center; margin-bottom: 1px; }
    .bar { display: flex; height: 4px; border-radius: 2px; overflow: hidden; margin-top: 1px; background: var(--surface-muted); }
    .bar-p1 { background: var(--brand); }
    .bar-p2 { background: var(--text-subtle); }
    .bar-err { background: var(--danger); }
    .bar-good { background: var(--success); }
    .back-row { text-align: center; margin-top: 20px; }
    .set-tabs { display: flex; flex-wrap: wrap; gap: 6px; justify-content: center; margin: 4px 0 12px; }
    .set-tab { background: #fff; border: 1px solid var(--text); color: var(--text); border-radius: var(--radius-pill); padding: 4px 12px; font-size: 12px; font-weight: 600; cursor: pointer; }
    .set-tab.active { background: var(--brand); border-color: var(--brand); color: #fff; }
  `],
})
export class StatisticsComponent implements OnInit {
  private readonly route  = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api    = inject(ApiService);

  stats     = signal<MatchStatistics | null>(null);
  setScores = signal<{ p1: number; p2: number }[]>([]);
  p1Name    = signal('Spieler 1');
  p2Name    = signal('Spieler 2');

  selectedView = signal<'total' | number>('total');

  readonly sets = computed(() => this.stats()?.sets ?? []);

  /** The player stats currently shown in the grid: the total, or the selected set. */
  readonly activeStats = computed<{ p1: PlayerStatistics; p2: PlayerStatistics } | null>(() => {
    const s = this.stats();
    if (!s) return null;
    const v = this.selectedView();
    if (v !== 'total') {
      const set = (s.sets ?? []).find(x => x.setNumber === v);
      if (set) return { p1: set.player1, p2: set.player2 };
    }
    return { p1: s.player1, p2: s.player2 };
  });

  selectView(view: 'total' | number): void {
    this.selectedView.set(view);
  }

  private matchId = '';

  ngOnInit() {
    this.matchId = this.route.snapshot.paramMap.get('id') ?? '';

    const setsParam = this.route.snapshot.queryParamMap.get('sets') ?? '';
    if (setsParam) {
      this.setScores.set(
        setsParam.split(',').map(s => {
          const [p1, p2] = s.split('-').map(Number);
          return { p1, p2 };
        })
      );
    }
    this.p1Name.set(this.route.snapshot.queryParamMap.get('p1') ?? 'Spieler 1');
    this.p2Name.set(this.route.snapshot.queryParamMap.get('p2') ?? 'Spieler 2');

    this.api.getMatchStatistics(this.matchId).subscribe({
      next: s => this.stats.set(s),
    });
  }

  pct(value: number): string {
    return Math.round(value * 100) + '%';
  }

  goToAnalysis() {
    this.router.navigate(
      ['/matches', this.matchId, 'analysis'],
      { queryParams: { p1: this.p1Name(), p2: this.p2Name() } }
    );
  }

  goBack() {
    this.router.navigate(['/players']);
  }
}
