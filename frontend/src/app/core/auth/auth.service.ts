import { Injectable, inject } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { authConfig } from './auth.config';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly oauthService = inject(OAuthService);
  private initPromise: Promise<boolean> | null = null;

  initialize(): Promise<boolean> {
    if (!this.initPromise) {
      this.initPromise = this.doInit();
    }
    return this.initPromise;
  }

  private async doInit(): Promise<boolean> {
    this.oauthService.configure(authConfig);
    try {
      await this.oauthService.loadDiscoveryDocumentAndTryLogin();
    } catch {
      this.oauthService.logOut(true);
      return false;
    }
    if (this.oauthService.hasValidAccessToken()) {
      this.oauthService.setupAutomaticSilentRefresh();
    }
    return this.oauthService.hasValidAccessToken();
  }

  login(): void {
    this.oauthService.initCodeFlow();
  }

  logout(): void {
    this.oauthService.logOut();
  }

  isAuthenticated(): boolean {
    return this.oauthService.hasValidAccessToken();
  }

  userName(): string {
    const claims = this.oauthService.getIdentityClaims() as { name?: string } | null;
    return claims?.name ?? '';
  }
}
