package com.cas.tsas.application.service;

import com.cas.tsas.application.port.in.match.CreateMatchUseCase;
import com.cas.tsas.application.port.in.match.EndMatchUseCase;
import com.cas.tsas.application.port.in.match.GetMatchUseCase;
import com.cas.tsas.application.port.in.match.RecordPointUseCase;
import com.cas.tsas.application.port.in.match.SetScoreUseCase;
import com.cas.tsas.application.port.out.LoadMatchPort;
import com.cas.tsas.application.port.out.LoadMatchScorePort;
import com.cas.tsas.application.port.out.LoadPlayerPort;
import com.cas.tsas.application.port.out.SaveMatchPort;
import com.cas.tsas.application.port.out.SaveMatchScorePort;
import com.cas.tsas.domain.exception.MatchNotFoundException;
import com.cas.tsas.domain.exception.PlayerNotFoundException;
import com.cas.tsas.domain.model.Match;
import com.cas.tsas.domain.model.MatchScore;
import com.cas.tsas.domain.model.MatchStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MatchService implements CreateMatchUseCase, GetMatchUseCase, RecordPointUseCase,
        SetScoreUseCase, EndMatchUseCase {

    private final LoadMatchPort loadMatchPort;
    private final SaveMatchPort saveMatchPort;
    private final LoadPlayerPort loadPlayerPort;
    private final LoadMatchScorePort loadMatchScorePort;
    private final SaveMatchScorePort saveMatchScorePort;
    private final ScoringService scoringService;

    public MatchService(LoadMatchPort loadMatchPort, SaveMatchPort saveMatchPort,
                        LoadPlayerPort loadPlayerPort,
                        LoadMatchScorePort loadMatchScorePort,
                        SaveMatchScorePort saveMatchScorePort,
                        ScoringService scoringService) {
        this.loadMatchPort = loadMatchPort;
        this.saveMatchPort = saveMatchPort;
        this.loadPlayerPort = loadPlayerPort;
        this.loadMatchScorePort = loadMatchScorePort;
        this.saveMatchScorePort = saveMatchScorePort;
        this.scoringService = scoringService;
    }

    @Override
    public Match createMatch(CreateMatchCommand command) {
        loadPlayerPort.loadPlayer(command.player1Id())
                .orElseThrow(() -> new PlayerNotFoundException(command.player1Id()));
        loadPlayerPort.loadPlayer(command.player2Id())
                .orElseThrow(() -> new PlayerNotFoundException(command.player2Id()));

        Match match = new Match(
                null,
                command.player1Id(),
                command.player2Id(),
                command.setsToWin(),
                command.matchTiebreak(),
                command.shortSet(),
                MatchStatus.IN_PROGRESS
        );
        Match saved = saveMatchPort.saveMatch(match);

        // Create initial score
        MatchScore score = new MatchScore(
                null, saved.getId(),
                0, 0,
                0, 0,
                0, 0,
                false, null,
                1, false, null
        );
        saveMatchScorePort.saveMatchScore(score);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Match findById(UUID id) {
        return loadMatchPort.loadMatch(id)
                .orElseThrow(() -> new MatchNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Match> findAll() {
        return loadMatchPort.loadAllMatches();
    }

    @Override
    @Transactional(readOnly = true)
    public MatchScore getScore(UUID matchId) {
        loadMatchPort.loadMatch(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));
        return loadMatchScorePort.loadMatchScore(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));
    }

    @Override
    public MatchScore recordPoint(RecordPointCommand command) {
        Match match = loadMatchPort.loadMatch(command.matchId())
                .orElseThrow(() -> new MatchNotFoundException(command.matchId()));

        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new IllegalStateException("Match is already completed");
        }

        MatchScore score = loadMatchScorePort.loadMatchScore(command.matchId())
                .orElseThrow(() -> new MatchNotFoundException(command.matchId()));

        scoringService.applyPoint(match, score, command.player1Scored());

        MatchScore saved = saveMatchScorePort.saveMatchScore(score);

        if (saved.isDone()) {
            match.setStatus(MatchStatus.COMPLETED);
            saveMatchPort.saveMatch(match);
        }

        return saved;
    }

    @Override
    public MatchScore setScore(SetScoreCommand command) {
        Match match = loadMatchPort.loadMatch(command.matchId())
                .orElseThrow(() -> new MatchNotFoundException(command.matchId()));

        MatchScore score = loadMatchScorePort.loadMatchScore(command.matchId())
                .orElseThrow(() -> new MatchNotFoundException(command.matchId()));

        score.setPointsPlayer1(command.pointsPlayer1());
        score.setPointsPlayer2(command.pointsPlayer2());
        score.setGamesPlayer1(command.gamesPlayer1());
        score.setGamesPlayer2(command.gamesPlayer2());
        score.setSetsPlayer1(command.setsPlayer1());
        score.setSetsPlayer2(command.setsPlayer2());
        score.setDeuce(command.isDeuce());
        score.setIsAdvantagePlayer1(command.isAdvantagePlayer1());
        score.setCurrentSet(command.currentSet());
        score.setDone(command.isDone());
        score.setWinner(command.winner());

        MatchScore saved = saveMatchScorePort.saveMatchScore(score);

        if (saved.isDone() && match.getStatus() != MatchStatus.COMPLETED) {
            match.setStatus(MatchStatus.COMPLETED);
            saveMatchPort.saveMatch(match);
        } else if (!saved.isDone() && match.getStatus() == MatchStatus.COMPLETED) {
            match.setStatus(MatchStatus.IN_PROGRESS);
            saveMatchPort.saveMatch(match);
        }

        return saved;
    }

    @Override
    public Match endMatch(UUID matchId) {
        Match match = loadMatchPort.loadMatch(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));

        match.setStatus(MatchStatus.COMPLETED);
        Match saved = saveMatchPort.saveMatch(match);

        // Update score to done if not already
        loadMatchScorePort.loadMatchScore(matchId).ifPresent(score -> {
            if (!score.isDone()) {
                score.setDone(true);
                // Determine winner based on sets
                if (score.getSetsPlayer1() > score.getSetsPlayer2()) {
                    score.setWinner("PLAYER1");
                } else if (score.getSetsPlayer2() > score.getSetsPlayer1()) {
                    score.setWinner("PLAYER2");
                }
                saveMatchScorePort.saveMatchScore(score);
            }
        });

        return saved;
    }
}
