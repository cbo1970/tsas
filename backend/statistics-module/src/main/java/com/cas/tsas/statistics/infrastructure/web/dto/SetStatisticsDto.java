package com.cas.tsas.statistics.infrastructure.web.dto;

import com.cas.tsas.statistics.domain.model.SetStatistics;

public record SetStatisticsDto(
        int setNumber,
        PlayerStatisticsDto player1,
        PlayerStatisticsDto player2,
        int totalPoints
) {
    public static SetStatisticsDto from(SetStatistics s) {
        return new SetStatisticsDto(
                s.setNumber(),
                PlayerStatisticsDto.from(s.stats().player1()),
                PlayerStatisticsDto.from(s.stats().player2()),
                s.stats().totalPoints());
    }
}
