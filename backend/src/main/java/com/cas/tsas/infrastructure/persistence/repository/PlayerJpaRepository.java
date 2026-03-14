package com.cas.tsas.infrastructure.persistence.repository;

import com.cas.tsas.infrastructure.persistence.entity.PlayerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlayerJpaRepository extends JpaRepository<PlayerJpaEntity, UUID> {
}
