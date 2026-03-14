package com.cas.tsas.infrastructure.persistence.repository;

import com.cas.tsas.infrastructure.persistence.entity.MatchScoreJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MatchScoreJpaRepository extends JpaRepository<MatchScoreJpaEntity, UUID> {

    Optional<MatchScoreJpaEntity> findByMatchId(UUID matchId);
}
