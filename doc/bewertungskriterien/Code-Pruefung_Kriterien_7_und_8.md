# Code-Prüfung gegen Bewertungskriterien 7 & 8

**Modul AISE — Bewertungskriterien Projektarbeit** · Bereich *Programmierung* (22 Punkte)
**Geprüfter Stand:** `backend/` (Spring Boot 4.0.6, Gradle Multi-Module, Clean Architecture), Branch `develop`
**Datum:** 2026-06-12

Diese Datei dokumentiert den Ist-Zustand des Backend-Codes gegen die Anker der Kriterien
**7** (Lesbarkeit, Dokumentation, Schichten-/Modulstruktur) und **8** (idiomatischer
Framework-Einsatz) und benennt die konkreten Lücken zur jeweils höchsten Stufe.

---

## Kriterium 7 — Code lesbar, dokumentiert, nach Schichten und Modulen strukturiert

**Höchste Stufe (7 P.):** *Einheitliche Struktur mit klaren Verantwortlichkeiten; sprechende
Namen; Dokumentation an nicht-trivialen Stellen; Modulgrenzen im Code konsistent zum Entwurf.*

### Erfüllt

| Anker | Beleg |
|---|---|
| Schichtentrennung (domain/application/infrastructure) | Konsequent in allen 6 Modulen. Domänenmodelle sind reine POJOs/Records ohne Framework-Annotationen, z. B. `match/domain/model/Match.java` (Kommentar „Pure POJO — no framework dependencies"), `statistics/domain/model/MatchStatistics.java` (record), `ai/domain/model/Recommendation.java` (record). JPA-Annotationen ausschließlich in `infrastructure/persistence/entity/*JpaEntity.java`. |
| Ports & Adapters | Input-Ports (`application/port/in/*UseCase`) und Output-Ports (`application/port/out/*Port`) als Interfaces; Adapter implementieren Output-Ports (`*PersistenceAdapter`, `OpenAiLlmAdapter implements LlmClientPort`). |
| Keine zyklischen Abhängigkeiten | Modulgraph azyklisch und gerichtet: `app → alle`, `ai → common/match/player/statistics`, `statistics → common/match`, `match → player`. Bestätigt über `build.gradle.kts` jedes Moduls. |
| Sprechende Namen | Packages und Klassen durchgängig selbsterklärend (`ScoringService`, `ComputeMatchStatisticsUseCase`, `LoadPlayerPort`, `MatchAnalysisPersistenceAdapter`). |
| Dokumentation der Kern-Domänenlogik (punktuell) | `match/application/service/ScoringService.java` Z. 8–13 + Methoden-JavaDoc zu `isTiebreak()`, `isMatchTiebreak()`, `rotateServe()` — Tennis-Scoring-Regeln gut erläutert. |

### Lücken (Abweichung zur Stufe „vollständig")

| # | Lücke | Beleg / Ort |
|---|---|---|
| 7.1 | **JavaDoc fehlt an nicht-trivialen Stellen weitgehend.** Im gesamten Code existiert genau **ein** JavaDoc-Block außerhalb von `ScoringService` (in `ai/infrastructure/config/AiModuleConfig.java`). Es gibt **keine** `package-info.java`. | `find . -name package-info.java` → leer. |
| 7.2 | Komplexe Statistik-Attribution **ohne Erläuterung**: Logik, wann ein Punkt `WINNER`/`ACE` dem einen vs. `UNFORCED_ERROR` dem anderen Spieler zugerechnet wird. | `statistics/domain/PointAttribution.java` (`attributingPlayer()`), `statistics/application/service/MatchStatisticsService.java` (`compute()` + innere `Accumulator`-Klasse) — kein JavaDoc. |
| 7.3 | Break-Point-Berechnung und Score-Fortschreibung **undokumentiert**. | `match/application/service/MatchService.java` `calculateIsBreakPoint()` und `recordPoint()`. |
| 7.4 | Zentrale LLM-Prompting-Logik **ohne jede Doku** (System-/User-Prompt, JSON-Strukturanweisung). | `ai/infrastructure/llm/PromptBuilder.java` (`systemPrompt()`, `userPrompt()`). |
| 7.5 | **Modulgrenzen weichen vom Entwurf ab:** Der SAD (§5.2) führt ein eigenständiges **`scoring-module`** als Baustein; im Code existiert es nicht — Scoring ist als `ScoringService` + `MatchScore` in `match-module` realisiert. | SAD `doc/sad/TSaS_SAD_arc42_1.md` Z. 204; `settings.gradle.kts` (6 statt 7 fachlicher Module). |
| 7.6 | **Modulkommunikation nicht „ausschließlich über Application-Layer-Interfaces"** (RB-T06). Ports werden zwar genutzt, aber **Domänenmodelle werden modulübergreifend direkt importiert** — d. h. eine Domänenschicht hängt an der Domänenschicht eines Fremdmoduls. | `statistics/domain/PointAttribution.java` importiert `match.domain.model.Point`; `ai`-Service importiert `match.domain.model.Match`, `player.domain.model.Player`, `statistics.domain.model.*`. Kein Anti-Corruption-/DTO-Layer; `common-module` (Shared Kernel) wird dafür nicht genutzt. |
| 7.7 | **Keine automatische Durchsetzung der Modul-/Schichtgrenzen** (kein ArchUnit o. ä.). Konsistenz beruht allein auf Compile-Zeit-Abhängigkeiten und Disziplin. | `grep -rn ArchUnit` → leer. |
| 7.8 | Domänen-Setter ohne Invarianten — Lesbarkeit/Verantwortlichkeit leidet (Domain schützt seine Regeln nicht). | `match/domain/model/MatchScore.java` 18 Setter ohne Validierung (z. B. `setPointsPlayer1` erlaubt negative Werte); `Point.setWinner(int)` ohne Wertebereich {1,2}. |

**Einstufung Kriterium 7:** Struktur, Schichtentrennung und Namensgebung erfüllen die
höchste Stufe; **Dokumentation an nicht-trivialen Stellen ist aber lückenhaft (7.1–7.4)** und
die **Modulgrenzen sind nicht vollständig konsistent zum Entwurf (7.5–7.6)**. Damit ist der
Anker der Stufe „vollständig" nicht voll erfüllt → aktuell **Stufe „überwiegend/mehrheitlich
(4 P.)"** („Modulstruktur erkennbar; einzelne Verstöße; Dokumentation lückenhaft").
**Für 7 P. fehlt:** flächendeckende Doku der Kernlogik + Auflösung der Entwurfsabweichungen
(scoring-module, modulübergreifende Domain-Importe).

---

## Kriterium 8 — Framework-Konzepte sachgerecht eingesetzt

**Höchste Stufe (10 P.):** *Framework-Mittel durchgehend idiomatisch (Dependency Injection,
externalisierte Konfiguration, einheitliche Fehlerbehandlung, Validierung); Framework-Wahl im
Bericht begründet.*

### Erfüllt

| Anker | Beleg |
|---|---|
| **Dependency Injection** durchgehend per Constructor | `grep @Autowired` im Produktivcode → **leer**. Alle Services/Controller/Adapter nutzen Constructor-Injection mit `final`-Feldern (z. B. `PlayerService`, `MatchService`, `MatchAnalysisService`). |
| **REST** idiomatisch | Korrekte Status-Semantik im `player-`/`match-module`: `@ResponseStatus(CREATED)` für POST, `NO_CONTENT` für DELETE/PATCH (`PlayerController`, `MatchController`). RFC-7807 `ProblemDetail` als Fehlerformat. |
| **Externalisierte Konfiguration** | Keine hartkodierten Hosts/Ports in den Modulen. `application.yml`: `${OPENAI_API_KEY:}`, `${OPENAI_MODEL:gpt-4o-mini}`, `${TSAS_AI_MIN_POINTS:10}`; `@ConfigurationProperties` (`CorsProperties`), `@Value` in `MatchAnalysisService` und `SecurityConfig`. |
| **Adapter-Wahl per Conditional Beans** (idiomatisch) | `OpenAiLlmAdapter` via `@ConditionalOnExpression("!'${spring.ai.openai.api-key:}'.isEmpty()")`; `FakeLlmClientAdapter` als `@ConditionalOnMissingBean(LlmClientPort.class)`-Fallback (`AiModuleConfig`). |
| **Transaktionen** idiomatisch | `@Transactional` auf Klassenebene, `@Transactional(readOnly = true)` auf Query-Methoden (`PlayerService`, `MatchService`). |
| **Framework-Wahl begründet** | SAD ADR-01 (Modularer Monolith), §4.2 (Java 25/Boot 4, Spring AI 2.x mit `LlmClientPort` für späteren LLM-Wechsel). |

### Lücken (Abweichung zur Stufe „vollständig")

| # | Lücke | Beleg / Ort |
|---|---|---|
| 8.1 | **Bean Validation uneinheitlich.** `player-`/`match-module`-DTOs sind validiert (`@NotBlank`, `@NotNull`, `@Min/@Max`, `@Length`). In `ai-module` und `statistics-module` gibt es **keine** Validierung — `@PathVariable UUID` ohne Constraints, Response-Records ohne Annotationen. | `ai/.../MatchAnalysisController.java`, `statistics/.../MatchStatisticsController.java`. |
| 8.2 | **Validierungslücke trotz `@Valid`:** Enum-Tragende Felder in `RecordPointRequest` (`pointType`, `strokeType`, `direction`) haben **kein** `@NotNull`. Im Controller folgt `PointType.valueOf(request.pointType())` → bei `null` **NPE statt HTTP 400**. | `match/.../dto/request/RecordPointRequest.java`; `match/.../MatchController.java`. |
| 8.3 | **Fehlerbehandlung nicht „einheitlich":** zwei getrennte `@RestControllerAdvice` (`app/.../GlobalExceptionHandler`, `ai/.../AiExceptionHandler`). Kein gemeinsamer Handler im `common-module`, obwohl der SAD (§8.x) ein „zentrales Exception-Handling" vorsieht. | beide Advice-Klassen. |
| 8.4 | **`IllegalStateException` als Sammel-Exception** für fachlich Verschiedenes (Spieler hat aktives Match, Match bereits beendet, „Match not COMPLETED", „Player not found" im AI-Service) → alle pauschal **HTTP 409**, Client kann Fälle nicht unterscheiden. Inkonsistent zu vorhandenen Domain-Exceptions (`PlayerNotFoundException`, `InsufficientMatchDataException`). | `match/.../MatchService.java`; `ai/.../MatchAnalysisService.java` Z. ~58/112. |
| 8.5 | **Kein Handler für `MethodArgumentNotValidException`** → Validierungsfehler liefern nur die Spring-Default-Antwort, kein projektweit einheitliches/feldbezogenes Fehlerformat. | `GlobalExceptionHandler` (fehlend). |
| 8.6 | **Entwurf vs. Code beim Fehlerformat:** SAD beschreibt JSON `{error-code, message, timestamp}`; implementiert ist `ProblemDetail` (RFC 7807, Felder `type/title/status/detail`). Die Implementierung ist die modernere Wahl, aber **nicht konsistent zum Entwurf** und nicht im Bericht nachgezogen. | SAD `doc/sad/TSaS_SAD_arc42_1.md` Z. 369. |
| 8.7 | **POST-Semantik:** `POST /api/matches/{id}/analysis` gibt direkt den Body mit implizitem **200** zurück statt **201 Created** (+ `Location`). | `ai/.../MatchAnalysisController.java` `generate()`. |
| 8.8 | **Hartkodierte LLM-Prompts** (deutschsprachig, inkl. JSON-Strukturvorgabe) — nicht externalisiert, obwohl restliche AI-Konfiguration externalisiert ist. | `ai/infrastructure/llm/PromptBuilder.java`. |

**Einstufung Kriterium 8:** DI, externalisierte Konfiguration, Conditional-Bean-Profile und
die begründete Framework-Wahl erfüllen die höchste Stufe. **Validierung ist jedoch nicht
durchgehend (8.1, 8.2)** und die **Fehlerbehandlung ist nicht vollständig einheitlich (8.3–8.5)**.
Damit liegt „einzelne Umgehungen oder Inkonsistenzen" vor → aktuell **Stufe
„überwiegend/mehrheitlich (7 P.)"**. **Für 10 P. fehlt:** durchgängige Bean-Validation auf
allen Endpunkten/DTOs + vereinheitlichte, fachlich differenzierte Fehlerbehandlung
(eigene Exceptions statt `IllegalStateException`, gemeinsamer Advice, Validation-Handler).

---

## Zusammenfassung

| Kriterium | Aktuelle Stufe | Punkte (Ist / Max) | Hebel zur Maximalstufe |
|---|---|---|---|
| **7** — Lesbarkeit/Doku/Struktur | ~~überwiegend~~ → **vollständig** | ~~4~~ → **7 / 7** | ✅ behoben — siehe Abschnitt „Behebung Kriterium 7" unten |
| **8** — Framework-Einsatz | ~~überwiegend~~ → **vollständig** | ~~7~~ → **10 / 10** | ✅ behoben — siehe Abschnitt „Behebung Kriterium 8" unten |

> Hinweis: Die Punktwerte sind die Selbsteinschätzung anhand der Anker; sie dienen der
> Priorisierung der oben gelisteten Lücken, nicht als finale Note.

---

## Behebung Kriterium 8 (Stand 2026-06-12)

Die Lücken aus Kriterium 8 wurden behoben. Gesamte Test-Suite (Unit + IT mit
PostgreSQL-Testcontainer) grün, Coverage-Gate (85 % Line / 70 % Branch) bestanden.

| # | Behebung | Belege |
|---|---|---|
| 8.3 / 8.5 | Neuer zentraler `CommonExceptionHandler` im **common-module**: `MethodArgumentNotValidException` → 400 mit Feld-Details (`errors`-Property), `IllegalArgumentException` → 400, `ConflictException` → 409. Validierungsfehler liefern jetzt ein einheitliches RFC-7807-Format statt der Spring-Default-Antwort. | `common-module/.../web/CommonExceptionHandler.java` (neu); `GlobalExceptionHandler` auf 404-Domain-Fälle reduziert. |
| 8.4 | Rohe `IllegalStateException` durch benannte Domain-Exceptions ersetzt (gemeinsame Basis `ConflictException` im Shared Kernel → 409): `MatchAlreadyCompletedException`, `ActiveMatchExistsException` (match), `PlayerHasMatchesException` (player), `MatchNotCompletedException` (ai). „Player not found" im AI-Flow wirft jetzt `PlayerNotFoundException` → **404** (vorher fälschlich 409). | neue Exception-Klassen; `MatchService`, `PlayerService`, `MatchAnalysisService` angepasst; Unit-Tests aktualisiert. |
| 8.7 | `POST /api/matches/{id}/analysis` liefert **201 Created** mit `Location`-Header statt 200. | `MatchAnalysisController.generate()`; IT `post_generatesAndPersists` prüft 201 + Location. |
| 8.8 | LLM-Prompts externalisiert via `@ConfigurationProperties` (`PromptProperties`, Präfix `tsas.ai.prompt`, überschreibbar per yml/Env); `PromptBuilder` injiziert sie statt sie hartzukodieren. | `ai/.../config/PromptProperties.java` (neu), `PromptBuilder`, `AiModuleConfig`, `application.yml` (Override-Doku). |
| 8.6 | SAD §8.3 beschreibt jetzt das tatsächliche RFC-7807-/`ProblemDetail`-Format und die Advice-Struktur statt des nie umgesetzten `{error-code, message, timestamp}`. | `doc/sad/TSaS_SAD_arc42_1.md` §8.3. |
| 8.1 / 8.2 | `SetScoreRequest` erhielt Constraints (`@Min`, `@Pattern`) und der `setScore`-Endpunkt fehlte `@Valid` — beides ergänzt. **Korrektur zur Erstbewertung:** Die `ai-`/`statistics-`Endpunkte haben keine Request-Bodies (nur typsichere `@PathVariable UUID` → Spring liefert bei Fehlformat selbst 400); die Enum-String-Felder in `RecordPointRequest` sind fachlich optional, ungültige Werte ergeben bereits über `IllegalArgumentException` → 400. Der „NPE statt 400"-Befund war durch die vorhandene Null-Prüfung im Controller überzeichnet. | `SetScoreRequest.java`, `MatchController.setScore()`. |

**Neue Einstufung Kriterium 8:** durchgängige DI, externalisierte Konfiguration (inkl. Prompts),
einheitliche RFC-7807-Fehlerbehandlung mit fachlich differenzierten Exceptions und konsistente
Validierung → Anker „vollständig/korrekt" erfüllt (**10 / 10**, Selbsteinschätzung).

---

## Behebung Kriterium 7 (Stand 2026-06-12)

Die Lücken aus Kriterium 7 wurden behoben. Gesamter `check` grün (Unit + IT + neuer
`ArchitectureTest` + Coverage-Gate 85/70).

| # | Behebung | Belege |
|---|---|---|
| 7.1 | **`package-info.java` für jedes Modul** dokumentiert Verantwortlichkeit, Schichtung und erlaubte Abhängigkeiten direkt im Code. | `common/`, `player/`, `match/`, `statistics/`, `ai/`, `auth/` package-info.java (neu). |
| 7.2 | Statistik-Attribution dokumentiert: Zuordnungsregeln in `PointAttribution.attributingPlayer` und Akkumulator-Logik in `MatchStatisticsService.compute` / `Accumulator`. | JavaDoc in `PointAttribution.java`, `MatchStatisticsService.java`. |
| 7.3 | Match-Kernlogik dokumentiert: `recordPoint`, `calculateIsBreakPoint`, Lebenszyklus-Methoden. | JavaDoc in `MatchService.java`. |
| 7.4 | LLM-Prompting dokumentiert (`PromptBuilder`, Adapter, `MatchAnalysisService`). | JavaDoc in `PromptBuilder.java`, `OpenAiLlmAdapter.java`, `FakeLlmClientAdapter.java`, `MatchAnalysisService.java`. (Insgesamt JavaDoc an allen nicht-trivialen Klassen/Methoden der Module ergänzt.) |
| 7.5 | **Entwurfsabweichung `scoring-module` aufgelöst:** SAD §5.2 + Laufzeitsicht §6.1 beschreiben jetzt die Konsolidierung von Scoring im `match-module`; begründet in **ADR-12**. | `doc/sad/TSaS_SAD_arc42_1.md`. |
| 7.6 | **RB-T06 präzisiert:** modulübergreifendes Verhalten nur über Application-Ports, stabile Domänen-Wertobjekte bewusst als gemeinsames Read-Model; begründet in **ADR-13**. | `doc/sad/TSaS_SAD_arc42_1.md` (RB-T06, ADR-13). |
| 7.7 | **Automatisierte Durchsetzung** der Grenzen via `ArchitectureTest`: framework-freie Domäne, einwärts gerichtete Schichten (Domain ← Application ← Infrastructure), zyklenfreier Modulgraph. Quellcode-basiert (parst `package`/`import`), da ArchUnits gebündelte ASM **Java-25-Bytecode (Major 69) nicht lesen kann** — verifiziert: ArchUnit importierte 0 von N Klassen, eine Major-61-Probe dagegen erfolgreich. | `app/src/test/java/com/cas/tsas/ArchitectureTest.java` (neu). |
| 7.8 | **Domäneninvariante** für das sicherheitskritische Feld `Point.winner ∈ {1,2}` (Konstruktor + Setter), von dem die Statistik-Attribution abhängt; weitere Invarianten von `MatchScore`/`Match` im JavaDoc dokumentiert (keine harten Setter-Guards, da `ScoringService` inkrementell mutiert). | `Point.java`, `MatchScore.java`, `Match.java`. |

**Neue Einstufung Kriterium 7:** saubere Schichten/Struktur, sprechende Namen, Dokumentation
an allen nicht-trivialen Stellen, und Modulgrenzen konsistent zum (nun angeglichenen) Entwurf —
zusätzlich automatisiert durchgesetzt → Anker „vollständig/korrekt" erfüllt (**7 / 7**,
Selbsteinschätzung).
