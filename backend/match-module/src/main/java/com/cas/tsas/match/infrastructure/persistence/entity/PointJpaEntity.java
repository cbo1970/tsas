package com.cas.tsas.match.infrastructure.persistence.entity;

import com.cas.tsas.match.domain.model.Direction;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.match.domain.model.StrokeType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "points")
public class PointJpaEntity {

    public PointJpaEntity() {}

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @Column(name = "set_number", nullable = false)
    private int setNumber;

    @Column(name = "game_number", nullable = false)
    private int gameNumber;

    @Column(name = "point_number", nullable = false)
    private int pointNumber;

    @Column(name = "winner", nullable = false)
    private int winner;

    @Enumerated(EnumType.STRING)
    @Column(name = "point_type", nullable = true, length = 50)
    private PointType pointType;

    @Enumerated(EnumType.STRING)
    @Column(name = "stroke_type", length = 50)
    private StrokeType strokeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 50)
    private Direction direction;

    @Column(name = "serving_player")
    private Integer servingPlayer;

    @Column(name = "is_break_point", nullable = false)
    private boolean isBreakPoint;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "serve_attempt")
    private Integer serveAttempt;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @PrePersist
    void prePersist() {
        recordedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getMatchId() { return matchId; }
    public void setMatchId(UUID matchId) { this.matchId = matchId; }
    public int getSetNumber() { return setNumber; }
    public void setSetNumber(int setNumber) { this.setNumber = setNumber; }
    public int getGameNumber() { return gameNumber; }
    public void setGameNumber(int gameNumber) { this.gameNumber = gameNumber; }
    public int getPointNumber() { return pointNumber; }
    public void setPointNumber(int pointNumber) { this.pointNumber = pointNumber; }
    public int getWinner() { return winner; }
    public void setWinner(int winner) { this.winner = winner; }
    public PointType getPointType() { return pointType; }
    public void setPointType(PointType pointType) { this.pointType = pointType; }
    public StrokeType getStrokeType() { return strokeType; }
    public void setStrokeType(StrokeType strokeType) { this.strokeType = strokeType; }
    public Direction getDirection() { return direction; }
    public void setDirection(Direction direction) { this.direction = direction; }
    public Integer getServingPlayer() { return servingPlayer; }
    public void setServingPlayer(Integer servingPlayer) { this.servingPlayer = servingPlayer; }
    public boolean isBreakPoint() { return isBreakPoint; }
    public void setBreakPoint(boolean breakPoint) { isBreakPoint = breakPoint; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getServeAttempt() { return serveAttempt; }
    public void setServeAttempt(Integer serveAttempt) { this.serveAttempt = serveAttempt; }
    public Instant getRecordedAt() { return recordedAt; }
}
