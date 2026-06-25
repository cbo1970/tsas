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

### 2. Keycloak + Mailhog starten

```bash
podman compose -f docker/compose.yml up -d keycloak mailhog
```

Keycloak ist unter **`https://localhost:8443`** erreichbar.

**Admin-Konsole:** `https://localhost:8443/admin` → Login: `admin` / `admin`

Der Realm `tsas` wird beim ersten Start automatisch importiert.

> Keycloak ist zusätzlich über HTTP auf `http://localhost:18080` erreichbar (wird intern vom Backend für den JWKS-Abruf verwendet).

**Mailhog (Dev-SMTP-Sink + Inbox-UI)** wird im selben Befehl gestartet — Keycloak schickt Verify-E-Mails an `mailhog:1025` (Service-Discovery im Compose-Netz, kein Host-Port nötig). Die Inbox ist im Browser unter **`http://localhost:8025`** sichtbar. Backend/Frontend müssen dafür **nicht** in Podman laufen: Keycloak in Podman → Mailhog in Podman → Inbox auf dem Host. Bei Bedarf den SMTP-Port auch auf den Host mappen via `KC_SMTP_*`-Override (siehe Konfigurations-Sektion unten).

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

Der Keycloak-Realm hat `verifyEmail: true` und ist standardmässig gegen den lokalen **Mailhog**-Container konfiguriert (UI auf `http://localhost:8025`). Für Prod-Deployments lassen sich die SMTP-Felder über folgende `.env`-Variablen überschreiben — die Platzhalter `${KC_SMTP_*:default}` im `realm-export.json` werden beim Realm-Import durch Keycloak ersetzt:

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

## Production Deployment (TEN-58)

Für eine produktionsnahe Auslieferung gibt es einen Compose-Overlay, der TLS-Terminierung, Secret-Hygiene und Hardening durchsetzt. Er fügt sich auf den Basis-Stack `docker/compose.yml` auf:

```bash
podman compose -f docker/compose.yml -f docker/compose.prod.yml up -d --build
```

Vor dem Start eine `.env`-Datei neben `docker/compose.yml` anlegen — Vorlage:

```bash
cp docker/.env.prod.example docker/.env
$EDITOR docker/.env  # echte Credentials eintragen
```

Pflichtfelder (alle anderen `${VAR:?…}`-Referenzen in `compose.prod.yml` brechen den Start ab, wenn nicht gesetzt):

| Variable | Zweck |
|---|---|
| `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | Postgres-DB für die App |
| `KC_DB_NAME`, `KC_DB_USERNAME`, `KC_DB_PASSWORD` | Eigene Postgres-DB für Keycloak — Bootstrap-Script unter `docker/db/init/01-create-keycloak-db.sh` legt sie beim ersten Start an |
| `KC_ADMIN_USERNAME`, `KC_ADMIN_PASSWORD` | Keycloak-Bootstrap-Admin (nur Erstinstallation) |
| `KC_HOSTNAME` | Öffentlicher FQDN, muss zur TLS-Cert-SAN passen |
| `KEYCLOAK_ISSUER_URI` | z. B. `https://auth.tsas.example.com/realms/tsas` |
| `KC_SMTP_*` | Echtes SMTP-Backend (Sendgrid, AWS SES, …) |
| `OPENAI_API_KEY` | LLM-Provider-Key (keine Defaults mehr) |
| `TLS_CERT_DIR` | Host-Pfad mit `tls.crt` + `tls.key` (z. B. Let's-Encrypt-`fullchain.pem` + `privkey.pem`) |

### Was der Overlay verändert

| Komponente | Dev (`compose.yml`) | Prod (`+ compose.prod.yml`) |
|---|---|---|
| **nginx** | HTTP auf Container 8080, Host 80 | TLS auf 8443 (Host 443); Port 80 redirected 301 → HTTPS; HSTS, CSP, etc. weiterhin gesetzt |
| **Keycloak** | `start-dev --import-realm` mit H2-Volume | `start --import-realm` mit Postgres-Backend, `--hostname=${KC_HOSTNAME}`, `--proxy-headers=xforwarded` |
| **Postgres** | Default-Creds `tsas/tsas` (`:-tsas`) | `${VAR:?required}` — Start bricht ab, wenn Credentials fehlen |
| **Keycloak-Admin** | Default `admin/admin` | `KC_BOOTSTRAP_ADMIN_*` aus `.env`, keine Defaults |
| **OPENAI_API_KEY** | Default leer | required, kein Default |
| **Mailhog** | Dev-SMTP-Sink | entfernt — echtes SMTP via `KC_SMTP_*` |
| **Backend-Port** | Host 8080 exponiert | nur intern; Zugriff via nginx-Reverse-Proxy |
| **Postgres-Port** | nur intern (schon im Dev so) | nur intern |

### TLS-Zertifikate bereitstellen

Der Overlay mountet `${TLS_CERT_DIR}` read-only auf `/etc/nginx/certs/`. Erwartet werden zwei Dateien:

- `tls.crt` — Vollständige Cert-Kette (z. B. Let's-Encrypt-`fullchain.pem`)
- `tls.key` — Privater Schlüssel (z. B. Let's-Encrypt-`privkey.pem`)

Beispiel-Hooks für `certbot` (Let's Encrypt) — als Renew-Hook in `/etc/letsencrypt/renewal-hooks/deploy/tsas.sh`:

```bash
#!/bin/sh
install -m 0644 "$RENEWED_LINEAGE/fullchain.pem" /etc/tsas/tls/tls.crt
install -m 0600 "$RENEWED_LINEAGE/privkey.pem"   /etc/tsas/tls/tls.key
podman compose -f /opt/tsas/docker/compose.yml -f /opt/tsas/docker/compose.prod.yml exec frontend nginx -s reload
```

### Smoke-Verifikation

```bash
# TLS-Handshake + Server-Cert prüfen
openssl s_client -connect ${KC_HOSTNAME}:443 -servername ${KC_HOSTNAME} < /dev/null

# HTTP → HTTPS Redirect
curl -sI http://${KC_HOSTNAME}/ | grep -i location

# Postgres NICHT exponiert
nc -zv ${KC_HOSTNAME} 5432   # Verbindung muss scheitern
```

### Lokaler Prod-Smoke (ohne DNS / öffentliche IP)

Den Overlay kann man auch komplett lokal ausprobieren — die selbstsignierten Dev-Certs unter `docker/keycloak/certs/` reichen aus, `KC_HOSTNAME=localhost` ersetzt den FQDN. Der Browser warnt dann wegen des Self-Signed-Certs; das ist für einen Smoke-Test akzeptabel.

```bash
# 1. Cert-Verzeichnis vorbereiten (Dev-Certs wiederverwenden)
mkdir -p /tmp/tsas-tls
cp docker/keycloak/certs/localhost.pem      /tmp/tsas-tls/tls.crt
cp docker/keycloak/certs/localhost-key.pem  /tmp/tsas-tls/tls.key

# 2. Minimal-Env anlegen (Demo-Werte; echte Prod braucht starke Credentials)
cat > docker/.env <<'EOF'
DB_NAME=tsas
DB_USERNAME=tsas_app
DB_PASSWORD=demo_pw_change_me
KC_DB_NAME=keycloak
KC_DB_USERNAME=keycloak
KC_DB_PASSWORD=demo_pw_change_me
KC_ADMIN_USERNAME=admin
KC_ADMIN_PASSWORD=demo_pw_change_me
KC_HOSTNAME=localhost
KEYCLOAK_ISSUER_URI=https://localhost/realms/tsas
KC_SMTP_HOST=mailhog
KC_SMTP_PORT=1025
KC_SMTP_FROM=noreply@tsas.local
KC_SMTP_STARTTLS=false
KC_SMTP_AUTH=false
KC_SMTP_USER=
KC_SMTP_PASSWORD=
OPENAI_API_KEY=sk-test
TLS_CERT_DIR=/tmp/tsas-tls
EOF

# 3. Stack hochfahren
podman compose -f docker/compose.yml -f docker/compose.prod.yml up -d --build

# 4. Smoke
curl -sIk https://localhost/ | head -1               # → HTTP/2 200
curl -sI  http://localhost/  | grep -i location      # → Location: https://localhost/
openssl s_client -connect localhost:443 -servername localhost < /dev/null 2>/dev/null \
  | openssl x509 -noout -subject                     # → CN=localhost
```

> **Hinweis:** Mailhog ist im Prod-Overlay entfernt — die Verify-Email-Funktion läuft daher in diesem Smoke nicht. Für einen Verify-Mail-Test stattdessen den Dev-Compose (`compose.yml` allein) verwenden oder einen lokalen MTA bereitstellen.

> **Cleanup:** `podman compose -f docker/compose.yml -f docker/compose.prod.yml down && rm -rf /tmp/tsas-tls docker/.env`.
