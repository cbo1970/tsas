package com.cas.tsas.ai.application.dto;

/** Contextual match data (players and format) supplied to the LLM alongside the statistics. */
public record MatchMetadata(
        PlayerInfo player1,
        PlayerInfo player2,
        int setsToWin,
        boolean matchTiebreak,
        boolean shortSet
) {
    public record PlayerInfo(
            String fullName,
            String ranking,
            String handedness,
            String backhandType
    ) {}
}
