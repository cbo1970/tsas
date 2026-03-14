package com.cas.tsas.application.port.in.player;

import com.cas.tsas.domain.model.BackhandType;
import com.cas.tsas.domain.model.Gender;
import com.cas.tsas.domain.model.Handedness;
import com.cas.tsas.domain.model.Player;

import java.time.LocalDate;

public interface CreatePlayerUseCase {

    Player createPlayer(CreatePlayerCommand command);

    record CreatePlayerCommand(
            String firstName,
            String lastName,
            Gender gender,
            Handedness handedness,
            BackhandType backhandType,
            Integer ranking,
            String nationality,
            LocalDate birthDate
    ) {}
}
