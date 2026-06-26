package com.cas.tsas.match.infrastructure.persistence.mapper;

import com.cas.tsas.match.domain.model.MatchPlayerNote;
import com.cas.tsas.match.infrastructure.persistence.entity.MatchPlayerNoteJpaEntity;
import org.springframework.stereotype.Component;

/** Maps between {@link MatchPlayerNote} and {@link MatchPlayerNoteJpaEntity}. */
@Component
public class MatchPlayerNoteMapper {

    public MatchPlayerNote toDomain(MatchPlayerNoteJpaEntity e) {
        return new MatchPlayerNote(e.getId(), e.getMatchId(), e.getPlayerId(), e.getNote(), e.getUpdatedAt());
    }
}
