package com.cas.tsas.match.application.port.out;

import com.cas.tsas.match.domain.model.Match;

import java.util.List;
import java.util.UUID;

/** Loads every match played between two players, regardless of who was player 1 or 2. */
public interface LoadMatchesByPlayersPort {

    List<Match> loadMatchesBetween(UUID playerA, UUID playerB);
}
