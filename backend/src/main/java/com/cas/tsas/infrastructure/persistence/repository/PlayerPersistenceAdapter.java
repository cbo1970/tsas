package com.cas.tsas.infrastructure.persistence.repository;

import com.cas.tsas.application.port.out.LoadPlayerPort;
import com.cas.tsas.application.port.out.SavePlayerPort;
import com.cas.tsas.domain.model.Player;
import com.cas.tsas.infrastructure.persistence.mapper.PlayerMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PlayerPersistenceAdapter implements LoadPlayerPort, SavePlayerPort {

    private final PlayerJpaRepository repository;
    private final PlayerMapper mapper;

    public PlayerPersistenceAdapter(PlayerJpaRepository repository, PlayerMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Player> loadPlayer(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Player> loadAllPlayers() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Player savePlayer(Player player) {
        var entity = mapper.toEntity(player);
        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
}
