import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'players', pathMatch: 'full' },
  {
    path: 'players',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/players/players.component').then(m => m.PlayersComponent)
  },
  {
    path: 'matches/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/matches/match-setup/match-setup.component').then(m => m.MatchSetupComponent)
  },
  {
    path: 'matches/:id/score',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/matches/score/score.component').then(m => m.ScoreComponent)
  },
  {
    path: 'matches/:id/statistics',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/matches/statistics/statistics.component').then(m => m.StatisticsComponent)
  },
  {
    path: 'matches/:id/analysis',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/matches/analysis/match-analysis.component').then(m => m.MatchAnalysisComponent)
  }
];
