package com.cas.tsas.player;

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

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PlayerApiIT extends AbstractIntegrationTest {

    @Autowired ObjectMapper objectMapper;
    @Autowired PlayerJpaRepository playerRepository;
    @Autowired MatchJpaRepository matchRepository;
    @Autowired MatchScoreJpaRepository matchScoreRepository;

    @BeforeEach
    void cleanUp() {
        matchScoreRepository.deleteAll();
        matchRepository.deleteAll();
        playerRepository.deleteAll();
    }

    private UUID createPlayer(String firstName) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "firstName", firstName, "lastName", "Muster",
                "gender", "MALE", "handedness", "RIGHT", "backhandType", "TWO_HANDED"
        ));
        String response = mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    // =========================================================================
    @Nested
    class CreatePlayer {

        @Test
        void returns_201_with_player_data() throws Exception {
            mockMvc.perform(post("/api/players")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "firstName", "Max", "lastName", "Muster",
                                    "gender", "MALE", "handedness", "RIGHT", "backhandType", "TWO_HANDED"
                            ))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.firstName").value("Max"))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        void returns_400_when_required_fields_missing() throws Exception {
            mockMvc.perform(post("/api/players")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    @Nested
    class GetPlayer {

        @Test
        void returns_200_with_player() throws Exception {
            UUID id = createPlayer("Max");

            mockMvc.perform(get("/api/players/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.firstName").value("Max"));
        }

        @Test
        void returns_404_when_not_found() throws Exception {
            mockMvc.perform(get("/api/players/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    @Nested
    class ListPlayers {

        @Test
        void returns_all_players() throws Exception {
            createPlayer("Max");
            createPlayer("Anna");

            mockMvc.perform(get("/api/players"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }
    }

    // =========================================================================
    @Nested
    class ActiveMatchIndicator {

        @Test
        void activeMatchId_is_set_when_player_is_in_active_match() throws Exception {
            UUID playerId = createPlayer("Max");
            UUID player2Id = createPlayer("Anna");

            var match = new com.cas.tsas.match.infrastructure.persistence.entity.MatchJpaEntity();
            match.setOwnerId(DEFAULT_USER);
            match.setPlayer1Id(playerId);
            match.setPlayer2Id(player2Id);
            match.setSetsToWin(2);
            match.setMatchTiebreak(false);
            match.setShortSet(false);
            match.setStatus(com.cas.tsas.match.domain.model.MatchStatus.IN_PROGRESS);
            var savedMatch = matchRepository.save(match);

            mockMvc.perform(get("/api/players"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].activeMatchId").value(savedMatch.getId().toString()));
        }

        @Test
        void activeMatchId_is_null_when_player_has_no_active_match() throws Exception {
            createPlayer("Max");

            mockMvc.perform(get("/api/players"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].activeMatchId").isEmpty());
        }

        @Test
        void activeMatchId_is_null_when_players_match_is_completed() throws Exception {
            UUID playerId = createPlayer("Max");
            UUID player2Id = createPlayer("Anna");

            var match = new com.cas.tsas.match.infrastructure.persistence.entity.MatchJpaEntity();
            match.setOwnerId(DEFAULT_USER);
            match.setPlayer1Id(playerId);
            match.setPlayer2Id(player2Id);
            match.setSetsToWin(2);
            match.setMatchTiebreak(false);
            match.setShortSet(false);
            match.setStatus(com.cas.tsas.match.domain.model.MatchStatus.COMPLETED);
            matchRepository.save(match);

            mockMvc.perform(get("/api/players"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].activeMatchId").isEmpty());
        }
    }

    // =========================================================================
    @Nested
    class UpdatePlayer {

        @Test
        void returns_200_with_updated_fields() throws Exception {
            UUID id = createPlayer("Max");

            mockMvc.perform(put("/api/players/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "firstName", "Anna", "lastName", "Müller",
                                    "gender", "FEMALE", "handedness", "LEFT", "backhandType", "ONE_HANDED"
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Anna"))
                    .andExpect(jsonPath("$.gender").value("FEMALE"));
        }
    }

    // =========================================================================
    @Nested
    class DeletePlayer {

        @Test
        void returns_204_and_player_no_longer_exists() throws Exception {
            UUID id = createPlayer("Max");

            mockMvc.perform(delete("/api/players/{id}", id))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/players/{id}", id))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    @Nested
    class DeactivatePlayer {

        @Test
        void returns_204() throws Exception {
            UUID id = createPlayer("Max");

            mockMvc.perform(patch("/api/players/{id}/deactivate", id))
                    .andExpect(status().isNoContent());
        }
    }
}
