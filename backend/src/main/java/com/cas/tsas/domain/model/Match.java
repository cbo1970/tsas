package com.cas.tsas.domain.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain entity representing a tennis match (Begegnung).
 * Pure POJO — no framework dependencies.
 */
public class Match {

    private Long id;
    private Player ownPlayer;
    private Player opponent;
    private LocalDate date;
    private int setsToWin;
    private boolean matchTieBreak;
    private boolean shortSet;
    private List<Point> points = new ArrayList<>();

    public Match() {}

    public Match(Long id, Player ownPlayer, Player opponent, LocalDate date,
                 int setsToWin, boolean matchTieBreak, boolean shortSet) {
        this.id = id;
        this.ownPlayer = ownPlayer;
        this.opponent = opponent;
        this.date = date;
        this.setsToWin = setsToWin;
        this.matchTieBreak = matchTieBreak;
        this.shortSet = shortSet;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Player getOwnPlayer() { return ownPlayer; }
    public void setOwnPlayer(Player ownPlayer) { this.ownPlayer = ownPlayer; }

    public Player getOpponent() { return opponent; }
    public void setOpponent(Player opponent) { this.opponent = opponent; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public int getSetsToWin() { return setsToWin; }
    public void setSetsToWin(int setsToWin) { this.setsToWin = setsToWin; }

    public boolean isMatchTieBreak() { return matchTieBreak; }
    public void setMatchTieBreak(boolean matchTieBreak) { this.matchTieBreak = matchTieBreak; }

    public boolean isShortSet() { return shortSet; }
    public void setShortSet(boolean shortSet) { this.shortSet = shortSet; }

    public List<Point> getPoints() { return points; }
    public void setPoints(List<Point> points) { this.points = points; }
}
