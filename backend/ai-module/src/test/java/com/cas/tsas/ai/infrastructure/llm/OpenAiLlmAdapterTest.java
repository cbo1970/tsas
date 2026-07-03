package com.cas.tsas.ai.infrastructure.llm;

import com.cas.tsas.ai.application.dto.MatchAnalysisResult;
import com.cas.tsas.ai.application.dto.MatchMetadata;
import com.cas.tsas.statistics.domain.model.DirectionDistribution;
import com.cas.tsas.statistics.domain.model.MatchStatistics;
import com.cas.tsas.statistics.domain.model.PlayerStatistics;
import com.cas.tsas.statistics.domain.model.StrokeDistribution;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.cas.tsas.ai.infrastructure.config.PromptProperties;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OpenAiLlmAdapterTest.TestApp.class,
        properties = {
                "spring.ai.openai.api-key=test-key",
                "spring.ai.openai.chat.options.model=gpt-4o-mini",
                "spring.main.web-application-type=none",
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
                        "org.springframework.boot.orm.jpa.autoconfigure.HibernateJpaAutoConfiguration"
        })
class OpenAiLlmAdapterTest {

    static WireMockServer wm = new WireMockServer(WireMockConfiguration.options()
            .dynamicPort()
            .notifier(new com.github.tomakehurst.wiremock.common.ConsoleNotifier(true)));

    @BeforeAll static void start() { wm.start(); }
    @AfterAll  static void stop()  { wm.stop(); }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.ai.openai.base-url", () -> "http://localhost:" + wm.port());
    }

    @Autowired OpenAiLlmAdapter adapter;

    @Test
    void parsesStructuredResponse() {
        wm.stubFor(post(urlPathEqualTo("/chat/completions"))
                .willReturn(okJson("""
                    {
                      "id":"x","object":"chat.completion","created":1,"model":"gpt-4o-mini",
                      "choices":[{"index":0,"finish_reason":"stop","message":{"role":"assistant",
                        "content":"{\\"keyMoments\\":\\"k\\",\\"ownStrengths\\":\\"os\\",\\"ownWeaknesses\\":\\"ow\\",\\"opponentStrengths\\":\\"ps\\",\\"opponentWeaknesses\\":\\"pw\\",\\"recommendations\\":[{\\"priority\\":1,\\"title\\":\\"t\\",\\"detail\\":\\"d\\"}]}"
                      }}],
                      "usage":{"prompt_tokens":1,"completion_tokens":1,"total_tokens":2}
                    }""")));

        MatchAnalysisResult result = adapter.generateAnalysis(stats(), metadata());

        assertThat(result.keyMoments()).isEqualTo("k");
        assertThat(result.ownStrengths()).isEqualTo("os");
        assertThat(result.recommendations()).hasSize(1);
        assertThat(result.recommendations().get(0).title()).isEqualTo("t");
    }

    @Test
    void modelNameReflectsConfiguration() {
        assertThat(adapter.modelName()).isEqualTo("gpt-4o-mini");
    }

    private static MatchStatistics stats() {
        PlayerStatistics ps = new PlayerStatistics(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                new StrokeDistribution(Map.of()), new DirectionDistribution(Map.of()));
        return new MatchStatistics(UUID.randomUUID(), ps,
                new PlayerStatistics(2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                        new StrokeDistribution(Map.of()), new DirectionDistribution(Map.of())),
                0, 0, Instant.now());
    }

    private static MatchMetadata metadata() {
        return new MatchMetadata(
                new MatchMetadata.PlayerInfo("A", "R1", "RIGHT", "TWO_HANDED"),
                new MatchMetadata.PlayerInfo("B", "R2", "LEFT", "ONE_HANDED"),
                2, false, false);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableConfigurationProperties(PromptProperties.class)
    @ComponentScan(basePackages = "com.cas.tsas.ai.infrastructure.llm")
    static class TestApp {}
}
