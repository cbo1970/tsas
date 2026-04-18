# Flyway Baseline Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce Flyway as the single source of truth for database schema management, replacing Hibernate `ddl-auto` with a versioned baseline migration that works on both PostgreSQL (prod) and H2 (local/test).

**Architecture:** Flyway dependency and all migration scripts live in the `app` module — the natural aggregator. `V1__baseline.sql` captures the current three-table schema in dialect-neutral ANSI SQL. Hibernate switches from schema-owner (`create-drop`/`update`) to schema-consumer (`none`/`validate`).

**Tech Stack:** Spring Boot 3.4.3, Flyway 10.x (via Spring Boot BOM), H2 2.x, PostgreSQL 16, Gradle Kotlin DSL

---

## File Map

| Action | File | Purpose |
|--------|------|---------|
| Modify | `backend/app/build.gradle.kts` | Add `flyway-core` + `flyway-database-postgresql` |
| Create | `backend/app/src/main/resources/db/migration/V1__baseline.sql` | Baseline schema (players, matches, match_scores) |
| Modify | `backend/app/src/main/resources/application.yml` | `ddl-auto: update` → `validate` |
| Modify | `backend/app/src/main/resources/application-local.yml` | `ddl-auto: create-drop` → `none` |

---

## Task 1: Add Flyway Dependencies

**Files:**
- Modify: `backend/app/build.gradle.kts`

- [ ] **Step 1: Add dependencies**

Replace the contents of `backend/app/build.gradle.kts` with:

```kotlin
plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":common-module"))
    implementation(project(":player-module"))
    implementation(project(":match-module"))
    implementation(project(":statistics-module"))
    implementation(project(":auth-module"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.springframework.security:spring-security-test")
}
```

- [ ] **Step 2: Verify the dependency resolves**

```bash
cd backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :app:dependencies --configuration runtimeClasspath | grep flyway
```

Expected output contains:
```
org.flywaydb:flyway-core:10.x.x
org.flywaydb:flyway-database-postgresql:10.x.x
```

---

## Task 2: Create V1 Baseline Migration

**Files:**
- Create: `backend/app/src/main/resources/db/migration/V1__baseline.sql`

The SQL must be dialect-neutral so it runs on both H2 and PostgreSQL. UUID is a native type in both. No sequences, no `gen_random_uuid()` — IDs are generated in Java by `@GeneratedValue(strategy = GenerationType.UUID)`.

- [ ] **Step 1: Create the migration directory and file**

Create `backend/app/src/main/resources/db/migration/V1__baseline.sql` with the following content:

```sql
CREATE TABLE players (
    id            UUID         NOT NULL,
    first_name    VARCHAR(255) NOT NULL,
    last_name     VARCHAR(255) NOT NULL,
    gender        VARCHAR(255),
    handedness    VARCHAR(255),
    backhand_type VARCHAR(255),
    ranking       VARCHAR(255),
    nationality   VARCHAR(255),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    birth_date    DATE,
    PRIMARY KEY (id)
);

CREATE TABLE matches (
    id             UUID         NOT NULL,
    player1_id     UUID         NOT NULL,
    player2_id     UUID         NOT NULL,
    sets_to_win    INTEGER      NOT NULL,
    match_tiebreak BOOLEAN      NOT NULL,
    short_set      BOOLEAN      NOT NULL,
    status         VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE match_scores (
    id                   UUID         NOT NULL,
    match_id             UUID         NOT NULL,
    points_player1       INTEGER      NOT NULL DEFAULT 0,
    points_player2       INTEGER      NOT NULL DEFAULT 0,
    games_player1        INTEGER      NOT NULL DEFAULT 0,
    games_player2        INTEGER      NOT NULL DEFAULT 0,
    sets_player1         INTEGER      NOT NULL DEFAULT 0,
    sets_player2         INTEGER      NOT NULL DEFAULT 0,
    is_deuce             BOOLEAN      NOT NULL DEFAULT FALSE,
    is_advantage_player1 BOOLEAN,
    current_set          INTEGER      NOT NULL DEFAULT 1,
    is_done              BOOLEAN      NOT NULL DEFAULT FALSE,
    winner               VARCHAR(255),
    aces_player1         INTEGER      NOT NULL DEFAULT 0,
    aces_player2         INTEGER      NOT NULL DEFAULT 0,
    serving_player       INTEGER,
    PRIMARY KEY (id),
    UNIQUE (match_id)
);
```

---

## Task 3: Switch Hibernate to Schema-Consumer Mode

**Files:**
- Modify: `backend/app/src/main/resources/application.yml`
- Modify: `backend/app/src/main/resources/application-local.yml`

Changing `ddl-auto` must happen together with Flyway being present — otherwise the app starts with no schema. Both files are changed in this task.

- [ ] **Step 1: Update `application.yml` (PostgreSQL / prod)**

Change only the `ddl-auto` line from `update` to `validate`:

```yaml
  jpa:
    hibernate:
      ddl-auto: validate
```

Full relevant section after the change:

```yaml
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    open-in-view: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
```

- [ ] **Step 2: Update `application-local.yml` (H2 / local dev)**

Change `ddl-auto` from `create-drop` to `none`:

```yaml
  jpa:
    hibernate:
      ddl-auto: none
```

Full relevant section after the change:

```yaml
  datasource:
    url: jdbc:h2:mem:tsasdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver

  jpa:
    hibernate:
      ddl-auto: none
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect

  h2:
    console:
      enabled: true
      path: /h2-console
```

---

## Task 4: Verify and Commit

- [ ] **Step 1: Run the smoke test (H2 + Flyway)**

```bash
cd backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test --tests "com.cas.tsas.TsasBackendApplicationTests" --info 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL` — Flyway applies `V1__baseline.sql` against H2, Hibernate validates the schema.

- [ ] **Step 2: Run all integration tests (PostgreSQL + Flyway)**

```bash
cd backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test
```

Expected: `BUILD SUCCESSFUL` — all existing IT tests pass. Testcontainers spins up PostgreSQL, Flyway creates the schema from scratch, Hibernate validates.

If a test fails with `Table "X" not found` or `Schema-validation: missing table`, the `V1__baseline.sql` is missing a column or table — compare against the JPA entity and fix the SQL.

If a test fails with `Flyway ... found non-empty schema(s) ... without schema history table`, add `spring.flyway.baseline-on-migrate=true` to the relevant profile config. This can happen if the Testcontainers DB already has tables (shouldn't occur with a fresh container).

- [ ] **Step 3: Commit**

```bash
git add backend/app/build.gradle.kts \
        backend/app/src/main/resources/db/migration/V1__baseline.sql \
        backend/app/src/main/resources/application.yml \
        backend/app/src/main/resources/application-local.yml
git commit -m "feat: introduce Flyway migrations, V1 baseline schema"
```
