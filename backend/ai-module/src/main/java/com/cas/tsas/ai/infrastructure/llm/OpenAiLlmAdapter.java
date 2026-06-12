package com.cas.tsas.ai.infrastructure.llm;

import com.cas.tsas.ai.application.dto.MatchAnalysisResult;
import com.cas.tsas.ai.application.dto.MatchMetadata;
import com.cas.tsas.ai.application.port.out.LlmClientPort;
import com.cas.tsas.statistics.domain.model.MatchStatistics;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * {@link LlmClientPort} implementation backed by Spring AI's {@link ChatClient} against OpenAI.
 *
 * <p>Only registered when an OpenAI API key is configured (see {@code @ConditionalOnExpression});
 * otherwise the {@link FakeLlmClientAdapter} acts as fallback.
 */
@Component
@ConditionalOnExpression("!'${spring.ai.openai.api-key:}'.isEmpty()")
public class OpenAiLlmAdapter implements LlmClientPort {

    private final ChatClient chatClient;
    private final PromptBuilder promptBuilder;
    private final String modelName;

    public OpenAiLlmAdapter(ChatClient.Builder chatClientBuilder,
                            PromptBuilder promptBuilder,
                            @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String modelName) {
        this.chatClient = chatClientBuilder.build();
        this.promptBuilder = promptBuilder;
        this.modelName = modelName;
    }

    @Override
    public MatchAnalysisResult generateAnalysis(MatchStatistics stats, MatchMetadata meta) {
        return chatClient.prompt()
                .system(promptBuilder.systemPrompt())
                .user(promptBuilder.userPrompt(stats, meta))
                .call()
                .entity(MatchAnalysisResult.class);
    }

    @Override
    public String modelName() {
        return modelName;
    }
}
