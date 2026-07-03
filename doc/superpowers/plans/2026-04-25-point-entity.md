# POINT-Entität: Implementierungsplan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Einzelne Tennispunkte persistent speichern via `POST /api/matches/{id}/points` und das Frontend-Dialog-Flow implementieren.

**Architecture:** Erweiterung des `match-module` um eine `Point`-Domänenentität und zugehörige Persistence-Schicht (Clean Architecture). Der neue Endpoint ersetzt die bisherigen granularen `/score/player1`, `/ace/player1` Endpoints. Das bestehende `match_scores`-Aggregat bleibt als Read-Modell erhalten.

**Tech Stack:** Spring Boot 3.4 / JPA / PostgreSQL / Flyway / Angular 19 / Angular Material

---

## Dateiübersicht

| Aktion | Pfad |
|--------|------|
| Neu | `match-module/src/main/java/com/cas/tsas/match/domain/model/PointType.java` |
| Neu | `match-module/src/main/java/com/cas/tsas/match/domain/model/StrokeType.java` |
| Neu | `match-module/src/main/java/com/cas/tsas/match/domain/model/Direction.java` |
| Neu | `match-module/src/main/java/com/cas/tsas/match/domain/model/Point.java` |
| Neu | `match-module/src/main/java/com/cas/tsas/match/application/port/out/SavePointPort.java` |
| Neu | `match-module/src/main/java/com/cas/tsas/match/application/port/out/CountPointsInGamePort.java` |
| Neu | `app/src/main/resources/db/migration/V2__add_points_table.sql` |
| Neu | `match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/entity/PointJpaEntity.java` |
| Neu | `match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/repository/PointJpaRepository.java` |
| Neu | `match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/mapper/PointMapper.java` |
| Neu | `match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/repository/PointPersistenceAdapter.java` |
| Neu | `app/src/test/java/com/cas/tsas/match/PointPersistenceAdapterIT.java` |
| Ändern | `match-module/src/main/java/com/cas/tsas/match/application/port/in/RecordPointUseCase.java` |
| Ändern | `match-module/src/main/java/com/cas/tsas/match/application/service/MatchService.java` |
| Löschen | `match-module/src/main/java/com/cas/tsas/match/application/port/in/RecordAceUseCase.java` |
| Neu | `match-module/src/main/java/com/cas/tsas/match/infrastructure/web/dto/request/RecordPointRequest.java` |
| Ändern | `match-module/src/main/java/com/cas/tsas/match/infrastructure/web/MatchController.java` |
| Ändern | `app/src/main/java/com/cas/tsas/app/infrastructure/web/GlobalExceptionHandler.java` |
| Ändern | `match-module/src/test/java/com/cas/tsas/match/application/service/MatchServiceTest.java` |
| Ändern | `app/src/test/java/com/cas/tsas/match/MatchApiIT.java` |
| Neu | `frontend/src/app/core/models/point.model.ts` |
| Ändern | `frontend/src/app/core/services/api.service.ts` |
| Neu | `frontend/src/app/features/matches/score/record-point-dialog.component.ts` |
| Ändern | `frontend/src/app/features/matches/score/score.component.ts` |

---

## Task 1: Domain-Enums und Point-Modell

**Files:**
- Create: `match-module/src/main/java/com/cas/tsas/match/domain/model/PointType.java`
- Create: `match-module/src/main/java/com/cas/tsas/match/domain/model/StrokeType.java`
- Create: `match-module/src/main/java/com/cas/tsas/match/domain/model/Direction.java`
- Create: `match-module/src/main/java/com/cas/tsas/match/domain/model/Point.java`

- [ ] **Step 1: PointType.java erstellen**

```java
package com.cas.tsas.match.domain.model;

public enum PointType {
    WINNER, UNFORCED_ERROR, FORCED_ERROR, ACE, DOUBLE_FAULT, NET, OUT_LONG, OUT_SIDE
}
```

- [ ] **Step 2: StrokeType.java erstellen**

```java
package com.cas.tsas.match.domain.model;

public enum StrokeType {
    FOREHAND, BACKHAND, SERVE, VOLLEY, SMASH
}
```

- [ ] **Step 3: Direction.java erstellen**

```java
package com.cas.tsas.match.domain.model;

public enum Direction {
    CROSS_COURT, DOWN_THE_LINE, MIDDLE
}
```

- [ ] **Step 4: Point.java erstellen**

```java
package com.cas.tsas.match.domain.model;

import java.util.UUID;

public class Point {

    private UUID id;
    private UUID matchId;
    private int setNumber;
    private int gameNumber;
    private int pointNumber;
    private int winner;
    private PointType pointType;
    private StrokeType strokeType;
    private Direction direction;
    private Integer servingPlayer;
    private boolean isBreakPoint;
    private String remark;

    public Point() {}

    public Point(UUID id, UUID matchId, int setNumber, int gameNumber, int pointNumber,
                 int winner, PointType pointType, StrokeType strokeType, Direction direction,
                 Integer servingPlayer, boolean isBreakPoint, String remark) {
        this.id = id;
        this.matchId = matchId;
        this.setNumber = setNumber;
        this.gameNumber = gameNumber;
        this.pointNumber = pointNumber;
        this.winner = winner;
        this.pointType = pointType;
        this.strokeType = strokeType;
        this.direction = direction;
        this.servingPlayer = servingPlayer;
        this.isBreakPoint = isBreakPoint;
        this.remark = remark;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getMatchId() { return matchId; }
    public void setMatchId(UUID matchId) { this.matchId = matchId; }
    public int getSetNumber() { return setNumber; }
    public void setSetNumber(int setNumber) { this.setNumber = setNumber; }
    public int getGameNumber() { return gameNumber; }
    public void setGameNumber(int gameNumber) { this.gameNumber = gameNumber; }
    public int getPointNumber() { return pointNumber; }
    public void setPointNumber(int pointNumber) { this.pointNumber = pointNumber; }
    public int getWinner() { return winner; }
    public void setWinner(int winner) { this.winner = winner; }
    public PointType getPointType() { return pointType; }
    public void setPointType(PointType pointType) { this.pointType = pointType; }
    public StrokeType getStrokeType() { return strokeType; }
    public void setStrokeType(StrokeType strokeType) { this.strokeType = strokeType; }
    public Direction getDirection() { return direction; }
    public void setDirection(Direction direction) { this.direction = direction; }
    public Integer getServingPlayer() { return servingPlayer; }
    public void setServingPlayer(Integer servingPlayer) { this.servingPlayer = servingPlayer; }
    public boolean isBreakPoint() { return isBreakPoint; }
    public void setBreakPoint(boolean breakPoint) { isBreakPoint = breakPoint; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
```

- [ ] **Step 5: Kompilierung prüfen**

```bash
cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :match-module:compileJava
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add backend/match-module/src/main/java/com/cas/tsas/match/domain/model/PointType.java \
        backend/match-module/src/main/java/com/cas/tsas/match/domain/model/StrokeType.java \
        backend/match-module/src/main/java/com/cas/tsas/match/domain/model/Direction.java \
        backend/match-module/src/main/java/com/cas/tsas/match/domain/model/Point.java
git commit -m "feat: add Point domain model and enums (PointType, StrokeType, Direction)"
```

---

## Task 2: Output-Ports für Point-Persistenz

**Files:**
- Create: `match-module/src/main/java/com/cas/tsas/match/application/port/out/SavePointPort.java`
- Create: `match-module/src/main/java/com/cas/tsas/match/application/port/out/CountPointsInGamePort.java`

- [ ] **Step 1: SavePointPort.java erstellen**

```java
package com.cas.tsas.match.application.port.out;

import com.cas.tsas.match.domain.model.Point;

public interface SavePointPort {
    Point savePoint(Point point);
}
```

- [ ] **Step 2: CountPointsInGamePort.java erstellen**

```java
package com.cas.tsas.match.application.port.out;

import java.util.UUID;

public interface CountPointsInGamePort {
    int countPointsInGame(UUID matchId, int setNumber, int gameNumber);
}
```

- [ ] **Step 3: Kompilierung prüfen**

```bash
cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :match-module:compileJava
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add backend/match-module/src/main/java/com/cas/tsas/match/application/port/out/SavePointPort.java \
        backend/match-module/src/main/java/com/cas/tsas/match/application/port/out/CountPointsInGamePort.java
git commit -m "feat: add SavePointPort and CountPointsInGamePort output ports"
```

---

## Task 3: Flyway-Migration V2

**Files:**
- Create: `app/src/main/resources/db/migration/V2__add_points_table.sql`

- [ ] **Step 1: Migration erstellen**

```sql
CREATE TABLE points (
    id             UUID         NOT NULL,
    match_id       UUID         NOT NULL,
    set_number     SMALLINT     NOT NULL,
    game_number    SMALLINT     NOT NULL,
    point_number   SMALLINT     NOT NULL,
    winner         SMALLINT     NOT NULL,
    point_type     VARCHAR(50)  NOT NULL,
    stroke_type    VARCHAR(50),
    direction      VARCHAR(50),
    serving_player SMALLINT,
    is_break_point BOOLEAN      NOT NULL DEFAULT FALSE,
    remark         VARCHAR(500),
    recorded_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT fk_points_match FOREIGN KEY (match_id) REFERENCES matches(id)
);

CREATE INDEX idx_points_match_id ON points(match_id);
```

Wichtig: Enums werden als `VARCHAR` gespeichert, konsistent mit `V1__baseline.sql` (kein `CREATE TYPE` nötig, H2-kompatibel).

- [ ] **Step 2: Commit**

```bash
git add backend/app/src/main/resources/db/migration/V2__add_points_table.sql
git commit -m "feat: add Flyway migration V2 for points table"
```

---

## Task 4: Point-Persistence-Layer (TDD)

**Files:**
- Create: `match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/entity/PointJpaEntity.java`
- Create: `match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/repository/PointJpaRepository.java`
- Create: `match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/mapper/PointMapper.java`
- Create: `match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/repository/PointPersistenceAdapter.java`
- Test: `app/src/test/java/com/cas/tsas/match/PointPersistenceAdapterIT.java`

- [ ] **Step 1: Failing test schreiben**

`app/src/test/java/com/cas/tsas/match/PointPersistenceAdapterIT.java`:

```java
package com.cas.tsas.match;

import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.match.domain.model.Point;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.match.infrastructure.persistence.mapper.MatchMapper;
import com.cas.tsas.match.infrastructure.persistence.mapper.PointMapper;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchPersistenceAdapter;
import com.cas.tsas.match.infrastructure.persistence.repository.PointPersistenceAdapter;
import com.cas.tsas.player.infrastructure.persistence.entity.PlayerJpaEntity;
import com.cas.tsas.player.infrastructure.persistence.repository.PlayerJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PointMapper.class, PointPersistenceAdapter.class,
         MatchMapper.class, MatchPersistenceAdapter.class})
class PointPersistenceAdapterIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired PointPersistenceAdapter pointAdapter;
    @Autowired MatchPersistenceAdapter matchAdapter;
    @Autowired PlayerJpaRepository playerJpaRepository;

    private UUID matchId;

    @BeforeEach
    void setUp() {
        PlayerJpaEntity p1 = new PlayerJpaEntity();
        p1.setFirstName("A"); p1.setLastName("B");
        UUID p1Id = playerJpaRepository.save(p1).getId();

        PlayerJpaEntity p2 = new PlayerJpaEntity();
        p2.setFirstName("C"); p2.setLastName("D");
        UUID p2Id = playerJpaRepository.save(p2).getId();

        Match match = matchAdapter.saveMatch(
                new Match(null, p1Id, p2Id, 2, false, false, MatchStatus.IN_PROGRESS));
        matchId = match.getId();
    }

    @Test
    void saves_point_and_can_be_counted() {
        Point point = new Point(null, matchId, 1, 1, 1, 1,
                PointType.WINNER, null, null, 1, false, null);

        pointAdapter.savePoint(point);

        assertThat(pointAdapter.countPointsInGame(matchId, 1, 1)).isEqualTo(1);
    }

    @Test
    void count_returns_zero_when_no_points_in_game() {
        assertThat(pointAdapter.countPointsInGame(matchId, 1, 1)).isEqualTo(0);
    }

    @Test
    void count_is_scoped_to_set_and_game() {
        pointAdapter.savePoint(new Point(null, matchId, 1, 1, 1, 1,
                PointType.WINNER, null, null, null, false, null));
        pointAdapter.savePoint(new Point(null, matchId, 1, 2, 1, 2,
                PointType.UNFORCED_ERROR, null, null, null, false, null));

        assertThat(pointAdapter.countPointsInGame(matchId, 1, 1)).isEqualTo(1);
        assertThat(pointAdapter.countPointsInGame(matchId, 1, 2)).isEqualTo(1);
    }

    @Test
    void count_accumulates_multiple_points_in_same_game() {
        for (int i = 1; i <= 4; i++) {
            pointAdapter.savePoint(new Point(null, matchId, 1, 1, i, 1,
                    PointType.WINNER, null, null, null, false, null));
        }
        assertThat(pointAdapter.countPointsInGame(matchId, 1, 1)).isEqualTo(4);
    }
}
```

- [ ] **Step 2: Test laufen lassen — Fehler erwartet (Klassen fehlen noch)**

```bash
cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :app:compileTestJava 2>&1 | tail -20
```

Expected: Compilerfehler wegen fehlender Klassen `PointMapper`, `PointPersistenceAdapter` etc.

- [ ] **Step 3: PointJpaEntity erstellen**

`match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/entity/PointJpaEntity.java`:

```java
package com.cas.tsas.match.infrastructure.persistence.entity;

import com.cas.tsas.match.domain.model.Direction;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.match.domain.model.StrokeType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "points")
public class PointJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @Column(name = "set_number", nullable = false)
    private int setNumber;

    @Column(name = "game_number", nullable = false)
    private int gameNumber;

    @Column(name = "point_number", nullable = false)
    private int pointNumber;

    @Column(name = "winner", nullable = false)
    private int winner;

    @Enumerated(EnumType.STRING)
    @Column(name = "point_type", nullable = false, length = 50)
    private PointType pointType;

    @Enumerated(EnumType.STRING)
    @Column(name = "stroke_type", length = 50)
    private StrokeType strokeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 50)
    private Direction direction;

    @Column(name = "serving_player")
    private Integer servingPlayer;

    @Column(name = "is_break_point", nullable = false)
    private boolean isBreakPoint;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @PrePersist
    void prePersist() {
        recordedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getMatchId() { return matchId; }
    public void setMatchId(UUID matchId) { this.matchId = matchId; }
    public int getSetNumber() { return setNumber; }
    public void setSetNumber(int setNumber) { this.setNumber = setNumber; }
    public int getGameNumber() { return gameNumber; }
    public void setGameNumber(int gameNumber) { this.gameNumber = gameNumber; }
    public int getPointNumber() { return pointNumber; }
    public void setPointNumber(int pointNumber) { this.pointNumber = pointNumber; }
    public int getWinner() { return winner; }
    public void setWinner(int winner) { this.winner = winner; }
    public PointType getPointType() { return pointType; }
    public void setPointType(PointType pointType) { this.pointType = pointType; }
    public StrokeType getStrokeType() { return strokeType; }
    public void setStrokeType(StrokeType strokeType) { this.strokeType = strokeType; }
    public Direction getDirection() { return direction; }
    public void setDirection(Direction direction) { this.direction = direction; }
    public Integer getServingPlayer() { return servingPlayer; }
    public void setServingPlayer(Integer servingPlayer) { this.servingPlayer = servingPlayer; }
    public boolean isBreakPoint() { return isBreakPoint; }
    public void setBreakPoint(boolean breakPoint) { isBreakPoint = breakPoint; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Instant getRecordedAt() { return recordedAt; }
}
```

- [ ] **Step 4: PointJpaRepository erstellen**

`match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/repository/PointJpaRepository.java`:

```java
package com.cas.tsas.match.infrastructure.persistence.repository;

import com.cas.tsas.match.infrastructure.persistence.entity.PointJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PointJpaRepository extends JpaRepository<PointJpaEntity, UUID> {
    int countByMatchIdAndSetNumberAndGameNumber(UUID matchId, int setNumber, int gameNumber);
}
```

- [ ] **Step 5: PointMapper erstellen**

`match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/mapper/PointMapper.java`:

```java
package com.cas.tsas.match.infrastructure.persistence.mapper;

import com.cas.tsas.match.domain.model.Point;
import com.cas.tsas.match.infrastructure.persistence.entity.PointJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PointMapper {

    public PointJpaEntity toEntity(Point point) {
        PointJpaEntity entity = new PointJpaEntity();
        entity.setId(point.getId());
        entity.setMatchId(point.getMatchId());
        entity.setSetNumber(point.getSetNumber());
        entity.setGameNumber(point.getGameNumber());
        entity.setPointNumber(point.getPointNumber());
        entity.setWinner(point.getWinner());
        entity.setPointType(point.getPointType());
        entity.setStrokeType(point.getStrokeType());
        entity.setDirection(point.getDirection());
        entity.setServingPlayer(point.getServingPlayer());
        entity.setBreakPoint(point.isBreakPoint());
        entity.setRemark(point.getRemark());
        return entity;
    }

    public Point toDomain(PointJpaEntity entity) {
        return new Point(
                entity.getId(),
                entity.getMatchId(),
                entity.getSetNumber(),
                entity.getGameNumber(),
                entity.getPointNumber(),
                entity.getWinner(),
                entity.getPointType(),
                entity.getStrokeType(),
                entity.getDirection(),
                entity.getServingPlayer(),
                entity.isBreakPoint(),
                entity.getRemark()
        );
    }
}
```

- [ ] **Step 6: PointPersistenceAdapter erstellen**

`match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/repository/PointPersistenceAdapter.java`:

```java
package com.cas.tsas.match.infrastructure.persistence.repository;

import com.cas.tsas.match.application.port.out.CountPointsInGamePort;
import com.cas.tsas.match.application.port.out.SavePointPort;
import com.cas.tsas.match.domain.model.Point;
import com.cas.tsas.match.infrastructure.persistence.mapper.PointMapper;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PointPersistenceAdapter implements SavePointPort, CountPointsInGamePort {

    private final PointJpaRepository pointJpaRepository;
    private final PointMapper pointMapper;

    public PointPersistenceAdapter(PointJpaRepository pointJpaRepository, PointMapper pointMapper) {
        this.pointJpaRepository = pointJpaRepository;
        this.pointMapper = pointMapper;
    }

    @Override
    public Point savePoint(Point point) {
        var entity = pointJpaRepository.save(pointMapper.toEntity(point));
        return pointMapper.toDomain(entity);
    }

    @Override
    public int countPointsInGame(UUID matchId, int setNumber, int gameNumber) {
        return pointJpaRepository.countByMatchIdAndSetNumberAndGameNumber(matchId, setNumber, gameNumber);
    }
}
```

- [ ] **Step 7: Tests laufen lassen — alle müssen grün sein**

```bash
cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test --tests "com.cas.tsas.match.PointPersistenceAdapterIT" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, 4 Tests grün.

- [ ] **Step 8: Commit**

```bash
git add backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/ \
        backend/app/src/test/java/com/cas/tsas/match/PointPersistenceAdapterIT.java
git commit -m "feat: add Point persistence layer (Entity, Repository, Mapper, Adapter) with integration tests"
```

---

## Task 5: Service-Layer aktualisieren (TDD)

**Files:**
- Modify: `match-module/src/main/java/com/cas/tsas/match/application/port/in/RecordPointUseCase.java`
- Modify: `match-module/src/test/java/com/cas/tsas/match/application/service/MatchServiceTest.java`
- Modify: `match-module/src/main/java/com/cas/tsas/match/application/service/MatchService.java`
- Delete: `match-module/src/main/java/com/cas/tsas/match/application/port/in/RecordAceUseCase.java`
- Modify: `app/src/main/java/com/cas/tsas/app/infrastructure/web/GlobalExceptionHandler.java`

- [ ] **Step 1: RecordPointUseCase.RecordPointCommand ersetzen**

Kompletter neuer Inhalt von `RecordPointUseCase.java`:

```java
package com.cas.tsas.match.application.port.in;

import com.cas.tsas.match.domain.model.Direction;
import com.cas.tsas.match.domain.model.MatchScore;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.match.domain.model.StrokeType;

import java.util.UUID;

public interface RecordPointUseCase {

    MatchScore recordPoint(RecordPointCommand command);

    record RecordPointCommand(
            UUID matchId,
            int winner,
            PointType pointType,
            StrokeType strokeType,
            Direction direction,
            String remark
    ) {}
}
```

- [ ] **Step 2: MatchServiceTest aktualisieren (Tests schreiben, bevor Implementierung)**

Kompletter neuer Inhalt von `MatchServiceTest.java` — die `RecordPoint`-Tests werden auf das neue Command-Schema umgestellt, `RecordAce` wird gelöscht, ein neuer Test für Assen-Zählung wird ergänzt:

```java
package com.cas.tsas.match.application.service;

import com.cas.tsas.match.application.port.in.CreateMatchUseCase;
import com.cas.tsas.match.application.port.in.RecordPointUseCase;
import com.cas.tsas.match.application.port.in.SetScoreUseCase;
import com.cas.tsas.match.application.port.in.SetServingPlayerUseCase;
import com.cas.tsas.match.application.port.out.CountPointsInGamePort;
import com.cas.tsas.match.application.port.out.LoadMatchPort;
import com.cas.tsas.match.application.port.out.LoadMatchScorePort;
import com.cas.tsas.match.application.port.out.SaveMatchPort;
import com.cas.tsas.match.application.port.out.SaveMatchScorePort;
import com.cas.tsas.match.application.port.out.SavePointPort;
import com.cas.tsas.match.domain.exception.MatchNotFoundException;
import com.cas.tsas.match.domain.model.Direction;
import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchScore;
import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.match.domain.model.StrokeType;
import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.player.domain.exception.PlayerNotFoundException;
import com.cas.tsas.player.domain.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock private LoadMatchPort loadMatchPort;
    @Mock private SaveMatchPort saveMatchPort;
    @Mock private LoadPlayerPort loadPlayerPort;
    @Mock private LoadMatchScorePort loadMatchScorePort;
    @Mock private SaveMatchScorePort saveMatchScorePort;
    @Mock private SavePointPort savePointPort;
    @Mock private CountPointsInGamePort countPointsInGamePort;

    private MatchService matchService;

    @BeforeEach
    void setUp() {
        matchService = new MatchService(loadMatchPort, saveMatchPort, loadPlayerPort,
                loadMatchScorePort, saveMatchScorePort, new ScoringService(),
                savePointPort, countPointsInGamePort);
    }

    private static final UUID MATCH_ID   = UUID.randomUUID();
    private static final UUID PLAYER1_ID = UUID.randomUUID();
    private static final UUID PLAYER2_ID = UUID.randomUUID();

    private static Match inProgressMatch() {
        return new Match(MATCH_ID, PLAYER1_ID, PLAYER2_ID, 2, false, false, MatchStatus.IN_PROGRESS);
    }

    private static Match completedMatch() {
        return new Match(MATCH_ID, PLAYER1_ID, PLAYER2_ID, 2, false, false, MatchStatus.COMPLETED);
    }

    private static MatchScore freshScore() {
        return new MatchScore(null, MATCH_ID, 0, 0, 0, 0, 0, 0, false, null, 1, false, null, 0, 0, null);
    }

    private static Player anyPlayer(UUID id) {
        return new Player(id, "Test", "Player", null, null, null, null, null, null);
    }

    private static RecordPointUseCase.RecordPointCommand winnerCommand() {
        return new RecordPointUseCase.RecordPointCommand(
                MATCH_ID, 1, PointType.WINNER, StrokeType.FOREHAND, Direction.CROSS_COURT, null);
    }

    // =========================================================================
    @Nested
    class CreateMatch {

        private final CreateMatchUseCase.CreateMatchCommand command =
                new CreateMatchUseCase.CreateMatchCommand(PLAYER1_ID, PLAYER2_ID, 2, false, false);

        @Test
        void saves_match_and_creates_initial_score() {
            when(loadPlayerPort.loadPlayer(PLAYER1_ID)).thenReturn(Optional.of(anyPlayer(PLAYER1_ID)));
            when(loadPlayerPort.loadPlayer(PLAYER2_ID)).thenReturn(Optional.of(anyPlayer(PLAYER2_ID)));
            when(saveMatchPort.saveMatch(any())).thenReturn(inProgressMatch());

            Match result = matchService.createMatch(command);

            assertThat(result.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
            verify(saveMatchScorePort).saveMatchScore(argThat(s ->
                    MATCH_ID.equals(s.getMatchId()) && !s.isDone() && s.getPointsPlayer1() == 0
            ));
        }

        @Test
        void throws_PlayerNotFoundException_when_player1_not_found() {
            when(loadPlayerPort.loadPlayer(PLAYER1_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.createMatch(command))
                    .isInstanceOf(PlayerNotFoundException.class);
        }

        @Test
        void throws_PlayerNotFoundException_when_player2_not_found() {
            when(loadPlayerPort.loadPlayer(PLAYER1_ID)).thenReturn(Optional.of(anyPlayer(PLAYER1_ID)));
            when(loadPlayerPort.loadPlayer(PLAYER2_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.createMatch(command))
                    .isInstanceOf(PlayerNotFoundException.class);
        }
    }

    // =========================================================================
    @Nested
    class FindById {

        @Test
        void returns_match_when_found() {
            Match match = inProgressMatch();
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(match));

            assertThat(matchService.findById(MATCH_ID)).isEqualTo(match);
        }

        @Test
        void throws_MatchNotFoundException_when_not_found() {
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.findById(MATCH_ID))
                    .isInstanceOf(MatchNotFoundException.class);
        }
    }

    // =========================================================================
    @Nested
    class FindAll {

        @Test
        void delegates_to_port_and_returns_all_matches() {
            List<Match> matches = List.of(inProgressMatch());
            when(loadMatchPort.loadAllMatches()).thenReturn(matches);

            assertThat(matchService.findAll()).isEqualTo(matches);
        }
    }

    // =========================================================================
    @Nested
    class GetScore {

        @Test
        void returns_score_when_match_and_score_exist() {
            MatchScore score = freshScore();
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(inProgressMatch()));
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));

            assertThat(matchService.getScore(MATCH_ID)).isEqualTo(score);
        }

        @Test
        void throws_MatchNotFoundException_when_match_not_found() {
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.getScore(MATCH_ID))
                    .isInstanceOf(MatchNotFoundException.class);
        }
    }

    // =========================================================================
    @Nested
    class RecordPoint {

        @Test
        void applies_point_to_score_and_saves_it() {
            MatchScore score = freshScore();
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(inProgressMatch()));
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));
            when(saveMatchScorePort.saveMatchScore(score)).thenReturn(score);
            when(countPointsInGamePort.countPointsInGame(any(), anyInt(), anyInt())).thenReturn(0);
            when(savePointPort.savePoint(any())).thenAnswer(inv -> inv.getArgument(0));

            matchService.recordPoint(winnerCommand());

            assertThat(score.getPointsPlayer1()).isEqualTo(1);
            verify(saveMatchScorePort).saveMatchScore(score);
            verify(savePointPort).savePoint(any());
        }

        @Test
        void ace_point_type_increments_ace_counter_for_winner() {
            MatchScore score = freshScore();
            score.setServingPlayer(1);
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(inProgressMatch()));
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));
            when(saveMatchScorePort.saveMatchScore(score)).thenReturn(score);
            when(countPointsInGamePort.countPointsInGame(any(), anyInt(), anyInt())).thenReturn(0);
            when(savePointPort.savePoint(any())).thenAnswer(inv -> inv.getArgument(0));

            var aceCommand = new RecordPointUseCase.RecordPointCommand(
                    MATCH_ID, 1, PointType.ACE, null, null, null);
            matchService.recordPoint(aceCommand);

            assertThat(score.getAcesPlayer1()).isEqualTo(1);
            assertThat(score.getAcesPlayer2()).isEqualTo(0);
        }

        @Test
        void throws_MatchNotFoundException_when_match_not_found() {
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.recordPoint(winnerCommand()))
                    .isInstanceOf(MatchNotFoundException.class);
        }

        @Test
        void throws_IllegalStateException_when_match_already_completed() {
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(completedMatch()));

            assertThatThrownBy(() -> matchService.recordPoint(winnerCommand()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already completed");
        }

        @Test
        void marks_match_completed_when_score_is_done_after_point() {
            Match match = inProgressMatch();
            MatchScore score = freshScore();
            score.setDone(true);
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(match));
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));
            when(saveMatchScorePort.saveMatchScore(score)).thenReturn(score);
            when(countPointsInGamePort.countPointsInGame(any(), anyInt(), anyInt())).thenReturn(0);
            when(savePointPort.savePoint(any())).thenAnswer(inv -> inv.getArgument(0));

            matchService.recordPoint(winnerCommand());

            verify(saveMatchPort).saveMatch(argThat(m -> m.getStatus() == MatchStatus.COMPLETED));
        }
    }

    // =========================================================================
    @Nested
    class SetScore {

        @Test
        void updates_all_score_fields_and_saves() {
            Match match = inProgressMatch();
            MatchScore score = freshScore();
            var command = new SetScoreUseCase.SetScoreCommand(
                    MATCH_ID, 1, 2, 3, 1, 0, 0, false, null, 1, false, null);
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(match));
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));
            when(saveMatchScorePort.saveMatchScore(score)).thenReturn(score);

            matchService.setScore(command);

            verify(saveMatchScorePort).saveMatchScore(score);
            assertThat(score.getPointsPlayer1()).isEqualTo(1);
            assertThat(score.getPointsPlayer2()).isEqualTo(2);
            assertThat(score.getGamesPlayer1()).isEqualTo(3);
        }

        @Test
        void marks_match_completed_when_score_set_to_done() {
            Match match = inProgressMatch();
            MatchScore score = freshScore();
            var command = new SetScoreUseCase.SetScoreCommand(
                    MATCH_ID, 0, 0, 0, 0, 2, 0, false, null, 3, true, "PLAYER1");
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(match));
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));
            when(saveMatchScorePort.saveMatchScore(any())).thenReturn(score);

            matchService.setScore(command);

            verify(saveMatchPort).saveMatch(argThat(m -> m.getStatus() == MatchStatus.COMPLETED));
        }

        @Test
        void reopens_completed_match_when_score_set_to_not_done() {
            Match match = completedMatch();
            MatchScore score = freshScore();
            var command = new SetScoreUseCase.SetScoreCommand(
                    MATCH_ID, 0, 0, 1, 0, 1, 0, false, null, 2, false, null);
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(match));
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));
            when(saveMatchScorePort.saveMatchScore(any())).thenReturn(score);

            matchService.setScore(command);

            verify(saveMatchPort).saveMatch(argThat(m -> m.getStatus() == MatchStatus.IN_PROGRESS));
        }
    }

    // =========================================================================
    @Nested
    class EndMatch {

        @Test
        void sets_match_status_to_completed() {
            Match match = inProgressMatch();
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(match));
            when(saveMatchPort.saveMatch(any())).thenReturn(match);
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.empty());

            matchService.endMatch(MATCH_ID);

            verify(saveMatchPort).saveMatch(argThat(m -> m.getStatus() == MatchStatus.COMPLETED));
        }

        @Test
        void marks_score_done_and_determines_winner_by_sets() {
            Match match = inProgressMatch();
            MatchScore score = new MatchScore(null, MATCH_ID, 0, 0, 0, 0, 2, 1, false, null, 3, false, null, 0, 0, null);
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(match));
            when(saveMatchPort.saveMatch(any())).thenReturn(match);
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));

            matchService.endMatch(MATCH_ID);

            verify(saveMatchScorePort).saveMatchScore(argThat(s ->
                    s.isDone() && "PLAYER1".equals(s.getWinner())
            ));
        }

        @Test
        void does_not_update_score_when_already_done() {
            Match match = inProgressMatch();
            MatchScore score = freshScore();
            score.setDone(true);
            score.setWinner("PLAYER2");
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(match));
            when(saveMatchPort.saveMatch(any())).thenReturn(match);
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));

            matchService.endMatch(MATCH_ID);

            verify(saveMatchScorePort, never()).saveMatchScore(any());
        }
    }

    // =========================================================================
    @Nested
    class SetServingPlayer {

        @Test
        void throws_MatchNotFoundException_when_match_not_found() {
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                matchService.setServingPlayer(
                    new SetServingPlayerUseCase.SetServingPlayerCommand(MATCH_ID, true)))
                .isInstanceOf(MatchNotFoundException.class);
        }

        @Test
        void throws_IllegalStateException_when_match_already_completed() {
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(completedMatch()));

            assertThatThrownBy(() ->
                matchService.setServingPlayer(
                    new SetServingPlayerUseCase.SetServingPlayerCommand(MATCH_ID, true)))
                .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void sets_serving_player_to_1_for_player1() {
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(inProgressMatch()));
            MatchScore score = freshScore();
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));
            when(saveMatchScorePort.saveMatchScore(any())).thenAnswer(inv -> inv.getArgument(0));

            MatchScore result = matchService.setServingPlayer(
                    new SetServingPlayerUseCase.SetServingPlayerCommand(MATCH_ID, true));

            assertThat(result.getServingPlayer()).isEqualTo(1);
        }

        @Test
        void sets_serving_player_to_2_for_player2() {
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(inProgressMatch()));
            MatchScore score = freshScore();
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));
            when(saveMatchScorePort.saveMatchScore(any())).thenAnswer(inv -> inv.getArgument(0));

            MatchScore result = matchService.setServingPlayer(
                    new SetServingPlayerUseCase.SetServingPlayerCommand(MATCH_ID, false));

            assertThat(result.getServingPlayer()).isEqualTo(2);
        }

        @Test
        void throws_MatchNotFoundException_when_score_not_found() {
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(inProgressMatch()));
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                matchService.setServingPlayer(
                    new SetServingPlayerUseCase.SetServingPlayerCommand(MATCH_ID, true)))
                .isInstanceOf(MatchNotFoundException.class);
        }
    }
}
```

- [ ] **Step 3: Tests laufen lassen — Fehler erwartet (MatchService Konstruktor stimmt nicht mehr)**

```bash
cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :match-module:test 2>&1 | tail -30
```

Expected: Compilation error wegen fehlendem Konstruktor-Parameter in `MatchService`.

- [ ] **Step 4: MatchService aktualisieren**

Kompletter neuer Inhalt von `MatchService.java`:

```java
package com.cas.tsas.match.application.service;

import com.cas.tsas.match.application.port.in.CreateMatchUseCase;
import com.cas.tsas.match.application.port.in.EndMatchUseCase;
import com.cas.tsas.match.application.port.in.GetMatchUseCase;
import com.cas.tsas.match.application.port.in.RecordPointUseCase;
import com.cas.tsas.match.application.port.in.SetScoreUseCase;
import com.cas.tsas.match.application.port.in.SetServingPlayerUseCase;
import com.cas.tsas.match.application.port.out.CountPointsInGamePort;
import com.cas.tsas.match.application.port.out.LoadMatchPort;
import com.cas.tsas.match.application.port.out.LoadMatchScorePort;
import com.cas.tsas.match.application.port.out.SaveMatchPort;
import com.cas.tsas.match.application.port.out.SaveMatchScorePort;
import com.cas.tsas.match.application.port.out.SavePointPort;
import com.cas.tsas.match.domain.exception.MatchNotFoundException;
import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchScore;
import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.match.domain.model.Point;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.player.domain.exception.PlayerNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MatchService implements CreateMatchUseCase, GetMatchUseCase, RecordPointUseCase,
        SetScoreUseCase, EndMatchUseCase, SetServingPlayerUseCase {

    private final LoadMatchPort loadMatchPort;
    private final SaveMatchPort saveMatchPort;
    private final LoadPlayerPort loadPlayerPort;
    private final LoadMatchScorePort loadMatchScorePort;
    private final SaveMatchScorePort saveMatchScorePort;
    private final ScoringService scoringService;
    private final SavePointPort savePointPort;
    private final CountPointsInGamePort countPointsInGamePort;

    public MatchService(LoadMatchPort loadMatchPort, SaveMatchPort saveMatchPort,
                        LoadPlayerPort loadPlayerPort,
                        LoadMatchScorePort loadMatchScorePort,
                        SaveMatchScorePort saveMatchScorePort,
                        ScoringService scoringService,
                        SavePointPort savePointPort,
                        CountPointsInGamePort countPointsInGamePort) {
        this.loadMatchPort = loadMatchPort;
        this.saveMatchPort = saveMatchPort;
        this.loadPlayerPort = loadPlayerPort;
        this.loadMatchScorePort = loadMatchScorePort;
        this.saveMatchScorePort = saveMatchScorePort;
        this.scoringService = scoringService;
        this.savePointPort = savePointPort;
        this.countPointsInGamePort = countPointsInGamePort;
    }

    @Override
    public Match createMatch(CreateMatchCommand command) {
        loadPlayerPort.loadPlayer(command.player1Id())
                .orElseThrow(() -> new PlayerNotFoundException(command.player1Id()));
        loadPlayerPort.loadPlayer(command.player2Id())
                .orElseThrow(() -> new PlayerNotFoundException(command.player2Id()));

        if (loadMatchPort.existsActiveMatchForPlayer(command.player1Id())) {
            throw new IllegalStateException("Player 1 already has an active match");
        }
        if (loadMatchPort.existsActiveMatchForPlayer(command.player2Id())) {
            throw new IllegalStateException("Player 2 already has an active match");
        }

        Match match = new Match(null, command.player1Id(), command.player2Id(),
                command.setsToWin(), command.matchTiebreak(), command.shortSet(),
                MatchStatus.IN_PROGRESS);
        Match saved = saveMatchPort.saveMatch(match);

        MatchScore score = new MatchScore(null, saved.getId(),
                0, 0, 0, 0, 0, 0, false, null, 1, false, null, 0, 0, null);
        saveMatchScorePort.saveMatchScore(score);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Match findById(UUID id) {
        return loadMatchPort.loadMatch(id).orElseThrow(() -> new MatchNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Match> findAll() {
        return loadMatchPort.loadAllMatches();
    }

    @Override
    @Transactional(readOnly = true)
    public MatchScore getScore(UUID matchId) {
        loadMatchPort.loadMatch(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
        return loadMatchScorePort.loadMatchScore(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));
    }

    @Override
    public MatchScore recordPoint(RecordPointCommand command) {
        Match match = loadMatchPort.loadMatch(command.matchId())
                .orElseThrow(() -> new MatchNotFoundException(command.matchId()));

        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new IllegalStateException("Match is already completed");
        }

        MatchScore score = loadMatchScorePort.loadMatchScore(command.matchId())
                .orElseThrow(() -> new MatchNotFoundException(command.matchId()));

        boolean player1Scored = command.winner() == 1;
        int setNumber = score.getCurrentSet();
        int gameNumber = score.getGamesPlayer1() + score.getGamesPlayer2() + 1;
        int pointNumber = countPointsInGamePort.countPointsInGame(
                command.matchId(), setNumber, gameNumber) + 1;
        boolean isBreakPoint = calculateIsBreakPoint(score);

        savePointPort.savePoint(new Point(null, command.matchId(),
                setNumber, gameNumber, pointNumber,
                command.winner(), command.pointType(), command.strokeType(), command.direction(),
                score.getServingPlayer(), isBreakPoint, command.remark()));

        if (command.pointType() == PointType.ACE) {
            if (player1Scored) {
                score.setAcesPlayer1(score.getAcesPlayer1() + 1);
            } else {
                score.setAcesPlayer2(score.getAcesPlayer2() + 1);
            }
        }

        scoringService.applyPoint(match, score, player1Scored);
        MatchScore saved = saveMatchScorePort.saveMatchScore(score);

        if (saved.isDone()) {
            match.setStatus(MatchStatus.COMPLETED);
            saveMatchPort.saveMatch(match);
        }

        return saved;
    }

    private boolean calculateIsBreakPoint(MatchScore score) {
        Integer serving = score.getServingPlayer();
        if (serving == null) return false;

        boolean receiverIsP1 = serving == 2;
        int receiverPts = receiverIsP1 ? score.getPointsPlayer1() : score.getPointsPlayer2();
        int serverPts   = receiverIsP1 ? score.getPointsPlayer2() : score.getPointsPlayer1();

        if (receiverPts == 3 && serverPts < 3) return true;

        if (score.isDeuce() && score.getIsAdvantagePlayer1() != null) {
            return receiverIsP1 == score.getIsAdvantagePlayer1();
        }

        return false;
    }

    @Override
    public MatchScore setScore(SetScoreCommand command) {
        Match match = loadMatchPort.loadMatch(command.matchId())
                .orElseThrow(() -> new MatchNotFoundException(command.matchId()));

        MatchScore score = loadMatchScorePort.loadMatchScore(command.matchId())
                .orElseThrow(() -> new MatchNotFoundException(command.matchId()));

        score.setPointsPlayer1(command.pointsPlayer1());
        score.setPointsPlayer2(command.pointsPlayer2());
        score.setGamesPlayer1(command.gamesPlayer1());
        score.setGamesPlayer2(command.gamesPlayer2());
        score.setSetsPlayer1(command.setsPlayer1());
        score.setSetsPlayer2(command.setsPlayer2());
        score.setDeuce(command.isDeuce());
        score.setIsAdvantagePlayer1(command.isAdvantagePlayer1());
        score.setCurrentSet(command.currentSet());
        score.setDone(command.isDone());
        score.setWinner(command.winner());

        MatchScore saved = saveMatchScorePort.saveMatchScore(score);

        if (saved.isDone() && match.getStatus() != MatchStatus.COMPLETED) {
            match.setStatus(MatchStatus.COMPLETED);
            saveMatchPort.saveMatch(match);
        } else if (!saved.isDone() && match.getStatus() == MatchStatus.COMPLETED) {
            match.setStatus(MatchStatus.IN_PROGRESS);
            saveMatchPort.saveMatch(match);
        }

        return saved;
    }

    @Override
    public MatchScore setServingPlayer(SetServingPlayerCommand command) {
        Match match = loadMatchPort.loadMatch(command.matchId())
                .orElseThrow(() -> new MatchNotFoundException(command.matchId()));

        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new IllegalStateException("Match is already completed");
        }

        MatchScore score = loadMatchScorePort.loadMatchScore(command.matchId())
                .orElseThrow(() -> new MatchNotFoundException(command.matchId()));

        score.setServingPlayer(command.forPlayer1() ? 1 : 2);
        return saveMatchScorePort.saveMatchScore(score);
    }

    @Override
    public Match endMatch(UUID matchId) {
        Match match = loadMatchPort.loadMatch(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));

        match.setStatus(MatchStatus.COMPLETED);
        Match saved = saveMatchPort.saveMatch(match);

        loadMatchScorePort.loadMatchScore(matchId).ifPresent(score -> {
            if (!score.isDone()) {
                score.setDone(true);
                if (score.getSetsPlayer1() > score.getSetsPlayer2()) {
                    score.setWinner("PLAYER1");
                } else if (score.getSetsPlayer2() > score.getSetsPlayer1()) {
                    score.setWinner("PLAYER2");
                }
                saveMatchScorePort.saveMatchScore(score);
            }
        });

        return saved;
    }

    @Override
    public Match endMatchWalkover(UUID matchId, boolean player1Wins) {
        Match match = loadMatchPort.loadMatch(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));

        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new IllegalStateException("Match is already completed");
        }

        match.setStatus(MatchStatus.COMPLETED);
        Match saved = saveMatchPort.saveMatch(match);

        loadMatchScorePort.loadMatchScore(matchId).ifPresent(score -> {
            score.setDone(true);
            score.setWinner(player1Wins ? "PLAYER1" : "PLAYER2");
            saveMatchScorePort.saveMatchScore(score);
        });

        return saved;
    }
}
```

- [ ] **Step 5: RecordAceUseCase.java löschen**

```bash
rm backend/match-module/src/main/java/com/cas/tsas/match/application/port/in/RecordAceUseCase.java
```

- [ ] **Step 6: GlobalExceptionHandler — IllegalArgumentException-Handler ergänzen**

In `GlobalExceptionHandler.java` folgende Methode hinzufügen:

```java
@ExceptionHandler(IllegalArgumentException.class)
public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
}
```

- [ ] **Step 7: Unit-Tests laufen lassen — alle müssen grün sein**

```bash
cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :match-module:test 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, alle Tests grün.

- [ ] **Step 8: Commit**

```bash
git add backend/match-module/src/main/java/com/cas/tsas/match/application/ \
        backend/match-module/src/test/java/com/cas/tsas/match/application/ \
        backend/app/src/main/java/com/cas/tsas/app/infrastructure/web/GlobalExceptionHandler.java
git commit -m "feat: replace RecordAceUseCase with unified RecordPointUseCase (PointType, ace counter, break-point detection)"
```

---

## Task 6: Controller und API-Integrationstests aktualisieren

**Files:**
- Create: `match-module/src/main/java/com/cas/tsas/match/infrastructure/web/dto/request/RecordPointRequest.java`
- Modify: `match-module/src/main/java/com/cas/tsas/match/infrastructure/web/MatchController.java`
- Modify: `app/src/test/java/com/cas/tsas/match/MatchApiIT.java`

- [ ] **Step 1: RecordPointRequest erstellen**

```java
package com.cas.tsas.match.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record RecordPointRequest(
        @NotNull Integer winner,
        @NotBlank String pointType,
        String strokeType,
        String direction,
        @Length(max = 500) String remark
) {}
```

- [ ] **Step 2: Failing MatchApiIT-Tests schreiben**

Kompletter neuer Inhalt von `MatchApiIT.java` — die alten `/score/player1`, `/ace/player1` Endpunkte werden durch `/points` ersetzt:

```java
package com.cas.tsas.match;

import com.cas.tsas.AbstractIntegrationTest;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchScoreJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.PointJpaRepository;
import com.cas.tsas.player.infrastructure.persistence.repository.PlayerJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MatchApiIT extends AbstractIntegrationTest {

    @Autowired ObjectMapper objectMapper;
    @Autowired MatchJpaRepository matchRepository;
    @Autowired MatchScoreJpaRepository matchScoreRepository;
    @Autowired PointJpaRepository pointRepository;
    @Autowired PlayerJpaRepository playerRepository;

    @BeforeEach
    void cleanUp() {
        pointRepository.deleteAll();
        matchScoreRepository.deleteAll();
        matchRepository.deleteAll();
        playerRepository.deleteAll();
    }

    private UUID createPlayer() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "firstName", "Test", "lastName", "Player",
                "gender", "MALE", "handedness", "RIGHT", "backhandType", "TWO_HANDED"
        ));
        String response = mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private UUID createMatch(UUID p1, UUID p2) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "player1Id", p1, "player2Id", p2,
                "setsToWin", 2, "matchTiebreak", false, "shortSet", false
        ));
        String response = mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    // =========================================================================
    @Nested
    class CreateMatch {

        @Test
        void returns_201_with_match_in_progress() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();

            mockMvc.perform(post("/api/matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "player1Id", p1, "player2Id", p2,
                                    "setsToWin", 2, "matchTiebreak", false, "shortSet", false
                            ))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        @Test
        void returns_404_when_player_not_found() throws Exception {
            mockMvc.perform(post("/api/matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "player1Id", UUID.randomUUID(),
                                    "player2Id", UUID.randomUUID(),
                                    "setsToWin", 2, "matchTiebreak", false, "shortSet", false
                            ))))
                    .andExpect(status().isNotFound());
        }

        @Test
        void returns_409_when_player1_already_has_active_match() throws Exception {
            UUID p1 = createPlayer(); UUID p2 = createPlayer(); UUID p3 = createPlayer();
            createMatch(p1, p2);

            mockMvc.perform(post("/api/matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "player1Id", p1, "player2Id", p3,
                                    "setsToWin", 2, "matchTiebreak", false, "shortSet", false
                            ))))
                    .andExpect(status().isConflict());
        }

        @Test
        void returns_409_when_player2_already_has_active_match() throws Exception {
            UUID p1 = createPlayer(); UUID p2 = createPlayer(); UUID p3 = createPlayer();
            createMatch(p1, p2);

            mockMvc.perform(post("/api/matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "player1Id", p3, "player2Id", p2,
                                    "setsToWin", 2, "matchTiebreak", false, "shortSet", false
                            ))))
                    .andExpect(status().isConflict());
        }
    }

    // =========================================================================
    @Nested
    class GetMatch {

        @Test
        void returns_match_with_initial_score() throws Exception {
            UUID p1 = createPlayer(); UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(get("/api/matches/{id}", matchId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(matchId.toString()))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.score.pointsPlayer1").value(0));
        }

        @Test
        void returns_404_when_match_not_found() throws Exception {
            mockMvc.perform(get("/api/matches/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    @Nested
    class ListMatches {

        @Test
        void returns_all_matches() throws Exception {
            UUID p1 = createPlayer(); UUID p2 = createPlayer();
            UUID p3 = createPlayer(); UUID p4 = createPlayer();
            createMatch(p1, p2);
            createMatch(p3, p4);

            mockMvc.perform(get("/api/matches"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }
    }

    // =========================================================================
    @Nested
    class RecordPoint {

        @Test
        void returns_201_with_updated_score() throws Exception {
            UUID p1 = createPlayer(); UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(post("/api/matches/{id}/points", matchId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "winner", 1, "pointType", "WINNER"
                            ))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.score.pointsPlayer1").value(1));
        }

        @Test
        void ace_increments_ace_counter() throws Exception {
            UUID p1 = createPlayer(); UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);
            mockMvc.perform(post("/api/matches/{id}/serve/player1", matchId));

            mockMvc.perform(post("/api/matches/{id}/points", matchId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "winner", 1, "pointType", "ACE"
                            ))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.score.acesPlayer1").value(1))
                    .andExpect(jsonPath("$.score.pointsPlayer1").value(1));
        }

        @Test
        void returns_400_for_invalid_point_type() throws Exception {
            UUID p1 = createPlayer(); UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(post("/api/matches/{id}/points", matchId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "winner", 1, "pointType", "INVALID_TYPE"
                            ))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void returns_409_when_match_already_completed() throws Exception {
            UUID p1 = createPlayer(); UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);
            mockMvc.perform(post("/api/matches/{id}/end", matchId));

            mockMvc.perform(post("/api/matches/{id}/points", matchId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "winner", 1, "pointType", "WINNER"
                            ))))
                    .andExpect(status().isConflict());
        }

        @Test
        void persists_point_with_remark_and_optional_fields() throws Exception {
            UUID p1 = createPlayer(); UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(post("/api/matches/{id}/points", matchId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "winner", 2,
                                    "pointType", "UNFORCED_ERROR",
                                    "strokeType", "BACKHAND",
                                    "direction", "DOWN_THE_LINE",
                                    "remark", "netband"
                            ))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.score.pointsPlayer2").value(1));
        }
    }

    // =========================================================================
    @Nested
    class EndMatch {

        @Test
        void sets_status_to_completed() throws Exception {
            UUID p1 = createPlayer(); UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(post("/api/matches/{id}/end", matchId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }
    }

    // =========================================================================
    @Nested
    class EndMatchWalkover {

        @Test
        void player1_wins_sets_status_completed_and_winner() throws Exception {
            UUID p1 = createPlayer(); UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(post("/api/matches/{id}/end/walkover", matchId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("winner", "PLAYER1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));

            mockMvc.perform(get("/api/matches/{id}", matchId))
                    .andExpect(jsonPath("$.score.winner").value("PLAYER1"))
                    .andExpect(jsonPath("$.score.isDone").value(true));
        }

        @Test
        void player2_wins_sets_winner_to_player2() throws Exception {
            UUID p1 = createPlayer(); UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(post("/api/matches/{id}/end/walkover", matchId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("winner", "PLAYER2"))))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/matches/{id}", matchId))
                    .andExpect(jsonPath("$.score.winner").value("PLAYER2"));
        }

        @Test
        void returns_400_for_invalid_winner_value() throws Exception {
            UUID p1 = createPlayer(); UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(post("/api/matches/{id}/end/walkover", matchId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("winner", "INVALID"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void returns_404_for_unknown_match() throws Exception {
            mockMvc.perform(post("/api/matches/{id}/end/walkover", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("winner", "PLAYER1"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        void returns_409_when_match_already_completed() throws Exception {
            UUID p1 = createPlayer(); UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);
            mockMvc.perform(post("/api/matches/{id}/end", matchId));

            mockMvc.perform(post("/api/matches/{id}/end/walkover", matchId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("winner", "PLAYER1"))))
                    .andExpect(status().isConflict());
        }
    }

    // =========================================================================
    @Nested
    class SetServingPlayer {

        @Test
        void sets_serving_player_1() throws Exception {
            UUID p1 = createPlayer(); UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(post("/api/matches/{id}/serve/player1", matchId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.servingPlayer").value(1));
        }

        @Test
        void sets_serving_player_2() throws Exception {
            UUID p1 = createPlayer(); UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(post("/api/matches/{id}/serve/player2", matchId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.servingPlayer").value(2));
        }

        @Test
        void returns_409_when_match_already_completed() throws Exception {
            UUID p1 = createPlayer(); UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);
            mockMvc.perform(post("/api/matches/{id}/end", matchId));

            mockMvc.perform(post("/api/matches/{id}/serve/player1", matchId))
                    .andExpect(status().isConflict());
        }
    }
}
```

- [ ] **Step 3: MatchController aktualisieren**

Kompletter neuer Inhalt von `MatchController.java`:

```java
package com.cas.tsas.match.infrastructure.web;

import com.cas.tsas.match.application.port.in.CreateMatchUseCase;
import com.cas.tsas.match.application.port.in.EndMatchUseCase;
import com.cas.tsas.match.application.port.in.GetMatchUseCase;
import com.cas.tsas.match.application.port.in.RecordPointUseCase;
import com.cas.tsas.match.application.port.in.SetScoreUseCase;
import com.cas.tsas.match.application.port.in.SetServingPlayerUseCase;
import com.cas.tsas.match.domain.model.Direction;
import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchScore;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.match.domain.model.StrokeType;
import com.cas.tsas.match.infrastructure.web.dto.request.CreateMatchRequest;
import com.cas.tsas.match.infrastructure.web.dto.request.EndMatchWalkoverRequest;
import com.cas.tsas.match.infrastructure.web.dto.request.RecordPointRequest;
import com.cas.tsas.match.infrastructure.web.dto.request.SetScoreRequest;
import com.cas.tsas.match.infrastructure.web.dto.response.MatchResponse;
import com.cas.tsas.match.infrastructure.web.dto.response.MatchScoreResponse;
import com.cas.tsas.match.infrastructure.web.dto.response.MatchWithScoreResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final CreateMatchUseCase createMatchUseCase;
    private final GetMatchUseCase getMatchUseCase;
    private final RecordPointUseCase recordPointUseCase;
    private final SetScoreUseCase setScoreUseCase;
    private final EndMatchUseCase endMatchUseCase;
    private final SetServingPlayerUseCase setServingPlayerUseCase;

    public MatchController(CreateMatchUseCase createMatchUseCase,
                           GetMatchUseCase getMatchUseCase,
                           RecordPointUseCase recordPointUseCase,
                           SetScoreUseCase setScoreUseCase,
                           EndMatchUseCase endMatchUseCase,
                           SetServingPlayerUseCase setServingPlayerUseCase) {
        this.createMatchUseCase = createMatchUseCase;
        this.getMatchUseCase = getMatchUseCase;
        this.recordPointUseCase = recordPointUseCase;
        this.setScoreUseCase = setScoreUseCase;
        this.endMatchUseCase = endMatchUseCase;
        this.setServingPlayerUseCase = setServingPlayerUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchResponse createMatch(@Valid @RequestBody CreateMatchRequest request) {
        var command = new CreateMatchUseCase.CreateMatchCommand(
                request.player1Id(), request.player2Id(),
                request.setsToWin(), request.matchTiebreak(), request.shortSet());
        return MatchResponse.from(createMatchUseCase.createMatch(command));
    }

    @GetMapping
    public List<MatchResponse> listMatches() {
        return getMatchUseCase.findAll().stream().map(MatchResponse::from).toList();
    }

    @GetMapping("/{id}")
    public MatchWithScoreResponse getMatch(@PathVariable UUID id) {
        Match match = getMatchUseCase.findById(id);
        MatchScore score = getMatchUseCase.getScore(id);
        return MatchWithScoreResponse.from(match, score);
    }

    @PostMapping("/{id}/points")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchWithScoreResponse recordPoint(@PathVariable UUID id,
                                              @Valid @RequestBody RecordPointRequest request) {
        StrokeType strokeType = request.strokeType() != null
                ? StrokeType.valueOf(request.strokeType()) : null;
        Direction direction = request.direction() != null
                ? Direction.valueOf(request.direction()) : null;

        var command = new RecordPointUseCase.RecordPointCommand(
                id,
                request.winner(),
                PointType.valueOf(request.pointType()),
                strokeType,
                direction,
                request.remark());
        MatchScore score = recordPointUseCase.recordPoint(command);
        Match match = getMatchUseCase.findById(id);
        return MatchWithScoreResponse.from(match, score);
    }

    @PostMapping("/{id}/serve/player1")
    public MatchScoreResponse servePlayer1(@PathVariable UUID id) {
        var command = new SetServingPlayerUseCase.SetServingPlayerCommand(id, true);
        return MatchScoreResponse.from(setServingPlayerUseCase.setServingPlayer(command));
    }

    @PostMapping("/{id}/serve/player2")
    public MatchScoreResponse servePlayer2(@PathVariable UUID id) {
        var command = new SetServingPlayerUseCase.SetServingPlayerCommand(id, false);
        return MatchScoreResponse.from(setServingPlayerUseCase.setServingPlayer(command));
    }

    @PutMapping("/{id}/score")
    public MatchScoreResponse setScore(@PathVariable UUID id,
                                       @RequestBody SetScoreRequest request) {
        var command = new SetScoreUseCase.SetScoreCommand(
                id,
                request.pointsPlayer1(), request.pointsPlayer2(),
                request.gamesPlayer1(), request.gamesPlayer2(),
                request.setsPlayer1(), request.setsPlayer2(),
                request.isDeuce(), request.isAdvantagePlayer1(),
                request.currentSet(), request.isDone(), request.winner());
        return MatchScoreResponse.from(setScoreUseCase.setScore(command));
    }

    @PostMapping("/{id}/end")
    public MatchResponse endMatch(@PathVariable UUID id) {
        return MatchResponse.from(endMatchUseCase.endMatch(id));
    }

    @PostMapping("/{id}/end/walkover")
    public MatchResponse endMatchWalkover(@PathVariable UUID id,
                                          @Valid @RequestBody EndMatchWalkoverRequest request) {
        return MatchResponse.from(endMatchUseCase.endMatchWalkover(id, "PLAYER1".equals(request.winner())));
    }
}
```

- [ ] **Step 4: Alle Tests laufen lassen**

```bash
cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL`, alle Tests grün.

- [ ] **Step 5: Commit**

```bash
git add backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/web/ \
        backend/app/src/test/java/com/cas/tsas/match/MatchApiIT.java
git commit -m "feat: replace granular score/ace endpoints with POST /api/matches/{id}/points"
```

---

## Task 7: Frontend — TypeScript-Modelle und ApiService

**Files:**
- Create: `frontend/src/app/core/models/point.model.ts`
- Modify: `frontend/src/app/core/services/api.service.ts`

- [ ] **Step 1: point.model.ts erstellen**

```typescript
export type PointType =
  | 'WINNER' | 'UNFORCED_ERROR' | 'FORCED_ERROR'
  | 'ACE' | 'DOUBLE_FAULT' | 'NET' | 'OUT_LONG' | 'OUT_SIDE';

export type StrokeType = 'FOREHAND' | 'BACKHAND' | 'SERVE' | 'VOLLEY' | 'SMASH';

export type Direction = 'CROSS_COURT' | 'DOWN_THE_LINE' | 'MIDDLE';

export interface RecordPointRequest {
  winner: 1 | 2;
  pointType: PointType;
  strokeType?: StrokeType;
  direction?: Direction;
  remark?: string;
}
```

- [ ] **Step 2: ApiService aktualisieren**

Kompletter neuer Inhalt von `api.service.ts`:

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Player, CreatePlayerRequest } from '../models/player.model';
import {
  Match,
  MatchWithScore,
  MatchScore,
  CreateMatchRequest,
  SetScoreRequest
} from '../models/match.model';
import { RecordPointRequest } from '../models/point.model';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl + '/api';

  // Players
  getPlayers(): Observable<Player[]> {
    return this.http.get<Player[]>(`${this.base}/players`);
  }

  getPlayer(id: string): Observable<Player> {
    return this.http.get<Player>(`${this.base}/players/${id}`);
  }

  createPlayer(request: CreatePlayerRequest): Observable<Player> {
    return this.http.post<Player>(`${this.base}/players`, request);
  }

  updatePlayer(id: string, request: CreatePlayerRequest): Observable<Player> {
    return this.http.put<Player>(`${this.base}/players/${id}`, request);
  }

  deletePlayer(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/players/${id}`);
  }

  deactivatePlayer(id: string): Observable<void> {
    return this.http.patch<void>(`${this.base}/players/${id}/deactivate`, {});
  }

  // Matches
  getMatches(): Observable<Match[]> {
    return this.http.get<Match[]>(`${this.base}/matches`);
  }

  getMatch(id: string): Observable<MatchWithScore> {
    return this.http.get<MatchWithScore>(`${this.base}/matches/${id}`);
  }

  createMatch(request: CreateMatchRequest): Observable<Match> {
    return this.http.post<Match>(`${this.base}/matches`, request);
  }

  recordPoint(matchId: string, request: RecordPointRequest): Observable<MatchWithScore> {
    return this.http.post<MatchWithScore>(`${this.base}/matches/${matchId}/points`, request);
  }

  setServingPlayer1(matchId: string): Observable<MatchScore> {
    return this.http.post<MatchScore>(`${this.base}/matches/${matchId}/serve/player1`, {});
  }

  setServingPlayer2(matchId: string): Observable<MatchScore> {
    return this.http.post<MatchScore>(`${this.base}/matches/${matchId}/serve/player2`, {});
  }

  setScore(matchId: string, request: SetScoreRequest): Observable<MatchScore> {
    return this.http.put<MatchScore>(`${this.base}/matches/${matchId}/score`, request);
  }

  endMatch(matchId: string): Observable<Match> {
    return this.http.post<Match>(`${this.base}/matches/${matchId}/end`, {});
  }

  endMatchWalkover(matchId: string, winner: 'PLAYER1' | 'PLAYER2'): Observable<Match> {
    return this.http.post<Match>(`${this.base}/matches/${matchId}/end/walkover`, { winner });
  }
}
```

- [ ] **Step 3: TypeScript-Kompilierung prüfen**

```bash
cd frontend && ng build --configuration=development 2>&1 | tail -20
```

Expected: Build erfolgreich, keine Fehler.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/core/models/point.model.ts \
        frontend/src/app/core/services/api.service.ts
git commit -m "feat: add point.model.ts and update ApiService (recordPoint, remove legacy score/ace methods)"
```

---

## Task 8: Frontend — RecordPointDialogComponent

**Files:**
- Create: `frontend/src/app/features/matches/score/record-point-dialog.component.ts`

- [ ] **Step 1: Dialog-Komponente erstellen**

```typescript
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { PointType, StrokeType, Direction, RecordPointRequest } from '../../../core/models/point.model';

export interface RecordPointDialogData {
  winner: 1 | 2;
  winnerName: string;
}

@Component({
  selector: 'app-record-point-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatFormFieldModule,
    MatInputModule
  ],
  template: `
    <h2 mat-dialog-title>Punkt für {{ data.winnerName }}</h2>

    <mat-dialog-content>

      <div class="section">
        <div class="section-label">Punkttyp *</div>
        <mat-button-toggle-group [(ngModel)]="pointType" class="toggle-group wrap">
          <mat-button-toggle value="WINNER">Winner</mat-button-toggle>
          <mat-button-toggle value="UNFORCED_ERROR">Eigenf.</mat-button-toggle>
          <mat-button-toggle value="FORCED_ERROR">Erzw. F.</mat-button-toggle>
          <mat-button-toggle value="ACE">Ass</mat-button-toggle>
          <mat-button-toggle value="DOUBLE_FAULT">DF</mat-button-toggle>
          <mat-button-toggle value="NET">Netz</mat-button-toggle>
          <mat-button-toggle value="OUT_LONG">Aus lang</mat-button-toggle>
          <mat-button-toggle value="OUT_SIDE">Aus Seite</mat-button-toggle>
        </mat-button-toggle-group>
      </div>

      <div class="section">
        <div class="section-label">Schlagart (optional)</div>
        <mat-button-toggle-group [(ngModel)]="strokeType" class="toggle-group">
          <mat-button-toggle value="FOREHAND">FH</mat-button-toggle>
          <mat-button-toggle value="BACKHAND">RH</mat-button-toggle>
          <mat-button-toggle value="SERVE">Aufschlag</mat-button-toggle>
          <mat-button-toggle value="VOLLEY">Volley</mat-button-toggle>
          <mat-button-toggle value="SMASH">Smash</mat-button-toggle>
        </mat-button-toggle-group>
      </div>

      <div class="section">
        <div class="section-label">Richtung (optional)</div>
        <mat-button-toggle-group [(ngModel)]="direction" class="toggle-group">
          <mat-button-toggle value="CROSS_COURT">Cross</mat-button-toggle>
          <mat-button-toggle value="DOWN_THE_LINE">DTL</mat-button-toggle>
          <mat-button-toggle value="MIDDLE">Mitte</mat-button-toggle>
        </mat-button-toggle-group>
      </div>

      <mat-form-field class="full-width">
        <mat-label>Bemerkung (optional)</mat-label>
        <input matInput [(ngModel)]="remark" maxlength="500" />
      </mat-form-field>

    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Abbrechen</button>
      <button mat-raised-button color="primary"
              [disabled]="!pointType"
              (click)="confirm()">Speichern</button>
    </mat-dialog-actions>
  `,
  styles: [`
    mat-dialog-content { display: flex; flex-direction: column; gap: 16px; min-width: 320px; }
    .section { display: flex; flex-direction: column; gap: 6px; }
    .section-label { font-size: 12px; color: rgba(0,0,0,.6); font-weight: 500; }
    .toggle-group { flex-wrap: wrap; }
    .toggle-group.wrap ::ng-deep .mat-button-toggle-group { flex-wrap: wrap; }
    .full-width { width: 100%; }
  `]
})
export class RecordPointDialogComponent {
  readonly data: RecordPointDialogData = inject(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<RecordPointDialogComponent>);

  pointType: PointType | null = null;
  strokeType: StrokeType | null = 'FOREHAND';
  direction: Direction | null = 'CROSS_COURT';
  remark = '';

  cancel(): void {
    this.dialogRef.close(null);
  }

  confirm(): void {
    if (!this.pointType) return;
    const result: RecordPointRequest = {
      winner: this.data.winner,
      pointType: this.pointType,
      ...(this.strokeType && { strokeType: this.strokeType }),
      ...(this.direction && { direction: this.direction }),
      ...(this.remark.trim() && { remark: this.remark.trim() })
    };
    this.dialogRef.close(result);
  }
}
```

- [ ] **Step 2: TypeScript-Kompilierung prüfen**

```bash
cd frontend && ng build --configuration=development 2>&1 | tail -10
```

Expected: `Build at:` ohne Fehler.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/features/matches/score/record-point-dialog.component.ts
git commit -m "feat: add RecordPointDialogComponent with point type, stroke type, direction, remark"
```

---

## Task 9: Frontend — ScoreComponent aktualisieren

**Files:**
- Modify: `frontend/src/app/features/matches/score/score.component.ts`

- [ ] **Step 1: ScoreComponent aktualisieren**

`handleCourtClick` öffnet jetzt immer den Dialog (wenn Aufschläger gesetzt ist). Ace-Buttons werden entfernt (Asse werden über den Dialog erfasst).

Folgende Änderungen in `score.component.ts`:

**Imports ergänzen:**
```typescript
import { RecordPointDialogComponent, RecordPointDialogData } from './record-point-dialog.component';
import { RecordPointRequest } from '../../../core/models/point.model';
```

**`imports`-Array in `@Component` ergänzen:** `MatButtonToggleModule` (falls noch nicht vorhanden, aber es wird nicht benötigt, da der Dialog eine eigene Komponente ist).

**`handleCourtClick` ersetzen:**
```typescript
handleCourtClick(forPlayer1: boolean) {
  if (this.servingPlayer() === null) {
    this.setServe(forPlayer1);
  } else {
    this.openPointDialog(forPlayer1 ? 1 : 2);
  }
}
```

**Neue Methode `openPointDialog` hinzufügen:**
```typescript
openPointDialog(winner: 1 | 2) {
  const m = this.matchData();
  if (!m || m.status === 'COMPLETED') return;

  const winnerName = winner === 1 ? this.player1Name() : this.player2Name();
  const data: RecordPointDialogData = { winner, winnerName };

  const ref = this.dialog.open(RecordPointDialogComponent, {
    width: '420px',
    data
  });

  ref.afterClosed().subscribe((result: RecordPointRequest | null) => {
    if (!result) return;
    this.api.recordPoint(this.matchId, result).subscribe({
      next: (updated) => {
        this.matchData.set(updated);
        if (updated.score.isDone) this.loadMatch();
      },
      error: () => this.snackBar.open('Fehler beim Speichern', 'OK', { duration: 3000 })
    });
  });
}
```

**`recordAce`-Methode und Ace-Buttons entfernen:**
- Methode `recordAce` löschen
- Im Template: die beiden `.ace-btn`-Buttons und `.bottom-area`-Layout vereinfachen (nur noch `.action-buttons` in der Mitte)

**Neue Template `.bottom-area`** (Ace-Buttons entfernt):
```html
<div class="bottom-area">
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
</div>
```

**CSS `.bottom-area` vereinfachen:**
```css
.bottom-area {
  display: flex;
  justify-content: center;
  width: 100%;
  margin-top: 20px;
}
.action-buttons {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
```

- [ ] **Step 2: TypeScript-Kompilierung prüfen**

```bash
cd frontend && ng build --configuration=development 2>&1 | tail -10
```

Expected: `Build at:` ohne Fehler.

- [ ] **Step 3: Vollständige Backend-Tests laufen lassen**

```bash
cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, alle Tests grün.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/features/matches/score/score.component.ts
git commit -m "feat: update ScoreComponent — court click opens RecordPointDialog, remove ace buttons"
```

---

## Spec-Abgleich

| Spec-Anforderung | Task |
|-----------------|------|
| `POINT`-Entität mit allen Feldern | T1 (Point.java), T4 (PointJpaEntity) |
| Enums PointType / StrokeType / Direction | T1 |
| Ports SavePointPort / CountPointsInGamePort | T2 |
| Flyway V2 Migration | T3 |
| `POST /api/matches/{id}/points` | T6 (Controller) |
| `winner`, `pointType` Pflichtfelder; optionale Felder | T6 (RecordPointRequest) |
| 400 für ungültige Enum-Werte | T5 (GlobalExceptionHandler) |
| 409 bei COMPLETED Match | T5 (MatchService) |
| Assen-Counter via pointType=ACE | T5 (MatchService) |
| Break-Point-Berechnung | T5 (calculateIsBreakPoint) |
| Entfernung alter Endpoints (score/player, ace/player) | T6 (Controller) |
| Serve-Endpoints bleiben | T6 (Controller) |
| Frontend-Dialog mit Punkttyp/Schlagart/Richtung | T8 |
| ScoreComponent öffnet Dialog statt direktem Score | T9 |
| ApiService.recordPoint | T7 |
| Entfernung scorePlayer1/2, acePlayer1/2 aus ApiService | T7 |
| PointPersistenceAdapterIT | T4 |
| MatchServiceTest aktualisiert | T5 |
| MatchApiIT aktualisiert | T6 |
