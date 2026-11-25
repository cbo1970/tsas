import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-serve-picker',
  standalone: true,
  templateUrl: './serve-picker.component.html',
  styles: [`
    :host { display: block; grid-column: 1 / -1; }
    .serve-picker { display: flex; flex-direction: column; gap: 12px; padding: 4px 0; }
    h2 { margin: 0; font-size: 20px; font-weight: 800; color: var(--text); }
    p { margin: 0; font-size: 14px; color: var(--text-muted); }
    .pick-btn {
      min-height: 88px;
      padding: 12px 16px;
      border-radius: 12px;
      border: 2px solid var(--brand-border);
      background: var(--brand-soft);
      color: var(--text);
      font-size: 18px;
      font-weight: 700;
      cursor: pointer;
      overflow-wrap: anywhere;
    }
    .pick-btn:active { background: var(--surface-muted); }
  `]
})
export class ServePickerComponent {
  players = input.required<{ player1: string; player2: string }>();
  picked  = output<1 | 2>();
}
