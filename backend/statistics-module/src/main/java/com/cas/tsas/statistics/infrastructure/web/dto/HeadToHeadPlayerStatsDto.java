package com.cas.tsas.statistics.infrastructure.web.dto;

import com.cas.tsas.statistics.domain.model.HeadToHeadPlayerStats;

import java.util.UUID;

public record HeadToHeadPlayerStatsDto(
        UUID playerId,
        double firstServePercentage,
        double firstServeWonPercentage,
        double secondServeWonPercentage,
        int aces,
        int doubleFaults,
        double returnPointsWonFirstPercentage,
        double returnPointsWonSecondPercentage,
        int breakPointsWon,
        int breakPointsPlayed,
        double breakPointsWonPercentage,
        double returnGamesWonPercentage,
        int winners,
        int unforcedErrors,
        double winnersPercentage,
        double unforcedErrorPercentage,
        int matchesWon,
        int matchesLost,
        int setsWon,
        int setsLost
) {
    public static HeadToHeadPlayerStatsDto from(HeadToHeadPlayerStats s) {
        return new HeadToHeadPlayerStatsDto(
                s.playerId(),
                s.firstServePercentage(), s.firstServeWonPercentage(), s.secondServeWonPercentage(),
                s.aces(), s.doubleFaults(),
                s.returnPointsWonFirstPercentage(), s.returnPointsWonSecondPercentage(),
                s.breakPointsWon(), s.breakPointsPlayed(), s.breakPointsWonPercentage(),
                s.returnGamesWonPercentage(),
                s.winners(), s.unforcedErrors(), s.winnersPercentage(), s.unforcedErrorPercentage(),
                s.matchesWon(), s.matchesLost(), s.setsWon(), s.setsLost());
    }
}
