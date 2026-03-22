package com.cas.tsas.match.infrastructure.persistence.repository;

import com.cas.tsas.match.application.port.out.LoadMatchScorePort;
import com.cas.tsas.match.application.port.out.SaveMatchScorePort;
import com.cas.tsas.match.domain.model.MatchScore;
import com.cas.tsas.match.infrastructure.persistence.mapper.MatchScoreMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class MatchScorePersistenceAdapter implements LoadMatchScorePort, SaveMatchScorePort {

    private final MatchScoreJpaRepository repository;
    private final MatchScoreMapper mapper;

    public MatchScorePersistenceAdapter(MatchScoreJpaRepository repository, MatchScoreMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<MatchScore> loadMatchScore(UUID matchId) {
        return repository.findByMatchId(matchId).map(mapper::toDomain);
    }

    @Override
    public MatchScore saveMatchScore(MatchScore matchScore) {
        var entity = mapper.toEntity(matchScore);
        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
}
