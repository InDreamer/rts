#!/usr/bin/env python3
"""Generate the RTS AI Analysis Service LLM API key review brief PDF.

Input markdown and all outputs are intentionally scoped under .hermes/.
"""

from __future__ import annotations

import html
import json
import re
from pathlib import Path
from typing import Iterable, List, Tuple

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    ListFlowable,
    ListItem,
    PageBreak,
    PageTemplate,
    Paragraph,
    Preformatted,
    Spacer,
    Table,
    TableStyle,
)

ROOT = Path("/home/ubuntu/repos/rts")
SOURCE_MD = ROOT / ".hermes/reports/rts-ai-analysis-service-key-request-review-brief.md"
OUTPUT_PDF = ROOT / ".hermes/pdf/rts-ai-analysis-service-key-request-review-brief.pdf"
VERIFY_JSON = ROOT / ".hermes/pdf/rts-ai-analysis-service-key-request-review-brief.verify.json"

PAGE_W, PAGE_H = A4
MARGIN_LEFT = 19 * mm
MARGIN_RIGHT = 19 * mm
MARGIN_TOP = 18 * mm
MARGIN_BOTTOM = 18 * mm

NAVY = colors.HexColor("#17365D")
BLUE = colors.HexColor("#2F75B5")
LIGHT_BLUE = colors.HexColor("#EAF3FB")
PALE_BLUE = colors.HexColor("#F5F9FD")
MID_GREY = colors.HexColor("#7A8793")
DARK = colors.HexColor("#1F2933")
BORDER = colors.HexColor("#C8D4E0")
ACCENT = colors.HexColor("#8EAADB")


def register_fonts() -> None:
    """Register a Chinese-capable CID font for body text."""
    try:
        pdfmetrics.registerFont(UnicodeCIDFont("STSong-Light"))
    except Exception:
        # ReportLab may already have it registered; continue if so.
        pass


def make_styles() -> dict:
    register_fonts()
    styles = getSampleStyleSheet()
    return {
        "cover_label": ParagraphStyle(
            "CoverLabel",
            parent=styles["Normal"],
            fontName="Helvetica-Bold",
            fontSize=10,
            leading=13,
            textColor=BLUE,
            alignment=TA_LEFT,
            spaceAfter=8,
        ),
        "cover_title": ParagraphStyle(
            "CoverTitle",
            parent=styles["Title"],
            fontName="Helvetica-Bold",
            fontSize=28,
            leading=34,
            textColor=NAVY,
            alignment=TA_LEFT,
            spaceAfter=12,
        ),
        "cover_subtitle": ParagraphStyle(
            "CoverSubtitle",
            parent=styles["Normal"],
            fontName="STSong-Light",
            fontSize=13.2,
            leading=18,
            textColor=DARK,
            alignment=TA_LEFT,
            spaceAfter=18,
        ),
        "body": ParagraphStyle(
            "Body",
            parent=styles["Normal"],
            fontName="STSong-Light",
            fontSize=9.3,
            leading=12.7,
            textColor=DARK,
            spaceAfter=6,
        ),
        "small": ParagraphStyle(
            "Small",
            parent=styles["Normal"],
            fontName="STSong-Light",
            fontSize=8.3,
            leading=11.2,
            textColor=DARK,
        ),
        "h2": ParagraphStyle(
            "H2",
            parent=styles["Heading2"],
            fontName="Helvetica-Bold",
            fontSize=15.5,
            leading=19,
            textColor=NAVY,
            spaceBefore=0,
            spaceAfter=8,
            keepWithNext=True,
        ),
        "h3": ParagraphStyle(
            "H3",
            parent=styles["Heading3"],
            fontName="Helvetica-Bold",
            fontSize=10.8,
            leading=13.5,
            textColor=BLUE,
            spaceBefore=7,
            spaceAfter=4,
            keepWithNext=True,
        ),
        "bullet": ParagraphStyle(
            "Bullet",
            parent=styles["Normal"],
            fontName="STSong-Light",
            fontSize=9.0,
            leading=12.0,
            textColor=DARK,
        ),
        "callout": ParagraphStyle(
            "Callout",
            parent=styles["Normal"],
            fontName="STSong-Light",
            fontSize=9.0,
            leading=12.3,
            textColor=DARK,
        ),
        "code": ParagraphStyle(
            "Code",
            parent=styles["Code"],
            fontName="Courier",
            fontSize=7.4,
            leading=9.2,
            textColor=colors.HexColor("#263238"),
        ),
    }


def inline_markup(text: str) -> str:
    """Convert a deliberately small subset of Markdown inline markup to ReportLab XML."""
    text = text.strip().replace("  ", " ")
    escaped = html.escape(text, quote=False)

    def bold_repl(match: re.Match) -> str:
        return f'<font name="Helvetica-Bold">{match.group(1)}</font>'

    escaped = re.sub(r"\*\*(.+?)\*\*", bold_repl, escaped)
    escaped = re.sub(
        r"`([^`]+)`",
        lambda m: f'<font name="Courier">{m.group(1)}</font>',
        escaped,
    )
    return escaped


def make_callout(text: str, styles: dict) -> Table:
    para = Paragraph(inline_markup(text), styles["callout"])
    table = Table([[para]], colWidths=[PAGE_W - MARGIN_LEFT - MARGIN_RIGHT - 6 * mm])
    table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, -1), LIGHT_BLUE),
                ("BOX", (0, 0), (-1, -1), 0.45, BORDER),
                ("LINEBEFORE", (0, 0), (0, -1), 3.0, BLUE),
                ("LEFTPADDING", (0, 0), (-1, -1), 8),
                ("RIGHTPADDING", (0, 0), (-1, -1), 8),
                ("TOPPADDING", (0, 0), (-1, -1), 7),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
            ]
        )
    )
    return table


def make_code_block(text: str, styles: dict) -> Table:
    pre = Preformatted(text.rstrip(), styles["code"], maxLineLength=96)
    table = Table([[pre]], colWidths=[PAGE_W - MARGIN_LEFT - MARGIN_RIGHT - 6 * mm])
    table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#F7F9FB")),
                ("BOX", (0, 0), (-1, -1), 0.4, colors.HexColor("#D8E0E8")),
                ("LEFTPADDING", (0, 0), (-1, -1), 7),
                ("RIGHTPADDING", (0, 0), (-1, -1), 7),
                ("TOPPADDING", (0, 0), (-1, -1), 7),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
            ]
        )
    )
    return table


def make_bullet_list(items: Iterable[str], styles: dict, ordered: bool = False) -> ListFlowable:
    flow_items = []
    for item in items:
        flow_items.append(
            ListItem(
                Paragraph(inline_markup(item), styles["bullet"]),
                leftIndent=10,
                bulletColor=BLUE,
            )
        )
    return ListFlowable(
        flow_items,
        bulletType="1" if ordered else "bullet",
        start="1",
        leftIndent=16,
        bulletFontName="Helvetica",
        bulletFontSize=8.5,
        bulletColor=BLUE,
        spaceBefore=1,
        spaceAfter=5,
    )


def flush_paragraph(lines: List[str], story: list, styles: dict) -> None:
    if not lines:
        return
    text = " ".join(line.strip() for line in lines).strip()
    if text:
        story.append(Paragraph(inline_markup(text), styles["body"]))
    lines.clear()


def flush_list(items: List[str], story: list, styles: dict, ordered: bool) -> None:
    if not items:
        return
    story.append(make_bullet_list(items, styles, ordered=ordered))
    items.clear()


def parse_markdown_body(markdown: str, styles: dict) -> list:
    story: list = []
    lines = markdown.splitlines()
    para_lines: List[str] = []
    bullet_items: List[str] = []
    ordered_items: List[str] = []
    in_code = False
    code_lines: List[str] = []
    blockquote_lines: List[str] = []

    def flush_all_text() -> None:
        nonlocal blockquote_lines
        flush_paragraph(para_lines, story, styles)
        flush_list(bullet_items, story, styles, ordered=False)
        flush_list(ordered_items, story, styles, ordered=True)
        if blockquote_lines:
            story.append(make_callout(" ".join(blockquote_lines), styles))
            story.append(Spacer(1, 5))
            blockquote_lines = []

    for raw in lines:
        line = raw.rstrip("\n")

        if in_code:
            if line.startswith("```"):
                story.append(make_code_block("\n".join(code_lines), styles))
                story.append(Spacer(1, 8))
                code_lines = []
                in_code = False
            else:
                code_lines.append(line)
            continue

        if line.strip() == "<!-- pagebreak -->":
            flush_all_text()
            story.append(PageBreak())
            continue

        if line.startswith("```"):
            flush_all_text()
            in_code = True
            code_lines = []
            continue

        if line.startswith(">"):
            flush_paragraph(para_lines, story, styles)
            flush_list(bullet_items, story, styles, ordered=False)
            flush_list(ordered_items, story, styles, ordered=True)
            blockquote_lines.append(line.lstrip("> ").strip())
            continue
        elif blockquote_lines and line.strip():
            story.append(make_callout(" ".join(blockquote_lines), styles))
            story.append(Spacer(1, 5))
            blockquote_lines = []

        if not line.strip():
            flush_all_text()
            continue

        if line.startswith("## "):
            flush_all_text()
            story.append(Paragraph(inline_markup(line[3:]), styles["h2"]))
            story.append(Spacer(1, 2))
            continue

        if line.startswith("### "):
            flush_all_text()
            story.append(Paragraph(inline_markup(line[4:]), styles["h3"]))
            continue

        bullet_match = re.match(r"^[-*]\s+(.+)$", line)
        if bullet_match:
            flush_paragraph(para_lines, story, styles)
            flush_list(ordered_items, story, styles, ordered=True)
            bullet_items.append(bullet_match.group(1).strip())
            continue

        ordered_match = re.match(r"^\d+\.\s+(.+)$", line)
        if ordered_match:
            flush_paragraph(para_lines, story, styles)
            flush_list(bullet_items, story, styles, ordered=False)
            ordered_items.append(ordered_match.group(1).strip())
            continue

        flush_list(bullet_items, story, styles, ordered=False)
        flush_list(ordered_items, story, styles, ordered=True)
        para_lines.append(line)

    if in_code and code_lines:
        story.append(make_code_block("\n".join(code_lines), styles))
    flush_all_text()
    return story


def split_cover(markdown: str) -> Tuple[str, str]:
    marker = "<!-- pagebreak -->"
    if marker not in markdown:
        return markdown, ""
    cover, rest = markdown.split(marker, 1)
    return cover.strip(), rest.strip()


def parse_cover(cover_markdown: str) -> dict:
    lines = [line.rstrip() for line in cover_markdown.splitlines()]
    title = ""
    subtitle = ""
    meta = []
    callout = []
    in_callout = False
    for line in lines:
        if line.startswith("# "):
            title = line[2:].strip()
        elif line.startswith("**") and line.endswith("**") and not subtitle:
            subtitle = line.strip("*").strip()
        elif line.startswith(">"):
            in_callout = True
            callout.append(line.lstrip("> ").strip())
        elif in_callout and line.strip():
            callout.append(line.strip())
        elif line.strip() and not line.startswith("#"):
            cleaned = line.replace("  ", " ").strip()
            if cleaned:
                meta.append(cleaned)
    return {"title": title, "subtitle": subtitle, "meta": meta, "callout": " ".join(callout)}


def make_cover(cover_md: str, styles: dict) -> list:
    data = parse_cover(cover_md)
    title = data["title"]
    subtitle = data["subtitle"]
    meta = [m for m in data["meta"] if not m.startswith("**")]
    callout = data["callout"]

    story: list = []
    story.append(Spacer(1, 28 * mm))
    story.append(Paragraph("AI USE-CASE / API KEY REVIEW BRIEF", styles["cover_label"]))
    story.append(Paragraph(inline_markup(title), styles["cover_title"]))
    story.append(Paragraph(inline_markup(subtitle), styles["cover_subtitle"]))
    story.append(Spacer(1, 6 * mm))

    rows = []
    for line in meta:
        if ":" in line:
            key, value = line.split(":", 1)
            rows.append(
                [
                    Paragraph(inline_markup(key.strip()), styles["small"]),
                    Paragraph(inline_markup(value.strip()), styles["body"]),
                ]
            )
    if rows:
        table = Table(rows, colWidths=[35 * mm, PAGE_W - MARGIN_LEFT - MARGIN_RIGHT - 35 * mm - 4 * mm])
        table.setStyle(
            TableStyle(
                [
                    ("BACKGROUND", (0, 0), (-1, -1), PALE_BLUE),
                    ("BOX", (0, 0), (-1, -1), 0.45, BORDER),
                    ("INNERGRID", (0, 0), (-1, -1), 0.25, colors.HexColor("#D7E1EA")),
                    ("LEFTPADDING", (0, 0), (-1, -1), 8),
                    ("RIGHTPADDING", (0, 0), (-1, -1), 8),
                    ("TOPPADDING", (0, 0), (-1, -1), 7),
                    ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
                    ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ]
            )
        )
        story.append(table)
        story.append(Spacer(1, 8 * mm))

    if callout:
        story.append(make_callout(callout, styles))

    story.append(Spacer(1, 24 * mm))
    story.append(
        Paragraph(
            "Prepared as a concise approval brief: business value first, controlled scope, measurable pilot outcomes.",
            styles["small"],
        )
    )
    story.append(PageBreak())
    return story


def draw_page(canvas, doc) -> None:  # noqa: ANN001 - ReportLab callback signature
    canvas.saveState()
    page_num = canvas.getPageNumber()
    width, height = A4

    # Header accent.
    if page_num == 1:
        canvas.setFillColor(NAVY)
        canvas.rect(0, height - 9 * mm, width, 9 * mm, stroke=0, fill=1)
        canvas.setFillColor(ACCENT)
        canvas.rect(0, height - 10.5 * mm, width, 1.5 * mm, stroke=0, fill=1)
    else:
        canvas.setStrokeColor(colors.HexColor("#D6DEE8"))
        canvas.setLineWidth(0.6)
        canvas.line(MARGIN_LEFT, height - 13 * mm, width - MARGIN_RIGHT, height - 13 * mm)
        canvas.setFont("Helvetica", 7.5)
        canvas.setFillColor(MID_GREY)
        canvas.drawString(MARGIN_LEFT, height - 10 * mm, "RTS AI Analysis Service PoC | LLM API Key Review Brief")

    # Footer.
    canvas.setStrokeColor(colors.HexColor("#D6DEE8"))
    canvas.setLineWidth(0.5)
    canvas.line(MARGIN_LEFT, 12 * mm, width - MARGIN_RIGHT, 12 * mm)
    canvas.setFont("Helvetica", 7.5)
    canvas.setFillColor(MID_GREY)
    canvas.drawString(MARGIN_LEFT, 7.5 * mm, "Internal review brief | Controlled PoC request")
    canvas.drawRightString(width - MARGIN_RIGHT, 7.5 * mm, f"Page {page_num}")
    canvas.restoreState()


def build_pdf() -> None:
    OUTPUT_PDF.parent.mkdir(parents=True, exist_ok=True)
    VERIFY_JSON.parent.mkdir(parents=True, exist_ok=True)

    markdown = SOURCE_MD.read_text(encoding="utf-8")
    cover_md, body_md = split_cover(markdown)
    styles = make_styles()

    story = make_cover(cover_md, styles)
    story.extend(parse_markdown_body(body_md, styles))

    doc = BaseDocTemplate(
        str(OUTPUT_PDF),
        pagesize=A4,
        leftMargin=MARGIN_LEFT,
        rightMargin=MARGIN_RIGHT,
        topMargin=MARGIN_TOP,
        bottomMargin=MARGIN_BOTTOM,
        title="RTS AI Analysis Service PoC",
        author="RTS",
        subject="LLM API key review brief",
    )
    frame = Frame(
        MARGIN_LEFT,
        MARGIN_BOTTOM + 4 * mm,
        PAGE_W - MARGIN_LEFT - MARGIN_RIGHT,
        PAGE_H - MARGIN_TOP - MARGIN_BOTTOM - 8 * mm,
        id="normal",
    )
    doc.addPageTemplates([PageTemplate(id="all", frames=[frame], onPage=draw_page)])
    doc.build(story)


def count_pages(pdf_path: Path) -> int | None:
    try:
        from pypdf import PdfReader  # type: ignore

        return len(PdfReader(str(pdf_path)).pages)
    except Exception:
        pass
    try:
        from PyPDF2 import PdfReader  # type: ignore

        return len(PdfReader(str(pdf_path)).pages)
    except Exception:
        pass
    try:
        data = pdf_path.read_bytes()
        return len(re.findall(rb"/Type\s*/Page\b", data))
    except Exception:
        return None


def verify_pdf() -> dict:
    data = OUTPUT_PDF.read_bytes() if OUTPUT_PDF.exists() else b""
    page_count = count_pages(OUTPUT_PDF) if data else None
    result = {
        "pdf_path": str(OUTPUT_PDF),
        "source_markdown_path": str(SOURCE_MD),
        "script_path": str(Path(__file__).resolve()),
        "exists": OUTPUT_PDF.exists(),
        "starts_with_pdf": data.startswith(b"%PDF"),
        "size_bytes": len(data),
        "page_count": page_count,
        "verified": bool(data.startswith(b"%PDF") and len(data) > 20_000 and page_count),
    }
    VERIFY_JSON.write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return result


def main() -> None:
    build_pdf()
    result = verify_pdf()
    print(json.dumps(result, indent=2, ensure_ascii=False))
    if not result["verified"]:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
