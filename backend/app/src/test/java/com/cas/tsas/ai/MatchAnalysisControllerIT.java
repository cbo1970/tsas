package com.cas.tsas.ai;

import com.cas.tsas.AbstractIntegrationTest;
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
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
        mockMvc.perform(post("/api/matches/{id}/analysis", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_returns404IfAnalysisNotGenerated() throws Exception {
        UUID matchId = createMatch(MatchStatus.COMPLETED, 15);
        mockMvc.perform(get("/api/matches/{id}/analysis", matchId))
                .andExpect(status().isNotFound());
    }

    @Test
    void post_generatesAndPersists_thenGetReturnsSameAnalysis() throws Exception {
        UUID matchId = createMatch(MatchStatus.COMPLETED, 15);

        mockMvc.perform(post("/api/matches/{id}/analysis", matchId))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/matches/" + matchId + "/analysis")))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.modelUsed").value("fake-llm"))
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.recommendations[0].title").exists());

        mockMvc.perform(get("/api/matches/{id}/analysis", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.modelUsed").value("fake-llm"));
    }

    @Test
    void post_returns409IfMatchNotCompleted() throws Exception {
        UUID matchId = createMatch(MatchStatus.IN_PROGRESS, 15);
        mockMvc.perform(post("/api/matches/{id}/analysis", matchId))
                .andExpect(status().isConflict());
    }

    @Test
    void post_returns422IfTooFewPoints() throws Exception {
        UUID matchId = createMatch(MatchStatus.COMPLETED, 3);
        mockMvc.perform(post("/api/matches/{id}/analysis", matchId))
                .andExpect(status().isUnprocessableEntity());
    }

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

    private UUID createMatch(MatchStatus status, int pointCount) {
        PlayerJpaEntity p1 = new PlayerJpaEntity();
        p1.setOwnerId(DEFAULT_USER);
        p1.setFirstName("Max"); p1.setLastName("Müller");
        UUID p1Id = playerRepo.save(p1).getId();

        PlayerJpaEntity p2 = new PlayerJpaEntity();
        p2.setOwnerId(DEFAULT_USER);
        p2.setFirstName("Tom"); p2.setLastName("Schmidt");
        UUID p2Id = playerRepo.save(p2).getId();

        Match match = matchAdapter.saveMatch(
                new Match(null, DEFAULT_USER, p1Id, p2Id, 2, false, false, status));
        UUID matchId = match.getId();

        for (int i = 1; i <= pointCount; i++) {
            savePoint.savePoint(new Point(null, matchId, 1, 1, i, 1,
                    PointType.WINNER, StrokeType.FOREHAND, Direction.CROSS_COURT, 1, false, null, 1));
        }
        return matchId;
    }
}
