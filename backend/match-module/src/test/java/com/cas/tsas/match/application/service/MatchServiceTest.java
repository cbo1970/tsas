package com.cas.tsas.match.application.service;

import com.cas.tsas.match.application.port.in.CreateMatchUseCase;
import com.cas.tsas.match.application.port.in.RecordPointUseCase;
import com.cas.tsas.match.application.port.in.SetScoreUseCase;
import com.cas.tsas.match.application.port.out.LoadMatchPort;
import com.cas.tsas.match.application.port.out.LoadMatchScorePort;
import com.cas.tsas.match.application.port.out.SaveMatchPort;
import com.cas.tsas.match.application.port.out.SaveMatchScorePort;
import com.cas.tsas.match.domain.exception.MatchNotFoundException;
import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchScore;
import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.player.domain.exception.PlayerNotFoundException;
import com.cas.tsas.player.domain.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock private LoadMatchPort loadMatchPort;
    @Mock private SaveMatchPort saveMatchPort;
    @Mock private LoadPlayerPort loadPlayerPort;
    @Mock private LoadMatchScorePort loadMatchScorePort;
    @Mock private SaveMatchScorePort saveMatchScorePort;

    // ScoringService has no external deps — use real instance to avoid ByteBuddy issues with Java 21+
    private MatchService matchService;

    @BeforeEach
    void setUp() {
        matchService = new MatchService(loadMatchPort, saveMatchPort, loadPlayerPort,
                loadMatchScorePort, saveMatchScorePort, new ScoringService());
    }

    private static final UUID MATCH_ID   = UUID.randomUUID();
    private static final UUID PLAYER1_ID = UUID.randomUUID();
    private static final UUID PLAYER2_ID = UUID.randomUUID();

    // -------------------------------------------------------------------------
    // Factories
    // -------------------------------------------------------------------------

    private static Match inProgressMatch() {
        return new Match(MATCH_ID, PLAYER1_ID, PLAYER2_ID, 2, false, false, MatchStatus.IN_PROGRESS);
    }

    private static Match completedMatch() {
        return new Match(MATCH_ID, PLAYER1_ID, PLAYER2_ID, 2, false, false, MatchStatus.COMPLETED);
    }

    private static MatchScore freshScore() {
        return new MatchScore(null, MATCH_ID, 0, 0, 0, 0, 0, 0, false, null, 1, false, null);
    }

    private static Player anyPlayer(UUID id) {
        return new Player(id, "Test", "Player", null, null, null, null, null, null);
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
    }

    // =========================================================================
    @Nested
    class FindById {

        @Test
        void returns_match_when_found() {
            Match match = inProgressMatch();
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(match));

            assertThat(matchService.findById(MATCH_ID)).isEqualTo(match);
        }

        @Test
        void throws_MatchNotFoundException_when_not_found() {
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.findById(MATCH_ID))
                    .isInstanceOf(MatchNotFoundException.class);
        }
    }

    // =========================================================================
    @Nested
    class FindAll {

        @Test
        void delegates_to_port_and_returns_all_matches() {
            List<Match> matches = List.of(inProgressMatch());
            when(loadMatchPort.loadAllMatches()).thenReturn(matches);

            assertThat(matchService.findAll()).isEqualTo(matches);
        }
    }

    // =========================================================================
    @Nested
    class GetScore {

        @Test
        void returns_score_when_match_and_score_exist() {
            MatchScore score = freshScore();
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(inProgressMatch()));
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));

            assertThat(matchService.getScore(MATCH_ID)).isEqualTo(score);
        }

        @Test
        void throws_MatchNotFoundException_when_match_not_found() {
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.getScore(MATCH_ID))
                    .isInstanceOf(MatchNotFoundException.class);
        }
    }

    // =========================================================================
    @Nested
    class RecordPoint {

        private final RecordPointUseCase.RecordPointCommand command =
                new RecordPointUseCase.RecordPointCommand(MATCH_ID, true);

        @Test
        void applies_point_to_score_and_saves_it() {
            MatchScore score = freshScore(); // p1 at 0 points
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(inProgressMatch()));
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));
            when(saveMatchScorePort.saveMatchScore(score)).thenReturn(score);

            matchService.recordPoint(command);

            // Real ScoringService increments p1 from 0 to 1 (15)
            assertThat(score.getPointsPlayer1()).isEqualTo(1);
            verify(saveMatchScorePort).saveMatchScore(score);
        }

        @Test
        void throws_MatchNotFoundException_when_match_not_found() {
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> matchService.recordPoint(command))
                    .isInstanceOf(MatchNotFoundException.class);
        }

        @Test
        void throws_IllegalStateException_when_match_already_completed() {
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(completedMatch()));

            assertThatThrownBy(() -> matchService.recordPoint(command))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already completed");
        }

        @Test
        void marks_match_completed_when_score_is_done_after_point() {
            Match match = inProgressMatch();
            MatchScore score = freshScore();
            score.setDone(true);
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(match));
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));
            when(saveMatchScorePort.saveMatchScore(score)).thenReturn(score);

            matchService.recordPoint(command);

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
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(match));
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
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(match));
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
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(match));
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
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(match));
            when(saveMatchPort.saveMatch(any())).thenReturn(match);
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.empty());

            matchService.endMatch(MATCH_ID);

            verify(saveMatchPort).saveMatch(argThat(m -> m.getStatus() == MatchStatus.COMPLETED));
        }

        @Test
        void marks_score_done_and_determines_winner_by_sets() {
            Match match = inProgressMatch();
            MatchScore score = new MatchScore(null, MATCH_ID, 0, 0, 0, 0, 2, 1, false, null, 3, false, null);
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(match));
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
            when(loadMatchPort.loadMatch(MATCH_ID)).thenReturn(Optional.of(match));
            when(saveMatchPort.saveMatch(any())).thenReturn(match);
            when(loadMatchScorePort.loadMatchScore(MATCH_ID)).thenReturn(Optional.of(score));

            matchService.endMatch(MATCH_ID);

            verify(saveMatchScorePort, never()).saveMatchScore(any());
        }
    }
}
