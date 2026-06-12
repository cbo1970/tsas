package com.cas.tsas.ai.infrastructure.web;

import com.cas.tsas.ai.application.port.in.GenerateMatchAnalysisUseCase;
import com.cas.tsas.ai.application.port.in.GetMatchAnalysisUseCase;
import com.cas.tsas.ai.infrastructure.web.dto.MatchAnalysisResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/** REST endpoints to trigger generation of and retrieve the AI analysis for a match. */
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
    public ResponseEntity<MatchAnalysisResponse> generate(@PathVariable UUID matchId,
                                                           UriComponentsBuilder uriBuilder) {
        MatchAnalysisResponse body = MatchAnalysisResponse.from(generateUseCase.generate(matchId));
        URI location = uriBuilder.path("/api/matches/{matchId}/analysis")
                .buildAndExpand(matchId).toUri();
        return ResponseEntity.created(location).body(body);
    }

    @GetMapping
    public MatchAnalysisResponse get(@PathVariable UUID matchId) {
        return getUseCase.findByMatchId(matchId)
                .map(MatchAnalysisResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No analysis for match " + matchId));
    }
}
