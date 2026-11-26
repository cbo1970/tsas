# Task 3 report: Aufschlagwahl

## Implemented
- New `serve-picker.component.ts/.html`: `players = input.required<{player1; player2}>()`, `picked = output<1|2>()`, heading "Wer schlägt auf?", explanatory sentence, two buttons `serve-pick-1/2` with `min-height: 88px`, `overflow-wrap: anywhere`. Hard-coded German, no TranslatePipe. Host spans the full grid width.
- `score.component`: panels (p1/p2) rendered only when `servingPlayer() !== null`, otherwise `<app-serve-picker>`. `setServe(boolean)` became public `pickServer(1|2)`. Both implicit `servingPlayer() === null` paths in `recordQuickPoint` / `recordObservation` removed. ACE/DF guards untouched. Coach notes stay visible.
- No fixed heights/100vh added.

## Tests
- Cypress added 4 tests (`panel-p1/p2` instead of `panel`, per instruction; plus an 88px height test). Full Cypress: 95/95 (90 + 5 new... 4 new in this describe plus 1 counted; spec file 29 tests). Vitest: 31 passed.
- Existing tests changed (legitimately): in `score.component.spec.ts` two tests asserted "recordQuickPoint with no server calls setServingPlayerN and does not record". That behaviour is intentionally removed. Replaced by `pickServer(1)`, `pickServer(2)` tests (API call + signal patched) and one asserting recordQuickPoint no longer sets the server.
- Note: the brief's intercept `PUT .../serving-player-1` is wrong; the real endpoint is `POST .../serve/player1`, used in the test.

## TDD evidence
- RED: `npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts` -> 29 tests, 26 passing, 3 failing (picker visible, picker then panel, 88px height); expected since `serve-picker` did not exist. The 4th new test ("not shown when serving set") passes trivially pre-implementation, by design.
- GREEN: same spec 29/29; full `npx cypress run --component` 95/95 all specs; `npx ng test --no-watch` 4 files, 31 tests passed.

## Files
frontend/src/app/features/matches/score/{serve-picker.component.ts, serve-picker.component.html, score.component.ts, score.component.html, score.component.cy.ts, score.component.spec.ts}

## Self-review
Complete against brief. Diff minimal. Tests assert DOM behaviour and the real HTTP request (via intercept). Not verified visually/in a browser at phone width.

## Concerns
- With no server set, header quick-score buttons (`quick-score-p1/p2`) still record points (picker only replaces panels). Spec said remove implicit path with no replacement; flag for later tasks.
- Task 5 must migrate the `panel-p1/p2` absence assertions.

## Fix report (coordinator ruling: picker owns the screen)
- Added `needsServer` computed (match loaded, not COMPLETED, servingPlayer null). Score header is rendered only when `!needsServer()`; picker rendered when `needsServer()`.
- `ServePickerComponent` got its own bar: back arrow (`serve-picker-back`, output `back` -> `goToMatches()`) plus "Name1 – Name2" title.
- Test added: header (`quick-score-p1/p2`) absent with servingPlayer null, back button visible, present after picking server 2.
- Commands: `npx cypress run --component --spec .../score.component.cy.ts` -> 30/30 passing; `npx ng test --no-watch` -> 31/31 passing.
- Concern 3 closed. Remaining: coach notes still render below the picker (inside panels-area).
