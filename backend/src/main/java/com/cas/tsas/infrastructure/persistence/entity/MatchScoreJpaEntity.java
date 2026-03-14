package com.cas.tsas.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "match_scores")
public class MatchScoreJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "match_id", nullable = false, unique = true)
    private UUID matchId;

    @Column(name = "points_player1")
    private int pointsPlayer1;

    @Column(name = "points_player2")
    private int pointsPlayer2;

    @Column(name = "games_player1")
    private int gamesPlayer1;

    @Column(name = "games_player2")
    private int gamesPlayer2;

    @Column(name = "sets_player1")
    private int setsPlayer1;

    @Column(name = "sets_player2")
    private int setsPlayer2;

    @Column(name = "is_deuce")
    private boolean isDeuce;

    @Column(name = "is_advantage_player1")
    private Boolean isAdvantagePlayer1;

    @Column(name = "current_set")
    private int currentSet;

    @Column(name = "is_done")
    private boolean isDone;

    @Column(name = "winner")
    private String winner;

    public MatchScoreJpaEntity() {}

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
