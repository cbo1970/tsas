package com.cas.tsas.match.application.service;

import com.cas.tsas.auth.application.port.in.CurrentUserProvider;
import com.cas.tsas.match.application.port.in.GetMatchHistoryUseCase;
import com.cas.tsas.match.application.port.out.LoadMatchHistoryPort;
import com.cas.tsas.match.application.port.out.MatchHistoryRow;
import com.cas.tsas.match.domain.model.MatchHistoryEntry;
import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.player.domain.exception.PlayerNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Erstellt die abgeschlossene Match-History eines Spielers (TEN-35), owner-scoped. */
@Service
@Transactional(readOnly = true)
public class MatchHistoryService implements GetMatchHistoryUseCase {

    private final LoadMatchHistoryPort loadMatchHistoryPort;
    private final LoadPlayerPort loadPlayerPort;
    private final CurrentUserProvider currentUserProvider;

    public MatchHistoryService(LoadMatchHistoryPort loadMatchHistoryPort,
                               LoadPlayerPort loadPlayerPort,
                               CurrentUserProvider currentUserProvider) {
        this.loadMatchHistoryPort = loadMatchHistoryPort;
        this.loadPlayerPort = loadPlayerPort;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public List<MatchHistoryEntry> forPlayer(UUID playerId) {
        UUID ownerId = currentUserProvider.get().id();
        // IDOR-Schutz: fremder/unbekannter Spieler → 404 (gleich wie Gegner-Vorbereitung).
        loadPlayerPort.findByIdAndOwner(playerId, ownerId)
                .orElseThrow(() -> new PlayerNotFoundException(playerId));

        return loadMatchHistoryPort.findCompletedByPlayer(playerId, ownerId).stream()
                .map(row -> toEntry(playerId, row))
                .toList();
    }

    private MatchHistoryEntry toEntry(UUID playerId, MatchHistoryRow row) {
        boolean isP1 = playerId.equals(row.player1Id());
        UUID opponentId = isP1 ? row.player2Id() : row.player1Id();
        int setsWon = isP1 ? row.setsPlayer1() : row.setsPlayer2();
        int setsLost = isP1 ? row.setsPlayer2() : row.setsPlayer1();
        boolean won = (isP1 && "PLAYER1".equals(row.winner()))
                || (!isP1 && "PLAYER2".equals(row.winner()));
        String opponentName = loadPlayerPort.loadPlayer(opponentId)
                .map(p -> p.getFirstName() + " " + p.getLastName())
                .orElse("—");
        return new MatchHistoryEntry(row.matchId(), opponentId, opponentName,
                setsWon, setsLost, won, row.completedAt());
    }
}
