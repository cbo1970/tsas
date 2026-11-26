# Task 6 report: Aufschlagversuch gehört zum Punkt

## Implemented
- `serviceContextP1/P2` replaced by one `serveAttempt = signal<1|2|null>(null)`; `toggleService(attempt)` (panel arg dropped); `recordObservation` reads it regardless of panel; `resetPointContext()` clears it. `activeServeAttempt()` repointed at it. Template: `toggleService($event)`. Panel component untouched. Stroke signals untouched, no TODO.

## TDD evidence
- RED (Cypress): `npx cypress run --component --spec .../score.component.cy.ts` -> 52 passing, 1 failing: "schickt den ... Aufschlagversuch mit, auch wenn vom Returnierenden-Panel erfasst wird: expected null to equal 2" (expected: serve attempt was per panel).
- RED (Vitest) was not run separately before implementing (spec calls the new signature; it would only fail by type/undefined error). Honest gap.
- GREEN: Cypress spec 53 passing; full `npx cypress run --component` 119/119 (was 117, +2); `npx ng test --no-watch` 37 passed (was 36, +1).

## Long-name test (Review Focus 4)
Added "schneidet sehr lange Spielernamen ab..." (40-char surnames, 360x692): header <= 100px, row children within row rect, p2 right <= 360, body.scrollWidth <= clientWidth, plus per-name: name box inside its half, text longer than its box, computed overflow hidden + text-overflow ellipsis.
Mutation: the brief's assertions alone (height, overflow) PASSED even with ellipsis removed, because the grid uses minmax(0,1fr) and the name box is clamped while the text paints beyond it. So I added the computed-style/scrollWidth checks. Removing `overflow: hidden; text-overflow: ellipsis` from `.player-name` then made the test fail (52 pass / 1 fail); restored afterwards.
Note: the ellipsis assertions check CSS properties rather than pixels; that is the only observable of a clipped-but-painted overflow.

## Changed existing tests (spec.ts, Vitest) and where coverage lives
- "default serviceContext null for both players" -> "default serveAttempt to null" (same coverage, single signal).
- "set serviceContext when toggled on" / "clear when toggled off" -> same, on `serveAttempt`, `toggleService(attempt)`.
- recordObservation context test: `toggleService(1,1)` -> `toggleService(1)`; same body assertion.
- "reset context signals after recordObservation" and "reset BOTH panels" (the Task-5 guard): now check `serveAttempt()` null plus strokeTypeP1 null. Caveat: with one serve signal the "both panels" serve half is structural; the "both panels" test still meaningfully guards the stroke pair (recorded from panel 2, stroke set on panel 1). Not deleted.
- "expose active player panel inputs": `toggleService(2)`; still asserts activeServeAttempt() = 2.
- Added: "send the serve attempt regardless of which panel records the point" (Vitest).
No tests deleted. Cypress: existing tests unchanged; the two Task-5 Cypress guards (aria-pressed, next request body) still pass and still meaningful (stroke part, and serve via the single signal reset). Added 2 Cypress tests (cross-panel serveAttempt=2; long names).

## Files
frontend/src/app/features/matches/score/score.component.{ts,html,spec.ts,cy.ts}

## Concerns
- `activeServeAttempt` is now a trivial alias of `serveAttempt`; kept as instructed (panel binding indirection).
- Brief's "reset after capture" Cypress test already existed ("context is reset after recording a point"), so not duplicated.
