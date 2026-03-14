package com.cas.tsas.infrastructure.persistence.mapper;

import com.cas.tsas.domain.model.Match;
import com.cas.tsas.infrastructure.persistence.entity.MatchJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class MatchMapper {

    public Match toDomain(MatchJpaEntity entity) {
        return new Match(
                entity.getId(),
                entity.getPlayer1Id(),
                entity.getPlayer2Id(),
                entity.getSetsToWin(),
                entity.isMatchTiebreak(),
                entity.isShortSet(),
                entity.getStatus()
        );
    }

    public MatchJpaEntity toEntity(Match match) {
        MatchJpaEntity entity = new MatchJpaEntity();
        entity.setId(match.getId());
        entity.setPlayer1Id(match.getPlayer1Id());
        entity.setPlayer2Id(match.getPlayer2Id());
        entity.setSetsToWin(match.getSetsToWin());
        entity.setMatchTiebreak(match.isMatchTiebreak());
        entity.setShortSet(match.isShortSet());
        entity.setStatus(match.getStatus());
        return entity;
    }
}
