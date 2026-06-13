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

export interface HeadToHeadPlayerStats {
  playerId: string;
  firstServePercentage: number;
  firstServeWonPercentage: number;
  secondServeWonPercentage: number;
  aces: number;
  doubleFaults: number;
  returnPointsWonFirstPercentage: number;
  returnPointsWonSecondPercentage: number;
  breakPointsWon: number;
  breakPointsPlayed: number;
  breakPointsWonPercentage: number;
  returnGamesWonPercentage: number;
  winners: number;
  unforcedErrors: number;
  winnersPercentage: number;
  unforcedErrorPercentage: number;
  matchesWon: number;
  matchesLost: number;
  setsWon: number;
  setsLost: number;
}

export interface HeadToHeadStatistics {
  player1Id: string;
  player2Id: string;
  matchesPlayed: number;
  player1: HeadToHeadPlayerStats;
  player2: HeadToHeadPlayerStats;
}
