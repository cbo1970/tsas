package com.cas.tsas.ai.application.dto;

import com.cas.tsas.ai.domain.model.Recommendation;

import java.util.List;

public record MatchAnalysisResult(
        String keyMoments,
        String ownStrengths,
        String ownWeaknesses,
        String opponentStrengths,
        String opponentWeaknesses,
        List<Recommendation> recommendations
) {}
