import { Provider } from '@angular/core';
import { provideTranslateService } from '@ngx-translate/core';

/**
 * TEN-6 — Provider-Set für Cypress-Component- und Vitest-Specs. Aktiviert den
 * `TranslateService`, ohne einen Loader anzubinden — der `translate`-Pipe liefert dann
 * den Key selbst zurück (z. B. `players.title`), was für Selektoren und Test-Assertions
 * stabil und sprachunabhängig ist. Ohne diese Provider würden Komponenten mit
 * `TranslatePipe` im imports-Array NG0201 werfen.
 */
export const testTranslateProviders: Provider[] = [
  provideTranslateService({ fallbackLang: 'de' }),
];
