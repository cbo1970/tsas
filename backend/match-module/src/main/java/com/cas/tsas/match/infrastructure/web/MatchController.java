package com.cas.tsas.match.infrastructure.web;

import com.cas.tsas.match.application.port.in.CreateMatchUseCase;
import com.cas.tsas.match.application.port.in.EndMatchUseCase;
import com.cas.tsas.match.application.port.in.GetMatchUseCase;
import com.cas.tsas.match.application.port.in.GetPlayerNotesUseCase;
import com.cas.tsas.match.application.port.in.RecordPointUseCase;
import com.cas.tsas.match.application.port.in.SavePlayerNoteUseCase;
import com.cas.tsas.match.application.port.in.SetScoreUseCase;
import com.cas.tsas.match.application.port.in.SetServingPlayerUseCase;
import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchScore;
import com.cas.tsas.match.infrastructure.web.dto.request.CreateMatchRequest;
import com.cas.tsas.match.infrastructure.web.dto.request.EndMatchWalkoverRequest;
import com.cas.tsas.match.infrastructure.web.dto.request.RecordPointRequest;
import com.cas.tsas.match.infrastructure.web.dto.request.SavePlayerNoteRequest;
import com.cas.tsas.match.infrastructure.web.dto.request.SetScoreRequest;
import com.cas.tsas.match.infrastructure.web.dto.response.MatchResponse;
import com.cas.tsas.match.infrastructure.web.dto.response.MatchScoreResponse;
import com.cas.tsas.match.infrastructure.web.dto.response.MatchWithScoreResponse;
import com.cas.tsas.match.infrastructure.web.dto.response.PlayerNoteResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST adapter exposing the match use cases under {@code /api/matches}.
 * Translates HTTP request DTOs into use-case commands and domain objects into
 * response DTOs.
 */
@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final CreateMatchUseCase createMatchUseCase;
    private final GetMatchUseCase getMatchUseCase;
    private final RecordPointUseCase recordPointUseCase;
    private final SetScoreUseCase setScoreUseCase;
    private final EndMatchUseCase endMatchUseCase;
    private final SetServingPlayerUseCase setServingPlayerUseCase;
    private final SavePlayerNoteUseCase savePlayerNoteUseCase;
    private final GetPlayerNotesUseCase getPlayerNotesUseCase;

    public MatchController(CreateMatchUseCase createMatchUseCase,
                           GetMatchUseCase getMatchUseCase,
                           RecordPointUseCase recordPointUseCase,
                           SetScoreUseCase setScoreUseCase,
                           EndMatchUseCase endMatchUseCase,
                           SetServingPlayerUseCase setServingPlayerUseCase,
                           SavePlayerNoteUseCase savePlayerNoteUseCase,
                           GetPlayerNotesUseCase getPlayerNotesUseCase) {
        this.createMatchUseCase = createMatchUseCase;
        this.getMatchUseCase = getMatchUseCase;
        this.recordPointUseCase = recordPointUseCase;
        this.setScoreUseCase = setScoreUseCase;
        this.endMatchUseCase = endMatchUseCase;
        this.setServingPlayerUseCase = setServingPlayerUseCase;
        this.savePlayerNoteUseCase = savePlayerNoteUseCase;
        this.getPlayerNotesUseCase = getPlayerNotesUseCase;
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

    /**
     * Records a point on the match. The optional point/stroke/direction fields
     * are deserialised by Jackson directly into their domain enums (or {@code
     * null} when absent / blank) before being passed to the use case.
     */
    @PostMapping("/{id}/points")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchWithScoreResponse recordPoint(@PathVariable UUID id,
                                              @Valid @RequestBody RecordPointRequest request) {
        var command = new RecordPointUseCase.RecordPointCommand(
                id,
                request.winner(),
                request.pointType(),
                request.strokeType(),
                request.direction(),
                request.remark(),
                request.serveAttempt()
        );
        MatchScore score = recordPointUseCase.recordPoint(command);
        Match match = getMatchUseCase.findById(id);
        return MatchWithScoreResponse.from(match, score);
    }

    @PostMapping("/{id}/serve/player1")
    public MatchScoreResponse servePlayer1(@PathVariable UUID id) {
        var command = new SetServingPlayerUseCase.SetServingPlayerCommand(id, true);
        return MatchScoreResponse.from(setServingPlayerUseCase.setServingPlayer(command));
    }

    @PostMapping("/{id}/serve/player2")
    public MatchScoreResponse servePlayer2(@PathVariable UUID id) {
        var command = new SetServingPlayerUseCase.SetServingPlayerCommand(id, false);
        return MatchScoreResponse.from(setServingPlayerUseCase.setServingPlayer(command));
    }

    @PutMapping("/{id}/score")
    public MatchScoreResponse setScore(@PathVariable UUID id,
                                       @Valid @RequestBody SetScoreRequest request) {
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

    /** Ends the match by walkover; the request body names the winning player. */
    @PostMapping("/{id}/end/walkover")
    public MatchResponse endMatchWalkover(@PathVariable UUID id,
                                          @Valid @RequestBody EndMatchWalkoverRequest request) {
        return MatchResponse.from(endMatchUseCase.endMatchWalkover(id, "PLAYER1".equals(request.winner())));
    }

    /** TEN-68: the coach's free-text notes for both players of the match (0–2 entries). */
    @GetMapping("/{id}/notes")
    public List<PlayerNoteResponse> getNotes(@PathVariable UUID id) {
        return getPlayerNotesUseCase.forMatch(id).stream()
                .map(PlayerNoteResponse::from)
                .toList();
    }

    /**
     * TEN-68: upserts the note for one player. A blank note deletes it ({@code 204}); a present
     * note returns the saved entry ({@code 200}).
     */
    @PutMapping("/{id}/notes/{playerId}")
    public ResponseEntity<PlayerNoteResponse> saveNote(@PathVariable UUID id,
                                                       @PathVariable UUID playerId,
                                                       @Valid @RequestBody SavePlayerNoteRequest request) {
        return savePlayerNoteUseCase.save(id, playerId, request.note())
                .map(n -> ResponseEntity.ok(PlayerNoteResponse.from(n)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
