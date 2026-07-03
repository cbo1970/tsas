# Ass-Button Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ass-Button pro Spieler in der Score-Page — zählt Ass und Punkt gleichzeitig; Ass-Zähler wird in der Match-Statistik gespeichert.

**Architecture:** Zwei neue Endpoints `POST /api/matches/{id}/ace/player1|player2` folgen dem bestehenden `RecordPointUseCase`-Muster. `MatchScore` bekommt zwei neue Felder `acesPlayer1`/`acesPlayer2`, die durch alle Schichten (Domain → JPA → Response) durchgezogen werden. Im Frontend erscheinen die Ass-Buttons links/rechts im dunkelblauen Aussenfeld unterhalb des Courts (Option A Layout).

**Tech Stack:** Spring Boot 3.4 (Java 21), JPA/H2/PostgreSQL, Angular 21, Angular Material

---

## Betroffene Dateien

**Backend — modifiziert:**
- `backend/match-module/src/main/java/com/cas/tsas/match/domain/model/MatchScore.java`
- `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/entity/MatchScoreJpaEntity.java`
- `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/mapper/MatchScoreMapper.java`
- `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/dto/response/MatchScoreResponse.java`
- `backend/match-module/src/main/java/com/cas/tsas/match/application/service/MatchService.java`
- `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/MatchController.java`

**Backend — neu:**
- `backend/match-module/src/main/java/com/cas/tsas/match/application/port/in/RecordAceUseCase.java`

**Backend — Tests:**
- `backend/match-module/src/test/java/com/cas/tsas/match/application/service/ScoringServiceTest.java` (neuer Testfall)
- `backend/app/src/test/java/com/cas/tsas/match/MatchApiIT.java` (neuer Testfall)

**Frontend — modifiziert:**
- `frontend/src/app/core/models/match.model.ts`
- `frontend/src/app/core/services/api.service.ts`
- `frontend/src/app/features/matches/score/score.component.ts`

---

## Task 1: MatchScore Domain Model erweitern

**Files:**
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/domain/model/MatchScore.java`

- [ ] **Schritt 1: Felder, Konstruktor, Getter/Setter hinzufügen**

Ersetze den Inhalt der Datei komplett:

```java
package com.cas.tsas.match.domain.model;

import java.util.UUID;

public class MatchScore {

    private UUID id;
    private UUID matchId;
    private int pointsPlayer1;
    private int pointsPlayer2;
    private int gamesPlayer1;
    private int gamesPlayer2;
    private int setsPlayer1;
    private int setsPlayer2;
    private boolean isDeuce;
    private Boolean isAdvantagePlayer1;
    private int currentSet;
    private boolean isDone;
    private String winner;
    private int acesPlayer1;
    private int acesPlayer2;

    public MatchScore() {}

    public MatchScore(UUID id, UUID matchId,
                      int pointsPlayer1, int pointsPlayer2,
                      int gamesPlayer1, int gamesPlayer2,
                      int setsPlayer1, int setsPlayer2,
                      boolean isDeuce, Boolean isAdvantagePlayer1,
                      int currentSet, boolean isDone, String winner,
                      int acesPlayer1, int acesPlayer2) {
        this.id = id;
        this.matchId = matchId;
        this.pointsPlayer1 = pointsPlayer1;
        this.pointsPlayer2 = pointsPlayer2;
        this.gamesPlayer1 = gamesPlayer1;
        this.gamesPlayer2 = gamesPlayer2;
        this.setsPlayer1 = setsPlayer1;
        this.setsPlayer2 = setsPlayer2;
        this.isDeuce = isDeuce;
        this.isAdvantagePlayer1 = isAdvantagePlayer1;
        this.currentSet = currentSet;
        this.isDone = isDone;
        this.winner = winner;
        this.acesPlayer1 = acesPlayer1;
        this.acesPlayer2 = acesPlayer2;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getMatchId() { return matchId; }
    public void setMatchId(UUID matchId) { this.matchId = matchId; }

    public int getPointsPlayer1() { return pointsPlayer1; }
    public void setPointsPlayer1(int pointsPlayer1) { this.pointsPlayer1 = pointsPlayer1; }

    public int getPointsPlayer2() { return pointsPlayer2; }
    public void setPointsPlayer2(int pointsPlayer2) { this.pointsPlayer2 = pointsPlayer2; }

    public int getGamesPlayer1() { return gamesPlayer1; }
    public void setGamesPlayer1(int gamesPlayer1) { this.gamesPlayer1 = gamesPlayer1; }

    public int getGamesPlayer2() { return gamesPlayer2; }
    public void setGamesPlayer2(int gamesPlayer2) { this.gamesPlayer2 = gamesPlayer2; }

    public int getSetsPlayer1() { return setsPlayer1; }
    public void setSetsPlayer1(int setsPlayer1) { this.setsPlayer1 = setsPlayer1; }

    public int getSetsPlayer2() { return setsPlayer2; }
    public void setSetsPlayer2(int setsPlayer2) { this.setsPlayer2 = setsPlayer2; }

    public boolean isDeuce() { return isDeuce; }
    public void setDeuce(boolean deuce) { isDeuce = deuce; }

    public Boolean getIsAdvantagePlayer1() { return isAdvantagePlayer1; }
    public void setIsAdvantagePlayer1(Boolean isAdvantagePlayer1) { this.isAdvantagePlayer1 = isAdvantagePlayer1; }

    public int getCurrentSet() { return currentSet; }
    public void setCurrentSet(int currentSet) { this.currentSet = currentSet; }

    public boolean isDone() { return isDone; }
    public void setDone(boolean done) { isDone = done; }

    public String getWinner() { return winner; }
    public void setWinner(String winner) { this.winner = winner; }

    public int getAcesPlayer1() { return acesPlayer1; }
    public void setAcesPlayer1(int acesPlayer1) { this.acesPlayer1 = acesPlayer1; }

    public int getAcesPlayer2() { return acesPlayer2; }
    public void setAcesPlayer2(int acesPlayer2) { this.acesPlayer2 = acesPlayer2; }
}
```

- [ ] **Schritt 2: Konstruktoraufruf in MatchService.createMatch() aktualisieren**

In `backend/match-module/src/main/java/com/cas/tsas/match/application/service/MatchService.java`, ersetze den `new MatchScore(...)`-Aufruf in `createMatch()`:

```java
MatchScore score = new MatchScore(
        null, saved.getId(),
        0, 0,
        0, 0,
        0, 0,
        false, null,
        1, false, null,
        0, 0
);
```

- [ ] **Schritt 3: Mapper aktualisieren**

In `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/mapper/MatchScoreMapper.java`:

```java
public MatchScore toDomain(MatchScoreJpaEntity entity) {
    return new MatchScore(
            entity.getId(),
            entity.getMatchId(),
            entity.getPointsPlayer1(),
            entity.getPointsPlayer2(),
            entity.getGamesPlayer1(),
            entity.getGamesPlayer2(),
            entity.getSetsPlayer1(),
            entity.getSetsPlayer2(),
            entity.isDeuce(),
            entity.getIsAdvantagePlayer1(),
            entity.getCurrentSet(),
            entity.isDone(),
            entity.getWinner(),
            entity.getAcesPlayer1(),
            entity.getAcesPlayer2()
    );
}

public MatchScoreJpaEntity toEntity(MatchScore score) {
    MatchScoreJpaEntity entity = new MatchScoreJpaEntity();
    entity.setId(score.getId());
    entity.setMatchId(score.getMatchId());
    entity.setPointsPlayer1(score.getPointsPlayer1());
    entity.setPointsPlayer2(score.getPointsPlayer2());
    entity.setGamesPlayer1(score.getGamesPlayer1());
    entity.setGamesPlayer2(score.getGamesPlayer2());
    entity.setSetsPlayer1(score.getSetsPlayer1());
    entity.setSetsPlayer2(score.getSetsPlayer2());
    entity.setDeuce(score.isDeuce());
    entity.setIsAdvantagePlayer1(score.getIsAdvantagePlayer1());
    entity.setCurrentSet(score.getCurrentSet());
    entity.setDone(score.isDone());
    entity.setWinner(score.getWinner());
    entity.setAcesPlayer1(score.getAcesPlayer1());
    entity.setAcesPlayer2(score.getAcesPlayer2());
    return entity;
}
```

- [ ] **Schritt 4: JPA Entity aktualisieren**

In `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/entity/MatchScoreJpaEntity.java`, nach dem `winner`-Feld einfügen:

```java
@Column(name = "aces_player1")
private int acesPlayer1;

@Column(name = "aces_player2")
private int acesPlayer2;
```

Sowie Getter/Setter am Ende der Klasse:

```java
public int getAcesPlayer1() { return acesPlayer1; }
public void setAcesPlayer1(int acesPlayer1) { this.acesPlayer1 = acesPlayer1; }

public int getAcesPlayer2() { return acesPlayer2; }
public void setAcesPlayer2(int acesPlayer2) { this.acesPlayer2 = acesPlayer2; }
```

- [ ] **Schritt 5: MatchScoreResponse aktualisieren**

In `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/dto/response/MatchScoreResponse.java`:

```java
public record MatchScoreResponse(
        UUID id,
        UUID matchId,
        int pointsPlayer1,
        int pointsPlayer2,
        int gamesPlayer1,
        int gamesPlayer2,
        int setsPlayer1,
        int setsPlayer2,
        boolean isDeuce,
        Boolean isAdvantagePlayer1,
        int currentSet,
        boolean isDone,
        String winner,
        int acesPlayer1,
        int acesPlayer2
) {
    public static MatchScoreResponse from(MatchScore score) {
        return new MatchScoreResponse(
                score.getId(),
                score.getMatchId(),
                score.getPointsPlayer1(),
                score.getPointsPlayer2(),
                score.getGamesPlayer1(),
                score.getGamesPlayer2(),
                score.getSetsPlayer1(),
                score.getSetsPlayer2(),
                score.isDeuce(),
                score.getIsAdvantagePlayer1(),
                score.getCurrentSet(),
                score.isDone(),
                score.getWinner(),
                score.getAcesPlayer1(),
                score.getAcesPlayer2()
        );
    }
}
```

- [ ] **Schritt 6: Backend kompilieren**

```bash
cd backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew compileJava --no-daemon 2>&1 | tail -5
```
Erwartet: `BUILD SUCCESSFUL`

- [ ] **Schritt 7: Commit**

```bash
git add backend/match-module/src/main/java/com/cas/tsas/match/domain/model/MatchScore.java \
        backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/entity/MatchScoreJpaEntity.java \
        backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/mapper/MatchScoreMapper.java \
        backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/dto/response/MatchScoreResponse.java \
        backend/match-module/src/main/java/com/cas/tsas/match/application/service/MatchService.java
git commit -m "feat: add acesPlayer1/acesPlayer2 fields to MatchScore"
```

---

## Task 2: RecordAceUseCase + MatchService-Implementierung

**Files:**
- Create: `backend/match-module/src/main/java/com/cas/tsas/match/application/port/in/RecordAceUseCase.java`
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/application/service/MatchService.java`

- [ ] **Schritt 1: Failing Test schreiben**

In `backend/match-module/src/test/java/com/cas/tsas/match/application/service/ScoringServiceTest.java`, füge am Ende der Klasse hinzu (vor der letzten `}`):

```java
@Test
void recordAce_incrementsAceCounterAndScoresPoint() {
    // Arrange
    Match match = new Match(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            2, false, false, MatchStatus.IN_PROGRESS);
    MatchScore score = new MatchScore(UUID.randomUUID(), match.getId(),
            0, 0, 0, 0, 0, 0, false, null, 1, false, null, 0, 0);

    // Act — ace for player 1
    score.setAcesPlayer1(score.getAcesPlayer1() + 1);
    scoringService.applyPoint(match, score, true);

    // Assert
    assertThat(score.getAcesPlayer1()).isEqualTo(1);
    assertThat(score.getAcesPlayer2()).isEqualTo(0);
    assertThat(score.getPointsPlayer1()).isEqualTo(1); // 0 -> 15
}
```

Stelle sicher dass `ScoringServiceTest` die nötigen Imports hat:
```java
import com.cas.tsas.match.domain.model.MatchStatus;
```

- [ ] **Schritt 2: Test ausführen — muss PASS**

```bash
cd backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :match-module:test --tests "*ScoringServiceTest*" --no-daemon 2>&1 | tail -8
```
Erwartet: `BUILD SUCCESSFUL` (der Test testet bestehende Methoden, soll von Anfang an grün sein)

- [ ] **Schritt 3: RecordAceUseCase Interface erstellen**

```java
package com.cas.tsas.match.application.port.in;

import com.cas.tsas.match.domain.model.MatchScore;

import java.util.UUID;

public interface RecordAceUseCase {

    MatchScore recordAce(RecordAceCommand command);

    record RecordAceCommand(
            UUID matchId,
            boolean forPlayer1
    ) {}
}
```

- [ ] **Schritt 4: MatchService implementiert RecordAceUseCase**

In `MatchService.java`:

1. Klassendeklaration erweitern:
```java
public class MatchService implements CreateMatchUseCase, GetMatchUseCase, RecordPointUseCase,
        SetScoreUseCase, EndMatchUseCase, RecordAceUseCase {
```

2. Import hinzufügen:
```java
import com.cas.tsas.match.application.port.in.RecordAceUseCase;
```

3. Neue Methode am Ende der Klasse (vor der letzten `}`) einfügen:
```java
@Override
public MatchScore recordAce(RecordAceCommand command) {
    Match match = loadMatchPort.loadMatch(command.matchId())
            .orElseThrow(() -> new MatchNotFoundException(command.matchId()));

    if (match.getStatus() == MatchStatus.COMPLETED) {
        throw new IllegalStateException("Match is already completed");
    }

    MatchScore score = loadMatchScorePort.loadMatchScore(command.matchId())
            .orElseThrow(() -> new MatchNotFoundException(command.matchId()));

    if (command.forPlayer1()) {
        score.setAcesPlayer1(score.getAcesPlayer1() + 1);
    } else {
        score.setAcesPlayer2(score.getAcesPlayer2() + 1);
    }

    scoringService.applyPoint(match, score, command.forPlayer1());
    MatchScore saved = saveMatchScorePort.saveMatchScore(score);

    if (saved.isDone()) {
        match.setStatus(MatchStatus.COMPLETED);
        saveMatchPort.saveMatch(match);
    }

    return saved;
}
```

- [ ] **Schritt 5: Tests ausführen**

```bash
cd backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :match-module:test --no-daemon 2>&1 | tail -8
```
Erwartet: `BUILD SUCCESSFUL`

- [ ] **Schritt 6: Commit**

```bash
git add backend/match-module/src/main/java/com/cas/tsas/match/application/port/in/RecordAceUseCase.java \
        backend/match-module/src/main/java/com/cas/tsas/match/application/service/MatchService.java \
        backend/match-module/src/test/java/com/cas/tsas/match/application/service/ScoringServiceTest.java
git commit -m "feat: add RecordAceUseCase — increments ace counter and scores point"
```

---

## Task 3: REST Endpoints

**Files:**
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/MatchController.java`

- [ ] **Schritt 1: Failing Integration Test schreiben**

In `backend/app/src/test/java/com/cas/tsas/match/MatchApiIT.java`, füge einen neuen Testfall hinzu (nach den bestehenden Tests):

```java
@Test
void acePlayer1_incrementsAceCounterAndScoresPoint() throws Exception {
    // Arrange: create players and match
    UUID p1 = createPlayer("Ace", "Player1");
    UUID p2 = createPlayer("Ace", "Player2");
    UUID matchId = createMatch(p1, p2);

    // Act
    String response = mockMvc.perform(post("/api/matches/{id}/ace/player1", matchId))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

    // Assert
    assertThatJson(response)
            .node("acesPlayer1").isEqualTo(1)
            .node("acesPlayer2").isEqualTo(0)
            .node("pointsPlayer1").isEqualTo(1);
}
```

Stelle sicher dass die Helfer-Methoden `createPlayer` und `createMatch` im Test vorhanden sind (prüfe `MatchApiIT.java` auf bestehende Hilfsmethoden und passe den Test an das vorhandene Muster an).

- [ ] **Schritt 2: Test ausführen — muss FAIL**

```bash
cd backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :app:test --tests "*MatchApiIT*" --no-daemon 2>&1 | tail -10
```
Erwartet: FAIL mit `404 Not Found` (Endpoint existiert noch nicht)

- [ ] **Schritt 3: Endpoints in MatchController hinzufügen**

In `MatchController.java`:

1. Import hinzufügen:
```java
import com.cas.tsas.match.application.port.in.RecordAceUseCase;
```

2. Field und Konstruktor erweitern:
```java
private final RecordAceUseCase recordAceUseCase;

public MatchController(CreateMatchUseCase createMatchUseCase,
                       GetMatchUseCase getMatchUseCase,
                       RecordPointUseCase recordPointUseCase,
                       SetScoreUseCase setScoreUseCase,
                       EndMatchUseCase endMatchUseCase,
                       RecordAceUseCase recordAceUseCase) {
    this.createMatchUseCase = createMatchUseCase;
    this.getMatchUseCase = getMatchUseCase;
    this.recordPointUseCase = recordPointUseCase;
    this.setScoreUseCase = setScoreUseCase;
    this.endMatchUseCase = endMatchUseCase;
    this.recordAceUseCase = recordAceUseCase;
}
```

3. Zwei neue Endpoint-Methoden nach `scorePlayer2` einfügen:
```java
@PostMapping("/{id}/ace/player1")
public MatchScoreResponse acePlayer1(@PathVariable UUID id) {
    var command = new RecordAceUseCase.RecordAceCommand(id, true);
    return MatchScoreResponse.from(recordAceUseCase.recordAce(command));
}

@PostMapping("/{id}/ace/player2")
public MatchScoreResponse acePlayer2(@PathVariable UUID id) {
    var command = new RecordAceUseCase.RecordAceCommand(id, false);
    return MatchScoreResponse.from(recordAceUseCase.recordAce(command));
}
```

- [ ] **Schritt 4: Integration Test ausführen — muss PASS**

```bash
cd backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :app:test --tests "*MatchApiIT*" --no-daemon 2>&1 | tail -10
```
Erwartet: `BUILD SUCCESSFUL`

- [ ] **Schritt 5: Alle Backend-Tests ausführen**

```bash
cd backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test --no-daemon 2>&1 | tail -8
```
Erwartet: `BUILD SUCCESSFUL`

- [ ] **Schritt 6: Commit**

```bash
git add backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/MatchController.java \
        backend/app/src/test/java/com/cas/tsas/match/MatchApiIT.java
git commit -m "feat: add POST /api/matches/{id}/ace/player1|player2 endpoints"
```

---

## Task 4: Frontend — Model und ApiService

**Files:**
- Modify: `frontend/src/app/core/models/match.model.ts`
- Modify: `frontend/src/app/core/services/api.service.ts`

- [ ] **Schritt 1: MatchScore Interface erweitern**

In `frontend/src/app/core/models/match.model.ts`, füge in `MatchScore` zwei neue Felder hinzu:

```typescript
export interface MatchScore {
  id: string;
  matchId: string;
  pointsPlayer1: number;
  pointsPlayer2: number;
  gamesPlayer1: number;
  gamesPlayer2: number;
  setsPlayer1: number;
  setsPlayer2: number;
  isDeuce: boolean;
  isAdvantagePlayer1: boolean | null;
  currentSet: number;
  isDone: boolean;
  winner: string | null;
  acesPlayer1: number;
  acesPlayer2: number;
}
```

- [ ] **Schritt 2: ApiService erweitern**

In `frontend/src/app/core/services/api.service.ts`, füge nach `scorePlayer2` hinzu:

```typescript
acePlayer1(matchId: string): Observable<MatchScore> {
  return this.http.post<MatchScore>(`${this.base}/matches/${matchId}/ace/player1`, {});
}

acePlayer2(matchId: string): Observable<MatchScore> {
  return this.http.post<MatchScore>(`${this.base}/matches/${matchId}/ace/player2`, {});
}
```

- [ ] **Schritt 3: Angular Build prüfen**

```bash
cd frontend
npx ng build --configuration development 2>&1 | tail -5
```
Erwartet: `Application bundle generation complete`

- [ ] **Schritt 4: Commit**

```bash
git add frontend/src/app/core/models/match.model.ts \
        frontend/src/app/core/services/api.service.ts
git commit -m "feat: add acesPlayer1/acesPlayer2 to MatchScore model and acePlayer1/acePlayer2 to ApiService"
```

---

## Task 5: Frontend — ScoreComponent UI

**Files:**
- Modify: `frontend/src/app/features/matches/score/score.component.ts`

- [ ] **Schritt 1: `recordAce`-Methode hinzufügen**

In `score.component.ts`, füge nach der `scorePoint`-Methode ein:

```typescript
recordAce(forPlayer1: boolean) {
  const m = this.matchData();
  if (!m || m.status === 'COMPLETED') return;

  const obs = forPlayer1
    ? this.api.acePlayer1(this.matchId)
    : this.api.acePlayer2(this.matchId);

  obs.subscribe({
    next: (score) => {
      this.matchData.update(md => md ? { ...md, score } : md);
      if (score.isDone) {
        this.loadMatch();
      }
    },
    error: () => this.snackBar.open('Fehler beim Speichern', 'OK', { duration: 3000 })
  });
}
```

- [ ] **Schritt 2: Template anpassen — Ass-Buttons links/rechts, Action-Buttons zentriert**

Ersetze den `<div class="action-buttons">...</div>` Block (der sich innerhalb von `court-wrapper` befindet) durch:

```html
<div class="bottom-area">
  <button class="ace-btn"
          [disabled]="matchData()!.status === 'COMPLETED'"
          (click)="recordAce(true)">
    <span class="ace-icon">🎾</span>
    <span class="ace-count">{{ matchData()!.score.acesPlayer1 }}</span>
    <span class="ace-lbl">Asse {{ player1Name() }}</span>
  </button>

  <div class="action-buttons">
    <button mat-stroked-button (click)="openEditDialog()">
      <mat-icon>edit</mat-icon>
      Score korrigieren
    </button>
    @if (matchData()!.status !== 'COMPLETED') {
      <button mat-raised-button color="warn" (click)="endMatch()">
        <mat-icon>stop</mat-icon>
        Match beenden
      </button>
    }
  </div>

  <button class="ace-btn"
          [disabled]="matchData()!.status === 'COMPLETED'"
          (click)="recordAce(false)">
    <span class="ace-icon">🎾</span>
    <span class="ace-count">{{ matchData()!.score.acesPlayer2 }}</span>
    <span class="ace-lbl">Asse {{ player2Name() }}</span>
  </button>
</div>
```

- [ ] **Schritt 3: CSS hinzufügen**

Ersetze die bestehende `.action-buttons`-Regel und füge neue Regeln hinzu:

```css
/* ── Bottom area (ace buttons + action buttons) ── */
.bottom-area {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  margin-top: 20px;
  gap: 12px;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  flex: 1;
}
.action-buttons .mat-mdc-outlined-button {
  color: white;
  --mdc-outlined-button-outline-color: rgba(255,255,255,.6);
}

.ace-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  background: rgba(255,255,255,.1);
  border: 1px solid rgba(255,255,255,.4);
  border-radius: 10px;
  padding: 10px 16px;
  cursor: pointer;
  color: white;
  min-width: 80px;
  transition: background 0.15s;
}
.ace-btn:hover:not([disabled]) { background: rgba(255,255,255,.2); }
.ace-btn:active:not([disabled]) { background: rgba(255,255,255,.28); }
.ace-btn[disabled] { opacity: 0.4; cursor: default; }
.ace-icon { font-size: 20px; line-height: 1; }
.ace-count { font-size: 28px; font-weight: bold; line-height: 1; }
.ace-lbl { font-size: 10px; text-transform: uppercase; letter-spacing: 0.5px; color: rgba(255,255,255,.7); text-align: center; }
```

- [ ] **Schritt 4: Build prüfen**

```bash
cd frontend
npx ng build --configuration development 2>&1 | tail -5
```
Erwartet: `Application bundle generation complete`

- [ ] **Schritt 5: Manuell testen**

```bash
cd frontend
npm start
```
- Im Browser `http://localhost:4200` öffnen
- Ein Match starten
- Auf Ass-Button klicken → Zähler erhöht sich, Punkte-Anzeige aktualisiert sich
- Match-Status `COMPLETED` → Ass-Buttons sind deaktiviert (opacity 0.4)

- [ ] **Schritt 6: Commit**

```bash
git add frontend/src/app/features/matches/score/score.component.ts
git commit -m "feat: add ace buttons to score page — Option A layout (left/right in outer field)"
```
