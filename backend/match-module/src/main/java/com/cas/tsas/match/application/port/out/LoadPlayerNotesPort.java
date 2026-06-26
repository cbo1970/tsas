package com.cas.tsas.match.application.port.out;

import com.cas.tsas.match.domain.model.MatchPlayerNote;

import java.util.List;
import java.util.UUID;

public interface LoadPlayerNotesPort {

    /** The 0–2 notes of the given match. */
    List<MatchPlayerNote> findByMatch(UUID matchId);

    /** Notes about one player across matches, newest first, capped at {@code limit}. */
    List<MatchPlayerNote> findAboutPlayer(UUID playerId, int limit);
}
