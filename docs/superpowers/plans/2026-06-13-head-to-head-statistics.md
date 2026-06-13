# Head-to-Head Statistics (FA-08) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement `GET /api/statistics/head-to-head?player1={id}&player2={id}` (FA-08) — aggregated per-player metrics across all matches two players have played against each other — plus an Angular UI and SAD update.

**Architecture:** New use case in `statistics-module` streams the raw points of every shared match, accumulates raw counters **per real player UUID** (mapping each match's positional 1/2 to the actual player), and derives percentages once. Match/set balance comes from the persisted `MatchScore` (completed matches only). A standalone Angular route renders a mirrored comparison view; the Players page links into it.

**Tech Stack:** Spring Boot 4 (Java 25, Gradle Kotlin DSL, Clean Architecture), JUnit 5 + Mockito + AssertJ + Testcontainers; Angular 21 standalone + Angular Material + Cypress component tests.

**Spec:** `docs/superpowers/specs/2026-06-13-ten46-head-to-head-statistics-design.md`

**Build prefix for all Gradle commands:** `JAVA_HOME=/opt/java/jdk-25.0.1` (run from `backend/`). Integration tests additionally need `DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true`.

---

## File Structure

**Backend — `match-module`:**
- Create: `match-module/src/main/java/com/cas/tsas/match/application/port/out/LoadMatchesByPlayersPort.java` — output port: matches between two players.
- Modify: `match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/repository/MatchJpaRepository.java` — add the `findMatchesBetween` query.
- Modify: `match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/repository/MatchPersistenceAdapter.java` — implement the new port.

**Backend — `statistics-module`:**
- Modify: `statistics-module/build.gradle.kts` — depend on `:player-module`.
- Create: `statistics-module/src/main/java/com/cas/tsas/statistics/domain/model/HeadToHeadPlayerStats.java` — immutable per-player result (counts + percentages).
- Create: `statistics-module/src/main/java/com/cas/tsas/statistics/domain/model/HeadToHeadStatistics.java` — aggregate result.
- Create: `statistics-module/src/main/java/com/cas/tsas/statistics/application/port/in/ComputeHeadToHeadStatisticsUseCase.java` — input port.
- Create: `statistics-module/src/main/java/com/cas/tsas/statistics/application/service/HeadToHeadStatisticsService.java` — aggregation logic + validation.
- Create: `statistics-module/src/main/java/com/cas/tsas/statistics/infrastructure/web/dto/HeadToHeadPlayerStatsDto.java`
- Create: `statistics-module/src/main/java/com/cas/tsas/statistics/infrastructure/web/dto/HeadToHeadStatisticsDto.java`
- Create: `statistics-module/src/main/java/com/cas/tsas/statistics/infrastructure/web/HeadToHeadController.java`
- Test: `statistics-module/src/test/java/com/cas/tsas/statistics/application/service/HeadToHeadStatisticsServiceTest.java`
- Test: `app/src/test/java/com/cas/tsas/statistics/HeadToHeadApiIT.java`

**Frontend:**
- Modify: `frontend/src/app/core/models/statistics.model.ts` — add H2H interfaces.
- Modify: `frontend/src/app/core/services/api.service.ts` — add `getHeadToHead`.
- Create: `frontend/src/app/features/statistics/head-to-head/head-to-head.component.ts`
- Create: `frontend/src/app/features/statistics/head-to-head/head-to-head.component.html`
- Test: `frontend/src/app/features/statistics/head-to-head/head-to-head.component.cy.ts`
- Modify: `frontend/src/app/app.routes.ts` — add `/statistics/head-to-head`.
- Modify: `frontend/src/app/features/players/players.component.ts` + `.html` — toolbar button + row action.
- Modify: `frontend/src/app/features/players/players.component.cy.ts` — assert the entry points.

**Docs:**
- Modify: `doc/sad/TSaS_SAD_arc42_1.md` — FA-08 status, remove Net Points clause, document inclusion rule + module dependency.

---

## Task 1: Output port + repository query for matches between two players

**Files:**
- Create: `backend/match-module/src/main/java/com/cas/tsas/match/application/port/out/LoadMatchesByPlayersPort.java`
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/repository/MatchJpaRepository.java`
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/repository/MatchPersistenceAdapter.java`

This is plumbing the service unit test (Task 5) and the IT (Task 8) depend on. It is verified by compilation here and exercised end-to-end by the IT.

- [ ] **Step 1: Create the output port**

`LoadMatchesByPlayersPort.java`:

```java
package com.cas.tsas.match.application.port.out;

import com.cas.tsas.match.domain.model.Match;

import java.util.List;
import java.util.UUID;

/** Loads every match played between two players, regardless of who was player 1 or 2. */
public interface LoadMatchesByPlayersPort {

    List<Match> loadMatchesBetween(UUID playerA, UUID playerB);
}
```

- [ ] **Step 2: Add the repository query**

In `MatchJpaRepository.java`, add this method inside the interface (after `existsByStatusAndPlayerId`):

```java
    @Query("SELECT m FROM MatchJpaEntity m WHERE (m.player1Id = :a AND m.player2Id = :b) "
            + "OR (m.player1Id = :b AND m.player2Id = :a)")
    List<MatchJpaEntity> findMatchesBetween(@Param("a") UUID a, @Param("b") UUID b);
```

(`@Query`, `@Param`, `List`, `UUID` are already imported in this file.)

- [ ] **Step 3: Implement the port in the adapter**

In `MatchPersistenceAdapter.java`, add `LoadMatchesByPlayersPort` to the `implements` list:

```java
public class MatchPersistenceAdapter implements LoadMatchPort, SaveMatchPort, HasMatchesPort, FindActiveMatchPort, LoadMatchesByPlayersPort {
```

Add the import `import com.cas.tsas.match.application.port.out.LoadMatchesByPlayersPort;` and this method:

```java
    @Override
    public List<Match> loadMatchesBetween(UUID playerA, UUID playerB) {
        return repository.findMatchesBetween(playerA, playerB).stream()
                .map(mapper::toDomain)
                .toList();
    }
```

- [ ] **Step 4: Verify it compiles**

Run: `JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :match-module:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add backend/match-module
git commit -m "feat(match): add LoadMatchesByPlayersPort for head-to-head queries"
```

---

## Task 2: Let statistics-module depend on player-module

**Files:**
- Modify: `backend/statistics-module/build.gradle.kts`

The service must validate player existence via `LoadPlayerPort` (player-module).

- [ ] **Step 1: Add the dependency**

Replace the contents of `statistics-module/build.gradle.kts` with:

```kotlin
dependencies {
    implementation(project(":common-module"))
    implementation(project(":match-module"))
    implementation(project(":player-module"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
}
```

- [ ] **Step 2: Verify it resolves**

Run: `JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :statistics-module:dependencies --configuration compileClasspath -q`
Expected: BUILD SUCCESSFUL and `:player-module` listed.

- [ ] **Step 3: Commit**

```bash
git add backend/statistics-module/build.gradle.kts
git commit -m "build(statistics): depend on player-module for existence checks"
```

---

## Task 3: Head-to-Head domain result models

**Files:**
- Create: `backend/statistics-module/src/main/java/com/cas/tsas/statistics/domain/model/HeadToHeadPlayerStats.java`
- Create: `backend/statistics-module/src/main/java/com/cas/tsas/statistics/domain/model/HeadToHeadStatistics.java`

- [ ] **Step 1: Create `HeadToHeadPlayerStats`**

```java
package com.cas.tsas.statistics.domain.model;

import java.util.UUID;

/**
 * Aggregated head-to-head statistics for one player across all matches against one opponent.
 * Carries both raw counts and derived percentages so the API can present "23 (45%)".
 * Net points are intentionally not part of FA-08.
 */
public record HeadToHeadPlayerStats(
        UUID playerId,
        // serve
        double firstServePercentage,
        double firstServeWonPercentage,
        double secondServeWonPercentage,
        int aces,
        int doubleFaults,
        // return
        double returnPointsWonFirstPercentage,
        double returnPointsWonSecondPercentage,
        int breakPointsWon,
        int breakPointsPlayed,
        double breakPointsWonPercentage,
        double returnGamesWonPercentage,
        // rally
        int winners,
        int unforcedErrors,
        double winnersPercentage,
        double unforcedErrorPercentage,
        // balance (completed matches only)
        int matchesWon,
        int matchesLost,
        int setsWon,
        int setsLost
) {}
```

- [ ] **Step 2: Create `HeadToHeadStatistics`**

```java
package com.cas.tsas.statistics.domain.model;

import java.util.UUID;

/**
 * Result aggregate for FA-08: two players compared over all their shared matches.
 * {@code matchesPlayed} counts only completed matches (those with a decided winner).
 */
public record HeadToHeadStatistics(
        UUID player1Id,
        UUID player2Id,
        int matchesPlayed,
        HeadToHeadPlayerStats player1,
        HeadToHeadPlayerStats player2
) {}
```

- [ ] **Step 3: Verify it compiles**

Run: `JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :statistics-module:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add backend/statistics-module/src/main/java/com/cas/tsas/statistics/domain/model
git commit -m "feat(statistics): add head-to-head result models"
```

---

## Task 4: Input port (use case interface)

**Files:**
- Create: `backend/statistics-module/src/main/java/com/cas/tsas/statistics/application/port/in/ComputeHeadToHeadStatisticsUseCase.java`

- [ ] **Step 1: Create the input port**

```java
package com.cas.tsas.statistics.application.port.in;

import com.cas.tsas.statistics.domain.model.HeadToHeadStatistics;

import java.util.UUID;

public interface ComputeHeadToHeadStatisticsUseCase {

    /**
     * Aggregates head-to-head statistics for the two players over all their shared matches.
     *
     * @throws IllegalArgumentException if both ids are equal (→ 400)
     * @throws com.cas.tsas.player.domain.exception.PlayerNotFoundException if a player does not exist (→ 404)
     */
    HeadToHeadStatistics compute(UUID player1Id, UUID player2Id);
}
```

- [ ] **Step 2: Verify it compiles**

Run: `JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :statistics-module:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add backend/statistics-module/src/main/java/com/cas/tsas/statistics/application/port/in/ComputeHeadToHeadStatisticsUseCase.java
git commit -m "feat(statistics): add ComputeHeadToHeadStatisticsUseCase port"
```

---

## Task 5: Head-to-Head service (TDD)

**Files:**
- Test: `backend/statistics-module/src/test/java/com/cas/tsas/statistics/application/service/HeadToHeadStatisticsServiceTest.java`
- Create: `backend/statistics-module/src/main/java/com/cas/tsas/statistics/application/service/HeadToHeadStatisticsService.java`

- [ ] **Step 1: Write the failing test**

`HeadToHeadStatisticsServiceTest.java`:

```java
package com.cas.tsas.statistics.application.service;

import com.cas.tsas.match.application.port.out.LoadMatchScorePort;
import com.cas.tsas.match.application.port.out.LoadMatchesByPlayersPort;
import com.cas.tsas.match.application.port.out.LoadPointsByMatchPort;
import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchScore;
import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.match.domain.model.Point;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.player.domain.exception.PlayerNotFoundException;
import com.cas.tsas.player.domain.model.Player;
import com.cas.tsas.statistics.domain.model.HeadToHeadStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class HeadToHeadStatisticsServiceTest {

    private LoadPlayerPort loadPlayerPort;
    private LoadMatchesByPlayersPort loadMatchesByPlayersPort;
    private LoadPointsByMatchPort loadPointsByMatchPort;
    private LoadMatchScorePort loadMatchScorePort;
    private HeadToHeadStatisticsService service;

    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        loadPlayerPort = Mockito.mock(LoadPlayerPort.class);
        loadMatchesByPlayersPort = Mockito.mock(LoadMatchesByPlayersPort.class);
        loadPointsByMatchPort = Mockito.mock(LoadPointsByMatchPort.class);
        loadMatchScorePort = Mockito.mock(LoadMatchScorePort.class);
        service = new HeadToHeadStatisticsService(
                loadPlayerPort, loadMatchesByPlayersPort, loadPointsByMatchPort, loadMatchScorePort);

        // both players exist by default
        when(loadPlayerPort.loadPlayer(alice)).thenReturn(Optional.of(player(alice)));
        when(loadPlayerPort.loadPlayer(bob)).thenReturn(Optional.of(player(bob)));
    }

    private Player player(UUID id) {
        Player p = new Player();
        p.setId(id);
        return p;
    }

    private Match match(UUID id, UUID p1, UUID p2) {
        return new Match(id, p1, p2, 2, false, false, MatchStatus.COMPLETED);
    }

    /** Point builder: set/game/point, winner (1/2), type, serving player, serve attempt, break point. */
    private Point p(int set, int game, int pt, int winner, PointType type,
                    Integer servingPlayer, Integer serveAttempt, boolean breakPoint) {
        return new Point(UUID.randomUUID(), UUID.randomUUID(), set, game, pt, winner,
                type, null, null, servingPlayer, breakPoint, null, serveAttempt);
    }

    private MatchScore score(UUID matchId, String winner, int sets1, int sets2) {
        return new MatchScore(UUID.randomUUID(), matchId, 0, 0, 0, 0, sets1, sets2,
                false, null, 2, true, winner, 0, 0, null);
    }

    @Test
    void throws_when_both_players_are_equal() {
        assertThatThrownBy(() -> service.compute(alice, alice))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throws_when_player1_unknown() {
        when(loadPlayerPort.loadPlayer(alice)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.compute(alice, bob))
                .isInstanceOf(PlayerNotFoundException.class);
    }

    @Test
    void throws_when_player2_unknown() {
        when(loadPlayerPort.loadPlayer(bob)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.compute(alice, bob))
                .isInstanceOf(PlayerNotFoundException.class);
    }

    @Test
    void returns_zeroed_stats_when_no_matches() {
        when(loadMatchesByPlayersPort.loadMatchesBetween(alice, bob)).thenReturn(List.of());

        HeadToHeadStatistics result = service.compute(alice, bob);

        assertThat(result.matchesPlayed()).isZero();
        assertThat(result.player1().playerId()).isEqualTo(alice);
        assertThat(result.player2().playerId()).isEqualTo(bob);
        assertThat(result.player1().winnersPercentage()).isZero();
        assertThat(result.player1().matchesWon()).isZero();
    }

    @Test
    void aggregates_winners_and_unforced_errors_as_percentage_of_all_points() {
        UUID m = UUID.randomUUID();
        when(loadMatchesByPlayersPort.loadMatchesBetween(alice, bob))
                .thenReturn(List.of(match(m, alice, bob)));
        when(loadMatchScorePort.loadMatchScore(m)).thenReturn(Optional.empty());
        // 4 points: alice winner, alice winner, bob unforced error (alice wins point), quick point
        when(loadPointsByMatchPort.loadPointsByMatch(m)).thenReturn(List.of(
                p(1, 1, 1, 1, PointType.WINNER, null, null, false),
                p(1, 1, 2, 1, PointType.WINNER, null, null, false),
                p(1, 1, 3, 1, PointType.UNFORCED_ERROR, null, null, false), // alice wins, bob erred
                p(1, 1, 4, 2, null, null, null, false)
        ));

        HeadToHeadStatistics r = service.compute(alice, bob);

        // 4 total points; alice has 2 winners, bob has 1 unforced error
        assertThat(r.player1().winners()).isEqualTo(2);
        assertThat(r.player1().winnersPercentage()).isEqualTo(0.5, within(1e-9));
        assertThat(r.player2().unforcedErrors()).isEqualTo(1);
        assertThat(r.player2().unforcedErrorPercentage()).isEqualTo(0.25, within(1e-9));
    }

    @Test
    void maps_positional_players_to_real_ids_across_matches() {
        UUID m1 = UUID.randomUUID(); // alice is player 1
        UUID m2 = UUID.randomUUID(); // alice is player 2 (positions swapped)
        when(loadMatchesByPlayersPort.loadMatchesBetween(alice, bob))
                .thenReturn(List.of(match(m1, alice, bob), match(m2, bob, alice)));
        when(loadMatchScorePort.loadMatchScore(any())).thenReturn(Optional.empty());
        when(loadPointsByMatchPort.loadPointsByMatch(m1)).thenReturn(List.of(
                p(1, 1, 1, 1, PointType.WINNER, null, null, false))); // pos1=alice winner
        when(loadPointsByMatchPort.loadPointsByMatch(m2)).thenReturn(List.of(
                p(1, 1, 1, 2, PointType.WINNER, null, null, false))); // pos2=alice winner

        HeadToHeadStatistics r = service.compute(alice, bob);

        assertThat(r.player1().winners()).isEqualTo(2); // both winners belong to alice
        assertThat(r.player2().winners()).isZero();
    }

    @Test
    void computes_serve_and_return_metrics_from_serve_attempts() {
        UUID m = UUID.randomUUID();
        when(loadMatchesByPlayersPort.loadMatchesBetween(alice, bob))
                .thenReturn(List.of(match(m, alice, bob)));
        when(loadMatchScorePort.loadMatchScore(m)).thenReturn(Optional.empty());
        // alice (pos 1) serves all four points:
        //  - 1st serve, alice wins   -> firstServeWon
        //  - 1st serve, bob wins     -> bob returnFirstWon
        //  - 2nd serve, alice wins   -> secondServeWon
        //  - 2nd serve double fault, bob wins -> bob returnSecondWon
        when(loadPointsByMatchPort.loadPointsByMatch(m)).thenReturn(List.of(
                p(1, 1, 1, 1, PointType.WINNER, 1, 1, false),
                p(1, 1, 2, 2, PointType.WINNER, 1, 1, false),
                p(1, 1, 3, 1, PointType.WINNER, 1, 2, false),
                p(1, 1, 4, 2, PointType.DOUBLE_FAULT, 1, 2, false)
        ));

        HeadToHeadStatistics r = service.compute(alice, bob);

        // alice serve: 2 first-serve points (1 won), 2 second-serve points (1 won)
        assertThat(r.player1().firstServePercentage()).isEqualTo(0.5, within(1e-9)); // 2 first of 4 attempts
        assertThat(r.player1().firstServeWonPercentage()).isEqualTo(0.5, within(1e-9));
        assertThat(r.player1().secondServeWonPercentage()).isEqualTo(0.5, within(1e-9));
        assertThat(r.player1().doubleFaults()).isEqualTo(1);
        // bob return: 2 first-serve returns (1 won), 2 second-serve returns (1 won)
        assertThat(r.player2().returnPointsWonFirstPercentage()).isEqualTo(0.5, within(1e-9));
        assertThat(r.player2().returnPointsWonSecondPercentage()).isEqualTo(0.5, within(1e-9));
    }

    @Test
    void computes_break_points_won_for_returner() {
        UUID m = UUID.randomUUID();
        when(loadMatchesByPlayersPort.loadMatchesBetween(alice, bob))
                .thenReturn(List.of(match(m, alice, bob)));
        when(loadMatchScorePort.loadMatchScore(m)).thenReturn(Optional.empty());
        // alice (pos1) serves two break points; bob (returner) wins one of them
        when(loadPointsByMatchPort.loadPointsByMatch(m)).thenReturn(List.of(
                p(1, 1, 1, 2, null, 1, 1, true),  // break point, bob wins -> converted
                p(1, 2, 1, 1, null, 1, 1, true)   // break point, alice (server) wins -> saved
        ));

        HeadToHeadStatistics r = service.compute(alice, bob);

        assertThat(r.player2().breakPointsPlayed()).isEqualTo(2);
        assertThat(r.player2().breakPointsWon()).isEqualTo(1);
        assertThat(r.player2().breakPointsWonPercentage()).isEqualTo(0.5, within(1e-9));
    }

    @Test
    void computes_return_games_won_from_game_winner() {
        UUID m = UUID.randomUUID();
        when(loadMatchesByPlayersPort.loadMatchesBetween(alice, bob))
                .thenReturn(List.of(match(m, alice, bob)));
        when(loadMatchScorePort.loadMatchScore(m)).thenReturn(Optional.empty());
        // Game (set1,game1) served by alice (pos1); last point won by bob -> bob breaks (return game won).
        // Game (set1,game2) served by bob (pos2); last point won by bob -> alice's return game lost.
        when(loadPointsByMatchPort.loadPointsByMatch(m)).thenReturn(List.of(
                p(1, 1, 1, 1, null, 1, null, false),
                p(1, 1, 2, 2, null, 1, null, false), // last point of game1, bob wins -> bob wins return game
                p(1, 2, 1, 2, null, 2, null, false),
                p(1, 2, 2, 2, null, 2, null, false)  // last point of game2, bob wins his serve
        ));

        HeadToHeadStatistics r = service.compute(alice, bob);

        // bob returned in game1 and won it
        assertThat(r.player2().returnGamesWonPercentage()).isEqualTo(1.0, within(1e-9));
        // alice returned in game2 and lost it
        assertThat(r.player1().returnGamesWonPercentage()).isZero();
    }

    @Test
    void counts_match_and_set_balance_only_for_completed_matches() {
        UUID m1 = UUID.randomUUID(); // alice pos1, completed, alice wins 2:0
        UUID m2 = UUID.randomUUID(); // alice pos2, completed, bob(pos1) wins 2:1
        UUID m3 = UUID.randomUUID(); // in progress, no winner -> ignored for balance
        when(loadMatchesByPlayersPort.loadMatchesBetween(alice, bob))
                .thenReturn(List.of(match(m1, alice, bob), match(m2, bob, alice), match(m3, alice, bob)));
        when(loadPointsByMatchPort.loadPointsByMatch(any())).thenReturn(List.of());
        when(loadMatchScorePort.loadMatchScore(m1)).thenReturn(Optional.of(score(m1, "PLAYER1", 2, 0)));
        when(loadMatchScorePort.loadMatchScore(m2)).thenReturn(Optional.of(score(m2, "PLAYER1", 2, 1)));
        when(loadMatchScorePort.loadMatchScore(m3)).thenReturn(Optional.of(score(m3, null, 1, 0)));

        HeadToHeadStatistics r = service.compute(alice, bob);

        assertThat(r.matchesPlayed()).isEqualTo(2);
        // m1: alice wins (2:0). m2: positions swapped, PLAYER1=bob wins (2:1) -> alice loses, alice sets += 1
        assertThat(r.player1().matchesWon()).isEqualTo(1);
        assertThat(r.player1().matchesLost()).isEqualTo(1);
        assertThat(r.player1().setsWon()).isEqualTo(3);  // 2 (m1) + 1 (m2 as pos2)
        assertThat(r.player1().setsLost()).isEqualTo(2); // 0 (m1) + 2 (m2 as pos2)
        assertThat(r.player2().matchesWon()).isEqualTo(1);
        assertThat(r.player2().setsWon()).isEqualTo(2);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :statistics-module:test --tests "*HeadToHeadStatisticsServiceTest*"`
Expected: FAIL — `HeadToHeadStatisticsService` does not exist (compilation error).

- [ ] **Step 3: Implement the service**

`HeadToHeadStatisticsService.java`:

```java
package com.cas.tsas.statistics.application.service;

import com.cas.tsas.match.application.port.out.LoadMatchScorePort;
import com.cas.tsas.match.application.port.out.LoadMatchesByPlayersPort;
import com.cas.tsas.match.application.port.out.LoadPointsByMatchPort;
import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchScore;
import com.cas.tsas.match.domain.model.Point;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.player.domain.exception.PlayerNotFoundException;
import com.cas.tsas.statistics.application.port.in.ComputeHeadToHeadStatisticsUseCase;
import com.cas.tsas.statistics.domain.model.HeadToHeadPlayerStats;
import com.cas.tsas.statistics.domain.model.HeadToHeadStatistics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Computes head-to-head statistics (FA-08) on the fly from the recorded points of all matches
 * two players have played against each other. Raw counters are accumulated per real player UUID
 * (each match's positional player 1/2 is mapped to the actual player), then derived into
 * percentages. Match/set balance is taken from the persisted {@link MatchScore} of completed
 * matches only.
 */
@Service
public class HeadToHeadStatisticsService implements ComputeHeadToHeadStatisticsUseCase {

    private final LoadPlayerPort loadPlayerPort;
    private final LoadMatchesByPlayersPort loadMatchesByPlayersPort;
    private final LoadPointsByMatchPort loadPointsByMatchPort;
    private final LoadMatchScorePort loadMatchScorePort;

    public HeadToHeadStatisticsService(LoadPlayerPort loadPlayerPort,
                                       LoadMatchesByPlayersPort loadMatchesByPlayersPort,
                                       LoadPointsByMatchPort loadPointsByMatchPort,
                                       LoadMatchScorePort loadMatchScorePort) {
        this.loadPlayerPort = loadPlayerPort;
        this.loadMatchesByPlayersPort = loadMatchesByPlayersPort;
        this.loadPointsByMatchPort = loadPointsByMatchPort;
        this.loadMatchScorePort = loadMatchScorePort;
    }

    @Override
    @Transactional(readOnly = true)
    public HeadToHeadStatistics compute(UUID player1Id, UUID player2Id) {
        if (player1Id.equals(player2Id)) {
            throw new IllegalArgumentException("player1 and player2 must be different");
        }
        loadPlayerPort.loadPlayer(player1Id).orElseThrow(() -> new PlayerNotFoundException(player1Id));
        loadPlayerPort.loadPlayer(player2Id).orElseThrow(() -> new PlayerNotFoundException(player2Id));

        Accumulator acc1 = new Accumulator();
        Accumulator acc2 = new Accumulator();
        int totalPoints = 0;
        int matchesPlayed = 0;

        for (Match match : loadMatchesByPlayersPort.loadMatchesBetween(player1Id, player2Id)) {
            // map the match's positional player 1/2 onto acc1 (player1Id) / acc2 (player2Id)
            boolean p1IsPosition1 = match.getPlayer1Id().equals(player1Id);
            Accumulator pos1 = p1IsPosition1 ? acc1 : acc2;
            Accumulator pos2 = p1IsPosition1 ? acc2 : acc1;

            List<Point> points = loadPointsByMatchPort.loadPointsByMatch(match.getId());
            for (Point p : points) {
                totalPoints++;
                accumulatePoint(p, pos1, pos2);
            }
            accumulateReturnGames(points, pos1, pos2);

            Optional<MatchScore> score = loadMatchScorePort.loadMatchScore(match.getId());
            if (score.isPresent() && score.get().getWinner() != null) {
                matchesPlayed++;
                accumulateBalance(score.get(), pos1, pos2);
            }
        }

        return new HeadToHeadStatistics(player1Id, player2Id, matchesPlayed,
                acc1.toStats(player1Id, totalPoints), acc2.toStats(player2Id, totalPoints));
    }

    private void accumulatePoint(Point p, Accumulator pos1, Accumulator pos2) {
        Accumulator winnerAcc = p.getWinner() == 1 ? pos1 : pos2;
        Accumulator loserAcc = p.getWinner() == 1 ? pos2 : pos1;

        if (p.getPointType() == PointType.WINNER) {
            winnerAcc.winners++;
        } else if (p.getPointType() == PointType.UNFORCED_ERROR) {
            loserAcc.unforcedErrors++; // the error is committed by the player who lost the point
        }

        Integer server = p.getServingPlayer();
        if (server != null) {
            Accumulator serverAcc = server == 1 ? pos1 : pos2;
            Accumulator returnerAcc = server == 1 ? pos2 : pos1;
            if (p.getPointType() == PointType.ACE) serverAcc.aces++;
            if (p.getPointType() == PointType.DOUBLE_FAULT) serverAcc.doubleFaults++;

            Integer attempt = p.getServeAttempt();
            if (attempt != null) {
                serverAcc.serveAttemptsTotal++;
                boolean serverWon = p.getWinner() == server;
                if (attempt == 1) {
                    serverAcc.firstServesIn++;
                    serverAcc.firstServePlayed++;
                    returnerAcc.returnFirstPlayed++;
                    if (serverWon) serverAcc.firstServeWon++;
                    else returnerAcc.returnFirstWon++;
                } else if (attempt == 2) {
                    serverAcc.secondServePlayed++;
                    returnerAcc.returnSecondPlayed++;
                    if (serverWon) serverAcc.secondServeWon++;
                    else returnerAcc.returnSecondWon++;
                }
            }

            if (p.isBreakPoint()) {
                returnerAcc.breakPointsPlayed++;
                if (p.getWinner() != server) returnerAcc.breakPointsWon++;
            }
        }
    }

    /**
     * Credits return games: a game is grouped by (set, game); its server is the first non-null
     * servingPlayer seen, its winner is the winner of the highest-numbered point. The player who
     * did not serve the game played a return game there, and won it if they won the last point.
     */
    private void accumulateReturnGames(List<Point> points, Accumulator pos1, Accumulator pos2) {
        // points arrive ordered by set, game, point (repository contract)
        int i = 0;
        while (i < points.size()) {
            Point first = points.get(i);
            int set = first.getSetNumber();
            int game = first.getGameNumber();
            Integer server = null;
            int lastWinner = first.getWinner();
            int j = i;
            while (j < points.size()
                    && points.get(j).getSetNumber() == set
                    && points.get(j).getGameNumber() == game) {
                if (server == null) server = points.get(j).getServingPlayer();
                lastWinner = points.get(j).getWinner(); // ordered, so the last seen is the game-deciding point
                j++;
            }
            if (server != null) {
                Accumulator returnerAcc = server == 1 ? pos2 : pos1;
                int returnerPosition = server == 1 ? 2 : 1;
                returnerAcc.returnGamesPlayed++;
                if (lastWinner == returnerPosition) returnerAcc.returnGamesWon++;
            }
            i = j;
        }
    }

    private void accumulateBalance(MatchScore score, Accumulator pos1, Accumulator pos2) {
        pos1.setsWon += score.getSetsPlayer1();
        pos1.setsLost += score.getSetsPlayer2();
        pos2.setsWon += score.getSetsPlayer2();
        pos2.setsLost += score.getSetsPlayer1();
        if ("PLAYER1".equals(score.getWinner())) {
            pos1.matchesWon++;
            pos2.matchesLost++;
        } else if ("PLAYER2".equals(score.getWinner())) {
            pos2.matchesWon++;
            pos1.matchesLost++;
        }
    }

    /** Mutable per-player tally; derived into the immutable {@link HeadToHeadPlayerStats}. */
    private static final class Accumulator {
        int winners, unforcedErrors, aces, doubleFaults;
        int firstServesIn, serveAttemptsTotal;
        int firstServeWon, firstServePlayed;
        int secondServeWon, secondServePlayed;
        int returnFirstWon, returnFirstPlayed;
        int returnSecondWon, returnSecondPlayed;
        int breakPointsWon, breakPointsPlayed;
        int returnGamesWon, returnGamesPlayed;
        int matchesWon, matchesLost, setsWon, setsLost;

        HeadToHeadPlayerStats toStats(UUID playerId, int totalPoints) {
            return new HeadToHeadPlayerStats(
                    playerId,
                    ratio(firstServesIn, serveAttemptsTotal),
                    ratio(firstServeWon, firstServePlayed),
                    ratio(secondServeWon, secondServePlayed),
                    aces,
                    doubleFaults,
                    ratio(returnFirstWon, returnFirstPlayed),
                    ratio(returnSecondWon, returnSecondPlayed),
                    breakPointsWon,
                    breakPointsPlayed,
                    ratio(breakPointsWon, breakPointsPlayed),
                    ratio(returnGamesWon, returnGamesPlayed),
                    winners,
                    unforcedErrors,
                    ratio(winners, totalPoints),
                    ratio(unforcedErrors, totalPoints),
                    matchesWon,
                    matchesLost,
                    setsWon,
                    setsLost);
        }

        private static double ratio(int num, int den) {
            return den == 0 ? 0.0 : (double) num / den;
        }
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :statistics-module:test --tests "*HeadToHeadStatisticsServiceTest*"`
Expected: PASS (all test methods green).

- [ ] **Step 5: Commit**

```bash
git add backend/statistics-module/src/main/java/com/cas/tsas/statistics/application/service/HeadToHeadStatisticsService.java \
        backend/statistics-module/src/test/java/com/cas/tsas/statistics/application/service/HeadToHeadStatisticsServiceTest.java
git commit -m "feat(statistics): compute head-to-head statistics across shared matches"
```

---

## Task 6: Web DTOs

**Files:**
- Create: `backend/statistics-module/src/main/java/com/cas/tsas/statistics/infrastructure/web/dto/HeadToHeadPlayerStatsDto.java`
- Create: `backend/statistics-module/src/main/java/com/cas/tsas/statistics/infrastructure/web/dto/HeadToHeadStatisticsDto.java`

- [ ] **Step 1: Create `HeadToHeadPlayerStatsDto`**

```java
package com.cas.tsas.statistics.infrastructure.web.dto;

import com.cas.tsas.statistics.domain.model.HeadToHeadPlayerStats;

import java.util.UUID;

public record HeadToHeadPlayerStatsDto(
        UUID playerId,
        double firstServePercentage,
        double firstServeWonPercentage,
        double secondServeWonPercentage,
        int aces,
        int doubleFaults,
        double returnPointsWonFirstPercentage,
        double returnPointsWonSecondPercentage,
        int breakPointsWon,
        int breakPointsPlayed,
        double breakPointsWonPercentage,
        double returnGamesWonPercentage,
        int winners,
        int unforcedErrors,
        double winnersPercentage,
        double unforcedErrorPercentage,
        int matchesWon,
        int matchesLost,
        int setsWon,
        int setsLost
) {
    public static HeadToHeadPlayerStatsDto from(HeadToHeadPlayerStats s) {
        return new HeadToHeadPlayerStatsDto(
                s.playerId(),
                s.firstServePercentage(), s.firstServeWonPercentage(), s.secondServeWonPercentage(),
                s.aces(), s.doubleFaults(),
                s.returnPointsWonFirstPercentage(), s.returnPointsWonSecondPercentage(),
                s.breakPointsWon(), s.breakPointsPlayed(), s.breakPointsWonPercentage(),
                s.returnGamesWonPercentage(),
                s.winners(), s.unforcedErrors(), s.winnersPercentage(), s.unforcedErrorPercentage(),
                s.matchesWon(), s.matchesLost(), s.setsWon(), s.setsLost());
    }
}
```

- [ ] **Step 2: Create `HeadToHeadStatisticsDto`**

```java
package com.cas.tsas.statistics.infrastructure.web.dto;

import com.cas.tsas.statistics.domain.model.HeadToHeadStatistics;

import java.util.UUID;

public record HeadToHeadStatisticsDto(
        UUID player1Id,
        UUID player2Id,
        int matchesPlayed,
        HeadToHeadPlayerStatsDto player1,
        HeadToHeadPlayerStatsDto player2
) {
    public static HeadToHeadStatisticsDto from(HeadToHeadStatistics s) {
        return new HeadToHeadStatisticsDto(
                s.player1Id(), s.player2Id(), s.matchesPlayed(),
                HeadToHeadPlayerStatsDto.from(s.player1()),
                HeadToHeadPlayerStatsDto.from(s.player2()));
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :statistics-module:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add backend/statistics-module/src/main/java/com/cas/tsas/statistics/infrastructure/web/dto
git commit -m "feat(statistics): add head-to-head web DTOs"
```

---

## Task 7: REST controller

**Files:**
- Create: `backend/statistics-module/src/main/java/com/cas/tsas/statistics/infrastructure/web/HeadToHeadController.java`

Validation (same-player → 400, missing player → 404) lives in the service; the controller is thin.

- [ ] **Step 1: Create the controller**

```java
package com.cas.tsas.statistics.infrastructure.web;

import com.cas.tsas.statistics.application.port.in.ComputeHeadToHeadStatisticsUseCase;
import com.cas.tsas.statistics.infrastructure.web.dto.HeadToHeadStatisticsDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** REST endpoint serving the head-to-head comparison of two players (FA-08). */
@RestController
@RequestMapping("/api/statistics/head-to-head")
public class HeadToHeadController {

    private final ComputeHeadToHeadStatisticsUseCase computeHeadToHead;

    public HeadToHeadController(ComputeHeadToHeadStatisticsUseCase computeHeadToHead) {
        this.computeHeadToHead = computeHeadToHead;
    }

    @GetMapping
    public HeadToHeadStatisticsDto getHeadToHead(@RequestParam("player1") UUID player1,
                                                 @RequestParam("player2") UUID player2) {
        return HeadToHeadStatisticsDto.from(computeHeadToHead.compute(player1, player2));
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :statistics-module:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add backend/statistics-module/src/main/java/com/cas/tsas/statistics/infrastructure/web/HeadToHeadController.java
git commit -m "feat(statistics): expose GET /api/statistics/head-to-head"
```

---

## Task 8: Integration test (TDD end-to-end)

**Files:**
- Test: `backend/app/src/test/java/com/cas/tsas/statistics/HeadToHeadApiIT.java`

Seeds players via the API and matches/scores/points directly via JPA repositories (so scores and serve metadata can be controlled precisely).

- [ ] **Step 1: Write the integration test**

`HeadToHeadApiIT.java`:

```java
package com.cas.tsas.statistics;

import com.cas.tsas.AbstractIntegrationTest;
import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.match.infrastructure.persistence.entity.MatchJpaEntity;
import com.cas.tsas.match.infrastructure.persistence.entity.MatchScoreJpaEntity;
import com.cas.tsas.match.infrastructure.persistence.entity.PointJpaEntity;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchScoreJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.PointJpaRepository;
import com.cas.tsas.player.infrastructure.persistence.repository.PlayerJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HeadToHeadApiIT extends AbstractIntegrationTest {

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

    private UUID seedMatch(UUID p1, UUID p2) {
        MatchJpaEntity m = new MatchJpaEntity();
        m.setPlayer1Id(p1);
        m.setPlayer2Id(p2);
        m.setSetsToWin(2);
        m.setMatchTiebreak(false);
        m.setShortSet(false);
        m.setStatus(MatchStatus.COMPLETED);
        return matchRepository.save(m).getId();
    }

    private void seedScore(UUID matchId, String winner, int sets1, int sets2) {
        MatchScoreJpaEntity s = new MatchScoreJpaEntity();
        s.setMatchId(matchId);
        s.setSetsPlayer1(sets1);
        s.setSetsPlayer2(sets2);
        s.setDone(true);
        s.setWinner(winner);
        matchScoreRepository.save(s);
    }

    private void seedPoint(UUID matchId, int set, int game, int pointNo, int winner,
                           PointType type, Integer server, Integer serveAttempt) {
        PointJpaEntity p = new PointJpaEntity();
        p.setMatchId(matchId);
        p.setSetNumber(set);
        p.setGameNumber(game);
        p.setPointNumber(pointNo);
        p.setWinner(winner);
        p.setPointType(type);
        p.setServingPlayer(server);
        p.setServeAttempt(serveAttempt);
        p.setBreakPoint(false);
        pointRepository.save(p);
    }

    @Test
    void aggregates_head_to_head_across_two_matches_with_swapped_positions() throws Exception {
        UUID alice = createPlayer();
        UUID bob = createPlayer();

        UUID m1 = seedMatch(alice, bob);          // alice = player1
        seedScore(m1, "PLAYER1", 2, 0);            // alice wins
        seedPoint(m1, 1, 1, 1, 1, PointType.WINNER, 1, 1); // alice serves & wins on 1st serve

        UUID m2 = seedMatch(bob, alice);           // positions swapped: alice = player2
        seedScore(m2, "PLAYER1", 2, 1);            // bob (player1) wins
        seedPoint(m2, 1, 1, 1, 2, PointType.WINNER, 2, 1); // alice (pos2) serves & wins on 1st serve

        mockMvc.perform(get("/api/statistics/head-to-head")
                        .param("player1", alice.toString())
                        .param("player2", bob.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player1Id").value(alice.toString()))
                .andExpect(jsonPath("$.player2Id").value(bob.toString()))
                .andExpect(jsonPath("$.matchesPlayed").value(2))
                .andExpect(jsonPath("$.player1.matchesWon").value(1))   // m1
                .andExpect(jsonPath("$.player1.matchesLost").value(1))  // m2
                .andExpect(jsonPath("$.player1.winners").value(2))      // both winners are alice's
                .andExpect(jsonPath("$.player1.setsWon").value(3))      // 2 (m1) + 1 (m2 as pos2)
                .andExpect(jsonPath("$.player2.matchesWon").value(1));
    }

    @Test
    void returns_zeroed_stats_when_players_never_met() throws Exception {
        UUID alice = createPlayer();
        UUID bob = createPlayer();

        mockMvc.perform(get("/api/statistics/head-to-head")
                        .param("player1", alice.toString())
                        .param("player2", bob.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchesPlayed").value(0))
                .andExpect(jsonPath("$.player1.winners").value(0));
    }

    @Test
    void returns_404_when_a_player_does_not_exist() throws Exception {
        UUID alice = createPlayer();

        mockMvc.perform(get("/api/statistics/head-to-head")
                        .param("player1", alice.toString())
                        .param("player2", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns_400_when_both_players_are_equal() throws Exception {
        UUID alice = createPlayer();

        mockMvc.perform(get("/api/statistics/head-to-head")
                        .param("player1", alice.toString())
                        .param("player2", alice.toString()))
                .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 2: Run the IT to verify it passes**

Run: `JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew :app:test --tests "*HeadToHeadApiIT*"`
Expected: PASS (4 tests). If it fails on the same-player case with 500 instead of 400, confirm `CommonExceptionHandler` (common-module) is component-scanned by the app — it is registered via `@RestControllerAdvice`.

- [ ] **Step 3: Commit**

```bash
git add backend/app/src/test/java/com/cas/tsas/statistics/HeadToHeadApiIT.java
git commit -m "test(statistics): integration test for head-to-head endpoint"
```

---

## Task 9: Frontend model

**Files:**
- Modify: `frontend/src/app/core/models/statistics.model.ts`

- [ ] **Step 1: Append the H2H interfaces**

Add to the end of `statistics.model.ts`:

```typescript
export interface HeadToHeadPlayerStats {
  playerId: string;
  firstServePercentage: number;
  firstServeWonPercentage: number;
  secondServeWonPercentage: number;
  aces: number;
  doubleFaults: number;
  returnPointsWonFirstPercentage: number;
  returnPointsWonSecondPercentage: number;
  breakPointsWon: number;
  breakPointsPlayed: number;
  breakPointsWonPercentage: number;
  returnGamesWonPercentage: number;
  winners: number;
  unforcedErrors: number;
  winnersPercentage: number;
  unforcedErrorPercentage: number;
  matchesWon: number;
  matchesLost: number;
  setsWon: number;
  setsLost: number;
}

export interface HeadToHeadStatistics {
  player1Id: string;
  player2Id: string;
  matchesPlayed: number;
  player1: HeadToHeadPlayerStats;
  player2: HeadToHeadPlayerStats;
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/app/core/models/statistics.model.ts
git commit -m "feat(frontend): add head-to-head statistics model"
```

---

## Task 10: API service method

**Files:**
- Modify: `frontend/src/app/core/services/api.service.ts`

- [ ] **Step 1: Add the import**

Change the statistics-model import line to:

```typescript
import { MatchStatistics, HeadToHeadStatistics } from '../models/statistics.model';
```

- [ ] **Step 2: Add the method**

Add after `getMatchStatistics(...)`:

```typescript
  getHeadToHead(player1Id: string, player2Id: string): Observable<HeadToHeadStatistics> {
    return this.http.get<HeadToHeadStatistics>(
      `${this.base}/statistics/head-to-head`,
      { params: { player1: player1Id, player2: player2Id } }
    );
  }
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/core/services/api.service.ts
git commit -m "feat(frontend): add getHeadToHead API client method"
```

---

## Task 11: Head-to-Head component (TDD with Cypress)

**Files:**
- Test: `frontend/src/app/features/statistics/head-to-head/head-to-head.component.cy.ts`
- Create: `frontend/src/app/features/statistics/head-to-head/head-to-head.component.ts`
- Create: `frontend/src/app/features/statistics/head-to-head/head-to-head.component.html`

- [ ] **Step 1: Write the failing Cypress component test**

`head-to-head.component.cy.ts`:

```typescript
import { HeadToHeadComponent } from './head-to-head.component';
import { ActivatedRoute } from '@angular/router';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { Player } from '../../../core/models/player.model';
import { HeadToHeadStatistics } from '../../../core/models/statistics.model';

const PLAYERS: Player[] = [
  { id: 'a', firstName: 'Roger', lastName: 'Federer', gender: 'MALE', handedness: 'RIGHT', backhandType: 'ONE_HANDED', active: true },
  { id: 'b', firstName: 'Rafael', lastName: 'Nadal', gender: 'MALE', handedness: 'LEFT', backhandType: 'TWO_HANDED', active: true },
];

const STATS: HeadToHeadStatistics = {
  player1Id: 'a', player2Id: 'b', matchesPlayed: 3,
  player1: {
    playerId: 'a', firstServePercentage: 0.62, firstServeWonPercentage: 0.7, secondServeWonPercentage: 0.5,
    aces: 12, doubleFaults: 4, returnPointsWonFirstPercentage: 0.3, returnPointsWonSecondPercentage: 0.55,
    breakPointsWon: 6, breakPointsPlayed: 10, breakPointsWonPercentage: 0.6, returnGamesWonPercentage: 0.25,
    winners: 40, unforcedErrors: 22, winnersPercentage: 0.33, unforcedErrorPercentage: 0.18,
    matchesWon: 2, matchesLost: 1, setsWon: 5, setsLost: 3,
  },
  player2: {
    playerId: 'b', firstServePercentage: 0.58, firstServeWonPercentage: 0.65, secondServeWonPercentage: 0.45,
    aces: 8, doubleFaults: 6, returnPointsWonFirstPercentage: 0.28, returnPointsWonSecondPercentage: 0.5,
    breakPointsWon: 4, breakPointsPlayed: 9, breakPointsWonPercentage: 0.44, returnGamesWonPercentage: 0.2,
    winners: 35, unforcedErrors: 28, winnersPercentage: 0.29, unforcedErrorPercentage: 0.23,
    matchesWon: 1, matchesLost: 2, setsWon: 3, setsLost: 5,
  },
};

function routeStub(player1: string | null = null, player2: string | null = null) {
  return {
    snapshot: { queryParamMap: { get: (k: string) => (k === 'player1' ? player1 : k === 'player2' ? player2 : null) } },
  };
}

function mount(player1: string | null = null, player2: string | null = null) {
  cy.intercept('GET', '**/api/players', PLAYERS).as('getPlayers');
  cy.intercept('GET', '**/api/statistics/head-to-head*', STATS).as('getH2H');
  cy.mount(HeadToHeadComponent, {
    providers: [
      provideRouter([]),
      provideHttpClient(),
      provideAnimationsAsync(),
      { provide: ActivatedRoute, useValue: routeStub(player1, player2) },
    ],
  });
  cy.wait('@getPlayers');
}

describe('HeadToHeadComponent', () => {
  it('renders two player selects', () => {
    mount();
    cy.get('[data-testid="select-p1"]').should('exist');
    cy.get('[data-testid="select-p2"]').should('exist');
  });

  it('fetches and renders stats when both players are preselected via query params', () => {
    mount('a', 'b');
    cy.wait('@getH2H');
    cy.get('[data-testid="matches-played"]').should('contain', '3');
    cy.get('[data-testid="val-p1-aces"]').should('contain', '12');
    cy.get('[data-testid="val-p2-aces"]').should('contain', '8');
  });

  it('shows match balance', () => {
    mount('a', 'b');
    cy.wait('@getH2H');
    cy.get('[data-testid="val-p1-matches"]').should('contain', '2');
    cy.get('[data-testid="val-p2-matches"]').should('contain', '1');
  });

  it('does not fetch until both players are chosen', () => {
    mount('a', null);
    cy.get('[data-testid="empty-hint"]').should('be.visible');
  });
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd frontend && npm run cypress:run -- --spec "src/app/features/statistics/head-to-head/head-to-head.component.cy.ts"`
Expected: FAIL — component module cannot be imported (does not exist yet).

- [ ] **Step 3: Create the component class**

`head-to-head.component.ts`:

```typescript
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { Player } from '../../../core/models/player.model';
import { HeadToHeadStatistics } from '../../../core/models/statistics.model';

@Component({
  selector: 'app-head-to-head',
  standalone: true,
  imports: [MatButtonModule, MatFormFieldModule, MatSelectModule, NgClass, FormsModule],
  templateUrl: './head-to-head.component.html',
  styles: [`
    :host { display: block; min-height: 100dvh; background: #0f172a; color: #eee; font-family: sans-serif; }
    .page { max-width: 520px; margin: 0 auto; padding: 16px; }
    .pickers { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 16px; }
    .empty-hint { text-align: center; color: #64748b; padding: 32px; }
    .section-title { font-size: 12px; color: #38bdf8; text-transform: uppercase; letter-spacing: .05em; margin: 16px 0 8px; }
    .divider { border-top: 1px solid #1e293b; margin: 10px 0; }
    .stat-grid { display: grid; grid-template-columns: 64px 1fr 64px; gap: 3px 6px; align-items: center; }
    .val { font-size: 13px; font-weight: 600; }
    .val-left { text-align: right; color: #0ea5e9; }
    .val-right { text-align: left; padding-left: 4px; color: #94a3b8; }
    .val-right.leading { color: #eee; font-weight: 700; }
    .stat-label { font-size: 10px; color: #64748b; text-align: center; }
    .bar { display: flex; height: 4px; border-radius: 2px; overflow: hidden; margin-top: 1px; }
    .bar-p1 { background: #0ea5e9; }
    .bar-p2 { background: #475569; }
  `],
})
export class HeadToHeadComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(ApiService);

  players = signal<Player[]>([]);
  player1Id = signal<string | null>(null);
  player2Id = signal<string | null>(null);
  stats = signal<HeadToHeadStatistics | null>(null);

  bothSelected = computed(() => !!this.player1Id() && !!this.player2Id() && this.player1Id() !== this.player2Id());

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
    this.fetchIfReady();
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
```

- [ ] **Step 4: Create the template**

`head-to-head.component.html`:

```html
<div class="page">
  <div class="pickers">
    <mat-form-field appearance="outline">
      <mat-label>Spieler 1</mat-label>
      <mat-select data-testid="select-p1" [ngModel]="player1Id()" (ngModelChange)="player1Id.set($event); onSelect()">
        @for (p of players(); track p.id) {
          <mat-option [value]="p.id">{{ p.firstName }} {{ p.lastName }}</mat-option>
        }
      </mat-select>
    </mat-form-field>

    <mat-form-field appearance="outline">
      <mat-label>Spieler 2</mat-label>
      <mat-select data-testid="select-p2" [ngModel]="player2Id()" (ngModelChange)="player2Id.set($event); onSelect()">
        @for (p of players(); track p.id) {
          <mat-option [value]="p.id">{{ p.firstName }} {{ p.lastName }}</mat-option>
        }
      </mat-select>
    </mat-form-field>
  </div>

  @if (stats(); as s) {
    <div class="player-row stat-grid">
      <span class="val val-left">{{ playerName(player1Id()) }}</span>
      <div class="stat-label" data-testid="matches-played">{{ s.matchesPlayed }} Matches</div>
      <span class="val val-right">{{ playerName(player2Id()) }}</span>
    </div>

    <div class="section-title">Match-Bilanz</div>
    <div class="stat-grid">
      <span class="val val-left" data-testid="val-p1-matches">{{ s.player1.matchesWon }}:{{ s.player1.matchesLost }}</span>
      <div><div class="stat-label">Siege : Niederlagen</div>
        <div class="bar"><div class="bar-p1" [style.flex]="s.player1.matchesWon || 1"></div><div class="bar-p2" [style.flex]="s.player2.matchesWon || 1"></div></div>
      </div>
      <span class="val val-right" data-testid="val-p2-matches">{{ s.player2.matchesWon }}:{{ s.player2.matchesLost }}</span>

      <span class="val val-left">{{ s.player1.setsWon }}:{{ s.player1.setsLost }}</span>
      <div class="stat-label">Satzbilanz</div>
      <span class="val val-right">{{ s.player2.setsWon }}:{{ s.player2.setsLost }}</span>
    </div>

    <div class="section-title">Aufschlag</div>
    <div class="stat-grid">
      <span class="val val-left">{{ pct(s.player1.firstServePercentage) }}</span>
      <div class="stat-label">1. Aufschlag %</div>
      <span class="val val-right">{{ pct(s.player2.firstServePercentage) }}</span>

      <span class="val val-left">{{ pct(s.player1.firstServeWonPercentage) }}</span>
      <div class="stat-label">1. Aufschlag gewonnen %</div>
      <span class="val val-right">{{ pct(s.player2.firstServeWonPercentage) }}</span>

      <span class="val val-left">{{ pct(s.player1.secondServeWonPercentage) }}</span>
      <div class="stat-label">2. Aufschlag gewonnen %</div>
      <span class="val val-right">{{ pct(s.player2.secondServeWonPercentage) }}</span>

      <span class="val val-left" data-testid="val-p1-aces">{{ s.player1.aces }}</span>
      <div class="stat-label">Asse</div>
      <span class="val val-right" data-testid="val-p2-aces">{{ s.player2.aces }}</span>

      <span class="val val-left">{{ s.player1.doubleFaults }}</span>
      <div class="stat-label">Doppelfehler</div>
      <span class="val val-right">{{ s.player2.doubleFaults }}</span>
    </div>

    <div class="section-title">Return</div>
    <div class="stat-grid">
      <span class="val val-left">{{ pct(s.player1.returnPointsWonFirstPercentage) }}</span>
      <div class="stat-label">Return-Punkte 1. Aufschlag %</div>
      <span class="val val-right">{{ pct(s.player2.returnPointsWonFirstPercentage) }}</span>

      <span class="val val-left">{{ pct(s.player1.returnPointsWonSecondPercentage) }}</span>
      <div class="stat-label">Return-Punkte 2. Aufschlag %</div>
      <span class="val val-right">{{ pct(s.player2.returnPointsWonSecondPercentage) }}</span>

      <span class="val val-left">{{ s.player1.breakPointsWon }}/{{ s.player1.breakPointsPlayed }}</span>
      <div class="stat-label">Break Points</div>
      <span class="val val-right">{{ s.player2.breakPointsWon }}/{{ s.player2.breakPointsPlayed }}</span>

      <span class="val val-left">{{ pct(s.player1.returnGamesWonPercentage) }}</span>
      <div class="stat-label">Return-Games gewonnen %</div>
      <span class="val val-right">{{ pct(s.player2.returnGamesWonPercentage) }}</span>
    </div>

    <div class="section-title">Rallye</div>
    <div class="stat-grid">
      <span class="val val-left">{{ s.player1.winners }} ({{ pct(s.player1.winnersPercentage) }})</span>
      <div class="stat-label">Winners</div>
      <span class="val val-right">{{ s.player2.winners }} ({{ pct(s.player2.winnersPercentage) }})</span>

      <span class="val val-left">{{ s.player1.unforcedErrors }} ({{ pct(s.player1.unforcedErrorPercentage) }})</span>
      <div class="stat-label">Unforced Errors</div>
      <span class="val val-right">{{ s.player2.unforcedErrors }} ({{ pct(s.player2.unforcedErrorPercentage) }})</span>
    </div>
  } @else {
    <div class="empty-hint" data-testid="empty-hint">Bitte zwei verschiedene Spieler auswählen.</div>
  }
</div>
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `cd frontend && npm run cypress:run -- --spec "src/app/features/statistics/head-to-head/head-to-head.component.cy.ts"`
Expected: PASS (4 tests).

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/features/statistics/head-to-head
git commit -m "feat(frontend): head-to-head comparison view"
```

---

## Task 12: Route registration

**Files:**
- Modify: `frontend/src/app/app.routes.ts`

- [ ] **Step 1: Add the route**

Insert this object into the `routes` array (before the `matches/:id/...` entries is fine):

```typescript
  {
    path: 'statistics/head-to-head',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/statistics/head-to-head/head-to-head.component').then(m => m.HeadToHeadComponent)
  },
```

- [ ] **Step 2: Verify the app builds**

Run: `cd frontend && npm run build`
Expected: build succeeds; a lazy chunk for `head-to-head-component` is emitted.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/app.routes.ts
git commit -m "feat(frontend): register /statistics/head-to-head route"
```

---

## Task 13: Players-page entry points (TDD with Cypress)

**Files:**
- Modify: `frontend/src/app/features/players/players.component.cy.ts`
- Modify: `frontend/src/app/features/players/players.component.ts`
- Modify: `frontend/src/app/features/players/players.component.html`

- [ ] **Step 1: Add the failing tests**

Append inside the top-level `describe('PlayersComponent', ...)` block in `players.component.cy.ts` (after the existing `describe('empty state', ...)`), and add `Router` + `provideRouter` imports at the top:

At the top, change imports to include:

```typescript
import { Router, provideRouter } from '@angular/router';
```

Update `mountPlayers` to accept extra providers:

```typescript
function mountPlayers(players: Player[] = PLAYERS, extraProviders: any[] = []) {
  cy.intercept('GET', '**/api/players', players).as('getPlayers');
  cy.mount(PlayersComponent, {
    providers: [provideRouter([]), provideHttpClient(), provideAnimationsAsync(), ...extraProviders],
  });
  cy.wait('@getPlayers');
}
```

Add this describe block:

```typescript
  describe('head-to-head entry points', () => {
    it('shows the Head-to-Head toolbar button', () => {
      mountPlayers();
      cy.get('[data-testid="h2h-btn"]').should('be.visible');
    });

    it('navigates to the head-to-head route from the toolbar button', () => {
      const navigateSpy = cy.stub().as('navigate');
      mountPlayers(PLAYERS, [{ provide: Router, useValue: { navigate: navigateSpy } }]);
      cy.get('[data-testid="h2h-btn"]').click();
      cy.get('@navigate').should('have.been.calledWith', ['/statistics/head-to-head']);
    });

    it('navigates with a preselected player1 from the row action', () => {
      const navigateSpy = cy.stub().as('navigate');
      mountPlayers(PLAYERS, [{ provide: Router, useValue: { navigate: navigateSpy } }]);
      cy.get('[data-testid="compare-btn"]').first().click();
      cy.get('@navigate').should('have.been.calledWith',
        ['/statistics/head-to-head'], { queryParams: { player1: '1' } });
    });
  });
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `cd frontend && npm run cypress:run -- --spec "src/app/features/players/players.component.cy.ts"`
Expected: FAIL — `[data-testid="h2h-btn"]` and `[data-testid="compare-btn"]` not found.

- [ ] **Step 3: Add the navigation methods**

In `players.component.ts`, add two methods to the class (after `goToMatch`):

```typescript
  goToHeadToHead() {
    this.router.navigate(['/statistics/head-to-head']);
  }

  comparePlayer(player: Player) {
    this.router.navigate(['/statistics/head-to-head'], { queryParams: { player1: player.id } });
  }
```

- [ ] **Step 4: Add the toolbar button**

In `players.component.html`, add this button inside `.page-header`, immediately before the "Neuer Spieler" button:

```html
    <button mat-stroked-button data-testid="h2h-btn" (click)="goToHeadToHead()">
      <mat-icon>compare_arrows</mat-icon>
      Head-to-Head
    </button>
```

- [ ] **Step 5: Add the row action**

In `players.component.html`, inside the `actions` column `<td>` (before the existing delete/deactivate `@if`), add:

```html
            <button mat-icon-button data-testid="compare-btn" title="Head-to-Head vergleichen"
                    (click)="comparePlayer(player); $event.stopPropagation()">
              <mat-icon>compare_arrows</mat-icon>
            </button>
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `cd frontend && npm run cypress:run -- --spec "src/app/features/players/players.component.cy.ts"`
Expected: PASS (all existing + 3 new tests).

- [ ] **Step 7: Commit**

```bash
git add frontend/src/app/features/players
git commit -m "feat(frontend): add head-to-head entry points to players page"
```

---

## Task 14: Update the SAD

**Files:**
- Modify: `doc/sad/TSaS_SAD_arc42_1.md`

- [ ] **Step 1: Remove the Net Points clause from FA-08 (line ~471)**

In the FA-08 row, delete the sentence fragment `Net Points Won% (Anteil gewonnener Netzangriffe an allen Netzangriffen).` from the **Rallye** group so it reads `... **Rallye:** Winners% (Anteil direkter Gewinnschläge an allen Punkten), Unforced Error%. **Match-Bilanz:** ...`. Do not leave a remark or placeholder.

- [ ] **Step 2: Note FA-08 as implemented**

In the same FA-08 row (or the implementation-status section, matching how other implemented FAs are marked in this document — check how FA-07/criteria are noted), append a short note that FA-08 is implemented via `GET /api/statistics/head-to-head` in `statistics-module`, that match/set balance is computed from completed matches only (decided `MatchScore.winner`), and that Return-Games-Won% is derived from the game-deciding point per (set, game).

- [ ] **Step 3: Record the new module dependency**

In the building-block / module view section (search for `statistics-module` around line ~204), add that `statistics-module` now also depends on `player-module` (for player-existence validation → HTTP 404).

- [ ] **Step 4: Commit**

```bash
git add doc/sad/TSaS_SAD_arc42_1.md
git commit -m "docs(sad): mark FA-08 implemented, drop Net Points Won% clause"
```

---

## Task 15: Full verification

- [ ] **Step 1: Backend build + coverage gate**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew check`
Expected: BUILD SUCCESSFUL — all tests pass and the JaCoCo gate (85% line / 70% branch) holds.

- [ ] **Step 2: Frontend build + all component tests**

Run: `cd frontend && npm run build && npm run cypress:run`
Expected: build succeeds and all component specs pass.

- [ ] **Step 3: Final review**

Confirm the spec's acceptance criteria are met: endpoint + aggregation implemented and tested; SAD updated; frontend wired (route + players-page entry points). No commits pending beyond the feature.
```
