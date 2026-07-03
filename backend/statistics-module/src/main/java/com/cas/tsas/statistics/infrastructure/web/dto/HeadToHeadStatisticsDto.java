package com.cas.tsas.statistics.infrastructure.web.dto;

import com.cas.tsas.statistics.domain.model.HeadToHeadStatistics;

import java.util.UUID;

public record HeadToHeadStatisticsDto(
        UUID player1Id,
        UUID player2Id,
        int matchesPlayed,
        HeadToHeadPlayerStatsDto player1,
        HeadToHeadPlayerStatsDto player2
) {
    public static HeadToHeadStatisticsDto from(HeadToHeadStatistics s) {
        return new HeadToHeadStatisticsDto(
                s.player1Id(), s.player2Id(), s.matchesPlayed(),
                HeadToHeadPlayerStatsDto.from(s.player1()),
                HeadToHeadPlayerStatsDto.from(s.player2()));
    }
}
