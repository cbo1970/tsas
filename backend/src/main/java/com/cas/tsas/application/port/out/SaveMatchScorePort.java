package com.cas.tsas.application.port.out;

import com.cas.tsas.domain.model.MatchScore;

public interface SaveMatchScorePort {

    MatchScore saveMatchScore(MatchScore matchScore);
}
