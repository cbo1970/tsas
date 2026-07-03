package com.cas.tsas.match.application.port.out;

import java.time.Instant;
import java.util.UUID;

/** Rohe History-Zeile aus der Persistenz (Match + Score), noch nicht angereichert. */
public record MatchHistoryRow(
        UUID matchId, UUID player1Id, UUID player2Id,
        int setsPlayer1, int setsPlayer2, String winner, Instant completedAt) {}
