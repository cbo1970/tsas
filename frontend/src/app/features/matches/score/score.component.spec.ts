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

  // ── toggleStroke ─────────────────────────────────────────────────────────────

  it('should set strokeType when toggled on', () => {
    component.toggleStroke(1, 'FOREHAND');
    expect(component.strokeTypeP1()).toBe('FOREHAND');
  });

  it('should clear strokeType when toggled off (same value)', () => {
    component.toggleStroke(1, 'BACKHAND');
    component.toggleStroke(1, 'BACKHAND');
    expect(component.strokeTypeP1()).toBeNull();
  });

  it('should toggle strokeType independently for each panel', () => {
    component.toggleStroke(1, 'FOREHAND');
    component.toggleStroke(2, 'BACKHAND');
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

  it('should set serving player on recordQuickPoint when no server set (player 1)', () => {
    component.matchData.set({ ...MOCK_MATCH, score: { ...MOCK_SCORE, servingPlayer: null } as any });
    mockApi['setServingPlayer1'].mockReturnValue(of({ ...MOCK_SCORE, servingPlayer: 1 }));
    component.recordQuickPoint(1);
    expect(mockApi['setServingPlayer1']).toHaveBeenCalled();
    expect(mockApi['recordPoint']).not.toHaveBeenCalled();
  });

  it('should set serving player 2 on recordQuickPoint when no server set', () => {
    component.matchData.set({ ...MOCK_MATCH, score: { ...MOCK_SCORE, servingPlayer: null } as any });
    mockApi['setServingPlayer2'].mockReturnValue(of({ ...MOCK_SCORE, servingPlayer: 2 }));
    component.recordQuickPoint(2);
    expect(mockApi['setServingPlayer2']).toHaveBeenCalled();
    expect(mockApi['recordPoint']).not.toHaveBeenCalled();
  });

  it('should not record point when match is completed (recordQuickPoint)', () => {
    component.matchData.set({ ...MOCK_MATCH, status: 'COMPLETED' });
    component.recordQuickPoint(1);
    expect(mockApi['recordPoint']).not.toHaveBeenCalled();
  });

  // ── recordObservation ─────────────────────────────────────────────────────────

  it('should call api.recordPoint with pointType and context for recordObservation', () => {
    mockApi['recordPoint'].mockReturnValue(of(MOCK_MATCH));
    component.toggleStroke(1, 'BACKHAND');
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
    component.toggleStroke(1, 'FOREHAND');
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

  it('should render both player panels regardless of serving state', () => {
    component.matchData.set({ ...MOCK_MATCH, score: { ...MOCK_SCORE, servingPlayer: 1 } as any });
    fixture.detectChanges();
    const panels = fixture.debugElement.queryAll(By.css('[data-testid^="panel-"]'));
    expect(panels).toHaveLength(2);
  });
});
