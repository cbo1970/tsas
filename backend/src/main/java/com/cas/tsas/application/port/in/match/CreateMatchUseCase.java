package com.cas.tsas.application.port.in.match;

import com.cas.tsas.domain.model.Match;

import java.time.LocalDate;

public interface CreateMatchUseCase {

    Match createMatch(CreateMatchCommand command);

    record CreateMatchCommand(
            Long ownPlayerId,
            Long opponentId,
            LocalDate date,
            int setsToWin,
            boolean matchTieBreak,
            boolean shortSet
    ) {}
}
