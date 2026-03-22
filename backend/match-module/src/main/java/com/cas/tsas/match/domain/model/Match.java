package com.cas.tsas.match.domain.model;

import java.util.UUID;

/**
 * Domain entity representing a tennis match (Begegnung).
 * Pure POJO — no framework dependencies.
 */
public class Match {

    private UUID id;
    private UUID player1Id;
    private UUID player2Id;
    private int setsToWin;
    private boolean matchTiebreak;
    private boolean shortSet;
    private MatchStatus status;

    public Match() {}

    public Match(UUID id, UUID player1Id, UUID player2Id, int setsToWin,
                 boolean matchTiebreak, boolean shortSet, MatchStatus status) {
        this.id = id;
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.setsToWin = setsToWin;
        this.matchTiebreak = matchTiebreak;
        this.shortSet = shortSet;
        this.status = status;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPlayer1Id() { return player1Id; }
    public void setPlayer1Id(UUID player1Id) { this.player1Id = player1Id; }

    public UUID getPlayer2Id() { return player2Id; }
    public void setPlayer2Id(UUID player2Id) { this.player2Id = player2Id; }

    public int getSetsToWin() { return setsToWin; }
    public void setSetsToWin(int setsToWin) { this.setsToWin = setsToWin; }

    public boolean isMatchTiebreak() { return matchTiebreak; }
    public void setMatchTiebreak(boolean matchTiebreak) { this.matchTiebreak = matchTiebreak; }

    public boolean isShortSet() { return shortSet; }
    public void setShortSet(boolean shortSet) { this.shortSet = shortSet; }

    public MatchStatus getStatus() { return status; }
    public void setStatus(MatchStatus status) { this.status = status; }
}
