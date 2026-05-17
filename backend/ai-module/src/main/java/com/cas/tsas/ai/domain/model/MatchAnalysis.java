package com.cas.tsas.ai.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class MatchAnalysis {

    private UUID id;
    private UUID matchId;
    private AnalysisStatus status;
    private String keyMoments;
    private String ownStrengths;
    private String ownWeaknesses;
    private String opponentStrengths;
    private String opponentWeaknesses;
    private List<Recommendation> recommendations;
    private String modelUsed;
    private Instant generatedAt;
    private String errorMessage;

    public MatchAnalysis() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getMatchId() { return matchId; }
    public void setMatchId(UUID matchId) { this.matchId = matchId; }
    public AnalysisStatus getStatus() { return status; }
    public void setStatus(AnalysisStatus status) { this.status = status; }
    public String getKeyMoments() { return keyMoments; }
    public void setKeyMoments(String v) { this.keyMoments = v; }
    public String getOwnStrengths() { return ownStrengths; }
    public void setOwnStrengths(String v) { this.ownStrengths = v; }
    public String getOwnWeaknesses() { return ownWeaknesses; }
    public void setOwnWeaknesses(String v) { this.ownWeaknesses = v; }
    public String getOpponentStrengths() { return opponentStrengths; }
    public void setOpponentStrengths(String v) { this.opponentStrengths = v; }
    public String getOpponentWeaknesses() { return opponentWeaknesses; }
    public void setOpponentWeaknesses(String v) { this.opponentWeaknesses = v; }
    public List<Recommendation> getRecommendations() { return recommendations; }
    public void setRecommendations(List<Recommendation> v) { this.recommendations = v; }
    public String getModelUsed() { return modelUsed; }
    public void setModelUsed(String v) { this.modelUsed = v; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant v) { this.generatedAt = v; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { this.errorMessage = v; }
}
