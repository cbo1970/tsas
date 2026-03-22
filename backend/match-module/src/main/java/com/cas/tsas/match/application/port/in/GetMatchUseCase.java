package com.cas.tsas.match.application.port.in;

import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchScore;

import java.util.List;
import java.util.UUID;

public interface GetMatchUseCase {

    Match findById(UUID id);

    List<Match> findAll();

    MatchScore getScore(UUID matchId);
}
