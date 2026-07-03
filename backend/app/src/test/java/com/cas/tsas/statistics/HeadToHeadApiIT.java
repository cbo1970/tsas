package com.cas.tsas.statistics;

import com.cas.tsas.AbstractIntegrationTest;
import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.match.infrastructure.persistence.entity.MatchJpaEntity;
import com.cas.tsas.match.infrastructure.persistence.entity.MatchScoreJpaEntity;
import com.cas.tsas.match.infrastructure.persistence.entity.PointJpaEntity;
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

class HeadToHeadApiIT extends AbstractIntegrationTest {

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

    private UUID seedMatch(UUID p1, UUID p2) {
        MatchJpaEntity m = new MatchJpaEntity();
        m.setOwnerId(DEFAULT_USER);
        m.setPlayer1Id(p1);
        m.setPlayer2Id(p2);
        m.setSetsToWin(2);
        m.setMatchTiebreak(false);
        m.setShortSet(false);
        m.setStatus(MatchStatus.COMPLETED);
        return matchRepository.save(m).getId();
    }

    private void seedScore(UUID matchId, String winner, int sets1, int sets2) {
        MatchScoreJpaEntity s = new MatchScoreJpaEntity();
        s.setMatchId(matchId);
        s.setSetsPlayer1(sets1);
        s.setSetsPlayer2(sets2);
        s.setDone(true);
        s.setWinner(winner);
        matchScoreRepository.save(s);
    }

    private void seedPoint(UUID matchId, int set, int game, int pointNo, int winner,
                           PointType type, Integer server, Integer serveAttempt) {
        PointJpaEntity p = new PointJpaEntity();
        p.setMatchId(matchId);
        p.setSetNumber(set);
        p.setGameNumber(game);
        p.setPointNumber(pointNo);
        p.setWinner(winner);
        p.setPointType(type);
        p.setServingPlayer(server);
        p.setServeAttempt(serveAttempt);
        p.setBreakPoint(false);
        pointRepository.save(p);
    }

    @Test
    void aggregates_head_to_head_across_two_matches_with_swapped_positions() throws Exception {
        UUID alice = createPlayer();
        UUID bob = createPlayer();

        UUID m1 = seedMatch(alice, bob);
        seedScore(m1, "PLAYER1", 2, 0);
        seedPoint(m1, 1, 1, 1, 1, PointType.WINNER, 1, 1);

        UUID m2 = seedMatch(bob, alice);
        seedScore(m2, "PLAYER1", 2, 1);
        seedPoint(m2, 1, 1, 1, 2, PointType.WINNER, 2, 1);

        mockMvc.perform(get("/api/statistics/head-to-head")
                        .param("player1", alice.toString())
                        .param("player2", bob.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.player1Id").value(alice.toString()))
                .andExpect(jsonPath("$.player2Id").value(bob.toString()))
                .andExpect(jsonPath("$.matchesPlayed").value(2))
                .andExpect(jsonPath("$.player1.matchesWon").value(1))
                .andExpect(jsonPath("$.player1.matchesLost").value(1))
                .andExpect(jsonPath("$.player1.winners").value(2))
                .andExpect(jsonPath("$.player1.setsWon").value(3))
                .andExpect(jsonPath("$.player2.matchesWon").value(1));
    }

    @Test
    void returns_zeroed_stats_when_players_never_met() throws Exception {
        UUID alice = createPlayer();
        UUID bob = createPlayer();

        mockMvc.perform(get("/api/statistics/head-to-head")
                        .param("player1", alice.toString())
                        .param("player2", bob.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchesPlayed").value(0))
                .andExpect(jsonPath("$.player1.winners").value(0));
    }

    @Test
    void returns_404_when_a_player_does_not_exist() throws Exception {
        UUID alice = createPlayer();

        mockMvc.perform(get("/api/statistics/head-to-head")
                        .param("player1", alice.toString())
                        .param("player2", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns_400_when_both_players_are_equal() throws Exception {
        UUID alice = createPlayer();

        mockMvc.perform(get("/api/statistics/head-to-head")
                        .param("player1", alice.toString())
                        .param("player2", alice.toString()))
                .andExpect(status().isBadRequest());
    }
}
