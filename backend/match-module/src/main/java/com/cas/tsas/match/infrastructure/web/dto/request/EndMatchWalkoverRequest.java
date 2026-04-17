package com.cas.tsas.match.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record EndMatchWalkoverRequest(
        @NotNull
        @Pattern(regexp = "PLAYER1|PLAYER2")
        String winner
) {}
