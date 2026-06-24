package com.cas.tsas.ai.application.dto;

import com.cas.tsas.ai.domain.model.Recommendation;

import java.util.List;

/**
 * Struktur, die der LLM-Adapter für TEN-51 zurückgibt. Wird im Service auf eine
 * {@link com.cas.tsas.ai.domain.model.OpponentPreparation} gemappt.
 */
public record OpponentPreparationResult(
        String opponentProfile,
        String tacticalObservations,
        String serveStrategy,
        String returnStrategy,
        List<Recommendation> recommendations
) {}
