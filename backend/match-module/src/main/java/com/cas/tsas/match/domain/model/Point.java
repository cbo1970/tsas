package com.cas.tsas.match.domain.model;

import java.util.UUID;

public class Point {

    private UUID id;
    private UUID matchId;
    private int setNumber;
    private int gameNumber;
    private int pointNumber;
    private int winner;
    private PointType pointType;
    private StrokeType strokeType;
    private Direction direction;
    private Integer servingPlayer;
    private boolean isBreakPoint;
    private String remark;

    public Point() {}

    public Point(UUID id, UUID matchId, int setNumber, int gameNumber, int pointNumber,
                 int winner, PointType pointType, StrokeType strokeType, Direction direction,
                 Integer servingPlayer, boolean isBreakPoint, String remark) {
        this.id = id;
        this.matchId = matchId;
        this.setNumber = setNumber;
        this.gameNumber = gameNumber;
        this.pointNumber = pointNumber;
        this.winner = winner;
        this.pointType = pointType;
        this.strokeType = strokeType;
        this.direction = direction;
        this.servingPlayer = servingPlayer;
        this.isBreakPoint = isBreakPoint;
        this.remark = remark;
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
}
