# Task 8 report: Dialoge ohne feste Breite

Commit: b11f0fc feat(dialogs): volle Breite am Handy, 500px am Desktop

## Implemented
- `core/layout/responsive-dialog.ts`: `responsiveDialogConfig(isHandset)` (handset: 100vw x 100dvh + panelClass `tsas-dialog-fullscreen`; desktop: 500px, maxWidth 100vw) and `DialogHeaderComponent` (Abbrechen left, title, Speichern right; `data-testid` dialog-cancel / dialog-save; min 44px; uses existing `common.cancel`/`common.save` keys).
- Call sites (by component): 2x PlayerDialogComponent in players.component.ts, 1x ScoreEditDialogComponent in score.component.ts (now injects LayoutService).
- Both dialogs: on handset render the header instead of title + bottom actions; on desktop the original markup stays (testids added to the buttons). Removed `min-width: 400px` from both forms (it forced overflow on 360px phones). Checkbox row in score edit wraps and has 44px rows.
- styles.scss: fullscreen panel class (no radius, content flexes, no max-height).
- i18n: no new labels needed (existing common.save/cancel are in all four files). Dialog bodies remain hard-coded German, as before.

## TDD
RED: `npx cypress run --component --spec src/app/features/players/player-dialog.component.cy.ts` -> 4 tests, 1 pass, 3 fail: width test (surface fixed 500px), scroll test and tap-target test ("never found [data-testid=dialog-save]"). Expected: fixed width and no header/testid existed. (The 1280 test passed already, as it asserts unchanged desktop behaviour.)
GREEN: same command -> 4 passing. Full: Vitest 37/37; Cypress 130/130 (new spec included; score spec 60).

The spec mounts the real PlayersComponent and clicks "new player", so the production `dialog.open` config is under test. The ScoreEditDialog call site shares the helper but has no own test (not requested).
Note: tap-target assertion uses offsetWidth/offsetHeight; getBoundingClientRect is scaled by Cypress' viewport preview (44px read as 40.6).

## Selector
`.mat-mdc-dialog-surface` exists in Material 21 and is used unchanged.

## EndMatchDialogComponent
Fixed `width: '360px'` is set in score.component.ts `endMatch()`, left unchanged as instructed. Content: hint text, two radio buttons in a column, cancel/confirm at the bottom. Fits 390px; on narrower phones Material caps to the viewport. Radio touch area is Material default (~40px), slightly under 44px; not changed (readability fine). Flag for follow-up if 44px is required there.

## Existing tests changed/removed
None.

## Self-review
- Desktop unchanged except removal of `min-width: 400px` (no visual change inside a 500px dialog).
- Header only has title/save/cancel (YAGNI). Validators/defaults untouched; Task 10 can add pre-fill.

## Concerns
- Dialog bodies not translated (pre-existing); only header buttons are.
- EndMatch radio tap area ~40px.
- Not verified on a real device.

## Fix report: EndMatchDialogComponent tap targets (commit 93b4d12)

Changed: end-match-dialog.component.ts styles: radio options min-height 44px (flex, centred; group gap 12px -> 4px so rhythm stays similar), dialog action buttons 44px via --mat-button-{text,filled,protected}-container-height. No behaviour change.
Deliberate earlier change, restated: min-width: 400px was removed from PlayerDialog and ScoreEditDialog forms (it forced overflow at 360px). On desktop the dialog is 500px wide so the content area is unaffected, but the forms no longer enforce a minimum.
Test: new end-match-dialog.component.cy.ts mounts the dialog at 360x640 and asserts all 4 controls (2 radios, 2 buttons) have offsetHeight >= 44.
RED: `npx cypress run --component --spec src/app/features/matches/score/end-match-dialog.component.cy.ts` -> 1 failing, "expected 40 to be at least 44" (radio option, first in DOM). After radios were fixed it still failed on the "Match beenden" raised button at 40 (needed the --mat-button-protected-container-height var).
GREEN: same command -> 1 passing. Full: `npx ng test --no-watch` 37 passed; `npx cypress run --component` 131/131 passed.
