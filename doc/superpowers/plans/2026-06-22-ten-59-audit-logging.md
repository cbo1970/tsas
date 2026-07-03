# TEN-59 Audit-Logging Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Audit-Felder (`created_at`, `created_by`, `updated_at`, `updated_by`) auf den JPA-Entities Player/Match/Point automatisch über Spring Data `AuditingEntityListener` befüllen, plus MDC `correlationId` pro HTTP-Request für Log-Korrelation.

**Architecture:** `JpaAuditingConfig` aktiviert `@EnableJpaAuditing`. `CurrentUserAuditor` (`AuditorAware<UUID>`) liest `sub` aus dem bestehenden `CurrentUserProvider` (oder leeres `Optional` bei fehlendem Auth-Context). Die drei JPA-Entities bekommen `@EntityListeners(AuditingEntityListener.class)` plus vier Audit-Felder. Migration V7 fügt die Spalten hinzu mit Backfill aus `owner_id`. Ein `CorrelationIdFilter` im `common-module` setzt MDC pro Request und echoed via Response-Header.

**Tech Stack:** Spring Boot 4.0.6 (Java 25), Spring Data JPA (`@EnableJpaAuditing`), Hibernate, Flyway, SLF4J/Logback (MDC), JUnit 5 + AssertJ, Testcontainers Postgres.

**Spec:** `docs/superpowers/specs/2026-06-22-ten-59-audit-logging-design.md`

---

## File Structure

### Neue Dateien

| Pfad | Verantwortung |
|---|---|
| `auth-module/src/main/java/com/cas/tsas/auth/infrastructure/persistence/JpaAuditingConfig.java` | `@Configuration` mit `@EnableJpaAuditing(auditorAwareRef = "currentUserAuditor")` |
| `auth-module/src/main/java/com/cas/tsas/auth/infrastructure/persistence/CurrentUserAuditor.java` | `AuditorAware<UUID>` Bean, delegiert an `CurrentUserProvider` |
| `auth-module/src/test/java/com/cas/tsas/auth/infrastructure/persistence/CurrentUserAuditorTest.java` | Unit-Test (Provider liefert / wirft IllegalState) |
| `common-module/src/main/java/com/cas/tsas/common/web/CorrelationIdFilter.java` | `OncePerRequestFilter`, MDC + Echo-Header |
| `common-module/src/main/java/com/cas/tsas/common/web/CorrelationIdConfig.java` | `FilterRegistrationBean` für den Filter |
| `common-module/src/test/java/com/cas/tsas/common/web/CorrelationIdFilterTest.java` | Unit-Test (Header durchgereicht, generiert, MDC bereinigt) |
| `app/src/main/resources/db/migration/V7__add_audit_columns.sql` | Schema-Migration mit Backfill |
| `app/src/test/java/com/cas/tsas/security/AuditingIT.java` | E2E-IT (Create/Update/Admin-Override mit DB-Assertions) |

### Geänderte Dateien

| Pfad | Änderung |
|---|---|
| `player-module/src/main/java/com/cas/tsas/player/infrastructure/persistence/entity/PlayerJpaEntity.java` | `@EntityListeners` + 4 Audit-Felder |
| `match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/entity/MatchJpaEntity.java` | `@EntityListeners` + 4 Audit-Felder |
| `match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/entity/PointJpaEntity.java` | `@EntityListeners` + 4 Audit-Felder |
| `app/src/main/resources/application-local.yml` | Logback-Pattern erweitert um `[%X{correlationId:-}]` |

`PlayerMapper`/`MatchMapper`/`PointMapper` werden **nicht** angefasst — Audit-Felder existieren nur auf JPA-Layer.

---

## Konstanten / Werte (Referenz für alle Tasks)

- Build-Command: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew <task>`
- IT-Command: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew <task>`
- MDC-Key: `correlationId`
- Header-Name: `X-Correlation-Id`
- Auditor-Bean-Name: `currentUserAuditor`

---

## Phase 1 — AuditorAware Foundation

### Task 1: `CurrentUserAuditor` + Test

**Files:**
- Create: `backend/auth-module/src/main/java/com/cas/tsas/auth/infrastructure/persistence/CurrentUserAuditor.java`
- Create: `backend/auth-module/src/test/java/com/cas/tsas/auth/infrastructure/persistence/CurrentUserAuditorTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.cas.tsas.auth.infrastructure.persistence;

import com.cas.tsas.auth.application.port.in.CurrentUserProvider;
import com.cas.tsas.auth.domain.CurrentUser;
import com.cas.tsas.auth.domain.Role;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserAuditorTest {

    private final CurrentUserProvider provider = mock(CurrentUserProvider.class);
    private final CurrentUserAuditor auditor = new CurrentUserAuditor(provider);

    @Test
    void returns_current_user_id_when_present() {
        UUID userId = UUID.randomUUID();
        when(provider.get()).thenReturn(new CurrentUser(userId, Set.of(Role.COACH)));

        Optional<UUID> result = auditor.getCurrentAuditor();

        assertThat(result).contains(userId);
    }

    @Test
    void returns_empty_when_no_auth_context() {
        when(provider.get()).thenThrow(new IllegalStateException("No authenticated JWT"));

        Optional<UUID> result = auditor.getCurrentAuditor();

        assertThat(result).isEmpty();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :auth-module:test --tests "com.cas.tsas.auth.infrastructure.persistence.CurrentUserAuditorTest"`
Expected: compile error (`CurrentUserAuditor` not defined).

- [ ] **Step 3: Implement `CurrentUserAuditor`**

```java
package com.cas.tsas.auth.infrastructure.persistence;

import com.cas.tsas.auth.application.port.in.CurrentUserProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Liefert für JPA-Auditing die UUID des aktuell authentifizierten Nutzers. Wenn kein Auth-Kontext
 * vorhanden ist (z. B. Flyway-Migration, scheduled Job, DataJpaTest ohne MockMvc), wird ein leeres
 * Optional zurückgegeben — Hibernate lässt die Audit-Spalten dann auf NULL.
 */
@Component("currentUserAuditor")
public class CurrentUserAuditor implements AuditorAware<UUID> {

    private final CurrentUserProvider currentUserProvider;

    public CurrentUserAuditor(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public Optional<UUID> getCurrentAuditor() {
        try {
            return Optional.of(currentUserProvider.get().id());
        } catch (IllegalStateException e) {
            return Optional.empty();
        }
    }
}
```

- [ ] **Step 4: Run test — expect PASS**

Run: same as Step 2. Expected: 2 tests pass.

- [ ] **Step 5: Commit**

```bash
git add backend/auth-module/src/main/java/com/cas/tsas/auth/infrastructure/persistence/CurrentUserAuditor.java \
        backend/auth-module/src/test/java/com/cas/tsas/auth/infrastructure/persistence/CurrentUserAuditorTest.java
git commit -m "feat(auth): add CurrentUserAuditor for JPA auditing (TEN-59)"
```

---

### Task 2: `JpaAuditingConfig`

**Files:**
- Create: `backend/auth-module/src/main/java/com/cas/tsas/auth/infrastructure/persistence/JpaAuditingConfig.java`

> Reines Configuration-Wiring; kein Unit-Test (Bean-Initialisierung wird im IT in Task 7 ohnehin durchexerziert).

- [ ] **Step 1: Implement config**

```java
package com.cas.tsas.auth.infrastructure.persistence;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Aktiviert JPA-Auditing: Spring Data füllt {@code @CreatedDate}, {@code @CreatedBy},
 * {@code @LastModifiedDate} und {@code @LastModifiedBy} auf den Entities automatisch.
 * Auditor-Quelle ist {@link CurrentUserAuditor}.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "currentUserAuditor")
public class JpaAuditingConfig {
}
```

- [ ] **Step 2: Compile sanity check**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :auth-module:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add backend/auth-module/src/main/java/com/cas/tsas/auth/infrastructure/persistence/JpaAuditingConfig.java
git commit -m "feat(auth): enable JPA auditing (TEN-59)"
```

---

## Phase 2 — Migration

### Task 3: Flyway V7 `add_audit_columns`

**Files:**
- Create: `backend/app/src/main/resources/db/migration/V7__add_audit_columns.sql`

- [ ] **Step 1: Write the migration**

```sql
-- TEN-59: Audit-Spalten auf Player, Match, Point.
-- Spalten sind nullable: Flyway-Migrations und scheduled-Jobs ohne Auth-Context
-- dürfen NULL hinterlassen. Hibernate setzt sie bei neuen Inserts via Spring Data Auditing.

ALTER TABLE players ADD COLUMN created_at TIMESTAMP;
ALTER TABLE players ADD COLUMN created_by UUID;
ALTER TABLE players ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE players ADD COLUMN updated_by UUID;

ALTER TABLE matches ADD COLUMN created_at TIMESTAMP;
ALTER TABLE matches ADD COLUMN created_by UUID;
ALTER TABLE matches ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE matches ADD COLUMN updated_by UUID;

ALTER TABLE points  ADD COLUMN created_at TIMESTAMP;
ALTER TABLE points  ADD COLUMN created_by UUID;
ALTER TABLE points  ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE points  ADD COLUMN updated_by UUID;

-- Backfill: best-effort attribution from owner (Player/Match) bzw. Match-Owner (Point).
UPDATE players SET created_by = owner_id, created_at = NOW(), updated_at = NOW();
UPDATE matches SET created_by = owner_id, created_at = NOW(), updated_at = NOW();
UPDATE points p
   SET created_at = NOW(),
       updated_at = NOW(),
       created_by = (SELECT m.owner_id FROM matches m WHERE m.id = p.match_id);
```

- [ ] **Step 2: Verify migration applies cleanly**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew :app:test --tests "com.cas.tsas.TsasBackendApplicationTests"`
Expected: BUILD SUCCESSFUL (Flyway applies V7 against Testcontainers Postgres).

- [ ] **Step 3: Commit**

```bash
git add backend/app/src/main/resources/db/migration/V7__add_audit_columns.sql
git commit -m "db: migration V7 — audit columns on players, matches, points (TEN-59)"
```

---

## Phase 3 — JPA Entities

### Task 4: Add audit fields to `PlayerJpaEntity`

**Files:**
- Modify: `backend/player-module/src/main/java/com/cas/tsas/player/infrastructure/persistence/entity/PlayerJpaEntity.java`

Add `auth-module` dep to `player-module/build.gradle.kts` *(already added in TEN-55 — verify; if missing, add `implementation(project(":auth-module"))`)*. Add Spring Data JPA's auditing types via existing `spring-boot-starter-data-jpa`.

- [ ] **Step 1: Edit `PlayerJpaEntity`**

Open the file and:
1. Change the class annotation block at the top to:

```java
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "players")
public class PlayerJpaEntity {
```

2. Add imports:

```java
import jakarta.persistence.EntityListeners;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
```

3. After the existing fields (before getters), add the four audit fields plus getters/setters:

```java
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

public Instant getCreatedAt() { return createdAt; }
public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

public UUID getCreatedBy() { return createdBy; }
public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

public Instant getUpdatedAt() { return updatedAt; }
public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

public UUID getUpdatedBy() { return updatedBy; }
public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
```

- [ ] **Step 2: Compile**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :player-module:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run player-module tests**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :player-module:test`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit (gemeinsam mit Task 5 + 6)**

> Halt — diese Änderung steht alleine, aber der Sinn ergibt sich erst zusammen mit Task 5 + 6. Stage nur, committe am Ende von Task 6.

---

### Task 5: Add audit fields to `MatchJpaEntity`

**Files:**
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/entity/MatchJpaEntity.java`

- [ ] **Step 1: Edit `MatchJpaEntity`**

Analog zu Task 4: `@EntityListeners(AuditingEntityListener.class)` an die Class-Annotation, dieselben Imports, dieselben 4 Felder + Accessors.

Konkret: class-level annotations werden zu:

```java
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "matches")
public class MatchJpaEntity {
```

Imports und Felder identisch zu Task 4.

- [ ] **Step 2: Compile + test**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :match-module:test`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit (gemeinsam mit Task 4 + 6)**

---

### Task 6: Add audit fields to `PointJpaEntity`

**Files:**
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/entity/PointJpaEntity.java`

- [ ] **Step 1: Edit `PointJpaEntity`**

Analog: `@EntityListeners(AuditingEntityListener.class)` plus dieselben 4 Felder.

```java
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "points")
public class PointJpaEntity {
```

Imports und Felder identisch zu Task 4.

- [ ] **Step 2: Compile + test**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :match-module:test`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Wider sanity check**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew compileJava compileTestJava`
Expected: BUILD SUCCESSFUL across all modules.

- [ ] **Step 4: Commit Tasks 4 + 5 + 6 together**

```bash
git add backend/player-module/src/main/java/com/cas/tsas/player/infrastructure/persistence/entity/PlayerJpaEntity.java \
        backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/entity/MatchJpaEntity.java \
        backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/entity/PointJpaEntity.java
git commit -m "feat(persistence): audit fields on Player/Match/Point JPA entities (TEN-59)"
```

---

## Phase 4 — Integration Test

### Task 7: `AuditingIT` end-to-end

**Files:**
- Create: `backend/app/src/test/java/com/cas/tsas/security/AuditingIT.java`

- [ ] **Step 1: Write the IT**

```java
package com.cas.tsas.security;

import com.cas.tsas.AbstractIntegrationTest;
import com.cas.tsas.auth.domain.Role;
import com.cas.tsas.auth.testsupport.JwtTestSupport;
import com.cas.tsas.player.infrastructure.persistence.entity.PlayerJpaEntity;
import com.cas.tsas.player.infrastructure.persistence.repository.PlayerJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditingIT extends AbstractIntegrationTest {

    private static final UUID USER_A = UUID.randomUUID();
    private static final UUID ADMIN  = UUID.randomUUID();

    @Autowired ObjectMapper json;
    @Autowired PlayerJpaRepository playerRepo;

    @Test
    void create_sets_created_and_updated_for_owner() throws Exception {
        Instant before = Instant.now().minusSeconds(2);

        String body = json.writeValueAsString(Map.of("firstName", "Alice", "lastName", "A"));
        MvcResult res = mockMvc.perform(post("/api/players")
                        .with(JwtTestSupport.withUser(USER_A, Role.COACH))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        UUID id = UUID.fromString(json.readTree(res.getResponse().getContentAsString()).get("id").asText());

        PlayerJpaEntity stored = playerRepo.findById(id).orElseThrow();

        assertThat(stored.getCreatedBy()).isEqualTo(USER_A);
        assertThat(stored.getUpdatedBy()).isEqualTo(USER_A);
        assertThat(stored.getCreatedAt()).isAfter(before);
        assertThat(stored.getUpdatedAt()).isAfter(before);
        assertThat(stored.getCreatedAt()).isEqualTo(stored.getUpdatedAt());
    }

    @Test
    void admin_update_preserves_created_by_and_sets_updated_by() throws Exception {
        // create as USER_A
        String createBody = json.writeValueAsString(Map.of("firstName", "Bob", "lastName", "B"));
        MvcResult res = mockMvc.perform(post("/api/players")
                        .with(JwtTestSupport.withUser(USER_A, Role.COACH))
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode createdJson = json.readTree(res.getResponse().getContentAsString());
        UUID id = UUID.fromString(createdJson.get("id").asText());
        Instant initialCreatedAt = playerRepo.findById(id).orElseThrow().getCreatedAt();

        // Force a clock gap so updated_at strictly increases
        Thread.sleep(50);

        // update as ADMIN
        String updateBody = json.writeValueAsString(Map.of("firstName", "Bobby", "lastName", "B"));
        mockMvc.perform(put("/api/players/" + id)
                        .with(JwtTestSupport.withUser(ADMIN, Role.COACH, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk());

        PlayerJpaEntity stored = playerRepo.findById(id).orElseThrow();
        assertThat(stored.getCreatedBy()).isEqualTo(USER_A);
        assertThat(stored.getCreatedAt()).isEqualTo(initialCreatedAt);
        assertThat(stored.getUpdatedBy()).isEqualTo(ADMIN);
        assertThat(stored.getUpdatedAt()).isAfter(initialCreatedAt);
    }
}
```

- [ ] **Step 2: Run the IT**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew :app:test --tests "com.cas.tsas.security.AuditingIT"`
Expected: 2 tests pass.

- [ ] **Step 3: Run full `:app:test`**

Run: `cd backend && env -u OPENAI_API_KEY JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew :app:test`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add backend/app/src/test/java/com/cas/tsas/security/AuditingIT.java
git commit -m "test(security): JPA auditing end-to-end (TEN-59)"
```

---

## Phase 5 — Correlation ID

### Task 8: `CorrelationIdFilter` + Test

**Files:**
- Create: `backend/common-module/src/main/java/com/cas/tsas/common/web/CorrelationIdFilter.java`
- Create: `backend/common-module/src/test/java/com/cas/tsas/common/web/CorrelationIdFilterTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.cas.tsas.common.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void uses_request_header_when_present() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        req.addHeader("X-Correlation-Id", "abc-123");
        String[] mdcInsideFilter = new String[1];
        FilterChain chain = (r, s) -> mdcInsideFilter[0] = MDC.get("correlationId");

        filter.doFilter(req, resp, chain);

        assertThat(mdcInsideFilter[0]).isEqualTo("abc-123");
        assertThat(resp.getHeader("X-Correlation-Id")).isEqualTo("abc-123");
        assertThat(MDC.get("correlationId")).isNull(); // cleaned up
    }

    @Test
    void generates_uuid_when_header_missing() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        String[] mdcInsideFilter = new String[1];
        FilterChain chain = (r, s) -> mdcInsideFilter[0] = MDC.get("correlationId");

        filter.doFilter(req, resp, chain);

        assertThat(mdcInsideFilter[0]).isNotBlank();
        assertThat(resp.getHeader("X-Correlation-Id")).isEqualTo(mdcInsideFilter[0]);
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void cleans_mdc_on_exception() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        FilterChain chain = (r, s) -> { throw new RuntimeException("boom"); };

        try {
            filter.doFilter(req, resp, chain);
        } catch (RuntimeException ignored) {
            // expected
        }

        assertThat(MDC.get("correlationId")).isNull();
    }
}
```

- [ ] **Step 2: Run — expect compile error**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :common-module:test --tests "com.cas.tsas.common.web.CorrelationIdFilterTest"`
Expected: compile error.

- [ ] **Step 3: Implement the filter**

```java
package com.cas.tsas.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Setzt eine {@code correlationId} pro HTTP-Request in den SLF4J-MDC und echoed sie über den
 * Response-Header {@code X-Correlation-Id}. Liest einen vorhandenen Request-Header desselben
 * Namens; generiert sonst eine UUID.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String id = request.getHeader(HEADER);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, id);
        response.setHeader(HEADER, id);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
```

- [ ] **Step 4: Run — expect PASS**

Run: same as Step 2. Expected: 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add backend/common-module/src/main/java/com/cas/tsas/common/web/CorrelationIdFilter.java \
        backend/common-module/src/test/java/com/cas/tsas/common/web/CorrelationIdFilterTest.java
git commit -m "feat(common): correlation-id servlet filter (TEN-59)"
```

---

### Task 9: Register the filter

**Files:**
- Create: `backend/common-module/src/main/java/com/cas/tsas/common/web/CorrelationIdConfig.java`

- [ ] **Step 1: Write the config**

```java
package com.cas.tsas.common.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registriert den {@link CorrelationIdFilter} ganz früh in der Filter-Chain — vor allem vor
 * Spring Security — damit jede Log-Aussage (auch Auth-Fehler) eine Correlation-Id trägt.
 */
@Configuration
public class CorrelationIdConfig {

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> bean = new FilterRegistrationBean<>(new CorrelationIdFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        bean.addUrlPatterns("/*");
        return bean;
    }
}
```

- [ ] **Step 2: Compile + Wide-Build Sanity**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :common-module:compileJava :app:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Smoke-IT via existing AbstractIntegrationTest**

Add this test to `AuditingIT` from Task 7 (im selben Commit erlaubt — siehe Step 4 unten):

```java
@Test
void correlation_id_is_echoed_back() throws Exception {
    MvcResult res = mockMvc.perform(get("/api/players")
                    .header(CorrelationIdFilter.HEADER, "correlation-test-id")
                    .with(JwtTestSupport.withUser(USER_A, Role.COACH)))
            .andExpect(status().isOk())
            .andReturn();

    assertThat(res.getResponse().getHeader(CorrelationIdFilter.HEADER))
            .isEqualTo("correlation-test-id");
}
```

Imports oben in `AuditingIT.java` ergänzen: `import com.cas.tsas.common.web.CorrelationIdFilter;`, `import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;`.

- [ ] **Step 4: Run + Commit**

Run: `cd backend && env -u OPENAI_API_KEY JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew :app:test --tests "com.cas.tsas.security.AuditingIT"`
Expected: 3 tests pass.

```bash
git add backend/common-module/src/main/java/com/cas/tsas/common/web/CorrelationIdConfig.java \
        backend/app/src/test/java/com/cas/tsas/security/AuditingIT.java
git commit -m "feat(common): register correlation-id filter + smoke IT (TEN-59)"
```

---

### Task 10: Logback pattern with correlationId

**Files:**
- Modify: `backend/app/src/main/resources/application-local.yml`
- Modify: `backend/app/src/main/resources/application.yml` (optional — production logback)

- [ ] **Step 1: Read current logback config**

Run: `cat backend/app/src/main/resources/application-local.yml backend/app/src/main/resources/application.yml`

- [ ] **Step 2: Extend `application-local.yml` pattern**

Add (or update) under `logging.pattern.console` to include the MDC variable. Append to the file or merge into existing `logging:` block:

```yaml
logging:
  pattern:
    console: "%d{HH:mm:ss.SSS} %-5level [%X{correlationId:-}] %logger{36} - %msg%n"
```

If `logging.pattern.console` is already defined, modify it to include `[%X{correlationId:-}]`. Leave default Spring pattern otherwise.

- [ ] **Step 3: Manual smoke (optional, not part of automated test)**

Document in commit message: `bootRun --args='--spring.profiles.active=local'` followed by `curl -H 'X-Correlation-Id: smoke-1' http://localhost:8080/actuator/health` and check the log output shows `[smoke-1]`.

(Not running this here; the IT in Task 9 already verifies the header round-trip.)

- [ ] **Step 4: Commit**

```bash
git add backend/app/src/main/resources/application-local.yml
git commit -m "chore(logging): include correlationId in local log pattern (TEN-59)"
```

---

## Phase 6 — Final Verification

### Task 11: Full check + Acceptance walkthrough

- [ ] **Step 1: Full backend `check`**

Run: `cd backend && env -u OPENAI_API_KEY JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew check`
Expected: BUILD SUCCESSFUL including JaCoCo gate (85 % line / 70 % branch).

- [ ] **Step 2: Walkthrough Spec §9 (acceptance)**

Manually check each box in the Spec acceptance list. If any fail, BLOCK and report.

- [ ] **Step 3: Open PR (via `finishing-a-development-branch` skill)**

---

## Self-Review (durchgeführt)

- ✅ **Spec coverage:** alle Akzeptanzkriterien aus §9 haben einen Task:
  - `@EnableJpaAuditing` → Task 2
  - `AuditorAware<UUID>` → Task 1
  - `@EntityListeners` + 4 Felder auf Player/Match/Point → Tasks 4 / 5 / 6
  - Migration V7 + Backfill → Task 3
  - IT belegt created/updated/admin-override → Task 7
  - `CorrelationIdFilter` + MDC → Tasks 8 / 9
  - Unit-Tests für Auditor und Filter → Tasks 1, 8
  - JaCoCo-Gate → Task 11
- ✅ **Keine Platzhalter:** alle Code-Steps zeigen Code; YAML-Pattern explizit; SQL ausgeschrieben.
- ✅ **Type-Consistency:** `CurrentUser.id()` (record accessor), `Optional<UUID>`, `Instant` für Timestamps, `UUID` für `*_by`-Felder, `correlationId` MDC-Key durchgängig.
- ⚠️ **Risiko R2 aus Spec** (DataJpaTest ohne Auth-Context lässt Audit-Felder NULL): nicht als eigener Task abgebildet — wird nur aktuell, wenn ein Repository-Slice-Test fehlschlägt; dann punktuell mit `setCreatedBy(...)` im Fixture beheben.

---

## Execution

Plan complete and saved to `docs/superpowers/plans/2026-06-22-ten-59-audit-logging.md`. Two execution options:

1. **Subagent-Driven (recommended)** — Ich dispatche pro Task einen frischen Subagenten, reviewe zwischen Tasks, schnelle Iteration.
2. **Inline Execution** — Tasks in dieser Session ausführen via `superpowers:executing-plans`, Batch mit Checkpoints.
