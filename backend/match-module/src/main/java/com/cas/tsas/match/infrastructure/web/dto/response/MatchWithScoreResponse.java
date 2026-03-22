package com.cas.tsas.match.infrastructure.web.dto.response;

import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchScore;
import com.cas.tsas.match.domain.model.MatchStatus;

import java.util.UUID;

public record MatchWithScoreResponse(
        UUID id,
        UUID player1Id,
        UUID player2Id,
        int setsToWin,
        boolean matchTiebreak,
        boolean shortSet,
        MatchStatus status,
        MatchScoreResponse score
) {
    public static MatchWithScoreResponse from(Match match, MatchScore matchScore) {
        return new MatchWithScoreResponse(
                match.getId(),
                match.getPlayer1Id(),
                match.getPlayer2Id(),
                match.getSetsToWin(),
                match.isMatchTiebreak(),
                match.isShortSet(),
                match.getStatus(),
                MatchScoreResponse.from(matchScore)
        );
    }
}
