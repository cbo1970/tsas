package com.cas.tsas.match.application.port.out;

import com.cas.tsas.match.domain.model.Point;

import java.util.List;
import java.util.UUID;

public interface LoadPointsByMatchPort {
    List<Point> loadPointsByMatch(UUID matchId);
}
