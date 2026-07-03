# TEN-33 — Scoring-UI: Redesign & Statistik-Erfassung

## Kontext

Die bestehende Scoring-Seite (`ScoreComponent`) verwendet Kacheln (Tiles) die gleichzeitig den Punkt vergeben und den Typ erfassen. TEN-33 ersetzt dieses Layout durch ein neues Design mit zentriertem Score-Header und separaten Beobachtungs-Panels pro Spieler.

## Interaktionsmodell

Zwei gleichwertige Wege um einen Punkt zu vergeben:

**Schnell-Klick (ohne Beobachtung):** Klick auf die Punkte-Zahl im Score-Header (z. B. «15») → Punkt geht an diesen Spieler, kein Typ / kein Service-Kontext.

**Beobachtungs-Button (mit Statistik):** Klick auf einen farbigen Button im Beobachtungs-Panel → vergibt Punkt *und* erfasst Statistik gleichzeitig. Zuvor gewählte Kontext-Buttons (Service, Schlagart) werden mitgesendet.

## Layout

### Score-Header

```
[ Müller (zentriert)   3 : 2   Meier (zentriert) ]   ← Games-Score
[                     30 : 15                     ]   ← Punkte-Score (klickbar)
                   Satz 1: 6:3 · Satz 2 laufend
```

- Spielernamen zentriert in ihrer Hälfte
- Games-Score (gross) **über** Punkte-Score
- Vergangene Sätze einzeilig darunter
- Punkte-Zahlen sind Buttons; Klick → `recordPoint(winner, null, null)`

### Beobachtungs-Panels

Zwei Panels nebeneinander, eines pro Spieler. Aktiver Spieler (nach Score-Klick wird kein Panel hervorgehoben — beide immer gleichwertig bedienbar).

| Gruppe | Buttons | Farbe | Punkt-Effekt |
|---|---|---|---|
| Service | 1. Service · 2. Service | Gelb (Toggle) | Keiner — nur Kontext |
| Gewinn | Ass 🎯 · Winner 🏆 | Grün | Punkt → **eigener** Spieler |
| Fehler | DF ❌ · Forced 💨 · Unforced 😓 · Netz 🔴 | Rot | Punkt → **Gegner** |
| Schlagart | Vorhand · Rückhand | Gelb (Toggle) | Keiner — nur Kontext |

Fehler-Buttons (Forced, Unforced, Netz) auf **einer** Zeile. DF auf eigener Zeile (da anderer Kontext: nur für Aufschläger).

### Validierung (Snackbar-Fehler)

- Ass auf Non-Server → «Ass nur für den Aufschläger»
- DF auf Non-Server → «Doppelfehler nur für den Aufschläger»

Ass und DF sind sichtbar, aber bei Fehlbedienung wird die Aktion abgebrochen und die Meldung angezeigt (kein Punkt vergeben).

## Backend-Änderungen

### `ServiceType` Enum (neu)
```java
public enum ServiceType { FIRST_SERVICE, SECOND_SERVICE }
```

### `Point`-Entität
- Neue nullable Spalte `service_type` (Flyway-Migration)
- `point_type` wird nullable (Schnell-Klick sendet keinen Typ)

### `RecordPointRequest`
```java
@Nullable PointType pointType;      // war required, neu optional
@Nullable ServiceType serviceType;  // neu
```

### `PointType`-Enum
`OUT_LONG` und `OUT_SIDE` werden aus der API-Schnittstelle entfernt (Werte bleiben in DB für Rückwärtskompatibilität, werden im Backend-Mapping ignoriert / auf `UNFORCED_ERROR` gemappt).

### Statistik-Endpunkt
Bei Berechnung «% Punkte bei 1. Aufschlag» wird `service_type` ausgewertet.

## Frontend-Änderungen

### `ScoreComponent`

Neue Signals:
```typescript
serviceContextP1 = signal<'FIRST_SERVICE' | 'SECOND_SERVICE' | null>(null);
serviceContextP2 = signal<'FIRST_SERVICE' | 'SECOND_SERVICE' | null>(null);
strokeTypeP1     = signal<'FOREHAND' | 'BACKHAND' | null>(null);
strokeTypeP2     = signal<'FOREHAND' | 'BACKHAND' | null>(null);
```

Kontext-Signals werden nach jedem erfassten Punkt zurückgesetzt.

Button-Klick-Logik:
```
recordObservation(panel: 1|2, pointType: PointType):
  winner    = (ACE | WINNER) ? panel : opponent(panel)
  service   = panel===1 ? serviceContextP1() : serviceContextP2()
  stroke    = panel===1 ? strokeTypeP1()     : strokeTypeP2()

  if (ACE | DOUBLE_FAULT) && panel !== servingPlayer():
    → snackBar(Fehlermeldung); return

  api.recordPoint(matchId, { winner, pointType, serviceType: service, strokeType: stroke })
  → resetContextSignals(panel)   // setzt nur Signals des geklickten Panels zurück
```

Schnell-Klick auf Punkte-Zahl:
```
recordQuickPoint(winner: 1|2):
  api.recordPoint(matchId, { winner, pointType: null, serviceType: null })
```

### `RecordPointRequest` Model (Frontend)
```typescript
export interface RecordPointRequest {
  winner:       1 | 2;
  pointType?:   PointType | null;
  serviceType?: 'FIRST_SERVICE' | 'SECOND_SERVICE' | null;
  strokeType?:  'FOREHAND' | 'BACKHAND' | null; // nur noch VH/RH
  // direction entfernt (nicht im Drawio-Design)
}
```

### Entfernte PointTypes
`OUT_LONG` und `OUT_SIDE` werden aus dem Frontend-Enum und allen Templates entfernt.

## Testplan

### Frontend (Cypress E2E)

| Test | Prüfung |
|---|---|
| Winner → P1-Stats | Klick Winner auf P1-Panel → P1 Punkt +1, Winner-Stat P1 +1 |
| Forced Error → P2-Stats | Klick Forced auf P1-Panel → P2 Punkt +1, Forced-Stat P2 +1 |
| Unforced Error → P2-Stats | Klick Unforced auf P1-Panel → P2 Punkt +1, Unforced-Stat P2 +1 |
| Netz → P2-Stats | Klick Netz auf P1-Panel → P2 Punkt +1, Netz-Stat P2 +1 |
| Ass → nur Aufschläger (ok) | Ass auf Aufschläger-Panel → Punkt vergeben |
| Ass → falscher Spieler | Ass auf Non-Server → Snackbar, kein Punkt |
| DF → Aufschläger (ok) | DF auf Aufschläger-Panel → Gegner Punkt +1, DF-Stat Aufschläger +1 |
| DF → falscher Spieler | DF auf Non-Server → Snackbar, kein Punkt |
| Games-Anzeige | 4 Punkte gespielt → Games-Score korrekt erhöht |
| Satz-Anzeige | Satz gewonnen → vergangener Satz im Header sichtbar |
| Satz-Tiebreak 6:6 | Bei 6:6 → Tiebreak aktiv, Anzeige 0/1/2… |
| Schnell-Klick Score | Klick auf Punkte-Zahl → Punkt vergeben ohne pointType |
| Service-Kontext | 1. Service wählen, dann Punkt → serviceType in Request |
| Context-Reset | Nach Punkt → Service + Schlagart zurückgesetzt |

### Backend (JUnit)

| Test | Prüfung |
|---|---|
| `serviceType` persistiert | `recordPoint` mit `FIRST_SERVICE` → Wert in DB gespeichert |
| `pointType` nullable | `recordPoint` ohne `pointType` → Punkt korrekt erfasst |
| Statistik 1st service | Punkte mit `FIRST_SERVICE` fliessen in Statistik-Berechnung |

## Nicht in Scope

- Richtung (Cross, DTL, Mitte) wird vorerst entfernt (war in Drawio nicht vorgesehen)
- Volley / Smash als Schlagart entfernt (Drawio zeigt nur Vorhand / Rückhand)
- Match-Tiebreak-Anzeige: bestehende Logik bleibt unverändert
