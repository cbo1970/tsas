package com.cas.tsas.ai.application.port.in;

import com.cas.tsas.ai.domain.model.MatchAnalysis;

import java.util.UUID;

public interface GenerateMatchAnalysisUseCase {
    MatchAnalysis generate(UUID matchId);
}
