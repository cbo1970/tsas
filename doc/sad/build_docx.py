#!/usr/bin/env python3
"""Builds TSaS_SAD.docx from the maintainable Markdown source TSaS_SAD.md.

Slim pandoc build: a Helvetica reference doc gives consistent typography and
rsvg-convert (librsvg) embeds the diagram SVGs — with a raster fallback — so
they render in Word 2016+. The diagram links in TSaS_SAD.md point at
diagrams/*.svg (resolved via --resource-path). Run from anywhere:

    python3 doc/sad/build_docx.py

Requirements: pandoc, rsvg-convert (librsvg).
"""
import os
import subprocess
import tempfile
import zipfile

BASE = os.path.dirname(os.path.abspath(__file__))
MD = os.path.join(BASE, "TSaS_SAD.md")
OUT = os.path.join(BASE, "TSaS_SAD.docx")


def helvetica_reference(tmp):
    """Derive a reference.docx from pandoc's default, theme fonts swapped to Helvetica."""
    ref = os.path.join(tmp, "reference.docx")
    with open(ref, "wb") as f:
        subprocess.run(["pandoc", "--print-default-data-file", "reference.docx"],
                       stdout=f, check=True)
    helv = os.path.join(tmp, "reference_helvetica.docx")
    swap = lambda d: (d.replace(b'typeface="Aptos Display"', b'typeface="Helvetica"')
                       .replace(b'typeface="Aptos"', b'typeface="Helvetica"'))
    with zipfile.ZipFile(ref) as zin, \
            zipfile.ZipFile(helv, "w", zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            data = zin.read(item.filename)
            if item.filename == "word/theme/theme1.xml":
                data = swap(data)
            zout.writestr(item, data)
    return helv


def main():
    with tempfile.TemporaryDirectory() as tmp:
        ref = helvetica_reference(tmp)
        subprocess.run(["pandoc", MD, "-f", "gfm", "-t", "docx",
                        "--reference-doc", ref, "--resource-path", BASE,
                        "-o", OUT], check=True)
    print("built", OUT)


if __name__ == "__main__":
    main()
