# Design: Human-in-the-Loop Review für KI-Empfehlungen (TEN-39)

| Feld | Wert |
|------|------|
| **Ticket** | TEN-39 — Zweite substanzielle KI-Rolle + Guardrails/Human-in-the-Loop (Bewertungskriterium 16) |
| **Datum** | 2026-06-25 |
| **Status** | Design (genehmigt) |
| **Ansatz** | A — Review-Zustand in die eingebettete `Recommendation` (kein Schema-Change) |

## 1. Ziel & Kontext

Bewertungskriterium 16 verlangt — neben mindestens zwei KI-Rollen und greifenden Guardrails — **Human-in-the-Loop, wo sinnvoll** („Coach bestätigt/verwirft Empfehlungen"). Im Audit waren Guardrails (Rate-Limit/Validierung/Fallback) und die SAD-Doku bereits erfüllt; die zweite KI-Rolle (FA-20 Gegner-Vorbereitung) ist vorhanden. **Offen war HITL**: bisher nur als Prozess-Narrativ in SAD §14 beschrieben, nicht als Feature.

Dieses Design schliesst die Lücke: Der Coach kann jede einzelne Empfehlung einer persistierten **FA-11-Match-Analyse** **annehmen** oder **verwerfen** — mit optionaler Begründung. Der Review-Zustand wird persistiert und im UI sichtbar gemacht.

### Nicht-Ziele (YAGNI)

- **FA-20 OpponentPreparation** bleibt aussen vor: Das Ergebnis wird bewusst nicht persistiert (Head-to-Head ändert sich laufend, siehe `OpponentPreparation`-Javadoc), daher wäre ein dauerhafter Review-Zustand fachlich sinnlos.
- Kein Workflow/Freigabekette, keine Mehr-Personen-Reviews, keine Benachrichtigungen.
- Keine stabilen IDs je Empfehlung (Empfehlungen werden ohnehin nur als komplette Liste neu generiert — siehe §4).

## 2. Verhalten / Semantik

- Jede Empfehlung hat einen Review-Status: **`OPEN`** (Default), **`ACCEPTED`**, **`REJECTED`**.
- Der Coach kann jederzeit zwischen den Zuständen wechseln (idempotent).
- Beim Setzen eines Status kann eine optionale Begründung (`reviewNote`, Freitext) mitgegeben werden — typischerweise beim Verwerfen.
- `reviewedAt` (Zeitstempel) wird bei jedem Review-Update auf „jetzt" gesetzt.
- **Re-Generieren der Analyse setzt den Review-Zustand zurück**: FA-11 ist überschreibbar; eine neu generierte Empfehlungsliste startet mit `status=OPEN`. Das ist gewollt — neue Empfehlungen = neuer Review.

## 3. Domänenmodell (`ai-module`)

Neues Enum:

```java
package com.cas.tsas.ai.domain.model;

public enum RecommendationStatus { OPEN, ACCEPTED, REJECTED }
```

`Recommendation` wird von 3 auf 6 Felder erweitert:

```java
public record Recommendation(
        int priority,
        String title,
        String detail,
        RecommendationStatus status,
        String reviewNote,
        Instant reviewedAt
) {
    // Default-Status absichern (Abwärtskompatibilität alter JSON-Zeilen / null aus Deserialisierung)
    public Recommendation {
        status = status == null ? RecommendationStatus.OPEN : status;
    }

    // Generierungs-Aufrufstellen (LLM-Adapter, Fakes, Tests) bleiben unverändert:
    public Recommendation(int priority, String title, String detail) {
        this(priority, title, detail, RecommendationStatus.OPEN, null, null);
    }

    // Immutable Status-Übergang
    public Recommendation withReview(RecommendationStatus newStatus, String note, Instant at) {
        return new Recommendation(priority, title, detail, newStatus, note, at);
    }
}
```

Damit kompilieren alle bestehenden `new Recommendation(p, t, d)`-Aufrufe (OpenAiLlmAdapter, FakeLlmClientAdapter, Tests) ohne Änderung weiter.

## 4. Persistenz (`ai-module`)

- **Keine DB-Migration.** Die Empfehlungsliste liegt bereits als JSON in `match_analysis.recommendations` (`TEXT NOT NULL DEFAULT '[]'`). Die drei neuen Felder werden vom bestehenden Jackson-`ObjectMapper` mitserialisiert.
- **Legacy-Zeilen** (ohne die neuen Felder) deserialisieren null → der Compact-Constructor setzt `status=OPEN`; `reviewNote`/`reviewedAt` bleiben null. Voraussetzung: Der Mapper ignoriert fehlende Properties (Standard bei Jackson-Records). Falls `FAIL_ON_NULL_FOR_PRIMITIVES`/`FAIL_ON_UNKNOWN_PROPERTIES` projektweit verschärft ist, wird der Mapper im Persistenz-Adapter entsprechend tolerant konfiguriert.
- **Adressierung per Listenindex** ist stabil, weil die Liste immer als Ganzes generiert und ersetzt wird — innerhalb einer Analyse-Version verschiebt sich nichts.

## 5. Application-Layer

Neuer Input-Port:

```java
public interface ReviewRecommendationUseCase {
    MatchAnalysis review(UUID matchId, int recommendationIndex,
                         RecommendationStatus status, String note, CurrentUser user);
}
```

Umsetzung im bestehenden `MatchAnalysisService` (kein neuer Service — die Verantwortung „Match-Analyse" bleibt zusammen):

1. Analyse zum `matchId` laden (`LoadMatchAnalysisPort`).
2. **Owner-Check** wie bei FA-11: das Match muss dem `user.sub` gehören; fremd/unbekannt → `MatchAnalysisNotFoundException` (→ 404, IDOR-Schutz wie FA-11/FA-20).
3. Analyse muss `COMPLETED` sein, sonst `AnalysisNotReviewableException` (→ 409).
4. Index gegen `recommendations.size()` prüfen; ausserhalb → `RecommendationNotFoundException` (→ 404).
5. Empfehlung an Position `index` via `withReview(status, note, now)` ersetzen, Liste neu setzen, persistieren (`SaveMatchAnalysisPort`).
6. Aktualisierte `MatchAnalysis` zurückgeben.

`now` kommt aus einem injizierten `Clock`/`InstantSource` (testbar), passend zum bestehenden Muster.

## 6. Guardrails / Fehlerbehandlung

| Situation | Verhalten |
|---|---|
| Match/Analyse unbekannt oder fremder Owner | **404** (kein 403 — verhindert ID-Enumeration, wie FA-11/FA-20) |
| Analyse nicht `COMPLETED` (PENDING/FAILED) | **409** |
| `recommendationIndex` ausserhalb der Liste | **404** |
| `status` fehlt / ungültiger Enum-Wert | **400** (Bean-Validation) |
| `note` länger als 500 Zeichen | **400** (`@Size(max=500)`) |

Alle Antworten als RFC-7807-`ProblemDetail` über den bestehenden `AiExceptionHandler` (neue Exceptions dort ergänzen). Diese Guardrails ergänzen die bestehenden Cost-/Eingabe-Guardrails (Rate-Limit TEN-64, Mindestpunktzahl, `COMPLETED`-Vorbedingung).

## 7. REST-API (`app`-Modul)

```
PATCH /api/matches/{matchId}/analysis/recommendations/{index}
Body:  { "status": "ACCEPTED" | "REJECTED" | "OPEN", "note": "…optional, max 500" }
200:   aktualisierte MatchAnalysis (gleiches DTO wie GET …/analysis)
```

- Request-DTO `ReviewRecommendationRequest(@NotNull RecommendationStatus status, @Size(max=500) String note)`.
- Controller-Methode im bestehenden `MatchAnalysisController`.
- Response nutzt das bestehende Analyse-Response-DTO, erweitert um `status/reviewNote/reviewedAt` je Empfehlung.
- Endpunkt ist wie die übrigen `/api/**` JWT-geschützt.
- OpenAPI: wird automatisch von springdoc unter `/v3/api-docs` publiziert (TEN-41).

## 8. Frontend (`match-analysis.component`)

- `core/models/analysis.model.ts`: Recommendation-Interface um `status`, `reviewNote`, `reviewedAt` erweitern; Enum-Typ `RecommendationStatus`.
- Analyse-Service: Methode `reviewRecommendation(matchId, index, {status, note})` → `PATCH`.
- Komponente (`match-analysis.component.html`/`.ts`): je Empfehlung
  - Buttons **Annehmen** / **Verwerfen** (Material), plus „Zurücksetzen" (→ OPEN) bei bereits bewerteten.
  - Beim Verwerfen erscheint ein optionales Notiz-Eingabefeld; Notiz wird mit dem Status gesendet.
  - Visueller Zustand: `ACCEPTED` = grünes Häkchen/Badge; `REJECTED` = ausgegraut/durchgestrichen + sichtbare Begründung; `OPEN` = neutral.
  - Nach erfolgreichem `PATCH` lokalen Zustand aus der Antwort aktualisieren; Fehler (409/404/400) über das bestehende statuscode-spezifische Error-Handling der Komponente anzeigen.
- Mehrsprachigkeit (FA-21): neue UI-Texte in `de/en/it/fr.json`.

## 9. Tests

- **Service-Unit** (`MatchAnalysisServiceTest`): je Status-Übergang (OPEN→ACCEPTED/REJECTED, zurück zu OPEN), Index-OOB → 404, nicht-COMPLETED → 409, cross-tenant → 404, `reviewedAt` wird gesetzt. (Die Notiz-Längenprüfung ist Bean-Validation im Web-Layer und wird im IT abgedeckt, nicht hier.)
- **Persistenz-Round-Trip**: `Recommendation` mit Review-Feldern serialisieren/deserialisieren; Legacy-JSON ohne die Felder → `status=OPEN`, Rest null.
- **Integration (`MatchAnalysisControllerIT`, echtes PostgreSQL)**: `PATCH` 200 (Status + Notiz persistiert, via erneutem GET verifiziert), 400 (ungültiger Status / Notiz zu lang), 404 (unbekannter Index, cross-tenant), 409 (nicht COMPLETED).
- **Frontend (Cypress Component)**: Annehmen/Verwerfen rendert korrekten Zustand; Verwerfen-mit-Notiz sendet `PATCH` mit Body; Fehlerpfad zeigt Meldung.

## 10. SAD-Doku (im selben PR)

- **FA-11** (§10.1) um die Review-Fähigkeit ergänzen (neuer `PATCH`-Endpunkt, Statuscodes).
- HITL in der KI-/Guardrails-Beschreibung und in **§14** vom „Prozess-Narrativ" zum **umgesetzten Feature** hochstufen, mit Bezug zu Bewertungskriterium 16.
- §10.3 Abnahmekriterien (KI-Analyse-Zeile) um die Review-Codes 200/400/404/409 ergänzen.

## 11. Auswirkungen / Risiken

- **Abwärtskompatibilität**: bestehende `match_analysis`-Zeilen bleiben gültig (neue Felder optional, Default `OPEN`). Kein Datenmigrations-Risiko.
- **Kein neuer LLM-Aufruf**: Review ist rein lokal/persistent — keine Kosten, kein Rate-Limit nötig.
- **Bewertungskriterium 16**: Mit diesem Feature ist HITL umgesetzt; zusammen mit den bestehenden zwei KI-Rollen und greifenden Guardrails wird die Stufe „vollständig" adressiert.
