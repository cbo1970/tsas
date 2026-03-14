package com.cas.tsas.application.port.in.match;

import com.cas.tsas.domain.model.Match;

import java.util.UUID;

public interface EndMatchUseCase {

    Match endMatch(UUID matchId);
}
