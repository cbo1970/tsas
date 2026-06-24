package com.cas.tsas.ai;

import com.cas.tsas.AbstractIntegrationTest;
import com.cas.tsas.ai.infrastructure.web.AnalysisRateLimitProperties;
import com.cas.tsas.auth.domain.Role;
import com.cas.tsas.auth.testsupport.JwtTestSupport;
import com.cas.tsas.match.application.port.out.SavePointPort;
import com.cas.tsas.match.domain.model.Direction;
import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.match.domain.model.Point;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.match.domain.model.StrokeType;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchPersistenceAdapter;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchScoreJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.PointJpaRepository;
import com.cas.tsas.player.infrastructure.persistence.entity.PlayerJpaEntity;
import com.cas.tsas.player.infrastructure.persistence.repository.PlayerJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.UUID;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the per-user token bucket on POST /api/matches/{id}/analysis.
 * Replaces the auto-bound AnalysisRateLimitProperties bean with a tight one
 * (2/min, 2/day) — overriding the bean is reliable, whereas property-source
 * precedence between AbstractIntegrationTest's @DynamicPropertySource and a
 * subclass override is unspecified.
 */
@Import(MatchAnalysisRateLimitIT.TightRateLimitConfig.class)
class MatchAnalysisRateLimitIT extends AbstractIntegrationTest {

    /** Distinct users per test so the in-memory bucket of one test does not leak into another. */
    private static final UUID BURST_USER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID GET_USER   = UUID.fromString("00000000-0000-0000-0000-0000000000bb");

    @TestConfiguration
    static class TightRateLimitConfig {
        @Bean
        @Primary
        AnalysisRateLimitProperties tightRateLimit() {
            return new AnalysisRateLimitProperties(2, 2);
        }
    }

    @Autowired MatchPersistenceAdapter matchAdapter;
    @Autowired SavePointPort savePoint;
    @Autowired PlayerJpaRepository playerRepo;
    @Autowired MatchJpaRepository matchRepo;
    @Autowired MatchScoreJpaRepository matchScoreRepo;
    @Autowired PointJpaRepository pointRepo;

    @BeforeEach
    void cleanUp() {
        pointRepo.deleteAll();
        matchScoreRepo.deleteAll();
        matchRepo.deleteAll();
        playerRepo.deleteAll();
    }

    @Test
    void post_returns429AfterBurstExceedsBucket() throws Exception {
        UUID matchId = createMatch();

        // First two POSTs succeed (bucket size 2).
        mockMvc.perform(post("/api/matches/{id}/analysis", matchId)
                        .with(JwtTestSupport.withUser(BURST_USER, Role.COACH)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-RateLimit-Limit", "2"))
                .andExpect(header().string("X-RateLimit-Remaining", matchesPattern("^\\d+$")));

        mockMvc.perform(post("/api/matches/{id}/analysis", matchId)
                        .with(JwtTestSupport.withUser(BURST_USER, Role.COACH)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-RateLimit-Remaining", matchesPattern("^\\d+$")));

        // Third POST exhausts the bucket → 429 with Retry-After and ProblemDetail body.
        mockMvc.perform(post("/api/matches/{id}/analysis", matchId)
                        .with(JwtTestSupport.withUser(BURST_USER, Role.COACH)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-RateLimit-Limit", "2"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(header().string("Retry-After", matchesPattern("^\\d+$")))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("Too Many Requests"));
    }

    @Test
    void get_isNotRateLimited() throws Exception {
        UUID matchId = createMatch(GET_USER);

        // Generate once (uses 1 of 2 tokens for GET_USER).
        mockMvc.perform(post("/api/matches/{id}/analysis", matchId)
                        .with(JwtTestSupport.withUser(GET_USER, Role.COACH)))
                .andExpect(status().isCreated());

        // Many GETs do not consume bucket tokens — would 429 otherwise.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/matches/{id}/analysis", matchId)
                            .with(JwtTestSupport.withUser(GET_USER, Role.COACH)))
                    .andExpect(status().isOk());
        }
    }

    private UUID createMatch() {
        return createMatch(BURST_USER);
    }

    private UUID createMatch(UUID owner) {
        PlayerJpaEntity p1 = new PlayerJpaEntity();
        p1.setOwnerId(owner);
        p1.setFirstName("Max"); p1.setLastName("Müller");
        UUID p1Id = playerRepo.save(p1).getId();

        PlayerJpaEntity p2 = new PlayerJpaEntity();
        p2.setOwnerId(owner);
        p2.setFirstName("Tom"); p2.setLastName("Schmidt");
        UUID p2Id = playerRepo.save(p2).getId();

        Match match = matchAdapter.saveMatch(
                new Match(null, owner, p1Id, p2Id, 2, false, false, MatchStatus.COMPLETED));
        UUID matchId = match.getId();

        for (int i = 1; i <= 15; i++) {
            savePoint.savePoint(new Point(null, matchId, 1, 1, i, 1,
                    PointType.WINNER, StrokeType.FOREHAND, Direction.CROSS_COURT, 1, false, null, 1));
        }
        return matchId;
    }
}
