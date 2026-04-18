# Active Match Indicator — Design Spec

**Datum:** 2026-04-18
**Branch:** develop

---

## Zusammenfassung

In der Spielerübersicht wird angezeigt, ob ein Spieler gerade in einem laufenden Match ist (`IN_PROGRESS`). Ein kleines Icon in der Tabelle signalisiert den Status; ein Klick navigiert direkt zum laufenden Match.

---

## Backend

### Neues Output-Port `FindActiveMatchPort` (player-module)

```java
// player-module/src/main/java/com/cas/tsas/player/application/port/out/FindActiveMatchPort.java
public interface FindActiveMatchPort {
    Map<UUID, UUID> findActiveMatchIdsByPlayerIds(Set<UUID> playerIds);
}
```

Gibt eine Map `playerId → matchId` zurück — nur für Spieler, die in einem `IN_PROGRESS`-Match sind.

### `MatchJpaRepository` — neue Abfrage

```java
@Query("SELECT m FROM MatchJpaEntity m WHERE m.status = 'IN_PROGRESS' AND (m.player1Id IN :ids OR m.player2Id IN :ids)")
List<MatchJpaEntity> findInProgressByPlayerIds(@Param("ids") Set<UUID> ids);
```

### `MatchPersistenceAdapter` implementiert `FindActiveMatchPort`

Ruft `findInProgressByPlayerIds` auf und baut die Map: sowohl `player1Id` als auch `player2Id` werden auf die Match-ID gemappt.

### `PlayerResponse` — neues Feld

```java
public record PlayerResponse(
        UUID id,
        String firstName,
        String lastName,
        Gender gender,
        Handedness handedness,
        BackhandType backhandType,
        String ranking,
        String nationality,
        LocalDate birthDate,
        boolean active,
        boolean deletable,
        UUID activeMatchId   // null wenn kein laufendes Match
) { ... }
```

### `PlayerController.listPlayers()` — Bulk-Abfrage

Statt N+1-Calls für `hasMatches` wird `findActiveMatchIdsByPlayerIds` einmalig mit allen Spieler-IDs aufgerufen.

```java
@GetMapping
public List<PlayerResponse> listPlayers() {
    List<Player> players = searchPlayerUseCase.findAll();
    Set<UUID> ids = players.stream().map(Player::getId).collect(toSet());
    Map<UUID, UUID> activeMatchIds = findActiveMatchPort.findActiveMatchIdsByPlayerIds(ids);
    return players.stream()
        .map(p -> PlayerResponse.from(p, !deletePlayerUseCase.hasMatches(p.getId()), activeMatchIds.get(p.getId())))
        .toList();
}
```

---

## Frontend

### `player.model.ts`

```typescript
export interface Player {
  // ... bestehende Felder ...
  activeMatchId: string | null;
}
```

### `players.component.ts`

Neue Spalte `status` zwischen `ranking` und `actions` in `displayedColumns`.

```html
<ng-container matColumnDef="status">
  <th mat-header-cell *matHeaderCellDef></th>
  <td mat-cell *matCellDef="let player">
    @if (player.activeMatchId) {
      <button mat-icon-button color="primary"
              matTooltip="Laufendes Match anzeigen"
              (click)="goToMatch(player.activeMatchId); $event.stopPropagation()">
        <mat-icon>sports_tennis</mat-icon>
      </button>
    }
  </td>
</ng-container>
```

`goToMatch(matchId: string)` ruft `this.router.navigate(['/matches', matchId, 'score'])` auf.

### Imports

`MatTooltipModule` und `RouterModule` werden zu den `imports` des `PlayersComponent` hinzugefügt. `Router` wird per `inject(Router)` eingebunden.

---

## Out of Scope

- Echtzeit-Aktualisierung (Polling/WebSocket) — die Übersicht zeigt den Stand beim Laden der Seite
- Anzeige welches Match es ist (Gegner, Score) — das Icon reicht als Einstiegspunkt
- Mehrere gleichzeitige laufende Matches pro Spieler — architektonisch nicht vorgesehen
