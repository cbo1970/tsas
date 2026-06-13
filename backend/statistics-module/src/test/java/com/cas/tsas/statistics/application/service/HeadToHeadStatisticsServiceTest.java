package com.cas.tsas.statistics.application.service;

import com.cas.tsas.match.application.port.out.LoadMatchScorePort;
import com.cas.tsas.match.application.port.out.LoadMatchesByPlayersPort;
import com.cas.tsas.match.application.port.out.LoadPointsByMatchPort;
import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchScore;
import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.match.domain.model.Point;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.player.domain.exception.PlayerNotFoundException;
import com.cas.tsas.player.domain.model.Player;
import com.cas.tsas.statistics.domain.model.HeadToHeadStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class HeadToHeadStatisticsServiceTest {

    private LoadPlayerPort loadPlayerPort;
    private LoadMatchesByPlayersPort loadMatchesByPlayersPort;
    private LoadPointsByMatchPort loadPointsByMatchPort;
    private LoadMatchScorePort loadMatchScorePort;
    private HeadToHeadStatisticsService service;

    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        loadPlayerPort = Mockito.mock(LoadPlayerPort.class);
        loadMatchesByPlayersPort = Mockito.mock(LoadMatchesByPlayersPort.class);
        loadPointsByMatchPort = Mockito.mock(LoadPointsByMatchPort.class);
        loadMatchScorePort = Mockito.mock(LoadMatchScorePort.class);
        service = new HeadToHeadStatisticsService(
                loadPlayerPort, loadMatchesByPlayersPort, loadPointsByMatchPort, loadMatchScorePort);

        when(loadPlayerPort.loadPlayer(alice)).thenReturn(Optional.of(player(alice)));
        when(loadPlayerPort.loadPlayer(bob)).thenReturn(Optional.of(player(bob)));
    }

    private Player player(UUID id) {
        Player p = new Player();
        p.setId(id);
        return p;
    }

    private Match match(UUID id, UUID p1, UUID p2) {
        return new Match(id, p1, p2, 2, false, false, MatchStatus.COMPLETED);
    }

    private Point p(int set, int game, int pt, int winner, PointType type,
                    Integer servingPlayer, Integer serveAttempt, boolean breakPoint) {
        return new Point(UUID.randomUUID(), UUID.randomUUID(), set, game, pt, winner,
                type, null, null, servingPlayer, breakPoint, null, serveAttempt);
    }

    private MatchScore score(UUID matchId, String winner, int sets1, int sets2) {
        return new MatchScore(UUID.randomUUID(), matchId, 0, 0, 0, 0, sets1, sets2,
                false, null, 2, true, winner, 0, 0, null);
    }

    @Test
    void throws_when_both_players_are_equal() {
        assertThatThrownBy(() -> service.compute(alice, alice))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throws_when_player1_unknown() {
        when(loadPlayerPort.loadPlayer(alice)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.compute(alice, bob))
                .isInstanceOf(PlayerNotFoundException.class);
    }

    @Test
    void throws_when_player2_unknown() {
        when(loadPlayerPort.loadPlayer(bob)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.compute(alice, bob))
                .isInstanceOf(PlayerNotFoundException.class);
    }

    @Test
    void returns_zeroed_stats_when_no_matches() {
        when(loadMatchesByPlayersPort.loadMatchesBetween(alice, bob)).thenReturn(List.of());

        HeadToHeadStatistics result = service.compute(alice, bob);

        assertThat(result.matchesPlayed()).isZero();
        assertThat(result.player1().playerId()).isEqualTo(alice);
        assertThat(result.player2().playerId()).isEqualTo(bob);
        assertThat(result.player1().winnersPercentage()).isZero();
        assertThat(result.player1().matchesWon()).isZero();
    }

    @Test
    void aggregates_winners_and_unforced_errors_as_percentage_of_all_points() {
        UUID m = UUID.randomUUID();
        when(loadMatchesByPlayersPort.loadMatchesBetween(alice, bob))
                .thenReturn(List.of(match(m, alice, bob)));
        when(loadMatchScorePort.loadMatchScore(m)).thenReturn(Optional.empty());
        when(loadPointsByMatchPort.loadPointsByMatch(m)).thenReturn(List.of(
                p(1, 1, 1, 1, PointType.WINNER, null, null, false),
                p(1, 1, 2, 1, PointType.WINNER, null, null, false),
                p(1, 1, 3, 1, PointType.UNFORCED_ERROR, null, null, false),
                p(1, 1, 4, 2, null, null, null, false)
        ));

        HeadToHeadStatistics r = service.compute(alice, bob);

        assertThat(r.player1().winners()).isEqualTo(2);
        assertThat(r.player1().winnersPercentage()).isEqualTo(0.5, within(1e-9));
        assertThat(r.player2().unforcedErrors()).isEqualTo(1);
        assertThat(r.player2().unforcedErrorPercentage()).isEqualTo(0.25, within(1e-9));
    }

    @Test
    void maps_positional_players_to_real_ids_across_matches() {
        UUID m1 = UUID.randomUUID();
        UUID m2 = UUID.randomUUID();
        when(loadMatchesByPlayersPort.loadMatchesBetween(alice, bob))
                .thenReturn(List.of(match(m1, alice, bob), match(m2, bob, alice)));
        when(loadMatchScorePort.loadMatchScore(any())).thenReturn(Optional.empty());
        when(loadPointsByMatchPort.loadPointsByMatch(m1)).thenReturn(List.of(
                p(1, 1, 1, 1, PointType.WINNER, null, null, false)));
        when(loadPointsByMatchPort.loadPointsByMatch(m2)).thenReturn(List.of(
                p(1, 1, 1, 2, PointType.WINNER, null, null, false)));

        HeadToHeadStatistics r = service.compute(alice, bob);

        assertThat(r.player1().winners()).isEqualTo(2);
        assertThat(r.player2().winners()).isZero();
    }

    @Test
    void computes_serve_and_return_metrics_from_serve_attempts() {
        UUID m = UUID.randomUUID();
        when(loadMatchesByPlayersPort.loadMatchesBetween(alice, bob))
                .thenReturn(List.of(match(m, alice, bob)));
        when(loadMatchScorePort.loadMatchScore(m)).thenReturn(Optional.empty());
        when(loadPointsByMatchPort.loadPointsByMatch(m)).thenReturn(List.of(
                p(1, 1, 1, 1, PointType.WINNER, 1, 1, false),
                p(1, 1, 2, 2, PointType.WINNER, 1, 1, false),
                p(1, 1, 3, 1, PointType.WINNER, 1, 2, false),
                p(1, 1, 4, 2, PointType.DOUBLE_FAULT, 1, 2, false)
        ));

        HeadToHeadStatistics r = service.compute(alice, bob);

        assertThat(r.player1().firstServePercentage()).isEqualTo(0.5, within(1e-9));
        assertThat(r.player1().firstServeWonPercentage()).isEqualTo(0.5, within(1e-9));
        assertThat(r.player1().secondServeWonPercentage()).isEqualTo(0.5, within(1e-9));
        assertThat(r.player1().doubleFaults()).isEqualTo(1);
        assertThat(r.player2().returnPointsWonFirstPercentage()).isEqualTo(0.5, within(1e-9));
        assertThat(r.player2().returnPointsWonSecondPercentage()).isEqualTo(0.5, within(1e-9));
    }

    @Test
    void first_serve_percentage_uses_all_service_points_as_denominator() {
        // 4 service points by alice: 3 decided on the first serve, 1 on the second.
        // 1st-serve% must be 3/4 (first serves in / all service points), not 3/3.
        UUID m = UUID.randomUUID();
        when(loadMatchesByPlayersPort.loadMatchesBetween(alice, bob))
                .thenReturn(List.of(match(m, alice, bob)));
        when(loadMatchScorePort.loadMatchScore(m)).thenReturn(Optional.empty());
        when(loadPointsByMatchPort.loadPointsByMatch(m)).thenReturn(List.of(
                p(1, 1, 1, 1, PointType.WINNER, 1, 1, false),
                p(1, 1, 2, 1, PointType.WINNER, 1, 1, false),
                p(1, 1, 3, 1, PointType.WINNER, 1, 1, false),
                p(1, 1, 4, 1, PointType.WINNER, 1, 2, false)
        ));

        HeadToHeadStatistics r = service.compute(alice, bob);

        assertThat(r.player1().firstServePercentage()).isEqualTo(0.75, within(1e-9));
    }

    @Test
    void credits_aces_to_the_serving_player_across_positions() {
        UUID m = UUID.randomUUID();
        when(loadMatchesByPlayersPort.loadMatchesBetween(alice, bob))
                .thenReturn(List.of(match(m, alice, bob)));
        when(loadMatchScorePort.loadMatchScore(m)).thenReturn(Optional.empty());
        // alice (pos 1) serves an ace; bob (pos 2) serves an ace
        when(loadPointsByMatchPort.loadPointsByMatch(m)).thenReturn(List.of(
                p(1, 1, 1, 1, PointType.ACE, 1, 1, false),
                p(1, 1, 2, 2, PointType.ACE, 2, 1, false)
        ));

        HeadToHeadStatistics r = service.compute(alice, bob);

        assertThat(r.player1().aces()).isEqualTo(1);
        assertThat(r.player2().aces()).isEqualTo(1);
    }

    @Test
    void computes_break_points_won_for_returner() {
        UUID m = UUID.randomUUID();
        when(loadMatchesByPlayersPort.loadMatchesBetween(alice, bob))
                .thenReturn(List.of(match(m, alice, bob)));
        when(loadMatchScorePort.loadMatchScore(m)).thenReturn(Optional.empty());
        when(loadPointsByMatchPort.loadPointsByMatch(m)).thenReturn(List.of(
                p(1, 1, 1, 2, null, 1, 1, true),
                p(1, 2, 1, 1, null, 1, 1, true)
        ));

        HeadToHeadStatistics r = service.compute(alice, bob);

        assertThat(r.player2().breakPointsPlayed()).isEqualTo(2);
        assertThat(r.player2().breakPointsWon()).isEqualTo(1);
        assertThat(r.player2().breakPointsWonPercentage()).isEqualTo(0.5, within(1e-9));
    }

    @Test
    void computes_return_games_won_from_game_winner() {
        UUID m = UUID.randomUUID();
        when(loadMatchesByPlayersPort.loadMatchesBetween(alice, bob))
                .thenReturn(List.of(match(m, alice, bob)));
        when(loadMatchScorePort.loadMatchScore(m)).thenReturn(Optional.empty());
        when(loadPointsByMatchPort.loadPointsByMatch(m)).thenReturn(List.of(
                p(1, 1, 1, 1, null, 1, null, false),
                p(1, 1, 2, 2, null, 1, null, false),
                p(1, 2, 1, 2, null, 2, null, false),
                p(1, 2, 2, 2, null, 2, null, false)
        ));

        HeadToHeadStatistics r = service.compute(alice, bob);

        assertThat(r.player2().returnGamesWonPercentage()).isEqualTo(1.0, within(1e-9));
        assertThat(r.player1().returnGamesWonPercentage()).isZero();
    }

    @Test
    void counts_match_and_set_balance_only_for_completed_matches() {
        UUID m1 = UUID.randomUUID();
        UUID m2 = UUID.randomUUID();
        UUID m3 = UUID.randomUUID();
        when(loadMatchesByPlayersPort.loadMatchesBetween(alice, bob))
                .thenReturn(List.of(match(m1, alice, bob), match(m2, bob, alice), match(m3, alice, bob)));
        when(loadPointsByMatchPort.loadPointsByMatch(any())).thenReturn(List.of());
        when(loadMatchScorePort.loadMatchScore(m1)).thenReturn(Optional.of(score(m1, "PLAYER1", 2, 0)));
        when(loadMatchScorePort.loadMatchScore(m2)).thenReturn(Optional.of(score(m2, "PLAYER1", 2, 1)));
        when(loadMatchScorePort.loadMatchScore(m3)).thenReturn(Optional.of(score(m3, null, 1, 0)));

        HeadToHeadStatistics r = service.compute(alice, bob);

        assertThat(r.matchesPlayed()).isEqualTo(2);
        assertThat(r.player1().matchesWon()).isEqualTo(1);
        assertThat(r.player1().matchesLost()).isEqualTo(1);
        assertThat(r.player1().setsWon()).isEqualTo(3);
        assertThat(r.player1().setsLost()).isEqualTo(2);
        assertThat(r.player2().matchesWon()).isEqualTo(1);
        assertThat(r.player2().setsWon()).isEqualTo(2);
    }
}
