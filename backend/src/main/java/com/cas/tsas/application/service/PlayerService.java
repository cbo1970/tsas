package com.cas.tsas.application.service;

import com.cas.tsas.application.port.in.player.CreatePlayerUseCase;
import com.cas.tsas.application.port.in.player.DeletePlayerUseCase;
import com.cas.tsas.application.port.in.player.SearchPlayerUseCase;
import com.cas.tsas.application.port.in.player.UpdatePlayerUseCase;
import com.cas.tsas.application.port.out.DeletePlayerPort;
import com.cas.tsas.application.port.out.LoadMatchPort;
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
public class PlayerService implements CreatePlayerUseCase, SearchPlayerUseCase, UpdatePlayerUseCase, DeletePlayerUseCase {

    private final LoadPlayerPort loadPlayerPort;
    private final SavePlayerPort savePlayerPort;
    private final DeletePlayerPort deletePlayerPort;
    private final LoadMatchPort loadMatchPort;

    public PlayerService(LoadPlayerPort loadPlayerPort, SavePlayerPort savePlayerPort,
                         DeletePlayerPort deletePlayerPort, LoadMatchPort loadMatchPort) {
        this.loadPlayerPort = loadPlayerPort;
        this.savePlayerPort = savePlayerPort;
        this.deletePlayerPort = deletePlayerPort;
        this.loadMatchPort = loadMatchPort;
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

    @Override
    public Player updatePlayer(UpdatePlayerCommand command) {
        Player player = loadPlayerPort.loadPlayer(command.id())
                .orElseThrow(() -> new PlayerNotFoundException(command.id()));
        player.setFirstName(command.firstName());
        player.setLastName(command.lastName());
        player.setGender(command.gender());
        player.setHandedness(command.handedness());
        player.setBackhandType(command.backhandType());
        player.setRanking(command.ranking());
        player.setNationality(command.nationality());
        player.setBirthDate(command.birthDate());
        return savePlayerPort.savePlayer(player);
    }

    @Override
    public boolean hasMatches(UUID id) {
        return loadMatchPort.existsByPlayerId(id);
    }

    @Override
    public void deletePlayer(UUID id) {
        if (loadMatchPort.existsByPlayerId(id)) {
            throw new IllegalStateException("Spieler hat Matches und kann nicht gelöscht werden.");
        }
        loadPlayerPort.loadPlayer(id).orElseThrow(() -> new PlayerNotFoundException(id));
        deletePlayerPort.deletePlayer(id);
    }

    @Override
    public void deactivatePlayer(UUID id) {
        Player player = loadPlayerPort.loadPlayer(id)
                .orElseThrow(() -> new PlayerNotFoundException(id));
        player.setActive(false);
        savePlayerPort.savePlayer(player);
    }
}
