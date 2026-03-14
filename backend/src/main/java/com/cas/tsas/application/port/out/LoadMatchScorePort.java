package com.cas.tsas.application.port.out;

import com.cas.tsas.domain.model.MatchScore;

import java.util.Optional;
import java.util.UUID;

public interface LoadMatchScorePort {

    Optional<MatchScore> loadMatchScore(UUID matchId);
}
