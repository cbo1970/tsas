/**
 * Auth module — integration with Keycloak.
 *
 * <p>Configures the application as an OAuth2 resource server: JWT validation against the
 * Keycloak realm, securing all {@code /api/**} endpoints while leaving the actuator
 * health/info endpoints open. The {@code test} profile is intentionally excluded so
 * integration tests can run without a live Keycloak. Contains only infrastructure
 * (security configuration); no domain or persistence.
 */
package com.cas.tsas.auth;
