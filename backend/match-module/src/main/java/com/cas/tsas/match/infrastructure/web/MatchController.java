package com.cas.tsas.match.infrastructure.web;

import com.cas.tsas.match.application.port.in.CreateMatchUseCase;
import com.cas.tsas.match.application.port.in.EndMatchUseCase;
import com.cas.tsas.match.application.port.in.GetMatchUseCase;
import com.cas.tsas.match.application.port.in.RecordPointUseCase;
import com.cas.tsas.match.application.port.in.SetScoreUseCase;
import com.cas.tsas.match.infrastructure.web.dto.request.CreateMatchRequest;
import com.cas.tsas.match.infrastructure.web.dto.request.SetScoreRequest;
import com.cas.tsas.match.infrastructure.web.dto.response.MatchResponse;
import com.cas.tsas.match.infrastructure.web.dto.response.MatchScoreResponse;
import com.cas.tsas.match.infrastructure.web.dto.response.MatchWithScoreResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final CreateMatchUseCase createMatchUseCase;
    private final GetMatchUseCase getMatchUseCase;
    private final RecordPointUseCase recordPointUseCase;
    private final SetScoreUseCase setScoreUseCase;
    private final EndMatchUseCase endMatchUseCase;

    public MatchController(CreateMatchUseCase createMatchUseCase,
                           GetMatchUseCase getMatchUseCase,
                           RecordPointUseCase recordPointUseCase,
                           SetScoreUseCase setScoreUseCase,
                           EndMatchUseCase endMatchUseCase) {
        this.createMatchUseCase = createMatchUseCase;
        this.getMatchUseCase = getMatchUseCase;
        this.recordPointUseCase = recordPointUseCase;
        this.setScoreUseCase = setScoreUseCase;
        this.endMatchUseCase = endMatchUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchResponse createMatch(@Valid @RequestBody CreateMatchRequest request) {
        var command = new CreateMatchUseCase.CreateMatchCommand(
                request.player1Id(),
                request.player2Id(),
                request.setsToWin(),
                request.matchTiebreak(),
                request.shortSet()
        );
        return MatchResponse.from(createMatchUseCase.createMatch(command));
    }

    @GetMapping
    public List<MatchResponse> listMatches() {
        return getMatchUseCase.findAll().stream()
                .map(MatchResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public MatchWithScoreResponse getMatch(@PathVariable UUID id) {
        var match = getMatchUseCase.findById(id);
        var score = getMatchUseCase.getScore(id);
        return MatchWithScoreResponse.from(match, score);
    }

    @PostMapping("/{id}/score/player1")
    public MatchScoreResponse scorePlayer1(@PathVariable UUID id) {
        var command = new RecordPointUseCase.RecordPointCommand(id, true);
        return MatchScoreResponse.from(recordPointUseCase.recordPoint(command));
    }

    @PostMapping("/{id}/score/player2")
    public MatchScoreResponse scorePlayer2(@PathVariable UUID id) {
        var command = new RecordPointUseCase.RecordPointCommand(id, false);
        return MatchScoreResponse.from(recordPointUseCase.recordPoint(command));
    }

    @PutMapping("/{id}/score")
    public MatchScoreResponse setScore(@PathVariable UUID id,
                                       @RequestBody SetScoreRequest request) {
        var command = new SetScoreUseCase.SetScoreCommand(
                id,
                request.pointsPlayer1(),
                request.pointsPlayer2(),
                request.gamesPlayer1(),
                request.gamesPlayer2(),
                request.setsPlayer1(),
                request.setsPlayer2(),
                request.isDeuce(),
                request.isAdvantagePlayer1(),
                request.currentSet(),
                request.isDone(),
                request.winner()
        );
        return MatchScoreResponse.from(setScoreUseCase.setScore(command));
    }

    @PostMapping("/{id}/end")
    public MatchResponse endMatch(@PathVariable UUID id) {
        return MatchResponse.from(endMatchUseCase.endMatch(id));
    }
}
