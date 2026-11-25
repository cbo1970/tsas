import { Component, input, output } from '@angular/core';
import { PointType, StrokeType } from '../../../core/models/point.model';

/**
 * Beobachtungs-Panel für genau einen Spieler — Variante C der Erfassungsmaske.
 *
 * Der Inhalt hängt an der Rolle: Ass, Doppelfehler und die 1./2.-Service-Wahl
 * kann es fachlich nur beim Aufschläger geben. Diese fünf Elemente fallen beim
 * Returnierenden weg und bezahlen damit die 56px-Kacheln.
 */
@Component({
  selector: 'app-observation-panel',
  standalone: true,
  templateUrl: './observation-panel.component.html',
  styles: [`
    :host { display: flex; flex: 1; min-height: 0; }

    .panel {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 6px;
      background: var(--surface-card);
      border: 1px solid var(--text);
      border-radius: 10px;
      padding: 8px;
    }
    .panel.serving {
      background: var(--brand-soft);
      border-color: var(--brand-border);
    }

    .panel-head {
      display: flex;
      align-items: baseline;
      justify-content: space-between;
      gap: 8px;
      min-width: 0;
    }
    .panel-player {
      font-size: 15px;
      font-weight: 800;
      color: var(--text-muted);
      overflow-wrap: anywhere;
    }
    .panel.serving .panel-player { color: var(--brand-strong); }
    .panel-role {
      flex-shrink: 0;
      font-size: 10px;
      font-weight: 700;
      letter-spacing: .5px;
      color: var(--text);
    }

    .section-label {
      font-size: 10px;
      color: var(--text);
      font-weight: 700;
      letter-spacing: .5px;
    }
    .section-label.green { color: var(--success); opacity: .8; }
    .section-label.red   { color: var(--danger);  opacity: .8; }

    /* Kontextblöcke (Service, Schlagart) wachsen nicht mit */
    .ctx-block { flex: 0 0 auto; display: flex; flex-direction: column; gap: 4px; }
    .ctx-row { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; }
    .ctx-btn {
      min-height: 44px;
      background: var(--border);
      border: 1px solid var(--border);
      border-radius: 6px;
      padding: 6px 4px;
      text-align: center;
      font-size: 13px;
      color: var(--text-muted);
      cursor: pointer;
      white-space: normal;
      overflow-wrap: anywhere;
      transition: background .15s;
    }
    .ctx-btn.active {
      background: var(--brand-soft);
      border-color: var(--brand);
      color: var(--brand-strong);
      font-weight: 600;
    }

    /* Beobachtungskacheln füllen die verbleibende Höhe.
       Kein min-height: 0 — bei zu wenig Platz soll der Block lieber unten
       abgeschnitten werden als sich über die Kontextzeilen legen. */
    .obs-block {
      flex: 1 1 auto;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .obs-row {
      flex: 1 1 0;
      min-height: 56px;
      display: grid;
      grid-template-columns: 1fr;
      gap: 6px;
    }
    .obs-row.cols-2 { grid-template-columns: 1fr 1fr; }
    .obs-row.cols-3 { grid-template-columns: 1fr 1fr 1fr; }

    .obs-btn {
      min-height: 56px;
      border: none;
      border-radius: 8px;
      padding: 6px 4px;
      text-align: center;
      font-size: 14px;
      font-weight: 700;
      line-height: 1.2;
      cursor: pointer;
      white-space: normal;
      overflow-wrap: anywhere;
      transition: opacity .15s;
    }
    .obs-btn:hover  { opacity: .85; }
    .obs-btn:active { opacity: .7; }

    .win-btn { background: var(--success); color: #fff; }
    .err-btn { background: var(--danger);  color: #fff; }
    /* Gelbes Icon-Glyph auf den roten Kacheln (auf Rot deutlich sichtbar) */
    .ico-y { color: #FACC15; font-weight: 900; }
  `],
})
export class ObservationPanelComponent {
  player       = input.required<{ id: string; name: string }>();
  opponentName = input.required<string>();
  isServing    = input.required<boolean>();
  /** Aufschlagversuch des gerade erfassten Punktes — gehört zum Punkt, nicht zum Panel. */
  serveAttempt = input.required<1 | 2 | null>();
  strokeType   = input.required<StrokeType | null>();

  observation        = output<PointType>();
  serveAttemptChange = output<1 | 2>();
  strokeTypeChange   = output<StrokeType | null>();

  toggleStroke(type: StrokeType): void {
    this.strokeTypeChange.emit(this.strokeType() === type ? null : type);
  }
}
