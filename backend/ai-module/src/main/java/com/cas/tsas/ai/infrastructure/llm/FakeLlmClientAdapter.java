package com.cas.tsas.ai.infrastructure.llm;

import com.cas.tsas.ai.application.dto.MatchAnalysisResult;
import com.cas.tsas.ai.application.dto.MatchMetadata;
import com.cas.tsas.ai.application.port.out.LlmClientPort;
import com.cas.tsas.ai.domain.model.Recommendation;
import com.cas.tsas.statistics.domain.model.MatchStatistics;

import java.util.List;

public class FakeLlmClientAdapter implements LlmClientPort {

    public static final String MODEL_NAME = "fake-llm";

    @Override
    public MatchAnalysisResult generateAnalysis(MatchStatistics stats, MatchMetadata meta) {
        return new MatchAnalysisResult(
                "Schlüsselmomente (Fake): " + stats.totalPoints() + " Punkte gespielt",
                "Stärken eigen (Fake)",
                "Schwächen eigen (Fake)",
                "Stärken Gegner (Fake)",
                "Schwächen Gegner (Fake)",
                List.of(
                        new Recommendation(1, "Mehr Aufschlag-Variation", "1./2. Aufschlag mischen."),
                        new Recommendation(2, "Rückhand Cross spielen", "Gegner ist auf der RH schwächer.")
                )
        );
    }

    @Override
    public String modelName() {
        return MODEL_NAME;
    }
}
