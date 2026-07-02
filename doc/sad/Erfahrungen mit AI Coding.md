# Erfahrungen und Learnings mit AI-assisted Software Engineering

## Ausgangslage vor dem CAS AI Assisted Software Engineering

- 30 Jahre Erfahrung in der Softwareentwicklung von C-, C++-, Java- und Python-Applikationen.
- Keinerlei Erfahrung in der Softwareentwicklung mit AI.

## Erste Schritte

Die Installation des Claude Code CLI war etwas hakelig.

Nach dem ersten Vor-Ort-Tag war ich etwas verunsichert, wie ich beginnen sollte. Es gab zwei Ansätze: alles die AI machen lassen – oder das SAD klassisch von Hand. Ich habe dann begonnen, meine Gedanken, Ideen und Ziele zum Projekt in einem initialen Markdown-Dokument zu notieren.

Nach dem ersten Start des Claude Code CLI im Projektverzeichnis durchsuchte Claude das Verzeichnis, fand das Dokument und wollte gleich mit der Implementation des Backends loslegen. Meine Neugier war geweckt, und ich liess Claude mit ein paar Eckpfeilern gewähren. Nach ca. 10–20 Minuten war er mit dem initialen BE fertig und wollte mit dem FE beginnen. Das war mir dann doch zu unheimlich, und ich wollte zuerst ein zu 70 % fertiges SAD haben.

## Erstellung des SAD

Persönlich finde ich Markdown gut für schnelle Notizen und READMEs, aber für ein SAD mit Grafiken ist mir Markdown zu unübersichtlich. Speziell Grafik-Formate wie Mermaid finde ich unübersichtlich. So habe ich begonnen, das SAD nach arc42 von Hand mit dem Word-Template zu schreiben. Das wurde auch schnell mühsam, und da wir ja AI nutzen sollten, habe ich dann Claude (nicht dem CLI) die bereits erstellten Dokumente hochgeladen und ihn angewiesen, ein sauberes Word-Dokument nach arc42 zu erstellen. Das funktionierte gut, aber der Roundtrip mit Word ist schlecht – somit wieder zurück zu Markdown mit seinen Problemen.

**Fazit:** Das Arbeiten mit Dokumenten finde ich mit AI bislang eher mühsam. Ich werde wohl noch etwas Zeit investieren müssen, um meinen Weg zu finden.

## Programmieren mit AI

Nach den ersten Schritten und einem zu 70 % fertigen SAD habe ich dann mit dem Claude CLI Schritt für Schritt meine Applikation entwickeln lassen – die ersten paar Wochen nur mit Prompts, später dann auch mit Linear-Tickets.

## Eingesetzte Tools

- Claude Code CLI und App
- Linear
- GitHub
- MCP-Server, um Linear und GitHub mit dem Claude CLI zu verknüpfen
- Superpowers-Plugin von Claude Code

## Beobachtungen

- Je genauer der Prompt, desto besser das Ergebnis.
- Man kommt sehr weit. Teilweise muss man aber sehr hartnäckig mit Claude sein, bis Claude das macht, was man a) will und b) dachte, man hätte es ihm auch klar und deutlich gesagt.
- Für mich als BE-Entwickler mit minimalem FE-/Angular-Wissen war es schön, mit der AI einen Sparring-Partner zu haben, dem ich meine Vorstellungen mitteilen und den ich auch um Rat fragen konnte, welcher Style nun am besten für meine App geeignet ist.
- Nachträgliche «Refactorings» an der Architektur dauern sehr lange und benötigen viele Tokens. Deshalb bin ich der Überzeugung, dass eine sehr solide Vorarbeit von Architekt, UX- und Frontend-Designer essenziell wichtig ist. Die Eckpfeiler der Applikation wie BE-Architektur, FE und System-Architektur müssen klar in einem Dokument festgehalten sein und von jedem Mitarbeiter als Erstes von seinem Claude CLI gelesen werden. Ich gehe sogar so weit, dass das `CLAUDE.md` zu einem zentralen Bestandteil für den Architekten wird, damit ein Team von Entwicklern gleichwertigen Code erzeugen kann. Dazu gehört auch ein DoD (Definition of Done).
- Tasks/Tickets müssen mit einem Template erstellt werden und benötigen wesentlich mehr Sorgfalt, als wenn nur Menschen am Code arbeiten. Auch hier kann die AI beim Erstellen helfen.
- Neben den klassischen FA und NFA's müssen wir Architekten in Zukunft auch die Rahmenbedingen "Pflöcke" für die AI bestimmen und einschlagen, so dass die AI für alle Teammitglieder die gleichen Grundvoraussetzungen wie in diesem Projekt gearbeitet wird hat und befolgt.
- Die Überwachung, dass die AI-Richtlinien befolgt werden, gehört neu zu den Aufgaben des Architekten. Evt. kann hier ein anderes LLM zur Unterstützung verwendet werden. 

## Konkrete Beispiele: AI-Vorschläge akzeptiert, korrigiert, verworfen

Drei Situationen aus dem Projekt zeigen, wie ich mit den Vorschlägen der AI umgegangen bin – jeweils belegt mit dem zugehörigen Commit.

**Akzeptiert – strukturierter LLM-Output statt selbstgebautem Parser.**
Für die KI-Match-Analyse schlug Claude vor, die Antwort des LLM nicht als Text zu parsen, sondern über Spring AI direkt in ein typisiertes Java-Objekt zu mappen (`ChatClient … .entity(MatchAnalysisResult.class)`). Diesen Vorschlag habe ich unverändert übernommen, weil er den fragilen String-Parser komplett überflüssig macht und die Antwortstruktur erzwingt. Begründet in ADR-10. → Commit `e02ee0b` (`OpenAiLlmAdapter.java`).

**Korrigiert – NullPointer in der Statistik-Berechnung.**
Der von der AI generierte `MatchStatisticsService` iterierte über alle Punkte und griff dabei auf `pointType` zu. Die später eingeführten «Quick-Points» (schnelle Punkterfassung ohne Attribut) haben aber keinen `pointType` – das führte zu einer NullPointerException. Ich habe den Vorschlag korrigiert (Guard-Klausel: solche Punkte zählen nur zum Punktestand und überspringen die Attribution) und einen Regressionstest ergänzt. → Commit `9e35106` (+6 Zeilen Service, +17 Zeilen Test).


## Fazit

AI wird aus der Softwareentwicklung wohl nicht mehr wegzudenken sein und in kurzer Zeit so selbstverständlich genutzt werden wie heute eine IDE mit Code-Completion.

Es wird auch in naher Zukunft noch SW-Ingenieure brauchen, aber die Arbeit wird sich ändern. Anstelle des Codierens rücken mehr und mehr das Dokumentieren und das Erfassen von Issues in den Vordergrund – ob mit oder ohne AI (wohl eher mit). Das macht unseren Job etwas langweiliger.

## Manuelles Review des Codes

Ich habe mich dabei selbst ertappt (speziell beim FE-Code), dass ich auch auf das Review mehr und mehr verzichtet habe, da es einfach zu viele Änderungen waren, als dass man deren Tragweite allein mit einem Review noch erfassen könnte. Mein Fazit hier: Die Issues müssen kleiner werden, die Tests werden noch wichtiger. Sonst verlieren wir mehr und mehr den Überblick und überlassen der AI das Feld.

## Gefahren

- Unerfahrene Entwickler setzen ein Ticket dank AI vielleicht schneller um – aber auch besser? Bzw.: Wie können wir den Junior zum Senior machen? Das wird eine Herausforderung.
- Die Fragen, die ich mir stelle: Müssen Juniors in Zukunft noch Code lesen können, oder reicht es, ihnen beizubringen, was ein guter Aufbau einer Software ist, wie sie gute Tickets schreiben, was die Vor- und Nachteile einer Architektur sind und wie sie solche Entscheidungen treffen können?
- Wie bringe ich einem Junior das Verständnis bei, dass er sich um solche Fragen selbst kümmern muss und nicht alles der AI überlassen kann?
- Wir verlieren die Kontrolle über den Code, bzw. das Gefühl «Das ist mein Code» geht mehr und mehr verloren. Die manuelle Fehlersuche, wenn die AI nicht weiterkommt, kann zeitaufwändig werden, da wir uns erst wirklich in den Code einarbeiten müssen.

## Offene Fragen

- Wie weit kann die AI bei sehr spezifischen Algorithmen unterstützen, die in Bereichen zum Einsatz kommen, die weit mehr als reines Datenlesen sind? Wie weit dürfen wir AI für Code einsetzen, der strikten Regulatorien unterliegt (Medizin, Maschinen)?
- Wie gehen wir ethisch mit AI um? Überlassen wir der AI den Entscheid über Leben und Tod (selbstfahrende Autos, Drohnen)? Wie weit wollen wir uns von der AI überwachen lassen? Das hat ja schon mit ML und Big Data begonnen. Wo ziehen wir die Grenzen, wenn die AI beginnt, sich selbst zu beschäftigen? Menschen, die keine Arbeit mehr haben, kommen auf sehr, sehr dumme Ideen.
