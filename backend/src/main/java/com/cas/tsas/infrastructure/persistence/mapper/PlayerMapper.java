package com.cas.tsas.infrastructure.persistence.mapper;

import com.cas.tsas.domain.model.Player;
import com.cas.tsas.infrastructure.persistence.entity.PlayerJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PlayerMapper {

    public Player toDomain(PlayerJpaEntity entity) {
        return new Player(
                entity.getId(),
                entity.getName(),
                entity.getGender(),
                entity.getRanking(),
                entity.getHandedness(),
                entity.getBackhandType()
        );
    }

    public PlayerJpaEntity toEntity(Player player) {
        PlayerJpaEntity entity = new PlayerJpaEntity();
        entity.setId(player.getId());
        entity.setName(player.getName());
        entity.setGender(player.getGender());
        entity.setRanking(player.getRanking());
        entity.setHandedness(player.getHandedness());
        entity.setBackhandType(player.getBackhandType());
        return entity;
    }
}
