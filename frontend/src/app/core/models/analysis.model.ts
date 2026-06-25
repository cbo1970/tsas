export type AnalysisStatus = 'PENDING' | 'COMPLETED' | 'FAILED';

export type RecommendationReviewStatus = 'OPEN' | 'ACCEPTED' | 'REJECTED';

export interface AnalysisRecommendation {
  priority: number;
  title: string;
  detail: string;
  status: RecommendationReviewStatus;
  reviewNote: string | null;
  reviewedAt: string | null;
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

/** TEN-51 — KI-Vorbereitung gegen einen Gegner (Head-to-Head-basiert). */
export interface OpponentPreparation {
  ownPlayerId: string;
  opponentId: string;
  matchesPlayed: number;
  opponentProfile: string;
  tacticalObservations: string;
  serveStrategy: string;
  returnStrategy: string;
  recommendations: AnalysisRecommendation[];
  modelUsed: string;
  generatedAt: string;
}
