package com.cas.tsas.player.infrastructure.persistence.repository;

import com.cas.tsas.player.application.port.out.DeletePlayerPort;
import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.player.application.port.out.SavePlayerPort;
import com.cas.tsas.player.domain.model.Player;
import com.cas.tsas.player.infrastructure.persistence.mapper.PlayerMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;



/**
 * Persistence adapter implementing the player output ports (load, save,
 * delete), mapping between {@link Player} and its JPA entity via
 * {@link PlayerMapper}.
 */
@Component
public class PlayerPersistenceAdapter implements LoadPlayerPort, SavePlayerPort, DeletePlayerPort {

    private final PlayerJpaRepository repository;
    private final PlayerMapper mapper;

    public PlayerPersistenceAdapter(PlayerJpaRepository repository, PlayerMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Player> loadPlayer(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Player> loadAllPlayers() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Player> findByIdAndOwner(UUID id, UUID ownerId) {
        return repository.findByIdAndOwnerId(id, ownerId).map(mapper::toDomain);
    }

    @Override
    public List<Player> findAllByOwner(UUID ownerId) {
        return repository.findAllByOwnerId(ownerId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Player savePlayer(Player player) {
        var entity = mapper.toEntity(player);
        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void deletePlayer(UUID id) {
        repository.deleteById(id);
    }
}
