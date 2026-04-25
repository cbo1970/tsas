# TSaS – Feature Gap Analyse

| Feld | Wert |
|------|------|
| **Erstellt** | 2026-04-25 |
| **Stand Codebase** | Branch `develop`, letzter Commit `4401cee` |
| **Referenz** | SAD TSaS arc42 v1.0 (Entwurf) |

---

## Legende

| Symbol | Bedeutung |
|--------|-----------|
| ✅ | Vollständig umgesetzt |
| ⚠️ | Teilweise / abweichend umgesetzt |
| ❌ | Nicht umgesetzt |

---

## 1. Funktionale Anforderungen (V1 MVP)

| ID | Anforderung | Status | Bemerkung |
|----|-------------|--------|-----------|
| FA-01 | **User-Registrierung** | ✅ | Keycloak Self-Service-Registrierung konfiguriert (`registrationAllowed: true`). Redirect nach Registration via `APP_INITIALIZER` umgesetzt. |
| FA-02 | **Authentifizierung** | ✅ | OAuth2 Authorization Code Flow + PKCE vollständig implementiert. Frontend (`angular-oauth2-oidc`), Backend JWT-Validierung via Keycloak, `AuthGuard`, `AuthInterceptor`. |
| FA-03 | **Spieler erfassen** | ⚠️ | `POST /api/players` mit allen Pflichtfeldern umgesetzt. **Abweichung:** `Gender`-Enum enthält nur `MALE`/`FEMALE` — SAD spezifiziert zusätzlich `OTHER`. Pagination in GET-Response fehlt. |
| FA-04 | **Spieler suchen** | ⚠️ | `GET /api/players` existiert. **Abweichung:** Keine Query-Parameter `?firstName=&lastName=` — SAD fordert Pflichtfilter. Keine Pagination (SAD: max. 50 pro Seite). Frontend-seitige Filterung vorhanden, aber nicht über API. |
| FA-05 | **Match erstellen** | ✅ | `POST /api/matches` mit allen Pflichtfeldern (`player1Id`, `player2Id`, `setsToWin`, `matchTiebreak`, `shortSet`) umgesetzt. HTTP 201 + UUID. |
| FA-06 | **Punkte erfassen** | ⚠️ | Punkterfassung technisch funktionsfähig, aber **abweichendes API-Design:** SAD fordert `POST /api/matches/{id}/points` mit `pointType`/`strokeType`/`direction`. Implementiert sind granulare Endpoints (`/score/player1`, `/ace/player1`, `/serve/player1`). Kein einzelner `POINT`-Datensatz pro Punkt — nur Aggregat-Score wird aktualisiert. Freitext-Feld `remark` fehlt. |
| FA-07 | **Spielstand anzeigen** | ⚠️ | Score-Anzeige im Frontend (`ScoreComponent`) vorhanden. `GET /api/matches/{id}` liefert `MatchWithScoreResponse`. **Offen:** Vollständige Verifikation der ITF-Regelkonformität (Tiebreak, Match-Tiebreak, Short Set) steht aus. |
| FA-08 | **Head-to-Head-Statistik** | ❌ | `statistics-module` ist ein leerer Placeholder. Kein REST-Endpoint, keine Berechnungslogik, kein Datenmodell. |

---

## 2. Datenmodell (SAD Kapitel 11)

| Entität | SAD-Spezifikation | Status | Bemerkung |
|---------|-------------------|--------|-----------|
| `PLAYER` | Spielerprofile | ✅ | `PlayerJpaEntity` mit allen Feldern vorhanden. |
| `MATCH` | Begegnungen | ✅ | `MatchJpaEntity` vorhanden. |
| `MATCH_SET` | Einzelne Sätze | ❌ | Nicht als eigene Entität implementiert. Set-Stand wird in `MatchScoreJpaEntity` aggregiert. |
| `CURRENT_SCORE` | Live-Spielstand | ⚠️ | Als `MatchScoreJpaEntity` (Tabelle `match_scores`) umgesetzt. Enthält mehr Felder als im SAD, deckt aber den Anwendungsfall ab. |
| `MATCH_STATS` | Aggregierte Statistiken | ❌ | Keine Tabelle, keine Entität vorhanden. |
| `POINT` | Einzelpunkte | ❌ | Keine Tabelle, keine Entität vorhanden. Punkte werden nicht einzeln persistiert. |

---

## 3. Backend-Module (SAD Kapitel 5.2)

| Modul | SAD-Beschreibung | Status | Bemerkung |
|-------|-----------------|--------|-----------|
| `player-module` | Spielerverwaltung CRUD | ✅ | Vollständig. Inkl. `deactivate`-Lifecycle. |
| `match-module` | Match + Sets verwalten | ✅ | Vollständig für Match-Lifecycle. Sets als Aggregat in Score, nicht als eigenes Modul. |
| `scoring-module` | Spielstand erfassen | ⚠️ | **Kein separates Gradle-Modul.** `ScoringService` ist Teil von `match-module`. Inhaltlich funktionsfähig. |
| `statistics-module` | Statistiken berechnen | ❌ | Gradle-Modul existiert, ist aber ein leerer Placeholder ohne Implementierung. |
| `auth-module` | Keycloak-Integration | ✅ | `SecurityConfig` mit JWT-Validierung. Separates Gradle-Modul vorhanden. |
| `common-module` | Shared Kernel | ⚠️ | Enthält nur CORS-Konfiguration. DTOs, Exceptions, Utilities liegen direkt in den Fachmodulen. |

---

## 4. REST API

| Endpoint | SAD / FA | Status | Bemerkung |
|----------|----------|--------|-----------|
| `POST /api/players` | FA-03 | ✅ | |
| `GET /api/players?firstName=&lastName=` | FA-04 | ⚠️ | Kein Query-Filter, keine Pagination |
| `GET /api/players/{id}` | FA-04 | ✅ | |
| `PUT /api/players/{id}` | — | ✅ | Nicht in SAD spezifiziert, sinnvolle Ergänzung |
| `DELETE /api/players/{id}` | — | ✅ | |
| `PATCH /api/players/{id}/deactivate` | — | ✅ | |
| `POST /api/matches` | FA-05 | ✅ | |
| `GET /api/matches` | — | ✅ | |
| `GET /api/matches/{id}` | FA-07 | ✅ | |
| `POST /api/matches/{id}/points` | FA-06 | ❌ | Nicht vorhanden. Stattdessen granulare Endpoints. |
| `POST /api/matches/{id}/score/player1` | — | ⚠️ | Abweichend von SAD, funktional |
| `POST /api/matches/{id}/ace/player1` | — | ⚠️ | Abweichend von SAD, funktional |
| `POST /api/matches/{id}/serve/player1` | — | ⚠️ | Abweichend von SAD, funktional |
| `POST /api/matches/{id}/end` | — | ✅ | Nicht in SAD spezifiziert, sinnvoll |
| `POST /api/matches/{id}/end/walkover` | — | ✅ | Nicht in SAD spezifiziert, sinnvoll |
| `GET /api/statistics/head-to-head` | FA-08 | ❌ | Nicht implementiert |

---

## 5. Querschnittliche Konzepte (SAD Kapitel 8)

| Konzept | SAD-Anforderung | Status | Bemerkung |
|---------|----------------|--------|-----------|
| **Sicherheit** (8.1) | OAuth2 Bearer Token, HTTP 401 für unauth. Requests | ✅ | `SecurityConfig` schützt alle Endpoints ausser `/actuator/health` und `/actuator/info`. |
| **Persistenz / Flyway** (8.2) | Flyway-Migrationen, `ddl-auto=validate` | ✅ | `V1__baseline.sql` vorhanden. `ddl-auto=validate` konfiguriert. |
| **Fehlerbehandlung** (8.3) | `@ControllerAdvice`, strukturiertes JSON | ⚠️ | `GlobalExceptionHandler` vorhanden (`NotFoundException` → 404, `IllegalStateException` → 409). Kein `error-code` + `timestamp` im Response-Format laut SAD. |
| **Logging / Monitoring** (8.4) | Strukturiertes Logging, Actuator, Prometheus | ⚠️ | Spring Boot Actuator aktiviert (`health`, `info`, `metrics`). Kein strukturiertes JSON-Logging konfiguriert. Kein Prometheus/Grafana-Setup. |
| **Testkonzept** (8.5) | Unit + Integration + API + Frontend Tests | ⚠️ | Unit Tests (3) und Integration Tests (4 API + 2 Persistence) vorhanden. Kein REST-Assured. Kein Frontend-Test (Jasmine/Karma). |
| **OpenAPI / Swagger** | Nicht im SAD spezifiziert | ❌ | Keine springdoc-Dependency, keine API-Dokumentation generiert. |

---

## 6. Architekturentscheidungen (Abweichungen)

| ADR | Entscheidung | Abweichung |
|-----|-------------|------------|
| ADR-02 | Frontend + Backend in separaten Docker-Containern | ✅ SAD korrigiert — entspricht der Implementierung. |
| ADR-06 | Flyway für DB-Migrationen | ✅ Umgesetzt wie spezifiziert. |
| ADR-07 | Gradle Multi-Module statt Spring Modulith | ✅ Umgesetzt. `scoring-module` fehlt als eigenständiges Modul. |
| ADR-08 | Angular Material + ngx-charts | ⚠️ Angular Material vollständig genutzt. **ngx-charts nicht installiert** (keine Statistiken implementiert). |
| ADR-09 | `angular-oauth2-oidc` statt `keycloak-angular` | ✅ Umgesetzt wie spezifiziert. |

---

## 7. Infrastruktur / Deployment

| Anforderung | Status | Bemerkung |
|-------------|--------|-----------|
| Docker Compose Setup | ✅ | Alle 4 Services: db, keycloak, backend, frontend (nginx). |
| Keycloak Realm Import | ✅ | `realm-export.json` wird beim Start automatisch importiert. |
| HTTPS / TLS | ✅ | Selbstsigniertes Zertifikat für Keycloak (8443), Backend (8080), Frontend (4200). |
| PostgreSQL Backup (NFA-04) | ❌ | Kein automatisierter Backup-Mechanismus konfiguriert. |
| Reverse Proxy / WAF (Produktionsempfehlung) | ❌ | Nur für lokale Entwicklung ausgelegt. Kein produktiver Ingress. |

---

## 8. Zusammenfassung

### Umgesetzt (V1 MVP)

- Spielerverwaltung (CRUD + Lifecycle)
- Match-Lifecycle (Erstellen, Spielstand erfassen, Beenden)
- OAuth2 PKCE Authentifizierung (Frontend + Backend)
- Flyway-Datenbankmigrationen
- Angular-Frontend mit Material Design
- Docker Compose Deployment

### Kritische Lücken (V1 nicht vollständig)

| Priorität | Gap |
|-----------|-----|
| **Hoch** | `statistics-module` (FA-08) — komplett fehlend |
| **Hoch** | `POINT`-Entität — keine einzelnen Punkte persistiert; Statistiken später nicht berechenbar |
| **Hoch** | `POST /api/matches/{id}/points` mit vollständiger Punkt-Struktur (FA-06) |
| **Mittel** | Spieler-Suche per API mit Query-Parametern + Pagination (FA-04) |
| **Mittel** | `MATCH_SET`- und `MATCH_STATS`-Entitäten fehlen |
| **Mittel** | Fehler-Response-Format (kein `error-code`, `timestamp`) |
| **Niedrig** | OpenAPI/Swagger-Dokumentation |
| **Niedrig** | Frontend-Unit-Tests (Jasmine/Karma) |
| **Niedrig** | PostgreSQL-Backup-Automatisierung (NFA-04) |
