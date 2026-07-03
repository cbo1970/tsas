# TEN-36 Set-by-Set Statistics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** On the match-statistics page, let the user switch between «Gesamt» and per-set views; the stats then reflect only the selected set.

**Architecture:** Bundle API — `GET /api/matches/{id}/statistics` returns the existing total stats **plus** a `sets` array (one entry per played set) in one request; the frontend switches client-side. The backend reuses the existing per-point accumulation, applied once over all points and once per `set_number`. The existing `MatchStatistics` record and the ai-module are untouched.

**Tech Stack:** Spring Boot 4 / Java 25 (statistics-module), Angular 21 standalone + Signals, Vitest + Cypress.

## Global Constraints

- **Do not modify** the `MatchStatistics` domain record or `ComputeMatchStatisticsUseCase.compute(UUID)` — the ai-module depends on them. Add per-set support via new additive types + a new `computeBreakdown` method.
- Pure feature addition: no persistence, no new metrics (same values, set-filtered), no new query params, Head-to-Head untouched.
- Build: `JAVA_HOME=/opt/java/jdk-25.0.1` prefix; backend ITs need `DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true`; do NOT export `OPENAI_API_KEY`.
- Tests stay green: backend `./gradlew check` (coverage gate 85% line / 70% branch), frontend Vitest + Cypress.
- The statistics page uses inline German text (no `TranslatePipe`); the new labels «Gesamt»/«Satz N» are inline too.

## Commands

- Backend single test: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :statistics-module:test --tests "com.cas.tsas.statistics.application.service.MatchStatisticsServiceTest"`
- Backend API IT: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew :app:test --tests "com.cas.tsas.statistics.StatisticsApiIT"`
- Frontend Cypress: `cd frontend && npx cypress run --component --spec "src/app/features/matches/statistics/statistics.component.cy.ts"`
- Frontend Vitest: `cd frontend && npx ng test --watch=false`

---

## Task 1: Backend — per-set breakdown (records, service, use case, DTO, controller, tests)

**Files:**
- Create: `backend/statistics-module/src/main/java/com/cas/tsas/statistics/domain/model/SetStatistics.java`
- Create: `backend/statistics-module/src/main/java/com/cas/tsas/statistics/domain/model/MatchStatisticsBreakdown.java`
- Create: `backend/statistics-module/src/main/java/com/cas/tsas/statistics/infrastructure/web/dto/SetStatisticsDto.java`
- Modify: `backend/statistics-module/src/main/java/com/cas/tsas/statistics/application/port/in/ComputeMatchStatisticsUseCase.java`
- Modify: `backend/statistics-module/src/main/java/com/cas/tsas/statistics/application/service/MatchStatisticsService.java`
- Modify: `backend/statistics-module/src/main/java/com/cas/tsas/statistics/infrastructure/web/dto/MatchStatisticsDto.java`
- Modify: `backend/statistics-module/src/main/java/com/cas/tsas/statistics/infrastructure/web/MatchStatisticsController.java`
- Test: `backend/statistics-module/src/test/java/com/cas/tsas/statistics/application/service/MatchStatisticsServiceTest.java`
- Test: `backend/app/src/test/java/com/cas/tsas/statistics/StatisticsApiIT.java`

**Interfaces:**
- Produces (consumed by Task 2 via JSON): the statistics response gains `sets: [{ setNumber, player1, player2, totalPoints }]`.

- [ ] **Step 1: Create the domain records**

`SetStatistics.java`:
```java
package com.cas.tsas.statistics.domain.model;

/** Per-set slice of a match's statistics (TEN-36). */
public record SetStatistics(int setNumber, MatchStatistics stats) {}
```

`MatchStatisticsBreakdown.java`:
```java
package com.cas.tsas.statistics.domain.model;

import java.util.List;

/** Total match statistics plus a per-set breakdown (TEN-36, set ascending). */
public record MatchStatisticsBreakdown(MatchStatistics total, List<SetStatistics> sets) {}
```

- [ ] **Step 2: Add the use-case method**

In `ComputeMatchStatisticsUseCase.java`, add the import and the method (keep `compute(UUID)`):
```java
import com.cas.tsas.statistics.domain.model.MatchStatisticsBreakdown;
// ...
public interface ComputeMatchStatisticsUseCase {
    MatchStatistics compute(UUID matchId);

    /** Total statistics + a per-set breakdown (one entry per played set, ascending). */
    MatchStatisticsBreakdown computeBreakdown(UUID matchId);
}
```

- [ ] **Step 3: Refactor the service to reuse the accumulation + add breakdown**

In `MatchStatisticsService.java`:

Add imports:
```java
import com.cas.tsas.statistics.domain.model.MatchStatisticsBreakdown;
import com.cas.tsas.statistics.domain.model.SetStatistics;
import java.util.TreeMap;
import java.util.stream.Collectors;
```

Change `compute` to delegate to a private helper, and add `computeBreakdown`. Replace the current `compute(UUID matchId)` method body so the accumulation loop lives in `computeFrom(UUID, List<Point>)`:

```java
    @Override
    public MatchStatistics compute(UUID matchId) {
        return computeFrom(matchId, loadPointsByMatchPort.loadPointsByMatch(matchId));
    }

    @Override
    public MatchStatisticsBreakdown computeBreakdown(UUID matchId) {
        List<Point> points = loadPointsByMatchPort.loadPointsByMatch(matchId);
        MatchStatistics total = computeFrom(matchId, points);
        List<SetStatistics> sets = points.stream()
                .collect(Collectors.groupingBy(Point::getSetNumber, TreeMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> new SetStatistics(e.getKey(), computeFrom(matchId, e.getValue())))
                .toList();
        return new MatchStatisticsBreakdown(total, sets);
    }

    /** Accumulates per-player statistics from the given points in a single pass. */
    private MatchStatistics computeFrom(UUID matchId, List<Point> points) {
        Accumulator acc1 = new Accumulator(1);
        Accumulator acc2 = new Accumulator(2);
        int breakPointsTotal = 0;

        for (Point p : points) {
            // ... MOVE THE ENTIRE EXISTING for-loop body here UNCHANGED ...
        }

        return new MatchStatistics(matchId, acc1.toStats(), acc2.toStats(),
                points.size(), breakPointsTotal, Instant.now());
    }
```

> The loop body, the `Accumulator` inner class, `countAttributed`, and `toStats()` are unchanged — only relocate the loop + return into `computeFrom`. `compute` and `computeBreakdown` both call it.

- [ ] **Step 4: Create the set DTO and extend the match DTO**

`SetStatisticsDto.java`:
```java
package com.cas.tsas.statistics.infrastructure.web.dto;

import com.cas.tsas.statistics.domain.model.SetStatistics;

public record SetStatisticsDto(
        int setNumber,
        PlayerStatisticsDto player1,
        PlayerStatisticsDto player2,
        int totalPoints
) {
    public static SetStatisticsDto from(SetStatistics s) {
        return new SetStatisticsDto(
                s.setNumber(),
                PlayerStatisticsDto.from(s.stats().player1()),
                PlayerStatisticsDto.from(s.stats().player2()),
                s.stats().totalPoints());
    }
}
```

Replace `MatchStatisticsDto.java` with the `sets`-carrying version (factory now takes the breakdown):
```java
package com.cas.tsas.statistics.infrastructure.web.dto;

import com.cas.tsas.statistics.domain.model.MatchStatistics;
import com.cas.tsas.statistics.domain.model.MatchStatisticsBreakdown;

import java.util.List;
import java.util.UUID;

public record MatchStatisticsDto(
        UUID matchId,
        PlayerStatisticsDto player1,
        PlayerStatisticsDto player2,
        int totalPoints,
        List<SetStatisticsDto> sets
) {
    public static MatchStatisticsDto from(MatchStatisticsBreakdown b) {
        MatchStatistics t = b.total();
        return new MatchStatisticsDto(
                t.matchId(),
                PlayerStatisticsDto.from(t.player1()),
                PlayerStatisticsDto.from(t.player2()),
                t.totalPoints(),
                b.sets().stream().map(SetStatisticsDto::from).toList());
    }
}
```

> The old `from(MatchStatistics)` factory is removed. Before deleting, confirm the controller is its only caller: `grep -rn "MatchStatisticsDto.from" backend`. (It is — updated in Step 5.)

- [ ] **Step 5: Point the controller at the breakdown**

In `MatchStatisticsController.java`, change the return line:
```java
        return MatchStatisticsDto.from(computeStatistics.computeBreakdown(id));
```
(The `getMatchUseCase.findById(id)` owner check stays above it unchanged.)

- [ ] **Step 6: Add the service breakdown unit test**

Append to `MatchStatisticsServiceTest.java` (it already has the `p(...)` helper and `service`/`loadPort`/`matchId`):
```java
    @Test
    void breakdownProducesTotalAndPerSet() {
        Mockito.when(loadPort.loadPointsByMatch(matchId)).thenReturn(List.of(
                // Set 1: P1 winner + P1 ace
                p(1,1,1,1,PointType.WINNER,StrokeType.FOREHAND,Direction.CROSS_COURT,1,false,1),
                p(1,1,2,1,PointType.ACE,null,null,1,false,1),
                // Set 2: P2 winner
                p(2,1,1,2,PointType.WINNER,StrokeType.BACKHAND,Direction.MIDDLE,2,false,1)
        ));

        var b = service.computeBreakdown(matchId);

        assertThat(b.total().totalPoints()).isEqualTo(3);
        assertThat(b.sets()).extracting(com.cas.tsas.statistics.domain.model.SetStatistics::setNumber)
                .containsExactly(1, 2);

        var set1 = b.sets().get(0).stats();
        assertThat(set1.totalPoints()).isEqualTo(2);
        assertThat(set1.player1().winners()).isEqualTo(1);
        assertThat(set1.player1().aces()).isEqualTo(1);

        var set2 = b.sets().get(1).stats();
        assertThat(set2.totalPoints()).isEqualTo(1);
        assertThat(set2.player2().winners()).isEqualTo(1);
    }

    @Test
    void breakdownOfEmptyMatchHasNoSets() {
        Mockito.when(loadPort.loadPointsByMatch(matchId)).thenReturn(List.of());
        var b = service.computeBreakdown(matchId);
        assertThat(b.total().totalPoints()).isZero();
        assertThat(b.sets()).isEmpty();
    }
```

Run:
```
cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :statistics-module:test --tests "com.cas.tsas.statistics.application.service.MatchStatisticsServiceTest"
```
Expected: PASS (existing tests + 2 new).

- [ ] **Step 7: Extend the API IT to assert the `sets` array**

In `StatisticsApiIT.java`, inside the `GetStatistics` nested class, add a test. Points recorded via the REST API all land in set 1 of a fresh match, so assert one set entry mirroring the total:
```java
        @Test
        void returns_per_set_breakdown() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);
            mockMvc.perform(post("/api/matches/{id}/serve/player1", matchId)).andExpect(status().isOk());
            recordPoint(matchId, 1, "ACE");
            recordPoint(matchId, 1, "WINNER");

            mockMvc.perform(get("/api/matches/{id}/statistics", matchId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sets.length()").value(1))
                    .andExpect(jsonPath("$.sets[0].setNumber").value(1))
                    .andExpect(jsonPath("$.sets[0].totalPoints").value(2))
                    .andExpect(jsonPath("$.sets[0].player1.aces").value(1))
                    .andExpect(jsonPath("$.sets[0].player1.winners").value(1));
        }
```
Also add to the existing `returns_statistics_for_match_with_points` test one assertion that the array is present:
```java
                    .andExpect(jsonPath("$.sets[0].setNumber").value(1))
```

Run:
```
cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew :app:test --tests "com.cas.tsas.statistics.StatisticsApiIT"
```
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add backend/statistics-module backend/app/src/test/java/com/cas/tsas/statistics/StatisticsApiIT.java
git commit -m "feat(statistics): per-set breakdown in the match statistics API (TEN-36)"
```

---

## Task 2: Frontend — set switcher on the statistics page

**Files:**
- Modify: `frontend/src/app/core/models/statistics.model.ts`
- Modify: `frontend/src/app/features/matches/statistics/statistics.component.ts`
- Modify: `frontend/src/app/features/matches/statistics/statistics.component.html`
- Test: `frontend/src/app/features/matches/statistics/statistics.component.cy.ts`

**Interfaces:**
- Consumes (Task 1): the statistics JSON now includes `sets: [{ setNumber, player1, player2, totalPoints }]`.

- [ ] **Step 1: Extend the model**

In `statistics.model.ts`, add a `SetStatistics` interface and a `sets` field on `MatchStatistics`:
```ts
export interface SetStatistics {
  setNumber: number;
  player1: PlayerStatistics;
  player2: PlayerStatistics;
  totalPoints: number;
}

export interface MatchStatistics {
  matchId: string;
  player1: PlayerStatistics;
  player2: PlayerStatistics;
  totalPoints: number;
  sets?: SetStatistics[];
}
```

- [ ] **Step 2: Component signal + computed + handler**

In `statistics.component.ts`:
- Add `computed` to the `@angular/core` import: `import { Component, OnInit, inject, signal, computed } from '@angular/core';`
- Import the type: `import { MatchStatistics, PlayerStatistics } from '../../../core/models/statistics.model';`
- Add to the class (after the existing signals):
```ts
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
```

- [ ] **Step 3: Add the segmented control + rebind the grid to `activeStats`**

In `statistics.component.html`:

(a) Inside the `@if (stats(); as s) {` block, immediately after the opening line, add a `@let` and the segmented control (above the `<div class="stat-grid">`):
```html
    @let a = activeStats()!;

    @if (sets().length > 0) {
      <div class="set-tabs">
        <button type="button" class="set-tab" [class.active]="selectedView() === 'total'"
                data-testid="set-tab-total" (click)="selectView('total')">Gesamt</button>
        @for (set of sets(); track set.setNumber) {
          <button type="button" class="set-tab" [class.active]="selectedView() === set.setNumber"
                  [attr.data-testid]="'set-tab-' + set.setNumber" (click)="selectView(set.setNumber)">Satz {{ set.setNumber }}</button>
        }
      </div>
    }
```

(b) Rebind every stat-value reference in the `stat-grid` from the total to the active view:
- Replace all `s.player1.` → `a.p1.`
- Replace all `s.player2.` → `a.p2.`

These tokens appear ONLY inside the stat-grid (the player-name header uses `p1Name()/p2Name()`, the set-score badges use `set.p1/set.p2` from `setScores()`), so a whole-file replace of `s.player1.`→`a.p1.` and `s.player2.`→`a.p2.` is correct. Verify afterwards: `grep -n "s.player1\|s.player2" statistics.component.html` returns nothing.

- [ ] **Step 4: Style the segmented control (design tokens)**

In `statistics.component.ts` `styles`, add:
```css
    .set-tabs { display: flex; flex-wrap: wrap; gap: 6px; justify-content: center; margin: 4px 0 12px; }
    .set-tab { background: #fff; border: 1px solid var(--text); color: var(--text); border-radius: var(--radius-pill); padding: 4px 12px; font-size: 12px; font-weight: 600; cursor: pointer; }
    .set-tab.active { background: var(--brand); border-color: var(--brand); color: #fff; }
```

- [ ] **Step 5: Cypress test**

In `statistics.component.cy.ts`, add a mock with sets and a test. Add after `MOCK_STATS`:
```ts
const MOCK_STATS_WITH_SETS: MatchStatistics = {
  ...MOCK_STATS,
  sets: [
    { setNumber: 1, totalPoints: 12,
      player1: { ...MOCK_STATS.player1, aces: 2 },
      player2: { ...MOCK_STATS.player2, aces: 0 } },
    { setNumber: 2, totalPoints: 18,
      player1: { ...MOCK_STATS.player1, aces: 1 },
      player2: { ...MOCK_STATS.player2, aces: 1 } },
  ],
};

function mountStatsWithSets(extraProviders: any[] = []) {
  cy.intercept('GET', '**/api/matches/match-1/statistics', MOCK_STATS_WITH_SETS).as('getStats');
  cy.mount(StatisticsComponent, {
    providers: [
      provideRouter([]),
      provideHttpClient(),
      provideAnimationsAsync(),
      { provide: ActivatedRoute, useValue: activatedRouteStub },
      ...extraProviders,
    ],
  });
  cy.wait('@getStats');
}
```
And add these tests inside the `describe`:
```ts
  it('renders a Gesamt tab plus one tab per set', () => {
    mountStatsWithSets();
    cy.get('[data-testid="set-tab-total"]').should('exist');
    cy.get('[data-testid="set-tab-1"]').should('exist');
    cy.get('[data-testid="set-tab-2"]').should('exist');
  });

  it('defaults to Gesamt and switches stats per set on click', () => {
    mountStatsWithSets();
    // total aces = 3 (from MOCK_STATS)
    cy.get('[data-testid="val-p1-aces"]').should('contain', '3');
    cy.get('[data-testid="set-tab-1"]').click();
    cy.get('[data-testid="val-p1-aces"]').should('contain', '2');
    cy.get('[data-testid="set-tab-2"]').click();
    cy.get('[data-testid="val-p1-aces"]').should('contain', '1');
    cy.get('[data-testid="set-tab-total"]').click();
    cy.get('[data-testid="val-p1-aces"]').should('contain', '3');
  });

  it('shows no set tabs when the response has no sets', () => {
    mountStats(); // MOCK_STATS has no sets
    cy.get('[data-testid="set-tab-total"]').should('not.exist');
  });
```

Run:
```
cd frontend && npx cypress run --component --spec "src/app/features/matches/statistics/statistics.component.cy.ts"
npx ng test --watch=false
```
Expected: all green (existing 9 + 3 new; the existing tests still pass because the default view is the total).

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/core/models/statistics.model.ts frontend/src/app/features/matches/statistics/
git commit -m "feat(frontend): set-by-set switcher on the statistics page (TEN-36)"
```

---

## Final verification

- [ ] Backend: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew check` → green incl. coverage gate.
- [ ] Frontend: `cd frontend && npx ng test --watch=false && npm run cypress:run` → green.
- [ ] Manual sight-check: open a completed match's statistics page; «Gesamt» + «Satz N» tabs appear; switching changes the grid values; «Gesamt» matches the previous behaviour.

## Self-Review (author checklist — completed)

**Spec coverage:** Bundle API (total + sets) → Task 1 (records, service `computeBreakdown`, DTO `sets`, controller). Frontend switcher → Task 2 (model, signal/computed, segmented control, rebind, styles). Tests → service unit + API IT (Task 1), Cypress (Task 2). `MatchStatistics`/ai-module untouched → Global Constraints + the `computeFrom` refactor leaving `compute(UUID)` behaviour identical. No gaps.

**Placeholder scan:** No TBD/TODO. The one "MOVE the existing loop body here" instruction in Task 1 Step 3 references concrete existing code (the implementer relocates it verbatim); everything else is complete code.

**Type/name consistency:** `computeBreakdown` / `MatchStatisticsBreakdown` / `SetStatistics(setNumber, stats)` / `SetStatisticsDto(setNumber, player1, player2, totalPoints)` / `MatchStatisticsDto.from(MatchStatisticsBreakdown)` are consistent across backend tasks; frontend `sets`/`SetStatistics`/`activeStats().p1|p2`/`selectView`/`set-tab-{total|N}` are consistent across model, component, template, and tests.
