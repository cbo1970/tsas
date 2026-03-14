package com.cas.tsas.infrastructure.persistence.entity;

import com.cas.tsas.domain.model.MatchStatus;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "matches")
public class MatchJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "player1_id", nullable = false)
    private UUID player1Id;

    @Column(name = "player2_id", nullable = false)
    private UUID player2Id;

    @Column(name = "sets_to_win", nullable = false)
    private int setsToWin;

    @Column(name = "match_tiebreak", nullable = false)
    private boolean matchTiebreak;

    @Column(name = "short_set", nullable = false)
    private boolean shortSet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    public MatchJpaEntity() {}

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
