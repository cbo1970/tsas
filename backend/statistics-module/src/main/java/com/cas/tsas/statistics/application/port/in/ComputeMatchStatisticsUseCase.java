package com.cas.tsas.statistics.application.port.in;

import com.cas.tsas.statistics.domain.model.MatchStatistics;
import com.cas.tsas.statistics.domain.model.MatchStatisticsBreakdown;

import java.util.UUID;

public interface ComputeMatchStatisticsUseCase {
    MatchStatistics compute(UUID matchId);

    /** Total statistics + a per-set breakdown (one entry per played set, ascending). */
    MatchStatisticsBreakdown computeBreakdown(UUID matchId);
}
