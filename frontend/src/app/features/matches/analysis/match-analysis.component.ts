import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ApiService } from '../../../core/services/api.service';
import { MatchAnalysis } from '../../../core/models/analysis.model';

@Component({
  selector: 'app-match-analysis',
  standalone: true,
  imports: [MatButtonModule, MatProgressSpinnerModule, DatePipe],
  templateUrl: './match-analysis.component.html',
  styles: [`
    :host { display: block; min-height: 100dvh; background: #0f172a; color: #eee; font-family: sans-serif; }
    .page { max-width: 560px; margin: 0 auto; padding: 16px; }
    .title { text-align: center; font-weight: 700; font-size: 18px; margin: 4px 0 2px; }
    .subtitle { text-align: center; font-size: 13px; color: #94a3b8; margin-bottom: 4px; }
    .meta { text-align: center; font-size: 11px; color: #64748b; margin-bottom: 16px; }
    .centered { display: flex; flex-direction: column; align-items: center; gap: 14px; padding: 40px 8px; text-align: center; }
    .hint { font-size: 13px; color: #94a3b8; max-width: 340px; }
    .section { background: #1e293b; border-radius: 10px; padding: 12px 14px; margin-bottom: 10px; }
    .section-label { font-size: 11px; text-transform: uppercase; letter-spacing: .04em; color: #0ea5e9; font-weight: 700; margin-bottom: 4px; }
    .section-text { font-size: 14px; line-height: 1.45; color: #e2e8f0; white-space: pre-wrap; }
    .rec-heading { font-size: 13px; font-weight: 700; color: #94a3b8; margin: 18px 0 8px; text-transform: uppercase; letter-spacing: .04em; }
    .rec { display: flex; gap: 10px; background: #1e293b; border-radius: 10px; padding: 10px 12px; margin-bottom: 8px; }
    .rec-prio { flex: 0 0 auto; width: 26px; height: 26px; border-radius: 50%; background: #0ea5e9; color: #001018; font-weight: 700; font-size: 13px; display: flex; align-items: center; justify-content: center; }
    .rec-body { flex: 1; }
    .rec-title { font-size: 14px; font-weight: 600; color: #f1f5f9; }
    .rec-detail { font-size: 13px; color: #cbd5e1; line-height: 1.4; margin-top: 2px; white-space: pre-wrap; }
    .error-box { background: #422; border: 1px solid #7f1d1d; border-radius: 10px; padding: 14px; color: #fecaca; font-size: 14px; text-align: center; margin-bottom: 14px; }
    .actions { text-align: center; margin: 18px 0; }
    .back-row { text-align: center; margin-top: 20px; }
  `],
})
export class MatchAnalysisComponent implements OnInit {
  private readonly route  = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api    = inject(ApiService);

  analysis   = signal<MatchAnalysis | null>(null);
  loading    = signal(false);
  generating = signal(false);
  error      = signal<string | null>(null);

  p1Name = signal('Spieler 1');
  p2Name = signal('Spieler 2');

  private matchId = '';

  /** Analysis is shown only when it completed successfully. */
  readonly hasResult = computed(() => this.analysis()?.status === 'COMPLETED');

  readonly textSections = computed(() => {
    const a = this.analysis();
    if (!a) return [];
    return [
      { key: 'key-moments',        label: 'Schlüsselmomente',     value: a.keyMoments },
      { key: 'own-strengths',      label: 'Eigene Stärken',        value: a.ownStrengths },
      { key: 'own-weaknesses',     label: 'Eigene Schwächen',      value: a.ownWeaknesses },
      { key: 'opponent-strengths', label: 'Stärken des Gegners',   value: a.opponentStrengths },
      { key: 'opponent-weaknesses',label: 'Schwächen des Gegners', value: a.opponentWeaknesses },
    ].filter(s => !!s.value);
  });

  readonly recommendations = computed(() => {
    const a = this.analysis();
    return a ? [...a.recommendations].sort((x, y) => x.priority - y.priority) : [];
  });

  /** Either a request error, or a persisted analysis that failed to generate. */
  readonly displayError = computed(() => {
    if (this.error()) return this.error();
    const a = this.analysis();
    return a?.status === 'FAILED' ? (a.errorMessage ?? 'Analyse fehlgeschlagen.') : null;
  });

  ngOnInit() {
    this.matchId = this.route.snapshot.paramMap.get('id') ?? '';
    this.p1Name.set(this.route.snapshot.queryParamMap.get('p1') ?? 'Spieler 1');
    this.p2Name.set(this.route.snapshot.queryParamMap.get('p2') ?? 'Spieler 2');
    this.loadExisting();
  }

  /** Fetch a previously generated analysis; a 404 simply means none exists yet. */
  private loadExisting() {
    this.loading.set(true);
    this.error.set(null);
    this.api.getMatchAnalysis(this.matchId).subscribe({
      next: a => { this.analysis.set(a); this.loading.set(false); },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        if (err.status === 404) {
          this.analysis.set(null);
        } else {
          this.error.set(this.messageFor(err));
        }
      },
    });
  }

  generate() {
    this.generating.set(true);
    this.error.set(null);
    this.api.generateMatchAnalysis(this.matchId).subscribe({
      next: a => { this.analysis.set(a); this.generating.set(false); },
      error: (err: HttpErrorResponse) => {
        this.generating.set(false);
        this.error.set(this.messageFor(err));
      },
    });
  }

  private messageFor(err: HttpErrorResponse): string {
    const detail: string | undefined = err.error?.detail ?? err.error?.errorMessage;
    switch (err.status) {
      case 404: return 'Match nicht gefunden.';
      case 409: return 'Match noch nicht beendet.';
      case 422: return 'Zu wenig Punkte erfasst (mindestens 10).';
      case 502: return 'LLM-Fehler, bitte erneut versuchen.' + (detail ? ` (${detail})` : '');
      default:  return 'Analyse fehlgeschlagen, bitte erneut versuchen.';
    }
  }

  goBack() {
    this.router.navigate(['/players']);
  }
}
