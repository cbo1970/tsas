# Diagramme – draw.io Quellen & SVG-Export

Die Architekturdiagramme des SAD werden als **draw.io** (`.drawio`) gepflegt
und als **SVG** in die finale `.docx` eingebettet. (Die früheren PlantUML-`.puml`-
Quellen wurden ersetzt.)

## Bearbeiten

`.drawio` ist die Quelle. Bearbeiten mit der draw.io-Desktop-App oder online auf
<https://app.diagrams.net>. Nach Änderungen die SVGs neu exportieren.

## SVG (neu) exportieren

Mit der draw.io-Desktop-App (macOS):

```bash
DRAWIO="/Applications/draw.io.app/Contents/MacOS/draw.io"

# Einzelnes Diagramm
"$DRAWIO" --export --format svg --border 12 \
  --output TSaS_System_Context.svg TSaS_System_Context.drawio

# Alle Diagramme
for f in TSaS_System_Context TSaS_Backend_Module TSaS_Backend_CleanArchitecture; do
  "$DRAWIO" --export --format svg --border 12 --output "$f.svg" "$f.drawio"
done
```

## Dateien

| Datei | Inhalt |
|-------|--------|
| `TSaS_Fachlicher_Kontext.drawio` | Fachlicher Kontext – Akteure, System-Grenze, externe Systeme (Keycloak, Google, OpenAI LLM, Swisstennis, Kamera) → Kapitel 3.1 |
| `TSaS_System_Context.drawio` | Produktive Systemarchitektur – Services, Protokolle, Ports (Edge, DMZ, Private/DB-Subnet, externe Dienste inkl. OpenAI LLM) |
| `TSaS_Deployment.drawio` | Verteilungssicht – Docker-Compose-Container (frontend, backend, db, keycloak) + externer LLM-Dienst → Kapitel 7.1 |
| `TSaS_Backend_Module.drawio` | Backend – Fachliche Module (Übersicht) → Kapitel 5.2 |
| `TSaS_Backend_CleanArchitecture.drawio` | Backend – Clean Architecture Klassen-Detail (Infrastructure → Application → Domain) → Kapitel 5.3 |
| `TSaS_Datenmodell.drawio` | Datenmodell (ER) – `players`, `matches`, `match_scores`, `points`, `match_analysis`; deckungsgleich mit den Flyway-Migrationen V1–V5 → Kapitel 11 |
| `TSaS_Seq_RecordPoint.puml` / `.svg` / `.png` | Sequenzdiagramm „Punkt erfassen" – Ablauf POST /api/matches/{id}/points inkl. JWT-Validierung, ScoringService und DB-Persistierung → Kapitel 6.1 |
| `TSaS_Seq_GenerateAnalysis.puml` / `.svg` / `.png` | Sequenzdiagramm „KI-Match-Analyse generieren" – Ablauf POST /api/matches/{id}/analysis inkl. LlmClientPort (OpenAI / Fake), alt/else-Verzweigungen und Fehlerbehandlung → Kapitel 6.2 |

Beim SVG-Export oben die Dateinamen entsprechend ergänzen (`TSaS_Fachlicher_Kontext`, `TSaS_Deployment`, `TSaS_Datenmodell`).

> Inhaltlich abgeglichen mit dem aktuellen Code: Scoring ist im `match-module`
> konsolidiert (kein eigenes `scoring-module`), das `ai-module` (KI-Match-Analyse
> über `LlmClientPort` mit `OpenAiLlmAdapter`/`FakeLlmClientAdapter`) ist enthalten,
> Controller/Use-Cases/Ports/Adapter entsprechen den realen Klassen.
