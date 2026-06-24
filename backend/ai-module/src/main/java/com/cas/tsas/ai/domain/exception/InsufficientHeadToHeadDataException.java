package com.cas.tsas.ai.domain.exception;

/**
 * Geworfen, wenn die KI-Vorbereitung gegen einen Gegner angefragt wird, obwohl noch kein
 * gemeinsames, abgeschlossenes Match in der Head-to-Head-Historie liegt (TEN-51).
 * Wird vom AiExceptionHandler zu HTTP 422 gemappt — analog zu InsufficientMatchDataException
 * bei der Match-Analyse (FA-11).
 */
public class InsufficientHeadToHeadDataException extends RuntimeException {

    public InsufficientHeadToHeadDataException(String message) {
        super(message);
    }
}
