package com.cas.tsas.application.port.out;

import com.cas.tsas.domain.model.Match;

import java.util.List;
import java.util.Optional;

public interface LoadMatchPort {

    Optional<Match> loadMatch(Long id);

    List<Match> loadAllMatches();
}
