package com.cas.tsas.match.infrastructure.persistence.mapper;

import com.cas.tsas.match.domain.model.Point;
import com.cas.tsas.match.infrastructure.persistence.entity.PointJpaEntity;
import org.springframework.stereotype.Component;

/** Maps between {@link Point} domain objects and {@link PointJpaEntity}. */
@Component
public class PointMapper {

    public PointJpaEntity toEntity(Point point) {
        PointJpaEntity entity = new PointJpaEntity();
        entity.setId(point.getId());
        entity.setMatchId(point.getMatchId());
        entity.setSetNumber(point.getSetNumber());
        entity.setGameNumber(point.getGameNumber());
        entity.setPointNumber(point.getPointNumber());
        entity.setWinner(point.getWinner());
        entity.setPointType(point.getPointType());
        entity.setStrokeType(point.getStrokeType());
        entity.setDirection(point.getDirection());
        entity.setServingPlayer(point.getServingPlayer());
        entity.setBreakPoint(point.isBreakPoint());
        entity.setRemark(point.getRemark());
        entity.setServeAttempt(point.getServeAttempt());
        return entity;
    }

    public Point toDomain(PointJpaEntity entity) {
        return new Point(
                entity.getId(),
                entity.getMatchId(),
                entity.getSetNumber(),
                entity.getGameNumber(),
                entity.getPointNumber(),
                entity.getWinner(),
                entity.getPointType(),
                entity.getStrokeType(),
                entity.getDirection(),
                entity.getServingPlayer(),
                entity.isBreakPoint(),
                entity.getRemark(),
                entity.getServeAttempt()
        );
    }
}
