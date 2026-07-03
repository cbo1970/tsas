# TEN-60 — Bean-Validation auf DTOs schärfen (Design)

**Datum:** 2026-06-22
**Ticket:** [TEN-60](https://linear.app/tennis-score-and-statistic/issue/TEN-60)
**STRIDE-Befund:** D3 (Request-Size-Limit), allg. Eingabevalidierung
**Scope:** Ein PR / ein Implementierungsschritt.
**Folgt auf:** TEN-55 (Owner-Bindung), TEN-59 (Audit-Logging).

---

## 1. Problem

Die heutigen Request-DTOs validieren teilweise, aber nicht durchgängig:

| DTO | Lücke |
|---|---|
| `CreatePlayerRequest`, `UpdatePlayerRequest` | Kein `@Size`-Limit auf String-Feldern → ein Angreifer kann beliebig grosse Strings posten (DSGVO-Spam, DB-Bloat) |
| `RecordPointRequest` | `pointType`, `strokeType`, `direction` als roher `String`; Controller ruft `Enum.valueOf(...)` auf → bei ungültigem Wert wird **500** zurückgegeben (`IllegalArgumentException` bubbelt) statt **400** |
| `RecordPointRequest.remark` | Validiert mit `@Length(max=500)` (Hibernate-Validator-spezifisch); inkonsistent mit den anderen DTOs die `@NotBlank`/`@NotNull` (Jakarta) verwenden |

`EndMatchWalkoverRequest.winner` und `SetScoreRequest.winner` sind über `@Pattern("PLAYER1\|PLAYER2")` bereits korrekt whitelisted.

## 2. Ziel

Alle Request-DTOs lehnen ungültige Eingaben mit **HTTP 400** ab, **ohne 500-Fallout**. String-Felder haben definierte Längenobergrenzen. Enum-Felder sind im DTO bereits getypt; das Controller-Mapping wird trivial.

## 3. Designentscheidungen

| # | Entscheidung | Begründung |
|---|---|---|
| D1 | Typisierte Enums (`PointType`, `StrokeType`, `Direction`) in `RecordPointRequest`, kein eigener `@ValidEnum`-Constraint. | Jackson deserialisiert Enums direkt; bei ungültigem Wert wird `HttpMessageNotReadableException` geworfen → Spring Boot 4 liefert standardmässig 400. Spart Custom-Code. |
| D2 | `@Size`-Grenzen: firstName/lastName 100, ranking 50, nationality 64, remark 500. | Übliche Defaults; firstName/lastName generös genug für Doppelnamen mit Hyphen; nationality deckt vollständige Ländernamen in beliebiger Sprache. |
| D3 | `@Length(max=500)` durch `@Size(max=500)` ersetzen. | Konsistenz: Jakarta-Bean-Validation statt Hibernate-Validator-spezifischer Annotation. |
| D4 | Kein eigenes `@ControllerAdvice` für Validierungsfehler in diesem PR. | TEN-61 deckt die problem+json-Formatierung global ab. Default-Verhalten von Spring Boot 4 (`MethodArgumentNotValidException` → 400) reicht für TEN-60. |
| D5 | DTOs in `ai-module`, `statistics-module` werden **nicht** angefasst. | Beide haben keine Freitext-Felder; Inputs sind nur `matchId`-Path-Variables. |

## 4. Architektur

### 4.1 Geänderte DTOs

**`CreatePlayerRequest` / `UpdatePlayerRequest`:**

```java
public record CreatePlayerRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotNull Gender gender,
        @NotNull Handedness handedness,
        @NotNull BackhandType backhandType,
        @Size(max = 50)  String ranking,
        @Size(max = 64)  String nationality,
        LocalDate birthDate
) {}
```

(`UpdatePlayerRequest` analog — gleiche Felder, gleiche Constraints.)

**`RecordPointRequest`:**

```java
import com.cas.tsas.match.domain.model.Direction;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.match.domain.model.StrokeType;
import jakarta.validation.constraints.Size;

public record RecordPointRequest(
        @NotNull @Min(1) @Max(2) Integer winner,
        PointType pointType,
        StrokeType strokeType,
        Direction direction,
        @Size(max = 500) String remark,
        @Min(1) @Max(2) Integer serveAttempt
) {}
```

### 4.2 Controller-Änderung

`MatchController.recordPoint` heute:

```java
var command = new RecordPointUseCase.RecordPointCommand(
        id,
        request.winner(),
        request.pointType() != null ? PointType.valueOf(request.pointType()) : null,
        request.strokeType() != null ? StrokeType.valueOf(request.strokeType()) : null,
        request.direction() != null ? Direction.valueOf(request.direction()) : null,
        request.remark(),
        request.serveAttempt()
);
```

Wird zu:

```java
var command = new RecordPointUseCase.RecordPointCommand(
        id,
        request.winner(),
        request.pointType(),
        request.strokeType(),
        request.direction(),
        request.remark(),
        request.serveAttempt()
);
```

## 5. Fehlerpfade

| Input | Spring-Verhalten | Response |
|---|---|---|
| `firstName` > 100 Zeichen | `MethodArgumentNotValidException` | **400** |
| `pointType: "XYZ"` (invalid enum) | `HttpMessageNotReadableException` (Jackson `InvalidFormatException`) | **400** |
| `pointType: null` | erlaubt — keine `@NotNull`, Optional im Use-Case | **201** (kein Fehler) |
| `winner: 3` (out of range) | `MethodArgumentNotValidException` | **400** |
| `remark` > 500 Zeichen | `MethodArgumentNotValidException` | **400** |

Body-Format der 400er ist Spring-Default in diesem PR — TEN-61 ersetzt sie durch problem+json.

## 6. Tests

### Unit
Nicht erforderlich — DTOs sind Records mit Annotations; das Verhalten kommt aus der Validation-Engine, nicht aus eigenem Code.

### Integration (`:app`)
**`PlayerValidationIT extends AbstractIntegrationTest`:**
- `create_rejects_too_long_first_name_with_400`
- `create_rejects_too_long_ranking_with_400`
- `update_rejects_too_long_last_name_with_400`
- Happy-Path (Sanity, dass das schärfere Schema valide Requests nicht zerbricht).

**`RecordPointValidationIT extends AbstractIntegrationTest`:**
- `record_point_rejects_invalid_point_type_with_400`
- `record_point_rejects_too_long_remark_with_400`
- `record_point_rejects_winner_out_of_range_with_400`
- Happy-Path mit nullable Enums + voller Enum-Konstellation.

Bestehende ITs (`OwnershipIntegrationTest`, `AuditingIT`, `MatchAnalysisControllerIT`) werden nicht angepasst — ihre Test-Bodies nutzen valide Strings und Enums.

## 7. Touch-Points

| Modul | Dateien (neu / geändert) |
|---|---|
| `player-module` | **geändert:** `CreatePlayerRequest`, `UpdatePlayerRequest` (Imports + `@Size`-Annotations) |
| `match-module` | **geändert:** `RecordPointRequest` (Imports + Enum-Typen + `@Size` statt `@Length`), `MatchController.recordPoint` (Mapping vereinfacht) |
| `app` | **neu:** `PlayerValidationIT`, `RecordPointValidationIT` |

`statistics-module`, `ai-module`, `auth-module`, `common-module`: keine Änderungen.

## 8. YAGNI bewusst weggelassen

- Custom `@ValidEnum`-Annotation.
- `@ControllerAdvice` für problem+json — TEN-61.
- Validation-Groups (`create` vs `update`).
- `@Pattern` für andere String-Felder (firstName etc.) — `@NotBlank @Size` reicht; eine Whitelist auf Namen wäre zu invasiv (Unicode-Namen, Apostrophe, etc.).
- DTO-Versionierung.

## 9. Risiken und Annahmen

| # | Risiko | Mitigation |
|---|---|---|
| R1 | Bestehende Frontend-Tests/Clients senden `pointType` als String — der Wert ist heute schon ein gültiger Enum-Name (z. B. `"WINNER"`); Jackson akzeptiert String-Enum-Mapping unverändert. | Smoke-Test gegen Frontend lokal (manuell, nach Merge). |
| R2 | `OwnershipIntegrationTest` / `AuditingIT` setzen `pointType` als String — gleiches Argument: heute schon gültige Enum-Namen, kein Wechsel nötig. | Existing ITs werden grün bleiben. |
| R3 | Bestehende DB-Daten könnten Strings haben, die jetzt das `@Size`-Limit übersteigen. | Validation gilt nur auf Eingangs-Requests, nicht beim Laden. Bestehende Daten bleiben unangetastet. |

## 10. Akzeptanzkriterien (aus TEN-60, präzisiert)

- [ ] `@Size`-Limits auf `firstName`, `lastName`, `ranking`, `nationality`, `remark`.
- [ ] `RecordPointRequest` typisiert Enum-Felder als `PointType`/`StrokeType`/`Direction`; `MatchController.recordPoint` ruft kein `Enum.valueOf` mehr auf.
- [ ] `@Length(max=500)` durch `@Size(max=500)` ersetzt.
- [ ] Walkover akzeptiert nur `PLAYER1`/`PLAYER2` (heute bereits ✓ via `@Pattern` — Spec dokumentiert das).
- [ ] Integration-Tests belegen: zu lang → 400, ungültiges Enum → 400, valider Request → 201.
- [ ] JaCoCo-Gate hält.

## 11. Out-of-Scope (separate Tickets)

- TEN-61 (STRIDE/E3): problem+json-Antwort-Body für Fehler.
- Globales Request-Size-Limit auf Servlet-Ebene (z. B. `server.tomcat.max-http-form-post-size`) — kann in TEN-62 oder TEN-63 abgedeckt werden.
- Validation-Annotations auf Domain-Modellen (heute keine — Domain validiert eigene Invarianten im Konstruktor).
