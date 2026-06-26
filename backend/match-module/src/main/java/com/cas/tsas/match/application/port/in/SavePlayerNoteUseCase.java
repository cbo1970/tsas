package com.cas.tsas.match.application.port.in;

import com.cas.tsas.match.domain.model.MatchPlayerNote;

import java.util.Optional;
import java.util.UUID;

/**
 * Upserts the coach note for {@code (matchId, playerId)} (TEN-68). A blank note deletes it.
 * Owner-scoped via the match: unknown/foreign matches yield {@code MatchNotFoundException} (→404),
 * a {@code playerId} not belonging to the match yields {@code IllegalArgumentException} (→400).
 *
 * @return the saved note, or empty when the note was deleted (blank input).
 */
public interface SavePlayerNoteUseCase {
    Optional<MatchPlayerNote> save(UUID matchId, UUID playerId, String note);
}
