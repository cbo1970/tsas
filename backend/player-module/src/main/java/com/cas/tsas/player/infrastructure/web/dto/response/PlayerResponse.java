package com.cas.tsas.player.infrastructure.web.dto.response;

import com.cas.tsas.player.domain.model.BackhandType;
import com.cas.tsas.player.domain.model.Gender;
import com.cas.tsas.player.domain.model.Handedness;
import com.cas.tsas.player.domain.model.Player;

import java.time.LocalDate;
import java.util.UUID;

public record PlayerResponse(
        UUID id,
        String firstName,
        String lastName,
        Gender gender,
        Handedness handedness,
        BackhandType backhandType,
        String ranking,
        String nationality,
        LocalDate birthDate,
        boolean active,
        boolean deletable
) {
    public static PlayerResponse from(Player player, boolean deletable) {
        return new PlayerResponse(
                player.getId(),
                player.getFirstName(),
                player.getLastName(),
                player.getGender(),
                player.getHandedness(),
                player.getBackhandType(),
                player.getRanking(),
                player.getNationality(),
                player.getBirthDate(),
                player.isActive(),
                deletable
        );
    }
}
