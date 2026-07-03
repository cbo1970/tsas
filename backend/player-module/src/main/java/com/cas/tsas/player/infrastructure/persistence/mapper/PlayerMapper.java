package com.cas.tsas.player.infrastructure.persistence.mapper;

import com.cas.tsas.player.domain.model.Player;
import com.cas.tsas.player.infrastructure.persistence.entity.PlayerJpaEntity;
import org.springframework.stereotype.Component;

/** Maps between {@link Player} domain objects and {@link PlayerJpaEntity}. */
@Component
public class PlayerMapper {

    public Player toDomain(PlayerJpaEntity entity) {
        Player player = new Player(
                entity.getId(),
                entity.getOwnerId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getGender(),
                entity.getHandedness(),
                entity.getBackhandType(),
                entity.getRanking(),
                entity.getNationality(),
                entity.getBirthDate()
        );
        player.setActive(entity.isActive());
        return player;
    }

    public PlayerJpaEntity toEntity(Player player) {
        PlayerJpaEntity entity = new PlayerJpaEntity();
        entity.setId(player.getId());
        entity.setOwnerId(player.getOwnerId());
        entity.setFirstName(player.getFirstName());
        entity.setLastName(player.getLastName());
        entity.setGender(player.getGender());
        entity.setHandedness(player.getHandedness());
        entity.setBackhandType(player.getBackhandType());
        entity.setRanking(player.getRanking());
        entity.setNationality(player.getNationality());
        entity.setBirthDate(player.getBirthDate());
        entity.setActive(player.isActive());
        return entity;
    }
}
