package com.cas.tsas.match.domain.exception;

import com.cas.tsas.common.exception.ConflictException;

import java.util.UUID;

/**
 * Geworfen, wenn für einen Spieler ein neues Match erstellt werden soll, obwohl er bereits
 * ein laufendes Match hat. Ein Spieler darf zu einem Zeitpunkt nur ein aktives Match haben.
 * Bildet auf HTTP 409 ab.
 */
public class ActiveMatchExistsException extends ConflictException {

    public ActiveMatchExistsException(UUID playerId) {
        super("Player already has an active match: " + playerId);
    }
}
