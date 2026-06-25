package com.cas.tsas.ai.infrastructure.web.dto;

import com.cas.tsas.ai.domain.model.RecommendationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request-Body für das Review einer Empfehlung (TEN-39). */
public record ReviewRecommendationRequest(
        @NotNull RecommendationStatus status,
        @Size(max = 500) String note
) {}
