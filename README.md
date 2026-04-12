# tsas

## Docker / Podman

Die Docker-Konfiguration befindet sich im Verzeichnis `docker/`.

### Vollständiges Deployment (Frontend + Backend + Datenbank)

Alle drei Services werden als Container gestartet:

```bash
cd docker
docker-compose up --build
```

| Service  | URL                       |
|----------|---------------------------|
| Frontend | http://localhost          |
| Backend  | http://localhost:8080     |

Zum Stoppen:

```bash
docker-compose down
```

### Nur Datenbank (lokale Entwicklung)

Nur PostgreSQL im Container starten, Frontend und Backend laufen lokal:

```bash
cd docker/db
docker-compose up -d
```

PostgreSQL ist dann unter `localhost:5432` erreichbar.

### Konfiguration

Zugangsdaten und Datenbankname können per `.env`-Datei im jeweiligen Verzeichnis überschrieben werden:

```env
DB_NAME=tsas
DB_USERNAME=tsas
DB_PASSWORD=geheimesPasswort
```

