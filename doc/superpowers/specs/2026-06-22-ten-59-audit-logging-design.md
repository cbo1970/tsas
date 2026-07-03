# TEN-59 — Audit-Logging via JPA AuditingEntityListener (Design)

**Datum:** 2026-06-22
**Ticket:** [TEN-59](https://linear.app/tennis-score-and-statistic/issue/TEN-59)
**STRIDE-Befunde:** R1, R2 (siehe `doc/sad/TSaS_STRIDE_Threat_Analysis.md`)
**Scope:** Ein PR / ein Implementierungsschritt.
**Folgt auf:** TEN-55 (Owner-Bindung). Audit-Felder kommen auf bestehende Aggregate.

---

## 1. Problem

Schreiboperationen (`createPlayer`, `updatePlayer`, `recordPoint`, `setScore`, `endMatch`, `deactivatePlayer`, `deletePlayer`) hinterlassen aktuell keine `who/when`-Spur. Folgen:

- Streitfälle („wer hat den Score überschrieben?") nicht aufklärbar.
- Compliance-/Forensik-Anforderungen nicht erfüllbar.
- Kein Operations-Diagnose-Trace bei Datenanomalien.

## 2. Ziel

Auf jedem schreibbaren Aggregat (`Player`, `Match`, `Point`) sind ab sofort `created_at`, `created_by`, `updated_at`, `updated_by` automatisch befüllt. Korrelation über Anfragegrenzen hinweg via MDC `correlationId` in jeder Log-Zeile.

## 3. Designentscheidungen

| # | Entscheidung | Begründung |
|---|---|---|
| D1 | Audit-Felder **nur auf JPA-Entity-Layer**, nicht im Domain. | Audit ist Persistence/Compliance-Concern; Domain-Modell bleibt frameworkfrei. YAGNI: Use-Cases brauchen die Felder heute nicht. |
| D2 | Spalten **nullable**. | Scheduled-Jobs / Tools ohne Auth-Context dürfen NULL hinterlassen. Hibernate setzt sie automatisch bei neuen Inserts; Backfill bei Migration. |
| D3 | Migration-Backfill `created_by = owner_id`, `created_at = NOW()`. | Beste verfügbare Annäherung für Pre-Migration-Daten (Dev-Bestand). Prod startet leer → kein Backfill nötig. |
| D4 | `updated_by` kann sich von `created_by`/`owner_id` unterscheiden. | Admin-Bypass aus TEN-55: Admin X kann Daten von Owner Y ändern → wir wollen wissen, dass Admin X die Änderung gemacht hat. |
| D5 | MDC `correlationId` Servlet-Filter im `common-module`. | Cross-cutting; ein Filter im common-module ist einfacher als pro-Service-Logging-Code. |
| D6 | Audit-Felder **nicht** in API-Responses sichtbar (vorerst). | Out-of-Scope; separates Frontend-Ticket bei Bedarf. |

## 4. Architektur

### 4.1 AuditorAware + Config

**Neu in `auth-module/infrastructure/persistence/`:**

```
auth-module/
  infrastructure/persistence/
    JpaAuditingConfig.java          # @EnableJpaAuditing, AuditorAware bean
    CurrentUserAuditor.java         # AuditorAware<UUID> impl
```

- `JpaAuditingConfig` annotiert mit `@Configuration` und `@EnableJpaAuditing(auditorAwareRef = "currentUserAuditor")`.
- `CurrentUserAuditor implements AuditorAware<UUID>`:
  ```java
  public Optional<UUID> getCurrentAuditor() {
      try {
          return Optional.of(currentUserProvider.get().id());
      } catch (IllegalStateException e) {
          return Optional.empty(); // no auth context (Flyway, scheduled jobs, tests w/o user)
      }
  }
  ```

### 4.2 JPA-Entity-Annotationen

Auf `PlayerJpaEntity`, `MatchJpaEntity`, `PointJpaEntity`:

```java
@Entity
@EntityListeners(AuditingEntityListener.class)
public class PlayerJpaEntity {
    // ... existing fields ...

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;
}
```

`updatable = false` auf `created_*` schützt vor versehentlichem Überschreiben.

### 4.3 Migration V7

```sql
-- V7__add_audit_columns.sql

ALTER TABLE players ADD COLUMN created_at TIMESTAMP;
ALTER TABLE players ADD COLUMN created_by UUID;
ALTER TABLE players ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE players ADD COLUMN updated_by UUID;

ALTER TABLE matches ADD COLUMN created_at TIMESTAMP;
ALTER TABLE matches ADD COLUMN created_by UUID;
ALTER TABLE matches ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE matches ADD COLUMN updated_by UUID;

ALTER TABLE points  ADD COLUMN created_at TIMESTAMP;
ALTER TABLE points  ADD COLUMN created_by UUID;
ALTER TABLE points  ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE points  ADD COLUMN updated_by UUID;

-- Backfill: best-effort attribution from owner.
UPDATE players SET created_by = owner_id, created_at = NOW(), updated_at = NOW();
UPDATE matches SET created_by = owner_id, created_at = NOW(), updated_at = NOW();
UPDATE points p
   SET created_at = NOW(),
       updated_at = NOW(),
       created_by = (SELECT m.owner_id FROM matches m WHERE m.id = p.match_id);

-- Spalten bleiben nullable; keine NOT NULL constraint.
```

H2- und Postgres-kompatibel. Indizes auf `created_at`/`updated_at` nicht nötig (keine Query-Pattern darauf).

### 4.4 MDC Correlation-ID

**Neu in `common-module/web/`:**

```
common-module/
  web/
    CorrelationIdFilter.java        # OncePerRequestFilter
    CorrelationIdConfig.java        # @Configuration registriert den Filter
```

- `CorrelationIdFilter extends OncePerRequestFilter`:
  - Header `X-Correlation-Id` lesen; wenn fehlt, `UUID.randomUUID()`.
  - `MDC.put("correlationId", id)`.
  - Response-Header `X-Correlation-Id` setzen (Echo).
  - Im `finally`: `MDC.remove("correlationId")`.
- `CorrelationIdConfig` registriert den Filter via `FilterRegistrationBean` (Order < Security-Filter).
- `logback-spring.xml` (oder `application.yml`-Pattern) erweitert um `[%X{correlationId:-}]`. Lokale Variante in `application-local.yml` reicht für Dev.

## 5. Tests

### Unit
- `CurrentUserAuditorTest`: liefert UUID aus Provider; liefert `Optional.empty()` wenn Provider `IllegalStateException` wirft.
- `CorrelationIdFilterTest`: Header durchgereicht; UUID generiert bei Abwesenheit; MDC nach Request aufgeräumt; Response-Header gesetzt.

### Integration
- `AuditingIT extends AbstractIntegrationTest`:
  - Player als `USER_A` per REST anlegen → DB-Zeile hat `created_by=USER_A`, `updated_by=USER_A`, `created_at` und `updated_at` ≈ jetzt.
  - Player als `ADMIN` updaten → DB-Zeile hat `created_by` unverändert, `updated_by=ADMIN`, `updated_at` > `created_at`.
  - Match als `USER_A` anlegen, Point als `USER_A` recorden → Point hat `created_by=USER_A`.
  - Cross-tenant Update als `USER_B` → schlägt mit 404 fehl (aus TEN-55); audit-Felder unverändert.

### Korrelations-Ende-zu-Ende
- IT verifiziert `X-Correlation-Id` durchgereicht via Response-Header.
- Log-Aussagen werden hier *nicht* assertiert (zu fragil); manueller Check beim Smoke-Test.

## 6. Touch-Points

| Modul | Dateien (neu / geändert) |
|---|---|
| `auth-module` | **neu:** `infrastructure/persistence/JpaAuditingConfig`, `infrastructure/persistence/CurrentUserAuditor`. Test: `CurrentUserAuditorTest`. |
| `common-module` | **neu:** `web/CorrelationIdFilter`, `web/CorrelationIdConfig`. Test: `CorrelationIdFilterTest`. |
| `player-module` | **geändert:** `PlayerJpaEntity` (`@EntityListeners` + 4 Felder). |
| `match-module` | **geändert:** `MatchJpaEntity`, `PointJpaEntity` (`@EntityListeners` + 4 Felder je). |
| `app` | **neu:** `db/migration/V7__add_audit_columns.sql`. Test: `AuditingIT`. **geändert:** `application-local.yml` logback-pattern (optional). |

`PlayerMapper` / `MatchMapper` werden **nicht** angefasst: Domain hat keine Audit-Felder.

## 7. YAGNI bewusst weggelassen

- Audit-Felder im Domain-Model.
- API-Exposure (PlayerResponse zeigt kein `lastModifiedBy`).
- Separater Audit-Trail in eigener Tabelle (alle historischen Werte) — Spring Envers wäre overkill.
- Hard-Delete-Tracking — Points werden hart gelöscht; das adressiert ein späteres „Soft-Delete-Pattern"-Ticket.
- Indizes auf `created_at`/`updated_at`.
- Frontend-Anzeige.

## 8. Risiken und Annahmen

| # | Risiko | Mitigation |
|---|---|---|
| R1 | `CurrentUserProvider.get()` wirft im Flyway-Migration-Thread (kein Auth-Context). | `CurrentUserAuditor` fängt `IllegalStateException` → `Optional.empty()`. Migration läuft mit NULL-Audit-Spalten (gewollt). |
| R2 | Tests die direkt JPA-Entities via Repository persistieren (ohne MockMvc-Request) haben keinen Auth-Context. | Audit-Felder bleiben NULL — OK für DataJpaTest-Slices. Falls dort konkrete Werte gebraucht: Test setzt manuell. |
| R3 | `updatable=false` auf `created_at`/`created_by` würde eine Backfill-Korrektur unmöglich machen. | Akzeptiert; Backfill passiert nur einmal in V7-Migration, danach append-only. |
| R4 | MDC-Leak bei async-Threads (z. B. `@Async`-Methoden). | TSaS hat heute keine `@Async`-Pfade. Wenn später eingeführt: `MdcContextAwareDelegatingExecutor` o. Ä. ergänzen. Ausdrücklich out-of-scope. |
| R5 | Spring AI / OpenAI-Adapter ruft Provider in eigenem Thread? | OpenAI-Adapter läuft synchron im Request-Thread — kein Thread-Hop, MDC bleibt. |

## 9. Akzeptanzkriterien (aus TEN-59, ggf. präzisiert)

- [ ] `@EnableJpaAuditing` aktiv via `JpaAuditingConfig`.
- [ ] `AuditorAware<UUID>`-Bean liest `sub` aus `CurrentUserProvider`; empty Optional bei fehlendem Context.
- [ ] `@EntityListeners(AuditingEntityListener.class)` auf `PlayerJpaEntity`, `MatchJpaEntity`, `PointJpaEntity` mit `@CreatedDate`/`@CreatedBy`/`@LastModifiedDate`/`@LastModifiedBy`.
- [ ] Migration V7 fügt die 4 Spalten je Tabelle hinzu, Backfill aus `owner_id`.
- [ ] Integration-Test belegt: Erstellt-/Geändert-Spalten korrekt befüllt; Admin-Update setzt `updated_by=ADMIN`, `created_by` bleibt.
- [ ] `CorrelationIdFilter` im `common-module`; MDC-Key `correlationId` gesetzt; Response-Header durchgereicht.
- [ ] Unit-Tests für `CurrentUserAuditor` und `CorrelationIdFilter`.
- [ ] JaCoCo-Gate hält (85 % line / 70 % branch).

## 10. Out-of-Scope (separate Tickets)

- Frontend-Anzeige „zuletzt geändert von X".
- Voller Audit-Trail (separate Tabelle mit allen Mutationen).
- Soft-Delete für Points.
- MDC-Propagation in `@Async`-Pfaden.
