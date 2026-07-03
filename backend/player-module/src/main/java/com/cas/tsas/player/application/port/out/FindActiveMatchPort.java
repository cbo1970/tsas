package com.cas.tsas.player.application.port.out;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Output port (implemented by the match module) returning, for each of the
 * given players that currently has an in-progress match, a mapping from the
 * player id to that match id. Players without an active match are omitted.
 */
public interface FindActiveMatchPort {
    Map<UUID, UUID> findActiveMatchIdsByPlayerIds(Set<UUID> playerIds);
}
