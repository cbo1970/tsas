package com.cas.tsas.dataexport;

import com.cas.tsas.AbstractIntegrationTest;
import com.cas.tsas.ai.infrastructure.persistence.entity.MatchAnalysisJpaEntity;
import com.cas.tsas.ai.infrastructure.persistence.repository.MatchAnalysisJpaRepository;
import com.cas.tsas.auth.domain.Role;
import com.cas.tsas.auth.testsupport.JwtTestSupport;
import com.cas.tsas.match.application.port.out.SavePointPort;
import com.cas.tsas.match.domain.model.Direction;
import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.match.domain.model.Point;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.match.domain.model.StrokeType;
import com.cas.tsas.match.infrastructure.persistence.entity.MatchScoreJpaEntity;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchPersistenceAdapter;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchScoreJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.PointJpaRepository;
import com.cas.tsas.player.infrastructure.persistence.entity.PlayerJpaEntity;
import com.cas.tsas.player.infrastructure.persistence.repository.PlayerJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** TEN-66: DSGVO Art. 17 (Löschung) und Art. 20 (Export). */
class DataExportApiIT extends AbstractIntegrationTest {

    /** Two distinct users so that we can verify cross-tenant isolation on both export and delete. */
    private static final UUID USER_A = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID USER_B = UUID.fromString("00000000-0000-0000-0000-0000000000a2");

    @Autowired MatchPersistenceAdapter matchAdapter;
    @Autowired SavePointPort savePoint;
    @Autowired PlayerJpaRepository playerRepo;
    @Autowired MatchJpaRepository matchRepo;
    @Autowired MatchScoreJpaRepository matchScoreRepo;
    @Autowired PointJpaRepository pointRepo;
    @Autowired MatchAnalysisJpaRepository analysisRepo;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        // Drains in FK order to avoid violating constraints when other ITs share the DB.
        analysisRepo.deleteAll();
        pointRepo.deleteAll();
        matchScoreRepo.deleteAll();
        matchRepo.deleteAll();
        playerRepo.deleteAll();
    }

    @Test
    void export_returnsOwnAggregatesOnly() throws Exception {
        seed(USER_A, /*matches*/ 2, /*pointsPerMatch*/ 3, /*addAnalysis*/ true);
        seed(USER_B, 1, 1, false);

        mockMvc.perform(get("/api/dataexport/export")
                        .with(JwtTestSupport.withUser(USER_A, Role.COACH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.userId").value(USER_A.toString()))
                .andExpect(jsonPath("$.players.length()").value(3))  // 2× p1 + 1 shared p2
                .andExpect(jsonPath("$.matches.length()").value(2))
                .andExpect(jsonPath("$.points.length()").value(6))
                .andExpect(jsonPath("$.scores.length()").value(2))
                .andExpect(jsonPath("$.analyses.length()").value(2));
    }

    @Test
    void delete_removesOwnAggregates_andLeavesOtherUsersIntact() throws Exception {
        seed(USER_A, 2, 3, true);
        seed(USER_B, 1, 1, false);

        mockMvc.perform(delete("/api/dataexport")
                        .with(JwtTestSupport.withUser(USER_A, Role.COACH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_A.toString()))
                .andExpect(jsonPath("$.players").value(3))  // 2× p1 + 1 shared p2
                .andExpect(jsonPath("$.matches").value(2))
                .andExpect(jsonPath("$.points").value(6))
                .andExpect(jsonPath("$.scores").value(2));

        // User A is empty …
        org.junit.jupiter.api.Assertions.assertTrue(playerRepo.findAllByOwnerId(USER_A).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(matchRepo.findAllByOwnerId(USER_A).isEmpty());

        // … but User B is untouched (1 match × 2 players = 2 players, 1 match for User B).
        org.junit.jupiter.api.Assertions.assertEquals(2, playerRepo.findAllByOwnerId(USER_B).size());
        org.junit.jupiter.api.Assertions.assertEquals(1, matchRepo.findAllByOwnerId(USER_B).size());
    }

    @Test
    void delete_isIdempotent_returnsZeroCountsWhenNothingToDelete() throws Exception {
        mockMvc.perform(delete("/api/dataexport")
                        .with(JwtTestSupport.withUser(USER_A, Role.COACH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players").value(0))
                .andExpect(jsonPath("$.matches").value(0))
                .andExpect(jsonPath("$.points").value(0))
                .andExpect(jsonPath("$.scores").value(0));
    }

    private void seed(UUID owner, int matchCount, int pointsPerMatch, boolean addAnalysis) {
        for (int i = 0; i < matchCount; i++) {
            PlayerJpaEntity p1 = new PlayerJpaEntity();
            p1.setOwnerId(owner);
            p1.setFirstName("P1-" + owner.toString().substring(34) + "-" + i);
            p1.setLastName("X");
            UUID p1Id = playerRepo.save(p1).getId();

            // Reuse a single second player per owner block to keep totals predictable.
            PlayerJpaEntity p2 = playerRepo.findAllByOwnerId(owner).stream()
                    .filter(p -> p.getLastName().equals("Y"))
                    .findFirst()
                    .orElseGet(() -> {
                        PlayerJpaEntity n = new PlayerJpaEntity();
                        n.setOwnerId(owner);
                        n.setFirstName("P2-" + owner.toString().substring(34));
                        n.setLastName("Y");
                        return playerRepo.save(n);
                    });

            Match match = matchAdapter.saveMatch(
                    new Match(null, owner, p1Id, p2.getId(), 2, false, false, MatchStatus.COMPLETED));
            // Persist the match_score row directly — saveMatch only writes the match aggregate.
            MatchScoreJpaEntity score = new MatchScoreJpaEntity();
            score.setMatchId(match.getId());
            score.setCurrentSet(1);
            score.setServingPlayer(1);
            matchScoreRepo.save(score);
            for (int j = 1; j <= pointsPerMatch; j++) {
                savePoint.savePoint(new Point(null, match.getId(), 1, 1, j, 1,
                        PointType.WINNER, StrokeType.FOREHAND, Direction.CROSS_COURT, 1, false, null, 1));
            }
            if (addAnalysis) {
                MatchAnalysisJpaEntity analysis = new MatchAnalysisJpaEntity();
                analysis.setMatchId(match.getId());
                analysis.setStatus(com.cas.tsas.ai.domain.model.AnalysisStatus.COMPLETED);
                analysis.setOwnStrengths("ok");
                analysis.setOwnWeaknesses("ok");
                analysis.setOpponentStrengths("ok");
                analysis.setOpponentWeaknesses("ok");
                analysis.setKeyMoments("ok");
                analysis.setRecommendationsJson("[]");
                analysis.setModelUsed("fake-llm");
                analysis.setGeneratedAt(java.time.Instant.now());
                analysisRepo.save(analysis);
            }
        }
    }
}
