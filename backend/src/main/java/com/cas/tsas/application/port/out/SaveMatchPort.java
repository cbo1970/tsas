package com.cas.tsas.application.port.out;

import com.cas.tsas.domain.model.Match;

public interface SaveMatchPort {

    Match saveMatch(Match match);
}
