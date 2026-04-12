package com.cas.tsas.match.application.port.in;

import com.cas.tsas.match.domain.model.MatchScore;

import java.util.UUID;

public interface RecordAceUseCase {

    MatchScore recordAce(RecordAceCommand command);

    record RecordAceCommand(
            UUID matchId,
            boolean forPlayer1
    ) {}
}
