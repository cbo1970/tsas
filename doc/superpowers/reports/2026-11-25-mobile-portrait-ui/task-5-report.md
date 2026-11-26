# Task 5 report — Ein Panel, rollenabhängig, mit Spielerumschalter

Commit: `b4774c9 feat(score): ein rollenabhängiges Panel mit Spielerumschalter`

## What I implemented

### `ObservationPanelComponent` (new)
`frontend/src/app/features/matches/score/observation-panel.component.ts` / `.html`.
Standalone, signal-based, no `TranslatePipe` (screen stays hard-coded German).

Interface exactly as the brief specifies — this is the contract for Tasks 6 and 7:

```ts
player       = input.required<{ id: string; name: string }>();
opponentName = input.required<string>();
isServing    = input.required<boolean>();
serveAttempt = input.required<1 | 2 | null>();   // gehört zum Punkt, nicht zum Panel
strokeType   = input.required<StrokeType | null>();

observation        = output<PointType>();
serveAttemptChange = output<1 | 2>();            // getippter Versuch, Toggle macht der Parent
strokeTypeChange   = output<StrokeType | null>(); // bereits getoggelter Wert
```

Role-dependent content:
- serving: `serve-attempt-1|2` · `obs-ACE` · `obs-WINNER` · `obs-DOUBLE_FAULT` · `obs-FORCED_ERROR` · `obs-UNFORCED_ERROR` · `obs-NET` · `stroke-FOREHAND|BACKHAND`
- returning: `obs-WINNER` · `obs-FORCED_ERROR` · `obs-UNFORCED_ERROR` · `obs-NET` · `stroke-FOREHAND|BACKHAND`

Every `obs-*` tile has `min-height: 56px` and no fixed height; its row is `flex: 1 1 0; min-height: 56px`,
so the tiles *grow* into the available height. Labels are `white-space: normal; overflow-wrap: anywhere`
so TEN-77's longer translations can wrap. Context buttons are `min-height: 44px`.

`.obs-block` deliberately keeps `min-height: auto` (no `min-height: 0`): when the viewport is too short,
the block is clipped at the bottom instead of overlapping the context rows. The three
Forced/Unforced/Netz clicks reproduced exactly that overlap before the fix — Cypress refused the click
with "is being covered by `stroke-FOREHAND`".

### `score.component`
- `activePlayer = signal<1 | 2>(1)` plus four computed panel inputs:
  `activePlayerInfo()`, `activeOpponentName()`, `activeServeAttempt()`, `activeStrokeType()`.
  `activeServeAttempt()` reads the *active* player's `serviceContextP1/P2` signal, so Task 6 can
  collapse those two into one per-point signal without touching the panel's interface.
- `effect()` in the constructor reads `matchData()` (not `servingPlayer()`) and sets `activePlayer`
  to the then-valid server. Reading `matchData()` rather than the `servingPlayer` computed is
  deliberate: a computed only notifies on *change*, so an unchanged server would not switch the view
  back after a point recorded from the returner's panel.
- `toggleStroke(panel, type)` → `setStroke(panel, type | null)`: the toggle now lives in the panel
  (which owns the button state), the parent only stores.
- `recordObservation(panel, pointType)` unchanged, **including the ACE / DOUBLE_FAULT guard against
  the non-serving panel.** The buttons no longer exist on the returner panel, so the guard is now
  pure defence in depth — I kept it on purpose, as instructed.
- Template: `data-testid="panels-area"` on the flex:1 area that holds either the serve picker or the
  panel; below it a `.player-switch` with two 56px `switch-player-1|2` buttons carrying `aria-pressed`
  and a 🎾 for whoever is serving; coach notes `<details>` below that (Task 7 replaces it).
- Both the panel and the switcher are inside the existing `status !== 'COMPLETED'` branch, and the
  switcher is additionally gated on `!needsServer()` — Task 3's serve picker still owns the screen alone.
- Panel/observation CSS moved out of `score.component.ts` into the new component; what is left is the
  area layout, the switcher and the notes strip.

No backend change, no new point attributes, no migration. No breakpoint branch — variant C at every width.

## Tests and results

### TDD evidence

**RED** — `cd frontend && npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts`

```
│ Tests:        46   │ Passing: 35   │ Failing: 11 │

1) ScoreComponent — ein Panel / zeigt genau ein Panel:
   Expected to find element: `[data-testid="panel"]`, but never found it.
2) … zeigt beim Aufschläger Ass, Doppelfehler und die Service-Wahl:
   Expected to find element: `[data-testid="obs-ACE"]`, but never found it.
3) … zeigt beim Returnierenden weder Ass noch Doppelfehler noch Service-Wahl:
   Expected to find element: `[data-testid="switch-player-2"]`, but never found it.
4) … gibt jeder Beobachtungs-Kachel mindestens 56px Höhe:
   Expected to find element: `[data-testid^="obs-"]`, but never found it.
5) … gibt auch dem Returnierenden-Panel Kacheln von mindestens 56px:
   Expected to find element: `[data-testid="switch-player-2"]`, but never found it.
6) … scrollt auf 360x692 nicht:
   Expected to find element: `[data-testid="panels-area"]`, but never found it.
7) … gibt den Umschaltflächen mindestens 56px Höhe:
   Expected to find element: `[data-testid="switch-player-1"]`, but never found it.
8) … markiert den aktiven Spieler per aria-pressed:
   Expected to find element: `[data-testid="switch-player-1"]`, but never found it.
9) … zeigt auch bei 1280x900 genau ein Panel:
   Expected to find element: `[data-testid="panel"]`, but never found it.
10) ScoreComponent — Panel folgt dem Aufschlag / folgt dem Aufschlagwechsel nach einem Game:
   Expected to find element: `[data-testid="panel"]`, but never found it.
11) … kehrt nach einem erfassten Punkt zum Aufschläger zurück:
   Expected to find element: `[data-testid="switch-player-2"]`, but never found it.
```

Expected exactly: the screen still rendered two panels with the old `panel-p1`/`panel-p2`,
`p1-ace`, `p1-service-1` test ids, there was no single `panel`, no `panels-area`, no switcher.

**GREEN** — same command after implementing:

```
│ Tests:        47   │ Passing: 47   │ Failing: 0 │
```

Full suites before committing:

```
cd frontend && npx cypress run --component
  ✔  All specs passed!   00:09   113   113   -   -   -      (9 specs; baseline was 101)

cd frontend && npx ng test --no-watch
  Test Files  4 passed (4)
  Tests      35 passed (35)                                  (baseline was 31)
```

### New tests

Cypress `score.component.cy.ts`:
- `ScoreComponent — ein Panel`: genau ein Panel · Aufschläger zeigt Ass/DF/Service-Wahl · Returnierender
  zeigt keines davon (und sehr wohl Winner/Netz) · ≥56px Kacheln beim Aufschläger · ≥56px Kacheln beim
  Returnierenden · kein Scroll auf 360×692 · Umschaltflächen ≥56px · `aria-pressed` folgt dem Umschalten ·
  genau ein Panel auch bei 1280×900.
- `ScoreComponent — Panel folgt dem Aufschlag`: Aufschlagwechsel nach einem Game (Review Focus 2) ·
  Rückkehr zum Aufschläger nach einem von der Gegenseite erfassten Punkt.
- `ScoreComponent — observation buttons`: zusätzlich „Winner on the returning panel → winner=2".
- `ScoreComponent — Schlagart` (neu): Schlagart wird mitgeschickt · zweiter Tipp hebt sie wieder auf.

Vitest `score.component.spec.ts`:
- `should render exactly one observation panel`
- `should start on the serving player`
- `should follow the serve when it changes with a score response`
- `should return to the serving player after a recorded point`
- `should expose the active player panel inputs`

I also eyeballed the result: a throwaway Cypress spec rendered the screen at 360×692 for both roles
and I inspected the screenshots (tiles fill the screen, switcher sits at the bottom, nothing clipped).
The spec and its screenshots were deleted before committing.

## Files changed

- new `frontend/src/app/features/matches/score/observation-panel.component.ts`
- new `frontend/src/app/features/matches/score/observation-panel.component.html`
- `frontend/src/app/features/matches/score/score.component.ts`
- `frontend/src/app/features/matches/score/score.component.html`
- `frontend/src/app/features/matches/score/score.component.cy.ts`
- `frontend/src/app/features/matches/score/score.component.spec.ts`

## Existing tests changed or removed — and where the coverage lives now

| Test | What it covered | Where now |
|---|---|---|
| cy `layout / shows two observation panels` | both panels render | **renamed** to `shows a single observation panel`; the count is asserted by `ein Panel / zeigt genau ein Panel` |
| cy `Winner/Forced/Unforced/Netz/DF/Ass on P1 panel …` (6) | point attribution per `pointType` from the `p1-*` test ids | **rewritten** onto `obs-*` on the serving panel; same assertions on `winner`/`pointType`. Added `Winner on the returning panel → winner=2` so the switched-panel attribution is covered too |
| cy `Ass/DF validation / Ass on non-serving P2 panel → snackbar, no API call` (2) | the ACE/DF guard, exercised through the P2 panel's buttons | **deleted** — those buttons no longer exist on the returner panel, so the test could not be ported. Their absence is now asserted by `zeigt beim Returnierenden weder Ass noch Doppelfehler noch Service-Wahl`; the guard itself (which I kept) stays covered by the two Vitest tests `should reject ACE if the panel is not serving` / `should reject DOUBLE_FAULT …`, which call `recordObservation` directly. The snackbar text is no longer asserted anywhere — it is unreachable from the UI by design |
| cy `service context / selecting 1. Service on P1 panel …` | `serveAttempt` reaches the request | **rewritten** onto `serve-attempt-1` |
| cy `service context / context is reset after recording a point` | service context cleared after a point; asserted via the `.active` CSS class | **rewritten** onto `serve-attempt-1` and asserted via `aria-pressed` (behaviour, not styling); this is the attribute Task 6's brief uses |
| cy `Aufschlagwahl / zeigt die Aufschlagwahl …` (Task 3) | `panel-p1`/`panel-p2` absent while the picker owns the screen | **migrated** to `panel` absent **and** `switch-player-1` absent (as decided) |
| cy `Aufschlagwahl / setzt den Aufschläger und zeigt danach die Panels` (Task 3) | panel appears after picking | **migrated** to `[data-testid="panel"]` |
| vitest `should set / clear / toggle strokeType …` (3) | `toggleStroke` toggle semantics on the component | **rewritten** for `setStroke` (set / clear / per-panel independence). The *toggle* half moved into the panel and is now covered end-to-end by the new cy test `hebt die Schlagart beim zweiten Tipp wieder auf`, which also asserts `strokeType: null` reaches the request |
| vitest `should call api.recordPoint with pointType and context` / `should reset context signals` | unchanged assertions, only `toggleStroke` → `setStroke` in the arrange step | same tests |
| vitest `should render both player panels regardless of serving state` | two panels rendered | **rewritten** as `should render exactly one observation panel` |

Nothing else was touched. Task 4's header (`score-header`, `quick-score-p1|p2`) and its tests are untouched.

## Self-review findings

- `player.id` is never read by the panel. It is in the brief's interface and Tasks 6/7 consume that
  interface, so I kept it rather than trimming it; `activePlayerInfo()` fills it from `matchData()`.
  If the reviewer prefers, dropping it to `player: { name: string }` is a one-line change — but it
  would be a contract change.
- Asymmetric toggle ownership: `serveAttemptChange` emits the tapped attempt and the parent toggles,
  while `strokeTypeChange` emits the already-toggled value. That is the brief's signature (`output<1 | 2>`
  vs `output<StrokeType | null>`) and I followed it verbatim rather than "fixing" an interface two
  later tasks depend on.
- The `switch-player-*` buttons use `aria-pressed` on a plain `<button>`, which is the correct toggle
  semantics; they also carry the 🎾 for whoever serves, so the coach can see at a glance that the
  default panel is the server's.
- Three observation-button describes now set `cy.viewport(360, 692)` in a `beforeEach`. At Cypress's
  default 500×500 component viewport the panel does not fit and the bottom tiles are clipped, which
  made those clicks fail. These are behaviour tests, not layout tests, so running them at the actual
  target size is the honest fix; the layout tests assert the fit explicitly.
- The tests assert rendered geometry and real request bodies (`cy.intercept` + `request.body`), not
  mock call counts, except for the two Vitest guard tests that necessarily assert "no API call".

## Concerns

- **Panel reset policy.** The brief says the effect sets `activePlayer` "nach jeder Score-Antwort",
  so a manual switch survives only until the next point is recorded, then the view returns to the
  server. That matches the documented trade-off ("one extra tap when the point belongs to the other
  player") but it is a product decision worth confirming: the alternative — switching only when the
  server actually changes — is a one-word change (`matchData()` → `servingPlayer()`) and is covered
  by the test `kehrt nach einem erfassten Punkt zum Aufschläger zurück`, which would then have to go.
- **Returner tile proportions.** With only two observation rows, the returner's Winner tile grows to
  roughly half the panel (~350px at 360×692) while Forced/Unforced/Netz share the other half. Large
  is the goal, but if that looks odd to the designer a `max-height` on `.obs-row` plus
  `justify-content: flex-start` would cap it at the cost of blank space.
- **Coach notes.** Still the `<details>` strip, now below the switcher with `max-height: 40vh` and its
  own scroll (`.panels-area` is `overflow: hidden`, so it could no longer scroll there). Task 7
  replaces it with the bottom bar + full-screen notes, so I did not invest further.
- The score header's icon buttons render as ligature text in the Cypress harness (the Material Icons
  font is not loaded there). Pre-existing, unrelated to this task, visible only in screenshots.

---

# Fix report — coordinator rulings

Commit: `2db8ffe fix(score): Beobachtungskacheln behalten ihre natürliche Höhe`

Three rulings came back. Only #2 required a code change.

## Ruling 1 — reset policy: keep it. No change.

The design document says „Nach jedem erfassten Punkt springt die Ansicht auf den Aufschläger des
nächsten Punkts". My literal reading was correct, so the effect still reads `matchData()` and the
test `kehrt nach einem erfassten Punkt zum Aufschläger zurück` stays. Nothing touched.

## Ruling 2 — observation tiles must not stretch. Fixed.

`min-height: 56px` is a floor, not an instruction to consume leftover vertical space. Changed in
`observation-panel.component.ts`:

- `.obs-row`: `flex: 1 1 0` → `flex: 0 0 auto` — the tile rows no longer grow.
- `.obs-block`: `flex: 1 1 auto` → `flex: 0 1 auto` — the block no longer grows either.
- `:host`: `flex: 1` → `flex: 0 1 auto` — the panel is as tall as its content, so the returner panel
  is simply shorter than the server panel; the leftover space stays empty and the switcher stays
  where it was at the bottom.

Both `.obs-block` and `:host` keep the implicit `min-height: auto` that stops a flex item shrinking
below its content, so the "clipped, never overlapping" behaviour from the first round is preserved.

### Covering tests (new)

In `describe('ScoreComponent — ein Panel')`, at 360 × 692:

- `streckt die Kacheln des Aufschläger-Panels nicht über ihre natürliche Höhe`
- `streckt die Kacheln des Returnierenden-Panels nicht über ihre natürliche Höhe`

Both assert `expect(h).to.be.within(56, 96)` for every `[data-testid^="obs-"]`, i.e. the 56px floor
*and* the 96px ceiling in one assertion, with the tile's test id in the failure message. The two
existing `mindestens 56px` tests are untouched.

### The ceiling test fails against the pre-fix layout

Added the tests first and ran them against the unchanged CSS:

```
$ cd frontend && npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts
│ Tests:  49  │ Passing: 48  │ Failing: 1 │

1) ScoreComponent — ein Panel
     streckt die Kacheln des Returnierenden-Panels nicht über ihre natürliche Höhe:
   AssertionError: obs-WINNER Höhe: expected 176.25 to be within 56..96
```

Exactly the reported defect: the returner's Winner tile had stretched to 176px. (The serving-panel
variant passed before the fix — with four rows sharing the height, no single tile exceeded 96px — so
the ceiling catches precisely the ballooning case the ruling is about.)

### After the fix

```
$ cd frontend && npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts
│ Tests:  49  │ Passing: 49  │ Failing: 0 │

$ cd frontend && npx ng test --no-watch
 Test Files  4 passed (4)
      Tests  35 passed (35)

$ cd frontend && npx cypress run --component
    ✔  All specs passed!   00:09   115   115   -   -   -
```

I re-rendered both roles at 360 × 692 with a throwaway spec and checked the screenshots: tiles are
uniform, the returner panel ends after its stroke row with empty space below it, and the switcher is
unchanged at the bottom. The throwaway spec and its screenshots were deleted before committing.

## Ruling 3 — the two deleted Ass/DF snackbar tests: accepted, table left in place. No change.

## Ruling 4 — `cy.viewport(360, 692)` in the observation describes: kept. No change.

## Totals after the fix

Cypress 115/115 across 9 specs (baseline 101, +14). Vitest 35/35 (baseline 31, +4).

---

# Fix report 2 — code review findings

Commit: `6df737e fix(score): Punkt-Kontext nach jedem Punkt auf beiden Panels räumen`

Two Important findings, both fixed.

## Finding 1 — the Vitest test that asserted what it arranged

`should follow the serve when it changes with a score response` arranged `activePlayer = 1`, set the
response's `servingPlayer` to 1 and expected 1. The effect was a no-op, so the test would have passed
against a component with no `effect()` at all — and it was the only Vitest test named for the
serve-change path.

Fixed by arranging the *other* player, so the expected value differs from the arranged one:

```ts
component.activePlayer.set(2);                       // war: 1
component.matchData.set({ ...MOCK_MATCH, score: { ...MOCK_SCORE, servingPlayer: 1 } as any });
fixture.detectChanges();
expect(component.activePlayer()).toBe(1);
```

**Mutation proof.** With `this.activePlayer.set(serving)` commented out inside the effect:

```
× should follow the serve when it changes with a score response
  AssertionError: expected 2 to be 1 // Object.is equality
```

`activePlayer` stayed at the arranged 2 instead of following the response's 1 — the test now fails
when the effect is removed, which is exactly what it previously did not do. (Three further tests
failed under that mutation as collateral: `should start on the serving player`, `should return to the
serving player after a recorded point` and the Cypress serve-change pair.) The mutation was reverted
and the suite re-run green.

## Finding 2 — stale per-panel context shipped with the next point

`recordObservation` called `resetContextForPanel(panel)`, clearing only the panel the point was
recorded on. With one panel plus the auto-switch this records wrong data: tap `2. Service` on the
server's panel → switch to the returner → record there → `serviceContextP1` keeps `2` → the effect
returns the view to the server's panel with `2. Service` still pressed → the next point silently ships
`serveAttempt: 2`. Same for `strokeType`.

Fixed in `score.component.ts`: `resetContextForPanel(panel)` → `resetPointContext()`, which clears
`serviceContextP1/P2` and `strokeTypeP1/P2`. The context belongs to the point, not to the panel.

### RED proof for the new cross-panel tests

Added first, run against the unfixed code:

```
$ cd frontend && npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts
│ Tests: 51  │ Passing: 49  │ Failing: 2 │

1) ScoreComponent — service context
     räumt den Kontext auch auf, wenn der Punkt vom anderen Panel erfasst wurde:
   Timed out retrying after 4000ms
   + expected - actual

2) ScoreComponent — service context
     schickt nach einem Punkt vom anderen Panel keinen alten Kontext mit:
   AssertionError: Timed out retrying after 4000ms: serveAttempt: expected 2 to be one of [ null, undefined ]
```

The second failure is the defect itself: the stale `2` really did travel with the following point.

### Covering tests

Cypress, in `describe('ScoreComponent — service context')`:
- `räumt den Kontext auch auf, wenn der Punkt vom anderen Panel erfasst wurde` — after recording from
  the returner's panel, `serve-attempt-2` and `stroke-BACKHAND` are back to `aria-pressed="false"`
  (this is the regression test the reviewer asked for; Task 6 inherits it).
- `schickt nach einem Punkt vom anderen Panel keinen alten Kontext mit` — the *next* point's request
  body carries neither `serveAttempt` nor `strokeType`. This is the one that pins the data, not just
  the highlight.

Vitest:
- `should reset the context of BOTH panels after a recorded point` — context set on panel 1, point
  recorded on panel 2, panel 1's signals expected null. **Mutation proof:** with `resetPointContext`
  reduced to its old panel-2-only body, this test fails with
  `AssertionError: expected 'FOREHAND' to be null` (as does the pre-existing same-panel reset test,
  which the mutation also breaks).

## Commands and output

```
$ cd frontend && npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts
│ Tests:  51  │ Passing: 51  │ Failing: 0 │

$ cd frontend && npx ng test --no-watch
 Test Files  4 passed (4)
      Tests  36 passed (36)

$ cd frontend && npx cypress run --component
    ✔  All specs passed!   00:10   117   117   -   -   -
```

## Totals

Cypress 117/117 across 9 specs (baseline 101, +16). Vitest 36/36 (baseline 31, +5).

The items the coordinator recorded as deferred (`player.id`, duplicated toggle logic, the `within(56,96)`
overlap and the magic 96, redundant `cy.viewport` calls, `.panels-area` overflow with the notes open,
the remaining `TranslatePipe` via `playerNotes.title`, and the untested `serve-attempt-2` /
`.ctx-btn` tap-target floor / returner error attribution / snackbar strings) were left untouched.
