package com.cas.tsas.common.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** CORS settings bound from {@code tsas.cors}, notably the allowed frontend origins. */
@ConfigurationProperties(prefix = "tsas.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
