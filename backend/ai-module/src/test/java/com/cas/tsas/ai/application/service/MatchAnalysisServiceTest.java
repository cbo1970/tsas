package com.cas.tsas.ai.application.service;

import com.cas.tsas.ai.application.port.out.LlmClientPort;
import com.cas.tsas.ai.application.port.out.LoadMatchAnalysisPort;
import com.cas.tsas.ai.application.port.out.SaveMatchAnalysisPort;
import com.cas.tsas.ai.domain.exception.AnalysisGenerationException;
import com.cas.tsas.ai.domain.exception.InsufficientMatchDataException;
import com.cas.tsas.ai.domain.exception.MatchNotCompletedException;
import com.cas.tsas.ai.domain.model.AnalysisStatus;
import com.cas.tsas.ai.domain.model.MatchAnalysis;
import com.cas.tsas.ai.infrastructure.llm.FakeLlmClientAdapter;
import com.cas.tsas.match.application.port.in.GetMatchUseCase;
import com.cas.tsas.match.application.port.in.GetPlayerNotesUseCase;
import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.player.domain.model.BackhandType;
import com.cas.tsas.player.domain.model.Gender;
import com.cas.tsas.player.domain.model.Handedness;
import com.cas.tsas.player.domain.model.Player;
import com.cas.tsas.statistics.application.port.in.ComputeMatchStatisticsUseCase;
import com.cas.tsas.statistics.domain.model.DirectionDistribution;
import com.cas.tsas.statistics.domain.model.MatchStatistics;
import com.cas.tsas.statistics.domain.model.PlayerStatistics;
import com.cas.tsas.statistics.domain.model.StrokeDistribution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class MatchAnalysisServiceTest {

    private GetMatchUseCase getMatchUseCase;
    private GetPlayerNotesUseCase getPlayerNotesUseCase;
    private LoadPlayerPort loadPlayerPort;
    private ComputeMatchStatisticsUseCase statisticsUseCase;
    private LlmClientPort llm;
    private SaveMatchAnalysisPort savePort;
    private LoadMatchAnalysisPort loadPort;
    private MatchAnalysisService service;

    private UUID matchId, p1Id, p2Id;

    @BeforeEach
    void setUp() {
        getMatchUseCase = Mockito.mock(GetMatchUseCase.class);
        getPlayerNotesUseCase = Mockito.mock(GetPlayerNotesUseCase.class);
        when(getPlayerNotesUseCase.forMatch(any())).thenReturn(List.of());
        loadPlayerPort = Mockito.mock(LoadPlayerPort.class);
        statisticsUseCase = Mockito.mock(ComputeMatchStatisticsUseCase.class);
        llm = new FakeLlmClientAdapter();
        savePort = Mockito.mock(SaveMatchAnalysisPort.class);
        loadPort = Mockito.mock(LoadMatchAnalysisPort.class);
        service = new MatchAnalysisService(getMatchUseCase, getPlayerNotesUseCase, loadPlayerPort,
                statisticsUseCase, llm, savePort, loadPort, () -> "de", 10);

        matchId = UUID.randomUUID();
        p1Id = UUID.randomUUID();
        p2Id = UUID.randomUUID();

        when(savePort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Match completedMatch() {
        return new Match(matchId, UUID.randomUUID(), p1Id, p2Id, 2, false, false, MatchStatus.COMPLETED);
    }

    private Player player(UUID id, String first, String last) {
        return new Player(id, UUID.randomUUID(), first, last, Gender.MALE, Handedness.RIGHT,
                BackhandType.TWO_HANDED, "N3", "GER", null);
    }

    private MatchStatistics stats(int total) {
        PlayerStatistics p1 = new PlayerStatistics(1, total / 2, 5, 4, 1, 2, 1, 0.6, 0.5,
                1, 3, new StrokeDistribution(Map.of()), new DirectionDistribution(Map.of()));
        PlayerStatistics p2 = new PlayerStatistics(2, total - total / 2, 4, 5, 1, 1, 2, 0.55, 0.5,
                2, 4, new StrokeDistribution(Map.of()), new DirectionDistribution(Map.of()));
        return new MatchStatistics(matchId, p1, p2, total, 7, Instant.now());
    }

    @Test
    void generate_succeedsAndPersistsCompletedAnalysis() {
        when(getMatchUseCase.findById(matchId)).thenReturn(completedMatch());
        when(loadPlayerPort.loadPlayer(p1Id)).thenReturn(Optional.of(player(p1Id, "Max", "Müller")));
        when(loadPlayerPort.loadPlayer(p2Id)).thenReturn(Optional.of(player(p2Id, "Tom", "Schmidt")));
        when(statisticsUseCase.compute(matchId)).thenReturn(stats(50));

        MatchAnalysis a = service.generate(matchId);

        assertThat(a.getStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(a.getMatchId()).isEqualTo(matchId);
        assertThat(a.getKeyMoments()).contains("50");
        assertThat(a.getRecommendations()).hasSize(2);
        assertThat(a.getModelUsed()).isEqualTo("fake-llm");
        assertThat(a.getGeneratedAt()).isNotNull();
        Mockito.verify(savePort).save(any());
    }

    @Test
    void generate_throwsWhenMatchNotCompleted() {
        Match inProgress = new Match(matchId, UUID.randomUUID(), p1Id, p2Id, 2, false, false, MatchStatus.IN_PROGRESS);
        when(getMatchUseCase.findById(matchId)).thenReturn(inProgress);

        assertThatThrownBy(() -> service.generate(matchId))
                .isInstanceOf(MatchNotCompletedException.class)
                .hasMessageContaining("not COMPLETED");

        Mockito.verifyNoInteractions(savePort);
    }

    @Test
    void generate_throwsWhenTooFewPoints() {
        when(getMatchUseCase.findById(matchId)).thenReturn(completedMatch());
        when(loadPlayerPort.loadPlayer(p1Id)).thenReturn(Optional.of(player(p1Id, "A", "B")));
        when(loadPlayerPort.loadPlayer(p2Id)).thenReturn(Optional.of(player(p2Id, "C", "D")));
        when(statisticsUseCase.compute(matchId)).thenReturn(stats(5));

        assertThatThrownBy(() -> service.generate(matchId))
                .isInstanceOf(InsufficientMatchDataException.class)
                .hasMessageContaining("at least 10 points");

        Mockito.verifyNoInteractions(savePort);
    }

    @Test
    void generate_persistsFailedAnalysisAndRethrowsOnLlmError() {
        when(getMatchUseCase.findById(matchId)).thenReturn(completedMatch());
        when(loadPlayerPort.loadPlayer(p1Id)).thenReturn(Optional.of(player(p1Id, "A", "B")));
        when(loadPlayerPort.loadPlayer(p2Id)).thenReturn(Optional.of(player(p2Id, "C", "D")));
        when(statisticsUseCase.compute(matchId)).thenReturn(stats(50));

        LlmClientPort failing = Mockito.mock(LlmClientPort.class);
        when(failing.modelName()).thenReturn("failing-llm");
        when(failing.generateAnalysis(any(), any(), any())).thenThrow(new RuntimeException("boom"));

        MatchAnalysisService failingService = new MatchAnalysisService(getMatchUseCase,
                getPlayerNotesUseCase, loadPlayerPort, statisticsUseCase, failing, savePort, loadPort,
                () -> "de", 10);

        assertThatThrownBy(() -> failingService.generate(matchId))
                .isInstanceOf(AnalysisGenerationException.class)
                .hasMessageContaining(matchId.toString());

        Mockito.verify(savePort).save(Mockito.argThat(a ->
                a.getStatus() == AnalysisStatus.FAILED
                        && a.getErrorMessage() != null
                        && a.getErrorMessage().contains("boom")
                        && "failing-llm".equals(a.getModelUsed())));
    }

    @Test
    void findByMatchId_delegatesToPort() {
        MatchAnalysis a = new MatchAnalysis();
        a.setMatchId(matchId);
        when(loadPort.loadByMatchId(matchId)).thenReturn(Optional.of(a));

        assertThat(service.findByMatchId(matchId)).contains(a);
    }
}
