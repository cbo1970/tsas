package com.cas.tsas.ai.application.port.out;

import com.cas.tsas.ai.domain.model.MatchAnalysis;

import java.util.Optional;
import java.util.UUID;

public interface LoadMatchAnalysisPort {
    Optional<MatchAnalysis> loadByMatchId(UUID matchId);
}
