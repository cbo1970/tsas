package com.cas.tsas.statistics.domain.model;

public record PlayerStatistics(
        int playerNumber,
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
        StrokeDistribution strokeDistribution,
        DirectionDistribution directionDistribution
) {}
