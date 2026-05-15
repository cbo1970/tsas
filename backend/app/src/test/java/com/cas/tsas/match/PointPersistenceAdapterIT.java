package com.cas.tsas.match;

import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.match.domain.model.Point;
import com.cas.tsas.match.domain.model.PointType;
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
