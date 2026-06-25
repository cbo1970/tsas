package com.cas.tsas.ai.domain.exception;

import com.cas.tsas.common.exception.ConflictException;

import java.util.UUID;

/** Geworfen, wenn eine nicht-COMPLETED-Analyse reviewt werden soll. Bildet auf HTTP 409 ab. */
public class AnalysisNotReviewableException extends ConflictException {
    public AnalysisNotReviewableException(UUID matchId) {
        super("Analysis for match " + matchId + " is not COMPLETED and cannot be reviewed");
    }
}
