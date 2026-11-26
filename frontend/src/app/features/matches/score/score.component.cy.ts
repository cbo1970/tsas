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

function mountScore(match: MatchWithScore = makeMatch(), player1: Player = PLAYER1, player2: Player = PLAYER2, notes: unknown[] = []) {
  cy.intercept('GET', '**/api/matches/match-1', match).as('getMatch');
  cy.intercept('GET', '**/api/players/p1', player1).as('getPlayer1');
  cy.intercept('GET', '**/api/players/p2', player2).as('getPlayer2');
  cy.intercept('GET', '**/api/matches/*/notes', notes).as('notes');

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

/**
 * Faellt durch, wenn das Element von einem overflow:hidden-Vorfahren
 * abgeschnitten wird. Ein solcher Container laesst sich vom Benutzer nicht
 * scrollen — was unterhalb liegt, ist schlicht nicht erreichbar.
 */
function assertNotClipped(testid: string) {
  cy.get(`[data-testid="${testid}"]`).then($el => {
    const el = $el[0];
    const rect = el.getBoundingClientRect();
    for (let a: HTMLElement | null = el.parentElement; a; a = a.parentElement) {
      const overflowY = getComputedStyle(a).overflowY;
      if (overflowY !== 'hidden' && overflowY !== 'clip') continue;
      const box = a.getBoundingClientRect();
      const name = a.getAttribute('data-testid') ?? a.className ?? a.tagName;
      expect(rect.bottom, `${testid} wird von "${name}" abgeschnitten`).to.be.at.most(box.bottom + 1);
    }
  });
}

// ─── Layout tests ─────────────────────────────────────────────────────────

describe('ScoreComponent — layout', () => {
  beforeEach(() => mountScore());

  it('shows both player names in the header', () => {
    cy.contains('Roger Federer').should('be.visible');
    cy.contains('Rafael Nadal').should('be.visible');
  });

  it('shows a single observation panel', () => {
    cy.get('[data-testid="panel"]').should('be.visible');
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
  beforeEach(() => cy.viewport(360, 692));

  it('Winner on the serving panel → winner=1 in request', () => {
    const updated = makeUpdatedScore(1, 0);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore();

    cy.get('[data-testid="obs-WINNER"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.winner).to.equal(1);
      expect(body.pointType).to.equal('WINNER');
    });
  });

  it('Forced Error on the serving panel → winner=2 (point for opponent)', () => {
    const updated = makeUpdatedScore(0, 1);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore();

    cy.get('[data-testid="obs-FORCED_ERROR"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.winner).to.equal(2);
      expect(body.pointType).to.equal('FORCED_ERROR');
    });
  });

  it('Unforced Error on the serving panel → winner=2', () => {
    const updated = makeUpdatedScore(0, 1);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore();

    cy.get('[data-testid="obs-UNFORCED_ERROR"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.winner).to.equal(2);
      expect(body.pointType).to.equal('UNFORCED_ERROR');
    });
  });

  it('Netz on the serving panel → winner=2', () => {
    const updated = makeUpdatedScore(0, 1);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore();

    cy.get('[data-testid="obs-NET"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.winner).to.equal(2);
      expect(body.pointType).to.equal('NET');
    });
  });

  it('DF on the serving panel → winner=2, pointType=DOUBLE_FAULT', () => {
    const updated = makeUpdatedScore(0, 1);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore(makeMatch()); // P1 is serving by default

    cy.get('[data-testid="obs-DOUBLE_FAULT"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.winner).to.equal(2);
      expect(body.pointType).to.equal('DOUBLE_FAULT');
    });
  });

  it('Ass on the serving panel → winner=1, pointType=ACE', () => {
    const updated = makeUpdatedScore(1, 0);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore(makeMatch()); // P1 is serving

    cy.get('[data-testid="obs-ACE"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.winner).to.equal(1);
      expect(body.pointType).to.equal('ACE');
    });
  });

  it('Winner on the returning panel → winner=2', () => {
    const updated = makeUpdatedScore(0, 1);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore();

    cy.get('[data-testid="switch-player-2"]').click();
    cy.get('[data-testid="obs-WINNER"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.winner).to.equal(2);
      expect(body.pointType).to.equal('WINNER');
    });
  });
});

// ─── Service context ───────────────────────────────────────────────────────

describe('ScoreComponent — service context', () => {
  beforeEach(() => cy.viewport(360, 692));

  it('selecting 1. Service sends serveAttempt=1 in request', () => {
    const updated = makeUpdatedScore(1, 0);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore();

    cy.get('[data-testid="serve-attempt-1"]').click();
    cy.get('[data-testid="obs-WINNER"]').click();

    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.serveAttempt).to.equal(1);
    });
  });

  it('schickt den auf dem Aufschläger-Panel gewählten Aufschlagversuch mit, auch wenn vom Returnierenden-Panel erfasst wird', () => {
    cy.intercept('POST', '**/api/matches/match-1/points', makeUpdatedScore(0, 1)).as('point');
    mountScore();
    cy.get('[data-testid="serve-attempt-2"]').click();
    cy.get('[data-testid="switch-player-2"]').click();
    cy.get('[data-testid="obs-UNFORCED_ERROR"]').click();
    cy.wait('@point').its('request.body.serveAttempt').should('eq', 2);
  });

  it('context is reset after recording a point', () => {
    const updated = makeUpdatedScore(1, 0);
    cy.intercept('POST', '**/api/matches/match-1/points', updated).as('recordPoint');
    mountScore();

    cy.get('[data-testid="serve-attempt-1"]').click();
    cy.get('[data-testid="serve-attempt-1"]').should('have.attr', 'aria-pressed', 'true');

    cy.get('[data-testid="obs-WINNER"]').click();
    cy.wait('@recordPoint');

    // After point, service context should be cleared
    cy.get('[data-testid="serve-attempt-1"]').should('have.attr', 'aria-pressed', 'false');
  });

  // Der Kontext gehört zum Punkt, nicht zum Panel: wird der Punkt vom anderen
  // Panel erfasst, darf die Auswahl auf dem Aufschläger-Panel nicht stehen
  // bleiben — sonst reist sie stumm mit dem nächsten Punkt mit.
  it('räumt den Kontext auch auf, wenn der Punkt vom anderen Panel erfasst wurde', () => {
    cy.intercept('POST', '**/api/matches/match-1/points', makeUpdatedScore(0, 1)).as('recordPoint');
    mountScore();

    cy.get('[data-testid="serve-attempt-2"]').click();
    cy.get('[data-testid="stroke-BACKHAND"]').click();
    cy.get('[data-testid="switch-player-2"]').click();
    cy.get('[data-testid="obs-UNFORCED_ERROR"]').click();
    cy.wait('@recordPoint');

    // Der effect hat auf das Aufschläger-Panel zurückgeschaltet
    cy.get('[data-testid="serve-attempt-2"]').should('have.attr', 'aria-pressed', 'false');
    cy.get('[data-testid="stroke-BACKHAND"]').should('have.attr', 'aria-pressed', 'false');
  });

  // C1: Ein Schnellpunkt (Tipp auf die Punktzahl) ist ein erfasster Punkt. Der
  // Kontext gehoert zum Punkt, nicht zum Panel — er darf danach nicht stehen
  // bleiben und stumm mit dem naechsten Punkt mitreisen.
  it('schickt nach einem Schnellpunkt keinen alten Kontext mit', () => {
    cy.intercept('POST', '**/api/matches/match-1/points', makeUpdatedScore(0, 1)).as('recordPoint');
    mountScore();

    cy.get('[data-testid="serve-attempt-2"]').click();
    cy.get('[data-testid="stroke-BACKHAND"]').click();
    cy.get('[data-testid="quick-score-p1"]').click();
    cy.wait('@recordPoint');

    cy.get('[data-testid="serve-attempt-2"]').should('have.attr', 'aria-pressed', 'false');
    cy.get('[data-testid="stroke-BACKHAND"]').should('have.attr', 'aria-pressed', 'false');

    // Naechster Punkt ueber das Panel, ohne neue Auswahl
    cy.get('[data-testid="obs-WINNER"]').click();
    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.serveAttempt, 'serveAttempt').to.be.oneOf([null, undefined]);
      expect(body.strokeType, 'strokeType').to.be.oneOf([null, undefined]);
    });
  });

  it('schickt nach einem Punkt vom anderen Panel keinen alten Kontext mit', () => {
    cy.intercept('POST', '**/api/matches/match-1/points', makeUpdatedScore(0, 1)).as('recordPoint');
    mountScore();

    cy.get('[data-testid="serve-attempt-2"]').click();
    cy.get('[data-testid="stroke-BACKHAND"]').click();
    cy.get('[data-testid="switch-player-2"]').click();
    cy.get('[data-testid="obs-UNFORCED_ERROR"]').click();
    cy.wait('@recordPoint');

    // Nächster Punkt, jetzt wieder auf dem Aufschläger-Panel, ohne neue Auswahl
    cy.get('[data-testid="obs-WINNER"]').click();
    cy.wait('@recordPoint').its('request.body').should(body => {
      expect(body.serveAttempt, 'serveAttempt').to.be.oneOf([null, undefined]);
      expect(body.strokeType, 'strokeType').to.be.oneOf([null, undefined]);
    });
  });
});

// ─── Stroke context ────────────────────────────────────────────────────────

describe('ScoreComponent — Schlagart', () => {
  beforeEach(() => cy.viewport(360, 692));

  it('schickt die gewählte Schlagart mit', () => {
    cy.intercept('POST', '**/api/matches/match-1/points', makeUpdatedScore(1, 0)).as('recordPoint');
    mountScore();

    cy.get('[data-testid="stroke-BACKHAND"]').click();
    cy.get('[data-testid="obs-WINNER"]').click();

    cy.wait('@recordPoint').its('request.body.strokeType').should('eq', 'BACKHAND');
  });

  it('hebt die Schlagart beim zweiten Tipp wieder auf', () => {
    cy.intercept('POST', '**/api/matches/match-1/points', makeUpdatedScore(1, 0)).as('recordPoint');
    mountScore();

    cy.get('[data-testid="stroke-FOREHAND"]').click();
    cy.get('[data-testid="stroke-FOREHAND"]').should('have.attr', 'aria-pressed', 'true');
    cy.get('[data-testid="stroke-FOREHAND"]').click();
    cy.get('[data-testid="stroke-FOREHAND"]').should('have.attr', 'aria-pressed', 'false');

    cy.get('[data-testid="obs-WINNER"]').click();
    cy.wait('@recordPoint').its('request.body.strokeType').should('eq', null);
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
    cy.get('[data-testid="panel"]').should('not.exist');
    cy.get('[data-testid="switch-player-1"]').should('not.exist');
  });

  it('setzt den Aufschläger und zeigt danach die Panels', () => {
    cy.intercept('POST', '**/api/matches/match-1/serve/player1', {
      body: { ...makeMatch().score, servingPlayer: 1 },
    }).as('setServe');
    mountScore(makeMatch({ score: { ...makeMatch().score, servingPlayer: null } }));
    cy.get('[data-testid="serve-pick-1"]').click();
    cy.wait('@setServe');
    cy.get('[data-testid="serve-picker"]').should('not.exist');
    cy.get('[data-testid="panel"]').should('be.visible');
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

  it('schneidet sehr lange Spielernamen ab, statt die Zeile zu sprengen', () => {
    cy.viewport(360, 692);
    const long1: Player = { ...PLAYER1, firstName: 'Roger', lastName: 'Abcdefghijklmnopqrstuvwxyzabcdefghijklmn' };
    const long2: Player = { ...PLAYER2, firstName: 'Rafael', lastName: 'Zyxwvutsrqponmlkjihgfedcbazyxwvutsrqpon' };
    mountScore(makeMatch(), long1, long2);
    cy.get('[data-testid="score-header"]').invoke('outerHeight').should('be.lte', 100);
    cy.get('[data-testid="score-header"]').then($header => {
      const row = $header.find('.header-row')[0].getBoundingClientRect();
      Array.from($header.find('.header-row')[0].children).forEach(c => {
        const r = c.getBoundingClientRect();
        expect(r.left, `${c.className} links`).to.be.gte(row.left - 0.5);
        expect(r.right, `${c.className} rechts`).to.be.lte(row.right + 0.5);
      });
      expect($header.find('[data-testid="quick-score-p2"]')[0].getBoundingClientRect().right, 'p2 rechts').to.be.lte(360);
      // Der Name selbst bleibt in seiner Spaltenhälfte (abgeschnitten, nicht überlaufend)
      $header.find('.player-half').each((i, half) => {
        const h = half.getBoundingClientRect();
        const n = half.querySelector('.player-name')!.getBoundingClientRect();
        expect(n.left, `Name ${i + 1} links`).to.be.gte(h.left - 0.5);
        expect(n.right, `Name ${i + 1} rechts`).to.be.lte(h.right + 0.5);
        // Der Text ragt über den Namenskasten hinaus (Fixture ist wirklich zu lang) und
        // wird dort beschnitten, nicht in die Nachbarspalte gezeichnet
        const name = half.querySelector('.player-name') as HTMLElement;
        const cs = getComputedStyle(name);
        expect(name.scrollWidth, `Name ${i + 1} ist länger als sein Kasten`).to.be.gt(name.clientWidth);
        expect(cs.overflow, `Name ${i + 1} overflow`).to.equal('hidden');
        expect(cs.textOverflow, `Name ${i + 1} text-overflow`).to.equal('ellipsis');
      });
    });
    cy.document().its('body').then(b => expect(b.scrollWidth, 'Body-Überlauf').to.be.at.most(b.clientWidth));
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

// ─── Ein Panel, rollenabhängig, mit Spielerumschalter ──────────────────────

describe('ScoreComponent — ein Panel', () => {
  beforeEach(() => {
    cy.viewport(360, 692);
    mountScore();   // servingPlayer: 1 → Roger Federer schlägt auf
  });

  it('zeigt genau ein Panel', () => {
    cy.get('[data-testid="panel"]').should('have.length', 1);
  });

  it('zeigt beim Aufschläger Ass, Doppelfehler und die Service-Wahl', () => {
    cy.get('[data-testid="obs-ACE"]').should('be.visible');
    cy.get('[data-testid="obs-DOUBLE_FAULT"]').should('be.visible');
    cy.get('[data-testid="serve-attempt-1"]').should('be.visible');
  });

  it('zeigt beim Returnierenden weder Ass noch Doppelfehler noch Service-Wahl', () => {
    cy.get('[data-testid="switch-player-2"]').click();
    cy.get('[data-testid="panel"]').should('contain', 'Rafael Nadal');
    cy.get('[data-testid="obs-ACE"]').should('not.exist');
    cy.get('[data-testid="obs-DOUBLE_FAULT"]').should('not.exist');
    cy.get('[data-testid="serve-attempt-1"]').should('not.exist');
    cy.get('[data-testid="obs-WINNER"]').should('be.visible');
    cy.get('[data-testid="obs-NET"]').should('be.visible');
  });

  it('gibt jeder Beobachtungs-Kachel mindestens 56px Höhe', () => {
    cy.viewport(360, 692);
    cy.get('[data-testid^="obs-"]').each($el => {
      expect($el[0].getBoundingClientRect().height).to.be.at.least(56);
    });
  });

  it('gibt auch dem Returnierenden-Panel Kacheln von mindestens 56px', () => {
    cy.viewport(360, 692);
    cy.get('[data-testid="switch-player-2"]').click();
    cy.get('[data-testid^="obs-"]').each($el => {
      expect($el[0].getBoundingClientRect().height).to.be.at.least(56);
    });
  });

  // 56px ist eine Untergrenze, keine Aufforderung, den Restplatz aufzufüllen:
  // die Kacheln behalten ihre natürliche Höhe, übriger Platz bleibt leer.
  it('streckt die Kacheln des Aufschläger-Panels nicht über ihre natürliche Höhe', () => {
    cy.viewport(360, 692);
    cy.get('[data-testid^="obs-"]').each($el => {
      const h = $el[0].getBoundingClientRect().height;
      expect(h, `${$el.attr('data-testid')} Höhe`).to.be.within(56, 96);
    });
  });

  it('streckt die Kacheln des Returnierenden-Panels nicht über ihre natürliche Höhe', () => {
    cy.viewport(360, 692);
    cy.get('[data-testid="switch-player-2"]').click();
    cy.get('[data-testid^="obs-"]').each($el => {
      const h = $el[0].getBoundingClientRect().height;
      expect(h, `${$el.attr('data-testid')} Höhe`).to.be.within(56, 96);
    });
  });

  // Die Zusicherung haengt am Dokument, nicht an .panels-area: seit die Seite
  // ihre natuerliche Hoehe behaelt (overflow sichtbar), waere eine Aussage
  // ueber .panels-area trivial erfuellt. Im Hochformat darf nichts scrollen.
  it('scrollt auf 360x692 nicht', () => {
    cy.viewport(360, 692);
    cy.get('[data-testid="panel"]').should('be.visible');
    cy.document().then(doc => {
      const de = doc.documentElement;
      expect(de.scrollHeight, 'Dokumenthoehe').to.be.at.most(de.clientHeight + 1);
    });
  });

  // I1: Im Querformat (~360px hoch) passt das Aufschlaeger-Panel nicht in die
  // Erfassungsflaeche. Ohne Ausweg schneidet overflow:hidden die unteren
  // Zeilen ab — Forced/Unforced/Netz und die Schlagart sind dann unerreichbar.
  // Geprueft wird das Clipping selbst, nicht der Klick: Cypress' .click()
  // scrollt vorher per scrollIntoView, und das funktioniert auch in einem
  // overflow:hidden-Container, den der Benutzer gar nicht scrollen kann.
  it('schneidet im Querformat (800x360) keine Kachel unerreichbar ab', () => {
    cy.viewport(800, 360);
    mountScore();

    for (const id of ['obs-NET', 'stroke-BACKHAND']) {
      assertNotClipped(id);
    }
  });

  it('laesst im Querformat (800x360) die untere Kachel erfassen', () => {
    cy.viewport(800, 360);
    cy.intercept('POST', '**/api/matches/match-1/points', makeUpdatedScore(0, 1)).as('recordPoint');
    mountScore();

    cy.get('[data-testid="stroke-BACKHAND"]').click();
    cy.get('[data-testid="obs-NET"]').click();
    cy.wait('@recordPoint').its('request.body.pointType').should('eq', 'NET');
  });

  // I1b: Der fruehere Ausweg griff erst unter 561px Viewporthoehe. Das
  // Aufschlaeger-Panel braucht aber bis ~577px, darunter schnitt
  // overflow:hidden die Schlagart-Zeile ohne Scrollmoeglichkeit ab.
  // 375x568 (iPhone SE/5s im Hochformat) liegt in dieser Luecke.
  it('laesst bei 375x568 die unteren Kacheln erreichen', () => {
    cy.viewport(375, 568);
    cy.intercept('POST', '**/api/matches/match-1/points', makeUpdatedScore(0, 1)).as('recordPoint');
    mountScore();

    for (const id of ['obs-NET', 'stroke-BACKHAND']) {
      assertNotClipped(id);
    }

    cy.get('[data-testid="stroke-BACKHAND"]').click();
    cy.get('[data-testid="obs-NET"]').click();
    cy.wait('@recordPoint').its('request.body.pointType').should('eq', 'NET');
  });

  it('gibt den Umschaltflächen mindestens 56px Höhe', () => {
    cy.viewport(360, 692);
    cy.get('[data-testid="switch-player-1"]').invoke('outerHeight').should('be.gte', 56);
    cy.get('[data-testid="switch-player-2"]').invoke('outerHeight').should('be.gte', 56);
  });

  it('markiert den aktiven Spieler per aria-pressed', () => {
    cy.get('[data-testid="switch-player-1"]').should('have.attr', 'aria-pressed', 'true');
    cy.get('[data-testid="switch-player-2"]').should('have.attr', 'aria-pressed', 'false');
    cy.get('[data-testid="switch-player-2"]').click();
    cy.get('[data-testid="switch-player-1"]').should('have.attr', 'aria-pressed', 'false');
    cy.get('[data-testid="switch-player-2"]').should('have.attr', 'aria-pressed', 'true');
  });

  it('zeigt auch bei 1280x900 genau ein Panel', () => {
    cy.viewport(1280, 900);
    cy.get('[data-testid="panel"]').should('have.length', 1);
    cy.get('[data-testid="switch-player-1"]').should('be.visible');
  });
});

describe('ScoreComponent — Panel folgt dem Aufschlag', () => {
  it('folgt dem Aufschlagwechsel nach einem Game', () => {   // Review Focus 2
    cy.viewport(360, 692);
    // Punkt, der das Game beendet: die Antwort nennt Spieler 2 als Aufschläger
    const afterGame = makeMatch({
      score: { ...makeMatch().score, gamesPlayer1: 1, servingPlayer: 2 },
    });
    cy.intercept('POST', '**/api/matches/match-1/points', afterGame).as('point');
    mountScore();

    cy.get('[data-testid="panel"]').should('contain', 'Roger Federer');
    cy.get('[data-testid="obs-WINNER"]').click();
    cy.wait('@point');

    cy.get('[data-testid="panel"]').should('contain', 'Rafael Nadal');
    cy.get('[data-testid="switch-player-2"]').should('have.attr', 'aria-pressed', 'true');
  });

  it('kehrt nach einem erfassten Punkt zum Aufschläger zurück', () => {
    cy.viewport(360, 692);
    cy.intercept('POST', '**/api/matches/match-1/points', makeUpdatedScore(0, 1)).as('point');
    mountScore();

    cy.get('[data-testid="switch-player-2"]').click();
    cy.get('[data-testid="panel"]').should('contain', 'Rafael Nadal');
    cy.get('[data-testid="obs-UNFORCED_ERROR"]').click();
    cy.wait('@point');

    cy.get('[data-testid="panel"]').should('contain', 'Roger Federer');
    cy.get('[data-testid="switch-player-1"]').should('have.attr', 'aria-pressed', 'true');
  });
});

// ─── Untere Leiste, Notizen-Vollbild, beendetes Match ─────────────────────

// I3: Die Breite steckt am Aufrufort (dialog.open), nicht in der Dialog-
// Komponente — end-match-dialog.component.cy.ts mountet sie direkt und kann
// das deshalb nicht pruefen.
describe('ScoreComponent — Beenden-Dialog responsiv', () => {
  function openEndMatchDialog(width: number, height: number) {
    cy.viewport(width, height);
    mountScore();
    cy.get('[data-testid="end-match-btn"]').click();
    cy.get('.mat-mdc-dialog-surface').should('be.visible');
  }

  it('fuellt bei 390x736 die volle Breite', () => {
    openEndMatchDialog(390, 736);
    cy.get('.mat-mdc-dialog-surface').invoke('outerWidth').should('be.gte', 380);
  });

  it('bleibt bei 1280x900 bei rund 500px', () => {
    openEndMatchDialog(1280, 900);
    cy.get('.mat-mdc-dialog-surface').invoke('outerWidth').should('be.within', 480, 520);
  });
});

describe('ScoreComponent — untere Leiste und Notizen', () => {
  beforeEach(() => cy.viewport(360, 692));

  it('zeigt unten die zuletzt erfasste Beobachtung und den Notiz-Button', () => {
    mountScore();
    cy.intercept('POST', '**/api/matches/match-1/points', makeUpdatedScore(1, 0)).as('recordPoint');
    cy.get('[data-testid="last-observation"]').should('not.contain', 'Winner');
    cy.get('[data-testid="obs-WINNER"]').click();
    cy.wait('@recordPoint');
    cy.get('[data-testid="last-observation"]').should('contain', 'Winner');
    cy.get('[data-testid="notes-btn"]').should('be.visible')
      .invoke('outerHeight').should('be.gte', 40);
  });

  // M5: Ein Schnellpunkt traegt keine Beobachtung — die Leiste darf danach
  // nicht weiter die Beobachtung des vorherigen Punktes behaupten.
  it('behauptet nach einem Schnellpunkt nicht mehr die alte Beobachtung', () => {
    mountScore();
    cy.intercept('POST', '**/api/matches/match-1/points', makeUpdatedScore(1, 0)).as('recordPoint');
    cy.get('[data-testid="obs-WINNER"]').click();
    cy.wait('@recordPoint');
    cy.get('[data-testid="last-observation"]').should('contain', 'Winner');

    cy.get('[data-testid="quick-score-p1"]').click();
    cy.wait('@recordPoint');
    cy.get('[data-testid="last-observation"]').should('not.contain', 'Winner');
  });

  it('öffnet die Notizen als Vollbild und kehrt zurück', () => {
    mountScore();
    cy.get('[data-testid="notes-btn"]').click();
    cy.get('[data-testid="player-notes"]').should('be.visible');
    cy.get('[data-testid="panel"]').should('not.exist');
    cy.get('[data-testid="notes-back"]').click();
    cy.get('[data-testid="panel"]').should('be.visible');
    cy.get('[data-testid="player-notes"]').should('not.exist');
  });

  // Die Notizen fuellen die Seite: sonst klebt die Kopfzeile mitten im Bild
  // und darunter steht leerer Hintergrund. Haengt daran, dass .notes-view
  // seine Hoehe nicht per Prozent von einer auto-hohen Seite ableitet.
  it('fuellt im Vollbild die ganze Seitenhoehe', () => {
    mountScore();
    cy.get('[data-testid="notes-btn"]').click();
    cy.get('[data-testid="notes-view"]').then($el => {
      const el = $el[0];
      const viewportH = el.ownerDocument.documentElement.clientHeight;
      expect(el.getBoundingClientRect().height, 'Hoehe der Notizen-Ansicht')
        .to.be.at.least(viewportH - 1);
    });
  });

  it('gibt dem Notiz-Button einen Punkt, wenn Notizen existieren', () => {
    mountScore(makeMatch(), PLAYER1, PLAYER2, [{ playerId: 'p1', note: 'Rückhand schwach' }]);
    cy.get('[data-testid="notes-dot"]').should('exist');
  });

  it('zeigt ohne Notizen keinen Punkt', () => {
    mountScore();
    cy.get('[data-testid="notes-btn"]').should('be.visible');
    cy.get('[data-testid="notes-dot"]').should('not.exist');
  });

  it('zeigt bei beendetem Match weder Panel noch Umschalter', () => {
    mountScore(makeMatch({ status: 'COMPLETED', score: { ...makeMatch().score, isDone: true, winner: 'PLAYER1' } }));
    cy.get('[data-testid="panel"]').should('not.exist');
    cy.get('[data-testid="switch-player-1"]').should('not.exist');
    cy.contains('Sieger').should('be.visible');
  });

  it('bietet bei beendetem Match mit Siegerüberlag die Notizen in der Sieger-Karte an', () => {
    mountScore(makeMatch({ status: 'COMPLETED', score: { ...makeMatch().score, isDone: true, winner: 'PLAYER1' } }));
    cy.get('[data-testid="panel"]').should('not.exist');
    cy.get('[data-testid="switch-player-1"]').should('not.exist');
    cy.get('[data-testid="notes-btn"]').should('not.exist');
    cy.get('.winner-card [data-testid="winner-notes-btn"]').should('be.visible').click();
    cy.get('[data-testid="player-notes"]').should('be.visible');
    cy.get('[data-testid="notes-back"]').click();
    cy.get('[data-testid="player-notes"]').should('not.exist');
    cy.get('.winner-card').should('be.visible');
  });

  it('hält die Notizen bei beendetem Match ohne Überlay über die untere Leiste erreichbar', () => {
    mountScore(makeMatch({ status: 'COMPLETED', score: { ...makeMatch().score, isDone: false } }));
    cy.get('[data-testid="panel"]').should('not.exist');
    cy.get('[data-testid="switch-player-1"]').should('not.exist');
    cy.get('[data-testid="last-observation"]').should('not.exist');
    cy.get('.winner-overlay').should('not.exist');
    cy.get('[data-testid="notes-btn"]').should('be.visible').click();
    cy.get('[data-testid="player-notes"]').should('be.visible');
    cy.get('[data-testid="notes-back"]').click();
    cy.get('[data-testid="player-notes"]').should('not.exist');
  });
});
