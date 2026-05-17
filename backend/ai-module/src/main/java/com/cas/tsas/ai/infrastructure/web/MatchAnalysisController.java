package com.cas.tsas.ai.infrastructure.web;

import com.cas.tsas.ai.application.port.in.GenerateMatchAnalysisUseCase;
import com.cas.tsas.ai.application.port.in.GetMatchAnalysisUseCase;
import com.cas.tsas.ai.infrastructure.web.dto.MatchAnalysisResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/matches/{matchId}/analysis")
public class MatchAnalysisController {

    private final GenerateMatchAnalysisUseCase generateUseCase;
    private final GetMatchAnalysisUseCase getUseCase;

    public MatchAnalysisController(GenerateMatchAnalysisUseCase generateUseCase,
                                   GetMatchAnalysisUseCase getUseCase) {
        this.generateUseCase = generateUseCase;
        this.getUseCase = getUseCase;
    }

    @PostMapping
    public MatchAnalysisResponse generate(@PathVariable UUID matchId) {
        return MatchAnalysisResponse.from(generateUseCase.generate(matchId));
    }

    @GetMapping
    public MatchAnalysisResponse get(@PathVariable UUID matchId) {
        return getUseCase.findByMatchId(matchId)
                .map(MatchAnalysisResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No analysis for match " + matchId));
    }
}
