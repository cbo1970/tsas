# TSaS am Handy — Hochformat und Einhandbedienung

**Datum:** 2026-11-25
**Status:** Entwurf zur Freigabe
**Tickets:** TEN-71 (Scoring-Screen), TEN-70 (Coach-Kommentare, separat), TEN-72 (Spieler-Stammdaten, separat)
**Mockups:** [Ist-Zustand mit Messwerten](https://claude.ai/artifact/PwSq4NZpt7Bp1v2Moy8mmk) · [Varianten Scoring-Screen](https://claude.ai/artifact/KbV8ggyGuUtwXRUH14qVCK)

---

## Absicht und Warum

Die Angular-Web-App von TSaS ist am PC und am Tablet brauchbar. Am Handy im Hochformat ist sie es nicht — und das Handy ist genau das Gerät, das der Coach am Platz dabei hat.

Zwei Situationen bestimmen die Arbeit:

**Während des Matches.** Der Coach hält das Gerät in einer Hand und erfasst Punkt für Punkt mit dem Daumen, während er auf den Platz schaut. Heute sind die Beobachtungs-Kacheln rund 50 × 25 px gross und liegen paarweise nebeneinander — das verlangt genaues Hinsehen und genaues Treffen, beides hat man in dem Moment nicht. Hier zählt, dass man blind trifft.

**Zwischen zwei Turnierrunden.** Steht der Gegner der nächsten Runde fest, muss er erfasst und ein Match angelegt werden. Zeit ist dabei nicht knapp — zwischen zwei Runden liegt mindestens eine Stunde Erholung. Was zählt, ist, dass der Weg naheliegt und nicht aus unnötig vielen Schritten besteht. Heute sind es rund fünfzehn Interaktionen über zwei Seiten und einen Dialog, weil sich aus dem Match-Setup heraus kein neuer Spieler anlegen lässt.

Der Anstoss war ursprünglich weiter gefasst — eine native, offlinefähige App für iOS und Android. Im Gespräch hat sich gezeigt, dass das eigentliche Ärgernis die Bedienbarkeit im Hochformat ist, nicht die Technik. Offline-Fähigkeit und native App sind damit ausdrücklich zurückgestellt, nicht verworfen.

## Ziele

- Der Scoring-Screen ist auf 360 × 800, 390 × 844 und 430 × 932 (jeweils mit sichtbarer Browser-Adressleiste) ohne Scrollen bedienbar, mit einem Daumen, ohne Hinsehen.
- Jedes Beobachtungs-Tippziel misst mindestens 56 px in der Höhe.
- Spieler anlegen und Match erstellen funktionieren am Handy ohne Umweg über die Spielerliste.
- Die Navigation ist am Handy erreichbar, ohne seitlich aus dem Bild zu laufen.
- Die Darstellung am Desktop bleibt unverändert — mit einer bewussten Ausnahme, dem Scoring-Screen (siehe unten).

## Nicht-Ziele

- Offline-Fähigkeit, lokale Datenhaltung, Synchronisation.
- Native Apps für iOS oder Android, Capacitor, PWA-Installation.
- Neue Punkt-Attribute oder sonstige fachliche Erweiterungen.
- Änderungen am Backend, am Datenmodell oder an den Flyway-Migrationen.
- Dark Mode. Die App hat heute nur das helle Theme; das bleibt so.
- Querformat als eigener Entwurf. Es bekommt kein eigenes Layout. „Funktioniert" heisst hier: nichts wird abgeschnitten, nichts läuft waagerecht über, alles bleibt erreichbar. Senkrechtes Scrollen ist im Querformat zulässig.

## Anti-Ziele

Woran die Arbeit scheitert, auch wenn sie technisch funktioniert:

- **Ein Scoring-Screen, der zwar passt, aber Fehlerfassungen produziert.** Ein falsch erfasster Punkt ist teurer als ein zusätzlicher Tipp. Diese Abwägung hat die Variantenwahl entschieden (siehe unten).
- **Eine verschlechterte Desktop-Ansicht.** Der Coach arbeitet dort weiter, die Vorbereitung passiert am Rechner.
- **Ein Umbau, der erst am Schluss nutzbar ist.** Jeder Screen muss für sich fertig werden und einzeln am Platz testbar sein.

## Randbedingungen

- Angular 21 standalone, Angular Material, `@angular/cdk` ist bereits Abhängigkeit. Keine neuen Abhängigkeiten ohne Not.
- Die Design-Tokens in `styles.scss` (`--surface-*`, `--text*`, `--brand*`, `--success`, `--danger`) sind gesetzt und werden weiterverwendet.
- Tests: Vitest-Unit-Tests und Cypress-Komponententests. Die Cypress-Tests hängen an `data-testid`-Attributen; die ziehen bei Umbauten mit um.
- Das Frontend hat keine instrumentierte Coverage, es gibt also keine Abdeckungsschwelle, die die Arbeit blockiert.
- Viersprachigkeit (de/en/it/fr) über `@ngx-translate`. Neue Beschriftungen gehören in alle vier Dateien. **Ausnahme Scoring-Screen:** er ist heute zu hundert Prozent hart auf Deutsch — jede Kachel, jede Meldung. Das nachzuholen ist ein eigenes Ticket und nicht Teil dieser Arbeit. Die Kacheln sind aber von Anfang an so zu bauen, dass längere Beschriftungen passen: umbruchfähig, Mindesthöhe statt fester Höhe. Französisch und Italienisch fallen länger aus als Deutsch, und das erst nach dem Layout zu merken ist teuer.

## Ausgangslage, gemessen

Im gerenderten Layout bei 390 px Breite:

| | heute |
|---|---|
| Kachel „Forced" / „Unforced" / „Netz" | ca. 50 × 25 px |
| Empfohlenes Tippziel | 44 × 44 px |
| Scoring-Seite gegenüber Bildschirmhöhe | ca. +64 px, scrollt |
| Media Queries im gesamten Frontend | 1 (in `player-notes.component.ts`) |

Ursachen:

1. `.panels-area` hat `grid-template-columns: 1fr 1fr` — zwei Beobachtungs-Panels nebeneinander.
2. `.scoring-page` hat `height: 100dvh`, sitzt aber unter der 64 px hohen `mat-toolbar`. Da `.scoring-page` zugleich `overflow: hidden` trägt, lässt sich innen nicht scrollen.
3. Schriftgrössen aus der Desktop-Welt: Abschnitts-Labels 9 px, Satzverlauf 10 px, Spielernamen 14 px.
4. Die `mat-toolbar` enthält Titel, zwei Textbuttons, Sprachwahl, Benutzernamen und Menü-Icon in einer Zeile, die nicht umbricht.
5. Die Spielerliste ist eine `mat-table` mit fünf Spalten bei 24 px Seitenrand.
6. Drei Dialoge sind fest auf `width: '500px'` gesetzt: Spieler anlegen, Spieler bearbeiten, Spielstand korrigieren.
7. Im Match-Setup stehen Label (120 px Mindestbreite) und Bedienelement nebeneinander, ohne Umbruch.
8. Aus dem Match-Setup lässt sich kein neuer Spieler anlegen; die Autocompletes finden nur bestehende.

Erfreulich: Die vier Leseseiten sind bereits schmal gebaut — Match-Statistik 480 px, Head-to-Head 520 px, Matchanalyse 560 px, Match-Historie 560 px. Dort ist nur Feinarbeit zu erwarten.

## Ansätze

### Grundrichtung

Zwei Wege standen zur Wahl:

- **Ein mitwachsendes Layout.** Schmal gebaut, am grossen Bildschirm mit mehr Luft und mehr Spalten. Die Tabelle wäre überall eine Liste. Weniger Code, eine Darstellung weniger zu testen — aber die Desktop-Ansicht ändert sich mit.
- **Zwei Layouts nebeneinander, per Media Query getrennt.** Am Desktop bleibt alles wie heute, am Handy kommt eine eigene Darstellung. Mehr Code, mehr Tests.

**Gewählt: zwei Layouts per Media Query.** Entscheidung des Auftraggebers, in Kenntnis des höheren Aufwands. Begründung: Die Desktop-Ansicht ist in Ordnung und soll unangetastet bleiben.

Das gilt für die Spielerliste, das Match-Setup, die Dialoge und die App-Navigation. **Nicht für den Scoring-Screen:** dort ersetzt Variante C die heutige Darstellung auf allen Bildschirmbreiten.

### Scoring-Screen

Drei Entwürfe wurden als tippbare Mockups gebaut und am Gerät geprüft. Allen gemeinsam: Die App-Toolbar verschwindet auf dieser Seite, die Kopfzeile wird auf rund 95 px verdichtet. Diese rund 120 px bezahlen die grösseren Kacheln.

- **A — beide Panels gestapelt und vollständig.** Wörtlich das ursprünglich Gefragte. Kacheln werden über 160 px breit. Aber zwanzig Bedienelemente teilen sich einen Bildschirm, es bleiben rund 36 px Höhe pro Kachel, und auf 360 px scrollt die Seite.
- **B — gestapelt und nach Aufschlag zugeschnitten.** Beide Panels sichtbar, der Returnierende zeigt kein Ass, keinen Doppelfehler und keine Service-Wahl. Damit sind 44 px je Ziel erreichbar, bei einem Tipp pro Punkt. Preis: unsymmetrische Panels, und das obere liegt ausserhalb der bequemen Daumenreichweite.
- **C — nur der aktive Spieler.** Ein Panel füllt den Bildschirm, Kacheln 56 px, darunter ein Umschalter zwischen den Spielern. Alles Bediente liegt im unteren Bildschirmdrittel. Preis: Gehört der Punkt zum anderen Spieler, kostet es einen zusätzlichen Tipp — weil im Ballwechsel wechselt, wer den letzten Ball schlägt, betrifft das grob die Hälfte der Punkte.

**Gewählt: C.** Der zweite Tipp ist bewusst in Kauf genommen. Ausschlaggebend war, dass der Coach beim Erfassen auf den Platz schaut und nicht aufs Gerät; dann wiegt Treffsicherheit schwerer als ein gleichmässiger Ein-Tipp-Rhythmus.

### Erfassungs-Screens

- **1 — nur Layout.** Jede Seite bekommt per Media Query ein Handy-Layout, kein Ablauf ändert sich. Risikoarm, aber die rund fünfzehn Schritte zum Match gegen einen neuen Gegner bleiben.
- **2 — Layout plus Spieler direkt im Match-Setup anlegen.** Zusätzlich erscheint in der Namenssuche ein Eintrag „«Name» neu anlegen", der das Spielerformular vorbefüllt öffnet und den Spieler nach dem Speichern direkt setzt. Rund acht Interaktionen statt fünfzehn, ohne Backend-Änderung.
- **3 — zusätzlich die Pflichtfelder entschärfen.** Geschlecht, Spielhand und Backhand nicht mehr still vorbelegen, sondern als „unbekannt" führen. Berührt Datenmodell, Migration und Auswertungen.

**Gewählt: 2.** Ansatz 3 beschreibt ein echtes Problem, aber ein datenseitiges — es ist als **TEN-72** herausgelöst und blockiert diese Arbeit nicht.

## Entscheidungen im Einzelnen

### App-Shell und Navigation

- Unterhalb der Handy-Grenze ersetzt eine **untere Navigationsleiste** die `mat-toolbar`. Begründung wie beim Scoring-Screen: Der Daumen erreicht den unteren Rand.
- Einträge: **Spieler**, **Neues Match**, **Mehr**. Hinter „Mehr" liegt, was heute im Toolbar-Menü steckt — Sprachwahl, Benutzername, Datenexport, Daten löschen, Abmelden — sowie das Admin-Abzeichen, wenn es zutrifft.
- Die Leiste berücksichtigt `env(safe-area-inset-bottom)`, damit sie auf Geräten mit Home-Indikator nicht darunter liegt.
- **Auf der Scoring-Route erscheint weder Toolbar noch Navigationsleiste.** Diese Seite trägt ihre eigene Kopfzeile mit Zurück-Pfeil. Dabei ist `main { min-height: calc(100vh - 64px) }` in `app.ts` mit zu prüfen — diese Regel ist Teil der heutigen Überlaufursache.
- Am Desktop bleibt die Toolbar unverändert.

### Scoring-Screen

Vollständig in **TEN-71** beschrieben, inklusive Zielaufbau, Panel-Inhalten je Rolle, Verhalten und Akzeptanzkriterien. Kurzfassung: eine verdichtete Kopfzeile, ein Panel mit 56-px-Kacheln, ein Spielerumschalter und eine untere Leiste mit letzter Beobachtung und Notiz-Button.

Fachlich bestätigt und dem Entwurf zugrunde gelegt: **Ass und Doppelfehler können nur beim aufschlagenden Spieler vorkommen**, ebenso die Unterscheidung 1./2. Service. Ass heisst, der Aufschlag wird vom Gegner nicht erreicht und führt direkt zum Punkt; Doppelfehler heisst, beide Aufschläge waren nicht korrekt (Netz oder im Aus) und der Punkt geht an den Returnierenden.

**Variante C gilt auf allen Bildschirmbreiten.** Der Scoring-Screen bekommt als einziger keine zweite Darstellung für den Desktop. Begründung: Erfasst wird im Match, und das passiert am Handy. Zwei Scoring-Layouts parallel zu pflegen hiesse, die komplexeste Komponente der App auf Dauer doppelt zu ändern. Bewusst in Kauf genommen: Am Desktop entfällt die heutige Zwei-Panel-Ansicht mit einem Tipp pro Punkt. Die Toolbar ist auf dieser Route entsprechend auf jeder Breite ausgeblendet.

**Der Punktestand bleibt antippbar.** Ein Tipp auf die Punktzahl erfasst wie heute einen Punkt ohne Attribut (`quick-score-p1` / `quick-score-p2`; Backend und `MatchStatisticsService` behandeln Punkte mit `pointType == null` ausdrücklich). Die verdichtete Kopfzeile trägt dafür zwei Tippziele, keine reinen Anzeigewerte.

Weitere Punkte, die beim Gegenlesen aufgefallen sind und hier entschieden werden:

**Wer schlägt auf — der Anfangszustand.** `MatchScore.servingPlayer` ist nullable; ein frisch angelegtes Match hat keinen Aufschläger. Heute behilft sich der Screen damit, dass der allererste Tipp auf eine beliebige Kachel den Aufschläger setzt, statt einen Punkt zu erfassen. Für Variante C trägt das nicht, weil sich ohne Aufschläger nicht entscheiden lässt, welches Panel in welcher Rolle erscheint. **Entscheidung:** Ist `servingPlayer` leer, zeigt der Screen vor dem Panel eine Auswahl „Wer schlägt auf?" mit den beiden Spielernamen als zwei grossen Flächen. Nach der Wahl erscheint das Panel. Der stillschweigende erste Tipp entfällt.

**Der Aufschlagversuch gehört zum Punkt, nicht zum Panel.** Heute hält jedes Panel seinen eigenen 1./2.-Service-Zustand. In Variante C hat der Returnierende keine Service-Wahl mehr — genau das bezahlt die 56-px-Kacheln. Würde der Zustand weiterhin am Panel hängen, trügen alle vom Returnierenden-Panel erfassten Punkte `serveAttempt = null`, und da das rund die Hälfte aller Punkte ist, bräche die Statistik „% 1./2. Service" ein: `MatchStatisticsService` und `HeadToHeadStatisticsService` zählen einen Punkt nur, wenn `serveAttempt` gesetzt ist. **Entscheidung:** Es gibt genau einen Aufschlagversuch für den laufenden Punkt. Er wird auf dem Aufschläger-Panel gewählt und gilt für den nächsten erfassten Punkt, unabhängig davon, von welchem Panel aus erfasst wird. Nach dem Erfassen wird er zurückgesetzt.

**Die Notizen-Ansicht.** Eine eigene Notizen-Route gibt es nicht; die Notizen stecken heute als aufklappbares `<details>` mit `PlayerNotesComponent` in der Scoring-Seite — das verträgt sich nicht mit der Vorgabe, dass nichts scrollt. **Entscheidung:** Der Notiz-Button öffnet innerhalb der Scoring-Route eine Vollbildansicht mit Zurück-Pfeil, die `PlayerNotesComponent` unverändert wiederverwendet. Das aufklappbare `<details>` entfällt. Der Punkt-Indikator am Button ergibt sich aus den beim Laden ohnehin abrufbaren Notizen; ein neuer Endpunkt ist dafür nicht nötig.

### Spielerliste

- Unterhalb der Handy-Grenze ersetzt eine **Kartenliste** die `mat-table`. Je Karte: Name, darunter Ranking und Spielhand, rechts das Ball-Symbol für ein laufendes Match (führt direkt dorthin) und ein Überlauf-Menü.
- Die Zeile hat heute vier Aktionen — Match-Historie, Head-to-Head, Löschen beziehungsweise Inaktivieren, plus den Ball für das laufende Match. Vier Tippziele à 44 px passen bei 390 px nicht neben einen Namen. **Entscheidung:** Nur das Ball-Symbol bleibt direkt auf der Karte, die übrigen Aktionen liegen hinter einem Überlauf-Menü (⋮).
- Der Seitenrand sinkt von 24 px auf 16 px, die Kopfzeile (Titel, Suchfeld, Button) stapelt statt nebeneinander zu stehen.
- Antippen der Karte öffnet wie heute den Bearbeiten-Dialog (`openEditDialog`). Die Match-Historie bleibt eine eigene Schaltfläche auf der Karte. Das Verhalten der Tabellenzeile am Desktop ändert sich nicht.
- Am Desktop bleibt die Tabelle mit allen fünf Spalten.

### Dialoge

- Die feste Breite `width: '500px'` entfällt zugunsten einer Angabe, die am Handy die volle Breite nutzt und am Desktop bei rund 500 px bleibt.
- Betroffen: `PlayerDialogComponent` (zwei Aufrufstellen) und `ScoreEditDialogComponent`. `EndMatchDialogComponent` setzt keine feste Breite und ist nur auf Lesbarkeit zu prüfen.
- Am Handy bekommen die Dialoge die volle Höhe, damit die Tastatur die Felder nicht verdeckt.

### Match anlegen

- Seitenrand 16 px statt 24 px, Überschrift kleiner als 28 px.
- Die Einstellzeilen (Sätze bis Sieg, Match-Tiebreak, Short Set) stellen das Label über das Bedienelement statt daneben. Die Umschalter bleiben am rechten Rand gut erreichbar.
- **Neu: Spieler aus der Namenssuche heraus anlegen.** Findet die Autocomplete keinen passenden Eintrag, erscheint als letzte Option „«Name» neu anlegen". Sie öffnet `PlayerDialogComponent` mit dem getippten Text vorbefüllt — sinnvoll aufgeteilt auf Vor- und Nachname, siehe offene Punkte. Nach dem Speichern wird der neue Spieler direkt in das Feld übernommen, aus dem heraus er angelegt wurde.
- Der Dialog bedient damit zwei Aufrufer. Er bleibt dieselbe Komponente; die Vorbefüllung kommt über die bestehenden `data`-Parameter.

### Leseseiten

Match-Statistik, Head-to-Head, Matchanalyse und Match-Historie sind bereits auf 480–560 px begrenzt und passen damit auf jedes Handy hochkant. Hier ist kein Umbau vorgesehen, sondern eine Durchsicht: Seitenränder, Schriftgrössen unter 11 px, Tippziele und waagerechtes Überlaufen. Was dabei auffällt, wird als eigenes Ticket erfasst, nicht in dieser Arbeit mitgemacht.

## Was dem Umsetzenden überlassen bleibt

- **Die genaue Umschaltgrenze.** Vorschlag: eine gemeinsame Grenze bei rund 700 px, damit das Verhalten über alle Seiten gleich ist. Wo eine Seite eine andere Grenze braucht, ist das in Ordnung, soll aber begründet im Code stehen. Achtung auf das Querformat: Ein Handy quer misst rund 800 × 360 px und liegt damit **über** jeder reinen Breitengrenze — eine Breiten-Media-Query allein liefert dort die Desktop-Darstellung auf 360 px Höhe. `Breakpoints.Handset` aus `@angular/cdk/layout` deckt Hoch- und Querformat ab und ist deshalb die naheliegende Bedingung für die strukturellen Wechsel.
- **Wie strukturell umgeschaltet wird.** Eine reine CSS-Media-Query kann kein DOM austauschen; Tabelle gegen Kartenliste und Toolbar gegen Navigationsleiste sind Strukturwechsel. Naheliegend ist `BreakpointObserver` aus `@angular/cdk/layout` für diese Fälle und CSS-Media-Queries für alles, was reine Anordnung ist. Beides doppelt zu rendern und eines auszublenden ist ebenfalls möglich, verdoppelt aber die Treffer der Testselektoren — dann sind die Tests entsprechend zu schärfen.
- **Aufteilung des getippten Namens** auf Vor- und Nachname bei „neu anlegen". Eine einfache Regel (erstes Wort als Vorname, Rest als Nachname) genügt, solange der Nutzer im Dialog korrigieren kann.
- **Benennung und Symbolik der Navigationseinträge**, im Rahmen des bestehenden Material-Icon-Satzes.
- **Reihenfolge der Umsetzung.** Vorschlag: App-Shell zuerst, danach Scoring-Screen, dann Match-Setup samt Dialogen, dann Spielerliste, zum Schluss die Durchsicht der Leseseiten.

## Abgrenzung und zugehörige Tickets

Teil dieser Arbeit, in Umsetzungsreihenfolge:

| Ticket | Inhalt |
|---|---|
| **TEN-73** | App-Shell: untere Navigationsleiste am Handy |
| **TEN-71** | Scoring-Screen Variante C |
| **TEN-74** | Match anlegen am Handy, Spieler direkt aus der Namenssuche anlegen (samt Dialogen) |
| **TEN-75** | Spielerliste am Handy als Kartenliste statt Tabelle |
| **TEN-76** | Durchsicht der vier Leseseiten im Hochformat |

Eigenständig, blockiert nichts:

| Ticket | Inhalt | Verhältnis |
|---|---|---|
| **TEN-70** | Mehrere Coach-Kommentare pro Spieler und Match | Der Notiz-Button führt vorerst auf das heutige Einzelfeld je Spieler |
| **TEN-72** | Spieler-Stammdaten als „unbekannt" statt stiller Vorgaben | Datenfrage, nicht Layout |
| **TEN-77** | Scoring-Screen in alle vier Sprachen übersetzen | Nach TEN-71, sonst wird zweimal übersetzt |

**Lieferform:** ein Pull Request je Screen gegen `develop`, in der oben vorgeschlagenen Reihenfolge. Jeder PR ist für sich am Platz testbar. Der Status in Linear wird mitgeführt — In Progress beim Start, In Review beim Öffnen des PR, Done beim Merge.

Ausdrücklich nicht Teil dieser Arbeit und nicht verworfen, sondern zurückgestellt: Offline-Erfassung, lokale Datenhaltung ohne Server-Konto, native Apps für iOS und Android. Fällt diese Entscheidung später anders aus, betrifft sie vor allem die Scoring- und Statistiklogik, die heute ausschliesslich im Backend liegt (`ScoringService`, 253 Zeilen; Statistik-Services, 388 Zeilen) und für eine Offline-Nutzung auf das Gerät müsste.
