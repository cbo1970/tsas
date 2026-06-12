package com.cas.tsas.match.domain.exception;

import com.cas.tsas.common.exception.ConflictException;

import java.util.UUID;

/**
 * Geworfen, wenn eine zustandsändernde Operation (Punkt erfassen, Aufschläger setzen,
 * Walkover) auf einem bereits abgeschlossenen Match versucht wird. Bildet auf HTTP 409 ab.
 */
public class MatchAlreadyCompletedException extends ConflictException {

    public MatchAlreadyCompletedException(UUID matchId) {
        super("Match is already completed: " + matchId);
    }
}
