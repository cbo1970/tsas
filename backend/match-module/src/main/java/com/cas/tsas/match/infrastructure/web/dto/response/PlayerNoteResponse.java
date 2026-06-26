package com.cas.tsas.match.infrastructure.web.dto.response;

import com.cas.tsas.match.domain.model.MatchPlayerNote;

import java.time.Instant;
import java.util.UUID;

public record PlayerNoteResponse(UUID playerId, String note, Instant updatedAt) {
    public static PlayerNoteResponse from(MatchPlayerNote n) {
        return new PlayerNoteResponse(n.playerId(), n.note(), n.updatedAt());
    }
}
