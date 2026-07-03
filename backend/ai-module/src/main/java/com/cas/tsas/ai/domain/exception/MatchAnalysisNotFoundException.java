package com.cas.tsas.ai.domain.exception;

import java.util.UUID;

/** Geworfen, wenn für ein Match keine Analyse existiert. Bildet auf HTTP 404 ab. */
public class MatchAnalysisNotFoundException extends RuntimeException {
    public MatchAnalysisNotFoundException(UUID matchId) {
        super("No analysis for match " + matchId);
    }
}
