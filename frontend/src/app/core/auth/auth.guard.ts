import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = async () => {
  const authService = inject(AuthService);
  const authenticated = await authService.initialize();
  if (!authenticated) {
    authService.login();
    return false;
  }
  return true;
};
