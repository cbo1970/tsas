package com.cas.tsas.ai;

import com.cas.tsas.AbstractIntegrationTest;
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

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MatchAnalysisControllerIT extends AbstractIntegrationTest {

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
    void post_returns404ForUnknownMatch() throws Exception {
        mockMvc.perform(post("/api/matches/{id}/analysis", UUID.randomUUID()).with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_returns404IfAnalysisNotGenerated() throws Exception {
        UUID matchId = createMatch(MatchStatus.COMPLETED, 15);
        mockMvc.perform(get("/api/matches/{id}/analysis", matchId).with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void post_generatesAndPersists_thenGetReturnsSameAnalysis() throws Exception {
        UUID matchId = createMatch(MatchStatus.COMPLETED, 15);

        mockMvc.perform(post("/api/matches/{id}/analysis", matchId).with(jwt()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/matches/" + matchId + "/analysis")))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.modelUsed").value("fake-llm"))
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.recommendations[0].title").exists());

        mockMvc.perform(get("/api/matches/{id}/analysis", matchId).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.modelUsed").value("fake-llm"));
    }

    @Test
    void post_returns409IfMatchNotCompleted() throws Exception {
        UUID matchId = createMatch(MatchStatus.IN_PROGRESS, 15);
        mockMvc.perform(post("/api/matches/{id}/analysis", matchId).with(jwt()))
                .andExpect(status().isConflict());
    }

    @Test
    void post_returns422IfTooFewPoints() throws Exception {
        UUID matchId = createMatch(MatchStatus.COMPLETED, 3);
        mockMvc.perform(post("/api/matches/{id}/analysis", matchId).with(jwt()))
                .andExpect(status().isUnprocessableEntity());
    }

    private UUID createMatch(MatchStatus status, int pointCount) {
        PlayerJpaEntity p1 = new PlayerJpaEntity();
        p1.setFirstName("Max"); p1.setLastName("Müller");
        UUID p1Id = playerRepo.save(p1).getId();

        PlayerJpaEntity p2 = new PlayerJpaEntity();
        p2.setFirstName("Tom"); p2.setLastName("Schmidt");
        UUID p2Id = playerRepo.save(p2).getId();

        Match match = matchAdapter.saveMatch(
                new Match(null, p1Id, p2Id, 2, false, false, status));
        UUID matchId = match.getId();

        for (int i = 1; i <= pointCount; i++) {
            savePoint.savePoint(new Point(null, matchId, 1, 1, i, 1,
                    PointType.WINNER, StrokeType.FOREHAND, Direction.CROSS_COURT, 1, false, null, 1));
        }
        return matchId;
    }
}
