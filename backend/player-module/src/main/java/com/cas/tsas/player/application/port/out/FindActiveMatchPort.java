package com.cas.tsas.player.application.port.out;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface FindActiveMatchPort {
    Map<UUID, UUID> findActiveMatchIdsByPlayerIds(Set<UUID> playerIds);
}
