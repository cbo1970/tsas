package com.cas.tsas.match;

import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchScore;
import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.match.infrastructure.persistence.mapper.MatchMapper;
import com.cas.tsas.match.infrastructure.persistence.mapper.MatchScoreMapper;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchPersistenceAdapter;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchScorePersistenceAdapter;
import org.junit.jupiter.api.Nested;
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
@Import({MatchMapper.class, MatchPersistenceAdapter.class,
         MatchScoreMapper.class, MatchScorePersistenceAdapter.class})
class MatchPersistenceAdapterIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MatchPersistenceAdapter matchAdapter;
    @Autowired MatchScorePersistenceAdapter scoreAdapter;

    private static final UUID PLAYER1_ID = UUID.randomUUID();
    private static final UUID PLAYER2_ID = UUID.randomUUID();

    private static Match newMatch() {
        return new Match(null, PLAYER1_ID, PLAYER2_ID, 2, false, false, MatchStatus.IN_PROGRESS);
    }

    // =========================================================================
    @Nested
    class SaveAndLoad {

        @Test
        void saves_match_and_loads_by_id() {
            Match saved = matchAdapter.saveMatch(newMatch());

            assertThat(matchAdapter.loadMatch(saved.getId()))
                    .isPresent()
                    .hasValueSatisfying(m -> {
                        assertThat(m.getPlayer1Id()).isEqualTo(PLAYER1_ID);
                        assertThat(m.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
                    });
        }

        @Test
        void returns_empty_when_match_not_found() {
            assertThat(matchAdapter.loadMatch(UUID.randomUUID())).isEmpty();
        }

        @Test
        void loads_all_saved_matches() {
            matchAdapter.saveMatch(newMatch());
            matchAdapter.saveMatch(newMatch());

            assertThat(matchAdapter.loadAllMatches()).hasSize(2);
        }
    }

    // =========================================================================
    @Nested
    class ExistsByPlayerId {

        @Test
        void returns_true_when_player_has_a_match() {
            matchAdapter.saveMatch(newMatch());
            assertThat(matchAdapter.existsByPlayerId(PLAYER1_ID)).isTrue();
        }

        @Test
        void returns_false_when_player_has_no_match() {
            assertThat(matchAdapter.existsByPlayerId(UUID.randomUUID())).isFalse();
        }
    }

    // =========================================================================
    @Nested
    class Score {

        @Test
        void saves_and_loads_score_by_match_id() {
            Match savedMatch = matchAdapter.saveMatch(newMatch());
            MatchScore score = new MatchScore(
                    null, savedMatch.getId(), 2, 1, 3, 2, 1, 0, false, null, 2, false, null);

            scoreAdapter.saveMatchScore(score);

            assertThat(scoreAdapter.loadMatchScore(savedMatch.getId()))
                    .isPresent()
                    .hasValueSatisfying(s -> {
                        assertThat(s.getPointsPlayer1()).isEqualTo(2);
                        assertThat(s.getSetsPlayer1()).isEqualTo(1);
                    });
        }

        @Test
        void returns_empty_when_no_score_for_match() {
            assertThat(scoreAdapter.loadMatchScore(UUID.randomUUID())).isEmpty();
        }
    }
}
