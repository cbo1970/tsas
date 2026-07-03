# Serving Player Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Track who is serving in a tennis match, automatically rotate the serve after each game, and disable the ace button for the returning player.

**Architecture:** `servingPlayer: Integer` (null/1/2) is added to `MatchScore` and flows through all layers. `ScoringService.awardGame()` auto-rotates the serve after each game. A new `SetServingPlayerUseCase` allows the score page to set the initial server. `MatchService.recordAce()` rejects ace attempts from the returning player with 409.

**Tech Stack:** Spring Boot 3.4 (Java 21), JPA/H2/PostgreSQL, Angular 21, Angular Material

---

## Betroffene Dateien

**Backend — modifiziert:**
- `backend/match-module/src/main/java/com/cas/tsas/match/domain/model/MatchScore.java`
- `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/entity/MatchScoreJpaEntity.java`
- `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/mapper/MatchScoreMapper.java`
- `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/dto/response/MatchScoreResponse.java`
- `backend/match-module/src/main/java/com/cas/tsas/match/application/service/MatchService.java`
- `backend/match-module/src/main/java/com/cas/tsas/match/application/service/ScoringService.java`
- `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/MatchController.java`

**Backend — neu:**
- `backend/match-module/src/main/java/com/cas/tsas/match/application/port/in/SetServingPlayerUseCase.java`

**Backend — Tests:**
- `backend/match-module/src/test/java/com/cas/tsas/match/application/service/ScoringServiceTest.java`
- `backend/match-module/src/test/java/com/cas/tsas/match/application/service/MatchServiceTest.java`
- `backend/app/src/test/java/com/cas/tsas/match/MatchApiIT.java`
- `backend/app/src/test/java/com/cas/tsas/match/MatchPersistenceAdapterIT.java`

**Frontend — modifiziert:**
- `frontend/src/app/core/models/match.model.ts`
- `frontend/src/app/core/services/api.service.ts`
- `frontend/src/app/features/matches/score/score.component.ts`
- `frontend/src/app/features/matches/score/score.component.cy.ts`

---

## Task 1: MatchScore Datenmodell + ScoringService Aufschlagwechsel

**Files:**
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/domain/model/MatchScore.java`
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/entity/MatchScoreJpaEntity.java`
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/mapper/MatchScoreMapper.java`
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/dto/response/MatchScoreResponse.java`
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/application/service/MatchService.java`
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/application/service/ScoringService.java`
- Modify: `backend/match-module/src/test/java/com/cas/tsas/match/application/service/ScoringServiceTest.java`
- Modify: `backend/match-module/src/test/java/com/cas/tsas/match/application/service/MatchServiceTest.java`
- Modify: `backend/app/src/test/java/com/cas/tsas/match/MatchPersistenceAdapterIT.java`

- [ ] **Schritt 1: `MatchScore.java` komplett ersetzen**

```java
package com.cas.tsas.match.domain.model;

import java.util.UUID;

/**
 * Domain entity representing the running score of a match.
 * Pure POJO — no framework dependencies.
 */
public class MatchScore {

    private UUID id;
    private UUID matchId;
    private int pointsPlayer1;
    private int pointsPlayer2;
    private int gamesPlayer1;
    private int gamesPlayer2;
    private int setsPlayer1;
    private int setsPlayer2;
    private boolean isDeuce;
    private Boolean isAdvantagePlayer1;
    private int currentSet;
    private boolean isDone;
    private String winner;
    private int acesPlayer1;
    private int acesPlayer2;
    private Integer servingPlayer;

    public MatchScore() {}

    public MatchScore(UUID id, UUID matchId,
                      int pointsPlayer1, int pointsPlayer2,
                      int gamesPlayer1, int gamesPlayer2,
                      int setsPlayer1, int setsPlayer2,
                      boolean isDeuce, Boolean isAdvantagePlayer1,
                      int currentSet, boolean isDone, String winner,
                      int acesPlayer1, int acesPlayer2,
                      Integer servingPlayer) {
        this.id = id;
        this.matchId = matchId;
        this.pointsPlayer1 = pointsPlayer1;
        this.pointsPlayer2 = pointsPlayer2;
        this.gamesPlayer1 = gamesPlayer1;
        this.gamesPlayer2 = gamesPlayer2;
        this.setsPlayer1 = setsPlayer1;
        this.setsPlayer2 = setsPlayer2;
        this.isDeuce = isDeuce;
        this.isAdvantagePlayer1 = isAdvantagePlayer1;
        this.currentSet = currentSet;
        this.isDone = isDone;
        this.winner = winner;
        this.acesPlayer1 = acesPlayer1;
        this.acesPlayer2 = acesPlayer2;
        this.servingPlayer = servingPlayer;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getMatchId() { return matchId; }
    public void setMatchId(UUID matchId) { this.matchId = matchId; }

    public int getPointsPlayer1() { return pointsPlayer1; }
    public void setPointsPlayer1(int pointsPlayer1) { this.pointsPlayer1 = pointsPlayer1; }

    public int getPointsPlayer2() { return pointsPlayer2; }
    public void setPointsPlayer2(int pointsPlayer2) { this.pointsPlayer2 = pointsPlayer2; }

    public int getGamesPlayer1() { return gamesPlayer1; }
    public void setGamesPlayer1(int gamesPlayer1) { this.gamesPlayer1 = gamesPlayer1; }

    public int getGamesPlayer2() { return gamesPlayer2; }
    public void setGamesPlayer2(int gamesPlayer2) { this.gamesPlayer2 = gamesPlayer2; }

    public int getSetsPlayer1() { return setsPlayer1; }
    public void setSetsPlayer1(int setsPlayer1) { this.setsPlayer1 = setsPlayer1; }

    public int getSetsPlayer2() { return setsPlayer2; }
    public void setSetsPlayer2(int setsPlayer2) { this.setsPlayer2 = setsPlayer2; }

    public boolean isDeuce() { return isDeuce; }
    public void setDeuce(boolean deuce) { isDeuce = deuce; }

    public Boolean getIsAdvantagePlayer1() { return isAdvantagePlayer1; }
    public void setIsAdvantagePlayer1(Boolean isAdvantagePlayer1) { this.isAdvantagePlayer1 = isAdvantagePlayer1; }

    public int getCurrentSet() { return currentSet; }
    public void setCurrentSet(int currentSet) { this.currentSet = currentSet; }

    public boolean isDone() { return isDone; }
    public void setDone(boolean done) { isDone = done; }

    public String getWinner() { return winner; }
    public void setWinner(String winner) { this.winner = winner; }

    public int getAcesPlayer1() { return acesPlayer1; }
    public void setAcesPlayer1(int acesPlayer1) { this.acesPlayer1 = acesPlayer1; }

    public int getAcesPlayer2() { return acesPlayer2; }
    public void setAcesPlayer2(int acesPlayer2) { this.acesPlayer2 = acesPlayer2; }

    public Integer getServingPlayer() { return servingPlayer; }
    public void setServingPlayer(Integer servingPlayer) { this.servingPlayer = servingPlayer; }
}
```

- [ ] **Schritt 2: `MatchScoreJpaEntity.java` — Feld nach `acesPlayer2` einfügen**

Nach der `acesPlayer2`-Deklaration (Zeile 55) und vor dem leeren Konstruktor einfügen:

```java
@Column(name = "serving_player")
private Integer servingPlayer;
```

Am Ende der Klasse (nach `setAcesPlayer2`) Getter/Setter hinzufügen:

```java
public Integer getServingPlayer() { return servingPlayer; }
public void setServingPlayer(Integer servingPlayer) { this.servingPlayer = servingPlayer; }
```

- [ ] **Schritt 3: `MatchScoreMapper.java` komplett ersetzen**

```java
package com.cas.tsas.match.infrastructure.persistence.mapper;

import com.cas.tsas.match.domain.model.MatchScore;
import com.cas.tsas.match.infrastructure.persistence.entity.MatchScoreJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class MatchScoreMapper {

    public MatchScore toDomain(MatchScoreJpaEntity entity) {
        return new MatchScore(
                entity.getId(),
                entity.getMatchId(),
                entity.getPointsPlayer1(),
                entity.getPointsPlayer2(),
                entity.getGamesPlayer1(),
                entity.getGamesPlayer2(),
                entity.getSetsPlayer1(),
                entity.getSetsPlayer2(),
                entity.isDeuce(),
                entity.getIsAdvantagePlayer1(),
                entity.getCurrentSet(),
                entity.isDone(),
                entity.getWinner(),
                entity.getAcesPlayer1(),
                entity.getAcesPlayer2(),
                entity.getServingPlayer()
        );
    }

    public MatchScoreJpaEntity toEntity(MatchScore score) {
        MatchScoreJpaEntity entity = new MatchScoreJpaEntity();
        entity.setId(score.getId());
        entity.setMatchId(score.getMatchId());
        entity.setPointsPlayer1(score.getPointsPlayer1());
        entity.setPointsPlayer2(score.getPointsPlayer2());
        entity.setGamesPlayer1(score.getGamesPlayer1());
        entity.setGamesPlayer2(score.getGamesPlayer2());
        entity.setSetsPlayer1(score.getSetsPlayer1());
        entity.setSetsPlayer2(score.getSetsPlayer2());
        entity.setDeuce(score.isDeuce());
        entity.setIsAdvantagePlayer1(score.getIsAdvantagePlayer1());
        entity.setCurrentSet(score.getCurrentSet());
        entity.setDone(score.isDone());
        entity.setWinner(score.getWinner());
        entity.setAcesPlayer1(score.getAcesPlayer1());
        entity.setAcesPlayer2(score.getAcesPlayer2());
        entity.setServingPlayer(score.getServingPlayer());
        return entity;
    }
}
```

- [ ] **Schritt 4: `MatchScoreResponse.java` komplett ersetzen**

```java
package com.cas.tsas.match.infrastructure.web.dto.response;

import com.cas.tsas.match.domain.model.MatchScore;

import java.util.UUID;

public record MatchScoreResponse(
        UUID id,
        UUID matchId,
        int pointsPlayer1,
        int pointsPlayer2,
        int gamesPlayer1,
        int gamesPlayer2,
        int setsPlayer1,
        int setsPlayer2,
        boolean isDeuce,
        Boolean isAdvantagePlayer1,
        int currentSet,
        boolean isDone,
        String winner,
        int acesPlayer1,
        int acesPlayer2,
        Integer servingPlayer
) {
    public static MatchScoreResponse from(MatchScore score) {
        return new MatchScoreResponse(
                score.getId(),
                score.getMatchId(),
                score.getPointsPlayer1(),
                score.getPointsPlayer2(),
                score.getGamesPlayer1(),
                score.getGamesPlayer2(),
                score.getSetsPlayer1(),
                score.getSetsPlayer2(),
                score.isDeuce(),
                score.getIsAdvantagePlayer1(),
                score.getCurrentSet(),
                score.isDone(),
                score.getWinner(),
                score.getAcesPlayer1(),
                score.getAcesPlayer2(),
                score.getServingPlayer()
        );
    }
}
```

- [ ] **Schritt 5: `MatchService.java` — `createMatch()` Konstruktoraufruf aktualisieren**

Den `new MatchScore(...)`-Aufruf in `createMatch()` ersetzen:

```java
MatchScore score = new MatchScore(
        null, saved.getId(),
        0, 0,
        0, 0,
        0, 0,
        false, null,
        1, false, null,
        0, 0,
        null
);
```

- [ ] **Schritt 6: `ScoringService.java` — `rotateServe()` und Aufrufe hinzufügen**

**6a** — `awardGame()` am Ende (nach `checkSetWon`) ergänzen:

```java
private void awardGame(Match match, MatchScore score, boolean player1Scored) {
    score.setPointsPlayer1(0);
    score.setPointsPlayer2(0);
    score.setDeuce(false);
    score.setIsAdvantagePlayer1(null);

    if (player1Scored) {
        score.setGamesPlayer1(score.getGamesPlayer1() + 1);
    } else {
        score.setGamesPlayer2(score.getGamesPlayer2() + 1);
    }

    checkSetWon(match, score);
    rotateServe(score);
}
```

**6b** — In `applyTiebreakPoint()` den `else`-Zweig (regulärer Tiebreak-Gewinn) ergänzen:

```java
} else {
    // Regular tiebreak: award a game win to complete the set
    if (p1Won) {
        score.setGamesPlayer1(score.getGamesPlayer1() + 1);
        awardSet(match, score, true);
    } else {
        score.setGamesPlayer2(score.getGamesPlayer2() + 1);
        awardSet(match, score, false);
    }
    rotateServe(score);
}
```

**6c** — Private Methode am Ende der Klasse (vor der letzten `}`) einfügen:

```java
private void rotateServe(MatchScore score) {
    if (score.getServingPlayer() != null) {
        score.setServingPlayer(score.getServingPlayer() == 1 ? 2 : 1);
    }
}
```

- [ ] **Schritt 7: Test-Konstruktoraufrufe aktualisieren**

In **`ScoringServiceTest.java`** die `score()`-Factory (Zeile 46-49) aktualisieren — `, null` am Ende:

```java
private static MatchScore score(int pointsP1, int pointsP2,
                                int gamesP1, int gamesP2,
                                int setsP1, int setsP2) {
    return new MatchScore(null, UUID.randomUUID(),
            pointsP1, pointsP2, gamesP1, gamesP2, setsP1, setsP2,
            false, null, 1, false, null, 0, 0, null);
}
```

In **`MatchServiceTest.java`** die `freshScore()`-Factory (Zeile 70) aktualisieren:

```java
private static MatchScore freshScore() {
    return new MatchScore(null, MATCH_ID, 0, 0, 0, 0, 0, 0, false, null, 1, false, null, 0, 0, null);
}
```

In **`MatchPersistenceAdapterIT.java`** Zeile 107 aktualisieren:

```java
MatchScore score = new MatchScore(
        null, savedMatch.getId(), 2, 1, 3, 2, 1, 0, false, null, 2, false, null, 0, 0, null);
```

- [ ] **Schritt 8: `ScoringServiceTest.java` — `ServeRotation` Nested Class hinzufügen**

Am Ende der Klasse (vor der letzten `}`) einfügen:

```java
// =========================================================================
@Nested
class ServeRotation {

    @Test
    void serve_rotates_to_player2_after_player1_wins_game() {
        Match match = normalMatch();
        MatchScore s = score(3, 0, 0, 0, 0, 0);
        s.setServingPlayer(1);

        scoringService.applyPoint(match, s, true); // player1 wins game (40:0 → game)

        assertThat(s.getServingPlayer()).isEqualTo(2);
    }

    @Test
    void serve_rotates_to_player1_after_player2_wins_game() {
        Match match = normalMatch();
        MatchScore s = score(0, 3, 0, 0, 0, 0);
        s.setServingPlayer(2);

        scoringService.applyPoint(match, s, false); // player2 wins game

        assertThat(s.getServingPlayer()).isEqualTo(1);
    }

    @Test
    void serve_not_rotated_when_serving_player_is_null() {
        Match match = normalMatch();
        MatchScore s = score(3, 0, 0, 0, 0, 0);
        // servingPlayer is null (default from factory)

        scoringService.applyPoint(match, s, true);

        assertThat(s.getServingPlayer()).isNull();
    }

    @Test
    void serve_rotates_after_regular_tiebreak_win() {
        // Tiebreak at 6:4 points — one more point wins for player1 (7:4 → tiebreak won)
        Match match = normalMatch();
        MatchScore s = score(6, 4, 6, 6, 0, 0);
        s.setServingPlayer(1);

        scoringService.applyPoint(match, s, true);

        assertThat(s.getServingPlayer()).isEqualTo(2);
    }
}
```

- [ ] **Schritt 9: Backend kompilieren und Tests ausführen**

```bash
cd /Users/cbo/Projects/cas/tsas/backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test --no-daemon 2>&1 | tail -8
```
Erwartet: `BUILD SUCCESSFUL`

- [ ] **Schritt 10: Commit**

```bash
git add \
  backend/match-module/src/main/java/com/cas/tsas/match/domain/model/MatchScore.java \
  backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/entity/MatchScoreJpaEntity.java \
  backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/mapper/MatchScoreMapper.java \
  backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/dto/response/MatchScoreResponse.java \
  backend/match-module/src/main/java/com/cas/tsas/match/application/service/MatchService.java \
  backend/match-module/src/main/java/com/cas/tsas/match/application/service/ScoringService.java \
  backend/match-module/src/test/java/com/cas/tsas/match/application/service/ScoringServiceTest.java \
  backend/match-module/src/test/java/com/cas/tsas/match/application/service/MatchServiceTest.java \
  backend/app/src/test/java/com/cas/tsas/match/MatchPersistenceAdapterIT.java
git commit -m "feat: add servingPlayer to MatchScore; auto-rotate serve after each game"
```

---

## Task 2: SetServingPlayerUseCase + MatchService-Implementierung

**Files:**
- Create: `backend/match-module/src/main/java/com/cas/tsas/match/application/port/in/SetServingPlayerUseCase.java`
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/application/service/MatchService.java`
- Modify: `backend/match-module/src/test/java/com/cas/tsas/match/application/service/MatchServiceTest.java`

- [ ] **Schritt 1: `SetServingPlayerUseCase.java` erstellen**

```java
package com.cas.tsas.match.application.port.in;

import com.cas.tsas.match.domain.model.MatchScore;

import java.util.UUID;

public interface SetServingPlayerUseCase {

    MatchScore setServingPlayer(SetServingPlayerCommand command);

    record SetServingPlayerCommand(
            UUID matchId,
            boolean forPlayer1
    ) {}
}
```

- [ ] **Schritt 2: Failing Tests schreiben**

In `MatchServiceTest.java`, `import com.cas.tsas.match.application.port.in.SetServingPlayerUseCase;` ergänzen und folgende Nested Class am Ende der Klasse (vor der letzten `}`) hinzufügen:

```java
// =========================================================================
@Nested
class SetServingPlayer {

    @Test
    void throws_MatchNotFoundException_when_match_not_found() {
        when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            matchService.setServingPlayer(
                new SetServingPlayerUseCase.SetServingPlayerCommand(MATCH_ID, true)))
            .isInstanceOf(MatchNotFoundException.class);
    }

    @Test
    void throws_IllegalStateException_when_match_already_completed() {
        when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(completedMatch()));

        assertThatThrownBy(() ->
            matchService.setServingPlayer(
                new SetServingPlayerUseCase.SetServingPlayerCommand(MATCH_ID, true)))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void sets_serving_player_to_1_for_player1() {
        when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(inProgressMatch()));
        MatchScore score = freshScore();
        when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));
        when(saveMatchScorePort.saveMatchScore(any())).thenAnswer(inv -> inv.getArgument(0));

        MatchScore result = matchService.setServingPlayer(
                new SetServingPlayerUseCase.SetServingPlayerCommand(MATCH_ID, true));

        assertThat(result.getServingPlayer()).isEqualTo(1);
    }

    @Test
    void sets_serving_player_to_2_for_player2() {
        when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(inProgressMatch()));
        MatchScore score = freshScore();
        when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));
        when(saveMatchScorePort.saveMatchScore(any())).thenAnswer(inv -> inv.getArgument(0));

        MatchScore result = matchService.setServingPlayer(
                new SetServingPlayerUseCase.SetServingPlayerCommand(MATCH_ID, false));

        assertThat(result.getServingPlayer()).isEqualTo(2);
    }
}
```

- [ ] **Schritt 3: Tests ausführen — müssen FAIL**

```bash
cd /Users/cbo/Projects/cas/tsas/backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :match-module:test --tests "*MatchServiceTest*SetServingPlayer*" --no-daemon 2>&1 | tail -8
```
Erwartet: FAIL (MatchService kennt `setServingPlayer` noch nicht)

- [ ] **Schritt 4: `MatchService.java` — implements und Methode hinzufügen**

Klassendeklaration erweitern:

```java
public class MatchService implements CreateMatchUseCase, GetMatchUseCase, RecordPointUseCase,
        SetScoreUseCase, EndMatchUseCase, RecordAceUseCase, SetServingPlayerUseCase {
```

Import hinzufügen:

```java
import com.cas.tsas.match.application.port.in.SetServingPlayerUseCase;
```

Neue Methode am Ende der Klasse (vor der letzten `}`) einfügen:

```java
@Override
public MatchScore setServingPlayer(SetServingPlayerCommand command) {
    Match match = loadMatchPort.loadMatch(command.matchId())
            .orElseThrow(() -> new MatchNotFoundException(command.matchId()));

    if (match.getStatus() == MatchStatus.COMPLETED) {
        throw new IllegalStateException("Match is already completed");
    }

    MatchScore score = loadMatchScorePort.loadMatchScore(command.matchId())
            .orElseThrow(() -> new MatchNotFoundException(command.matchId()));

    score.setServingPlayer(command.forPlayer1() ? 1 : 2);

    return saveMatchScorePort.saveMatchScore(score);
}
```

- [ ] **Schritt 5: Tests ausführen — müssen PASS**

```bash
cd /Users/cbo/Projects/cas/tsas/backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :match-module:test --no-daemon 2>&1 | tail -8
```
Erwartet: `BUILD SUCCESSFUL`

- [ ] **Schritt 6: Commit**

```bash
git add \
  backend/match-module/src/main/java/com/cas/tsas/match/application/port/in/SetServingPlayerUseCase.java \
  backend/match-module/src/main/java/com/cas/tsas/match/application/service/MatchService.java \
  backend/match-module/src/test/java/com/cas/tsas/match/application/service/MatchServiceTest.java
git commit -m "feat: add SetServingPlayerUseCase — sets initial serving player"
```

---

## Task 3: REST Endpoints + recordAce-Validierung

**Files:**
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/MatchController.java`
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/application/service/MatchService.java`
- Modify: `backend/app/src/test/java/com/cas/tsas/match/MatchApiIT.java`
- Modify: `backend/match-module/src/test/java/com/cas/tsas/match/application/service/MatchServiceTest.java`

- [ ] **Schritt 1: Failing Integration Tests schreiben**

In `MatchApiIT.java` zwei neue Nested Classes hinzufügen (nach `RecordAce`):

```java
// =========================================================================
@Nested
class SetServingPlayer {

    @Test
    void sets_serving_player_1() throws Exception {
        UUID p1 = createPlayer();
        UUID p2 = createPlayer();
        UUID matchId = createMatch(p1, p2);

        mockMvc.perform(post("/api/matches/{id}/serve/player1", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servingPlayer").value(1));
    }

    @Test
    void sets_serving_player_2() throws Exception {
        UUID p1 = createPlayer();
        UUID p2 = createPlayer();
        UUID matchId = createMatch(p1, p2);

        mockMvc.perform(post("/api/matches/{id}/serve/player2", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servingPlayer").value(2));
    }

    @Test
    void returns_409_when_match_already_completed() throws Exception {
        UUID p1 = createPlayer();
        UUID p2 = createPlayer();
        UUID matchId = createMatch(p1, p2);

        mockMvc.perform(post("/api/matches/{id}/end", matchId));

        mockMvc.perform(post("/api/matches/{id}/serve/player1", matchId))
                .andExpect(status().isConflict());
    }
}
```

Ausserdem in der bestehenden `RecordAce`-Nested-Class zwei neue Tests hinzufügen:

```java
@Test
void returns_409_when_no_serving_player_set() throws Exception {
    UUID p1 = createPlayer();
    UUID p2 = createPlayer();
    UUID matchId = createMatch(p1, p2);
    // No /serve call — servingPlayer is null

    mockMvc.perform(post("/api/matches/{id}/ace/player1", matchId))
            .andExpect(status().isConflict());
}

@Test
void returns_409_when_wrong_player_tries_to_ace() throws Exception {
    UUID p1 = createPlayer();
    UUID p2 = createPlayer();
    UUID matchId = createMatch(p1, p2);

    mockMvc.perform(post("/api/matches/{id}/serve/player1", matchId)); // player1 serves

    mockMvc.perform(post("/api/matches/{id}/ace/player2", matchId)) // player2 tries to ace
            .andExpect(status().isConflict());
}
```

Den bestehenden Test `acePlayer1_incrementsAceCounterAndScoresPoint` aktualisieren — vor dem Ass-Aufruf Aufschlag setzen:

```java
@Test
void acePlayer1_incrementsAceCounterAndScoresPoint() throws Exception {
    UUID p1 = createPlayer();
    UUID p2 = createPlayer();
    UUID matchId = createMatch(p1, p2);

    mockMvc.perform(post("/api/matches/{id}/serve/player1", matchId)); // player1 serves

    mockMvc.perform(post("/api/matches/{id}/ace/player1", matchId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.acesPlayer1").value(1))
            .andExpect(jsonPath("$.acesPlayer2").value(0))
            .andExpect(jsonPath("$.pointsPlayer1").value(1));
}
```

- [ ] **Schritt 2: Failing Test ausführen**

```bash
cd /Users/cbo/Projects/cas/tsas/backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :app:test --tests "*MatchApiIT*SetServingPlayer*" --no-daemon 2>&1 | tail -8
```
Erwartet: FAIL mit 404 (Endpoints existieren noch nicht)

- [ ] **Schritt 3: `MatchController.java` — SetServingPlayerUseCase verdrahten**

Import hinzufügen:

```java
import com.cas.tsas.match.application.port.in.SetServingPlayerUseCase;
```

Field und Konstruktor erweitern:

```java
private final SetServingPlayerUseCase setServingPlayerUseCase;

public MatchController(CreateMatchUseCase createMatchUseCase,
                       GetMatchUseCase getMatchUseCase,
                       RecordPointUseCase recordPointUseCase,
                       SetScoreUseCase setScoreUseCase,
                       EndMatchUseCase endMatchUseCase,
                       RecordAceUseCase recordAceUseCase,
                       SetServingPlayerUseCase setServingPlayerUseCase) {
    this.createMatchUseCase = createMatchUseCase;
    this.getMatchUseCase = getMatchUseCase;
    this.recordPointUseCase = recordPointUseCase;
    this.setScoreUseCase = setScoreUseCase;
    this.endMatchUseCase = endMatchUseCase;
    this.recordAceUseCase = recordAceUseCase;
    this.setServingPlayerUseCase = setServingPlayerUseCase;
}
```

Zwei neue Endpoint-Methoden nach `acePlayer2` einfügen:

```java
@PostMapping("/{id}/serve/player1")
public MatchScoreResponse servePlayer1(@PathVariable UUID id) {
    var command = new SetServingPlayerUseCase.SetServingPlayerCommand(id, true);
    return MatchScoreResponse.from(setServingPlayerUseCase.setServingPlayer(command));
}

@PostMapping("/{id}/serve/player2")
public MatchScoreResponse servePlayer2(@PathVariable UUID id) {
    var command = new SetServingPlayerUseCase.SetServingPlayerCommand(id, false);
    return MatchScoreResponse.from(setServingPlayerUseCase.setServingPlayer(command));
}
```

- [ ] **Schritt 4: `MatchService.recordAce()` — Validierung hinzufügen**

In `recordAce()`, nach dem COMPLETED-Guard und dem Score-Laden, vor dem Ace-Increment einfügen:

```java
Integer serving = score.getServingPlayer();
if (serving == null) {
    throw new IllegalStateException("No serving player set");
}
if (command.forPlayer1() && serving != 1) {
    throw new IllegalStateException("Player 1 is not serving");
}
if (!command.forPlayer1() && serving != 2) {
    throw new IllegalStateException("Player 2 is not serving");
}
```

Die vollständige `recordAce()`-Methode sieht danach so aus:

```java
@Override
public MatchScore recordAce(RecordAceCommand command) {
    Match match = loadMatchPort.loadMatch(command.matchId())
            .orElseThrow(() -> new MatchNotFoundException(command.matchId()));

    if (match.getStatus() == MatchStatus.COMPLETED) {
        throw new IllegalStateException("Match is already completed");
    }

    MatchScore score = loadMatchScorePort.loadMatchScore(command.matchId())
            .orElseThrow(() -> new MatchNotFoundException(command.matchId()));

    Integer serving = score.getServingPlayer();
    if (serving == null) {
        throw new IllegalStateException("No serving player set");
    }
    if (command.forPlayer1() && serving != 1) {
        throw new IllegalStateException("Player 1 is not serving");
    }
    if (!command.forPlayer1() && serving != 2) {
        throw new IllegalStateException("Player 2 is not serving");
    }

    if (command.forPlayer1()) {
        score.setAcesPlayer1(score.getAcesPlayer1() + 1);
    } else {
        score.setAcesPlayer2(score.getAcesPlayer2() + 1);
    }

    scoringService.applyPoint(match, score, command.forPlayer1());
    MatchScore saved = saveMatchScorePort.saveMatchScore(score);

    if (saved.isDone()) {
        match.setStatus(MatchStatus.COMPLETED);
        saveMatchPort.saveMatch(match);
    }

    return saved;
}
```

- [ ] **Schritt 5: `MatchServiceTest.java` — bestehende RecordAce-Tests reparieren**

Die bestehenden Tests in `@Nested class RecordAce` in `MatchServiceTest.java` schlagen jetzt fehl, weil `freshScore()` `servingPlayer = null` zurückgibt. Die Tests, die einen Ass aufzeichnen, müssen `servingPlayer = 1` (für player1) bzw. `servingPlayer = 2` (für player2) setzen.

Test `increments_acesPlayer1_and_scores_point` anpassen:

```java
@Test
void increments_acesPlayer1_and_scores_point() {
    when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(inProgressMatch()));
    MatchScore score = freshScore();
    score.setServingPlayer(1); // player1 serves
    when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));
    when(saveMatchScorePort.saveMatchScore(any())).thenAnswer(inv -> inv.getArgument(0));

    MatchScore result = matchService.recordAce(new RecordAceUseCase.RecordAceCommand(MATCH_ID, true));

    assertThat(result.getAcesPlayer1()).isEqualTo(1);
    assertThat(result.getAcesPlayer2()).isEqualTo(0);
    assertThat(result.getPointsPlayer1()).isEqualTo(1);
}
```

Test `increments_acesPlayer2_for_player2` anpassen:

```java
@Test
void increments_acesPlayer2_for_player2() {
    when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(inProgressMatch()));
    MatchScore score = freshScore();
    score.setServingPlayer(2); // player2 serves
    when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));
    when(saveMatchScorePort.saveMatchScore(any())).thenAnswer(inv -> inv.getArgument(0));

    MatchScore result = matchService.recordAce(new RecordAceUseCase.RecordAceCommand(MATCH_ID, false));

    assertThat(result.getAcesPlayer2()).isEqualTo(1);
    assertThat(result.getAcesPlayer1()).isEqualTo(0);
}
```

Zwei neue Tests in `@Nested class RecordAce` hinzufügen (Serving-Validierung):

```java
@Test
void throws_IllegalStateException_when_no_serving_player_set() {
    when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(inProgressMatch()));
    MatchScore score = freshScore(); // servingPlayer = null
    when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));

    assertThatThrownBy(() ->
        matchService.recordAce(new RecordAceUseCase.RecordAceCommand(MATCH_ID, true)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No serving player set");
}

@Test
void throws_IllegalStateException_when_wrong_player_tries_to_ace() {
    when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(inProgressMatch()));
    MatchScore score = freshScore();
    score.setServingPlayer(2); // player2 serves
    when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));

    assertThatThrownBy(() ->
        matchService.recordAce(new RecordAceUseCase.RecordAceCommand(MATCH_ID, true))) // player1 tries
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Player 1 is not serving");
}
```

- [ ] **Schritt 6: Alle Backend-Tests ausführen**

```bash
cd /Users/cbo/Projects/cas/tsas/backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test --no-daemon 2>&1 | tail -8
```
Erwartet: `BUILD SUCCESSFUL`

- [ ] **Schritt 7: Commit**

```bash
git add \
  backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/MatchController.java \
  backend/match-module/src/main/java/com/cas/tsas/match/application/service/MatchService.java \
  backend/app/src/test/java/com/cas/tsas/match/MatchApiIT.java \
  backend/match-module/src/test/java/com/cas/tsas/match/application/service/MatchServiceTest.java
git commit -m "feat: add POST /api/matches/{id}/serve/player1|player2; validate server in recordAce"
```

---

## Task 4: Frontend — Model und ApiService

**Files:**
- Modify: `frontend/src/app/core/models/match.model.ts`
- Modify: `frontend/src/app/core/services/api.service.ts`
- Modify: `frontend/src/app/features/matches/score/score.component.cy.ts`

- [ ] **Schritt 1: `match.model.ts` — `MatchScore` Interface erweitern**

```typescript
export interface MatchScore {
  id: string;
  matchId: string;
  pointsPlayer1: number;
  pointsPlayer2: number;
  gamesPlayer1: number;
  gamesPlayer2: number;
  setsPlayer1: number;
  setsPlayer2: number;
  isDeuce: boolean;
  isAdvantagePlayer1: boolean | null;
  currentSet: number;
  isDone: boolean;
  winner: string | null;
  acesPlayer1: number;
  acesPlayer2: number;
  servingPlayer: number | null;
}
```

- [ ] **Schritt 2: `api.service.ts` — zwei Methoden nach `acePlayer2` hinzufügen**

```typescript
setServingPlayer1(matchId: string): Observable<MatchScore> {
  return this.http.post<MatchScore>(`${this.base}/matches/${matchId}/serve/player1`, {});
}

setServingPlayer2(matchId: string): Observable<MatchScore> {
  return this.http.post<MatchScore>(`${this.base}/matches/${matchId}/serve/player2`, {});
}
```

- [ ] **Schritt 3: `score.component.cy.ts` — Fixture aktualisieren**

Im `score`-Objekt innerhalb von `makeMatch()` `servingPlayer: null` ergänzen (nach `acesPlayer2`):

```typescript
acesPlayer2: 0,
servingPlayer: null,
```

- [ ] **Schritt 4: Angular Build prüfen**

```bash
cd /Users/cbo/Projects/cas/tsas/frontend
npx ng build --configuration development 2>&1 | tail -5
```
Erwartet: `Application bundle generation complete`

- [ ] **Schritt 5: Commit**

```bash
git add \
  frontend/src/app/core/models/match.model.ts \
  frontend/src/app/core/services/api.service.ts \
  frontend/src/app/features/matches/score/score.component.cy.ts
git commit -m "feat: add servingPlayer to MatchScore model and setServingPlayer to ApiService"
```

---

## Task 5: Frontend — ScoreComponent UI

**Files:**
- Modify: `frontend/src/app/features/matches/score/score.component.ts`

- [ ] **Schritt 1: `servingPlayer` Signal und `setServe()` Methode hinzufügen**

Nach dem `player2Name`-Signal einfügen:

```typescript
servingPlayer = computed(() => this.matchData()?.score?.servingPlayer ?? null);
```

Nach der `recordAce()`-Methode einfügen:

```typescript
setServe(forPlayer1: boolean) {
  const m = this.matchData();
  if (!m || m.status === 'COMPLETED') return;

  const obs = forPlayer1
    ? this.api.setServingPlayer1(this.matchId)
    : this.api.setServingPlayer2(this.matchId);

  obs.subscribe({
    next: (score) => {
      this.matchData.update(md => md ? { ...md, score } : md);
    },
    error: () => this.snackBar.open('Fehler beim Speichern', 'OK', { duration: 3000 })
  });
}
```

- [ ] **Schritt 2: Ace-Button `[disabled]`-Bedingungen aktualisieren**

Den Player-1-Ass-Button (bei `(click)="recordAce(true)"`) anpassen:

```html
<button class="ace-btn"
        [disabled]="matchData()!.status === 'COMPLETED' || servingPlayer() !== 1"
        (click)="recordAce(true)">
  <span class="ace-icon">🎾</span>
  <span class="ace-count">{{ matchData()!.score.acesPlayer1 }}</span>
  <span class="ace-lbl">Asse {{ player1Name() }}</span>
</button>
```

Den Player-2-Ass-Button anpassen:

```html
<button class="ace-btn"
        [disabled]="matchData()!.status === 'COMPLETED' || servingPlayer() !== 2"
        (click)="recordAce(false)">
  <span class="ace-icon">🎾</span>
  <span class="ace-count">{{ matchData()!.score.acesPlayer2 }}</span>
  <span class="ace-lbl">Asse {{ player2Name() }}</span>
</button>
```

- [ ] **Schritt 3: Aufschlag-Toggle im Template einfügen**

Im `action-buttons`-Div, ganz oben (vor dem `mat-stroked-button`), einfügen:

```html
@if (servingPlayer() === null && matchData()!.status !== 'COMPLETED') {
  <div class="serve-toggle">
    <span class="serve-lbl">Aufschlag</span>
    <div class="serve-btns">
      <button class="serve-btn" (click)="setServe(true)">{{ player1Name() }}</button>
      <button class="serve-btn" (click)="setServe(false)">{{ player2Name() }}</button>
    </div>
  </div>
}
```

Das vollständige `action-buttons`-Div sieht danach so aus:

```html
<div class="action-buttons">
  @if (servingPlayer() === null && matchData()!.status !== 'COMPLETED') {
    <div class="serve-toggle">
      <span class="serve-lbl">Aufschlag</span>
      <div class="serve-btns">
        <button class="serve-btn" (click)="setServe(true)">{{ player1Name() }}</button>
        <button class="serve-btn" (click)="setServe(false)">{{ player2Name() }}</button>
      </div>
    </div>
  }
  <button mat-stroked-button (click)="openEditDialog()">
    <mat-icon>edit</mat-icon>
    Score korrigieren
  </button>
  @if (matchData()!.status !== 'COMPLETED') {
    <button mat-raised-button color="warn" (click)="endMatch()">
      <mat-icon>stop</mat-icon>
      Match beenden
    </button>
  }
</div>
```

- [ ] **Schritt 4: Aufschlag-Indikator in Player-Overlays einfügen**

Im Player-2-Overlay (top-half), die `pname`-Zeile ersetzen:

```html
<div class="pname">
  @if (servingPlayer() === 2) { <span class="serve-indicator">🎾 </span> }{{ player2Name() }}
</div>
```

Im Player-1-Overlay (bottom-half), die `pname`-Zeile ersetzen:

```html
<div class="pname">
  @if (servingPlayer() === 1) { <span class="serve-indicator">🎾 </span> }{{ player1Name() }}
</div>
```

- [ ] **Schritt 5: CSS hinzufügen**

Am Ende des `styles`-Arrays (vor dem abschliessenden `` ` ``) einfügen:

```css
.serve-toggle {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}
.serve-lbl {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: rgba(255,255,255,.6);
}
.serve-btns { display: flex; gap: 8px; }
.serve-btn {
  background: transparent;
  border: 1px solid rgba(255,255,255,.5);
  border-radius: 6px;
  color: white;
  font-size: 12px;
  padding: 5px 10px;
  cursor: pointer;
  transition: background 0.15s;
}
.serve-btn:hover { background: rgba(255,255,255,.15); }
.serve-indicator { font-size: 14px; }
```

- [ ] **Schritt 6: Build prüfen**

```bash
cd /Users/cbo/Projects/cas/tsas/frontend
npx ng build --configuration development 2>&1 | tail -5
```
Erwartet: `Application bundle generation complete`

- [ ] **Schritt 7: Manuell testen**

```bash
cd /Users/cbo/Projects/cas/tsas/frontend
npm start
```

- Im Browser `http://localhost:4200` öffnen
- Ein Match starten → Score-Screen öffnet: beide Ass-Buttons sind deaktiviert (opacity 0.4), Toggle "Aufschlag / Spieler 1 / Spieler 2" ist sichtbar
- Auf "Spieler 1" klicken → Toggle verschwindet, 🎾 erscheint beim Player-1-Namen, Player-1-Ass-Button wird aktiv, Player-2-Ass-Button bleibt deaktiviert
- Player-1-Ass klicken → Ass-Zähler erhöht sich, Punkt wird gezählt
- Player-2-Ass-Button ist weiterhin deaktiviert
- Match beenden → beide Ass-Buttons werden deaktiviert

- [ ] **Schritt 8: Commit**

```bash
git add frontend/src/app/features/matches/score/score.component.ts
git commit -m "feat: add serving player toggle and indicator to score page; disable ace button for returner"
```
