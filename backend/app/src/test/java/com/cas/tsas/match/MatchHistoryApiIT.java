package com.cas.tsas.match;

import com.cas.tsas.AbstractIntegrationTest;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchScoreJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.PointJpaRepository;
import com.cas.tsas.player.infrastructure.persistence.repository.PlayerJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MatchHistoryApiIT extends AbstractIntegrationTest {

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

    private UUID createPlayer(String last) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "firstName", "T", "lastName", last,
                "gender", "MALE", "handedness", "RIGHT", "backhandType", "TWO_HANDED"));
        String r = mockMvc.perform(post("/api/players").contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(r).get("id").asText());
    }

    private UUID createMatch(UUID p1, UUID p2) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "player1Id", p1, "player2Id", p2, "setsToWin", 2, "matchTiebreak", false, "shortSet", false));
        String r = mockMvc.perform(post("/api/matches").contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(r).get("id").asText());
    }

    @Test
    void returns_completed_matches_newest_first_with_opponent_and_result() throws Exception {
        UUID self = createPlayer("Self");
        UUID opp1 = createPlayer("Opp1");
        UUID opp2 = createPlayer("Opp2");
        UUID m1 = createMatch(self, opp1);
        mockMvc.perform(post("/api/matches/{id}/end", m1)).andExpect(status().isOk());
        UUID m2 = createMatch(self, opp2);
        mockMvc.perform(post("/api/matches/{id}/end", m2)).andExpect(status().isOk());
        // Ein laufendes Match darf NICHT erscheinen
        createMatch(self, createPlayer("Opp3"));

        mockMvc.perform(get("/api/players/{id}/matches", self))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].matchId").value(m2.toString())) // neueste zuerst
                .andExpect(jsonPath("$[0].opponentName").value("T Opp2"))
                .andExpect(jsonPath("$[1].matchId").value(m1.toString()));
    }

    @Test
    void player_without_matches_returns_empty() throws Exception {
        UUID solo = createPlayer("Solo");
        mockMvc.perform(get("/api/players/{id}/matches", solo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unknown_player_returns_404() throws Exception {
        mockMvc.perform(get("/api/players/{id}/matches", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
