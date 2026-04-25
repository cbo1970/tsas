# TSaS – Tennis Score and Statistic

Web application for tennis match tracking and statistics.

## Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.4.x (Java 21) |
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

### 1. Keycloak starten

```bash
cd docker
podman compose up keycloak -d
```

Keycloak ist unter **`https://localhost:8443`** erreichbar.

**Admin-Konsole:** `https://localhost:8443/admin` → Login: `admin` / `admin`

Der Realm `tsas` wird beim ersten Start automatisch importiert.

> Keycloak ist zusätzlich über HTTP auf `http://localhost:18080` erreichbar (wird intern vom Backend für den JWKS-Abruf verwendet).

### 2. Backend starten

```bash
cd backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew bootRun --args='--spring.profiles.active=local'
```

Backend läuft auf **`https://localhost:8080`**.

### 3. Frontend starten

```bash
cd frontend
npm start
```

Frontend läuft auf **`https://localhost:4200`**.

---

## Spring Profile

| Profil | Datasource | Security |
|--------|-----------|---------|
| _(kein / default)_ | PostgreSQL | JWT (Keycloak erforderlich) |
| `local` | PostgreSQL | JWT (Keycloak erforderlich) |
| `test` | H2 in-memory | permitAll (kein Keycloak) |

Das `test`-Profil wird ausschliesslich von der automatisierten Test-Suite verwendet (Testcontainers + Smoke-Test).

---

## Tests ausführen

```bash
cd backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test
```

Integration Tests nutzen Testcontainers (PostgreSQL). Kein laufendes Keycloak erforderlich — JWT-Validierung wird via Spring Security Test gemockt.

---

## Vollständiges Deployment via Compose

Alle Services als Container starten (Frontend + Backend + Keycloak + PostgreSQL):

```bash
cd docker
podman compose up -d
```

| Service | URL |
|---------|-----|
| Keycloak | `https://localhost:8443` (Admin: `https://localhost:8443/admin`) |
| Keycloak HTTP (intern) | `http://localhost:18080` |
| Backend | `https://localhost:8080` |
| Frontend | `https://localhost:4200` |
| PostgreSQL | `localhost:5432` |

Zum Stoppen:

```bash
podman compose down
```

### Konfiguration

Zugangsdaten können per `.env`-Datei im `docker/`-Verzeichnis überschrieben werden:

```env
DB_NAME=tsas
DB_USERNAME=tsas
DB_PASSWORD=geheimesPasswort
KC_ADMIN_PASSWORD=adminPasswort
```
