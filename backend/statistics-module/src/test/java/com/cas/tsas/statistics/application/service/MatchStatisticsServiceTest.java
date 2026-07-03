package com.cas.tsas.statistics.application.service;

import com.cas.tsas.match.application.port.out.LoadPointsByMatchPort;
import com.cas.tsas.match.domain.model.Direction;
import com.cas.tsas.match.domain.model.Point;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.match.domain.model.StrokeType;
import com.cas.tsas.statistics.domain.model.MatchStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MatchStatisticsServiceTest {

    private LoadPointsByMatchPort loadPort;
    private MatchStatisticsService service;
    private UUID matchId;

    @BeforeEach
    void setUp() {
        loadPort = Mockito.mock(LoadPointsByMatchPort.class);
        service = new MatchStatisticsService(loadPort);
        matchId = UUID.randomUUID();
    }

    private Point p(int set, int game, int num, int winner, PointType type,
                    StrokeType stroke, Direction dir, Integer server,
                    boolean bp, Integer serveAttempt) {
        return new Point(UUID.randomUUID(), matchId, set, game, num, winner,
                type, stroke, dir, server, bp, null, serveAttempt);
    }

    @Test
    void emptyMatchProducesZeroStatistics() {
        Mockito.when(loadPort.loadPointsByMatch(matchId)).thenReturn(List.of());

        MatchStatistics s = service.compute(matchId);

        assertThat(s.totalPoints()).isZero();
        assertThat(s.player1().pointsWon()).isZero();
        assertThat(s.player2().pointsWon()).isZero();
        assertThat(s.breakPointsTotal()).isZero();
    }

    @Test
    void totalPointsAndPointsWonPerPlayer() {
        Mockito.when(loadPort.loadPointsByMatch(matchId)).thenReturn(List.of(
                p(1,1,1,1,PointType.WINNER,StrokeType.FOREHAND,Direction.CROSS_COURT,1,false,1),
                p(1,1,2,2,PointType.UNFORCED_ERROR,StrokeType.BACKHAND,Direction.MIDDLE,1,false,1),
                p(1,1,3,1,PointType.ACE,null,null,1,false,1)
        ));

        MatchStatistics s = service.compute(matchId);

        assertThat(s.totalPoints()).isEqualTo(3);
        assertThat(s.player1().pointsWon()).isEqualTo(2);
        assertThat(s.player2().pointsWon()).isEqualTo(1);
    }

    @Test
    void winnersAndUnforcedErrorsByAttribution() {
        Mockito.when(loadPort.loadPointsByMatch(matchId)).thenReturn(List.of(
                p(1,1,1,1,PointType.WINNER,StrokeType.FOREHAND,Direction.CROSS_COURT,1,false,1),
                p(1,1,2,1,PointType.UNFORCED_ERROR,StrokeType.BACKHAND,Direction.MIDDLE,1,false,1)
        ));

        MatchStatistics s = service.compute(matchId);

        assertThat(s.player1().winners()).isEqualTo(1);
        assertThat(s.player2().unforcedErrors()).isEqualTo(1);
        assertThat(s.player1().unforcedErrors()).isZero();
    }

    @Test
    void acesAndDoubleFaultsByServer() {
        Mockito.when(loadPort.loadPointsByMatch(matchId)).thenReturn(List.of(
                p(1,1,1,1,PointType.ACE,null,null,1,false,1),
                p(1,2,1,2,PointType.DOUBLE_FAULT,null,null,1,false,2)
        ));

        MatchStatistics s = service.compute(matchId);

        assertThat(s.player1().aces()).isEqualTo(1);
        assertThat(s.player1().doubleFaults()).isEqualTo(1);
        assertThat(s.player2().aces()).isZero();
    }

    @Test
    void firstServePercentageFromServeAttemptField() {
        // Player 1 serves all four: 3 first-serve points (serveAttempt=1) and 1 second-serve (serveAttempt=2)
        Mockito.when(loadPort.loadPointsByMatch(matchId)).thenReturn(List.of(
                p(1,1,1,1,PointType.WINNER,StrokeType.SERVE,Direction.CROSS_COURT,1,false,1),
                p(1,1,2,1,PointType.WINNER,StrokeType.SERVE,Direction.CROSS_COURT,1,false,1),
                p(1,1,3,2,PointType.UNFORCED_ERROR,StrokeType.FOREHAND,Direction.MIDDLE,1,false,2),
                p(1,1,4,1,PointType.WINNER,StrokeType.SERVE,Direction.CROSS_COURT,1,false,1)
        ));

        MatchStatistics s = service.compute(matchId);

        assertThat(s.player1().firstServePercentage()).isCloseTo(0.75, within(0.001));
    }

    @Test
    void secondServePercentageExcludesDoubleFaults() {
        // 2 second serves: one in (UE on FH), one DF
        Mockito.when(loadPort.loadPointsByMatch(matchId)).thenReturn(List.of(
                p(1,1,1,2,PointType.UNFORCED_ERROR,StrokeType.FOREHAND,Direction.MIDDLE,1,false,2),
                p(1,2,1,2,PointType.DOUBLE_FAULT,null,null,1,false,2)
        ));

        MatchStatistics s = service.compute(matchId);

        assertThat(s.player1().secondServePercentage()).isCloseTo(0.5, within(0.001));
    }

    @Test
    void breakPointsCounting() {
        Mockito.when(loadPort.loadPointsByMatch(matchId)).thenReturn(List.of(
                p(1,1,1,2,PointType.UNFORCED_ERROR,StrokeType.FOREHAND,Direction.CROSS_COURT,1,true,1),
                p(1,2,1,1,PointType.WINNER,StrokeType.FOREHAND,Direction.CROSS_COURT,1,true,1)
        ));

        MatchStatistics s = service.compute(matchId);

        assertThat(s.player1().breakPointsFaced()).isEqualTo(2);
        assertThat(s.player2().breakPointsWon()).isEqualTo(1);
        assertThat(s.breakPointsTotal()).isEqualTo(2);
    }

    @Test
    void strokeAndDirectionDistribution() {
        // p3: winner=2, UE → attribuiert auf Player 1 (Verlierer); sein FH-Fehler zählt bei ihm
        Mockito.when(loadPort.loadPointsByMatch(matchId)).thenReturn(List.of(
                p(1,1,1,1,PointType.WINNER,StrokeType.FOREHAND,Direction.CROSS_COURT,1,false,1),
                p(1,1,2,1,PointType.WINNER,StrokeType.BACKHAND,Direction.DOWN_THE_LINE,1,false,1),
                p(1,1,3,2,PointType.UNFORCED_ERROR,StrokeType.FOREHAND,Direction.MIDDLE,1,false,1)
        ));

        MatchStatistics s = service.compute(matchId);

        assertThat(s.player1().strokeDistribution().counts().get(StrokeType.FOREHAND)).isEqualTo(2);
        assertThat(s.player1().strokeDistribution().counts().get(StrokeType.BACKHAND)).isEqualTo(1);
        assertThat(s.player2().strokeDistribution().counts()).isEmpty();
        assertThat(s.player1().directionDistribution().counts().get(Direction.MIDDLE)).isEqualTo(1);
    }

    @Test
    void zeroServeAttemptsResultsInZeroPercentages() {
        // No points with serveAttempt set
        Mockito.when(loadPort.loadPointsByMatch(matchId)).thenReturn(List.of(
                p(1,1,1,1,PointType.WINNER,StrokeType.FOREHAND,Direction.CROSS_COURT,null,false,null)
        ));

        MatchStatistics s = service.compute(matchId);

        assertThat(s.player1().firstServePercentage()).isZero();
        assertThat(s.player1().secondServePercentage()).isZero();
    }

    @Test
    void nullPointTypeIsSkippedForAttribution() {
        // Quick-point ohne pointType (TEN-33): nur pointsWon zählen, kein Absturz
        Mockito.when(loadPort.loadPointsByMatch(matchId)).thenReturn(List.of(
                p(1,1,1,1,null,StrokeType.FOREHAND,Direction.CROSS_COURT,1,false,null),
                p(1,1,2,2,null,null,null,null,false,null)
        ));

        MatchStatistics s = service.compute(matchId);

        assertThat(s.totalPoints()).isEqualTo(2);
        assertThat(s.player1().pointsWon()).isEqualTo(1);
        assertThat(s.player2().pointsWon()).isEqualTo(1);
        assertThat(s.player1().winners()).isZero();
        assertThat(s.player2().winners()).isZero();
    }

    @Test
    void breakdownProducesTotalAndPerSet() {
        Mockito.when(loadPort.loadPointsByMatch(matchId)).thenReturn(List.of(
                // Set 1: P1 winner + P1 ace
                p(1,1,1,1,PointType.WINNER,StrokeType.FOREHAND,Direction.CROSS_COURT,1,false,1),
                p(1,1,2,1,PointType.ACE,null,null,1,false,1),
                // Set 2: P2 winner
                p(2,1,1,2,PointType.WINNER,StrokeType.BACKHAND,Direction.MIDDLE,2,false,1)
        ));

        var b = service.computeBreakdown(matchId);

        assertThat(b.total().totalPoints()).isEqualTo(3);
        assertThat(b.sets()).extracting(com.cas.tsas.statistics.domain.model.SetStatistics::setNumber)
                .containsExactly(1, 2);

        var set1 = b.sets().get(0).stats();
        assertThat(set1.totalPoints()).isEqualTo(2);
        assertThat(set1.player1().winners()).isEqualTo(1);
        assertThat(set1.player1().aces()).isEqualTo(1);

        var set2 = b.sets().get(1).stats();
        assertThat(set2.totalPoints()).isEqualTo(1);
        assertThat(set2.player2().winners()).isEqualTo(1);
    }

    @Test
    void breakdownOfEmptyMatchHasNoSets() {
        Mockito.when(loadPort.loadPointsByMatch(matchId)).thenReturn(List.of());
        var b = service.computeBreakdown(matchId);
        assertThat(b.total().totalPoints()).isZero();
        assertThat(b.sets()).isEmpty();
    }
}
