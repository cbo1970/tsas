package com.cas.tsas.match.application.port.out;

import com.cas.tsas.match.domain.model.MatchScore;

import java.util.Optional;
import java.util.UUID;

public interface LoadMatchScorePort {

    Optional<MatchScore> loadMatchScore(UUID matchId);
}
