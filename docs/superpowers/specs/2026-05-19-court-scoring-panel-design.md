# Court Scoring Panel – Design

**Datum:** 2026-05-19
**Status:** Approved

## Zusammenfassung

Der bisherige `ScoreComponent` (dunkles Panel-Layout ohne Tennisfeld) wird durch eine vollflächige Tennisplatz-Visualisierung ersetzt. Die Scoring-Panels beider Spieler liegen *innerhalb* des Feldes, je in der linken und rechten Feldhälfte. Score und Spielernamen erscheinen oben. Farbgebung: US Open (Deco Turf Blau).

---

## Layout (Querformat)

```
┌──────────────────────────────────────────────────────────────────┐
│  ←  │   🎾 Müller   0  :  0   Meier   │  ✏️   ⏹              │  ← Score-Streifen
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ░░░░░░░░░░░░░░░░░░░▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░  │  ← Aussenfeld
│  ░░ ┌────────────┬─┊─┬────────────┐  ░░  (Doppelfeld-Linien)  │
│  ░░ │  MÜLLER   │ │ │  MEIER    │  ░░                          │
│  ░░ │ [🏆][😓][💨]│ │ │[🏆][😓][💨]│  ░░  ← Tile-Grid im Feld  │
│  ░░ │ [🎯][❌][🔴]│ │ │[🎯][❌][🔴]│  ░░                          │
│  ░░ │ [↑][→][ ] │ │ │[↑][→][ ] │  ░░                          │
│  ░░ │ FH RH Aufschl│ │ │FH RH Aufschl│  ░░  ← Schlagart-Pills  │
│  ░░ │ Cross DTL  │ │ │Cross DTL  │  ░░  ← Richtungs-Pills     │
│  ░░ └────────────┴─┊─┴────────────┘  ░░                          │
│  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
└──────────────────────────────────────────────────────────────────┘
```

---

## Farbgebung (US Open)

| Element | Farbe |
|---|---|
| Aussenfeld | `#1a5276` (dunkles Blau) |
| Spielfeld (Infield) | `#1565c0` (helles Blau) |
| Court-Linien | `white`, 2px |
| Score-Streifen | `rgba(0,0,0,.82)` |
| Spieler-Tiles (aktiv) | `rgba(255,255,255,.15)` |
| Active Pill | `#1565c0` (Blau) |
| Schrift | weiss / `rgba(255,255,255,.6)` |

---

## Court-Linien (Doppelfeld, Vogelperspektive)

- **Aussenrahmen**: Doppelfeld-Aussenlinien
- **Singles-Seitenlinien**: ~50px von der Aussenlinie nach innen (beide Seiten)
- **Netz-Mittellinie**: vertikale Linie in der Platzmitte (3px, volle Höhe)
- **Aufschlag-Querlinien**: horizontale Linien auf ~50% der Feldhöhe (eine pro Hälfte)
- **Aufschlag-Mittellinien**: vertikale Linien in der Mitte jeder Aufschlagbox

---

## Spieler-Panels (innerhalb des Feldes)

Beide Panels liegen absolut positioniert über dem Court-Feld. **Kein Opacity-Unterschied** zwischen den Spielern — beide Panels sind immer gleich sichtbar. Der Aufschläger wird ausschliesslich durch das 🎾-Icon neben dem Namen gekennzeichnet (im Score-Streifen oben und im Panel-Header).

### Panel-Aufbau (identisch für beide Spieler)

```
┌──────────────────────────────────┐
│ [🎾] SPIELERNAME                 │  ← Name (blau wenn Aufschläger, sonst weiss)
│ ┌──┬──┬──┐                       │
│ │🏆│😓│💨│  Punkttyp-Grid        │
│ ├──┼──┼──┤  (3×3, 8 Kacheln)    │
│ │🎯│❌│🔴│                       │
│ ├──┼──┼──┤                       │
│ │↑ │→ │  │                       │
│ └──┴──┴──┘                       │
│ Schlagart: FH RH Aufschl Volley  │
│ Richtung:  Cross DTL Mitte       │
└──────────────────────────────────┘
```

- Tile-Hintergrund: `rgba(255,255,255,.15)` — gleich für beide Spieler
- Panel-Hintergrund: kein eigener Hintergrund — Tiles direkt auf dem Court
- Aufschläger-Indikator: 🎾 vor dem Namen (Name in `#90caf9`), ohne Aufschlag weiss/gedimmt

---

## Score-Streifen (oben, fixiert)

- Hintergrund: `rgba(0,0,0,.82)`, volle Breite
- Inhalt: `←` Zurück | Spieler-1-Score mittig | `:` | Spieler-2-Score | ✏️ ⏹
- 🎾-Icon erscheint neben dem Namen des aufschlagenden Spielers

---

## Interaktionslogik

Identisch zum vorherigen Design:

1. **Kein Aufschläger gesetzt**: Hinweistext „Tippen = Aufschlag setzen" unter dem Namen. Erster Klick auf eine Kachel → setzt Aufschläger, kein Punkt.
2. **Aufschläger gesetzt**: Klick auf Kachel → `recordPoint(winner, pointType)` wird sofort aufgerufen.
3. **Schlagart/Richtung-Pills**: Vorauswahl, Standard `FOREHAND` / `CROSS_COURT`.

---

## Orientierung

- **Querformat**: Panels nebeneinander (primäre Nutzungsart)
- **Hochformat**: `@media (orientation: portrait)` → Panels untereinander

---

## Betroffene Dateien

| Datei | Aktion |
|---|---|
| `frontend/src/app/features/matches/score/score.component.ts` | Vollständig ersetzen — Court + neue Styles |

Keine Backend-Änderungen. `RecordPointRequest`-Struktur unverändert.

---

## Nicht in Scope

- Interaktive Ball-Landepunkt-Erfassung auf dem Court (V3 Roadmap)
- Hochformat als eigenständiges Design
