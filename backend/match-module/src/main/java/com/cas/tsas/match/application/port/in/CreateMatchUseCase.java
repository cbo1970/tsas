package com.cas.tsas.match.application.port.in;

import com.cas.tsas.match.domain.model.Match;

import java.util.UUID;

public interface CreateMatchUseCase {

    Match createMatch(CreateMatchCommand command);

    record CreateMatchCommand(
            UUID player1Id,
            UUID player2Id,
            int setsToWin,
            boolean matchTiebreak,
            boolean shortSet
    ) {}
}
