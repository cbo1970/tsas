export type MatchStatus = 'IN_PROGRESS' | 'COMPLETED';

export interface Match {
  id: string;
  ownerId: string;
  player1Id: string;
  player2Id: string;
  setsToWin: number;
  matchTiebreak: boolean;
  shortSet: boolean;
  status: MatchStatus;
}

export interface MatchScore {
  id: string;
  matchId: string;
  pointsPlayer1: number;
  pointsPlayer2: number;
  gamesPlayer1: number;
  gamesPlayer2: number;
  setsPlayer1: number;
  setsPlayer2: number;
  isDeuce: boolean;
  isAdvantagePlayer1: boolean | null;
  currentSet: number;
  isDone: boolean;
  winner: string | null;
  acesPlayer1: number;
  acesPlayer2: number;
  servingPlayer: number | null;
}

export interface MatchWithScore extends Match {
  score: MatchScore;
}

export interface CreateMatchRequest {
  player1Id: string;
  player2Id: string;
  setsToWin: number;
  matchTiebreak: boolean;
  shortSet: boolean;
}

export interface SetScoreRequest {
  pointsPlayer1: number;
  pointsPlayer2: number;
  gamesPlayer1: number;
  gamesPlayer2: number;
  setsPlayer1: number;
  setsPlayer2: number;
  isDeuce: boolean;
  isAdvantagePlayer1: boolean | null;
  currentSet: number;
  isDone: boolean;
  winner: string | null;
}

export interface MatchHistoryEntry {
  matchId: string;
  opponentName: string;
  setsWon: number;
  setsLost: number;
  won: boolean;
  completedAt: string;
}
