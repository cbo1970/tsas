# TEN-14 Match-Statistik-Seite — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Neue `/matches/:id/statistics`-Seite die nach Matchende automatisch geöffnet wird und einen ATP-Stil Spieler-Vergleich mit Satz-Scores und Match-Statistiken zeigt.

**Architecture:** Neuer `GET /api/matches/{id}/statistics` Endpoint im `statistics-module`; neues `StatisticsComponent` (Angular standalone, lazy-loaded) unter `/matches/:id/statistics`; `ScoreComponent` navigiert nach Matchende dorthin statt `loadMatch()` aufzurufen. Set-Scores und Spielernamen werden als Query-Parameter übergeben.

**Tech Stack:** Spring Boot 4 (Clean Architecture), Angular 19 (signals, standalone), Angular Material, Cypress (component tests), Vitest (unit tests)

---

## File Map

**Neu — Backend:**
- `backend/statistics-module/src/main/java/com/cas/tsas/statistics/infrastructure/web/dto/PlayerStatisticsDto.java`
- `backend/statistics-module/src/main/java/com/cas/tsas/statistics/infrastructure/web/dto/MatchStatisticsDto.java`
- `backend/statistics-module/src/main/java/com/cas/tsas/statistics/infrastructure/web/MatchStatisticsController.java`
- `backend/app/src/test/java/com/cas/tsas/statistics/StatisticsApiIT.java`

**Geändert — Backend:**
- `backend/statistics-module/build.gradle.kts` — `spring-boot-starter-web` hinzufügen
- `backend/statistics-module/src/main/java/com/cas/tsas/statistics/application/service/MatchStatisticsService.java` — null-pointType Guard

**Neu — Frontend:**
- `frontend/src/app/core/models/statistics.model.ts`
- `frontend/src/app/features/matches/statistics/statistics.component.ts`
- `frontend/src/app/features/matches/statistics/statistics.component.html`
- `frontend/src/app/features/matches/statistics/statistics.component.cy.ts`

**Geändert — Frontend:**
- `frontend/src/app/core/services/api.service.ts` — `getMatchStatistics()` hinzufügen
- `frontend/src/app/app.routes.ts` — neue Route
- `frontend/src/app/features/matches/score/score.component.ts` — Navigation nach isDone

---

## Task 1: MatchStatisticsService — null-pointType Guard

TEN-33 macht `pointType` nullable. `MatchStatisticsService.compute()` ruft `PointAttribution.attributingPlayer(p)` auf, das mit einem NPE abbricht wenn `pointType == null`. Diese Aufgabe behebt das, bevor der neue Endpoint es auslöst.

**Files:**
- Modify: `backend/statistics-module/src/main/java/com/cas/tsas/statistics/application/service/MatchStatisticsService.java`
- Test: `backend/statistics-module/src/test/java/com/cas/tsas/statistics/application/service/MatchStatisticsServiceTest.java`

- [ ] **Step 1: Write the failing test**

Füge folgenden Test in `MatchStatisticsServiceTest` ein (nach dem letzten Test):

```java
@Test
void nullPointTypeIsSkippedForAttribution() {
    // Quick-point ohne pointType (TEN-33): nur pointsWon zählen, kein Absturz
    Mockito.when(loadPort.loadPointsByMatch(matchId)).thenReturn(List.of(
            p(1,1,1,1,null,StrokeType.FOREHAND,Direction.CROSS_COURT,1,false,null),
            p(1,1,2,2,null,null,null,null,false,null)
    ));

    MatchStatistics s = service.compute(matchId);

    assertThat(s.totalPoints()).isEqualTo(2);
    assertThat(s.player1().pointsWon()).isEqualTo(1);
    assertThat(s.player2().pointsWon()).isEqualTo(1);
    assertThat(s.player1().winners()).isZero();
    assertThat(s.player2().winners()).isZero();
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :statistics-module:test --tests "*.MatchStatisticsServiceTest.nullPointTypeIsSkippedForAttribution" 2>&1 | tail -20
```

Expected: FAIL with `NullPointerException` (switch on null pointType).

- [ ] **Step 3: Fix MatchStatisticsService.compute()**

Ersetze den Loop-Anfang in `compute()` — ergänze einen null-Guard ganz am Anfang der For-Schleife:

```java
for (Point p : points) {
    // Quick-points (TEN-33) have no pointType — count only winner, skip attribution
    if (p.getPointType() == null) {
        if (p.getWinner() == 1) acc1.pointsWon++;
        else acc2.pointsWon++;
        continue;
    }

    int attribTo = PointAttribution.attributingPlayer(p);
    // ... rest of existing loop body unchanged
```

Der vollständige Loop sieht danach so aus:

```java
for (Point p : points) {
    if (p.getPointType() == null) {
        if (p.getWinner() == 1) acc1.pointsWon++;
        else acc2.pointsWon++;
        continue;
    }

    int attribTo = PointAttribution.attributingPlayer(p);
    Accumulator attribAcc = attribTo == 1 ? acc1 : acc2;
    attribAcc.countAttributed(p);

    if (p.getWinner() == 1) {
        acc1.pointsWon++;
    } else {
        acc2.pointsWon++;
    }

    if (p.getStrokeType() != null) {
        attribAcc.strokes.merge(p.getStrokeType(), 1, Integer::sum);
    }
    if (p.getDirection() != null) {
        attribAcc.directions.merge(p.getDirection(), 1, Integer::sum);
    }

    if (p.getServingPlayer() != null && p.getServeAttempt() != null) {
        Accumulator serverAcc = p.getServingPlayer() == 1 ? acc1 : acc2;
        serverAcc.serveAttemptsTotal++;
        if (p.getServeAttempt() == 1) {
            serverAcc.firstServesIn++;
        } else if (p.getServeAttempt() == 2) {
            serverAcc.secondServesPlayed++;
            if (p.getPointType() != PointType.DOUBLE_FAULT) {
                serverAcc.secondServesIn++;
            }
        }
    }

    if (p.isBreakPoint() && p.getServingPlayer() != null) {
        breakPointsTotal++;
        Accumulator serverAcc = p.getServingPlayer() == 1 ? acc1 : acc2;
        Accumulator returnerAcc = p.getServingPlayer() == 1 ? acc2 : acc1;
        serverAcc.breakPointsFaced++;
        if (p.getWinner() != p.getServingPlayer()) {
            returnerAcc.breakPointsWon++;
        }
    }
}
```

- [ ] **Step 4: Run all statistics-module tests**

```bash
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :statistics-module:test 2>&1 | tail -15
```

Expected: `BUILD SUCCESSFUL`, alle Tests grün.

- [ ] **Step 5: Commit**

```bash
git add backend/statistics-module/src/main/java/com/cas/tsas/statistics/application/service/MatchStatisticsService.java \
        backend/statistics-module/src/test/java/com/cas/tsas/statistics/application/service/MatchStatisticsServiceTest.java
git commit -m "fix(statistics): skip null-pointType points to avoid NPE"
```

---

## Task 2: Backend — MatchStatisticsController + DTOs

Neuer REST-Endpoint `GET /api/matches/{id}/statistics`. Die Berechnung ist in `MatchStatisticsService` bereits implementiert — dieser Task erstellt nur den Web-Adapter.

**Files:**
- Modify: `backend/statistics-module/build.gradle.kts`
- Create: `backend/statistics-module/src/main/java/com/cas/tsas/statistics/infrastructure/web/dto/PlayerStatisticsDto.java`
- Create: `backend/statistics-module/src/main/java/com/cas/tsas/statistics/infrastructure/web/dto/MatchStatisticsDto.java`
- Create: `backend/statistics-module/src/main/java/com/cas/tsas/statistics/infrastructure/web/MatchStatisticsController.java`

- [ ] **Step 1: Add spring-boot-starter-web to statistics-module**

Aktueller Inhalt von `backend/statistics-module/build.gradle.kts`:
```kotlin
dependencies {
    implementation(project(":common-module"))
    implementation(project(":match-module"))
    implementation("org.springframework.boot:spring-boot-starter")
}
```

Ersetze durch:
```kotlin
dependencies {
    implementation(project(":common-module"))
    implementation(project(":match-module"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
}
```

- [ ] **Step 2: Create PlayerStatisticsDto**

Erstelle `backend/statistics-module/src/main/java/com/cas/tsas/statistics/infrastructure/web/dto/PlayerStatisticsDto.java`:

```java
package com.cas.tsas.statistics.infrastructure.web.dto;

import com.cas.tsas.match.domain.model.StrokeType;
import com.cas.tsas.statistics.domain.model.PlayerStatistics;

public record PlayerStatisticsDto(
        int pointsWon,
        int winners,
        int unforcedErrors,
        int forcedErrors,
        int aces,
        int doubleFaults,
        double firstServePercentage,
        double secondServePercentage,
        int breakPointsWon,
        int breakPointsFaced,
        double forehandPercentage
) {
    public static PlayerStatisticsDto from(PlayerStatistics s) {
        var strokes = s.strokeDistribution().counts();
        int strokeTotal = strokes.values().stream().mapToInt(Integer::intValue).sum();
        double forehandPct = strokeTotal == 0 ? 0.0
                : (double) strokes.getOrDefault(StrokeType.FOREHAND, 0) / strokeTotal;
        return new PlayerStatisticsDto(
                s.pointsWon(), s.winners(), s.unforcedErrors(), s.forcedErrors(),
                s.aces(), s.doubleFaults(), s.firstServePercentage(), s.secondServePercentage(),
                s.breakPointsWon(), s.breakPointsFaced(), forehandPct);
    }
}
```

- [ ] **Step 3: Create MatchStatisticsDto**

Erstelle `backend/statistics-module/src/main/java/com/cas/tsas/statistics/infrastructure/web/dto/MatchStatisticsDto.java`:

```java
package com.cas.tsas.statistics.infrastructure.web.dto;

import com.cas.tsas.statistics.domain.model.MatchStatistics;

import java.util.UUID;

public record MatchStatisticsDto(
        UUID matchId,
        PlayerStatisticsDto player1,
        PlayerStatisticsDto player2,
        int totalPoints
) {
    public static MatchStatisticsDto from(MatchStatistics s) {
        return new MatchStatisticsDto(
                s.matchId(),
                PlayerStatisticsDto.from(s.player1()),
                PlayerStatisticsDto.from(s.player2()),
                s.totalPoints());
    }
}
```

- [ ] **Step 4: Create MatchStatisticsController**

Erstelle `backend/statistics-module/src/main/java/com/cas/tsas/statistics/infrastructure/web/MatchStatisticsController.java`:

```java
package com.cas.tsas.statistics.infrastructure.web;

import com.cas.tsas.match.application.port.in.GetMatchUseCase;
import com.cas.tsas.statistics.application.port.in.ComputeMatchStatisticsUseCase;
import com.cas.tsas.statistics.infrastructure.web.dto.MatchStatisticsDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/matches/{id}/statistics")
public class MatchStatisticsController {

    private final GetMatchUseCase getMatchUseCase;
    private final ComputeMatchStatisticsUseCase computeStatistics;

    public MatchStatisticsController(GetMatchUseCase getMatchUseCase,
                                     ComputeMatchStatisticsUseCase computeStatistics) {
        this.getMatchUseCase = getMatchUseCase;
        this.computeStatistics = computeStatistics;
    }

    @GetMapping
    public MatchStatisticsDto getStatistics(@PathVariable UUID id) {
        getMatchUseCase.findById(id); // throws MatchNotFoundException → 404 via GlobalExceptionHandler
        return MatchStatisticsDto.from(computeStatistics.compute(id));
    }
}
```

- [ ] **Step 5: Compile check**

```bash
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :statistics-module:compileJava 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add backend/statistics-module/build.gradle.kts \
        backend/statistics-module/src/main/java/com/cas/tsas/statistics/infrastructure/
git commit -m "feat(statistics): add GET /api/matches/{id}/statistics endpoint"
```

---

## Task 3: Backend — Integration Test

**Files:**
- Create: `backend/app/src/test/java/com/cas/tsas/statistics/StatisticsApiIT.java`

- [ ] **Step 1: Write the failing test**

Erstelle `backend/app/src/test/java/com/cas/tsas/statistics/StatisticsApiIT.java`:

```java
package com.cas.tsas.statistics;

import com.cas.tsas.AbstractIntegrationTest;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchScoreJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.PointJpaRepository;
import com.cas.tsas.player.infrastructure.persistence.repository.PlayerJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatisticsApiIT extends AbstractIntegrationTest {

    @Autowired ObjectMapper objectMapper;
    @Autowired MatchJpaRepository matchRepository;
    @Autowired MatchScoreJpaRepository matchScoreRepository;
    @Autowired PointJpaRepository pointRepository;
    @Autowired PlayerJpaRepository playerRepository;

    @BeforeEach
    void cleanUp() {
        pointRepository.deleteAll();
        matchScoreRepository.deleteAll();
        matchRepository.deleteAll();
        playerRepository.deleteAll();
    }

    private UUID createPlayer() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "firstName", "Test", "lastName", "Player",
                "gender", "MALE", "handedness", "RIGHT", "backhandType", "TWO_HANDED"
        ));
        String response = mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private UUID createMatch(UUID p1, UUID p2) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "player1Id", p1, "player2Id", p2,
                "setsToWin", 2, "matchTiebreak", false, "shortSet", false
        ));
        String response = mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private void recordPoint(UUID matchId, int winner, String pointType) throws Exception {
        var body = pointType != null
                ? Map.of("winner", winner, "pointType", pointType)
                : Map.of("winner", winner);
        mockMvc.perform(post("/api/matches/{id}/points", matchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    @Nested
    class GetStatistics {

        @Test
        void returns_statistics_for_match_with_points() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            // Record an ace for player 1 (must set server first)
            mockMvc.perform(post("/api/matches/{id}/serve/player1", matchId))
                    .andExpect(status().isOk());
            recordPoint(matchId, 1, "ACE");

            // Record a winner for player 1
            recordPoint(matchId, 1, "WINNER");

            // Record an unforced error (point goes to player 1)
            recordPoint(matchId, 1, "UNFORCED_ERROR");

            // Record a quick-point (null pointType)
            recordPoint(matchId, 2, null);

            mockMvc.perform(get("/api/matches/{id}/statistics", matchId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matchId").value(matchId.toString()))
                    .andExpect(jsonPath("$.totalPoints").value(4))
                    .andExpect(jsonPath("$.player1.pointsWon").value(3))
                    .andExpect(jsonPath("$.player2.pointsWon").value(1))
                    .andExpect(jsonPath("$.player1.aces").value(1))
                    .andExpect(jsonPath("$.player1.winners").value(1))
                    .andExpect(jsonPath("$.player2.unforcedErrors").value(1));
        }

        @Test
        void returns_404_for_unknown_match() throws Exception {
            mockMvc.perform(get("/api/matches/{id}/statistics", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :app:test --tests "*.StatisticsApiIT" 2>&1 | tail -20
```

Expected: FAIL — Controller-Bean nicht gefunden oder 404 für gültige ID.

- [ ] **Step 3: Run tests to verify they pass**

Nach Task 2 sind die Tests bereits implementiert — führe sie erneut aus:

```bash
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :app:test --tests "*.StatisticsApiIT" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, beide Tests grün.

- [ ] **Step 4: Run full test suite**

```bash
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test 2>&1 | tail -15
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add backend/app/src/test/java/com/cas/tsas/statistics/StatisticsApiIT.java
git commit -m "test(statistics): add integration test for GET /statistics endpoint"
```

---

## Task 4: Frontend — Model + ApiService

**Files:**
- Create: `frontend/src/app/core/models/statistics.model.ts`
- Modify: `frontend/src/app/core/services/api.service.ts`

- [ ] **Step 1: Create statistics.model.ts**

Erstelle `frontend/src/app/core/models/statistics.model.ts`:

```typescript
export interface PlayerStatistics {
  pointsWon: number;
  winners: number;
  unforcedErrors: number;
  forcedErrors: number;
  aces: number;
  doubleFaults: number;
  firstServePercentage: number;
  secondServePercentage: number;
  breakPointsWon: number;
  breakPointsFaced: number;
  forehandPercentage: number;
}

export interface MatchStatistics {
  matchId: string;
  player1: PlayerStatistics;
  player2: PlayerStatistics;
  totalPoints: number;
}
```

- [ ] **Step 2: Add getMatchStatistics() to ApiService**

In `frontend/src/app/core/services/api.service.ts`, füge folgendes hinzu:

Am Anfang der Datei die Import-Zeile erweitern:
```typescript
import { MatchStatistics } from '../models/statistics.model';
```

Am Ende der Klasse (nach `endMatchWalkover`):
```typescript
getMatchStatistics(matchId: string): Observable<MatchStatistics> {
  return this.http.get<MatchStatistics>(`${this.base}/matches/${matchId}/statistics`);
}
```

- [ ] **Step 3: Verify compile**

```bash
cd frontend && npx ng build --configuration=development 2>&1 | tail -10
```

Expected: Keine Fehler.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/core/models/statistics.model.ts \
        frontend/src/app/core/services/api.service.ts
git commit -m "feat(statistics): add MatchStatistics model and ApiService method"
```

---

## Task 5: Frontend — StatisticsComponent

**Files:**
- Create: `frontend/src/app/features/matches/statistics/statistics.component.ts`
- Create: `frontend/src/app/features/matches/statistics/statistics.component.html`
- Create: `frontend/src/app/features/matches/statistics/statistics.component.cy.ts`

- [ ] **Step 1: Write the Cypress test first**

Erstelle `frontend/src/app/features/matches/statistics/statistics.component.cy.ts`:

```typescript
import { StatisticsComponent } from './statistics.component';
import { ActivatedRoute } from '@angular/router';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MatchStatistics } from '../../../core/models/statistics.model';

const MOCK_STATS: MatchStatistics = {
  matchId: 'match-1',
  totalPoints: 30,
  player1: {
    pointsWon: 18, winners: 8, unforcedErrors: 5, forcedErrors: 3,
    aces: 3, doubleFaults: 1, firstServePercentage: 0.68,
    secondServePercentage: 0.72, breakPointsWon: 2, breakPointsFaced: 4,
    forehandPercentage: 0.65,
  },
  player2: {
    pointsWon: 12, winners: 5, unforcedErrors: 9, forcedErrors: 2,
    aces: 1, doubleFaults: 2, firstServePercentage: 0.55,
    secondServePercentage: 0.60, breakPointsWon: 1, breakPointsFaced: 3,
    forehandPercentage: 0.58,
  },
};

const activatedRouteStub = {
  snapshot: {
    paramMap: { get: (key: string) => key === 'id' ? 'match-1' : null },
    queryParamMap: {
      get: (key: string) => {
        if (key === 'sets') return '6-4,3-6,7-5';
        if (key === 'p1') return 'Müller';
        if (key === 'p2') return 'Meier';
        return null;
      },
    },
  },
};

function mountStats() {
  cy.intercept('GET', '**/api/matches/match-1/statistics', MOCK_STATS).as('getStats');
  cy.mount(StatisticsComponent, {
    providers: [
      provideRouter([]),
      provideHttpClient(),
      provideAnimationsAsync(),
      { provide: ActivatedRoute, useValue: activatedRouteStub },
    ],
  });
  cy.wait('@getStats');
}

describe('StatisticsComponent', () => {
  it('renders player names from query params', () => {
    mountStats();
    cy.contains('Müller').should('exist');
    cy.contains('Meier').should('exist');
  });

  it('renders one set score row per set', () => {
    mountStats();
    cy.get('[data-testid="set-row"]').should('have.length', 3);
  });

  it('highlights winning set score badge', () => {
    mountStats();
    cy.get('[data-testid="set-row"]').eq(0)
      .find('[data-testid="badge-p1"]').should('have.class', 'winner');
    cy.get('[data-testid="set-row"]').eq(1)
      .find('[data-testid="badge-p2"]').should('have.class', 'winner');
  });

  it('renders all stat rows', () => {
    mountStats();
    cy.get('[data-testid="stat-row"]').should('have.length.gte', 9);
  });

  it('displays correct values for aces', () => {
    mountStats();
    cy.get('[data-testid="val-p1-aces"]').should('contain', '3');
    cy.get('[data-testid="val-p2-aces"]').should('contain', '1');
  });

  it('displays firstServePercentage as percent', () => {
    mountStats();
    cy.get('[data-testid="val-p1-first-serve"]').should('contain', '68%');
    cy.get('[data-testid="val-p2-first-serve"]').should('contain', '55%');
  });

  it('displays break points as fraction', () => {
    mountStats();
    cy.get('[data-testid="val-p1-break-points"]').should('contain', '2/4');
    cy.get('[data-testid="val-p2-break-points"]').should('contain', '1/3');
  });

  it('navigates to /players when back button clicked', () => {
    mountStats();
    cy.get('[data-testid="back-btn"]').click();
    cy.url().should('include', '/players');
  });
});
```

- [ ] **Step 2: Create statistics.component.ts**

Erstelle `frontend/src/app/features/matches/statistics/statistics.component.ts`:

```typescript
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { DecimalPipe, NgClass } from '@angular/common';
import { ApiService } from '../../../core/services/api.service';
import { MatchStatistics } from '../../../core/models/statistics.model';

@Component({
  selector: 'app-statistics',
  standalone: true,
  imports: [MatButtonModule, DecimalPipe, NgClass],
  templateUrl: './statistics.component.html',
  styles: [`
    :host { display: block; min-height: 100dvh; background: #0f172a; color: #eee; font-family: sans-serif; }
    .page { max-width: 480px; margin: 0 auto; padding: 16px; }
    .player-row { display: grid; grid-template-columns: 1fr 48px 1fr; align-items: end; margin-bottom: 8px; }
    .player-name { text-align: center; font-weight: 700; font-size: 15px; }
    .set-scores { margin-bottom: 12px; }
    .set-row { display: grid; grid-template-columns: 1fr 48px 1fr; align-items: center; gap: 4px; margin-bottom: 4px; }
    .set-label { text-align: center; font-size: 10px; color: #555; }
    .badge { text-align: center; }
    .badge span { display: inline-block; border-radius: 4px; padding: 3px 14px; font-size: 14px; font-weight: 600; background: #1e293b; color: #94a3b8; }
    .badge.winner span { background: #0ea5e9; color: #000; }
    .divider { border-top: 1px solid #1e293b; margin: 10px 0; }
    .stat-grid { display: grid; grid-template-columns: 48px 1fr 48px; gap: 3px 6px; align-items: center; }
    .val { font-size: 13px; font-weight: 600; }
    .val-left { text-align: right; }
    .val-right { text-align: left; padding-left: 4px; color: #94a3b8; }
    .val-right.leading { color: #eee; font-weight: 700; }
    .stat-label { font-size: 10px; color: #64748b; text-align: center; margin-bottom: 1px; }
    .bar { display: flex; height: 4px; border-radius: 2px; overflow: hidden; margin-top: 1px; }
    .bar-p1 { background: #0ea5e9; }
    .bar-p2 { background: #475569; }
    .bar-err { background: #f87171; }
    .bar-good { background: #4ade80; }
    .back-row { text-align: center; margin-top: 20px; }
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

  goBack() {
    this.router.navigate(['/players']);
  }
}
```

- [ ] **Step 3: Create statistics.component.html**

Erstelle `frontend/src/app/features/matches/statistics/statistics.component.html`:

```html
<div class="page">

  <!-- Spielernamen -->
  <div class="player-row">
    <div class="player-name">{{ p1Name() }}</div>
    <div></div>
    <div class="player-name">{{ p2Name() }}</div>
  </div>

  <!-- Set-Scores -->
  <div class="set-scores">
    @for (set of setScores(); track $index) {
      <div class="set-row" data-testid="set-row">
        <div class="badge" [ngClass]="{ winner: set.p1 > set.p2 }" data-testid="badge-p1">
          <span>{{ set.p1 }}</span>
        </div>
        <div class="set-label">Satz {{ $index + 1 }}</div>
        <div class="badge" [ngClass]="{ winner: set.p2 > set.p1 }" data-testid="badge-p2">
          <span>{{ set.p2 }}</span>
        </div>
      </div>
    }
  </div>

  <div class="divider"></div>

  @if (stats(); as s) {
    <div class="stat-grid">

      <!-- Gewonnene Punkte -->
      <span class="val val-left" style="color:#0ea5e9">{{ s.player1.pointsWon }}</span>
      <div data-testid="stat-row">
        <div class="stat-label">Gewonnene Punkte</div>
        <div class="bar">
          <div class="bar-p1" [style.flex]="s.player1.pointsWon"></div>
          <div class="bar-p2" [style.flex]="s.player2.pointsWon"></div>
        </div>
      </div>
      <span class="val val-right" [ngClass]="{ leading: s.player2.pointsWon > s.player1.pointsWon }">{{ s.player2.pointsWon }}</span>

      <!-- Asse -->
      <span class="val val-left" [ngClass]="{ 'leading': s.player1.aces >= s.player2.aces }" style="color:#0ea5e9" data-testid="val-p1-aces">{{ s.player1.aces }}</span>
      <div data-testid="stat-row">
        <div class="stat-label">Asse</div>
        <div class="bar">
          <div class="bar-p1" [style.flex]="s.player1.aces || 1"></div>
          <div class="bar-p2" [style.flex]="s.player2.aces || 1"></div>
        </div>
      </div>
      <span class="val val-right" [ngClass]="{ leading: s.player2.aces > s.player1.aces }" data-testid="val-p2-aces">{{ s.player2.aces }}</span>

      <!-- Winners -->
      <span class="val val-left" style="color:#0ea5e9">{{ s.player1.winners }}</span>
      <div data-testid="stat-row">
        <div class="stat-label">Winners</div>
        <div class="bar">
          <div class="bar-p1" [style.flex]="s.player1.winners || 1"></div>
          <div class="bar-p2" [style.flex]="s.player2.winners || 1"></div>
        </div>
      </div>
      <span class="val val-right" [ngClass]="{ leading: s.player2.winners > s.player1.winners }">{{ s.player2.winners }}</span>

      <!-- Forced Errors -->
      <span class="val val-right" [ngClass]="{ leading: s.player1.forcedErrors <= s.player2.forcedErrors }">{{ s.player1.forcedErrors }}</span>
      <div data-testid="stat-row">
        <div class="stat-label">Forced Errors</div>
        <div class="bar">
          <div [class]="s.player1.forcedErrors <= s.player2.forcedErrors ? 'bar-good' : 'bar-err'" [style.flex]="s.player1.forcedErrors || 1"></div>
          <div [class]="s.player2.forcedErrors <= s.player1.forcedErrors ? 'bar-good' : 'bar-err'" [style.flex]="s.player2.forcedErrors || 1"></div>
        </div>
      </div>
      <span class="val val-right" [ngClass]="{ leading: s.player2.forcedErrors <= s.player1.forcedErrors }">{{ s.player2.forcedErrors }}</span>

      <!-- Unforced Errors -->
      <span class="val val-right" [ngClass]="{ leading: s.player1.unforcedErrors <= s.player2.unforcedErrors }">{{ s.player1.unforcedErrors }}</span>
      <div data-testid="stat-row">
        <div class="stat-label">Unforced Errors</div>
        <div class="bar">
          <div [class]="s.player1.unforcedErrors <= s.player2.unforcedErrors ? 'bar-good' : 'bar-err'" [style.flex]="s.player1.unforcedErrors || 1"></div>
          <div [class]="s.player2.unforcedErrors <= s.player1.unforcedErrors ? 'bar-good' : 'bar-err'" [style.flex]="s.player2.unforcedErrors || 1"></div>
        </div>
      </div>
      <span class="val val-right" [ngClass]="{ leading: s.player2.unforcedErrors <= s.player1.unforcedErrors }">{{ s.player2.unforcedErrors }}</span>

      <!-- Doppelfehler -->
      <span class="val val-right" [ngClass]="{ leading: s.player1.doubleFaults <= s.player2.doubleFaults }">{{ s.player1.doubleFaults }}</span>
      <div data-testid="stat-row">
        <div class="stat-label">Doppelfehler</div>
        <div class="bar">
          <div [class]="s.player1.doubleFaults <= s.player2.doubleFaults ? 'bar-good' : 'bar-err'" [style.flex]="s.player1.doubleFaults || 1"></div>
          <div [class]="s.player2.doubleFaults <= s.player1.doubleFaults ? 'bar-good' : 'bar-err'" [style.flex]="s.player2.doubleFaults || 1"></div>
        </div>
      </div>
      <span class="val val-right" [ngClass]="{ leading: s.player2.doubleFaults <= s.player1.doubleFaults }">{{ s.player2.doubleFaults }}</span>

      <div style="grid-column:span 3" class="divider"></div>

      <!-- 1. Aufschlag % -->
      <span class="val val-left" style="color:#0ea5e9" data-testid="val-p1-first-serve">{{ pct(s.player1.firstServePercentage) }}</span>
      <div data-testid="stat-row">
        <div class="stat-label">1. Aufschlag %</div>
        <div class="bar">
          <div class="bar-p1" [style.flex]="s.player1.firstServePercentage * 100 || 1"></div>
          <div class="bar-p2" [style.flex]="s.player2.firstServePercentage * 100 || 1"></div>
        </div>
      </div>
      <span class="val val-right" [ngClass]="{ leading: s.player2.firstServePercentage > s.player1.firstServePercentage }" data-testid="val-p2-first-serve">{{ pct(s.player2.firstServePercentage) }}</span>

      <!-- 2. Aufschlag % -->
      <span class="val val-left" style="color:#0ea5e9">{{ pct(s.player1.secondServePercentage) }}</span>
      <div data-testid="stat-row">
        <div class="stat-label">2. Aufschlag %</div>
        <div class="bar">
          <div class="bar-p1" [style.flex]="s.player1.secondServePercentage * 100 || 1"></div>
          <div class="bar-p2" [style.flex]="s.player2.secondServePercentage * 100 || 1"></div>
        </div>
      </div>
      <span class="val val-right" [ngClass]="{ leading: s.player2.secondServePercentage > s.player1.secondServePercentage }">{{ pct(s.player2.secondServePercentage) }}</span>

      <!-- Break Points -->
      <span class="val val-left" style="color:#0ea5e9" data-testid="val-p1-break-points">{{ s.player1.breakPointsWon }}/{{ s.player1.breakPointsFaced }}</span>
      <div data-testid="stat-row">
        <div class="stat-label">Break Points</div>
        <div class="bar">
          <div class="bar-p1" [style.flex]="s.player1.breakPointsFaced || 1"></div>
          <div class="bar-p2" [style.flex]="s.player2.breakPointsFaced || 1"></div>
        </div>
      </div>
      <span class="val val-right" data-testid="val-p2-break-points">{{ s.player2.breakPointsWon }}/{{ s.player2.breakPointsFaced }}</span>

      <div style="grid-column:span 3" class="divider"></div>

      <!-- Vorhand / Rückhand -->
      <span class="val val-left" style="color:#0ea5e9;font-size:11px;">VH {{ pct(s.player1.forehandPercentage) }}</span>
      <div data-testid="stat-row">
        <div class="stat-label">Vorhand / Rückhand</div>
        <div class="bar">
          <div class="bar-p1" [style.flex]="s.player1.forehandPercentage * 100 || 1"></div>
          <div class="bar-p2" [style.flex]="s.player2.forehandPercentage * 100 || 1"></div>
        </div>
      </div>
      <span class="val val-right" style="font-size:11px;">VH {{ pct(s.player2.forehandPercentage) }}</span>

      <span class="val val-left" style="color:#94a3b8;font-size:11px;">RH {{ pct(1 - s.player1.forehandPercentage) }}</span>
      <div></div>
      <span class="val val-right" style="font-size:11px;">RH {{ pct(1 - s.player2.forehandPercentage) }}</span>

    </div>
  }

  <div class="back-row">
    <button mat-flat-button data-testid="back-btn" (click)="goBack()">
      Zur Spielerübersicht
    </button>
  </div>

</div>
```

- [ ] **Step 4: Run Cypress tests**

```bash
cd frontend && npx cypress run --component --spec "src/app/features/matches/statistics/statistics.component.cy.ts" 2>&1 | tail -20
```

Expected: Alle Tests grün.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/features/matches/statistics/
git commit -m "feat(statistics): add StatisticsComponent with ATP-style layout"
```

---

## Task 6: Routing + Score-Navigation nach Matchende

**Files:**
- Modify: `frontend/src/app/app.routes.ts`
- Modify: `frontend/src/app/features/matches/score/score.component.ts`

- [ ] **Step 1: Add route to app.routes.ts**

Füge in `frontend/src/app/app.routes.ts` nach der `matches/:id/score`-Route ein:

```typescript
{
  path: 'matches/:id/statistics',
  canActivate: [authGuard],
  loadComponent: () =>
    import('./features/matches/statistics/statistics.component').then(m => m.StatisticsComponent)
},
```

Die vollständige Datei danach:

```typescript
import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'players', pathMatch: 'full' },
  {
    path: 'players',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/players/players.component').then(m => m.PlayersComponent)
  },
  {
    path: 'matches/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/matches/match-setup/match-setup.component').then(m => m.MatchSetupComponent)
  },
  {
    path: 'matches/:id/score',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/matches/score/score.component').then(m => m.ScoreComponent)
  },
  {
    path: 'matches/:id/statistics',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/matches/statistics/statistics.component').then(m => m.StatisticsComponent)
  },
];
```

- [ ] **Step 2: Update handlePointResponse in score.component.ts**

Ersetze `handlePointResponse` und füge `navigateToStatistics` hinzu.

**Aktueller Code** (`score.component.ts`, ca. Zeile 345):
```typescript
private handlePointResponse(updated: MatchWithScore): void {
  const prev = this.matchData();
  if (prev && updated.score.currentSet > prev.score.currentSet) {
    let p1 = prev.score.gamesPlayer1;
    let p2 = prev.score.gamesPlayer2;
    if (updated.score.setsPlayer1 > prev.score.setsPlayer1) { p1 += 1; } else { p2 += 1; }
    this.setHistory.update(h => [...h, { p1, p2 }]);
  }
  this.matchData.set(updated);
  if (updated.score.isDone) this.loadMatch();
}
```

**Ersetzen durch:**
```typescript
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
      // Backend did not increment currentSet on match end — add final set manually
      const p1Won = updated.score.setsPlayer1 > prev.score.setsPlayer1;
      const p1 = updated.score.gamesPlayer1 + (p1Won ? 1 : 0);
      const p2 = updated.score.gamesPlayer2 + (p1Won ? 0 : 1);
      this.setHistory.update(h => [...h, { p1, p2 }]);
    }
    this.navigateToStatistics();
    return;
  }
}

private navigateToStatistics(): void {
  const sets = this.setHistory().map(s => `${s.p1}-${s.p2}`).join(',');
  this.router.navigate(
    ['/matches', this.matchId, 'statistics'],
    { queryParams: { sets, p1: this.player1Name(), p2: this.player2Name() } }
  );
}
```

- [ ] **Step 3: Run Angular unit tests**

```bash
cd frontend && npx ng test --watch=false 2>&1 | tail -20
```

Expected: Alle bestehenden Tests grün (score.component.spec.ts, app.spec.ts).

- [ ] **Step 4: Run Cypress component tests**

```bash
npx cypress run --component 2>&1 | tail -20
```

Expected: Alle Tests grün.

- [ ] **Step 5: Run full backend tests**

```bash
cd ../backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test 2>&1 | tail -15
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/app.routes.ts \
        frontend/src/app/features/matches/score/score.component.ts
git commit -m "feat(statistics): navigate to statistics page on match end [TEN-14]"
```
