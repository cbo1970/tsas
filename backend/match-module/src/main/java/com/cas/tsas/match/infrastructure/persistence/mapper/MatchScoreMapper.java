package com.cas.tsas.match.infrastructure.persistence.mapper;

import com.cas.tsas.match.domain.model.MatchScore;
import com.cas.tsas.match.infrastructure.persistence.entity.MatchScoreJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class MatchScoreMapper {

    public MatchScore toDomain(MatchScoreJpaEntity entity) {
        return new MatchScore(
                entity.getId(),
                entity.getMatchId(),
                entity.getPointsPlayer1(),
                entity.getPointsPlayer2(),
                entity.getGamesPlayer1(),
                entity.getGamesPlayer2(),
                entity.getSetsPlayer1(),
                entity.getSetsPlayer2(),
                entity.isDeuce(),
                entity.getIsAdvantagePlayer1(),
                entity.getCurrentSet(),
                entity.isDone(),
                entity.getWinner(),
                entity.getAcesPlayer1(),
                entity.getAcesPlayer2()
        );
    }

    public MatchScoreJpaEntity toEntity(MatchScore score) {
        MatchScoreJpaEntity entity = new MatchScoreJpaEntity();
        entity.setId(score.getId());
        entity.setMatchId(score.getMatchId());
        entity.setPointsPlayer1(score.getPointsPlayer1());
        entity.setPointsPlayer2(score.getPointsPlayer2());
        entity.setGamesPlayer1(score.getGamesPlayer1());
        entity.setGamesPlayer2(score.getGamesPlayer2());
        entity.setSetsPlayer1(score.getSetsPlayer1());
        entity.setSetsPlayer2(score.getSetsPlayer2());
        entity.setDeuce(score.isDeuce());
        entity.setIsAdvantagePlayer1(score.getIsAdvantagePlayer1());
        entity.setCurrentSet(score.getCurrentSet());
        entity.setDone(score.isDone());
        entity.setWinner(score.getWinner());
        entity.setAcesPlayer1(score.getAcesPlayer1());
        entity.setAcesPlayer2(score.getAcesPlayer2());
        return entity;
    }
}
