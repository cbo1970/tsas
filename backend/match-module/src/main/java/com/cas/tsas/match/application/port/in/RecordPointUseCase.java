package com.cas.tsas.match.application.port.in;

import com.cas.tsas.match.domain.model.MatchScore;

import java.util.UUID;

public interface RecordPointUseCase {

    MatchScore recordPoint(RecordPointCommand command);

    record RecordPointCommand(
            UUID matchId,
            boolean player1Scored
    ) {}
}
