#!/usr/bin/env python3
"""Builds TSaS_SAD.docx from the maintainable Markdown source TSaS_SAD.md.

Slim pandoc build: a Helvetica reference doc gives consistent typography and
rsvg-convert (librsvg) embeds the diagram SVGs — with a raster fallback — so
they render in Word 2016+. The diagram links in TSaS_SAD.md point at
diagrams/*.svg (resolved via --resource-path). Run from anywhere:

    python3 doc/sad/build_docx.py

Manually tuned table column widths in the existing TSaS_SAD.docx survive a
rebuild: the tblGrid of each table is carried over (matched by table order;
skipped when the column count changed, e.g. after adding/removing a column).

The manual "# Inhaltsverzeichnis" block in the Markdown (kept for GitHub
rendering) is replaced by a native Word TOC field over the chapter and
section headings; w:updateFields makes Word fill in the page numbers on open.

Requirements: pandoc, rsvg-convert (librsvg).
"""
import os
import re
import subprocess
import tempfile
import zipfile

BASE = os.path.dirname(os.path.abspath(__file__))
MD = os.path.join(BASE, "TSaS_SAD.md")
OUT = os.path.join(BASE, "TSaS_SAD.docx")


def chapter_page_breaks(styles_xml):
    """Give Heading 2 (the '## n. …' main chapters) pageBreakBefore."""
    block = re.search(rb'<w:style [^>]*w:styleId="Heading2">.*?</w:style>',
                      styles_xml, re.S).group(0)
    if b"pageBreakBefore" in block:
        return styles_xml
    patched = block.replace(b"<w:keepLines />",
                            b"<w:keepLines /><w:pageBreakBefore />", 1)
    return styles_xml.replace(block, patched, 1)


def helvetica_reference(tmp):
    """Derive a reference.docx from pandoc's default: theme fonts swapped to
    Helvetica, main chapters starting on a new page."""
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
            elif item.filename == "word/styles.xml":
                data = chapter_page_breaks(data)
            zout.writestr(item, data)
    return helv


# Markdown "## 1. …" chapters land as Heading 2, "### 1.1 …" as Heading 3,
# so the TOC field collects outline levels 2-3 (the document title stays out).
TOC_XML = (
    '<w:sdt><w:sdtPr><w:docPartObj>'
    '<w:docPartGallery w:val="Table of Contents"/><w:docPartUnique/>'
    '</w:docPartObj></w:sdtPr><w:sdtContent>'
    '<w:p><w:r><w:fldChar w:fldCharType="begin" w:dirty="true"/></w:r>'
    '<w:r><w:instrText xml:space="preserve"> TOC \\o "2-3" \\h \\z \\u </w:instrText></w:r>'
    '<w:r><w:fldChar w:fldCharType="separate"/></w:r>'
    '<w:r><w:t>Inhaltsverzeichnis wird beim Öffnen aktualisiert (sonst F9).</w:t></w:r>'
    '<w:r><w:fldChar w:fldCharType="end"/></w:r></w:p>'
    '</w:sdtContent></w:sdt>'
)


def md_with_toc_field(tmp):
    """Swap the hand-written TOC block (everything from '# Inhaltsverzeichnis'
    up to the first numbered chapter) for a raw-openxml Word TOC field."""
    src = open(MD, encoding="utf-8").read()
    block = ("# Inhaltsverzeichnis\n\n```{=openxml}\n" + TOC_XML + "\n```\n\n")
    out, n = re.subn(r"^# Inhaltsverzeichnis\n.*?(?=^## \d)",
                     lambda m: block, src, count=1, flags=re.S | re.M)
    if n == 0:
        print("  no manual TOC block found, building without TOC field")
    path = os.path.join(tmp, "TSaS_SAD_toc.md")
    with open(path, "w", encoding="utf-8") as f:
        f.write(out)
    return path


TBL_RE = re.compile(r"<w:tbl>.*?</w:tbl>", re.S)
GRID_RE = re.compile(r"<w:tblGrid>.*?</w:tblGrid>", re.S)
COL_RE = re.compile(r'<w:gridCol w:w="\d+"\s*/>')


def read_document_xml(path):
    with zipfile.ZipFile(path) as z:
        return z.read("word/document.xml").decode("utf-8")


def keep_table_widths(prev_xml):
    """Copy each table's tblGrid (and tblLayout fixed) from the previous build."""
    old_tables = TBL_RE.findall(prev_xml)
    new_doc = read_document_xml(OUT)
    new_tables = TBL_RE.findall(new_doc)

    kept = 0
    for i, (old_tbl, new_tbl) in enumerate(zip(old_tables, new_tables)):
        old_grid = GRID_RE.search(old_tbl).group(0)
        new_grid = GRID_RE.search(new_tbl).group(0)
        if len(COL_RE.findall(old_grid)) != len(COL_RE.findall(new_grid)):
            print(f"  table {i}: column count changed, pandoc default widths kept")
            continue
        patched = new_tbl.replace(new_grid, old_grid, 1)
        if "tblLayout" in old_tbl and "tblLayout" not in patched:
            patched = patched.replace(
                '<w:tblLook', '<w:tblLayout w:type="fixed"/><w:tblLook', 1)
        new_doc = new_doc.replace(new_tbl, patched, 1)
        kept += 1

    if len(old_tables) != len(new_tables):
        print(f"  table count changed ({len(old_tables)} -> {len(new_tables)}), "
              f"widths matched by order for the first {kept} tables")

    with zipfile.ZipFile(OUT) as zin:
        items = [(it, zin.read(it.filename)) for it in zin.infolist()]
    with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as zout:
        for it, data in items:
            if it.filename == "word/document.xml":
                data = new_doc.encode("utf-8")
            zout.writestr(it, data)
    print(f"table widths kept: {kept}/{len(new_tables)}")


def update_fields_on_open():
    """Set w:updateFields so Word refreshes the TOC (page numbers) on open."""
    with zipfile.ZipFile(OUT) as zin:
        items = [(it, zin.read(it.filename)) for it in zin.infolist()]
    with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as zout:
        for it, data in items:
            if it.filename == "word/settings.xml" and b"updateFields" not in data:
                data = re.sub(rb"(<w:settings[^>]*>)",
                              rb'\1<w:updateFields w:val="true"/>', data, count=1)
            zout.writestr(it, data)


def main():
    prev_xml = read_document_xml(OUT) if os.path.exists(OUT) else None
    with tempfile.TemporaryDirectory() as tmp:
        ref = helvetica_reference(tmp)
        subprocess.run(["pandoc", md_with_toc_field(tmp),
                        "-f", "gfm+raw_attribute", "-t", "docx",
                        "--reference-doc", ref, "--resource-path", BASE,
                        "-o", OUT], check=True)
    if prev_xml is not None:
        keep_table_widths(prev_xml)
    update_fields_on_open()
    print("built", OUT)


if __name__ == "__main__":
    main()
