package com.cas.tsas.match.infrastructure.web;

import com.cas.tsas.match.application.port.in.GetMatchHistoryUseCase;
import com.cas.tsas.match.infrastructure.web.dto.MatchHistoryEntryDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Abgeschlossene Match-History eines Spielers (TEN-35). */
@RestController
public class PlayerMatchHistoryController {

    private final GetMatchHistoryUseCase getMatchHistoryUseCase;

    public PlayerMatchHistoryController(GetMatchHistoryUseCase getMatchHistoryUseCase) {
        this.getMatchHistoryUseCase = getMatchHistoryUseCase;
    }

    @GetMapping("/api/players/{playerId}/matches")
    public List<MatchHistoryEntryDto> getHistory(@PathVariable UUID playerId) {
        return getMatchHistoryUseCase.forPlayer(playerId).stream()
                .map(MatchHistoryEntryDto::from)
                .toList();
    }
}
