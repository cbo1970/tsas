package com.cas.tsas.ai.infrastructure.persistence.adapter;

import com.cas.tsas.ai.application.port.out.LoadMatchAnalysisPort;
import com.cas.tsas.ai.application.port.out.SaveMatchAnalysisPort;
import com.cas.tsas.ai.domain.model.MatchAnalysis;
import com.cas.tsas.ai.domain.model.Recommendation;
import com.cas.tsas.ai.infrastructure.persistence.entity.MatchAnalysisJpaEntity;
import com.cas.tsas.ai.infrastructure.persistence.repository.MatchAnalysisJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MatchAnalysisPersistenceAdapter implements SaveMatchAnalysisPort, LoadMatchAnalysisPort {

    private static final TypeReference<List<Recommendation>> RECOMMENDATION_LIST =
            new TypeReference<>() {};

    private final MatchAnalysisJpaRepository repo;
    private final ObjectMapper objectMapper;

    public MatchAnalysisPersistenceAdapter(MatchAnalysisJpaRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

    @Override
    public MatchAnalysis save(MatchAnalysis a) {
        MatchAnalysisJpaEntity e = repo.findByMatchId(a.getMatchId())
                .orElseGet(MatchAnalysisJpaEntity::new);
        e.setMatchId(a.getMatchId());
        e.setStatus(a.getStatus());
        e.setKeyMoments(a.getKeyMoments());
        e.setOwnStrengths(a.getOwnStrengths());
        e.setOwnWeaknesses(a.getOwnWeaknesses());
        e.setOpponentStrengths(a.getOpponentStrengths());
        e.setOpponentWeaknesses(a.getOpponentWeaknesses());
        e.setRecommendationsJson(writeJson(a.getRecommendations()));
        e.setModelUsed(a.getModelUsed());
        e.setErrorMessage(a.getErrorMessage());
        e.setGeneratedAt(a.getGeneratedAt());
        MatchAnalysisJpaEntity saved = repo.save(e);
        a.setId(saved.getId());
        return a;
    }

    @Override
    public Optional<MatchAnalysis> loadByMatchId(UUID matchId) {
        return repo.findByMatchId(matchId).map(this::toDomain);
    }

    private MatchAnalysis toDomain(MatchAnalysisJpaEntity e) {
        MatchAnalysis a = new MatchAnalysis();
        a.setId(e.getId());
        a.setMatchId(e.getMatchId());
        a.setStatus(e.getStatus());
        a.setKeyMoments(e.getKeyMoments());
        a.setOwnStrengths(e.getOwnStrengths());
        a.setOwnWeaknesses(e.getOwnWeaknesses());
        a.setOpponentStrengths(e.getOpponentStrengths());
        a.setOpponentWeaknesses(e.getOpponentWeaknesses());
        a.setRecommendations(readJson(e.getRecommendationsJson()));
        a.setModelUsed(e.getModelUsed());
        a.setErrorMessage(e.getErrorMessage());
        a.setGeneratedAt(e.getGeneratedAt());
        return a;
    }

    private String writeJson(List<Recommendation> list) {
        try {
            return objectMapper.writeValueAsString(list == null ? List.of() : list);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize recommendations", ex);
        }
    }

    private List<Recommendation> readJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, RECOMMENDATION_LIST);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot deserialize recommendations: " + json, ex);
        }
    }
}
