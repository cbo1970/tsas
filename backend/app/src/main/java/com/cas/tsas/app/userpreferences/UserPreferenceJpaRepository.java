package com.cas.tsas.app.userpreferences;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserPreferenceJpaRepository extends JpaRepository<UserPreferenceJpaEntity, UUID> {
}
