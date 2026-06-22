package com.cas.tsas.match;

import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchScore;
import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchPersistenceAdapter;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchScorePersistenceAdapter;
import com.cas.tsas.player.infrastructure.persistence.entity.PlayerJpaEntity;
import com.cas.tsas.player.infrastructure.persistence.repository.PlayerJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Testcontainers
class MatchPersistenceAdapterIT {

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

    @Autowired MatchPersistenceAdapter matchAdapter;
    @Autowired MatchScorePersistenceAdapter scoreAdapter;
    @Autowired MatchJpaRepository matchJpaRepository;
    @Autowired PlayerJpaRepository playerJpaRepository;

    private UUID player1Id;
    private UUID player2Id;

    @BeforeEach
    void insertPlayers() {
        PlayerJpaEntity p1 = new PlayerJpaEntity();
        p1.setOwnerId(TEST_OWNER);
        p1.setFirstName("Player");
        p1.setLastName("One");
        player1Id = playerJpaRepository.save(p1).getId();

        PlayerJpaEntity p2 = new PlayerJpaEntity();
        p2.setOwnerId(TEST_OWNER);
        p2.setFirstName("Player");
        p2.setLastName("Two");
        player2Id = playerJpaRepository.save(p2).getId();
    }

    Match newMatch() {
        return new Match(null, TEST_OWNER, player1Id, player2Id, 2, false, false, MatchStatus.IN_PROGRESS);
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
                        assertThat(m.getPlayer1Id()).isEqualTo(player1Id);
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
            assertThat(matchAdapter.existsByPlayerId(player1Id)).isTrue();
        }

        @Test
        void returns_false_when_player_has_no_match() {
            assertThat(matchAdapter.existsByPlayerId(UUID.randomUUID())).isFalse();
        }
    }

    // =========================================================================
    @Nested
    class FindActiveMatchIdsByPlayerIds {

        @BeforeEach
        void setUp() {
            matchJpaRepository.deleteAll();
        }

        @Test
        void returns_match_id_for_player_in_active_match() {
            Match saved = matchAdapter.saveMatch(newMatch()); // IN_PROGRESS

            Map<UUID, UUID> result = matchAdapter.findActiveMatchIdsByPlayerIds(Set.of(player1Id));

            assertThat(result).containsEntry(player1Id, saved.getId());
        }

        @Test
        void returns_match_id_for_player2_as_well() {
            Match saved = matchAdapter.saveMatch(newMatch());

            Map<UUID, UUID> result = matchAdapter.findActiveMatchIdsByPlayerIds(Set.of(player2Id));

            assertThat(result).containsEntry(player2Id, saved.getId());
        }

        @Test
        void excludes_completed_matches() {
            Match completed = new Match(null, TEST_OWNER, player1Id, player2Id, 2, false, false, MatchStatus.COMPLETED);
            matchAdapter.saveMatch(completed);

            Map<UUID, UUID> result = matchAdapter.findActiveMatchIdsByPlayerIds(Set.of(player1Id));

            assertThat(result).isEmpty();
        }

        @Test
        void returns_empty_map_for_player_with_no_match() {
            Map<UUID, UUID> result = matchAdapter.findActiveMatchIdsByPlayerIds(Set.of(UUID.randomUUID()));

            assertThat(result).isEmpty();
        }

        @Test
        void returns_empty_map_when_input_is_empty() {
            Map<UUID, UUID> result = matchAdapter.findActiveMatchIdsByPlayerIds(Set.of());

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    @Nested
    class Score {

        @Test
        void saves_and_loads_score_by_match_id() {
            Match savedMatch = matchAdapter.saveMatch(newMatch());
            MatchScore score = new MatchScore(
                    null, savedMatch.getId(), 2, 1, 3, 2, 1, 0, false, null, 2, false, null, 0, 0, null);

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
