package com.cas.tsas.ai.domain.model;

import java.time.Instant;

/**
 * Eine einzelne taktische KI-Empfehlung samt Human-in-the-Loop-Review-Zustand (TEN-39).
 * Generierte Empfehlungen starten in {@link RecommendationStatus#OPEN}; der Coach kann sie
 * annehmen oder verwerfen (mit optionaler Begründung).
 */
public record Recommendation(
        int priority,
        String title,
        String detail,
        RecommendationStatus status,
        String reviewNote,
        Instant reviewedAt
) {

    /** Defaultet einen fehlenden Status (z. B. aus Alt-JSON deserialisiert) auf OPEN. */
    public Recommendation {
        status = status == null ? RecommendationStatus.OPEN : status;
    }

    /** Generierungs-Konstruktor: neue Empfehlung ohne Review. */
    public Recommendation(int priority, String title, String detail) {
        this(priority, title, detail, RecommendationStatus.OPEN, null, null);
    }

    /** Liefert eine Kopie mit aktualisiertem Review-Zustand (immutable). */
    public Recommendation withReview(RecommendationStatus newStatus, String note, Instant at) {
        return new Recommendation(priority, title, detail, newStatus, note, at);
    }
}
