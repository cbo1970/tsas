# Spring Boot 4 Migration: Implementierungsplan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Backend von Spring Boot 3.4.3 auf 4.0.6 migrieren, gleichzeitig Java release 21 → 25 und Keycloak Docker Image 26.0.7 → 26.6.1 anheben. Verifikation gegen lokal laufenden Stack (Keycloak + Postgres via Podman).

**Architecture:** Isolierte Arbeit in `feature/springboot-4` worktree. Sequenzielle Commits — Keycloak zuerst (unabhängig), dann SB4-Bump mit allen erforderlichen Code-Anpassungen, am Schluss Memory-Doku-Update. **Regel:** jeder Commit lässt `./gradlew build` grün — falls ein "Concern" Commit (Security, Persistence, Web, Test) nicht für sich genommen grün ist, werden seine Änderungen in den vorausgehenden grünen Commit gefaltet (Spec sagt: "Each commit must build green before the next begins" → das überschreibt die "Commit 2 may be red"-Erwartung).

**Tech Stack:** Spring Boot 4.0.6 / Spring Framework 7 / Spring Security 7 / Hibernate 7 / Jackson 3 / Jakarta EE 11 / Java 25 / Gradle 9.3.1 / Keycloak 26.6.1 / PostgreSQL 16

**Reference spec:** `docs/superpowers/specs/2026-05-15-springboot-4-migration-design.md`
**Linear issue:** [TEN-5](https://linear.app/tennis-score-and-statistic/issue/TEN-5)

---

## Wichtige Migration-Befunde (aus Boot 4 Release Notes)

Diese Punkte sind bestätigt aus dem [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide) und müssen im Code adressiert werden:

| Bereich | Bekannte Breaking Change |
|---|---|
| **Jackson** | Boot 4 zieht Jackson **3** (Default). Group IDs: `com.fasterxml.jackson` → `tools.jackson`. `@JsonComponent` → `@JacksonComponent`. Fallback: `spring-boot-jackson2` Modul vorhanden, wenn wir bei 2.x bleiben wollen. |
| **Flyway** | Direkte `org.flywaydb:flyway-core` Abhängigkeit reicht nicht mehr — Boot 4 verlangt **`spring-boot-starter-flyway`** (gleiches Schema für Liquibase). |
| **Tests** | `@MockBean`/`@SpyBean` entfernt → **`@MockitoBean`/`@MockitoSpyBean`**. `@SpringBootTest` liefert MockMVC/WebClient/TestRestTemplate **nicht mehr automatisch** → explizit hinzufügen. `MockitoTestExecutionListener` entfernt. |
| **Undertow** | Nicht mehr unterstützt (Servlet 6.1). Betrifft uns nicht — wir nutzen Tomcat. |
| **BootstrapRegistry** | Umgezogen `org.springframework.boot` → `org.springframework.boot.bootstrap`. Betrifft uns nicht — wir nutzen keinen Bootstrap. |
| **DevTools LiveReload** | Default `false`. Nicht relevant — wir nutzen keine DevTools. |
| **Java/Kotlin/GraalVM** | Java 17+, Kotlin 2.2+, GraalVM 25+. Wir sind ab jetzt auf Java 25. |
| **Spring Framework 7 / Security 7** | Keine Pflicht-Breaking-Changes für unser Resource-Server-DSL — bestätigt durch Security-7-Migration-Doku. |

---

## Dateiübersicht

| Aktion | Pfad |
|--------|------|
| Ändern | `docker/compose.yml` |
| Ändern | `backend/build.gradle.kts` |
| Ändern | `backend/app/build.gradle.kts` |
| Prüfen / ggf. ändern | `backend/auth-module/src/main/java/com/cas/tsas/auth/infrastructure/security/SecurityConfig.java` |
| Prüfen / ggf. ändern | `backend/auth-module/src/main/java/com/cas/tsas/auth/infrastructure/security/SecurityConfigLocal.java` |
| Prüfen / ggf. ändern | Alle `@Entity`-Klassen in `player-module` und `match-module` (Hibernate 7 ID-Generator-Verhalten) |
| Prüfen / ggf. ändern | Alle Controller + DTOs in `common-module`, `player-module`, `match-module` (Jackson 3 falls Migration, sonst Jackson-2-Fallback einrichten) |
| Prüfen / ggf. ändern | Alle Tests mit `@MockBean` / `@SpyBean` → `@MockitoBean` / `@MockitoSpyBean` |
| Ändern | `CLAUDE.md` |
| Ändern | `/Users/cbo/.claude/projects/-Users-cbo-Projects-cas-tsas/memory/MEMORY.md` |

---

## Task 0: Isolierten Worktree anlegen

**Files:** keine (Worktree-Operation)

- [ ] **Step 1: Worktree von `develop` aus erzeugen**

```bash
cd /Users/cbo/Projects/cas/tsas
git worktree add -b feature/springboot-4 ../tsas-sb4 develop
```

Expected: `Preparing worktree (new branch 'feature/springboot-4')` + `HEAD is now at <sha> ...`

- [ ] **Step 2: In Worktree wechseln**

```bash
cd /Users/cbo/Projects/cas/tsas-sb4
git status
```

Expected: `On branch feature/springboot-4` + `nothing to commit, working tree clean`.

- [ ] **Step 3: Baseline-Build im Worktree, vor Änderungen**

```bash
JAVA_HOME=/opt/java/jdk-25.0.1 ./backend/gradlew -p backend clean build
```

Expected: `BUILD SUCCESSFUL`. Dauer ~1–3 min. Diese Baseline beweist, dass `develop` im Worktree grün startet. Falls rot → STOP, vorher fixen.

---

## Task 1: Keycloak Docker Image bumpen (26.0.7 → 26.6.1)

**Files:**
- Modify: `docker/compose.yml:18`

- [ ] **Step 1: Image-Tag in compose.yml ändern**

```diff
-    image: quay.io/keycloak/keycloak:26.0.7
+    image: quay.io/keycloak/keycloak:26.6.1
```

Konkret: in `docker/compose.yml` Zeile 18 die Versionsnummer ersetzen.

- [ ] **Step 2: Neuen Image-Pull verifizieren**

```bash
cd /Users/cbo/Projects/cas/tsas-sb4/docker
podman pull quay.io/keycloak/keycloak:26.6.1
```

Expected: `Storing signatures` oder `Image is up to date`. Layer-Pull dauert ~30–60s.

- [ ] **Step 3: Bestehenden Keycloak-Container stoppen**

```bash
podman compose -f /Users/cbo/Projects/cas/tsas-sb4/docker/compose.yml down
```

Expected: Keycloak + DB-Container gestoppt.

- [ ] **Step 4: Mit neuem Image starten**

```bash
podman compose -f /Users/cbo/Projects/cas/tsas-sb4/docker/compose.yml up -d
```

Expected: zwei Container laufen (`tsas-postgres`, `keycloak`). Logs prüfen mit `podman logs keycloak --tail 50` — Realm-Import `tsas` muss erfolgen, keine ERROR-Lines.

- [ ] **Step 5: Smoke-Test Keycloak-Endpoints**

```bash
curl -k -s https://localhost:8443/realms/tsas/.well-known/openid-configuration | python3 -c "import json,sys; d=json.load(sys.stdin); print('issuer:', d['issuer']); print('jwks:', d['jwks_uri'])"
```

Expected:
```
issuer: https://localhost:8443/realms/tsas
jwks: https://localhost:8443/realms/tsas/protocol/openid-connect/certs
```

```bash
curl -s http://localhost:18080/realms/tsas/protocol/openid-connect/certs | head -c 200
```

Expected: JSON beginnt mit `{"keys":[`.

- [ ] **Step 6: Admin-UI erreichbar?**

`tsas-frontend` ist als public PKCE Client konfiguriert mit `directAccessGrantsEnabled: false` — kein einfacher `curl`-Token-Flow möglich. Stattdessen: Browser auf `https://localhost:8443/admin` mit `admin/admin`. Realm `tsas` muss vorhanden sein, Client `tsas-frontend` listed.

Expected: Login klappt, Realm sichtbar, Client-Liste enthält `tsas-frontend`. Damit ist bewiesen, dass das Realm-Export-File mit Keycloak 26.6.1 lädt und administrierbar ist. Der echte Token-Test passiert in Task 9 via Frontend-OAuth-Flow.

- [ ] **Step 7: Commit**

```bash
cd /Users/cbo/Projects/cas/tsas-sb4
git add docker/compose.yml
git commit -m "$(cat <<'EOF'
chore(docker): bump Keycloak 26.0.7 → 26.6.1

Patch-bump innerhalb von Major 26. Realm-Import und JWKS-Endpoint
gegen den neuen Container verifiziert.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

Expected: `[feature/springboot-4 <sha>] chore(docker): ...`

---

## Task 2: Spring Boot 3.4.3 → 4.0.6 + JDK 25 Bump

**Files:**
- Modify: `backend/build.gradle.kts`

- [ ] **Step 1: Plugin- und BOM-Version anheben, Java release auf 25**

In `backend/build.gradle.kts` exakt diese Änderungen:

```diff
 plugins {
-    id("org.springframework.boot") version "3.4.3" apply false
+    id("org.springframework.boot") version "4.0.6" apply false
     id("io.spring.dependency-management") version "1.1.7" apply false
 }
 
 subprojects {
     apply(plugin = "java")
     apply(plugin = "io.spring.dependency-management")
 
     configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
         imports {
-            mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.3")
+            mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.6")
         }
     }
 
     tasks.withType<JavaCompile> {
-        options.release = 21
+        options.release = 25
         options.compilerArgs.add("-parameters")
     }
```

- [ ] **Step 2: Build laufen lassen — wir wollen ein vollständiges Failure-Bild**

```bash
cd /Users/cbo/Projects/cas/tsas-sb4/backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew clean build --continue 2>&1 | tee /tmp/sb4-build.log
```

`--continue` erzwingt, dass alle Module versuchen zu kompilieren, statt beim ersten Fehler aufzuhören. Output wird gespeichert, damit wir ihn auswerten können.

Expected: höchstwahrscheinlich `BUILD FAILED`. Output enthält Hinweise auf:
- Fehlende Klassen aus `com.fasterxml.jackson.*` (Jackson 3 Migration)
- Fehlende `org.flywaydb.flyway-core` (Starter-Migration)
- Evtl. `@MockBean` deprecated/removed in Tests

- [ ] **Step 3: Failure-Klassifizierung**

Aus `/tmp/sb4-build.log` extrahieren:
```bash
grep -E "(error:|FAILED|Could not resolve|cannot find symbol|deprecated)" /tmp/sb4-build.log | sort -u | head -50
```

Ergebnis dient als Grundlage für die nächsten Tasks. Jede Kategorie wird in Task 3+ adressiert.

- [ ] **Step 4: NOCH NICHT COMMITTEN**

Begründung: Spec verlangt grünen Build pro Commit. Wir committen Task 2 erst, wenn nach Tasks 3–6 wieder grün. Bis dahin hängt der einzelne Diff lokal.

---

## Task 3: Flyway-Starter-Migration

**Files:**
- Modify: `backend/app/build.gradle.kts`

Boot 4 verlangt explizit `spring-boot-starter-flyway`, wenn man Flyway automatisch konfiguriert haben will. Aktuell nutzen wir `org.flywaydb:flyway-core` direkt — wahrscheinlich kommt aus Task 2 ein Fehler à la "Flyway not auto-configured" oder "ClassNotFoundException".

- [ ] **Step 1: Dependency umstellen**

In `backend/app/build.gradle.kts`:

```diff
     implementation("org.springframework.boot:spring-boot-starter-web")
     implementation("org.springframework.boot:spring-boot-starter-actuator")
-    implementation("org.flywaydb:flyway-core")
+    implementation("org.springframework.boot:spring-boot-starter-flyway")
     runtimeOnly("org.flywaydb:flyway-database-postgresql")
```

(`flyway-database-postgresql` bleibt als runtime-only Driver für den Postgres-Dialekt.)

- [ ] **Step 2: Build wiederholen**

```bash
cd /Users/cbo/Projects/cas/tsas-sb4/backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew :app:compileJava 2>&1 | tee -a /tmp/sb4-build.log
```

Expected: `:app:compileJava` grün. Falls weitere Flyway-bezogene Fehler → in Step 3 dieses Tasks adressieren.

---

## Task 4: Jackson-Strategie entscheiden

Die wichtigste Architektur-Entscheidung der Migration: **Jackson 3 mitmachen** (Code-Änderungen) oder **Jackson 2 Fallback** (`spring-boot-jackson2` Modul behalten) ?

Aktuell nutzen wir keine `@JsonComponent`, keine eigene `ObjectMapper`-Konfiguration, keine `SecurityJackson2Modules`. Standard-DTO-Serialisierung mit Spring-Default-Mapper. Daher ist Jackson 3 wahrscheinlich problemlos — wir haben keine direkten Touch-Points auf Jackson-APIs in unserem Code.

- [ ] **Step 1: Codebase auf Jackson-API-Nutzungen prüfen**

```bash
cd /Users/cbo/Projects/cas/tsas-sb4
grep -rn "com.fasterxml.jackson\|@JsonComponent\|ObjectMapper\|Jackson2ObjectMapperBuilderCustomizer\|JsonObjectSerializer" backend/ --include="*.java"
```

Expected: keine Treffer in `src/main/java` oder `src/test/java`. Falls Treffer → bei der Klasse + Methode notieren und in Step 2 migrieren.

- [ ] **Step 2: Bei keinen Treffern — Jackson 3 akzeptieren**

Keine Code-Änderung nötig. Jackson 3 wird transparent via BOM gezogen. Direkt zu Task 5 springen.

- [ ] **Step 3: Bei Treffern (Fallback Jackson 2)**

In `backend/build.gradle.kts` im subprojects-Block:

```diff
     dependencies {
         "testImplementation"("org.springframework.boot:spring-boot-starter-test")
+        "implementation"("org.springframework.boot:spring-boot-jackson2")
         "testImplementation"("org.testcontainers:junit-jupiter")
```

Damit bleibt Jackson 2 die Standard-Auto-Konfiguration und unsere Imports `com.fasterxml.jackson.*` funktionieren weiter.

- [ ] **Step 4: Bei Treffern — Build wiederholen**

```bash
cd /Users/cbo/Projects/cas/tsas-sb4/backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew compileJava
```

Expected: alle `:*:compileJava` grün.

---

## Task 5: Tests auf `@MockitoBean` migrieren

**Files:** alle Test-Dateien mit `@MockBean` oder `@SpyBean` (zu identifizieren)

- [ ] **Step 1: Treffer suchen**

```bash
cd /Users/cbo/Projects/cas/tsas-sb4
grep -rn "@MockBean\|@SpyBean" backend/ --include="*.java"
```

Falls keine Treffer → Task überspringen, direkt zu Task 6.

- [ ] **Step 2: Imports und Annotations ersetzen (replace_all sicher)**

Für jede Datei aus Step 1:

```diff
-import org.springframework.boot.test.mock.mockito.MockBean;
+import org.springframework.test.context.bean.override.mockito.MockitoBean;
...
-    @MockBean
+    @MockitoBean
     private SomeService service;
```

Analog `SpyBean` → `MockitoSpyBean`:
```diff
-import org.springframework.boot.test.mock.mockito.SpyBean;
+import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
```

- [ ] **Step 3: Test-Kompilation prüfen**

```bash
cd /Users/cbo/Projects/cas/tsas-sb4/backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew compileTestJava
```

Expected: alle `:*:compileTestJava` grün.

---

## Task 6: Hibernate 7 ID-Generator-Verhalten überprüfen

**Files:** alle `@Entity`-Klassen, primär in `match-module` und `player-module`

Hibernate 7 ist strenger mit `@GeneratedValue(strategy = GenerationType.AUTO)` — die abgeleitete Strategie kann sich ändern (z.B. von `IDENTITY` auf `SEQUENCE` für Postgres). Das kann gegen bestehende Flyway-Migrationen kollidieren.

- [ ] **Step 1: ID-Generator-Strategien im Code suchen**

```bash
cd /Users/cbo/Projects/cas/tsas-sb4
grep -rn "@GeneratedValue\|@SequenceGenerator\|@TableGenerator" backend/ --include="*.java"
```

Erwartet: in jeder `*JpaEntity.java` der Module steht typischerweise:
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

- [ ] **Step 2: Tests gegen H2 (`test`-Profil) laufen lassen**

```bash
cd /Users/cbo/Projects/cas/tsas-sb4/backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew test
```

Expected: alle Tests grün. Falls `IT`-Tests rot mit Hibernate-Fehlern à la "No generator named 'GENERATED'" oder Flyway-Konflikten → Step 3.

- [ ] **Step 3: Bei Hibernate-7-spezifischen Fehlern**

Häufige Fix-Patterns:
- `@GeneratedValue(strategy = GenerationType.AUTO)` → explizit `IDENTITY` (Postgres serial) oder `SEQUENCE` (mit definierter Sequence)
- Bei `@GeneratedValue` ohne Strategie: explizit `IDENTITY` setzen, sonst Default-Drift
- Hibernate-7 erzwingt evtl. naming-strategy-Verhalten — falls Spalten anders heißen als erwartet, in `application.yml` festnageln:
  ```yaml
  spring:
    jpa:
      hibernate:
        naming:
          physical-strategy: org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy
  ```

Konkrete Änderung pro Entity dokumentieren beim Fix.

---

## Task 7: Spring Security 7 prüfen

**Files:**
- `backend/auth-module/src/main/java/com/cas/tsas/auth/infrastructure/security/SecurityConfig.java`
- `backend/auth-module/src/main/java/com/cas/tsas/auth/infrastructure/security/SecurityConfigLocal.java`

Aktueller Code nutzt schon Lambda-DSL — voraussichtlich nichts zu tun.

- [ ] **Step 1: Tests im `local`-Pfad als IT prüfen (oder bewusst überspringen)**

Wenn kein IT existiert, der Security mit JWT testet, bleibt der Final-Gate (Task 9) der einzige Verifier dafür.

```bash
cd /Users/cbo/Projects/cas/tsas-sb4/backend
grep -rn "JwtDecoder\|@WithMockUser\|@WithJwt" --include="*.java" .
```

- [ ] **Step 2: Wenn `compileJava` für `auth-module` grün ist, in diesem Schritt nichts machen**

Konkret: Spring Security 7 hält API-Kompatibilität für `authorizeHttpRequests`, `oauth2ResourceServer().jwt(Customizer.withDefaults())`, `csrf().disable()`, `cors(Customizer.withDefaults())`. Final-Gate (Task 9) prüft echte Funktionalität.

---

## Task 8: Vollbuild + Commit der SB4-Migration

**Files:** alle bisher in Tasks 2–7 geänderten Dateien

- [ ] **Step 1: Full clean build**

```bash
cd /Users/cbo/Projects/cas/tsas-sb4/backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew clean build
```

Expected: `BUILD SUCCESSFUL`. Falls rot → in das relevante Task (3–7) zurück.

- [ ] **Step 2: Diff sichten**

```bash
cd /Users/cbo/Projects/cas/tsas-sb4
git diff --stat
git diff backend/build.gradle.kts backend/app/build.gradle.kts
```

- [ ] **Step 3: Stage und Commit (alles zusammen)**

```bash
cd /Users/cbo/Projects/cas/tsas-sb4
git add backend/build.gradle.kts backend/app/build.gradle.kts
# Falls Code-Änderungen in Tasks 3-7 nötig waren, diese hier auch addieren:
# git add backend/auth-module/... backend/player-module/... backend/match-module/...
git status
```

- [ ] **Step 4: Commit-Message zusammenstellen**

Wähle pro Concern aus den vorausgehenden Tasks: behalten oder streichen. Beispiel-Message-Skelett (Bullet löschen, wenn der Concern keine Änderung brauchte):

```bash
git commit -m "$(cat <<'EOF'
feat(deps): migrate to Spring Boot 4.0.6 + JDK release 25

* BOM und Plugin auf Spring Boot 4.0.6
* Java release-target 21 → 25 (Boot-4 Gradle plugin liest jetzt JDK-25 class files)
* Flyway: org.flywaydb:flyway-core → spring-boot-starter-flyway
* Jackson 3 ohne Code-Änderungen übernommen (keine direkten Touchpoints auf Jackson-API)
* Tests: @MockBean/@SpyBean → @MockitoBean/@MockitoSpyBean
* Hibernate 7: keine Änderungen an Entity-Generatoren nötig
* Spring Security 7: keine Änderungen nötig (Lambda-DSL bereits modern)

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

Falls Jackson-2-Fallback gewählt wurde (Task 4 Step 3): Bullet ersetzen durch
`* Jackson: 2.x-Fallback via spring-boot-jackson2 (Code nutzt com.fasterxml.jackson direkt)`.

Falls Hibernate-Anpassungen nötig waren (Task 6 Step 3): Bullet konkretisieren mit Klassen-Namen, z.B.
`* Hibernate 7: PlayerJpaEntity/MatchJpaEntity ID-Generator von AUTO auf IDENTITY explizit gesetzt`.

---

## Task 9: Final Gate (lokaler Stack)

**Files:** keine

- [ ] **Step 1: Stack hochfahren**

```bash
cd /Users/cbo/Projects/cas/tsas-sb4
podman compose -f docker/compose.yml up -d
# Status prüfen
podman ps
```

Expected: 2 Container laufen, `healthy` (Postgres) bzw. `running` (Keycloak).

- [ ] **Step 2: Backend mit `local` Profil starten**

In separatem Terminal (oder background mit logging):

```bash
cd /Users/cbo/Projects/cas/tsas-sb4/backend
JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew bootRun --args='--spring.profiles.active=local' 2>&1 | tee /tmp/sb4-bootrun.log
```

Expected: in den Logs erscheint `Started TsasBackendApplication in <N> seconds`. Keine `ERROR`-Lines bis dahin.

- [ ] **Step 3: Health-Endpoint**

```bash
curl -k -s https://localhost:8080/actuator/health
```

Expected: `{"status":"UP"}` (mindestens).

- [ ] **Step 4: JWT holen**

Da `tsas-frontend` kein Direct-Access-Grant erlaubt, zwei Optionen:

**Option A (empfohlen): Frontend hochfahren und Login**
```bash
cd /Users/cbo/Projects/cas/tsas-sb4/frontend
npm install && npm run start  # https://localhost:4200
```
Im Browser bei Keycloak einloggen (registrieren falls nötig — `registrationAllowed: true`), dann in den DevTools (Network/Storage) das `access_token` aus dem Session-State kopieren.

**Option B (schneller, einmaliger Admin-Eingriff): Direct Access Grant temporär aktivieren**
Im Keycloak-Admin (`https://localhost:8443/admin`, admin/admin) → Realm `tsas` → Clients → `tsas-frontend` → Settings → "Direct access grants" einschalten → Save. Danach:
```bash
TOKEN=$(curl -k -s -X POST https://localhost:8443/realms/tsas/protocol/openid-connect/token \
  -d "client_id=tsas-frontend" -d "grant_type=password" \
  -d "username=<TESTUSER>" -d "password=<TESTPASS>" -d "scope=openid" \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])")
echo "Token length: ${#TOKEN}"
```
Nach dem Test Direct Access Grant **wieder ausschalten** (Production-Default beibehalten). `<TESTUSER>` / `<TESTPASS>`: ein Realm-User; falls keiner existiert, vorher anlegen unter Users → Add user (+Set Password).

Expected: Token-Länge > 500.

- [ ] **Step 5: Authentifizierter API-Call**

```bash
curl -k -s -H "Authorization: Bearer $TOKEN" https://localhost:8080/api/players
```

Expected: HTTP 200 + JSON Array (leer ist OK, wenn keine Spieler existieren).

Mit nicht-authentifiziertem Call gegentesten:
```bash
curl -k -s -o /dev/null -w "%{http_code}\n" https://localhost:8080/api/players
```

Expected: `401`.

- [ ] **Step 6: Backend stoppen, Stack runter**

```bash
# bootRun mit Ctrl+C beenden
cd /Users/cbo/Projects/cas/tsas-sb4
podman compose -f docker/compose.yml down
```

---

## Task 10: Dokumentation / Memory aktualisieren

**Files:**
- Modify: `CLAUDE.md`
- Modify: `/Users/cbo/.claude/projects/-Users-cbo-Projects-cas-tsas/memory/MEMORY.md`

- [ ] **Step 1: `CLAUDE.md` aktualisieren**

In `CLAUDE.md` den Java-Block ersetzen:

```diff
-Java 25 is the installed JDK; source/target compatibility is set to Java 21.
+Java 25 is the installed JDK and the source/target compatibility (set via `options.release = 25`).
 Set `JAVA_HOME=/opt/java/jdk-25.0.1` before running Gradle commands.
```

(Falls weitere Stellen die alte Aussage referenzieren, im selben Schritt mit-fixen.)

- [ ] **Step 2: `MEMORY.md` aktualisieren**

In `~/.claude/projects/-Users-cbo-Projects-cas-tsas/memory/MEMORY.md`:

```diff
 ## Java / Build Environment
 - Java 25.0.1 installed at `/opt/java/jdk-25.0.1` (only JDK on this machine)
 - Must prefix Gradle commands: `JAVA_HOME=/opt/java/jdk-25.0.1 ./gradlew <task>`
-- Source/target compiled to Java 21 (`options.release = 21`) because Spring Boot 3.4.x's Gradle plugin ASM can't read Java 25 class files (major version 69)
+- Source/target compiled to Java 25 (`options.release = 25`). Boot 4's Gradle plugin handles JDK 25 class files.

 ## Backend
 - Location: `backend/` subdirectory
-- Spring Boot 3.4.3, Gradle 9.3.1 (Kotlin DSL wrapper)
+- Spring Boot 4.0.6, Gradle 9.3.1 (Kotlin DSL wrapper)
```

Und für Keycloak:

```diff
 ## Auth (Keycloak)
+- Image: `quay.io/keycloak/keycloak:26.6.1`
 - Realm: `tsas`, imported from `docker/keycloak/realm-export.json`
```

- [ ] **Step 3: Commit Dokumentations-Updates**

```bash
cd /Users/cbo/Projects/cas/tsas-sb4
git add CLAUDE.md
git commit -m "$(cat <<'EOF'
docs: update CLAUDE.md for Spring Boot 4.0.6 + Java 25

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

`MEMORY.md` lebt außerhalb des Repos — kein Git-Commit dafür. Speicher-Update reicht.

---

## Task 11: Push, PR, TEN-5 schließen

**Files:** keine

- [ ] **Step 1: Push**

```bash
cd /Users/cbo/Projects/cas/tsas-sb4
git push -u origin feature/springboot-4
```

Expected: Branch auf GitHub angelegt + Tracking gesetzt.

- [ ] **Step 2: PR erstellen**

```bash
gh pr create --base develop --head feature/springboot-4 --title "feat: migrate to Spring Boot 4.0.6 (TEN-5)" --body "$(cat <<'EOF'
## Summary
- Spring Boot 3.4.3 → 4.0.6
- Java release 21 → 25 (Boot 4 ASM handles JDK 25 class files)
- Keycloak Docker Image 26.0.7 → 26.6.1
- Flyway dependency `org.flywaydb:flyway-core` → `spring-boot-starter-flyway`
- Tests: `@MockBean`/`@SpyBean` → `@MockitoBean`/`@MockitoSpyBean` (oder weggelassen, falls keine Treffer existierten)

## Out of scope
- Frontend
- PostgreSQL major bump (stays on 16-alpine)
- Adoption of new SB4 features

## Final gate
- [x] `./gradlew build` green
- [x] `bootRun --spring.profiles.active=local` startet ohne ERROR
- [x] `/actuator/health` → 200
- [x] Authenticated `GET /api/players` mit Keycloak-26.6.1-JWT → 200
- [x] Unauthenticated → 401

Closes TEN-5.

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

- [ ] **Step 3: PR-URL festhalten und Review abwarten**

Nach Approval Squash-Merge im GitHub-UI oder per `gh pr merge --squash --delete-branch`.

- [ ] **Step 4: TEN-5 in Linear auf Done setzen**

State-UUID aus dem Memory: `7a784171-6b0b-40cd-b02f-63b86d733959` (Done für Team `Tennis_score_and_statistic`). Mit Linear-MCP `update_issue`:

```
issueId: 57a5373a-0704-453a-8837-5ac085d05da3
status:  7a784171-6b0b-40cd-b02f-63b86d733959
```

- [ ] **Step 5: Worktree aufräumen**

```bash
cd /Users/cbo/Projects/cas/tsas
git worktree remove ../tsas-sb4
git branch -d feature/springboot-4   # falls schon gemerged + lokal noch da
```

Expected: `Preparing worktree (removed)` + `Deleted branch feature/springboot-4`.

---

## Notfall-Rollback

Falls die Migration zu einem nicht-lösbaren Punkt kommt:

```bash
cd /Users/cbo/Projects/cas/tsas
git worktree remove --force ../tsas-sb4
git branch -D feature/springboot-4
```

TEN-5 in Linear zurück auf Backlog (`cf909d2d-d527-4710-ad8b-88f715c46ee5`). `develop` ist unangefasst.
