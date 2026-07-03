package com.cas.tsas.match.application.port.out;

import com.cas.tsas.match.domain.model.MatchPlayerNote;

import java.util.UUID;

/** Upserts the single note for {@code (matchId, playerId)} (insert or update of the note text). */
public interface SavePlayerNotePort {
    MatchPlayerNote upsert(UUID matchId, UUID playerId, String note);
}
