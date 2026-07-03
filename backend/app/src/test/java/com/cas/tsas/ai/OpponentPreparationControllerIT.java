package com.cas.tsas.ai;

import com.cas.tsas.AbstractIntegrationTest;
import com.cas.tsas.match.application.port.out.SavePointPort;
import com.cas.tsas.match.domain.model.Direction;
import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.match.domain.model.Point;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.match.domain.model.StrokeType;
import com.cas.tsas.match.infrastructure.persistence.entity.MatchScoreJpaEntity;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchPersistenceAdapter;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchScoreJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.PointJpaRepository;
import com.cas.tsas.player.infrastructure.persistence.entity.PlayerJpaEntity;
import com.cas.tsas.player.infrastructure.persistence.repository.PlayerJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** TEN-51 / FA-20 — KI-Vorbereitung gegen einen Gegner. */
class OpponentPreparationControllerIT extends AbstractIntegrationTest {

    @Autowired MatchPersistenceAdapter matchAdapter;
    @Autowired SavePointPort savePoint;
    @Autowired PlayerJpaRepository playerRepo;
    @Autowired MatchJpaRepository matchRepo;
    @Autowired MatchScoreJpaRepository matchScoreRepo;
    @Autowired PointJpaRepository pointRepo;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        pointRepo.deleteAll();
        matchScoreRepo.deleteAll();
        matchRepo.deleteAll();
        playerRepo.deleteAll();
    }

    @Test
    void post_returnsPreparation_whenH2hMatchExists() throws Exception {
        UUID own = createPlayer("Own", "Coach");
        UUID opponent = createPlayer("Opp", "Onent");
        createCompletedMatchBetween(own, opponent, /* winner */ "PLAYER1", /* points */ 12);

        mockMvc.perform(post("/api/players/{ownId}/opponent-preparation/{opponentId}", own, opponent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownPlayerId").value(own.toString()))
                .andExpect(jsonPath("$.opponentId").value(opponent.toString()))
                .andExpect(jsonPath("$.matchesPlayed").value(1))
                .andExpect(jsonPath("$.opponentProfile").exists())
                .andExpect(jsonPath("$.tacticalObservations").exists())
                .andExpect(jsonPath("$.serveStrategy").exists())
                .andExpect(jsonPath("$.returnStrategy").exists())
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.recommendations[0].title").exists())
                .andExpect(jsonPath("$.modelUsed").value("fake-llm"));
    }

    @Test
    void post_returns404_whenPlayerNotFound() throws Exception {
        UUID own = createPlayer("Lonely", "Player");
        UUID nonExistent = UUID.randomUUID();
        mockMvc.perform(post("/api/players/{ownId}/opponent-preparation/{opponentId}", own, nonExistent))
                .andExpect(status().isNotFound());
    }

    @Test
    void post_returns422_whenNoSharedMatch() throws Exception {
        UUID own = createPlayer("Own", "Coach");
        UUID opponent = createPlayer("Opp", "Onent");
        // No common match → matchesPlayed = 0 → 422.
        mockMvc.perform(post("/api/players/{ownId}/opponent-preparation/{opponentId}", own, opponent))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void post_returns400_whenOwnEqualsOpponent() throws Exception {
        UUID own = createPlayer("Same", "Player");
        mockMvc.perform(post("/api/players/{ownId}/opponent-preparation/{opponentId}", own, own))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_returns404_whenOwnPlayerBelongsToOtherUser() throws Exception {
        // IDOR-Schutz: Spieler eines anderen Owners darf nicht enumerierbar sein → 404 statt 403.
        UUID foreignOwner = UUID.randomUUID();
        UUID foreignOwn = createPlayerForOwner("Foreign", "Owner", foreignOwner);
        UUID opponent = createPlayer("My", "Opponent");
        mockMvc.perform(post("/api/players/{ownId}/opponent-preparation/{opponentId}", foreignOwn, opponent))
                .andExpect(status().isNotFound());
    }

    @Test
    void post_returns404_whenOpponentBelongsToOtherUser() throws Exception {
        UUID foreignOwner = UUID.randomUUID();
        UUID own = createPlayer("My", "Coach");
        UUID foreignOpponent = createPlayerForOwner("Foreign", "Opp", foreignOwner);
        mockMvc.perform(post("/api/players/{ownId}/opponent-preparation/{opponentId}", own, foreignOpponent))
                .andExpect(status().isNotFound());
    }

    private UUID createPlayer(String first, String last) {
        return createPlayerForOwner(first, last, DEFAULT_USER);
    }

    private UUID createPlayerForOwner(String first, String last, UUID ownerId) {
        PlayerJpaEntity p = new PlayerJpaEntity();
        p.setOwnerId(ownerId);
        p.setFirstName(first);
        p.setLastName(last);
        return playerRepo.save(p).getId();
    }

    private void createCompletedMatchBetween(UUID p1, UUID p2, String winner, int pointCount) {
        Match match = matchAdapter.saveMatch(
                new Match(null, DEFAULT_USER, p1, p2, 2, false, false, MatchStatus.COMPLETED));
        MatchScoreJpaEntity score = new MatchScoreJpaEntity();
        score.setMatchId(match.getId());
        score.setCurrentSet(1);
        score.setServingPlayer(1);
        score.setGamesPlayer1(6);
        score.setGamesPlayer2(3);
        score.setSetsPlayer1(2);
        score.setSetsPlayer2(0);
        score.setDone(true);
        score.setWinner(winner);
        matchScoreRepo.save(score);

        for (int i = 1; i <= pointCount; i++) {
            savePoint.savePoint(new Point(null, match.getId(), 1, 1, i, /* winner */ (i % 2 == 0 ? 2 : 1),
                    PointType.WINNER, StrokeType.FOREHAND, Direction.CROSS_COURT,
                    /* servingPlayer */ 1, /* breakPoint */ false, null, /* serveAttempt */ 1));
        }
    }
}
