package com.cas.tsas.match.infrastructure.persistence.repository;

import com.cas.tsas.match.application.port.out.DeletePlayerNotePort;
import com.cas.tsas.match.application.port.out.LoadPlayerNotesPort;
import com.cas.tsas.match.application.port.out.SavePlayerNotePort;
import com.cas.tsas.match.domain.model.MatchPlayerNote;
import com.cas.tsas.match.infrastructure.persistence.entity.MatchPlayerNoteJpaEntity;
import com.cas.tsas.match.infrastructure.persistence.mapper.MatchPlayerNoteMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Persistence adapter implementing the player-note ports (TEN-68). */
@Component
public class MatchPlayerNotePersistenceAdapter
        implements SavePlayerNotePort, DeletePlayerNotePort, LoadPlayerNotesPort {

    private final MatchPlayerNoteJpaRepository repository;
    private final MatchPlayerNoteMapper mapper;

    public MatchPlayerNotePersistenceAdapter(MatchPlayerNoteJpaRepository repository,
                                             MatchPlayerNoteMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public MatchPlayerNote upsert(UUID matchId, UUID playerId, String note) {
        MatchPlayerNoteJpaEntity entity = repository.findByMatchIdAndPlayerId(matchId, playerId)
                .orElseGet(() -> {
                    MatchPlayerNoteJpaEntity e = new MatchPlayerNoteJpaEntity();
                    e.setMatchId(matchId);
                    e.setPlayerId(playerId);
                    return e;
                });
        entity.setNote(note);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID matchId, UUID playerId) {
        repository.deleteByMatchIdAndPlayerId(matchId, playerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchPlayerNote> findByMatch(UUID matchId) {
        return repository.findByMatchId(matchId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchPlayerNote> findAboutPlayer(UUID playerId, int limit) {
        return repository.findAboutPlayerInCompletedMatches(playerId, PageRequest.of(0, limit))
                .stream().map(mapper::toDomain).toList();
    }
}
