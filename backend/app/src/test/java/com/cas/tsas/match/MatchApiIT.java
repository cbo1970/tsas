package com.cas.tsas.match;

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

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MatchApiIT extends AbstractIntegrationTest {

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

        @Test
        void returns_409_when_player1_already_has_active_match() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();
            UUID p3 = createPlayer();
            createMatch(p1, p2);

            mockMvc.perform(post("/api/matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "player1Id", p1, "player2Id", p3,
                                    "setsToWin", 2, "matchTiebreak", false, "shortSet", false
                            ))))
                    .andExpect(status().isConflict());
        }

        @Test
        void returns_409_when_player2_already_has_active_match() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();
            UUID p3 = createPlayer();
            createMatch(p1, p2);

            mockMvc.perform(post("/api/matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "player1Id", p3, "player2Id", p2,
                                    "setsToWin", 2, "matchTiebreak", false, "shortSet", false
                            ))))
                    .andExpect(status().isConflict());
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
            UUID p3 = createPlayer();
            UUID p4 = createPlayer();
            createMatch(p1, p2);
            createMatch(p3, p4);

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

            mockMvc.perform(post("/api/matches/{id}/points", matchId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "winner", 1,
                                    "pointType", "WINNER",
                                    "strokeType", "FOREHAND",
                                    "direction", "CROSS_COURT"
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pointsPlayer1").value(1));
        }

        @Test
        void returns_409_when_match_already_completed() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(post("/api/matches/{id}/end", matchId));

            mockMvc.perform(post("/api/matches/{id}/points", matchId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "winner", 1,
                                    "pointType", "WINNER"
                            ))))
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

    // =========================================================================
    @Nested
    class EndMatchWalkover {

        @Test
        void player1_wins_sets_status_completed_and_winner() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(post("/api/matches/{id}/end/walkover", matchId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("winner", "PLAYER1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));

            mockMvc.perform(get("/api/matches/{id}", matchId))
                    .andExpect(jsonPath("$.score.winner").value("PLAYER1"))
                    .andExpect(jsonPath("$.score.isDone").value(true));
        }

        @Test
        void player2_wins_sets_winner_to_player2() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(post("/api/matches/{id}/end/walkover", matchId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("winner", "PLAYER2"))))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/matches/{id}", matchId))
                    .andExpect(jsonPath("$.score.winner").value("PLAYER2"));
        }

        @Test
        void returns_400_for_invalid_winner_value() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(post("/api/matches/{id}/end/walkover", matchId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("winner", "INVALID"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void returns_404_for_unknown_match() throws Exception {
            mockMvc.perform(post("/api/matches/{id}/end/walkover", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("winner", "PLAYER1"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        void returns_409_when_match_already_completed() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(post("/api/matches/{id}/end", matchId));

            mockMvc.perform(post("/api/matches/{id}/end/walkover", matchId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("winner", "PLAYER1"))))
                    .andExpect(status().isConflict());
        }
    }

    // =========================================================================
    @Nested
    class RecordAce {

        @Test
        void acePlayer1_incrementsAceCounterAndScoresPoint() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(post("/api/matches/{id}/serve/player1", matchId)); // player1 serves

            mockMvc.perform(post("/api/matches/{id}/points", matchId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "winner", 1,
                                    "pointType", "ACE"
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.acesPlayer1").value(1))
                    .andExpect(jsonPath("$.acesPlayer2").value(0))
                    .andExpect(jsonPath("$.pointsPlayer1").value(1));
        }

        @Test
        void returns_409_when_match_already_completed() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(post("/api/matches/{id}/end", matchId));

            mockMvc.perform(post("/api/matches/{id}/points", matchId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "winner", 1,
                                    "pointType", "ACE"
                            ))))
                    .andExpect(status().isConflict());
        }

        @Test
        void acePlayer2_incrementsAceCounterForPlayer2() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(post("/api/matches/{id}/serve/player2", matchId)); // player2 serves

            mockMvc.perform(post("/api/matches/{id}/points", matchId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "winner", 2,
                                    "pointType", "ACE"
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.acesPlayer2").value(1))
                    .andExpect(jsonPath("$.acesPlayer1").value(0));
        }
    }

    // =========================================================================
    @Nested
    class SetServingPlayer {

        @Test
        void sets_serving_player_1() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(post("/api/matches/{id}/serve/player1", matchId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.servingPlayer").value(1));
        }

        @Test
        void sets_serving_player_2() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(post("/api/matches/{id}/serve/player2", matchId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.servingPlayer").value(2));
        }

        @Test
        void returns_409_when_match_already_completed() throws Exception {
            UUID p1 = createPlayer();
            UUID p2 = createPlayer();
            UUID matchId = createMatch(p1, p2);

            mockMvc.perform(post("/api/matches/{id}/end", matchId));

            mockMvc.perform(post("/api/matches/{id}/serve/player1", matchId))
                    .andExpect(status().isConflict());
        }
    }
}
