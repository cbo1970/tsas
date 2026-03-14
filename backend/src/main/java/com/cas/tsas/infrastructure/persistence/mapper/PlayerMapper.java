package com.cas.tsas.infrastructure.persistence.mapper;

import com.cas.tsas.domain.model.Player;
import com.cas.tsas.infrastructure.persistence.entity.PlayerJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PlayerMapper {

    public Player toDomain(PlayerJpaEntity entity) {
        return new Player(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getGender(),
                entity.getHandedness(),
                entity.getBackhandType(),
                entity.getRanking(),
                entity.getNationality(),
                entity.getBirthDate()
        );
    }

    public PlayerJpaEntity toEntity(Player player) {
        PlayerJpaEntity entity = new PlayerJpaEntity();
        entity.setId(player.getId());
        entity.setFirstName(player.getFirstName());
        entity.setLastName(player.getLastName());
        entity.setGender(player.getGender());
        entity.setHandedness(player.getHandedness());
        entity.setBackhandType(player.getBackhandType());
        entity.setRanking(player.getRanking());
        entity.setNationality(player.getNationality());
        entity.setBirthDate(player.getBirthDate());
        return entity;
    }
}
