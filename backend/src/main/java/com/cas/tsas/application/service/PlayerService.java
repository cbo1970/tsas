package com.cas.tsas.application.service;

import com.cas.tsas.application.port.in.player.CreatePlayerUseCase;
import com.cas.tsas.application.port.in.player.SearchPlayerUseCase;
import com.cas.tsas.application.port.out.LoadPlayerPort;
import com.cas.tsas.application.port.out.SavePlayerPort;
import com.cas.tsas.domain.exception.PlayerNotFoundException;
import com.cas.tsas.domain.model.Player;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PlayerService implements CreatePlayerUseCase, SearchPlayerUseCase {

    private final LoadPlayerPort loadPlayerPort;
    private final SavePlayerPort savePlayerPort;

    public PlayerService(LoadPlayerPort loadPlayerPort, SavePlayerPort savePlayerPort) {
        this.loadPlayerPort = loadPlayerPort;
        this.savePlayerPort = savePlayerPort;
    }

    @Override
    public Player createPlayer(CreatePlayerCommand command) {
        Player player = new Player(
                null,
                command.firstName(),
                command.lastName(),
                command.gender(),
                command.handedness(),
                command.backhandType(),
                command.ranking(),
                command.nationality(),
                command.birthDate()
        );
        return savePlayerPort.savePlayer(player);
    }

    @Override
    @Transactional(readOnly = true)
    public Player findById(UUID id) {
        return loadPlayerPort.loadPlayer(id)
                .orElseThrow(() -> new PlayerNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Player> findAll() {
        return loadPlayerPort.loadAllPlayers();
    }
}
