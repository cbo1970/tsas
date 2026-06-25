package com.cas.tsas.ai.application.port.in;

import com.cas.tsas.ai.domain.model.MatchAnalysis;
import com.cas.tsas.ai.domain.model.RecommendationStatus;

import java.util.UUID;

/** Human-in-the-Loop: Coach reviewt eine einzelne KI-Empfehlung (TEN-39). */
public interface ReviewRecommendationUseCase {
    MatchAnalysis review(UUID matchId, int recommendationIndex, RecommendationStatus status, String note);
}
