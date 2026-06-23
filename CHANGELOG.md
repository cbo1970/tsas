# Changelog

Alle nennenswerten Änderungen an TSaS (Tennis Score and Statistic) werden in dieser Datei dokumentiert.

Das Format orientiert sich an [Keep a Changelog 1.1.0](https://keepachangelog.com/de/1.1.0/) und die Versionierung folgt [Semantic Versioning 2.0.0](https://semver.org/lang/de/). Solange die Major-Version `0.x` ist, gelten MINOR-Erhöhungen als potenziell breaking (Pre-1.0-Konvention).

## [Unreleased]

### Added
- Semantic Versioning + CHANGELOG eingeführt (TEN-67) — `allprojects { version = "0.1.0" }` im Backend, `0.1.0` im Frontend, ADR-14 im SAD §9.

## [0.1.0] – 2026-06-23

Erstes versioniertes Release des MVP — Web-App für Tennis-Score-Erfassung, Statistiken und KI-Match-Analyse, ausgeliefert via Docker Compose.

### Added

**Backend (Spring Boot 4, Java 25, Clean Architecture, Gradle Multi-Module)**
- Spieler-CRUD inklusive Soft-Delete (TEN-55, FA-03/04/12/13).
- Match-Lebenszyklus: Erstellen, Beenden, Walkover, manuelle Spielstand-Korrektur, Aufschläger setzen (FA-05/14/15/16).
- Punkterfassung Punkt-für-Punkt mit ITF-Scoring (Punkte/Spiele/Sätze, Tiebreak, Match-Tiebreak, Short Set, Break-Point-Erkennung) im `ScoringService` (FA-06/07).
- Head-to-Head-Statistik und Match-Einzel-Statistik aggregiert aus `points` (TEN-14, TEN-46, FA-08/17).
- KI-Match-Analyse (Postmortem) via Spring AI 2.x mit OpenAI `gpt-4o-mini` als Default, strukturiertem JSON-Output und deterministischem Fallback-Adapter (TEN-15, TEN-26, FA-11).
- Frontend-Komponenten für die KI-Analyse mit Spinner, Empfehlungs-Anzeige und HTTP-Code-spezifischer Fehlerbehandlung (TEN-28).
- Owner-Binding und RBAC: Spieler und Matches sind mandantengebunden über `owner_id` (JWT-Sub), `COACH`- und `ADMIN`-Rollen aus dem Keycloak-Realm (TEN-55).
- JPA-Auditing-Felder (`created_at`, `created_by`, `updated_at`, `updated_by`) auf Player/Match/Point sowie Correlation-ID-Servlet-Filter für strukturiertes Logging (TEN-59).
- Bean Validation auf REST-DTOs: `@Size`-Limits auf Player-/RecordPoint-Feldern und typisierte Enums (TEN-60).
- OpenAPI 3-Vertrag via springdoc-openapi unter `/v3/api-docs` und `/swagger-ui.html`, Security-Scheme `bearer-jwt` (TEN-61).
- ArchitectureTest setzt Schichten- und Modulgrenzen (Domain framework-frei, einwärts gerichtete Abhängigkeiten) im Build-Gate durch.
- Flyway-Migrationen V1–V7 als alleinige Schema-Quelle (Boot-Hibernate auf `validate` bzw. `none`).
- JaCoCo-Coverage-Gate (85 % Line / 70 % Branch) modulübergreifend aggregiert; an `check` gekoppelt; aggregierter Report bei jedem CI-Lauf als Build-Artifact (TEN-37).
- GitHub-Actions-Workflows `backend-ci.yml` und `frontend-ci.yml` als required status checks auf `develop`/`main`.

**Frontend (Angular 21)**
- Standalone-App mit `angular-oauth2-oidc` 20 (PKCE-S256, APP_INITIALIZER-basierter Login-Flow vor Routing).
- Court-Scoring-Panel mit Inline-Scoring und Touch-optimiertem UI (TEN-32).
- Match-Analyse-Route `/matches/:id/analysis` (TEN-28).
- Statistiken-Seite (Match-Einzelstatistik, Head-to-Head).
- Cypress Component Tests + Vitest Unit Tests, beide in der CI.
- Externe HTML-Templates statt Inline-Templates (TEN-34).

**Auth + Deployment**
- Keycloak 26.6.1 mit Realm-Export `tsas` (PKCE-Public-Client, Self-Registration, Realm-Rollen `COACH`/`ADMIN`) (TEN-5).
- Docker-Compose-Stack: PostgreSQL 16, Keycloak, Backend, Nginx-Frontend; HTTPS via selbstsigniertem Zertifikat.

**Dokumentation**
- SAD nach arc42 in Deutsch (`doc/sad/TSaS_SAD_arc42_1.md`) mit 17 funktionalen Anforderungen, 10 SMART-NfAs, 13 ADRs, Bausteinsicht, Laufzeitsicht mit zwei PlantUML-Sequenzdiagrammen, Verteilungssicht, Datenmodell, Querschnitts-Konzepten (TEN-47, TEN-61).
- STRIDE-Threat-Analyse (`doc/sad/TSaS_STRIDE_Threat_Analysis.md`).
- KI-Werkzeuge-Kapitel + Eigenständigkeitserklärung + Reflexion mit drei Veto-Entscheidungen (TEN-61).
- Cloud-Deployment-Proposal (`doc/deployment/`).
- Plan-/Spec-Sammlung unter `docs/superpowers/` als Beleg für den Spec-Plan-Implementierungs-Workflow.

### Changed
- Scoring-Konsolidierung: ursprünglich als eigenständiges `scoring-module` geplant, in `match-module` integriert (ADR-12).
- Gemeinsames lesendes Domänenmodell statt Anti-Corruption-DTO-Mapping an jeder Modulgrenze (ADR-13).
- Fehlerantworten folgen RFC 7807 `ProblemDetail` statt eines Ad-hoc-Formats; zentrale `@RestControllerAdvice`s pro Bereich.

### Security
- Alle API-Endpunkte (ausser `/actuator/health`, `/actuator/info`, `/v3/api-docs/**`, `/swagger-ui/**`) erfordern ein gültiges OAuth2-Bearer-Token aus dem Keycloak-Realm `tsas` (FA-02, QZ-05).
- Cross-Tenant-Schutz über `owner_id` mit End-to-End-Tests gegen Cross-Tenant-404 (TEN-55).
- Audit-Spalten ermöglichen Nachvollziehbarkeit von Datenänderungen (TEN-59).

### Known Issues
- STRIDE-Befund S1 (`aud`-Prüfung im JwtDecoder) noch offen — Mitigation in Folge-Ticket.
- Self-Registration ohne E-Mail-Verifizierung (TEN-64).
- AI-Endpoint ohne Rate-Limit (TEN-64).
- Container laufen aktuell als Root (TEN-63).
- Lokales `MatchAnalysisControllerIT` schlägt fehl, wenn `OPENAI_API_KEY` in der Shell gesetzt ist (Env-Leak in Spring-Boot-Relaxed-Binding) — CI nicht betroffen.

[Unreleased]: https://github.com/cbo1970/tsas/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/cbo1970/tsas/releases/tag/v0.1.0
