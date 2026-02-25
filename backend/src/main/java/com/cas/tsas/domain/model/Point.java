package com.cas.tsas.domain.model;

/**
 * Domain entity representing a single point in a match.
 * Pure POJO — no framework dependencies.
 */
public class Point {

    private Long id;
    private Long matchId;
    private String ownAttribute;
    private String opponentAttribute;
    private String remark;

    public Point() {}

    public Point(Long id, Long matchId, String ownAttribute,
                 String opponentAttribute, String remark) {
        this.id = id;
        this.matchId = matchId;
        this.ownAttribute = ownAttribute;
        this.opponentAttribute = opponentAttribute;
        this.remark = remark;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }

    public String getOwnAttribute() { return ownAttribute; }
    public void setOwnAttribute(String ownAttribute) { this.ownAttribute = ownAttribute; }

    public String getOpponentAttribute() { return opponentAttribute; }
    public void setOpponentAttribute(String opponentAttribute) { this.opponentAttribute = opponentAttribute; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
