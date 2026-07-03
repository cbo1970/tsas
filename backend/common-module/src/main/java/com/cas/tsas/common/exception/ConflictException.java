package com.cas.tsas.common.exception;

/**
 * Basis für fachliche Konflikte, die auf HTTP 409 abgebildet werden — z. B. eine Operation,
 * die mit dem aktuellen Zustand einer Ressource unvereinbar ist (Match bereits beendet,
 * Spieler hat bereits ein laufendes Match). Module werfen spezifische Subklassen; der
 * {@code CommonExceptionHandler} im common-module bildet alle zentral auf 409 ab.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
