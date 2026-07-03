# TEN-33 Scoring UI Redesign — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the ScoreComponent with a centred score header (games above points, both clickable for quick entry), two side-by-side observation panels per player with service context, win/error buttons, and stroke-type toggles.

**Architecture:** Backend removes the `@NotBlank` constraint from `pointType` and adds a V5 migration to make the DB column nullable. Frontend rewrites `score.component.ts/.html` around new signals (`serviceContextP1/P2`, `strokeTypeP1/P2`) and two new methods (`recordQuickPoint`, `recordObservation`). Existing `serveAttempt` field on the backend and `setServe` call-path are reused as-is.

**Tech Stack:** Angular 19 (standalone, signals), Angular Material (MatSnackBar), Spring Boot 4, Flyway, JUnit 5 integration tests, Cypress component tests.

---

## File Map

| Action | Path |
|--------|------|
| Create | `backend/app/src/main/resources/db/migration/V5__make_point_type_nullable.sql` |
| Modify | `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/dto/request/RecordPointRequest.java` |
| Modify | `backend/app/src/test/java/com/cas/tsas/match/MatchApiIT.java` (add 2 tests to `RecordPoint`) |
| Modify | `frontend/src/app/core/models/point.model.ts` |
| Modify | `frontend/src/app/features/matches/score/score.component.ts` |
| Modify | `frontend/src/app/features/matches/score/score.component.html` |
| Modify | `frontend/src/app/features/matches/score/score.component.cy.ts` |

---

## Task 1: V5 Migration — make `point_type` nullable

**Files:**
- Create: `backend/app/src/main/resources/db/migration/V5__make_point_type_nullable.sql`

- [ ] **Step 1: Write the failing backend IT test**

Open `backend/app/src/test/java/com/cas/tsas/match/MatchApiIT.java`.  
Inside the existing `RecordPoint` nested class (after the `ace_increments_ace_counter` test), add:

```java
@Test
void records_point_without_point_type() throws Exception {
    UUID p1 = createPlayer(); UUID p2 = createPlayer();
    UUID matchId = createMatch(p1, p2);

    mockMvc.perform(post("/api/matches/{id}/points", matchId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("winner", 1))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.score.pointsPlayer1").value(1));
}

@Test
void records_point_with_serve_attempt() throws Exception {
    UUID p1 = createPlayer(); UUID p2 = createPlayer();
    UUID matchId = createMatch(p1, p2);
    mockMvc.perform(post("/api/matches/{id}/serve/player1", matchId));

    mockMvc.perform(post("/api/matches/{id}/points", matchId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                            "winner", 1, "serveAttempt", 1
                    ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.score.pointsPlayer1").value(1));
}
```

- [ ] **Step 2: Run the new tests — expect failure**

```bash
cd backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test \
  --tests "com.cas.tsas.match.MatchApiIT\$RecordPoint.records_point_without_point_type" \
  --tests "com.cas.tsas.match.MatchApiIT\$RecordPoint.records_point_with_serve_attempt" \
  -i 2>&1 | tail -30
```

Expected: `records_point_without_point_type` fails with 400 (validation error on blank `pointType`).  
`records_point_with_serve_attempt` may pass already — note whether it does.

- [ ] **Step 3: Remove `@NotBlank` from `RecordPointRequest`**

Replace the entire file `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/dto/request/RecordPointRequest.java`:

```java
package com.cas.tsas.match.infrastructure.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record RecordPointRequest(
        @NotNull @Min(1) @Max(2) Integer winner,
        String pointType,
        String strokeType,
        String direction,
        @Length(max = 500) String remark,
        @Min(1) @Max(2) Integer serveAttempt
) {}
```

(Remove the `import jakarta.validation.constraints.NotBlank;` line and the `@NotBlank` annotation from `pointType`.)

- [ ] **Step 4: Create the V5 Flyway migration**

Create `backend/app/src/main/resources/db/migration/V5__make_point_type_nullable.sql`:

```sql
ALTER TABLE points ALTER COLUMN point_type DROP NOT NULL;
```

- [ ] **Step 5: Run the tests — expect both to pass**

```bash
cd backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test \
  --tests "com.cas.tsas.match.MatchApiIT\$RecordPoint.records_point_without_point_type" \
  --tests "com.cas.tsas.match.MatchApiIT\$RecordPoint.records_point_with_serve_attempt" \
  -i 2>&1 | tail -20
```

Expected: both tests `PASS`.

- [ ] **Step 6: Run full backend test suite**

```bash
cd backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL` — no regressions.

- [ ] **Step 7: Commit**

```bash
git add \
  backend/app/src/main/resources/db/migration/V5__make_point_type_nullable.sql \
  backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/dto/request/RecordPointRequest.java \
  backend/app/src/test/java/com/cas/tsas/match/MatchApiIT.java
git commit -m "feat(score): make pointType optional in API and DB [TEN-33]"
```

---

## Task 2: Update Frontend Type Model

**Files:**
- Modify: `frontend/src/app/core/models/point.model.ts`

- [ ] **Step 1: Replace the file contents**

```typescript
export type PointType =
  | 'WINNER' | 'UNFORCED_ERROR' | 'FORCED_ERROR'
  | 'ACE' | 'DOUBLE_FAULT' | 'NET';

export type StrokeType = 'FOREHAND' | 'BACKHAND';

export interface RecordPointRequest {
  winner: 1 | 2;
  pointType?: PointType | null;
  strokeType?: StrokeType | null;
  serveAttempt?: 1 | 2 | null;
  remark?: string;
}
```

Changes vs before:
- `OUT_LONG`, `OUT_SIDE` removed from `PointType`
- `StrokeType` reduced to `FOREHAND | BACKHAND` (removes SERVE, VOLLEY, SMASH)
- `Direction` type deleted entirely
- `pointType` is now optional
- `serveAttempt?: 1 | 2 | null` added
- `direction` field removed from `RecordPointRequest`

- [ ] **Step 2: Verify TypeScript compiles**

```bash
cd frontend
npx tsc --noEmit 2>&1 | head -40
```

Expected: errors only in `score.component.ts` (which we'll fix in Task 3). No errors in any other file.

If errors appear in other files (e.g. referencing `OUT_LONG`, `Direction`, `VOLLEY`), fix them now before committing.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/core/models/point.model.ts
git commit -m "feat(score): trim point model — remove OUT_LONG/SIDE, Direction, Volley/Smash [TEN-33]"
```

---

## Task 3: Rewrite ScoreComponent TypeScript

**Files:**
- Modify: `frontend/src/app/features/matches/score/score.component.ts`

- [ ] **Step 1: Replace the component class**

Replace the entire file with:

```typescript
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
      const p1 = prev.score.gamesPlayer1;
      const p2 = prev.score.gamesPlayer2;
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
```

- [ ] **Step 2: Verify TypeScript compiles**

```bash
cd frontend
npx tsc --noEmit 2>&1 | head -30
```

Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/features/matches/score/score.component.ts
git commit -m "feat(score): rewrite ScoreComponent TS — new signals, recordObservation, recordQuickPoint [TEN-33]"
```

---

## Task 4: Rewrite ScoreComponent HTML

**Files:**
- Modify: `frontend/src/app/features/matches/score/score.component.html`

- [ ] **Step 1: Replace the template**

```html
<div class="scoring-page">

  @if (!matchData()) {
    <div class="loading">Lade Match...</div>
  }

  @if (matchData()) {

    <!-- ── Score Header ── -->
    <div class="score-header">

      <!-- Row 1: Names + Games -->
      <div class="header-grid">
        <div class="player-half-left">
          <span class="player-name" [class.serving]="servingPlayer() === 1">
            @if (servingPlayer() === 1) { 🎾 } {{ player1Name() }}
          </span>
        </div>
        <div class="score-center">
          <span class="games-num" [class.winning]="matchData()!.score.setsPlayer1 > matchData()!.score.setsPlayer2">
            {{ matchData()!.score.gamesPlayer1 }}
          </span>
          <span class="sep">:</span>
          <span class="games-num" [class.winning]="matchData()!.score.setsPlayer2 > matchData()!.score.setsPlayer1">
            {{ matchData()!.score.gamesPlayer2 }}
          </span>
        </div>
        <div class="player-half-right">
          <span class="player-name" [class.serving]="servingPlayer() === 2">
            {{ player2Name() }} @if (servingPlayer() === 2) { 🎾 }
          </span>
        </div>
      </div>

      <!-- Row 2: Points (clickable) -->
      <div class="header-grid" style="margin-top:4px;">
        <div class="pts-label">PUNKTE</div>
        <div class="score-center">
          <button class="pts-btn p1"
                  data-testid="quick-score-p1"
                  (click)="recordQuickPoint(1)">
            {{ formatPoints(matchData()!.score, true) }}
          </button>
          <span class="sep" style="font-size:22px;">:</span>
          <button class="pts-btn"
                  data-testid="quick-score-p2"
                  (click)="recordQuickPoint(2)">
            {{ formatPoints(matchData()!.score, false) }}
          </button>
        </div>
        <div class="pts-label">PUNKTE</div>
      </div>

      <!-- Row 3: Set history -->
      <div class="set-history">{{ setHistoryText() }}</div>

      <!-- Actions -->
      <div class="header-actions">
        <button mat-icon-button (click)="goToMatches()">
          <mat-icon>arrow_back</mat-icon>
        </button>
        <button mat-icon-button data-testid="edit-score-btn" (click)="openEditDialog()">
          <mat-icon>edit</mat-icon>
        </button>
        @if (matchData()!.status !== 'COMPLETED') {
          <button mat-mini-fab color="warn" data-testid="end-match-btn" (click)="endMatch()">
            <mat-icon>stop</mat-icon>
          </button>
        }
      </div>
    </div>

    <!-- ── Observation Panels ── -->
    @if (matchData()!.status !== 'COMPLETED') {
      <div class="panels-area">

        <!-- ── Player 1 Panel ── -->
        <div class="obs-panel" [class.serving]="servingPlayer() === 1" data-testid="panel-p1">
          <div class="panel-title">
            {{ player1Name() }}
            @if (servingPlayer() === 1) {
              <span class="serve-badge">&nbsp;(Aufschlag)</span>
            }
          </div>

          <div class="section-label">SERVICE (Kontext)</div>
          <div class="ctx-row">
            <button class="ctx-btn"
                    [class.active]="serviceContextP1() === 1"
                    data-testid="p1-service-1"
                    (click)="toggleService(1, 1)">1. Service</button>
            <button class="ctx-btn"
                    [class.active]="serviceContextP1() === 2"
                    data-testid="p1-service-2"
                    (click)="toggleService(1, 2)">2. Service</button>
          </div>

          <div class="section-label green">→ PUNKT FÜR {{ player1Name() }}</div>
          <div class="obs-row-2">
            <button class="obs-btn win-btn"
                    data-testid="p1-ace"
                    (click)="recordObservation(1, 'ACE')">🎯 Ass</button>
            <button class="obs-btn win-btn"
                    data-testid="p1-winner"
                    (click)="recordObservation(1, 'WINNER')">🏆 Winner</button>
          </div>

          <div class="section-label red">→ PUNKT FÜR {{ player2Name() }}</div>
          <div class="obs-row-1">
            <button class="obs-btn err-btn"
                    data-testid="p1-df"
                    (click)="recordObservation(1, 'DOUBLE_FAULT')">❌ DF</button>
          </div>
          <div class="obs-row-3">
            <button class="obs-btn err-btn err-sm"
                    data-testid="p1-forced"
                    (click)="recordObservation(1, 'FORCED_ERROR')">💨 Forced</button>
            <button class="obs-btn err-btn err-sm"
                    data-testid="p1-unforced"
                    (click)="recordObservation(1, 'UNFORCED_ERROR')">😓 Unforced</button>
            <button class="obs-btn err-btn err-sm"
                    data-testid="p1-net"
                    (click)="recordObservation(1, 'NET')">🔴 Netz</button>
          </div>

          <div class="section-label">SCHLAGART (Kontext)</div>
          <div class="ctx-row">
            <button class="ctx-btn"
                    [class.active]="strokeTypeP1() === 'FOREHAND'"
                    (click)="toggleStroke(1, 'FOREHAND')">Vorhand</button>
            <button class="ctx-btn"
                    [class.active]="strokeTypeP1() === 'BACKHAND'"
                    (click)="toggleStroke(1, 'BACKHAND')">Rückhand</button>
          </div>
        </div>

        <!-- ── Player 2 Panel ── -->
        <div class="obs-panel" [class.serving]="servingPlayer() === 2" data-testid="panel-p2">
          <div class="panel-title">
            {{ player2Name() }}
            @if (servingPlayer() === 2) {
              <span class="serve-badge">&nbsp;(Aufschlag)</span>
            }
          </div>

          <div class="section-label">SERVICE (Kontext)</div>
          <div class="ctx-row">
            <button class="ctx-btn"
                    [class.active]="serviceContextP2() === 1"
                    data-testid="p2-service-1"
                    (click)="toggleService(2, 1)">1. Service</button>
            <button class="ctx-btn"
                    [class.active]="serviceContextP2() === 2"
                    data-testid="p2-service-2"
                    (click)="toggleService(2, 2)">2. Service</button>
          </div>

          <div class="section-label green">→ PUNKT FÜR {{ player2Name() }}</div>
          <div class="obs-row-2">
            <button class="obs-btn win-btn"
                    data-testid="p2-ace"
                    (click)="recordObservation(2, 'ACE')">🎯 Ass</button>
            <button class="obs-btn win-btn"
                    data-testid="p2-winner"
                    (click)="recordObservation(2, 'WINNER')">🏆 Winner</button>
          </div>

          <div class="section-label red">→ PUNKT FÜR {{ player1Name() }}</div>
          <div class="obs-row-1">
            <button class="obs-btn err-btn"
                    data-testid="p2-df"
                    (click)="recordObservation(2, 'DOUBLE_FAULT')">❌ DF</button>
          </div>
          <div class="obs-row-3">
            <button class="obs-btn err-btn err-sm"
                    data-testid="p2-forced"
                    (click)="recordObservation(2, 'FORCED_ERROR')">💨 Forced</button>
            <button class="obs-btn err-btn err-sm"
                    data-testid="p2-unforced"
                    (click)="recordObservation(2, 'UNFORCED_ERROR')">😓 Unforced</button>
            <button class="obs-btn err-btn err-sm"
                    data-testid="p2-net"
                    (click)="recordObservation(2, 'NET')">🔴 Netz</button>
          </div>

          <div class="section-label">SCHLAGART (Kontext)</div>
          <div class="ctx-row">
            <button class="ctx-btn"
                    [class.active]="strokeTypeP2() === 'FOREHAND'"
                    (click)="toggleStroke(2, 'FOREHAND')">Vorhand</button>
            <button class="ctx-btn"
                    [class.active]="strokeTypeP2() === 'BACKHAND'"
                    (click)="toggleStroke(2, 'BACKHAND')">Rückhand</button>
          </div>
        </div>

      </div>
    }

    <!-- ── Winner overlay (completed match) ── -->
    @if (matchData()!.score.isDone) {
      <div class="winner-overlay">
        <div class="winner-card">
          <div class="winner-text">🏆 Sieger: {{ getWinnerName() }}</div>
          <div class="final-score">
            {{ matchData()!.score.setsPlayer1 }} : {{ matchData()!.score.setsPlayer2 }} Sätze
          </div>
          <button mat-raised-button color="primary" (click)="goToMatches()">
            Zurück zur Übersicht
          </button>
        </div>
      </div>
    }

  }

</div>
```

- [ ] **Step 2: Start the dev server and verify visually**

```bash
cd frontend
ng serve --open
```

Navigate to a match scoring page. Verify:
- Score header shows games (top) above clickable points (bottom)
- Player names centred in their halves
- Two side-by-side observation panels visible
- Set history line shows below points
- Serving player's panel has green border and name

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/features/matches/score/score.component.html
git commit -m "feat(score): new score header + two-panel observation layout [TEN-33]"
```

---

## Task 5: Update Cypress Component Tests

**Files:**
- Modify: `frontend/src/app/features/matches/score/score.component.cy.ts`

- [ ] **Step 1: Replace the test file**

The new tests cover: panel visibility, quick-score click, Winner/Forced/Unforced/Net observation, Ass/DF validation snackbar, service context toggle, context reset after point.

```typescript
import { ScoreComponent } from './score.component';
import { ActivatedRoute } from '@angular/router';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MatchWithScore } from '../../../core/models/match.model';
import { Player } from '../../../core/models/player.model';

// ─── Fixtures ──────────────────────────────────────────────────────────────

const PLAYER1: Player = {
  id: 'p1', firstName: 'Roger', lastName: 'Federer',
  gender: 'MALE', handedness: 'RIGHT', backhandType: 'ONE_HANDED',
};

const PLAYER2: Player = {
  id: 'p2', firstName: 'Rafael', lastName: 'Nadal',
  gender: 'MALE', handedness: 'LEFT', backhandType: 'TWO_HANDED',
};

function makeMatch(overrides: Partial<MatchWithScore> = {}): MatchWithScore {
  return {
    id: 'match-1',
    player1Id: 'p1',
    player2Id: 'p2',
    setsToWin: 2,
    matchTiebreak: false,
    shortSet: false,
    status: 'IN_PROGRESS',
    score: {
      id: 'score-1',
      matchId: 'match-1',
      pointsPlayer1: 0,
      pointsPlayer2: 0,
      gamesPlayer1: 0,
      gamesPlayer2: 0,
      setsPlayer1: 0,
      setsPlayer2: 0,
      isDeuce: false,
      isAdvantagePlayer1: null,
      currentSet: 1,
      isDone: false,
      winner: null,
      acesPlayer1: 0,
      acesPlayer2: 0,
      servingPlayer: 1,   // default: P1 is serving
    },
    ...overrides,
  };
}

const activatedRouteStub = {
  snapshot: { paramMap: { get: () => 'match-1' } },
};

function mountScore(match: MatchWithScore = makeMatch()) {
  cy.intercept('GET', '**/api/matches/match-1', match).as('getMatch');
  cy.intercept('GET', '**/api/players/p1', PLAYER1).as('getPlayer1');
  cy.intercept('GET', '**/api/players/p2', PLAYER2).as('getPlayer2');

  cy.mount(ScoreComponent, {
    providers: [
      provideHttpClient(),
      provideAnimationsAsync(),
      provideRouter([]),
      { provide: ActivatedRoute, useValue: activatedRouteStub },
    ],
  });
  cy.wait('@getMatch');
  cy.wait('@getPlayer1');
  cy.wait('@getPlayer2');
}

// ─── Helpers ──────────────────────────────────────────────────────────────

function makeUpdatedScore(pointsP1: number, pointsP2: number): MatchWithScore {
  return makeMatch({
    score: { ...makeMatch().score, pointsPlayer1: pointsP1, pointsPlayer2: pointsP2 }
  });
}

// ─── Layout tests ─────────────────────────────────────────────────────────

describe('ScoreComponent — layout', () => {
  beforeEach(() => mountScore());

  it('shows both player names in the header', () => {
    cy.contains('Roger Federer').should('be.visible');
    cy.contains('Rafael Nadal').should('be.visible');
  });

  it('shows two observation panels', () => {
    cy.get('[data-testid="panel-p1"]').should('be.visible');
    cy.get('[data-testid="panel-p2"]').should('be.visible');
  });

  it('shows edit-score and end-match buttons', () => {
    cy.get('[data-testid="edit-score-btn"]').should('be.visible');
    cy.get('[data-testid="end-match-btn"]').should('be.visible');
  });
});

// ─── Quick-click score ─────────────────────────────────────────────────────

describe('ScoreComponent — quick-click', () => {
  it('sends recordPoint without pointType when clicking P1 score', () => {
    const updated = makeUpdatedScore(1, 0);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore();

    cy.get('[data-testid="quick-score-p1"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.winner).to.equal(1);
      expect(body.pointType).to.be.oneOf([undefined, null]);
    });
  });

  it('sends winner=2 when clicking P2 score', () => {
    const updated = makeUpdatedScore(0, 1);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore();

    cy.get('[data-testid="quick-score-p2"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.winner).to.equal(2);
    });
  });
});

// ─── Observation buttons — point attribution ───────────────────────────────

describe('ScoreComponent — observation buttons', () => {

  it('Winner on P1 panel → winner=1 in request', () => {
    const updated = makeUpdatedScore(1, 0);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore();

    cy.get('[data-testid="p1-winner"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.winner).to.equal(1);
      expect(body.pointType).to.equal('WINNER');
    });
  });

  it('Forced Error on P1 panel → winner=2 (point for opponent)', () => {
    const updated = makeUpdatedScore(0, 1);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore();

    cy.get('[data-testid="p1-forced"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.winner).to.equal(2);
      expect(body.pointType).to.equal('FORCED_ERROR');
    });
  });

  it('Unforced Error on P1 panel → winner=2', () => {
    const updated = makeUpdatedScore(0, 1);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore();

    cy.get('[data-testid="p1-unforced"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.winner).to.equal(2);
      expect(body.pointType).to.equal('UNFORCED_ERROR');
    });
  });

  it('Netz on P1 panel → winner=2', () => {
    const updated = makeUpdatedScore(0, 1);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore();

    cy.get('[data-testid="p1-net"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.winner).to.equal(2);
      expect(body.pointType).to.equal('NET');
    });
  });

  it('DF on P1 panel (serving) → winner=2, pointType=DOUBLE_FAULT', () => {
    const updated = makeUpdatedScore(0, 1);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore(makeMatch()); // P1 is serving by default

    cy.get('[data-testid="p1-df"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.winner).to.equal(2);
      expect(body.pointType).to.equal('DOUBLE_FAULT');
    });
  });

  it('Ass on P1 panel (serving) → winner=1, pointType=ACE', () => {
    const updated = makeUpdatedScore(1, 0);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore(makeMatch()); // P1 is serving

    cy.get('[data-testid="p1-ace"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.winner).to.equal(1);
      expect(body.pointType).to.equal('ACE');
    });
  });
});

// ─── Ass / DF validation ───────────────────────────────────────────────────

describe('ScoreComponent — Ass/DF validation', () => {

  it('Ass on non-serving P2 panel → shows snackbar, no API call', () => {
    cy.intercept('POST', '**/api/matches/match-1/points').as('recordPoint');
    // P1 is serving (servingPlayer: 1)
    mountScore(makeMatch());

    cy.get('[data-testid="p2-ace"]').click();

    cy.contains('Ass nur für den Aufschläger').should('be.visible');
    cy.get('@recordPoint.all').should('have.length', 0);
  });

  it('DF on non-serving P2 panel → shows snackbar, no API call', () => {
    cy.intercept('POST', '**/api/matches/match-1/points').as('recordPoint');
    mountScore(makeMatch());

    cy.get('[data-testid="p2-df"]').click();

    cy.contains('Doppelfehler nur für den Aufschläger').should('be.visible');
    cy.get('@recordPoint.all').should('have.length', 0);
  });
});

// ─── Service context ───────────────────────────────────────────────────────

describe('ScoreComponent — service context', () => {

  it('selecting 1. Service on P1 panel sends serveAttempt=1 in request', () => {
    const updated = makeUpdatedScore(1, 0);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore();

    cy.get('[data-testid="p1-service-1"]').click();
    cy.get('[data-testid="p1-winner"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.serveAttempt).to.equal(1);
    });
  });

  it('context is reset after recording a point', () => {
    const updated = makeUpdatedScore(1, 0);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore();

    cy.get('[data-testid="p1-service-1"]').click();
    cy.get('[data-testid="p1-service-1"]').should('have.class', 'active');

    cy.get('[data-testid="p1-winner"]').click();
    cy.wait('@recordPoint');

    // After point, service context should be cleared
    cy.get('[data-testid="p1-service-1"]').should('not.have.class', 'active');
  });
});

// ─── Completed match ───────────────────────────────────────────────────────

describe('ScoreComponent — completed match', () => {
  beforeEach(() => {
    const completedMatch = makeMatch({
      status: 'COMPLETED',
      score: {
        ...makeMatch().score,
        setsPlayer1: 2,
        setsPlayer2: 1,
        isDone: true,
        winner: 'PLAYER1',
      },
    });
    mountScore(completedMatch);
  });

  it('shows winner overlay with player name', () => {
    cy.contains('Sieger: Roger Federer').should('be.visible');
  });

  it('hides end match button', () => {
    cy.get('[data-testid="end-match-btn"]').should('not.exist');
  });

  it('shows Zurück zur Übersicht button', () => {
    cy.contains('button', 'Zurück zur Übersicht').should('be.visible');
  });
});

// ─── Pure-logic tests for formatPoints ────────────────────────────────────

describe('formatPoints logic', () => {
  function formatPoints(score: MatchWithScore['score'], forPlayer1: boolean): string {
    if (score.isDeuce) {
      if (score.isAdvantagePlayer1 === null || score.isAdvantagePlayer1 === undefined) return '40';
      return score.isAdvantagePlayer1 === forPlayer1 ? 'A' : '40';
    }
    const pts = forPlayer1 ? score.pointsPlayer1 : score.pointsPlayer2;
    return (['0', '15', '30', '40'] as const)[pts] ?? pts.toString();
  }

  const base = makeMatch().score;

  it('0 points → "0"',  () => expect(formatPoints({ ...base, pointsPlayer1: 0 }, true)).to.equal('0'));
  it('1 point → "15"',  () => expect(formatPoints({ ...base, pointsPlayer1: 1 }, true)).to.equal('15'));
  it('2 points → "30"', () => expect(formatPoints({ ...base, pointsPlayer1: 2 }, true)).to.equal('30'));
  it('3 points → "40"', () => expect(formatPoints({ ...base, pointsPlayer1: 3 }, true)).to.equal('40'));

  it('deuce without advantage → "40" for both', () => {
    const score = { ...base, isDeuce: true, isAdvantagePlayer1: null };
    expect(formatPoints(score, true)).to.equal('40');
    expect(formatPoints(score, false)).to.equal('40');
  });

  it('player1 advantage → "A" for player1, "40" for player2', () => {
    const score = { ...base, isDeuce: true, isAdvantagePlayer1: true };
    expect(formatPoints(score, true)).to.equal('A');
    expect(formatPoints(score, false)).to.equal('40');
  });

  it('player2 advantage → "A" for player2, "40" for player1', () => {
    const score = { ...base, isDeuce: true, isAdvantagePlayer1: false };
    expect(formatPoints(score, false)).to.equal('A');
    expect(formatPoints(score, true)).to.equal('40');
  });
});
```

- [ ] **Step 2: Run the Cypress tests**

```bash
cd frontend
npx cypress run --component --spec "src/app/features/matches/score/score.component.cy.ts" 2>&1 | tail -40
```

Expected: all tests pass. Fix any failures before committing.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/features/matches/score/score.component.cy.ts
git commit -m "test(score): update Cypress spec for new observation panel UI [TEN-33]"
```

---

## Self-Review Checklist

**Spec coverage:**
- [x] Quick-click on score number → `recordQuickPoint` (no pointType) — Task 3 + 4
- [x] Observation button → `recordObservation` (point + stat) — Task 3 + 4
- [x] Score header: games above points, names centred — Task 4
- [x] Service context toggle (1./2.) yellow buttons — Task 3 + 4
- [x] Green buttons: Ass, Winner → own point — Task 3 + 4
- [x] Red buttons: DF on own row; Forced/Unforced/Netz on one row — Task 4
- [x] Snackbar validation: Ass/DF on non-server — Task 3 + 5
- [x] Backend: `@NotBlank` removed, `pointType` nullable in DB — Task 1
- [x] `serveAttempt` wired from context signals — Task 3 (reuses existing backend field)
- [x] `OUT_LONG`, `OUT_SIDE` removed from frontend — Task 2
- [x] `Direction`, Volley/Smash removed — Task 2
- [x] Context signals reset after each point — Task 3 + 5

**No placeholders:** verified — all steps have exact code.

**Type consistency:** `PointType`, `StrokeType`, `RecordPointRequest` defined in Task 2; used identically in Tasks 3, 4, 5.
