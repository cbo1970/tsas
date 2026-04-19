# Keycloak Authentication & Authorization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Activate end-to-end authentication via Keycloak 26 (HTTPS, Podman/Docker Compose), Spring Boot JWT resource server, and Angular `angular-oauth2-oidc` with Authorization Code + PKCE.

**Architecture:** Keycloak runs as a Compose service exposing HTTPS on port 8443 with an auto-imported realm JSON. The Spring Boot backend validates JWTs against Keycloak's JWKS endpoint (fetched via internal HTTP to avoid cert-trust issues). The Angular frontend uses `angular-oauth2-oidc` to drive the PKCE flow; an HTTP interceptor injects `Authorization: Bearer` on every API request. Integration tests use Spring Security Test's `jwt()` post-processor so Keycloak does not need to run during CI.

**Tech Stack:** Keycloak 26, `angular-oauth2-oidc` 18.x, Spring Security OAuth2 Resource Server, Spring Security Test, Podman Compose / Docker Compose

**Spec:** `docs/superpowers/specs/2026-04-19-keycloak-auth-design.md`

---

## File Map

| Action | File | Purpose |
|--------|------|---------|
| Modify | `docker/compose.yml` | Add Keycloak service, backend Keycloak env vars |
| Create | `docker/keycloak/realm-export.json` | Realm definition auto-imported by Keycloak on first start |
| Modify | `backend/auth-module/src/main/java/com/cas/tsas/auth/infrastructure/security/SecurityConfig.java` | Enable JWT resource server, custom JwtDecoder, `@Profile("!local")` |
| Create | `backend/auth-module/src/main/java/com/cas/tsas/auth/infrastructure/security/SecurityConfigLocal.java` | `@Profile("local")` permitAll stub |
| Modify | `backend/app/src/main/resources/application.yml` | Add `issuer-uri` + `jwk-set-uri` |
| Modify | `backend/app/src/test/java/com/cas/tsas/AbstractIntegrationTest.java` | Build MockMvc with JWT default via `defaultRequest` |
| Modify | `backend/app/src/test/java/com/cas/tsas/player/PlayerApiIT.java` | Remove `@Autowired MockMvc` (inherited from base) |
| Modify | `backend/app/src/test/java/com/cas/tsas/match/MatchApiIT.java` | Remove `@Autowired MockMvc` (inherited from base) |
| Modify | `frontend/src/environments/environment.ts` | Add `keycloakIssuer`, `keycloakClientId` |
| Modify | `frontend/src/environments/environment.prod.ts` | Add `keycloakIssuer`, `keycloakClientId` |
| Modify | `frontend/src/environments/environment.docker.ts` | Add `keycloakIssuer`, `keycloakClientId` |
| Modify | `frontend/src/environments/environment.mobile.ts` | Add `keycloakIssuer`, `keycloakClientId` |
| Create | `frontend/src/app/core/auth/auth.config.ts` | `AuthConfig` object for `angular-oauth2-oidc` |
| Create | `frontend/src/app/core/auth/auth.service.ts` | Wraps `OAuthService`; handles init, login, logout |
| Create | `frontend/src/app/core/auth/auth.guard.ts` | `CanActivateFn` — blocks unauthenticated navigation |
| Create | `frontend/src/app/core/auth/auth.interceptor.ts` | `HttpInterceptorFn` — injects Bearer token |
| Modify | `frontend/src/app/app.config.ts` | Add `provideOAuthClient()`, wire interceptor |
| Modify | `frontend/src/app/app.routes.ts` | Add `canActivate: [authGuard]` to all routes |
| Modify | `frontend/src/app/app.ts` | Add user chip + Logout button to toolbar |
| Modify | `frontend/angular.json` | Enable `ssl: true` for `ng serve` |

---

## Task 1: Create Feature Branch

- [ ] **Step 1: Create and switch to feature branch**

```bash
git checkout -b feature/keycloak-auth
```

Expected: `Switched to a new branch 'feature/keycloak-auth'`

---

## Task 2: Keycloak Compose Service + Realm JSON

**Files:**
- Create: `docker/keycloak/realm-export.json`
- Modify: `docker/compose.yml`

- [ ] **Step 1: Create realm JSON**

Create `docker/keycloak/realm-export.json`:

```json
{
  "realm": "tsas",
  "enabled": true,
  "registrationAllowed": true,
  "registrationEmailAsUsername": false,
  "loginWithEmailAllowed": true,
  "duplicateEmailsAllowed": false,
  "resetPasswordAllowed": true,
  "editUsernameAllowed": false,
  "accessTokenLifespan": 900,
  "ssoSessionMaxLifespan": 2592000,
  "clients": [
    {
      "clientId": "tsas-frontend",
      "name": "TSaS Frontend",
      "enabled": true,
      "publicClient": true,
      "standardFlowEnabled": true,
      "implicitFlowEnabled": false,
      "directAccessGrantsEnabled": false,
      "serviceAccountsEnabled": false,
      "redirectUris": [
        "https://localhost:4200/*",
        "https://localhost/*"
      ],
      "webOrigins": [
        "https://localhost:4200",
        "https://localhost"
      ],
      "attributes": {
        "pkce.code.challenge.method": "S256"
      }
    }
  ],
  "roles": {
    "realm": [
      {
        "name": "user",
        "description": "Standard TSaS user",
        "composite": false
      }
    ]
  },
  "defaultRoles": ["user"],
  "requiredCredentials": ["password"]
}
```

- [ ] **Step 2: Update `docker/compose.yml`**

Replace the full content of `docker/compose.yml`:

```yaml
services:

  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ${DB_NAME:-tsas}
      POSTGRES_USER: ${DB_USERNAME:-tsas}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-tsas}
    volumes:
      - ../volume/postgres:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME:-tsas}"]
      interval: 10s
      timeout: 5s
      retries: 5

  keycloak:
    image: quay.io/keycloak/keycloak:26
    command: start-dev --import-realm --https-port=8443 --hostname-url=https://localhost:8443
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: ${KC_ADMIN_PASSWORD:-admin}
    volumes:
      - ./keycloak:/opt/keycloak/data/import
    ports:
      - "8443:8443"
    healthcheck:
      test: ["CMD-SHELL", "curl -sf http://localhost:8080/health/ready"]
      interval: 15s
      timeout: 5s
      retries: 8

  backend:
    build:
      context: ..
      dockerfile: docker/backend/Dockerfile
    environment:
      DB_URL: jdbc:postgresql://db:5432/${DB_NAME:-tsas}
      DB_USERNAME: ${DB_USERNAME:-tsas}
      DB_PASSWORD: ${DB_PASSWORD:-tsas}
      KEYCLOAK_ISSUER_URI: https://localhost:8443/realms/tsas
      KEYCLOAK_JWK_SET_URI: http://keycloak:8080/realms/tsas/protocol/openid-connect/certs
    ports:
      - "8080:8080"
    depends_on:
      db:
        condition: service_healthy
      keycloak:
        condition: service_healthy

  frontend:
    build:
      context: ..
      dockerfile: docker/frontend/Dockerfile
    ports:
      - "80:80"
    depends_on:
      - backend
```

- [ ] **Step 3: Start Keycloak and verify realm import**

```bash
cd docker
podman compose up keycloak -d
# Wait ~30 seconds for startup
podman compose logs keycloak 2>&1 | grep -E "Import|tsas|Started|ERROR"
```

Expected output contains: `Realm 'tsas' imported` or `Realm tsas already exists`.

Navigate to `https://localhost:8443` in a browser, accept the self-signed cert warning, and verify the Keycloak admin console loads.

- [ ] **Step 4: Commit**

```bash
git add docker/compose.yml docker/keycloak/realm-export.json
git commit -m "feat: add Keycloak 26 service to compose with tsas realm import"
```

---

## Task 3: Backend JWT Security

**Files:**
- Modify: `backend/auth-module/src/main/java/com/cas/tsas/auth/infrastructure/security/SecurityConfig.java`
- Create: `backend/auth-module/src/main/java/com/cas/tsas/auth/infrastructure/security/SecurityConfigLocal.java`
- Modify: `backend/app/src/main/resources/application.yml`

- [ ] **Step 1: Replace `SecurityConfig.java`**

Full replacement of `backend/auth-module/src/main/java/com/cas/tsas/auth/infrastructure/security/SecurityConfig.java`:

```java
package com.cas.tsas.auth.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Profile("!local")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configure(http))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
        return decoder;
    }
}
```

- [ ] **Step 2: Create `SecurityConfigLocal.java`**

Create `backend/auth-module/src/main/java/com/cas/tsas/auth/infrastructure/security/SecurityConfigLocal.java`:

```java
package com.cas.tsas.auth.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Profile("local")
public class SecurityConfigLocal {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configure(http))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
```

- [ ] **Step 3: Update `application.yml`**

In `backend/app/src/main/resources/application.yml`, replace the commented-out security block with the active configuration. The full `application.yml` should be:

```yaml
spring:
  application:
    name: tsas-backend

  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/tsas}
    username: ${DB_USERNAME:tsas}
    password: ${DB_PASSWORD:tsas}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    open-in-view: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:https://localhost:8443/realms/tsas}
          jwk-set-uri: ${KEYCLOAK_JWK_SET_URI:https://localhost:8443/realms/tsas/protocol/openid-connect/certs}

tsas:
  cors:
    allowed-origins:
      - "http://localhost:4200"
      - "https://localhost:4200"
      - "http://192.168.1.101:4200"
      - "https://192.168.1.101:4200"

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

- [ ] **Step 4: Verify the smoke test still passes (local profile)**

```bash
cd /Users/cbo/Projects/cas/tsas/backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test --tests "com.cas.tsas.TsasBackendApplicationTests"
```

Expected: `BUILD SUCCESSFUL` — local profile uses `SecurityConfigLocal` (permitAll), no Keycloak needed.

- [ ] **Step 5: Commit**

```bash
git add backend/auth-module/src/main/java/com/cas/tsas/auth/infrastructure/security/SecurityConfig.java \
        backend/auth-module/src/main/java/com/cas/tsas/auth/infrastructure/security/SecurityConfigLocal.java \
        backend/app/src/main/resources/application.yml
git commit -m "feat: enable JWT resource server (Keycloak), keep local profile open"
```

---

## Task 4: Backend Integration Tests — Add JWT Auth

All `@SpringBootTest` integration tests load `SecurityConfig` (non-local profile) and now require JWT tokens. We configure MockMvc in the base class to apply a mock JWT to every request automatically.

**Files:**
- Modify: `backend/app/src/test/java/com/cas/tsas/AbstractIntegrationTest.java`
- Modify: `backend/app/src/test/java/com/cas/tsas/player/PlayerApiIT.java`
- Modify: `backend/app/src/test/java/com/cas/tsas/match/MatchApiIT.java`

- [ ] **Step 1: Update `AbstractIntegrationTest.java`**

Replace the full file content:

```java
package com.cas.tsas;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private WebApplicationContext wac;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .defaultRequest(get("/").with(jwt()))
                .build();
    }
}
```

Key changes:
- Removed `@AutoConfigureMockMvc` (MockMvc is now built manually)
- Added `protected MockMvc mockMvc` field (subclasses inherit and use this)
- `@BeforeEach setUpMockMvc()` runs before each test, building MockMvc with `springSecurity()` + `jwt()` default on every request

- [ ] **Step 2: Remove `@Autowired MockMvc mockMvc` from `PlayerApiIT`**

In `backend/app/src/test/java/com/cas/tsas/player/PlayerApiIT.java`, remove the line:
```java
@Autowired MockMvc mockMvc;
```

The `mockMvc` field is now inherited from `AbstractIntegrationTest`.

- [ ] **Step 3: Remove `@Autowired MockMvc mockMvc` from `MatchApiIT`**

In `backend/app/src/test/java/com/cas/tsas/match/MatchApiIT.java`, remove the line:
```java
@Autowired MockMvc mockMvc;
```

- [ ] **Step 4: Run all integration tests**

```bash
cd /Users/cbo/Projects/cas/tsas/backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test
```

Expected: `BUILD SUCCESSFUL` — all tests pass with mock JWT auth.

If any test fails with `status().isUnauthorized()` (401), the `defaultRequest(get("/").with(jwt()))` was not applied. Check that `springSecurity()` is correctly applied and the `mockMvc` field in the subclass shadows the base class field (it shouldn't — the subclass field declaration must be removed).

- [ ] **Step 5: Commit**

```bash
git add backend/app/src/test/java/com/cas/tsas/AbstractIntegrationTest.java \
        backend/app/src/test/java/com/cas/tsas/player/PlayerApiIT.java \
        backend/app/src/test/java/com/cas/tsas/match/MatchApiIT.java
git commit -m "test: configure AbstractIntegrationTest with mock JWT for all API ITs"
```

---

## Task 5: Frontend — Install + Environment + Auth Files

**Files:**
- Modify: `frontend/src/environments/environment.ts`
- Modify: `frontend/src/environments/environment.prod.ts`
- Modify: `frontend/src/environments/environment.docker.ts`
- Create: `frontend/src/app/core/auth/auth.config.ts`
- Create: `frontend/src/app/core/auth/auth.service.ts`
- Create: `frontend/src/app/core/auth/auth.guard.ts`
- Create: `frontend/src/app/core/auth/auth.interceptor.ts`

- [ ] **Step 1: Install `angular-oauth2-oidc`**

```bash
cd /Users/cbo/Projects/cas/tsas/frontend
npm install angular-oauth2-oidc
```

Expected: package added to `package.json`, no peer dependency errors.

- [ ] **Step 2: Update environment files**

Replace `frontend/src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  keycloakIssuer: 'https://localhost:8443/realms/tsas',
  keycloakClientId: 'tsas-frontend'
};
```

Replace `frontend/src/environments/environment.prod.ts`:

```typescript
export const environment = {
  production: true,
  apiUrl: 'http://localhost:8080',
  keycloakIssuer: 'https://localhost:8443/realms/tsas',
  keycloakClientId: 'tsas-frontend'
};
```

Replace `frontend/src/environments/environment.docker.ts`:

```typescript
export const environment = {
  production: true,
  apiUrl: '',
  keycloakIssuer: 'https://localhost:8443/realms/tsas',
  keycloakClientId: 'tsas-frontend'
};
```

Replace `frontend/src/environments/environment.mobile.ts`:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://192.168.1.101:8080',
  keycloakIssuer: 'https://192.168.1.101:8443/realms/tsas',
  keycloakClientId: 'tsas-frontend'
};
```

- [ ] **Step 3: Create `auth.config.ts`**

Create `frontend/src/app/core/auth/auth.config.ts`:

```typescript
import { AuthConfig } from 'angular-oauth2-oidc';
import { environment } from '../../../environments/environment';

export const authConfig: AuthConfig = {
  issuer: environment.keycloakIssuer,
  clientId: environment.keycloakClientId,
  responseType: 'code',
  redirectUri: window.location.origin + '/',
  scope: 'openid profile email',
  showDebugInformation: false,
  clearHashAfterLogin: true,
};
```

- [ ] **Step 4: Create `auth.service.ts`**

Create `frontend/src/app/core/auth/auth.service.ts`:

```typescript
import { Injectable, inject } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { authConfig } from './auth.config';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly oauthService = inject(OAuthService);
  private initialized = false;

  async initialize(): Promise<boolean> {
    if (this.initialized) return this.oauthService.hasValidAccessToken();
    this.initialized = true;
    this.oauthService.configure(authConfig);
    await this.oauthService.loadDiscoveryDocumentAndTryLogin();
    return this.oauthService.hasValidAccessToken();
  }

  login(): void {
    this.oauthService.initCodeFlow();
  }

  logout(): void {
    this.oauthService.logOut();
  }

  isAuthenticated(): boolean {
    return this.oauthService.hasValidAccessToken();
  }

  userName(): string {
    const claims = this.oauthService.getIdentityClaims() as { name?: string } | null;
    return claims?.name ?? '';
  }
}
```

- [ ] **Step 5: Create `auth.guard.ts`**

Create `frontend/src/app/core/auth/auth.guard.ts`:

```typescript
import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = async () => {
  const authService = inject(AuthService);
  const authenticated = await authService.initialize();
  if (!authenticated) {
    authService.login();
    return false;
  }
  return true;
};
```

- [ ] **Step 6: Create `auth.interceptor.ts`**

Create `frontend/src/app/core/auth/auth.interceptor.ts`:

```typescript
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(OAuthService).getAccessToken();
  if (token) {
    return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
  }
  return next(req);
};
```

- [ ] **Step 7: Verify TypeScript compiles**

```bash
cd /Users/cbo/Projects/cas/tsas/frontend
npx ng build --configuration development 2>&1 | tail -20
```

Expected: `Build at:` line with no errors. If errors about `window` in `auth.config.ts` (SSR), ignore — this is a browser-only app.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/environments/ \
        frontend/src/app/core/auth/ \
        frontend/package.json \
        frontend/package-lock.json \
        frontend/node_modules/.package-lock.json 2>/dev/null; true
git commit -m "feat: add angular-oauth2-oidc auth config, service, guard, interceptor"
```

---

## Task 6: Frontend — App Wiring

**Files:**
- Modify: `frontend/src/app/app.config.ts`
- Modify: `frontend/src/app/app.routes.ts`
- Modify: `frontend/src/app/app.ts`
- Modify: `frontend/angular.json`

- [ ] **Step 1: Update `app.config.ts`**

Replace `frontend/src/app/app.config.ts`:

```typescript
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideOAuthClient } from 'angular-oauth2-oidc';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimationsAsync(),
    provideOAuthClient()
  ]
};
```

- [ ] **Step 2: Update `app.routes.ts`**

Replace `frontend/src/app/app.routes.ts`:

```typescript
import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'players', pathMatch: 'full' },
  {
    path: 'players',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/players/players.component').then(m => m.PlayersComponent)
  },
  {
    path: 'matches/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/matches/match-setup/match-setup.component').then(m => m.MatchSetupComponent)
  },
  {
    path: 'matches/:id/score',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/matches/score/score.component').then(m => m.ScoreComponent)
  }
];
```

- [ ] **Step 3: Update `app.ts` — add user chip and logout button**

Replace `frontend/src/app/app.ts`:

```typescript
import { Component, inject, signal, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from './core/auth/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatToolbarModule, MatButtonModule, MatIconModule],
  template: `
    <mat-toolbar color="primary">
      <mat-icon>sports_tennis</mat-icon>
      <span class="app-title">TSaS – Tennis Score &amp; Statistic</span>
      <span class="spacer"></span>
      <a mat-button routerLink="/players" routerLinkActive="active-link">
        <mat-icon>people</mat-icon> Spieler
      </a>
      <a mat-button routerLink="/matches/new" routerLinkActive="active-link">
        <mat-icon>add_circle</mat-icon> Neues Match
      </a>
      @if (userName()) {
        <span class="user-name">{{ userName() }}</span>
        <button mat-button (click)="logout()">
          <mat-icon>logout</mat-icon>
        </button>
      }
    </mat-toolbar>
    <main>
      <router-outlet />
    </main>
  `,
  styles: [`
    :host { display: flex; flex-direction: column; min-height: 100vh; }
    .app-title { margin-left: 12px; font-size: 20px; font-weight: 500; }
    .spacer { flex: 1; }
    main { flex: 1; background: #f5f5f5; min-height: calc(100vh - 64px); }
    .active-link { background: rgba(255,255,255,0.15); border-radius: 4px; }
    mat-toolbar mat-icon { font-size: 28px; }
    .user-name { font-size: 14px; margin-right: 4px; opacity: 0.9; }
  `]
})
export class App implements OnInit {
  private readonly authService = inject(AuthService);

  protected userName = signal('');

  ngOnInit() {
    this.authService.initialize().then(() => {
      this.userName.set(this.authService.userName());
    });
  }

  protected logout() {
    this.authService.logout();
  }
}
```

- [ ] **Step 4: Enable SSL in `angular.json`**

In `frontend/angular.json`, find the `"serve"` → `"options"` section and add `"ssl": true`:

```json
"serve": {
  "builder": "@angular/build:dev-server",
  "options": {
    "host": "0.0.0.0",
    "ssl": true
  },
  ...
}
```

- [ ] **Step 5: Verify TypeScript compiles**

```bash
cd /Users/cbo/Projects/cas/tsas/frontend
npx ng build --configuration development 2>&1 | tail -20
```

Expected: clean build, no TypeScript errors.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/app.config.ts \
        frontend/src/app/app.routes.ts \
        frontend/src/app/app.ts \
        frontend/angular.json
git commit -m "feat: wire angular-oauth2-oidc into app (routes, toolbar, SSL)"
```

---

## Task 7: End-to-End Smoke Test

Verify the full stack works: Keycloak starts, realm imports, Angular redirects to login, login succeeds, API calls return 200.

- [ ] **Step 1: Start full Compose stack**

```bash
cd /Users/cbo/Projects/cas/tsas/docker
podman compose up -d
podman compose logs -f 2>&1 | grep -E "Started|ERROR|WARN|Realm" &
```

Wait until all containers are healthy (typically 60–90 seconds for Keycloak).

- [ ] **Step 2: Verify Keycloak is healthy**

```bash
curl -sf http://localhost:8080/health/ready 2>/dev/null || \
curl -sk https://localhost:8443/realms/tsas/.well-known/openid-configuration | python3 -m json.tool | grep issuer
```

Expected: `"issuer": "https://localhost:8443/realms/tsas"`

- [ ] **Step 3: Verify backend rejects unauthenticated requests**

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/players
```

Expected: `401`

- [ ] **Step 4: Start Angular dev server with SSL**

```bash
cd /Users/cbo/Projects/cas/tsas/frontend
npm start
```

Expected output includes: `Local: https://localhost:4200/`

- [ ] **Step 5: Manual browser test**

1. Open `https://localhost:4200` — browser shows cert warning for Angular's self-signed cert, accept it.
2. Also open `https://localhost:8443` — accept Keycloak's self-signed cert.
3. Navigate back to `https://localhost:4200` — you are redirected to Keycloak login page.
4. Click "Register" and create a test user.
5. Log in — you are redirected back to the app and see the Players list.
6. Your name appears in the toolbar, the Logout button is visible.
7. Click Logout — redirected to Keycloak, session ended.

- [ ] **Step 6: Stop Compose**

```bash
cd /Users/cbo/Projects/cas/tsas/docker
podman compose down
```

---

## Task 8: Run Backend Tests + Final Commit

- [ ] **Step 1: Run full backend test suite**

```bash
cd /Users/cbo/Projects/cas/tsas/backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Confirm all files are committed**

```bash
git status
```

Expected: `nothing to commit, working tree clean`

If anything is uncommitted, commit it now with an appropriate message.
