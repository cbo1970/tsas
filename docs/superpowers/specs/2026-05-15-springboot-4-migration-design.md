# Spring Boot 4 Migration — Design Spec

**Linear**: TEN-5
**Date**: 2026-05-15
**Author**: Christian Bonnhoff (via Claude Code brainstorming)
**Source branch**: `develop` (commit at brainstorming time: `47010ee`)
**Target branch**: `feature/springboot-4` (in isolated git worktree)

## Goal

Upgrade the TSaS backend from Spring Boot **3.4.3** to Spring Boot **4.0.6** (latest stable as of 2026-04-23). Scope is *clean migration*: do whatever the version bump forces, plus clean up Spring-deprecations that appear in code we have to touch. No new SB4 feature adoption.

## Out of Scope

- Frontend (Angular). Only touched if the REST API surface changes, which is not expected.
- Keycloak version bump.
- Docker / Podman setup.
- Adoption of new SB4 features (new Actuator endpoints, Observability API, etc.).
- Refactoring unrelated to the migration.

## Migration Path

**Direct jump** 3.4.3 → 4.0.6. No intermediate 3.5 stop. Rationale: the codebase is small (81 Java files, 5 modules, mostly skeleton work) — incremental stops would create overhead without proportional safety.

## Workflow

Work happens in an isolated **git worktree** off `develop`, on branch `feature/springboot-4`. This keeps the current `develop` workspace (which has uncommitted `doc/sad/TSAS.drawio` edits and an in-progress `docs/superpowers/plans/2026-04-25-point-entity.md`) untouched.

## Sequence of Commits

The work is split into up to 5 commits. Each commit must build green (`./gradlew build`) before the next begins. **Commits 2–5 may be omitted** if the corresponding concern needs no changes — the PR description must explicitly state which were skipped and why, so the negative finding stays documented.

| # | Commit message | Inhalt |
|---|---|---|
| 1 | `chore(deps): bump Spring Boot 3.4.3 → 4.0.6` | Plugin version + BOM in `backend/build.gradle.kts`. Verify `io.spring.dependency-management 1.1.7` still compatible; bump if required. Nothing else. Expected: build red until later commits. |
| 2 | `fix(security): adapt to Spring Security 7` | Whatever Security 7 changes break the current `SecurityConfig` / `SecurityConfigLocal` in `auth-module`. Current config already uses modern lambda DSL — expectation is minor adjustments. |
| 3 | `fix(persistence): adapt to Hibernate 7 / Jakarta 11` | ID-generator strategies (Hibernate 7 stricter on `@GeneratedValue(AUTO)`), possible `@Column` default changes, Flyway plugin/driver compatibility. |
| 4 | `fix(web): adapt to Spring Framework 7 / Jackson` | Affects `player-module`, `match-module`, `common-module` controllers and DTOs. Possible Jackson major-version package renames. |
| 5 | `chore(test): adapt to upgraded JUnit / Testcontainers / Mockito` | Boot 4 BOM may pull newer JUnit Platform / Mockito with breaking changes. |

## Per-Module Expectations

| Modul | Java-Files | Erwartete Touch-Points |
|---|---|---|
| **root** (`backend/build.gradle.kts`) | — | Plugin + BOM version. |
| **app** | 9 | Probably no code changes. `BootRun.workingDir` API is stable. |
| **common-module** | 2 | Only if a Spring-Core rename hits it. |
| **player-module** | 24 (JPA + Web) | Highest chance of Hibernate-7 / Jakarta-Validation adjustments. |
| **match-module** | 44 (JPA + Web + Point entity) | Same as player-module, plus recent Point-entity work. |
| **statistics-module** | 0 | Skip — placeholder only. |
| **auth-module** | 2 (Security) | Spring Security 7 adjustments if needed. |

## Research to Be Done in Implementation Plan

Before writing code, the implementation plan must read and reference:
1. Spring Boot 4.0 Release Notes & Migration Guide
2. Hibernate 7 Migration Guide (focus: ID generator strategies)
3. Spring Security 7 Migration Guide (focus: Resource Server, `authorizeHttpRequests`)
4. Flyway compatibility with Boot 4 BOM (use BOM-managed version, do not pin manually)
5. Testcontainers + JUnit / Mockito versions from Boot 4 BOM — breaking changes?
6. Jackson major version in Boot 4 (2.x vs 3.x — affects package paths if 3.x)

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Hibernate 7 `@GeneratedValue(AUTO)` behavior change breaks IT | Medium | Switch to explicit `IDENTITY` / `SEQUENCE`, check Flyway migrations |
| Flyway plugin incompatible with Boot 4 | Low | Use BOM-managed version; pin separately only if BOM choice is broken |
| Spring Security 7 default changes (CSRF/CORS) | Medium | Config is already explicit — should survive unchanged |
| `local` profile (HTTPS + Keycloak) startup regression | Medium | Final-gate (`bootRun local`) catches this; fix lands in Commit 2 |
| Test profile (`permitAll`) hides Security-path regression | High | `bootRun --spring.profiles.active=local` is mandatory in the final gate, not optional |
| Java 21 release target incompatible with Boot 4 | Very low | Boot 4 requires Java 17+; 21 is supported |

## Verification

### Per-commit gate
```
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew clean build
```
Must be green before the next commit. If a commit cannot be made green in isolation, revisit the sequence — do not stack red commits.

### Final gate (before opening the PR)

Mandatory — all five must pass:

1. `./gradlew build` is green
2. Keycloak (Podman) + PostgreSQL (Podman) are running locally
3. `./gradlew bootRun --args='--spring.profiles.active=local'` starts without ERROR-level log lines
4. `curl -k https://localhost:8080/actuator/health` returns `{"status":"UP"}`
5. An authenticated call (e.g. `GET /players`) with a valid Keycloak JWT returns 200

### Rollback

Work happens in an isolated worktree on its own branch. If the migration becomes infeasible: discard the worktree, delete the branch, set TEN-5 back to Backlog. `develop` is never touched.

## Definition of Done

- `./gradlew build` is green
- `bootRun --spring.profiles.active=local` starts cleanly
- `/actuator/health` returns 200
- Authenticated endpoint call with Keycloak JWT returns 200
- PR is opened, reviewed, squash-merged to `develop`
- TEN-5 in Linear is set to **Done** (this time with real work behind it)
- Worktree is cleaned up
- `MEMORY.md` is updated to reflect Spring Boot 4.0.6

## Next Step

Implementation plan via `superpowers:writing-plans`. The plan owns:
- Concrete migration-guide findings
- Exact code locations to change
- Per-commit checklist with verification commands
- Order of attempts when uncertainty exists
