/**
 * Auth module — integration with Keycloak.
 *
 * <p>Configures the application as an OAuth2 resource server: JWT validation against the
 * Keycloak realm, securing all {@code /api/**} endpoints while leaving the actuator
 * health/info endpoints open. The {@code test} profile is intentionally excluded so
 * integration tests can run without a live Keycloak. Contains security infrastructure
 * plus a small {@code domain} subpackage with the authenticated-user value object
 * ({@link com.cas.tsas.auth.domain.CurrentUser}) and role enum
 * ({@link com.cas.tsas.auth.domain.Role}); no persistence.
 */
package com.cas.tsas.auth;
