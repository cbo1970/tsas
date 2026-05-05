import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Player, CreatePlayerRequest } from '../models/player.model';
import {
  Match,
  MatchWithScore,
  MatchScore,
  CreateMatchRequest,
  SetScoreRequest
} from '../models/match.model';
import { RecordPointRequest } from '../models/point.model';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiUrl + '/api';

  // Players
  getPlayers(): Observable<Player[]> {
    return this.http.get<Player[]>(`${this.base}/players`);
  }

  getPlayer(id: string): Observable<Player> {
    return this.http.get<Player>(`${this.base}/players/${id}`);
  }

  createPlayer(request: CreatePlayerRequest): Observable<Player> {
    return this.http.post<Player>(`${this.base}/players`, request);
  }

  updatePlayer(id: string, request: CreatePlayerRequest): Observable<Player> {
    return this.http.put<Player>(`${this.base}/players/${id}`, request);
  }

  deletePlayer(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/players/${id}`);
  }

  deactivatePlayer(id: string): Observable<void> {
    return this.http.patch<void>(`${this.base}/players/${id}/deactivate`, {});
  }

  // Matches
  getMatches(): Observable<Match[]> {
    return this.http.get<Match[]>(`${this.base}/matches`);
  }

  getMatch(id: string): Observable<MatchWithScore> {
    return this.http.get<MatchWithScore>(`${this.base}/matches/${id}`);
  }

  createMatch(request: CreateMatchRequest): Observable<Match> {
    return this.http.post<Match>(`${this.base}/matches`, request);
  }

  recordPoint(matchId: string, request: RecordPointRequest): Observable<MatchWithScore> {
    return this.http.post<MatchWithScore>(`${this.base}/matches/${matchId}/points`, request);
  }

  setServingPlayer1(matchId: string): Observable<MatchScore> {
    return this.http.post<MatchScore>(`${this.base}/matches/${matchId}/serve/player1`, {});
  }

  setServingPlayer2(matchId: string): Observable<MatchScore> {
    return this.http.post<MatchScore>(`${this.base}/matches/${matchId}/serve/player2`, {});
  }

  setScore(matchId: string, request: SetScoreRequest): Observable<MatchScore> {
    return this.http.put<MatchScore>(`${this.base}/matches/${matchId}/score`, request);
  }

  endMatch(matchId: string): Observable<Match> {
    return this.http.post<Match>(`${this.base}/matches/${matchId}/end`, {});
  }

  endMatchWalkover(matchId: string, winner: 'PLAYER1' | 'PLAYER2'): Observable<Match> {
    return this.http.post<Match>(`${this.base}/matches/${matchId}/end/walkover`, { winner });
  }
}
