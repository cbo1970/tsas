export type PointType =
  | 'WINNER' | 'UNFORCED_ERROR' | 'FORCED_ERROR'
  | 'ACE' | 'DOUBLE_FAULT' | 'NET';

export type StrokeType = 'FOREHAND' | 'BACKHAND';

export interface RecordPointRequest {
  winner: 1 | 2;
  pointType?: PointType | null;
  strokeType?: StrokeType | null;
  serveAttempt?: 1 | 2 | null;
  remark?: string;
}
