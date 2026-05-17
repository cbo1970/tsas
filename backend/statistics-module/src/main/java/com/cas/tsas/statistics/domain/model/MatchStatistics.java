package com.cas.tsas.statistics.domain.model;

import java.time.Instant;
import java.util.UUID;

public record MatchStatistics(
        UUID matchId,
        PlayerStatistics player1,
        PlayerStatistics player2,
        int totalPoints,
        int breakPointsTotal,
        Instant computedAt
) {}
