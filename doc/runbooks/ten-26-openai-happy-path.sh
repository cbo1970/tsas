#!/usr/bin/env bash
# TEN-26 — OpenAI Happy-Path Smoke-Test (echter OpenAI-Key, manuell, nicht in CI).
#
# Voraussetzung: Backend läuft im test-Profil mit echtem OpenAI-Key, z. B.:
#   cd backend
#   JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew bootRun \
#       --args='--spring.profiles.active=test --spring.ai.openai.api-key=sk-...'
#
# Nutzung:  ./docs/runbooks/ten-26-openai-happy-path.sh [BASE_URL]
# Default BASE_URL = http://localhost:8080/api
set -euo pipefail

BASE="${1:-http://localhost:8080/api}"
OUT_DIR="${OUT_DIR:-docs/runbooks/ten-26-results}"
mkdir -p "$OUT_DIR"

say() { printf '\n\033[1;36m== %s ==\033[0m\n' "$*"; }
ok()  { printf '  \033[1;32m✓\033[0m %s\n' "$*"; }
bad() { printf '  \033[1;31m✗\033[0m %s\n' "$*"; }

say "0) Backend erreichbar?"
if ! curl -sf "$BASE/players" -o /dev/null; then
  bad "Backend unter $BASE nicht erreichbar — bitte zuerst starten."
  exit 1
fi
ok "Backend erreichbar unter $BASE"

say "1) Zwei Spieler anlegen"
P1=$(curl -s -X POST "$BASE/players" -H "Content-Type: application/json" -d '{
    "firstName":"Max","lastName":"Müller","gender":"MALE",
    "handedness":"RIGHT","backhandType":"TWO_HANDED","ranking":"N3","nationality":"GER"
}' | jq -r .id)
P2=$(curl -s -X POST "$BASE/players" -H "Content-Type: application/json" -d '{
    "firstName":"Tom","lastName":"Schmidt","gender":"MALE",
    "handedness":"RIGHT","backhandType":"TWO_HANDED","ranking":"N4","nationality":"GER"
}' | jq -r .id)
ok "P1=$P1  P2=$P2"

say "2) Match anlegen (Best-of-3) + Aufschläger P1"
MATCH=$(curl -s -X POST "$BASE/matches" -H "Content-Type: application/json" -d "{
    \"player1Id\":\"$P1\",\"player2Id\":\"$P2\",
    \"setsToWin\":2,\"matchTiebreak\":false,\"shortSet\":false
}" | jq -r .id)
curl -s -X POST "$BASE/matches/$MATCH/serve/player1" > /dev/null
ok "MATCH=$MATCH"

say "3) 20 Punkte einspielen (P1, Forehand-Winner)"
for _ in $(seq 1 20); do
  curl -s -X POST "$BASE/matches/$MATCH/points" -H "Content-Type: application/json" -d '{
      "winner":1,"pointType":"WINNER","strokeType":"FOREHAND",
      "direction":"CROSS_COURT","serveAttempt":1
  }' > /dev/null
done
ok "20 Punkte erfasst"

say "4) Match per Walkover beenden (P1)"
curl -s -X POST "$BASE/matches/$MATCH/end/walkover" \
    -H "Content-Type: application/json" -d '{"winner":"PLAYER1"}' > /dev/null
ok "Match COMPLETED"

say "5) KI-Analyse generieren — HTTP 201 + Antwortzeit messen"
START=$(date +%s.%N)
HTTP=$(curl -s -o "$OUT_DIR/analysis-post.json" -w '%{http_code}' -X POST "$BASE/matches/$MATCH/analysis")
END=$(date +%s.%N)
ELAPSED=$(awk "BEGIN{printf \"%.2f\", $END-$START}")
echo "  HTTP=$HTTP  Antwortzeit=${ELAPSED}s"
jq . "$OUT_DIR/analysis-post.json" || cat "$OUT_DIR/analysis-post.json"

say "6) GET Re-Read (soll identisch sein, kein neuer OpenAI-Call)"
curl -s "$BASE/matches/$MATCH/analysis" -o "$OUT_DIR/analysis-get.json"
ok "GET gespeichert -> $OUT_DIR/analysis-get.json"

say "7) Re-POST (generatedAt soll sich ändern)"
curl -s -X POST "$BASE/matches/$MATCH/analysis" -o "$OUT_DIR/analysis-repost.json"
ok "Re-POST gespeichert -> $OUT_DIR/analysis-repost.json"

say "== VALIDIERUNG =="
J="$OUT_DIR/analysis-post.json"
fail=0
[ "$HTTP" = "201" ] && ok "HTTP 201 Created" || { bad "HTTP=$HTTP (erwartet 201)"; fail=1; }
[ "$(jq -r .status "$J")" = "COMPLETED" ] && ok "status=COMPLETED" || { bad "status=$(jq -r .status "$J")"; fail=1; }
MODEL=$(jq -r .modelUsed "$J")
[ "$MODEL" = "gpt-4o-mini" ] && ok "modelUsed=$MODEL" || bad "modelUsed=$MODEL (erwartet gpt-4o-mini)"
[ "$(jq -r .errorMessage "$J")" = "null" ] && ok "errorMessage=null" || { bad "errorMessage=$(jq -r .errorMessage "$J")"; fail=1; }
for f in keyMoments ownStrengths ownWeaknesses opponentStrengths opponentWeaknesses; do
  v=$(jq -r ".$f // \"\"" "$J")
  [ -n "$v" ] && [ "$v" != "null" ] && ok "$f gefüllt (${#v} Zeichen)" || { bad "$f leer"; fail=1; }
done
RECS=$(jq '.recommendations | length' "$J")
{ [ "$RECS" -ge 3 ] && [ "$RECS" -le 5 ]; } && ok "recommendations=$RECS (3–5)" || { bad "recommendations=$RECS (erwartet 3–5)"; fail=1; }
awk "BEGIN{exit !($ELAPSED < 60)}" && ok "Antwortzeit ${ELAPSED}s < 60s (QZ-06)" || { bad "Antwortzeit ${ELAPSED}s ≥ 60s"; fail=1; }

# generatedAt-Vergleich POST vs Re-POST
G1=$(jq -r .generatedAt "$OUT_DIR/analysis-post.json")
G3=$(jq -r .generatedAt "$OUT_DIR/analysis-repost.json")
[ "$G1" != "$G3" ] && ok "Re-POST: generatedAt geändert ($G1 -> $G3)" || bad "generatedAt unverändert"

echo
if [ "$fail" = "0" ]; then
  printf '\033[1;32mHAPPY-PATH BESTANDEN\033[0m — Antwortzeit %ss, Modell %s, %s Empfehlungen\n' "$ELAPSED" "$MODEL" "$RECS"
else
  printf '\033[1;31mHAPPY-PATH FEHLGESCHLAGEN\033[0m — siehe ✗ oben, JSON unter %s\n' "$OUT_DIR"
  exit 1
fi
