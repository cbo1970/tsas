package com.cas.tsas.match.application.port.out;

import com.cas.tsas.match.domain.model.Match;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadMatchPort {

    Optional<Match> loadMatch(UUID id);

    List<Match> loadAllMatches();

    boolean existsActiveMatchForPlayer(UUID playerId);
}
