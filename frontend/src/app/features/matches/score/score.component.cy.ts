import { ScoreComponent } from './score.component';
import { ActivatedRoute } from '@angular/router';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MatchWithScore } from '../../../core/models/match.model';
import { Player } from '../../../core/models/player.model';

// ─── Test fixtures ─────────────────────────────────────────────────────────

const PLAYER1: Player = {
  id: 'p1', firstName: 'Roger', lastName: 'Federer',
  gender: 'MALE', handedness: 'RIGHT', backhandType: 'ONE_HANDED',
};

const PLAYER2: Player = {
  id: 'p2', firstName: 'Rafael', lastName: 'Nadal',
  gender: 'MALE', handedness: 'LEFT', backhandType: 'TWO_HANDED',
};

function makeMatch(overrides: Partial<MatchWithScore> = {}): MatchWithScore {
  return {
    id: 'match-1',
    player1Id: 'p1',
    player2Id: 'p2',
    setsToWin: 2,
    matchTiebreak: false,
    shortSet: false,
    status: 'IN_PROGRESS',
    score: {
      id: 'score-1',
      matchId: 'match-1',
      pointsPlayer1: 0,
      pointsPlayer2: 0,
      gamesPlayer1: 0,
      gamesPlayer2: 0,
      setsPlayer1: 0,
      setsPlayer2: 0,
      isDeuce: false,
      isAdvantagePlayer1: null,
      currentSet: 1,
      isDone: false,
      winner: null,
      acesPlayer1: 0,
      acesPlayer2: 0,
      servingPlayer: null,
    },
    ...overrides,
  };
}

const activatedRouteStub = {
  snapshot: { paramMap: { get: () => 'match-1' } },
};

function mountScore(match: MatchWithScore = makeMatch()) {
  cy.intercept('GET', '**/api/matches/match-1', match).as('getMatch');
  cy.intercept('GET', '**/api/players/p1', PLAYER1).as('getPlayer1');
  cy.intercept('GET', '**/api/players/p2', PLAYER2).as('getPlayer2');

  cy.mount(ScoreComponent, {
    providers: [
      provideHttpClient(),
      provideAnimationsAsync(),
      provideRouter([]),
      { provide: ActivatedRoute, useValue: activatedRouteStub },
    ],
  });
  cy.wait('@getMatch');
  cy.wait('@getPlayer1');
  cy.wait('@getPlayer2');
}

// ─── Component mount tests ─────────────────────────────────────────────────

describe('ScoreComponent', () => {

  describe('initial display', () => {
    beforeEach(() => mountScore());

    it('shows player names', () => {
      cy.contains('Roger Federer').should('be.visible');
      cy.contains('Rafael Nadal').should('be.visible');
    });

    it('shows match status as "Läuft"', () => {
      cy.contains('Läuft').should('be.visible');
    });

    it('shows current set number', () => {
      cy.contains('Satz 1').should('be.visible');
    });

    it('shows "Score korrigieren" button', () => {
      cy.contains('button', 'Score korrigieren').should('be.visible');
    });

    it('shows "Match beenden" button for in-progress match', () => {
      cy.contains('button', 'Match beenden').should('be.visible');
    });
  });

  describe('"Match beenden" w.o. dialog', () => {
    beforeEach(() => mountScore());

    it('opens dialog with both player names as radio options', () => {
      cy.contains('button', 'Match beenden').click();
      cy.contains('Roger Federer').should('be.visible');
      cy.contains('Rafael Nadal').should('be.visible');
    });

    it('confirm button is disabled until a player is selected', () => {
      cy.contains('button', 'Match beenden').click();
      cy.get('mat-dialog-actions').contains('button', 'Match beenden').should('be.disabled');
    });

    it('enables confirm button after selecting a player', () => {
      cy.contains('button', 'Match beenden').click();
      cy.get('mat-dialog-container').contains('mat-radio-button', 'Roger Federer').find('input[type="radio"]').check({ force: true });
      cy.get('mat-dialog-actions').contains('button', 'Match beenden').should('not.be.disabled');
    });

    it('calls walkover endpoint with PLAYER1 when player 1 selected and confirmed', () => {
      cy.intercept('POST', '**/end/walkover', { statusCode: 200, body: { id: 'match-1', status: 'COMPLETED' } }).as('walkover');
      cy.intercept('GET', '**/api/matches/match-1', makeMatch({ status: 'COMPLETED' })).as('reload');

      cy.contains('button', 'Match beenden').click();
      cy.get('mat-dialog-container').contains('mat-radio-button', 'Roger Federer').find('input[type="radio"]').check({ force: true });
      cy.get('mat-dialog-actions').contains('button', 'Match beenden').click();

      cy.wait('@walkover').its('request.body').should('deep.equal', { winner: 'PLAYER1' });
    });

    it('calls walkover endpoint with PLAYER2 when player 2 selected and confirmed', () => {
      cy.intercept('POST', '**/end/walkover', { statusCode: 200, body: { id: 'match-1', status: 'COMPLETED' } }).as('walkover');
      cy.intercept('GET', '**/api/matches/match-1', makeMatch({ status: 'COMPLETED' })).as('reload');

      cy.contains('button', 'Match beenden').click();
      cy.get('mat-dialog-container').contains('mat-radio-button', 'Rafael Nadal').find('input[type="radio"]').check({ force: true });
      cy.get('mat-dialog-actions').contains('button', 'Match beenden').click();

      cy.wait('@walkover').its('request.body').should('deep.equal', { winner: 'PLAYER2' });
    });

    it('does not call walkover endpoint when dialog is cancelled', () => {
      cy.intercept('POST', '**/end/walkover').as('walkover');

      cy.contains('button', 'Match beenden').click();
      cy.contains('button', 'Abbrechen').click();

      cy.get('@walkover.all').should('have.length', 0);
    });
  });

  describe('completed match', () => {
    beforeEach(() => {
      const completedMatch = makeMatch({
        status: 'COMPLETED',
        score: {
          ...makeMatch().score,
          setsPlayer1: 2,
          setsPlayer2: 1,
          isDone: true,
          winner: 'PLAYER1',
        },
      });
      mountScore(completedMatch);
    });

    it('shows "Beendet" status', () => {
      cy.contains('Beendet').should('be.visible');
    });

    it('shows winner card with player name', () => {
      cy.contains('Sieger: Roger Federer').should('be.visible');
    });

    it('hides "Match beenden" button', () => {
      cy.contains('button', 'Match beenden').should('not.exist');
    });

    it('shows "Zurück zur Übersicht" button', () => {
      cy.contains('button', 'Zurück zur Übersicht').should('be.visible');
    });
  });
});

// ─── Pure-logic tests for formatPoints ────────────────────────────────────

describe('formatPoints logic', () => {
  // Mirror the function from ScoreComponent for isolated unit testing
  function formatPoints(
    score: MatchWithScore['score'],
    forPlayer1: boolean
  ): string {
    if (score.isDeuce) {
      if (score.isAdvantagePlayer1 === null || score.isAdvantagePlayer1 === undefined) {
        return '40';
      }
      return score.isAdvantagePlayer1 === forPlayer1 ? 'A' : '40';
    }
    const pts = forPlayer1 ? score.pointsPlayer1 : score.pointsPlayer2;
    const map = ['0', '15', '30', '40'];
    return map[pts] ?? pts.toString();
  }

  const base = makeMatch().score;

  it('0 points → "0"', () => {
    expect(formatPoints({ ...base, pointsPlayer1: 0 }, true)).to.equal('0');
  });

  it('1 point → "15"', () => {
    expect(formatPoints({ ...base, pointsPlayer1: 1 }, true)).to.equal('15');
  });

  it('2 points → "30"', () => {
    expect(formatPoints({ ...base, pointsPlayer1: 2 }, true)).to.equal('30');
  });

  it('3 points → "40"', () => {
    expect(formatPoints({ ...base, pointsPlayer1: 3 }, true)).to.equal('40');
  });

  it('deuce without advantage → "40" for both', () => {
    const score = { ...base, isDeuce: true, isAdvantagePlayer1: null };
    expect(formatPoints(score, true)).to.equal('40');
    expect(formatPoints(score, false)).to.equal('40');
  });

  it('player1 advantage → "A" for player1, "40" for player2', () => {
    const score = { ...base, isDeuce: true, isAdvantagePlayer1: true };
    expect(formatPoints(score, true)).to.equal('A');
    expect(formatPoints(score, false)).to.equal('40');
  });

  it('player2 advantage → "A" for player2, "40" for player1', () => {
    const score = { ...base, isDeuce: true, isAdvantagePlayer1: false };
    expect(formatPoints(score, false)).to.equal('A');
    expect(formatPoints(score, true)).to.equal('40');
  });
});
