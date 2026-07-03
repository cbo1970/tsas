package com.cas.tsas.statistics.application.service;

import com.cas.tsas.match.application.port.out.LoadPointsByMatchPort;
import com.cas.tsas.match.domain.exception.MatchNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regression tests for TEN-55: documents that {@link MatchStatisticsService} does not have its
 * own ownership filter — it simply consumes the match aggregate via the in-port. The cross-tenant
 * 404 enforcement lives in {@code GetMatchUseCase.findById} (called by
 * {@code MatchStatisticsController}) and in the owner-filtered {@code LoadMatchPort}.
 *
 * <p>If a future refactor were to swallow or remap the {@link MatchNotFoundException} thrown by
 * any underlying port, these tests would catch it.
 */
class MatchStatisticsServiceOwnershipTest {

    private LoadPointsByMatchPort loadPointsByMatchPort;
    private MatchStatisticsService service;

    @BeforeEach
    void setUp() {
        loadPointsByMatchPort = Mockito.mock(LoadPointsByMatchPort.class);
        service = new MatchStatisticsService(loadPointsByMatchPort);
    }

    @Test
    void compute_propagatesCrossTenantNotFoundFromLoadPointsPort() {
        UUID foreignMatchId = UUID.randomUUID();
        Mockito.when(loadPointsByMatchPort.loadPointsByMatch(foreignMatchId))
                .thenThrow(new MatchNotFoundException(foreignMatchId));

        assertThatThrownBy(() -> service.compute(foreignMatchId))
                .isInstanceOf(MatchNotFoundException.class)
                .hasMessageContaining(foreignMatchId.toString());
    }
}
