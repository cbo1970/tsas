// frontend/src/app/features/matches/score/score.component.spec.ts
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { ScoreComponent } from './score.component';
import { ApiService } from '../../../core/services/api.service';
import { MatchWithScore } from '../../../core/models/match.model';

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
  let mockApi: jasmine.SpyObj<ApiService>;
  let mockDialog: jasmine.SpyObj<MatDialog>;
  let mockSnackBar: jasmine.SpyObj<MatSnackBar>;
  let mockRouter: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    mockApi = jasmine.createSpyObj('ApiService', [
      'getMatch', 'getPlayer', 'recordPoint',
      'setServingPlayer1', 'setServingPlayer2',
      'setScore', 'endMatchWalkover',
    ]);
    mockDialog = jasmine.createSpyObj('MatDialog', ['open']);
    mockSnackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

    mockApi.getMatch.and.returnValue(of(MOCK_MATCH));
    mockApi.getPlayer.and.returnValue(of({ id: 'p1', firstName: 'Anna', lastName: 'Müller' } as any));

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
    mockApi.recordPoint.and.returnValue(of(MOCK_MATCH));
    component.recordPoint(1, 'WINNER');
    expect(mockDialog.open).not.toHaveBeenCalled();
    expect(mockApi.recordPoint).toHaveBeenCalledWith('match-1', {
      winner: 1, pointType: 'WINNER',
      strokeType: 'FOREHAND', direction: 'CROSS_COURT',
    });
  });

  it('should send player-specific pre-selection with the point', () => {
    mockApi.recordPoint.and.returnValue(of(MOCK_MATCH));
    component.strokeTypeP1.set('BACKHAND');
    component.directionP1.set('DOWN_THE_LINE');
    component.recordPoint(1, 'WINNER');
    expect(mockApi.recordPoint).toHaveBeenCalledWith('match-1', {
      winner: 1, pointType: 'WINNER',
      strokeType: 'BACKHAND', direction: 'DOWN_THE_LINE',
    });
  });

  it('should set serving player on first tile click when no server set', () => {
    component.matchData.set({ ...MOCK_MATCH, score: { ...MOCK_SCORE, servingPlayer: null } as any });
    mockApi.setServingPlayer1.and.returnValue(of({ ...MOCK_SCORE, servingPlayer: 1 } as any));
    component.recordPoint(1, 'WINNER');
    expect(mockApi.setServingPlayer1).toHaveBeenCalled();
    expect(mockApi.recordPoint).not.toHaveBeenCalled();
  });
});
