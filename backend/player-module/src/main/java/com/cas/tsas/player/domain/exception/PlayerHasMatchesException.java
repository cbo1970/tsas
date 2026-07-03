package com.cas.tsas.player.domain.exception;

import com.cas.tsas.common.exception.ConflictException;

import java.util.UUID;

/**
 * Geworfen, wenn ein Spieler gelöscht werden soll, der noch an Matches beteiligt ist.
 * Solche Spieler werden stattdessen deaktiviert. Bildet auf HTTP 409 ab.
 */
public class PlayerHasMatchesException extends ConflictException {

    public PlayerHasMatchesException(UUID playerId) {
        super("Player has matches and cannot be deleted: " + playerId);
    }
}
