package com.cas.tsas.ai.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Strukturierte taktische Vorbereitung gegen einen bestimmten Gegner (TEN-51 / Roadmap V2).
 * Wird je Aufruf des KI-Vorbereitungs-Endpoints neu generiert — keine Persistenz, da sich der
 * Head-to-Head-Datenstand laufend ändert und veraltete Empfehlungen schädlicher sind als der
 * zusätzliche LLM-Aufruf. Cost-Kontrolle erfolgt über den bestehenden Rate-Limiter (TEN-64).
 *
 * <p>Im Gegensatz zur {@link MatchAnalysis} (Postmortem zu einem konkreten beendeten Match)
 * blickt eine OpponentPreparation nach vorne und aggregiert über die gesamte Head-to-Head-
 * Historie zweier Spieler.
 */
public record OpponentPreparation(
        UUID ownPlayerId,
        UUID opponentId,
        int matchesPlayed,
        String opponentProfile,
        String tacticalObservations,
        String serveStrategy,
        String returnStrategy,
        List<Recommendation> recommendations,
        String modelUsed,
        Instant generatedAt
) {

    public OpponentPreparation {
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }
}
