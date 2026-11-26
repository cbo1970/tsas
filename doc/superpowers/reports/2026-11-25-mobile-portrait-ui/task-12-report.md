# Task 12 – Vier Leseseiten am Gerät prüfen

## 1. How I measured

- Throwaway Cypress component specs (`src/app/zz-probe.cy.ts`, `zz-probe2.cy.ts`) mounted each of the four components with `cy.viewport` set explicitly to 360x692, 390x736 and 800x360. 430 was not measured.
- Per page and viewport the probe recorded:
  - `.page` margins, document `scrollWidth` versus the viewport.
  - Every element with own text and a computed font-size under 11px.
  - Every button, input, mat-select, `.set-tab` and `.entry` under 43.5px in either dimension, plus the Material touch-target elements.
  - Elements whose box leaves the viewport, and `scrollWidth > clientWidth`.
  - Wrapped labels.
- Stress data: names of 45 and 31 characters, five sets, an analysis with a long recommendation title plus an OPEN and a REJECTED recommendation (so the reset button shows), and history entries with a very long opponent.
- The first run used the key-returning test translate provider, which showed keys instead of text. I redid it with the real `public/i18n/de.json` so label widths are real.
- I re-ran Head-to-Head with realistic names ("Alexander Zverev" vs "Stefanos Tsitsipas") and took a screenshot, to separate the stress-data failure from the realistic case.
- Not measured: the opened mat-select panel, and the Head-to-Head AI preparation card (it has plain 13/14px text and no grid).
- All throwaway specs, JSON output and screenshots are deleted. `git status` shows only the two intended source files.

## 2. Findings

Side margins: all four pages have 16px padding at 360 and 390. Nothing to fix. At 800x360 the pages are centred (120-160px margins).

### Match statistics (`features/matches/statistics`)
1. **Font under 11px.** `.stat-label` and `.set-label` were 10px (15 elements). Fixed, see section 3.
2. **Set tabs are too small to tap.** `.set-tab` (Gesamt, Satz 1 to 5) measures 61-70 x 24px against 44px. With 5 sets they wrap to a second row, only 6px apart. Fix: raise the pill height with padding and `min-height: 44px`, which is a layout change on the tab row.
3. **"VH xx%" and "RH xx%" in the right-hand value column wrap to two lines.** The column is 48px minus 4px padding, and the right value is 26px high where the left is 13px. This is also visible on desktop. Fix: widen the `48px 1fr 48px` columns to about 56px, or drop the prefix.
4. **Long player names wrap to several lines.** A 45-character name takes 70px at 360. There is no horizontal overflow. This is acceptable, and I made no change.
5. Mat-buttons (Taktische Analyse, Zur Spielerübersicht) are 40px high but have a 48px touch target, so they are OK.
6. No overflow at any viewport, and landscape is fine (vertical scroll only).

### Head-to-Head (`features/statistics/head-to-head`)
1. **Font under 11px.** `.stat-label` was 10px (14 elements). Fixed.
2. **Horizontal overflow with long names.**
   - The player-name row sits in the `64px 1fr 64px` grid. Unbreakable words wider than 64px (measured: "Hohenzollern-Sigmaringen" about 90-98px) overflow the column.
   - The document becomes 378px wide at 360 and 408px wide at 390. At 800 the 520px page overflows by 18px.
   - It overlaps or pushes the centre column.
   - With realistic names such as "Alexander Zverev" it fits. They wrap onto 2 lines and nothing overflows (screenshot-checked).
   - Fix: add `overflow-wrap: anywhere` or `hyphens` to `.val` in the name row, or widen the side columns. This is a layout change, so I left it.
3. **Combined values wrap.** "140 (33%)" and "122 (18%)" wrap to 2 lines in the 64px column at all widths. Fix: widen the columns, or show the percentage on a second line deliberately.
4. **Selected player name truncated in the select.** The name is cut to "Alexander Zv…" (field about 158px, trigger about 116px). For similar names the user cannot see which player is selected. Fix: let the picker row stack (1 column) at phone width. This is a layout change.
5. **Tap targets.** The form fields are 158x76 (wider than 44, and the whole field is clickable). The `mat-select` element is 24px high, but that is a false positive. The AI preparation button is 40px with a 48px touch target, so it is OK.
6. Landscape: no horizontal overflow with realistic names.

### Match analysis (`features/matches/analysis`)
1. **`rec-note-input` is far too small.** It is 24px high (width 266px at 360) against 44px. This is the free-text note field of the AI recommendation review. Fix: `min-height: 44px` and more padding, plus a larger font for legibility. It is a layout change inside `.rec-review`, so I left it.
2. **`rec-review` buttons.** Accept, Reject and Reset are mat-buttons, 40px high with a 48px touch target, so they are OK. They sit 6px apart. This is acceptable, but tight if the layout is ever reworked.
3. **Embedded `PlayerNotesComponent` (outside the four directories).**
   - The save buttons (`note-save-N`) are 22px high and span the full width.
   - `.slot-role` is 10px (2 elements). This is a font-size fix, but the component belongs to the notes feature and is also used elsewhere, so I left it. It is a candidate for the same ticket as the finding above.
4. No horizontal overflow, even with a very long recommendation title and 45-character names. The page is 1337px high at 360, so it scrolls vertically, which is fine.

### Match history (`features/players/match-history`)
1. **Nothing to fix.** No font under 11px, no overflow. The long opponent name wraps (114px high at 360) and the result pill and date stay inside. The `.entry` cards are at least 44px high. Only the "Zur Spielerübersicht" button is 40px, but its touch target is 48px.

## 3. What I fixed

| File | Selector | Before | After |
|---|---|---|---|
| `features/matches/statistics/statistics.component.ts` | `.set-label` | 10px | 11px |
| `features/matches/statistics/statistics.component.ts` | `.stat-label` | 10px | 11px |
| `features/statistics/head-to-head/head-to-head.component.ts` | `.stat-label` | 10px | 11px |

After the change no text on the four pages is under 11px except the embedded notes `.slot-role`. The labels stay on one line, including the longest German label "Return-Punkte 1. Aufschlag %" at 360 (measured height 13px). No other change. Side margins needed no fix.

## 4. What I deliberately left (ticket candidates)

1. Statistics: set tabs 24px high, need 44px, and the wrapped row needs spacing.
2. Statistics: "VH/RH %" in the right value column wraps (also on desktop).
3. Head-to-Head: long player names overflow the 64px columns, with horizontal page overflow (378 at 360, 408 at 390, +18px at 800).
4. Head-to-Head: "140 (33%)" and "122 (18%)" wrap in the 64px columns.
5. Head-to-Head: the select truncates the chosen name, so the pickers want to stack at phone width.
6. Analysis: `rec-note-input` is 24px high, needs 44px.
7. Embedded `PlayerNotesComponent`: save buttons 22px high, and `.slot-role` at 10px.
8. Optional: history, statistics and analysis rely on the 48px Material touch target (buttons visually 40px). No change needed, just recorded.

## 5. Test results

- Vitest: 37 passed.
- Cypress component: 162 passed across 12 specs, all green.
