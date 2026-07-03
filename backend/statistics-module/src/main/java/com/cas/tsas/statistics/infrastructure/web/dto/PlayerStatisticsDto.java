package com.cas.tsas.statistics.infrastructure.web.dto;

import com.cas.tsas.match.domain.model.StrokeType;
import com.cas.tsas.statistics.domain.model.PlayerStatistics;

public record PlayerStatisticsDto(
        int pointsWon,
        int winners,
        int unforcedErrors,
        int forcedErrors,
        int aces,
        int doubleFaults,
        double firstServePercentage,
        double secondServePercentage,
        int breakPointsWon,
        int breakPointsFaced,
        double forehandPercentage
) {
    public static PlayerStatisticsDto from(PlayerStatistics s) {
        var strokes = s.strokeDistribution().counts();
        int strokeTotal = strokes.values().stream().mapToInt(Integer::intValue).sum();
        double forehandPct = strokeTotal == 0 ? 0.0
                : (double) strokes.getOrDefault(StrokeType.FOREHAND, 0) / strokeTotal;
        return new PlayerStatisticsDto(
                s.pointsWon(), s.winners(), s.unforcedErrors(), s.forcedErrors(),
                s.aces(), s.doubleFaults(), s.firstServePercentage(), s.secondServePercentage(),
                s.breakPointsWon(), s.breakPointsFaced(), forehandPct);
    }
}
