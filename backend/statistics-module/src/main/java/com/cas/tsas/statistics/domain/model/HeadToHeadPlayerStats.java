package com.cas.tsas.statistics.domain.model;

import java.util.UUID;

/**
 * Aggregated head-to-head statistics for one player across all matches against one opponent.
 * Carries both raw counts and derived percentages so the API can present "23 (45%)".
 * Net points are intentionally not part of FA-08.
 */
public record HeadToHeadPlayerStats(
        UUID playerId,
        // serve
        double firstServePercentage,
        double firstServeWonPercentage,
        double secondServeWonPercentage,
        int aces,
        int doubleFaults,
        // return
        double returnPointsWonFirstPercentage,
        double returnPointsWonSecondPercentage,
        int breakPointsWon,
        int breakPointsPlayed,
        double breakPointsWonPercentage,
        double returnGamesWonPercentage,
        // rally
        int winners,
        int unforcedErrors,
        double winnersPercentage,
        double unforcedErrorPercentage,
        // balance (completed matches only)
        int matchesWon,
        int matchesLost,
        int setsWon,
        int setsLost
) {}
