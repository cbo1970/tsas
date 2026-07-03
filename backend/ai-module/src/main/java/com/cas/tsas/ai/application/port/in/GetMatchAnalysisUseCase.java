package com.cas.tsas.ai.application.port.in;

import com.cas.tsas.ai.domain.model.MatchAnalysis;

import java.util.Optional;
import java.util.UUID;

public interface GetMatchAnalysisUseCase {
    Optional<MatchAnalysis> findByMatchId(UUID matchId);
}
