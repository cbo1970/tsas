package com.cas.tsas.ai.infrastructure.web.dto;

import com.cas.tsas.ai.domain.model.AnalysisStatus;
import com.cas.tsas.ai.domain.model.MatchAnalysis;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MatchAnalysisResponse(
        UUID matchId,
        AnalysisStatus status,
        String keyMoments,
        String ownStrengths,
        String ownWeaknesses,
        String opponentStrengths,
        String opponentWeaknesses,
        List<RecommendationResponse> recommendations,
        String modelUsed,
        Instant generatedAt,
        String errorMessage
) {

    public static MatchAnalysisResponse from(MatchAnalysis a) {
        List<RecommendationResponse> recs = a.getRecommendations() == null
                ? List.of()
                : a.getRecommendations().stream()
                        .map(RecommendationResponse::from)
                        .toList();
        return new MatchAnalysisResponse(
                a.getMatchId(),
                a.getStatus(),
                a.getKeyMoments(),
                a.getOwnStrengths(),
                a.getOwnWeaknesses(),
                a.getOpponentStrengths(),
                a.getOpponentWeaknesses(),
                recs,
                a.getModelUsed(),
                a.getGeneratedAt(),
                a.getErrorMessage());
    }
}
