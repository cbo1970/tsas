import { TestBed } from '@angular/core/testing';
import { BreakpointObserver, Breakpoints, BreakpointState } from '@angular/cdk/layout';
import { BehaviorSubject } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { LayoutService } from './layout.service';

function createService(matches: boolean) {
  const state$ = new BehaviorSubject<BreakpointState>({ matches, breakpoints: {} });
  const observe = vi.fn().mockReturnValue(state$);
  TestBed.configureTestingModule({
    providers: [{ provide: BreakpointObserver, useValue: { observe } }],
  });
  return { service: TestBed.inject(LayoutService), state$, observe };
}

describe('LayoutService', () => {
  it('meldet isHandset true, wenn Breakpoints.Handset greift', () => {
    const { service, observe } = createService(true);
    expect(service.isHandset()).toBe(true);
    expect(observe).toHaveBeenCalledWith(Breakpoints.Handset);
  });

  it('meldet isHandset false, wenn Breakpoints.Handset nicht greift', () => {
    const { service } = createService(false);
    expect(service.isHandset()).toBe(false);
  });

  it('folgt Aenderungen des Breakpoints (z. B. Drehen des Geraets)', () => {
    const { service, state$ } = createService(true);
    state$.next({ matches: false, breakpoints: {} });
    expect(service.isHandset()).toBe(false);
  });
});
