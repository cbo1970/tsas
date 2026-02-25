package com.cas.tsas.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "matches")
public class MatchJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "own_player_id")
    private PlayerJpaEntity ownPlayer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opponent_id")
    private PlayerJpaEntity opponent;

    @Column(nullable = false)
    private LocalDate date;

    private int setsToWin;
    private boolean matchTieBreak;
    private boolean shortSet;

    public MatchJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PlayerJpaEntity getOwnPlayer() { return ownPlayer; }
    public void setOwnPlayer(PlayerJpaEntity ownPlayer) { this.ownPlayer = ownPlayer; }

    public PlayerJpaEntity getOpponent() { return opponent; }
    public void setOpponent(PlayerJpaEntity opponent) { this.opponent = opponent; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public int getSetsToWin() { return setsToWin; }
    public void setSetsToWin(int setsToWin) { this.setsToWin = setsToWin; }

    public boolean isMatchTieBreak() { return matchTieBreak; }
    public void setMatchTieBreak(boolean matchTieBreak) { this.matchTieBreak = matchTieBreak; }

    public boolean isShortSet() { return shortSet; }
    public void setShortSet(boolean shortSet) { this.shortSet = shortSet; }
}
