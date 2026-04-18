package com.cas.tsas.match.infrastructure.persistence.repository;

import com.cas.tsas.match.application.port.out.LoadMatchPort;
import com.cas.tsas.match.application.port.out.SaveMatchPort;
import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.match.infrastructure.persistence.entity.MatchJpaEntity;
import com.cas.tsas.match.infrastructure.persistence.mapper.MatchMapper;
import com.cas.tsas.player.application.port.out.FindActiveMatchPort;
import com.cas.tsas.player.application.port.out.HasMatchesPort;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class MatchPersistenceAdapter implements LoadMatchPort, SaveMatchPort, HasMatchesPort, FindActiveMatchPort {

    private final MatchJpaRepository repository;
    private final MatchMapper mapper;

    public MatchPersistenceAdapter(MatchJpaRepository repository, MatchMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Match> loadMatch(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Match> loadAllMatches() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByPlayerId(UUID playerId) {
        return repository.existsByPlayer1IdOrPlayer2Id(playerId, playerId);
    }

    @Override
    public Map<UUID, UUID> findActiveMatchIdsByPlayerIds(Set<UUID> playerIds) {
        // JPQL IN clause with empty collection throws at runtime in most JPA providers
        if (playerIds.isEmpty()) return Map.of();
        List<MatchJpaEntity> matches = repository.findByStatusAndPlayerIdIn(MatchStatus.IN_PROGRESS, playerIds);
        Map<UUID, UUID> result = new HashMap<>();
        for (MatchJpaEntity m : matches) {
            if (playerIds.contains(m.getPlayer1Id())) result.put(m.getPlayer1Id(), m.getId());
            if (playerIds.contains(m.getPlayer2Id())) result.put(m.getPlayer2Id(), m.getId());
        }
        return result;
    }

    @Override
    public Match saveMatch(Match match) {
        var entity = mapper.toEntity(match);
        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
}
