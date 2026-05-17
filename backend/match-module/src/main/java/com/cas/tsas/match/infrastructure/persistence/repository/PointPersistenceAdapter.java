package com.cas.tsas.match.infrastructure.persistence.repository;

import com.cas.tsas.match.application.port.out.CountPointsInGamePort;
import com.cas.tsas.match.application.port.out.LoadPointsByMatchPort;
import com.cas.tsas.match.application.port.out.SavePointPort;
import com.cas.tsas.match.domain.model.Point;
import com.cas.tsas.match.infrastructure.persistence.mapper.PointMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class PointPersistenceAdapter implements SavePointPort, CountPointsInGamePort, LoadPointsByMatchPort {

    private final PointJpaRepository pointJpaRepository;
    private final PointMapper pointMapper;

    public PointPersistenceAdapter(PointJpaRepository pointJpaRepository, PointMapper pointMapper) {
        this.pointJpaRepository = pointJpaRepository;
        this.pointMapper = pointMapper;
    }

    @Override
    public Point savePoint(Point point) {
        var entity = pointJpaRepository.save(pointMapper.toEntity(point));
        return pointMapper.toDomain(entity);
    }

    @Override
    public int countPointsInGame(UUID matchId, int setNumber, int gameNumber) {
        return pointJpaRepository.countByMatchIdAndSetNumberAndGameNumber(matchId, setNumber, gameNumber);
    }

    @Override
    public List<Point> loadPointsByMatch(UUID matchId) {
        return pointJpaRepository
                .findAllByMatchIdOrderBySetNumberAscGameNumberAscPointNumberAsc(matchId)
                .stream()
                .map(pointMapper::toDomain)
                .toList();
    }
}
