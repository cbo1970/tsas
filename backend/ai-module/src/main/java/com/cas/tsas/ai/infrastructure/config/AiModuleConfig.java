package com.cas.tsas.ai.infrastructure.config;

import com.cas.tsas.ai.application.port.out.LlmClientPort;
import com.cas.tsas.ai.infrastructure.llm.FakeLlmClientAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiModuleConfig {

    /**
     * Registriert den Fake-Adapter, wenn kein anderer {@link LlmClientPort} im Kontext ist.
     * Sobald TEN-22 den {@code OpenAiLlmAdapter} als {@code @Component} hinzufügt, übernimmt der;
     * in Tests und in Builds ohne OpenAI-Stack bleibt der Fake als sichere Fallback-Implementierung.
     */
    @Bean
    @ConditionalOnMissingBean(LlmClientPort.class)
    LlmClientPort fakeLlmClientAdapter() {
        return new FakeLlmClientAdapter();
    }
}
