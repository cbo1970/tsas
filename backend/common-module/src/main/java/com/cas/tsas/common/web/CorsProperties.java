package com.cas.tsas.common.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "tsas.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
