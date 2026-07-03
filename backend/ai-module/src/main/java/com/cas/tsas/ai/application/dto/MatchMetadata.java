package com.cas.tsas.ai.application.dto;

import java.util.List;

/** Contextual match data (players, format, and TEN-68 coach notes) supplied to the LLM. */
public record MatchMetadata(
        PlayerInfo player1,
        PlayerInfo player2,
        int setsToWin,
        boolean matchTiebreak,
        boolean shortSet,
        String player1Note,         // nullable — coach note for player1 (postmortem)
        String player2Note,         // nullable — coach note for player2 (postmortem)
        List<String> opponentNotes  // never null — notes about the opponent (opponent preparation)
) {
    /** Normalises {@code opponentNotes} to an empty list when null. */
    public MatchMetadata {
        if (opponentNotes == null) {
            opponentNotes = List.of();
        }
    }

    /** Backward-compatible constructor without coach notes (existing call sites / tests). */
    public MatchMetadata(PlayerInfo player1, PlayerInfo player2, int setsToWin,
                         boolean matchTiebreak, boolean shortSet) {
        this(player1, player2, setsToWin, matchTiebreak, shortSet, null, null, List.of());
    }

    public record PlayerInfo(
            String fullName,
            String ranking,
            String handedness,
            String backhandType
    ) {}
}
