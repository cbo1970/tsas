import { TestBed } from '@angular/core/testing';
import { OAuthService } from 'angular-oauth2-oidc';
import { describe, expect, it, beforeEach, vi } from 'vitest';

import { AuthService } from './auth.service';

/** Build a fake JWT with the given payload — header + signature are throwaway. */
function fakeAccessToken(payload: object): string {
  const enc = (obj: object) =>
    btoa(JSON.stringify(obj)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${enc({ alg: 'none' })}.${enc(payload)}.signature`;
}

describe('AuthService role + identity claims', () => {
  let oauth: { getAccessToken: ReturnType<typeof vi.fn> };
  let service: AuthService;

  beforeEach(() => {
    oauth = { getAccessToken: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: OAuthService, useValue: oauth }
      ],
    });
    service = TestBed.inject(AuthService);
  });

  it('returns empty roles and null userId when no token present', () => {
    oauth.getAccessToken.mockReturnValue(null);
    expect(service.roles()).toEqual([]);
    expect(service.isAdmin()).toBe(false);
    expect(service.userId()).toBeNull();
  });

  it('extracts ADMIN role from realm_access.roles', () => {
    oauth.getAccessToken.mockReturnValue(fakeAccessToken({
      sub: 'user-1',
      realm_access: { roles: ['COACH', 'ADMIN'] }
    }));
    expect(service.roles()).toEqual(['COACH', 'ADMIN']);
    expect(service.isAdmin()).toBe(true);
    expect(service.userId()).toBe('user-1');
  });

  it('isAdmin is false when user has only COACH', () => {
    oauth.getAccessToken.mockReturnValue(fakeAccessToken({
      sub: 'user-2',
      realm_access: { roles: ['COACH'] }
    }));
    expect(service.isAdmin()).toBe(false);
    expect(service.userId()).toBe('user-2');
  });

  it('handles malformed token gracefully', () => {
    oauth.getAccessToken.mockReturnValue('not.a.jwt.at.all');
    expect(service.roles()).toEqual([]);
    expect(service.userId()).toBeNull();
  });
});
