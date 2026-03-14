package com.cas.tsas.application.port.in.match;

import com.cas.tsas.domain.model.Match;
import com.cas.tsas.domain.model.MatchScore;

import java.util.List;
import java.util.UUID;

public interface GetMatchUseCase {

    Match findById(UUID id);

    List<Match> findAll();

    MatchScore getScore(UUID matchId);
}
