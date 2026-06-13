package com.cas.tsas.statistics.infrastructure.web;

import com.cas.tsas.statistics.application.port.in.ComputeHeadToHeadStatisticsUseCase;
import com.cas.tsas.statistics.infrastructure.web.dto.HeadToHeadStatisticsDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** REST endpoint serving the head-to-head comparison of two players (FA-08). */
@RestController
@RequestMapping("/api/statistics/head-to-head")
public class HeadToHeadController {

    private final ComputeHeadToHeadStatisticsUseCase computeHeadToHead;

    public HeadToHeadController(ComputeHeadToHeadStatisticsUseCase computeHeadToHead) {
        this.computeHeadToHead = computeHeadToHead;
    }

    @GetMapping
    public HeadToHeadStatisticsDto getHeadToHead(@RequestParam("player1") UUID player1,
                                                 @RequestParam("player2") UUID player2) {
        return HeadToHeadStatisticsDto.from(computeHeadToHead.compute(player1, player2));
    }
}
