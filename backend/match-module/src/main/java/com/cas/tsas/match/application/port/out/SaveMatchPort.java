package com.cas.tsas.match.application.port.out;

import com.cas.tsas.match.domain.model.Match;

public interface SaveMatchPort {

    Match saveMatch(Match match);
}
