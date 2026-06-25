# Smoke-Test: KI-Match-Analyse (Postmortem)

| Feld | Wert |
|---|---|
| Ticket | [TEN-25](https://linear.app/tennis-score-and-statistic/issue/TEN-25) |
| Epic | [TEN-15](https://linear.app/tennis-score-and-statistic/issue/TEN-15) |
| Spec | `docs/superpowers/specs/2026-05-17-ai-match-analysis-postmortem-design.md` |
| Plan | `docs/superpowers/plans/2026-05-17-ai-match-analysis-postmortem.md` |
| Zweck | End-to-End-Verifikation des KI-Analyse-Flows gegen die echte OpenAI-API (manuell, nicht in CI) |

---

## Voraussetzungen

- `OPENAI_API_KEY` mit Guthaben (https://platform.openai.com/api-keys)
- Podman / Docker für Postgres
- JDK 25 installiert: `JAVA_HOME=/opt/java/jdk-25.0.1`
- `curl` + `jq` (für lesbare JSON-Antworten)

---

## Empfohlener Weg: Test-Profil + echter OpenAI-Key

Das `test`-Profil nutzt H2 in-memory + `permitAll` (kein Keycloak nötig). Wir überschreiben den (leeren) Default-`api-key`, damit der `OpenAiLlmAdapter` aktiviert wird statt des Fakes.

### 1. API-Key setzen

```bash
export OPENAI_API_KEY=sk-...   # echter Key
```

### 2. Backend starten

```bash
cd backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew bootRun \
    --args='--spring.profiles.active=test --spring.ai.openai.api-key='"$OPENAI_API_KEY"
```

Backend läuft danach unter `http://localhost:8080` (HTTP, kein HTTPS im test-Profil), Security ist `permitAll`, DB ist H2 in-memory.

> **Hinweis:** H2-Daten werden bei Neustart gelöscht — für Smoke-Tests akzeptabel.

### 3. Match + Spieler + Points anlegen

In einer zweiten Shell:

```bash
BASE=http://localhost:8080/api

# Zwei Spieler anlegen
P1=$(curl -s -X POST "$BASE/players" -H "Content-Type: application/json" -d '{
    "firstName":"Max","lastName":"Müller","gender":"MALE",
    "handedness":"RIGHT","backhandType":"TWO_HANDED","ranking":"N3","nationality":"GER"
}' | jq -r .id)

P2=$(curl -s -X POST "$BASE/players" -H "Content-Type: application/json" -d '{
    "firstName":"Tom","lastName":"Schmidt","gender":"MALE",
    "handedness":"RIGHT","backhandType":"TWO_HANDED","ranking":"N4","nationality":"GER"
}' | jq -r .id)

echo "P1=$P1  P2=$P2"

# Match starten (Best-of-3)
MATCH=$(curl -s -X POST "$BASE/matches" -H "Content-Type: application/json" -d "{
    \"player1Id\":\"$P1\",\"player2Id\":\"$P2\",
    \"setsToWin\":2,\"matchTiebreak\":false,\"shortSet\":false
}" | jq -r .id)

echo "MATCH=$MATCH"

# Aufschläger setzen (P1)
curl -s -X POST "$BASE/matches/$MATCH/serve/player1" > /dev/null

# 20 Punkte einspielen (alle für P1, Forehand-Winner)
for i in $(seq 1 20); do
  curl -s -X POST "$BASE/matches/$MATCH/points" -H "Content-Type: application/json" -d '{
      "winner":1,"pointType":"WINNER","strokeType":"FOREHAND",
      "direction":"CROSS_COURT","serveAttempt":1
  }' > /dev/null
done

# Match beenden (per Walkover P1 — am schnellsten)
curl -s -X POST "$BASE/matches/$MATCH/end/walkover" \
    -H "Content-Type: application/json" -d '{"winner":"PLAYER1"}' | jq .
```

### 4. KI-Analyse generieren (200 erwartet)

```bash
curl -s -X POST "$BASE/matches/$MATCH/analysis" | jq .
```

**Erwartet:**
- HTTP 200
- `status: "COMPLETED"`
- `modelUsed: "gpt-4o-mini"` (oder das via `OPENAI_MODEL` gesetzte Modell)
- 4 Textfelder gefüllt (`keyMoments`, `ownStrengths`, `ownWeaknesses`, `opponentStrengths`, `opponentWeaknesses`) — auf Deutsch
- `recommendations`: 3–5 Einträge mit `priority`, `title`, `detail`
- `generatedAt`: aktueller Timestamp
- `errorMessage`: `null`

Im Backend-Log: ein einzelner OpenAI-Call ist zu sehen.

### 5. GET liefert aus DB (200, kein neuer OpenAI-Call)

```bash
curl -s "$BASE/matches/$MATCH/analysis" | jq .
```

**Erwartet:** identische Antwort wie POST. Im Log: **kein** OpenAI-Call (Lese-Pfad aus `match_analysis`).

### 6. Re-Generierung überschreibt

```bash
curl -s -X POST "$BASE/matches/$MATCH/analysis" | jq '.generatedAt, .keyMoments[:60]'
```

`generatedAt` ändert sich; Inhalt kann variieren (Temperature 0.4). Nur ein DB-Eintrag pro Match.

---

## Fehlerpfade prüfen

### 404: Match unbekannt

```bash
curl -i -X POST "$BASE/matches/00000000-0000-0000-0000-000000000000/analysis"
```
→ HTTP 404, `ProblemDetail` mit `title: "Not Found"`.

### 404: GET ohne generierte Analyse

Match anlegen wie oben, aber nicht POSTen — direkt GET:
```bash
curl -i "$BASE/matches/$MATCH/analysis"
```
→ HTTP 404.

### 409: Match nicht COMPLETED

Neues Match anlegen, Punkte einspielen, **kein** `end`/`walkover`:
```bash
MATCH2=$(curl -s -X POST "$BASE/matches" -H "Content-Type: application/json" -d "{
    \"player1Id\":\"$P1\",\"player2Id\":\"$P2\",\"setsToWin\":2,\"matchTiebreak\":false,\"shortSet\":false
}" | jq -r .id)
curl -s -X POST "$BASE/matches/$MATCH2/serve/player1" > /dev/null
for i in $(seq 1 15); do
  curl -s -X POST "$BASE/matches/$MATCH2/points" -H "Content-Type: application/json" -d '{
      "winner":1,"pointType":"WINNER","strokeType":"FOREHAND","direction":"CROSS_COURT","serveAttempt":1
  }' > /dev/null
done

curl -i -X POST "$BASE/matches/$MATCH2/analysis"
```
→ HTTP 409, Detail enthält „not COMPLETED".

### 422: Beendetes Match mit < 10 Points

Match per Walkover beenden, ohne Punkte zu erfassen:
```bash
MATCH3=$(curl -s -X POST "$BASE/matches" -H "Content-Type: application/json" -d "{
    \"player1Id\":\"$P1\",\"player2Id\":\"$P2\",\"setsToWin\":2,\"matchTiebreak\":false,\"shortSet\":false
}" | jq -r .id)
curl -s -X POST "$BASE/matches/$MATCH3/end/walkover" \
    -H "Content-Type: application/json" -d '{"winner":"PLAYER1"}' > /dev/null

curl -i -X POST "$BASE/matches/$MATCH3/analysis"
```
→ HTTP 422, Detail enthält „at least 10 points".

### Fake-Adapter ohne API-Key

Backend neu starten **ohne** `OPENAI_API_KEY`-Override:
```bash
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew bootRun --args='--spring.profiles.active=test'
```
POST auf ein gültiges, COMPLETED-Match → HTTP 200 mit `modelUsed: "fake-llm"` und deterministischem Stub-Text. Bestätigt, dass `@ConditionalOnExpression` korrekt auf den Fake umschaltet, wenn kein Key da ist.

### 502: LLM-Fehler

Schwer reproduzierbar ohne OpenAI-Outage. Workaround: ungültigen Key setzen:
```bash
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew bootRun \
    --args='--spring.profiles.active=test --spring.ai.openai.api-key=sk-invalid'
```
POST → HTTP 502. In der DB ist `match_analysis.status = 'FAILED'`, `error_message` gefüllt.

---

## Optional: Voller Stack mit Keycloak

Wenn du den Auth-Pfad mittesten willst:

```bash
# Stack hochfahren (Postgres + Keycloak)
podman compose -f docker/compose.yml up -d keycloak
podman compose -f docker/db/compose.yaml up -d

# Backend mit local-Profil (PostgreSQL + JWT + HTTPS)
cd backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew bootRun --args='--spring.profiles.active=local'
```

Backend läuft dann unter `https://localhost:8080` und erfordert JWT. Im Frontend (`https://localhost:4200`) registrieren, einloggen, Access-Token aus den Browser-DevTools (Application → Local Storage → `access_token`) entnehmen, dann allen `curl`-Calls `-H "Authorization: Bearer $TOKEN" -k` anhängen.

---

## Abnahme-Checkliste

- [ ] Erfolgsfall: HTTP 200, deutscher Inhalt, 3–5 Empfehlungen, `modelUsed = gpt-4o-mini`
- [ ] GET-Re-Read: gleiche Antwort, kein zusätzlicher OpenAI-Call im Log
- [ ] Re-POST: `generatedAt` neu, ein DB-Eintrag bleibt
- [ ] 404 unbekanntes Match
- [ ] 404 GET ohne Analyse
- [ ] 409 Match nicht COMPLETED
- [ ] 422 < 10 Points
- [ ] Fake-Adapter aktiv ohne API-Key
- [ ] 502 + persistierter FAILED-Datensatz bei ungültigem Key

Ergebnis als Kommentar an [TEN-15](https://linear.app/tennis-score-and-statistic/issue/TEN-15) anhängen, dann TEN-25 schliessen.
