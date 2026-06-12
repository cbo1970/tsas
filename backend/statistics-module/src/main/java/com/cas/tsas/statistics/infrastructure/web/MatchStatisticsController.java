package com.cas.tsas.statistics.infrastructure.web;

import com.cas.tsas.match.application.port.in.GetMatchUseCase;
import com.cas.tsas.statistics.application.port.in.ComputeMatchStatisticsUseCase;
import com.cas.tsas.statistics.infrastructure.web.dto.MatchStatisticsDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** REST endpoint serving the computed statistics for a single match. */
@RestController
@RequestMapping("/api/matches/{id}/statistics")
public class MatchStatisticsController {

    private final GetMatchUseCase getMatchUseCase;
    private final ComputeMatchStatisticsUseCase computeStatistics;

    public MatchStatisticsController(GetMatchUseCase getMatchUseCase,
                                     ComputeMatchStatisticsUseCase computeStatistics) {
        this.getMatchUseCase = getMatchUseCase;
        this.computeStatistics = computeStatistics;
    }

    @GetMapping
    public MatchStatisticsDto getStatistics(@PathVariable UUID id) {
        getMatchUseCase.findById(id); // throws MatchNotFoundException → 404 via GlobalExceptionHandler
        return MatchStatisticsDto.from(computeStatistics.compute(id));
    }
}
