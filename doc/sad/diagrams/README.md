# Diagramme – Konvertierung zu SVG

## Voraussetzungen

PlantUML benötigt Java. Entweder die JAR direkt verwenden oder via Homebrew:

```bash
brew install plantuml
```

## SVG generieren

```bash
# Einzelnes Diagramm
plantuml -tsvg backend_module.puml
plantuml -tsvg backend_clean_architecture.puml

# Alle .puml Dateien im Verzeichnis auf einmal
plantuml -tsvg *.puml
```

Die SVG-Dateien werden im selben Verzeichnis erzeugt.

## Dateien

| Datei | Inhalt |
|-------|--------|
| `backend_module.puml` | Backend – Fachliche Module (Übersicht) → Kapitel 5.2 |
| `backend_clean_architecture.puml` | Backend – Clean Architecture Klassen-Detail → Kapitel 5.3 |
