"""Shared helpers to turn `pdftohtml -xml` output into structured rows.

Every MTC balotario PDF is a single wide table (Nº | Materia | Categoria |
Tema | Descripcion | Alternativa 1-4 | Respuesta | [Fundamento]) whose cells
wrap across multiple lines. `pdftohtml -xml` emits every text fragment and
every embedded image with pixel bounding boxes in one shared coordinate
space per page, which lets us reconstruct rows and images->row associations
positionally instead of parsing wrapped plain text.
"""
from __future__ import annotations

import subprocess
import unicodedata
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path
from typing import NamedTuple


class TextEl(NamedTuple):
    page: int
    top: int
    left: int
    width: int
    height: int
    font: str
    text: str


class ImageEl(NamedTuple):
    page: int
    top: int
    left: int
    width: int
    height: int
    src: Path


def run_pdftohtml(pdf_path: Path, workdir: Path) -> Path:
    workdir.mkdir(parents=True, exist_ok=True)
    prefix = workdir / pdf_path.stem
    subprocess.run(
        ["pdftohtml", "-xml", "-q", str(pdf_path), str(prefix)],
        check=True,
    )
    return prefix.with_suffix(".xml")


def _norm(s: str) -> str:
    s = unicodedata.normalize("NFKD", s)
    return "".join(c for c in s if not unicodedata.combining(c)).strip().upper()


def parse_xml(xml_path: Path) -> tuple[list[TextEl], list[ImageEl]]:
    tree = ET.parse(xml_path)
    texts: list[TextEl] = []
    images: list[ImageEl] = []
    for page in tree.getroot().findall("page"):
        page_num = int(page.get("number"))
        for el in page.findall("text"):
            content = "".join(el.itertext()).strip()
            if not content:
                continue
            texts.append(
                TextEl(
                    page=page_num,
                    top=int(el.get("top")),
                    left=int(el.get("left")),
                    width=int(el.get("width")),
                    height=int(el.get("height")),
                    font=el.get("font", ""),
                    text=content,
                )
            )
        for el in page.findall("image"):
            images.append(
                ImageEl(
                    page=page_num,
                    top=int(el.get("top")),
                    left=int(el.get("left")),
                    width=int(el.get("width")),
                    height=int(el.get("height")),
                    src=xml_path.parent / el.get("src"),
                )
            )
    texts.sort(key=lambda t: (t.page, t.top, t.left))
    images.sort(key=lambda i: (i.page, i.top, i.left))
    return texts, images


# Header labels we look for on the first page to derive column x-ranges.
# A given header may be split across two stacked lines (A-license PDFs) or
# a single line (B-license PDFs) - substring match handles both.
_HEADER_LABELS = {
    "numero": ["N"],
    "tema": ["TEMA"],
    "descripcion": ["DESCRIPCION DE LA PREGUNTA"],
    "alt1": ["ALTERNATIVA 1"],
    "alt2": ["ALTERNATIVA 2"],
    "alt3": ["ALTERNATIVA 3"],
    "alt4": ["ALTERNATIVA 4"],
    "respuesta": ["RESPUESTA"],
    "fundamento": ["FUNDAMENTO"],
}


def _cluster_lefts(lefts: list[int], target_count: int, radius: int = 15) -> list[int]:
    """Find each column's true left edge via local-maximum peak detection on
    the raw per-pixel left-position histogram, sorted ascending.

    Some balotario PDFs (mainly B-license ones) emit one text fragment per
    *word* rather than per line, so a narrow column's wrapped paragraph
    produces word-start x-positions spread almost all the way across the
    column. A naive "merge everything within N px, sum the weights" cluster
    pass falls apart here: chaining together many nearby-but-distinct word
    starts (each 1-2px apart) across an entire densely-packed column can
    bridge all the way into the next column, or manufacture a large but
    spurious "shadow" cluster next to the real one. Genuine column edges
    instead show up as sharp local maxima in the raw per-pixel histogram - a
    real column's left edge is shared by every row (at least one line each)
    and stands out clearly against its immediate neighborhood, even though
    the neighborhood itself is noisy - so peak-picking finds them directly
    without ever summing weight across a wide span.

    A column can still be right-aligned or centered rather than flush left
    (e.g. the Nº column, where a 1-digit and 3-digit question number don't
    start at the same x), spreading its true total frequency across several
    adjacent x-positions instead of one. Comparing peaks by their own single
    -pixel count would then undercount that column relative to a narrow,
    perfectly flush-left one, so each peak's raw weight is only used to find
    its *position* - every data point is then reassigned to its nearest
    peak and summed, giving each peak its true aggregate frequency before
    `target_count` (from the table's known column count, not a pixel value)
    keeps only the most frequent ones - real columns are still far more
    frequent overall than incidental word-start coincidences.
    """
    counts = Counter(lefts)
    if not counts:
        return []
    positions = sorted(counts)
    n = len(positions)
    peak_positions: list[int] = []
    lo = hi = 0
    for x in positions:
        while positions[lo] < x - radius:
            lo += 1
        while hi < n and positions[hi] <= x + radius:
            hi += 1
        w = counts[x]
        if all(counts[positions[j]] <= w for j in range(lo, hi) if positions[j] != x):
            # Consolidate plateaus / near-duplicate peaks within `radius` of
            # the previous one into a single representative (higher weight
            # wins), rather than keeping both.
            if peak_positions and x - peak_positions[-1] <= radius:
                if w > counts[peak_positions[-1]]:
                    peak_positions[-1] = x
            else:
                peak_positions.append(x)

    # Reassign every raw data point within `radius` of its nearest peak and
    # sum, so each peak's weight reflects its true total frequency rather
    # than just the single x-position that happened to be the local
    # maximum. Points farther than `radius` from every peak are dropped
    # instead of forced onto the closest one regardless of distance -
    # otherwise the noisy "carpet" of incidental word-start positions in a
    # wide gap between two real columns (e.g. spread across a long wrapped
    # paragraph) gets vacuumed into whichever low-weight spurious peak
    # happens to sit in that gap, inflating it enough to outrank a genuine
    # but naturally low-frequency column.
    totals = {p: 0 for p in peak_positions}
    for x in positions:
        nearest = min(peak_positions, key=lambda p: abs(p - x))
        if abs(nearest - x) <= radius:
            totals[nearest] += counts[x]

    ranked = sorted(totals.items(), key=lambda kv: kv[1])
    while len(ranked) > target_count:
        ranked.pop(0)
    return sorted(p for p, _ in ranked)


def _snap_headers_to_clusters(
    ordered_headers: list[tuple[str, int]], clusters: list[int]
) -> dict[str, int]:
    """Column header labels are drawn centered/right-shifted over their
    column and are not reliably aligned with where the column's actual
    content starts (e.g. a wide "DESCRIPCIÓN DE LA PREGUNTA" header sits
    well to the right of the paragraph text beneath it). Snap each header,
    in left-to-right order, to the nearest not-yet-used content-column
    x-cluster so `columns` reflects where the data actually lives instead
    of where the label happens to be drawn. Clusters between two assigned
    headers (e.g. untracked Materia/Categoria columns) are skipped.

    NOTE: exact-match verified only against CLASE_A_I.pdf (via
    test_pdf_layout.py's full text comparison against the human-verified
    a1_questions.json). The other 8 PDFs only get a coarser smoke check
    (all expected columns present, plausible row counts, no header-word
    leakage) - if a future task's own validation against those finds a
    column boundary off, this greedy nearest-cluster assignment is the
    first place to check."""
    snapped: dict[str, int] = {}
    ci = 0
    for key, hleft in ordered_headers:
        if ci >= len(clusters):
            snapped[key] = hleft
            continue
        while ci + 1 < len(clusters) and abs(clusters[ci + 1] - hleft) <= abs(clusters[ci] - hleft):
            ci += 1
        snapped[key] = clusters[ci]
        ci += 1
    return snapped


def _match_header_labels(candidate_texts: list[TextEl]) -> dict[str, TextEl]:
    """Find header label text elements (by matching known column labels,
    substring/exact rather than relying on a fixed x/y position since page
    geometry differs per PDF) within a given set of texts. Shared by
    detect_columns and _find_header_rows.

    Callers must pre-filter `candidate_texts` to a single candidate header
    line (see _find_header_rows) rather than passing a whole page's texts:
    some header labels are short enough to appear as a substring of an
    unrelated word elsewhere on the page - e.g. "TEMA" inside "SISTEMA" -
    and matching against a whole page can pick up a coincidental hit like
    that instead of the real header, especially on pages that contain more
    than one balotario table (see _find_header_rows)."""
    found: dict[str, TextEl] = {}
    for t in candidate_texts:
        norm = _norm(t.text)
        for key, labels in _HEADER_LABELS.items():
            if key in found:
                continue
            for label in labels:
                if key == "numero":
                    if norm == "N" or norm == "NO" or norm.startswith("N "):
                        found[key] = t
                elif label in norm:
                    found[key] = t
    return found


# A real header row always has every one of its (up to 9) labels land on
# the exact same line, so requiring most of them to co-occur is enough to
# tell a genuine header row apart from a single incidental substring match
# (which essentially never lands several different header words on the
# same exact `top`).
_MIN_HEADER_LABELS_PER_ROW = 5


def _find_header_rows(texts: list[TextEl]) -> list[tuple[int, int, dict[str, TextEl]]]:
    """Find every occurrence of a genuine header row anywhere in the
    document, sorted by document order (page, top). Each is
    `(page, top, matched_labels)`.

    pdftohtml reprints the same table header at the top of every page, not
    just page 1 - and some balotario PDFs contain more than one table
    (e.g. the main N-question table followed by a supplementary section
    with its own header row, occasionally even adding a Fundamento column
    an A-license table otherwise wouldn't have), so this doesn't assume
    exactly one header per page. Header cells belonging to the same header
    row all share the same exact `top` (verified across every PDF checked
    so far), so grouping candidates that way - and requiring a strong
    majority of labels to match within a group - avoids the false-positive
    risk described in _match_header_labels's docstring: an incidental
    substring collision only ever produces a single match at its own
    `top`, never enough to pass _MIN_HEADER_LABELS_PER_ROW.
    """
    by_page_top: dict[tuple[int, int], list[TextEl]] = {}
    for t in texts:
        by_page_top.setdefault((t.page, t.top), []).append(t)
    header_rows: list[tuple[int, int, dict[str, TextEl]]] = []
    for (page, top), group in by_page_top.items():
        found = _match_header_labels(group)
        if len(found) >= _MIN_HEADER_LABELS_PER_ROW:
            header_rows.append((page, top, found))
    header_rows.sort(key=lambda r: (r[0], r[1]))
    return header_rows


def detect_header_rows(texts: list[TextEl]) -> list[tuple[int, int, int]]:
    """Public: `(page, top, bottom)` for every header-row occurrence in the
    document, sorted by document order. See _find_header_rows for why
    there can be more than one per page. `build_row_bands` uses this so
    that:

    - a question row that starts a new page begins its band *after* that
      page's header, instead of at the physical top of the page (which
      would otherwise sweep the reprinted header's own text - e.g. the
      literal word "TEMA" - into that question, and double-claim that
      space with the previous question's band too, since without this it
      has no way to know a header sits there and stop before the new page
      begins); and
    - the very last detected question's band, which otherwise extends all
      the way to the end of the document, stops instead at the next
      header-row occurrence after it, if any - so a second, unrelated
      table later in the same PDF doesn't get swept into the last real
      question.
    """
    return [
        (page, top, max(t.top + t.height for t in found.values()))
        for page, top, found in _find_header_rows(texts)
    ]


def detect_columns(texts: list[TextEl]) -> dict[str, tuple[int, int]]:
    page1_header_rows = _find_header_rows([t for t in texts if t.page == 1])
    found = page1_header_rows[0][2] if page1_header_rows else {}
    ordered = sorted(((key, t.left) for key, t in found.items()), key=lambda kv: kv[1])

    # Snap each header's x-position to where its column's content actually
    # starts, using the left edges of text below the header row. Sample
    # across the whole document (not just page 1) so the frequency signal
    # used to separate real column edges from noise has enough data even
    # when only a couple of rows fit on the first page.
    header_bottom = max((t.top + t.height for t in found.values()), default=0)
    body_lefts = [t.left for t in texts if t.page > 1 or t.top > header_bottom]
    # Every balotario PDF shares the same table schema (see module
    # docstring): Nº, Materia, Categoria, Tema, Descripcion, Alternativa
    # 1-4, Respuesta, [Fundamento]. Materia and Categoria aren't in
    # _HEADER_LABELS (nothing needs their text), so `ordered` never
    # includes them - but they still show up as two extra body-text
    # clusters between "numero" and "tema" that the noise-pruning in
    # _cluster_lefts needs to know to keep.
    target_count = len(ordered) + 2
    clusters = _cluster_lefts(body_lefts, target_count)
    lefts = _snap_headers_to_clusters(ordered, clusters) if clusters else dict(ordered)

    columns: dict[str, tuple[int, int]] = {}
    keys_in_order = [key for key, _ in ordered]
    for i, key in enumerate(keys_in_order):
        left = lefts[key]
        right = lefts[keys_in_order[i + 1]] if i + 1 < len(keys_in_order) else 10**6
        columns[key] = (left, right)
    return columns


def detect_question_rows(
    texts: list[TextEl], columns: dict[str, tuple[int, int]]
) -> list[tuple[int, int, int]]:
    lo, hi = columns["numero"]
    rows: list[tuple[int, int, int]] = []
    for t in texts:
        if not (lo - 5 <= t.left < hi):
            continue
        if not t.text.strip().isdigit():
            continue
        rows.append((int(t.text.strip()), t.page, t.top))
    # Keep strictly increasing question numbers in document order; drop
    # accidental digit matches (e.g. a number that lands in the Nº column
    # x-range but isn't actually the next question).
    #
    # NOTE: this has no recovery path if a genuine question number is
    # missed or misdetected (e.g. OCR/rendering noise merges "23" into an
    # unrecognizable glyph) - `expected` never advances past that point, so
    # every subsequent row in the document is silently dropped rather than
    # just the one bad row. Not observed in any of the 9 balotario PDFs
    # (each yields exactly its expected question count), but if a future
    # PDF comes up short, check here first rather than assuming a column-
    # detection problem.
    cleaned: list[tuple[int, int, int]] = []
    expected = 1
    for qnum, page, top in rows:
        if qnum == expected:
            cleaned.append((qnum, page, top))
            expected += 1
    return cleaned


def build_row_bands(
    rows: list[tuple[int, int, int]],
    last_page: int,
    header_rows: list[tuple[int, int, int]],
) -> list[tuple[int, int, int, int, int]]:
    # The Nº cell is vertically centered within its (possibly multi-line)
    # row rather than pinned to the row's top line, so a question's other
    # columns routinely start several lines above its own digit's `top`
    # (e.g. a 4-line alternative wrapped around a 1-line Nº). Splitting
    # bands at the raw digit tops therefore clips a row's leading lines
    # into the previous band. Instead, split at the midpoint between two
    # consecutive rows' digit tops when they're on the same page, so each
    # band's boundary falls in the gap between rows rather than mid-row.
    #
    # A row that starts a new page (the document's first row, or any row
    # whose predecessor is on an earlier page) has no predecessor on that
    # page to split against - and pdftohtml reprints the table header at
    # the top of every page, so the naive fallback of "start of page" would
    # sweep that reprinted header text into the row. `header_rows` (see
    # detect_header_rows) gives the real per-page boundary instead: the
    # first header-row occurrence on each page.
    first_header_bottom_by_page: dict[int, int] = {}
    for page, _top, bottom in header_rows:
        first_header_bottom_by_page.setdefault(page, bottom)

    # Symmetrically, a row whose *successor* is on a later page never
    # extends into that later page itself: in every balotario PDF we've
    # checked, a question's own content is fully contained on the page its
    # Nº digit is on (the page break falls between rows, not mid-row), so
    # capping the band at the bottom of its own starting page - rather
    # than reaching into the next row's page up to the next row's raw
    # digit top - avoids the previous bug where two adjacent bands both
    # claimed the reprinted-header region of the new page and duplicated
    # its content between them.
    bands = []
    n = len(rows)
    for i, (qnum, page, top) in enumerate(rows):
        if i > 0 and rows[i - 1][1] == page:
            start_page, start_top = page, (rows[i - 1][2] + top) // 2
        else:
            start_page, start_top = page, first_header_bottom_by_page.get(page, 0)

        if i + 1 < n and rows[i + 1][1] == page:
            end_page, end_top = page, (top + rows[i + 1][2]) // 2
        elif i + 1 < n:
            end_page, end_top = page, 10**6
        else:
            # Last detected question: its band would otherwise extend all
            # the way to the document end, but some balotario PDFs contain
            # a second, unrelated table later in the same document (e.g. a
            # supplementary section after the main N questions) with its
            # own header row - without capping there, that whole second
            # table gets swept into this last question. Stop at the next
            # header-row occurrence after this row, if any; only fall back
            # to the true document end when there isn't one.
            next_header = next(
                ((hp, ht) for hp, ht, _hb in header_rows if (hp, ht) > (page, top)),
                None,
            )
            end_page, end_top = next_header if next_header else (last_page, 10**6)
        bands.append((qnum, start_page, start_top, end_page, end_top))
    return bands


def text_in_band(
    texts: list[TextEl],
    band: tuple[int, int, int, int, int],
    col_range: tuple[int, int],
) -> str:
    qnum, sp, st, ep, et = band
    lo, hi = col_range
    lines: list[tuple[int, int, str]] = []
    for t in texts:
        if not (lo - 5 <= t.left < hi):
            continue
        if (t.page, t.top) < (sp, st) or (t.page, t.top) >= (ep, et):
            continue
        lines.append((t.page, t.top, t.text))
    lines.sort(key=lambda l: (l[0], l[1]))
    return " ".join(l[2] for l in lines).strip()
