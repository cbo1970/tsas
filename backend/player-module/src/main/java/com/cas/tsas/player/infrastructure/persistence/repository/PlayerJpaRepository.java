package com.cas.tsas.player.infrastructure.persistence.repository;

import com.cas.tsas.player.infrastructure.persistence.entity.PlayerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlayerJpaRepository extends JpaRepository<PlayerJpaEntity, UUID> {
}
