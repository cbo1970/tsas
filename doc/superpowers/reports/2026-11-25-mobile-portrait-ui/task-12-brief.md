### Task 12: Vier Leseseiten am Gerät prüfen

**Files:**
- Review: `features/matches/statistics`, `features/statistics/head-to-head`, `features/matches/analysis`, `features/players/match-history`

Keine TDD-Aufgabe — eine Durchsicht, deren Ergebnis Tickets sind.

- [ ] **Step 1: Durchsicht bei 360 × 692 und 390 × 736**

Je Seite prüfen: Seitenränder, Schriftgrössen unter 11 px, Tippziele unter 44 px, waagerechtes Überlaufen (besonders das Gitter `64px 1fr 64px` im Head-to-Head und lange Spielernamen), Verhalten im Querformat.

- [ ] **Step 2: Kleinstkorrekturen anwenden**

Nur Seitenrand und Schriftgrösse, wo keine Layoutänderung nötig ist. Alles andere wird notiert, nicht behoben.

- [ ] **Step 3: Befunde erfassen**

Kommentar an TEN-76 mit der Liste des Gefundenen, dazu je ein Ticket für alles, was eine Layoutänderung braucht.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/features/
git commit -m "fix(ui): Seitenränder und Schriftgrössen der Leseseiten am Handy"
```

---

## Abschluss

- [ ] Alle Tests grün: `cd frontend && npx ng test --no-watch && npx cypress run --component`
- [ ] Jede der drei Zielbreiten (360 / 390 / 430) am Gerät oder in der Emulation durchgeklickt
- [ ] Linear-Status aller fünf Tickets auf Done
