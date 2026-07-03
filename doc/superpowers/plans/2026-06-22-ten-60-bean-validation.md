# TEN-60 Bean-Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Alle Request-DTOs lehnen ungültige Eingaben mit HTTP 400 ab (statt 500). String-Felder haben definierte Längenobergrenzen; `RecordPointRequest` typisiert seine Enum-Felder direkt.

**Architecture:** Reine DTO + Controller-Änderung. Keine neuen Klassen, keine Architektur-Sprünge. Jakarta-Bean-Validation-Annotations (`@Size`) ersetzen die fehlenden Limits; Jackson übernimmt die Enum-Deserialisierung (invalide Werte → `HttpMessageNotReadableException` → 400 by default). Integration-Tests in `:app` decken die 400er-Pfade ab.

**Tech Stack:** Spring Boot 4.0.6 (Java 25), Jakarta Bean Validation, Jackson, MockMvc + Spring Security Test (`jwt()` post-processor via `JwtTestSupport` aus TEN-55).

**Spec:** `docs/superpowers/specs/2026-06-22-ten-60-bean-validation-design.md`

---

## File Structure

### Geänderte Dateien

| Pfad | Änderung |
|---|---|
| `player-module/src/main/java/com/cas/tsas/player/infrastructure/web/dto/request/CreatePlayerRequest.java` | `@Size`-Limits auf firstName/lastName/ranking/nationality |
| `player-module/src/main/java/com/cas/tsas/player/infrastructure/web/dto/request/UpdatePlayerRequest.java` | analog |
| `match-module/src/main/java/com/cas/tsas/match/infrastructure/web/dto/request/RecordPointRequest.java` | Enum-Typen statt String; `@Size` statt `@Length` |
| `match-module/src/main/java/com/cas/tsas/match/infrastructure/web/MatchController.java` | `recordPoint`-Mapping vereinfacht (kein `Enum.valueOf` mehr) |

### Neue Dateien

| Pfad | Verantwortung |
|---|---|
| `app/src/test/java/com/cas/tsas/validation/PlayerValidationIT.java` | 4 Integration-Tests: zu lange Felder → 400, valider Request → 201 |
| `app/src/test/java/com/cas/tsas/validation/RecordPointValidationIT.java` | 4 Integration-Tests: invalid enum / zu lang / out-of-range → 400, valid → 201 |

`auth-module`, `common-module`, `statistics-module`, `ai-module`: keine Änderungen.

---

## Konstanten (Referenz für alle Tasks)

- Build-Command: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew <task>`
- IT-Command: `cd backend && env -u OPENAI_API_KEY JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew <task>`
- `JwtTestSupport.withUser(UUID, Role...)` aus `auth-module` testFixtures liefert MockMvc-Auth (TEN-55).
- `DEFAULT_USER` in `AbstractIntegrationTest` ist der Default-JWT-`sub`.

---

## Phase 1 — Player-DTOs

### Task 1: `@Size`-Limits auf `CreatePlayerRequest` und `UpdatePlayerRequest`

**Files:**
- Modify: `backend/player-module/src/main/java/com/cas/tsas/player/infrastructure/web/dto/request/CreatePlayerRequest.java`
- Modify: `backend/player-module/src/main/java/com/cas/tsas/player/infrastructure/web/dto/request/UpdatePlayerRequest.java`

- [ ] **Step 1: Replace `CreatePlayerRequest` content**

```java
package com.cas.tsas.player.infrastructure.web.dto.request;

import com.cas.tsas.player.domain.model.BackhandType;
import com.cas.tsas.player.domain.model.Gender;
import com.cas.tsas.player.domain.model.Handedness;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreatePlayerRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotNull Gender gender,
        @NotNull Handedness handedness,
        @NotNull BackhandType backhandType,
        @Size(max = 50) String ranking,
        @Size(max = 64) String nationality,
        LocalDate birthDate
) {}
```

- [ ] **Step 2: Replace `UpdatePlayerRequest` content (identical Constraints)**

```java
package com.cas.tsas.player.infrastructure.web.dto.request;

import com.cas.tsas.player.domain.model.BackhandType;
import com.cas.tsas.player.domain.model.Gender;
import com.cas.tsas.player.domain.model.Handedness;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdatePlayerRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotNull Gender gender,
        @NotNull Handedness handedness,
        @NotNull BackhandType backhandType,
        @Size(max = 50) String ranking,
        @Size(max = 64) String nationality,
        LocalDate birthDate
) {}
```

- [ ] **Step 3: Compile + existing tests pass**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :player-module:test`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit (zusammen mit Task 2 — IT)**

> Halt: nicht einzeln committen. Stage die Änderungen und committe erst nach Task 2.

---

### Task 2: `PlayerValidationIT` — IT für Player-Validation

**Files:**
- Create: `backend/app/src/test/java/com/cas/tsas/validation/PlayerValidationIT.java`

- [ ] **Step 1: Write the IT**

```java
package com.cas.tsas.validation;

import com.cas.tsas.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlayerValidationIT extends AbstractIntegrationTest {

    @Autowired ObjectMapper json;

    private static Map<String, Object> validPlayerBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", "Alice");
        body.put("lastName",  "Anders");
        body.put("gender",    "FEMALE");
        body.put("handedness","RIGHT");
        body.put("backhandType","TWO_HANDED");
        return body;
    }

    @Test
    void create_rejects_too_long_first_name_with_400() throws Exception {
        Map<String, Object> body = validPlayerBody();
        body.put("firstName", "X".repeat(101));

        mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_rejects_too_long_ranking_with_400() throws Exception {
        Map<String, Object> body = validPlayerBody();
        body.put("ranking", "R".repeat(51));

        mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_rejects_too_long_nationality_with_400() throws Exception {
        Map<String, Object> body = validPlayerBody();
        body.put("nationality", "N".repeat(65));

        mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_rejects_too_long_last_name_with_400() throws Exception {
        // create as DEFAULT_USER
        MvcResult res = mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(validPlayerBody())))
                .andExpect(status().isCreated())
                .andReturn();
        UUID id = UUID.fromString(json.readTree(res.getResponse().getContentAsString()).get("id").asText());

        Map<String, Object> body = validPlayerBody();
        body.put("lastName", "Y".repeat(101));

        mockMvc.perform(put("/api/players/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_accepts_max_length_values() throws Exception {
        Map<String, Object> body = validPlayerBody();
        body.put("firstName", "A".repeat(100));
        body.put("lastName",  "B".repeat(100));
        body.put("ranking",   "R".repeat(50));
        body.put("nationality","N".repeat(64));

        MvcResult res = mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(res.getResponse().getStatus()).isEqualTo(201);
    }
}
```

> Notes:
> - `AbstractIntegrationTest` setzt einen Default-JWT (`DEFAULT_USER`, COACH) via `defaultRequest(...)` aus TEN-55, daher ist hier kein `JwtTestSupport.withUser(...)` nötig.
> - `validPlayerBody()` enthält bewusst die `@NotNull` Enum-Felder (Gender, Handedness, BackhandType), damit die 400er ausschliesslich aus dem Grössenlimit kommen.

- [ ] **Step 2: Run the IT**

Run: `cd backend && env -u OPENAI_API_KEY JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew :app:test --tests "com.cas.tsas.validation.PlayerValidationIT"`
Expected: 5 tests pass.

- [ ] **Step 3: Commit Tasks 1 + 2 together**

```bash
git add backend/player-module/src/main/java/com/cas/tsas/player/infrastructure/web/dto/request/CreatePlayerRequest.java \
        backend/player-module/src/main/java/com/cas/tsas/player/infrastructure/web/dto/request/UpdatePlayerRequest.java \
        backend/app/src/test/java/com/cas/tsas/validation/PlayerValidationIT.java
git commit -m "feat(player): @Size limits on DTO string fields + IT (TEN-60)"
```

---

## Phase 2 — Match-RecordPoint-DTO

### Task 3: `RecordPointRequest` mit typisierten Enums

**Files:**
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/dto/request/RecordPointRequest.java`
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/MatchController.java`

- [ ] **Step 1: Replace `RecordPointRequest` content**

```java
package com.cas.tsas.match.infrastructure.web.dto.request;

import com.cas.tsas.match.domain.model.Direction;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.match.domain.model.StrokeType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecordPointRequest(
        @NotNull @Min(1) @Max(2) Integer winner,
        PointType pointType,
        StrokeType strokeType,
        Direction direction,
        @Size(max = 500) String remark,
        @Min(1) @Max(2) Integer serveAttempt
) {}
```

- [ ] **Step 2: Update `MatchController.recordPoint`**

Open `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/MatchController.java`, find the `recordPoint` method:

Replace the existing command construction:

```java
var command = new RecordPointUseCase.RecordPointCommand(
        id,
        request.winner(),
        request.pointType() != null ? PointType.valueOf(request.pointType()) : null,
        request.strokeType() != null ? StrokeType.valueOf(request.strokeType()) : null,
        request.direction() != null ? Direction.valueOf(request.direction()) : null,
        request.remark(),
        request.serveAttempt()
);
```

With:

```java
var command = new RecordPointUseCase.RecordPointCommand(
        id,
        request.winner(),
        request.pointType(),
        request.strokeType(),
        request.direction(),
        request.remark(),
        request.serveAttempt()
);
```

The `PointType`, `StrokeType`, `Direction` imports in `MatchController.java` are now unused — remove them:
- `import com.cas.tsas.match.domain.model.Direction;`
- `import com.cas.tsas.match.domain.model.PointType;`
- `import com.cas.tsas.match.domain.model.StrokeType;`

Verify by re-reading the file: those types should only appear inside the (now untouched) command field declarations, not as identifiers in the controller logic.

- [ ] **Step 3: Compile + existing tests pass**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :match-module:test`
Expected: BUILD SUCCESSFUL.

Run wider sanity:
`cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew compileJava compileTestJava`
Expected: BUILD SUCCESSFUL across all modules.

- [ ] **Step 4: Commit (zusammen mit Task 4 — IT)**

> Halt: stage, aber committe erst nach Task 4.

---

### Task 4: `RecordPointValidationIT`

**Files:**
- Create: `backend/app/src/test/java/com/cas/tsas/validation/RecordPointValidationIT.java`

- [ ] **Step 1: Write the IT**

```java
package com.cas.tsas.validation;

import com.cas.tsas.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecordPointValidationIT extends AbstractIntegrationTest {

    @Autowired ObjectMapper json;

    private UUID matchId;

    private Map<String, Object> validPlayerBody(String firstName) {
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", firstName);
        body.put("lastName",  "X");
        body.put("gender",    "FEMALE");
        body.put("handedness","RIGHT");
        body.put("backhandType","TWO_HANDED");
        return body;
    }

    @BeforeEach
    void createMatchFixture() throws Exception {
        UUID p1 = createPlayer("Alice");
        UUID p2 = createPlayer("Bob");

        Map<String, Object> matchBody = new HashMap<>();
        matchBody.put("player1Id", p1);
        matchBody.put("player2Id", p2);
        matchBody.put("setsToWin", 2);
        matchBody.put("matchTiebreak", false);
        matchBody.put("shortSet", false);

        MvcResult res = mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(matchBody)))
                .andExpect(status().isCreated())
                .andReturn();
        matchId = UUID.fromString(json.readTree(res.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID createPlayer(String firstName) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(validPlayerBody(firstName))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json.readTree(res.getResponse().getContentAsString()).get("id").asText());
    }

    @Test
    void rejects_invalid_point_type_with_400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("winner", 1);
        body.put("pointType", "NOT_A_REAL_POINT_TYPE");

        mockMvc.perform(post("/api/matches/" + matchId + "/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejects_too_long_remark_with_400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("winner", 1);
        body.put("remark", "X".repeat(501));

        mockMvc.perform(post("/api/matches/" + matchId + "/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejects_winner_out_of_range_with_400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("winner", 3);

        mockMvc.perform(post("/api/matches/" + matchId + "/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejects_missing_winner_with_400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        // winner is @NotNull but omitted

        mockMvc.perform(post("/api/matches/" + matchId + "/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void accepts_valid_full_request_with_201() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("winner", 1);
        body.put("pointType", "WINNER");
        body.put("strokeType", "FOREHAND");
        body.put("direction", "LONG");
        body.put("remark", "A".repeat(500));
        body.put("serveAttempt", 1);

        mockMvc.perform(post("/api/matches/" + matchId + "/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    @Test
    void accepts_minimal_request_with_null_optionals() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("winner", 1);

        mockMvc.perform(post("/api/matches/" + matchId + "/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }
}
```

> Notes:
> - `pointType=WINNER`, `strokeType=FOREHAND`, `direction=LONG` sind gültige Werte aus `PointType`, `StrokeType`, `Direction`. Falls Enum-Konstanten anders heissen, beim Implementieren anpassen — verifizieren mit:
>   ```
>   grep -E "^\s+[A-Z_]+," backend/match-module/src/main/java/com/cas/tsas/match/domain/model/PointType.java backend/match-module/src/main/java/com/cas/tsas/match/domain/model/StrokeType.java backend/match-module/src/main/java/com/cas/tsas/match/domain/model/Direction.java
>   ```

- [ ] **Step 2: Run the IT**

Run: `cd backend && env -u OPENAI_API_KEY JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew :app:test --tests "com.cas.tsas.validation.RecordPointValidationIT"`
Expected: 6 tests pass.

- [ ] **Step 3: Run full `:app:test` (regression check)**

Run: `cd backend && env -u OPENAI_API_KEY JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew :app:test`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit Tasks 3 + 4 together**

```bash
git add backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/dto/request/RecordPointRequest.java \
        backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/MatchController.java \
        backend/app/src/test/java/com/cas/tsas/validation/RecordPointValidationIT.java
git commit -m "feat(match): typed enums + @Size on RecordPointRequest + IT (TEN-60)"
```

---

## Phase 3 — Final Verification

### Task 5: Full check + PR

- [ ] **Step 1: Full backend `check`**

Run: `cd backend && env -u OPENAI_API_KEY JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew check`
Expected: BUILD SUCCESSFUL including JaCoCo gate (85 % line / 70 % branch).

- [ ] **Step 2: Spec §10 acceptance walkthrough**

Manually verify against Spec §10:
- [ ] `@Size`-Limits auf `firstName`/`lastName`/`ranking`/`nationality`/`remark` → Task 1, Task 3
- [ ] `RecordPointRequest` typisiert Enum-Felder; `MatchController.recordPoint` ohne `Enum.valueOf` → Task 3
- [ ] `@Length(max=500)` durch `@Size(max=500)` ersetzt → Task 3
- [ ] Walkover-Whitelist (war schon ✓ in `EndMatchWalkoverRequest` aus früherem Stand) — Spec dokumentiert das, kein Code-Task.
- [ ] Integration-Tests belegen zu lang / invalid enum / out-of-range → 400 → Tasks 2, 4
- [ ] JaCoCo-Gate hält → Step 1

- [ ] **Step 3: Open PR via `finishing-a-development-branch` skill**

---

## Self-Review (durchgeführt)

- ✅ **Spec coverage:** alle Akzeptanzkriterien aus §10 haben einen Task. Walkover-Whitelist ist im Bestand und wird in Spec §10 als „bereits ✓" markiert; explizit als zu-verifizierender Bullet im Acceptance-Walkthrough (Task 5 Step 2) gelistet.
- ✅ **Keine Platzhalter:** alle DTO-Bodies, IT-Bodies und Controller-Mapping sind ausgeschrieben. Kein „similar to". Kein „add error handling".
- ✅ **Type-Consistency:** `PointType` / `StrokeType` / `Direction` als typed fields in DTO + Command (existieren im Domain seit langem, keine Renames). `@Size` durchgängig (Jakarta). `validPlayerBody()` Helper liefert dieselben Felder in beiden IT-Klassen.
- ⚠️ **Risiko (aus Spec R1):** Frontend sendet möglicherweise `pointType`/`strokeType`/`direction` als String. Wenn die Strings exakte Enum-Namen sind, akzeptiert Jackson sie unverändert. Wenn die Strings andere Schreibweisen verwenden, würde der bisher silent funktionierende `valueOf` jetzt 400 liefern. Manueller Smoke-Test nach Merge ist in der PR-Description vermerkt.

---

## Execution

Plan complete and saved to `docs/superpowers/plans/2026-06-22-ten-60-bean-validation.md`. Two execution options:

1. **Subagent-Driven (recommended)** — Ich dispatche pro Task einen frischen Subagenten, reviewe zwischen Tasks, schnelle Iteration.
2. **Inline Execution** — Tasks in dieser Session ausführen via `superpowers:executing-plans`, Batch mit Checkpoints.
