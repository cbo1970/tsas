# Task 1 report: LayoutService

## Implemented
`frontend/src/app/core/layout/layout.service.ts`: `LayoutService` (providedIn root) with `readonly isHandset: Signal<boolean>` = `toSignal(BreakpointObserver.observe(Breakpoints.Handset).pipe(map(s => s.matches)), { initialValue: false })`.

## Tests
`layout.service.spec.ts`, 3 tests: matches true, matches false (and observe called with `Breakpoints.Handset`), and signal follows later emissions (orientation change).
Full run: 4 files, 29 tests, 0 failures (baseline 26 + 3).

## TDD evidence
RED: `cd frontend && npx ng test --no-watch` before implementation failed to compile the spec (layout.service module missing, so `TS18046: 'service' is of type 'unknown'` in layout.service.spec.ts). Expected because the service file did not exist.
GREEN: same command after implementation: `Test Files 4 passed (4)`, `Tests 29 passed (29)`.

## Files changed
- frontend/src/app/core/layout/layout.service.ts (new)
- frontend/src/app/core/layout/layout.service.spec.ts (new)

Commit: 81fb281 feat(layout): LayoutService mit isHandset-Signal

## Self-review
Matches brief. No extra API. Tests assert behaviour via a BehaviorSubject-driven stub (including change over time), not just the mock's value.

## Concerns
None. Note: RED output showed a type-resolution error rather than a literal "module not found" line (tail truncated), same cause.
