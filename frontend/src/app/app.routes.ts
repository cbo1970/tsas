import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'players', pathMatch: 'full' },
  {
    path: 'players',
    loadComponent: () =>
      import('./features/players/players.component').then(m => m.PlayersComponent)
  },
  {
    path: 'matches/new',
    loadComponent: () =>
      import('./features/matches/match-setup/match-setup.component').then(m => m.MatchSetupComponent)
  },
  {
    path: 'matches/:id/score',
    loadComponent: () =>
      import('./features/matches/score/score.component').then(m => m.ScoreComponent)
  }
];
