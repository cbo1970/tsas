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

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MatchPlayerNotesApiIT extends AbstractIntegrationTest {

    @Autowired ObjectMapper objectMapper;
    @Autowired MatchJpaRepository matchRepository;
    @Autowired MatchScoreJpaRepository matchScoreRepository;
    @Autowired PointJpaRepository pointRepository;
    @Autowired PlayerJpaRepository playerRepository;

    private UUID p1;
    private UUID p2;
    private UUID matchId;

    @BeforeEach
    void cleanUp() throws Exception {
        pointRepository.deleteAll();
        matchScoreRepository.deleteAll();
        matchRepository.deleteAll();
        playerRepository.deleteAll();
        p1 = createPlayer();
        p2 = createPlayer();
        matchId = createMatch(p1, p2);
    }

    private UUID createPlayer() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "firstName", "Test", "lastName", "Player",
                "gender", "MALE", "handedness", "RIGHT", "backhandType", "TWO_HANDED"));
        String response = mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private UUID createMatch(UUID a, UUID b) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "player1Id", a, "player2Id", b,
                "setsToWin", 2, "matchTiebreak", false, "shortSet", false));
        String response = mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private String noteBody(String note) throws Exception {
        return objectMapper.writeValueAsString(Collections.singletonMap("note", note));
    }

    @Test
    void put_creates_note_and_get_returns_it() throws Exception {
        mockMvc.perform(put("/api/matches/{id}/notes/{pid}", matchId, p1)
                        .contentType(MediaType.APPLICATION_JSON).content(noteBody("2. Aufschlag zu kurz")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value(p1.toString()))
                .andExpect(jsonPath("$.note").value("2. Aufschlag zu kurz"));

        mockMvc.perform(get("/api/matches/{id}/notes", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].note").value("2. Aufschlag zu kurz"));
    }

    @Test
    void put_overwrites_existing_note() throws Exception {
        mockMvc.perform(put("/api/matches/{id}/notes/{pid}", matchId, p1)
                .contentType(MediaType.APPLICATION_JSON).content(noteBody("alt")));
        mockMvc.perform(put("/api/matches/{id}/notes/{pid}", matchId, p1)
                        .contentType(MediaType.APPLICATION_JSON).content(noteBody("neu")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").value("neu"));

        mockMvc.perform(get("/api/matches/{id}/notes", matchId))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void blank_note_deletes_and_returns_204() throws Exception {
        mockMvc.perform(put("/api/matches/{id}/notes/{pid}", matchId, p1)
                .contentType(MediaType.APPLICATION_JSON).content(noteBody("etwas")));
        mockMvc.perform(put("/api/matches/{id}/notes/{pid}", matchId, p1)
                        .contentType(MediaType.APPLICATION_JSON).content(noteBody("   ")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/matches/{id}/notes", matchId))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void put_with_player_not_in_match_returns_400() throws Exception {
        UUID stranger = createPlayer();
        mockMvc.perform(put("/api/matches/{id}/notes/{pid}", matchId, stranger)
                        .contentType(MediaType.APPLICATION_JSON).content(noteBody("x")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void put_on_unknown_match_returns_404() throws Exception {
        mockMvc.perform(put("/api/matches/{id}/notes/{pid}", UUID.randomUUID(), p1)
                        .contentType(MediaType.APPLICATION_JSON).content(noteBody("x")))
                .andExpect(status().isNotFound());
    }

    @Test
    void put_note_over_2000_chars_returns_400() throws Exception {
        String tooLong = "a".repeat(2001);
        mockMvc.perform(put("/api/matches/{id}/notes/{pid}", matchId, p1)
                        .contentType(MediaType.APPLICATION_JSON).content(noteBody(tooLong)))
                .andExpect(status().isBadRequest());
    }
}
