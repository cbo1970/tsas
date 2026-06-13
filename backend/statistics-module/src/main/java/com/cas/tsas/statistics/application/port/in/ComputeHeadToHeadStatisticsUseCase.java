package com.cas.tsas.statistics.application.port.in;

import com.cas.tsas.statistics.domain.model.HeadToHeadStatistics;

import java.util.UUID;

public interface ComputeHeadToHeadStatisticsUseCase {

    /**
     * Aggregates head-to-head statistics for the two players over all their shared matches.
     *
     * @throws IllegalArgumentException if both ids are equal (→ 400)
     * @throws com.cas.tsas.player.domain.exception.PlayerNotFoundException if a player does not exist (→ 404)
     */
    HeadToHeadStatistics compute(UUID player1Id, UUID player2Id);
}
