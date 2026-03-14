package com.cas.tsas.infrastructure.web;

import com.cas.tsas.application.port.in.player.CreatePlayerUseCase;
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

    public PlayerController(CreatePlayerUseCase createPlayerUseCase,
                            SearchPlayerUseCase searchPlayerUseCase) {
        this.createPlayerUseCase = createPlayerUseCase;
        this.searchPlayerUseCase = searchPlayerUseCase;
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
        return PlayerResponse.from(createPlayerUseCase.createPlayer(command));
    }

    @GetMapping("/{id}")
    public PlayerResponse getPlayer(@PathVariable UUID id) {
        return PlayerResponse.from(searchPlayerUseCase.findById(id));
    }

    @GetMapping
    public List<PlayerResponse> listPlayers() {
        return searchPlayerUseCase.findAll().stream()
                .map(PlayerResponse::from)
                .toList();
    }
}
