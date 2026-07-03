package com.cas.tsas.match.application.port.out;

import java.util.List;
import java.util.UUID;

public interface LoadMatchHistoryPort {
    /** Abgeschlossene Matches des Spielers, gefiltert nach ownerId, neueste zuerst. */
    List<MatchHistoryRow> findCompletedByPlayer(UUID playerId, UUID ownerId);
}
