package com.cas.tsas.player.application.service;

import com.cas.tsas.player.application.port.in.CreatePlayerUseCase;
import com.cas.tsas.player.application.port.in.UpdatePlayerUseCase;
import com.cas.tsas.player.application.port.out.DeletePlayerPort;
import com.cas.tsas.player.application.port.out.HasMatchesPort;
import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.player.application.port.out.SavePlayerPort;
import com.cas.tsas.player.domain.exception.PlayerNotFoundException;
import com.cas.tsas.player.domain.model.BackhandType;
import com.cas.tsas.player.domain.model.Gender;
import com.cas.tsas.player.domain.model.Handedness;
import com.cas.tsas.player.domain.model.Player;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock private LoadPlayerPort loadPlayerPort;
    @Mock private SavePlayerPort savePlayerPort;
    @Mock private DeletePlayerPort deletePlayerPort;
    @Mock private HasMatchesPort hasMatchesPort;

    @InjectMocks private PlayerService playerService;

    private static final UUID PLAYER_ID = UUID.randomUUID();

    // -------------------------------------------------------------------------
    // Factories
    // -------------------------------------------------------------------------

    private static Player activePlayer() {
        Player p = new Player(PLAYER_ID, "Max", "Muster", Gender.MALE,
                Handedness.RIGHT, BackhandType.TWO_HANDED, "4.0", "CH", LocalDate.of(2000, 1, 1));
        p.setActive(true);
        return p;
    }

    private static CreatePlayerUseCase.CreatePlayerCommand createCommand() {
        return new CreatePlayerUseCase.CreatePlayerCommand(
                "Max", "Muster", Gender.MALE, Handedness.RIGHT,
                BackhandType.TWO_HANDED, "4.0", "CH", LocalDate.of(2000, 1, 1));
    }

    private static UpdatePlayerUseCase.UpdatePlayerCommand updateCommand() {
        return new UpdatePlayerUseCase.UpdatePlayerCommand(
                PLAYER_ID, "Anna", "Muster", Gender.FEMALE, Handedness.LEFT,
                BackhandType.ONE_HANDED, "3.5", "DE", LocalDate.of(1995, 6, 15));
    }

    // =========================================================================
    @Nested
    class CreatePlayer {

        @Test
        void saves_new_player_with_given_attributes() {
            when(savePlayerPort.savePlayer(any())).thenAnswer(inv -> inv.getArgument(0));

            Player result = playerService.createPlayer(createCommand());

            assertThat(result.getFirstName()).isEqualTo("Max");
            assertThat(result.getLastName()).isEqualTo("Muster");
            assertThat(result.getGender()).isEqualTo(Gender.MALE);
            assertThat(result.isActive()).isTrue();
            verify(savePlayerPort).savePlayer(any());
        }

        @Test
        void new_player_has_no_id_before_persistence() {
            when(savePlayerPort.savePlayer(any())).thenAnswer(inv -> inv.getArgument(0));

            playerService.createPlayer(createCommand());

            verify(savePlayerPort).savePlayer(argThat(p -> p.getId() == null));
        }
    }

    // =========================================================================
    @Nested
    class FindById {

        @Test
        void returns_player_when_found() {
            Player player = activePlayer();
            when(loadPlayerPort.loadPlayer(PLAYER_ID)).thenReturn(Optional.of(player));

            assertThat(playerService.findById(PLAYER_ID)).isEqualTo(player);
        }

        @Test
        void throws_PlayerNotFoundException_when_not_found() {
            when(loadPlayerPort.loadPlayer(PLAYER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> playerService.findById(PLAYER_ID))
                    .isInstanceOf(PlayerNotFoundException.class);
        }
    }

    // =========================================================================
    @Nested
    class FindAll {

        @Test
        void delegates_to_port_and_returns_all_players() {
            List<Player> players = List.of(activePlayer());
            when(loadPlayerPort.loadAllPlayers()).thenReturn(players);

            assertThat(playerService.findAll()).isEqualTo(players);
        }
    }

    // =========================================================================
    @Nested
    class UpdatePlayer {

        @Test
        void updates_all_fields_and_saves() {
            Player existing = activePlayer();
            when(loadPlayerPort.loadPlayer(PLAYER_ID)).thenReturn(Optional.of(existing));
            when(savePlayerPort.savePlayer(existing)).thenReturn(existing);

            Player result = playerService.updatePlayer(updateCommand());

            assertThat(result.getFirstName()).isEqualTo("Anna");
            assertThat(result.getLastName()).isEqualTo("Muster");
            assertThat(result.getGender()).isEqualTo(Gender.FEMALE);
            assertThat(result.getHandedness()).isEqualTo(Handedness.LEFT);
            assertThat(result.getBackhandType()).isEqualTo(BackhandType.ONE_HANDED);
            assertThat(result.getRanking()).isEqualTo("3.5");
            assertThat(result.getNationality()).isEqualTo("DE");
            verify(savePlayerPort).savePlayer(existing);
        }

        @Test
        void throws_PlayerNotFoundException_when_player_not_found() {
            when(loadPlayerPort.loadPlayer(PLAYER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> playerService.updatePlayer(updateCommand()))
                    .isInstanceOf(PlayerNotFoundException.class);
        }
    }

    // =========================================================================
    @Nested
    class HasMatches {

        @Test
        void returns_true_when_player_has_matches() {
            when(hasMatchesPort.existsByPlayerId(PLAYER_ID)).thenReturn(true);

            assertThat(playerService.hasMatches(PLAYER_ID)).isTrue();
        }

        @Test
        void returns_false_when_player_has_no_matches() {
            when(hasMatchesPort.existsByPlayerId(PLAYER_ID)).thenReturn(false);

            assertThat(playerService.hasMatches(PLAYER_ID)).isFalse();
        }
    }

    // =========================================================================
    @Nested
    class DeletePlayer {

        @Test
        void deletes_player_when_no_matches_exist() {
            when(hasMatchesPort.existsByPlayerId(PLAYER_ID)).thenReturn(false);
            when(loadPlayerPort.loadPlayer(PLAYER_ID)).thenReturn(Optional.of(activePlayer()));

            playerService.deletePlayer(PLAYER_ID);

            verify(deletePlayerPort).deletePlayer(PLAYER_ID);
        }

        @Test
        void throws_IllegalStateException_when_player_has_matches() {
            when(hasMatchesPort.existsByPlayerId(PLAYER_ID)).thenReturn(true);

            assertThatThrownBy(() -> playerService.deletePlayer(PLAYER_ID))
                    .isInstanceOf(IllegalStateException.class);
            verify(deletePlayerPort, never()).deletePlayer(any());
        }

        @Test
        void throws_PlayerNotFoundException_when_player_not_found() {
            when(hasMatchesPort.existsByPlayerId(PLAYER_ID)).thenReturn(false);
            when(loadPlayerPort.loadPlayer(PLAYER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> playerService.deletePlayer(PLAYER_ID))
                    .isInstanceOf(PlayerNotFoundException.class);
            verify(deletePlayerPort, never()).deletePlayer(any());
        }
    }

    // =========================================================================
    @Nested
    class DeactivatePlayer {

        @Test
        void sets_active_to_false_and_saves() {
            Player player = activePlayer();
            when(loadPlayerPort.loadPlayer(PLAYER_ID)).thenReturn(Optional.of(player));
            when(savePlayerPort.savePlayer(player)).thenReturn(player);

            playerService.deactivatePlayer(PLAYER_ID);

            assertThat(player.isActive()).isFalse();
            verify(savePlayerPort).savePlayer(player);
        }

        @Test
        void throws_PlayerNotFoundException_when_player_not_found() {
            when(loadPlayerPort.loadPlayer(PLAYER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> playerService.deactivatePlayer(PLAYER_ID))
                    .isInstanceOf(PlayerNotFoundException.class);
        }
    }
}
