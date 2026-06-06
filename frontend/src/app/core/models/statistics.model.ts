export interface PlayerStatistics {
  pointsWon: number;
  winners: number;
  unforcedErrors: number;
  forcedErrors: number;
  aces: number;
  doubleFaults: number;
  firstServePercentage: number;
  secondServePercentage: number;
  breakPointsWon: number;
  breakPointsFaced: number;
  forehandPercentage: number;
}

export interface MatchStatistics {
  matchId: string;
  player1: PlayerStatistics;
  player2: PlayerStatistics;
  totalPoints: number;
}
