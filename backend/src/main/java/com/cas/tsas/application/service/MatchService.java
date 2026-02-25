package com.cas.tsas.application.service;

import com.cas.tsas.application.port.in.match.CreateMatchUseCase;
import com.cas.tsas.application.port.in.match.RecordPointUseCase;
import com.cas.tsas.application.port.out.LoadMatchPort;
import com.cas.tsas.application.port.out.LoadPlayerPort;
import com.cas.tsas.application.port.out.SaveMatchPort;
import com.cas.tsas.domain.exception.MatchNotFoundException;
import com.cas.tsas.domain.exception.PlayerNotFoundException;
import com.cas.tsas.domain.model.Match;
import com.cas.tsas.domain.model.Player;
import com.cas.tsas.domain.model.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MatchService implements CreateMatchUseCase, RecordPointUseCase {

    private final LoadMatchPort loadMatchPort;
    private final SaveMatchPort saveMatchPort;
    private final LoadPlayerPort loadPlayerPort;

    public MatchService(LoadMatchPort loadMatchPort, SaveMatchPort saveMatchPort,
                        LoadPlayerPort loadPlayerPort) {
        this.loadMatchPort = loadMatchPort;
        this.saveMatchPort = saveMatchPort;
        this.loadPlayerPort = loadPlayerPort;
    }

    @Override
    public Match createMatch(CreateMatchCommand command) {
        Player ownPlayer = loadPlayerPort.loadPlayer(command.ownPlayerId())
                .orElseThrow(() -> new PlayerNotFoundException(command.ownPlayerId()));
        Player opponent = loadPlayerPort.loadPlayer(command.opponentId())
                .orElseThrow(() -> new PlayerNotFoundException(command.opponentId()));

        Match match = new Match(
                null,
                ownPlayer,
                opponent,
                command.date(),
                command.setsToWin(),
                command.matchTieBreak(),
                command.shortSet()
        );
        return saveMatchPort.saveMatch(match);
    }

    @Override
    public Point recordPoint(RecordPointCommand command) {
        Match match = loadMatchPort.loadMatch(command.matchId())
                .orElseThrow(() -> new MatchNotFoundException(command.matchId()));

        Point point = new Point(
                null,
                match.getId(),
                command.ownAttribute(),
                command.opponentAttribute(),
                command.remark()
        );
        match.getPoints().add(point);
        saveMatchPort.saveMatch(match);
        return point;
    }
}
