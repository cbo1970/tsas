package com.cas.tsas.infrastructure.persistence.repository;

import com.cas.tsas.infrastructure.persistence.entity.MatchJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchJpaRepository extends JpaRepository<MatchJpaEntity, Long> {
}
