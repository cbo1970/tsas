# Final fix wave — mobile portrait UI review findings

Worktree: `/Users/cbo/Projects/cas/tsas/.claude/worktrees/mobile-portrait-ui`
Branch: `worktree-mobile-portrait-ui`

Commits (on top of `8c7db56`):

| SHA | Subject |
|---|---|
| `026eb07` | fix(score): Schnellpunkt raeumt Aufschlagversuch und Schlagart auf |
| `136e129` | fix(score): Erfassungsmaske im Querformat nicht mehr abschneiden |
| `e240ea7` | fix(ui): keine Spieler-Dubletten, responsiver Beenden-Dialog, Dialogname |

Test totals: **Cypress 172 passing across 12 specs** (baseline 162, +10 new tests),
**Vitest 37 passing**. Production build (`ng build --configuration production`) succeeds;
the only warning is the pre-existing initial-bundle budget warning.

---

## C1 (Critical) — quick point left serve attempt and stroke armed

### What changed
`frontend/src/app/features/matches/score/score.component.ts`

`recordQuickPoint`'s `next` handler now mirrors `recordObservation`:

```ts
next: (updated) => {
  this.handlePointResponse(updated);
  // Auch der Schnellpunkt ist ein erfasster Punkt: Aufschlagversuch und
  // Schlagart gehoeren zu ihm und duerfen nicht am naechsten mitreisen.
  this.lastObservation.set('QUICK');
  this.resetPointContext();
},
```

### RED proof
```
$ cd frontend && npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts

  1) ScoreComponent — service context
       schickt nach einem Schnellpunkt keinen alten Kontext mit:
      Timed out retrying after 4000ms
      + expected - actual
      -'true'
      +'false'

  2) ScoreComponent — untere Leiste und Notizen
       behauptet nach einem Schnellpunkt nicht mehr die alte Beobachtung:
     AssertionError: Timed out retrying after 4000ms: expected
     '<span.last-observation>' not to contain 'Winner'

  Tests: 62  Passing: 60  Failing: 2
```

### Covering tests
`score.component.cy.ts`
- `ScoreComponent — service context` › **`schickt nach einem Schnellpunkt keinen alten Kontext mit`** —
  selects `2. Service` + `Rückhand`, ends the point with `quick-score-p1`, asserts both
  `aria-pressed` flags fall back to `false`, then records the *next* point via `obs-WINNER`
  and asserts the request body carries neither `serveAttempt` nor `strokeType`. This is the
  mirror of the existing `'schickt nach einem Punkt vom anderen Panel keinen alten Kontext mit'`.
- `ScoreComponent — untere Leiste und Notizen` › **`behauptet nach einem Schnellpunkt nicht mehr die alte Beobachtung`** (M5).

### GREEN
`score.component.cy.ts`: 62/62 passing immediately after the fix.

### M5 — which option I chose
I made the bottom strip **truthful** rather than blank. `lastObservation` is now
`signal<PointType | 'QUICK' | null>`; a quick point sets `'QUICK'` and
`lastObservationText` renders **„Zuletzt: Punkt ohne Zuordnung"**.

Rationale for not simply clearing it to `null`: `null` renders „Noch nichts erfasst", which is
itself a lie after a point *was* recorded — on a screen the coach only glances at, that reads as
„my tap did not register" and invites a double entry. „Punkt ohne Zuordnung" says exactly what
happened: a point was stored, with no attribute on it.

---

## I1 (Important) — scoring screen clipped unreachable tiles in landscape

### What changed
`score.component.ts`, styles block — an escape hatch for flat viewports:

```scss
@media (max-height: 560px) {
  :host { height: auto; min-height: 100dvh; }
  .scoring-page { height: auto; min-height: 100dvh; overflow: visible; }
  .panels-area { overflow: visible; }
}
```

`.panels-area` gets `overflow: visible` rather than `auto`: with the page itself no longer
height-capped, the content pushes the document down and the *document* scrolls, which is the
one scroll a one-handed user will actually find. An inner `auto` scroller would have produced a
second, nested scroll area inside a page that also scrolls. I verified the surrounding shell
does not re-trap it: `body` (`src/styles.scss:53`) sets no `overflow`, and `main`
(`app.ts` styles, `main { flex: 1; }`) sets neither `overflow` nor a fixed height.

### The reviewer's suggested test does not actually test this — note
The brief proposed clicking `obs-NET` / `stroke-BACKHAND` at 800×360, on the grounds that
"Cypress' visibility check honours ancestor overflow clipping". **I wrote that test first and it
passed against the unfixed code.** Cypress calls `scrollIntoView()` before clicking, and
`scrollIntoView` scrolls an `overflow: hidden` container perfectly well — such a container is
programmatically scrollable even though the user cannot scroll it. So the click is green on a
layout where the tile is unreachable in the real app.

I therefore added a helper that asserts the clipping itself, with the click test kept as a
secondary smoke check:

```ts
function assertNotClipped(testid: string) {
  cy.get(`[data-testid="${testid}"]`).then($el => {
    const el = $el[0];
    const rect = el.getBoundingClientRect();
    for (let a: HTMLElement | null = el.parentElement; a; a = a.parentElement) {
      const overflowY = getComputedStyle(a).overflowY;
      if (overflowY !== 'hidden' && overflowY !== 'clip') continue;
      const box = a.getBoundingClientRect();
      const name = a.getAttribute('data-testid') ?? a.className ?? a.tagName;
      expect(rect.bottom, `${testid} wird von "${name}" abgeschnitten`).to.be.at.most(box.bottom + 1);
    }
  });
}
```

It walks the ancestor chain without scrolling anything and fails if an un-scrollable ancestor
cuts the element off.

### RED proof
```
$ npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts

    1) schneidet im Querformat (800x360) keine Kachel unerreichbar ab
    ✓ laesst im Querformat (800x360) die untere Kachel erfassen (196ms)

  63 passing, 1 failing
       schneidet im Querformat (800x360) keine Kachel unerreichbar ab:
       obs-NET wird von "panels-area" abgeschnitten
```

Note the second line: the click-based test the brief proposed was **green while RED was true**.

### Covering tests
`score.component.cy.ts` › `ScoreComponent — ein Panel`:
- **`schneidet im Querformat (800x360) keine Kachel unerreichbar ab`** — `assertNotClipped` on
  `obs-NET` and `stroke-BACKHAND`.
- **`laesst im Querformat (800x360) die untere Kachel erfassen`** — selects `stroke-BACKHAND`,
  taps `obs-NET`, asserts `pointType === 'NET'` reaches the API.

### GREEN / no portrait regression
```
✓ streckt die Kacheln des Aufschläger-Panels nicht über ihre natürliche Höhe (33ms)
✓ streckt die Kacheln des Returnierenden-Panels nicht über ihre natürliche Höhe (101ms)
✓ scrollt auf 360x692 nicht (30ms)
✓ schneidet im Querformat (800x360) keine Kachel unerreichbar ab (52ms)
✓ laesst im Querformat (800x360) die untere Kachel erfassen (196ms)
64 passing
```
692 > 560, so the media query never applies in portrait; the existing
`scrollt auf 360x692 nicht` assertion still holds.

---

## I2 (Important) — "neu anlegen" offered for existing players

### What changed
`match-setup.component.ts` / `.html`

- New file-level helper `matchesName(player, term)`, shared by the filter and the new gate.
- New private signal `playersLoaded`, set to `true` only in `getPlayers()`'s `next` handler
  (left `false` on error).
- Two new public computeds driving the template:

```ts
showCreatePlayer1 = computed(() => this.canCreatePlayer(this.player1Query()));
showCreatePlayer2 = computed(() => this.canCreatePlayer(this.player2Query()));

private canCreatePlayer(query: string): boolean {
  const term = query.toLowerCase().trim();
  if (!term || !this.playersLoaded()) return false;
  return !this.players().some(p => matchesName(p, term));
}
```

- `match-setup.component.html`: the two `@empty` branches now read
  `@if (showCreatePlayer1())` / `@if (showCreatePlayer2())` instead of
  `@if (player1Query().trim())` / `@if (player2Query().trim())`.

**One deliberate deviation from the brief.** The brief's sketch was "non-empty trimmed query
**and the loaded list is non-empty** and no player in the full list matches". Testing
`players().length > 0` would also have silenced the create option for a brand-new coach who
genuinely has zero players — the one case where creating from the name field is most useful.
An explicit `playersLoaded` flag closes exactly the stated hole (in-flight **or** failed request)
without that collateral damage, and the brief's own deferred-twin test passes against it. The
brief said "e.g.", so I read this as within the latitude given.

### RED proof
```
$ npx cypress run --component --spec src/app/features/matches/match-setup/match-setup.component.cy.ts

    1) bietet das Anlegen nicht an, wenn der Spieler ein laufendes Match hat (Spieler 1)
    2) bietet das Anlegen nicht an, wenn der Spieler ein laufendes Match hat (Spieler 2)
    3) bietet das Anlegen nicht an, solange die Spielerliste noch laedt

  20 passing, 3 failing

  AssertionError: Timed out retrying after 4000ms: Expected
  <mat-option#mat-option-a45.mat-mdc-option.mdc-list-item.create-player-option>
  not to exist in the DOM, but it was continuously found.
```

(First run of the deferred test used a 1000 ms intercept delay and passed trivially — the
request had already resolved by the time the assertion ran. Raised to 6000 ms, which puts the
whole 4 s retry window inside the in-flight period, and it then went RED as above.)

### Covering tests
`match-setup.component.cy.ts` › `MatchSetupComponent — Spieler aus der Namenssuche anlegen`:
- **`bietet das Anlegen nicht an, wenn der Spieler ein laufendes Match hat (Spieler 1)`** —
  types `Nadal` (fixture `BUSY` has `activeMatchId: 'match-99'`), asserts no create option.
- **`… (Spieler 2)`** — same for the second field.
- **`bietet das Anlegen nicht an, solange die Spielerliste noch laedt`** — intercepts
  `GET /api/players` with `delay: 6000`, mounts without waiting, types `Federer`, asserts no
  create option during the in-flight window; then waits for the response and asserts the option
  is still absent while `Federer Roger` is offered as a normal match.

The spec's `mountSetup` was split into `mountComponent()` (mount only) + `mountSetup()`
(intercept, mount, wait) so the deferred case can mount without waiting.

### GREEN
`match-setup.component.cy.ts`: 23/23 passing.

---

## I3 (Important) — end-match dialog was never made responsive

### What changed
`score.component.ts`, `endMatch()`:

```ts
const ref = this.dialog.open(EndMatchDialogComponent, {
  ...responsiveDialogConfig(this.layout.isHandset()),
  data: { player1Name: this.player1Name(), player2Name: this.player2Name() }
});
```

`data` is preserved; `width: '360px'` is gone.

### Where the test lives — and why
The brief is right that `end-match-dialog.component.cy.ts` mounts the component directly and can
never catch this: the width exists only at the `dialog.open` call site. I put the test in
`score.component.cy.ts`, which drives the real `[data-testid="end-match-btn"]` through
`ScoreComponent` — the same pattern `player-dialog.component.cy.ts` already uses by going through
`PlayersComponent`.

### RED proof
```
$ npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts

    1) fuellt bei 390x736 die volle Breite
    2) bleibt bei 1280x900 bei rund 500px

  2) ScoreComponent — Beenden-Dialog responsiv
     AssertionError: Timed out retrying after 4000ms: expected 360 to be within 480..520
```

(The 390 px case failed on `outerWidth >= 380`; the measured surface was Material's
80vw ≈ 312 px, exactly as the finding describes.)

### Covering tests
`score.component.cy.ts` › `ScoreComponent — Beenden-Dialog responsiv`:
- **`fuellt bei 390x736 die volle Breite`** — `outerWidth >= 380`.
- **`bleibt bei 1280x900 bei rund 500px`** — `outerWidth` within 480…520.

### GREEN
`score.component.cy.ts`: 66/66; `end-match-dialog.component.cy.ts` unchanged and still 1/1.

Note a deliberate behaviour change on desktop: the dialog goes from 360 px to the shared
500 px desktop width, which is what `responsiveDialogConfig` prescribes and what the other two
dialogs already use.

---

## M1 (Minor) — handset dialogs had no accessible name

### What changed
`frontend/src/app/core/layout/responsive-dialog.ts`

- `MatDialogModule` added to the import list and to `imports: [...]` of `DialogHeaderComponent`.
- The header title carries the directive:

```html
<!-- mat-dialog-title loest den umschliessenden Dialog per DI auf und setzt
     dort aria-labelledby: ohne das bleibt role="dialog" am Handy namenlos. -->
<h2 mat-dialog-title class="dialog-header-title">{{ title() }}</h2>
```

This fixes both handset dialogs that use the header (`PlayerDialogComponent`,
`ScoreEditDialogComponent`) in one place.

### RED proof
```
$ npx cypress run --component --spec src/app/features/players/player-dialog.component.cy.ts

  1) PlayerDialogComponent responsive
       gibt dem Dialog am Handy einen zugänglichen Namen:
     AssertionError: Timed out retrying after 4000ms: expected undefined to be a string

  4 passing, 1 failing
```

### Covering test
`player-dialog.component.cy.ts` › **`gibt dem Dialog am Handy einen zugänglichen Namen`** —
opens the real dialog at 390×736, reads `aria-labelledby` off `.mat-mdc-dialog-container`,
asserts it is a non-empty string and that the element it points at contains „Neuer Spieler".

### GREEN
`player-dialog.component.cy.ts`: 5/5. The existing handset header assertions
(`hält Abbrechen und Speichern sichtbar, wenn das Formular scrollt`, the 44×44 tap-target test)
still pass, so Material's `mat-mdc-dialog-title` styles did not disturb the flex header.

---

## Also (Minor) — `aria-label` hid the visible language label

### What changed
`frontend/src/app/app.html`, the bottom-nav "more" menu entry (introduced on this branch by
`36033f3`): the redundant `[attr.aria-label]="'app.language.label' | translate"` is removed, so
the accessible name falls back to the visible text „Sprache (DE)", which already carries the
current language.

I left the desktop toolbar trigger (`app.html:12`) alone: it predates this branch, and its
visible content there is a flag glyph (`aria-hidden`) plus the bare code „DE", where an
`aria-label` of „Sprache" is a reasonable name rather than a regression. Changing it was not in
scope for this wave. If it matters, it belongs in the follow-up ticket as "name should include
the language code".

No dedicated test: the existing `app.cy.ts` assertion on `[data-cy="lang-menu-trigger"]`
(`should('exist')`) still passes, and asserting the absence of an attribute would lock in the
implementation rather than the behaviour.

---

## Verification run (full suites, before the final commit)

```
$ cd frontend && npx cypress run --component
  ✔  All specs passed!    00:25   172  172   -   -   -

  app.cy.ts                                                  6    6
  features/players/player-dialog.component.cy.ts             5    5
  features/players/players.component.cy.ts                  36   36
  features/players/match-history/…cy.ts                      3    3
  features/statistics/head-to-head/…cy.ts                    4    4
  features/matches/analysis/…cy.ts                          11   11
  features/matches/match-setup/…cy.ts                       23   23
  features/matches/score/end-match-dialog.component.cy.ts    1    1
  features/matches/score/score.component.cy.ts              66   66
  features/matches/notes/player-notes.component.cy.ts        2    2
  features/matches/statistics/…cy.ts                        13   13
```

```
$ cd frontend && npx ng test --no-watch
 Test Files  4 passed (4)
      Tests  37 passed (37)
```

```
$ cd frontend && npx ng build --configuration production
Application bundle generation complete. [2.617 seconds]
▲ [WARNING] bundle initial exceeded maximum budget …   (pre-existing)
```

Baseline was Vitest 37 / Cypress 162 across 12 specs; now Vitest 37 / Cypress 172 across the
same 12 specs — 10 added tests, no spec files added or removed, nothing disabled or weakened.

---

## Not fixed / disagreements

**Nothing in this wave was left unfixed.** All items (C1, M5, I1, I2, I3, M1, plus the
`aria-label`) are implemented, each with a RED-first test.

Two places where I did not do literally what the brief said, both argued above:

1. **I1's proposed test is not a valid test.** The click-at-800×360 assertion the brief
   prescribed passes against the broken code, because Cypress scrolls with `scrollIntoView()`,
   which works inside `overflow: hidden`. The finding itself is entirely correct — the tiles
   really were clipped and unreachable — but it needed a different probe to prove it. I kept the
   click as a secondary check and added `assertNotClipped`, which went RED as required.

2. **I2's gate uses an explicit load flag, not "list is non-empty."** The literal sketch would
   have broken the empty-library case (a new coach with no players could never create one from
   the name field). `playersLoaded` closes the same hole without that side effect.

Items listed as explicitly out of scope (sub-11px fonts, duplicated toolbar/bottom-nav markup,
`showAppNav`'s initial value, the score buttons' `aria-label`, the four read-only pages, TEN-72,
TEN-77) were not touched.
