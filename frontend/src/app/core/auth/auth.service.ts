import { Injectable, inject, computed, signal } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { authConfig } from './auth.config';

interface AccessClaims {
  sub?: string;
  realm_access?: { roles?: string[] };
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly oauthService = inject(OAuthService);
  private initPromise: Promise<boolean> | null = null;

  /** Increments on every successful (re-)auth event so role-derived signals re-read the access token. */
  private readonly tokenVersion = signal(0);

  /** Realm roles from the access token (Keycloak `realm_access.roles`). Empty when unauthenticated. */
  readonly roles = computed<string[]>(() => {
    this.tokenVersion();
    const claims = this.accessClaims();
    return claims?.realm_access?.roles ?? [];
  });

  /** True when the current user has the `ADMIN` realm role. */
  readonly isAdmin = computed(() => this.roles().includes('ADMIN'));

  /** Sub-claim (= internal user id used as `owner_id` on persisted entities). */
  readonly userId = computed<string | null>(() => {
    this.tokenVersion();
    return this.accessClaims()?.sub ?? null;
  });

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
    this.tokenVersion.update(v => v + 1);
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

  private accessClaims(): AccessClaims | null {
    const token = this.oauthService.getAccessToken();
    if (!token) return null;
    const payload = token.split('.')[1];
    if (!payload) return null;
    try {
      const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(json) as AccessClaims;
    } catch {
      return null;
    }
  }
}
