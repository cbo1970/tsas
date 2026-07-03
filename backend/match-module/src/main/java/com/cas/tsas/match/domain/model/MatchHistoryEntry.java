package com.cas.tsas.match.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Ein abgeschlossenes Match aus der Perspektive eines Spielers, fuer die Match-History-Liste (TEN-35). */
public record MatchHistoryEntry(
        UUID matchId, UUID opponentId, String opponentName,
        int setsWon, int setsLost, boolean won, Instant completedAt) {}
