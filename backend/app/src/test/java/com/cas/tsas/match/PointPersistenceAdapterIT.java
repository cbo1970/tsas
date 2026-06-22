package com.cas.tsas.match;

import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.match.domain.model.Point;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.match.domain.model.StrokeType;
import com.cas.tsas.match.domain.model.Direction;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchPersistenceAdapter;
import com.cas.tsas.match.infrastructure.persistence.repository.PointPersistenceAdapter;
import com.cas.tsas.player.infrastructure.persistence.entity.PlayerJpaEntity;
import com.cas.tsas.player.infrastructure.persistence.repository.PlayerJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Testcontainers
class PointPersistenceAdapterIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    private static final UUID TEST_OWNER = UUID.randomUUID();

    @Autowired PointPersistenceAdapter pointAdapter;
    @Autowired MatchPersistenceAdapter matchAdapter;
    @Autowired PlayerJpaRepository playerJpaRepository;

    private UUID matchId;

    @BeforeEach
    void setUp() {
        PlayerJpaEntity p1 = new PlayerJpaEntity();
        p1.setOwnerId(TEST_OWNER);
        p1.setFirstName("A"); p1.setLastName("B");
        UUID p1Id = playerJpaRepository.save(p1).getId();

        PlayerJpaEntity p2 = new PlayerJpaEntity();
        p2.setOwnerId(TEST_OWNER);
        p2.setFirstName("C"); p2.setLastName("D");
        UUID p2Id = playerJpaRepository.save(p2).getId();

        Match match = matchAdapter.saveMatch(
                new Match(null, TEST_OWNER, p1Id, p2Id, 2, false, false, MatchStatus.IN_PROGRESS));
        matchId = match.getId();
    }

    @Test
    void saves_point_and_can_be_counted() {
        Point point = new Point(null, matchId, 1, 1, 1, 1,
                PointType.WINNER, null, null, 1, false, null, null);

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
                PointType.WINNER, null, null, null, false, null, null));
        pointAdapter.savePoint(new Point(null, matchId, 1, 2, 1, 2,
                PointType.UNFORCED_ERROR, null, null, null, false, null, null));

        assertThat(pointAdapter.countPointsInGame(matchId, 1, 1)).isEqualTo(1);
        assertThat(pointAdapter.countPointsInGame(matchId, 1, 2)).isEqualTo(1);
    }

    @Test
    void count_accumulates_multiple_points_in_same_game() {
        for (int i = 1; i <= 4; i++) {
            pointAdapter.savePoint(new Point(null, matchId, 1, 1, i, 1,
                    PointType.WINNER, null, null, null, false, null, null));
        }
        assertThat(pointAdapter.countPointsInGame(matchId, 1, 1)).isEqualTo(4);
    }

    @Test
    void loadPointsByMatch_returns_all_points_in_set_game_point_order() {
        pointAdapter.savePoint(new Point(null, matchId, 1, 2, 1, 2,
                PointType.UNFORCED_ERROR, StrokeType.BACKHAND, Direction.DOWN_THE_LINE, 1, false, null, 1));
        pointAdapter.savePoint(new Point(null, matchId, 1, 1, 2, 1,
                PointType.WINNER, StrokeType.FOREHAND, Direction.CROSS_COURT, 1, false, null, 1));
        pointAdapter.savePoint(new Point(null, matchId, 1, 1, 1, 1,
                PointType.WINNER, StrokeType.FOREHAND, Direction.CROSS_COURT, 1, false, null, 1));
        pointAdapter.savePoint(new Point(null, matchId, 2, 1, 1, 2,
                PointType.UNFORCED_ERROR, StrokeType.BACKHAND, Direction.MIDDLE, 2, false, null, 2));

        var points = pointAdapter.loadPointsByMatch(matchId);

        assertThat(points).hasSize(4);
        assertThat(points.get(0).getSetNumber()).isEqualTo(1);
        assertThat(points.get(0).getGameNumber()).isEqualTo(1);
        assertThat(points.get(0).getPointNumber()).isEqualTo(1);
        assertThat(points.get(1).getPointNumber()).isEqualTo(2);
        assertThat(points.get(2).getGameNumber()).isEqualTo(2);
        assertThat(points.get(3).getSetNumber()).isEqualTo(2);
    }

    @Test
    void loadPointsByMatch_returns_empty_list_for_match_with_no_points() {
        assertThat(pointAdapter.loadPointsByMatch(matchId)).isEmpty();
    }

    @Test
    void loadPointsByMatch_does_not_return_points_of_other_matches() {
        PlayerJpaEntity p1 = new PlayerJpaEntity();
        p1.setOwnerId(TEST_OWNER);
        p1.setFirstName("X"); p1.setLastName("Y");
        UUID p1Id = playerJpaRepository.save(p1).getId();
        PlayerJpaEntity p2 = new PlayerJpaEntity();
        p2.setOwnerId(TEST_OWNER);
        p2.setFirstName("Z"); p2.setLastName("Q");
        UUID p2Id = playerJpaRepository.save(p2).getId();
        Match otherMatch = matchAdapter.saveMatch(
                new Match(null, TEST_OWNER, p1Id, p2Id, 2, false, false, MatchStatus.IN_PROGRESS));

        pointAdapter.savePoint(new Point(null, matchId, 1, 1, 1, 1,
                PointType.WINNER, null, null, 1, false, null, null));
        pointAdapter.savePoint(new Point(null, otherMatch.getId(), 1, 1, 1, 1,
                PointType.WINNER, null, null, 1, false, null, null));

        assertThat(pointAdapter.loadPointsByMatch(matchId)).hasSize(1);
        assertThat(pointAdapter.loadPointsByMatch(otherMatch.getId())).hasSize(1);
    }
}
