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
import com.cas.tsas.statistics.domain.model.MatchStatisticsBreakdown;
import com.cas.tsas.statistics.domain.model.PlayerStatistics;
import com.cas.tsas.statistics.domain.model.SetStatistics;
import com.cas.tsas.statistics.domain.model.StrokeDistribution;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Computes match statistics on the fly from the recorded points; nothing is persisted.
 *
 * <p>Each point is attributed to a player via {@link PointAttribution} and the raw counts are
 * accumulated per player, then derived into the {@link MatchStatistics} aggregate.
 */
@Service
public class MatchStatisticsService implements ComputeMatchStatisticsUseCase {

    private final LoadPointsByMatchPort loadPointsByMatchPort;

    public MatchStatisticsService(LoadPointsByMatchPort loadPointsByMatchPort) {
        this.loadPointsByMatchPort = loadPointsByMatchPort;
    }

    @Override
    public MatchStatistics compute(UUID matchId) {
        return computeFrom(matchId, loadPointsByMatchPort.loadPointsByMatch(matchId));
    }

    @Override
    public MatchStatisticsBreakdown computeBreakdown(UUID matchId) {
        List<Point> points = loadPointsByMatchPort.loadPointsByMatch(matchId);
        MatchStatistics total = computeFrom(matchId, points);
        List<SetStatistics> sets = points.stream()
                .collect(Collectors.groupingBy(Point::getSetNumber, TreeMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> new SetStatistics(e.getKey(), computeFrom(matchId, e.getValue())))
                .toList();
        return new MatchStatisticsBreakdown(total, sets);
    }

    /** Accumulates per-player statistics from the given points in a single pass. */
    private MatchStatistics computeFrom(UUID matchId, List<Point> points) {
        Accumulator acc1 = new Accumulator(1);
        Accumulator acc2 = new Accumulator(2);
        int breakPointsTotal = 0;

        for (Point p : points) {
            if (p.getPointType() == null) {
                if (p.getWinner() == 1) acc1.pointsWon++;
                else acc2.pointsWon++;
                continue;
            }

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

    /** Mutable per-player tally of raw counters collected while iterating the points. */
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

        /** Increments the counter matching the point type; NET/OUT outcomes are only reflected
         *  in the stroke/direction distribution, not in a dedicated counter. */
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

        /** Derives the serve percentages from the raw counters and builds the immutable
         *  {@link PlayerStatistics}; percentages default to 0 when no serves were attempted. */
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
