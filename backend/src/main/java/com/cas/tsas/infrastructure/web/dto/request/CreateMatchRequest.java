package com.cas.tsas.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateMatchRequest(
        @NotNull Long ownPlayerId,
        @NotNull Long opponentId,
        @NotNull LocalDate date,
        int setsToWin,
        boolean matchTieBreak,
        boolean shortSet
) {}
