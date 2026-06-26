import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { ApiService } from '../../../core/services/api.service';
import { MatchHistoryEntry } from '../../../core/models/match.model';

@Component({
  selector: 'app-match-history',
  standalone: true,
  imports: [MatButtonModule, DatePipe],
  templateUrl: './match-history.component.html',
  styles: [`
    :host { display: block; min-height: 100dvh; background: var(--surface-bg); color: var(--text); }
    .page { max-width: 560px; margin: 0 auto; padding: 16px; }
    .title { text-align: center; font-weight: 700; font-size: 18px; margin: 4px 0 12px; }
    .entry {
      display: flex; align-items: center; gap: 12px; cursor: pointer;
      background: var(--surface-card); border: 1px solid var(--text); border-radius: 10px;
      padding: 12px 14px; margin-bottom: 8px;
    }
    .opp { flex: 1; font-weight: 600; }
    .result { font-weight: 700; border-radius: var(--radius-pill); padding: 2px 10px; font-size: 13px; color: #fff; }
    .result.won { background: var(--success); }
    .result.lost { background: var(--danger); }
    .date { font-size: 12px; color: var(--text-muted); }
    .empty { text-align: center; color: var(--text-muted); padding: 40px 8px; }
    .back-row { text-align: center; margin-top: 20px; }
  `],
})
export class MatchHistoryComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(ApiService);

  entries = signal<MatchHistoryEntry[]>([]);
  loaded = signal(false);
  playerName = signal('');

  private playerId = '';

  ngOnInit() {
    this.playerId = this.route.snapshot.paramMap.get('id') ?? '';
    this.api.getPlayer(this.playerId).subscribe(p => this.playerName.set(`${p.firstName} ${p.lastName}`));
    this.api.getPlayerMatchHistory(this.playerId).subscribe({
      next: e => { this.entries.set(e); this.loaded.set(true); },
      error: () => this.loaded.set(true),
    });
  }

  openStatistics(matchId: string) {
    this.router.navigate(['/matches', matchId, 'statistics']);
  }

  goBack() {
    this.router.navigate(['/players']);
  }
}
