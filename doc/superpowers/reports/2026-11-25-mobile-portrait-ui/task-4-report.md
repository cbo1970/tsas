# Task 4 Report: Verdichtete Kopfzeile

## Implemented
- `score.component.html` / `.ts`: the `.score-header` is now a single grid row (back, P1 name above the `quick-score-p1` button, Games with label, P2 name above `quick-score-p2`, edit, end-match) plus a 10px set-history line. Root carries `data-testid="score-header"`.
- The `@if (!needsServer())` gate is kept. `servingPlayer`, `formatPoints` and `recordQuickPoint` are unchanged.
- The point numbers are real `<button>` elements with the existing ids. They use `font-variant-numeric: tabular-nums` and `white-space: nowrap`, with `min-width/min-height: 44px`.
- Back, edit and end-match are `mat-icon-button` at 44x44. End-match was a 40px `mat-mini-fab`, which would have violated the 44px rule.
- Names use ellipsis on one line, and no heights are fixed, so longer labels degrade by truncation rather than growing the header.
- Grid columns: `44px 1fr auto 1fr 44px 44px`. When the match is COMPLETED the end button is absent and the last column stays empty.

## RED
Run: HEAD component files restored (`git checkout` of `score.component.ts` and `.html`) with the new tests in place.
Command: `cd frontend && npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts`
Result: 30 passing, 5 failing.

1. "ist höchstens 100px hoch": `AssertionError: Timed out retrying after 4000ms: Expected to find element: [data-testid="score-header"], but never found it.` This is the expected failure, because the old header has no `score-header` id.
2. "ist auch bei 360px Breite höchstens 100px hoch": same AssertionError. Expected for the same reason.
3. "erfasst beim Tipp auf die Punktzahl einen Punkt ohne Attribut": `CypressError: Timed out retrying after 4000ms: cy.its() errored because the property: request.body.pointType does not exist on your subject.`
   - This is NOT the missing header. The old header already has `quick-score-p1`, so the click worked and the request was sent.
   - It failed on the `its('request.body.pointType')` accessor, because the frontend omits `pointType` from the body and `its` rejects an absent property.
   - So this test only partly demonstrates RED. It showed the brief's assertion is unusable, not that the header was missing.
   - I then switched to `.its('request.body').should(body => { expect(body.winner).to.equal(1); expect(body.pointType).to.be.oneOf([null, undefined]); })`, matching the existing quick-click test.
4. "bricht bei zweistelligen Tie-Break-Punkten nicht um": `AssertionError ... Expected to find element: [data-testid="score-header"], but never found it.` Expected.
5. "alle Tipp-Ziele in der Kopfzeile sind mindestens 44x44px": `AssertionError ... Expected to find element: [data-testid="score-header"] button, but never found it.` Expected.

## GREEN
- `cd frontend && npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts` gave Tests 35, Passing 35, Failing 0.
- `cd frontend && npx cypress run --component` gave all 9 specs passing, 101 tests, 0 failing. The score spec shows 35/35.
- `cd frontend && npx ng test --no-watch` gave Test Files 4 passed (4), Tests 31 passed (31).
- Baseline was Cypress 96 and Vitest 31, and 96 + 5 new = 101. No existing test was modified.

## Files changed
- `frontend/src/app/features/matches/score/score.component.ts` (styles)
- `frontend/src/app/features/matches/score/score.component.html`
- `frontend/src/app/features/matches/score/score.component.cy.ts` (5 new tests)

Commit: d418af6.

## Self-review and test weaknesses (stated plainly)
- The pointType test in its final form is a regression guard, not a RED-then-GREEN test. It would also pass on the old header, because quick-click behaviour is unchanged. Its added `should('match', 'button')` also passes on the old header.
- The height tests only assert `<= 100`. I never measured the actual header height. My estimate is about 84px (4+4 padding, 44px row, 14px history line), but that is unverified. The tests do not prove the 100px budget has headroom for Task 5.
- The tie-break test asserts that the header is `<= 100` and the button `<= 50` tall at 360px with 10:12. I added games 6:6 to the brief's fixture to make the state realistic. It does not directly assert "no wrapping", only that height did not grow. It also uses short names (Federer/Nadal), so name-ellipsis under long names is untested.
- The 44x44 test iterates all buttons in the header, but only at 360px. At COMPLETED status the end-match button is absent, which is untested for header height.
- Task 3's unindented `@if` blocks were reindented only in the header region. The observation panel region is untouched.

## Concerns
None beyond the weaknesses above.

---

# Fix round: Tie-Break-Test (Review finding "Important")

## What changed (test only, no production code)
`score.component.cy.ts`:
- `mountScore` takes optional `player1`/`player2` (default PLAYER1/PLAYER2, so all other callers are unchanged).
- The tie-break test (renamed "bricht bei zweistelligen Tie-Break-Punkten und langen Namen nicht um oder über") now uses "Alexander Zverev" vs "Stefanos Tsitsipas" at 360px with 10:12 and games 6:6. It keeps the `<= 100` height assertion and adds:
  - the p1/p2 buttons contain "10"/"12";
  - `scrollWidth <= clientWidth` for both point buttons;
  - p1 button `right <= games.left` and p2 button `left >= games.right`;
  - p1 `left >= 0` and p2 `right <= 360`;
  - every child of `.header-row` lies within the row's bounding rect (+/-0.5px).

Finding during the work: my first attempt asserted `row.scrollWidth <= row.clientWidth`. It failed at 346 vs 344 even in the correct layout. A debug dump showed all children inside 8..352, so the 2px is Material's 48px touch-target pseudo-element on the 44px icon buttons, not real overflow. I replaced it with the per-child bounding-rect check, which measures what is visible. Production CSS was left alone.

## Proof the test can fail
Deliberate break: in `score.component.ts` I changed `.header-row` `grid-template-columns` from `44px minmax(0, 1fr) auto minmax(0, 1fr) 44px 44px` to `44px 30px auto 30px 44px 44px`, so the point columns are too narrow for two digits. Height does not change under this break, which is exactly why the old assertions could not catch it.
Command: `cd frontend && npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts`
RED (with the break): Passing 34, Failing 1. The failing test is the tie-break test with `p1 rechts <= Games links`, `-93.6875` (the p1 button overlaps the games block by about 94px).
The break was then reverted (the grid-template-columns line was checked as restored to the committed value).

## GREEN (after revert)
- `cd frontend && npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts` gave Passing 35, Failing 0.
- `cd frontend && npx ng test --no-watch` gave Tests 31 passed (31).
- `git status` showed only `score.component.cy.ts` modified, and the production files are identical to d418af6.

## Remaining honesty notes
- The break I used makes the point columns too narrow. I did not separately test a break that only hits `.pts-btn` `nowrap` or an overflowing digit inside a correctly sized column. The `scrollWidth <= clientWidth` button assertions target that case, but were not individually proven to fail.
- The deferred minors listed by the coordinator are untouched.
