package com.cas.tsas.ai.infrastructure.web;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(AnalysisRateLimitProperties.class)
class AiWebMvcConfig implements WebMvcConfigurer {

    private final AnalysisRateLimitInterceptor rateLimitInterceptor;

    AiWebMvcConfig(AnalysisRateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns(
                        "/api/matches/*/analysis",            // FA-11 — Postmortem (TEN-15)
                        "/api/players/*/opponent-preparation/*" // FA-20 — KI-Vorbereitung (TEN-51)
                );
    }
}
