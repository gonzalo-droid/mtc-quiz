"""Extract questions from a balotario PDF into the app's JSON schema.

Usage:
  python3 parse_questions.py <examId>

Writes/overwrites app/src/main/assets/json/<examId>_questions.json. Only
touches question text fields (id, section, category, topic, title, answer,
options, fundamento) - never any image-association field (e.g. "image"),
that's extract_images.py's job and running this script again must not
clobber image associations: any non-text-field key already present on an
existing entry for the same question id is carried over unchanged into the
freshly generated entry.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from pdf_layout import (  # noqa: E402
    build_row_bands,
    detect_columns,
    detect_header_rows,
    detect_question_rows,
    parse_xml,
    run_pdftohtml,
    text_in_band,
)

REPO = Path(__file__).resolve().parents[4]
PDF_DIR = REPO / "app/src/main/assets/pdf"
JSON_DIR = REPO / "app/src/main/assets/json"

EXAM_TO_PDF = {
    "a1": ("CLASE_A_I", "AI"),
    "a2a": ("CLASE_A_IIA", "AIIA"),
    "a2b": ("CLASE_A_IIB", "AIIB"),
    "a3a": ("CLASE_A_IIIA", "AIIIA"),
    "a3b": ("CLASE_A_IIIB", "AIIIB"),
    "a3c": ("CLASE_A_IIIC", "AIIIC"),
    "b2a": ("CLASE_B_IIA", "BIIA"),
    "b2b": ("CLASE_B_IIB", "BIIB"),
    "b2c": ("CLASE_B_IIC", "BIIC"),
}

_SECTION_KEYWORDS = {
    "MATERIAS GENERALES": "Materias generales",
    "MATERIAS ESPECIFICAS": "Materias específicas",
    "MATERIAS ESPECÍFICAS": "Materias específicas",
}

# Fields this script owns and (re)generates from the PDF on every run. Any
# other key found on an existing entry for the same question id (e.g. an
# "image" association written by extract_images.py) is preserved as-is.
_TEXT_FIELDS = {"id", "section", "category", "topic", "title", "answer", "options", "fundamento"}

# Any wrapped cell (descripcion, tema, alt1-4, fundamento) wraps at ~13px
# per line (checked across every one of CLASE_A_I.pdf's 200 questions -
# see _split_leaked_lines). build_row_bands splits consecutive rows at
# the midpoint of their two Nº-digit tops, which is normally safely past
# a row's own content in every column but before the next row's - except
# when a row embeds a picture in its own descripcion cell (e.g. a
# traffic-sign image the question refers to): the picture inflates that
# row's visual height, pushing its own Nº digit down well past where its
# own content starts, sometimes past where the *next* row's content in
# one or more columns genuinely begins too (observed for descripcion,
# alt1 and alt2 - not just descripcion). When that happens the next
# row's line (or its first wrapped line) gets swept into the current
# row's band instead. Checked against all 200 questions in
# CLASE_A_I.pdf: every genuine same-cell line wrap sits 13px below the
# previous line; every observed leaked-in line from the next question
# sits >=20px below - a clean, wide margin either side. `respuesta` is
# deliberately excluded from this treatment: it's always a single
# character with no wrapping, so it was never affected (verified: 0
# answer mismatches against a1_questions.json's 200 ground-truth rows).
_LEAK_GAP_THRESHOLD = 18
_WRAPPED_COLUMN_KEYS = ("tema", "descripcion", "alt1", "alt2", "alt3", "alt4", "fundamento")

# alt1-4 have a further wrinkle beyond the image-inflation leak above: a
# genuine option boundary can sit as little as ~16-18px from the previous
# option's last line - overlapping the same ~13px-vs-"bigger gap" range a
# leaked line would occupy - so gap size alone can't always tell "next
# option starts here" apart from "this option's own paragraph continues"
# (verified: a plain gap-threshold reconstruction of alt1-4 across all 200
# CLASE_A_I.pdf questions produces a few wrongly-merged pairs). alt1-4
# don't need that guesswork though: every option's first line literally
# starts with its own column's letter (alt1 -> "a)", alt2 -> "b)", etc.),
# by the exam's own printed formatting - a reliable, content-based
# paragraph boundary that doesn't depend on pixel gaps at all. Used as the
# primary extraction method for alt1-4 (see _lettered_column_paragraphs),
# falling back to the gap/leak-based _LeakAwareExtractor above only if its
# own paragraph count doesn't land on exactly one per question (e.g. a
# genuinely different PDF layout quirk _lettered_column_paragraphs isn't
# meant to handle, such as CLASE_A_I.pdf q179's alt1 and alt2 - both short
# enough that the PDF itself renders them as one merged text fragment at
# alt1's own x-position, with no separate alt2 fragment at all to find a
# "b)" prefix on in the first place).
_OPTION_LETTERS = {"alt1": "a", "alt2": "b", "alt3": "c", "alt4": "d"}


def detect_section_for_band(texts, band, columns) -> str:
    """Find the closest preceding full-width section-header line."""
    qnum, sp, st, ep, et = band
    numero_left = columns["numero"][0]
    best = "Materias generales"
    for t in texts:
        if (t.page, t.top) >= (sp, st):
            break
        if t.left > numero_left + 40:
            continue  # section headers start at/near the left margin
        for kw, label in _SECTION_KEYWORDS.items():
            if kw in t.text.upper():
                best = label
    return best


def _column_lines(
    texts, band, col_range: tuple[int, int]
) -> list[tuple[int, int, str]]:
    """Every individual text line (page, top, text) in `band` within
    `col_range`, in document order - like `text_in_band`, but returning the
    raw per-line list instead of one joined string, so `_split_leaked_lines`
    can inspect the vertical gaps between lines."""
    lo, hi = col_range
    _qnum, sp, st, ep, et = band
    lines = [
        (t.page, t.top, t.text)
        for t in texts
        if lo - 5 <= t.left < hi and (t.page, t.top) >= (sp, st) and (t.page, t.top) < (ep, et)
    ]
    # Sort by (page, top) only, NOT the full tuple (which would fall back
    # to comparing `text` alphabetically whenever two fragments share the
    # same top - scrambling word order). Some balotario PDFs (mainly
    # B-license ones, per pdf_layout.py's _cluster_lefts docstring) emit
    # one fragment per *word* rather than per line, so many same-line
    # fragments genuinely share the same top and rely on Python's stable
    # sort to preserve `texts`' own left-to-right document order (parse_xml
    # sorts by (page, top, left)) for those ties - exactly how
    # `text_in_band` already does it.
    lines.sort(key=lambda line: (line[0], line[1]))
    return lines


def _split_leaked_lines(
    lines: list[tuple[int, int, str]],
) -> tuple[list[tuple[int, int, str]], list[tuple[int, int, str]]]:
    """Split a band's raw lines (for one column) at the first abnormally
    large same-page gap (see _LEAK_GAP_THRESHOLD), if any. Returns
    `(own_lines, leaked_lines)` - `leaked_lines` actually belongs to the
    *next* question, not this one (see the module-level comment on
    _LEAK_GAP_THRESHOLD). A gap across a page boundary is never treated as
    a leak - unlike the image-inflation bug, a cell that legitimately
    continues onto the next page has no well-defined "same page" pixel gap
    to compare against 13px, and every observed leak is a same-page
    phenomenon anyway (the picture and the leaked line both sit on the
    row's own starting page)."""
    for i in range(1, len(lines)):
        if (
            lines[i][0] == lines[i - 1][0]
            and lines[i][1] - lines[i - 1][1] > _LEAK_GAP_THRESHOLD
        ):
            return lines[:i], lines[i:]
    return lines, []


class _LeakAwareExtractor:
    """Extracts wrapped-column text band-by-band (must be called in
    ascending qnum order, matching `bands`' own order), carrying any line
    detected as leaked from row i's band into row i+1's text - see
    _LEAK_GAP_THRESHOLD. One instance covers one column; each of
    _WRAPPED_COLUMN_KEYS gets its own, so a leak in one column (e.g.
    descripcion) doesn't affect the leak tracking of another (e.g. alt1) -
    each column's own picture-adjacency is independent."""

    def __init__(self, texts, columns, key: str) -> None:
        self._texts = texts
        self._col_range = columns[key]
        self._pending: dict[int, list[tuple[int, int, str]]] = {}

    def extract(self, band) -> str:
        qnum = band[0]
        lines = _column_lines(self._texts, band, self._col_range)
        own, leaked = _split_leaked_lines(lines)
        combined = self._pending.pop(qnum, []) + own
        if leaked:
            self._pending[qnum + 1] = leaked
        return " ".join(text for _page, _top, text in combined).strip()


def _lettered_column_paragraphs(
    texts,
    col_range: tuple[int, int],
    letter: str,
    overall_start: tuple[int, int],
    overall_end: tuple[int, int],
    header_rows,
) -> list[str]:
    """Reconstruct one alt1-4 column's options across the *whole* question
    table (not band-by-band): every text line in `col_range` between
    `overall_start` and `overall_end` (the table's own full extent, from
    the first question's band start to the last one's end - excludes
    reprinted table headers via `header_rows`, and any unrelated content
    before/after the table entirely), in document order, starting a new
    option at each line whose text starts with `letter + ")"` (case-
    insensitive - see _OPTION_LETTERS) and otherwise treating it as a
    continuation of the current option. Returns each option as
    `((page, top), text)` - the position of its own first line, and the
    full joined text - in document order; the caller maps each one to a
    question number via _map_by_position rather than assuming a strict
    1:1 ordinal correspondence, since an option can rarely go missing
    entirely (see _OPTION_LETTERS's docstring on CLASE_A_I.pdf q179)."""
    lo, hi = col_range
    prefix_re = re.compile(rf"^{letter}\)", re.IGNORECASE)
    # Sort by (page, top) only - see _column_lines's comment on why: word-
    # per-fragment PDFs need the stable sort to fall back to `texts`' own
    # pre-sorted left-to-right order for fragments sharing the same top,
    # not an alphabetical comparison of `text`.
    lines = sorted(
        (
            (t.page, t.top, t.text)
            for t in texts
            if lo - 5 <= t.left < hi
            and overall_start <= (t.page, t.top) < overall_end
            and not any(hp == t.page and ht <= t.top < hb for hp, ht, hb in header_rows)
        ),
        key=lambda line: (line[0], line[1]),
    )
    paragraphs: list[list[tuple[int, int, str]]] = []
    current: list[tuple[int, int, str]] = []
    for page, top, text in lines:
        if prefix_re.match(text.strip()) and current:
            paragraphs.append(current)
            current = []
        current.append((page, top, text))
    if current:
        paragraphs.append(current)
    return [
        ((p[0][0], p[0][1]), " ".join(text for _p, _t, text in p).strip())
        for p in paragraphs
    ]


def _map_by_position(
    items: list[tuple[tuple[int, int], str]],
    bands: list[tuple[int, int, int, int, int]],
) -> dict[int, str]:
    """Map each `((page, top), text)` item (see
    _lettered_column_paragraphs) to the question number of the band whose
    own start it falls at-or-after, most recently - i.e. the band that
    "owns" the vertical territory the item's first line sits in. `bands`
    must be in ascending document order (as `build_row_bands` returns
    them). Unlike a plain ordinal zip, this tolerates an item going
    missing for one question (that qnum is simply absent from the
    returned dict - the caller falls back to a different extraction
    method for it) without throwing every later item's assignment off by
    one."""
    starts = [((b[1], b[2]), b[0]) for b in bands]
    mapping: dict[int, str] = {}
    for pos, text in items:
        owner = None
        for start_pos, qnum in starts:
            if start_pos <= pos:
                owner = qnum
            else:
                break
        if owner is not None:
            mapping[owner] = text
    return mapping


def _load_existing_extras(json_path: Path) -> dict[int, dict]:
    """Load any non-text-field keys (e.g. "image") from an existing output
    file, keyed by question id, so a re-run doesn't clobber them."""
    if not json_path.exists():
        return {}
    try:
        existing = json.loads(json_path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError):
        return {}
    extras: dict[int, dict] = {}
    for entry in existing.get("data", []):
        extra = {k: v for k, v in entry.items() if k not in _TEXT_FIELDS}
        if extra:
            extras[entry["id"]] = extra
    return extras


def main() -> None:
    exam_id = sys.argv[1]
    pdf_stem, category = EXAM_TO_PDF[exam_id]
    pdf_path = PDF_DIR / f"{pdf_stem}.pdf"
    json_path = JSON_DIR / f"{exam_id}_questions.json"
    workdir = Path(f"/tmp/mtc_extractor_{exam_id}")

    existing_extras = _load_existing_extras(json_path)

    xml_path = run_pdftohtml(pdf_path, workdir)
    texts, images = parse_xml(xml_path)
    columns = detect_columns(texts)
    rows = detect_question_rows(texts, columns)
    header_rows = detect_header_rows(texts)
    last_page = max(t.page for t in texts)
    bands = build_row_bands(rows, last_page, header_rows, texts, columns)

    # See _LEAK_GAP_THRESHOLD: a picture embedded in a row's own
    # descripcion cell can push that row's band boundary past where the
    # *next* row's content in a wrapped column actually starts, sweeping
    # the next question's line(s) into the current band. Each wrapped
    # column present gets its own extractor so a leak in one doesn't
    # affect another; each extractor carries any detected leak forward to
    # prepend onto the question it actually belongs to.
    extractors = {
        key: _LeakAwareExtractor(texts, columns, key)
        for key in _WRAPPED_COLUMN_KEYS
        if key in columns
    }

    # Primary extraction for alt1-4: whole-table, letter-prefix-based
    # reconstruction (see _OPTION_LETTERS), mapped to question numbers by
    # position (see _map_by_position) rather than a strict ordinal zip, so
    # one missing option (e.g. CLASE_A_I.pdf q179 - see _OPTION_LETTERS's
    # docstring) only drops that single cell to the gap/leak-based
    # extractor below instead of discarding the whole column's otherwise-
    # correct reconstruction.
    overall_start = (bands[0][1], bands[0][2])
    overall_end = (bands[-1][3], bands[-1][4])
    option_by_qnum: dict[str, dict[int, str]] = {}
    for key, letter in _OPTION_LETTERS.items():
        if key not in columns:
            continue
        items = _lettered_column_paragraphs(
            texts, columns[key], letter, overall_start, overall_end, header_rows
        )
        option_by_qnum[key] = _map_by_position(items, bands)

    questions = []
    for band in bands:
        qnum = band[0]
        title = extractors["descripcion"].extract(band)
        topic = extractors["tema"].extract(band)
        options = [
            option_by_qnum[k][qnum] if qnum in option_by_qnum.get(k, {}) else extractors[k].extract(band)
            for k in ("alt1", "alt2", "alt3", "alt4")
        ]
        answer_raw = text_in_band(texts, band, columns["respuesta"]).strip().lower()
        answer = re.sub(r"[^a-d]", "", answer_raw)[:1] or "a"
        entry = {
            "id": qnum,
            "section": detect_section_for_band(texts, band, columns),
            "category": category,
            "topic": topic,
            "title": title,
            "answer": answer,
            "options": options,
        }
        if "fundamento" in extractors:
            fundamento = extractors["fundamento"].extract(band)
            if fundamento:
                entry["fundamento"] = fundamento
        entry.update(existing_extras.get(qnum, {}))
        questions.append(entry)

    json_path.write_text(
        json.dumps({"data": questions}, ensure_ascii=False, indent=4), encoding="utf-8"
    )
    print(f"Wrote {len(questions)} questions to {json_path}")


if __name__ == "__main__":
    main()
