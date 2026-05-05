export type PointType =
  | 'WINNER' | 'UNFORCED_ERROR' | 'FORCED_ERROR'
  | 'ACE' | 'DOUBLE_FAULT' | 'NET' | 'OUT_LONG' | 'OUT_SIDE';

export type StrokeType = 'FOREHAND' | 'BACKHAND' | 'SERVE' | 'VOLLEY' | 'SMASH';

export type Direction = 'CROSS_COURT' | 'DOWN_THE_LINE' | 'MIDDLE';

export interface RecordPointRequest {
  winner: 1 | 2;
  pointType: PointType;
  strokeType?: StrokeType;
  direction?: Direction;
  remark?: string;
}
