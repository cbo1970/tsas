#!/usr/bin/env python3
"""Builds TSaS_SAD_arc42_2.docx from the arc42 Markdown.

Pipeline:
  1. derive a Helvetica reference doc from pandoc's default (theme fonts swapped),
  2. rasterise the draw.io diagrams to high-res PNG,
  3. transform the Markdown (manual "Inhaltsverzeichnis" -> TOC marker,
     SVG image links -> the rendered PNGs),
  4. run pandoc (--toc) against the Helvetica reference,
  5. move pandoc's TOC field to the marker (i.e. after the title/metadata) and
     rename its heading to "Inhaltsverzeichnis".

Requirements: pandoc, the draw.io desktop app. Run from anywhere:
    python3 doc/sad/build_docx.py
"""
import os, re, subprocess, sys, tempfile, zipfile

BASE = os.path.dirname(os.path.abspath(__file__))
DIAGRAMS = os.path.join(BASE, "diagrams")
# Version stem: `python3 build_docx.py [N]` builds TSaS_SAD_arc42_{N}.{md,docx} (default 2).
VER = sys.argv[1] if len(sys.argv) > 1 else "2"
MD = os.path.join(BASE, f"TSaS_SAD_arc42_{VER}.md")
OUT = os.path.join(BASE, f"TSaS_SAD_arc42_{VER}.docx")

DRAWIO = next((p for p in (
    "/Applications/draw.io.app/Contents/MacOS/draw.io",
    "/usr/local/bin/drawio", "drawio") if os.path.exists(p) or p == "drawio"), "drawio")

# diagram source stem -> (png stem, width in the docx)
DIAGRAM_WIDTHS = {
    "TSaS_Fachlicher_Kontext":       ("Fachlicher_Kontext", "14cm"),
    "TSaS_Whitebox_Gesamtsystem":    ("Whitebox_Gesamtsystem", "15.5cm"),
    "TSaS_Backend_Module":           ("Backend_Module", "15.5cm"),
    "TSaS_Backend_CleanArchitecture":("Backend_CleanArchitecture", "16cm"),
    "TSaS_Deployment":               ("Deployment", "15cm"),
    "TSaS_Datenmodell":              ("Datenmodell", "16cm"),
    "TSaS_Seq_RecordPoint":          ("Seq_RecordPoint", "14cm"),
    "TSaS_Seq_GenerateAnalysis":     ("Seq_GenerateAnalysis", "15cm"),
    "TSaS_Seq_Authentication":       ("Seq_Authentication", "15cm"),
}

PLANTUML_SOURCES = {"TSaS_Seq_RecordPoint", "TSaS_Seq_GenerateAnalysis",
                    "TSaS_Seq_Authentication"}


def patch_zip(src, dst, edits):
    """Copy a zip, applying edits {entry_name: bytes->bytes}."""
    with zipfile.ZipFile(src) as zin, \
            zipfile.ZipFile(dst, "w", zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            data = zin.read(item.filename)
            if item.filename in edits:
                data = edits[item.filename](data)
            zout.writestr(item, data)


def helvetica_reference(tmp):
    ref = os.path.join(tmp, "reference.docx")
    with open(ref, "wb") as f:
        subprocess.run(["pandoc", "--print-default-data-file", "reference.docx"],
                       stdout=f, check=True)
    helv = os.path.join(tmp, "reference_helvetica.docx")
    swap = lambda d: (d.replace(b'typeface="Aptos Display"', b'typeface="Helvetica"')
                       .replace(b'typeface="Aptos"', b'typeface="Helvetica"'))
    patch_zip(ref, helv, {"word/theme/theme1.xml": swap})
    return helv


def export_pngs(tmp):
    import shutil
    for src, (dst, _) in DIAGRAM_WIDTHS.items():
        out_png = os.path.join(tmp, dst + ".png")
        if src in PLANTUML_SOURCES:
            shutil.copyfile(os.path.join(DIAGRAMS, src + ".png"), out_png)
            continue
        subprocess.run([DRAWIO, "--export", "--format", "png", "--scale", "2.5",
                        "--border", "8", "--output", out_png,
                        os.path.join(DIAGRAMS, src + ".drawio")],
                       check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def transform_md(tmp):
    txt = open(MD, encoding="utf-8").read()
    for src, (dst, w) in DIAGRAM_WIDTHS.items():
        link = f"(diagrams/{src}.svg)"
        assert link in txt, "missing image link: " + link
        txt = txt.replace(link, f"({dst}.png){{width={w}}}")
    # manual table of contents -> marker (auto TOC is moved here later)
    txt, n = re.subn(r"## Inhaltsverzeichnis\n\n(?:\d+\..*\n)+", "TOCMARKERXYZ\n", txt)
    assert n == 1, "manual Inhaltsverzeichnis block not found"
    txt = "---\nlang: de-DE\n---\n\n" + txt
    path = os.path.join(tmp, "sad.md")
    open(path, "w", encoding="utf-8").write(txt)
    return path


def move_toc(data):
    d = data.decode("utf-8")
    sdt = re.search(r"<w:sdt>.*?</w:sdt>", d, re.S)
    assert sdt, "pandoc TOC sdt not found"
    block = sdt.group(0).replace("Table of Contents", "Inhaltsverzeichnis")
    d = d.replace(sdt.group(0), "", 1)
    d, n = re.subn(r"<w:p[^>]*>(?:(?!</w:p>).)*TOCMARKERXYZ(?:(?!</w:p>).)*</w:p>",
                   lambda _: block, d, count=1)
    assert n == 1, "TOC marker paragraph not found"
    assert d.count("<w:sdt>") == 1 and "TOCMARKERXYZ" not in d
    return d.encode("utf-8")


def main():
    with tempfile.TemporaryDirectory() as tmp:
        ref = helvetica_reference(tmp)
        export_pngs(tmp)
        md = transform_md(tmp)
        raw = os.path.join(tmp, "out.docx")
        subprocess.run(["pandoc", md, "-f", "markdown", "-t", "docx", "--toc",
                        "--toc-depth=3", "--reference-doc", ref,
                        "--resource-path", tmp, "-o", raw], check=True)
        patch_zip(raw, OUT, {"word/document.xml": move_toc})
    print("built", OUT)


if __name__ == "__main__":
    main()
