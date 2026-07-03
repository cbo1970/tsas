package com.cas.tsas.validation;

import com.cas.tsas.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecordPointValidationIT extends AbstractIntegrationTest {

    @Autowired ObjectMapper json;

    private UUID matchId;

    private Map<String, Object> validPlayerBody(String firstName) {
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", firstName);
        body.put("lastName",  "X");
        body.put("gender",    "FEMALE");
        body.put("handedness","RIGHT");
        body.put("backhandType","TWO_HANDED");
        return body;
    }

    @BeforeEach
    void createMatchFixture() throws Exception {
        UUID p1 = createPlayer("Alice");
        UUID p2 = createPlayer("Bob");

        Map<String, Object> matchBody = new HashMap<>();
        matchBody.put("player1Id", p1);
        matchBody.put("player2Id", p2);
        matchBody.put("setsToWin", 2);
        matchBody.put("matchTiebreak", false);
        matchBody.put("shortSet", false);

        MvcResult res = mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(matchBody)))
                .andExpect(status().isCreated())
                .andReturn();
        matchId = UUID.fromString(json.readTree(res.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID createPlayer(String firstName) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(validPlayerBody(firstName))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json.readTree(res.getResponse().getContentAsString()).get("id").asText());
    }

    @Test
    void rejects_invalid_point_type_with_400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("winner", 1);
        body.put("pointType", "NOT_A_REAL_POINT_TYPE");

        mockMvc.perform(post("/api/matches/" + matchId + "/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejects_too_long_remark_with_400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("winner", 1);
        body.put("remark", "X".repeat(501));

        mockMvc.perform(post("/api/matches/" + matchId + "/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejects_winner_out_of_range_with_400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("winner", 3);

        mockMvc.perform(post("/api/matches/" + matchId + "/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejects_missing_winner_with_400() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/matches/" + matchId + "/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void accepts_valid_full_request_with_201() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("winner", 1);
        body.put("pointType", "WINNER");
        body.put("strokeType", "FOREHAND");
        body.put("direction", "CROSS_COURT");
        body.put("remark", "A".repeat(500));
        body.put("serveAttempt", 1);

        mockMvc.perform(post("/api/matches/" + matchId + "/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    @Test
    void accepts_minimal_request_with_null_optionals() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("winner", 1);

        mockMvc.perform(post("/api/matches/" + matchId + "/points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }
}
