# UI Design-System „Daylight · Hard Court" Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the three divergent visual "worlds" of the TSaS frontend with one consistent, modern light design system driven by central CSS tokens.

**Architecture:** A token-first restyle. Task 1 establishes `:root` design tokens + a blue Material M3 theme + the Inter font in `src/styles.scss` / `src/index.html`. Every subsequent task restyles one page/area by replacing hardcoded colours in its existing inline `styles: [...]` block with `var(--…)` tokens and converting the dark pages to light — **without** changing templates, selectors, `data-testid`/`data-cy` hooks, `data-status`, routing, i18n, or behaviour.

**Tech Stack:** Angular 21 standalone components, Angular Material M3, SCSS, Inter (Google Fonts), Vitest (unit), Cypress (component tests).

## Global Constraints

- **Pure restyle.** No changes to templates' DOM structure, selectors, `data-testid`/`data-cy`, `data-status`, component logic, routing, or i18n keys. Only colours/typography/spacing/borders/shadows change.
- **Single source of truth:** all colours come from the `:root` tokens defined in Task 1. No new hardcoded hex values in components after a page is migrated.
- **Token values (verbatim):** `--surface-bg:#F8FAFC` · `--surface-card:#FFFFFF` · `--surface-muted:#EEF2F6` · `--border:#E2E8F0` · `--text:#0F172A` · `--text-muted:#64748B` · `--text-subtle:#94A3B8` · `--brand:#2563EB` · `--brand-strong:#1D4ED8` · `--brand-soft:#EFF6FF` · `--brand-border:#BFDBFE` · `--success:#16A34A` · `--danger:#DC2626` · `--warning:#D97706` · `--radius-card:14px` · `--radius-control:10px` · `--radius-pill:99px` · `--shadow-card:0 4px 14px rgba(15,23,42,.05)`.
- **One blue only:** Material Azure and Cyan `#0ea5e9`/`#38bdf8` are fully removed in favour of `--brand`.
- **Status colours fixed:** Winner/Accepted/positive = `--success`; Fehler/Doppelfehler/Reject = `--danger`; Hinweise = `--warning` — independent of the brand accent.
- **Tests must stay green:** Cypress component specs (75) and Vitest unit specs (26). A pure restyle should not break them; if a spec asserts a colour or a removed CSS hook, fix the spec minimally (it is testing structure, not visuals).
- **Light only.** Tokens are named to allow a future dark mode, but no dark mode / theme switcher is built (YAGNI).
- **Font:** Inter everywhere; no `font-family: sans-serif` or Roboto left in migrated files.

## Commands (run from `frontend/`)

- One Cypress spec: `npx cypress run --component --spec "<path>"`
- All Cypress: `npm run cypress:run`
- Vitest (all, headless): `npx ng test --watch=false`
- Dev server for visual check: `npm start` (then open the page in a browser at desktop + ~390px mobile width)

## File Structure

| File | Responsibility |
|---|---|
| `src/styles.scss` | Tokens, Material blue theme, Inter base, light `.h2h-panel` override (Task 1) |
| `src/index.html` | Inter font load, `theme-color` meta (Task 1) |
| `src/app/app.ts` + `app.html` + `app.scss` | App shell / toolbar (Task 2) |
| `src/app/features/players/players.component.ts` (+ `player-dialog.component.ts`) | Players page (Task 3) |
| `src/app/features/matches/match-setup/match-setup.component.ts` | Match setup (Task 3) |
| `src/app/features/matches/score/score.component.ts` | Live score (Task 4) |
| `src/app/features/matches/statistics/statistics.component.ts` | Match statistics (Task 5) |
| `src/app/features/statistics/head-to-head/head-to-head.component.ts` | Head-to-Head (Task 6) |
| `src/app/features/matches/analysis/match-analysis.component.ts` | Match analysis (Task 7) |
| `src/app/features/matches/notes/player-notes.component.ts` | Notes panel (Task 7) |
| `src/app/features/matches/score/score-edit-dialog.component.ts`, `end-match-dialog.component.ts`, `players/player-dialog.component.ts` | Dialogs (Task 8) |

## How to migrate a page (applies to Tasks 2–8)

1. Open the component's `.ts` (styling lives in the inline `styles: [\`…\`]` block) and its `.html` only if a colour is set inline there.
2. Replace every hardcoded colour with the token from the mapping table in that task. Replace any `font-family: sans-serif` with nothing (inherits Inter) or `font-family: 'Inter', system-ui, sans-serif`.
3. For dark pages: remove the dark/gradient background and white text — set the page container to `background: var(--surface-bg); color: var(--text);` and panels to `background: var(--surface-card); border: 1px solid var(--border);`.
4. Do **not** touch DOM structure, class names used by tests, `data-testid`/`data-cy`, `data-status`, `@if`/`@for`, bindings, or text.
5. Run that page's Cypress spec (and Vitest spec if one exists) → must stay green. Then a visual check at desktop + mobile width.

---

## Task 1: Foundations — tokens, Material blue theme, Inter font

**Files:**
- Modify: `frontend/src/styles.scss` (full rewrite below)
- Modify: `frontend/src/index.html` (font + meta)

**Interfaces:**
- Produces: the `:root` CSS custom properties listed in Global Constraints, consumed by every later task via `var(--…)`. The light `.h2h-panel` override consumed by Task 6.

- [ ] **Step 1: Rewrite `src/styles.scss`**

Replace the entire file with:

```scss
@use '@angular/material' as mat;

$theme: mat.define-theme((
  color: (
    theme-type: light,
    primary: mat.$blue-palette,
    tertiary: mat.$blue-palette,
  ),
  typography: (
    plain-family: ('Inter', system-ui, sans-serif),
    brand-family: ('Inter', system-ui, sans-serif),
  ),
));

:root {
  @include mat.all-component-themes($theme);

  /* ===== Design tokens — TSaS „Daylight · Hard Court" ===== */
  /* Flächen / Struktur */
  --surface-bg: #F8FAFC;
  --surface-card: #FFFFFF;
  --surface-muted: #EEF2F6;
  --border: #E2E8F0;

  /* Text */
  --text: #0F172A;
  --text-muted: #64748B;
  --text-subtle: #94A3B8;

  /* Marke / Akzent (Hard Court Blau) */
  --brand: #2563EB;
  --brand-strong: #1D4ED8;
  --brand-soft: #EFF6FF;
  --brand-border: #BFDBFE;

  /* Status (fix) */
  --success: #16A34A;
  --danger: #DC2626;
  --warning: #D97706;

  /* Form */
  --radius-card: 14px;
  --radius-control: 10px;
  --radius-pill: 99px;
  --shadow-card: 0 4px 14px rgba(15, 23, 42, .05);
  --shadow-pop: 0 10px 28px rgba(15, 23, 42, .12);
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  font-family: 'Inter', system-ui, 'Helvetica Neue', sans-serif;
  background: var(--surface-bg);
  color: var(--text);
}

a {
  text-decoration: none;
}

/* Head-to-Head Spieler-Auswahl: helles Dropdown-Panel auf Tokens
   (Overlay liegt ausserhalb der Komponente, daher global per panelClass gescoped) */
.h2h-panel.mat-mdc-select-panel {
  background: var(--surface-card);
}
.h2h-panel .mat-mdc-option .mdc-list-item__primary-text {
  color: var(--text);
}
.h2h-panel .mat-mdc-option:hover:not(.mdc-list-item--disabled),
.h2h-panel .mat-mdc-option.mat-mdc-option-active {
  background: var(--brand-soft);
}
.h2h-panel .mat-mdc-option.mdc-list-item--selected:not(.mat-mdc-option-multiple) {
  background: var(--brand-soft);
}
```

> Note: `mat.$blue-palette` is a prebuilt Angular Material M3 palette (a hard-court blue). If a later visual check shows the Material primary drifting noticeably from `#2563EB`, that is acceptable for v1 — the custom tokens drive all non-Material surfaces. Do not hand-roll a custom palette unless asked.

- [ ] **Step 2: Update `src/index.html`**

Replace the Roboto stylesheet link:
```html
  <link href="https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;500&display=swap" rel="stylesheet">
```
with the Inter link, and add a `theme-color` meta. The `<head>` font/meta region becomes:
```html
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="theme-color" content="#FFFFFF">
  <link rel="icon" type="image/svg+xml" href="favicon.svg">
  <link rel="alternate icon" type="image/x-icon" href="favicon.ico">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">
```
Leave `<body class="mat-typography">` unchanged.

- [ ] **Step 3: Build sanity + full suites stay green**

Run:
```
npx ng test --watch=false
npm run cypress:run
```
Expected: Vitest 26/26, Cypress 75/75 still pass (tokens/theme/font do not change structure or text). If a spec fails purely because it asserted the old Roboto/Material colour, fix the spec minimally; otherwise investigate.

- [ ] **Step 4: Visual check**

`npm start`, open the app. Toolbar/buttons should now read as a blue Material theme; body font is Inter. (Pages still carry their own dark styles until later tasks — that's expected.)

- [ ] **Step 5: Commit**

```bash
git add frontend/src/styles.scss frontend/src/index.html
git commit -m "feat(ui): design tokens, blue Material theme, Inter font (design system)"
```

---

## Task 2: App shell / toolbar

**Files:**
- Modify: `frontend/src/app/app.ts` (inline `styles: [...]`), `frontend/src/app/app.html` (only if inline colours), `frontend/src/app/app.scss` (if it carries shell styles)

**Interfaces:**
- Consumes: tokens from Task 1.

- [ ] **Step 1: Inspect the shell styles**

Read `app.ts` (the `styles: [\`…\`]` block), `app.html`, and `app.scss`. Identify colour declarations: the `mat-toolbar color="primary"` (now blue via theme), `.app-title`, `.active-link`, `.admin-badge` (currently `#c62828`), `.user-name`, `.danger-item`, the `<main>` background (currently `#f5f5f5`).

- [ ] **Step 2: Apply token mapping**

| Element | Current | New |
|---|---|---|
| `<main>` background | `#f5f5f5` | `var(--surface-bg)` |
| Toolbar | Material primary (azure) | leave `color="primary"` (now blue); if a custom bg hex exists, → `var(--surface-card)` with `border-bottom: 1px solid var(--border)` and title/links in `var(--text)`/`var(--brand-strong)` |
| `.app-title` | inherited | `color: var(--brand-strong); font-weight: 800;` (only if currently coloured) |
| `.active-link` | Material default | underline/indicator in `var(--brand)` (e.g. `border-bottom: 2px solid var(--brand)`) |
| `.admin-badge` | `#c62828` | `var(--danger)` |
| `.danger-item` | red-ish | `color: var(--danger)` |

> If the existing toolbar is the default Material `color="primary"` bar (coloured blue by the theme) and you prefer the spec's white toolbar look, set the toolbar background to `var(--surface-card)`, text to `var(--text)`, and the active link indicator to `var(--brand)` via the inline styles. Keep `data-cy` hooks (`lang-menu-trigger`, `user-menu-trigger`, `admin-badge`, `lang-*`, `export-my-data`, `delete-my-data`) and all template structure unchanged.

- [ ] **Step 3: Verify**

Run `npx ng test --watch=false` (covers `app.spec.ts`). Expected: green. Visual check the toolbar at desktop + mobile width — logo, nav, language picker, admin badge, user menu all legible on the new surface.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/app.ts frontend/src/app/app.html frontend/src/app/app.scss
git commit -m "feat(ui): restyle app shell to design tokens (design system)"
```

---

## Task 3: Players + Match-Setup (light Material pages)

**Files:**
- Modify: `frontend/src/app/features/players/players.component.ts`
- Modify: `frontend/src/app/features/matches/match-setup/match-setup.component.ts`

**Interfaces:**
- Consumes: tokens from Task 1.

These are already light Material pages — the work is small: align stray hardcoded colours to tokens and ensure Inter.

- [ ] **Step 1: Players — map colours**

Open `players.component.ts`. Apply:

| Element | Current | New |
|---|---|---|
| Page container bg (if any) | `#f5f5f5`/white | `var(--surface-bg)` |
| Cards | Material default | leave Material; if custom, `var(--surface-card)` + `1px solid var(--border)` + `var(--radius-card)` |
| Admin filter banner | bg `#fff3e0`, border `#ffcc80`, text `#5d4037` | bg `var(--brand-soft)`, border `1px solid var(--brand-border)`, text `var(--text)` (keep the warning semantics with `var(--warning)` for the icon if present) |
| Any heading font-size / sans-serif | ad-hoc | leave sizes; ensure no `sans-serif`/Roboto override (inherits Inter) |

Do not touch the `mat-table`, sort, `data-cy`/`data-testid`, or row actions.

- [ ] **Step 2: Match-Setup — map colours**

Open `match-setup.component.ts`. It is mostly Material defaults; replace any hardcoded background or text colour with `var(--surface-bg)`/`var(--text)`, ensure form-field focus reads blue (theme handles it), remove any `font-family: sans-serif`.

- [ ] **Step 3: Verify**

```
npx cypress run --component --spec "src/app/features/players/players.component.cy.ts"
npx cypress run --component --spec "src/app/features/matches/match-setup/match-setup.component.cy.ts"
```
Expected: 17/17 and 7/7 green. Visual check both pages.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/features/players/players.component.ts frontend/src/app/features/matches/match-setup/match-setup.component.ts
git commit -m "feat(ui): align players + match-setup to tokens (design system)"
```

---

## Task 4: Score (live entry) — dark gradient → light

**Files:**
- Modify: `frontend/src/app/features/matches/score/score.component.ts`

**Interfaces:**
- Consumes: tokens from Task 1.

This is the largest restyle. The page currently uses a US-Open blue gradient with white text and bespoke buttons. Convert to the light system while keeping the full-screen courtside layout (`100dvh`, 2-column panels, large touch targets).

- [ ] **Step 1: Map the score-page colours**

Open `score.component.ts` (big inline `styles` block). Apply:

| Element / current | New |
|---|---|
| `.scoring-page` bg `linear-gradient(160deg,#103A6B,#2D72B8)`, `color: white` | `background: var(--surface-bg); color: var(--text);` |
| `.score-header` bg `rgba(0,0,0,.8)`, border `rgba(255,255,255,.1)` | `background: var(--surface-card); border-bottom: 1px solid var(--border);` |
| `.player-name` `rgba(255,255,255,.75)`; `.player-name.serving` `#4ade80` | `var(--text-muted)`; serving → `var(--brand-strong)` |
| `.pts-btn` `color:white`; `.pts-btn.p1` `#4ade80` | `var(--text)`; `.p1` → `var(--brand-strong)`; hover bg `var(--surface-muted)` |
| `.games-num.winning` `#4ade80` | `var(--brand-strong)` |
| `.obs-panel` bg `rgba(255,255,255,.05)`, border `rgba(255,255,255,.12)` | `background: var(--surface-card); border: 1px solid var(--border);` |
| `.obs-panel.serving` bg `rgba(74,222,128,.08)`, border `rgba(74,222,128,.3)` | `background: var(--brand-soft); border-color: var(--brand-border);` |
| `.panel-title` `rgba(255,255,255,.7)`; serving variant `#4ade80` | `var(--text-muted)`; serving → `var(--brand-strong)` |
| `.section-label.green` `#4ade80`; `.section-label.red` `#fca5a5` | `var(--success)`; `var(--danger)` |
| `.ctx-btn` `rgba(254,240,138,.1)` bg, `#fef08a` text; `.ctx-btn.active` bg `#854d0e`, border `#fef08a`, text `#fef08a` | inactive: `background:#fff; border:1px solid var(--border); color:var(--text-muted)`; active: `background: var(--brand-soft); border-color: var(--brand); color: var(--brand-strong); font-weight:600;` |
| `.win-btn` bg `#166534`, border `#4ade80`, text `#4ade80` | `background: var(--success); border: none; color: #fff;` |
| `.err-btn` bg `#7f1d1d`, border `#fca5a5`, text `#fca5a5` | `background: #fff; border: 1px solid var(--danger); color: var(--danger);` |
| `.winner-overlay` bg `rgba(0,0,0,.6)` | `background: rgba(15,23,42,.45)` |
| `.winner-card` bg `linear-gradient(135deg,#1a237e,#283593)`, white text | `background: var(--surface-card); color: var(--text); box-shadow: var(--shadow-pop);` with `.winner-text` in `var(--brand-strong)`, `.final-score` in `var(--text-muted)` |
| `.loading` `rgba(255,255,255,.5)` | `var(--text-subtle)` |
| `font-family: sans-serif` | remove (inherits Inter) |

Keep all class names, `@if/@for`, button handlers, and the grid/`100dvh` layout intact.

- [ ] **Step 2: Verify**

```
npx cypress run --component --spec "src/app/features/matches/score/score.component.cy.ts"
npx ng test --watch=false
```
Expected: Cypress 25/25 and Vitest 26/26 green. The Vitest `score.component.spec.ts` and the Cypress spec assert structure/`data-testid`, not colours.

- [ ] **Step 3: Visual check (mobile is primary)**

`npm start`, open a match's score page at ~390px width. Verify: score header legible on white, serving panel reads blue, Winner buttons solid green, Fehler buttons white/red-outline, context toggles blue when active, winner overlay light. Touch targets unchanged in size.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/features/matches/score/score.component.ts
git commit -m "feat(ui): restyle live score page to light tokens (design system)"
```

---

## Task 5: Match statistics — dark gradient → light

**Files:**
- Modify: `frontend/src/app/features/matches/statistics/statistics.component.ts`

**Interfaces:**
- Consumes: tokens from Task 1.

- [ ] **Step 1: Map the statistics colours**

Open `statistics.component.ts`. Apply:

| Element / current | New |
|---|---|
| `.page` bg `linear-gradient(160deg,#103A6B,#2D72B8)`, `color:#eee`, `font-family:sans-serif` | `background: var(--surface-bg); color: var(--text);` (drop `sans-serif`) |
| Set badge bg `#1e293b`, text `#94a3b8` | `background: var(--surface-muted); color: var(--text-muted);` |
| Winner set badge bg `#0ea5e9`, text `#000` | `background: var(--brand); color: #fff;` |
| Stat value left `#0ea5e9` / leading `#eee` | `var(--brand-strong)` / leading `var(--text)` |
| Stat value right `#94a3b8` / leading `#eee` | `var(--text-subtle)` / leading `var(--text)` |
| `.bar-p1` `#0ea5e9` | `var(--brand)` |
| `.bar-p2` `#475569` | `var(--text-subtle)` |
| bar track (container) dark | `var(--surface-muted)` |
| `.bar-err` `#f87171` | `var(--danger)` |
| `.bar-good` `#4ade80` | `var(--success)` |
| Divider `border-top: 1px solid #1e293b` | `1px solid var(--border)` |

Buttons already use Material `color="primary"` (now blue) — leave them.

- [ ] **Step 2: Verify**

```
npx cypress run --component --spec "src/app/features/matches/statistics/statistics.component.cy.ts"
```
Expected: 9/9 green. Visual check.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/features/matches/statistics/statistics.component.ts
git commit -m "feat(ui): restyle match statistics to light tokens (design system)"
```

---

## Task 6: Head-to-Head — dark gradient → light

**Files:**
- Modify: `frontend/src/app/features/statistics/head-to-head/head-to-head.component.ts`

**Interfaces:**
- Consumes: tokens from Task 1 (incl. the light `.h2h-panel` override already applied globally).

- [ ] **Step 1: Map the H2H colours**

Open `head-to-head.component.ts`. Apply:

| Element / current | New |
|---|---|
| `.page` bg `linear-gradient(160deg,#103A6B,#2D72B8)`, `color:#eee` | `background: var(--surface-bg); color: var(--text);` |
| `.pickers .mat-mdc-select-value { color:#fff }` and outline `rgba(255,255,255,.45)` | remove these overrides (light theme handles select text/outline); or set value `color: var(--text)` |
| Section titles `#38bdf8`, uppercase | `var(--brand-strong)` |
| `.val-left` `#0ea5e9` | `var(--brand-strong)` |
| `.val-right` `#94a3b8` / leading `#eee` | `var(--text-subtle)` / `var(--text)` |
| `.bar-p1` `#0ea5e9` / `.bar-p2` `#475569` | `var(--brand)` / `var(--text-subtle)` |
| bar track dark | `var(--surface-muted)` |
| Prep card bg `rgba(15,23,42,.55)`, border `rgba(56,189,248,.35)` | `background: var(--surface-card); border: 1px solid var(--brand-border);` + `box-shadow: var(--shadow-card)` |
| Prep headings `#38bdf8`, body `#e2e8f0` | `var(--brand-strong)`, `var(--text)` |
| Buttons `color:#fff; background:#0ea5e9` | `background: var(--brand); color: #fff;` |

The global `.h2h-panel` dropdown is already light (Task 1). Keep the `panelClass="h2h-panel"` binding.

- [ ] **Step 2: Verify**

```
npx cypress run --component --spec "src/app/features/statistics/head-to-head/head-to-head.component.cy.ts"
```
Expected: 4/4 green. Visual check, including opening the player select dropdown (now light).

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/features/statistics/head-to-head/head-to-head.component.ts
git commit -m "feat(ui): restyle head-to-head to light tokens (design system)"
```

---

## Task 7: Match analysis + Player-Notes panel — dark slate → light

**Files:**
- Modify: `frontend/src/app/features/matches/analysis/match-analysis.component.ts`
- Modify: `frontend/src/app/features/matches/notes/player-notes.component.ts`

**Interfaces:**
- Consumes: tokens from Task 1. (Player-notes is embedded in both analysis and score; restyling it here makes it consistent in both hosts — Task 4 already lightened the score surface around it.)

- [ ] **Step 1: Map the analysis colours**

Open `match-analysis.component.ts`. Apply:

| Element / current | New |
|---|---|
| `:host` bg `#0f172a`, `color:#eee`, `font-family:sans-serif` | `background: var(--surface-bg); color: var(--text);` (drop `sans-serif`) |
| `.subtitle` `#94a3b8`; `.meta` `#64748b` | `var(--text-subtle)`; `var(--text-muted)` |
| `.section` / `.rec` bg `#1e293b` | `background: var(--surface-card); border: 1px solid var(--border);` |
| `.section-label` `#0ea5e9` | `var(--brand-strong)` |
| `.section-text` `#e2e8f0` | `var(--text)` |
| `.rec-prio` bg `#0ea5e9`, text `#001018` | `background: var(--brand); color: #fff;` |
| `.rec-title` `#f1f5f9` | `var(--text)` |
| `.rec-detail` `#cbd5e1` | `var(--text-muted)` |
| `.rec[data-status="ACCEPTED"]` border `#22c55e` | `var(--success)` |
| `.rec[data-status="REJECTED"]` opacity/line-through | keep (semantic), no colour change needed |
| `.rec-note` `#fca5a5` | `var(--danger)` |
| `.rec-note-input` bg `#0f172a`, text `#e2e8f0`, border `#334155` | `background: #fff; color: var(--text); border: 1px solid var(--border);` |
| `.error-box` bg `#422`, border `#7f1d1d`, text `#fecaca` | `background: #FEF2F2; border: 1px solid var(--danger); color: var(--danger);` |

Keep `data-testid`/`data-status` and the recommendation/review structure intact.

- [ ] **Step 2: Map the player-notes colours**

Open `player-notes.component.ts`. The textareas are currently dark-on-translucent. Apply:

| Element / current | New |
|---|---|
| textarea bg `rgba(255,255,255,.04)`, border `rgba(127,127,127,.4)`, inherited text | `background: var(--surface-card); border: 1px solid var(--border); color: var(--text);` |
| `.slot-label` / `.slot-role` muted | `var(--text)` / `var(--text-subtle)` |
| `.saved-hint` `#4ade80` | `var(--success)` |

Keep `data-testid` (`note-input-*`, `note-save-*`) and the two-column/`@media` layout.

- [ ] **Step 3: Verify**

```
npx cypress run --component --spec "src/app/features/matches/analysis/match-analysis.component.cy.ts"
npx cypress run --component --spec "src/app/features/matches/notes/player-notes.component.cy.ts"
```
Expected: 11/11 (some specs report 7 — use the actual count) and 2/2 green. Visual check the analysis page and the notes panel inside both analysis and score.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/features/matches/analysis/match-analysis.component.ts frontend/src/app/features/matches/notes/player-notes.component.ts
git commit -m "feat(ui): restyle analysis + notes panel to light tokens (design system)"
```

---

## Task 8: Dialogs + final verification

**Files:**
- Modify: `frontend/src/app/features/matches/score/score-edit-dialog.component.ts`
- Modify: `frontend/src/app/features/matches/score/end-match-dialog.component.ts`
- Modify: `frontend/src/app/features/players/player-dialog.component.ts`

**Interfaces:**
- Consumes: tokens from Task 1.

- [ ] **Step 1: Map dialog colours**

Open each dialog component. They are Material dialogs; replace any hardcoded background/text/accent hex with tokens: dialog surface `var(--surface-card)`, text `var(--text)`, primary action buttons blue (`color="primary"` via theme, or `background: var(--brand)`), destructive actions `var(--danger)`. Remove any `font-family: sans-serif`. Keep all `data-cy`/`data-testid` and template structure.

- [ ] **Step 2: Full-suite verification (DoD gate)**

Run both suites in full:
```
npx ng test --watch=false
npm run cypress:run
```
Expected: Vitest 26/26, Cypress 75/75 all green.

- [ ] **Step 3: Manual visual sweep (DoD gate)**

`npm start` and walk every page at desktop **and** ~390px mobile width: Players, Match-Setup, Score (live), Match-Statistics, Head-to-Head (open the select), Match-Analysis (with recommendations + notes), dialogs (score edit, end match, create/edit player). Confirm: one light system, one blue (`#2563EB`), Inter everywhere, no leftover dark gradient/slate/cyan, status colours correct. Note any page that still shows an old colour and fix it before finishing.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/features/matches/score/score-edit-dialog.component.ts frontend/src/app/features/matches/score/end-match-dialog.component.ts frontend/src/app/features/players/player-dialog.component.ts
git commit -m "feat(ui): restyle dialogs to tokens + final design-system pass (design system)"
```

---

## Self-Review (author checklist — completed)

**Spec coverage:**
- §1 Tokens + Material theme → Task 1. §2 Typography (Inter) → Task 1. §3 Component patterns → applied across Tasks 2–8 via the token mappings. §4 Page-by-page table → Tasks 2 (shell), 3 (players + match-setup), 4 (score), 5 (statistics), 6 (h2h), 7 (analysis + notes), 8 (dialogs); `styles.scss`/`index.html`/`.h2h-panel` → Task 1/6. §5 YAGNI (light only, no structural change) → Global Constraints + per-task "do not touch" notes. §6 Accessibility → token values chosen for AA (verified in spec). §7/§8 Testing/DoD → per-task verify steps + Task 8 full gate + manual sweep. No gaps.

**Placeholder scan:** No TBD/TODO; every page task carries a concrete colour→token mapping table and exact commands. The mappings reference the actual current hex values captured in the UI audit; the implementer applies them in the existing inline `styles` blocks.

**Type/name consistency:** Token names are identical across all tasks and match Task 1's `:root` definitions verbatim (`--surface-bg`, `--surface-card`, `--surface-muted`, `--border`, `--text`, `--text-muted`, `--text-subtle`, `--brand`, `--brand-strong`, `--brand-soft`, `--brand-border`, `--success`, `--danger`, `--warning`, `--radius-card`, `--radius-control`, `--radius-pill`, `--shadow-card`, `--shadow-pop`).
