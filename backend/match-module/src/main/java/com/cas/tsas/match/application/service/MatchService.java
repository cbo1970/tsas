package com.cas.tsas.match.application.service;

import com.cas.tsas.match.application.port.in.CreateMatchUseCase;
import com.cas.tsas.match.application.port.in.EndMatchUseCase;
import com.cas.tsas.match.application.port.in.GetMatchUseCase;
import com.cas.tsas.match.application.port.in.RecordPointUseCase;
import com.cas.tsas.match.application.port.in.SetScoreUseCase;
import com.cas.tsas.match.application.port.in.SetServingPlayerUseCase;
import com.cas.tsas.match.application.port.out.CountPointsInGamePort;
import com.cas.tsas.match.application.port.out.LoadMatchPort;
import com.cas.tsas.match.application.port.out.LoadMatchScorePort;
import com.cas.tsas.match.application.port.out.SaveMatchPort;
import com.cas.tsas.match.application.port.out.SaveMatchScorePort;
import com.cas.tsas.match.application.port.out.SavePointPort;
import com.cas.tsas.match.domain.exception.MatchNotFoundException;
import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchScore;
import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.match.domain.model.Point;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.player.domain.exception.PlayerNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MatchService implements CreateMatchUseCase, GetMatchUseCase, RecordPointUseCase,
        SetScoreUseCase, EndMatchUseCase, SetServingPlayerUseCase {

    private final LoadMatchPort loadMatchPort;
    private final SaveMatchPort saveMatchPort;
    private final LoadPlayerPort loadPlayerPort;
    private final LoadMatchScorePort loadMatchScorePort;
    private final SaveMatchScorePort saveMatchScorePort;
    private final ScoringService scoringService;
    private final SavePointPort savePointPort;
    private final CountPointsInGamePort countPointsInGamePort;

    public MatchService(LoadMatchPort loadMatchPort, SaveMatchPort saveMatchPort,
                        LoadPlayerPort loadPlayerPort,
                        LoadMatchScorePort loadMatchScorePort,
                        SaveMatchScorePort saveMatchScorePort,
                        ScoringService scoringService,
                        SavePointPort savePointPort,
                        CountPointsInGamePort countPointsInGamePort) {
        this.loadMatchPort = loadMatchPort;
        this.saveMatchPort = saveMatchPort;
        this.loadPlayerPort = loadPlayerPort;
        this.loadMatchScorePort = loadMatchScorePort;
        this.saveMatchScorePort = saveMatchScorePort;
        this.scoringService = scoringService;
        this.savePointPort = savePointPort;
        this.countPointsInGamePort = countPointsInGamePort;
    }

    @Override
    public Match createMatch(CreateMatchCommand command) {
        loadPlayerPort.loadPlayer(command.player1Id())
                .orElseThrow(() -> new PlayerNotFoundException(command.player1Id()));
        loadPlayerPort.loadPlayer(command.player2Id())
                .orElseThrow(() -> new PlayerNotFoundException(command.player2Id()));

        if (loadMatchPort.existsActiveMatchForPlayer(command.player1Id())) {
            throw new IllegalStateException("Player 1 already has an active match");
        }
        if (loadMatchPort.existsActiveMatchForPlayer(command.player2Id())) {
            throw new IllegalStateException("Player 2 already has an active match");
        }

        Match match = new Match(null, command.player1Id(), command.player2Id(),
                command.setsToWin(), command.matchTiebreak(), command.shortSet(),
                MatchStatus.IN_PROGRESS);
        Match saved = saveMatchPort.saveMatch(match);

        MatchScore score = new MatchScore(null, saved.getId(),
                0, 0, 0, 0, 0, 0, false, null, 1, false, null, 0, 0, null);
        saveMatchScorePort.saveMatchScore(score);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Match findById(UUID id) {
        return loadMatchPort.loadMatch(id).orElseThrow(() -> new MatchNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Match> findAll() {
        return loadMatchPort.loadAllMatches();
    }

    @Override
    @Transactional(readOnly = true)
    public MatchScore getScore(UUID matchId) {
        loadMatchPort.loadMatch(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
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

        boolean player1Scored = command.winner() == 1;
        int setNumber = score.getCurrentSet();
        int gameNumber = score.getGamesPlayer1() + score.getGamesPlayer2() + 1;
        int pointNumber = countPointsInGamePort.countPointsInGame(
                command.matchId(), setNumber, gameNumber) + 1;
        boolean isBreakPoint = calculateIsBreakPoint(score);

        savePointPort.savePoint(new Point(null, command.matchId(),
                setNumber, gameNumber, pointNumber,
                command.winner(), command.pointType(), command.strokeType(), command.direction(),
                score.getServingPlayer(), isBreakPoint, command.remark(), null));

        if (command.pointType() == PointType.ACE) {
            if (player1Scored) {
                score.setAcesPlayer1(score.getAcesPlayer1() + 1);
            } else {
                score.setAcesPlayer2(score.getAcesPlayer2() + 1);
            }
        }

        scoringService.applyPoint(match, score, player1Scored);
        MatchScore saved = saveMatchScorePort.saveMatchScore(score);

        if (saved.isDone()) {
            match.setStatus(MatchStatus.COMPLETED);
            saveMatchPort.saveMatch(match);
        }

        return saved;
    }

    private boolean calculateIsBreakPoint(MatchScore score) {
        Integer serving = score.getServingPlayer();
        if (serving == null) return false;

        boolean receiverIsP1 = serving == 2;
        int receiverPts = receiverIsP1 ? score.getPointsPlayer1() : score.getPointsPlayer2();
        int serverPts   = receiverIsP1 ? score.getPointsPlayer2() : score.getPointsPlayer1();

        if (receiverPts == 3 && serverPts < 3) return true;

        if (score.isDeuce() && score.getIsAdvantagePlayer1() != null) {
            return receiverIsP1 == score.getIsAdvantagePlayer1();
        }

        return false;
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
    public MatchScore setServingPlayer(SetServingPlayerCommand command) {
        Match match = loadMatchPort.loadMatch(command.matchId())
                .orElseThrow(() -> new MatchNotFoundException(command.matchId()));

        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new IllegalStateException("Match is already completed");
        }

        MatchScore score = loadMatchScorePort.loadMatchScore(command.matchId())
                .orElseThrow(() -> new MatchNotFoundException(command.matchId()));

        score.setServingPlayer(command.forPlayer1() ? 1 : 2);
        return saveMatchScorePort.saveMatchScore(score);
    }

    @Override
    public Match endMatch(UUID matchId) {
        Match match = loadMatchPort.loadMatch(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));

        match.setStatus(MatchStatus.COMPLETED);
        Match saved = saveMatchPort.saveMatch(match);

        loadMatchScorePort.loadMatchScore(matchId).ifPresent(score -> {
            if (!score.isDone()) {
                score.setDone(true);
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

    @Override
    public Match endMatchWalkover(UUID matchId, boolean player1Wins) {
        Match match = loadMatchPort.loadMatch(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));

        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new IllegalStateException("Match is already completed");
        }

        match.setStatus(MatchStatus.COMPLETED);
        Match saved = saveMatchPort.saveMatch(match);

        loadMatchScorePort.loadMatchScore(matchId).ifPresent(score -> {
            score.setDone(true);
            score.setWinner(player1Wins ? "PLAYER1" : "PLAYER2");
            saveMatchScorePort.saveMatchScore(score);
        });

        return saved;
    }
}
