package com.cas.tsas.match.infrastructure.web.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record SetScoreRequest(
        @Min(0) int pointsPlayer1,
        @Min(0) int pointsPlayer2,
        @Min(0) int gamesPlayer1,
        @Min(0) int gamesPlayer2,
        @Min(0) int setsPlayer1,
        @Min(0) int setsPlayer2,
        boolean isDeuce,
        Boolean isAdvantagePlayer1,
        @Min(1) int currentSet,
        boolean isDone,
        @Pattern(regexp = "PLAYER1|PLAYER2") String winner
) {}
