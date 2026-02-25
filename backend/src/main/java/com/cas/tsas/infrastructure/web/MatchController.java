package com.cas.tsas.infrastructure.web;

import com.cas.tsas.application.port.in.match.CreateMatchUseCase;
import com.cas.tsas.infrastructure.web.dto.request.CreateMatchRequest;
import com.cas.tsas.infrastructure.web.dto.response.MatchResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/matches")
public class MatchController {

    private final CreateMatchUseCase createMatchUseCase;

    public MatchController(CreateMatchUseCase createMatchUseCase) {
        this.createMatchUseCase = createMatchUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchResponse createMatch(@Valid @RequestBody CreateMatchRequest request) {
        var command = new CreateMatchUseCase.CreateMatchCommand(
                request.ownPlayerId(),
                request.opponentId(),
                request.date(),
                request.setsToWin(),
                request.matchTieBreak(),
                request.shortSet()
        );
        return MatchResponse.from(createMatchUseCase.createMatch(command));
    }
}
