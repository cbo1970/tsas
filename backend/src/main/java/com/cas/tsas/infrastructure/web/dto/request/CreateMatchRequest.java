package com.cas.tsas.infrastructure.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateMatchRequest(
        @NotNull UUID player1Id,
        @NotNull UUID player2Id,
        @Min(2) @Max(3) int setsToWin,
        boolean matchTiebreak,
        boolean shortSet
) {}
