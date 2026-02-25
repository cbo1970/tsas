package com.cas.tsas.infrastructure.persistence.mapper;

import com.cas.tsas.domain.model.Match;
import com.cas.tsas.infrastructure.persistence.entity.MatchJpaEntity;
import com.cas.tsas.infrastructure.persistence.entity.PlayerJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class MatchMapper {

    private final PlayerMapper playerMapper;

    public MatchMapper(PlayerMapper playerMapper) {
        this.playerMapper = playerMapper;
    }

    public Match toDomain(MatchJpaEntity entity) {
        return new Match(
                entity.getId(),
                playerMapper.toDomain(entity.getOwnPlayer()),
                playerMapper.toDomain(entity.getOpponent()),
                entity.getDate(),
                entity.getSetsToWin(),
                entity.isMatchTieBreak(),
                entity.isShortSet()
        );
    }

    public MatchJpaEntity toEntity(Match match) {
        MatchJpaEntity entity = new MatchJpaEntity();
        entity.setId(match.getId());

        PlayerJpaEntity ownPlayerEntity = new PlayerJpaEntity();
        ownPlayerEntity.setId(match.getOwnPlayer().getId());
        entity.setOwnPlayer(ownPlayerEntity);

        PlayerJpaEntity opponentEntity = new PlayerJpaEntity();
        opponentEntity.setId(match.getOpponent().getId());
        entity.setOpponent(opponentEntity);

        entity.setDate(match.getDate());
        entity.setSetsToWin(match.getSetsToWin());
        entity.setMatchTieBreak(match.isMatchTieBreak());
        entity.setShortSet(match.isShortSet());
        return entity;
    }
}
