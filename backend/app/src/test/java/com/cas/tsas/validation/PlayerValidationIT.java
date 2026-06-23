package com.cas.tsas.validation;

import com.cas.tsas.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlayerValidationIT extends AbstractIntegrationTest {

    @Autowired ObjectMapper json;

    private static Map<String, Object> validPlayerBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", "Alice");
        body.put("lastName",  "Anders");
        body.put("gender",    "FEMALE");
        body.put("handedness","RIGHT");
        body.put("backhandType","TWO_HANDED");
        return body;
    }

    @Test
    void create_rejects_too_long_first_name_with_400() throws Exception {
        Map<String, Object> body = validPlayerBody();
        body.put("firstName", "X".repeat(101));

        mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_rejects_too_long_ranking_with_400() throws Exception {
        Map<String, Object> body = validPlayerBody();
        body.put("ranking", "R".repeat(51));

        mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_rejects_too_long_nationality_with_400() throws Exception {
        Map<String, Object> body = validPlayerBody();
        body.put("nationality", "N".repeat(65));

        mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_rejects_too_long_last_name_with_400() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(validPlayerBody())))
                .andExpect(status().isCreated())
                .andReturn();
        UUID id = UUID.fromString(json.readTree(res.getResponse().getContentAsString()).get("id").asText());

        Map<String, Object> body = validPlayerBody();
        body.put("lastName", "Y".repeat(101));

        mockMvc.perform(put("/api/players/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_accepts_max_length_values() throws Exception {
        Map<String, Object> body = validPlayerBody();
        body.put("firstName", "A".repeat(100));
        body.put("lastName",  "B".repeat(100));
        body.put("ranking",   "R".repeat(50));
        body.put("nationality","N".repeat(64));

        MvcResult res = mockMvc.perform(post("/api/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(res.getResponse().getStatus()).isEqualTo(201);
    }
}
