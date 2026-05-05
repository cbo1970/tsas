package com.cas.tsas.match.application.port.in;

import com.cas.tsas.match.domain.model.Direction;
import com.cas.tsas.match.domain.model.MatchScore;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.match.domain.model.StrokeType;

import java.util.UUID;

public interface RecordPointUseCase {

    MatchScore recordPoint(RecordPointCommand command);

    record RecordPointCommand(
            UUID matchId,
            int winner,
            PointType pointType,
            StrokeType strokeType,
            Direction direction,
            String remark
    ) {}
}
