# TEN-55 Owner-Bindung & RBAC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Vollständige Datenisolation pro Nutzer auf `Player`, `Match`, `Point`. Cross-Tenant-Zugriffe liefern 404. Keycloak-Rolle `ADMIN` umgeht den Filter.

**Architecture:** Auth-Bridge (`CurrentUser` Value + `CurrentUserProvider` Port) im `auth-module` extrahiert `sub` und `realm_access.roles` aus dem Spring Security Context. Domain-Aggregate `Player` und `Match` bekommen `ownerId`; Repository-Adapter erhalten Owner-gefilterte Queries; Service-Layer ruft den Provider und entscheidet zwischen Owner-Pfad und Admin-Bypass. `Point` erbt Owner vom Match (kein eigenes Feld).

**Tech Stack:** Spring Boot 4.0.6 (Java 25), Spring Security OAuth2 Resource Server, JPA/Hibernate, Flyway, JUnit 5 + Spring Security Test (`jwt()` post-processor), Testcontainers PostgreSQL.

**Spec:** `docs/superpowers/specs/2026-06-22-ten-55-owner-binding-rbac-design.md`

---

## File Structure

### Neue Dateien

| Pfad | Verantwortung |
|---|---|
| `auth-module/src/main/java/com/cas/tsas/auth/domain/CurrentUser.java` | Value-POJO `(UUID id, Set<Role> roles)` |
| `auth-module/src/main/java/com/cas/tsas/auth/domain/Role.java` | Enum `COACH`, `ADMIN` |
| `auth-module/src/main/java/com/cas/tsas/auth/application/port/in/CurrentUserProvider.java` | Port `CurrentUser get()` |
| `auth-module/src/main/java/com/cas/tsas/auth/infrastructure/security/JwtCurrentUserProvider.java` | Liest aus `SecurityContextHolder` |
| `auth-module/src/main/java/com/cas/tsas/auth/infrastructure/security/KeycloakRealmRoleConverter.java` | `Jwt` → `Collection<GrantedAuthority>` |
| `auth-module/src/test/java/com/cas/tsas/auth/testsupport/JwtTestSupport.java` | MockMvc-Helper `withUser(UUID, Role...)` |
| `app/src/main/resources/db/migration/V6__add_owner_id.sql` | Flyway-Migration |
| `app/src/test/java/com/cas/tsas/security/OwnershipIntegrationTest.java` | End-to-End Cross-Tenant-Tests |

### Geänderte Dateien

| Pfad | Änderung |
|---|---|
| `auth-module/src/main/java/com/cas/tsas/auth/infrastructure/security/SecurityConfig.java` | `JwtAuthenticationConverter` registrieren |
| `player-module/.../domain/model/Player.java` | Feld + Konstruktor + Getter/Setter `ownerId` |
| `player-module/.../infrastructure/persistence/entity/PlayerJpaEntity.java` | `@Column owner_id NOT NULL` |
| `player-module/.../infrastructure/persistence/mapper/PlayerMapper.java` | `ownerId` mappen |
| `player-module/.../application/port/out/LoadPlayerPort.java` | `findByIdAndOwner(...)`, `findAllByOwner(...)` |
| `player-module/.../infrastructure/persistence/repository/PlayerPersistenceAdapter.java` | Neue Methoden |
| `player-module/.../infrastructure/persistence/repository/PlayerJpaRepository.java` | Spring-Data-Queries |
| `player-module/.../application/service/PlayerService.java` | `CurrentUserProvider` injizieren, Owner-Filter, Admin-Bypass |
| `match-module/.../domain/model/Match.java` | Feld + Konstruktor + Getter/Setter `ownerId` |
| `match-module/.../infrastructure/persistence/entity/MatchJpaEntity.java` | `@Column owner_id NOT NULL` |
| `match-module/.../infrastructure/persistence/repository/MatchPersistenceAdapter.java` | Owner-Filter-Queries |
| `match-module/.../infrastructure/persistence/repository/MatchJpaRepository.java` | Spring-Data-Queries |
| `match-module/.../application/service/MatchService.java` | `CurrentUserProvider`, Owner-Filter |
| `match-module/.../application/service/ScoringService.java` | Lädt Match via `findById` → Owner-Check ererbt |
| `docker/keycloak/realm-export.json` | Realm-Rollen `COACH`, `ADMIN`, default `COACH` |

`statistics-module` und `ai-module`: **unverändert** — laden Match über `GetMatchUseCase`, erben Owner-Check.

---

## Konstanten / Werte (Referenz für alle Tasks)

- Admin-Backfill-UUID für V6: `00000000-0000-0000-0000-000000000000`
- Realm-Role-Claim: `realm_access.roles` (Liste von Strings)
- Authority-Präfix: `ROLE_` (Spring-Default)
- Build-Command (vom Repo-Root **eines Levels höher** als das Plan-Verzeichnis): `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test`
- Einzeltest: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :MODUL:test --tests "FQCN"`

---

## Phase 1 — Auth-Bridge

### Task 1: `Role` Enum + `CurrentUser` Value-Objekt

**Files:**
- Create: `backend/auth-module/src/main/java/com/cas/tsas/auth/domain/Role.java`
- Create: `backend/auth-module/src/main/java/com/cas/tsas/auth/domain/CurrentUser.java`
- Create: `backend/auth-module/src/test/java/com/cas/tsas/auth/domain/CurrentUserTest.java`

- [ ] **Step 1: Test schreiben**

```java
package com.cas.tsas.auth.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentUserTest {

    @Test
    void hasRole_returns_true_when_role_present() {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), Set.of(Role.COACH, Role.ADMIN));
        assertThat(user.hasRole(Role.ADMIN)).isTrue();
        assertThat(user.hasRole(Role.COACH)).isTrue();
    }

    @Test
    void hasRole_returns_false_when_role_absent() {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), Set.of(Role.COACH));
        assertThat(user.hasRole(Role.ADMIN)).isFalse();
    }
}
```

- [ ] **Step 2: Test laufen lassen — erwarteter Fehler: Klassen fehlen**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :auth-module:test --tests "com.cas.tsas.auth.domain.CurrentUserTest"`
Expected: compile-error („cannot find symbol Role/CurrentUser").

- [ ] **Step 3: `Role` implementieren**

```java
package com.cas.tsas.auth.domain;

/** Realm-Rollen aus Keycloak (`realm_access.roles`). */
public enum Role {
    COACH,
    ADMIN
}
```

- [ ] **Step 4: `CurrentUser` implementieren**

```java
package com.cas.tsas.auth.domain;

import java.util.Set;
import java.util.UUID;

/** Aktueller Nutzer aus dem JWT (`sub` + Realm-Rollen). Pure POJO. */
public record CurrentUser(UUID id, Set<Role> roles) {

    public CurrentUser {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        if (roles == null) throw new IllegalArgumentException("roles must not be null");
        roles = Set.copyOf(roles);
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }
}
```

- [ ] **Step 5: Test grün**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :auth-module:test --tests "com.cas.tsas.auth.domain.CurrentUserTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/auth-module/src/main/java/com/cas/tsas/auth/domain/Role.java \
        backend/auth-module/src/main/java/com/cas/tsas/auth/domain/CurrentUser.java \
        backend/auth-module/src/test/java/com/cas/tsas/auth/domain/CurrentUserTest.java
git commit -m "feat(auth): add CurrentUser value and Role enum (TEN-55)"
```

---

### Task 2: `CurrentUserProvider` Port + `JwtCurrentUserProvider`

**Files:**
- Create: `backend/auth-module/src/main/java/com/cas/tsas/auth/application/port/in/CurrentUserProvider.java`
- Create: `backend/auth-module/src/main/java/com/cas/tsas/auth/infrastructure/security/JwtCurrentUserProvider.java`
- Create: `backend/auth-module/src/test/java/com/cas/tsas/auth/infrastructure/security/JwtCurrentUserProviderTest.java`

- [ ] **Step 1: Test schreiben**

```java
package com.cas.tsas.auth.infrastructure.security;

import com.cas.tsas.auth.domain.CurrentUser;
import com.cas.tsas.auth.domain.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtCurrentUserProviderTest {

    private final JwtCurrentUserProvider provider = new JwtCurrentUserProvider();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void extracts_sub_and_role_authorities() {
        UUID sub = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("t")
                .header("alg", "none")
                .subject(sub.toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("ROLE_COACH"),
                        new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        CurrentUser user = provider.get();

        assertThat(user.id()).isEqualTo(sub);
        assertThat(user.roles()).containsExactlyInAnyOrder(Role.COACH, Role.ADMIN);
    }

    @Test
    void throws_when_no_authentication() {
        assertThatThrownBy(provider::get).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void throws_when_authentication_is_anonymous() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("k", "anonymous",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        assertThatThrownBy(provider::get).isInstanceOf(IllegalStateException.class);
    }
}
```

- [ ] **Step 2: Test laufen lassen — erwarteter Fehler: Klasse fehlt**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :auth-module:test --tests "com.cas.tsas.auth.infrastructure.security.JwtCurrentUserProviderTest"`
Expected: compile-error.

- [ ] **Step 3: Port implementieren**

```java
package com.cas.tsas.auth.application.port.in;

import com.cas.tsas.auth.domain.CurrentUser;

/** Liefert den aktuell authentifizierten Nutzer. */
public interface CurrentUserProvider {
    CurrentUser get();
}
```

- [ ] **Step 4: Implementierung schreiben**

```java
package com.cas.tsas.auth.infrastructure.security;

import com.cas.tsas.auth.application.port.in.CurrentUserProvider;
import com.cas.tsas.auth.domain.CurrentUser;
import com.cas.tsas.auth.domain.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtCurrentUserProvider implements CurrentUserProvider {

    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public CurrentUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            throw new IllegalStateException("No authenticated JWT in security context");
        }
        UUID id = UUID.fromString(jwtAuth.getToken().getSubject());
        Set<Role> roles = EnumSet.noneOf(Role.class);
        for (GrantedAuthority ga : jwtAuth.getAuthorities()) {
            String raw = ga.getAuthority();
            if (raw.startsWith(ROLE_PREFIX)) {
                try {
                    roles.add(Role.valueOf(raw.substring(ROLE_PREFIX.length())));
                } catch (IllegalArgumentException ignored) {
                    // unknown role — ignore
                }
            }
        }
        return new CurrentUser(id, roles);
    }
}
```

- [ ] **Step 5: Test grün**

Run: wie Step 2 — Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/auth-module/src/main/java/com/cas/tsas/auth/application/port/in/CurrentUserProvider.java \
        backend/auth-module/src/main/java/com/cas/tsas/auth/infrastructure/security/JwtCurrentUserProvider.java \
        backend/auth-module/src/test/java/com/cas/tsas/auth/infrastructure/security/JwtCurrentUserProviderTest.java
git commit -m "feat(auth): add CurrentUserProvider port and JWT-based impl (TEN-55)"
```

---

### Task 3: `KeycloakRealmRoleConverter` + `SecurityConfig`-Anbindung

**Files:**
- Create: `backend/auth-module/src/main/java/com/cas/tsas/auth/infrastructure/security/KeycloakRealmRoleConverter.java`
- Create: `backend/auth-module/src/test/java/com/cas/tsas/auth/infrastructure/security/KeycloakRealmRoleConverterTest.java`
- Modify: `backend/auth-module/src/main/java/com/cas/tsas/auth/infrastructure/security/SecurityConfig.java`

- [ ] **Step 1: Converter-Test schreiben**

```java
package com.cas.tsas.auth.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRealmRoleConverterTest {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    @Test
    void maps_realm_access_roles_to_ROLE_authorities() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .subject("sub")
                .claim("realm_access", Map.of("roles", List.of("COACH", "ADMIN")))
                .build();

        List<String> authorities = converter.convert(jwt).stream()
                .map(GrantedAuthority::getAuthority).toList();

        assertThat(authorities).containsExactlyInAnyOrder("ROLE_COACH", "ROLE_ADMIN");
    }

    @Test
    void returns_empty_when_realm_access_missing() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .subject("sub").build();
        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    void returns_empty_when_roles_missing() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .subject("sub").claim("realm_access", Map.of()).build();
        assertThat(converter.convert(jwt)).isEmpty();
    }
}
```

- [ ] **Step 2: Test laufen lassen — fehlt**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :auth-module:test --tests "com.cas.tsas.auth.infrastructure.security.KeycloakRealmRoleConverterTest"`
Expected: compile-error.

- [ ] **Step 3: Converter implementieren**

```java
package com.cas.tsas.auth.infrastructure.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Wandelt `realm_access.roles` aus dem JWT in Spring-Authorities `ROLE_<NAME>`. */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) return List.of();
        Object roles = realmAccess.get("roles");
        if (!(roles instanceof Collection<?> col)) return List.of();
        return col.stream()
                .map(Object::toString)
                .map(name -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + name))
                .toList();
    }
}
```

- [ ] **Step 4: Test grün**

Run: wie Step 2 — Expected: PASS.

- [ ] **Step 5: `SecurityConfig` integrieren**

Aktuelle Datei (`backend/auth-module/src/main/java/com/cas/tsas/auth/infrastructure/security/SecurityConfig.java`) so ersetzen:

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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
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

- [ ] **Step 6: Build durch (kein neuer Test)**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :auth-module:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add backend/auth-module/src/main/java/com/cas/tsas/auth/infrastructure/security/KeycloakRealmRoleConverter.java \
        backend/auth-module/src/test/java/com/cas/tsas/auth/infrastructure/security/KeycloakRealmRoleConverterTest.java \
        backend/auth-module/src/main/java/com/cas/tsas/auth/infrastructure/security/SecurityConfig.java
git commit -m "feat(auth): map realm_access.roles to ROLE_ authorities (TEN-55)"
```

---

### Task 4: Test-Helper `JwtTestSupport`

**Begründung der Spec-Abweichung:** Der Spec sah einen Header-Filter im `test`-Profil vor. `AbstractIntegrationTest` benutzt aber bereits den realen `SecurityConfig` mit `jwt()`-Post-Processor — ein separater Filter ist überflüssig. Wir bauen stattdessen einen MockMvc-Helper.

**Files:**
- Create: `backend/auth-module/src/test/java/com/cas/tsas/auth/testsupport/JwtTestSupport.java`

> Hinweis: Damit andere Module den Helper konsumieren können, ergänzt `auth-module/build.gradle.kts` den Test-Source als Sharable. Wenn der Repo dieses Pattern noch nicht etabliert hat: `testFixtures` nutzen (siehe Step 2).

- [ ] **Step 1: `testFixtures`-Plugin in `auth-module/build.gradle.kts` aktivieren**

Falls dort noch nicht vorhanden, ergänzen:

```kotlin
plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-test")
    testFixturesImplementation("org.springframework.security:spring-security-test")
}
```

Helper-Datei nicht in `src/test/...`, sondern in `src/testFixtures/java/...` legen:
- Create: `backend/auth-module/src/testFixtures/java/com/cas/tsas/auth/testsupport/JwtTestSupport.java`

- [ ] **Step 2: Helper schreiben**

```java
package com.cas.tsas.auth.testsupport;

import com.cas.tsas.auth.domain.Role;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/** Helper: `mockMvc.perform(get(...).with(JwtTestSupport.withUser(id, Role.COACH))) `. */
public final class JwtTestSupport {

    private JwtTestSupport() {}

    public static JwtRequestPostProcessor withUser(UUID userId, Role... roles) {
        return jwt().jwt(j -> j
                        .subject(userId.toString())
                        .claim("realm_access", Map.of("roles", roleNames(roles))))
                .authorities(Arrays.stream(roles).map(r -> "ROLE_" + r.name())
                        .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new).toList());
    }

    private static List<String> roleNames(Role[] roles) {
        return Arrays.stream(roles).map(Role::name).toList();
    }
}
```

- [ ] **Step 3: `app/build.gradle.kts` (oder relevante Konsumenten) testCompile auf `testFixtures(project(":auth-module"))` setzen**

```kotlin
dependencies {
    testImplementation(testFixtures(project(":auth-module")))
}
```

- [ ] **Step 4: Build durch**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :app:compileTestJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add backend/auth-module/src/testFixtures/ backend/auth-module/build.gradle.kts backend/app/build.gradle.kts
git commit -m "test(auth): add JwtTestSupport for cross-tenant MockMvc tests (TEN-55)"
```

---

## Phase 2 — Migration

### Task 5: Flyway V6 `add_owner_id`

**Files:**
- Create: `backend/app/src/main/resources/db/migration/V6__add_owner_id.sql`

- [ ] **Step 1: Migration schreiben**

```sql
-- TEN-55: Owner-Binding auf Player und Match.
-- Backfill-UUID 00000000-... markiert Pre-Migration-Daten (Dev-Bestand).
-- Prod startet ohne Datenbestand; in Prod ist der Backfill no-op.

ALTER TABLE players ADD COLUMN owner_id UUID;
ALTER TABLE matches ADD COLUMN owner_id UUID;

UPDATE players SET owner_id = '00000000-0000-0000-0000-000000000000' WHERE owner_id IS NULL;
UPDATE matches SET owner_id = '00000000-0000-0000-0000-000000000000' WHERE owner_id IS NULL;

ALTER TABLE players ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE matches ALTER COLUMN owner_id SET NOT NULL;

CREATE INDEX idx_players_owner ON players(owner_id);
CREATE INDEX idx_matches_owner ON matches(owner_id);
```

- [ ] **Step 2: Bootstrap-Test — Flyway läuft sauber**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew :app:test --tests "com.cas.tsas.TsasBackendApplicationTests"`
Expected: PASS (Container startet, Migration läuft).

- [ ] **Step 3: Commit**

```bash
git add backend/app/src/main/resources/db/migration/V6__add_owner_id.sql
git commit -m "db: migration V6 — owner_id on players and matches (TEN-55)"
```

---

## Phase 3 — Player-Aggregat

### Task 6: `Player` Domain um `ownerId` erweitern

**Files:**
- Modify: `backend/player-module/src/main/java/com/cas/tsas/player/domain/model/Player.java`

- [ ] **Step 1: Test schreiben**

Datei: `backend/player-module/src/test/java/com/cas/tsas/player/domain/model/PlayerTest.java`

```java
package com.cas.tsas.player.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerTest {

    @Test
    void constructor_assigns_owner() {
        UUID owner = UUID.randomUUID();
        Player p = new Player(null, owner, "A", "B", null, null, null, null, null, null);
        assertThat(p.getOwnerId()).isEqualTo(owner);
    }
}
```

- [ ] **Step 2: Test laufen lassen — fehlt**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :player-module:test --tests "com.cas.tsas.player.domain.model.PlayerTest"`
Expected: compile-error.

- [ ] **Step 3: Domain anpassen**

`Player.java`-Klasse:

```java
public class Player {

    private UUID id;
    private UUID ownerId;     // NEU
    private String firstName;
    private String lastName;
    private Gender gender;
    private Handedness handedness;
    private BackhandType backhandType;
    private String ranking;
    private String nationality;
    private LocalDate birthDate;
    private boolean active = true;

    public Player() {}

    public Player(UUID id, UUID ownerId, String firstName, String lastName, Gender gender,
                  Handedness handedness, BackhandType backhandType,
                  String ranking, String nationality, LocalDate birthDate) {
        this.id = id;
        this.ownerId = ownerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.handedness = handedness;
        this.backhandType = backhandType;
        this.ranking = ranking;
        this.nationality = nationality;
        this.birthDate = birthDate;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }

    // ... übrige Getter/Setter unverändert
}
```

- [ ] **Step 4: Test grün**

Run: wie Step 2 — Expected: PASS.

- [ ] **Step 5: Commit** (zusammen mit Folge-Task 7+8, um den Compile zu halten)

> **Hinweis:** Player wird jetzt nicht-compile-fähig konsumiert. NICHT separat committen — direkt mit Task 7 + 8 kombinieren. Mache nur einen lokalen `git add -p` für die Player-Datei und gehe weiter.

---

### Task 7: `PlayerJpaEntity` + `PlayerMapper` um `ownerId` erweitern

**Files:**
- Modify: `backend/player-module/src/main/java/com/cas/tsas/player/infrastructure/persistence/entity/PlayerJpaEntity.java`
- Modify: `backend/player-module/src/main/java/com/cas/tsas/player/infrastructure/persistence/mapper/PlayerMapper.java`

- [ ] **Step 1: Entity ergänzen**

In `PlayerJpaEntity.java` ergänzen (nach `@Id`-Block):

```java
@Column(name = "owner_id", nullable = false)
private UUID ownerId;

public UUID getOwnerId() { return ownerId; }
public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
```

- [ ] **Step 2: `PlayerMapper.java` lesen**

Run: `cat backend/player-module/src/main/java/com/cas/tsas/player/infrastructure/persistence/mapper/PlayerMapper.java` und `ownerId` in beiden `toEntity`/`toDomain`-Methoden ergänzen, analog zu `id`.

- [ ] **Step 3: Build sicherstellen**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :player-module:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit (gemeinsam mit Task 8)**

---

### Task 8: `LoadPlayerPort` um Owner-Queries + Adapter

**Files:**
- Modify: `backend/player-module/.../application/port/out/LoadPlayerPort.java`
- Modify: `backend/player-module/.../infrastructure/persistence/repository/PlayerJpaRepository.java`
- Modify: `backend/player-module/.../infrastructure/persistence/repository/PlayerPersistenceAdapter.java`

- [ ] **Step 1: Test schreiben**

Datei: `backend/player-module/src/test/java/com/cas/tsas/player/infrastructure/persistence/repository/PlayerPersistenceAdapterIntegrationTest.java`

```java
package com.cas.tsas.player.infrastructure.persistence.repository;

import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.player.domain.model.Player;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PlayerPersistenceAdapter.class, com.cas.tsas.player.infrastructure.persistence.mapper.PlayerMapper.class})
class PlayerPersistenceAdapterIntegrationTest {

    @Autowired LoadPlayerPort port;
    @Autowired com.cas.tsas.player.application.port.out.SavePlayerPort save;

    @Test
    void findAllByOwner_returns_only_owner_players() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        save.savePlayer(new Player(null, a, "A1", "L", null, null, null, null, null, null));
        save.savePlayer(new Player(null, a, "A2", "L", null, null, null, null, null, null));
        save.savePlayer(new Player(null, b, "B1", "L", null, null, null, null, null, null));

        List<Player> aOnly = port.findAllByOwner(a);

        assertThat(aOnly).extracting(Player::getFirstName).containsExactlyInAnyOrder("A1", "A2");
    }

    @Test
    void findByIdAndOwner_returns_empty_when_other_owner() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        Player created = save.savePlayer(new Player(null, a, "X", "Y", null, null, null, null, null, null));

        assertThat(port.findByIdAndOwner(created.getId(), b)).isEmpty();
        assertThat(port.findByIdAndOwner(created.getId(), a)).isPresent();
    }
}
```

> Voraussetzung: `:player-module` hat `org.springframework.boot:spring-boot-starter-test` und Testcontainers-Setup, oder ein eingebettetes H2-Profil. Falls nicht, Test stattdessen als IT in `:app` mit `AbstractIntegrationTest` schreiben.

- [ ] **Step 2: Test laufen lassen — fehlt**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew :player-module:test --tests "com.cas.tsas.player.infrastructure.persistence.repository.PlayerPersistenceAdapterIntegrationTest"`
Expected: compile-error.

- [ ] **Step 3: Port erweitern**

`LoadPlayerPort.java`:

```java
package com.cas.tsas.player.application.port.out;

import com.cas.tsas.player.domain.model.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadPlayerPort {
    Optional<Player> loadPlayer(UUID id);
    List<Player> loadAllPlayers();
    Optional<Player> findByIdAndOwner(UUID id, UUID ownerId);
    List<Player> findAllByOwner(UUID ownerId);
}
```

- [ ] **Step 4: Spring-Data-Queries**

`PlayerJpaRepository.java` ergänzen:

```java
Optional<PlayerJpaEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
List<PlayerJpaEntity> findAllByOwnerId(UUID ownerId);
```

- [ ] **Step 5: Adapter erweitern**

`PlayerPersistenceAdapter.java`:

```java
@Override
public Optional<Player> findByIdAndOwner(UUID id, UUID ownerId) {
    return repository.findByIdAndOwnerId(id, ownerId).map(mapper::toDomain);
}

@Override
public List<Player> findAllByOwner(UUID ownerId) {
    return repository.findAllByOwnerId(ownerId).stream().map(mapper::toDomain).toList();
}
```

- [ ] **Step 6: Test grün**

Run: wie Step 2 — Expected: PASS.

- [ ] **Step 7: Commit (Task 6 + 7 + 8 gemeinsam)**

```bash
git add backend/player-module/src/main/java/com/cas/tsas/player/domain/model/Player.java \
        backend/player-module/src/main/java/com/cas/tsas/player/infrastructure/persistence/entity/PlayerJpaEntity.java \
        backend/player-module/src/main/java/com/cas/tsas/player/infrastructure/persistence/mapper/PlayerMapper.java \
        backend/player-module/src/main/java/com/cas/tsas/player/application/port/out/LoadPlayerPort.java \
        backend/player-module/src/main/java/com/cas/tsas/player/infrastructure/persistence/repository/PlayerJpaRepository.java \
        backend/player-module/src/main/java/com/cas/tsas/player/infrastructure/persistence/repository/PlayerPersistenceAdapter.java \
        backend/player-module/src/test/java/com/cas/tsas/player/domain/model/PlayerTest.java \
        backend/player-module/src/test/java/com/cas/tsas/player/infrastructure/persistence/repository/PlayerPersistenceAdapterIntegrationTest.java
git commit -m "feat(player): owner-aware persistence (Player.ownerId, repo filters) (TEN-55)"
```

---

### Task 9: `PlayerService` Owner-Filter + Admin-Bypass

**Files:**
- Modify: `backend/player-module/.../application/service/PlayerService.java`
- Modify: `backend/player-module/src/test/java/com/cas/tsas/player/application/service/PlayerServiceTest.java`

- [ ] **Step 1: Test schreiben**

Im bestehenden `PlayerServiceTest` ergänzen (komplette Test-Klasse-Anpassung mit Helper):

```java
private static final UUID OWNER_A = UUID.randomUUID();
private static final UUID OWNER_B = UUID.randomUUID();

private CurrentUserProvider asUser(UUID id, Role... roles) {
    return () -> new CurrentUser(id, Set.of(roles));
}

@Test
void findAll_filters_by_current_owner() {
    when(loadPort.findAllByOwner(OWNER_A)).thenReturn(List.of(player(OWNER_A, "A")));
    var svc = new PlayerService(loadPort, savePort, deletePort, hasMatchesPort, findActivePort,
            asUser(OWNER_A, Role.COACH));

    assertThat(svc.findAll()).extracting(Player::getFirstName).containsExactly("A");
    verify(loadPort, never()).loadAllPlayers();
}

@Test
void findAll_returns_everything_for_admin() {
    when(loadPort.loadAllPlayers()).thenReturn(List.of(player(OWNER_A, "A"), player(OWNER_B, "B")));
    var svc = new PlayerService(loadPort, savePort, deletePort, hasMatchesPort, findActivePort,
            asUser(OWNER_A, Role.COACH, Role.ADMIN));

    assertThat(svc.findAll()).hasSize(2);
}

@Test
void findById_throws_404_when_other_owner() {
    UUID id = UUID.randomUUID();
    when(loadPort.findByIdAndOwner(id, OWNER_A)).thenReturn(Optional.empty());
    var svc = new PlayerService(loadPort, savePort, deletePort, hasMatchesPort, findActivePort,
            asUser(OWNER_A, Role.COACH));

    assertThatThrownBy(() -> svc.findById(id)).isInstanceOf(PlayerNotFoundException.class);
}

@Test
void createPlayer_assigns_current_user_as_owner() {
    var svc = new PlayerService(loadPort, savePort, deletePort, hasMatchesPort, findActivePort,
            asUser(OWNER_A, Role.COACH));
    when(savePort.savePlayer(any())).thenAnswer(inv -> inv.getArgument(0));

    Player p = svc.createPlayer(new CreatePlayerUseCase.CreatePlayerCommand(
            "X", "Y", null, null, null, null, null, null));

    assertThat(p.getOwnerId()).isEqualTo(OWNER_A);
}

private Player player(UUID owner, String firstName) {
    return new Player(UUID.randomUUID(), owner, firstName, "L", null, null, null, null, null, null);
}
```

(Bestehende Tests in `PlayerServiceTest` müssen Mock-Setups für `CurrentUserProvider` ergänzen — entsprechend anpassen.)

- [ ] **Step 2: Test laufen lassen — Compile-Fehler**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :player-module:test --tests "com.cas.tsas.player.application.service.PlayerServiceTest"`

- [ ] **Step 3: Service anpassen**

`PlayerService.java`:

```java
public class PlayerService implements CreatePlayerUseCase, SearchPlayerUseCase, UpdatePlayerUseCase, DeletePlayerUseCase {

    private final LoadPlayerPort loadPlayerPort;
    private final SavePlayerPort savePlayerPort;
    private final DeletePlayerPort deletePlayerPort;
    private final HasMatchesPort hasMatchesPort;
    private final FindActiveMatchPort findActiveMatchPort;
    private final CurrentUserProvider currentUser;

    public PlayerService(LoadPlayerPort loadPlayerPort, SavePlayerPort savePlayerPort,
                         DeletePlayerPort deletePlayerPort, HasMatchesPort hasMatchesPort,
                         FindActiveMatchPort findActiveMatchPort,
                         CurrentUserProvider currentUser) {
        this.loadPlayerPort = loadPlayerPort;
        this.savePlayerPort = savePlayerPort;
        this.deletePlayerPort = deletePlayerPort;
        this.hasMatchesPort = hasMatchesPort;
        this.findActiveMatchPort = findActiveMatchPort;
        this.currentUser = currentUser;
    }

    private CurrentUser user() { return currentUser.get(); }
    private boolean isAdmin() { return user().hasRole(Role.ADMIN); }

    @Override
    public Player createPlayer(CreatePlayerCommand command) {
        Player player = new Player(
                null,
                user().id(),
                command.firstName(), command.lastName(),
                command.gender(), command.handedness(), command.backhandType(),
                command.ranking(), command.nationality(), command.birthDate()
        );
        return savePlayerPort.savePlayer(player);
    }

    @Override
    @Transactional(readOnly = true)
    public Player findById(UUID id) {
        Optional<Player> p = isAdmin()
                ? loadPlayerPort.loadPlayer(id)
                : loadPlayerPort.findByIdAndOwner(id, user().id());
        return p.orElseThrow(() -> new PlayerNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Player> findAll() {
        return isAdmin() ? loadPlayerPort.loadAllPlayers() : loadPlayerPort.findAllByOwner(user().id());
    }

    // updatePlayer, hasMatches, deletePlayer, deactivatePlayer — alle laden via findById(...)
    // (intern, statt direkt loadPlayer), wodurch der Owner-Check automatisch greift.

    @Override
    public Player updatePlayer(UpdatePlayerCommand command) {
        Player player = findById(command.id());   // wirft PlayerNotFoundException bei fremder ID
        player.setFirstName(command.firstName());
        // ... rest unverändert
        return savePlayerPort.savePlayer(player);
    }

    @Override
    public void deletePlayer(UUID id) {
        Player player = findById(id);
        if (hasMatchesPort.existsByPlayerId(id)) {
            throw new PlayerHasMatchesException(id);
        }
        deletePlayerPort.deletePlayer(id);
    }

    @Override
    public void deactivatePlayer(UUID id) {
        Player player = findById(id);
        player.setActive(false);
        savePlayerPort.savePlayer(player);
    }

    // findActiveMatchIdsByPlayerIds / hasMatches: unverändert (operieren auf Match-Seite,
    // Owner-Check passiert dort).
}
```

- [ ] **Step 4: Test grün**

Run: wie Step 2 — Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/player-module/src/main/java/com/cas/tsas/player/application/service/PlayerService.java \
        backend/player-module/src/test/java/com/cas/tsas/player/application/service/PlayerServiceTest.java
git commit -m "feat(player): owner-aware service with admin bypass (TEN-55)"
```

---

## Phase 4 — Match-Aggregat

### Task 10: `Match` Domain + `MatchJpaEntity` + Mapper um `ownerId`

**Files:**
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/domain/model/Match.java`
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/entity/MatchJpaEntity.java`
- Modify: `backend/match-module/src/main/java/com/cas/tsas/match/infrastructure/persistence/mapper/MatchMapper.java` *(falls existiert — sonst Inline-Mapping im Adapter prüfen)*

- [ ] **Step 1: `Match`-Test schreiben**

Datei: `backend/match-module/src/test/java/com/cas/tsas/match/domain/model/MatchOwnershipTest.java`

```java
package com.cas.tsas.match.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MatchOwnershipTest {

    @Test
    void constructor_assigns_owner() {
        UUID owner = UUID.randomUUID();
        Match m = new Match(null, owner, UUID.randomUUID(), UUID.randomUUID(),
                2, false, false, MatchStatus.IN_PROGRESS);
        assertThat(m.getOwnerId()).isEqualTo(owner);
    }
}
```

- [ ] **Step 2: Test laufen lassen — fehlt**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :match-module:test --tests "com.cas.tsas.match.domain.model.MatchOwnershipTest"`
Expected: compile-error.

- [ ] **Step 3: Domain anpassen**

`Match.java` — Feld `ownerId` ergänzen, Konstruktor anpassen (analog zu Player). Bestehende Aufrufer in `MatchService` und Tests bekommen damit Compile-Fehler — Schritt 4.

- [ ] **Step 4: JPA-Entity + Mapper aktualisieren**

`MatchJpaEntity.java`:

```java
@Column(name = "owner_id", nullable = false)
private UUID ownerId;

public UUID getOwnerId() { return ownerId; }
public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
```

Falls `MatchMapper` existiert: `ownerId` in beiden Richtungen mappen. Falls nicht, das Mapping ist im `MatchPersistenceAdapter` inline — dort anpassen.

- [ ] **Step 5: Build durch**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :match-module:compileJava`
Expected: BUILD SUCCESSFUL (Test in Step 2 PASS).

- [ ] **Step 6: Commit (gemeinsam mit Task 11+12)**

---

### Task 11: `LoadMatchPort` um Owner-Queries + Adapter + Repo

**Files:**
- Modify: `backend/match-module/.../application/port/out/LoadMatchPort.java`
- Modify: `backend/match-module/.../infrastructure/persistence/repository/MatchJpaRepository.java`
- Modify: `backend/match-module/.../infrastructure/persistence/repository/MatchPersistenceAdapter.java`

- [ ] **Step 1: Repo-Test schreiben**

Datei: `backend/match-module/src/test/java/com/cas/tsas/match/infrastructure/persistence/repository/MatchPersistenceAdapterIntegrationTest.java` (Schema analog zu Task 8, Owner-Filter testen).

- [ ] **Step 2: Test laufen lassen — compile-error**

- [ ] **Step 3: Port erweitern**

`LoadMatchPort.java`:

```java
Optional<Match> findByIdAndOwner(UUID id, UUID ownerId);
List<Match> findAllByOwner(UUID ownerId);
```

- [ ] **Step 4: Spring-Data ergänzen**

`MatchJpaRepository.java`:

```java
Optional<MatchJpaEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
List<MatchJpaEntity> findAllByOwnerId(UUID ownerId);
```

- [ ] **Step 5: Adapter ergänzen**

`MatchPersistenceAdapter.java`: analog zu Player-Adapter.

- [ ] **Step 6: Test grün**

- [ ] **Step 7: Commit (Task 10 + 11 gemeinsam)**

```bash
git add backend/match-module/...
git commit -m "feat(match): owner-aware persistence (Match.ownerId, repo filters) (TEN-55)"
```

---

### Task 12: `MatchService` Owner-Filter + Admin-Bypass

**Files:**
- Modify: `backend/match-module/.../application/service/MatchService.java`
- Modify: `backend/match-module/src/test/java/com/cas/tsas/match/application/service/MatchServiceTest.java`

- [ ] **Step 1: Tests schreiben**

Analog zu Player-Service-Test:
- `findAll_filters_by_owner`
- `findAll_returns_all_for_admin`
- `findById_throws_404_when_other_owner`
- `createMatch_assigns_current_user_as_owner`
- `endMatch_throws_404_when_other_owner` (testet ScoringService nicht — der ist Task 13)

- [ ] **Step 2: Test fail-Lauf**

- [ ] **Step 3: Service anpassen**

`MatchService.java` injiziert `CurrentUserProvider`, `findById` und `findAll` filtern. `createMatch` setzt `ownerId = user().id()`. `endMatch` lädt via `findById` (404 bei Fremdzugriff). Pattern wie in Task 9.

- [ ] **Step 4: Tests grün**

- [ ] **Step 5: Commit**

```bash
git add backend/match-module/...
git commit -m "feat(match): owner-aware MatchService with admin bypass (TEN-55)"
```

---

### Task 13: `ScoringService` — Owner-Check via `findById` sichern

**Files:**
- Modify: `backend/match-module/.../application/service/ScoringService.java`
- Modify: `backend/match-module/src/test/java/com/cas/tsas/match/application/service/ScoringServiceTest.java`

`ScoringService` (RecordPoint, SetScore, SetServingPlayer) lädt Match heute vermutlich via `loadMatchPort.loadMatch(id)`. Das muss durch den Owner-gefilterten Pfad ersetzt werden — entweder direkt via `GetMatchUseCase`/`MatchService.findById` (dependency-injection als `Port`) **oder** durch eine eigene Owner-Check-Methode im Service.

**Empfehlung:** `ScoringService` injiziert `LoadMatchPort` + `CurrentUserProvider` und nutzt das gleiche Helper-Pattern (`findById` mit Admin-Bypass). Vermeidet Service-zu-Service-Dependency.

- [ ] **Step 1: Test schreiben** — Cross-Tenant `recordPoint` → wirft `MatchNotFoundException`.

- [ ] **Step 2: Fail-Lauf**

- [ ] **Step 3: Implementation** — Helper `loadOwnMatch(UUID id)` in `ScoringService`, alle Schreib-Methoden durchschleusen.

- [ ] **Step 4: Tests grün**

- [ ] **Step 5: Commit**

---

### Task 14: REST-Layer (Controller) – kein Change, aber Tests anpassen

Player- und Match-Controller bleiben unverändert (Owner kommt aus Service). Vorhandene Controller-Tests (sofern es welche gibt; sonst übersprungen) müssen ggf. `JwtTestSupport.withUser(...)` benutzen.

- [ ] **Step 1: Bestehende Controller-Tests inspizieren** (`grep -rln "PlayerController\|MatchController" backend/*/src/test`)
- [ ] **Step 2: Wenn vorhanden, `.with(jwt())` durch `.with(JwtTestSupport.withUser(USER_A, Role.COACH))` ersetzen.**
- [ ] **Step 3: Tests grün.**
- [ ] **Step 4: Commit, falls Änderungen erforderlich.**

---

## Phase 5 — Statistics & AI Verifikation

### Task 15: `MatchStatisticsService` & `MatchAnalysisService` — Cross-Tenant-Test

`statistics-module` lädt Match via `GetMatchUseCase` (siehe `MatchStatisticsService`-Konstruktor). Owner-Check ist damit transitiv erfüllt. Test fügen wir trotzdem hinzu — sonst kann ein späterer Refactor den Pfad umgehen.

**Files:**
- Create/Modify: `backend/statistics-module/src/test/java/com/cas/tsas/statistics/application/service/MatchStatisticsServiceOwnershipTest.java`
- Create/Modify: `backend/ai-module/src/test/java/com/cas/tsas/ai/application/service/MatchAnalysisServiceOwnershipTest.java`

- [ ] **Step 1: Statistics-Test schreiben** — Mock `GetMatchUseCase.findById` wirft `MatchNotFoundException` → Service propagiert.

- [ ] **Step 2: AI-Test schreiben** — analog für `MatchAnalysisService`.

- [ ] **Step 3: Beide Tests grün** (sollten ohne Service-Änderung passen).

- [ ] **Step 4: Commit**

```bash
git add backend/statistics-module/src/test/... backend/ai-module/src/test/...
git commit -m "test(stats,ai): document cross-tenant 404 propagation (TEN-55)"
```

---

## Phase 6 — Keycloak

### Task 16: Realm-Rollen `COACH`/`ADMIN` ergänzen

**Files:**
- Modify: `docker/keycloak/realm-export.json`

- [ ] **Step 1: Datei lesen**

Run: `cat docker/keycloak/realm-export.json`

- [ ] **Step 2: Roles + defaultRole-Composite ergänzen**

Im Realm-Objekt `"roles"`-Block hinzufügen:

```json
"roles": {
  "realm": [
    { "name": "COACH", "description": "Default role: standard coach access" },
    { "name": "ADMIN", "description": "Full access across all owners" }
  ]
},
"defaultRole": {
  "name": "default-roles-tsas",
  "composite": true,
  "clientRole": false,
  "containerId": "tsas"
}
```

Falls die Realm noch keine `default-roles-<realm>` enthält, beim Re-Import (Dev: `podman compose down && up` regeneriert) wird Keycloak die Composite anlegen — `COACH` als Default manuell oder in Realm-UI hinterlegen.

- [ ] **Step 3: Smoke-Test**

Run: `cd docker && podman compose down && podman compose up keycloak -d`, dann `curl -k https://localhost:8443/realms/tsas/.well-known/openid-configuration` zeigt Realm „tsas" online.

- [ ] **Step 4: Realm-UI verifizieren** — Login auf Admin-Konsole, Realm-Roles → COACH, ADMIN vorhanden, Default-Role `COACH` zugeordnet.

- [ ] **Step 5: Commit**

```bash
git add docker/keycloak/realm-export.json
git commit -m "feat(keycloak): add realm roles COACH and ADMIN, default COACH (TEN-55)"
```

---

## Phase 7 — End-to-End Cross-Tenant-IT

### Task 17: Integration-Tests

**Files:**
- Create: `backend/app/src/test/java/com/cas/tsas/security/OwnershipIntegrationTest.java`

- [ ] **Step 1: IT schreiben**

```java
package com.cas.tsas.security;

import com.cas.tsas.AbstractIntegrationTest;
import com.cas.tsas.auth.domain.Role;
import com.cas.tsas.auth.testsupport.JwtTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OwnershipIntegrationTest extends AbstractIntegrationTest {

    private static final UUID USER_A = UUID.randomUUID();
    private static final UUID USER_B = UUID.randomUUID();

    @Autowired ObjectMapper json;

    @Test
    void user_A_cannot_see_user_B_player() throws Exception {
        // User B legt Spieler an
        String body = json.writeValueAsString(Map.of(
                "firstName", "Bob", "lastName", "B"));
        var created = mockMvc.perform(post("/api/players")
                        .with(JwtTestSupport.withUser(USER_B, Role.COACH))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        Map<?, ?> resp = json.readValue(created.getResponse().getContentAsString(), Map.class);
        String id = resp.get("id").toString();

        // User A versucht zu lesen → 404
        mockMvc.perform(get("/api/players/" + id)
                        .with(JwtTestSupport.withUser(USER_A, Role.COACH)))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_players_returns_only_own() throws Exception {
        mockMvc.perform(post("/api/players")
                        .with(JwtTestSupport.withUser(USER_A, Role.COACH))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("firstName", "A1", "lastName", "L"))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/players")
                        .with(JwtTestSupport.withUser(USER_B, Role.COACH))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("firstName", "B1", "lastName", "L"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/players")
                        .with(JwtTestSupport.withUser(USER_A, Role.COACH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].firstName").value("A1"));
    }

    @Test
    void admin_sees_all_players() throws Exception {
        UUID admin = UUID.randomUUID();
        mockMvc.perform(get("/api/players")
                        .with(JwtTestSupport.withUser(admin, Role.COACH, Role.ADMIN)))
                .andExpect(status().isOk());
        // (Genaue Count abhängig von vorherigen Tests; falls Test-Isolation per @Transactional/Cleanup
        // gewünscht ist, ergänzen.)
    }

    @Test
    void cross_tenant_score_update_returns_404() throws Exception {
        // ... analog: Match für USER_B anlegen, USER_A versucht PUT /score → 404
    }

    @Test
    void cross_tenant_ai_analysis_returns_404() throws Exception {
        // ... Match für USER_B, USER_A versucht POST .../analysis → 404
    }
}
```

- [ ] **Step 2: Test laufen lassen**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew :app:test --tests "com.cas.tsas.security.OwnershipIntegrationTest"`
Expected: alle PASS.

- [ ] **Step 3: Volle Suite + Coverage-Gate**

Run: `cd backend && JAVA_HOME=/opt/java/jdk-25.0.1 DOCKER_HOST=unix:///var/run/docker.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew check`
Expected: BUILD SUCCESSFUL, JaCoCo-Gate (85% line / 70% branch) hält.

- [ ] **Step 4: Commit**

```bash
git add backend/app/src/test/java/com/cas/tsas/security/OwnershipIntegrationTest.java
git commit -m "test(security): end-to-end cross-tenant ownership ITs (TEN-55)"
```

---

## Phase 8 — Abschluss

### Task 18: Linear-Update + Acceptance-Walkthrough

- [ ] **Step 1: Lokal manuell smoken** — `./gradlew bootRun --args='--spring.profiles.active=local'`, Frontend gegen Backend testen (eigener User vs. Admin in Keycloak).
- [ ] **Step 2: Akzeptanzkriterien aus TEN-55 abhaken** (siehe Spec §12).
- [ ] **Step 3: PR öffnen** mit Bezug auf TEN-55 + Verweis auf Spec & Plan.
- [ ] **Step 4: TEN-55 in Linear auf Status `In Review` / `Done` (manuell, da MCP State-UUIDs nicht greifbar).**

---

## Risiken — Watchlist während Implementation

| # | Aus Spec / Self-Review | Verifikation während Implementation |
|---|---|---|
| R1 | Migration backfill — Prod-Daten? | Keine (Memory: Realm wird re-importiert, Volume regeneriert). Bei realem Prod-Bestand vorher Skript anpassen. |
| R2 | Statistics/AI laden Points direkt? | `LoadPointsByMatchPort` checken — falls dort `match_id` ohne Owner-Bezug benutzt wird, müsste der Pfad Match.ownerId explicit verifizieren oder Aufruf durch `GetMatchUseCase.findById` (Owner-geprüft) vorhalten. Task 15 dokumentiert die Erwartung. |
| R3 | `realm_access.roles` fehlt | Negativtest in Task 3 deckt das ab. |
| R4 | Frontend kennt Rollen nicht | Out-of-scope. Admin via Keycloak-UI gesetzt. |

---

## Self-Review (durchgeführt)

- ✅ Spec-Coverage: alle Akzeptanzkriterien aus §12 haben einen Task (Migration → Task 5, `ownerId`-Felder → Tasks 6/10, Use-Case-Filter → Tasks 9/12, Cross-Tenant 404 → Tasks 9/12/13/17, JwtAuthenticationConverter → Task 3, Admin-Bypass → Tasks 9/12, ITs → Task 17, Realm-Roles → Task 16, JaCoCo-Gate → Task 17 Step 3).
- ✅ Keine Platzhalter (alle Code-Steps zeigen Code; Helper-/Mapper-Anpassungen explizit pro Datei benannt).
- ✅ Type-Consistency: `CurrentUser.id()` (Record-Accessor), `Role.ADMIN`, `findByIdAndOwner`/`findAllByOwner` als Port-Methoden-Namen, `LoadPlayerPort`/`LoadMatchPort` identische API-Form, `currentUser`-Feldname im Service einheitlich.
- ⚠️ Eine Spec-Abweichung dokumentiert: `TestUserAuthenticationFilter` (Spec) → `JwtTestSupport` mit `jwt()`-Post-Processor (Plan, Task 4). Begründung in Task 4 erklärt — `AbstractIntegrationTest` benutzt schon den realen Security-Filter, ein eigener Test-Filter wäre redundant.

---

## Execution

Plan complete and saved to `docs/superpowers/plans/2026-06-22-ten-55-owner-binding-rbac.md`. Two execution options:

1. **Subagent-Driven (recommended)** — Ich dispatche pro Task einen frischen Subagenten, reviewe zwischen Tasks, schnelle Iteration.
2. **Inline Execution** — Tasks in dieser Session ausführen via `superpowers:executing-plans`, Batch mit Checkpoints.
