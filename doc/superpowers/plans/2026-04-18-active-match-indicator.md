# Active Match Indicator Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show a clickable sports_tennis icon in the player list for players currently in an IN_PROGRESS match, navigating to that match on click.

**Architecture:** New `FindActiveMatchPort` output port in `player-module` (implemented by `MatchPersistenceAdapter`), consumed by `PlayerService` via a new `SearchPlayerUseCase` method. `PlayerController.listPlayers()` does one bulk lookup instead of per-player calls. Frontend adds a `status` column to the players table.

**Tech Stack:** Spring Boot 3.4, Spring Data JPA (JPQL), JUnit 5 + Mockito, Angular 19 Signals, Angular Material

---

## File Map

| Action | File |
|--------|------|
| Create | `backend/player-module/src/main/java/com/cas/tsas/player/application/port/out/FindActiveMatchPort.java` |
| Modify | `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/repository/MatchJpaRepository.java` |
| Modify | `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/repository/MatchPersistenceAdapter.java` |
| Modify | `backend/player-module/src/main/java/com/cas/tsas/player/application/port/in/SearchPlayerUseCase.java` |
| Modify | `backend/player-module/src/main/java/com/cas/tsas/player/application/service/PlayerService.java` |
| Modify | `backend/player-module/src/main/java/com/cas/tsas/player/infrastructure/web/dto/response/PlayerResponse.java` |
| Modify | `backend/player-module/src/main/java/com/cas/tsas/player/infrastructure/web/PlayerController.java` |
| Modify | `backend/player-module/src/test/java/com/cas/tsas/player/application/service/PlayerServiceTest.java` |
| Modify | `backend/app/src/test/java/com/cas/tsas/match/MatchPersistenceAdapterIT.java` |
| Modify | `backend/app/src/test/java/com/cas/tsas/player/PlayerApiIT.java` |
| Modify | `frontend/src/app/core/models/player.model.ts` |
| Modify | `frontend/src/app/features/players/players.component.ts` |

---

### Task 1: Create `FindActiveMatchPort` output port

**Files:**
- Create: `backend/player-module/src/main/java/com/cas/tsas/player/application/port/out/FindActiveMatchPort.java`

- [ ] **Step 1: Create the port interface**

```java
package com.cas.tsas.player.application.port.out;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface FindActiveMatchPort {
    Map<UUID, UUID> findActiveMatchIdsByPlayerIds(Set<UUID> playerIds);
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/player-module/src/main/java/com/cas/tsas/player/application/port/out/FindActiveMatchPort.java
git commit -m "feat: add FindActiveMatchPort output port"
```

---

### Task 2: Add JPQL query to `MatchJpaRepository` (TDD)

**Files:**
- Modify: `backend/app/src/test/java/com/cas/tsas/match/MatchPersistenceAdapterIT.java`
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/repository/MatchJpaRepository.java`

- [ ] **Step 1: Write failing test**

Add a new `@Nested` class to `MatchPersistenceAdapterIT` after the existing `ExistsByPlayerId` class:

```java
// =========================================================================
@Nested
class FindActiveMatchIdsByPlayerIds {

    @Test
    void returns_match_id_for_player_in_active_match() {
        Match saved = matchAdapter.saveMatch(newMatch()); // IN_PROGRESS

        Map<UUID, UUID> result = matchAdapter.findActiveMatchIdsByPlayerIds(Set.of(PLAYER1_ID));

        assertThat(result).containsEntry(PLAYER1_ID, saved.getId());
    }

    @Test
    void returns_match_id_for_player2_as_well() {
        Match saved = matchAdapter.saveMatch(newMatch());

        Map<UUID, UUID> result = matchAdapter.findActiveMatchIdsByPlayerIds(Set.of(PLAYER2_ID));

        assertThat(result).containsEntry(PLAYER2_ID, saved.getId());
    }

    @Test
    void excludes_completed_matches() {
        Match completed = new Match(null, PLAYER1_ID, PLAYER2_ID, 2, false, false, MatchStatus.COMPLETED);
        matchAdapter.saveMatch(completed);

        Map<UUID, UUID> result = matchAdapter.findActiveMatchIdsByPlayerIds(Set.of(PLAYER1_ID));

        assertThat(result).isEmpty();
    }

    @Test
    void returns_empty_map_for_player_with_no_match() {
        Map<UUID, UUID> result = matchAdapter.findActiveMatchIdsByPlayerIds(Set.of(UUID.randomUUID()));

        assertThat(result).isEmpty();
    }

    @Test
    void returns_empty_map_when_input_is_empty() {
        Map<UUID, UUID> result = matchAdapter.findActiveMatchIdsByPlayerIds(Set.of());

        assertThat(result).isEmpty();
    }
}
```

Add the missing import at the top of `MatchPersistenceAdapterIT`:
```java
import java.util.Map;
import java.util.Set;
```

- [ ] **Step 2: Run to confirm tests fail**

```bash
cd backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test --tests "com.cas.tsas.match.MatchPersistenceAdapterIT.FindActiveMatchIdsByPlayerIds*" 2>&1 | tail -20
```

Expected: compilation error — `findActiveMatchIdsByPlayerIds` does not exist yet.

- [ ] **Step 3: Add JPQL query to `MatchJpaRepository`**

Full file after change:

```java
package com.cas.tsas.match.infrastructure.persistence.repository;

import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.match.infrastructure.persistence.entity.MatchJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface MatchJpaRepository extends JpaRepository<MatchJpaEntity, UUID> {

    boolean existsByPlayer1IdOrPlayer2Id(UUID player1Id, UUID player2Id);

    @Query("SELECT m FROM MatchJpaEntity m WHERE m.status = :status AND (m.player1Id IN :ids OR m.player2Id IN :ids)")
    List<MatchJpaEntity> findByStatusAndPlayerIdIn(@Param("status") MatchStatus status, @Param("ids") Set<UUID> ids);
}
```

- [ ] **Step 4: Add `FindActiveMatchPort` implementation to `MatchPersistenceAdapter`**

Full file after change:

```java
package com.cas.tsas.match.infrastructure.persistence.repository;

import com.cas.tsas.match.application.port.out.LoadMatchPort;
import com.cas.tsas.match.application.port.out.SaveMatchPort;
import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.match.infrastructure.persistence.entity.MatchJpaEntity;
import com.cas.tsas.match.infrastructure.persistence.mapper.MatchMapper;
import com.cas.tsas.player.application.port.out.FindActiveMatchPort;
import com.cas.tsas.player.application.port.out.HasMatchesPort;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class MatchPersistenceAdapter implements LoadMatchPort, SaveMatchPort, HasMatchesPort, FindActiveMatchPort {

    private final MatchJpaRepository repository;
    private final MatchMapper mapper;

    public MatchPersistenceAdapter(MatchJpaRepository repository, MatchMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Match> loadMatch(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Match> loadAllMatches() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByPlayerId(UUID playerId) {
        return repository.existsByPlayer1IdOrPlayer2Id(playerId, playerId);
    }

    @Override
    public Map<UUID, UUID> findActiveMatchIdsByPlayerIds(Set<UUID> playerIds) {
        if (playerIds.isEmpty()) return Map.of();
        List<MatchJpaEntity> matches = repository.findByStatusAndPlayerIdIn(MatchStatus.IN_PROGRESS, playerIds);
        Map<UUID, UUID> result = new HashMap<>();
        for (MatchJpaEntity m : matches) {
            if (playerIds.contains(m.getPlayer1Id())) result.put(m.getPlayer1Id(), m.getId());
            if (playerIds.contains(m.getPlayer2Id())) result.put(m.getPlayer2Id(), m.getId());
        }
        return result;
    }

    @Override
    public Match saveMatch(Match match) {
        var entity = mapper.toEntity(match);
        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
}
```

- [ ] **Step 5: Run tests and confirm they pass**

```bash
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test --tests "com.cas.tsas.match.MatchPersistenceAdapterIT*" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/repository/MatchJpaRepository.java
git add backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/repository/MatchPersistenceAdapter.java
git add backend/app/src/test/java/com/cas/tsas/match/MatchPersistenceAdapterIT.java
git commit -m "feat: implement FindActiveMatchPort in MatchPersistenceAdapter"
```

---

### Task 3: Wire `FindActiveMatchPort` through `SearchPlayerUseCase` and `PlayerService` (TDD)

**Files:**
- Modify: `backend/player-module/src/main/java/com/cas/tsas/player/application/port/in/SearchPlayerUseCase.java`
- Modify: `backend/player-module/src/main/java/com/cas/tsas/player/application/service/PlayerService.java`
- Modify: `backend/player-module/src/test/java/com/cas/tsas/player/application/service/PlayerServiceTest.java`

- [ ] **Step 1: Add method to `SearchPlayerUseCase`**

Full file after change:

```java
package com.cas.tsas.player.application.port.in;

import com.cas.tsas.player.domain.model.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface SearchPlayerUseCase {

    Player findById(UUID id);

    List<Player> findAll();

    Map<UUID, UUID> findActiveMatchIdsByPlayerIds(Set<UUID> playerIds);
}
```

- [ ] **Step 2: Write failing unit test in `PlayerServiceTest`**

Add `@Mock private FindActiveMatchPort findActiveMatchPort;` to the mock fields (Mockito's `@InjectMocks` will inject it automatically):

```java
@Mock private FindActiveMatchPort findActiveMatchPort;
```

Add missing import:
```java
import com.cas.tsas.player.application.port.out.FindActiveMatchPort;
import java.util.Map;
import java.util.Set;
```

Add a new `@Nested` class at the end of `PlayerServiceTest`:

```java
// =========================================================================
@Nested
class FindActiveMatchIdsByPlayerIds {

    @Test
    void delegates_to_port_and_returns_result() {
        UUID matchId = UUID.randomUUID();
        when(findActiveMatchPort.findActiveMatchIdsByPlayerIds(Set.of(PLAYER_ID)))
                .thenReturn(Map.of(PLAYER_ID, matchId));

        Map<UUID, UUID> result = playerService.findActiveMatchIdsByPlayerIds(Set.of(PLAYER_ID));

        assertThat(result).containsEntry(PLAYER_ID, matchId);
    }

    @Test
    void returns_empty_map_when_no_active_matches() {
        when(findActiveMatchPort.findActiveMatchIdsByPlayerIds(Set.of(PLAYER_ID)))
                .thenReturn(Map.of());

        Map<UUID, UUID> result = playerService.findActiveMatchIdsByPlayerIds(Set.of(PLAYER_ID));

        assertThat(result).isEmpty();
    }
}
```

- [ ] **Step 3: Run test to confirm it fails**

```bash
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test --tests "com.cas.tsas.player.application.service.PlayerServiceTest.FindActiveMatchIdsByPlayerIds*" 2>&1 | tail -20
```

Expected: compilation error — `findActiveMatchIdsByPlayerIds` not implemented.

- [ ] **Step 4: Implement `findActiveMatchIdsByPlayerIds` in `PlayerService`**

Add `FindActiveMatchPort` field and constructor param, then implement the method. Full file after change:

```java
package com.cas.tsas.player.application.service;

import com.cas.tsas.player.application.port.in.CreatePlayerUseCase;
import com.cas.tsas.player.application.port.in.DeletePlayerUseCase;
import com.cas.tsas.player.application.port.in.SearchPlayerUseCase;
import com.cas.tsas.player.application.port.in.UpdatePlayerUseCase;
import com.cas.tsas.player.application.port.out.DeletePlayerPort;
import com.cas.tsas.player.application.port.out.FindActiveMatchPort;
import com.cas.tsas.player.application.port.out.HasMatchesPort;
import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.player.application.port.out.SavePlayerPort;
import com.cas.tsas.player.domain.exception.PlayerNotFoundException;
import com.cas.tsas.player.domain.model.Player;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class PlayerService implements CreatePlayerUseCase, SearchPlayerUseCase, UpdatePlayerUseCase, DeletePlayerUseCase {

    private final LoadPlayerPort loadPlayerPort;
    private final SavePlayerPort savePlayerPort;
    private final DeletePlayerPort deletePlayerPort;
    private final HasMatchesPort hasMatchesPort;
    private final FindActiveMatchPort findActiveMatchPort;

    public PlayerService(LoadPlayerPort loadPlayerPort, SavePlayerPort savePlayerPort,
                         DeletePlayerPort deletePlayerPort, HasMatchesPort hasMatchesPort,
                         FindActiveMatchPort findActiveMatchPort) {
        this.loadPlayerPort = loadPlayerPort;
        this.savePlayerPort = savePlayerPort;
        this.deletePlayerPort = deletePlayerPort;
        this.hasMatchesPort = hasMatchesPort;
        this.findActiveMatchPort = findActiveMatchPort;
    }

    @Override
    public Player createPlayer(CreatePlayerCommand command) {
        Player player = new Player(
                null,
                command.firstName(),
                command.lastName(),
                command.gender(),
                command.handedness(),
                command.backhandType(),
                command.ranking(),
                command.nationality(),
                command.birthDate()
        );
        return savePlayerPort.savePlayer(player);
    }

    @Override
    @Transactional(readOnly = true)
    public Player findById(UUID id) {
        return loadPlayerPort.loadPlayer(id)
                .orElseThrow(() -> new PlayerNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Player> findAll() {
        return loadPlayerPort.loadAllPlayers();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, UUID> findActiveMatchIdsByPlayerIds(Set<UUID> playerIds) {
        return findActiveMatchPort.findActiveMatchIdsByPlayerIds(playerIds);
    }

    @Override
    public Player updatePlayer(UpdatePlayerCommand command) {
        Player player = loadPlayerPort.loadPlayer(command.id())
                .orElseThrow(() -> new PlayerNotFoundException(command.id()));
        player.setFirstName(command.firstName());
        player.setLastName(command.lastName());
        player.setGender(command.gender());
        player.setHandedness(command.handedness());
        player.setBackhandType(command.backhandType());
        player.setRanking(command.ranking());
        player.setNationality(command.nationality());
        player.setBirthDate(command.birthDate());
        return savePlayerPort.savePlayer(player);
    }

    @Override
    public boolean hasMatches(UUID id) {
        return hasMatchesPort.existsByPlayerId(id);
    }

    @Override
    public void deletePlayer(UUID id) {
        if (hasMatchesPort.existsByPlayerId(id)) {
            throw new IllegalStateException("Spieler hat Matches und kann nicht gelöscht werden.");
        }
        loadPlayerPort.loadPlayer(id).orElseThrow(() -> new PlayerNotFoundException(id));
        deletePlayerPort.deletePlayer(id);
    }

    @Override
    public void deactivatePlayer(UUID id) {
        Player player = loadPlayerPort.loadPlayer(id)
                .orElseThrow(() -> new PlayerNotFoundException(id));
        player.setActive(false);
        savePlayerPort.savePlayer(player);
    }
}
```

- [ ] **Step 5: Run tests and confirm they pass**

```bash
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test --tests "com.cas.tsas.player.application.service.PlayerServiceTest*" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add backend/player-module/src/main/java/com/cas/tsas/player/application/port/in/SearchPlayerUseCase.java
git add backend/player-module/src/main/java/com/cas/tsas/player/application/service/PlayerService.java
git add backend/player-module/src/test/java/com/cas/tsas/player/application/service/PlayerServiceTest.java
git commit -m "feat: add findActiveMatchIdsByPlayerIds to SearchPlayerUseCase and PlayerService"
```

---

### Task 4: Add `activeMatchId` to `PlayerResponse` and wire `PlayerController`

**Files:**
- Modify: `backend/player-module/src/main/java/com/cas/tsas/player/infrastructure/web/dto/response/PlayerResponse.java`
- Modify: `backend/player-module/src/main/java/com/cas/tsas/player/infrastructure/web/PlayerController.java`

- [ ] **Step 1: Update `PlayerResponse` record**

Full file after change:

```java
package com.cas.tsas.player.infrastructure.web.dto.response;

import com.cas.tsas.player.domain.model.BackhandType;
import com.cas.tsas.player.domain.model.Gender;
import com.cas.tsas.player.domain.model.Handedness;
import com.cas.tsas.player.domain.model.Player;

import java.time.LocalDate;
import java.util.UUID;

public record PlayerResponse(
        UUID id,
        String firstName,
        String lastName,
        Gender gender,
        Handedness handedness,
        BackhandType backhandType,
        String ranking,
        String nationality,
        LocalDate birthDate,
        boolean active,
        boolean deletable,
        UUID activeMatchId
) {
    public static PlayerResponse from(Player player, boolean deletable, UUID activeMatchId) {
        return new PlayerResponse(
                player.getId(),
                player.getFirstName(),
                player.getLastName(),
                player.getGender(),
                player.getHandedness(),
                player.getBackhandType(),
                player.getRanking(),
                player.getNationality(),
                player.getBirthDate(),
                player.isActive(),
                deletable,
                activeMatchId
        );
    }
}
```

- [ ] **Step 2: Update `PlayerController`**

Full file after change:

```java
package com.cas.tsas.player.infrastructure.web;

import com.cas.tsas.player.application.port.in.CreatePlayerUseCase;
import com.cas.tsas.player.application.port.in.DeletePlayerUseCase;
import com.cas.tsas.player.application.port.in.SearchPlayerUseCase;
import com.cas.tsas.player.application.port.in.UpdatePlayerUseCase;
import com.cas.tsas.player.domain.model.Player;
import com.cas.tsas.player.infrastructure.web.dto.request.CreatePlayerRequest;
import com.cas.tsas.player.infrastructure.web.dto.request.UpdatePlayerRequest;
import com.cas.tsas.player.infrastructure.web.dto.response.PlayerResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final CreatePlayerUseCase createPlayerUseCase;
    private final SearchPlayerUseCase searchPlayerUseCase;
    private final UpdatePlayerUseCase updatePlayerUseCase;
    private final DeletePlayerUseCase deletePlayerUseCase;

    public PlayerController(CreatePlayerUseCase createPlayerUseCase,
                            SearchPlayerUseCase searchPlayerUseCase,
                            UpdatePlayerUseCase updatePlayerUseCase,
                            DeletePlayerUseCase deletePlayerUseCase) {
        this.createPlayerUseCase = createPlayerUseCase;
        this.searchPlayerUseCase = searchPlayerUseCase;
        this.updatePlayerUseCase = updatePlayerUseCase;
        this.deletePlayerUseCase = deletePlayerUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerResponse createPlayer(@Valid @RequestBody CreatePlayerRequest request) {
        var command = new CreatePlayerUseCase.CreatePlayerCommand(
                request.firstName(),
                request.lastName(),
                request.gender(),
                request.handedness(),
                request.backhandType(),
                request.ranking(),
                request.nationality(),
                request.birthDate()
        );
        var player = createPlayerUseCase.createPlayer(command);
        return PlayerResponse.from(player, true, null);
    }

    @GetMapping("/{id}")
    public PlayerResponse getPlayer(@PathVariable UUID id) {
        Player player = searchPlayerUseCase.findById(id);
        Map<UUID, UUID> activeMatchIds = searchPlayerUseCase.findActiveMatchIdsByPlayerIds(Set.of(id));
        return PlayerResponse.from(player, !deletePlayerUseCase.hasMatches(id), activeMatchIds.get(id));
    }

    @GetMapping
    public List<PlayerResponse> listPlayers() {
        List<Player> players = searchPlayerUseCase.findAll();
        Set<UUID> ids = players.stream().map(Player::getId).collect(Collectors.toSet());
        Map<UUID, UUID> activeMatchIds = searchPlayerUseCase.findActiveMatchIdsByPlayerIds(ids);
        return players.stream()
                .map(p -> PlayerResponse.from(p, !deletePlayerUseCase.hasMatches(p.getId()), activeMatchIds.get(p.getId())))
                .toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlayer(@PathVariable UUID id) {
        deletePlayerUseCase.deletePlayer(id);
    }

    @PutMapping("/{id}")
    public PlayerResponse updatePlayer(@PathVariable UUID id,
                                       @Valid @RequestBody UpdatePlayerRequest request) {
        var command = new UpdatePlayerUseCase.UpdatePlayerCommand(
                id,
                request.firstName(),
                request.lastName(),
                request.gender(),
                request.handedness(),
                request.backhandType(),
                request.ranking(),
                request.nationality(),
                request.birthDate()
        );
        var player = updatePlayerUseCase.updatePlayer(command);
        Map<UUID, UUID> activeMatchIds = searchPlayerUseCase.findActiveMatchIdsByPlayerIds(Set.of(id));
        return PlayerResponse.from(player, !deletePlayerUseCase.hasMatches(id), activeMatchIds.get(id));
    }

    @PatchMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivatePlayer(@PathVariable UUID id) {
        deletePlayerUseCase.deactivatePlayer(id);
    }
}
```

- [ ] **Step 3: Compile to catch errors**

```bash
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :player-module:compileJava :app:compileJava 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add backend/player-module/src/main/java/com/cas/tsas/player/infrastructure/web/dto/response/PlayerResponse.java
git add backend/player-module/src/main/java/com/cas/tsas/player/infrastructure/web/PlayerController.java
git commit -m "feat: add activeMatchId to PlayerResponse and wire bulk lookup in PlayerController"
```

---

### Task 5: Integration test for `activeMatchId` in player list API

**Files:**
- Modify: `backend/app/src/test/java/com/cas/tsas/player/PlayerApiIT.java`

- [ ] **Step 1: Write failing IT**

Add a new `@Nested` class to `PlayerApiIT` after `ListPlayers`:

```java
// =========================================================================
@Nested
class ActiveMatchIndicator {

    @Test
    void activeMatchId_is_set_when_player_is_in_active_match() throws Exception {
        UUID playerId = createPlayer("Max");

        var match = new com.cas.tsas.match.infrastructure.persistence.entity.MatchJpaEntity();
        match.setPlayer1Id(playerId);
        match.setPlayer2Id(UUID.randomUUID());
        match.setSetsToWin(2);
        match.setMatchTiebreak(false);
        match.setShortSet(false);
        match.setStatus(com.cas.tsas.match.domain.model.MatchStatus.IN_PROGRESS);
        var savedMatch = matchRepository.save(match);

        mockMvc.perform(get("/api/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activeMatchId").value(savedMatch.getId().toString()));
    }

    @Test
    void activeMatchId_is_null_when_player_has_no_active_match() throws Exception {
        createPlayer("Max");

        mockMvc.perform(get("/api/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activeMatchId").isEmpty());
    }

    @Test
    void activeMatchId_is_null_when_players_match_is_completed() throws Exception {
        UUID playerId = createPlayer("Max");

        var match = new com.cas.tsas.match.infrastructure.persistence.entity.MatchJpaEntity();
        match.setPlayer1Id(playerId);
        match.setPlayer2Id(UUID.randomUUID());
        match.setSetsToWin(2);
        match.setMatchTiebreak(false);
        match.setShortSet(false);
        match.setStatus(com.cas.tsas.match.domain.model.MatchStatus.COMPLETED);
        matchRepository.save(match);

        mockMvc.perform(get("/api/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activeMatchId").isEmpty());
    }
}
```

- [ ] **Step 2: Run to confirm tests fail**

```bash
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test --tests "com.cas.tsas.player.PlayerApiIT.ActiveMatchIndicator*" 2>&1 | tail -20
```

Expected: FAIL — `activeMatchId` field missing from response.

- [ ] **Step 3: Run all tests to confirm everything compiles and all existing tests still pass**

```bash
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL` with the 3 new IT tests now passing (they should pass after Task 4).

- [ ] **Step 4: Commit**

```bash
git add backend/app/src/test/java/com/cas/tsas/player/PlayerApiIT.java
git commit -m "test: add PlayerApiIT for activeMatchId in player list"
```

---

### Task 6: Frontend — add `activeMatchId` to `Player` model

**Files:**
- Modify: `frontend/src/app/core/models/player.model.ts`

- [ ] **Step 1: Add `activeMatchId` field**

Full file after change:

```typescript
export type Gender = 'MALE' | 'FEMALE' | 'OTHER';
export type Handedness = 'LEFT' | 'RIGHT';
export type BackhandType = 'ONE_HANDED' | 'TWO_HANDED';

export interface Player {
  id: string;
  firstName: string;
  lastName: string;
  gender: Gender;
  handedness: Handedness;
  backhandType: BackhandType;
  ranking?: string;
  nationality?: string;
  birthDate?: string;
  active?: boolean;
  deletable?: boolean;
  activeMatchId?: string | null;
}

export interface CreatePlayerRequest {
  firstName: string;
  lastName: string;
  gender: Gender;
  handedness: Handedness;
  backhandType: BackhandType;
  ranking?: string;
  nationality?: string;
  birthDate?: string;
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/app/core/models/player.model.ts
git commit -m "feat: add activeMatchId to Player model"
```

---

### Task 7: Frontend — add active match indicator column to players table

**Files:**
- Modify: `frontend/src/app/features/players/players.component.ts`

- [ ] **Step 1: Update `PlayersComponent`**

Full file after change:

```typescript
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';

import { ApiService } from '../../core/services/api.service';
import { Player, CreatePlayerRequest } from '../../core/models/player.model';
import { PlayerDialogComponent } from './player-dialog.component';

@Component({
  selector: 'app-players',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatCardModule,
    MatSnackBarModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>Spieler</h1>
        <mat-form-field appearance="outline" class="search-field">
          <mat-icon matPrefix>search</mat-icon>
          <input matInput placeholder="Suchen..." [ngModel]="searchTerm()" (ngModelChange)="searchTerm.set($event)" />
          @if (searchTerm()) {
            <button matSuffix mat-icon-button (click)="searchTerm.set('')">
              <mat-icon>close</mat-icon>
            </button>
          }
        </mat-form-field>
        <button mat-raised-button color="primary" (click)="openCreateDialog()">
          <mat-icon>add</mat-icon>
          Neuer Spieler
        </button>
      </div>

      <mat-card>
        <mat-card-content>
          <table mat-table [dataSource]="filteredPlayers()" matSort (matSortChange)="onSort($event)" class="full-width">
            <ng-container matColumnDef="firstName">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Vorname</th>
              <td mat-cell *matCellDef="let player">{{ player.firstName }}</td>
            </ng-container>

            <ng-container matColumnDef="lastName">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Name</th>
              <td mat-cell *matCellDef="let player">{{ player.lastName }}</td>
            </ng-container>

            <ng-container matColumnDef="ranking">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Ranking</th>
              <td mat-cell *matCellDef="let player">{{ player.ranking ?? '–' }}</td>
            </ng-container>

            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef></th>
              <td mat-cell *matCellDef="let player" class="status-cell">
                @if (player.activeMatchId) {
                  <button mat-icon-button color="primary"
                          matTooltip="Laufendes Match anzeigen"
                          (click)="goToMatch(player.activeMatchId); $event.stopPropagation()">
                    <mat-icon>sports_tennis</mat-icon>
                  </button>
                }
              </td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef></th>
              <td mat-cell *matCellDef="let player" class="actions-cell">
                @if (player.deletable !== false) {
                  <button mat-icon-button color="warn" title="Spieler löschen"
                          (click)="deletePlayer(player); $event.stopPropagation()">
                    <mat-icon>delete</mat-icon>
                  </button>
                } @else {
                  <button mat-icon-button title="Spieler inaktivieren"
                          [class.inactive-btn]="!player.active"
                          (click)="deactivatePlayer(player); $event.stopPropagation()"
                          [disabled]="player.active === false">
                    <mat-icon>person_off</mat-icon>
                  </button>
                }
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"
                [class.inactive-row]="row.active === false"
                (click)="openEditDialog(row)"></tr>
          </table>

          @if (filteredPlayers().length === 0) {
            <div class="empty-state">
              <p>{{ searchTerm() ? 'Keine Spieler gefunden.' : 'Noch keine Spieler angelegt.' }}</p>
            </div>
          }
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-container { padding: 24px; max-width: 900px; margin: 0 auto; }
    .page-header { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
    .page-header h1 { margin: 0; font-size: 28px; }
    .search-field { flex: 1; margin-bottom: -1.25em; }
    .full-width { width: 100%; }
    .empty-state { text-align: center; padding: 48px; color: #666; }
    table { border-radius: 8px; overflow: hidden; }
    .status-cell { width: 48px; }
    .actions-cell { width: 56px; text-align: right; }
    .inactive-row { opacity: 0.45; }
    .inactive-btn { opacity: 0.3; }
    tr.mat-mdc-row { cursor: pointer; }
  `]
})
export class PlayersComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);

  players = signal<Player[]>([]);
  searchTerm = signal('');
  sort = signal<Sort>({ active: 'lastName', direction: 'asc' });
  displayedColumns = ['firstName', 'lastName', 'ranking', 'status', 'actions'];

  filteredPlayers = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    const { active, direction } = this.sort();

    let result = this.players().filter(p =>
      !term ||
      `${p.firstName} ${p.lastName}`.toLowerCase().includes(term) ||
      (p.ranking ?? '').toString().toLowerCase().includes(term)
    );

    if (direction) {
      result = [...result].sort((a, b) => {
        const valA = (a[active as keyof Player] ?? '') as string;
        const valB = (b[active as keyof Player] ?? '') as string;
        return direction === 'asc'
          ? valA.toString().localeCompare(valB.toString())
          : valB.toString().localeCompare(valA.toString());
      });
    }
    return result;
  });

  onSort(sort: Sort) {
    this.sort.set(sort);
  }

  ngOnInit() {
    this.loadPlayers();
  }

  loadPlayers() {
    this.api.getPlayers().subscribe({
      next: (players) => this.players.set(players),
      error: () => this.snackBar.open('Fehler beim Laden der Spieler', 'OK', { duration: 3000 })
    });
  }

  goToMatch(matchId: string) {
    this.router.navigate(['/matches', matchId, 'score']);
  }

  openCreateDialog() {
    const ref = this.dialog.open(PlayerDialogComponent, { width: '500px' });
    ref.afterClosed().subscribe((result: CreatePlayerRequest | undefined) => {
      if (result) {
        this.api.createPlayer(result).subscribe({
          next: () => {
            this.loadPlayers();
            this.snackBar.open('Spieler angelegt', 'OK', { duration: 3000 });
          },
          error: () => this.snackBar.open('Fehler beim Anlegen', 'OK', { duration: 3000 })
        });
      }
    });
  }

  openEditDialog(player: Player) {
    const ref = this.dialog.open(PlayerDialogComponent, { width: '500px', data: player });
    ref.afterClosed().subscribe((result: CreatePlayerRequest | undefined) => {
      if (result) {
        this.api.updatePlayer(player.id, result).subscribe({
          next: () => {
            this.loadPlayers();
            this.snackBar.open('Spieler gespeichert', 'OK', { duration: 3000 });
          },
          error: () => this.snackBar.open('Fehler beim Speichern', 'OK', { duration: 3000 })
        });
      }
    });
  }

  deletePlayer(player: Player) {
    this.api.deletePlayer(player.id).subscribe({
      next: () => {
        this.loadPlayers();
        this.snackBar.open(`${player.firstName} ${player.lastName} gelöscht`, 'OK', { duration: 3000 });
      },
      error: () => this.snackBar.open('Fehler beim Löschen', 'OK', { duration: 3000 })
    });
  }

  deactivatePlayer(player: Player) {
    this.api.deactivatePlayer(player.id).subscribe({
      next: () => {
        this.loadPlayers();
        this.snackBar.open(`${player.firstName} ${player.lastName} inaktiviert`, 'OK', { duration: 3000 });
      },
      error: () => this.snackBar.open('Fehler beim Inaktivieren', 'OK', { duration: 3000 })
    });
  }
}
```

- [ ] **Step 2: Run backend tests and frontend type check**

```bash
cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test 2>&1 | tail -10
cd ../frontend && ng build --configuration development 2>&1 | tail -20
```

Expected: both succeed with no errors.

- [ ] **Step 3: Start dev server and verify manually**

```bash
cd frontend && ng serve
```

Open http://localhost:4200/players. Confirm:
- Player with an IN_PROGRESS match shows a `sports_tennis` icon in the new column
- Hovering the icon shows tooltip "Laufendes Match anzeigen"
- Clicking the icon navigates to `/matches/{id}/score`
- Clicking the icon does NOT open the edit dialog
- Player without active match shows no icon
- Existing sort, search, delete, deactivate still work

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/features/players/players.component.ts
git commit -m "feat: show active match indicator in player list with navigation"
```
