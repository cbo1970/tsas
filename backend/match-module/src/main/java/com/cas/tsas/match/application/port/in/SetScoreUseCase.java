package com.cas.tsas.match.application.port.in;

import com.cas.tsas.match.domain.model.MatchScore;

import java.util.UUID;

public interface SetScoreUseCase {

    MatchScore setScore(SetScoreCommand command);

    record SetScoreCommand(
            UUID matchId,
            int pointsPlayer1,
            int pointsPlayer2,
            int gamesPlayer1,
            int gamesPlayer2,
            int setsPlayer1,
            int setsPlayer2,
            boolean isDeuce,
            Boolean isAdvantagePlayer1,
            int currentSet,
            boolean isDone,
            String winner
    ) {}
}
