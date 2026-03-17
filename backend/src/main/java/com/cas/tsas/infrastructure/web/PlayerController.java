package com.cas.tsas.infrastructure.web;

import com.cas.tsas.application.port.in.player.CreatePlayerUseCase;
import com.cas.tsas.application.port.in.player.DeletePlayerUseCase;
import com.cas.tsas.application.port.in.player.SearchPlayerUseCase;
import com.cas.tsas.infrastructure.web.dto.request.CreatePlayerRequest;
import com.cas.tsas.infrastructure.web.dto.response.PlayerResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final CreatePlayerUseCase createPlayerUseCase;
    private final SearchPlayerUseCase searchPlayerUseCase;
    private final DeletePlayerUseCase deletePlayerUseCase;

    public PlayerController(CreatePlayerUseCase createPlayerUseCase,
                            SearchPlayerUseCase searchPlayerUseCase,
                            DeletePlayerUseCase deletePlayerUseCase) {
        this.createPlayerUseCase = createPlayerUseCase;
        this.searchPlayerUseCase = searchPlayerUseCase;
        this.deletePlayerUseCase = deletePlayerUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerResponse createPlayer(@Valid @RequestBody CreatePlayerRequest request) {
        var command = new CreatePlayerUseCase.CreatePlayerCommand(
                request.firstName(),
                request.lastName(),
                request.gender(),
                request.handedness(),
                request.backhandType(),
                request.ranking(),
                request.nationality(),
                request.birthDate()
        );
        var player = createPlayerUseCase.createPlayer(command);
        return PlayerResponse.from(player, true);
    }

    @GetMapping("/{id}")
    public PlayerResponse getPlayer(@PathVariable UUID id) {
        var player = searchPlayerUseCase.findById(id);
        return PlayerResponse.from(player, !deletePlayerUseCase.hasMatches(id));
    }

    @GetMapping
    public List<PlayerResponse> listPlayers() {
        return searchPlayerUseCase.findAll().stream()
                .map(p -> PlayerResponse.from(p, !deletePlayerUseCase.hasMatches(p.getId())))
                .toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlayer(@PathVariable UUID id) {
        deletePlayerUseCase.deletePlayer(id);
    }

    @PatchMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivatePlayer(@PathVariable UUID id) {
        deletePlayerUseCase.deactivatePlayer(id);
    }
}
