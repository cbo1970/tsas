package com.cas.tsas.infrastructure.web.dto.response;

import com.cas.tsas.domain.model.Player;

public record PlayerResponse(
        Long id,
        String name,
        String gender,
        Integer ranking,
        String handedness,
        String backhandType
) {
    public static PlayerResponse from(Player player) {
        return new PlayerResponse(
                player.getId(),
                player.getName(),
                player.getGender(),
                player.getRanking(),
                player.getHandedness(),
                player.getBackhandType()
        );
    }
}
