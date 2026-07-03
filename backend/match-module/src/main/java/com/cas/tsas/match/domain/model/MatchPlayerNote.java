package com.cas.tsas.match.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Coach free-text note (TEN-68) about one player in the context of one match. Decoupled from the
 * score; exactly one note exists per {@code (matchId, playerId)}. Pure POJO — no framework deps.
 */
public record MatchPlayerNote(UUID id, UUID matchId, UUID playerId, String note, Instant updatedAt) {
}
