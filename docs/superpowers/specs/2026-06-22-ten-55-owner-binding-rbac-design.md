# TEN-55 — Owner-Bindung & RBAC für Player/Match/Point (Design)

**Datum:** 2026-06-22
**Ticket:** [TEN-55](https://linear.app/tennis-score-and-statistic/issue/TEN-55)
**STRIDE-Befunde:** E1, I1, T2, S5 (siehe `doc/sad/TSaS_STRIDE_Threat_Analysis.md`)
**Scope:** Ein PR / ein Implementierungsschritt.

---

## 1. Problem

Jeder authentifizierte Nutzer kann derzeit auf *alle* Player-, Match- und Point-Daten lesend und schreibend zugreifen. Es gibt keinen Ownership-Discriminator auf Aggregaten, keine Rollen-Prüfung und keine `@PreAuthorize`-Annotationen. Folgen:

- Beliebiges Lesen fremder Spielerdaten (DSGVO-relevant — Geburtsdatum, Nationalität, Ranking).
- Score- und Match-Manipulation fremder Begegnungen (`PUT /api/matches/{id}/score`, `POST .../end/walkover`).
- Löschen fremder Player (`DELETE /api/players/{id}`).

## 2. Ziel

**Vollständige Datenisolation pro Nutzer.** Coach A sieht und schreibt nur seine eigenen Player, Matches und Points. Admin sieht alles. Cross-Tenant-Zugriffe geben `404 Not Found` (Existenz wird verborgen).

## 3. Designentscheidungen (bestätigt)

| # | Entscheidung | Begründung |
|---|---|---|
| D1 | Vollständige Isolation (Reads + Writes pro Owner gefiltert). | Kollidiert mit TEN-7-Wortlaut „alle lesen, eigene schreiben", aber STRIDE I1/I2 priorisiert (DSGVO). |
| D2 | Cross-Tenant-HTTP-Status: **404**. | Existenz fremder Aggregate bleibt verborgen — passt zu „vollständiger Isolation". |
| D3 | Admin-Bypass im Service-Layer (kein `@PreAuthorize`). | Owner-Check ist semantisch und gehört in die Use-Case-Schicht; Rollen-Check nur für Bypass. |
| D4 | Test-Header-Filter (`X-Test-User-Id`, `X-Test-User-Roles`) im `test`-Profil als Auth-Stand-in. | Integration-Tests laufen weiter ohne Keycloak. |
| D5 | `Point` erbt Owner vom `Match` — keine eigene `owner_id`-Spalte. | Aggregat-Konsistenz: ein Point gehört nie zu einem anderen User als sein Match. |

## 4. Architektur

### 4.1 Auth-Bridge (neu im `auth-module`)

```
auth-module/
  application/port/in/         CurrentUserProvider (Port)
  domain/                      CurrentUser (Value: UUID id, Set<Role> roles)
                               Role (enum: COACH, ADMIN)
  infrastructure/security/     JwtCurrentUserProvider (SecurityContextHolder → CurrentUser)
                               SecurityConfig (JwtAuthenticationConverter mit realm_access.roles)
                               SecurityConfigLocal (test-Profil: Test-Header-Filter)
```

- `CurrentUser` ist ein **POJO** ohne Framework-Bezug — wird im Domain/Application-Layer benutzt.
- `CurrentUserProvider#get()` wirft `IllegalStateException`, wenn kein User im Context steht (Bug-Indikator, nicht Auth-Fehler — den hat schon der Security-Filter abgefangen).
- `JwtAuthenticationConverter` extrahiert `realm_access.roles` → `ROLE_COACH`, `ROLE_ADMIN` (Spring-Authorities).

### 4.2 Domain-Änderung

| Aggregat | Owner-Feld | Bemerkung |
|---|---|---|
| `Player` | `UUID ownerId` (Pflicht) | wird im `CreatePlayerCommand` mitgegeben |
| `Match` | `UUID ownerId` (Pflicht) | wird im `CreateMatchCommand` mitgegeben |
| `Point` | — | erbt Owner vom zugehörigen `Match` |

Owner wird **vom Service** aus `CurrentUserProvider` befüllt, nicht vom Controller. Damit bleibt der Controller ein dünner DTO-Mapper.

### 4.3 Migration V6

```sql
-- V6__add_owner_id.sql

-- Dev-time backfill: NUR Dev-Daten betroffen (Realm/Postgres-Volume wird im Dev re-erstellt).
-- Sollte in Prod kein Datenbestand existieren, ist dieser Backfill no-op.
ALTER TABLE players ADD COLUMN owner_id UUID;
ALTER TABLE matches ADD COLUMN owner_id UUID;

UPDATE players SET owner_id = '00000000-0000-0000-0000-000000000000' WHERE owner_id IS NULL;
UPDATE matches SET owner_id = '00000000-0000-0000-0000-000000000000' WHERE owner_id IS NULL;

ALTER TABLE players ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE matches ALTER COLUMN owner_id SET NOT NULL;

CREATE INDEX idx_players_owner ON players(owner_id);
CREATE INDEX idx_matches_owner ON matches(owner_id);
```

H2-/Postgres-kompatibel (`ALTER COLUMN ... SET NOT NULL`). Backfill-Owner ist dokumentiert.

## 5. Use-Case-Verhalten

### 5.1 Reads

| Methode | Verhalten |
|---|---|
| `findAll()` | filtert nach `ownerId = currentUser.id` (Admin: kein Filter) |
| `findById(id)` | lädt; wenn `ownerId ≠ currentUser.id` und kein Admin → wirft `NotFoundException` → **404** |
| `getScore(matchId)`, `findActiveMatchIdsByPlayerIds(...)` | erben Owner-Check des umschließenden Aggregats |

### 5.2 Writes

`update/delete/setScore/endMatch/recordPoint/setServingPlayer/deactivate` laden das Aggregat über `findById` (das wirft schon 404 bei Cross-Tenant), dann erst Mutation. **Kein eigener Owner-Check pro Write-Methode** — single source of truth ist `findById`.

### 5.3 Admin-Bypass

`CurrentUser#hasRole(Role.ADMIN)` aktiviert in den Services den Bypass. Wird in jedem Service als private Methode `private boolean canAccess(UUID ownerId, CurrentUser user)` zentralisiert; im Test einfach abdeckbar.

## 6. Statistics- und AI-Module

`MatchStatisticsService` und `MatchAnalysisService` laden Match über `GetMatchUseCase` → erben den Owner-Check **automatisch**, ohne eigene Anpassung. Cross-Tenant-Analyse-Trigger (`POST /api/matches/{id}/analysis`) liefert dann ebenfalls 404 — gewollt.

## 7. Rollen-Setup

`docker/keycloak/realm-export.json` ergänzt um:

```json
"roles": {
  "realm": [
    { "name": "COACH", "description": "Default role for registered users" },
    { "name": "ADMIN", "description": "Full access across all owners" }
  ]
},
"defaultRoles": ["COACH"]
```

Bestehende Test-User bekommen `COACH` automatisch; `ADMIN` wird manuell vergeben.

## 8. Test-Strategie

**Unit-Tests (Service-Layer):**
- `createX` setzt `ownerId` = current.
- `findAll`/`findById` filtern korrekt.
- Cross-Tenant `findById` → `NotFoundException`.
- Admin sieht alles.

**Integration-Tests (IT, `test`-Profil):**
- Neuer Test-Filter `TestUserAuthenticationFilter` liest `X-Test-User-Id` und `X-Test-User-Roles` (komma-separierte Rollen-Namen), setzt `Authentication` mit `CurrentUser`-Principal. Nur im `test`-Profil aktiv.
- Test-Helper `withUser(UUID, Role...)` in `auth-module-test` (neuer test-Source-Folder, `api`-sichtbar).
- Szenarien je Endpoint: Owner A sieht/schreibt eigene → 200; Owner A liest/schreibt fremde → 404; Admin sieht/schreibt alle → 200.

## 9. Touch-Points (Dateien)

| Modul | Dateien (neu / geändert) |
|---|---|
| `auth-module` | **neu:** `domain/CurrentUser`, `domain/Role`, `application/port/in/CurrentUserProvider`, `infrastructure/security/JwtCurrentUserProvider`, `infrastructure/security/TestUserAuthenticationFilter` (test-Profil), `application/test/MockCurrentUserProvider` (Test-Helper). **geändert:** `SecurityConfig` (JwtAuthenticationConverter mit `realm_access.roles`), `SecurityConfigLocal` (Filter-Registrierung). |
| `player-module` | **geändert:** `Player` (Feld `ownerId`), `PlayerJpaEntity`, alle `*PlayerUseCase`-Commands und Service-Impls, `PlayerPersistenceAdapter` (Filter-Queries `findAllByOwnerId`, `findByIdAndOwnerId`), `PlayerController` (unverändert — Owner kommt aus Service). |
| `match-module` | **geändert:** analog für `Match` + alle Match-Services (`RecordPoint`, `SetScore`, `EndMatch`, `SetServingPlayer`, `GetMatch`). `Point` unverändert. |
| `statistics-module`, `ai-module` | **keine** Änderungen (erben Owner-Check via `GetMatchUseCase`). |
| `app` | **neu:** `db/migration/V6__add_owner_id.sql`. |
| `docker/keycloak/realm-export.json` | **geändert:** `roles.realm`, `defaultRoles`. |

## 10. YAGNI bewusst weggelassen

- Keine Team-/Org-Berechtigungen.
- Kein „Player mit anderem Coach teilen".
- Kein Audit-Trail (steht separat als TEN-59 / STRIDE/E1).
- Kein Tabellen-Refactoring.
- Kein `@PreAuthorize` (siehe D3).
- Keine separate `owner_id`-Spalte auf `points`.

## 11. Risiken und Annahmen

| # | Risiko | Mitigation |
|---|---|---|
| R1 | Bestehende Test-User in der Dev-DB haben noch keinen `owner_id`. | Backfill-UUID `00000000-...-000000000000`; Tests resetten DB pro Lauf. |
| R2 | Statistics-/AI-Module könnten Points direkt laden (statt über `GetMatchUseCase`) und damit Owner-Check umgehen. | Beim Implementieren prüfen; `LoadPointsByMatchPort` ggf. um Owner-Check ergänzen. |
| R3 | `realm_access.roles` ist je nach Keycloak-Token-Mapper-Config nicht zwingend im Token enthalten. | Realm-Export prüft die Default-Mapper; Negativtest „Token ohne Roles → ROLE_COACH". |
| R4 | Frontend kennt heute keine Rollen — Admin-UI fehlt. | Out-of-Scope; TEN-55 ändert nur Backend. Admin-Rolle wird im Keycloak-Admin-Konsole gesetzt. |

## 12. Akzeptanzkriterien (aus TEN-55, ggf. präzisiert)

- [ ] Migration V6 fügt `owner_id NOT NULL` auf `players` und `matches` hinzu, mit Index.
- [ ] `Player` und `Match` haben `ownerId`-Pflichtfeld; Create-Commands tragen es aus `CurrentUserProvider`.
- [ ] Use-Case-Reads filtern per Owner; cross-tenant `findById` → 404.
- [ ] Use-Case-Writes lehnen fremde Aggregate ab (404 via `findById`).
- [ ] `JwtAuthenticationConverter` mappt `realm_access.roles` auf `ROLE_COACH` / `ROLE_ADMIN`.
- [ ] Admin-Bypass aktiv (Service-Layer-Check).
- [ ] Integration-Tests decken cross-tenant Read- und Write-Versuche ab (404), Owner-Pfad (200), Admin (200).
- [ ] Realm-Export enthält Realm-Rollen `COACH` und `ADMIN`, `defaultRoles: [COACH]`.
- [ ] Jacoco-Gate hält (85 % line / 70 % branch).

## 13. Out-of-Scope (separate Tickets)

- Audit-Logging (TEN-59 / STRIDE/E1).
- Admin-UI im Frontend.
- DSGVO-Workflow „Recht auf Löschung / Export" (eigenes späteres Ticket).
