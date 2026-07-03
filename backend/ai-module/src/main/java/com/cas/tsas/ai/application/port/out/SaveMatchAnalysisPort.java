package com.cas.tsas.ai.application.port.out;

import com.cas.tsas.ai.domain.model.MatchAnalysis;

public interface SaveMatchAnalysisPort {
    MatchAnalysis save(MatchAnalysis analysis);
}
