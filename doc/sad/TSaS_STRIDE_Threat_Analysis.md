# TSaS — STRIDE Threat-Analyse (App & Deployment)

**Stand:** 2026-06-20
**Scope:** Spring-Boot-Backend (`backend/`), Docker-Compose-Deployment (`docker/`), Keycloak-Realm-Config.
**Methodik:** STRIDE (Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, Elevation of Privilege).

---

## 1. Systemüberblick

| Komponente | Adresse (lokal) | Funktion |
|---|---|---|
| Angular SPA | `https://localhost:4200` (Dev) · `http://nginx:80` (Container) | Frontend, OAuth2-Public-Client mit PKCE |
| Spring-Boot-Backend | `https://localhost:8080` (Dev) · `http://backend:8080` (Container) | REST-API, OAuth2-Resource-Server |
| PostgreSQL | `localhost:5432` | Domänenpersistenz |
| Keycloak | `https://localhost:8443`, `http://localhost:18080` | OIDC-IdP, Realm `tsas` |
| OpenAI | `https://api.openai.com` | LLM-Backend für Match-Analyse |

**Trust Boundaries:** Browser ↔ nginx · nginx ↔ Backend · Backend ↔ DB · Backend ↔ Keycloak · Backend ↔ OpenAI.

---

## 2. Befunde nach STRIDE

### 2.1 S — Spoofing

| # | Befund | Risiko | Quelle |
|---|---|---|---|
| S1 | ~~**Keine `aud`-Prüfung im JwtDecoder.** Nur `issuer` wird validiert.~~ **MITIGATED (TEN-56)** — `JwtClaimValidator("aud", a -> a != null && a.contains("tsas-frontend"))` ist via `DelegatingOAuth2TokenValidator` zusätzlich registriert; Erwartungs-Audience konfigurierbar über `tsas.security.expected-audience`. Keycloak-Realm hat den passenden `oidc-audience-mapper` auf dem `tsas-frontend`-Client. | erledigt | `auth-module/.../SecurityConfig.java`, `docker/keycloak/realm-export.json` |
| S2 | **Self-Registration offen** (`registrationAllowed: true`), keine E-Mail-Verifizierung, kein Admin-Approval. | Hoch | `docker/keycloak/realm-export.json:3` |
| S3 | ~~**Keycloak-Admin `admin/admin`** als Default.~~ **MITIGATED (TEN-58)** im Prod-Overlay: `KC_BOOTSTRAP_ADMIN_USERNAME`/`KC_BOOTSTRAP_ADMIN_PASSWORD` sind `${VAR:?…}`-required, kein Default. Dev behält `admin/admin` für lokales Debugging. | erledigt (Prod-Overlay) | `docker/compose.prod.yml` |
| S4 | ~~**Postgres-Default-Creds `tsas/tsas/tsas`** hart kodiert; Port 5432 auf Host exponiert.~~ **MITIGATED (TEN-58)** im Prod-Overlay: `DB_USERNAME`/`DB_PASSWORD` required (`${VAR:?…}`); kein Host-Port; Postgres nur im Compose-Netz. Dev behält Defaults; `docker/db/compose.yaml` exponiert 5432 weiterhin für lokalen `bootRun` (kein Prod-Pfad). | erledigt (Prod-Overlay) | `docker/compose.prod.yml`, `docker/db/init/` |
| S5 | **Keine User-Bindung der Domänenobjekte.** `Player`/`Match`/`Point` haben keinen `owner`/`userId`. | Hoch | `PlayerController`, `MatchController` |

### 2.2 T — Tampering

| # | Befund | Risiko | Quelle |
|---|---|---|---|
| T1 | ~~**Keine TLS-Terminierung im Container-Deployment.** nginx auf `:80`, Backend auf `:8080` (HTTP).~~ **MITIGATED (TEN-58)** im Prod-Overlay: `docker/frontend/nginx.prod.conf` terminiert TLS auf Container-Port 8443 (Host 443) mit Mozilla-„intermediate"-Cipher-Profile + HSTS; Container-Port 8080 liefert nur HTTP-301-Redirects. Cert-Mount via `${TLS_CERT_DIR}`. | erledigt (Prod-Overlay) | `docker/compose.prod.yml`, `docker/frontend/nginx.prod.conf` |
| T2 | **`PUT /api/matches/{id}/score`** lässt beliebige Score-Felder überschreiben, kein Owner-Check. | Hoch | `MatchController.java:120-138` |
| T3 | **`POST /api/matches/{id}/end/walkover`** beendet Match beliebig, kein Owner-Check. | Mittel | `MatchController.java:146-150` |
| T4 | **Postgres-Daten unverschlüsselt** im Bind-Mount `../volume/postgres`. | Mittel | `docker/db/compose.yaml:12` |
| T5 | **Self-signed Cert-Material im Repo** (`docker/keycloak/certs/`). Private Key gitignored, Public-Teil committet — Dev-Bequemlichkeit. | Niedrig | `docker/keycloak/certs/` |

### 2.3 R — Repudiation

| # | Befund | Risiko | Quelle |
|---|---|---|---|
| R1 | **Kein Audit-Log auf Schreiboperationen.** `recordPoint`, `setScore`, `endMatch`, `deletePlayer`, `deactivatePlayer` ohne `who/when`-Trace. | Mittel | Controller-Layer |
| R2 | **Kein `created_by`/`updated_by`** auf Entities (folgt aus S5). | Mittel | Persistence-Layer |
| R3 | **Keine Korrelations-IDs** im Log; `show-sql: true` im local-Profil ersetzt kein Audit-Logging. | Niedrig | `application-local.yml:8` |

### 2.4 I — Information Disclosure

| # | Befund | Risiko | Quelle |
|---|---|---|---|
| I1 | **Vollständige Datenfreigabe quer über Nutzer.** `GET /api/players`, `GET /api/matches` liefern alle Daten an jeden Authentifizierten. | Kritisch | `PlayerController.java:69-78`, `MatchController.java:72-75` |
| I2 | **Geburtsdatum / Nationalität / Ranking** im `PlayerResponse`, kein Filter pro Abrufer (DSGVO). | Hoch | `PlayerController.java:62-78` |
| I3 | ~~**`OPENAI_API_KEY` als Klartext-Env-Var**, kein Secret-Manager, kein Rotation-Konzept.~~ **PARTIAL (TEN-58)** im Prod-Overlay: `OPENAI_API_KEY` ist required ohne Default, wird über `.env`-Datei (mode 600) versorgt. Volle Secret-Manager-Integration (Vault / AWS Secrets / Docker Secrets via File-Mount + Spring `EnvironmentPostProcessor`) bleibt Folge-Ticket; Rotation aktuell manuell. | teilweise erledigt | `docker/compose.prod.yml`, `docker/.env.prod.example` |
| I4 | **JWK-Fetch über HTTP** (`http://keycloak:8080/...`) — innerhalb Docker-Bridge vertretbar, aber HTTP als Default zementiert. | Mittel | `docker/compose.yml:50` |
| I5 | **Keine globale `@ControllerAdvice`** für sanitisierte Fehler-Responses (Stack-Trace-Leak-Risiko). | Niedrig–Mittel | Default Spring |
| I6 | `server.error.include-message/stacktrace` nicht explizit gesetzt. | Niedrig | `application*.yml` |

### 2.5 D — Denial of Service

| # | Befund | Risiko | Quelle |
|---|---|---|---|
| D1 | **Keine Rate-Limits.** `POST /api/matches/{id}/analysis` triggert kostenpflichtige OpenAI-Calls → Cost-DoS. | Hoch (finanziell) | `nginx.conf`, `MatchAnalysisController.java:33-40` |
| D2 | **Self-Registration ohne CAPTCHA/Quota** kombiniert mit D1. | Hoch | siehe S2 + D1 |
| D3 | **Kein Request-Size-Limit**; `RecordPointRequest.remark` (Freitext) ungebremst. | Mittel | `RecordPointRequest` |
| D4 | `POST /api/players` ohne Throttle / Duplicate-Check (DB-Bloat). | Mittel | `PlayerController.java:45-60` |
| D5 | Container ohne `mem_limit`/`cpus`-Begrenzung. | Niedrig | beide compose-Files |
| D6 | ~~**Keycloak `start-dev`** mit H2-Volume — nicht produktionstauglich.~~ **MITIGATED (TEN-58)** im Prod-Overlay: `start --db=postgres --db-url-host=db --db-url-database=${KC_DB_NAME}` mit eigener Keycloak-DB (Bootstrap via `docker/db/init/01-create-keycloak-db.sh`); `--hostname=${KC_HOSTNAME}` + `--proxy-headers=xforwarded` für TLS-Termination am Frontend. | erledigt (Prod-Overlay) | `docker/compose.prod.yml`, `docker/db/init/` |

### 2.6 E — Elevation of Privilege

| # | Befund | Risiko | Quelle |
|---|---|---|---|
| E1 | **Autorisierung = `authenticated()`**, keine Rollen, kein `@PreAuthorize`. Jeder eingeloggte User kann jeden Endpoint nutzen. | **Kritisch** | `SecurityConfig.java:32-35`, alle Controller |
| E2 | ~~**`test`-Profil = `permitAll`.** Kein Startup-Guard gegen „test in Prod".~~ **MITIGATED (TEN-57)** — `TestProfileSecurityConfig` ist aus `main` entfernt und lebt nur noch in `auth-module/src/testFixtures/` (nicht im Boot-Jar). Zusätzlich `TestProfileGuard` als `EnvironmentPostProcessor`: bricht den Boot ab, wenn `test` mit `prod`/`production`/`docker` kombiniert ist oder `spring.datasource.url` nicht H2-in-memory ist. 8 Unit-Tests + `@PostConstruct`-Warning-Log bei aktivem `permitAll`. | erledigt | `auth-module/.../TestProfileGuard.java`, `auth-module/src/testFixtures/.../TestProfileSecurityConfig.java` |
| E3 | **Wildcard-`redirectUris`** im Keycloak-Client (`http://localhost:4200/*`). Lokal OK, in Prod Open-Redirect-Risiko. | Niedrig (lokal) / Hoch (Prod) | `realm-export.json:22-25` |
| E4 | **CORS** `allowedHeaders=*` + `allowCredentials=true` + kein Aud-Check → ein kompromittierter Realm-Client bekommt Vollzugriff. | Mittel | `CorsConfig.java:25-36` |
| E5 | **JwtAuthenticationConverter** nicht angepasst → Keycloak-Rollen (`realm_access.roles`) werden nicht zu Spring-Authorities. Latentes Risiko bei späterem `@PreAuthorize`. | Mittel | `SecurityConfig.java:36` |
| E6 | Actuator `metrics` (auth-pflichtig, OK) ohne expliziten Role-Guard. | Niedrig | `application.yml:52-58` |

---

## 3. Priorisierte Top-5 Sofortmaßnahmen

1. **Owner-Bindung & RBAC einführen** (E1, I1, T2, S5) — `created_by` (sub-Claim) auf `Player`, `Match`, `Point`; Use-Cases filtern per User; optional Keycloak-Rolle `COACH`/`ADMIN` + `JwtAuthenticationConverter`. → **[TEN-55](https://linear.app/tennis-score-and-statistic/issue/TEN-55)**
2. **JWT-Audience prüfen** (S1) — `JwtClaimValidator("aud", ...)` zusätzlich registrieren. → **[TEN-56](https://linear.app/tennis-score-and-statistic/issue/TEN-56)**
3. **Test-Profil-Guard** (E2) — Bean-Init-Fail, wenn `test` parallel zu `prod`/`docker` aktiv; oder Profil nur in `src/test/...`. → **[TEN-57](https://linear.app/tennis-score-and-statistic/issue/TEN-57)**
4. **Cost-/Rate-Limit für AI** (D1, D2) — Bucket4j/Resilience4j auf `POST /api/matches/{id}/analysis`; Self-Registration auf `verifyEmail: true` / Admin-Approval. → **[TEN-64](https://linear.app/tennis-score-and-statistic/issue/TEN-64)**
5. **TLS & Secrets im Deployment** (T1, S3, S4, I3) — nginx auf 443 mit echtem Zert; Secrets aus Compose-Defaults entfernen (Vault/.env); Keycloak produktiv mit `start` + externer DB; Postgres-Port nicht öffentlich. → **[TEN-58](https://linear.app/tennis-score-and-statistic/issue/TEN-58)**

## 4. Weitere Empfehlungen

- **Audit-Logging** (R1, R2): `@EntityListeners(AuditingEntityListener.class)` mit `AuditorAware<UUID>` aus JWT-`sub`. → **[TEN-59](https://linear.app/tennis-score-and-statistic/issue/TEN-59)**
- **Bean-Validation auf DTOs**: `@Size`-Limits (z. B. `remark`), Enum-Coercion sauber kapseln, Whitelist für Walkover-Werte. → **[TEN-60](https://linear.app/tennis-score-and-statistic/issue/TEN-60)**
- **Globale `@ControllerAdvice`** für `problem+json`-Antworten (I5). → **[TEN-61](https://linear.app/tennis-score-and-statistic/issue/TEN-61)**
- **CSP/HSTS-Header** im nginx (T1). → **[TEN-62](https://linear.app/tennis-score-and-statistic/issue/TEN-62)**
- **Container-Hardening**: non-root User in beiden Dockerfiles, `--read-only` / tmpfs, `cap-drop ALL`. → **[TEN-63](https://linear.app/tennis-score-and-statistic/issue/TEN-63)**

## 5. Bestätigte Stärken (nicht problematisch)

- CSRF-Disable korrekt für stateless JWT-API.
- PKCE S256 im Public-Client.
- `ddl-auto: validate` in Prod.
- `open-in-view: false`.
- Actuator-Health `show-details: when-authorized`.
