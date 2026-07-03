# UI Design-System „Daylight · Hard Court" — Design

**Status:** Design abgenommen
**Datum:** 2026-06-26
**Kontext:** TSaS Angular-Frontend. Ziel: ein konsistentes, modernes Design-System über **alle** Seiten, das die heutige visuelle Zersplitterung auflöst.

## Problem

Das Frontend hat heute drei divergierende „Welten":
- **Helle Material-M3-Seiten** (Players, Match-Setup) auf `#f5f5f5`, Azure-Palette.
- **Dunkle US-Open-Blau-Gradient-Seiten** (Score, Statistik, Head-to-Head): `linear-gradient(160deg,#103A6B,#2D72B8)`, weisser/`#eee`-Text, Cyan-Akzent `#0ea5e9`.
- **Separate dunkle Slate-Seite** (Match-Analyse): `#0f172a`.

Dazu: zwei verschiedene Blautöne (Material-Azure vs. Cyan), **kein** Token-System, gemischte Fonts (Roboto vs. generisches `sans-serif`), ad-hoc `font-size`-Werte und viel hartcodiertes Inline-CSS pro Komponente.

## Zielbild (abgenommene Entscheidungen)

| Entscheidung | Wahl |
|---|---|
| Grundstimmung | **Hell** („Daylight Clean") — courtside-/sonnentauglich, konsistent |
| Akzent/Marke | **Hard-Court-Blau** `#2563EB` (eine Blau-Welt statt Azure + Cyan) |
| Score-Seite | **Durchgehend hell** (kein separater Dark-Court-Modus) |
| Typografie | **Inter**, feste Skala |
| Light/Dark | **nur hell** jetzt; Tokens dark-fähig strukturiert, Dark-Mode wird **nicht** gebaut (YAGNI) |
| Projektform | Ein zusammenhängendes Design-System: eine Spec → ein Plan |

## 1. Design-Tokens (Single Source of Truth)

Zentrale CSS-Custom-Properties in `src/styles.scss` unter `:root`. Komponenten verwenden ausschliesslich `var(--…)` — **keine** hartcodierten Hex-Werte mehr.

```
/* Flächen / Struktur */
--surface-bg:     #F8FAFC;   /* Seitenhintergrund */
--surface-card:   #FFFFFF;   /* Karten, Panels, Topbar */
--surface-muted:  #EEF2F6;   /* Balkenspur, zarte Flächen */
--border:         #E2E8F0;   /* Rahmen, Trenner */

/* Text */
--text:           #0F172A;
--text-muted:     #64748B;
--text-subtle:    #94A3B8;

/* Marke / Akzent (Hard Court Blau) */
--brand:          #2563EB;   /* Primär: Buttons, Links, Balken P1, Akzente */
--brand-strong:   #1D4ED8;   /* Hover, Führungswert, Logo */
--brand-soft:     #EFF6FF;   /* zarte Akzentfläche (z. B. Aufschläger-Panel) */
--brand-border:   #BFDBFE;   /* Rahmen auf brand-soft */

/* Status (fix, unabhängig vom Akzent) */
--success:        #16A34A;   /* Winner, Accepted, Positiv-Balken */
--danger:         #DC2626;   /* Fehler, Doppelfehler, Reject */
--warning:        #D97706;   /* Hinweise/Banner */

/* Form */
--radius-card:    14px;
--radius-control: 10px;
--radius-pill:    99px;
--shadow-card:    0 4px 14px rgba(15,23,42,.05);
--shadow-pop:     0 10px 28px rgba(15,23,42,.12);

/* Spacing-Skala: 4 / 8 / 12 / 16 / 24 (px) */
```

**Material-Theme-Angleichung:** `mat.define-theme` in `styles.scss` wird von Azure/Rose auf ein Blau nahe `--brand` umgestellt, sodass Material-Komponenten (Buttons, Form-Fields, Table, Toolbar) farblich mit den Tokens zusammenpassen. Theme bleibt `theme-type: light`.

## 2. Typografie

- **Font:** Inter (in `index.html` laden, Roboto ersetzen). Body-Font global auf Inter; vorhandene `font-family: sans-serif`-Stellen entfernt.
- **Skala** (global, keine ad-hoc-Grössen mehr):
  - Display 26 / 700, `letter-spacing:-.02em`
  - Heading 19 / 700
  - Body 14 / 400–500
  - Small 12
  - Label 11 / uppercase / `letter-spacing:.06em`, Farbe `--text-muted`
- Statistik-Zahlen in Inter-Bold; Führungswert in `--brand-strong`.

## 3. Komponenten-Muster

Material bleibt, wo bereits genutzt; handgerollte Teile werden auf Tokens umgestellt. Kein neuer Komponenten-Wildwuchs.

- **App-Shell / Toolbar:** `--surface-card`, dünner `--border`-Unterrand, Logo `--brand-strong`, aktiver Nav-Link mit blauem Indikator. Admin-Badge → `--danger`-Chip. Sprachwähler/User-Menü funktional unverändert, neu eingefärbt.
- **Karte/Panel** (`.card`-Muster): `--surface-card`, 1px `--border`, `--radius-card`, `--shadow-card`. Ersetzt heutige `#1e293b`/Gradient-Panels überall.
- **Buttons (4 Varianten):**
  - Primär: `--brand` Fläche, weisser Text, `--radius-control`.
  - Sekundär: weiss + 1px `--border`.
  - Ghost: transparent, `--brand`-Text.
  - Danger: `--danger` Fläche, weisser Text.
  - Material-`mat-button`-Varianten werden aufs selbe Bild getrimmt; die custom `.obs-btn`/`.pts-btn`/`.ctx-btn` der Score-Seite erben dieselben Tokens (Winner = `--success`, Fehler = `--danger`, Kontext-Toggle aktiv = `--brand`/`--brand-soft`).
- **Inputs/Selects:** Material `appearance="outline"`, Fokus-Rahmen `--brand`. Notiz-Textareas (player-notes) hell mit `--border` statt heutigem Dunkel.
- **Statistik-Balken:** Spur `--surface-muted`; Füllung Spieler 1 `--brand`, Spieler 2 `--text-subtle`; Fehler-Balken `--danger`, Positiv-Balken `--success`.
- **Badges/Empfehlungen:** Prioritäts-Badge `--brand`; Empfehlungs-Status Accepted = `--success`-Leiste, Rejected = ausgegraut/durchgestrichen (Logik bleibt).
- **Feedback:** Snackbar/Dialog hell; Status über `--success`/`--danger`.

## 4. Anwendung Seite für Seite

| Seite | Datei | Heute | Neu |
|---|---|---|---|
| App-Shell/Toolbar | `src/app/app.ts` / `app.html` | helle Material-Topbar, Azure | Token-Topbar, `--brand`, aktiver Nav-Indikator |
| Players | `features/players/players.component.ts` | hell, Material, Azure | Tokens; Admin-Banner → `--brand-soft`/`--warning` |
| Match-Setup | `features/matches/match-setup/match-setup.component.ts` | hell, Material | Tokens, Inter, Fokus-Blau |
| **Score (Live)** | `features/matches/score/score.component.ts` | dunkler Blau-Gradient, weisser Text, custom Buttons | **durchgehend hell**: weisse Score-Karte, helle Panels, Winner-grün/Fehler-rot auf Weiss, Aufschläger-Panel `--brand-soft` |
| Statistik (Match) | `features/matches/statistics/statistics.component.ts` | dunkler Gradient, `#eee`, Cyan | hell, Balken/Werte auf Tokens, Führung `--brand-strong` |
| Head-to-Head | `features/statistics/head-to-head/head-to-head.component.ts` | dunkler Gradient, Cyan, white-on-dark Selects | hell, Select-Overrides raus, Prep-Karte hell |
| Match-Analyse | `features/matches/analysis/match-analysis.component.ts` | dunkles Slate `#0f172a`, Cyan | hell, Sektions-/Empfehlungskarten auf Tokens, Notiz-Input hell |
| Player-Notes | `features/matches/notes/player-notes.component.ts` | dunkle Textareas | helle Textareas + `--border` |
| Dialoge | score-edit / end-match / player-dialog | Material hell | Tokens, Button-Varianten |
| Global | `src/styles.scss`, `src/index.html` | Material-Azure-Theme, Roboto, kein theme-color | Tokens + Blau-Theme, Inter, `theme-color`-Meta |

**Konsolidierung der Blautöne:** Material-Azure und Cyan `#0ea5e9` entfallen vollständig zugunsten von `--brand` `#2563EB`.

Die heutigen globalen Overrides in `styles.scss` (`.h2h-panel` dunkles Select-Overlay) werden auf hell/Tokens umgestellt, da H2H nicht mehr dunkel ist.

## 5. Umfang / Nicht-Ziele (YAGNI)

- **Reine Style-Änderungen.** Keine Änderungen an Logik, Daten, Routing, i18n-Keys oder Template-**Struktur**. Selektoren, `data-testid`, `data-status` bleiben erhalten, damit bestehende Tests gültig bleiben.
- **Nur hell.** Tokens dark-fähig benannt, aber **kein** Dark-Mode-Bau, kein Theme-Switcher.
- Kein Umbau der Material-vs-Custom-Aufteilung über das Nötige hinaus.
- Keine neuen Seiten/Features.

## 6. Barrierefreiheit

Kontraste auf WCAG AA geprüft:
- Body-Text `#0F172A` auf `#F8FAFC`/`#FFFFFF`: weit über AA.
- `--brand` `#2563EB` auf Weiss: AA für Bold-Text/Buttons (Button-Text ist weiss auf `--brand` → > 4.5:1).
- Status-Farben `--success`/`--danger`/`--warning` als Flächen mit weissem Text bzw. als Text auf hell: AA.
- Fokuszustände sichtbar (`--brand`-Rahmen).

## 7. Teststrategie

- **Bestehende Suiten bleiben gültig:** Cypress-Component-Specs (75) und Vitest-Unit-Specs (26) prüfen Struktur/Text/`data-testid`/`data-status` — **nicht** Farben. Reine Restyles lassen sie grün. Nach jeder Seiten-Umstellung beide Suiten laufen lassen.
  - `npm run cypress:run` (Component), `npx ng test --watch=false` (Vitest).
- **Risiko:** Specs, die auf inline-Style/Farbe oder auf einen entfernten CSS-Hook prüfen, müssten angepasst werden — bei der Umsetzung pro Seite gezielt prüfen (Grep nach `style=`/Farb-Asserts im jeweiligen `.cy.ts`).
- **Visuelle Regression ist nicht automatisiert.** Finaler manueller Sicht-Check über alle Seiten (Desktop + Handy-Breite) gehört zum DoD.

## 8. DoD

- Alle Seiten beziehen Farben/Typo aus den Tokens; keine der drei Alt-„Welten" mehr sichtbar; ein Blau überall.
- `npm run cypress:run` + Vitest grün.
- Manueller Sicht-Check aller Seiten (inkl. Handy-Breite, courtside Score-Erfassung) ok.
