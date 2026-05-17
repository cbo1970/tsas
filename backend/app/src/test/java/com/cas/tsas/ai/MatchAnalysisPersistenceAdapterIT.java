package com.cas.tsas.ai;

import com.cas.tsas.ai.domain.model.AnalysisStatus;
import com.cas.tsas.ai.domain.model.MatchAnalysis;
import com.cas.tsas.ai.domain.model.Recommendation;
import com.cas.tsas.ai.infrastructure.persistence.adapter.MatchAnalysisPersistenceAdapter;
import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchPersistenceAdapter;
import com.cas.tsas.player.infrastructure.persistence.entity.PlayerJpaEntity;
import com.cas.tsas.player.infrastructure.persistence.repository.PlayerJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Testcontainers
class MatchAnalysisPersistenceAdapterIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired MatchAnalysisPersistenceAdapter adapter;
    @Autowired MatchPersistenceAdapter matchAdapter;
    @Autowired PlayerJpaRepository playerJpaRepository;

    private UUID matchId;

    @BeforeEach
    void setUp() {
        PlayerJpaEntity p1 = new PlayerJpaEntity();
        p1.setFirstName("A"); p1.setLastName("B");
        UUID p1Id = playerJpaRepository.save(p1).getId();

        PlayerJpaEntity p2 = new PlayerJpaEntity();
        p2.setFirstName("C"); p2.setLastName("D");
        UUID p2Id = playerJpaRepository.save(p2).getId();

        matchId = matchAdapter.saveMatch(
                new Match(null, p1Id, p2Id, 2, false, false, MatchStatus.COMPLETED)).getId();
    }

    @Test
    void saveAndLoadRoundTrip() {
        MatchAnalysis a = new MatchAnalysis();
        a.setMatchId(matchId);
        a.setStatus(AnalysisStatus.COMPLETED);
        a.setKeyMoments("Schlüsselmomente …");
        a.setOwnStrengths("Stärken eigen");
        a.setOwnWeaknesses("Schwächen eigen");
        a.setOpponentStrengths("Stärken Gegner");
        a.setOpponentWeaknesses("Schwächen Gegner");
        a.setRecommendations(List.of(
                new Recommendation(1, "Aufschlag", "Mehr 1. Aufschläge spielen"),
                new Recommendation(2, "Rückhand", "Cross spielen")));
        a.setModelUsed("gpt-4o-mini");
        a.setGeneratedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS));

        adapter.save(a);

        var loaded = adapter.loadByMatchId(matchId).orElseThrow();
        assertThat(loaded.getStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(loaded.getKeyMoments()).isEqualTo("Schlüsselmomente …");
        assertThat(loaded.getOwnStrengths()).isEqualTo("Stärken eigen");
        assertThat(loaded.getRecommendations()).hasSize(2);
        assertThat(loaded.getRecommendations().get(0).title()).isEqualTo("Aufschlag");
        assertThat(loaded.getRecommendations().get(1).priority()).isEqualTo(2);
        assertThat(loaded.getModelUsed()).isEqualTo("gpt-4o-mini");
    }

    @Test
    void loadByMatchId_returnsEmptyWhenNoAnalysisExists() {
        assertThat(adapter.loadByMatchId(matchId)).isEmpty();
    }

    @Test
    void save_overwritesExistingAnalysisForSameMatch() {
        MatchAnalysis first = new MatchAnalysis();
        first.setMatchId(matchId);
        first.setStatus(AnalysisStatus.COMPLETED);
        first.setKeyMoments("v1");
        first.setRecommendations(List.of(new Recommendation(1, "old", "old detail")));
        first.setModelUsed("gpt-4o-mini");
        first.setGeneratedAt(Instant.now());
        adapter.save(first);

        MatchAnalysis second = new MatchAnalysis();
        second.setMatchId(matchId);
        second.setStatus(AnalysisStatus.COMPLETED);
        second.setKeyMoments("v2");
        second.setRecommendations(List.of(new Recommendation(1, "new", "new detail")));
        second.setModelUsed("gpt-4o-mini");
        second.setGeneratedAt(Instant.now());
        adapter.save(second);

        var loaded = adapter.loadByMatchId(matchId).orElseThrow();
        assertThat(loaded.getKeyMoments()).isEqualTo("v2");
        assertThat(loaded.getRecommendations()).hasSize(1);
        assertThat(loaded.getRecommendations().get(0).title()).isEqualTo("new");
    }

    @Test
    void save_failedAnalysisWithErrorMessage() {
        MatchAnalysis failed = new MatchAnalysis();
        failed.setMatchId(matchId);
        failed.setStatus(AnalysisStatus.FAILED);
        failed.setErrorMessage("OpenAI timeout after 60s");
        failed.setModelUsed("gpt-4o-mini");
        failed.setGeneratedAt(Instant.now());

        adapter.save(failed);

        var loaded = adapter.loadByMatchId(matchId).orElseThrow();
        assertThat(loaded.getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(loaded.getErrorMessage()).contains("OpenAI timeout");
        assertThat(loaded.getRecommendations()).isEmpty();
    }
}
