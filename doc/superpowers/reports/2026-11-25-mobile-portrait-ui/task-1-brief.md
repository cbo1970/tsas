### Task 1: LayoutService

**Files:**
- Create: `frontend/src/app/core/layout/layout.service.ts`
- Test: `frontend/src/app/core/layout/layout.service.spec.ts`

**Interfaces:**
- Produces: `LayoutService` mit `readonly isHandset: Signal<boolean>`, gespeist aus `BreakpointObserver.observe(Breakpoints.Handset)`.

- [ ] **Step 1: Write the failing test**

```ts
describe('LayoutService', () => {
  it('meldet isHandset true, wenn Breakpoints.Handset greift', () => { /* BreakpointObserver mit { matches: true } stubben */ });
  it('meldet isHandset false, wenn Breakpoints.Handset nicht greift', () => { /* ... { matches: false } */ });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npx ng test --no-watch`
Expected: FAIL — `layout.service` nicht gefunden.

- [ ] **Step 3: Implement `LayoutService` in `frontend/src/app/core/layout/layout.service.ts`**

`@Injectable({ providedIn: 'root' })`. `isHandset` entsteht via `toSignal` aus `BreakpointObserver.observe(Breakpoints.Handset)`, Startwert `false`.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npx ng test --no-watch`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/core/layout/
git commit -m "feat(layout): LayoutService mit isHandset-Signal"
```

---

