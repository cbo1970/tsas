package com.cas.tsas.player.application.port.out;

import java.util.UUID;

public interface HasMatchesPort {
    boolean existsByPlayerId(UUID playerId);
}
