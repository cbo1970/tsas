package com.cas.tsas.security;

import com.cas.tsas.AbstractIntegrationTest;
import com.cas.tsas.ai.infrastructure.persistence.repository.MatchAnalysisJpaRepository;
import com.cas.tsas.auth.domain.Role;
import com.cas.tsas.auth.testsupport.JwtTestSupport;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchScoreJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.PointJpaRepository;
import com.cas.tsas.player.infrastructure.persistence.repository.PlayerJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end cross-tenant ownership checks for the public REST API (TEN-55).
 *
 * <p>Each test uses {@link JwtTestSupport#withUser(UUID, Role...)} to mint a JWT with
 * a specific {@code sub} claim and realm role. Two coach identities ({@link #USER_A}
 * and {@link #USER_B}) plus an admin identity ({@link #ADMIN}) exercise the
 * owner-bound resources across {@code /api/players}, {@code /api/matches} and
 * {@code /api/matches/{id}/analysis}.
 *
 * <p>Expected behaviour: a coach must never see, mutate or delete another coach's
 * resources (404); a coach with the {@link Role#ADMIN} role bypasses the filter and
 * can read everything.
 */
class OwnershipIntegrationTest extends AbstractIntegrationTest {

    private static final UUID USER_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USER_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID ADMIN = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Autowired ObjectMapper json;

    @Autowired PlayerJpaRepository playerRepo;
    @Autowired MatchJpaRepository matchRepo;
    @Autowired MatchScoreJpaRepository matchScoreRepo;
    @Autowired PointJpaRepository pointRepo;
    @Autowired MatchAnalysisJpaRepository analysisRepo;

    @BeforeEach
    void cleanUp() {
        analysisRepo.deleteAll();
        pointRepo.deleteAll();
        matchScoreRepo.deleteAll();
        matchRepo.deleteAll();
        playerRepo.deleteAll();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private RequestPostProcessor coach(UUID user) {
        return JwtTestSupport.withUser(user, Role.COACH);
    }

    private RequestPostProcessor admin(UUID user) {
        return JwtTestSupport.withUser(user, Role.ADMIN);
    }

    private UUID createPlayerAs(UUID user, String firstName) throws Exception {
        String body = json.writeValueAsString(Map.of(
                "firstName", firstName,
                "lastName", "Tester",
                "gender", "MALE",
                "handedness", "RIGHT",
                "backhandType", "TWO_HANDED"
        ));
        String response = mockMvc.perform(jsonPost("/api/players", body).with(coach(user)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(response).get("id").asText());
    }

    private UUID createMatchAs(UUID user) throws Exception {
        UUID p1 = createPlayerAs(user, "P1");
        UUID p2 = createPlayerAs(user, "P2");
        String body = json.writeValueAsString(Map.of(
                "player1Id", p1, "player2Id", p2,
                "setsToWin", 2, "matchTiebreak", false, "shortSet", false
        ));
        String response = mockMvc.perform(jsonPost("/api/matches", body).with(coach(user)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(response).get("id").asText());
    }

    private MockHttpServletRequestBuilder jsonPost(String url, String body) {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockHttpServletRequestBuilder jsonPut(String url, String body) {
        return put(url).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    // -------------------------------------------------------------------------
    // Player cross-tenant
    // -------------------------------------------------------------------------

    @Test
    void user_A_cannot_read_user_B_player() throws Exception {
        UUID playerId = createPlayerAs(USER_B, "Bob");

        mockMvc.perform(get("/api/players/{id}", playerId).with(coach(USER_A)))
                .andExpect(status().isNotFound());
    }

    @Test
    void user_A_cannot_update_user_B_player() throws Exception {
        UUID playerId = createPlayerAs(USER_B, "Bob");

        String body = json.writeValueAsString(Map.of(
                "firstName", "Hijack", "lastName", "Tester",
                "gender", "MALE", "handedness", "RIGHT", "backhandType", "TWO_HANDED"
        ));
        mockMvc.perform(jsonPut("/api/players/" + playerId, body).with(coach(USER_A)))
                .andExpect(status().isNotFound());
    }

    @Test
    void user_A_cannot_delete_user_B_player() throws Exception {
        UUID playerId = createPlayerAs(USER_B, "Bob");

        mockMvc.perform(delete("/api/players/{id}", playerId).with(coach(USER_A)))
                .andExpect(status().isNotFound());

        // Confirm the resource still exists for its owner.
        mockMvc.perform(get("/api/players/{id}", playerId).with(coach(USER_B)))
                .andExpect(status().isOk());
    }

    @Test
    void list_players_filters_to_own() throws Exception {
        createPlayerAs(USER_A, "Alice");
        createPlayerAs(USER_B, "Bob");

        mockMvc.perform(get("/api/players").with(coach(USER_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Alice"));
    }

    @Test
    void admin_lists_all_players() throws Exception {
        createPlayerAs(USER_A, "Alice");
        createPlayerAs(USER_B, "Bob");

        mockMvc.perform(get("/api/players").with(admin(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void admin_can_read_any_player() throws Exception {
        UUID playerId = createPlayerAs(USER_A, "Alice");

        mockMvc.perform(get("/api/players/{id}", playerId).with(admin(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(playerId.toString()));
    }

    // -------------------------------------------------------------------------
    // Match score / state cross-tenant
    // -------------------------------------------------------------------------

    @Test
    void user_A_cannot_read_user_B_match() throws Exception {
        UUID matchId = createMatchAs(USER_B);

        mockMvc.perform(get("/api/matches/{id}", matchId).with(coach(USER_A)))
                .andExpect(status().isNotFound());
    }

    @Test
    void user_A_cannot_record_point_on_user_B_match() throws Exception {
        UUID matchId = createMatchAs(USER_B);

        String body = json.writeValueAsString(Map.of(
                "winner", 1,
                "pointType", "WINNER",
                "strokeType", "FOREHAND",
                "direction", "CROSS_COURT"
        ));
        mockMvc.perform(jsonPost("/api/matches/" + matchId + "/points", body).with(coach(USER_A)))
                .andExpect(status().isNotFound());
    }

    @Test
    void user_A_cannot_update_user_B_match_score() throws Exception {
        UUID matchId = createMatchAs(USER_B);

        String body = json.writeValueAsString(Map.of(
                "pointsPlayer1", 0,
                "pointsPlayer2", 0,
                "gamesPlayer1", 1,
                "gamesPlayer2", 0,
                "setsPlayer1", 0,
                "setsPlayer2", 0,
                "isDeuce", false,
                "currentSet", 1,
                "isDone", false
        ));
        mockMvc.perform(jsonPut("/api/matches/" + matchId + "/score", body).with(coach(USER_A)))
                .andExpect(status().isNotFound());
    }

    @Test
    void user_A_cannot_end_user_B_match() throws Exception {
        UUID matchId = createMatchAs(USER_B);

        mockMvc.perform(post("/api/matches/{id}/end", matchId).with(coach(USER_A)))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // AI analysis cross-tenant
    // -------------------------------------------------------------------------

    @Test
    void user_A_cannot_trigger_analysis_on_user_B_match() throws Exception {
        UUID matchId = createMatchAs(USER_B);

        mockMvc.perform(post("/api/matches/{id}/analysis", matchId).with(coach(USER_A)))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // Head-to-head statistics cross-tenant
    // -------------------------------------------------------------------------

    @Test
    void user_A_cannot_compute_h2h_between_user_B_players() throws Exception {
        UUID p1 = createPlayerAs(USER_B, "Bob1");
        UUID p2 = createPlayerAs(USER_B, "Bob2");

        mockMvc.perform(get("/api/statistics/head-to-head")
                        .param("player1", p1.toString())
                        .param("player2", p2.toString())
                        .with(coach(USER_A)))
                .andExpect(status().isNotFound());
    }
}
