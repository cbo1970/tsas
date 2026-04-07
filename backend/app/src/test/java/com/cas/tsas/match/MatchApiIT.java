package com.cas.tsas.match;

import com.cas.tsas.AbstractIntegrationTest;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchScoreJpaRepository;
import com.cas.tsas.player.infrastructure.persistence.repository.PlayerJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MatchApiIT extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MatchJpaRepository matchRepository;
    @Autowired MatchScoreJpaRepository matchScoreRepository;
    @Autowired PlayerJpaRepository playerRepository;

    @BeforeEach
    void cleanUp() {
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

    // =========================================================================
    @Nested
    class CreateMatch {

        @Test
        void returns_201_with_match_in_progress() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();

            mockMvc.perform(post("/api/matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "player1Id", p1, "player2Id", p2,
                                    "setsToWin", 2, "matchTiebreak", false, "shortSet", false
                            ))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        @Test
        void returns_404_when_player_not_found() throws Exception {
            mockMvc.perform(post("/api/matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "player1Id", UUID.randomUUID(),
                                    "player2Id", UUID.randomUUID(),
                                    "setsToWin", 2, "matchTiebreak", false, "shortSet", false
                            ))))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    @Nested
    class GetMatch {

        @Test
        void returns_match_with_initial_score() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(get("/api/matches/{id}", matchId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(matchId.toString()))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.score.pointsPlayer1").value(0));
        }

        @Test
        void returns_404_when_match_not_found() throws Exception {
            mockMvc.perform(get("/api/matches/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    @Nested
    class ListMatches {

        @Test
        void returns_all_matches() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();
            createMatch(p1, p2);
            createMatch(p1, p2);

            mockMvc.perform(get("/api/matches"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }
    }

    // =========================================================================
    @Nested
    class RecordPoint {

        @Test
        void increments_player1_score() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(post("/api/matches/{id}/score/player1", matchId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pointsPlayer1").value(1));
        }

        @Test
        void returns_409_when_match_already_completed() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(post("/api/matches/{id}/end", matchId));

            mockMvc.perform(post("/api/matches/{id}/score/player1", matchId))
                    .andExpect(status().isConflict());
        }
    }

    // =========================================================================
    @Nested
    class EndMatch {

        @Test
        void sets_status_to_completed() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(post("/api/matches/{id}/end", matchId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }
    }
}
