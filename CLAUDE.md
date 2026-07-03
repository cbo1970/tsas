# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**TSaS (Tennis Score and Statistic)** is a web application for tennis match tracking and statistics. Coaches and parents can document matches point-by-point with fixed attributes (winners, faults, aces, errors, etc.) and generate head-to-head statistics to prepare for upcoming matches.

The Spring Boot backend (Clean Architecture, Gradle multi-module) and the Angular frontend are both implemented for V1, including AI-assisted match analysis. The native iOS app (v2+) is not yet started.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 4.0.6 (Java 25), Gradle Kotlin DSL (multi-module) |
| Auth | Keycloak 26 (OAuth2 / OIDC, PKCE) |
| Frontend (Web) | Angular 21 (standalone, `angular-oauth2-oidc`) |
| Frontend (iOS, v2+) | Swift (planned) |
| Database | PostgreSQL 16 (Flyway migrations) |
| AI analysis | Spring AI → OpenAI via `LlmClientPort` (deterministic Fake adapter as fallback) |

## Architecture

The system follows a multi-tier architecture:

- **Angular SPA** → REST API calls → **Spring Boot backend** → **PostgreSQL**
- **Keycloak** handles authentication/authorization via OAuth2; Spring Boot delegates to it
- The backend exposes a REST API consumed by both the Angular web frontend and (v2+) the iOS native app

### Core Domain Concepts

- **Player**: name, gender, ranking, handedness, backhand type
- **Match (Begegnung)**: configurable attributes (number of sets to win, match tie-break, short set)
- **Point**: belongs to a match, has a fixed attribute (e.g. forehand winner, ace, double fault, net, out long/side) for both own player and opponent, plus optional free-text remark
- **Statistics**: head-to-head, % winners, % unforced errors, % 1st/2nd service, aces/double faults; computed on-the-fly from points
- **MatchAnalysis**: AI-generated tactical postmortem for a completed match (key moments, strengths/weaknesses, 3–5 recommendations) via Spring AI/OpenAI; 1:1 to a match, human-in-the-loop recommendation status
- **Coach notes**: free-text notes per (match, player)

### Versioned Roadmap (from SAD)

- **V1 (MVP)**: Web app, score tracking, fixed point attributes, basic statistics, own Keycloak user registration
- **V2**: Google as external IDP, advanced statistics, native iOS app
- **V3**: Ball landing point capture via touch on court diagram
- **V4**: Swisstennis API integration
- **V5**: Camera-based automatic ball landing point detection

## Quality Targets

- 95% availability
- Max 100 concurrent users
- Data entry response time: ≤ 250 ms
- Statistics generation: ≤ 1 min

## Development Commands

### Backend (Spring Boot — Gradle Kotlin DSL)

Java 25 is the installed JDK and the source/target release (`options.release = 25`).
Set `JAVA_HOME=/opt/java/jdk-25.0.1` before running Gradle commands.

```bash
cd backend

# Run the app (requires PostgreSQL — see docker/db/compose.yaml)
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew bootRun

# Run with the `local` profile (also PostgreSQL; adds HTTPS + local JWKS/CORS)
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew bootRun --args='--spring.profiles.active=local'

# Run all tests. Unit/slice tests + a couple of context tests use the `test`
# profile (in-memory H2, application-test.yml). The integration tests (*IT)
# run against real PostgreSQL 16 via Testcontainers, so a container runtime
# (Docker/Podman) must be available.
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test

# Run a single test class
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test --tests "com.cas.tsas.*ClassName*"

# Compile + test + checks
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew check

# Full build (produces bootJar)
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew build
```

#### Test coverage (JaCoCo)

Coverage is aggregated across all modules (integration tests live in `:app` but
exercise classes everywhere, so a per-module report would under-count). A gate is
wired into `check` and fails the build below **85% line / 70% branch** coverage.
The IT tests need a container runtime — point `DOCKER_HOST` at the Podman socket.

```bash
# Aggregated HTML/XML/CSV report -> backend/build/reports/jacoco/jacocoRootReport/
JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock \
  TESTCONTAINERS_RYUK_DISABLED=true ./gradlew jacocoRootReport

# The coverage gate on its own (also runs as part of `check`)
JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock \
  TESTCONTAINERS_RYUK_DISABLED=true ./gradlew jacocoRootCoverageVerification
```

Thresholds live in `backend/build.gradle.kts` (`violationRules`).

### Frontend (Angular 21)

```bash
cd frontend

npm install                          # Install dependencies
npm start                            # Dev server (ng serve) — https://localhost:4200
npm run build                        # Production build

npx ng test --no-watch               # Unit tests (Vitest)
npx cypress run --component          # Component tests (Cypress)
```

## Repository Structure

```
tsas/
├── backend/                         # Spring Boot 4.0.6 backend (Gradle Kotlin DSL, multi-module)
│   ├── app/                         # @SpringBootApplication, wiring/config, Flyway migrations,
│   │                                #   integration tests (*IT), plus data-export + user-preferences features
│   ├── common-module/               # Shared kernel: common DTOs, exceptions, config, web utilities
│   ├── player-module/               # Player profiles (CRUD + search)
│   ├── match-module/                # Matches, scoring (ScoringService), coach player-notes
│   ├── statistics-module/           # Head-to-head + per-match statistics (computed on-the-fly from points)
│   ├── auth-module/                 # Keycloak/JWT resource-server security, RBAC; testFixtures permitAll chain
│   ├── ai-module/                   # AI match analysis (Spring AI → OpenAI via LlmClientPort)
│   └── app/src/main/resources/
│       ├── application.yml          # Default config (PostgreSQL, Keycloak JWT)
│       ├── application-local.yml    # Local dev config (PostgreSQL + HTTPS/JWKS/CORS)
│       ├── application-test.yml     # Test profile (in-memory H2; ITs override to Testcontainers PostgreSQL)
│       └── db/migration/            # Flyway scripts V1__…Vn (V1__baseline.sql = initial schema)
├── frontend/                        # Angular 21 SPA (standalone, angular-oauth2-oidc + PKCE)
├── docker/                          # Compose stack (frontend/nginx, backend, db, keycloak, mailhog) + prod overlay
├── doc/
│   ├── sad/                         # arc42 SAD — TSaS_SAD.md = maintainable source (TSaS_SAD.docx = pandoc export) + STRIDE, diagrams
│   ├── superpowers/                 # Superpowers specs/ + plans/ (brainstorming & planning artifacts)
│   └── runbooks/                    # Operational runbooks (e.g. AI-analysis smoke test)
├── .github/workflows/               # CI: backend-ci.yml, frontend-ci.yml
└── CLAUDE.md
```

**Superpowers workflow:** Spec and plan documents from brainstorming / writing-plans
sessions live under `doc/superpowers/specs/` and `doc/superpowers/plans/` (moved from
the former `docs/superpowers/`). Use this location for future superpowers sessions.

Each backend module is internally layered per Clean Architecture / Ports & Adapters:
`domain/` (model + exception, no framework deps) → `application/` (`port/in`, `port/out`, `service`)
→ `infrastructure/` (`web`, `persistence`, `security`). An `ArchitectureTest` enforces the
inward-only dependency direction. `SecurityConfig` (auth-module) is a real JWT resource server —
only `/actuator/health|info` and the Swagger endpoints are `permitAll`.

The project uses `develop` as the main working branch; `main` is the stable branch.
