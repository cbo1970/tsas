# TSaS – Tennis Score and Statistic

Web application for tennis match tracking and statistics.

**Version:** 0.1.0 — Releases und Änderungen sind in [`CHANGELOG.md`](CHANGELOG.md) dokumentiert (Keep a Changelog 1.1.0). Die Versionierung folgt [Semantic Versioning 2.0.0](https://semver.org/) — solange `0.x`, gelten MINOR-Erhöhungen als potenziell breaking (siehe ADR-14 im SAD).

## Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 4.0.6 (Java 25) |
| Auth | Keycloak 26 (OAuth2 / PKCE) |
| Frontend | Angular 21 |
| Database | PostgreSQL 16 |

---

## Lokale Entwicklung

### Voraussetzungen

- Java 25 (`JAVA_HOME=/opt/java/jdk-25.0.1`)
- Node.js / npm
- Podman oder Docker mit Compose

### 0. Zertifikat generieren (einmalig)

Für HTTPS wird ein selbstsigniertes Zertifikat benötigt:

```bash
cd docker/keycloak/certs
openssl req -x509 -newkey rsa:4096 -keyout localhost-key.pem -out localhost.pem \
  -days 365 -nodes -subj "/CN=localhost" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
```

Das Zertifikat muss einmalig dem Browser / Betriebssystem als vertrauenswürdig hinzugefügt werden.

### 1. Datenbank starten

```bash
podman compose -f docker/db/compose.yaml up -d
```

PostgreSQL läuft auf `localhost:5432`.

### 2. Keycloak starten

```bash
podman compose -f docker/compose.yml up keycloak -d
```

Keycloak ist unter **`https://localhost:8443`** erreichbar.

**Admin-Konsole:** `https://localhost:8443/admin` → Login: `admin` / `admin`

Der Realm `tsas` wird beim ersten Start automatisch importiert.

> Keycloak ist zusätzlich über HTTP auf `http://localhost:18080` erreichbar (wird intern vom Backend für den JWKS-Abruf verwendet).

### 3. Backend starten

```bash
cd backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew bootRun --args='--spring.profiles.active=local'
```

Backend läuft auf **`https://localhost:8080`**.

### 4. Frontend starten

```bash
cd frontend
npm start
```

Frontend (UI) läuft auf **`https://localhost:4200`**.

---

## Spring Profile

| Profil | Datasource | Security |
|--------|-----------|---------|
| _(kein / default)_ | PostgreSQL | JWT (Keycloak erforderlich) |
| `local` | PostgreSQL | JWT (Keycloak erforderlich) |
| `test` | H2 in-memory | permitAll (kein Keycloak) |

Das `test`-Profil wird ausschliesslich von der automatisierten Test-Suite verwendet.

---

## Tests ausführen

### Backend

```bash
cd backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test
```

Integration Tests nutzen Testcontainers (PostgreSQL) — ein Container-Runtime (Docker/Podman) muss laufen. Kein laufendes Keycloak erforderlich; JWT-Validierung wird via Spring Security Test gemockt.

#### Coverage (JaCoCo)

Die Coverage wird modulübergreifend aggregiert (Integrationstests liegen im `app`-Modul, decken aber Klassen aller Module ab). Ein **Coverage-Gate** ist in `check` eingehängt und bricht den Build unter **85 % Line / 70 % Branch**:

```bash
# Aggregierter HTML/XML/CSV-Report -> backend/build/reports/jacoco/jacocoRootReport/
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew jacocoRootReport

# Nur das Gate (läuft auch als Teil von `check`)
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew jacocoRootCoverageVerification
```

> Auf macOS mit rootless Podman zusätzlich voranstellen: `DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true`. Die Schwellen liegen in `backend/build.gradle.kts` (`violationRules`).

### Frontend

```bash
cd frontend
npx ng test --no-watch       # Unit-Tests (Vitest)
npx cypress run --component   # Komponententests (Cypress)
```

---

## Continuous Integration

GitHub Actions führt bei jedem Push und Pull Request auf `develop` und `main` zwei Workflows aus:

| Workflow | Datei | Inhalt |
|----------|-------|--------|
| **Backend CI** | `.github/workflows/backend-ci.yml` | `./gradlew check` (Tests + JaCoCo-Coverage-Gate) auf Ubuntu / JDK 25; lädt den aggregierten Coverage-Report als Artifact hoch. |
| **Frontend CI** | `.github/workflows/frontend-ci.yml` | `ng build` + Vitest-Unit-Tests + Cypress-Komponententests auf Node 22. |

Beide Checks sind auf `develop` und `main` als **required status checks** konfiguriert (Branch Protection) — ein PR kann erst gemergt werden, wenn beide grün sind. Auf `ubuntu-latest` ist Docker nativ vorhanden, sodass Testcontainers ohne Zusatzkonfiguration läuft.

---

## Vollständiges Deployment via Compose

Alle Services als Container starten (Frontend + Backend + Keycloak + PostgreSQL):

```bash
podman compose -f docker/compose.yml up -d
```

| Service | URL |
|---------|-----|
| **Frontend (UI)** | **`http://localhost`** |
| Backend | `http://localhost:8080` |
| Keycloak | `https://localhost:8443` (Admin: `https://localhost:8443/admin`) |
| Keycloak HTTP (intern) | `http://localhost:18080` |
| Mailhog (Dev-SMTP + UI) | `http://localhost:8025` |
| PostgreSQL | nur im Docker-Netzwerk erreichbar (kein Host-Port) |

Zum Stoppen:

```bash
podman compose -f docker/compose.yml down
```

### Konfiguration

Zugangsdaten können per `.env`-Datei im `docker/`-Verzeichnis überschrieben werden:

```env
DB_NAME=tsas
DB_USERNAME=tsas
DB_PASSWORD=geheimesPasswort
KC_ADMIN_PASSWORD=adminPasswort
```

#### Keycloak SMTP (E-Mail-Verifizierung — TEN-64)

Der Keycloak-Realm hat `verifyEmail: true` und ist standardmäßig gegen den lokalen **Mailhog**-Container konfiguriert (UI auf `http://localhost:8025`). Für Prod-Deployments lassen sich die SMTP-Felder über folgende `.env`-Variablen überschreiben — die Platzhalter `${KC_SMTP_*:default}` im `realm-export.json` werden beim Realm-Import durch Keycloak ersetzt:

```env
# Beispiel: Sendgrid
KC_SMTP_HOST=smtp.sendgrid.net
KC_SMTP_PORT=587
KC_SMTP_FROM=no-reply@tsas.app
KC_SMTP_FROM_NAME=TSaS
KC_SMTP_REPLY_TO=no-reply@tsas.app
KC_SMTP_ENVELOPE_FROM=no-reply@tsas.app
KC_SMTP_STARTTLS=true
KC_SMTP_SSL=false
KC_SMTP_AUTH=true
KC_SMTP_USER=apikey
KC_SMTP_PASSWORD=SG.xxxxxxxxxxxxxxxxxx
```

Ohne diese Variablen läuft der Stack mit Mailhog-Defaults — keine externe SMTP-Verbindung, alle Mails landen im Browser unter `http://localhost:8025`.

> **Wichtig:** Der Realm-Import erfolgt nur, wenn das Keycloak-`keycloak_data`-Volume frisch ist (`IGNORE_EXISTING`-Strategie). Änderungen an den SMTP-Variablen greifen erst nach `podman compose down && podman volume rm docker_keycloak_data && podman compose up -d` oder per Admin-API-PATCH zur Laufzeit.
