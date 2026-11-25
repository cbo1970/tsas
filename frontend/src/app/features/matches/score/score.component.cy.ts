import { ScoreComponent } from './score.component';
import { ActivatedRoute } from '@angular/router';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MatchWithScore } from '../../../core/models/match.model';
import { Player } from '../../../core/models/player.model';
import { testTranslateProviders } from '../../../core/i18n/test-providers';

// ─── Fixtures ──────────────────────────────────────────────────────────────

const PLAYER1: Player = {
  id: 'p1', ownerId: 'owner-1', firstName: 'Roger', lastName: 'Federer',
  gender: 'MALE', handedness: 'RIGHT', backhandType: 'ONE_HANDED',
};

const PLAYER2: Player = {
  id: 'p2', ownerId: 'owner-1', firstName: 'Rafael', lastName: 'Nadal',
  gender: 'MALE', handedness: 'LEFT', backhandType: 'TWO_HANDED',
};

function makeMatch(overrides: Partial<MatchWithScore> = {}): MatchWithScore {
  return {
    id: 'match-1',
    ownerId: 'owner-1',
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
      servingPlayer: 1,   // default: P1 is serving
    },
    ...overrides,
  };
}

const activatedRouteStub = {
  snapshot: { paramMap: { get: () => 'match-1' } },
};

function mountScore(match: MatchWithScore = makeMatch(), player1: Player = PLAYER1, player2: Player = PLAYER2) {
  cy.intercept('GET', '**/api/matches/match-1', match).as('getMatch');
  cy.intercept('GET', '**/api/players/p1', player1).as('getPlayer1');
  cy.intercept('GET', '**/api/players/p2', player2).as('getPlayer2');
  cy.intercept('GET', '**/api/matches/*/notes', []).as('notes');

  cy.mount(ScoreComponent, {
    providers: [
      provideHttpClient(),
      provideAnimationsAsync(),
      provideRouter([]),
      ...testTranslateProviders,
      { provide: ActivatedRoute, useValue: activatedRouteStub },
    ],
  });
  cy.wait('@getMatch');
  cy.wait('@getPlayer1');
  cy.wait('@getPlayer2');
}

// ─── Helpers ──────────────────────────────────────────────────────────────

function makeUpdatedScore(pointsP1: number, pointsP2: number): MatchWithScore {
  return makeMatch({
    score: { ...makeMatch().score, pointsPlayer1: pointsP1, pointsPlayer2: pointsP2 }
  });
}

// ─── Layout tests ─────────────────────────────────────────────────────────

describe('ScoreComponent — layout', () => {
  beforeEach(() => mountScore());

  it('shows both player names in the header', () => {
    cy.contains('Roger Federer').should('be.visible');
    cy.contains('Rafael Nadal').should('be.visible');
  });

  it('shows two observation panels', () => {
    cy.get('[data-testid="panel-p1"]').should('be.visible');
    cy.get('[data-testid="panel-p2"]').should('be.visible');
  });

  it('shows edit-score and end-match buttons', () => {
    cy.get('[data-testid="edit-score-btn"]').should('be.visible');
    cy.get('[data-testid="end-match-btn"]').should('be.visible');
  });
});

// ─── Quick-click score ─────────────────────────────────────────────────────

describe('ScoreComponent — quick-click', () => {
  it('sends recordPoint without pointType when clicking P1 score', () => {
    const updated = makeUpdatedScore(1, 0);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore();

    cy.get('[data-testid="quick-score-p1"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.winner).to.equal(1);
      expect(body.pointType).to.be.oneOf([undefined, null]);
    });
  });

  it('sends winner=2 when clicking P2 score', () => {
    const updated = makeUpdatedScore(0, 1);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore();

    cy.get('[data-testid="quick-score-p2"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.winner).to.equal(2);
    });
  });
});

// ─── Observation buttons — point attribution ───────────────────────────────

describe('ScoreComponent — observation buttons', () => {

  it('Winner on P1 panel → winner=1 in request', () => {
    const updated = makeUpdatedScore(1, 0);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore();

    cy.get('[data-testid="p1-winner"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.winner).to.equal(1);
      expect(body.pointType).to.equal('WINNER');
    });
  });

  it('Forced Error on P1 panel → winner=2 (point for opponent)', () => {
    const updated = makeUpdatedScore(0, 1);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore();

    cy.get('[data-testid="p1-forced"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.winner).to.equal(2);
      expect(body.pointType).to.equal('FORCED_ERROR');
    });
  });

  it('Unforced Error on P1 panel → winner=2', () => {
    const updated = makeUpdatedScore(0, 1);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore();

    cy.get('[data-testid="p1-unforced"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.winner).to.equal(2);
      expect(body.pointType).to.equal('UNFORCED_ERROR');
    });
  });

  it('Netz on P1 panel → winner=2', () => {
    const updated = makeUpdatedScore(0, 1);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore();

    cy.get('[data-testid="p1-net"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.winner).to.equal(2);
      expect(body.pointType).to.equal('NET');
    });
  });

  it('DF on P1 panel (serving) → winner=2, pointType=DOUBLE_FAULT', () => {
    const updated = makeUpdatedScore(0, 1);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore(makeMatch()); // P1 is serving by default

    cy.get('[data-testid="p1-df"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.winner).to.equal(2);
      expect(body.pointType).to.equal('DOUBLE_FAULT');
    });
  });

  it('Ass on P1 panel (serving) → winner=1, pointType=ACE', () => {
    const updated = makeUpdatedScore(1, 0);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore(makeMatch()); // P1 is serving

    cy.get('[data-testid="p1-ace"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.winner).to.equal(1);
      expect(body.pointType).to.equal('ACE');
    });
  });
});

// ─── Ass / DF validation ───────────────────────────────────────────────────

describe('ScoreComponent — Ass/DF validation', () => {

  it('Ass on non-serving P2 panel → shows snackbar, no API call', () => {
    cy.intercept('POST', '**/api/matches/match-1/points').as('recordPoint');
    // P1 is serving (servingPlayer: 1)
    mountScore(makeMatch());

    cy.get('[data-testid="p2-ace"]').click();

    cy.contains('Ass nur für den Aufschläger').should('be.visible');
    cy.get('@recordPoint.all').should('have.length', 0);
  });

  it('DF on non-serving P2 panel → shows snackbar, no API call', () => {
    cy.intercept('POST', '**/api/matches/match-1/points').as('recordPoint');
    mountScore(makeMatch());

    cy.get('[data-testid="p2-df"]').click();

    cy.contains('Doppelfehler nur für den Aufschläger').should('be.visible');
    cy.get('@recordPoint.all').should('have.length', 0);
  });
});

// ─── Service context ───────────────────────────────────────────────────────

describe('ScoreComponent — service context', () => {

  it('selecting 1. Service on P1 panel sends serveAttempt=1 in request', () => {
    const updated = makeUpdatedScore(1, 0);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore();

    cy.get('[data-testid="p1-service-1"]').click();
    cy.get('[data-testid="p1-winner"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.serveAttempt).to.equal(1);
    });
  });

  it('context is reset after recording a point', () => {
    const updated = makeUpdatedScore(1, 0);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore();

    cy.get('[data-testid="p1-service-1"]').click();
    cy.get('[data-testid="p1-service-1"]').should('have.class', 'active');

    cy.get('[data-testid="p1-winner"]').click();
    cy.wait('@recordPoint');

    // After point, service context should be cleared
    cy.get('[data-testid="p1-service-1"]').should('not.have.class', 'active');
  });
});

// ─── Completed match ───────────────────────────────────────────────────────

describe('ScoreComponent — completed match', () => {
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

  it('shows winner overlay with player name', () => {
    cy.contains('Sieger: Roger Federer').should('be.visible');
  });

  it('hides end match button', () => {
    cy.get('[data-testid="end-match-btn"]').should('not.exist');
  });

  it('shows Zurück zur Übersicht button', () => {
    cy.contains('button', 'Zurück zur Übersicht').should('be.visible');
  });
});

// ─── Pure-logic tests for formatPoints ────────────────────────────────────

describe('formatPoints logic', () => {
  function formatPoints(score: MatchWithScore['score'], forPlayer1: boolean): string {
    if (score.isDeuce) {
      if (score.isAdvantagePlayer1 === null || score.isAdvantagePlayer1 === undefined) return '40';
      return score.isAdvantagePlayer1 === forPlayer1 ? 'A' : '40';
    }
    const pts = forPlayer1 ? score.pointsPlayer1 : score.pointsPlayer2;
    return (['0', '15', '30', '40'] as const)[pts] ?? pts.toString();
  }

  const base = makeMatch().score;

  it('0 points → "0"',  () => expect(formatPoints({ ...base, pointsPlayer1: 0 }, true)).to.equal('0'));
  it('1 point → "15"',  () => expect(formatPoints({ ...base, pointsPlayer1: 1 }, true)).to.equal('15'));
  it('2 points → "30"', () => expect(formatPoints({ ...base, pointsPlayer1: 2 }, true)).to.equal('30'));
  it('3 points → "40"', () => expect(formatPoints({ ...base, pointsPlayer1: 3 }, true)).to.equal('40'));

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

describe('ScoreComponent — Aufschlagwahl', () => {
  it('zeigt die Aufschlagwahl, wenn servingPlayer null ist', () => {
    mountScore(makeMatch({ score: { ...makeMatch().score, servingPlayer: null } }));
    cy.get('[data-testid="serve-picker"]').should('be.visible');
    cy.get('[data-testid="panel-p1"]').should('not.exist');
    cy.get('[data-testid="panel-p2"]').should('not.exist');
  });

  it('setzt den Aufschläger und zeigt danach die Panels', () => {
    cy.intercept('POST', '**/api/matches/match-1/serve/player1', {
      body: { ...makeMatch().score, servingPlayer: 1 },
    }).as('setServe');
    mountScore(makeMatch({ score: { ...makeMatch().score, servingPlayer: null } }));
    cy.get('[data-testid="serve-pick-1"]').click();
    cy.wait('@setServe');
    cy.get('[data-testid="serve-picker"]').should('not.exist');
    cy.get('[data-testid="panel-p1"]').should('be.visible');
  });

  it('blendet den Punktestand-Header aus, bis ein Aufschläger gewählt ist', () => {
    cy.intercept('POST', '**/api/matches/match-1/serve/player2', {
      body: { ...makeMatch().score, servingPlayer: 2 },
    }).as('setServe2');
    mountScore(makeMatch({ score: { ...makeMatch().score, servingPlayer: null } }));
    cy.get('[data-testid="quick-score-p1"]').should('not.exist');
    cy.get('[data-testid="quick-score-p2"]').should('not.exist');
    cy.get('[data-testid="serve-picker-back"]').should('be.visible');
    cy.get('[data-testid="serve-pick-2"]').click();
    cy.wait('@setServe2');
    cy.get('[data-testid="quick-score-p1"]').should('be.visible');
    cy.get('[data-testid="quick-score-p2"]').should('be.visible');
  });

  it('die Auswahlflächen sind mindestens 88px hoch', () => {
    mountScore(makeMatch({ score: { ...makeMatch().score, servingPlayer: null } }));
    cy.get('[data-testid="serve-pick-1"]').invoke('outerHeight').should('be.gte', 88);
    cy.get('[data-testid="serve-pick-2"]').invoke('outerHeight').should('be.gte', 88);
  });

  it('zeigt die Aufschlagwahl nicht, wenn servingPlayer gesetzt ist', () => {
    mountScore();
    cy.get('[data-testid="serve-picker"]').should('not.exist');
  });
});

// ─── Kopfzeile ─────────────────────────────────────────────────────────────

describe('ScoreComponent — Kopfzeile', () => {
  it('ist höchstens 100px hoch', () => {
    cy.viewport(390, 736);
    mountScore();
    cy.get('[data-testid="score-header"]').invoke('outerHeight').should('be.lte', 100);
  });

  it('ist auch bei 360px Breite höchstens 100px hoch', () => {
    cy.viewport(360, 692);
    mountScore();
    cy.get('[data-testid="score-header"]').invoke('outerHeight').should('be.lte', 100);
  });

  it('erfasst beim Tipp auf die Punktzahl einen Punkt ohne Attribut', () => {
    cy.intercept('POST', '**/api/matches/match-1/points', makeUpdatedScore(1, 0)).as('point');
    mountScore();
    cy.get('[data-testid="quick-score-p1"]').should('match', 'button').click();
    cy.wait('@point').its('request.body').should(body => {
      expect(body.winner).to.equal(1);
      expect(body.pointType).to.be.oneOf([null, undefined]);
    });
  });

  it('bricht bei zweistelligen Tie-Break-Punkten und langen Namen nicht um oder über', () => {
    cy.viewport(360, 692);
    const long1: Player = { ...PLAYER1, firstName: 'Alexander', lastName: 'Zverev' };
    const long2: Player = { ...PLAYER2, firstName: 'Stefanos', lastName: 'Tsitsipas' };
    mountScore(
      makeMatch({ score: { ...makeMatch().score, gamesPlayer1: 6, gamesPlayer2: 6, pointsPlayer1: 10, pointsPlayer2: 12 } }),
      long1, long2);
    cy.get('[data-testid="score-header"]').invoke('outerHeight').should('be.lte', 100);
    cy.get('[data-testid="quick-score-p1"]').should('contain.text', '10');
    cy.get('[data-testid="quick-score-p2"]').should('contain.text', '12');

    cy.get('[data-testid="score-header"]').then($header => {
      const row = $header.find('.header-row')[0];
      const p1 = $header.find('[data-testid="quick-score-p1"]')[0];
      const p2 = $header.find('[data-testid="quick-score-p2"]')[0];
      const games = $header.find('.games')[0].getBoundingClientRect();
      const r1 = p1.getBoundingClientRect();
      const r2 = p2.getBoundingClientRect();
      // Zahl passt in ihre Schaltfläche, Zeile läuft nicht horizontal über
      expect(p1.scrollWidth, 'p1 scrollWidth').to.be.lte(p1.clientWidth);
      expect(p2.scrollWidth, 'p2 scrollWidth').to.be.lte(p2.clientWidth);
      // Kein Kind ragt über die Zeile hinaus (row.scrollWidth taugt nicht: Materials
      // 48px-Touch-Target der Icon-Buttons ragt 2px über, ohne sichtbar zu sein)
      const rowRect = row.getBoundingClientRect();
      Array.from(row.children).forEach(c => {
        const r = c.getBoundingClientRect();
        expect(r.left, `${c.className} links`).to.be.gte(rowRect.left - 0.5);
        expect(r.right, `${c.className} rechts`).to.be.lte(rowRect.right + 0.5);
      });
      // Punktzahlen überlappen die Games-Spalte nicht
      expect(r1.right, 'p1 rechts <= Games links').to.be.lte(games.left);
      expect(r2.left, 'p2 links >= Games rechts').to.be.gte(games.right);
      // und liegen im sichtbaren Bereich
      expect(r1.left, 'p1 links').to.be.gte(0);
      expect(r2.right, 'p2 rechts').to.be.lte(360);
    });
  });

  it('alle Tipp-Ziele in der Kopfzeile sind mindestens 44x44px', () => {
    cy.viewport(360, 692);
    mountScore();
    cy.get('[data-testid="score-header"] button').each($b => {
      const r = $b[0].getBoundingClientRect();
      expect(r.width, 'Breite').to.be.gte(44);
      expect(r.height, 'Höhe').to.be.gte(44);
    });
  });
});
