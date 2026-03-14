package com.cas.tsas.application.service;

import com.cas.tsas.domain.model.Match;
import com.cas.tsas.domain.model.MatchScore;
import com.cas.tsas.domain.model.MatchStatus;
import org.springframework.stereotype.Component;

/**
 * Implements tennis scoring rules.
 * Points: 0 -> 15 -> 30 -> 40 -> Deuce -> Advantage -> Game
 * Tiebreak: first to 7 with 2-point lead (or 10 for match tiebreak)
 * Short Set: games to win = 4, tiebreak at 3:3
 * Normal Set: win at 6 with 2-game lead, or 7:5, tiebreak at 6:6
 */
@Component
public class ScoringService {

    /**
     * Apply a point to the score and return the updated score.
     * The score object is mutated in place and returned.
     */
    public MatchScore applyPoint(Match match, MatchScore score, boolean player1Scored) {
        if (score.isDone()) {
            return score;
        }

        boolean isTiebreakActive = isTiebreak(match, score);
        boolean isMatchTiebreakActive = isMatchTiebreak(match, score);

        if (isTiebreakActive || isMatchTiebreakActive) {
            applyTiebreakPoint(match, score, player1Scored, isMatchTiebreakActive);
        } else {
            applyRegularPoint(match, score, player1Scored);
        }

        return score;
    }

    private void applyRegularPoint(Match match, MatchScore score, boolean player1Scored) {
        // Handle deuce/advantage
        if (score.isDeuce()) {
            if (score.getIsAdvantagePlayer1() == null) {
                // At deuce, grant advantage
                score.setIsAdvantagePlayer1(player1Scored);
            } else {
                boolean advantageP1 = score.getIsAdvantagePlayer1();
                if ((player1Scored && advantageP1) || (!player1Scored && !advantageP1)) {
                    // Advantage player wins the game
                    awardGame(match, score, player1Scored);
                } else {
                    // Back to deuce
                    score.setIsAdvantagePlayer1(null);
                }
            }
            return;
        }

        // Normal point progression
        if (player1Scored) {
            int pts = score.getPointsPlayer1();
            if (pts < 3) {
                score.setPointsPlayer1(pts + 1);
            } else {
                // pts == 3 means 40
                if (score.getPointsPlayer2() == 3) {
                    // Both at 40: deuce
                    score.setDeuce(true);
                    score.setIsAdvantagePlayer1(null);
                } else {
                    awardGame(match, score, true);
                }
            }
        } else {
            int pts = score.getPointsPlayer2();
            if (pts < 3) {
                score.setPointsPlayer2(pts + 1);
            } else {
                if (score.getPointsPlayer1() == 3) {
                    score.setDeuce(true);
                    score.setIsAdvantagePlayer1(null);
                } else {
                    awardGame(match, score, false);
                }
            }
        }
    }

    private void awardGame(Match match, MatchScore score, boolean player1Scored) {
        // Reset points
        score.setPointsPlayer1(0);
        score.setPointsPlayer2(0);
        score.setDeuce(false);
        score.setIsAdvantagePlayer1(null);

        if (player1Scored) {
            score.setGamesPlayer1(score.getGamesPlayer1() + 1);
        } else {
            score.setGamesPlayer2(score.getGamesPlayer2() + 1);
        }

        checkSetWon(match, score);
    }

    private void checkSetWon(Match match, MatchScore score) {
        int g1 = score.getGamesPlayer1();
        int g2 = score.getGamesPlayer2();
        int winsNeeded = match.isShortSet() ? 4 : 6;
        int tiebreakAt = match.isShortSet() ? 3 : 6;

        boolean setWonByP1 = false;
        boolean setWonByP2 = false;

        if (g1 >= winsNeeded && g1 - g2 >= 2) {
            setWonByP1 = true;
        } else if (g1 == winsNeeded + 1 && g2 == winsNeeded - 1 && !match.isShortSet()) {
            // 7:5 case for normal sets
            setWonByP1 = true;
        } else if (g2 >= winsNeeded && g2 - g1 >= 2) {
            setWonByP2 = true;
        } else if (g2 == winsNeeded + 1 && g1 == winsNeeded - 1 && !match.isShortSet()) {
            // 5:7 case for normal sets
            setWonByP2 = true;
        } else if (g1 == tiebreakAt + 1 && g2 == tiebreakAt) {
            // Won tiebreak game: player1 won tiebreak
            setWonByP1 = true;
        } else if (g2 == tiebreakAt + 1 && g1 == tiebreakAt) {
            setWonByP2 = true;
        }

        if (setWonByP1) {
            awardSet(match, score, true);
        } else if (setWonByP2) {
            awardSet(match, score, false);
        }
    }

    private void awardSet(Match match, MatchScore score, boolean player1Scored) {
        if (player1Scored) {
            score.setSetsPlayer1(score.getSetsPlayer1() + 1);
        } else {
            score.setSetsPlayer2(score.getSetsPlayer2() + 1);
        }
        score.setGamesPlayer1(0);
        score.setGamesPlayer2(0);
        score.setCurrentSet(score.getCurrentSet() + 1);

        // Check if match tiebreak should be played next
        int s1 = score.getSetsPlayer1();
        int s2 = score.getSetsPlayer2();
        int required = match.getSetsToWin();

        if (s1 >= required) {
            score.setDone(true);
            score.setWinner("PLAYER1");
        } else if (s2 >= required) {
            score.setDone(true);
            score.setWinner("PLAYER2");
        }
        // If match tiebreak and sets are 1:1 (setsToWin=2) or 2:2 (setsToWin=3), next set is match tiebreak
        // That is handled in isMatchTiebreak check
    }

    private void applyTiebreakPoint(Match match, MatchScore score, boolean player1Scored, boolean isMatchTiebreak) {
        int target = isMatchTiebreak ? 10 : 7;

        if (player1Scored) {
            score.setPointsPlayer1(score.getPointsPlayer1() + 1);
        } else {
            score.setPointsPlayer2(score.getPointsPlayer2() + 1);
        }

        int p1 = score.getPointsPlayer1();
        int p2 = score.getPointsPlayer2();

        boolean p1Won = p1 >= target && p1 - p2 >= 2;
        boolean p2Won = p2 >= target && p2 - p1 >= 2;

        if (p1Won || p2Won) {
            score.setPointsPlayer1(0);
            score.setPointsPlayer2(0);
            score.setDeuce(false);
            score.setIsAdvantagePlayer1(null);

            if (isMatchTiebreak) {
                // Match tiebreak counts as winning the last set
                if (p1Won) {
                    score.setSetsPlayer1(score.getSetsPlayer1() + 1);
                    score.setWinner("PLAYER1");
                } else {
                    score.setSetsPlayer2(score.getSetsPlayer2() + 1);
                    score.setWinner("PLAYER2");
                }
                score.setGamesPlayer1(0);
                score.setGamesPlayer2(0);
                score.setCurrentSet(score.getCurrentSet() + 1);
                score.setDone(true);
            } else {
                // Regular tiebreak: award a game win to complete the set
                if (p1Won) {
                    score.setGamesPlayer1(score.getGamesPlayer1() + 1);
                    awardSet(match, score, true);
                } else {
                    score.setGamesPlayer2(score.getGamesPlayer2() + 1);
                    awardSet(match, score, false);
                }
            }
        }
    }

    /**
     * Returns true if we are in a regular tiebreak (not match tiebreak).
     */
    private boolean isTiebreak(Match match, MatchScore score) {
        if (score.isDone()) return false;

        int tiebreakAt = match.isShortSet() ? 3 : 6;
        int g1 = score.getGamesPlayer1();
        int g2 = score.getGamesPlayer2();

        // If both players have reached the tiebreak threshold, we're in a tiebreak
        // But only if it's NOT a match tiebreak situation
        if (g1 == tiebreakAt && g2 == tiebreakAt) {
            return !isMatchTiebreakSet(match, score);
        }
        return false;
    }

    /**
     * Returns true if we are in a match tiebreak (10-point super tiebreak).
     */
    private boolean isMatchTiebreak(Match match, MatchScore score) {
        if (!match.isMatchTiebreak()) return false;
        if (score.isDone()) return false;
        return isMatchTiebreakSet(match, score);
    }

    private boolean isMatchTiebreakSet(Match match, MatchScore score) {
        if (!match.isMatchTiebreak()) return false;
        int required = match.getSetsToWin();
        int s1 = score.getSetsPlayer1();
        int s2 = score.getSetsPlayer2();
        // Match tiebreak: when sets are (required-1):(required-1)
        return s1 == required - 1 && s2 == required - 1;
    }
}
