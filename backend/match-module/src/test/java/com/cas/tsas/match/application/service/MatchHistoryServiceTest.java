package com.cas.tsas.match.application.service;

import com.cas.tsas.auth.application.port.in.CurrentUserProvider;
import com.cas.tsas.auth.domain.CurrentUser;
import com.cas.tsas.auth.domain.Role;
import com.cas.tsas.match.application.port.out.LoadMatchHistoryPort;
import com.cas.tsas.match.application.port.out.MatchHistoryRow;
import com.cas.tsas.match.domain.model.MatchHistoryEntry;
import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.player.domain.exception.PlayerNotFoundException;
import com.cas.tsas.player.domain.model.BackhandType;
import com.cas.tsas.player.domain.model.Gender;
import com.cas.tsas.player.domain.model.Handedness;
import com.cas.tsas.player.domain.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class MatchHistoryServiceTest {

    private LoadMatchHistoryPort loadHistory;
    private LoadPlayerPort loadPlayer;
    private MatchHistoryService service;
    private UUID owner, player, opponent;

    @BeforeEach
    void setUp() {
        loadHistory = Mockito.mock(LoadMatchHistoryPort.class);
        loadPlayer = Mockito.mock(LoadPlayerPort.class);
        CurrentUserProvider cup = Mockito.mock(CurrentUserProvider.class);
        owner = UUID.randomUUID();
        player = UUID.randomUUID();
        opponent = UUID.randomUUID();
        when(cup.get()).thenReturn(new CurrentUser(owner, Set.of(Role.COACH)));
        service = new MatchHistoryService(loadHistory, loadPlayer, cup);
        when(loadPlayer.findByIdAndOwner(player, owner)).thenReturn(Optional.of(player(player, "Self", "Player")));
        when(loadPlayer.loadPlayer(opponent)).thenReturn(Optional.of(player(opponent, "Tom", "Gegner")));
    }

    private Player player(UUID id, String first, String last) {
        return new Player(id, owner, first, last, Gender.MALE, Handedness.RIGHT, BackhandType.TWO_HANDED, "N3", "GER", null);
    }

    @Test
    void mapsFromPlayer1Perspective_won() {
        when(loadHistory.findCompletedByPlayer(player, owner)).thenReturn(List.of(
                new MatchHistoryRow(UUID.randomUUID(), player, opponent, 2, 1, "PLAYER1", Instant.now())));
        MatchHistoryEntry e = service.forPlayer(player).get(0);
        assertThat(e.opponentId()).isEqualTo(opponent);
        assertThat(e.opponentName()).isEqualTo("Tom Gegner");
        assertThat(e.setsWon()).isEqualTo(2);
        assertThat(e.setsLost()).isEqualTo(1);
        assertThat(e.won()).isTrue();
    }

    @Test
    void mapsFromPlayer2Perspective_lost() {
        when(loadHistory.findCompletedByPlayer(player, owner)).thenReturn(List.of(
                new MatchHistoryRow(UUID.randomUUID(), opponent, player, 2, 0, "PLAYER1", Instant.now())));
        MatchHistoryEntry e = service.forPlayer(player).get(0);
        assertThat(e.opponentId()).isEqualTo(opponent);
        assertThat(e.setsWon()).isZero();
        assertThat(e.setsLost()).isEqualTo(2);
        assertThat(e.won()).isFalse();
    }

    @Test
    void foreignPlayerThrows404() {
        UUID stranger = UUID.randomUUID();
        when(loadPlayer.findByIdAndOwner(stranger, owner)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.forPlayer(stranger)).isInstanceOf(PlayerNotFoundException.class);
    }
}
