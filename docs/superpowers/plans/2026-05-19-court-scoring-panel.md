# Court Scoring Panel – Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ersetze das dunkle Panel-Layout im `ScoreComponent` durch eine vollflächige US-Open-Tennisplatz-Visualisierung mit Doppelfeld-Linien, wobei die Scoring-Panels beider Spieler direkt innerhalb der jeweiligen Feldhälfte liegen.

**Architecture:** Nur `score.component.ts` ändert sich — Template-Struktur und Styles werden vollständig ersetzt. Die TypeScript-Logik (`recordPoint`, `setServe`, Signals) bleibt identisch. Kein Backend-Change. Die Spec liegt unter `docs/superpowers/specs/2026-05-19-court-scoring-panel-design.md`. Linear-Issue: TEN-32.

**Tech Stack:** Angular 21 · Angular Signals · CSS absolute positioning · `@media (orientation: portrait)`

---

## Dateiübersicht

| Aktion | Pfad |
|--------|------|
| Ändern | `frontend/src/app/features/matches/score/score.component.ts` |
| Ändern | `frontend/src/app/features/matches/score/score.component.spec.ts` |

---

## Task 1: Failing Specs für Court-Layout schreiben (TDD)

**Files:**
- Modify: `frontend/src/app/features/matches/score/score.component.spec.ts`

Das Projekt nutzt **Vitest** (nicht Karma/Jasmine). Bestehende Tests verwenden `vi.fn()`. Neue Tests folgen demselben Muster.

- [ ] **Step 1: `By` importieren und 2 neue Tests hinzufügen**

Öffne `frontend/src/app/features/matches/score/score.component.spec.ts`. Füge folgende Zeile zu den bestehenden Imports hinzu:

```typescript
import { By } from '@angular/platform-browser';
```

Füge dann **am Ende des `describe`-Blocks** (vor dem letzten `}`), nach dem letzten `it(...)`, diese zwei Tests hinzu:

```typescript
  it('should render the tennis court container', () => {
    const court = fixture.debugElement.query(By.css('.tennis-court'));
    expect(court).toBeTruthy();
  });

  it('should render both player panels regardless of serving state', () => {
    component.matchData.set({ ...MOCK_MATCH, score: { ...MOCK_SCORE, servingPlayer: 1 } as any });
    fixture.detectChanges();
    const panels = fixture.debugElement.queryAll(By.css('.player-panel'));
    expect(panels).toHaveLength(2);
  });
```

- [ ] **Step 2: Tests ausführen — müssen fehlschlagen**

```bash
cd /pfad/zum/projekt/frontend
npx ng test --include='**/score.component.spec.ts' --watch=false --browsers=ChromeHeadless 2>&1 | tail -20
```

Erwartetes Ergebnis: `FAILED` — `.tennis-court` und `.player-panel` im aktuellen Template nicht vorhanden (oder es gibt 0 player-panel-Elemente weil die Panels im `@if`-Block sind und kein `.tennis-court` existiert).

- [ ] **Step 3: Commit (nur Spec)**

```bash
git add frontend/src/app/features/matches/score/score.component.spec.ts
git commit -m "test(score): add failing specs for court layout"
```

---

## Task 2: ScoreComponent — Template + Styles ersetzen

**Files:**
- Modify: `frontend/src/app/features/matches/score/score.component.ts`

Ersetze den gesamten Inhalt der Datei mit folgendem Code. **Wichtig:** Die TypeScript-Klasse (ab `export class ScoreComponent`) ist identisch mit der aktuellen Implementierung — nur Template und Styles ändern sich.

- [ ] **Step 1: score.component.ts vollständig ersetzen**

```typescript
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
import { EndMatchDialogComponent, EndMatchDialogResult } from './end-match-dialog.component';
import { PointType, StrokeType, Direction, RecordPointRequest } from '../../../core/models/point.model';

@Component({
  selector: 'app-score',
  standalone: true,
  imports: [
    CommonModule,
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
              <span class="strip-sub">G:{{ matchData()!.score.gamesPlayer1 }} S:{{ matchData()!.score.setsPlayer1 }}</span>
            }
          </div>
          <span class="strip-sep">:</span>
          <div class="strip-player">
            @if (matchData()) {
              <span class="strip-pts">{{ formatPoints(matchData()!.score, false) }}</span>
              <span class="strip-sub">G:{{ matchData()!.score.gamesPlayer2 }} S:{{ matchData()!.score.setsPlayer2 }}</span>
            }
            <span class="strip-name">{{ player2Name() }}</span>
            @if (servingPlayer() === 2) { <span class="serve-ball">🎾</span> }
          </div>
        </div>
        <div class="strip-actions">
          <button mat-icon-button (click)="openEditDialog()">
            <mat-icon>edit</mat-icon>
          </button>
          @if (matchData() && matchData()!.status !== 'COMPLETED') {
            <button mat-mini-fab color="warn" (click)="endMatch()">
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

            <!-- Player 1 panel (left half) -->
            <div class="player-panel player-panel-left" [class.serving]="servingPlayer() === 1">
              <div class="panel-header">
                <span class="panel-name">{{ player1Name() }}</span>
                @if (servingPlayer() === null) {
                  <span class="serve-hint">Tippen = Aufschlag setzen</span>
                }
              </div>
              <div class="point-grid">
                @for (pt of pointTypes; track pt.value) {
                  <button class="tile" (click)="recordPoint(1, pt.value)">
                    <span class="tile-icon">{{ pt.icon }}</span>
                    <span class="tile-label">{{ pt.label }}</span>
                  </button>
                }
                <div class="tile-empty"></div>
              </div>
              <div class="pill-row">
                <span class="pill-label">Schlagart</span>
                @for (st of strokeTypes; track st.value) {
                  <button class="pill" [class.active]="strokeTypeP1() === st.value"
                          (click)="strokeTypeP1.set(st.value)">{{ st.label }}</button>
                }
              </div>
              <div class="pill-row">
                <span class="pill-label">Richtung</span>
                @for (d of directions; track d.value) {
                  <button class="pill" [class.active]="directionP1() === d.value"
                          (click)="directionP1.set(d.value)">{{ d.label }}</button>
                }
              </div>
            </div>

            <!-- Player 2 panel (right half) -->
            <div class="player-panel player-panel-right" [class.serving]="servingPlayer() === 2">
              <div class="panel-header">
                <span class="panel-name">{{ player2Name() }}</span>
                @if (servingPlayer() === null) {
                  <span class="serve-hint">Tippen = Aufschlag setzen</span>
                }
              </div>
              <div class="point-grid">
                @for (pt of pointTypes; track pt.value) {
                  <button class="tile" (click)="recordPoint(2, pt.value)">
                    <span class="tile-icon">{{ pt.icon }}</span>
                    <span class="tile-label">{{ pt.label }}</span>
                  </button>
                }
                <div class="tile-empty"></div>
              </div>
              <div class="pill-row">
                <span class="pill-label">Schlagart</span>
                @for (st of strokeTypes; track st.value) {
                  <button class="pill" [class.active]="strokeTypeP2() === st.value"
                          (click)="strokeTypeP2.set(st.value)">{{ st.label }}</button>
                }
              </div>
              <div class="pill-row">
                <span class="pill-label">Richtung</span>
                @for (d of directions; track d.value) {
                  <button class="pill" [class.active]="directionP2() === d.value"
                          (click)="directionP2.set(d.value)">{{ d.label }}</button>
                }
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
      background: #1a5276;
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
      border: 2px solid white;
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
      top: calc(8% + 4px);
      bottom: calc(8% + 4px);
      padding: 8px 10px;
      display: flex;
      flex-direction: column;
      z-index: 5;
    }

    /* Left panel: from left infield edge to net */
    .player-panel-left {
      left: calc(8% + 6px);
      right: calc(50% + 4px);
    }

    /* Right panel: from net to right infield edge */
    .player-panel-right {
      left: calc(50% + 4px);
      right: calc(8% + 6px);
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
        top: calc(8% + 4px);
        left: calc(8% + 6px);
        right: calc(8% + 6px);
        bottom: 50%;
      }
      .player-panel-right {
        top: 50%;
        left: calc(8% + 6px);
        right: calc(8% + 6px);
        bottom: calc(8% + 4px);
      }
    }

    /* ── Panel header ── */
    .panel-header {
      display: flex;
      align-items: center;
      gap: 6px;
      margin-bottom: 6px;
      flex-shrink: 0;
    }
    .panel-name {
      font-size: 10px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: .5px;
      color: rgba(255,255,255,.7);
    }
    /* Aufschläger: Name in Hellblau */
    .player-panel.serving .panel-name { color: #90caf9; }

    .serve-hint {
      font-size: 8px;
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
      background: rgba(255,255,255,.15);
      border: none;
      border-radius: 7px;
      padding: 7px 3px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 2px;
      cursor: pointer;
      color: white;
      transition: background .15s;
      min-height: 46px;
    }
    .tile:hover  { background: rgba(255,255,255,.25); }
    .tile:active { background: rgba(255,255,255,.35); }
    .tile-icon  { font-size: 15px; line-height: 1; }
    .tile-label { font-size: 8px; line-height: 1.2; text-align: center; }
    .tile-empty { /* grid placeholder */ }

    /* ── Pills ── */
    .pill-row {
      display: flex;
      align-items: center;
      gap: 3px;
      flex-wrap: wrap;
      margin-bottom: 4px;
      flex-shrink: 0;
    }
    .pill-label {
      font-size: 7px;
      text-transform: uppercase;
      letter-spacing: .5px;
      color: rgba(255,255,255,.35);
      min-width: 44px;
    }
    .pill {
      background: rgba(255,255,255,.15);
      border: none;
      border-radius: 20px;
      padding: 2px 8px;
      font-size: 8px;
      color: rgba(255,255,255,.7);
      cursor: pointer;
      transition: background .15s;
    }
    .pill:hover  { background: rgba(255,255,255,.25); }
    .pill.active { background: #1565c0; color: white; font-weight: 700; }

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

    .loading { text-align: center; padding: 48px; color: rgba(255,255,255,.5); }
  `]
})
export class ScoreComponent implements OnInit {
  private readonly api       = inject(ApiService);
  private readonly route     = inject(ActivatedRoute);
  private readonly router    = inject(Router);
  private readonly dialog    = inject(MatDialog);
  private readonly snackBar  = inject(MatSnackBar);

  matchData = signal<MatchWithScore | null>(null);
  player1   = signal<Player | null>(null);
  player2   = signal<Player | null>(null);

  strokeTypeP1 = signal<StrokeType>('FOREHAND');
  strokeTypeP2 = signal<StrokeType>('FOREHAND');
  directionP1  = signal<Direction>('CROSS_COURT');
  directionP2  = signal<Direction>('CROSS_COURT');

  readonly pointTypes: { value: PointType; icon: string; label: string }[] = [
    { value: 'WINNER',         icon: '🏆', label: 'Winner'    },
    { value: 'UNFORCED_ERROR', icon: '😓', label: 'Eigenf.'   },
    { value: 'FORCED_ERROR',   icon: '💨', label: 'Erz. F.'   },
    { value: 'ACE',            icon: '🎯', label: 'Ass'       },
    { value: 'DOUBLE_FAULT',   icon: '❌', label: 'DF'        },
    { value: 'NET',            icon: '🔴', label: 'Netz'      },
    { value: 'OUT_LONG',       icon: '↑',  label: 'Aus lang'  },
    { value: 'OUT_SIDE',       icon: '→',  label: 'Aus Seite' },
  ];

  readonly strokeTypes: { value: StrokeType; label: string }[] = [
    { value: 'FOREHAND', label: 'FH'        },
    { value: 'BACKHAND', label: 'RH'        },
    { value: 'SERVE',    label: 'Aufschlag' },
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

  goToMatches() {
    this.router.navigate(['/players']);
  }
}
```

- [ ] **Step 2: Tests ausführen — alle 10 Specs müssen grün sein**

```bash
cd /pfad/zum/projekt/frontend
npx ng test --include='**/score.component.spec.ts' --watch=false --browsers=ChromeHeadless 2>&1 | tail -20
```

Erwartetes Ergebnis: `10 specs, 0 failures`

Wenn Tests fehlschlagen: Fehlermeldung lesen. Häufige Ursache: Die MOCK_MATCH-Daten stimmen nicht mit dem Template überein (z.B. `matchData()` gibt null zurück). In dem Fall sicherstellen, dass `fixture.detectChanges()` nach `matchData.set(...)` aufgerufen wird.

- [ ] **Step 3: Build prüfen**

```bash
cd /pfad/zum/projekt/frontend
npx ng build --configuration development 2>&1 | grep -i error
```

Erwartetes Ergebnis: keine Ausgabe (kein Fehler).

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/features/matches/score/score.component.ts
git commit -m "feat(score): US Open court layout with doubles lines [TEN-32]"
```

---

## Task 3: Manueller Smoke-Test

- [ ] **Step 1: App starten**

```bash
# Terminal 1 — Backend
cd /pfad/zum/projekt/backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew bootRun --args='--spring.profiles.active=local'

# Terminal 2 — Frontend
cd /pfad/zum/projekt/frontend
npm start
```

- [ ] **Step 2: Flow prüfen**

1. `https://localhost:4200` öffnen, einloggen
2. Match öffnen → Score-Screen
3. Verifizieren: blauer Tennisplatz mit weißen Linien sichtbar (Außenfeld dunkelblau, Infield heller)
4. Doppelfeld-Linien sichtbar: Singles-Seitenlinien (horizontal oben/unten), Service-Linien (vertikal je Hälfte), Center-Servicelinie (horizontal, Netz bis Servicelinie)
5. Beide Spieler-Panels im Feld — gleich hell, kein Dimming
6. Erste Kachel → Aufschlag setzen, 🎾 im Score-Streifen
7. Zweite Kachel → Punkt gespeichert, Score aktualisiert
8. Pills wechseln korrekt

- [ ] **Step 3: Push**

```bash
git push
```
