package com.cas.tsas.match.application.service;

import com.cas.tsas.auth.application.port.in.CurrentUserProvider;
import com.cas.tsas.auth.domain.CurrentUser;
import com.cas.tsas.auth.domain.Role;
import com.cas.tsas.match.application.port.in.CreateMatchUseCase;
import com.cas.tsas.match.application.port.in.RecordPointUseCase;
import com.cas.tsas.match.application.port.in.SetScoreUseCase;
import com.cas.tsas.match.application.port.in.SetServingPlayerUseCase;
import com.cas.tsas.match.application.port.out.CountPointsInGamePort;
import com.cas.tsas.match.application.port.out.LoadMatchPort;
import com.cas.tsas.match.application.port.out.LoadMatchScorePort;
import com.cas.tsas.match.application.port.out.SaveMatchPort;
import com.cas.tsas.match.application.port.out.SaveMatchScorePort;
import com.cas.tsas.match.application.port.out.SavePointPort;
import com.cas.tsas.match.domain.exception.MatchAlreadyCompletedException;
import com.cas.tsas.match.domain.exception.MatchNotFoundException;
import com.cas.tsas.match.domain.model.Direction;
import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchScore;
import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.match.domain.model.Point;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.match.domain.model.StrokeType;
import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.player.domain.exception.PlayerNotFoundException;
import com.cas.tsas.player.domain.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchServiceTest {

    @Mock private LoadMatchPort loadMatchPort;
    @Mock private SaveMatchPort saveMatchPort;
    @Mock private LoadPlayerPort loadPlayerPort;
    @Mock private LoadMatchScorePort loadMatchScorePort;
    @Mock private SaveMatchScorePort saveMatchScorePort;
    @Mock private SavePointPort savePointPort;
    @Mock private CountPointsInGamePort countPointsInGamePort;
    @Mock private CurrentUserProvider currentUserProvider;

    private MatchService matchService;

    @BeforeEach
    void setUp() {
        matchService = new MatchService(loadMatchPort, saveMatchPort, loadPlayerPort,
                loadMatchScorePort, saveMatchScorePort, new ScoringService(),
                savePointPort, countPointsInGamePort, currentUserProvider);
        // Default identity: a COACH that owns the fixture match. Tests that
        // need a different identity (e.g. admin, foreign owner) override via
        // asUser(...).
        asUser(OWNER_ID, Role.COACH);
    }

    private void asUser(UUID id, Role... roles) {
        when(currentUserProvider.get()).thenReturn(new CurrentUser(id, Set.of(roles)));
    }

    private static final UUID MATCH_ID   = UUID.randomUUID();
    private static final UUID OWNER_ID   = UUID.randomUUID();
    private static final UUID OWNER_A    = UUID.randomUUID();
    private static final UUID OWNER_B    = UUID.randomUUID();
    private static final UUID PLAYER1_ID = UUID.randomUUID();
    private static final UUID PLAYER2_ID = UUID.randomUUID();

    private static Match inProgressMatch() {
        return new Match(MATCH_ID, OWNER_ID, PLAYER1_ID, PLAYER2_ID, 2, false, false, MatchStatus.IN_PROGRESS);
    }

    private static Match completedMatch() {
        return new Match(MATCH_ID, OWNER_ID, PLAYER1_ID, PLAYER2_ID, 2, false, false, MatchStatus.COMPLETED);
    }

    private static MatchScore freshScore() {
        return new MatchScore(null, MATCH_ID, 0, 0, 0, 0, 0, 0, false, null, 1, false, null, 0, 0, null);
    }

    private static Player anyPlayer(UUID id) {
        return new Player(id, UUID.randomUUID(), "Test", "Player", null, null, null, null, null, null);
    }

    private static RecordPointUseCase.RecordPointCommand winnerCommand() {
        return new RecordPointUseCase.RecordPointCommand(
                MATCH_ID, 1, PointType.WINNER, StrokeType.FOREHAND, Direction.CROSS_COURT, null, null);
    }

    // =========================================================================
    @Nested
    class CreateMatch {

        private final CreateMatchUseCase.CreateMatchCommand command =
                new CreateMatchUseCase.CreateMatchCommand(PLAYER1_ID, PLAYER2_ID, 2, false, false);

        @Test
        void saves_match_and_creates_initial_score() {
            when(loadPlayerPort.loadPlayer(PLAYER1_ID)).thenReturn(Optional.of(anyPlayer(PLAYER1_ID)));
            when(loadPlayerPort.loadPlayer(PLAYER2_ID)).thenReturn(Optional.of(anyPlayer(PLAYER2_ID)));
            when(saveMatchPort.saveMatch(any())).thenReturn(inProgressMatch());

            Match result = matchService.createMatch(command);

            assertThat(result.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
            verify(saveMatchScorePort).saveMatchScore(argThat(s ->
                    MATCH_ID.equals(s.getMatchId()) && !s.isDone() && s.getPointsPlayer1() == 0
            ));
        }

        @Test
        void throws_PlayerNotFoundException_when_player1_not_found() {
            when(loadPlayerPort.loadPlayer(PLAYER1_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.createMatch(command))
                    .isInstanceOf(PlayerNotFoundException.class);
        }

        @Test
        void throws_PlayerNotFoundException_when_player2_not_found() {
            when(loadPlayerPort.loadPlayer(PLAYER1_ID)).thenReturn(Optional.of(anyPlayer(PLAYER1_ID)));
            when(loadPlayerPort.loadPlayer(PLAYER2_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.createMatch(command))
                    .isInstanceOf(PlayerNotFoundException.class);
        }

        @Test
        void assigns_current_user_as_owner() {
            asUser(OWNER_A, Role.COACH);
            when(loadPlayerPort.loadPlayer(PLAYER1_ID)).thenReturn(Optional.of(anyPlayer(PLAYER1_ID)));
            when(loadPlayerPort.loadPlayer(PLAYER2_ID)).thenReturn(Optional.of(anyPlayer(PLAYER2_ID)));
            when(saveMatchPort.saveMatch(any())).thenAnswer(inv -> inv.getArgument(0));

            matchService.createMatch(command);

            verify(saveMatchPort).saveMatch(argThat(m -> OWNER_A.equals(m.getOwnerId())));
        }
    }

    // =========================================================================
    @Nested
    class FindById {

        @Test
        void returns_match_when_owned_by_current_user() {
            Match match = inProgressMatch();
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_ID)).thenReturn(Optional.of(match));

            assertThat(matchService.findById(MATCH_ID)).isEqualTo(match);
        }

        @Test
        void throws_MatchNotFoundException_when_not_found() {
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.findById(MATCH_ID))
                    .isInstanceOf(MatchNotFoundException.class);
        }

        @Test
        void returns_match_when_owner_matches() {
            UUID id = UUID.randomUUID();
            asUser(OWNER_A, Role.COACH);
            Match m = new Match(id, OWNER_A, PLAYER1_ID, PLAYER2_ID, 2, false, false, MatchStatus.IN_PROGRESS);
            when(loadMatchPort.findByIdAndOwner(id, OWNER_A)).thenReturn(Optional.of(m));

            assertThat(matchService.findById(id)).isEqualTo(m);
        }

        @Test
        void throws_MatchNotFoundException_when_owned_by_someone_else() {
            UUID id = UUID.randomUUID();
            asUser(OWNER_A, Role.COACH);
            when(loadMatchPort.findByIdAndOwner(id, OWNER_A)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.findById(id))
                    .isInstanceOf(MatchNotFoundException.class);
            // Owner-aware path: never falls back to the unfiltered load.
            verify(loadMatchPort, never()).loadMatch(any());
        }

        @Test
        void uses_unfiltered_load_for_admin() {
            UUID id = UUID.randomUUID();
            asUser(OWNER_A, Role.COACH, Role.ADMIN);
            Match foreign = new Match(id, OWNER_B, PLAYER1_ID, PLAYER2_ID, 2, false, false, MatchStatus.IN_PROGRESS);
            when(loadMatchPort.loadMatch(id)).thenReturn(Optional.of(foreign));

            assertThat(matchService.findById(id)).isEqualTo(foreign);
            verify(loadMatchPort, never()).findByIdAndOwner(any(), any());
        }
    }

    // =========================================================================
    @Nested
    class FindAll {

        @Test
        void filters_by_current_owner_for_non_admin() {
            asUser(OWNER_A, Role.COACH);
            Match m = new Match(UUID.randomUUID(), OWNER_A, PLAYER1_ID, PLAYER2_ID, 2, false, false, MatchStatus.IN_PROGRESS);
            when(loadMatchPort.findAllByOwner(OWNER_A)).thenReturn(List.of(m));

            assertThat(matchService.findAll()).containsExactly(m);
            verify(loadMatchPort, never()).loadAllMatches();
        }

        @Test
        void returns_everything_for_admin() {
            asUser(OWNER_A, Role.COACH, Role.ADMIN);
            Match a = new Match(UUID.randomUUID(), OWNER_A, PLAYER1_ID, PLAYER2_ID, 2, false, false, MatchStatus.IN_PROGRESS);
            Match b = new Match(UUID.randomUUID(), OWNER_B, PLAYER1_ID, PLAYER2_ID, 2, false, false, MatchStatus.IN_PROGRESS);
            when(loadMatchPort.loadAllMatches()).thenReturn(List.of(a, b));

            assertThat(matchService.findAll()).hasSize(2);
            verify(loadMatchPort, never()).findAllByOwner(any());
        }
    }

    // =========================================================================
    @Nested
    class GetScore {

        @Test
        void returns_score_when_match_and_score_exist() {
            MatchScore score = freshScore();
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_ID)).thenReturn(Optional.of(inProgressMatch()));
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));

            assertThat(matchService.getScore(MATCH_ID)).isEqualTo(score);
        }

        @Test
        void throws_MatchNotFoundException_when_match_not_found() {
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.getScore(MATCH_ID))
                    .isInstanceOf(MatchNotFoundException.class);
        }
    }

    // =========================================================================
    @Nested
    class RecordPoint {

        @Test
        void applies_point_to_score_and_saves_it() {
            MatchScore score = freshScore();
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_ID)).thenReturn(Optional.of(inProgressMatch()));
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));
            when(saveMatchScorePort.saveMatchScore(score)).thenReturn(score);
            when(countPointsInGamePort.countPointsInGame(any(), anyInt(), anyInt())).thenReturn(0);
            when(savePointPort.savePoint(any())).thenAnswer(inv -> inv.getArgument(0));

            matchService.recordPoint(winnerCommand());

            assertThat(score.getPointsPlayer1()).isEqualTo(1);
            verify(saveMatchScorePort).saveMatchScore(score);
            verify(savePointPort).savePoint(any());
        }

        @Test
        void ace_point_type_increments_ace_counter_for_winner() {
            MatchScore score = freshScore();
            score.setServingPlayer(1);
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_ID)).thenReturn(Optional.of(inProgressMatch()));
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));
            when(saveMatchScorePort.saveMatchScore(score)).thenReturn(score);
            when(countPointsInGamePort.countPointsInGame(any(), anyInt(), anyInt())).thenReturn(0);
            when(savePointPort.savePoint(any())).thenAnswer(inv -> inv.getArgument(0));

            var aceCommand = new RecordPointUseCase.RecordPointCommand(
                    MATCH_ID, 1, PointType.ACE, null, null, null, null);
            matchService.recordPoint(aceCommand);

            assertThat(score.getAcesPlayer1()).isEqualTo(1);
            assertThat(score.getAcesPlayer2()).isEqualTo(0);
        }

        @Test
        void throws_MatchNotFoundException_when_match_not_found() {
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.recordPoint(winnerCommand()))
                    .isInstanceOf(MatchNotFoundException.class);
        }

        @Test
        void throws_MatchAlreadyCompleted_when_match_already_completed() {
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_ID)).thenReturn(Optional.of(completedMatch()));

            assertThatThrownBy(() -> matchService.recordPoint(winnerCommand()))
                    .isInstanceOf(MatchAlreadyCompletedException.class)
                    .hasMessageContaining("already completed");
        }

        @Test
        void persists_serve_attempt_on_point() {
            MatchScore score = freshScore();
            score.setServingPlayer(1);
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_ID)).thenReturn(Optional.of(inProgressMatch()));
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));
            when(saveMatchScorePort.saveMatchScore(score)).thenReturn(score);
            when(countPointsInGamePort.countPointsInGame(any(), anyInt(), anyInt())).thenReturn(0);
            when(savePointPort.savePoint(any())).thenAnswer(inv -> inv.getArgument(0));

            var command = new RecordPointUseCase.RecordPointCommand(
                    MATCH_ID, 1, PointType.WINNER, StrokeType.FOREHAND, Direction.CROSS_COURT, null, 2);
            matchService.recordPoint(command);

            ArgumentCaptor<Point> captor = ArgumentCaptor.forClass(Point.class);
            verify(savePointPort).savePoint(captor.capture());
            assertThat(captor.getValue().getServeAttempt()).isEqualTo(2);
        }

        @Test
        void marks_match_completed_when_score_is_done_after_point() {
            Match match = inProgressMatch();
            MatchScore score = freshScore();
            score.setDone(true);
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_ID)).thenReturn(Optional.of(match));
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));
            when(saveMatchScorePort.saveMatchScore(score)).thenReturn(score);
            when(countPointsInGamePort.countPointsInGame(any(), anyInt(), anyInt())).thenReturn(0);
            when(savePointPort.savePoint(any())).thenAnswer(inv -> inv.getArgument(0));

            matchService.recordPoint(winnerCommand());

            verify(saveMatchPort).saveMatch(argThat(m -> m.getStatus() == MatchStatus.COMPLETED));
        }
    }

    // =========================================================================
    @Nested
    class SetScore {

        @Test
        void updates_all_score_fields_and_saves() {
            Match match = inProgressMatch();
            MatchScore score = freshScore();
            var command = new SetScoreUseCase.SetScoreCommand(
                    MATCH_ID, 1, 2, 3, 1, 0, 0, false, null, 1, false, null);
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_ID)).thenReturn(Optional.of(match));
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));
            when(saveMatchScorePort.saveMatchScore(score)).thenReturn(score);

            matchService.setScore(command);

            verify(saveMatchScorePort).saveMatchScore(score);
            assertThat(score.getPointsPlayer1()).isEqualTo(1);
            assertThat(score.getPointsPlayer2()).isEqualTo(2);
            assertThat(score.getGamesPlayer1()).isEqualTo(3);
        }

        @Test
        void marks_match_completed_when_score_set_to_done() {
            Match match = inProgressMatch();
            MatchScore score = freshScore();
            var command = new SetScoreUseCase.SetScoreCommand(
                    MATCH_ID, 0, 0, 0, 0, 2, 0, false, null, 3, true, "PLAYER1");
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_ID)).thenReturn(Optional.of(match));
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));
            when(saveMatchScorePort.saveMatchScore(any())).thenReturn(score);

            matchService.setScore(command);

            verify(saveMatchPort).saveMatch(argThat(m -> m.getStatus() == MatchStatus.COMPLETED));
        }

        @Test
        void reopens_completed_match_when_score_set_to_not_done() {
            Match match = completedMatch();
            MatchScore score = freshScore();
            var command = new SetScoreUseCase.SetScoreCommand(
                    MATCH_ID, 0, 0, 1, 0, 1, 0, false, null, 2, false, null);
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_ID)).thenReturn(Optional.of(match));
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));
            when(saveMatchScorePort.saveMatchScore(any())).thenReturn(score);

            matchService.setScore(command);

            verify(saveMatchPort).saveMatch(argThat(m -> m.getStatus() == MatchStatus.IN_PROGRESS));
        }
    }

    // =========================================================================
    @Nested
    class EndMatch {

        @Test
        void sets_match_status_to_completed() {
            Match match = inProgressMatch();
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_ID)).thenReturn(Optional.of(match));
            when(saveMatchPort.saveMatch(any())).thenReturn(match);
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.empty());

            matchService.endMatch(MATCH_ID);

            verify(saveMatchPort).saveMatch(argThat(m -> m.getStatus() == MatchStatus.COMPLETED));
        }

        @Test
        void marks_score_done_and_determines_winner_by_sets() {
            Match match = inProgressMatch();
            MatchScore score = new MatchScore(null, MATCH_ID, 0, 0, 0, 0, 2, 1, false, null, 3, false, null, 0, 0, null);
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_ID)).thenReturn(Optional.of(match));
            when(saveMatchPort.saveMatch(any())).thenReturn(match);
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));

            matchService.endMatch(MATCH_ID);

            verify(saveMatchScorePort).saveMatchScore(argThat(s ->
                    s.isDone() && "PLAYER1".equals(s.getWinner())
            ));
        }

        @Test
        void does_not_update_score_when_already_done() {
            Match match = inProgressMatch();
            MatchScore score = freshScore();
            score.setDone(true);
            score.setWinner("PLAYER2");
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_ID)).thenReturn(Optional.of(match));
            when(saveMatchPort.saveMatch(any())).thenReturn(match);
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));

            matchService.endMatch(MATCH_ID);

            verify(saveMatchScorePort, never()).saveMatchScore(any());
        }

        @Test
        void throws_MatchNotFoundException_when_owned_by_someone_else() {
            asUser(OWNER_A, Role.COACH);
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_A)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.endMatch(MATCH_ID))
                    .isInstanceOf(MatchNotFoundException.class);
            verify(saveMatchPort, never()).saveMatch(any());
        }
    }

    // =========================================================================
    @Nested
    class EndMatchWalkover {

        @Test
        void throws_MatchNotFoundException_when_owned_by_someone_else() {
            asUser(OWNER_A, Role.COACH);
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_A)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.endMatchWalkover(MATCH_ID, true))
                    .isInstanceOf(MatchNotFoundException.class);
            verify(saveMatchPort, never()).saveMatch(any());
        }

        @Test
        void marks_match_completed_and_assigns_winner() {
            Match match = inProgressMatch();
            MatchScore score = freshScore();
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_ID)).thenReturn(Optional.of(match));
            when(saveMatchPort.saveMatch(any())).thenReturn(match);
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));

            matchService.endMatchWalkover(MATCH_ID, true);

            verify(saveMatchPort).saveMatch(argThat(m -> m.getStatus() == MatchStatus.COMPLETED));
            verify(saveMatchScorePort).saveMatchScore(argThat(s ->
                    s.isDone() && "PLAYER1".equals(s.getWinner())
            ));
        }
    }

    // =========================================================================
    @Nested
    class SetServingPlayer {

        @Test
        void throws_MatchNotFoundException_when_match_not_found() {
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                matchService.setServingPlayer(
                    new SetServingPlayerUseCase.SetServingPlayerCommand(MATCH_ID, true)))
                .isInstanceOf(MatchNotFoundException.class);
        }

        @Test
        void throws_MatchAlreadyCompleted_when_match_already_completed() {
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_ID)).thenReturn(Optional.of(completedMatch()));

            assertThatThrownBy(() ->
                matchService.setServingPlayer(
                    new SetServingPlayerUseCase.SetServingPlayerCommand(MATCH_ID, true)))
                .isInstanceOf(MatchAlreadyCompletedException.class);
        }

        @Test
        void sets_serving_player_to_1_for_player1() {
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_ID)).thenReturn(Optional.of(inProgressMatch()));
            MatchScore score = freshScore();
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));
            when(saveMatchScorePort.saveMatchScore(any())).thenAnswer(inv -> inv.getArgument(0));

            MatchScore result = matchService.setServingPlayer(
                    new SetServingPlayerUseCase.SetServingPlayerCommand(MATCH_ID, true));

            assertThat(result.getServingPlayer()).isEqualTo(1);
        }

        @Test
        void sets_serving_player_to_2_for_player2() {
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_ID)).thenReturn(Optional.of(inProgressMatch()));
            MatchScore score = freshScore();
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));
            when(saveMatchScorePort.saveMatchScore(any())).thenAnswer(inv -> inv.getArgument(0));

            MatchScore result = matchService.setServingPlayer(
                    new SetServingPlayerUseCase.SetServingPlayerCommand(MATCH_ID, false));

            assertThat(result.getServingPlayer()).isEqualTo(2);
        }

        @Test
        void throws_MatchNotFoundException_when_score_not_found() {
            when(loadMatchPort.findByIdAndOwner(MATCH_ID, OWNER_ID)).thenReturn(Optional.of(inProgressMatch()));
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                matchService.setServingPlayer(
                    new SetServingPlayerUseCase.SetServingPlayerCommand(MATCH_ID, true)))
                .isInstanceOf(MatchNotFoundException.class);
        }
    }
}
