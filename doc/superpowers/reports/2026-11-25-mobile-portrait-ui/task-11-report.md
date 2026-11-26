# Task 11 report: Kartenliste unterhalb der Grenze (TEN-75)

Commit: dce0eb0 feat(players): Kartenliste am Handy, Tabelle am Desktop

## Implemented
- New `PlayerCardListComponent` (.ts/.html): input.required `players`; outputs edit, history, compare, remove, openMatch (per brief) plus `deactivate` (the brief lists only `remove`, but delete and inactivate are different API calls today, so a separate output was needed).
- Card: name, ranking + handedness below, tennis ball (`open-match`, only with activeMatchId) and overflow menu (`player-menu`) on the right. Menu items keep testids `player-history-btn`, `compare-btn`; new `delete-btn` / `deactivate-btn`. Tap on card = edit; ball and menu stop propagation. Inactive: `inactive-card` (opacity .45), deactivate item disabled.
- `players.component`: `@if (isHandset())` cards else the unchanged table, inside the same mat-card. `.handset` class on the page container: 16px margin, header stacks (column), 48px buttons, card content padding 0. Admin scope block, search and empty state are shared by both. `isHandset` = `LayoutService.isHandset` (no new breakpoint).
- i18n: `players.actions.{menu,history,openMatch}` in de/en/it/fr (reused existing `headToHead`, `deactivate`, `common.delete`).
- Added `data-testid="new-player-btn"` to the existing new-player button (for the tap-target test).

## Tests
- Cypress players spec: 34 green (18 existing + 16 new). Full Cypress: 160 green (144 + 16). Vitest: 37 green.
- Important finding: Cypress' default component viewport (500x500) counts as Breakpoints.Handset. Without a fix every existing table test would silently run against cards. `mountPlayers` now takes a viewport (default 1280x900) and always calls `cy.viewport` first.

## TDD evidence
- RED: `npx cypress run --component --spec src/app/features/players/players.component.cy.ts` -> 34 tests, 19 passing, 15 failing. All 15 new handset tests failed because `[data-testid="player-card"]` etc. did not exist (expected). The desktop-table test and the 18 existing passed.
- GREEN: same command -> 34 passing. Two failures during iteration were test bugs (default sort is by last name, so inactive "Muster" is index 1; `its('body')` yields the element, not jQuery); fixed in the tests, not the implementation.

## Desktop table unchanged
`git diff players.component.html` shows the table markup untouched: only an `@if/@else` wrapper around it, a `[class.handset]` binding (false on desktop), and a testid on the new-player button. The 18 pre-existing tests (table `td` assertions, search, row actions, admin scope) pass at 1280x900. Sorting is the unchanged `matSort`.

## Existing tests changed or removed
- None removed, none assertion-changed. Only the shared helper `mountPlayers` changed (explicit desktop viewport by default, optional viewport param) so they keep testing the table.

## Self-review
- New tests cover: card vs table at 390x736, 1280x900, landscape 800x360; content; tap-to-edit; ball only with running match + navigation; menu items + navigation; delete vs deactivate vs disabled; inactive dimming; search; admin scope; 16px margin + stacked header; 44px targets (card, ball, menu, h2h, new button); no horizontal overflow at 360.
- No sorting in card mode (no column headers); card order follows the current sort signal (default lastName asc).

## Concerns
- `(openMatch)="goToMatch($event.activeMatchId!)"` uses a non-null assertion (the ball only renders when set).
- Card list sits inside the existing mat-card (padding zeroed on handset); not checked visually on a device.
- Bottom-nav spacing (Task 2) is not handled in this page; relies on the app shell.
- Menu labels are only checked as i18n keys in tests (no loader).

---
## Fix report (review finding: Enter fires two actions)

Commit: de6c0e9 fix(players): Enter auf Ballsymbol und Menue oeffnet nicht den Bearbeiten-Dialog

Route chosen: restructure, not guard. It was small (template + a few CSS lines) and removes the whole class of bug: the card is now a plain div; the edit action is its own full-width button (data-testid player-card-main, name + meta, min-height 64px); ball and menu buttons are siblings, so no event can bubble into the edit action. All stopPropagation calls and role=button/tabindex on the div are gone. No existing test needed changes (clicking the card centre hits the main button).

RED (before restructure): `npx cypress run --component --spec src/app/features/players/players.component.cy.ts` -> 34 passing, 2 failing:
1. "does not open the edit dialog when pressing Enter on the menu or ball button": `Expected <input#mat-input-...> not to exist in the DOM, but it was continuously found` (edit dialog opened on Enter: the real bug).
2. "opens the edit dialog with the keyboard on the card": player-card-main not found (expected, new element).

GREEN: same command -> 36 passing. `npx ng test --no-watch` -> 37 passed. Full `npx cypress run --component` -> 162 passed.

Tests added (players.component.cy.ts): Enter-on-ball/menu regression test; keyboard-Enter-on-card test.
Deferred per coordinator: match-setup viewport pinning left alone.
