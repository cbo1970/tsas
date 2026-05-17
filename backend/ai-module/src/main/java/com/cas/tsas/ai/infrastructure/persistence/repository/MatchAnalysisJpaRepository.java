package com.cas.tsas.ai.infrastructure.persistence.repository;

import com.cas.tsas.ai.infrastructure.persistence.entity.MatchAnalysisJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MatchAnalysisJpaRepository extends JpaRepository<MatchAnalysisJpaEntity, UUID> {
    Optional<MatchAnalysisJpaEntity> findByMatchId(UUID matchId);
}
