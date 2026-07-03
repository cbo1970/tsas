# TEN-35 Match-History Entry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reach the statistics of a player's past (completed) matches from the player list via a per-player match-history list.

**Architecture:** A new read path in the match-module — `GET /api/players/{playerId}/matches` returns the player's completed matches (opponent, result, date) newest-first; the frontend renders a `MatchHistoryComponent` reachable from a history button in each player row; a row click opens `/matches/:id/statistics`, which is made self-sufficient for player names.

**Tech Stack:** Spring Boot 4 / Java 25 (match-module, Clean Architecture), Angular 21 standalone + Signals, Vitest + Cypress.

## Global Constraints

- Owner-scoping: the history is the **current user's** data only. The `playerId` must belong to the current user (`LoadPlayerPort.findByIdAndOwner`) → else 404 (IDOR pattern, like `OpponentPreparationService`); the query additionally filters `matches.owner_id`.
- Only `COMPLETED` matches, newest first (`matches.updated_at DESC`). Do NOT modify `MatchResponse` or the `Match` domain record.
- Do NOT touch the statistics backend or the ai-module. The statistics page change is frontend-only (name fallback) and keeps the score-page entry (with query params) behaving identically.
- Set-score badges are NOT reconstructed (not in scope); they appear only when `?sets` is present.
- Build: `JAVA_HOME=/opt/java/jdk-25.0.1` prefix; backend ITs need `DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true`; do NOT export `OPENAI_API_KEY`. Swiss German "ss" not "ß".
- Tests stay green: backend `./gradlew check` (coverage gate 85/70), frontend Vitest + Cypress.

## Commands
- Backend service test: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :match-module:test --tests "com.cas.tsas.match.application.service.MatchHistoryServiceTest"`
- Backend API IT: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew :app:test --tests "com.cas.tsas.match.MatchHistoryApiIT"`
- Frontend Cypress (one spec): `cd frontend && npx cypress run --component --spec "<path>"`
- Frontend Vitest: `cd frontend && npx ng test --watch=false`

---

## Task 1: Backend — per-player match-history read path

**Files:**
- Create: `backend/match-module/src/main/java/com/cas/tsas/match/domain/model/MatchHistoryEntry.java`
- Create: `backend/match-module/src/main/java/com/cas/tsas/match/application/port/out/MatchHistoryRow.java`
- Create: `backend/match-module/src/main/java/com/cas/tsas/match/application/port/out/LoadMatchHistoryPort.java`
- Create: `backend/match-module/src/main/java/com/cas/tsas/match/application/port/in/GetMatchHistoryUseCase.java`
- Create: `backend/match-module/src/main/java/com/cas/tsas/match/application/service/MatchHistoryService.java`
- Create: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/dto/MatchHistoryEntryDto.java`
- Create: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/PlayerMatchHistoryController.java`
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/repository/MatchJpaRepository.java`
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/repository/MatchPersistenceAdapter.java`
- Test: `backend/match-module/src/test/java/com/cas/tsas/match/application/service/MatchHistoryServiceTest.java`
- Test: `backend/app/src/test/java/com/cas/tsas/match/MatchHistoryApiIT.java`

**Interfaces:**
- Produces (consumed by Task 2 via JSON): `GET /api/players/{playerId}/matches` → `[{ matchId, opponentName, setsWon, setsLost, won, completedAt }]`.

- [ ] **Step 1: Domain entry + out-port row records**

`MatchHistoryEntry.java`:
```java
package com.cas.tsas.match.domain.model;

import java.time.Instant;
import java.util.UUID;

/** A completed match from one player's perspective, for the match-history list (TEN-35). */
public record MatchHistoryEntry(
        UUID matchId, UUID opponentId, String opponentName,
        int setsWon, int setsLost, boolean won, Instant completedAt) {}
```

`MatchHistoryRow.java` (top-level so the JPQL constructor expression can name it):
```java
package com.cas.tsas.match.application.port.out;

import java.time.Instant;
import java.util.UUID;

/** Raw history row from persistence (match + score), un-enriched. */
public record MatchHistoryRow(
        UUID matchId, UUID player1Id, UUID player2Id,
        int setsPlayer1, int setsPlayer2, String winner, Instant completedAt) {}
```

- [ ] **Step 2: Out-port + in-port interfaces**

`LoadMatchHistoryPort.java`:
```java
package com.cas.tsas.match.application.port.out;

import java.util.List;
import java.util.UUID;

public interface LoadMatchHistoryPort {
    /** Completed matches involving the player, owned by ownerId, newest first. */
    List<MatchHistoryRow> findCompletedByPlayer(UUID playerId, UUID ownerId);
}
```

`GetMatchHistoryUseCase.java`:
```java
package com.cas.tsas.match.application.port.in;

import com.cas.tsas.match.domain.model.MatchHistoryEntry;

import java.util.List;
import java.util.UUID;

public interface GetMatchHistoryUseCase {
    /** Completed matches of the player (newest first), owner-scoped (→404 for foreign/unknown player). */
    List<MatchHistoryEntry> forPlayer(UUID playerId);
}
```

- [ ] **Step 3: Repository query**

In `MatchJpaRepository.java` add the import + the constructor-expression query:
```java
import com.cas.tsas.match.application.port.out.MatchHistoryRow;
// ...
    @Query("""
            SELECT new com.cas.tsas.match.application.port.out.MatchHistoryRow(
                m.id, m.player1Id, m.player2Id, ms.setsPlayer1, ms.setsPlayer2, ms.winner, m.updatedAt)
            FROM MatchJpaEntity m, MatchScoreJpaEntity ms
            WHERE ms.matchId = m.id
              AND (m.player1Id = :playerId OR m.player2Id = :playerId)
              AND m.status = com.cas.tsas.match.domain.model.MatchStatus.COMPLETED
              AND m.ownerId = :ownerId
            ORDER BY m.updatedAt DESC
            """)
    List<MatchHistoryRow> findCompletedHistoryByPlayer(@Param("playerId") UUID playerId,
                                                       @Param("ownerId") UUID ownerId);
```
(`MatchJpaEntity.updatedAt` exists from V7 audit columns; `MatchScoreJpaEntity` has `setsPlayer1/2`, `winner`; the theta-join is valid since a match always has exactly one score row.)

- [ ] **Step 4: Adapter implements the out-port**

In `MatchPersistenceAdapter.java` (which already implements the match ports), add `LoadMatchHistoryPort` to the `implements` list and the method (passthrough to the repository):
```java
    @Override
    public List<MatchHistoryRow> findCompletedByPlayer(UUID playerId, UUID ownerId) {
        return matchJpaRepository.findCompletedHistoryByPlayer(playerId, ownerId);
    }
```
Add imports for `LoadMatchHistoryPort` and `MatchHistoryRow`. (Read the adapter first to confirm the repository field name — it is `matchJpaRepository` if present; otherwise use the actual injected `MatchJpaRepository` field name.)

- [ ] **Step 5: Service**

`MatchHistoryService.java`:
```java
package com.cas.tsas.match.application.service;

import com.cas.tsas.auth.application.port.in.CurrentUserProvider;
import com.cas.tsas.match.application.port.in.GetMatchHistoryUseCase;
import com.cas.tsas.match.application.port.out.LoadMatchHistoryPort;
import com.cas.tsas.match.application.port.out.MatchHistoryRow;
import com.cas.tsas.match.domain.model.MatchHistoryEntry;
import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.player.domain.exception.PlayerNotFoundException;
import com.cas.tsas.player.domain.model.Player;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Builds a player's completed-match history (TEN-35), owner-scoped. */
@Service
@Transactional(readOnly = true)
public class MatchHistoryService implements GetMatchHistoryUseCase {

    private final LoadMatchHistoryPort loadMatchHistoryPort;
    private final LoadPlayerPort loadPlayerPort;
    private final CurrentUserProvider currentUserProvider;

    public MatchHistoryService(LoadMatchHistoryPort loadMatchHistoryPort,
                               LoadPlayerPort loadPlayerPort,
                               CurrentUserProvider currentUserProvider) {
        this.loadMatchHistoryPort = loadMatchHistoryPort;
        this.loadPlayerPort = loadPlayerPort;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public List<MatchHistoryEntry> forPlayer(UUID playerId) {
        UUID ownerId = currentUserProvider.get().id();
        // IDOR guard: foreign/unknown player → 404 (same as opponent preparation).
        loadPlayerPort.findByIdAndOwner(playerId, ownerId)
                .orElseThrow(() -> new PlayerNotFoundException(playerId));

        return loadMatchHistoryPort.findCompletedByPlayer(playerId, ownerId).stream()
                .map(row -> toEntry(playerId, row))
                .toList();
    }

    private MatchHistoryEntry toEntry(UUID playerId, MatchHistoryRow row) {
        boolean isP1 = playerId.equals(row.player1Id());
        UUID opponentId = isP1 ? row.player2Id() : row.player1Id();
        int setsWon = isP1 ? row.setsPlayer1() : row.setsPlayer2();
        int setsLost = isP1 ? row.setsPlayer2() : row.setsPlayer1();
        boolean won = (isP1 && "PLAYER1".equals(row.winner()))
                || (!isP1 && "PLAYER2".equals(row.winner()));
        String opponentName = loadPlayerPort.loadPlayer(opponentId)
                .map(p -> p.getFirstName() + " " + p.getLastName())
                .orElse("—");
        return new MatchHistoryEntry(row.matchId(), opponentId, opponentName,
                setsWon, setsLost, won, row.completedAt());
    }
}
```
> Confirm the exact symbols before writing: `CurrentUserProvider.get().id()` (used by `MatchService`), `LoadPlayerPort.findByIdAndOwner(UUID,UUID)` + `loadPlayer(UUID)` returning `Optional<Player>`, `Player.getFirstName()/getLastName()`, `PlayerNotFoundException(UUID)`. If any differs, adapt to the real signature (read `MatchService.java` / `OpponentPreparationService.java` for the established usage).

- [ ] **Step 6: DTO + controller**

`MatchHistoryEntryDto.java`:
```java
package com.cas.tsas.match.infrastructure.web.dto;

import com.cas.tsas.match.domain.model.MatchHistoryEntry;

import java.time.Instant;
import java.util.UUID;

public record MatchHistoryEntryDto(
        UUID matchId, String opponentName, int setsWon, int setsLost, boolean won, Instant completedAt) {
    public static MatchHistoryEntryDto from(MatchHistoryEntry e) {
        return new MatchHistoryEntryDto(e.matchId(), e.opponentName(),
                e.setsWon(), e.setsLost(), e.won(), e.completedAt());
    }
}
```

`PlayerMatchHistoryController.java`:
```java
package com.cas.tsas.match.infrastructure.web;

import com.cas.tsas.match.application.port.in.GetMatchHistoryUseCase;
import com.cas.tsas.match.infrastructure.web.dto.MatchHistoryEntryDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Completed-match history for a player (TEN-35). */
@RestController
public class PlayerMatchHistoryController {

    private final GetMatchHistoryUseCase getMatchHistoryUseCase;

    public PlayerMatchHistoryController(GetMatchHistoryUseCase getMatchHistoryUseCase) {
        this.getMatchHistoryUseCase = getMatchHistoryUseCase;
    }

    @GetMapping("/api/players/{playerId}/matches")
    public List<MatchHistoryEntryDto> getHistory(@PathVariable UUID playerId) {
        return getMatchHistoryUseCase.forPlayer(playerId).stream()
                .map(MatchHistoryEntryDto::from)
                .toList();
    }
}
```

- [ ] **Step 7: Service unit test**

`MatchHistoryServiceTest.java` (Mockito; mirror the existing match-module service tests for fixture style):
```java
package com.cas.tsas.match.application.service;

import com.cas.tsas.auth.application.port.in.CurrentUserProvider;
import com.cas.tsas.auth.domain.CurrentUser;
import com.cas.tsas.match.application.port.out.LoadMatchHistoryPort;
import com.cas.tsas.match.application.port.out.MatchHistoryRow;
import com.cas.tsas.match.domain.model.MatchHistoryEntry;
import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.player.domain.exception.PlayerNotFoundException;
import com.cas.tsas.player.domain.model.BackhandType;
import com.cas.tsas.player.domain.model.Gender;
import com.cas.tsas.player.domain.model.Handedness;
import com.cas.tsas.player.domain.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class MatchHistoryServiceTest {

    private LoadMatchHistoryPort loadHistory;
    private LoadPlayerPort loadPlayer;
    private MatchHistoryService service;
    private UUID owner, player, opponent;

    @BeforeEach
    void setUp() {
        loadHistory = Mockito.mock(LoadMatchHistoryPort.class);
        loadPlayer = Mockito.mock(LoadPlayerPort.class);
        CurrentUserProvider cup = Mockito.mock(CurrentUserProvider.class);
        owner = UUID.randomUUID();
        player = UUID.randomUUID();
        opponent = UUID.randomUUID();
        when(cup.get()).thenReturn(new CurrentUser(owner, "coach", List.of()));
        service = new MatchHistoryService(loadHistory, loadPlayer, cup);
        when(loadPlayer.findByIdAndOwner(player, owner)).thenReturn(Optional.of(player(player, "Self", "Player")));
        when(loadPlayer.loadPlayer(opponent)).thenReturn(Optional.of(player(opponent, "Tom", "Gegner")));
    }

    private Player player(UUID id, String first, String last) {
        return new Player(id, owner, first, last, Gender.MALE, Handedness.RIGHT, BackhandType.TWO_HANDED, "N3", "GER", null);
    }

    @Test
    void mapsFromPlayer1Perspective_won() {
        when(loadHistory.findCompletedByPlayer(player, owner)).thenReturn(List.of(
                new MatchHistoryRow(UUID.randomUUID(), player, opponent, 2, 1, "PLAYER1", Instant.now())));
        MatchHistoryEntry e = service.forPlayer(player).get(0);
        assertThat(e.opponentId()).isEqualTo(opponent);
        assertThat(e.opponentName()).isEqualTo("Tom Gegner");
        assertThat(e.setsWon()).isEqualTo(2);
        assertThat(e.setsLost()).isEqualTo(1);
        assertThat(e.won()).isTrue();
    }

    @Test
    void mapsFromPlayer2Perspective_lost() {
        when(loadHistory.findCompletedByPlayer(player, owner)).thenReturn(List.of(
                new MatchHistoryRow(UUID.randomUUID(), opponent, player, 2, 0, "PLAYER1", Instant.now())));
        MatchHistoryEntry e = service.forPlayer(player).get(0);
        assertThat(e.opponentId()).isEqualTo(opponent);
        assertThat(e.setsWon()).isZero();
        assertThat(e.setsLost()).isEqualTo(2);
        assertThat(e.won()).isFalse();
    }

    @Test
    void foreignPlayerThrows404() {
        UUID stranger = UUID.randomUUID();
        when(loadPlayer.findByIdAndOwner(stranger, owner)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.forPlayer(stranger)).isInstanceOf(PlayerNotFoundException.class);
    }
}
```
> Verify the `CurrentUser` constructor signature and `PlayerNotFoundException(UUID)` against the real classes before running (read `CurrentUser.java`; the existing `MatchAnalysisServiceTest`/`OpponentPreparationService` show valid usage). Adjust the fixture constructor to the real `Player`/`CurrentUser` shapes.

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :match-module:test --tests "com.cas.tsas.match.application.service.MatchHistoryServiceTest"` → PASS.

- [ ] **Step 8: API IT**

`backend/app/src/test/java/com/cas/tsas/match/MatchHistoryApiIT.java` (mirror `MatchApiIT` helpers: create players/matches via REST; complete a match via `POST /api/matches/{id}/end`):
```java
package com.cas.tsas.match;

import com.cas.tsas.AbstractIntegrationTest;
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

class MatchHistoryApiIT extends AbstractIntegrationTest {

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

    private UUID createPlayer(String last) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "firstName", "T", "lastName", last,
                "gender", "MALE", "handedness", "RIGHT", "backhandType", "TWO_HANDED"));
        String r = mockMvc.perform(post("/api/players").contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(r).get("id").asText());
    }

    private UUID createMatch(UUID p1, UUID p2) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "player1Id", p1, "player2Id", p2, "setsToWin", 2, "matchTiebreak", false, "shortSet", false));
        String r = mockMvc.perform(post("/api/matches").contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(r).get("id").asText());
    }

    @Test
    void returns_completed_matches_newest_first_with_opponent_and_result() throws Exception {
        UUID self = createPlayer("Self");
        UUID opp1 = createPlayer("Opp1");
        UUID opp2 = createPlayer("Opp2");
        UUID m1 = createMatch(self, opp1);
        mockMvc.perform(post("/api/matches/{id}/end", m1)).andExpect(status().isOk());
        UUID m2 = createMatch(self, opp2);
        mockMvc.perform(post("/api/matches/{id}/end", m2)).andExpect(status().isOk());
        // an in-progress match must NOT appear
        createMatch(self, createPlayer("Opp3"));

        mockMvc.perform(get("/api/players/{id}/matches", self))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].matchId").value(m2.toString())) // newest first
                .andExpect(jsonPath("$[0].opponentName").value("T Opp2"))
                .andExpect(jsonPath("$[1].matchId").value(m1.toString()));
    }

    @Test
    void player_without_matches_returns_empty() throws Exception {
        UUID solo = createPlayer("Solo");
        mockMvc.perform(get("/api/players/{id}/matches", solo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unknown_player_returns_404() throws Exception {
        mockMvc.perform(get("/api/players/{id}/matches", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
```
> If `POST /api/matches/{id}/end` does not deterministically order by `updated_at` such that m2 > m1, the newest-first assertion still holds because m2 is created+ended after m1 (later `updated_at`). If the IT proves flaky on ordering, assert the **set** of returned matchIds instead and keep the count/opponent assertions.

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew :app:test --tests "com.cas.tsas.match.MatchHistoryApiIT"` → PASS.

- [ ] **Step 9: Commit**

```bash
git add backend/match-module backend/app/src/test/java/com/cas/tsas/match/MatchHistoryApiIT.java
git commit -m "feat(match): per-player completed-match history endpoint (TEN-35)"
```

---

## Task 2: Frontend — match-history view, entry button, statistics name fallback

**Files:**
- Modify: `frontend/src/app/core/models/match.model.ts` (add `MatchHistoryEntry`)
- Modify: `frontend/src/app/core/services/api.service.ts` (add `getPlayerMatchHistory`)
- Modify: `frontend/src/app/app.routes.ts` (new route)
- Create: `frontend/src/app/features/players/match-history/match-history.component.ts` + `.html`
- Create: `frontend/src/app/features/players/match-history/match-history.component.cy.ts`
- Modify: `frontend/src/app/features/players/players.component.ts` + `.html` (history button)
- Modify: `frontend/src/app/features/players/players.component.cy.ts` (button test)
- Modify: `frontend/src/app/features/matches/statistics/statistics.component.ts` (name fallback)
- Modify: `frontend/src/app/features/matches/statistics/statistics.component.cy.ts` (fallback test)

**Interfaces:**
- Consumes (Task 1): `GET /api/players/{id}/matches` → `MatchHistoryEntry[]`.

- [ ] **Step 1: Model + ApiService**

In `match.model.ts` add:
```ts
export interface MatchHistoryEntry {
  matchId: string;
  opponentName: string;
  setsWon: number;
  setsLost: number;
  won: boolean;
  completedAt: string;
}
```
In `api.service.ts`, import `MatchHistoryEntry` from the match model and add:
```ts
  getPlayerMatchHistory(playerId: string): Observable<MatchHistoryEntry[]> {
    return this.http.get<MatchHistoryEntry[]>(`${this.base}/players/${playerId}/matches`);
  }
```

- [ ] **Step 2: Route**

In `app.routes.ts`, add after the `players` route (keep `authGuard`):
```ts
  {
    path: 'players/:id/matches',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/players/match-history/match-history.component').then(m => m.MatchHistoryComponent)
  },
```

- [ ] **Step 3: MatchHistoryComponent**

`match-history.component.ts`:
```ts
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
```

`match-history.component.html`:
```html
<div class="page">
  <div class="title">Match-History @if (playerName()) { · {{ playerName() }} }</div>

  @if (loaded() && entries().length === 0) {
    <div class="empty" data-testid="history-empty">Noch keine abgeschlossenen Matches</div>
  }

  @for (e of entries(); track e.matchId) {
    <div class="entry" data-testid="history-entry" (click)="openStatistics(e.matchId)">
      <span class="opp">vs. {{ e.opponentName }}</span>
      <span class="result" [class.won]="e.won" [class.lost]="!e.won">
        {{ e.won ? 'S' : 'N' }} {{ e.setsWon }}:{{ e.setsLost }}
      </span>
      <span class="date">{{ e.completedAt | date:'dd.MM.yyyy HH:mm' }}</span>
    </div>
  }

  <div class="back-row">
    <button mat-button data-testid="back-btn" (click)="goBack()">Zur Spielerübersicht</button>
  </div>
</div>
```

- [ ] **Step 4: History button in the player row**

In `players.component.html`, in the `actions` column, add a history button before the compare button:
```html
            <button mat-icon-button data-testid="player-history-btn" title="Match-History"
                    (click)="goToHistory(player); $event.stopPropagation()">
              <mat-icon>history</mat-icon>
            </button>
```
In `players.component.ts`, add the handler (next to `comparePlayer`):
```ts
  goToHistory(player: Player) {
    this.router.navigate(['/players', player.id, 'matches']);
  }
```

- [ ] **Step 5: Statistics page name fallback**

In `statistics.component.ts` `ngOnInit`, after reading the query params, fetch names from the match when the `p1`/`p2` params are absent. Replace the current `p1Name`/`p2Name` assignment block:
```ts
    const p1Param = this.route.snapshot.queryParamMap.get('p1');
    const p2Param = this.route.snapshot.queryParamMap.get('p2');
    if (p1Param && p2Param) {
      this.p1Name.set(p1Param);
      this.p2Name.set(p2Param);
    } else {
      this.api.getMatch(this.matchId).subscribe(m => {
        this.api.getPlayer(m.player1Id).subscribe(p => this.p1Name.set(`${p.firstName} ${p.lastName}`));
        this.api.getPlayer(m.player2Id).subscribe(p => this.p2Name.set(`${p.firstName} ${p.lastName}`));
      });
    }
```
(Leave the `?sets` → `setScores` handling unchanged; badges stay empty when `?sets` is absent.)

- [ ] **Step 6: Cypress tests**

`match-history.component.cy.ts`:
```ts
import { MatchHistoryComponent } from './match-history.component';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

const ENTRIES = [
  { matchId: 'm2', opponentName: 'Tom Gegner', setsWon: 2, setsLost: 1, won: true, completedAt: '2026-06-26T10:00:00Z' },
  { matchId: 'm1', opponentName: 'Ann Other', setsWon: 0, setsLost: 2, won: false, completedAt: '2026-06-20T09:00:00Z' },
];
const routeStub = { snapshot: { paramMap: { get: (k: string) => (k === 'id' ? 'p1' : null) } } };

function mount(extra: any[] = []) {
  cy.intercept('GET', '**/api/players/p1', { id: 'p1', firstName: 'Self', lastName: 'Player' }).as('getPlayer');
  cy.intercept('GET', '**/api/players/p1/matches', ENTRIES).as('getHistory');
  cy.mount(MatchHistoryComponent, {
    providers: [provideRouter([]), provideHttpClient(), provideAnimationsAsync(),
      { provide: ActivatedRoute, useValue: routeStub }, ...extra],
  });
  cy.wait('@getHistory');
}

describe('MatchHistoryComponent', () => {
  it('renders one row per completed match with opponent + result', () => {
    mount();
    cy.get('[data-testid="history-entry"]').should('have.length', 2);
    cy.get('[data-testid="history-entry"]').first().should('contain', 'Tom Gegner').and('contain', 'S 2:1');
  });

  it('navigates to the statistics of the clicked match', () => {
    const nav = cy.stub().as('nav');
    mount([{ provide: Router, useValue: { navigate: nav } }]);
    cy.get('[data-testid="history-entry"]').first().click();
    cy.get('@nav').should('have.been.calledWith', ['/matches', 'm2', 'statistics']);
  });

  it('shows the empty state when there are no matches', () => {
    cy.intercept('GET', '**/api/players/p1', { id: 'p1', firstName: 'Self', lastName: 'Player' });
    cy.intercept('GET', '**/api/players/p1/matches', []).as('empty');
    cy.mount(MatchHistoryComponent, {
      providers: [provideRouter([]), provideHttpClient(), provideAnimationsAsync(),
        { provide: ActivatedRoute, useValue: routeStub }],
    });
    cy.wait('@empty');
    cy.get('[data-testid="history-empty"]').should('exist');
  });
});
```

In `players.component.cy.ts`, add a test that the history button navigates (mirror the existing compare-button test pattern — read the spec first for the exact mount helper / player fixture). Assert: `cy.get('[data-testid="player-history-btn"]').first().click()` triggers `router.navigate(['/players', <id>, 'matches'])`.

In `statistics.component.cy.ts`, add a test for the name fallback: a route stub WITHOUT `p1`/`p2` query params + intercepts for `GET **/api/matches/match-1` (returns `player1Id`/`player2Id`) and `GET **/api/players/*` (returns names); assert the names render. Keep the existing query-param tests unchanged (they must stay green).

Run:
```
cd frontend
npx cypress run --component --spec "src/app/features/players/match-history/match-history.component.cy.ts,src/app/features/players/players.component.cy.ts,src/app/features/matches/statistics/statistics.component.cy.ts"
npx ng test --watch=false
```
Expected: all green.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/app/core/models/match.model.ts frontend/src/app/core/services/api.service.ts \
        frontend/src/app/app.routes.ts frontend/src/app/features/players/ \
        frontend/src/app/features/matches/statistics/
git commit -m "feat(frontend): per-player match-history entry to statistics (TEN-35)"
```

---

## Final verification

- [ ] Backend: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew check` → green incl. coverage gate.
- [ ] Frontend: `cd frontend && npx ng test --watch=false && npm run cypress:run` → green.
- [ ] Manual sight-check: player list → history button → list (opponent/result/date) → click → statistics page shows player names (fetched) + the per-set tabs; the score-page entry still shows the set-score badges.

## Self-Review (author checklist — completed)

**Spec coverage:** Backend read path (read-model, port, query, adapter, service owner-check, DTO, controller) → Task 1. Frontend history view + entry button + route + stats name fallback → Task 2. Tests: service unit + API IT (Task 1), Cypress for history/players/statistics (Task 2). `MatchResponse`/Match domain/statistics backend/ai-module untouched → Global Constraints. Set-score badges out of scope → noted. No gaps.

**Placeholder scan:** No TBD/TODO. The "verify the real signature" notes (CurrentUser/Player/PlayerNotFoundException constructors, adapter repo field name) point at concrete existing code to confirm against — the code is complete; only signatures must be matched to reality.

**Type/name consistency:** `MatchHistoryRow(matchId, player1Id, player2Id, setsPlayer1, setsPlayer2, winner, completedAt)` ↔ JPQL constructor expression ↔ adapter return ↔ service `toEntry`. `MatchHistoryEntry(matchId, opponentId, opponentName, setsWon, setsLost, won, completedAt)` ↔ `MatchHistoryEntryDto(matchId, opponentName, setsWon, setsLost, won, completedAt)` ↔ frontend `MatchHistoryEntry` interface (no opponentId on the wire — intentional). `getPlayerMatchHistory` / route `players/:id/matches` / `data-testid` (`player-history-btn`, `history-entry`, `history-empty`) consistent across component, template, and tests.
