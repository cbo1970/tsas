package com.cas.tsas.ai.application.service;

import com.cas.tsas.ai.application.port.out.LlmClientPort;
import com.cas.tsas.ai.application.port.out.LoadMatchAnalysisPort;
import com.cas.tsas.ai.application.port.out.SaveMatchAnalysisPort;
import com.cas.tsas.ai.infrastructure.llm.FakeLlmClientAdapter;
import com.cas.tsas.match.application.port.in.GetMatchUseCase;
import com.cas.tsas.match.domain.exception.MatchNotFoundException;
import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.statistics.application.port.in.ComputeMatchStatisticsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Regression tests for TEN-55: documents that {@link MatchAnalysisService} does not implement its
 * own ownership filter — it loads the match through {@link GetMatchUseCase#findById(UUID)} and
 * therefore inherits the owner-binding enforced by the owner-filtered {@code LoadMatchPort}
 * (Task 12). A cross-tenant call surfaces as {@link MatchNotFoundException} (HTTP 404) before any
 * stats or LLM work happens, and no FAILED analysis is persisted.
 */
class MatchAnalysisServiceOwnershipTest {

    private GetMatchUseCase getMatchUseCase;
    private LoadPlayerPort loadPlayerPort;
    private ComputeMatchStatisticsUseCase statisticsUseCase;
    private SaveMatchAnalysisPort savePort;
    private LoadMatchAnalysisPort loadPort;
    private MatchAnalysisService service;

    @BeforeEach
    void setUp() {
        getMatchUseCase = Mockito.mock(GetMatchUseCase.class);
        loadPlayerPort = Mockito.mock(LoadPlayerPort.class);
        statisticsUseCase = Mockito.mock(ComputeMatchStatisticsUseCase.class);
        LlmClientPort llm = new FakeLlmClientAdapter();
        savePort = Mockito.mock(SaveMatchAnalysisPort.class);
        loadPort = Mockito.mock(LoadMatchAnalysisPort.class);
        service = new MatchAnalysisService(getMatchUseCase, loadPlayerPort,
                statisticsUseCase, llm, savePort, loadPort, 10);
    }

    @Test
    void generate_propagatesCrossTenantNotFoundFromGetMatchUseCase() {
        UUID foreignMatchId = UUID.randomUUID();
        when(getMatchUseCase.findById(foreignMatchId))
                .thenThrow(new MatchNotFoundException(foreignMatchId));

        assertThatThrownBy(() -> service.generate(foreignMatchId))
                .isInstanceOf(MatchNotFoundException.class)
                .hasMessageContaining(foreignMatchId.toString());

        // Cross-tenant access must short-circuit before stats/LLM/persistence are touched.
        Mockito.verifyNoInteractions(statisticsUseCase, loadPlayerPort, savePort);
    }
}
