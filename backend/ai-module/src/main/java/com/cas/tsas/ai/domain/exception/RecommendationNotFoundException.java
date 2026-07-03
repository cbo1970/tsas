package com.cas.tsas.ai.domain.exception;

import java.util.UUID;

/** Geworfen, wenn der Empfehlungs-Index ausserhalb der Liste liegt. Bildet auf HTTP 404 ab. */
public class RecommendationNotFoundException extends RuntimeException {
    public RecommendationNotFoundException(UUID matchId, int index) {
        super("No recommendation at index " + index + " for match " + matchId);
    }
}
