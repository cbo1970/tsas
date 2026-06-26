package com.cas.tsas.match.application.port.in;

import com.cas.tsas.match.domain.model.MatchHistoryEntry;

import java.util.List;
import java.util.UUID;

public interface GetMatchHistoryUseCase {
    /** Abgeschlossene Matches des Spielers (neueste zuerst), owner-scoped (404 fuer fremde/unbekannte Spieler). */
    List<MatchHistoryEntry> forPlayer(UUID playerId);
}
