package com.cas.tsas.infrastructure.web.dto.response;

import com.cas.tsas.domain.model.Match;
import com.cas.tsas.domain.model.MatchStatus;

import java.util.UUID;

public record MatchResponse(
        UUID id,
        UUID player1Id,
        UUID player2Id,
        int setsToWin,
        boolean matchTiebreak,
        boolean shortSet,
        MatchStatus status
) {
    public static MatchResponse from(Match match) {
        return new MatchResponse(
                match.getId(),
                match.getPlayer1Id(),
                match.getPlayer2Id(),
                match.getSetsToWin(),
                match.isMatchTiebreak(),
                match.isShortSet(),
                match.getStatus()
        );
    }
}
