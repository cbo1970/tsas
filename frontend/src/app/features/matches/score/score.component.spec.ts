// frontend/src/app/features/matches/score/score.component.spec.ts
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { ScoreComponent } from './score.component';
import { ApiService } from '../../../core/services/api.service';
import { MatchWithScore } from '../../../core/models/match.model';
import { PointType, StrokeType, Direction, RecordPointRequest } from '../../../core/models/point.model';

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
  id: 'match-1', player1Id: 'p1', player2Id: 'p2',
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

  it('should default strokeType to FOREHAND for both players', () => {
    expect(component.strokeTypeP1()).toBe('FOREHAND');
    expect(component.strokeTypeP2()).toBe('FOREHAND');
  });

  it('should default direction to CROSS_COURT for both players', () => {
    expect(component.directionP1()).toBe('CROSS_COURT');
    expect(component.directionP2()).toBe('CROSS_COURT');
  });

  it('should call api.recordPoint directly without opening a dialog', () => {
    mockApi['recordPoint'].mockReturnValue(of(MOCK_MATCH));
    component.recordPoint(1, 'WINNER');
    expect(mockDialog['open']).not.toHaveBeenCalled();
    expect(mockApi['recordPoint']).toHaveBeenCalledWith('match-1', {
      winner: 1, pointType: 'WINNER',
      strokeType: 'FOREHAND', direction: 'CROSS_COURT',
    });
  });

  it('should send player-specific pre-selection with the point', () => {
    mockApi['recordPoint'].mockReturnValue(of(MOCK_MATCH));
    component.strokeTypeP1.set('BACKHAND');
    component.directionP1.set('DOWN_THE_LINE');
    component.recordPoint(1, 'WINNER');
    expect(mockApi['recordPoint']).toHaveBeenCalledWith('match-1', {
      winner: 1, pointType: 'WINNER',
      strokeType: 'BACKHAND', direction: 'DOWN_THE_LINE',
    });
  });

  it('should set serving player on first tile click when no server set', () => {
    component.matchData.set({ ...MOCK_MATCH, score: { ...MOCK_SCORE, servingPlayer: null } as any });
    mockApi['setServingPlayer1'].mockReturnValue(of({ ...MOCK_SCORE, servingPlayer: 1 }));
    component.recordPoint(1, 'WINNER');
    expect(mockApi['setServingPlayer1']).toHaveBeenCalled();
    expect(mockApi['recordPoint']).not.toHaveBeenCalled();
    expect(component.matchData()?.score.servingPlayer).toBe(1);
  });

  it('should send player 2-specific pre-selection with the point', () => {
    mockApi['recordPoint'].mockReturnValue(of(MOCK_MATCH));
    component.strokeTypeP2.set('BACKHAND');
    component.directionP2.set('DOWN_THE_LINE');
    component.recordPoint(2, 'WINNER');
    expect(mockApi['recordPoint']).toHaveBeenCalledWith('match-1', {
      winner: 2, pointType: 'WINNER',
      strokeType: 'BACKHAND', direction: 'DOWN_THE_LINE',
    });
  });

  it('should set serving player 2 on first tile click when no server set', () => {
    component.matchData.set({ ...MOCK_MATCH, score: { ...MOCK_SCORE, servingPlayer: null } as any });
    mockApi['setServingPlayer2'].mockReturnValue(of({ ...MOCK_SCORE, servingPlayer: 2 }));
    component.recordPoint(2, 'WINNER');
    expect(mockApi['setServingPlayer2']).toHaveBeenCalled();
    expect(mockApi['recordPoint']).not.toHaveBeenCalled();
  });

  it('should not record point when match is completed', () => {
    component.matchData.set({ ...MOCK_MATCH, status: 'COMPLETED' });
    component.recordPoint(1, 'WINNER');
    expect(mockApi['recordPoint']).not.toHaveBeenCalled();
  });
});
