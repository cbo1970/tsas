package com.cas.tsas.ai.application.port.out;

import com.cas.tsas.ai.application.dto.MatchAnalysisResult;
import com.cas.tsas.ai.application.dto.MatchMetadata;
import com.cas.tsas.statistics.domain.model.MatchStatistics;

public interface LlmClientPort {

    MatchAnalysisResult generateAnalysis(MatchStatistics stats, MatchMetadata meta);

    String modelName();
}
