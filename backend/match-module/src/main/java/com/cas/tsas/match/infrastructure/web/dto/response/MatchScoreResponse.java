package com.cas.tsas.match.infrastructure.web.dto.response;

import com.cas.tsas.match.domain.model.MatchScore;

import java.util.UUID;

public record MatchScoreResponse(
        UUID id,
        UUID matchId,
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
        String winner,
        int acesPlayer1,
        int acesPlayer2,
        Integer servingPlayer
) {
    public static MatchScoreResponse from(MatchScore score) {
        return new MatchScoreResponse(
                score.getId(),
                score.getMatchId(),
                score.getPointsPlayer1(),
                score.getPointsPlayer2(),
                score.getGamesPlayer1(),
                score.getGamesPlayer2(),
                score.getSetsPlayer1(),
                score.getSetsPlayer2(),
                score.isDeuce(),
                score.getIsAdvantagePlayer1(),
                score.getCurrentSet(),
                score.isDone(),
                score.getWinner(),
                score.getAcesPlayer1(),
                score.getAcesPlayer2(),
                score.getServingPlayer()
        );
    }
}
