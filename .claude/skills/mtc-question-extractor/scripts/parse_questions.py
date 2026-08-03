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
from collections import Counter
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

# Any wrapped cell (descripcion, tema, alt1-4, fundamento) wraps at some
# roughly-constant per-line pixel gap that's stable *within* a given PDF
# (checked across every one of CLASE_A_I.pdf's 200 questions - see
# _adaptive_leak_threshold). build_row_bands splits consecutive rows at
# the midpoint of their two Nº-digit tops, which is normally safely past
# a row's own content in every column but before the next row's - except
# when a row embeds a picture in its own descripcion cell (e.g. a
# traffic-sign image the question refers to): the picture inflates that
# row's visual height, pushing its own Nº digit down well past where its
# own content starts, sometimes past where the *next* row's content in
# one or more columns genuinely begins too (observed for descripcion,
# alt1 and alt2 - not just descripcion), or cutting the *current* row's
# own trailing content off too early instead, stranding it at the start
# of the *next* row's naive band (observed for descripcion on
# CLASE_B_IIA.pdf - see _gap_based_column_paragraphs). Either way, content
# ends up on the wrong side of the digit-based band boundary.
#
# A fixed pixel threshold tuned against one PDF doesn't transfer: this
# was originally hardcoded to 18 (checked only against CLASE_A_I.pdf,
# where genuine same-cell wraps sit 12-13px apart and every observed leak
# sits >=20px apart - a wide, easy margin). CLASE_B_IIB.pdf/CLASE_B_IIC.pdf
# have a *tighter* normal wrap spacing (11-12px, not 12-13px) and at least
# one confirmed leak only 15px away from the previous line - comfortably
# under 18, so it went undetected and the two titles merged. Rather than
# pick a new fixed number that just happens to also work for the PDFs
# checked so far (and could just as easily fail the next one), the
# threshold is now derived per-document-per-column from that column's own
# observed gap-frequency distribution (see _adaptive_leak_threshold): the
# largest gap size that's still common enough to be "this is just how far
# apart two wrapped lines sit in this PDF", one past which a gap is
# assumed to be a genuine paragraph/question boundary instead. `respuesta`
# is deliberately excluded from this treatment: it's always a single
# character with no wrapping, so it was never affected (verified: 0
# answer mismatches against a1_questions.json's 200 ground-truth rows).
_WRAPPED_COLUMN_KEYS = ("tema", "descripcion", "alt1", "alt2", "alt3", "alt4", "fundamento")

# Fallback threshold only used when a column has too few observed gaps in
# this document to compute a meaningful distribution (see
# _adaptive_leak_threshold) - matches the original CLASE_A_I.pdf-derived
# value so behavior is unchanged in that degenerate case.
_LEAK_GAP_THRESHOLD_FALLBACK = 18

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
# falling back to plain per-band `text_in_band` only if its own paragraph
# count doesn't land on exactly one per question (e.g. a genuinely
# different PDF layout quirk _lettered_column_paragraphs isn't meant to
# handle, such as CLASE_A_I.pdf q179's alt1 and alt2 - both short enough
# that the PDF itself renders them as one merged text fragment at alt1's
# own x-position, with no separate alt2 fragment at all to find a "b)"
# prefix on in the first place).
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


def _adaptive_leak_threshold(
    texts,
    bands: list[tuple[int, int, int, int, int]],
    col_range: tuple[int, int],
    min_significant_count: int = 2,
    significant_fraction: float = 0.05,
) -> int:
    """Derive a leak-detection threshold for one column, specific to this
    document, from that column's own observed gap-frequency distribution -
    see the module-level comment above _WRAPPED_COLUMN_KEYS on why a fixed
    pixel constant tuned against one PDF doesn't transfer to another.

    Gathers every same-page consecutive-line gap within each band's own
    (possibly leak-contaminated, since these are the ordinary digit-based
    bands - see build_row_bands) capture of this column, across the whole
    document. A handful of genuine leaks mixed into this sample doesn't
    skew the result: leaks are rare relative to the hundreds of ordinary
    same-cell line wraps in a 200+-question exam, so the *frequent* gap
    sizes are reliably this column's normal wrap spacing regardless.
    Returns the largest gap size whose own frequency is still "common" -
    at least `min_significant_count`, or at least `significant_fraction`
    of the single most common gap's frequency, whichever is bigger - i.e.
    the upper edge of the normal cluster. Checked against CLASE_A_I.pdf
    (12-13px normal, un-populated 14-19px, genuine leaks at 20px+),
    CLASE_B_IIA.pdf/CLASE_B_IIB.pdf/CLASE_B_IIC.pdf (11-12px normal,
    confirmed leaks as close as 15px) and every column checked in each
    (tema, descripcion, fundamento) - correctly lands on each column's own
    real normal-wrap ceiling in every case, including the CLASE_B_*.pdf
    cases where 18 (this project's original hardcoded value, tuned only
    against CLASE_A_I.pdf) was too high to catch a real 15px leak.

    Falls back to _LEAK_GAP_THRESHOLD_FALLBACK when there's no gap data at
    all to compute a distribution from (e.g. a column that's always a
    single line, so no band ever has an internal gap to observe)."""
    lo, hi = col_range
    gaps: Counter[int] = Counter()
    for band in bands:
        _qnum, sp, st, ep, et = band
        lines = sorted(
            (t.page, t.top)
            for t in texts
            if lo - 5 <= t.left < hi and (t.page, t.top) >= (sp, st) and (t.page, t.top) < (ep, et)
        )
        for i in range(1, len(lines)):
            if lines[i][0] == lines[i - 1][0]:
                gap = lines[i][1] - lines[i - 1][1]
                if gap > 0:
                    gaps[gap] += 1
    if not gaps:
        return _LEAK_GAP_THRESHOLD_FALLBACK
    max_count = max(gaps.values())
    floor = max(min_significant_count, max_count * significant_fraction)
    significant = [g for g, c in gaps.items() if c >= floor]
    return max(significant)


def _gap_based_column_paragraphs(
    texts,
    col_range: tuple[int, int],
    overall_start: tuple[int, int],
    overall_end: tuple[int, int],
    header_rows,
    threshold: int,
    use_sentence_boundary: bool = True,
) -> list[tuple[tuple[int, int], str]]:
    """Reconstruct one column's per-question paragraphs across the *whole*
    question table (not band-by-band) - the same whole-document strategy
    _lettered_column_paragraphs uses for alt1-4, except the paragraph
    boundary is a same-page gap larger than `threshold` (see
    _adaptive_leak_threshold) instead of a literal letter marker, for
    columns (descripcion, tema, fundamento) that have no such marker to
    anchor on.

    This exists because an earlier per-band approach (splitting each
    band's own raw capture at an internal gap and carrying the excess
    forward onto the next question - since replaced by this function) had
    a structural blind spot: it could only ever move a band's *trailing*
    excess forward onto the next question, since it only ever looked
    inside one band's own (already digit-based, potentially wrong) raw
    capture at a time. But the same naive digit-based band boundary can
    just as easily cut a row's own trailing content off too *early*,
    leaving it stranded at the *start* of the *next* band's raw capture
    instead - and no amount of "split the excess off the end and carry it
    forward" logic can pull content backward out of a neighbor's capture.
    Confirmed case: CLASE_B_IIA.pdf q48/q49 - q48's own last line ("de
    paso?") lands 2px past band48's naive end boundary, inside band49's
    raw range instead; the per-band approach had no way to reclaim it for
    q48, and (once its threshold was made sensitive enough to notice the
    14px gap right after that stranded line) actively made it worse by
    treating the stranded fragment as q49's own real content and leaking
    q49's *genuine* title text forward onto q50 instead. Reconstructing
    paragraphs globally by gap continuity alone - entirely ignoring where
    the digit-based band boundaries happen to fall until *after*
    paragraphs are correctly formed - sidesteps this: "de paso?" is only
    12px below q48's own last line (well under this PDF's threshold), so
    it's correctly kept in the same paragraph as the rest of q48's title,
    regardless of which band's raw range it physically landed in.

    Returns each paragraph as `((page, top), text)` (its own first line's
    position, and the full joined text), in document order; the caller
    maps each one to a question number via _map_by_position. Like
    _lettered_column_paragraphs, this doesn't assume a strict 1:1
    correspondence with the question count - a paragraph can merge more
    than one question's content if a genuine boundary gap happens to be
    no bigger than this document's normal same-paragraph wrap spacing
    (the same ambiguity _OPTION_LETTERS's docstring notes gap-only
    detection can't always resolve for alt1-4 either) - the caller falls
    back to plain per-band `text_in_band` for any question number this
    leaves uncovered."""
    lo, hi = col_range
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
    prev_pos: tuple[int, int] | None = None
    for page, top, text in lines:
        if prev_pos is not None and (page, top) != prev_pos:
            new_page = page != prev_pos[0]
            # A page change always starts a new paragraph, never just a
            # same-question continuation: build_row_bands caps every
            # band's end at the bottom of its own *starting* page (never
            # extending into the next), and per its own docstring this was
            # verified true across all 9 real balotario PDFs - a single
            # question's content never actually spans a page break, so
            # there's no legitimate same-paragraph case to preserve here.
            # Not splitting unconditionally on a page change was a real
            # bug: it merged the tail of one page's last question with the
            # head of the next page's first one on every page boundary in
            # the document (~every 8-9 questions in CLASE_A_I.pdf) until
            # this was caught by the a1 ground-truth comparison.
            big_gap = (not new_page) and (top - prev_pos[1] > threshold)
            # A same-page gap can be genuinely indistinguishable from a
            # normal line wrap by size alone, no matter the threshold - a
            # confirmed real cluster (CLASE_B_IIB.pdf/CLASE_B_IIC.pdf
            # q169-q172) has *every* gap, wrap or question boundary alike,
            # sitting at this document's own normal 11-12px spacing, with
            # nothing size-wise to tell them apart. But every one of that
            # cluster's real question-title endings ends with terminal
            # punctuation ("reglamento.", "licencia vencida.", "...como
            # las siguientes:"), while every genuine same-title wrapped
            # line (checked against every band in both CLASE_A_I.pdf and
            # CLASE_B_IIA.pdf's descripcion/tema columns) does not - a
            # title only ends with ".", ":" or "?" once it's actually
            # finished, wrap or no wrap ("?" added after CLASE_B_IIB.pdf
            # q79/q80 confirmed a same-pattern leak across a "...seguridad
            # pública?" / "Es el acto..." boundary - many titles here are
            # phrased as questions, so excluding "?" left an entire class
            # of genuine boundaries uncaught). Treating this as an
            # independent split trigger - not contingent on gap size at
            # all - catches this class of leak a gap threshold
            # structurally never can.
            #
            # `use_sentence_boundary` exists because this signal is only
            # safe for descripcion/tema, where a title reads as one
            # coherent stem ending in exactly one terminal mark.
            # `fundamento` (free-form citation text) legitimately contains
            # several complete sentences *within* a single question's own
            # entry (e.g. "...actualizado mediante Resolución Directoral
            # N° 16-2016-MTC/14 de fecha 31 de mayo del 2016. Disponible
            # en: http://...") - applying this trigger there splits a
            # single citation into fragments at each internal period/colon
            # (confirmed regression against b2a_questions.json:
            # fundamento mismatches went 161 -> 181 when this was applied
            # unconditionally). Plain gap-based detection alone is already
            # sufficient for fundamento: every genuine fundamento boundary
            # checked has a comfortably large gap (e.g. 26px against an
            # 11-12px normal), unlike descripcion/tema's occasional
            # razor-thin cases.
            ends_sentence = (
                use_sentence_boundary
                and not new_page
                and current
                and bool(re.search(r"[.:?]\s*$", current[-1][2]))
            )
            if new_page or big_gap or ends_sentence:
                if current:
                    paragraphs.append(current)
                current = []
        current.append((page, top, text))
        prev_pos = (page, top)
    if current:
        paragraphs.append(current)
    return [
        ((p[0][0], p[0][1]), " ".join(text for _p, _t, text in p).strip())
        for p in paragraphs
    ]


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
    # Sort by (page, top) only, NOT the full tuple (which would fall back
    # to comparing `text` alphabetically whenever two fragments share the
    # same top - scrambling word order). Some balotario PDFs (mainly
    # B-license ones, per pdf_layout.py's _cluster_lefts docstring) emit
    # one fragment per *word* rather than per line, so many same-line
    # fragments genuinely share the same top and rely on Python's stable
    # sort to preserve `texts`' own left-to-right document order (parse_xml
    # sorts by (page, top, left)) for those ties - exactly how
    # `text_in_band` already does it.
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
    _lettered_column_paragraphs / _gap_based_column_paragraphs) to a
    question number, using the band whose own start it falls at-or-after,
    most recently, as a *starting point* - i.e. the band that "owns" the
    vertical territory the item's first line sits in. `items` must be in
    ascending document order (both callers already produce them that way)
    and `bands` must be in ascending document order (as `build_row_bands`
    returns them). Unlike a plain ordinal zip, this tolerates an item
    going missing for one question (that qnum is simply absent from the
    returned dict - the caller falls back to a different extraction
    method for it) without throwing every later item's assignment off by
    one.

    Two different items can independently compute the *same* "most
    recent" band start as their natural owner: this happens whenever that
    band's own successor's start is itself miscalculated too late (the
    same digit-based band-boundary problem _gap_based_column_paragraphs
    exists to route around in the first place - confirmed case:
    CLASE_A_I.pdf q27/q28, where band28's naive start (top 685) is later
    than q28's own real title (top 678), so both q27's and q28's
    reconstructed paragraphs independently compute q27 as their natural
    owner). Since both `items` and `bands` are already in the same
    document order, the fix doesn't need the band boundary to be right at
    all: each item's assigned qnum is forced to be strictly greater than
    the previous item's, bumping forward by the minimum amount needed
    (never backward, never skipping further than required) whenever the
    naturally-computed owner would otherwise collide with or precede an
    already-assigned one - so two same-owner items split cleanly across
    two consecutive question numbers instead of the second one silently
    overwriting the first."""
    starts = [((b[1], b[2]), b[0]) for b in bands]
    mapping: dict[int, str] = {}
    last_owner: int | None = None
    for pos, text in items:
        owner = None
        for start_pos, qnum in starts:
            if start_pos <= pos:
                owner = qnum
            else:
                break
        if owner is None:
            continue
        if last_owner is not None and owner <= last_owner:
            owner = last_owner + 1
        mapping[owner] = text
        last_owner = owner
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

    overall_start = (bands[0][1], bands[0][2])
    overall_end = (bands[-1][3], bands[-1][4])

    # Primary extraction for alt1-4: whole-table, letter-prefix-based
    # reconstruction (see _OPTION_LETTERS), mapped to question numbers by
    # position (see _map_by_position) rather than a strict ordinal zip, so
    # one missing option (e.g. CLASE_A_I.pdf q179 - see _OPTION_LETTERS's
    # docstring) only drops that single cell to the plain per-band
    # fallback below instead of discarding the whole column's otherwise-
    # correct reconstruction.
    text_by_qnum: dict[str, dict[int, str]] = {}
    for key, letter in _OPTION_LETTERS.items():
        if key not in columns:
            continue
        items = _lettered_column_paragraphs(
            texts, columns[key], letter, overall_start, overall_end, header_rows
        )
        text_by_qnum[key] = _map_by_position(items, bands)

    # Primary extraction for tema/descripcion: whole-table, gap-based
    # reconstruction (see _gap_based_column_paragraphs), also mapped to
    # question numbers by position - same paradigm as alt1-4 above, minus
    # the letter marker. Each column's own adaptive threshold (see
    # _adaptive_leak_threshold) is computed once, from this document's own
    # gap distribution for that column.
    #
    # `fundamento` deliberately isn't included here, unlike the other
    # three wrapped columns: it's free-form citation text that can
    # legitimately be split, *within one question's own entry*, into
    # several gap-separated pieces (e.g. a numbered list - "7. Parachoques
    # delantero... 8. Parachoques posterior..." - both part of the same
    # citation for one question, confirmed on CLASE_B_IIB.pdf q127).
    # _map_by_position's monotonic-forward-bump (needed to fix the
    # cross-question leak this whole reconstruction exists for - see
    # _gap_based_column_paragraphs) can't tell that apart from a genuine
    # next question's content arriving early: it bumps the second piece of
    # q127's own citation onto q128 instead of merging it back into q127,
    # and since the bump is unconditional, every subsequent fundamento
    # value cascades one question further off from there (confirmed
    # regression: b2a_questions.json fundamento mismatches jumped to 162
    # when this was included, most of them downstream of exactly one such
    # split, not independent errors). tema/descripcion don't share this
    # risk - a title or topic doesn't get authored as an internally
    # itemized list - so they keep the more accurate reconstruction;
    # fundamento falls back to plain per-band `text_in_band` for every
    # row instead (see `extract`'s fallback below), which can't cascade
    # since each row's value only ever depends on its own band.
    for key in ("tema", "descripcion"):
        if key not in columns:
            continue
        threshold = _adaptive_leak_threshold(texts, bands, columns[key])
        items = _gap_based_column_paragraphs(
            texts, columns[key], overall_start, overall_end, header_rows, threshold
        )
        text_by_qnum[key] = _map_by_position(items, bands)

    def extract(key: str, band) -> str:
        qnum = band[0]
        by_qnum = text_by_qnum.get(key, {})
        if qnum in by_qnum:
            return by_qnum[qnum]
        # Fallback: plain per-band extraction, no gap/leak correction at
        # all. Deliberately simple/conservative rather than another
        # heuristic layer - it can include a stray leaked-in fragment (the
        # same failure mode the primary methods above are built to avoid),
        # but unlike a leak-detector running on an unreliable digit-based
        # band it can't silently *drop* real content, which is the worse
        # failure mode of the two (see _gap_based_column_paragraphs's
        # docstring on the CLASE_B_IIA.pdf q48/q49 regression this
        # replaced).
        return text_in_band(texts, band, columns[key])

    questions = []
    for band in bands:
        qnum = band[0]
        title = extract("descripcion", band)
        topic = extract("tema", band)
        options = [extract(k, band) for k in ("alt1", "alt2", "alt3", "alt4")]
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
        if "fundamento" in columns:
            fundamento = extract("fundamento", band)
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
