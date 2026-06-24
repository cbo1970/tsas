import { TestBed } from '@angular/core/testing';
import { signal, computed } from '@angular/core';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { AuthService } from './core/auth/auth.service';
import { provideHttpClient } from '@angular/common/http';
import { testTranslateProviders } from './core/i18n/test-providers';

const mockAuthService: Partial<AuthService> = {
  initialize: () => Promise.resolve(true),
  userName: () => '',
  logout: () => {},
  login: () => {},
  isAuthenticated: () => false,
  roles: computed(() => []),
  isAdmin: computed(() => false),
  userId: computed(() => null),
};

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        ...testTranslateProviders,
        { provide: AuthService, useValue: mockAuthService },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the toolbar title', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    // Ohne Loader liefert ngx-translate den Key zurück — wir prüfen nur, dass das Element
    // gerendert wird und der Title-Slot über `translate`-Pipe an `app.title` bindet.
    expect(compiled.querySelector('.app-title')?.textContent?.trim()).toBe('app.title');
  });
});
