package com.cas.tsas.infrastructure.web.dto.response;

import com.cas.tsas.domain.model.Match;

import java.time.LocalDate;

public record MatchResponse(
        Long id,
        Long ownPlayerId,
        String ownPlayerName,
        Long opponentId,
        String opponentName,
        LocalDate date,
        int setsToWin,
        boolean matchTieBreak,
        boolean shortSet
) {
    public static MatchResponse from(Match match) {
        return new MatchResponse(
                match.getId(),
                match.getOwnPlayer().getId(),
                match.getOwnPlayer().getName(),
                match.getOpponent().getId(),
                match.getOpponent().getName(),
                match.getDate(),
                match.getSetsToWin(),
                match.isMatchTieBreak(),
                match.isShortSet()
        );
    }
}
