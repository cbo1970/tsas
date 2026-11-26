# Umsetzungsprotokoll: Hochformat und Einhandbedienung

Arbeitsbelege zum Umbau des Frontends auf Hochformat und Einhandbedienung (PR #58, Tickets TEN-71 und TEN-73 bis TEN-76).

Die Umsetzung lief subagent-getrieben: ein frischer Umsetzer je Aufgabe, danach ein frischer Prüfer, der dessen Diff gegen Auftrag und Spezifikation hält — gefolgt von einer Schlussprüfung über den gesamten Branch und einer Fix-Welle.

**Spezifikation:** `doc/superpowers/specs/2026-11-25-mobile-portrait-ui-design.md`
**Plan:** `doc/superpowers/plans/2026-11-25-mobile-portrait-ui.md`

## Was hier liegt

| Datei | Inhalt |
|---|---|
| `00-ledger.md` | Das Protokoll: Konfliktscan vor dem Start, jede getroffene Entscheidung mit Begründung und Irrtumskosten, jeder zurückgestellte Befund, Commit-Ranges je Aufgabe |
| `task-N-brief.md` | Der Auftrag, mit dem die Aufgabe N vergeben wurde |
| `task-N-report.md` | Der Bericht des Umsetzers: RED/GREEN-Belege, geänderte Dateien, Selbstprüfung, Bedenken |
| `final-fix-report.md` | Die Fix-Welle nach der Schlussprüfung über den ganzen Branch |
| `parked-fix-report.md` | Die beiden zunächst zurückgestellten und später doch behobenen Befunde |

Die Review-Pakete (Diffs) sind hier nicht abgelegt — sie lassen sich aus den im Ledger genannten Commit-Ranges jederzeit mit `git diff` erzeugen.

## Wozu das taugt

Drei Dinge, die sich daraus belegen lassen und die aus der Commit-Historie allein nicht hervorgehen:

**Testnachweise.** Jeder `task-N-report.md` enthält den RED-Lauf mit der konkreten Fehlermeldung vor der Umsetzung und den GREEN-Lauf danach. Mehrfach wurde ein Test zusätzlich durch Mutation geprüft: Code absichtlich kaputtgemacht, Fehlschlag belegt, zurückgenommen.

**Entscheidungen samt Begründung.** Das Ledger führt jede Entscheidung, die während der Umsetzung ohne Rückfrage getroffen wurde, mit dem Grund und der Angabe, was ein Irrtum kosten würde.

**Was die Prüfungen gefangen haben.** Drei Befunde der Schlussprüfung waren durch zwölf Einzelprüfungen gerutscht und betrafen alle stille Datenfehler: ein Schnellpunkt, der den Aufschlagversuch nicht aufräumte und den *nächsten* Punkt falsch stempelte; unerreichbare Kacheln im Querformat; und ein Weg, der Spieler-Dubletten erzeugt hätte. Nachlesbar in `final-fix-report.md`.

Bemerkenswert auch, was die Umsetzer an den Anweisungen korrigiert haben — zweimal hätte ein von mir vorgeschlagener Test gegen den kaputten Code bestanden und war damit wertlos; beide Male fiel es beim Nachmessen auf, nicht beim Lesen. Dokumentiert in `parked-fix-report.md` und im Ledger.
