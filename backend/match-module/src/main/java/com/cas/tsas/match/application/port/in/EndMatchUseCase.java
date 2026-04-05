package com.cas.tsas.match.application.port.in;

import com.cas.tsas.match.domain.model.Match;

import java.util.UUID;

public interface EndMatchUseCase {

    Match endMatch(UUID matchId);
}
