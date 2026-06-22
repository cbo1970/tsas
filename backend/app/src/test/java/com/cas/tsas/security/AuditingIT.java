package com.cas.tsas.security;

import com.cas.tsas.AbstractIntegrationTest;
import com.cas.tsas.auth.domain.Role;
import com.cas.tsas.auth.testsupport.JwtTestSupport;
import com.cas.tsas.player.infrastructure.persistence.entity.PlayerJpaEntity;
import com.cas.tsas.player.infrastructure.persistence.repository.PlayerJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditingIT extends AbstractIntegrationTest {

    private static final UUID USER_A = UUID.randomUUID();
    private static final UUID ADMIN  = UUID.randomUUID();

    @Autowired ObjectMapper json;
    @Autowired PlayerJpaRepository playerRepo;

    @Test
    void create_sets_created_and_updated_for_owner() throws Exception {
        Instant before = Instant.now().minusSeconds(2);

        String body = json.writeValueAsString(Map.of(
                "firstName", "Alice", "lastName", "A",
                "gender", "FEMALE", "handedness", "RIGHT", "backhandType", "TWO_HANDED"));
        MvcResult res = mockMvc.perform(post("/api/players")
                        .with(JwtTestSupport.withUser(USER_A, Role.COACH))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        UUID id = UUID.fromString(json.readTree(res.getResponse().getContentAsString()).get("id").asText());

        PlayerJpaEntity stored = playerRepo.findById(id).orElseThrow();

        assertThat(stored.getCreatedBy()).isEqualTo(USER_A);
        assertThat(stored.getUpdatedBy()).isEqualTo(USER_A);
        assertThat(stored.getCreatedAt()).isAfter(before);
        assertThat(stored.getUpdatedAt()).isAfter(before);
        assertThat(stored.getCreatedAt()).isEqualTo(stored.getUpdatedAt());
    }

    @Test
    void admin_update_preserves_created_by_and_sets_updated_by() throws Exception {
        // create as USER_A
        String createBody = json.writeValueAsString(Map.of(
                "firstName", "Bob", "lastName", "B",
                "gender", "MALE", "handedness", "RIGHT", "backhandType", "TWO_HANDED"));
        MvcResult res = mockMvc.perform(post("/api/players")
                        .with(JwtTestSupport.withUser(USER_A, Role.COACH))
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode createdJson = json.readTree(res.getResponse().getContentAsString());
        UUID id = UUID.fromString(createdJson.get("id").asText());
        Instant initialCreatedAt = playerRepo.findById(id).orElseThrow().getCreatedAt();

        // Force a clock gap so updated_at strictly increases
        Thread.sleep(50);

        // update as ADMIN
        String updateBody = json.writeValueAsString(Map.of(
                "firstName", "Bobby", "lastName", "B",
                "gender", "MALE", "handedness", "RIGHT", "backhandType", "TWO_HANDED"));
        mockMvc.perform(put("/api/players/" + id)
                        .with(JwtTestSupport.withUser(ADMIN, Role.COACH, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk());

        PlayerJpaEntity stored = playerRepo.findById(id).orElseThrow();
        assertThat(stored.getCreatedBy()).isEqualTo(USER_A);
        assertThat(stored.getCreatedAt()).isEqualTo(initialCreatedAt);
        assertThat(stored.getUpdatedBy()).isEqualTo(ADMIN);
        assertThat(stored.getUpdatedAt()).isAfter(initialCreatedAt);
    }
}
