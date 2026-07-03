package com.cas.tsas.player.application.port.out;

import java.util.UUID;

/**
 * Output port (implemented by the match module) reporting whether any match
 * references the given player; used to decide whether a player may be deleted.
 */
public interface HasMatchesPort {
    boolean existsByPlayerId(UUID playerId);
}
