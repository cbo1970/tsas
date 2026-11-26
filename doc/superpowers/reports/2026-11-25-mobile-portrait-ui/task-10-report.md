# Task 10 report: Spieler aus der Namenssuche anlegen

Commit: 3b94c88 feat(match-setup): Spieler direkt aus der Namenssuche anlegen

## Implemented
- `splitName` exported from match-setup.component.ts (split at first space; no space gives lastName '').
- Both autocompletes: `@for ... @empty` renders a last `mat-option` (`data-testid="create-player-option"`) only when the filtered list is empty and the trimmed query is non-empty. The option's value is the typed string, so selecting it does not touch the control; `onSelected` treats a string selection as "create".
- Dialog opened via `responsiveDialogConfig(layout.isHandset())`; on close the request is POSTed with the existing `api.createPlayer`, the player is set into the originating control (and form id) and added to `players`. Error shows a snackbar.
- `data-testid="player1-input"` / `player2-input` added.
- i18n `matchSetup.createPlayer` with `{{name}}` in de/en/it/fr (one line each).
- `player1Query`/`player2Query` made `protected readonly` (template reads them).
- Validators/defaults of PlayerDialogComponent untouched (TEN-72). Phone CSS untouched.

## Deviation from brief
Dialog data is `{ prefill: { firstName, lastName } }` instead of `{ prefillName: string }`. Reason: the dialog would otherwise have to import `splitName` from match-setup while match-setup imports the dialog (circular import). The dialog still reads it as "new player, pre-filled"; edit mode (data = Player) is unchanged, distinguished by `'id' in data`.

## TDD evidence
RED: `npx cypress run --component --spec .../match-setup.component.cy.ts` gave 12 passing / 7 failing; all 7 new tests failed with `Expected to find element: [data-testid="player2-input"] (or player1-input), but never found it`. Expected: hooks and feature not yet present. (Note: the two "option must not exist" tests also failed at this stage only because the hook was missing, not on the option itself.)
GREEN: same command, 19 passing. Full: `npx ng test --no-watch` 37 passed; `npx cypress run --component` all specs passed, 143/143 (136 + 7 new).

## Tests added (7, in a new describe)
offer on no match; not offered when a match exists (types 'Muster', first asserts the real option is shown, so an always-visible create option would fail it); not offered with empty input; dialog pre-filled (first word / rest); result lands in player 2 and player 1 stays empty; result lands in player 1 and player 2 stays empty; at 360x692 option height >= 44 and no horizontal overflow.
Test providers return bare keys, so the new describe's mount adds an app initializer setting the `matchSetup.createPlayer` German text, otherwise "contains Zzz" cannot be asserted. The 360px test remounts after `cy.viewport` and uses `.focus().type()` because Cypress reported the floating label covering the input otherwise.

## Existing tests changed or removed
None.

## Files changed
frontend/public/i18n/{de,en,it,fr}.json; frontend/src/app/features/matches/match-setup/match-setup.component.{ts,html,cy.ts}; frontend/src/app/features/players/player-dialog.component.ts.

## Self-review
- Backend untouched; desktop presentation unchanged apart from the new option.
- Option height >= 44 is verified at 360px by test (Material option min-height 48).
- Chosen value-as-string design avoids a sentinel object and avoids the control being overwritten on selection.

## Concerns
- Mutation check of the "not offered" test was reasoned, not executed.
- Created opponent is stored male / right-handed / two-handed by dialog defaults (TEN-72, out of scope).
- The player is created via POST immediately on dialog save; if the user then never starts the match, the player remains (same as creating from the list).

## Fix: cancel path
- Reproduced: new Cypress test (tap create, cancel dialog, expect input value "Zzz Test" and create option offered on refocus) went RED with `expected "" to equal "Zzz Test"` - displayPlayer(string) returned "" so the input blanked while control and query still held the text.
- Chosen: displayPlayer returns a string unchanged, so the typed text stays visible (predictable after a mis-tap; no retyping, visible input and control agree). Real players still display "Nachname Vorname".
- GREEN: match-setup spec 20 passing; ng test 37 passed. Test: "behält den getippten Text, wenn der Dialog abgebrochen wird".
