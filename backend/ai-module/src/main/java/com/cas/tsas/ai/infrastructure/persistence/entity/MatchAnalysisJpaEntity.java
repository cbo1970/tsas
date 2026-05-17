package com.cas.tsas.ai.infrastructure.persistence.entity;

import com.cas.tsas.ai.domain.model.AnalysisStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "match_analysis")
public class MatchAnalysisJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "match_id", nullable = false, unique = true)
    private UUID matchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AnalysisStatus status;

    @Column(name = "key_moments", columnDefinition = "TEXT")
    private String keyMoments;

    @Column(name = "own_strengths", columnDefinition = "TEXT")
    private String ownStrengths;

    @Column(name = "own_weaknesses", columnDefinition = "TEXT")
    private String ownWeaknesses;

    @Column(name = "opponent_strengths", columnDefinition = "TEXT")
    private String opponentStrengths;

    @Column(name = "opponent_weaknesses", columnDefinition = "TEXT")
    private String opponentWeaknesses;

    @Column(name = "recommendations", nullable = false, columnDefinition = "TEXT")
    private String recommendationsJson;

    @Column(name = "model_used", length = 64)
    private String modelUsed;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    public MatchAnalysisJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getMatchId() { return matchId; }
    public void setMatchId(UUID v) { this.matchId = v; }
    public AnalysisStatus getStatus() { return status; }
    public void setStatus(AnalysisStatus v) { this.status = v; }
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
    public String getRecommendationsJson() { return recommendationsJson; }
    public void setRecommendationsJson(String v) { this.recommendationsJson = v; }
    public String getModelUsed() { return modelUsed; }
    public void setModelUsed(String v) { this.modelUsed = v; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { this.errorMessage = v; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant v) { this.generatedAt = v; }
}
