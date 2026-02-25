package com.cas.tsas.application.port.in.match;

import com.cas.tsas.domain.model.Point;

public interface RecordPointUseCase {

    Point recordPoint(RecordPointCommand command);

    record RecordPointCommand(
            Long matchId,
            String ownAttribute,
            String opponentAttribute,
            String remark
    ) {}
}
