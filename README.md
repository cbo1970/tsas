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

### 1. Keycloak starten

```bash
cd docker
podman compose up keycloak -d
```

Keycloak ist unter `http://localhost:8180` erreichbar (Admin: `admin` / `admin`).
Der Realm `tsas` wird beim ersten Start automatisch importiert.

### 2. Backend starten

```bash
cd backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew bootRun --args='--spring.profiles.active=local'
```

Backend läuft auf `http://localhost:8080`.

### 3. Frontend starten

```bash
cd frontend
npm start
```

Frontend läuft auf `http://localhost:4200`.

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
| Keycloak | `http://localhost:8180` |
| Backend | `http://localhost:8080` |
| Frontend | `http://localhost:80` |
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
