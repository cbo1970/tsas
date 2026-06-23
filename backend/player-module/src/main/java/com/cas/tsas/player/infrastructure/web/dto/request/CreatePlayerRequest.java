package com.cas.tsas.player.infrastructure.web.dto.request;

import com.cas.tsas.player.domain.model.BackhandType;
import com.cas.tsas.player.domain.model.Gender;
import com.cas.tsas.player.domain.model.Handedness;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreatePlayerRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotNull Gender gender,
        @NotNull Handedness handedness,
        @NotNull BackhandType backhandType,
        @Size(max = 50) String ranking,
        @Size(max = 64) String nationality,
        LocalDate birthDate
) {}
