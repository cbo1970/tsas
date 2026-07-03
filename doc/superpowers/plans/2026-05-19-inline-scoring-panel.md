# Inline Scoring Panel – Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ersetze den Punkt-Erfassungs-Dialog durch ein inline Scoring-Panel das im Querformat dauerhaft auf dem Score-Screen sichtbar ist — ein Klick auf eine Punkttyp-Kachel speichert den Punkt sofort.

**Architecture:** `ScoreComponent` wird vollständig überarbeitet: Tennisplatz-Visualisierung entfällt, stattdessen ein kompakter Score-Streifen oben und zwei nebeneinander liegende Spieler-Panels mit Punkttyp-Kacheln und Pill-Vorauswahl für Schlagart/Richtung. `RecordPointDialogComponent` wird gelöscht. Keine Backend-Änderungen.

**Tech Stack:** Angular 21 · Angular Material · Angular Signals · CSS `@media (orientation)`

---

## Dateiübersicht

| Aktion | Pfad |
|--------|------|
| Neu | `frontend/src/app/features/matches/score/score.component.spec.ts` |
| Ändern | `frontend/src/app/features/matches/score/score.component.ts` |
| Löschen | `frontend/src/app/features/matches/score/record-point-dialog.component.ts` |

---

## Task 1: Component-Spec schreiben (TDD)

**Files:**
- Create: `frontend/src/app/features/matches/score/score.component.spec.ts`

- [ ] **Step 1: Spec-Datei anlegen**

```typescript
// frontend/src/app/features/matches/score/score.component.spec.ts
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { ScoreComponent } from './score.component';
import { ApiService } from '../../../core/services/api.service';
import { MatchWithScore } from '../../../core/models/match.model';

const MOCK_SCORE = {
  matchId: 'match-1',
  pointsPlayer1: 0, pointsPlayer2: 0,
  gamesPlayer1: 0, gamesPlayer2: 0,
  setsPlayer1: 0, setsPlayer2: 0,
  isDeuce: false, isAdvantagePlayer1: null,
  currentSet: 1, isDone: false, winner: null,
  acesPlayer1: 0, acesPlayer2: 0,
  servingPlayer: 2,
};

const MOCK_MATCH: MatchWithScore = {
  id: 'match-1', player1Id: 'p1', player2Id: 'p2',
  setsToWin: 2, matchTiebreak: false, shortSet: false,
  status: 'IN_PROGRESS', score: MOCK_SCORE as any,
};

describe('ScoreComponent — inline scoring', () => {
  let fixture: ComponentFixture<ScoreComponent>;
  let component: ScoreComponent;
  let mockApi: jasmine.SpyObj<ApiService>;
  let mockDialog: jasmine.SpyObj<MatDialog>;
  let mockSnackBar: jasmine.SpyObj<MatSnackBar>;
  let mockRouter: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    mockApi = jasmine.createSpyObj('ApiService', [
      'getMatch', 'getPlayer', 'recordPoint',
      'setServingPlayer1', 'setServingPlayer2',
      'setScore', 'endMatchWalkover',
    ]);
    mockDialog = jasmine.createSpyObj('MatDialog', ['open']);
    mockSnackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

    mockApi.getMatch.and.returnValue(of(MOCK_MATCH));
    mockApi.getPlayer.and.returnValue(of({ id: 'p1', firstName: 'Anna', lastName: 'Müller' } as any));

    await TestBed.configureTestingModule({
      imports: [ScoreComponent],
      providers: [
        { provide: ApiService,     useValue: mockApi },
        { provide: MatDialog,      useValue: mockDialog },
        { provide: MatSnackBar,    useValue: mockSnackBar },
        { provide: Router,         useValue: mockRouter },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'match-1' } } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ScoreComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should default strokeType to FOREHAND for both players', () => {
    expect(component.strokeTypeP1()).toBe('FOREHAND');
    expect(component.strokeTypeP2()).toBe('FOREHAND');
  });

  it('should default direction to CROSS_COURT for both players', () => {
    expect(component.directionP1()).toBe('CROSS_COURT');
    expect(component.directionP2()).toBe('CROSS_COURT');
  });

  it('should call api.recordPoint directly without opening a dialog', () => {
    mockApi.recordPoint.and.returnValue(of(MOCK_MATCH));
    component.recordPoint(1, 'WINNER');
    expect(mockDialog.open).not.toHaveBeenCalled();
    expect(mockApi.recordPoint).toHaveBeenCalledWith('match-1', {
      winner: 1, pointType: 'WINNER',
      strokeType: 'FOREHAND', direction: 'CROSS_COURT',
    });
  });

  it('should send player-specific pre-selection with the point', () => {
    mockApi.recordPoint.and.returnValue(of(MOCK_MATCH));
    component.strokeTypeP1.set('BACKHAND');
    component.directionP1.set('DOWN_THE_LINE');
    component.recordPoint(1, 'WINNER');
    expect(mockApi.recordPoint).toHaveBeenCalledWith('match-1', {
      winner: 1, pointType: 'WINNER',
      strokeType: 'BACKHAND', direction: 'DOWN_THE_LINE',
    });
  });

  it('should set serving player on first tile click when no server set', () => {
    component.matchData.set({ ...MOCK_MATCH, score: { ...MOCK_SCORE, servingPlayer: null } as any });
    mockApi.setServingPlayer1.and.returnValue(of({ ...MOCK_SCORE, servingPlayer: 1 } as any));
    component.recordPoint(1, 'WINNER');
    expect(mockApi.setServingPlayer1).toHaveBeenCalled();
    expect(mockApi.recordPoint).not.toHaveBeenCalled();
  });
});
```

- [ ] **Step 2: Test ausführen — muss fehlschlagen**

```bash
cd frontend
npx ng test --include='**/score.component.spec.ts' --watch=false --browsers=ChromeHeadless
```

Erwartetes Ergebnis: Kompilierungsfehler (`recordPoint`, `strokeTypeP1` etc. nicht gefunden) oder `FAILED` — die Implementierung existiert noch nicht.

- [ ] **Step 3: Commit (nur Spec)**

```bash
git add frontend/src/app/features/matches/score/score.component.spec.ts
git commit -m "test(score): add failing specs for inline scoring panel"
```

---

## Task 2: ScoreComponent refaktorieren

**Files:**
- Modify: `frontend/src/app/features/matches/score/score.component.ts`

- [ ] **Step 1: score.component.ts vollständig ersetzen**

Ersetze den gesamten Inhalt von `frontend/src/app/features/matches/score/score.component.ts` mit:

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
      @if (matchData()) {

        <div class="score-strip">
          <button mat-icon-button (click)="goToMatches()">
            <mat-icon>arrow_back</mat-icon>
          </button>
          <div class="strip-center">
            <div class="strip-player">
              @if (servingPlayer() === 1) { <span>🎾 </span> }
              <span class="strip-name">{{ player1Name() }}</span>
              <span class="strip-pts">{{ formatPoints(matchData()!.score, true) }}</span>
              <span class="strip-sub">G:{{ matchData()!.score.gamesPlayer1 }} S:{{ matchData()!.score.setsPlayer1 }}</span>
            </div>
            <span class="strip-sep">:</span>
            <div class="strip-player">
              @if (servingPlayer() === 2) { <span>🎾 </span> }
              <span class="strip-name">{{ player2Name() }}</span>
              <span class="strip-pts">{{ formatPoints(matchData()!.score, false) }}</span>
              <span class="strip-sub">G:{{ matchData()!.score.gamesPlayer2 }} S:{{ matchData()!.score.setsPlayer2 }}</span>
            </div>
          </div>
          <div class="strip-actions">
            <button mat-icon-button (click)="openEditDialog()">
              <mat-icon>edit</mat-icon>
            </button>
            @if (matchData()!.status !== 'COMPLETED') {
              <button mat-mini-fab color="warn" (click)="endMatch()">
                <mat-icon>stop</mat-icon>
              </button>
            }
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

        @if (matchData()!.status !== 'COMPLETED') {
          <div class="panels">

            <div class="player-panel" [class.serving]="servingPlayer() === 1">
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

            <div class="panel-divider"></div>

            <div class="player-panel" [class.serving]="servingPlayer() === 2">
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

          </div>
        }

      } @else {
        <div class="loading">Lade Match...</div>
      }
    </div>
  `,
  styles: [`
    .scoring-page {
      display: flex;
      flex-direction: column;
      height: 100dvh;
      background: #0d1b2a;
      color: white;
      overflow: hidden;
    }

    /* ── Score strip ── */
    .score-strip {
      display: flex;
      align-items: center;
      background: #1a2e4a;
      padding: 6px 12px;
      border-bottom: 1px solid rgba(255,255,255,.1);
      flex-shrink: 0;
      gap: 8px;
    }
    .strip-center {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 16px;
    }
    .strip-player { display: flex; align-items: baseline; gap: 6px; }
    .strip-name   { font-size: 13px; font-weight: 600; }
    .strip-pts    { font-size: 22px; font-weight: 800; line-height: 1; }
    .strip-sub    { font-size: 10px; opacity: .5; }
    .strip-sep    { font-size: 20px; opacity: .3; }
    .strip-actions { display: flex; align-items: center; gap: 4px; }

    /* ── Winner card ── */
    .winner-card { margin: 16px; background: linear-gradient(135deg, #1a237e, #283593); color: white; }
    .winner-text { display: flex; align-items: center; gap: 8px; font-size: 20px; font-weight: bold; margin-bottom: 8px; }
    .final-score { font-size: 15px; margin-bottom: 16px; }

    /* ── Panels ── */
    .panels {
      flex: 1;
      display: flex;
      flex-direction: row;
      overflow: hidden;
    }
    @media (orientation: portrait) {
      .panels { flex-direction: column; }
    }

    .panel-divider {
      width: 1px;
      background: rgba(255,255,255,.08);
      flex-shrink: 0;
    }
    @media (orientation: portrait) {
      .panel-divider { width: auto; height: 1px; }
    }

    .player-panel {
      flex: 1;
      padding: 10px 12px;
      overflow-y: auto;
      opacity: .75;
      transition: opacity .2s;
    }
    .player-panel.serving { opacity: 1; }

    /* ── Panel header ── */
    .panel-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
    .panel-name {
      font-size: 11px; font-weight: 700;
      text-transform: uppercase; letter-spacing: .5px;
      color: rgba(255,255,255,.6);
    }
    .player-panel.serving .panel-name { color: #64b5f6; }
    .serve-hint { font-size: 9px; color: rgba(255,255,255,.35); font-style: italic; }

    /* ── Point type grid ── */
    .point-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 5px;
      margin-bottom: 10px;
    }
    .tile {
      background: rgba(255,255,255,.08);
      border: none;
      border-radius: 8px;
      padding: 8px 4px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 3px;
      cursor: pointer;
      color: white;
      transition: background .15s;
      min-height: 52px;
    }
    .tile:hover  { background: rgba(255,255,255,.16); }
    .tile:active { background: rgba(255,255,255,.24); }
    .tile-icon  { font-size: 16px; line-height: 1; }
    .tile-label { font-size: 9px; line-height: 1.2; text-align: center; }
    .tile-empty { /* grid placeholder */ }

    /* ── Pills ── */
    .pill-row {
      display: flex;
      align-items: center;
      gap: 4px;
      flex-wrap: wrap;
      margin-bottom: 6px;
    }
    .pill-label {
      font-size: 7px; text-transform: uppercase;
      letter-spacing: .5px; color: rgba(255,255,255,.3);
      min-width: 48px;
    }
    .pill {
      background: rgba(255,255,255,.08);
      border: none; border-radius: 20px;
      padding: 3px 10px; font-size: 9px;
      color: rgba(255,255,255,.6); cursor: pointer;
      transition: background .15s;
    }
    .pill:hover { background: rgba(255,255,255,.16); }
    .pill.active { background: #1565c0; color: white; font-weight: 700; }

    .loading { text-align: center; padding: 48px; color: #666; }
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
    { value: 'WINNER',        icon: '🏆', label: 'Winner'   },
    { value: 'UNFORCED_ERROR',icon: '😓', label: 'Eigenf.'  },
    { value: 'FORCED_ERROR',  icon: '💨', label: 'Erz. F.'  },
    { value: 'ACE',           icon: '🎯', label: 'Ass'      },
    { value: 'DOUBLE_FAULT',  icon: '❌', label: 'DF'       },
    { value: 'NET',           icon: '🔴', label: 'Netz'     },
    { value: 'OUT_LONG',      icon: '↑',  label: 'Aus lang' },
    { value: 'OUT_SIDE',      icon: '→',  label: 'Aus Seite'},
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

- [ ] **Step 2: Tests ausführen — müssen jetzt grün sein**

```bash
cd frontend
npx ng test --include='**/score.component.spec.ts' --watch=false --browsers=ChromeHeadless
```

Erwartetes Ergebnis: `5 specs, 0 failures`

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/features/matches/score/score.component.ts
git commit -m "feat(score): replace dialog with inline scoring panel"
```

---

## Task 3: RecordPointDialogComponent löschen

**Files:**
- Delete: `frontend/src/app/features/matches/score/record-point-dialog.component.ts`

- [ ] **Step 1: Datei löschen**

```bash
rm frontend/src/app/features/matches/score/record-point-dialog.component.ts
```

- [ ] **Step 2: Build prüfen — kein Import mehr auf die gelöschte Datei**

```bash
cd frontend
npx ng build --configuration development 2>&1 | grep -i error
```

Erwartetes Ergebnis: keine Ausgabe (kein Fehler). Falls ein Fehler erscheint, suche nach weiteren Import-Stellen:

```bash
grep -r "record-point-dialog" frontend/src --include="*.ts"
```

Jede gefundene Import-Zeile entfernen.

- [ ] **Step 3: Alle Frontend-Tests ausführen**

```bash
cd frontend
npx ng test --watch=false --browsers=ChromeHeadless
```

Erwartetes Ergebnis: alle Specs grün.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(score): delete RecordPointDialogComponent"
```

---

## Task 4: Manueller Smoke-Test

- [ ] **Step 1: Frontend und Backend starten**

```bash
# Terminal 1 — Backend (aus tsas/backend)
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew bootRun --args='--spring.profiles.active=local'

# Terminal 2 — Frontend (aus tsas/frontend)
npm start
```

- [ ] **Step 2: Flow prüfen**

1. Öffne `https://localhost:4200`
2. Navigiere zu einem laufenden Match (oder erstelle eines)
3. Score-Screen im Querformat öffnen
4. Verifiziere: Score-Streifen oben, zwei Spieler-Panels nebeneinander
5. Tippe auf eine Kachel beim Spieler ohne Aufschlag → Aufschlag wird gesetzt, kein Punkt erfasst
6. Tippe auf eine Kachel beim aufschlagenden Spieler → Punkt wird sofort gespeichert, Score-Streifen aktualisiert sich
7. Schlagart- und Richtungs-Pills wechseln korrekt
8. „Score korrigieren" und „Match beenden" funktionieren noch

- [ ] **Step 3: Push**

```bash
git push
```
