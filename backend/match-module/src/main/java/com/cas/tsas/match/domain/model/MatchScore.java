package com.cas.tsas.match.domain.model;

import java.util.UUID;

/**
 * Domain entity representing the running score of a match.
 * Pure POJO — no framework dependencies.
 */
public class MatchScore {

    private UUID id;
    private UUID matchId;
    private int pointsPlayer1;
    private int pointsPlayer2;
    private int gamesPlayer1;
    private int gamesPlayer2;
    private int setsPlayer1;
    private int setsPlayer2;
    private boolean isDeuce;
    private Boolean isAdvantagePlayer1;
    private int currentSet;
    private boolean isDone;
    private String winner;

    public MatchScore() {}

    public MatchScore(UUID id, UUID matchId,
                      int pointsPlayer1, int pointsPlayer2,
                      int gamesPlayer1, int gamesPlayer2,
                      int setsPlayer1, int setsPlayer2,
                      boolean isDeuce, Boolean isAdvantagePlayer1,
                      int currentSet, boolean isDone, String winner) {
        this.id = id;
        this.matchId = matchId;
        this.pointsPlayer1 = pointsPlayer1;
        this.pointsPlayer2 = pointsPlayer2;
        this.gamesPlayer1 = gamesPlayer1;
        this.gamesPlayer2 = gamesPlayer2;
        this.setsPlayer1 = setsPlayer1;
        this.setsPlayer2 = setsPlayer2;
        this.isDeuce = isDeuce;
        this.isAdvantagePlayer1 = isAdvantagePlayer1;
        this.currentSet = currentSet;
        this.isDone = isDone;
        this.winner = winner;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getMatchId() { return matchId; }
    public void setMatchId(UUID matchId) { this.matchId = matchId; }

    public int getPointsPlayer1() { return pointsPlayer1; }
    public void setPointsPlayer1(int pointsPlayer1) { this.pointsPlayer1 = pointsPlayer1; }

    public int getPointsPlayer2() { return pointsPlayer2; }
    public void setPointsPlayer2(int pointsPlayer2) { this.pointsPlayer2 = pointsPlayer2; }

    public int getGamesPlayer1() { return gamesPlayer1; }
    public void setGamesPlayer1(int gamesPlayer1) { this.gamesPlayer1 = gamesPlayer1; }

    public int getGamesPlayer2() { return gamesPlayer2; }
    public void setGamesPlayer2(int gamesPlayer2) { this.gamesPlayer2 = gamesPlayer2; }

    public int getSetsPlayer1() { return setsPlayer1; }
    public void setSetsPlayer1(int setsPlayer1) { this.setsPlayer1 = setsPlayer1; }

    public int getSetsPlayer2() { return setsPlayer2; }
    public void setSetsPlayer2(int setsPlayer2) { this.setsPlayer2 = setsPlayer2; }

    public boolean isDeuce() { return isDeuce; }
    public void setDeuce(boolean deuce) { isDeuce = deuce; }

    public Boolean getIsAdvantagePlayer1() { return isAdvantagePlayer1; }
    public void setIsAdvantagePlayer1(Boolean isAdvantagePlayer1) { this.isAdvantagePlayer1 = isAdvantagePlayer1; }

    public int getCurrentSet() { return currentSet; }
    public void setCurrentSet(int currentSet) { this.currentSet = currentSet; }

    public boolean isDone() { return isDone; }
    public void setDone(boolean done) { isDone = done; }

    public String getWinner() { return winner; }
    public void setWinner(String winner) { this.winner = winner; }
}
