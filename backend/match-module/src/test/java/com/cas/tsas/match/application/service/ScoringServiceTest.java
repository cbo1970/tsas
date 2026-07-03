package com.cas.tsas.match.application.service;

import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchScore;
import com.cas.tsas.match.domain.model.MatchStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ScoringServiceTest {

    private ScoringService scoringService;

    @BeforeEach
    void setUp() {
        scoringService = new ScoringService();
    }

    // -------------------------------------------------------------------------
    // Factories
    // -------------------------------------------------------------------------

    private static Match normalMatch() {
        return new Match(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                2, false, false, MatchStatus.IN_PROGRESS);
    }

    private static Match matchWithMatchTiebreak() {
        return new Match(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                2, true, false, MatchStatus.IN_PROGRESS);
    }

    private static Match shortSetMatch() {
        return new Match(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                2, false, true, MatchStatus.IN_PROGRESS);
    }

    /** Creates a score with all fields configurable; deuce/advantage/done start at defaults. */
    private static MatchScore score(int pointsP1, int pointsP2,
                                    int gamesP1, int gamesP2,
                                    int setsP1, int setsP2) {
        return new MatchScore(null, UUID.randomUUID(),
                pointsP1, pointsP2, gamesP1, gamesP2, setsP1, setsP2,
                false, null, 1, false, null, 0, 0, null);
    }

    private static MatchScore freshScore() {
        return score(0, 0, 0, 0, 0, 0);
    }

    // =========================================================================
    @Nested
    class PointProgression {

        @Test
        void player1_first_point_advances_from_0_to_15() {
            MatchScore s = freshScore();
            scoringService.applyPoint(normalMatch(), s, true);
            assertThat(s.getPointsPlayer1()).isEqualTo(1);
            assertThat(s.getPointsPlayer2()).isEqualTo(0);
        }

        @Test
        void player1_advances_from_30_to_40() {
            MatchScore s = score(2, 0, 0, 0, 0, 0);
            scoringService.applyPoint(normalMatch(), s, true);
            assertThat(s.getPointsPlayer1()).isEqualTo(3);
        }

        @Test
        void player2_first_point_advances_from_0_to_15() {
            MatchScore s = freshScore();
            scoringService.applyPoint(normalMatch(), s, false);
            assertThat(s.getPointsPlayer2()).isEqualTo(1);
            assertThat(s.getPointsPlayer1()).isEqualTo(0);
        }
    }

    // =========================================================================
    @Nested
    class GameWin {

        @Test
        void player1_wins_game_at_40_when_opponent_below_40() {
            MatchScore s = score(3, 1, 0, 0, 0, 0); // 40:15
            scoringService.applyPoint(normalMatch(), s, true);
            assertThat(s.getGamesPlayer1()).isEqualTo(1);
        }

        @Test
        void player2_wins_game_at_40_when_opponent_below_40() {
            MatchScore s = score(0, 3, 0, 0, 0, 0); // 0:40
            scoringService.applyPoint(normalMatch(), s, false);
            assertThat(s.getGamesPlayer2()).isEqualTo(1);
        }

        @Test
        void winning_game_resets_both_points_to_zero() {
            MatchScore s = score(3, 0, 0, 0, 0, 0);
            scoringService.applyPoint(normalMatch(), s, true);
            assertThat(s.getPointsPlayer1()).isEqualTo(0);
            assertThat(s.getPointsPlayer2()).isEqualTo(0);
        }
    }

    // =========================================================================
    @Nested
    class DeuceAndAdvantage {

        @Test
        void both_at_40_creates_deuce() {
            // Code sets isDeuce reactively: deuce is triggered on the next point
            // scored when both players are already at 40 (internal value 3).
            MatchScore s = score(3, 3, 0, 0, 0, 0); // both at 40
            scoringService.applyPoint(normalMatch(), s, true); // p1 tries to score → deuce
            assertThat(s.isDeuce()).isTrue();
            assertThat(s.getIsAdvantagePlayer1()).isNull();
        }

        @Test
        void first_point_after_deuce_grants_advantage_to_player1() {
            MatchScore s = score(3, 3, 0, 0, 0, 0);
            s.setDeuce(true);
            scoringService.applyPoint(normalMatch(), s, true);
            assertThat(s.getIsAdvantagePlayer1()).isTrue();
        }

        @Test
        void first_point_after_deuce_grants_advantage_to_player2() {
            MatchScore s = score(3, 3, 0, 0, 0, 0);
            s.setDeuce(true);
            scoringService.applyPoint(normalMatch(), s, false);
            assertThat(s.getIsAdvantagePlayer1()).isFalse();
        }

        @Test
        void opponent_of_advantage_player_equalises_back_to_deuce() {
            MatchScore s = score(3, 3, 0, 0, 0, 0);
            s.setDeuce(true);
            s.setIsAdvantagePlayer1(true); // advantage p1
            scoringService.applyPoint(normalMatch(), s, false); // p2 wins
            assertThat(s.isDeuce()).isTrue();
            assertThat(s.getIsAdvantagePlayer1()).isNull();
        }

        @Test
        void advantage_player1_wins_next_point_wins_game() {
            MatchScore s = score(3, 3, 0, 0, 0, 0);
            s.setDeuce(true);
            s.setIsAdvantagePlayer1(true);
            scoringService.applyPoint(normalMatch(), s, true);
            assertThat(s.getGamesPlayer1()).isEqualTo(1);
            assertThat(s.isDeuce()).isFalse();
        }

        @Test
        void advantage_player2_wins_next_point_wins_game() {
            MatchScore s = score(3, 3, 0, 0, 0, 0);
            s.setDeuce(true);
            s.setIsAdvantagePlayer1(false);
            scoringService.applyPoint(normalMatch(), s, false);
            assertThat(s.getGamesPlayer2()).isEqualTo(1);
        }
    }

    // =========================================================================
    @Nested
    class SetWin {

        @Test
        void player1_wins_set_at_6_4() {
            MatchScore s = score(3, 0, 5, 4, 0, 0); // p1 at 40, games 5:4
            scoringService.applyPoint(normalMatch(), s, true); // 6:4
            assertThat(s.getSetsPlayer1()).isEqualTo(1);
        }

        @Test
        void player1_wins_set_at_7_5() {
            MatchScore s = score(3, 0, 6, 5, 0, 0); // games 6:5
            scoringService.applyPoint(normalMatch(), s, true); // 7:5
            assertThat(s.getSetsPlayer1()).isEqualTo(1);
        }

        @Test
        void player2_wins_set_at_5_7() {
            MatchScore s = score(0, 3, 5, 6, 0, 0); // games 5:6
            scoringService.applyPoint(normalMatch(), s, false); // 5:7
            assertThat(s.getSetsPlayer2()).isEqualTo(1);
        }

        @Test
        void winning_set_resets_games_to_zero() {
            MatchScore s = score(3, 0, 5, 3, 0, 0); // games 5:3 → 6:3
            scoringService.applyPoint(normalMatch(), s, true);
            assertThat(s.getGamesPlayer1()).isEqualTo(0);
            assertThat(s.getGamesPlayer2()).isEqualTo(0);
        }

        @Test
        void winning_set_increments_current_set_counter() {
            MatchScore s = score(3, 0, 5, 3, 0, 0);
            scoringService.applyPoint(normalMatch(), s, true);
            assertThat(s.getCurrentSet()).isEqualTo(2);
        }
    }

    // =========================================================================
    @Nested
    class RegularTiebreak {

        @Test
        void tiebreak_at_6_6_accumulates_tiebreak_points_not_game_counter() {
            MatchScore s = score(0, 0, 6, 6, 0, 0);
            scoringService.applyPoint(normalMatch(), s, true);
            assertThat(s.getPointsPlayer1()).isEqualTo(1);
            assertThat(s.getGamesPlayer1()).isEqualTo(6); // unchanged
        }

        @Test
        void tiebreak_won_at_7_with_2_point_lead() {
            MatchScore s = score(6, 0, 6, 6, 0, 0); // 6:0 in tiebreak
            scoringService.applyPoint(normalMatch(), s, true); // 7:0
            assertThat(s.getSetsPlayer1()).isEqualTo(1);
        }

        @Test
        void tiebreak_not_won_at_7_when_opponent_is_at_6() {
            MatchScore s = score(6, 6, 6, 6, 0, 0); // 6:6 in tiebreak
            scoringService.applyPoint(normalMatch(), s, true); // 7:6 — not won
            assertThat(s.getSetsPlayer1()).isEqualTo(0);
            assertThat(s.getPointsPlayer1()).isEqualTo(7);
        }

        @Test
        void tiebreak_won_at_8_6_after_extension() {
            MatchScore s = score(7, 6, 6, 6, 0, 0); // 7:6 in tiebreak
            scoringService.applyPoint(normalMatch(), s, true); // 8:6
            assertThat(s.getSetsPlayer1()).isEqualTo(1);
        }
    }

    // =========================================================================
    @Nested
    class ShortSet {

        @Test
        void short_set_won_at_4_games_with_2_lead() {
            MatchScore s = score(3, 0, 3, 1, 0, 0); // games 3:1
            scoringService.applyPoint(shortSetMatch(), s, true); // 4:1
            assertThat(s.getSetsPlayer1()).isEqualTo(1);
        }

        @Test
        void short_set_tiebreak_activates_at_3_3() {
            MatchScore s = score(0, 0, 3, 3, 0, 0);
            scoringService.applyPoint(shortSetMatch(), s, true);
            assertThat(s.getPointsPlayer1()).isEqualTo(1); // tiebreak point
            assertThat(s.getGamesPlayer1()).isEqualTo(3);  // games unchanged
        }

        @Test
        void short_set_tiebreak_won_at_7_points() {
            MatchScore s = score(6, 0, 3, 3, 0, 0);
            scoringService.applyPoint(shortSetMatch(), s, true); // 7:0
            assertThat(s.getSetsPlayer1()).isEqualTo(1);
        }
    }

    // =========================================================================
    @Nested
    class MatchTiebreak {

        @Test
        void match_tiebreak_active_when_sets_tied_at_required_minus_1() {
            // At sets 1:1 with setsToWin=2, every point goes to 10-point tiebreak.
            // 6 tiebreak points should NOT win (target=10), whereas regular tiebreak would (target=7).
            MatchScore s = score(6, 0, 0, 0, 1, 1);
            scoringService.applyPoint(matchWithMatchTiebreak(), s, true); // 7:0
            assertThat(s.isDone()).isFalse();
            assertThat(s.getPointsPlayer1()).isEqualTo(7);
        }

        @Test
        void match_tiebreak_won_at_10_with_2_point_lead() {
            MatchScore s = score(9, 0, 0, 0, 1, 1);
            scoringService.applyPoint(matchWithMatchTiebreak(), s, true); // 10:0
            assertThat(s.isDone()).isTrue();
            assertThat(s.getWinner()).isEqualTo("PLAYER1");
        }

        @Test
        void match_tiebreak_not_won_at_10_when_opponent_is_at_9() {
            MatchScore s = score(9, 9, 0, 0, 1, 1);
            scoringService.applyPoint(matchWithMatchTiebreak(), s, true); // 10:9
            assertThat(s.isDone()).isFalse();
        }

        @Test
        void match_tiebreak_won_at_11_9_after_extension() {
            MatchScore s = score(10, 9, 0, 0, 1, 1);
            scoringService.applyPoint(matchWithMatchTiebreak(), s, true); // 11:9
            assertThat(s.isDone()).isTrue();
            assertThat(s.getWinner()).isEqualTo("PLAYER1");
        }
    }

    // =========================================================================
    @Nested
    class MatchCompletion {

        @Test
        void match_done_when_player1_wins_required_sets() {
            // sets 1:0, games 5:3 → p1 wins game → set → 2nd set → done
            MatchScore s = score(3, 0, 5, 3, 1, 0);
            scoringService.applyPoint(normalMatch(), s, true);
            assertThat(s.isDone()).isTrue();
            assertThat(s.getWinner()).isEqualTo("PLAYER1");
        }

        @Test
        void match_done_when_player2_wins_required_sets() {
            // sets 0:1, games 3:5 → p2 wins game → set → 2nd set → done
            MatchScore s = score(0, 3, 3, 5, 0, 1);
            scoringService.applyPoint(normalMatch(), s, false);
            assertThat(s.isDone()).isTrue();
            assertThat(s.getWinner()).isEqualTo("PLAYER2");
        }

        @Test
        void done_score_is_not_modified() {
            MatchScore s = freshScore();
            s.setDone(true);
            s.setWinner("PLAYER1");

            scoringService.applyPoint(normalMatch(), s, true);

            assertThat(s.getPointsPlayer1()).isEqualTo(0);
            assertThat(s.getWinner()).isEqualTo("PLAYER1");
        }
    }

    // =========================================================================
    @Nested
    class ServeRotation {

        @Test
        void serve_rotates_to_player2_after_player1_wins_game() {
            Match match = normalMatch();
            MatchScore s = score(3, 0, 0, 0, 0, 0);
            s.setServingPlayer(1);

            scoringService.applyPoint(match, s, true); // player1 wins game (40:0 → game)

            assertThat(s.getServingPlayer()).isEqualTo(2);
        }

        @Test
        void serve_rotates_to_player1_after_player2_wins_game() {
            Match match = normalMatch();
            MatchScore s = score(0, 3, 0, 0, 0, 0);
            s.setServingPlayer(2);

            scoringService.applyPoint(match, s, false); // player2 wins game

            assertThat(s.getServingPlayer()).isEqualTo(1);
        }

        @Test
        void serve_not_rotated_when_serving_player_is_null() {
            Match match = normalMatch();
            MatchScore s = score(3, 0, 0, 0, 0, 0);
            // servingPlayer is null (default from factory)

            scoringService.applyPoint(match, s, true);

            assertThat(s.getServingPlayer()).isNull();
        }

        @Test
        void serve_rotates_after_regular_tiebreak_win() {
            // Tiebreak at 6:4 points — one more point wins for player1 (7:4 → tiebreak won)
            Match match = normalMatch();
            MatchScore s = score(6, 4, 6, 6, 0, 0);
            s.setServingPlayer(1);

            scoringService.applyPoint(match, s, true);

            assertThat(s.getServingPlayer()).isEqualTo(2);
        }
    }
}
