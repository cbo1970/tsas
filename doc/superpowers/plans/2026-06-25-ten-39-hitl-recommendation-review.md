# HITL Recommendation Review (TEN-39) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Der Coach kann jede Empfehlung einer persistierten FA-11-Match-Analyse annehmen oder verwerfen (3 Zustände + optionale Begründung); der Zustand wird persistiert und im UI angezeigt.

**Architecture:** Ansatz A — der Review-Zustand wird in die eingebettete `Recommendation` aufgenommen und in der bestehenden `match_analysis.recommendations`-JSON-Spalte mitserialisiert (kein Schema-Change). Ein neuer `PATCH /api/matches/{matchId}/analysis/recommendations/{index}` setzt Status + Notiz im `MatchAnalysisService`; das Frontend rendert Annehmen/Verwerfen pro Empfehlung in der bestehenden `match-analysis.component`.

**Tech Stack:** Java 25 / Spring Boot 4 (Gradle Multi-Module, Clean Architecture), Jackson 3 (`tools.jackson`), JUnit 5 + Mockito + AssertJ, Testcontainers (PostgreSQL), Angular (Signals, Material), Cypress Component Testing, ngx-translate.

## Global Constraints

- Java 25; alle Gradle-Befehle mit `JAVA_HOME=/opt/java/jdk-25.0.1` prefixen.
- Tests/IT brauchen ein Container-Runtime: `DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true`.
- Clean Architecture: Domain framework-frei; Modul-Kommunikation nur über Application-Layer-Ports; einwärts gerichtete Abhängigkeiten (`ArchitectureTest` erzwingt das).
- Fehlerantworten als RFC-7807 `ProblemDetail`. 409 = Subklasse von `com.cas.tsas.common.exception.ConflictException` (zentral gemappt); 404 via `@ExceptionHandler` im `AiExceptionHandler`; 400 via Bean-Validation.
- Deutsche Doku in Schweizer Rechtschreibung (`ss`, kein `ß`).
- Owner-Check/IDOR: fremde/unbekannte IDs → **404** (nicht 403).
- Coverage-Gate (in `check`): ≥ 85 % Line / 70 % Branch.

---

### Task 1: Domänenmodell — `RecommendationStatus` + erweitertes `Recommendation`

**Files:**
- Create: `backend/ai-module/src/main/java/com/cas/tsas/ai/domain/model/RecommendationStatus.java`
- Modify: `backend/ai-module/src/main/java/com/cas/tsas/ai/domain/model/Recommendation.java`
- Test: `backend/ai-module/src/test/java/com/cas/tsas/ai/domain/model/RecommendationTest.java`

**Interfaces:**
- Produces: `enum RecommendationStatus { OPEN, ACCEPTED, REJECTED }`; `record Recommendation(int priority, String title, String detail, RecommendationStatus status, String reviewNote, Instant reviewedAt)` mit Zusatz-Konstruktor `Recommendation(int, String, String)` (defaultet OPEN/null/null) und `Recommendation withReview(RecommendationStatus, String, Instant)`.

- [ ] **Step 1: Write the failing test**

`backend/ai-module/src/test/java/com/cas/tsas/ai/domain/model/RecommendationTest.java`:
```java
package com.cas.tsas.ai.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationTest {

    @Test
    void threeArgConstructor_defaultsToOpenWithoutReview() {
        Recommendation r = new Recommendation(1, "t", "d");
        assertThat(r.status()).isEqualTo(RecommendationStatus.OPEN);
        assertThat(r.reviewNote()).isNull();
        assertThat(r.reviewedAt()).isNull();
    }

    @Test
    void nullStatus_isNormalizedToOpen() {
        Recommendation r = new Recommendation(1, "t", "d", null, null, null);
        assertThat(r.status()).isEqualTo(RecommendationStatus.OPEN);
    }

    @Test
    void withReview_returnsUpdatedCopyKeepingContent() {
        Instant at = Instant.parse("2026-06-25T10:00:00Z");
        Recommendation r = new Recommendation(2, "Serve wide", "Detail")
                .withReview(RecommendationStatus.REJECTED, "nicht passend", at);
        assertThat(r.priority()).isEqualTo(2);
        assertThat(r.title()).isEqualTo("Serve wide");
        assertThat(r.detail()).isEqualTo("Detail");
        assertThat(r.status()).isEqualTo(RecommendationStatus.REJECTED);
        assertThat(r.reviewNote()).isEqualTo("nicht passend");
        assertThat(r.reviewedAt()).isEqualTo(at);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :ai-module:test --tests "com.cas.tsas.ai.domain.model.RecommendationTest"`
Expected: FAIL — compile error (`RecommendationStatus` fehlt, `withReview` fehlt, 6-arg-Konstruktor fehlt).

- [ ] **Step 3: Create the enum**

`backend/ai-module/src/main/java/com/cas/tsas/ai/domain/model/RecommendationStatus.java`:
```java
package com.cas.tsas.ai.domain.model;

/** Review-Status einer KI-Empfehlung im Human-in-the-Loop (TEN-39). */
public enum RecommendationStatus { OPEN, ACCEPTED, REJECTED }
```

- [ ] **Step 4: Extend the `Recommendation` record**

Ersetze den kompletten Inhalt von `backend/ai-module/src/main/java/com/cas/tsas/ai/domain/model/Recommendation.java`:
```java
package com.cas.tsas.ai.domain.model;

import java.time.Instant;

/**
 * Eine einzelne taktische KI-Empfehlung samt Human-in-the-Loop-Review-Zustand (TEN-39).
 * Generierte Empfehlungen starten in {@link RecommendationStatus#OPEN}; der Coach kann sie
 * annehmen oder verwerfen (mit optionaler Begründung).
 */
public record Recommendation(
        int priority,
        String title,
        String detail,
        RecommendationStatus status,
        String reviewNote,
        Instant reviewedAt
) {

    /** Defaultet einen fehlenden Status (z. B. aus Alt-JSON deserialisiert) auf OPEN. */
    public Recommendation {
        status = status == null ? RecommendationStatus.OPEN : status;
    }

    /** Generierungs-Konstruktor: neue Empfehlung ohne Review. */
    public Recommendation(int priority, String title, String detail) {
        this(priority, title, detail, RecommendationStatus.OPEN, null, null);
    }

    /** Liefert eine Kopie mit aktualisiertem Review-Zustand (immutable). */
    public Recommendation withReview(RecommendationStatus newStatus, String note, Instant at) {
        return new Recommendation(priority, title, detail, newStatus, note, at);
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :ai-module:test --tests "com.cas.tsas.ai.domain.model.RecommendationTest"`
Expected: PASS (3 Tests grün). Die bestehenden `new Recommendation(p,t,d)`-Aufrufstellen (LLM-Adapter, Fakes) kompilieren weiter über den Zusatz-Konstruktor.

- [ ] **Step 6: Commit**

```bash
git add backend/ai-module/src/main/java/com/cas/tsas/ai/domain/model/RecommendationStatus.java \
        backend/ai-module/src/main/java/com/cas/tsas/ai/domain/model/Recommendation.java \
        backend/ai-module/src/test/java/com/cas/tsas/ai/domain/model/RecommendationTest.java
git commit -m "feat(ai): add review state to Recommendation (TEN-39)"
```

---

### Task 2: Persistenz-Round-Trip inkl. Abwärtskompatibilität (Alt-JSON)

**Files:**
- Test: `backend/ai-module/src/test/java/com/cas/tsas/ai/infrastructure/persistence/RecommendationJsonRoundTripTest.java`

**Interfaces:**
- Consumes: `Recommendation`, `RecommendationStatus` (Task 1).
- Produces: nichts (reiner Charakterisierungs-/Schutztest, der belegt, dass die neuen Felder in der bestehenden JSON-Spalte mitserialisieren und Alt-JSON ohne die Felder auf `OPEN` deserialisiert).

- [ ] **Step 1: Write the test**

`backend/ai-module/src/test/java/com/cas/tsas/ai/infrastructure/persistence/RecommendationJsonRoundTripTest.java`:
```java
package com.cas.tsas.ai.infrastructure.persistence;

import com.cas.tsas.ai.domain.model.Recommendation;
import com.cas.tsas.ai.domain.model.RecommendationStatus;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Schützt die JSON-Persistenz der Empfehlungen (Spalte match_analysis.recommendations):
 * neue Review-Felder serialisieren mit, und Alt-JSON ohne die Felder bleibt lesbar.
 */
class RecommendationJsonRoundTripTest {

    private static final TypeReference<List<Recommendation>> LIST = new TypeReference<>() {};
    private final ObjectMapper mapper = JsonMapper.builder().build();

    @Test
    void roundTrip_preservesReviewState() {
        Recommendation r = new Recommendation(1, "Serve wide", "Detail")
                .withReview(RecommendationStatus.REJECTED, "zu riskant",
                        Instant.parse("2026-06-25T10:00:00Z"));

        String json = mapper.writeValueAsString(List.of(r));
        List<Recommendation> back = mapper.readValue(json, LIST);

        assertThat(back).hasSize(1);
        assertThat(back.get(0)).isEqualTo(r);
    }

    @Test
    void legacyJsonWithoutReviewFields_deserializesToOpen() {
        String legacy = "[{\"priority\":1,\"title\":\"t\",\"detail\":\"d\"}]";

        List<Recommendation> back = mapper.readValue(legacy, LIST);

        assertThat(back).hasSize(1);
        assertThat(back.get(0).status()).isEqualTo(RecommendationStatus.OPEN);
        assertThat(back.get(0).reviewNote()).isNull();
        assertThat(back.get(0).reviewedAt()).isNull();
    }
}
```

- [ ] **Step 2: Run test**

Run: `JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :ai-module:test --tests "com.cas.tsas.ai.infrastructure.persistence.RecommendationJsonRoundTripTest"`
Expected: PASS. Jackson 3 toleriert fehlende Record-Komponenten (→ `null`), und der Compact-Constructor normalisiert `null` → `OPEN`.

> Falls `legacyJsonWithoutReviewFields_*` wider Erwarten fehlschlägt (Mapper strikt konfiguriert), ist die Persistenz robust zu machen, indem der `MatchAnalysisPersistenceAdapter`-Mapper beim Lesen unbekannte/fehlende Properties ignoriert. Bei PASS ist keine Produktivänderung nötig.

- [ ] **Step 3: Commit**

```bash
git add backend/ai-module/src/test/java/com/cas/tsas/ai/infrastructure/persistence/RecommendationJsonRoundTripTest.java
git commit -m "test(ai): guard recommendation JSON round-trip incl. legacy (TEN-39)"
```

---

### Task 3: Application-Layer — Exceptions, Use-Case-Port, Service-Methode

**Files:**
- Create: `backend/ai-module/src/main/java/com/cas/tsas/ai/domain/exception/AnalysisNotReviewableException.java`
- Create: `backend/ai-module/src/main/java/com/cas/tsas/ai/domain/exception/MatchAnalysisNotFoundException.java`
- Create: `backend/ai-module/src/main/java/com/cas/tsas/ai/domain/exception/RecommendationNotFoundException.java`
- Create: `backend/ai-module/src/main/java/com/cas/tsas/ai/application/port/in/ReviewRecommendationUseCase.java`
- Modify: `backend/ai-module/src/main/java/com/cas/tsas/ai/application/service/MatchAnalysisService.java`
- Test: `backend/ai-module/src/test/java/com/cas/tsas/ai/application/service/ReviewRecommendationServiceTest.java`

**Interfaces:**
- Consumes: `MatchAnalysis`, `Recommendation`, `RecommendationStatus`, `AnalysisStatus` (Task 1 + bestehend); `GetMatchUseCase.findById(UUID)` (wirft `MatchNotFoundException` bei unbekannt/fremd); `LoadMatchAnalysisPort.loadByMatchId(UUID): Optional<MatchAnalysis>`; `SaveMatchAnalysisPort.save(MatchAnalysis): MatchAnalysis`.
- Produces: `ReviewRecommendationUseCase.review(UUID matchId, int recommendationIndex, RecommendationStatus status, String note): MatchAnalysis`, implementiert von `MatchAnalysisService`.

- [ ] **Step 1: Write the failing test**

`backend/ai-module/src/test/java/com/cas/tsas/ai/application/service/ReviewRecommendationServiceTest.java`:
```java
package com.cas.tsas.ai.application.service;

import com.cas.tsas.ai.application.port.out.LlmClientPort;
import com.cas.tsas.ai.application.port.out.LoadMatchAnalysisPort;
import com.cas.tsas.ai.application.port.out.SaveMatchAnalysisPort;
import com.cas.tsas.ai.application.port.out.UserLanguagePort;
import com.cas.tsas.ai.domain.exception.AnalysisNotReviewableException;
import com.cas.tsas.ai.domain.exception.MatchAnalysisNotFoundException;
import com.cas.tsas.ai.domain.exception.RecommendationNotFoundException;
import com.cas.tsas.ai.domain.model.AnalysisStatus;
import com.cas.tsas.ai.domain.model.MatchAnalysis;
import com.cas.tsas.ai.domain.model.Recommendation;
import com.cas.tsas.ai.domain.model.RecommendationStatus;
import com.cas.tsas.match.application.port.in.GetMatchUseCase;
import com.cas.tsas.match.domain.exception.MatchNotFoundException;
import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.statistics.application.port.in.ComputeMatchStatisticsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewRecommendationServiceTest {

    @Mock GetMatchUseCase getMatchUseCase;
    @Mock LoadPlayerPort loadPlayerPort;
    @Mock ComputeMatchStatisticsUseCase statisticsUseCase;
    @Mock LlmClientPort llmClient;
    @Mock SaveMatchAnalysisPort savePort;
    @Mock LoadMatchAnalysisPort loadPort;
    @Mock UserLanguagePort userLanguagePort;

    private MatchAnalysisService service;
    private final UUID matchId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new MatchAnalysisService(getMatchUseCase, loadPlayerPort, statisticsUseCase,
                llmClient, savePort, loadPort, userLanguagePort, 10);
    }

    private MatchAnalysis completedWith(Recommendation... recs) {
        MatchAnalysis a = new MatchAnalysis();
        a.setMatchId(matchId);
        a.setStatus(AnalysisStatus.COMPLETED);
        a.setRecommendations(new ArrayList<>(List.of(recs)));
        return a;
    }

    @Test
    void review_setsStatusNoteAndTimestamp_andPersists() {
        when(getMatchUseCase.findById(matchId)).thenReturn(org.mockito.Mockito.mock(Match.class));
        when(loadPort.loadByMatchId(matchId))
                .thenReturn(Optional.of(completedWith(new Recommendation(1, "t", "d"))));
        when(savePort.save(any(MatchAnalysis.class))).thenAnswer(inv -> inv.getArgument(0));

        MatchAnalysis result = service.review(matchId, 0, RecommendationStatus.ACCEPTED, "gut");

        Recommendation r = result.getRecommendations().get(0);
        assertThat(r.status()).isEqualTo(RecommendationStatus.ACCEPTED);
        assertThat(r.reviewNote()).isEqualTo("gut");
        assertThat(r.reviewedAt()).isNotNull();
    }

    @Test
    void review_propagatesCrossTenantNotFound() {
        when(getMatchUseCase.findById(matchId)).thenThrow(new MatchNotFoundException(matchId));
        assertThatThrownBy(() -> service.review(matchId, 0, RecommendationStatus.ACCEPTED, null))
                .isInstanceOf(MatchNotFoundException.class);
    }

    @Test
    void review_throwsWhenNoAnalysis() {
        when(getMatchUseCase.findById(matchId)).thenReturn(org.mockito.Mockito.mock(Match.class));
        when(loadPort.loadByMatchId(matchId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.review(matchId, 0, RecommendationStatus.ACCEPTED, null))
                .isInstanceOf(MatchAnalysisNotFoundException.class);
    }

    @Test
    void review_throwsWhenNotCompleted() {
        when(getMatchUseCase.findById(matchId)).thenReturn(org.mockito.Mockito.mock(Match.class));
        MatchAnalysis pending = new MatchAnalysis();
        pending.setMatchId(matchId);
        pending.setStatus(AnalysisStatus.FAILED);
        pending.setRecommendations(new ArrayList<>());
        when(loadPort.loadByMatchId(matchId)).thenReturn(Optional.of(pending));
        assertThatThrownBy(() -> service.review(matchId, 0, RecommendationStatus.ACCEPTED, null))
                .isInstanceOf(AnalysisNotReviewableException.class);
    }

    @Test
    void review_throwsWhenIndexOutOfRange() {
        when(getMatchUseCase.findById(matchId)).thenReturn(org.mockito.Mockito.mock(Match.class));
        when(loadPort.loadByMatchId(matchId))
                .thenReturn(Optional.of(completedWith(new Recommendation(1, "t", "d"))));
        assertThatThrownBy(() -> service.review(matchId, 5, RecommendationStatus.ACCEPTED, null))
                .isInstanceOf(RecommendationNotFoundException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :ai-module:test --tests "com.cas.tsas.ai.application.service.ReviewRecommendationServiceTest"`
Expected: FAIL — Compile-Fehler (`review`, Exceptions fehlen).

- [ ] **Step 3: Create the three exceptions**

`backend/ai-module/src/main/java/com/cas/tsas/ai/domain/exception/AnalysisNotReviewableException.java`:
```java
package com.cas.tsas.ai.domain.exception;

import com.cas.tsas.common.exception.ConflictException;

import java.util.UUID;

/** Geworfen, wenn eine nicht-COMPLETED-Analyse reviewt werden soll. Bildet auf HTTP 409 ab. */
public class AnalysisNotReviewableException extends ConflictException {
    public AnalysisNotReviewableException(UUID matchId) {
        super("Analysis for match " + matchId + " is not COMPLETED and cannot be reviewed");
    }
}
```

`backend/ai-module/src/main/java/com/cas/tsas/ai/domain/exception/MatchAnalysisNotFoundException.java`:
```java
package com.cas.tsas.ai.domain.exception;

import java.util.UUID;

/** Geworfen, wenn für ein Match keine Analyse existiert. Bildet auf HTTP 404 ab. */
public class MatchAnalysisNotFoundException extends RuntimeException {
    public MatchAnalysisNotFoundException(UUID matchId) {
        super("No analysis for match " + matchId);
    }
}
```

`backend/ai-module/src/main/java/com/cas/tsas/ai/domain/exception/RecommendationNotFoundException.java`:
```java
package com.cas.tsas.ai.domain.exception;

import java.util.UUID;

/** Geworfen, wenn der Empfehlungs-Index ausserhalb der Liste liegt. Bildet auf HTTP 404 ab. */
public class RecommendationNotFoundException extends RuntimeException {
    public RecommendationNotFoundException(UUID matchId, int index) {
        super("No recommendation at index " + index + " for match " + matchId);
    }
}
```

- [ ] **Step 4: Create the input port**

`backend/ai-module/src/main/java/com/cas/tsas/ai/application/port/in/ReviewRecommendationUseCase.java`:
```java
package com.cas.tsas.ai.application.port.in;

import com.cas.tsas.ai.domain.model.MatchAnalysis;
import com.cas.tsas.ai.domain.model.RecommendationStatus;

import java.util.UUID;

/** Human-in-the-Loop: Coach reviewt eine einzelne KI-Empfehlung (TEN-39). */
public interface ReviewRecommendationUseCase {
    MatchAnalysis review(UUID matchId, int recommendationIndex, RecommendationStatus status, String note);
}
```

- [ ] **Step 5: Implement the service method**

In `backend/ai-module/src/main/java/com/cas/tsas/ai/application/service/MatchAnalysisService.java`:

(a) Imports ergänzen (zu den bestehenden hinzufügen):
```java
import com.cas.tsas.ai.application.port.in.ReviewRecommendationUseCase;
import com.cas.tsas.ai.domain.exception.AnalysisNotReviewableException;
import com.cas.tsas.ai.domain.exception.MatchAnalysisNotFoundException;
import com.cas.tsas.ai.domain.exception.RecommendationNotFoundException;
import com.cas.tsas.ai.domain.model.Recommendation;
import com.cas.tsas.ai.domain.model.RecommendationStatus;
import java.util.ArrayList;
import java.util.List;
```

(b) Klassendeklaration um das Interface erweitern:
```java
public class MatchAnalysisService implements GenerateMatchAnalysisUseCase, GetMatchAnalysisUseCase,
        ReviewRecommendationUseCase {
```

(c) Methode hinzufügen (nach `findByMatchId`):
```java
    @Override
    public MatchAnalysis review(UUID matchId, int recommendationIndex,
                                RecommendationStatus status, String note) {
        // Owner-/Existenz-Check: wirft MatchNotFoundException (→404) bei unbekannt/fremd.
        getMatchUseCase.findById(matchId);

        MatchAnalysis analysis = loadPort.loadByMatchId(matchId)
                .orElseThrow(() -> new MatchAnalysisNotFoundException(matchId));

        if (analysis.getStatus() != AnalysisStatus.COMPLETED) {
            throw new AnalysisNotReviewableException(matchId);
        }

        List<Recommendation> recs = analysis.getRecommendations();
        if (recs == null || recommendationIndex < 0 || recommendationIndex >= recs.size()) {
            throw new RecommendationNotFoundException(matchId, recommendationIndex);
        }

        List<Recommendation> updated = new ArrayList<>(recs);
        updated.set(recommendationIndex,
                recs.get(recommendationIndex).withReview(status, note, Instant.now()));
        analysis.setRecommendations(updated);

        return savePort.save(analysis);
    }
```

- [ ] **Step 6: Run test to verify it passes**

Run: `JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :ai-module:test --tests "com.cas.tsas.ai.application.service.ReviewRecommendationServiceTest"`
Expected: PASS (5 Tests grün).

- [ ] **Step 7: Commit**

```bash
git add backend/ai-module/src/main/java/com/cas/tsas/ai/domain/exception/AnalysisNotReviewableException.java \
        backend/ai-module/src/main/java/com/cas/tsas/ai/domain/exception/MatchAnalysisNotFoundException.java \
        backend/ai-module/src/main/java/com/cas/tsas/ai/domain/exception/RecommendationNotFoundException.java \
        backend/ai-module/src/main/java/com/cas/tsas/ai/application/port/in/ReviewRecommendationUseCase.java \
        backend/ai-module/src/main/java/com/cas/tsas/ai/application/service/MatchAnalysisService.java \
        backend/ai-module/src/test/java/com/cas/tsas/ai/application/service/ReviewRecommendationServiceTest.java
git commit -m "feat(ai): review recommendation use case in MatchAnalysisService (TEN-39)"
```

---

### Task 4: Web-Layer — Request-DTO, Response-Erweiterung, PATCH-Endpunkt + 404-Mapping + IT

**Files:**
- Create: `backend/ai-module/src/main/java/com/cas/tsas/ai/infrastructure/web/dto/ReviewRecommendationRequest.java`
- Modify: `backend/ai-module/src/main/java/com/cas/tsas/ai/infrastructure/web/dto/RecommendationResponse.java`
- Modify: `backend/ai-module/src/main/java/com/cas/tsas/ai/infrastructure/web/dto/MatchAnalysisResponse.java`
- Modify: `backend/ai-module/src/main/java/com/cas/tsas/ai/infrastructure/web/MatchAnalysisController.java`
- Modify: `backend/ai-module/src/main/java/com/cas/tsas/ai/infrastructure/web/AiExceptionHandler.java`
- Test: `backend/app/src/test/java/com/cas/tsas/ai/MatchAnalysisControllerIT.java` (ergänzen)

**Interfaces:**
- Consumes: `ReviewRecommendationUseCase.review(...)` (Task 3); `RecommendationStatus` (Task 1).
- Produces: `PATCH /api/matches/{matchId}/analysis/recommendations/{index}` mit Body `ReviewRecommendationRequest(status, note)`, Antwort `MatchAnalysisResponse` (200). `RecommendationResponse` trägt jetzt `status`, `reviewNote`, `reviewedAt`.

- [ ] **Step 1: Write the failing IT**

In `backend/app/src/test/java/com/cas/tsas/ai/MatchAnalysisControllerIT.java` — Imports ergänzen:
```java
import com.cas.tsas.auth.domain.Role;
import com.cas.tsas.auth.testsupport.JwtTestSupport;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
```
und folgende Testmethoden in die Klasse einfügen:
```java
    @Test
    void patch_reviewsRecommendation_thenGetReflectsStatusAndNote() throws Exception {
        UUID matchId = createMatch(MatchStatus.COMPLETED, 15);
        mockMvc.perform(post("/api/matches/{id}/analysis", matchId)).andExpect(status().isCreated());

        mockMvc.perform(patch("/api/matches/{id}/analysis/recommendations/{i}", matchId, 0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\",\"note\":\"passt\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[0].status").value("ACCEPTED"))
                .andExpect(jsonPath("$.recommendations[0].reviewNote").value("passt"))
                .andExpect(jsonPath("$.recommendations[0].reviewedAt").exists());

        mockMvc.perform(get("/api/matches/{id}/analysis", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[0].status").value("ACCEPTED"));
    }

    @Test
    void patch_returns400ForInvalidStatus() throws Exception {
        UUID matchId = createMatch(MatchStatus.COMPLETED, 15);
        mockMvc.perform(post("/api/matches/{id}/analysis", matchId)).andExpect(status().isCreated());

        mockMvc.perform(patch("/api/matches/{id}/analysis/recommendations/{i}", matchId, 0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"MAYBE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patch_returns400ForTooLongNote() throws Exception {
        UUID matchId = createMatch(MatchStatus.COMPLETED, 15);
        mockMvc.perform(post("/api/matches/{id}/analysis", matchId)).andExpect(status().isCreated());

        String longNote = "x".repeat(501);
        mockMvc.perform(patch("/api/matches/{id}/analysis/recommendations/{i}", matchId, 0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\",\"note\":\"" + longNote + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patch_returns404ForIndexOutOfRange() throws Exception {
        UUID matchId = createMatch(MatchStatus.COMPLETED, 15);
        mockMvc.perform(post("/api/matches/{id}/analysis", matchId)).andExpect(status().isCreated());

        mockMvc.perform(patch("/api/matches/{id}/analysis/recommendations/{i}", matchId, 99)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void patch_returns404WhenNoAnalysisExists() throws Exception {
        UUID matchId = createMatch(MatchStatus.COMPLETED, 15);
        // Kein POST /analysis -> es existiert keine Analyse -> 404.
        // (Der 409-Pfad "Analyse nicht COMPLETED" ist im Service-Unit-Test (Task 3) abgedeckt,
        //  da ein persistierter nicht-COMPLETED-Datensatz über die API nicht trivial erzeugbar ist.)
        mockMvc.perform(patch("/api/matches/{id}/analysis/recommendations/{i}", matchId, 0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void patch_returns404ForCrossTenantMatch() throws Exception {
        UUID matchId = createMatch(MatchStatus.COMPLETED, 15);
        mockMvc.perform(post("/api/matches/{id}/analysis", matchId)).andExpect(status().isCreated());

        UUID otherUser = UUID.randomUUID();
        mockMvc.perform(patch("/api/matches/{id}/analysis/recommendations/{i}", matchId, 0)
                        .with(JwtTestSupport.withUser(otherUser, Role.COACH))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isNotFound());
    }
```

> Hinweis: Ein echtes 409 (`AnalysisNotReviewableException`) tritt nur bei einer persistierten nicht-COMPLETED-Analyse auf — dieser Pfad ist im Service-Unit-Test (Task 3) abgedeckt. Das IT prüft hier die end-to-end-Statuscodes 200/400/404 inkl. cross-tenant.

- [ ] **Step 2: Run IT to verify it fails**

Run: `JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew :app:test --tests "com.cas.tsas.ai.MatchAnalysisControllerIT"`
Expected: FAIL — Compile-Fehler (PATCH-Route/DTO fehlen) bzw. 404 auf dem PATCH-Pfad.

- [ ] **Step 3: Create the request DTO**

`backend/ai-module/src/main/java/com/cas/tsas/ai/infrastructure/web/dto/ReviewRecommendationRequest.java`:
```java
package com.cas.tsas.ai.infrastructure.web.dto;

import com.cas.tsas.ai.domain.model.RecommendationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request-Body für das Review einer Empfehlung (TEN-39). */
public record ReviewRecommendationRequest(
        @NotNull RecommendationStatus status,
        @Size(max = 500) String note
) {}
```

- [ ] **Step 4: Extend `RecommendationResponse`**

Ersetze den Inhalt von `backend/ai-module/src/main/java/com/cas/tsas/ai/infrastructure/web/dto/RecommendationResponse.java`:
```java
package com.cas.tsas.ai.infrastructure.web.dto;

import com.cas.tsas.ai.domain.model.Recommendation;
import com.cas.tsas.ai.domain.model.RecommendationStatus;

import java.time.Instant;

public record RecommendationResponse(
        int priority,
        String title,
        String detail,
        RecommendationStatus status,
        String reviewNote,
        Instant reviewedAt
) {
    public static RecommendationResponse from(Recommendation r) {
        return new RecommendationResponse(
                r.priority(), r.title(), r.detail(), r.status(), r.reviewNote(), r.reviewedAt());
    }
}
```

- [ ] **Step 5: Update `MatchAnalysisResponse.from` mapping**

In `backend/ai-module/src/main/java/com/cas/tsas/ai/infrastructure/web/dto/MatchAnalysisResponse.java` den Recommendation-Mapping-Ausdruck ersetzen:
```java
        List<RecommendationResponse> recs = a.getRecommendations() == null
                ? List.of()
                : a.getRecommendations().stream()
                        .map(RecommendationResponse::from)
                        .toList();
```

- [ ] **Step 6: Add the PATCH endpoint**

In `backend/ai-module/src/main/java/com/cas/tsas/ai/infrastructure/web/MatchAnalysisController.java`:

(a) Imports ergänzen:
```java
import com.cas.tsas.ai.application.port.in.ReviewRecommendationUseCase;
import com.cas.tsas.ai.infrastructure.web.dto.ReviewRecommendationRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
```

(b) Feld + Konstruktor erweitern:
```java
    private final GenerateMatchAnalysisUseCase generateUseCase;
    private final GetMatchAnalysisUseCase getUseCase;
    private final ReviewRecommendationUseCase reviewUseCase;

    public MatchAnalysisController(GenerateMatchAnalysisUseCase generateUseCase,
                                   GetMatchAnalysisUseCase getUseCase,
                                   ReviewRecommendationUseCase reviewUseCase) {
        this.generateUseCase = generateUseCase;
        this.getUseCase = getUseCase;
        this.reviewUseCase = reviewUseCase;
    }
```

(c) Methode hinzufügen (nach `get`):
```java
    @PatchMapping("/recommendations/{index}")
    public MatchAnalysisResponse reviewRecommendation(@PathVariable UUID matchId,
                                                      @PathVariable int index,
                                                      @Valid @RequestBody ReviewRecommendationRequest request) {
        return MatchAnalysisResponse.from(
                reviewUseCase.review(matchId, index, request.status(), request.note()));
    }
```

- [ ] **Step 7: Map the two 404 exceptions**

In `backend/ai-module/src/main/java/com/cas/tsas/ai/infrastructure/web/AiExceptionHandler.java`:

(a) Imports ergänzen:
```java
import com.cas.tsas.ai.domain.exception.MatchAnalysisNotFoundException;
import com.cas.tsas.ai.domain.exception.RecommendationNotFoundException;
```

(b) Handler-Methoden ergänzen:
```java
    @ExceptionHandler({MatchAnalysisNotFoundException.class, RecommendationNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
```

- [ ] **Step 8: Run the IT to verify it passes**

Run: `JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew :app:test --tests "com.cas.tsas.ai.MatchAnalysisControllerIT"`
Expected: PASS (alle Methoden grün, inkl. der neuen PATCH-Tests).

- [ ] **Step 9: Run the full backend check (Coverage-Gate)**

Run: `JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew check`
Expected: BUILD SUCCESSFUL (alle Module + Coverage-Gate grün).

- [ ] **Step 10: Commit**

```bash
git add backend/ai-module/src/main/java/com/cas/tsas/ai/infrastructure/web/dto/ReviewRecommendationRequest.java \
        backend/ai-module/src/main/java/com/cas/tsas/ai/infrastructure/web/dto/RecommendationResponse.java \
        backend/ai-module/src/main/java/com/cas/tsas/ai/infrastructure/web/dto/MatchAnalysisResponse.java \
        backend/ai-module/src/main/java/com/cas/tsas/ai/infrastructure/web/MatchAnalysisController.java \
        backend/ai-module/src/main/java/com/cas/tsas/ai/infrastructure/web/AiExceptionHandler.java \
        backend/app/src/test/java/com/cas/tsas/ai/MatchAnalysisControllerIT.java
git commit -m "feat(ai): PATCH endpoint to review recommendations (TEN-39)"
```

---

### Task 5: Frontend — Model, ApiService, Komponente, i18n + Cypress

**Files:**
- Modify: `frontend/src/app/core/models/analysis.model.ts`
- Modify: `frontend/src/app/core/services/api.service.ts`
- Modify: `frontend/src/app/features/matches/analysis/match-analysis.component.ts`
- Modify: `frontend/src/app/features/matches/analysis/match-analysis.component.html`
- Modify: `frontend/public/i18n/de.json`, `en.json`, `it.json`, `fr.json`
- Test: `frontend/src/app/features/matches/analysis/match-analysis.component.cy.ts` (ergänzen)

**Interfaces:**
- Consumes: `PATCH /api/matches/{matchId}/analysis/recommendations/{index}` (Task 4).
- Produces: `ApiService.reviewRecommendation(matchId, index, {status, note})`; Komponente rendert je Empfehlung Annehmen/Verwerfen + Notiz; `recommendations()` liefert `originalIndex` (Persistenz-Index, da die Anzeige nach `priority` sortiert).

- [ ] **Step 1: Write the failing Cypress component test**

In `frontend/src/app/features/matches/analysis/match-analysis.component.cy.ts` folgenden Test ergänzen (nutzt `cy.intercept`, wie projektüblich):
```ts
  it('sends a PATCH and reflects ACCEPTED when accepting a recommendation', () => {
    const analysis = {
      matchId: 'm1', status: 'COMPLETED',
      keyMoments: 'k', ownStrengths: '', ownWeaknesses: '',
      opponentStrengths: '', opponentWeaknesses: '',
      recommendations: [
        { priority: 1, title: 'Serve wide', detail: 'd', status: 'OPEN', reviewNote: null, reviewedAt: null },
      ],
      modelUsed: 'fake-llm', generatedAt: '2026-06-25T10:00:00Z', errorMessage: null,
    };
    cy.intercept('GET', '**/api/matches/*/analysis', analysis).as('get');
    cy.intercept('PATCH', '**/api/matches/*/analysis/recommendations/0', {
      ...analysis,
      recommendations: [{ ...analysis.recommendations[0], status: 'ACCEPTED', reviewedAt: '2026-06-25T11:00:00Z' }],
    }).as('review');

    cy.mount(MatchAnalysisComponent);
    cy.wait('@get');
    cy.get('[data-testid="accept-btn"]').first().click();
    cy.wait('@review').its('request.body').should('deep.include', { status: 'ACCEPTED' });
    cy.get('[data-testid="recommendation"]').first().should('have.attr', 'data-status', 'ACCEPTED');
  });
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd frontend && npx cypress run --component --spec "src/app/features/matches/analysis/match-analysis.component.cy.ts"`
Expected: FAIL — `[data-testid="accept-btn"]` existiert nicht.

- [ ] **Step 3: Extend the model**

In `frontend/src/app/core/models/analysis.model.ts` ergänzen/ersetzen:
```ts
export type RecommendationReviewStatus = 'OPEN' | 'ACCEPTED' | 'REJECTED';

export interface AnalysisRecommendation {
  priority: number;
  title: string;
  detail: string;
  status: RecommendationReviewStatus;
  reviewNote: string | null;
  reviewedAt: string | null;
}
```

- [ ] **Step 4: Add the ApiService method**

In `frontend/src/app/core/services/api.service.ts`:

(a) Import erweitern:
```ts
import { MatchAnalysis, OpponentPreparation, RecommendationReviewStatus } from '../models/analysis.model';
```

(b) Methode nach `generateMatchAnalysis` einfügen:
```ts
  reviewRecommendation(
    matchId: string,
    index: number,
    body: { status: RecommendationReviewStatus; note: string | null },
  ): Observable<MatchAnalysis> {
    return this.http.patch<MatchAnalysis>(
      `${this.base}/matches/${matchId}/analysis/recommendations/${index}`, body);
  }
```

- [ ] **Step 5: Update the component (`.ts`)**

In `frontend/src/app/features/matches/analysis/match-analysis.component.ts`:

(a) Import erweitern:
```ts
import { MatchAnalysis, RecommendationReviewStatus } from '../../../core/models/analysis.model';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
```
und `imports`-Array um `MatInputModule, FormsModule` ergänzen.

(b) `recommendations`-Computed ersetzen (originalIndex mitführen, da Anzeige nach priority sortiert):
```ts
  readonly recommendations = computed(() => {
    const a = this.analysis();
    if (!a) return [];
    return a.recommendations
      .map((rec, originalIndex) => ({ ...rec, originalIndex }))
      .sort((x, y) => x.priority - y.priority);
  });
```

(c) Felder + Methoden ergänzen (innerhalb der Klasse):
```ts
  noteDraft = signal<Record<number, string>>({});

  setNoteDraft(index: number, value: string) {
    this.noteDraft.update(m => ({ ...m, [index]: value }));
  }

  review(index: number, status: RecommendationReviewStatus) {
    const note = this.noteDraft()[index] ?? null;
    this.api.reviewRecommendation(this.matchId, index, { status, note }).subscribe({
      next: a => this.analysis.set(a),
      error: (err: HttpErrorResponse) => this.error.set(this.messageFor(err)),
    });
  }
```

- [ ] **Step 6: Update the template (`.html`)**

In `frontend/src/app/features/matches/analysis/match-analysis.component.html` den `@for`-Block der Empfehlungen (aktuell Z. ~42-50) ersetzen durch:
```html
          @for (rec of recommendations(); track rec.priority) {
            <div class="rec" data-testid="recommendation" [attr.data-status]="rec.status">
              <div class="rec-prio">{{ rec.priority }}</div>
              <div class="rec-body">
                <div class="rec-title">{{ rec.title }}</div>
                @if (rec.detail) { <div class="rec-detail">{{ rec.detail }}</div> }
                @if (rec.status === 'REJECTED' && rec.reviewNote) {
                  <div class="rec-note">{{ rec.reviewNote }}</div>
                }
                <div class="rec-review">
                  <button mat-button data-testid="accept-btn"
                          [disabled]="rec.status === 'ACCEPTED'"
                          (click)="review(rec.originalIndex, 'ACCEPTED')">
                    {{ 'analysis.review.accept' | translate }}
                  </button>
                  <button mat-button data-testid="reject-btn"
                          [disabled]="rec.status === 'REJECTED'"
                          (click)="review(rec.originalIndex, 'REJECTED')">
                    {{ 'analysis.review.reject' | translate }}
                  </button>
                  @if (rec.status !== 'OPEN') {
                    <button mat-button data-testid="reset-btn"
                            (click)="review(rec.originalIndex, 'OPEN')">
                      {{ 'analysis.review.reset' | translate }}
                    </button>
                  }
                  <input matInput class="rec-note-input" data-testid="note-input"
                         [placeholder]="'analysis.review.notePlaceholder' | translate"
                         (input)="setNoteDraft(rec.originalIndex, $any($event.target).value)" />
                </div>
              </div>
            </div>
          }
```

- [ ] **Step 7: Add styles for review state**

In `match-analysis.component.ts` im `styles`-Block ergänzen:
```css
    .rec[data-status="ACCEPTED"] { border-left: 3px solid #22c55e; }
    .rec[data-status="REJECTED"] { opacity: .55; }
    .rec[data-status="REJECTED"] .rec-title { text-decoration: line-through; }
    .rec-note { font-size: 12px; color: #fca5a5; margin-top: 4px; font-style: italic; }
    .rec-review { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; margin-top: 8px; }
    .rec-note-input { background: #0f172a; color: #e2e8f0; border: 1px solid #334155; border-radius: 6px; padding: 4px 8px; font-size: 12px; flex: 1 1 140px; }
```

- [ ] **Step 8: Add i18n keys**

In `frontend/public/i18n/de.json` im `"analysis"`-Objekt ergänzen:
```json
    "review": {
      "accept": "Annehmen",
      "reject": "Verwerfen",
      "reset": "Zurücksetzen",
      "notePlaceholder": "Begründung (optional)"
    }
```
Analog in `en.json` (`Accept`/`Reject`/`Reset`/`Reason (optional)`), `it.json` (`Accetta`/`Rifiuta`/`Reimposta`/`Motivo (facoltativo)`), `fr.json` (`Accepter`/`Rejeter`/`Réinitialiser`/`Justification (facultatif)`).

- [ ] **Step 9: Run the Cypress test to verify it passes**

Run: `cd frontend && npx cypress run --component --spec "src/app/features/matches/analysis/match-analysis.component.cy.ts"`
Expected: PASS.

- [ ] **Step 10: Run frontend lint + unit tests**

Run: `cd frontend && npm run test -- --run && npx ng lint`
Expected: grün (keine neuen Lint-Fehler; bestehende `*.spec.ts` weiter grün).

- [ ] **Step 11: Commit**

```bash
git add frontend/src/app/core/models/analysis.model.ts \
        frontend/src/app/core/services/api.service.ts \
        frontend/src/app/features/matches/analysis/match-analysis.component.ts \
        frontend/src/app/features/matches/analysis/match-analysis.component.html \
        frontend/src/app/features/matches/analysis/match-analysis.component.cy.ts \
        frontend/public/i18n/de.json frontend/public/i18n/en.json \
        frontend/public/i18n/it.json frontend/public/i18n/fr.json
git commit -m "feat(frontend): accept/reject AI recommendations (TEN-39)"
```

---

### Task 6: SAD-Dokumentation

**Files:**
- Modify: `doc/sad/TSaS_SAD_arc42_1.md`

**Interfaces:** keine (Doku).

- [ ] **Step 1: FA-11 um das Review ergänzen**

In `doc/sad/TSaS_SAD_arc42_1.md`, FA-11-Zeile (§10.1): am Ende der SMART-Beschreibung (vor `| V1.x |`) ergänzen:
```
 **Human-in-the-Loop:** Der Coach kann jede Empfehlung über `PATCH /api/matches/{matchId}/analysis/recommendations/{index}` annehmen oder verwerfen (Status `OPEN`/`ACCEPTED`/`REJECTED` + optionale Begründung, max. 500 Zeichen); der Review-Zustand wird in der Empfehlung persistiert. HTTP-Codes: 200, 400 (ungültiger Status / Notiz zu lang), 404 (Match/Analyse/Index unbekannt oder fremder Owner), 409 (Analyse nicht `COMPLETED`). Neu-Generieren setzt den Review-Zustand zurück.
```

- [ ] **Step 2: §10.3 (Abnahmekriterien, KI-Analyse-Zeile) ergänzen**

In der Zeile „**KI-Analyse & Gegner-Vorbereitung**" der §10.3-Tabelle hinter den bestehenden Codes ergänzen:
```
 Zusätzlich kann der Coach einzelne Empfehlungen reviewen (`PATCH …/recommendations/{index}`): 200 (Status/Notiz persistiert), 400 (ungültig), 404 (Index/Owner), 409 (nicht `COMPLETED`).
```

- [ ] **Step 3: §14 — HITL vom Narrativ zum Feature**

In §14 (Reflexion) bzw. der KI-/Guardrails-Beschreibung den HITL-Hinweis als umgesetztes Feature formulieren. Konkret nach dem bestehenden Veto-/Guardrail-Absatz ergänzen:
```
**Human-in-the-Loop (umgesetzt, TEN-39).** Die KI trifft keine endgültigen Entscheidungen: Generierte Empfehlungen einer Match-Analyse sind Vorschläge im Status `OPEN`; der Coach nimmt sie bewusst an oder verwirft sie (mit Begründung). Damit ist HITL nicht nur Prozess, sondern als Feature im Domänenmodell (`Recommendation.status`) und im UI verankert — passend zu Bewertungskriterium 16.
```

- [ ] **Step 4: Verify Schweizer ss**

Run: `! grep -n 'ß' doc/sad/TSaS_SAD_arc42_1.md && echo "ss ok"`
Expected: Ausgabe „ss ok" (kein `ß` im geänderten Dokument).

- [ ] **Step 5: Commit**

```bash
git add doc/sad/TSaS_SAD_arc42_1.md
git commit -m "docs(sad): document HITL recommendation review (TEN-39)"
```

---

## Self-Review

**Spec coverage** (gegen `2026-06-25-ten-39-hitl-recommendation-review-design.md`):
- §3 Domänenmodell → Task 1. §4 Persistenz/Legacy → Task 1 (Default) + Task 2 (Test). §5 Application/Port → Task 3. §6 Guardrails/Fehler → Task 3 (Service) + Task 4 (Web-Mapping/Validation). §7 REST-API → Task 4. §8 Frontend → Task 5. §9 Tests → Tasks 1-5. §10 SAD → Task 6. §11 (kein Migration/kein LLM-Call) → eingehalten (keine Migration, Review rein lokal).
- Abgrenzung FA-20 (kein Review) → keine Änderung an OpponentPreparation; implizit erfüllt.

**Placeholder scan:** Keine „TBD/TODO"; jeder Code-Step enthält vollständigen Code; Test-Steps enthalten Assertions; Commands mit erwarteter Ausgabe. Die einzige bedingte Stelle (Task 2 Step 2 Mapper-Fallback) ist mit konkreter Maßnahme beschrieben, nicht als Platzhalter.

**Type consistency:** `RecommendationStatus {OPEN,ACCEPTED,REJECTED}` durchgängig; `Recommendation.withReview(RecommendationStatus,String,Instant)` (Task 1) wird in Task 3 genutzt; `ReviewRecommendationUseCase.review(UUID,int,RecommendationStatus,String)` (Task 3) wird vom Controller (Task 4) und über `reviewRecommendation` (Frontend, Task 5) konsumiert; `RecommendationResponse.from` (Task 4) liefert `status/reviewNote/reviewedAt`, passend zum Frontend-`AnalysisRecommendation` (Task 5). `originalIndex` schliesst die Index-Lücke zwischen sortierter Anzeige und Persistenz-Reihenfolge.
