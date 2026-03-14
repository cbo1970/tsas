package com.cas.tsas.infrastructure.web.dto.request;

public record SetScoreRequest(
        int pointsPlayer1,
        int pointsPlayer2,
        int gamesPlayer1,
        int gamesPlayer2,
        int setsPlayer1,
        int setsPlayer2,
        boolean isDeuce,
        Boolean isAdvantagePlayer1,
        int currentSet,
        boolean isDone,
        String winner
) {}
