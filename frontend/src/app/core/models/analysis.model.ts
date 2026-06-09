export type AnalysisStatus = 'PENDING' | 'COMPLETED' | 'FAILED';

export interface AnalysisRecommendation {
  priority: number;
  title: string;
  detail: string;
}

export interface MatchAnalysis {
  matchId: string;
  status: AnalysisStatus;
  keyMoments: string;
  ownStrengths: string;
  ownWeaknesses: string;
  opponentStrengths: string;
  opponentWeaknesses: string;
  recommendations: AnalysisRecommendation[];
  modelUsed: string;
  generatedAt: string;
  errorMessage: string | null;
}
