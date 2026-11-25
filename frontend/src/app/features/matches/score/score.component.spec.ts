// frontend/src/app/features/matches/score/score.component.spec.ts
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { By } from '@angular/platform-browser';
import { ScoreComponent } from './score.component';
import { ApiService } from '../../../core/services/api.service';
import { MatchWithScore } from '../../../core/models/match.model';
import { PointType, StrokeType } from '../../../core/models/point.model';
import { testTranslateProviders } from '../../../core/i18n/test-providers';

const MOCK_SCORE = {
  matchId: 'match-1',
  pointsPlayer1: 0, pointsPlayer2: 0,
  gamesPlayer1: 0, gamesPlayer2: 0,
  setsPlayer1: 0, setsPlayer2: 0,
  isDeuce: false, isAdvantagePlayer1: null,
  currentSet: 1, isDone: false, winner: null,
  acesPlayer1: 0, acesPlayer2: 0,
  servingPlayer: 2,
};

const MOCK_MATCH: MatchWithScore = {
  id: 'match-1', ownerId: 'owner-1', player1Id: 'p1', player2Id: 'p2',
  setsToWin: 2, matchTiebreak: false, shortSet: false,
  status: 'IN_PROGRESS', score: MOCK_SCORE as any,
};

describe('ScoreComponent — inline scoring', () => {
  let fixture: ComponentFixture<ScoreComponent>;
  let component: ScoreComponent;
  let mockApi: Record<string, ReturnType<typeof vi.fn>>;
  let mockDialog: Record<string, ReturnType<typeof vi.fn>>;
  let mockSnackBar: Record<string, ReturnType<typeof vi.fn>>;
  let mockRouter: Record<string, ReturnType<typeof vi.fn>>;

  beforeEach(async () => {
    mockApi = {
      getMatch: vi.fn().mockReturnValue(of(MOCK_MATCH)),
      getPlayer: vi.fn().mockReturnValue(of({ id: 'p1', firstName: 'Anna', lastName: 'Müller' })),
      // embedded PlayerNotesComponent fires this on init (rendered even inside a collapsed <details>)
      getPlayerNotes: vi.fn().mockReturnValue(of([])),
      recordPoint: vi.fn(),
      setServingPlayer1: vi.fn(),
      setServingPlayer2: vi.fn(),
      setScore: vi.fn(),
      endMatchWalkover: vi.fn(),
    };
    mockDialog = { open: vi.fn() };
    mockSnackBar = { open: vi.fn() };
    mockRouter = { navigate: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [ScoreComponent],
      providers: [
        { provide: ApiService,     useValue: mockApi },
        { provide: MatDialog,      useValue: mockDialog },
        { provide: MatSnackBar,    useValue: mockSnackBar },
        { provide: Router,         useValue: mockRouter },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'match-1' } } } },
        ...testTranslateProviders,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ScoreComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ── Signal defaults ─────────────────────────────────────────────────────────

  it('should default strokeType to null for both players', () => {
    expect(component.strokeTypeP1()).toBeNull();
    expect(component.strokeTypeP2()).toBeNull();
  });

  it('should default serviceContext to null for both players', () => {
    expect(component.serviceContextP1()).toBeNull();
    expect(component.serviceContextP2()).toBeNull();
  });

  // ── setStroke ────────────────────────────────────────────────────────────────

  it('should set strokeType for the given panel', () => {
    component.setStroke(1, 'FOREHAND');
    expect(component.strokeTypeP1()).toBe('FOREHAND');
  });

  it('should clear strokeType when the panel emits null', () => {
    component.setStroke(1, 'BACKHAND');
    component.setStroke(1, null);
    expect(component.strokeTypeP1()).toBeNull();
  });

  it('should hold strokeType independently for each panel', () => {
    component.setStroke(1, 'FOREHAND');
    component.setStroke(2, 'BACKHAND');
    expect(component.strokeTypeP1()).toBe('FOREHAND');
    expect(component.strokeTypeP2()).toBe('BACKHAND');
  });

  // ── toggleService ─────────────────────────────────────────────────────────────

  it('should set serviceContext when toggled on', () => {
    component.toggleService(1, 1);
    expect(component.serviceContextP1()).toBe(1);
  });

  it('should clear serviceContext when toggled off (same value)', () => {
    component.toggleService(2, 2);
    component.toggleService(2, 2);
    expect(component.serviceContextP2()).toBeNull();
  });

  // ── recordQuickPoint ──────────────────────────────────────────────────────────

  it('should call api.recordPoint without pointType for recordQuickPoint', () => {
    mockApi['recordPoint'].mockReturnValue(of(MOCK_MATCH));
    component.recordQuickPoint(1);
    expect(mockDialog['open']).not.toHaveBeenCalled();
    expect(mockApi['recordPoint']).toHaveBeenCalledWith('match-1', { winner: 1 });
  });

  it('should call api.recordPoint for player 2 via recordQuickPoint', () => {
    mockApi['recordPoint'].mockReturnValue(of(MOCK_MATCH));
    component.recordQuickPoint(2);
    expect(mockApi['recordPoint']).toHaveBeenCalledWith('match-1', { winner: 2 });
  });

  it('pickServer(1) sets player 1 as server and patches the score', () => {
    component.matchData.set({ ...MOCK_MATCH, score: { ...MOCK_SCORE, servingPlayer: null } as any });
    mockApi['setServingPlayer1'].mockReturnValue(of({ ...MOCK_SCORE, servingPlayer: 1 }));
    component.pickServer(1);
    expect(mockApi['setServingPlayer1']).toHaveBeenCalledWith('match-1');
    expect(component.servingPlayer()).toBe(1);
  });

  it('pickServer(2) sets player 2 as server', () => {
    component.matchData.set({ ...MOCK_MATCH, score: { ...MOCK_SCORE, servingPlayer: null } as any });
    mockApi['setServingPlayer2'].mockReturnValue(of({ ...MOCK_SCORE, servingPlayer: 2 }));
    component.pickServer(2);
    expect(mockApi['setServingPlayer2']).toHaveBeenCalledWith('match-1');
    expect(component.servingPlayer()).toBe(2);
  });

  it('recordQuickPoint without a server no longer sets the server implicitly', () => {
    component.matchData.set({ ...MOCK_MATCH, score: { ...MOCK_SCORE, servingPlayer: null } as any });
    mockApi['recordPoint'].mockReturnValue(of(MOCK_MATCH));
    component.recordQuickPoint(1);
    expect(mockApi['setServingPlayer1']).not.toHaveBeenCalled();
    expect(mockApi['setServingPlayer2']).not.toHaveBeenCalled();
  });

  it('should not record point when match is completed (recordQuickPoint)', () => {
    component.matchData.set({ ...MOCK_MATCH, status: 'COMPLETED' });
    component.recordQuickPoint(1);
    expect(mockApi['recordPoint']).not.toHaveBeenCalled();
  });

  // ── recordObservation ─────────────────────────────────────────────────────────

  it('should call api.recordPoint with pointType and context for recordObservation', () => {
    mockApi['recordPoint'].mockReturnValue(of(MOCK_MATCH));
    component.setStroke(1, 'BACKHAND');
    component.toggleService(1, 1);
    component.recordObservation(1, 'WINNER');
    expect(mockApi['recordPoint']).toHaveBeenCalledWith('match-1', {
      winner: 1,
      pointType: 'WINNER',
      serveAttempt: 1,
      strokeType: 'BACKHAND',
    });
  });

  it('should derive correct winner for error observation (opponent wins)', () => {
    mockApi['recordPoint'].mockReturnValue(of(MOCK_MATCH));
    component.recordObservation(1, 'UNFORCED_ERROR');
    expect(mockApi['recordPoint']).toHaveBeenCalledWith('match-1', expect.objectContaining({
      winner: 2,
      pointType: 'UNFORCED_ERROR',
    }));
  });

  it('should not record point when match is completed (recordObservation)', () => {
    component.matchData.set({ ...MOCK_MATCH, status: 'COMPLETED' });
    component.recordObservation(1, 'WINNER');
    expect(mockApi['recordPoint']).not.toHaveBeenCalled();
  });

  it('should reset context signals after successful recordObservation', () => {
    mockApi['recordPoint'].mockReturnValue(of(MOCK_MATCH));
    component.setStroke(1, 'FOREHAND');
    component.toggleService(1, 2);
    component.recordObservation(1, 'WINNER');
    expect(component.strokeTypeP1()).toBeNull();
    expect(component.serviceContextP1()).toBeNull();
  });

  it('should reject ACE if the panel is not serving', () => {
    // MOCK_SCORE has servingPlayer: 2, so panel 1 is NOT serving
    component.recordObservation(1, 'ACE');
    expect(mockApi['recordPoint']).not.toHaveBeenCalled();
  });

  it('should reject DOUBLE_FAULT if the panel is not serving', () => {
    component.recordObservation(1, 'DOUBLE_FAULT');
    expect(mockApi['recordPoint']).not.toHaveBeenCalled();
  });

  // ── DOM ───────────────────────────────────────────────────────────────────────

  it('should render the scoring page container', () => {
    const page = fixture.debugElement.query(By.css('.scoring-page'));
    expect(page).toBeTruthy();
  });

  it('should render exactly one observation panel', () => {
    component.matchData.set({ ...MOCK_MATCH, score: { ...MOCK_SCORE, servingPlayer: 1 } as any });
    fixture.detectChanges();
    const panels = fixture.debugElement.queryAll(By.css('[data-testid="panel"]'));
    expect(panels).toHaveLength(1);
  });

  // ── activePlayer folgt dem Aufschlag ─────────────────────────────────────────

  it('should start on the serving player', () => {
    expect(component.activePlayer()).toBe(2);   // MOCK_SCORE: servingPlayer 2
  });

  it('should follow the serve when it changes with a score response', () => {
    component.activePlayer.set(1);
    component.matchData.set({ ...MOCK_MATCH, score: { ...MOCK_SCORE, servingPlayer: 1 } as any });
    fixture.detectChanges();
    expect(component.activePlayer()).toBe(1);
  });

  it('should return to the serving player after a recorded point', () => {
    // frische Antwort (wie vom HTTP-Client), servingPlayer unverändert 2
    mockApi['recordPoint'].mockReturnValue(of({ ...MOCK_MATCH, score: { ...MOCK_SCORE } as any }));
    component.activePlayer.set(1);
    component.recordObservation(1, 'UNFORCED_ERROR');
    fixture.detectChanges();
    expect(component.activePlayer()).toBe(2);
  });

  it('should expose the active player panel inputs', () => {
    component.activePlayer.set(1);
    component.setStroke(1, 'FOREHAND');
    component.toggleService(1, 2);
    expect(component.activePlayerInfo().id).toBe('p1');
    expect(component.activeOpponentName()).toBe(component.player2Name());
    expect(component.activeStrokeType()).toBe('FOREHAND');
    expect(component.activeServeAttempt()).toBe(2);
  });
});
