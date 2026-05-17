package com.cas.tsas.statistics.application.service;

import com.cas.tsas.match.application.port.out.LoadPointsByMatchPort;
import com.cas.tsas.match.domain.model.Direction;
import com.cas.tsas.match.domain.model.Point;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.match.domain.model.StrokeType;
import com.cas.tsas.statistics.application.port.in.ComputeMatchStatisticsUseCase;
import com.cas.tsas.statistics.domain.PointAttribution;
import com.cas.tsas.statistics.domain.model.DirectionDistribution;
import com.cas.tsas.statistics.domain.model.MatchStatistics;
import com.cas.tsas.statistics.domain.model.PlayerStatistics;
import com.cas.tsas.statistics.domain.model.StrokeDistribution;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MatchStatisticsService implements ComputeMatchStatisticsUseCase {

    private final LoadPointsByMatchPort loadPointsByMatchPort;

    public MatchStatisticsService(LoadPointsByMatchPort loadPointsByMatchPort) {
        this.loadPointsByMatchPort = loadPointsByMatchPort;
    }

    @Override
    public MatchStatistics compute(UUID matchId) {
        List<Point> points = loadPointsByMatchPort.loadPointsByMatch(matchId);
        Accumulator acc1 = new Accumulator(1);
        Accumulator acc2 = new Accumulator(2);
        int breakPointsTotal = 0;

        for (Point p : points) {
            int attribTo = PointAttribution.attributingPlayer(p);
            Accumulator attribAcc = attribTo == 1 ? acc1 : acc2;
            attribAcc.countAttributed(p);

            if (p.getWinner() == 1) {
                acc1.pointsWon++;
            } else {
                acc2.pointsWon++;
            }

            if (p.getStrokeType() != null) {
                attribAcc.strokes.merge(p.getStrokeType(), 1, Integer::sum);
            }
            if (p.getDirection() != null) {
                attribAcc.directions.merge(p.getDirection(), 1, Integer::sum);
            }

            if (p.getServingPlayer() != null && p.getServeAttempt() != null) {
                Accumulator serverAcc = p.getServingPlayer() == 1 ? acc1 : acc2;
                serverAcc.serveAttemptsTotal++;
                if (p.getServeAttempt() == 1) {
                    serverAcc.firstServesIn++;
                } else if (p.getServeAttempt() == 2) {
                    serverAcc.secondServesPlayed++;
                    if (p.getPointType() != PointType.DOUBLE_FAULT) {
                        serverAcc.secondServesIn++;
                    }
                }
            }

            if (p.isBreakPoint() && p.getServingPlayer() != null) {
                breakPointsTotal++;
                Accumulator serverAcc = p.getServingPlayer() == 1 ? acc1 : acc2;
                Accumulator returnerAcc = p.getServingPlayer() == 1 ? acc2 : acc1;
                serverAcc.breakPointsFaced++;
                if (p.getWinner() != p.getServingPlayer()) {
                    returnerAcc.breakPointsWon++;
                }
            }
        }

        return new MatchStatistics(matchId, acc1.toStats(), acc2.toStats(),
                points.size(), breakPointsTotal, Instant.now());
    }

    private static final class Accumulator {
        final int playerNumber;
        int pointsWon, winners, unforcedErrors, forcedErrors, aces, doubleFaults;
        int breakPointsWon, breakPointsFaced;
        int serveAttemptsTotal, firstServesIn, secondServesPlayed, secondServesIn;
        final Map<StrokeType, Integer> strokes = new EnumMap<>(StrokeType.class);
        final Map<Direction, Integer> directions = new EnumMap<>(Direction.class);

        Accumulator(int n) {
            this.playerNumber = n;
        }

        void countAttributed(Point p) {
            switch (p.getPointType()) {
                case WINNER -> winners++;
                case UNFORCED_ERROR -> unforcedErrors++;
                case FORCED_ERROR -> forcedErrors++;
                case ACE -> aces++;
                case DOUBLE_FAULT -> doubleFaults++;
                case NET, OUT_LONG, OUT_SIDE -> { /* tracked via stroke/direction distribution */ }
            }
        }

        PlayerStatistics toStats() {
            double firstPct = serveAttemptsTotal == 0 ? 0.0 : (double) firstServesIn / serveAttemptsTotal;
            double secondPct = secondServesPlayed == 0 ? 0.0 : (double) secondServesIn / secondServesPlayed;
            return new PlayerStatistics(
                    playerNumber, pointsWon, winners, unforcedErrors, forcedErrors,
                    aces, doubleFaults, firstPct, secondPct, breakPointsWon, breakPointsFaced,
                    new StrokeDistribution(Map.copyOf(strokes)),
                    new DirectionDistribution(Map.copyOf(directions)));
        }
    }
}
