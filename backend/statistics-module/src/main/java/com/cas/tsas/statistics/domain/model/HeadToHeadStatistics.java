package com.cas.tsas.statistics.domain.model;

import java.util.UUID;

/**
 * Result aggregate for FA-08: two players compared over all their shared matches.
 * {@code matchesPlayed} counts only completed matches (those with a decided winner).
 */
public record HeadToHeadStatistics(
        UUID player1Id,
        UUID player2Id,
        int matchesPlayed,
        HeadToHeadPlayerStats player1,
        HeadToHeadPlayerStats player2
) {}
