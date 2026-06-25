package com.cas.tsas.ai.application.service;

import com.cas.tsas.ai.application.port.out.LlmClientPort;
import com.cas.tsas.ai.application.port.out.LoadMatchAnalysisPort;
import com.cas.tsas.ai.application.port.out.SaveMatchAnalysisPort;
import com.cas.tsas.ai.application.port.out.UserLanguagePort;
import com.cas.tsas.ai.domain.exception.AnalysisNotReviewableException;
import com.cas.tsas.ai.domain.exception.MatchAnalysisNotFoundException;
import com.cas.tsas.ai.domain.exception.RecommendationNotFoundException;
import com.cas.tsas.ai.domain.model.AnalysisStatus;
import com.cas.tsas.ai.domain.model.MatchAnalysis;
import com.cas.tsas.ai.domain.model.Recommendation;
import com.cas.tsas.ai.domain.model.RecommendationStatus;
import com.cas.tsas.match.application.port.in.GetMatchUseCase;
import com.cas.tsas.match.domain.exception.MatchNotFoundException;
import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.statistics.application.port.in.ComputeMatchStatisticsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewRecommendationServiceTest {

    @Mock GetMatchUseCase getMatchUseCase;
    @Mock LoadPlayerPort loadPlayerPort;
    @Mock ComputeMatchStatisticsUseCase statisticsUseCase;
    @Mock LlmClientPort llmClient;
    @Mock SaveMatchAnalysisPort savePort;
    @Mock LoadMatchAnalysisPort loadPort;
    @Mock UserLanguagePort userLanguagePort;

    private MatchAnalysisService service;
    private final UUID matchId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new MatchAnalysisService(getMatchUseCase, loadPlayerPort, statisticsUseCase,
                llmClient, savePort, loadPort, userLanguagePort, 10);
    }

    private MatchAnalysis completedWith(Recommendation... recs) {
        MatchAnalysis a = new MatchAnalysis();
        a.setMatchId(matchId);
        a.setStatus(AnalysisStatus.COMPLETED);
        a.setRecommendations(new ArrayList<>(List.of(recs)));
        return a;
    }

    @Test
    void review_setsStatusNoteAndTimestamp_andPersists() {
        when(getMatchUseCase.findById(matchId)).thenReturn(org.mockito.Mockito.mock(Match.class));
        when(loadPort.loadByMatchId(matchId))
                .thenReturn(Optional.of(completedWith(new Recommendation(1, "t", "d"))));
        when(savePort.save(any(MatchAnalysis.class))).thenAnswer(inv -> inv.getArgument(0));

        MatchAnalysis result = service.review(matchId, 0, RecommendationStatus.ACCEPTED, "gut");

        Recommendation r = result.getRecommendations().get(0);
        assertThat(r.status()).isEqualTo(RecommendationStatus.ACCEPTED);
        assertThat(r.reviewNote()).isEqualTo("gut");
        assertThat(r.reviewedAt()).isNotNull();
    }

    @Test
    void review_propagatesCrossTenantNotFound() {
        when(getMatchUseCase.findById(matchId)).thenThrow(new MatchNotFoundException(matchId));
        assertThatThrownBy(() -> service.review(matchId, 0, RecommendationStatus.ACCEPTED, null))
                .isInstanceOf(MatchNotFoundException.class);
    }

    @Test
    void review_throwsWhenNoAnalysis() {
        when(getMatchUseCase.findById(matchId)).thenReturn(org.mockito.Mockito.mock(Match.class));
        when(loadPort.loadByMatchId(matchId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.review(matchId, 0, RecommendationStatus.ACCEPTED, null))
                .isInstanceOf(MatchAnalysisNotFoundException.class);
    }

    @Test
    void review_throwsWhenNotCompleted() {
        when(getMatchUseCase.findById(matchId)).thenReturn(org.mockito.Mockito.mock(Match.class));
        MatchAnalysis pending = new MatchAnalysis();
        pending.setMatchId(matchId);
        pending.setStatus(AnalysisStatus.FAILED);
        pending.setRecommendations(new ArrayList<>());
        when(loadPort.loadByMatchId(matchId)).thenReturn(Optional.of(pending));
        assertThatThrownBy(() -> service.review(matchId, 0, RecommendationStatus.ACCEPTED, null))
                .isInstanceOf(AnalysisNotReviewableException.class);
    }

    @Test
    void review_throwsWhenIndexOutOfRange() {
        when(getMatchUseCase.findById(matchId)).thenReturn(org.mockito.Mockito.mock(Match.class));
        when(loadPort.loadByMatchId(matchId))
                .thenReturn(Optional.of(completedWith(new Recommendation(1, "t", "d"))));
        assertThatThrownBy(() -> service.review(matchId, 5, RecommendationStatus.ACCEPTED, null))
                .isInstanceOf(RecommendationNotFoundException.class);
    }
}
