import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Injectable, Signal, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

/**
 * Single place that defines the responsive breakpoint.
 * Uses Breakpoints.Handset (orientation-aware) rather than a width-only query,
 * so a phone in landscape (~800x360) is still treated as a handset.
 */
@Injectable({ providedIn: 'root' })
export class LayoutService {
  private readonly breakpointObserver = inject(BreakpointObserver);

  readonly isHandset: Signal<boolean> = toSignal(
    this.breakpointObserver.observe(Breakpoints.Handset).pipe(map((state) => state.matches)),
    { initialValue: false },
  );
}
