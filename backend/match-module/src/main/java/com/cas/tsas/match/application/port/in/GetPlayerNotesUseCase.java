package com.cas.tsas.match.application.port.in;

import com.cas.tsas.match.domain.model.MatchPlayerNote;

import java.util.List;
import java.util.UUID;

public interface GetPlayerNotesUseCase {

    /** Owner-scoped: the 0–2 notes of the given match (→404 for unknown/foreign match). */
    List<MatchPlayerNote> forMatch(UUID matchId);

    /**
     * Notes about one player across matches, newest first, capped at {@code limit}.
     * NOT owner-scoped — callers must have verified the player belongs to the current user
     * (opponent preparation does so via {@code findByIdAndOwner}). A player is owner-bound, so
     * its notes are inherently the owner's.
     */
    List<MatchPlayerNote> aboutPlayer(UUID playerId, int limit);
}
