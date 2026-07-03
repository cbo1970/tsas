package com.cas.tsas.match.application.port.out;

import java.util.UUID;

/** Removes the note for {@code (matchId, playerId)}; no-op when none exists. */
public interface DeletePlayerNotePort {
    void delete(UUID matchId, UUID playerId);
}
