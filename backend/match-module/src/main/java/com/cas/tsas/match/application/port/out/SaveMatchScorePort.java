package com.cas.tsas.match.application.port.out;

import com.cas.tsas.match.domain.model.MatchScore;

public interface SaveMatchScorePort {

    MatchScore saveMatchScore(MatchScore matchScore);
}
