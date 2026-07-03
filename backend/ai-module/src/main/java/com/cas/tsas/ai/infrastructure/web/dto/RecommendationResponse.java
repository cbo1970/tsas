package com.cas.tsas.ai.infrastructure.web.dto;

import com.cas.tsas.ai.domain.model.Recommendation;
import com.cas.tsas.ai.domain.model.RecommendationStatus;

import java.time.Instant;

public record RecommendationResponse(
        int priority,
        String title,
        String detail,
        RecommendationStatus status,
        String reviewNote,
        Instant reviewedAt
) {
    public static RecommendationResponse from(Recommendation r) {
        return new RecommendationResponse(
                r.priority(), r.title(), r.detail(), r.status(), r.reviewNote(), r.reviewedAt());
    }
}
