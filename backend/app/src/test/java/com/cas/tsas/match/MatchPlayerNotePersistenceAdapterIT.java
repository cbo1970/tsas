package com.cas.tsas.match;

import com.cas.tsas.AbstractIntegrationTest;
import com.cas.tsas.match.application.port.out.DeletePlayerNotePort;
import com.cas.tsas.match.application.port.out.LoadPlayerNotesPort;
import com.cas.tsas.match.application.port.out.SavePlayerNotePort;
import com.cas.tsas.match.domain.model.MatchPlayerNote;
import com.cas.tsas.match.infrastructure.persistence.entity.MatchJpaEntity;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchPlayerNoteJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchScoreJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.PointJpaRepository;
import com.cas.tsas.player.infrastructure.persistence.entity.PlayerJpaEntity;
import com.cas.tsas.player.infrastructure.persistence.repository.PlayerJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MatchPlayerNotePersistenceAdapterIT extends AbstractIntegrationTest {

    @Autowired SavePlayerNotePort savePort;
    @Autowired DeletePlayerNotePort deletePort;
    @Autowired LoadPlayerNotesPort loadPort;
    @Autowired MatchPlayerNoteJpaRepository noteRepository;
    @Autowired MatchJpaRepository matchRepository;
    @Autowired MatchScoreJpaRepository matchScoreRepository;
    @Autowired PointJpaRepository pointRepository;
    @Autowired PlayerJpaRepository playerRepository;

    private UUID matchId;
    private UUID player1Id;
    private UUID player2Id;

    @BeforeEach
    void setUp() {
        // FK-safe order: this IT shares the JVM-wide Testcontainer with other classes that leave
        // points/match_scores referencing matches (fk_match_scores_match is not ON DELETE CASCADE),
        // so those children must be cleared before matches.
        noteRepository.deleteAll();
        pointRepository.deleteAll();
        matchScoreRepository.deleteAll();
        matchRepository.deleteAll();
        playerRepository.deleteAll();
        player1Id = persistPlayer("Max", "Muster");
        player2Id = persistPlayer("Tom", "Gegner");
        matchId = persistMatch(player1Id, player2Id);
    }

    private UUID persistPlayer(String first, String last) {
        PlayerJpaEntity p = new PlayerJpaEntity();
        p.setOwnerId(DEFAULT_USER);
        p.setFirstName(first);
        p.setLastName(last);
        p.setGender(com.cas.tsas.player.domain.model.Gender.MALE);
        p.setHandedness(com.cas.tsas.player.domain.model.Handedness.RIGHT);
        p.setBackhandType(com.cas.tsas.player.domain.model.BackhandType.TWO_HANDED);
        p.setActive(true);
        return playerRepository.save(p).getId();
    }

    private UUID persistMatch(UUID p1, UUID p2) {
        MatchJpaEntity m = new MatchJpaEntity();
        m.setOwnerId(DEFAULT_USER);
        m.setPlayer1Id(p1);
        m.setPlayer2Id(p2);
        m.setSetsToWin(2);
        m.setMatchTiebreak(false);
        m.setShortSet(false);
        m.setStatus(com.cas.tsas.match.domain.model.MatchStatus.IN_PROGRESS);
        return matchRepository.save(m).getId();
    }

    @Test
    void upsert_inserts_then_updates_same_row() {
        MatchPlayerNote first = savePort.upsert(matchId, player1Id, "erste Notiz");
        MatchPlayerNote second = savePort.upsert(matchId, player1Id, "aktualisiert");

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.note()).isEqualTo("aktualisiert");
        assertThat(noteRepository.findByMatchId(matchId)).hasSize(1);
    }

    @Test
    void findByMatch_returns_both_player_notes() {
        savePort.upsert(matchId, player1Id, "p1");
        savePort.upsert(matchId, player2Id, "p2");

        assertThat(loadPort.findByMatch(matchId)).hasSize(2)
                .extracting(MatchPlayerNote::playerId)
                .containsExactlyInAnyOrder(player1Id, player2Id);
    }

    @Test
    void delete_removes_the_note() {
        savePort.upsert(matchId, player1Id, "weg damit");
        deletePort.delete(matchId, player1Id);
        assertThat(loadPort.findByMatch(matchId)).isEmpty();
    }

    @Test
    void findAboutPlayer_returns_newest_first_capped_at_limit() {
        // second match so the opponent (player2Id) has two notes about them
        UUID match2 = persistMatch(persistPlayer("A", "B"), player2Id);
        savePort.upsert(matchId, player2Id, "alt");
        savePort.upsert(match2, player2Id, "neu");

        var about = loadPort.findAboutPlayer(player2Id, 1);
        assertThat(about).hasSize(1);
        assertThat(about.get(0).note()).isEqualTo("neu");
    }
}
