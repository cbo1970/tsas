package com.cas.tsas.application.port.in.match;

import com.cas.tsas.domain.model.MatchScore;

import java.util.UUID;

public interface RecordPointUseCase {

    MatchScore recordPoint(RecordPointCommand command);

    record RecordPointCommand(
            UUID matchId,
            boolean player1Scored
    ) {}
}
