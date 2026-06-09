# Betriebsvorschlag: TSaS auf europäischer Infrastruktur (CLI-betrieben)

> Vorschlag für den produktiven Betrieb von TSaS (Angular-SPA + Spring-Boot-Backend +
> Keycloak + PostgreSQL) auf **europäischer Infrastruktur**, vollständig **per CLI**
> provisionier- und betreibbar. Aufbauend auf dem bestehenden Docker-/Compose-Setup.
>
> Diagramm: [`TSaS_Cloud_Deployment.drawio`](./TSaS_Cloud_Deployment.drawio)

## 1. Ziele & Randbedingungen

| Anforderung | Umsetzung |
|---|---|
| **Europäische Infrastruktur** (Datenresidenz / DSGVO) | Hetzner Cloud, Region **Nürnberg / Falkenstein (DE)** oder **Helsinki (FI)** — alle EU/EWR. Anbieter ist ein deutsches Unternehmen. Identitäts- und Matchdaten verlassen die EU nicht. |
| **Betrieb per CLI** | `hcloud` CLI für die gesamte Infrastruktur (Server, Netze, Firewall, Load Balancer, Volumes); App-Lebenszyklus via Docker Compose über SSH / Docker-Context. Optional Terraform (Provider `hetznercloud/hcloud`) für reproduzierbares IaC. |
| **Zusätzliche Betriebs-Services** | Cloud Firewall, Managed Load Balancer (= Application Gateway), Reverse Proxy (Traefik), privates Netzwerk, Backups (EU-Object-Storage), Secrets-Management, optionales Monitoring. |
| **Basis** | Wiederverwendung des vorhandenen `docker/compose.yml` (frontend, backend, keycloak, db). |

## 2. Provider-Empfehlung

**Empfohlen: Hetzner Cloud.** Begründung: EU-souverän (DE), sehr gutes Preis-/Leistungsverhältnis für die Zielskala (≤ 100 gleichzeitige Nutzer, 95 % Verfügbarkeit), und – entscheidend – **vollständig CLI-/API-getrieben** (`hcloud`, Terraform-Provider). Cloud Firewall, Load Balancer und private Netze sind nativ verfügbar.

**EU-Alternativen** (gleiche Architektur, andere Service-Namen):

| Anbieter | Land | Managed Load Balancer | Managed PostgreSQL | CLI |
|---|---|---|---|---|
| **Hetzner Cloud** | DE | ✓ | ✗ (selbst betrieben) | `hcloud` |
| OVHcloud | FR | ✓ | ✓ | `ovhai` / API / Terraform |
| Scaleway | FR | ✓ | ✓ (Managed DB) | `scw` |
| STACKIT (Schwarz-Gruppe) | DE | ✓ | ✓ | `stackit` |
| IONOS Cloud | DE | ✓ | ✓ | `ionosctl` |
| Hyperscaler EU-Region / Sovereign Cloud (AWS/Azure/GCP) | EU | ✓ | ✓ | `aws` / `az` / `gcloud` |

> Wenn **Managed PostgreSQL** Priorität hat (weniger DB-Ops), sind **Scaleway, OVHcloud, STACKIT oder IONOS** die bessere Wahl als Hetzner. Die Architektur unten bleibt identisch — nur die „DB-Server VM" wird durch einen Managed-DB-Endpoint ersetzt.

## 3. Zielarchitektur

```
Internet ─▶ Cloud Firewall ─▶ Load Balancer (TLS) ─▶ Traefik (Reverse Proxy) ─┬▶ frontend (SPA)
                                                                              ├▶ backend (API)
                                                                              └▶ keycloak (Auth)
                                                          backend ─(privat)─▶ PostgreSQL
```

| Komponente | Technologie | Zweck |
|---|---|---|
| **Cloud Firewall** | Hetzner Cloud Firewall (stateful) | Erlaubt eingehend nur **443/80** (Welt) und **22/SSH** (nur Admin-IP-Range). Alles andere – insbesondere PostgreSQL – ist von außen unerreichbar. |
| **Load Balancer / Application Gateway** | Hetzner Managed Load Balancer (`lb11`) | Öffentlicher Eintrittspunkt, **TLS-Terminierung**, Health Checks, Verteilung auf App-Targets. Übernimmt die „Application Gateway"-Rolle. |
| **Reverse Proxy** | **Traefik** (Container) | TLS (Let's Encrypt, EU-ACME), Host-/Pfad-Routing: `/ → frontend`, `/api → backend`, `auth.<domain> → keycloak`. Security-Header, Rate-Limiting. |
| **Frontend** | Nginx + Angular-SPA (Container) | Auslieferung der statischen SPA-Artefakte. |
| **Backend** | Spring Boot :8080 (Container) | REST-API. Erreicht DB & Keycloak nur über das **private Netz**. |
| **Identity** | Keycloak :8443 (Container) | OAuth2/OIDC, Realm `tsas` (Import wie bisher). Identitätsdaten bleiben in der EU. |
| **Datenbank** | PostgreSQL 16 (eigene VM, **nur privates Netz**) | Persistenz. Kein öffentliches Interface. Alternativ Managed-EU-DB. |
| **Backups** | Hetzner Storage Box / Object Storage (EU) | Tägliche `pg_dump` (verschlüsselt) + Volume-Snapshots. Deckt NFA-04 (RPO 24 h, RTO 30 min) ab. |
| **Secrets** | SOPS + age (im Repo verschlüsselt) bzw. Docker Secrets / self-hosted Infisical | DB-Passwörter, OPENAI_API_KEY, Keycloak-Admin. Kein Klartext in der VM. |
| **DNS / TLS** | EU-Registrar, A-Record → LB-IP; Let's Encrypt | HTTPS Ende-zu-Ende. |
| **Monitoring (optional)** | Prometheus + Grafana + Loki (self-hosted, EU) | Spring Boot Actuator-Metriken, Logs, Alerting. |
| **CI → Deploy** | GitHub Actions → Image-Registry (EU) → SSH/`hcloud` | Build der Images, Push in EU-Registry, Deploy per CLI. |

## 4. Netzwerk & Sicherheit

- **Zwei Zonen:** öffentlich (LB) ↔ **privates Netz `10.0.0.0/16`** (App-VM, DB-VM, Keycloak). Nur der Load Balancer und SSH haben öffentliche Erreichbarkeit; die DB hat **keine** öffentliche IP.
- **Firewall-Regeln (eingehend):** `443` und `80` (→ Redirect auf 443) aus dem Internet auf die App-Targets; `22` nur aus der Admin-IP-Range; interner Traffic (Backend↔DB :5432, Backend↔Keycloak) ausschließlich über das private Netz.
- **TLS:** Browser↔LB und LB↔Traefik via HTTPS; intern über das private Netz. Erfüllt NFA-02 (TLS ≥ 1.2).
- **DSGVO / Datenresidenz:** Compute, DB, Backups, Identitätsdaten und Logs liegen ausschließlich in EU-Rechenzentren eines EU-Anbieters.
- **Härtung:** SSH nur per Key, `fail2ban`, automatische OS-Security-Updates, Docker-Rootless wo möglich.

## 5. Datenfluss (Request-Pfad)

1. Browser → `https://tsas.<domain>` → **Cloud Firewall** (443) → **Load Balancer** (TLS-Terminierung) → **Traefik**.
2. Traefik routet: `/` → **frontend** (SPA), `/api/*` → **backend**, `auth.<domain>` → **Keycloak**.
3. OAuth2-PKCE-Login: Browser ↔ Keycloak (über LB/Traefik).
4. **backend** → PostgreSQL `:5432` und Keycloak-JWKS — beides **nur über das private Netz**.
5. Cron-Job auf der DB-VM → `pg_dump` → **EU-Object-Storage**.

## 6. Betrieb per CLI

**Provisionierung (Auszug `hcloud`):**

```bash
# Privates Netz
hcloud network create --name tsas-net --ip-range 10.0.0.0/16
hcloud network add-subnet tsas-net --network-zone eu-central --type cloud --ip-range 10.0.1.0/24

# Firewall (nur 80/443 weltweit, SSH aus Admin-IP)
hcloud firewall create --name tsas-fw
hcloud firewall add-rule tsas-fw --direction in --protocol tcp --port 443 --source-ips 0.0.0.0/0
hcloud firewall add-rule tsas-fw --direction in --protocol tcp --port 80  --source-ips 0.0.0.0/0
hcloud firewall add-rule tsas-fw --direction in --protocol tcp --port 22  --source-ips <ADMIN_IP>/32

# App- und DB-Server (Docker-Image), ins private Netz, mit Firewall
hcloud server create --name tsas-app --type cpx31 --image docker-ce \
  --location nbg1 --network tsas-net --firewall tsas-fw
hcloud server create --name tsas-db  --type cpx21 --image docker-ce \
  --location nbg1 --network tsas-net           # keine öffentliche Firewall-Freigabe

# Managed Load Balancer mit TLS
hcloud load-balancer create --name tsas-lb --type lb11 --location nbg1
hcloud load-balancer add-target tsas-lb --server tsas-app
hcloud load-balancer add-service tsas-lb --protocol https --listen-port 443 --destination-port 443
```

**Deployment (CLI, über SSH / Docker-Context):**

```bash
# Variante A: Docker-Context auf die App-VM
docker context create tsas --docker "host=ssh://root@tsas-app"
docker --context tsas compose -f docker/compose.prod.yml pull
docker --context tsas compose -f docker/compose.prod.yml up -d

# Variante B: direkt per SSH
ssh root@tsas-app 'cd /opt/tsas && docker compose pull && docker compose up -d'
```

> **Optional reproduzierbar:** dieselbe Topologie als Terraform-Modul (`hetznercloud/hcloud`-Provider) — `terraform apply` statt einzelner `hcloud`-Befehle. Empfohlen, sobald mehr als eine Umgebung (staging/prod) betrieben wird.

## 7. CI/CD-Anbindung

Die bestehenden GitHub-Actions-Workflows werden um einen Deploy-Schritt ergänzt:

1. **Build & Test** (vorhanden): `backend-ci.yml` / `frontend-ci.yml` inkl. Coverage-Gate.
2. **Image-Build & Push** auf `main`: Backend- und Frontend-Image bauen → **EU-Image-Registry** (z. B. Scaleway Container Registry FR, GitHub Container Registry, oder self-hosted Registry auf der VM).
3. **Deploy** (manuell freigegeben / auf Tag): GitHub Action verbindet sich per SSH-Key zur App-VM und führt `docker compose pull && up -d` aus — derselbe CLI-Pfad wie beim manuellen Betrieb.

## 8. Backup, DR & Skalierung

- **Backup:** täglicher `pg_dump` (verschlüsselt, age/SOPS-Key) + wöchentliche Volume-Snapshots → EU-Object-Storage. RPO ≤ 24 h, RTO ≤ 30 min (NFA-04).
- **Verfügbarkeit (95 %):** Single-VM-Setup genügt der Zielvorgabe. Für höhere Verfügbarkeit: zweite App-VM als LB-Target (Keycloak/Backend sind zustandslos bzgl. DB) und Managed-DB mit HA.
- **Skalierung:** vertikal (größerer Servertyp) reicht für ≤ 100 Nutzer; horizontal über zusätzliche App-Targets am Load Balancer möglich.

## 9. Mapping zum bestehenden `docker/compose.yml`

| Compose-Service | Im Cloud-Betrieb |
|---|---|
| `frontend` (Nginx+SPA) | Container auf App-VM, hinter Traefik |
| `backend` (Spring Boot) | Container auf App-VM, nur privates Netz nach außen |
| `keycloak` | Container auf App-VM, über Traefik unter `auth.<domain>` |
| `db` (PostgreSQL) | Eigene DB-VM im privaten Netz **oder** Managed-EU-PostgreSQL |
| *(neu)* `traefik` | Reverse Proxy / TLS / Routing |
| *(neu, Infra)* Cloud Firewall, Load Balancer, Object-Storage-Backups | über `hcloud` provisioniert |

## 10. Offene Entscheidungen

- **Self-managed vs. Managed PostgreSQL** (Hetzner = self-managed; bei Bedarf EU-Managed-DB → Provider-Wechsel auf Scaleway/OVH/STACKIT/IONOS).
- **Image-Registry** in der EU (Anbieter-Registry vs. self-hosted).
- **Verfügbarkeitsziel** > 95 % → Multi-VM + Managed-DB-HA.
