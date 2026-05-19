# Inline Scoring Panel – Design

**Datum:** 2026-05-19
**Status:** Approved

## Zusammenfassung

Der bisherige Punkt-Erfassungs-Dialog (`RecordPointDialogComponent`) und die Tennisplatz-Visualisierung werden durch ein inline Scoring-Panel ersetzt, das im Querformat dauerhaft auf dem Score-Screen sichtbar ist. Beide Spieler werden nebeneinander dargestellt; ein Klick auf eine Punkttyp-Kachel speichert den Punkt sofort — kein Dialog öffnet sich.

---

## Layout (Querformat)

```
┌─────────────────────────────────────────────────────────────┐
│  ←  │   🎾 Müller  30  :  15  Meier   │  ✏️  ⏹           │  ← Score-Streifen
├─────────────────────────┬───────────────────────────────────┤
│  🎾 MÜLLER              │  MEIER                            │
│  ┌──┬──┬──┐             │  ┌──┬──┬──┐                       │
│  │🏆│😓│💨│  Punkttyp  │  │🏆│😓│💨│  Punkttyp            │
│  ├──┼──┼──┤             │  ├──┼──┼──┤                       │
│  │🎯│❌│🔴│             │  │🎯│❌│🔴│                       │
│  ├──┼──┼──┤             │  ├──┼──┼──┤                       │
│  │↑ │→ │  │             │  │↑ │→ │  │                       │
│  └──┴──┴──┘             │  └──┴──┴──┘                       │
│  [FH] RH  Aufschlag …  │  [FH] RH  Aufschlag …            │  ← Schlagart
│  [Cross] DTL  Mitte     │  [Cross] DTL  Mitte               │  ← Richtung
└─────────────────────────┴───────────────────────────────────┘
```

- **Score-Streifen** (oben, fixiert): Punkte, Games, Sätze beider Spieler; „Score korrigieren"- und „Match beenden"-Actions.
- **Zwei Spieler-Panels** (links/rechts, `flex: 1`): symmetrisch aufgebaut, durch einen 1px-Divider getrennt.
- **Aufschläger** ist visuell hervorgehoben: Panel-Header in Blau (`#64b5f6`), Kacheln heller.
- **Dark Theme**: Hintergrund `#0d1b2a`, Kacheln `rgba(255,255,255,.08)`, Text weiß/gedimmt — passend zum bestehenden Score-Screen.
- **Tennisplatz-Visualisierung** entfällt im Querformat vollständig.

---

## Interaktionslogik

### Punkttyp-Kacheln (required)
- 3×3-Grid mit 8 Kacheln + 1 leerem Platzhalter.
- Klick → `recordPoint(winner, pointType)` wird sofort aufgerufen mit der aktuellen Vorauswahl für Schlagart und Richtung.
- Kein weiterer Bestätigungsschritt.

### Schlagart-Pills (optional, Vor-Auswahl)
- Horizontal scrollbare Pill-Reihe pro Spieler (`FH | RH | Aufschlag | Volley | Smash`).
- Standard-Vorauswahl: `FOREHAND`.
- Aktive Pill: blauer Fill (`#1565c0`), alle anderen: `rgba(255,255,255,.1)`.
- Pill antippen = neue Vorauswahl für den nächsten Punkt dieses Spielers.

### Richtungs-Pills (optional, Vor-Auswahl)
- Gleich wie Schlagart (`Cross | DTL | Mitte`).
- Standard-Vorauswahl: `CROSS_COURT`.

### Aufschlag setzen
- Solange kein Aufschläger gesetzt ist, erscheint im Panel-Header beider Spieler ein Hinweistext: *„Tippen = Aufschlag setzen"*.
- Erster Klick auf eine Punkttyp-Kachel setzt diesen Spieler als Aufschläger — kein Punkt wird erfasst.
- Ab dem zweiten Klick (Aufschläger gesetzt) wird der Punkt direkt gespeichert.

---

## Orientierung

- **Querformat (`landscape`)**: Vollbild-Scoring-Panel wie oben beschrieben — primäre Nutzungsart.
- **Hochformat (`portrait`)**: Panels untereinander statt nebeneinander (dieselbe Logik, `flex-direction: column`). Kein separater Hochformat-Modus nötig — CSS-Breakpoint genügt.
- Angular CDK `BreakpointObserver` oder CSS `@media (orientation: landscape)` für das Layout-Switching.

---

## Betroffene Komponenten

| Datei | Aktion |
|---|---|
| `score.component.ts` | Refactor: Tennisplatz entfernen, Score-Streifen + Inline-Panels einbauen, Signals für `strokeType` + `direction` pro Spieler, `handleCourtClick` → `recordPoint(winner, pointType)` |
| `record-point-dialog.component.ts` | **Löschen** |
| `score.component.ts` (imports) | `MatDialog`, `RecordPointDialogComponent` entfernen |

Keine Backend-Änderungen. Die `RecordPointRequest`-Struktur bleibt unverändert.

---

## Nicht in Scope

- Bemerkungsfeld (Freitext): Bleibt über „Score korrigieren" erreichbar oder wird als separates Feature später ergänzt.
- Rückgängig machen eines Punktes.
- Hochformat als eigenständiges Design (wird per CSS adaptiert).
