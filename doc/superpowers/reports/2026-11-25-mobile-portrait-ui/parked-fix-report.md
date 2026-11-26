# Parked findings — fix report

Branch `feature/mobile-portrait-ui`, on top of `e240ea7`.

| Commit | Subject |
|---|---|
| `87fbfd9` | fix(score): Erfassungsmaske auf jeder Viewporthoehe erreichbar |
| `b7d1e11` | fix(ui): Dialog-Kopfzeile am Handy nicht mehr aufgeblaeht |

Final suite: Cypress **175/175** across 11 specs, Vitest **37/37**, `ng build --configuration production` OK
(pre-existing bundle-budget warning only). Not pushed.

---

## Finding 1 — the landscape escape hatch had the wrong trigger

### What changed — `frontend/src/app/features/matches/score/score.component.ts`

* `:host` and `.scoring-page`: `height: 100dvh` → `min-height: 100dvh`; `.scoring-page` `overflow: hidden` →
  `overflow: visible`.
* `.panels-area`: `overflow: hidden` → `overflow: auto` (keeps `flex: 1; min-height: 0`).
* The whole `@media (max-height: 560px)` block is gone.
* `.notes-view`: `height: 100%` → `flex: 1` (the consequence the re-reviewer predicted — see RED proof below).
* Comments rewritten to record *why* there is no media query any more (dvh vs. CSS viewport height).

### Finding confirmed, but the reviewer's numbers are wrong — recorded here because they changed the test

I measured the real layout in the component-test fixture before touching anything (throwaway spec, deleted again),
viewport width 375 unless noted:

| viewport | `panels-area` bottom | content bottom (`stroke-BACKHAND`) | clipped? |
|---|---|---|---|
| 360×692 | 580 | 465 | no |
| 375×594 | 482 | 465 | no |
| 375×590 | 478 | 465 | no |
| 375×580 | 468 | 465 | no (3px to spare) |
| 375×568 | 456 | 465 | **yes, by 9px** |
| 375×561 | 449 | 465 | **yes, by 16px** |
| 375×553 | 482 | 465 | no — old media query fires |

So:

* **The serving panel is ~380px tall, not ~580px.** The ~580 figure is the height of the *whole page's* content
  (`documentElement.scrollHeight` = 594 at 800×360). The reviewer conflated the two.
* `panels-area`'s bottom edge is `viewportHeight − 112` and the panel content always ends at y=465, so
  **clipping begins below 577px**, not somewhere in 590–690.
* The gap the old trigger left open is therefore **561 … 576**, not 561 … 692. Narrow — but real, un-scrollable, and
  it covers iPhone SE / 5s portrait (375×568). In a real browser with system fonts and longer player names the panel
  is taller and the gap correspondingly wider, so the fix is not academic.
* **Claim (b) is right as stated and is the stronger half of the finding**: `max-height` reads the CSS viewport while
  the layout is sized in `dvh`. An iPhone SE at 375×667 renders ~553dvh — clipped — while the media query sees 667 and
  never fires. No threshold can be made correct, which is why the conditional is gone rather than retuned.

**My first attempt at the gap test used 375×600 as the brief suggested and passed GREEN against the unfixed code**
(600 > 577). I measured, moved the test to 375×568, and only then had a RED.

### RED proof — gap test

Test added to `score.component.cy.ts` (`ScoreComponent — ein Panel`):
`laesst bei 375x568 die unteren Kacheln erreichen` — `assertNotClipped` for `obs-NET` and `stroke-BACKHAND`,
then `stroke-BACKHAND` + `obs-NET` clicked and `pointType: 'NET'` asserted on the request.

```
$ cd frontend && npx cypress run --component --spec src/app/features/matches/score/score.component.cy.ts
    1) laesst bei 375x568 die unteren Kacheln erreichen

       stroke-BACKHAND wird von "panels-area" abgeschnitten
       + expected - actual
       -465
       +457
```

GREEN after the fix (`panels-area` is `overflow: auto`, so it is scrollable rather than a hard clip).

### RED proof — the `.notes-view` consequence

Test added (`ScoreComponent — untere Leiste und Notizen`): `fuellt im Vollbild die ganze Seitenhoehe` — opens the
notes full-screen view at 360×692 and asserts `.notes-view` is at least `documentElement.clientHeight − 1` tall.

I applied the page change **without** the `.notes-view` change and ran the spec:

```
  67 passing
  1 failing
  1) ScoreComponent — untere Leiste und Notizen
       fuellt im Vollbild die ganze Seitenhoehe:
       Hoehe der Notizen-Ansicht
```

So the re-reviewer's consequence is confirmed empirically, not just by reading. `flex: 1` fixes it; spec then 68/68.

### The moved document-level assertion, and proof it is not vacuous

`scrollt auf 360x692 nicht` no longer asserts on `.panels-area` (which would now be trivially satisfied — with
`overflow: auto` and `flex-basis: 0` its `scrollHeight` tracks its own box). It now asserts
`document.documentElement.scrollHeight <= clientHeight + 1` at 360×692, after waiting for the panel to render.

Vacuity check — I temporarily made the layout deliberately over-tall (`.panels-area { min-height: 900px }`) and reran:

```
  1) scrollt auf 360x692 nicht
       Dokumenthoehe
       + expected - actual
       -1098
       +693
```

The assertion fails on an over-tall layout, so it still has teeth. The injected `min-height` was reverted immediately
(file restored from a pre-edit copy; `git status` confirmed only the two intended files were modified).

### Portrait no-scroll guarantee — not regressed

* `scrollt auf 360x692 nicht` passes at document level (693 vs 693), i.e. the page still fits the viewport exactly in
  portrait and nothing scrolls.
* `flex: 1` on `.panels-area` still fills the `min-height: 100dvh` box, so `.player-switch` and `.bottom-bar` stay
  pinned at the bottom. The existing tests that depend on this — `gibt den Umschaltflächen mindestens 56px Höhe`,
  `zeigt unten die zuletzt erfasste Beobachtung und den Notiz-Button`, `zeigt auch bei 1280x900 genau ein Panel` and
  the header `<= 100px` tests — all still pass.
* Tile sizing is unchanged: `gibt jeder Beobachtungs-Kachel mindestens 56px Höhe` and the
  `streckt die Kacheln … nicht über ihre natürliche Höhe` pair (`within(56, 96)`) still pass, so the looser page height
  did not let anything stretch.

### Landscape pair kept

`schneidet im Querformat (800x360) keine Kachel unerreichbar ab` and `laesst im Querformat (800x360) die untere Kachel
erfassen` are untouched and pass. `assertNotClipped` is now vacuous in landscape (no `overflow: hidden|clip` ancestor
remains) — expected; the click test is what keeps it honest.

---

## Finding 2 — `mat-dialog-title` inflated the handset dialog header

### What changed — `frontend/src/app/core/layout/responsive-dialog.ts`

`.dialog-header-title` gains `padding: 0` and `line-height: 1.2`, plus `.dialog-header-title::before { display: none }`.
The `mat-dialog-title` directive on the `<h2>` is untouched, so the accessible name still works.

### Finding confirmed — measured before the change (360×640, throwaway spec, deleted)

```
headerH   106.58       titlePadding  "6px 24px 13px"
titleH     91.16       beforeDisplay "inline-block"  beforeHeight "40px"
```

That is the finding exactly: `.dialog-header-title` overrode `margin` but not `padding`, and never touched the
`::before`.

**One addition to the prescribed fix.** `padding: 0` + `::before { display: none }` alone only got the header from
106.58 to **73.48** — still far above a compact header. Measuring the children showed a third inherited headline
property the review did not list: `.mat-mdc-dialog-title` also imposes `line-height: 32px`. It is the same class of
unwanted Material headline styling, so I added `line-height: 1.2`. Result: header **59.2px**, title box 43.2px, buttons
39.9px.

### RED proof

Test added to `frontend/src/app/features/players/player-dialog.component.cy.ts`:
`haelt die Kopfzeile bei 360px flach` — at 360×640 asserts the title box is at most two line-heights tall (catches the
40px `::before` and the 19px padding) and the whole `.dialog-header` is at most 64px.

Run against the unmodified `responsive-dialog.ts` (I reverted the fix, ran, then restored it):

```
$ cd frontend && npx cypress run --component --spec src/app/features/players/player-dialog.component.cy.ts
    1) haelt die Kopfzeile bei 360px flach

       Hoehe des Titels
       + expected - actual
       -91.16812896728516
       +65
```

(the header-height assertion was likewise RED at `-106.5947265625 / +64` in an earlier run of the same test.)

### GREEN

```
    ✓ füllt bei 390x736 die volle Breite und Höhe
    ✓ bleibt bei 1280x900 bei rund 500px
    ✓ hält Abbrechen und Speichern sichtbar, wenn das Formular scrollt
    ✓ gibt dem Dialog am Handy einen zugänglichen Namen
    ✓ haelt die Kopfzeile bei 360px flach
    ✓ hat am Handy Tap-Targets von mindestens 44x44
  6 passing
```

The accessible-name test (`aria-labelledby` non-empty, referenced element contains the title) still passes, so the
reason the directive is there is intact. Tap targets stay ≥ 44×44.

### Note on the wrap claim

The review predicted „Spieler bearbeiten" would wrap in a ~160px slot. In the component test the slot is only ~53px,
because the fixture renders raw i18n keys (`common.cancel` / `common.save`) which are wider than the real German
labels — so the test overstates the horizontal squeeze and the title still wraps to two lines there. I therefore did
**not** pin "single line": that would be an artifact of the fixture, and forcing `white-space: nowrap` would ellipsise
a dialog title on narrow real devices, which is worse than wrapping. The test pins the two things that actually
regressed and are fixture-independent: the phantom 59px in the title box, and the total header height.

### Other consumers

`DialogHeaderComponent` is shared; the full Cypress run (match-setup, score edit, end-match, players) is green, so no
other dialog header regressed.
