package com.cas.tsas.statistics.infrastructure.web.dto;

import com.cas.tsas.statistics.domain.model.MatchStatistics;

import java.util.UUID;

public record MatchStatisticsDto(
        UUID matchId,
        PlayerStatisticsDto player1,
        PlayerStatisticsDto player2,
        int totalPoints
) {
    public static MatchStatisticsDto from(MatchStatistics s) {
        return new MatchStatisticsDto(
                s.matchId(),
                PlayerStatisticsDto.from(s.player1()),
                PlayerStatisticsDto.from(s.player2()),
                s.totalPoints());
    }
}
