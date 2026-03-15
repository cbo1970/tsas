package com.cas.tsas.infrastructure.web.dto.response;

import com.cas.tsas.domain.model.BackhandType;
import com.cas.tsas.domain.model.Gender;
import com.cas.tsas.domain.model.Handedness;
import com.cas.tsas.domain.model.Player;

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
        LocalDate birthDate
) {
    public static PlayerResponse from(Player player) {
        return new PlayerResponse(
                player.getId(),
                player.getFirstName(),
                player.getLastName(),
                player.getGender(),
                player.getHandedness(),
                player.getBackhandType(),
                player.getRanking(),
                player.getNationality(),
                player.getBirthDate()
        );
    }
}
