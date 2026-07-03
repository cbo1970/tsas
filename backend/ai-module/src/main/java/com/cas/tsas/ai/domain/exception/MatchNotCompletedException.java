package com.cas.tsas.ai.domain.exception;

import com.cas.tsas.common.exception.ConflictException;

import java.util.UUID;

/**
 * Geworfen, wenn für ein Match eine Analyse angefordert wird, das noch nicht abgeschlossen
 * ({@code COMPLETED}) ist. Eine taktische Auswertung setzt ein beendetes Match voraus.
 * Bildet auf HTTP 409 ab.
 */
public class MatchNotCompletedException extends ConflictException {

    public MatchNotCompletedException(UUID matchId) {
        super("Match " + matchId + " is not COMPLETED");
    }
}
