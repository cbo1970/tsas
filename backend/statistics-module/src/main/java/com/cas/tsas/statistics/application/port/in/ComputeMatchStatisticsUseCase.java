package com.cas.tsas.statistics.application.port.in;

import com.cas.tsas.statistics.domain.model.MatchStatistics;

import java.util.UUID;

public interface ComputeMatchStatisticsUseCase {
    MatchStatistics compute(UUID matchId);
}
