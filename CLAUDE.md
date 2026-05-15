# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**TSaS (Tennis Score and Statistic)** is a web application for tennis match tracking and statistics. Coaches and parents can document matches point-by-point with fixed attributes (winners, faults, aces, errors, etc.) and generate head-to-head statistics to prepare for upcoming matches.

The Spring Boot backend skeleton has been implemented (Clean Architecture). The Angular frontend is not yet scaffolded.

## Planned Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot |
| Auth | Keycloak (OAuth2) |
| Frontend (Web) | Angular |
| Frontend (iOS, v2+) | Swift |
| Database | PostgreSQL |

## Architecture

The system follows a multi-tier architecture:

- **Angular SPA** → REST API calls → **Spring Boot backend** → **PostgreSQL**
- **Keycloak** handles authentication/authorization via OAuth2; Spring Boot delegates to it
- The backend exposes a REST API consumed by both the Angular web frontend and (v2+) the iOS native app

### Core Domain Concepts

- **Player**: name, gender, ranking, handedness, backhand type
- **Match (Begegnung)**: configurable attributes (number of sets to win, match tie-break, short set)
- **Point**: belongs to a match, has a fixed attribute (e.g. forehand winner, ace, double fault, net, out long/side) for both own player and opponent, plus optional free-text remark
- **Statistics**: head-to-head, % winners, % unforced errors, % 1st/2nd service, aces/double faults

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

# Run the app (requires PostgreSQL; or use -Plocal profile for H2)
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew bootRun

# Run with local H2 profile (no PostgreSQL required)
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew bootRun --args='--spring.profiles.active=local'

# Run all tests (uses H2 via @ActiveProfiles("local"))
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test

# Run a single test class
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test --tests "com.cas.tsas.*ClassName*"

# Compile + test + checks
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew check

# Full build (produces bootJar)
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew build
```

### Frontend (Angular — not yet scaffolded)

```bash
npm install                          # Install dependencies
ng serve                             # Start dev server
ng test                              # Run unit tests
ng test --include='**/foo.spec.ts'   # Run a single spec file
ng lint                              # Lint
```

## Repository Structure

```
tsas/
├── backend/                         # Spring Boot 4.0.x backend (Gradle Kotlin DSL)
│   ├── src/main/java/com/cas/tsas/
│   │   ├── TsasBackendApplication.java
│   │   ├── domain/                  # Enterprise rules — no framework deps
│   │   │   ├── model/               # Player, Match, Point
│   │   │   └── exception/
│   │   ├── application/             # Use cases (ports & services)
│   │   │   ├── port/in/             # Input ports (interfaces)
│   │   │   ├── port/out/            # Output ports (interfaces)
│   │   │   └── service/             # Use case implementations (@Service)
│   │   └── infrastructure/          # Framework & driver adapters
│   │       ├── web/                 # REST controllers + DTOs
│   │       ├── persistence/         # JPA entities, repositories, adapters
│   │       └── security/            # SecurityConfig (stub — permits all for dev)
│   └── src/main/resources/
│       ├── application.yml          # Production config (PostgreSQL)
│       └── application-local.yml    # Local dev config (H2 in-memory)
├── doc/
│   └── tsas_sad.md                  # Software Architecture Document (German)
└── CLAUDE.md
```

The project uses `develop` as the main working branch; `main` is the stable branch.
