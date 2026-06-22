package com.cas.tsas.statistics.application.service;

import com.cas.tsas.auth.application.port.in.CurrentUserProvider;
import com.cas.tsas.auth.domain.CurrentUser;
import com.cas.tsas.auth.domain.Role;
import com.cas.tsas.match.application.port.out.LoadMatchScorePort;
import com.cas.tsas.match.application.port.out.LoadMatchesByPlayersPort;
import com.cas.tsas.match.application.port.out.LoadPointsByMatchPort;
import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchScore;
import com.cas.tsas.match.domain.model.Point;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.player.domain.exception.PlayerNotFoundException;
import com.cas.tsas.statistics.application.port.in.ComputeHeadToHeadStatisticsUseCase;
import com.cas.tsas.statistics.domain.model.HeadToHeadPlayerStats;
import com.cas.tsas.statistics.domain.model.HeadToHeadStatistics;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Computes head-to-head statistics (FA-08) on the fly from the recorded points of all matches
 * two players have played against each other. Raw counters are accumulated per real player UUID
 * (each match's positional player 1/2 is mapped to the actual player), then derived into
 * percentages. Match/set balance is taken from the persisted {@link MatchScore} of completed
 * matches only.
 *
 * <p>Access is owner-bound: a non-admin caller must own both players, otherwise a
 * {@link PlayerNotFoundException} is raised (404). Matches considered for the aggregation are
 * filtered to the caller's owned matches; admins bypass both filters.
 */
@Service
public class HeadToHeadStatisticsService implements ComputeHeadToHeadStatisticsUseCase {

    private final LoadPlayerPort loadPlayerPort;
    private final LoadMatchesByPlayersPort loadMatchesByPlayersPort;
    private final LoadPointsByMatchPort loadPointsByMatchPort;
    private final LoadMatchScorePort loadMatchScorePort;
    private final CurrentUserProvider currentUserProvider;

    public HeadToHeadStatisticsService(LoadPlayerPort loadPlayerPort,
                                       LoadMatchesByPlayersPort loadMatchesByPlayersPort,
                                       LoadPointsByMatchPort loadPointsByMatchPort,
                                       LoadMatchScorePort loadMatchScorePort,
                                       CurrentUserProvider currentUserProvider) {
        this.loadPlayerPort = loadPlayerPort;
        this.loadMatchesByPlayersPort = loadMatchesByPlayersPort;
        this.loadPointsByMatchPort = loadPointsByMatchPort;
        this.loadMatchScorePort = loadMatchScorePort;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public HeadToHeadStatistics compute(UUID player1Id, UUID player2Id) {
        if (player1Id.equals(player2Id)) {
            throw new IllegalArgumentException("player1 and player2 must be different");
        }
        CurrentUser current = currentUserProvider.get();
        boolean admin = current.hasRole(Role.ADMIN);
        if (admin) {
            loadPlayerPort.loadPlayer(player1Id)
                    .orElseThrow(() -> new PlayerNotFoundException(player1Id));
            loadPlayerPort.loadPlayer(player2Id)
                    .orElseThrow(() -> new PlayerNotFoundException(player2Id));
        } else {
            loadPlayerPort.findByIdAndOwner(player1Id, current.id())
                    .orElseThrow(() -> new PlayerNotFoundException(player1Id));
            loadPlayerPort.findByIdAndOwner(player2Id, current.id())
                    .orElseThrow(() -> new PlayerNotFoundException(player2Id));
        }

        Accumulator acc1 = new Accumulator();
        Accumulator acc2 = new Accumulator();
        int totalPoints = 0;
        int matchesPlayed = 0;

        List<Match> matches = loadMatchesByPlayersPort.loadMatchesBetween(player1Id, player2Id);
        for (Match match : matches) {
            if (!admin && !current.id().equals(match.getOwnerId())) {
                continue;
            }
            boolean p1IsPosition1 = match.getPlayer1Id().equals(player1Id);
            Accumulator pos1 = p1IsPosition1 ? acc1 : acc2;
            Accumulator pos2 = p1IsPosition1 ? acc2 : acc1;

            List<Point> points = loadPointsByMatchPort.loadPointsByMatch(match.getId());
            for (Point p : points) {
                totalPoints++;
                accumulatePoint(p, pos1, pos2);
            }
            accumulateReturnGames(points, pos1, pos2);

            Optional<MatchScore> score = loadMatchScorePort.loadMatchScore(match.getId());
            if (score.isPresent() && score.get().getWinner() != null) {
                matchesPlayed++;
                accumulateBalance(score.get(), pos1, pos2);
            }
        }

        return new HeadToHeadStatistics(player1Id, player2Id, matchesPlayed,
                acc1.toStats(player1Id, totalPoints), acc2.toStats(player2Id, totalPoints));
    }

    private void accumulatePoint(Point p, Accumulator pos1, Accumulator pos2) {
        Accumulator winnerAcc = p.getWinner() == 1 ? pos1 : pos2;
        Accumulator loserAcc = p.getWinner() == 1 ? pos2 : pos1;

        if (p.getPointType() == PointType.WINNER) {
            winnerAcc.winners++;
        } else if (p.getPointType() == PointType.UNFORCED_ERROR) {
            loserAcc.unforcedErrors++;
        }

        Integer server = p.getServingPlayer();
        if (server != null) {
            Accumulator serverAcc = server == 1 ? pos1 : pos2;
            Accumulator returnerAcc = server == 1 ? pos2 : pos1;
            if (p.getPointType() == PointType.ACE) serverAcc.aces++;
            if (p.getPointType() == PointType.DOUBLE_FAULT) serverAcc.doubleFaults++;

            Integer attempt = p.getServeAttempt();
            if (attempt != null) {
                serverAcc.serveAttemptsTotal++;
                boolean serverWon = p.getWinner() == server;
                if (attempt == 1) {
                    serverAcc.firstServesIn++;
                    serverAcc.firstServePlayed++;
                    returnerAcc.returnFirstPlayed++;
                    if (serverWon) serverAcc.firstServeWon++;
                    else returnerAcc.returnFirstWon++;
                } else if (attempt == 2) {
                    serverAcc.secondServePlayed++;
                    returnerAcc.returnSecondPlayed++;
                    if (serverWon) serverAcc.secondServeWon++;
                    else returnerAcc.returnSecondWon++;
                }
            }

            if (p.isBreakPoint()) {
                returnerAcc.breakPointsPlayed++;
                if (p.getWinner() != server) returnerAcc.breakPointsWon++;
            }
        }
    }

    /**
     * Credits return games: a game is grouped by (set, game); its server is the first non-null
     * servingPlayer seen, its winner is the winner of the highest-numbered point. The player who
     * did not serve the game played a return game there, and won it if they won the last point.
     */
    private void accumulateReturnGames(List<Point> points, Accumulator pos1, Accumulator pos2) {
        int i = 0;
        while (i < points.size()) {
            Point first = points.get(i);
            int set = first.getSetNumber();
            int game = first.getGameNumber();
            Integer server = null;
            int lastWinner = first.getWinner();
            int j = i;
            while (j < points.size()
                    && points.get(j).getSetNumber() == set
                    && points.get(j).getGameNumber() == game) {
                if (server == null) server = points.get(j).getServingPlayer();
                lastWinner = points.get(j).getWinner();
                j++;
            }
            if (server != null) {
                Accumulator returnerAcc = server == 1 ? pos2 : pos1;
                int returnerPosition = server == 1 ? 2 : 1;
                returnerAcc.returnGamesPlayed++;
                if (lastWinner == returnerPosition) returnerAcc.returnGamesWon++;
            }
            i = j;
        }
    }

    private void accumulateBalance(MatchScore score, Accumulator pos1, Accumulator pos2) {
        pos1.setsWon += score.getSetsPlayer1();
        pos1.setsLost += score.getSetsPlayer2();
        pos2.setsWon += score.getSetsPlayer2();
        pos2.setsLost += score.getSetsPlayer1();
        if ("PLAYER1".equals(score.getWinner())) {
            pos1.matchesWon++;
            pos2.matchesLost++;
        } else if ("PLAYER2".equals(score.getWinner())) {
            pos2.matchesWon++;
            pos1.matchesLost++;
        }
    }

    /** Mutable per-player tally; derived into the immutable {@link HeadToHeadPlayerStats}. */
    private static final class Accumulator {
        int winners, unforcedErrors, aces, doubleFaults;
        int firstServesIn, serveAttemptsTotal;
        int firstServeWon, firstServePlayed;
        int secondServeWon, secondServePlayed;
        int returnFirstWon, returnFirstPlayed;
        int returnSecondWon, returnSecondPlayed;
        int breakPointsWon, breakPointsPlayed;
        int returnGamesWon, returnGamesPlayed;
        int matchesWon, matchesLost, setsWon, setsLost;

        HeadToHeadPlayerStats toStats(UUID playerId, int totalPoints) {
            return new HeadToHeadPlayerStats(
                    playerId,
                    ratio(firstServesIn, serveAttemptsTotal),
                    ratio(firstServeWon, firstServePlayed),
                    ratio(secondServeWon, secondServePlayed),
                    aces,
                    doubleFaults,
                    ratio(returnFirstWon, returnFirstPlayed),
                    ratio(returnSecondWon, returnSecondPlayed),
                    breakPointsWon,
                    breakPointsPlayed,
                    ratio(breakPointsWon, breakPointsPlayed),
                    ratio(returnGamesWon, returnGamesPlayed),
                    winners,
                    unforcedErrors,
                    ratio(winners, totalPoints),
                    ratio(unforcedErrors, totalPoints),
                    matchesWon,
                    matchesLost,
                    setsWon,
                    setsLost);
        }

        private static double ratio(int num, int den) {
            return den == 0 ? 0.0 : (double) num / den;
        }
    }
}
