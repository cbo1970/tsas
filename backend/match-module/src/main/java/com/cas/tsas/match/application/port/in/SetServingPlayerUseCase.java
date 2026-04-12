package com.cas.tsas.match.application.port.in;

import com.cas.tsas.match.domain.model.MatchScore;

import java.util.UUID;

public interface SetServingPlayerUseCase {

    MatchScore setServingPlayer(SetServingPlayerCommand command);

    record SetServingPlayerCommand(
            UUID matchId,
            boolean forPlayer1
    ) {}
}
