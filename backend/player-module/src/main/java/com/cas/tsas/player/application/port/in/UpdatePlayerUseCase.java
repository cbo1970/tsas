package com.cas.tsas.player.application.port.in;

import com.cas.tsas.player.domain.model.BackhandType;
import com.cas.tsas.player.domain.model.Gender;
import com.cas.tsas.player.domain.model.Handedness;
import com.cas.tsas.player.domain.model.Player;

import java.time.LocalDate;
import java.util.UUID;

public interface UpdatePlayerUseCase {

    Player updatePlayer(UpdatePlayerCommand command);

    record UpdatePlayerCommand(
            UUID id,
            String firstName,
            String lastName,
            Gender gender,
            Handedness handedness,
            BackhandType backhandType,
            String ranking,
            String nationality,
            LocalDate birthDate
    ) {}
}
