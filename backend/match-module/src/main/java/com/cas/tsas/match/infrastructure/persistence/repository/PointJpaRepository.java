package com.cas.tsas.match.infrastructure.persistence.repository;

import com.cas.tsas.match.infrastructure.persistence.entity.PointJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PointJpaRepository extends JpaRepository<PointJpaEntity, UUID> {
    int countByMatchIdAndSetNumberAndGameNumber(UUID matchId, int setNumber, int gameNumber);
}
