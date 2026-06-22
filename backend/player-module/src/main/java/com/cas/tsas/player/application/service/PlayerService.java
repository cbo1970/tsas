package com.cas.tsas.player.application.service;

import com.cas.tsas.auth.application.port.in.CurrentUserProvider;
import com.cas.tsas.auth.domain.CurrentUser;
import com.cas.tsas.auth.domain.Role;
import com.cas.tsas.player.application.port.in.CreatePlayerUseCase;
import com.cas.tsas.player.application.port.in.DeletePlayerUseCase;
import com.cas.tsas.player.application.port.in.SearchPlayerUseCase;
import com.cas.tsas.player.application.port.in.UpdatePlayerUseCase;
import com.cas.tsas.player.application.port.out.DeletePlayerPort;
import com.cas.tsas.player.application.port.out.FindActiveMatchPort;
import com.cas.tsas.player.application.port.out.HasMatchesPort;
import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.player.application.port.out.SavePlayerPort;
import com.cas.tsas.player.domain.exception.PlayerHasMatchesException;
import com.cas.tsas.player.domain.exception.PlayerNotFoundException;
import com.cas.tsas.player.domain.model.Player;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Implements the player use cases (create, search, update, delete). Reads and
 * writes are scoped to the current user as owner; users with the {@code ADMIN}
 * role bypass the owner filter. Deletion is guarded against players that
 * participate in matches, in which case deactivation (soft delete) is offered
 * instead. Match-related lookups are delegated to ports implemented by the
 * match module.
 */
@Service
@Transactional
public class PlayerService implements CreatePlayerUseCase, SearchPlayerUseCase, UpdatePlayerUseCase, DeletePlayerUseCase {

    private final LoadPlayerPort loadPlayerPort;
    private final SavePlayerPort savePlayerPort;
    private final DeletePlayerPort deletePlayerPort;
    private final HasMatchesPort hasMatchesPort;
    private final FindActiveMatchPort findActiveMatchPort;
    private final CurrentUserProvider currentUserProvider;

    public PlayerService(LoadPlayerPort loadPlayerPort, SavePlayerPort savePlayerPort,
                         DeletePlayerPort deletePlayerPort, HasMatchesPort hasMatchesPort,
                         FindActiveMatchPort findActiveMatchPort,
                         CurrentUserProvider currentUserProvider) {
        this.loadPlayerPort = loadPlayerPort;
        this.savePlayerPort = savePlayerPort;
        this.deletePlayerPort = deletePlayerPort;
        this.hasMatchesPort = hasMatchesPort;
        this.findActiveMatchPort = findActiveMatchPort;
        this.currentUserProvider = currentUserProvider;
    }

    private CurrentUser currentUser() {
        return currentUserProvider.get();
    }

    private boolean isAdmin() {
        return currentUser().hasRole(Role.ADMIN);
    }

    @Override
    public Player createPlayer(CreatePlayerCommand command) {
        Player player = new Player(
                null,
                currentUser().id(),
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
        Optional<Player> player = isAdmin()
                ? loadPlayerPort.loadPlayer(id)
                : loadPlayerPort.findByIdAndOwner(id, currentUser().id());
        return player.orElseThrow(() -> new PlayerNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Player> findAll() {
        return isAdmin()
                ? loadPlayerPort.loadAllPlayers()
                : loadPlayerPort.findAllByOwner(currentUser().id());
    }

    /**
     * Returns, for each of the given players that has one, the id of their
     * currently active match (players without an active match are omitted).
     */
    @Override
    @Transactional(readOnly = true)
    public Map<UUID, UUID> findActiveMatchIdsByPlayerIds(Set<UUID> playerIds) {
        return findActiveMatchPort.findActiveMatchIdsByPlayerIds(playerIds);
    }

    @Override
    public Player updatePlayer(UpdatePlayerCommand command) {
        Player player = findById(command.id());
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
        return hasMatchesPort.existsByPlayerId(id);
    }

    /**
     * Hard-deletes a player. Refused when the player is referenced by any match
     * (to preserve match history); use {@link #deactivatePlayer(UUID)} instead.
     *
     * @throws PlayerHasMatchesException if the player participates in any match
     * @throws PlayerNotFoundException if no such player exists (or is not owned
     *                                 by the current non-admin user)
     */
    @Override
    public void deletePlayer(UUID id) {
        Player player = findById(id);
        if (hasMatchesPort.existsByPlayerId(id)) {
            throw new PlayerHasMatchesException(id);
        }
        deletePlayerPort.deletePlayer(player.getId());
    }

    /**
     * Soft-deletes a player by setting {@code active = false}, keeping the
     * record (and its match history) intact.
     *
     * @throws PlayerNotFoundException if no such player exists (or is not owned
     *                                 by the current non-admin user)
     */
    @Override
    public void deactivatePlayer(UUID id) {
        Player player = findById(id);
        player.setActive(false);
        savePlayerPort.savePlayer(player);
    }
}
