package com.cas.tsas.match.application.port.out;

import com.cas.tsas.match.domain.model.Point;

public interface SavePointPort {

    Point savePoint(Point point);
}
