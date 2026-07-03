package com.cas.tsas.match.infrastructure.web.dto;

import com.cas.tsas.match.domain.model.MatchHistoryEntry;

import java.time.Instant;
import java.util.UUID;

public record MatchHistoryEntryDto(
        UUID matchId, String opponentName, int setsWon, int setsLost, boolean won, Instant completedAt) {
    public static MatchHistoryEntryDto from(MatchHistoryEntry e) {
        return new MatchHistoryEntryDto(e.matchId(), e.opponentName(),
                e.setsWon(), e.setsLost(), e.won(), e.completedAt());
    }
}
