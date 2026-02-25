package com.cas.tsas.domain.exception;

public class MatchNotFoundException extends RuntimeException {

    public MatchNotFoundException(Long id) {
        super("Match not found with id: " + id);
    }
}
