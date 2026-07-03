# TEN-68 Coach-Freetext-Notes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Coach can enter one free-text note per player per match (decoupled from the score), editable during and after the match; the notes feed both AI flows (postmortem analysis and opponent preparation).

**Architecture:** New `match_player_notes` table + aggregate in the match-module (persistence → application service → REST). The ai-module consumes the notes through a match-module application in-port (`GetPlayerNotesUseCase`), exactly like it already consumes `GetMatchUseCase` — no ai→match JPA coupling. Notes ride into the LLM prompts via extra fields on the existing `MatchMetadata` DTO; the structured LLM output is unchanged. A reusable Angular `player-notes` component is embedded in both the live-scoring and the analysis page.

**Tech Stack:** Spring Boot 4.0.6, Java 25 (Gradle multi-module), Flyway, JPA/Hibernate, Testcontainers (PostgreSQL 16), MockMvc; Angular standalone + Signals, Material, ngx-translate, Cypress component tests.

## Global Constraints

- **Build/JDK:** prefix every Gradle command with `JAVA_HOME=/opt/java/jdk-25.0.1`; run from `backend/`. Integration tests (`*IT`) need a container runtime — add `DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true`.
- **Clean Architecture:** ai-module must NOT depend on match-module persistence/JPA. It depends only on match-module **application in-ports** (`GetMatchUseCase`, the new `GetPlayerNotesUseCase`).
- **Owner-scoping:** every note read/write is authorised through `GetMatchUseCase.findById(matchId)`, which is owner-scoped and throws `MatchNotFoundException` (→ 404) for unknown/foreign matches. Mirrors the existing IDOR pattern.
- **Spelling:** Swiss German — always "ss", never "ß". Applies to all new strings, comments, i18n, and docs. (Do not rewrite existing files' spelling.)
- **Note limits:** `note` ≤ 2000 chars (Bean-Validation + DB column). One note per `(match_id, player_id)` (UNIQUE). Blank/empty note → delete (no empty row). Opponent-prep aggregation: newest first, capped at **10**.
- **Coverage gate:** `check` enforces 85% line / 70% branch aggregated. Every task adds tests for its own code.
- **i18n:** four files `frontend/public/i18n/{de,en,it,fr}.json`; German is the lead language. New keys must exist in all four.

## File Structure

**Backend — new**
- `app/src/main/resources/db/migration/V10__create_match_player_notes.sql` — table.
- `match-module/.../domain/model/MatchPlayerNote.java` — aggregate (record).
- `match-module/.../application/port/in/SavePlayerNoteUseCase.java`, `GetPlayerNotesUseCase.java` — in-ports.
- `match-module/.../application/port/out/SavePlayerNotePort.java`, `DeletePlayerNotePort.java`, `LoadPlayerNotesPort.java` — out-ports.
- `match-module/.../application/service/PlayerNoteService.java` — use-case impl.
- `match-module/.../infrastructure/persistence/entity/MatchPlayerNoteJpaEntity.java`
- `match-module/.../infrastructure/persistence/repository/MatchPlayerNoteJpaRepository.java`
- `match-module/.../infrastructure/persistence/mapper/MatchPlayerNoteMapper.java`
- `match-module/.../infrastructure/persistence/repository/MatchPlayerNotePersistenceAdapter.java`
- `match-module/.../infrastructure/web/dto/request/SavePlayerNoteRequest.java`
- `match-module/.../infrastructure/web/dto/response/PlayerNoteResponse.java`

**Backend — modified**
- `match-module/.../infrastructure/web/MatchController.java` — 2 endpoints.
- `ai-module/.../application/dto/MatchMetadata.java` — +note fields.
- `ai-module/.../application/service/MatchAnalysisService.java` — load match notes into metadata.
- `ai-module/.../application/service/OpponentPreparationService.java` — load opponent notes into metadata.
- `ai-module/.../infrastructure/llm/PromptBuilder.java` — render note blocks.

**Backend — tests**
- `app/src/test/java/com/cas/tsas/match/MatchPlayerNotePersistenceAdapterIT.java` (Task 1)
- `app/src/test/java/com/cas/tsas/match/MatchPlayerNotesApiIT.java` (Task 2)
- `ai-module/src/test/java/com/cas/tsas/ai/infrastructure/llm/PromptBuilderCoachNotesTest.java` (Task 3)
- Update `ai-module/.../application/service/MatchAnalysisServiceTest.java` and `MatchAnalysisServiceOwnershipTest.java` (Task 3, constructor change)

**Frontend — new**
- `frontend/src/app/features/matches/notes/player-notes.component.ts` + `.html`
- `frontend/src/app/features/matches/notes/player-notes.component.cy.ts`

**Frontend — modified**
- `frontend/src/app/core/services/api.service.ts` — 2 methods + `PlayerNote` interface.
- `frontend/src/app/features/matches/score/score.component.{ts,html}` — embed panel.
- `frontend/src/app/features/matches/analysis/match-analysis.component.{ts,html}` — embed panel.
- `frontend/public/i18n/{de,en,it,fr}.json` — `playerNotes` namespace.

---

## Task 1: Persistence foundation (match-module)

**Files:**
- Create: `backend/app/src/main/resources/db/migration/V10__create_match_player_notes.sql`
- Create: `backend/match-module/src/main/java/com/cas/tsas/match/domain/model/MatchPlayerNote.java`
- Create: `backend/match-module/src/main/java/com/cas/tsas/match/application/port/out/SavePlayerNotePort.java`
- Create: `backend/match-module/src/main/java/com/cas/tsas/match/application/port/out/DeletePlayerNotePort.java`
- Create: `backend/match-module/src/main/java/com/cas/tsas/match/application/port/out/LoadPlayerNotesPort.java`
- Create: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/entity/MatchPlayerNoteJpaEntity.java`
- Create: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/repository/MatchPlayerNoteJpaRepository.java`
- Create: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/mapper/MatchPlayerNoteMapper.java`
- Create: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/repository/MatchPlayerNotePersistenceAdapter.java`
- Test: `backend/app/src/test/java/com/cas/tsas/match/MatchPlayerNotePersistenceAdapterIT.java`

**Interfaces:**
- Produces:
  - `MatchPlayerNote(UUID id, UUID matchId, UUID playerId, String note, Instant updatedAt)` — domain record.
  - `SavePlayerNotePort.upsert(UUID matchId, UUID playerId, String note) -> MatchPlayerNote`
  - `DeletePlayerNotePort.delete(UUID matchId, UUID playerId) -> void`
  - `LoadPlayerNotesPort.findByMatch(UUID matchId) -> List<MatchPlayerNote>`
  - `LoadPlayerNotesPort.findAboutPlayer(UUID playerId, int limit) -> List<MatchPlayerNote>`
  - `MatchPlayerNotePersistenceAdapter` (`@Component`) implements all three ports.

- [ ] **Step 1: Write the migration**

`backend/app/src/main/resources/db/migration/V10__create_match_player_notes.sql`:

```sql
-- TEN-68: Coach-Freitext-Notizen je Spieler und Match (eine Notiz pro (match, player)).
-- created_by/updated_by sind UUID (Keycloak-sub), konsistent mit V7-Audit-Spalten; nullable,
-- damit Flyway/Jobs ohne Auth-Context NULL hinterlassen dürfen.
CREATE TABLE match_player_notes (
    id          UUID          NOT NULL,
    match_id    UUID          NOT NULL,
    player_id   UUID          NOT NULL,
    note        VARCHAR(2000) NOT NULL,
    created_at  TIMESTAMP,
    created_by  UUID,
    updated_at  TIMESTAMP,
    updated_by  UUID,
    PRIMARY KEY (id),
    CONSTRAINT uq_match_player_notes UNIQUE (match_id, player_id),
    CONSTRAINT fk_match_player_notes_match  FOREIGN KEY (match_id)  REFERENCES matches(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_player_notes_player FOREIGN KEY (player_id) REFERENCES players(id)
);

CREATE INDEX idx_match_player_notes_player ON match_player_notes(player_id);
```

- [ ] **Step 2: Write the domain record**

`MatchPlayerNote.java`:

```java
package com.cas.tsas.match.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Coach free-text note (TEN-68) about one player in the context of one match. Decoupled from the
 * score; exactly one note exists per {@code (matchId, playerId)}. Pure POJO — no framework deps.
 */
public record MatchPlayerNote(UUID id, UUID matchId, UUID playerId, String note, Instant updatedAt) {
}
```

- [ ] **Step 3: Write the out-ports**

`SavePlayerNotePort.java`:

```java
package com.cas.tsas.match.application.port.out;

import com.cas.tsas.match.domain.model.MatchPlayerNote;

import java.util.UUID;

/** Upserts the single note for {@code (matchId, playerId)} (insert or update of the note text). */
public interface SavePlayerNotePort {
    MatchPlayerNote upsert(UUID matchId, UUID playerId, String note);
}
```

`DeletePlayerNotePort.java`:

```java
package com.cas.tsas.match.application.port.out;

import java.util.UUID;

/** Removes the note for {@code (matchId, playerId)}; no-op when none exists. */
public interface DeletePlayerNotePort {
    void delete(UUID matchId, UUID playerId);
}
```

`LoadPlayerNotesPort.java`:

```java
package com.cas.tsas.match.application.port.out;

import com.cas.tsas.match.domain.model.MatchPlayerNote;

import java.util.List;
import java.util.UUID;

public interface LoadPlayerNotesPort {

    /** The 0–2 notes of the given match. */
    List<MatchPlayerNote> findByMatch(UUID matchId);

    /** Notes about one player across matches, newest first, capped at {@code limit}. */
    List<MatchPlayerNote> findAboutPlayer(UUID playerId, int limit);
}
```

- [ ] **Step 4: Write the JPA entity**

`MatchPlayerNoteJpaEntity.java` (audit fields mirror `PointJpaEntity`):

```java
package com.cas.tsas.match.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "match_player_notes")
@EntityListeners(AuditingEntityListener.class)
public class MatchPlayerNoteJpaEntity {

    public MatchPlayerNoteJpaEntity() {}

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "note", nullable = false, length = 2000)
    private String note;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getMatchId() { return matchId; }
    public void setMatchId(UUID matchId) { this.matchId = matchId; }
    public UUID getPlayerId() { return playerId; }
    public void setPlayerId(UUID playerId) { this.playerId = playerId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
}
```

- [ ] **Step 5: Write the repository**

`MatchPlayerNoteJpaRepository.java`:

```java
package com.cas.tsas.match.infrastructure.persistence.repository;

import com.cas.tsas.match.infrastructure.persistence.entity.MatchPlayerNoteJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchPlayerNoteJpaRepository extends JpaRepository<MatchPlayerNoteJpaEntity, UUID> {

    Optional<MatchPlayerNoteJpaEntity> findByMatchIdAndPlayerId(UUID matchId, UUID playerId);

    List<MatchPlayerNoteJpaEntity> findByMatchId(UUID matchId);

    List<MatchPlayerNoteJpaEntity> findByPlayerIdOrderByUpdatedAtDesc(UUID playerId, Pageable pageable);

    void deleteByMatchIdAndPlayerId(UUID matchId, UUID playerId);
}
```

- [ ] **Step 6: Write the mapper**

`MatchPlayerNoteMapper.java`:

```java
package com.cas.tsas.match.infrastructure.persistence.mapper;

import com.cas.tsas.match.domain.model.MatchPlayerNote;
import com.cas.tsas.match.infrastructure.persistence.entity.MatchPlayerNoteJpaEntity;
import org.springframework.stereotype.Component;

/** Maps between {@link MatchPlayerNote} and {@link MatchPlayerNoteJpaEntity}. */
@Component
public class MatchPlayerNoteMapper {

    public MatchPlayerNote toDomain(MatchPlayerNoteJpaEntity e) {
        return new MatchPlayerNote(e.getId(), e.getMatchId(), e.getPlayerId(), e.getNote(), e.getUpdatedAt());
    }
}
```

- [ ] **Step 7: Write the persistence adapter**

`MatchPlayerNotePersistenceAdapter.java` — the `upsert` loads-or-creates the managed entity so the audit `created_*` columns are preserved on update:

```java
package com.cas.tsas.match.infrastructure.persistence.repository;

import com.cas.tsas.match.application.port.out.DeletePlayerNotePort;
import com.cas.tsas.match.application.port.out.LoadPlayerNotesPort;
import com.cas.tsas.match.application.port.out.SavePlayerNotePort;
import com.cas.tsas.match.domain.model.MatchPlayerNote;
import com.cas.tsas.match.infrastructure.persistence.entity.MatchPlayerNoteJpaEntity;
import com.cas.tsas.match.infrastructure.persistence.mapper.MatchPlayerNoteMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Persistence adapter implementing the player-note ports (TEN-68). */
@Component
public class MatchPlayerNotePersistenceAdapter
        implements SavePlayerNotePort, DeletePlayerNotePort, LoadPlayerNotesPort {

    private final MatchPlayerNoteJpaRepository repository;
    private final MatchPlayerNoteMapper mapper;

    public MatchPlayerNotePersistenceAdapter(MatchPlayerNoteJpaRepository repository,
                                             MatchPlayerNoteMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public MatchPlayerNote upsert(UUID matchId, UUID playerId, String note) {
        MatchPlayerNoteJpaEntity entity = repository.findByMatchIdAndPlayerId(matchId, playerId)
                .orElseGet(() -> {
                    MatchPlayerNoteJpaEntity e = new MatchPlayerNoteJpaEntity();
                    e.setMatchId(matchId);
                    e.setPlayerId(playerId);
                    return e;
                });
        entity.setNote(note);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID matchId, UUID playerId) {
        repository.deleteByMatchIdAndPlayerId(matchId, playerId);
    }

    @Override
    public List<MatchPlayerNote> findByMatch(UUID matchId) {
        return repository.findByMatchId(matchId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<MatchPlayerNote> findAboutPlayer(UUID playerId, int limit) {
        return repository.findByPlayerIdOrderByUpdatedAtDesc(playerId, PageRequest.of(0, limit))
                .stream().map(mapper::toDomain).toList();
    }
}
```

- [ ] **Step 8: Write the failing persistence IT**

`backend/app/src/test/java/com/cas/tsas/match/MatchPlayerNotePersistenceAdapterIT.java`. This needs a real match + players (FKs). Reuse the `AbstractIntegrationTest` Testcontainer and persist fixtures via the JPA repositories directly. NOTE: the adapter is owner-agnostic; owner-scoping lives in the service (Task 2).

```java
package com.cas.tsas.match;

import com.cas.tsas.AbstractIntegrationTest;
import com.cas.tsas.match.application.port.out.DeletePlayerNotePort;
import com.cas.tsas.match.application.port.out.LoadPlayerNotesPort;
import com.cas.tsas.match.application.port.out.SavePlayerNotePort;
import com.cas.tsas.match.domain.model.MatchPlayerNote;
import com.cas.tsas.match.infrastructure.persistence.entity.MatchJpaEntity;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchPlayerNoteJpaRepository;
import com.cas.tsas.player.infrastructure.persistence.entity.PlayerJpaEntity;
import com.cas.tsas.player.infrastructure.persistence.repository.PlayerJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MatchPlayerNotePersistenceAdapterIT extends AbstractIntegrationTest {

    @Autowired SavePlayerNotePort savePort;
    @Autowired DeletePlayerNotePort deletePort;
    @Autowired LoadPlayerNotesPort loadPort;
    @Autowired MatchPlayerNoteJpaRepository noteRepository;
    @Autowired MatchJpaRepository matchRepository;
    @Autowired PlayerJpaRepository playerRepository;

    private UUID matchId;
    private UUID player1Id;
    private UUID player2Id;

    @BeforeEach
    void setUp() {
        noteRepository.deleteAll();
        matchRepository.deleteAll();
        playerRepository.deleteAll();
        player1Id = persistPlayer("Max", "Muster");
        player2Id = persistPlayer("Tom", "Gegner");
        matchId = persistMatch(player1Id, player2Id);
    }

    private UUID persistPlayer(String first, String last) {
        PlayerJpaEntity p = new PlayerJpaEntity();
        p.setOwnerId(DEFAULT_USER);
        p.setFirstName(first);
        p.setLastName(last);
        p.setGender(com.cas.tsas.player.domain.model.Gender.MALE);
        p.setHandedness(com.cas.tsas.player.domain.model.Handedness.RIGHT);
        p.setBackhandType(com.cas.tsas.player.domain.model.BackhandType.TWO_HANDED);
        p.setActive(true);
        return playerRepository.save(p).getId();
    }

    private UUID persistMatch(UUID p1, UUID p2) {
        MatchJpaEntity m = new MatchJpaEntity();
        m.setOwnerId(DEFAULT_USER);
        m.setPlayer1Id(p1);
        m.setPlayer2Id(p2);
        m.setSetsToWin(2);
        m.setMatchTiebreak(false);
        m.setShortSet(false);
        m.setStatus(com.cas.tsas.match.domain.model.MatchStatus.IN_PROGRESS);
        return matchRepository.save(m).getId();
    }

    @Test
    void upsert_inserts_then_updates_same_row() {
        MatchPlayerNote first = savePort.upsert(matchId, player1Id, "erste Notiz");
        MatchPlayerNote second = savePort.upsert(matchId, player1Id, "aktualisiert");

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.note()).isEqualTo("aktualisiert");
        assertThat(noteRepository.findByMatchId(matchId)).hasSize(1);
    }

    @Test
    void findByMatch_returns_both_player_notes() {
        savePort.upsert(matchId, player1Id, "p1");
        savePort.upsert(matchId, player2Id, "p2");

        assertThat(loadPort.findByMatch(matchId)).hasSize(2)
                .extracting(MatchPlayerNote::playerId)
                .containsExactlyInAnyOrder(player1Id, player2Id);
    }

    @Test
    void delete_removes_the_note() {
        savePort.upsert(matchId, player1Id, "weg damit");
        deletePort.delete(matchId, player1Id);
        assertThat(loadPort.findByMatch(matchId)).isEmpty();
    }

    @Test
    void findAboutPlayer_returns_newest_first_capped_at_limit() {
        // second match so the opponent (player2Id) has two notes about them
        UUID match2 = persistMatch(persistPlayer("A", "B"), player2Id);
        savePort.upsert(matchId, player2Id, "alt");
        savePort.upsert(match2, player2Id, "neu");

        var about = loadPort.findAboutPlayer(player2Id, 1);
        assertThat(about).hasSize(1);
        assertThat(about.get(0).note()).isEqualTo("neu");
    }
}
```

> If `PlayerJpaEntity` / `MatchJpaEntity` setters differ from the above, adjust to the actual setters (read those two entity files first). The intent — persist owned fixtures, then exercise the ports — must stay.

- [ ] **Step 9: Run the IT to verify it passes**

Run (from `backend/`):
```
JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true \
  ./gradlew :app:test --tests "com.cas.tsas.match.MatchPlayerNotePersistenceAdapterIT"
```
Expected: PASS (4 tests). Flyway applies V10 on the fresh container.

- [ ] **Step 10: Commit**

```bash
git add backend/app/src/main/resources/db/migration/V10__create_match_player_notes.sql \
        backend/match-module/src/main/java/com/cas/tsas/match/domain/model/MatchPlayerNote.java \
        backend/match-module/src/main/java/com/cas/tsas/match/application/port/out/SavePlayerNotePort.java \
        backend/match-module/src/main/java/com/cas/tsas/match/application/port/out/DeletePlayerNotePort.java \
        backend/match-module/src/main/java/com/cas/tsas/match/application/port/out/LoadPlayerNotesPort.java \
        backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/entity/MatchPlayerNoteJpaEntity.java \
        backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/repository/MatchPlayerNoteJpaRepository.java \
        backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/mapper/MatchPlayerNoteMapper.java \
        backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/repository/MatchPlayerNotePersistenceAdapter.java \
        backend/app/src/test/java/com/cas/tsas/match/MatchPlayerNotePersistenceAdapterIT.java
git commit -m "feat(match): persistence for coach player notes (TEN-68)"
```

---

## Task 2: Application service + REST endpoints (match-module)

**Files:**
- Create: `backend/match-module/src/main/java/com/cas/tsas/match/application/port/in/SavePlayerNoteUseCase.java`
- Create: `backend/match-module/src/main/java/com/cas/tsas/match/application/port/in/GetPlayerNotesUseCase.java`
- Create: `backend/match-module/src/main/java/com/cas/tsas/match/application/service/PlayerNoteService.java`
- Create: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/dto/request/SavePlayerNoteRequest.java`
- Create: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/dto/response/PlayerNoteResponse.java`
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/MatchController.java`
- Test: `backend/app/src/test/java/com/cas/tsas/match/MatchPlayerNotesApiIT.java`

**Interfaces:**
- Consumes (Task 1): `SavePlayerNotePort.upsert`, `DeletePlayerNotePort.delete`, `LoadPlayerNotesPort.findByMatch`/`findAboutPlayer`; `GetMatchUseCase.findById` (owner-scoped, throws `MatchNotFoundException`→404); `Match.getPlayer1Id()/getPlayer2Id()`.
- Produces (consumed by Task 3/4):
  - `SavePlayerNoteUseCase.save(UUID matchId, UUID playerId, String note) -> Optional<MatchPlayerNote>` (empty when deleted).
  - `GetPlayerNotesUseCase.forMatch(UUID matchId) -> List<MatchPlayerNote>` (owner-scoped).
  - `GetPlayerNotesUseCase.aboutPlayer(UUID playerId, int limit) -> List<MatchPlayerNote>` (NOT owner-scoped; caller guarantees ownership).
- REST:
  - `GET /api/matches/{id}/notes -> 200 List<PlayerNoteResponse>`
  - `PUT /api/matches/{id}/notes/{playerId}` body `SavePlayerNoteRequest` → `200 PlayerNoteResponse` or `204` (deleted).

- [ ] **Step 1: Write the in-ports**

`SavePlayerNoteUseCase.java`:

```java
package com.cas.tsas.match.application.port.in;

import com.cas.tsas.match.domain.model.MatchPlayerNote;

import java.util.Optional;
import java.util.UUID;

/**
 * Upserts the coach note for {@code (matchId, playerId)} (TEN-68). A blank note deletes it.
 * Owner-scoped via the match: unknown/foreign matches yield {@code MatchNotFoundException} (→404),
 * a {@code playerId} not belonging to the match yields {@code IllegalArgumentException} (→400).
 *
 * @return the saved note, or empty when the note was deleted (blank input).
 */
public interface SavePlayerNoteUseCase {
    Optional<MatchPlayerNote> save(UUID matchId, UUID playerId, String note);
}
```

`GetPlayerNotesUseCase.java`:

```java
package com.cas.tsas.match.application.port.in;

import com.cas.tsas.match.domain.model.MatchPlayerNote;

import java.util.List;
import java.util.UUID;

public interface GetPlayerNotesUseCase {

    /** Owner-scoped: the 0–2 notes of the given match (→404 for unknown/foreign match). */
    List<MatchPlayerNote> forMatch(UUID matchId);

    /**
     * Notes about one player across matches, newest first, capped at {@code limit}.
     * NOT owner-scoped — callers must have verified the player belongs to the current user
     * (opponent preparation does so via {@code findByIdAndOwner}). A player is owner-bound, so
     * its notes are inherently the owner's.
     */
    List<MatchPlayerNote> aboutPlayer(UUID playerId, int limit);
}
```

- [ ] **Step 2: Write the service**

`PlayerNoteService.java`:

```java
package com.cas.tsas.match.application.service;

import com.cas.tsas.match.application.port.in.GetMatchUseCase;
import com.cas.tsas.match.application.port.in.GetPlayerNotesUseCase;
import com.cas.tsas.match.application.port.in.SavePlayerNoteUseCase;
import com.cas.tsas.match.application.port.out.DeletePlayerNotePort;
import com.cas.tsas.match.application.port.out.LoadPlayerNotesPort;
import com.cas.tsas.match.application.port.out.SavePlayerNotePort;
import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchPlayerNote;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Coach player-note use cases (TEN-68). Authorises every match-bound operation through
 * {@link GetMatchUseCase#findById(UUID)} (owner-scoped). {@link #aboutPlayer} is intentionally not
 * owner-scoped — see {@link GetPlayerNotesUseCase}.
 */
@Service
@Transactional
public class PlayerNoteService implements SavePlayerNoteUseCase, GetPlayerNotesUseCase {

    private final GetMatchUseCase getMatchUseCase;
    private final SavePlayerNotePort savePort;
    private final DeletePlayerNotePort deletePort;
    private final LoadPlayerNotesPort loadPort;

    public PlayerNoteService(GetMatchUseCase getMatchUseCase,
                             SavePlayerNotePort savePort,
                             DeletePlayerNotePort deletePort,
                             LoadPlayerNotesPort loadPort) {
        this.getMatchUseCase = getMatchUseCase;
        this.savePort = savePort;
        this.deletePort = deletePort;
        this.loadPort = loadPort;
    }

    @Override
    public Optional<MatchPlayerNote> save(UUID matchId, UUID playerId, String note) {
        Match match = getMatchUseCase.findById(matchId); // owner check → 404
        if (!playerId.equals(match.getPlayer1Id()) && !playerId.equals(match.getPlayer2Id())) {
            throw new IllegalArgumentException(
                    "Player " + playerId + " is not part of match " + matchId);
        }
        if (note == null || note.isBlank()) {
            deletePort.delete(matchId, playerId);
            return Optional.empty();
        }
        return Optional.of(savePort.upsert(matchId, playerId, note));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchPlayerNote> forMatch(UUID matchId) {
        getMatchUseCase.findById(matchId); // owner check → 404
        return loadPort.findByMatch(matchId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchPlayerNote> aboutPlayer(UUID playerId, int limit) {
        return loadPort.findAboutPlayer(playerId, limit);
    }
}
```

- [ ] **Step 3: Write the DTOs**

`SavePlayerNoteRequest.java`:

```java
package com.cas.tsas.match.infrastructure.web.dto.request;

import jakarta.validation.constraints.Size;

public record SavePlayerNoteRequest(@Size(max = 2000) String note) {}
```

`PlayerNoteResponse.java`:

```java
package com.cas.tsas.match.infrastructure.web.dto.response;

import com.cas.tsas.match.domain.model.MatchPlayerNote;

import java.time.Instant;
import java.util.UUID;

public record PlayerNoteResponse(UUID playerId, String note, Instant updatedAt) {
    public static PlayerNoteResponse from(MatchPlayerNote n) {
        return new PlayerNoteResponse(n.playerId(), n.note(), n.updatedAt());
    }
}
```

- [ ] **Step 4: Wire the endpoints into MatchController**

Modify `MatchController.java`:

Add imports (near the existing imports):
```java
import com.cas.tsas.match.application.port.in.GetPlayerNotesUseCase;
import com.cas.tsas.match.application.port.in.SavePlayerNoteUseCase;
import com.cas.tsas.match.infrastructure.web.dto.request.SavePlayerNoteRequest;
import com.cas.tsas.match.infrastructure.web.dto.response.PlayerNoteResponse;
import org.springframework.http.ResponseEntity;
```

Add two fields + constructor params (extend the existing constructor):
```java
    private final SavePlayerNoteUseCase savePlayerNoteUseCase;
    private final GetPlayerNotesUseCase getPlayerNotesUseCase;
```
Append these two parameters to the constructor signature and assign them:
```java
                           SetServingPlayerUseCase setServingPlayerUseCase,
                           SavePlayerNoteUseCase savePlayerNoteUseCase,
                           GetPlayerNotesUseCase getPlayerNotesUseCase) {
        ...
        this.setServingPlayerUseCase = setServingPlayerUseCase;
        this.savePlayerNoteUseCase = savePlayerNoteUseCase;
        this.getPlayerNotesUseCase = getPlayerNotesUseCase;
    }
```

Add the two endpoints (before the closing brace):
```java
    /** TEN-68: the coach's free-text notes for both players of the match (0–2 entries). */
    @GetMapping("/{id}/notes")
    public List<PlayerNoteResponse> getNotes(@PathVariable UUID id) {
        return getPlayerNotesUseCase.forMatch(id).stream()
                .map(PlayerNoteResponse::from)
                .toList();
    }

    /**
     * TEN-68: upserts the note for one player. A blank note deletes it ({@code 204}); a present
     * note returns the saved entry ({@code 200}).
     */
    @PutMapping("/{id}/notes/{playerId}")
    public ResponseEntity<PlayerNoteResponse> saveNote(@PathVariable UUID id,
                                                       @PathVariable UUID playerId,
                                                       @Valid @RequestBody SavePlayerNoteRequest request) {
        return savePlayerNoteUseCase.save(id, playerId, request.note())
                .map(n -> ResponseEntity.ok(PlayerNoteResponse.from(n)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
```

- [ ] **Step 5: Write the failing API IT**

`backend/app/src/test/java/com/cas/tsas/match/MatchPlayerNotesApiIT.java` (mirror `MatchApiIT` helpers):

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

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MatchPlayerNotesApiIT extends AbstractIntegrationTest {

    @Autowired ObjectMapper objectMapper;
    @Autowired MatchJpaRepository matchRepository;
    @Autowired MatchScoreJpaRepository matchScoreRepository;
    @Autowired PointJpaRepository pointRepository;
    @Autowired PlayerJpaRepository playerRepository;

    private UUID p1;
    private UUID p2;
    private UUID matchId;

    @BeforeEach
    void cleanUp() throws Exception {
        pointRepository.deleteAll();
        matchScoreRepository.deleteAll();
        matchRepository.deleteAll();
        playerRepository.deleteAll();
        p1 = createPlayer();
        p2 = createPlayer();
        matchId = createMatch(p1, p2);
    }

    private UUID createPlayer() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "firstName", "Test", "lastName", "Player",
                "gender", "MALE", "handedness", "RIGHT", "backhandType", "TWO_HANDED"));
        String response = mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private UUID createMatch(UUID a, UUID b) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "player1Id", a, "player2Id", b,
                "setsToWin", 2, "matchTiebreak", false, "shortSet", false));
        String response = mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private String noteBody(String note) throws Exception {
        return objectMapper.writeValueAsString(Collections.singletonMap("note", note));
    }

    @Test
    void put_creates_note_and_get_returns_it() throws Exception {
        mockMvc.perform(put("/api/matches/{id}/notes/{pid}", matchId, p1)
                        .contentType(MediaType.APPLICATION_JSON).content(noteBody("2. Aufschlag zu kurz")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value(p1.toString()))
                .andExpect(jsonPath("$.note").value("2. Aufschlag zu kurz"));

        mockMvc.perform(get("/api/matches/{id}/notes", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].note").value("2. Aufschlag zu kurz"));
    }

    @Test
    void put_overwrites_existing_note() throws Exception {
        mockMvc.perform(put("/api/matches/{id}/notes/{pid}", matchId, p1)
                .contentType(MediaType.APPLICATION_JSON).content(noteBody("alt")));
        mockMvc.perform(put("/api/matches/{id}/notes/{pid}", matchId, p1)
                        .contentType(MediaType.APPLICATION_JSON).content(noteBody("neu")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value("neu"));

        mockMvc.perform(get("/api/matches/{id}/notes", matchId))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void blank_note_deletes_and_returns_204() throws Exception {
        mockMvc.perform(put("/api/matches/{id}/notes/{pid}", matchId, p1)
                .contentType(MediaType.APPLICATION_JSON).content(noteBody("etwas")));
        mockMvc.perform(put("/api/matches/{id}/notes/{pid}", matchId, p1)
                        .contentType(MediaType.APPLICATION_JSON).content(noteBody("   ")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/matches/{id}/notes", matchId))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void put_with_player_not_in_match_returns_400() throws Exception {
        UUID stranger = createPlayer();
        mockMvc.perform(put("/api/matches/{id}/notes/{pid}", matchId, stranger)
                        .contentType(MediaType.APPLICATION_JSON).content(noteBody("x")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void put_on_unknown_match_returns_404() throws Exception {
        mockMvc.perform(put("/api/matches/{id}/notes/{pid}", UUID.randomUUID(), p1)
                        .contentType(MediaType.APPLICATION_JSON).content(noteBody("x")))
                .andExpect(status().isNotFound());
    }

    @Test
    void put_note_over_2000_chars_returns_400() throws Exception {
        String tooLong = "a".repeat(2001);
        mockMvc.perform(put("/api/matches/{id}/notes/{pid}", matchId, p1)
                        .contentType(MediaType.APPLICATION_JSON).content(noteBody(tooLong)))
                .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 6: Run the API IT to verify it passes**

```
JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true \
  ./gradlew :app:test --tests "com.cas.tsas.match.MatchPlayerNotesApiIT"
```
Expected: PASS (6 tests). 400 comes from `IllegalArgumentException`/`@Size` via `CommonExceptionHandler`; 404 from `MatchNotFoundException` via `GlobalExceptionHandler`.

- [ ] **Step 7: Commit**

```bash
git add backend/match-module/src/main/java/com/cas/tsas/match/application/port/in/SavePlayerNoteUseCase.java \
        backend/match-module/src/main/java/com/cas/tsas/match/application/port/in/GetPlayerNotesUseCase.java \
        backend/match-module/src/main/java/com/cas/tsas/match/application/service/PlayerNoteService.java \
        backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/dto/request/SavePlayerNoteRequest.java \
        backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/dto/response/PlayerNoteResponse.java \
        backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/MatchController.java \
        backend/app/src/test/java/com/cas/tsas/match/MatchPlayerNotesApiIT.java
git commit -m "feat(match): REST endpoints for coach player notes (TEN-68)"
```

---

## Task 3: AI postmortem integration (ai-module)

**Files:**
- Modify: `backend/ai-module/src/main/java/com/cas/tsas/ai/application/dto/MatchMetadata.java`
- Modify: `backend/ai-module/src/main/java/com/cas/tsas/ai/application/service/MatchAnalysisService.java`
- Modify: `backend/ai-module/src/main/java/com/cas/tsas/ai/infrastructure/llm/PromptBuilder.java`
- Modify (tests): `backend/ai-module/src/test/java/com/cas/tsas/ai/application/service/MatchAnalysisServiceTest.java`, `MatchAnalysisServiceOwnershipTest.java`
- Create (test): `backend/ai-module/src/test/java/com/cas/tsas/ai/infrastructure/llm/PromptBuilderCoachNotesTest.java`

**Interfaces:**
- Consumes (Task 2): `GetPlayerNotesUseCase.forMatch(UUID) -> List<MatchPlayerNote>`; `MatchPlayerNote.playerId()/note()`.
- Produces: extended `MatchMetadata` record with `String player1Note`, `String player2Note`, `List<String> opponentNotes` (consumed in Task 4). A 5-arg backward-compatible constructor is retained.

- [ ] **Step 1: Extend MatchMetadata**

Replace the body of `MatchMetadata.java` with:

```java
package com.cas.tsas.ai.application.dto;

import java.util.List;

/** Contextual match data (players, format, and TEN-68 coach notes) supplied to the LLM. */
public record MatchMetadata(
        PlayerInfo player1,
        PlayerInfo player2,
        int setsToWin,
        boolean matchTiebreak,
        boolean shortSet,
        String player1Note,         // nullable — coach note for player1 (postmortem)
        String player2Note,         // nullable — coach note for player2 (postmortem)
        List<String> opponentNotes  // never null — notes about the opponent (opponent preparation)
) {
    /** Normalises {@code opponentNotes} to an empty list when null. */
    public MatchMetadata {
        if (opponentNotes == null) {
            opponentNotes = List.of();
        }
    }

    /** Backward-compatible constructor without coach notes (existing call sites / tests). */
    public MatchMetadata(PlayerInfo player1, PlayerInfo player2, int setsToWin,
                         boolean matchTiebreak, boolean shortSet) {
        this(player1, player2, setsToWin, matchTiebreak, shortSet, null, null, List.of());
    }

    public record PlayerInfo(
            String fullName,
            String ranking,
            String handedness,
            String backhandType
    ) {}
}
```

- [ ] **Step 2: Inject GetPlayerNotesUseCase into MatchAnalysisService**

In `MatchAnalysisService.java`:

Add imports:
```java
import com.cas.tsas.match.application.port.in.GetPlayerNotesUseCase;
import com.cas.tsas.match.domain.model.MatchPlayerNote;
import java.util.Map;
import java.util.stream.Collectors;
```

Add a field and constructor parameter:
```java
    private final GetPlayerNotesUseCase getPlayerNotesUseCase;
```
Extend the constructor (add the parameter right after `getMatchUseCase` for readability, assign it). Example new signature head:
```java
    public MatchAnalysisService(GetMatchUseCase getMatchUseCase,
                                GetPlayerNotesUseCase getPlayerNotesUseCase,
                                LoadPlayerPort loadPlayerPort,
                                ComputeMatchStatisticsUseCase statisticsUseCase,
                                LlmClientPort llmClient,
                                SaveMatchAnalysisPort savePort,
                                LoadMatchAnalysisPort loadPort,
                                UserLanguagePort userLanguagePort,
                                @Value("${tsas.ai.min-points-for-analysis:10}") int minPointsForAnalysis) {
        this.getMatchUseCase = getMatchUseCase;
        this.getPlayerNotesUseCase = getPlayerNotesUseCase;
        ...
```

Replace `buildMetadata` to load and map the notes:
```java
    private MatchMetadata buildMetadata(Match match) {
        Player p1 = loadPlayerPort.loadPlayer(match.getPlayer1Id())
                .orElseThrow(() -> new PlayerNotFoundException(match.getPlayer1Id()));
        Player p2 = loadPlayerPort.loadPlayer(match.getPlayer2Id())
                .orElseThrow(() -> new PlayerNotFoundException(match.getPlayer2Id()));

        Map<UUID, String> notes = getPlayerNotesUseCase.forMatch(match.getId()).stream()
                .collect(Collectors.toMap(MatchPlayerNote::playerId, MatchPlayerNote::note, (a, b) -> a));

        return new MatchMetadata(
                toInfo(p1), toInfo(p2),
                match.getSetsToWin(), match.isMatchTiebreak(), match.isShortSet(),
                notes.get(match.getPlayer1Id()), notes.get(match.getPlayer2Id()), List.of());
    }
```
(Ensure `java.util.List` is imported — it already is.)

- [ ] **Step 3: Render the note block in PromptBuilder.userPrompt**

In `PromptBuilder.java`, change the tail of `userPrompt` from:
```java
        sb.append("\n").append(prompts.userInstruction());
        return sb.toString();
```
to:
```java
        appendCoachNotes(sb, m.player1Note(), m.player2Note());

        sb.append("\n").append(prompts.userInstruction());
        return sb.toString();
```

Add this private helper (next to `appendPlayer`):
```java
    /**
     * TEN-68: appends the coach's free-text observations when present. Skips the whole block when
     * both notes are absent/blank, so the prompt is byte-for-byte unchanged for matches without notes.
     */
    private void appendCoachNotes(StringBuilder sb, String player1Note, String player2Note) {
        boolean has1 = player1Note != null && !player1Note.isBlank();
        boolean has2 = player2Note != null && !player2Note.isBlank();
        if (!has1 && !has2) {
            return;
        }
        sb.append("\nCoach-Beobachtungen (Freitext, nicht aus der Statistik abgeleitet):\n");
        if (has1) {
            sb.append("- Spieler 1 (eigener Spieler): ").append(player1Note.trim()).append("\n");
        }
        if (has2) {
            sb.append("- Spieler 2 (Gegner): ").append(player2Note.trim()).append("\n");
        }
    }
```

- [ ] **Step 4: Fix the existing service tests (constructor changed)**

In `MatchAnalysisServiceTest.java`:
- Add field + mock:
```java
    private GetPlayerNotesUseCase getPlayerNotesUseCase;
```
imports:
```java
import com.cas.tsas.match.application.port.in.GetPlayerNotesUseCase;
import java.util.List;
```
- In `setUp()`, create the mock and stub an empty default, and pass it to BOTH `new MatchAnalysisService(...)` calls (setUp + `failingService`):
```java
        getPlayerNotesUseCase = Mockito.mock(GetPlayerNotesUseCase.class);
        when(getPlayerNotesUseCase.forMatch(any())).thenReturn(List.of());
        service = new MatchAnalysisService(getMatchUseCase, getPlayerNotesUseCase, loadPlayerPort,
                statisticsUseCase, llm, savePort, loadPort, () -> "de", 10);
```
and the failing-service construction in `generate_persistsFailedAnalysisAndRethrowsOnLlmError`:
```java
        MatchAnalysisService failingService = new MatchAnalysisService(getMatchUseCase,
                getPlayerNotesUseCase, loadPlayerPort, statisticsUseCase, failing, savePort, loadPort,
                () -> "de", 10);
```

- In `MatchAnalysisServiceOwnershipTest.java`: read the file first; apply the same change — mock `GetPlayerNotesUseCase`, stub `forMatch(any()) -> List.of()`, and add it as the 2nd constructor argument at every `new MatchAnalysisService(...)` site.

- [ ] **Step 5: Write the failing PromptBuilder test**

`backend/ai-module/src/test/java/com/cas/tsas/ai/infrastructure/llm/PromptBuilderCoachNotesTest.java`:

```java
package com.cas.tsas.ai.infrastructure.llm;

import com.cas.tsas.ai.application.dto.MatchMetadata;
import com.cas.tsas.ai.infrastructure.config.PromptProperties;
import com.cas.tsas.statistics.domain.model.DirectionDistribution;
import com.cas.tsas.statistics.domain.model.MatchStatistics;
import com.cas.tsas.statistics.domain.model.PlayerStatistics;
import com.cas.tsas.statistics.domain.model.StrokeDistribution;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderCoachNotesTest {

    private final PromptBuilder builder = new PromptBuilder(
            new PromptProperties("sys", "userInstruction", "oppSys", "oppUserInstruction"));

    private MatchMetadata metaWithNotes(String n1, String n2) {
        return new MatchMetadata(
                new MatchMetadata.PlayerInfo("A", "R1", "RIGHT", "TWO_HANDED"),
                new MatchMetadata.PlayerInfo("B", "R2", "LEFT", "ONE_HANDED"),
                2, false, false, n1, n2, List.of());
    }

    private MatchStatistics stats() {
        PlayerStatistics p = new PlayerStatistics(1, 10, 5, 4, 1, 2, 1, 0.6, 0.5,
                1, 3, new StrokeDistribution(Map.of()), new DirectionDistribution(Map.of()));
        return new MatchStatistics(UUID.randomUUID(), p, p, 20, 7, Instant.now());
    }

    @Test
    void includes_both_notes_when_present() {
        String prompt = builder.userPrompt(stats(), metaWithNotes("RH longline schwach", "VH inside-out stark"));
        assertThat(prompt).contains("Coach-Beobachtungen");
        assertThat(prompt).contains("eigener Spieler): RH longline schwach");
        assertThat(prompt).contains("Gegner): VH inside-out stark");
    }

    @Test
    void omits_block_entirely_when_no_notes() {
        String prompt = builder.userPrompt(stats(), metaWithNotes(null, "   "));
        // " " is blank → no player2 line; player1 null → no block at all
        assertThat(prompt).doesNotContain("Coach-Beobachtungen");
    }
}
```

- [ ] **Step 6: Run the affected tests**

```
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :ai-module:test \
  --tests "com.cas.tsas.ai.infrastructure.llm.PromptBuilderCoachNotesTest" \
  --tests "com.cas.tsas.ai.application.service.MatchAnalysisServiceTest" \
  --tests "com.cas.tsas.ai.application.service.MatchAnalysisServiceOwnershipTest"
```
Expected: PASS (the two new PromptBuilder tests + the unchanged service tests still green).

- [ ] **Step 7: Commit**

```bash
git add backend/ai-module/src/main/java/com/cas/tsas/ai/application/dto/MatchMetadata.java \
        backend/ai-module/src/main/java/com/cas/tsas/ai/application/service/MatchAnalysisService.java \
        backend/ai-module/src/main/java/com/cas/tsas/ai/infrastructure/llm/PromptBuilder.java \
        backend/ai-module/src/test/java/com/cas/tsas/ai/application/service/MatchAnalysisServiceTest.java \
        backend/ai-module/src/test/java/com/cas/tsas/ai/application/service/MatchAnalysisServiceOwnershipTest.java \
        backend/ai-module/src/test/java/com/cas/tsas/ai/infrastructure/llm/PromptBuilderCoachNotesTest.java
git commit -m "feat(ai): feed coach notes into postmortem prompt (TEN-68)"
```

---

## Task 4: AI opponent-preparation integration (ai-module)

**Files:**
- Modify: `backend/ai-module/src/main/java/com/cas/tsas/ai/application/service/OpponentPreparationService.java`
- Modify: `backend/ai-module/src/main/java/com/cas/tsas/ai/infrastructure/llm/PromptBuilder.java`
- Create (test): `backend/ai-module/src/test/java/com/cas/tsas/ai/infrastructure/llm/PromptBuilderOpponentNotesTest.java`

**Interfaces:**
- Consumes: `GetPlayerNotesUseCase.aboutPlayer(UUID playerId, int limit) -> List<MatchPlayerNote>`; the `MatchMetadata.opponentNotes` field from Task 3.
- Constant: opponent-notes cap = **10**.

- [ ] **Step 1: Inject GetPlayerNotesUseCase + load opponent notes**

In `OpponentPreparationService.java`:

Add imports:
```java
import com.cas.tsas.match.application.port.in.GetPlayerNotesUseCase;
import com.cas.tsas.match.domain.model.MatchPlayerNote;
```

Add a constant and field:
```java
    /** TEN-68: cap on how many past notes about the opponent are fed into the prompt. */
    private static final int MAX_OPPONENT_NOTES = 10;

    private final GetPlayerNotesUseCase getPlayerNotesUseCase;
```

Add the constructor parameter (after `loadPlayerPort`) and assign it. New head:
```java
    public OpponentPreparationService(LoadPlayerPort loadPlayerPort,
                                      GetPlayerNotesUseCase getPlayerNotesUseCase,
                                      ComputeHeadToHeadStatisticsUseCase headToHeadUseCase,
                                      LlmClientPort llmClient,
                                      CurrentUserProvider currentUserProvider,
                                      UserLanguagePort userLanguagePort) {
        this.loadPlayerPort = loadPlayerPort;
        this.getPlayerNotesUseCase = getPlayerNotesUseCase;
        ...
```

Replace the `MatchMetadata meta = ...` construction (after the H2H check, where ownership of `opponentId` is already verified) with one that includes the opponent notes:
```java
        List<String> opponentNotes = getPlayerNotesUseCase.aboutPlayer(opponentId, MAX_OPPONENT_NOTES)
                .stream().map(MatchPlayerNote::note).toList();

        MatchMetadata meta = new MatchMetadata(toInfo(own), toInfo(opponent),
                /* setsToWin */ 2, /* matchTiebreak */ false, /* shortSet */ false,
                /* player1Note */ null, /* player2Note */ null, opponentNotes);
```
(`java.util.List` is already imported.)

- [ ] **Step 2: Render the opponent-notes block in PromptBuilder**

In `PromptBuilder.java`, change the tail of `opponentPreparationUserPrompt` from:
```java
        sb.append("\n").append(prompts.opponentUserInstruction());
        return sb.toString();
```
to:
```java
        appendOpponentNotes(sb, m.opponentNotes());

        sb.append("\n").append(prompts.opponentUserInstruction());
        return sb.toString();
```

Add the helper:
```java
    /**
     * TEN-68: appends the coach's past free-text observations about this opponent (newest first,
     * already capped by the caller). Skips the block when there are none.
     */
    private void appendOpponentNotes(StringBuilder sb, java.util.List<String> opponentNotes) {
        if (opponentNotes == null || opponentNotes.isEmpty()) {
            return;
        }
        sb.append("\nFrühere Coach-Beobachtungen zu diesem Gegner (neueste zuerst):\n");
        for (String note : opponentNotes) {
            if (note != null && !note.isBlank()) {
                sb.append("- ").append(note.trim()).append("\n");
            }
        }
    }
```

- [ ] **Step 3: Fix the opponent-prep service unit test (constructor changed), if present**

Search for constructions of `OpponentPreparationService` in tests:
```
grep -rln "new OpponentPreparationService(" backend/ai-module/src/test backend/app/src/test
```
For each hit, read the file and add a `GetPlayerNotesUseCase` mock as the 2nd constructor argument, stubbing `when(mock.aboutPlayer(any(), anyInt())).thenReturn(List.of())`. (If there is no such unit test, the `OpponentPreparationControllerIT` covers wiring via Spring — no change needed there because Spring injects the real `PlayerNoteService`.)

- [ ] **Step 4: Write the failing PromptBuilder opponent-notes test**

`backend/ai-module/src/test/java/com/cas/tsas/ai/infrastructure/llm/PromptBuilderOpponentNotesTest.java`:

```java
package com.cas.tsas.ai.infrastructure.llm;

import com.cas.tsas.ai.application.dto.MatchMetadata;
import com.cas.tsas.ai.infrastructure.config.PromptProperties;
import com.cas.tsas.statistics.domain.model.HeadToHeadPlayerStats;
import com.cas.tsas.statistics.domain.model.HeadToHeadStatistics;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderOpponentNotesTest {

    private final PromptBuilder builder = new PromptBuilder(
            new PromptProperties("sys", "userInstruction", "oppSys", "oppUserInstruction"));

    private MatchMetadata meta(List<String> opponentNotes) {
        return new MatchMetadata(
                new MatchMetadata.PlayerInfo("Own", "R1", "RIGHT", "TWO_HANDED"),
                new MatchMetadata.PlayerInfo("Opp", "R2", "LEFT", "ONE_HANDED"),
                2, false, false, null, null, opponentNotes);
    }

    private HeadToHeadStatistics h2h() {
        HeadToHeadPlayerStats s = new HeadToHeadPlayerStats(
                1, 0, 2, 1, 3, 1, 0.6, 0.5, 0.5, 0.4, 0.3, 2, 4, 0.5, 0.5, 5, 0.3, 3, 0.2);
        return new HeadToHeadStatistics(2, s, s);
    }

    @Test
    void includes_opponent_notes_when_present() {
        String prompt = builder.opponentPreparationUserPrompt(h2h(), meta(List.of("RH-Slice unter Druck schwach", "2. Aufschlag chip-bar")));
        assertThat(prompt).contains("Frühere Coach-Beobachtungen zu diesem Gegner");
        assertThat(prompt).contains("- RH-Slice unter Druck schwach");
        assertThat(prompt).contains("- 2. Aufschlag chip-bar");
    }

    @Test
    void omits_block_when_no_notes() {
        String prompt = builder.opponentPreparationUserPrompt(h2h(), meta(List.of()));
        assertThat(prompt).doesNotContain("Frühere Coach-Beobachtungen");
    }
}
```

> **Important:** the `HeadToHeadPlayerStats` / `HeadToHeadStatistics` constructors above are placeholders. Before writing the test, read `statistics-module/.../domain/model/HeadToHeadPlayerStats.java` and `HeadToHeadStatistics.java` and use their real constructor signatures (the existing `PromptBuilder` opponent path and `OpponentPreparationControllerIT` show valid usages to copy). The two assertions are the fixed requirement; the fixture construction must compile against the real records.

- [ ] **Step 5: Run the affected tests**

```
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :ai-module:test \
  --tests "com.cas.tsas.ai.infrastructure.llm.PromptBuilderOpponentNotesTest"
```
Expected: PASS (2 tests). Then run any opponent-prep unit test touched in Step 3.

- [ ] **Step 6: Commit**

```bash
git add backend/ai-module/src/main/java/com/cas/tsas/ai/application/service/OpponentPreparationService.java \
        backend/ai-module/src/main/java/com/cas/tsas/ai/infrastructure/llm/PromptBuilder.java \
        backend/ai-module/src/test/java/com/cas/tsas/ai/infrastructure/llm/PromptBuilderOpponentNotesTest.java
# add any opponent-prep unit test file touched in Step 3
git commit -m "feat(ai): feed past opponent notes into preparation prompt (TEN-68)"
```

---

## Task 5: Frontend — reusable note panel + integration (Angular)

**Files:**
- Create: `frontend/src/app/features/matches/notes/player-notes.component.ts`
- Create: `frontend/src/app/features/matches/notes/player-notes.component.html`
- Create: `frontend/src/app/features/matches/notes/player-notes.component.cy.ts`
- Modify: `frontend/src/app/core/services/api.service.ts`
- Modify: `frontend/src/app/features/matches/score/score.component.ts` + `score.component.html`
- Modify: `frontend/src/app/features/matches/analysis/match-analysis.component.ts` + `match-analysis.component.html`
- Modify: `frontend/public/i18n/de.json`, `en.json`, `it.json`, `fr.json`

**Interfaces:**
- Consumes (backend): `GET /api/matches/{id}/notes -> PlayerNote[]`; `PUT /api/matches/{id}/notes/{playerId}` body `{ note }` → `PlayerNote | 204`; `GET /api/matches/{id}` (for player IDs); `GET /api/players/{id}` (for names).
- Produces: `<app-player-notes [matchId]="…">` standalone component, self-contained (fetches its own data).

- [ ] **Step 1: Add API methods + model**

In `api.service.ts`, add inside the `ApiService` class (e.g. after `recordPoint`):
```ts
  // TEN-68 — Coach free-text notes per player
  getPlayerNotes(matchId: string): Observable<PlayerNote[]> {
    return this.http.get<PlayerNote[]>(`${this.base}/matches/${matchId}/notes`);
  }

  savePlayerNote(matchId: string, playerId: string, note: string): Observable<PlayerNote | void> {
    return this.http.put<PlayerNote | void>(
      `${this.base}/matches/${matchId}/notes/${playerId}`, { note });
  }
```
And add the interface near the bottom (next to `UserPreference`):
```ts
export interface PlayerNote {
  playerId: string;
  note: string;
  updatedAt: string;
}
```

- [ ] **Step 2: Write the reusable component (TS)**

`frontend/src/app/features/matches/notes/player-notes.component.ts`:

```ts
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslatePipe } from '@ngx-translate/core';
import { ApiService } from '../../../core/services/api.service';

interface NoteSlot {
  playerId: string;
  name: string;
  roleKey: 'playerNotes.roleOwn' | 'playerNotes.roleOpponent';
  note: string;
  saving: boolean;
  saved: boolean;
}

/**
 * TEN-68: reusable panel showing one editable free-text note per player of a match.
 * Self-contained — give it the matchId; it resolves the two players and loads/saves notes.
 */
@Component({
  selector: 'app-player-notes',
  standalone: true,
  imports: [FormsModule, MatSnackBarModule, TranslatePipe],
  templateUrl: './player-notes.component.html',
  styles: [`
    .notes { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    @media (max-width: 640px) { .notes { grid-template-columns: 1fr; } }
    .slot { display: flex; flex-direction: column; gap: 4px; }
    .slot-label { font-size: 12px; font-weight: 600; opacity: .8; }
    .slot-role { font-size: 10px; opacity: .55; }
    textarea {
      width: 100%; box-sizing: border-box; min-height: 90px; resize: vertical;
      border-radius: 6px; border: 1px solid rgba(127,127,127,.4);
      padding: 8px; font: inherit; background: rgba(255,255,255,.04); color: inherit;
    }
    .saved-hint { font-size: 10px; color: #4ade80; min-height: 12px; }
  `],
})
export class PlayerNotesComponent implements OnInit {
  @Input({ required: true }) matchId!: string;

  private readonly api = inject(ApiService);
  private readonly snackBar = inject(MatSnackBar);

  slots = signal<NoteSlot[]>([]);

  ngOnInit(): void {
    this.api.getMatch(this.matchId).subscribe({
      next: (m) => {
        const base: NoteSlot[] = [
          { playerId: m.player1Id, name: 'Spieler 1', roleKey: 'playerNotes.roleOwn', note: '', saving: false, saved: false },
          { playerId: m.player2Id, name: 'Spieler 2', roleKey: 'playerNotes.roleOpponent', note: '', saving: false, saved: false },
        ];
        this.slots.set(base);
        this.api.getPlayer(m.player1Id).subscribe(p => this.patch(0, s => s.name = `${p.firstName} ${p.lastName}`));
        this.api.getPlayer(m.player2Id).subscribe(p => this.patch(1, s => s.name = `${p.firstName} ${p.lastName}`));
        this.api.getPlayerNotes(this.matchId).subscribe(notes => {
          for (const n of notes) {
            const idx = this.slots().findIndex(s => s.playerId === n.playerId);
            if (idx >= 0) this.patch(idx, s => s.note = n.note);
          }
        });
      },
      error: () => this.snackBar.open('Match nicht gefunden', 'OK', { duration: 3000 }),
    });
  }

  save(index: number): void {
    const slot = this.slots()[index];
    if (!slot) return;
    this.patch(index, s => { s.saving = true; s.saved = false; });
    this.api.savePlayerNote(this.matchId, slot.playerId, slot.note).subscribe({
      next: () => this.patch(index, s => { s.saving = false; s.saved = true; }),
      error: () => {
        this.patch(index, s => s.saving = false);
        this.snackBar.open('Fehler beim Speichern', 'OK', { duration: 3000 });
      },
    });
  }

  onModel(index: number, value: string): void {
    this.patch(index, s => { s.note = value; s.saved = false; });
  }

  private patch(index: number, mutate: (s: NoteSlot) => void): void {
    this.slots.update(list => list.map((s, i) => {
      if (i !== index) return s;
      const copy = { ...s };
      mutate(copy);
      return copy;
    }));
  }
}
```

- [ ] **Step 3: Write the component template**

`frontend/src/app/features/matches/notes/player-notes.component.html`:

```html
<div class="notes" data-testid="player-notes">
  @for (slot of slots(); track slot.playerId; let i = $index) {
    <div class="slot">
      <span class="slot-label">{{ slot.name }}</span>
      <span class="slot-role">{{ slot.roleKey | translate }}</span>
      <textarea
        [attr.data-testid]="'note-input-' + i"
        [ngModel]="slot.note"
        (ngModelChange)="onModel(i, $event)"
        (blur)="save(i)"
        maxlength="2000"
        [placeholder]="'playerNotes.placeholder' | translate"></textarea>
      <button type="button" [attr.data-testid]="'note-save-' + i" (click)="save(i)" [disabled]="slot.saving">
        {{ 'playerNotes.save' | translate }}
      </button>
      <span class="saved-hint">@if (slot.saved) { {{ 'playerNotes.saved' | translate }} }</span>
    </div>
  }
</div>
```

- [ ] **Step 4: Add i18n keys (all four files)**

In `frontend/public/i18n/de.json`, add a top-level `playerNotes` object:
```json
  "playerNotes": {
    "title": "Coach-Notizen",
    "roleOwn": "eigener Spieler",
    "roleOpponent": "Gegner",
    "placeholder": "Beobachtungen zu diesem Spieler …",
    "save": "Speichern",
    "saved": "gespeichert"
  }
```
`en.json`:
```json
  "playerNotes": {
    "title": "Coach notes",
    "roleOwn": "own player",
    "roleOpponent": "opponent",
    "placeholder": "Observations about this player …",
    "save": "Save",
    "saved": "saved"
  }
```
`it.json`:
```json
  "playerNotes": {
    "title": "Note del coach",
    "roleOwn": "proprio giocatore",
    "roleOpponent": "avversario",
    "placeholder": "Osservazioni su questo giocatore …",
    "save": "Salva",
    "saved": "salvato"
  }
```
`fr.json`:
```json
  "playerNotes": {
    "title": "Notes du coach",
    "roleOwn": "propre joueur",
    "roleOpponent": "adversaire",
    "placeholder": "Observations sur ce joueur …",
    "save": "Enregistrer",
    "saved": "enregistré"
  }
```
(Add a comma after the preceding top-level block in each file; keep valid JSON.)

- [ ] **Step 5: Embed the panel in the live-scoring page**

In `score.component.ts`, add to imports array `PlayerNotesComponent` and `TranslatePipe`:
```ts
import { PlayerNotesComponent } from '../notes/player-notes.component';
import { TranslatePipe } from '@ngx-translate/core';
```
and in `imports: [ ... ]` add `PlayerNotesComponent, TranslatePipe,`.

In `score.component.html`, add a collapsible notes panel near the bottom of the scrollable `.panels-area` (or just after it). Minimal addition (place inside the page, below the observation panels):
```html
<details class="coach-notes">
  <summary>{{ 'playerNotes.title' | translate }}</summary>
  @if (matchData(); as m) {
    <app-player-notes [matchId]="m.id" />
  }
</details>
```
> Read `score.component.html` first to choose the exact insertion point and confirm the match-id accessor (`matchData()?.id`). Keep the dark theme readable — a `details > summary` with light text matches the existing panel styling; add a small style rule if needed.

- [ ] **Step 6: Embed the panel in the analysis page**

In `match-analysis.component.ts`, add to imports:
```ts
import { PlayerNotesComponent } from '../notes/player-notes.component';
```
and add `PlayerNotesComponent` to the `imports: [ ... ]` array.

In `match-analysis.component.html`, add (e.g. above the recommendations or actions), using the existing `matchId` (expose it if not already a field — add a public getter or a signal). The component already stores `private matchId`; make it readable by adding:
```ts
  readonly matchIdValue = computed(() => this.matchId);
```
is not possible (matchId is a plain field). Instead add a public field set in `ngOnInit`:
```ts
  matchId = '';   // change the existing `private matchId = ''` to public
```
Then in the template:
```html
<div class="section">
  <div class="section-label">{{ 'playerNotes.title' | translate }}</div>
  @if (matchId) {
    <app-player-notes [matchId]="matchId" />
  }
</div>
```
> Read `match-analysis.component.html` to choose placement; the `<details>` wrapper is optional here since the page is scrollable.

- [ ] **Step 7: Write the Cypress component test**

`frontend/src/app/features/matches/notes/player-notes.component.cy.ts`:

```ts
import { PlayerNotesComponent } from './player-notes.component';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { testTranslateProviders } from '../../../core/i18n/test-providers';

const MATCH = {
  id: 'match-1', player1Id: 'p1', player2Id: 'p2', status: 'IN_PROGRESS',
  score: {},
};
const PLAYER1 = { id: 'p1', firstName: 'Max', lastName: 'Muster' };
const PLAYER2 = { id: 'p2', firstName: 'Tom', lastName: 'Gegner' };

function mount() {
  cy.mount(PlayerNotesComponent, {
    providers: [provideHttpClient(), provideAnimationsAsync(), ...testTranslateProviders],
    componentProperties: { matchId: 'match-1' },
  });
}

describe('PlayerNotesComponent', () => {
  beforeEach(() => {
    cy.intercept('GET', '**/api/matches/match-1', MATCH).as('getMatch');
    cy.intercept('GET', '**/api/players/p1', PLAYER1).as('getP1');
    cy.intercept('GET', '**/api/players/p2', PLAYER2).as('getP2');
  });

  it('renders two note inputs and prefills existing notes', () => {
    cy.intercept('GET', '**/api/matches/match-1/notes', [
      { playerId: 'p1', note: 'RH longline', updatedAt: '2026-06-26T10:00:00Z' },
    ]).as('getNotes');
    mount();
    cy.wait(['@getMatch', '@getNotes']);
    cy.get('[data-testid="note-input-0"]').should('have.value', 'RH longline');
    cy.get('[data-testid="note-input-1"]').should('have.value', '');
    cy.contains('Max Muster').should('exist');
    cy.contains('Tom Gegner').should('exist');
  });

  it('PUTs the note to the correct player on save', () => {
    cy.intercept('GET', '**/api/matches/match-1/notes', []).as('getNotes');
    cy.intercept('PUT', '**/api/matches/match-1/notes/p2', {
      playerId: 'p2', note: 'Slice schwach', updatedAt: '2026-06-26T11:00:00Z',
    }).as('putNote');
    mount();
    cy.wait(['@getMatch', '@getNotes']);
    cy.get('[data-testid="note-input-1"]').type('Slice schwach');
    cy.get('[data-testid="note-save-1"]').click();
    cy.wait('@putNote').its('request.body').should('deep.equal', { note: 'Slice schwach' });
  });
});
```
> If `MatchWithScore` requires more `score` fields to type-check the intercept body at runtime, Cypress does not type-check JSON — the partial object is fine for the HTTP stub.

- [ ] **Step 8: Run the frontend tests**

Cypress component test (from `frontend/`):
```
npx cypress run --component --spec "src/app/features/matches/notes/player-notes.component.cy.ts"
```
Then re-run the two host specs to confirm the embeds did not break them:
```
npx cypress run --component --spec "src/app/features/matches/analysis/match-analysis.component.cy.ts,src/app/features/matches/score/score.component.cy.ts"
```
Expected: all green. If a host spec now needs to stub `GET /api/matches/*/notes` (because the embedded panel fires it), add a permissive `cy.intercept('GET', '**/api/matches/*/notes', []).as('notes')` to that spec's `beforeEach` — read the spec first and add only if a test fails on an unhandled request.

- [ ] **Step 9: Commit**

```bash
git add frontend/src/app/features/matches/notes/ \
        frontend/src/app/core/services/api.service.ts \
        frontend/src/app/features/matches/score/score.component.ts \
        frontend/src/app/features/matches/score/score.component.html \
        frontend/src/app/features/matches/analysis/match-analysis.component.ts \
        frontend/src/app/features/matches/analysis/match-analysis.component.html \
        frontend/public/i18n/de.json frontend/public/i18n/en.json \
        frontend/public/i18n/it.json frontend/public/i18n/fr.json
git commit -m "feat(frontend): coach player-notes panel in scoring + analysis (TEN-68)"
```

---

## Final verification (after all tasks)

- [ ] **Backend full check** (compile + all tests + coverage gate):
```
JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true \
  ./gradlew check
```
Expected: BUILD SUCCESSFUL, coverage gate ≥ 85% line / 70% branch.

- [ ] **Frontend full check**:
```
cd frontend && npm test -- --watch=false && npm run cypress:run
```
Expected: unit specs + all Cypress component specs green.

- [ ] **DoD:** both suites green (Linear ticket DoD: "Alle Tests wieder grün").

## Notes on cross-task consistency

- The `MatchAnalysisService` constructor gains `GetPlayerNotesUseCase` as its **2nd** parameter (Task 3). The `OpponentPreparationService` constructor gains it as its **2nd** parameter (Task 4). Spring injects `PlayerNoteService` (Task 2) for both — no manual wiring/config needed (component scanning already covers `com.cas.tsas`).
- `MatchMetadata` is constructed in three production sites and one adapter test. The 5-arg backward-compatible constructor (Task 3) keeps `OpenAiLlmAdapterTest` compiling unchanged; the two services use the 8-arg form.
- Opponent-prep owner safety: `aboutPlayer` is not owner-scoped by design; `OpponentPreparationService` already verifies `opponentId` ownership via `findByIdAndOwner` before calling it. Do not add a second owner filter — keep the query trivial.
- 400 vs 404: "player not in match" → `IllegalArgumentException` (→400 via `CommonExceptionHandler`); unknown/foreign match → `MatchNotFoundException` (→404 via `GlobalExceptionHandler`). No new exception classes.
