import { Injectable, inject, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { ApiService } from '../services/api.service';
import { AuthService } from '../auth/auth.service';

/**
 * TEN-6 — Sprach-State im Frontend.
 *
 * - Verwaltet die aktive Sprache via ngx-translate (de|en|it|fr).
 * - Beim ersten Boot wird die Sprache vom Backend gelesen (falls eingeloggt) oder aus
 *   `localStorage` als Fallback genommen; ohne beides bleibt der DE-Default aktiv.
 * - Bei Wechsel werden ngx-translate, `localStorage` und das Backend aktualisiert.
 */
@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly translate = inject(TranslateService);
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);

  static readonly SUPPORTED: readonly string[] = ['de', 'en', 'it', 'fr'];
  static readonly DEFAULT = 'de';
  private static readonly STORAGE_KEY = 'tsas.language';

  /** Aktive Sprache als Signal — die UI bindet daran für reaktive Updates. */
  readonly current = signal<string>(LanguageService.DEFAULT);

  /** Eine einmalige Initialisierung beim App-Boot (über APP_INITIALIZER). */
  async initialize(): Promise<void> {
    this.translate.addLangs([...LanguageService.SUPPORTED]);
    this.translate.setFallbackLang(LanguageService.DEFAULT);

    const local = this.readLocal();
    let chosen = local ?? LanguageService.DEFAULT;

    if (this.auth.isAuthenticated()) {
      try {
        const fromServer = await new Promise<string | null>(resolve => {
          this.api.getUserPreference().subscribe({
            next: r => resolve(r.language),
            error: () => resolve(null)
          });
        });
        if (fromServer && LanguageService.SUPPORTED.includes(fromServer)) {
          chosen = fromServer;
        }
      } catch {
        /* keep local fallback */
      }
    }

    await this.applyLanguage(chosen, /* persistRemote */ false);
  }

  /** Sprachwechsel zur Laufzeit. Persistiert lokal und beim Backend (falls eingeloggt). */
  async setLanguage(language: string): Promise<void> {
    if (!LanguageService.SUPPORTED.includes(language)) return;
    await this.applyLanguage(language, /* persistRemote */ true);
  }

  private async applyLanguage(language: string, persistRemote: boolean): Promise<void> {
    await new Promise<void>(resolve => {
      this.translate.use(language).subscribe({ next: () => resolve(), error: () => resolve() });
    });
    this.current.set(language);
    this.writeLocal(language);
    if (persistRemote && this.auth.isAuthenticated()) {
      this.api.updateUserPreference(language).subscribe({ error: () => {/* best effort */} });
    }
  }

  private readLocal(): string | null {
    try {
      const v = localStorage.getItem(LanguageService.STORAGE_KEY);
      return v && LanguageService.SUPPORTED.includes(v) ? v : null;
    } catch {
      return null;
    }
  }

  private writeLocal(language: string): void {
    try { localStorage.setItem(LanguageService.STORAGE_KEY, language); } catch { /* ignore */ }
  }
}
