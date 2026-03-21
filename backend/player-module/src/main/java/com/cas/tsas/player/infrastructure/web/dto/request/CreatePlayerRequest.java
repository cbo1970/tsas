package com.cas.tsas.player.infrastructure.web.dto.request;

import com.cas.tsas.player.domain.model.BackhandType;
import com.cas.tsas.player.domain.model.Gender;
import com.cas.tsas.player.domain.model.Handedness;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreatePlayerRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull Gender gender,
        @NotNull Handedness handedness,
        @NotNull BackhandType backhandType,
        String ranking,
        String nationality,
        LocalDate birthDate
) {}
