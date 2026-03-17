export type Gender = 'MALE' | 'FEMALE' | 'OTHER';
export type Handedness = 'LEFT' | 'RIGHT';
export type BackhandType = 'ONE_HANDED' | 'TWO_HANDED';

export interface Player {
  id: string;
  firstName: string;
  lastName: string;
  gender: Gender;
  handedness: Handedness;
  backhandType: BackhandType;
  ranking?: string;
  nationality?: string;
  birthDate?: string;
  active?: boolean;
  deletable?: boolean;
}

export interface CreatePlayerRequest {
  firstName: string;
  lastName: string;
  gender: Gender;
  handedness: Handedness;
  backhandType: BackhandType;
  ranking?: string;
  nationality?: string;
  birthDate?: string;
}
