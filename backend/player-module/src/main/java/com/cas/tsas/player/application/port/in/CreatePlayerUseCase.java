package com.cas.tsas.player.application.port.in;

import com.cas.tsas.player.domain.model.BackhandType;
import com.cas.tsas.player.domain.model.Gender;
import com.cas.tsas.player.domain.model.Handedness;
import com.cas.tsas.player.domain.model.Player;

import java.time.LocalDate;

public interface CreatePlayerUseCase {

    Player createPlayer(CreatePlayerCommand command);

    record CreatePlayerCommand(
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
