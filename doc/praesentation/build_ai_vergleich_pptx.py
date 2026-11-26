#!/usr/bin/env python3
"""
Einmal-Generator für AI_LLM_Vergleich_2026.pptx (python-pptx).

ACHTUNG — die gepflegte Quelle ist das .pptx, nicht dieses Skript.
Das Deck ist bewusst PowerPoint-nativ gebaut (Standard-Layouts, echte
Platzhalter, echte Tabellen, neutrales Office-Theme) und wird ab hier
direkt in PowerPoint weiterbearbeitet. Dieses Skript existiert nur, um
das Deck bei Bedarf von Null neu aufzubauen — ein erneuter Lauf
ueberschreibt alle manuellen Folienaenderungen.

Inhaltlicher Stand: Oktober 2026 (Preise und Modelle aendern sich schnell).
"""

from copy import deepcopy

from pptx import Presentation
from pptx.enum.text import MSO_ANCHOR
from pptx.util import Inches, Pt

OUT = "/Users/cbo/Projects/cas/tsas/doc/praesentation/AI_LLM_Vergleich_2026.pptx"
FOOTER = "LLM-Landschaft 2026 · Stand: Oktober 2026"

PH_FOOTER, PH_SLIDENUM = 11, 12


# ---------------------------------------------------------------- Grundgeruest

def _has_own_geometry(shape):
    """True, wenn die Form eine eigene <a:xfrm> traegt (statt zu erben).

    Vererbte Werte duerfen NICHT zurueckgeschrieben werden: python-pptx legt
    dabei ein unvollstaendiges xfrm an (y/cy = 0) und zerstoert so die vom
    Master geerbte Position.
    """
    spPr = getattr(shape._element, "spPr", None)
    return spPr is not None and spPr.xfrm is not None


def to_widescreen(prs):
    """4:3-Standardtemplate auf 16:9 bringen und Platzhalter mitskalieren."""
    factor = Inches(13.333) / prs.slide_width
    prs.slide_width = Inches(13.333)
    for master in prs.slide_masters:
        for container in [master] + list(master.slide_layouts):
            for shape in container.shapes:
                if not _has_own_geometry(shape):
                    continue  # erbt vom Master, der bereits skaliert wurde
                shape.left = int(shape.left * factor)
                shape.width = int(shape.width * factor)
    return prs


prs = to_widescreen(Presentation())
LAYOUT = {layout.name: layout for layout in prs.slide_layouts}


def slide(layout_name, title=None, footer=True):
    layout = LAYOUT[layout_name]
    s = prs.slides.add_slide(layout)
    if title is not None:
        s.shapes.title.text = title
    if footer:
        for ph in layout.placeholders:
            if ph.placeholder_format.idx in (PH_FOOTER, PH_SLIDENUM):
                s.shapes._spTree.append(deepcopy(ph._element))
        for shape in s.placeholders:
            if shape.placeholder_format.idx == PH_FOOTER:
                shape.text_frame.text = FOOTER
                shape.text_frame.paragraphs[0].runs[0].font.size = Pt(10)
    return s


def bullets(placeholder, items, size=None):
    """items: str oder (text, gliederungsebene).

    `size` nur setzen, wo der Layout-Standard sonst ueberlaufen wuerde —
    sonst gilt bewusst die Schriftgroesse aus dem Folienmaster.
    """
    tf = placeholder.text_frame
    tf.clear()
    tf.word_wrap = True
    for i, item in enumerate(items):
        text, level = item if isinstance(item, tuple) else (item, 0)
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.text = text
        p.level = level
        if size is not None:
            for run in p.runs:
                run.font.size = Pt(size)
    return tf


def table(s, headers, rows, left, top, width, height, col_weights=None, size=12,
          head_height=None, row_height=None):
    frame = s.shapes.add_table(len(rows) + 1, len(headers), left, top, width, height)
    tbl = frame.table
    if head_height:
        tbl.rows[0].height = head_height
    if row_height:
        for row in list(tbl.rows)[1:]:
            row.height = row_height
    if col_weights:
        total = sum(col_weights)
        for i, weight in enumerate(col_weights):
            tbl.columns[i].width = int(width * weight / total)
    for c, head in enumerate(headers):
        _fill(tbl.cell(0, c), head, size, bold=True)
    for r, row in enumerate(rows, start=1):
        for c, value in enumerate(row):
            _fill(tbl.cell(r, c), value, size)
    return tbl


def _fill(cell, text, size, bold=False):
    cell.text = text
    cell.margin_left = cell.margin_right = Inches(0.08)
    cell.margin_top = cell.margin_bottom = Inches(0.06)
    cell.vertical_anchor = MSO_ANCHOR.MIDDLE
    for p in cell.text_frame.paragraphs:
        for run in p.runs:
            run.font.size = Pt(size)
            run.font.bold = bold


# ------------------------------------------------------------- 1 · Titelfolie

s = slide("Title Slide", "LLM-Landschaft 2026", footer=False)
s.placeholders[1].text_frame.text = (
    "Frontier-Modelle und lokale Open-Weight-Modelle im Vergleich\n"
    "Claude · GPT · Gemini  —  Qwen · DeepSeek · Llama\n"
    "Stand: Oktober 2026"
)

# ----------------------------------------------------------------- 2 · Agenda

s = slide("Title and Content", "Agenda")
bullets(s.placeholders[1], [
    "Zwei Lager: Frontier-API gegen Open Weights",
    "Die drei Frontier-Familien: Claude, GPT, Gemini",
    "Direktvergleich: Context, Preis, Profil",
    "Die drei lokalen Familien: Qwen, DeepSeek, Llama",
    "Lokal betreiben: Hardware, Quantisierung, Runtimes",
    "Cloud oder lokal — Entscheidungshilfe je Use Case",
])

# ------------------------------------------------------------- 3 · Zwei Lager

s = slide("Two Content", "Zwei Lager, zwei Betriebsmodelle")
bullets(s.placeholders[1], [
    "Frontier-Modelle (API)",
    ("Gewichte bleiben beim Anbieter, Zugriff nur über API", 1),
    ("Abrechnung pro Token, kein Hardware-Invest", 1),
    ("Höchste Qualität, neue Versionen im Monatsrhythmus", 1),
    ("Daten verlassen das eigene Netz", 1),
    ("Abhängig von Rate Limits und Anbieter-Roadmap", 1),
])
bullets(s.placeholders[2], [
    "Open-Weight-Modelle (lokal)",
    ("Gewichte herunterladbar, Betrieb auf eigener Hardware", 1),
    ("Fixkosten statt Token-Preis", 1),
    ("Qualität rund 6–12 Monate hinter der Spitze", 1),
    ("Volle Datenhoheit, offline lauffähig", 1),
    ("Betrieb, Updates und GPU-Kapazität sind eigene Aufgabe", 1),
])

# ----------------------------------------------------------- 4 · Claude

s = slide("Title and Content", "Anthropic — Claude")
bullets(s.placeholders[1], [
    "Claude Opus 5 — Flaggschiff · 5 $ / 25 $ je 1 Mio. Tokens",
    ("1 Mio. Tokens Context, adaptives Reasoning, Modell-ID claude-opus-5", 1),
    "Claude Sonnet 5 — Arbeitspferd · 2 $ / 10 $",
    ("1 Mio. Tokens Context, nahe an Opus im Coding zum halben Preis", 1),
    "Claude Haiku 4.5 — schnell und günstig · 1 $ / 5 $",
    ("200 Tsd. Tokens Context, für Massenverarbeitung und Sub-Agents", 1),
    "Darüber: Claude Fable 5.1 (10 $ / 50 $) als Maximal-Tier",
    "Profil: stärkster Allrounder bei Coding und agentischen Langläufen",
], size=20)

# ----------------------------------------------------------- 5 · OpenAI

s = slide("Title and Content", "OpenAI — GPT-5.6")
bullets(s.placeholders[1], [
    "GPT-5.6 Sol — Flaggschiff · 4 $ / 20 $ je 1 Mio. Tokens",
    ("1,05 Mio. Tokens Context, 128 Tsd. Tokens Output", 1),
    ("Aktionspreis mindestens bis 21.11.2026", 1),
    "GPT-5.6 Terra — Mittelklasse · 2 $ / 12 $",
    ("Leistung auf GPT-5.5-Niveau zu deutlich geringeren Kosten", 1),
    "GPT-5.6 Luna — Volumenmodell · 0.20 $ / 1.20 $",
    ("Schnellstes und günstigstes Modell der Familie", 1),
    "Profil: breitestes Ökosystem, planbares Prompt-Caching (30 Min. Mindestlaufzeit)",
], size=20)

# ----------------------------------------------------------- 6 · Google

s = slide("Title and Content", "Google — Gemini 3.x")
bullets(s.placeholders[1], [
    "Gemini 3.1 Pro — Flaggschiff · 2 $ / 12 $, ab 200 Tsd. Tokens 4 $ / 18 $",
    ("1 Mio. Tokens Context, 64 Tsd. Tokens Output", 1),
    "Gemini 3.8 Flash — Agenten-Arbeitspferd · 0.75 $ / 3.75 $",
    ("Einführungspreis bis 31.12.2026, danach regulär 1.50 $ / 7.50 $", 1),
    "Gemini 3.1 Flash-Lite — Massenmodell · 0.25 $ / 1.50 $",
    ("Günstigste Frontier-Stufe am Markt", 1),
    "Profil: stark multimodal, tief in Google Cloud integriert",
], size=20)

# -------------------------------------------- 7 · Frontier-Direktvergleich

s = slide("Title Only", "Frontier-Flaggschiffe im Direktvergleich")
tbl = table(
    s,
    ["Modell", "Context", "$ / 1 Mio. Input", "$ / 1 Mio. Output", "Stärke"],
    [
        ["Claude Opus 5", "1 Mio.", "5.00", "25.00", "Coding, agentische Langläufe"],
        ["GPT-5.6 Sol", "1,05 Mio.", "4.00", "20.00", "Reasoning, grösstes Ökosystem"],
        ["Gemini 3.1 Pro", "1 Mio.", "2.00 / 4.00 *", "12.00 / 18.00 *", "Multimodal, bester Preis"],
        ["* Gemini 3.1 Pro: höherer Satz ab 200 Tsd. Tokens Prompt-Länge. Alle Preise Stand Oktober 2026.", "", "", "", ""],
    ],
    Inches(0.6), Inches(2.0), Inches(12.1), Inches(3.6),
    col_weights=[3, 1.4, 1.8, 1.8, 4],
    size=14, head_height=Inches(0.5), row_height=Inches(0.8),
)
tbl.cell(4, 0).merge(tbl.cell(4, 4))
for p in tbl.cell(4, 0).text_frame.paragraphs:
    for run in p.runs:
        run.font.size = Pt(10)
        run.font.italic = True

# ------------------------------------------------------- 8 · Lokale Modelle

s = slide("Title Only", "Lokale Open-Weight-Modelle: die drei Familien")
table(
    s,
    ["Familie", "Repräsentatives Modell", "Lizenz", "Context", "Einordnung"],
    [
        ["Qwen (Alibaba)", "Qwen3.8-27B (dense)", "Apache 2.0", "262 Tsd.",
         "Beste Ein-GPU-Wahl, stark im Coding; schnellst wachsende Familie bei Ollama"],
        ["DeepSeek", "DeepSeek-V4.1-Flash (MoE)", "MIT", "1 Mio.",
         "Reasoning und Mathematik; kleine Distill-Varianten laufen auf einer GPU"],
        ["Llama (Meta)", "Llama 4 Scout / Maverick (MoE)", "Llama Community", "lang (MoE)",
         "Grösste Verbreitung und Tooling-Reife; Meta hat die Spitze auf die geschlossene Muse-Linie verlagert"],
    ],
    Inches(0.6), Inches(2.0), Inches(12.1), Inches(3.9),
    col_weights=[2.2, 3.2, 2, 1.5, 6],
    size=12, head_height=Inches(0.5), row_height=Inches(1.1),
)

# --------------------------------------------------------- 9 · Lokal betreiben

s = slide("Title and Content", "Lokal betreiben: was es wirklich braucht")
bullets(s.placeholders[1], [
    "Hardware-Klassen",
    ("1 × 24 GB (RTX 4090/5090): 27B-Modelle in Q4 — der Sweet Spot", 1),
    ("1 × 48–96 GB (RTX 6000, Mac Studio): 70B in Q4, lange Kontexte", 1),
    ("Multi-GPU-Server: grosse MoE-Modelle mit dreistelliger Milliarden-Parameterzahl", 1),
    "Quantisierung: Q8 praktisch verlustfrei, Q4 der übliche Kompromiss, ab Q3 spürbar schlechter",
    "Runtimes: Ollama (Einstieg), LM Studio (GUI), vLLM (Durchsatz im Serverbetrieb)",
    "Faustregel VRAM ≈ Parameter × Bits / 8, plus Reserve für den Context-Cache",
], size=20)

# ------------------------------------------------------- 10 · Cloud vs. lokal

s = slide("Comparison", "Cloud oder lokal — was wofür spricht")
s.placeholders[1].text_frame.text = "Spricht für Frontier-API"
bullets(s.placeholders[2], [
    "Höchste Qualität, sofort verfügbar",
    "Keine Hardware, kein Betrieb, kein GPU-Beschaffungsrisiko",
    "Kosten skalieren mit der Nutzung — günstig bei kleinem Volumen",
    "Modell-Upgrades ohne eigenes Zutun",
])
s.placeholders[3].text_frame.text = "Spricht für lokale Modelle"
bullets(s.placeholders[4], [
    "Daten verlassen das Haus nicht (DSGVO, EU AI Act)",
    "Fixkosten, planbar bei hohem Volumen",
    "Offline- und Edge-Fähigkeit",
    "Keine Abhängigkeit von Rate Limits und Anbieter-Roadmap",
])

# ------------------------------------------------------ 11 · Entscheidungshilfe

s = slide("Title and Content", "Entscheidungshilfe je Use Case")
bullets(s.placeholders[1], [
    "Coding-Agent, komplexe Refactorings → Claude Opus 5 oder GPT-5.6 Sol",
    "Massen-Klassifikation und Extraktion → Gemini 3.1 Flash-Lite oder GPT-5.6 Luna",
    "Vertrauliche Daten, Betriebsgeheimnisse → Qwen3.8-27B lokal (Apache 2.0)",
    "Mathematik und Logik ohne Cloud → DeepSeek-Distill lokal",
    "Prototyp mit unklarem Volumen → Frontier-API, später migrieren",
    "Offline oder Edge, kleines Gerät → Llama-Modelle der 3B-Klasse",
    "Faustregel: in der Cloud starten; lokalisieren, sobald Volumen oder Datenschutz es erzwingen",
], size=20)

# ------------------------------------------------------- 12 · Fazit & Quellen

s = slide("Title and Content", "Fazit und Quellen")
bullets(s.placeholders[1], [
    "Der Abstand Frontier ↔ Open Weights: rund 6–12 Monate — und schrumpfend",
    "Frontier-Preise fallen schnell; „zu teuer“ ist selten das Hauptargument",
    "Open Weights kommen 2026 überwiegend aus chinesischen Laboren",
    "Entscheidend ist nicht das beste Modell, sondern das passende je Use Case",
    "Quellen: docs.anthropic.com · developers.openai.com · ai.google.dev · qwen.ai · ollama.com",
    "Preise und Modelle ändern sich monatlich — vor Weiterverwendung prüfen",
], size=20)

# ---------------------------------------------------------------- 13 · Vorlage

s = slide("Title and Content", "VORLAGE — diese Folie duplizieren")
body = s.placeholders[1]
# alle vier Werte lesen und gemeinsam setzen — sonst entsteht ein
# unvollstaendiges xfrm und die geerbte Position geht verloren
left, top, width = body.left, body.top, body.width
body.left, body.top, body.width, body.height = left, top, width, Inches(2.6)
bullets(body, [
    "Folie duplizieren: Rechtsklick auf die Miniatur, dann Texte überschreiben",
    "Neue Folie: Start → Neue Folie → Layout wählen",
    "Texte umschreiben: Ansicht → Gliederung",
    "Umfärben: Entwurf → Designs — wirkt auf das ganze Deck",
    "Tabellenzeile anfügen: in der letzten Zelle Tab drücken",
], size=20)
table(
    s,
    ["Spalte A", "Spalte B", "Spalte C"],
    [
        ["Beispielzeile", "Wert", "Bemerkung"],
        ["Beispielzeile", "Wert", "Bemerkung"],
    ],
    Inches(0.9), Inches(4.9), Inches(11.5), Inches(1.4),
    col_weights=[3, 2, 5],
    size=12, head_height=Inches(0.45), row_height=Inches(0.45),
)

prs.save(OUT)
print(f"geschrieben: {OUT}  ({len(prs.slides.__iter__.__self__._sldIdLst)} Folien)")
