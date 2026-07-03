package com.cas.tsas.statistics;

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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatisticsApiIT extends AbstractIntegrationTest {

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

    private void recordPoint(UUID matchId, int winner, String pointType) throws Exception {
        var body = new HashMap<String, Object>();
        body.put("winner", winner);
        if (pointType != null) {
            body.put("pointType", pointType);
        }
        mockMvc.perform(post("/api/matches/{id}/points", matchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    @Nested
    class GetStatistics {

        @Test
        void returns_statistics_for_match_with_points() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            // ACE requires a serving player to be set first
            mockMvc.perform(post("/api/matches/{id}/serve/player1", matchId))
                    .andExpect(status().isOk());

            // ACE: player 1 wins, attributed to player 1
            recordPoint(matchId, 1, "ACE");
            // WINNER: player 1 wins, attributed to player 1
            recordPoint(matchId, 1, "WINNER");
            // UNFORCED_ERROR: player 1 wins the point, but the error is attributed to player 2
            recordPoint(matchId, 1, "UNFORCED_ERROR");
            // Quick-point (null pointType): player 2 wins, only pointsWon counted
            recordPoint(matchId, 2, null);

            mockMvc.perform(get("/api/matches/{id}/statistics", matchId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matchId").value(matchId.toString()))
                    .andExpect(jsonPath("$.totalPoints").value(4))
                    .andExpect(jsonPath("$.player1.pointsWon").value(3))
                    .andExpect(jsonPath("$.player2.pointsWon").value(1))
                    .andExpect(jsonPath("$.player1.aces").value(1))
                    .andExpect(jsonPath("$.player1.winners").value(1))
                    .andExpect(jsonPath("$.player2.unforcedErrors").value(1))
                    .andExpect(jsonPath("$.sets[0].setNumber").value(1));
        }

        @Test
        void returns_per_set_breakdown() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);
            mockMvc.perform(post("/api/matches/{id}/serve/player1", matchId)).andExpect(status().isOk());
            recordPoint(matchId, 1, "ACE");
            recordPoint(matchId, 1, "WINNER");

            mockMvc.perform(get("/api/matches/{id}/statistics", matchId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sets.length()").value(1))
                    .andExpect(jsonPath("$.sets[0].setNumber").value(1))
                    .andExpect(jsonPath("$.sets[0].totalPoints").value(2))
                    .andExpect(jsonPath("$.sets[0].player1.aces").value(1))
                    .andExpect(jsonPath("$.sets[0].player1.winners").value(1));
        }

        @Test
        void returns_404_for_unknown_match() throws Exception {
            mockMvc.perform(get("/api/matches/{id}/statistics", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }
}
