package com.cas.tsas.statistics.infrastructure.web.dto;

import com.cas.tsas.statistics.domain.model.MatchStatistics;
import com.cas.tsas.statistics.domain.model.MatchStatisticsBreakdown;

import java.util.List;
import java.util.UUID;

public record MatchStatisticsDto(
        UUID matchId,
        PlayerStatisticsDto player1,
        PlayerStatisticsDto player2,
        int totalPoints,
        List<SetStatisticsDto> sets
) {
    public static MatchStatisticsDto from(MatchStatisticsBreakdown b) {
        MatchStatistics t = b.total();
        return new MatchStatisticsDto(
                t.matchId(),
                PlayerStatisticsDto.from(t.player1()),
                PlayerStatisticsDto.from(t.player2()),
                t.totalPoints(),
                b.sets().stream().map(SetStatisticsDto::from).toList());
    }
}
