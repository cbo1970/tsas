package com.cas.tsas.ai.infrastructure.web.dto;

import com.cas.tsas.ai.domain.model.OpponentPreparation;
import com.cas.tsas.ai.domain.model.Recommendation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** REST-Antwort für TEN-51 — KI-Vorbereitung gegen einen Gegner. */
public record OpponentPreparationResponse(
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

    public static OpponentPreparationResponse from(OpponentPreparation p) {
        return new OpponentPreparationResponse(
                p.ownPlayerId(), p.opponentId(), p.matchesPlayed(),
                p.opponentProfile(), p.tacticalObservations(),
                p.serveStrategy(), p.returnStrategy(),
                p.recommendations(), p.modelUsed(), p.generatedAt());
    }
}
