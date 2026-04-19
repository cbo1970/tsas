# Keycloak Authentication & Authorization Design

> **Status:** Approved
> **Date:** 2026-04-19
> **Branch:** feature/keycloak-auth

---

## Goal

Activate authentication and authorization for the TSaS application using Keycloak as IDP, running as a Podman/Docker container. All `/api/**` endpoints are protected by JWT Bearer tokens. The Angular frontend uses `angular-oauth2-oidc` with Authorization Code + PKCE flow.

## Architecture

**Flow:**
1. User opens Angular app → `AuthGuard` checks for valid token
2. No token → redirect to Keycloak login page (Authorization Code + PKCE)
3. After login → Keycloak redirects back with authorization code → `angular-oauth2-oidc` exchanges for tokens
4. All API requests carry `Authorization: Bearer <access_token>` via HTTP interceptor
5. Spring Boot validates token signature against Keycloak JWKS endpoint

**Stack:** Keycloak 26, `angular-oauth2-oidc`, Spring Security OAuth2 Resource Server (JWT)

---

## Section 1: Keycloak / Compose

### `docker/compose.yml` — new service

```yaml
keycloak:
  image: quay.io/keycloak/keycloak:26
  command: start-dev --import-realm
  environment:
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: ${KC_ADMIN_PASSWORD:-admin}
  volumes:
    - ./keycloak:/opt/keycloak/data/import
  ports:
    - "8180:8080"
  healthcheck:
    test: ["CMD-SHELL", "curl -sf http://localhost:8080/health/ready"]
    interval: 15s
    timeout: 5s
    retries: 8
```

The `backend` service gains `depends_on: keycloak: condition: service_healthy`.

### Realm JSON (`docker/keycloak/realm-export.json`)

| Setting | Value |
|---------|-------|
| Realm name | `tsas` |
| Client ID | `tsas-frontend` |
| Client type | public (no client secret) |
| Auth flow | Authorization Code + PKCE |
| Redirect URIs | `http://localhost:4200/*`, `http://localhost/*` |
| Web origins | `http://localhost:4200`, `http://localhost` |
| Self-registration | enabled |
| Access token lifetime | 15 min |
| Refresh token lifetime | 30 days |
| Required scopes | `openid profile email` |

---

## Section 2: Backend Security

### `SecurityConfig` (auth-module)

- `oauth2ResourceServer(jwt(Customizer.withDefaults()))` — validates Bearer tokens against Keycloak JWKS
- Protected: all `/api/**` → HTTP 401 without valid token
- Public: `/actuator/health`, `/actuator/info`
- CSRF remains disabled (stateless REST API)
- `local` Spring profile retains `permitAll()` for backend-only development without Compose

### `application.yml`

Uncomment and wire Keycloak issuer URI:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8180/realms/tsas}
```

### `docker/compose.yml` — backend env var

```yaml
backend:
  environment:
    KEYCLOAK_ISSUER_URI: http://keycloak:8080/realms/tsas
```

### Integration Tests

`AbstractIntegrationTest` and all API IT classes receive mock JWT tokens via Spring Security Test (`SecurityMockMvcRequestPostProcessors.jwt()`). No Testcontainers Keycloak needed — JWT validation is mocked at the security filter level.

Pattern per IT class:
```java
mockMvc.perform(get("/api/players")
    .with(jwt()))
    .andExpect(status().isOk());
```

---

## Section 3: Frontend Auth

### New files

| File | Responsibility |
|------|---------------|
| `core/auth/auth.config.ts` | `OAuthModuleConfig` — issuer, clientId, scopes, PKCE |
| `core/auth/auth.service.ts` | Wraps `OAuthService`: `login()`, `logout()`, `isAuthenticated()`, `userName()` |
| `core/auth/auth.guard.ts` | `CanActivateFn` — redirects to Keycloak if no valid token |
| `core/auth/auth.interceptor.ts` | `HttpInterceptorFn` — appends `Authorization: Bearer <token>` to API requests |

### `app.config.ts` changes

```typescript
provideOAuthClient()
provideHttpClient(withInterceptorsFromDi())
```

### `app.routes.ts` changes

All routes get `canActivate: [authGuard]`.

### Login flow

On app start, `AuthGuard` calls `oauthService.loadDiscoveryDocumentAndTryLogin()`. If no valid token → `oauthService.initCodeFlow()` → redirect to Keycloak. After login, Keycloak redirects back to the original URL.

### Logout

Small user chip in the main toolbar (displays `name` claim from ID token) with a Logout button. Logout calls `oauthService.logOut()` which redirects to Keycloak's end-session endpoint.

---

## Out of Scope

- Keycloak roles / fine-grained authorization (all authenticated users have full access in V1)
- Google federated IDP (V2)
- Keycloak in production Compose hardening (TLS, external DB — separate concern)
