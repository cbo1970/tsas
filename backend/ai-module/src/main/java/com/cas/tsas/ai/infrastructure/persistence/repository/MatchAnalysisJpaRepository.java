package com.cas.tsas.ai.infrastructure.persistence.repository;

import com.cas.tsas.ai.infrastructure.persistence.entity.MatchAnalysisJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchAnalysisJpaRepository extends JpaRepository<MatchAnalysisJpaEntity, UUID> {
    Optional<MatchAnalysisJpaEntity> findByMatchId(UUID matchId);

    /** Used by the DSGVO Art. 20 export endpoint (TEN-66) to gather all analyses of a user. */
    List<MatchAnalysisJpaEntity> findAllByMatchIdIn(Collection<UUID> matchIds);
}
