# Datenschutzerklärung — TSaS

**Stand:** 2026-06-24
**Verantwortlich:** Christian Bonnhoff (`ch.bonnhoff@gmail.com`)

Diese Erklärung deckt die Erhebung und Verarbeitung personenbezogener Daten in TSaS (Tennis Score and Statistic) gemäss DSGVO ab.

## 1. Erhobene Daten

| Kategorie | Inhalt | Rechtsgrundlage |
|---|---|---|
| Kontodaten | Benutzername, E-Mail-Adresse, gehashtes Passwort (Keycloak) | Art. 6 Abs. 1 lit. b DSGVO (Vertrag) |
| Spielerprofile | Vor-/Nachname, Geschlecht, Spielhand, Backhand-Typ, Ranking, Nationalität, Geburtsdatum | Art. 6 Abs. 1 lit. b DSGVO |
| Matchdaten | Begegnungen, Punkte, Spielstände, Aufschlag-Statistik | Art. 6 Abs. 1 lit. b DSGVO |
| KI-Match-Analysen | Strukturierte Auswertung pro Match (LLM-generiert) | Art. 6 Abs. 1 lit. b DSGVO |
| Audit-Spalten | `created_at`, `created_by`, `updated_at`, `updated_by` pro Datensatz | Art. 6 Abs. 1 lit. f DSGVO (berechtigtes Interesse: Nachvollziehbarkeit) |
| Server-Logs | Correlation-ID-basierte Request-Logs | Art. 6 Abs. 1 lit. f DSGVO |

Daten sind pro Nutzer über die `owner_id` (Keycloak-`sub`) mandantengebunden — andere Nutzer haben keinen Zugriff (siehe TEN-55).

## 2. Externe Empfänger

- **OpenAI** (USA) — beim Generieren einer KI-Match-Analyse werden aggregierte Match-Statistiken (anonymisiert: keine Spielernamen, nur Rankings + Schlagstatistik) sowie die JWT-`sub`-ID an die OpenAI Chat Completions API übermittelt. Auftragsverarbeitung gemäss OpenAI Data Processing Addendum. Anwendbar bei `OPENAI_API_KEY != ""`; ohne Key wird der deterministische `FakeLlmClientAdapter` verwendet und es findet keine externe Übertragung statt.
- **Keycloak** — selbst gehostet, keine Drittlandsübermittlung.

## 3. Speicherdauer

- Aktive Konten: Daten bleiben gespeichert, solange das Keycloak-Konto besteht.
- Bei Account-Löschung (Art. 17, siehe §4): unverzügliche Löschung aller Aggregate (Spieler, Matches, Punkte, KI-Analysen) innerhalb einer Transaktion.
- Server-Logs werden 30 Tage rotiert (Logback-Default).

## 4. Rechte der betroffenen Personen

### Art. 17 — Recht auf Löschung

`DELETE /api/me` löscht innerhalb einer einzigen Transaktion **alle** Aggregate des aktuell authentifizierten Nutzers in korrekter Foreign-Key-Reihenfolge:

1. `points` (alle Punkte aller eigenen Matches)
2. `match_scores` (alle Spielstand-Records)
3. `match_analysis` (cascade via `ON DELETE CASCADE`)
4. `matches` (alle eigenen Begegnungen)
5. `players` (alle eigenen Spielerprofile)

Die Antwort enthält die Counts der gelöschten Datensätze für Audit-Zwecke. Eine strukturierte Log-Zeile `DSGVO Art. 17 delete: user={uid} players={n} matches={n} points={n} scores={n}` dokumentiert die Operation.

**Im Frontend:** Über das Benutzermenü in der Toolbar → „Konto-Daten löschen" mit Bestätigungsdialog. Nach erfolgreicher Löschung wird die Seite neu geladen.

**Hinweis:** Das Keycloak-Konto selbst bleibt bestehen — bei erneutem Login startet der Nutzer mit einer leeren Datenbasis. Die Löschung des Keycloak-Kontos erfolgt manuell über die Keycloak-Admin-Konsole.

**DSGVO-Frist:** Bearbeitung sofort (Endpoint ist transaktional und synchron). Die Pflicht zur Erledigung innerhalb 30 Tagen (Art. 12 Abs. 3) ist damit eingehalten.

### Art. 20 — Recht auf Datenübertragbarkeit

`GET /api/me/export` liefert einen vollständigen JSON-Snapshot aller eigenen Aggregate, strukturiert als:

```json
{
  "header": {
    "userId": "<uuid>",
    "exportedAt": "2026-06-24T11:00:00Z",
    "format": "application/json; profile=dsgvo-art20-v1"
  },
  "players":  [ ... ],
  "matches":  [ ... ],
  "points":   [ ... ],
  "scores":   [ ... ],
  "analyses": [ ... ]
}
```

**Im Frontend:** Über das Benutzermenü → „Daten exportieren" — die JSON-Datei wird als `tsas-export-YYYY-MM-DD.json` heruntergeladen.

### Art. 15 — Auskunft

Vom Export-Endpoint mit abgedeckt (vollständiger Dump aller eigenen Daten).

### Art. 16 — Berichtigung

Über die bestehenden CRUD-Endpoints (`PUT /api/players/{id}`, `PUT /api/matches/{id}/score`) sowie die zugehörigen Dialoge im Frontend.

## 5. Technische Schutzmassnahmen

- OAuth2/OIDC-Authentifizierung via Keycloak; alle API-Endpunkte (ausser Actuator-Health und OpenAPI) durch Bearer-JWT geschützt.
- Owner-Binding pro Datensatz (`owner_id` = JWT-`sub`) durchgesetzt im Backend-Service-Layer; cross-tenant-404-Tests in der Test-Suite.
- TLS 1.2+ für alle externen Verbindungen (Browser ↔ Frontend ↔ Backend ↔ Keycloak).
- E-Mail-Verifizierung beim Self-Registration verhindert Pseudo-Konten (TEN-64).
- Rate-Limits auf der kostenintensiven KI-Analyse (Bucket4j 5/Tag + 1/Min, nginx 5r/m per IP — TEN-64).
- Audit-Spalten + Correlation-ID-Logging für die Nachvollziehbarkeit (TEN-59).
- Container-Hardening: non-root, read-only Filesystem, `cap_drop:[ALL]`, `no-new-privileges` (TEN-63).
- Security-Header im nginx: CSP, HSTS, X-Content-Type-Options, Referrer-Policy, Permissions-Policy (TEN-62).

## 6. Kontakt

Anfragen zu den vorgenannten Rechten oder zur Datenverarbeitung bitte an `ch.bonnhoff@gmail.com`.
