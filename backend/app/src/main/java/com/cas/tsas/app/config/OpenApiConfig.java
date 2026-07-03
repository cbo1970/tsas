package com.cas.tsas.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI tsasOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TSaS API")
                        .version("v1")
                        .description("Tennis Score and Statistic — REST API für Spieler-, Match-, Punkt- und Statistik-Verwaltung sowie KI-Match-Analyse.")
                        .license(new License().name("Internal")))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("OAuth2 Access Token (Keycloak realm tsas)")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
