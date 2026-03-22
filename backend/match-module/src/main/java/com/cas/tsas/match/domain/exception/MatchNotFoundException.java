package com.cas.tsas.match.domain.exception;

import java.util.UUID;

public class MatchNotFoundException extends RuntimeException {

    public MatchNotFoundException(UUID id) {
        super("Match not found with id: " + id);
    }
}
