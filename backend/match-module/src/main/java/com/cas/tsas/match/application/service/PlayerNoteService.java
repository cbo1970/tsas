package com.cas.tsas.match.application.service;

import com.cas.tsas.match.application.port.in.GetMatchUseCase;
import com.cas.tsas.match.application.port.in.GetPlayerNotesUseCase;
import com.cas.tsas.match.application.port.in.SavePlayerNoteUseCase;
import com.cas.tsas.match.application.port.out.DeletePlayerNotePort;
import com.cas.tsas.match.application.port.out.LoadPlayerNotesPort;
import com.cas.tsas.match.application.port.out.SavePlayerNotePort;
import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchPlayerNote;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Coach player-note use cases (TEN-68). Authorises every match-bound operation through
 * {@link GetMatchUseCase#findById(UUID)} (owner-scoped). {@link #aboutPlayer} is intentionally not
 * owner-scoped — see {@link GetPlayerNotesUseCase}.
 */
@Service
@Transactional
public class PlayerNoteService implements SavePlayerNoteUseCase, GetPlayerNotesUseCase {

    private final GetMatchUseCase getMatchUseCase;
    private final SavePlayerNotePort savePort;
    private final DeletePlayerNotePort deletePort;
    private final LoadPlayerNotesPort loadPort;

    public PlayerNoteService(GetMatchUseCase getMatchUseCase,
                             SavePlayerNotePort savePort,
                             DeletePlayerNotePort deletePort,
                             LoadPlayerNotesPort loadPort) {
        this.getMatchUseCase = getMatchUseCase;
        this.savePort = savePort;
        this.deletePort = deletePort;
        this.loadPort = loadPort;
    }

    @Override
    public Optional<MatchPlayerNote> save(UUID matchId, UUID playerId, String note) {
        Match match = getMatchUseCase.findById(matchId); // owner check → 404
        if (!playerId.equals(match.getPlayer1Id()) && !playerId.equals(match.getPlayer2Id())) {
            throw new IllegalArgumentException(
                    "Player " + playerId + " is not part of match " + matchId);
        }
        if (note == null || note.isBlank()) {
            deletePort.delete(matchId, playerId);
            return Optional.empty();
        }
        return Optional.of(savePort.upsert(matchId, playerId, note));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchPlayerNote> forMatch(UUID matchId) {
        getMatchUseCase.findById(matchId); // owner check → 404
        return loadPort.findByMatch(matchId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchPlayerNote> aboutPlayer(UUID playerId, int limit) {
        return loadPort.findAboutPlayer(playerId, limit);
    }
}
