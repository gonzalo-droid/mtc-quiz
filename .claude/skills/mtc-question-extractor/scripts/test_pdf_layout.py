"""Regression test: re-parsing CLASE_A_I.pdf must match the existing,
human-verified a1_questions.json for at least the first 5 questions. This
is the only ground truth we have, so it's the test fixture.
"""
import json
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

# Literal header words that must never appear in extracted question text.
# pdftohtml reprints the table header at the top of every page, so a bug
# in how row bands handle page breaks (see Task 1 review) shows up as one
# of these leaking into a real question's title/tema/options/answer -
# this class of bug isn't caught by only checking questions 1-5 (all on
# page 1, before any header reprint), so it's checked across all 200.
_HEADER_WORDS = ("TEMA", "DESCRIPCIÓN DE LA PREGUNTA", "ALTERNATIVA", "RESPUESTA")

# Section-title/instruction lines some balotario PDFs insert between the
# main N-question table and a supplementary section's own header row
# (e.g. "SEGUNDA PARTE - MATERIAS ESPECÍFICAS", right before "INSTRUCCIÓN:
# ELEGIR 20 PREGUNTAS"). These aren't header rows themselves, so the
# header-word check above doesn't catch them leaking into the last
# question's band - see the Task 1 review's second round.
_SECTION_KEYWORDS = ("PRIMERA PARTE", "SEGUNDA PARTE", "TERCERA PARTE", "INSTRUCCIÓN:")

REPO = Path(__file__).resolve().parents[4]
PDF_DIR = REPO / "app/src/main/assets/pdf"
PDF = PDF_DIR / "CLASE_A_I.pdf"
JSON = REPO / "app/src/main/assets/json/a1_questions.json"
WORKDIR = Path("/tmp/mtc_extractor_test_a1")

# 4 of the 9 real PDFs are known to have a "SEGUNDA PARTE - MATERIAS
# ESPECÍFICAS" section title sitting between the last real question and a
# second table's header row - a fresh check of these confirms the fix
# without needing ground-truth JSON for them (only a1_questions.json - for
# CLASE_A_I.pdf - exists).
_SECTION_BREAK_PDFS = (
    "CLASE_A_IIA.pdf",
    "CLASE_A_IIB.pdf",
    "CLASE_A_IIIB.pdf",
    "CLASE_A_IIIC.pdf",
)

# Pinned regression case (Task 1 review, third round): a section-break
# detection heuristic that compared a text block's width against the
# *specific column it happened to land in* falsely fired on
# CLASE_A_IIIC.pdf q200 - that PDF's detect_columns output gives `alt3` an
# overly wide range (900, 1079) vs. the near-identical CLASE_A_IIIB.pdf's
# correctly-narrower (900, 1030) - and truncated real content. Crucially,
# the truncation happened to also delete the banned "SEGUNDA PARTE"
# substring, so the leakage check above passed while content silently
# vanished - a substring-absence check alone can't distinguish "correctly
# truncated" from "incorrectly truncated". These exact strings (captured
# once the fix was verified correct) pin the full, un-truncated content so
# this can't silently regress again.
_A_IIIC_Q200_EXPECTED = {
    "tema": "Reglamento de Tránsito y Manual de Dispositivos de Control de Tránsito",
    "alt1": "a) La zona que permite adelantar inicia con las líneas amarillas continuas.",
    "alt2": "b) Los conductores pueden estacionarse al empezar las líneas continuas.",
}


def _leakage_check(pdf_path: Path, workdir: Path):
    """Parse `pdf_path` end to end and assert none of the header words or
    section-title keywords above leak into any extracted question's
    tema/descripcion/alternativas/respuesta - across the whole document,
    not just the first few questions on page 1."""
    xml_path = run_pdftohtml(pdf_path, workdir)
    texts, images = parse_xml(xml_path)
    columns = detect_columns(texts)
    for required in ("numero", "tema", "descripcion", "alt1", "respuesta"):
        assert required in columns, f"{pdf_path.name}: missing column {required}: {columns}"

    rows = detect_question_rows(texts, columns)
    assert rows, f"{pdf_path.name}: no questions detected"

    last_page = max(t.page for t in texts)
    header_rows = detect_header_rows(texts)
    bands = build_row_bands(rows, last_page, header_rows, texts, columns)

    col_keys = [k for k in ("tema", "descripcion", "alt1", "alt2", "alt3", "alt4", "respuesta") if k in columns]
    for band in bands:
        qnum = band[0]
        for key in col_keys:
            text = text_in_band(texts, band, columns[key])
            for word in _HEADER_WORDS + _SECTION_KEYWORDS:
                assert word not in text, (
                    f"{pdf_path.name} q{qnum} column {key!r} leaked {word!r}: {text!r}"
                )
    return bands, texts, columns


def _assert_last_question_nonempty(pdf_path: Path, workdir: Path, keys: tuple[str, ...]) -> None:
    """A substring-absence check alone can be fooled by a band boundary
    that's cut too early: the truncated text can happen to no longer
    contain the banned word either, so the leakage check above passes
    while real content silently vanishes (see the CLASE_A_IIIC.pdf q200
    case pinned below - the truncation there happened to also delete the
    banned "SEGUNDA PARTE" substring). The very last question's band is
    the one most exposed to this: every "cap at the next boundary"
    heuristic in build_row_bands has to get it right with no following
    question's band to sanity-check it against.

    Only checks `keys` (not necessarily every detected column) rather than
    sweeping all 9 PDFs unconditionally: while writing this check, it also
    surfaced CLASE_A_IIIA.pdf's `tema` column coming back empty for its
    last question and other rows - a genuine but separate pre-existing bug
    (that PDF's `numero` column range comes out as (52, 287), engulfing
    `tema`'s real territory instead of the normal ~(52, 210)), not caused
    or fixed by this round's change and out of scope for it (flagged
    separately in the report instead). Scoping this check to the columns
    and PDFs this round's fix actually touches keeps it from failing on
    that unrelated, pre-existing issue.
    """
    xml_path = run_pdftohtml(pdf_path, workdir)
    texts, _ = parse_xml(xml_path)
    columns = detect_columns(texts)
    rows = detect_question_rows(texts, columns)
    last_page = max(t.page for t in texts)
    header_rows = detect_header_rows(texts)
    bands = build_row_bands(rows, last_page, header_rows, texts, columns)
    last_band = bands[-1]
    for key in keys:
        text = text_in_band(texts, last_band, columns[key])
        assert text, f"{pdf_path.name} q{last_band[0]} column {key!r} is empty: band={last_band}"


def main() -> None:
    # Full-document leakage check (all 200 questions, not just the first
    # 5): a page-crossing question's band overlapping the reprinted header
    # on its new page - or stealing/duplicating the next question's or a
    # trailing section's content there - shows up as one of the words
    # above appearing somewhere it shouldn't. 20 of these 200 questions
    # cross a page boundary in CLASE_A_I.pdf, so this exercises that path
    # thoroughly.
    bands, texts, columns = _leakage_check(PDF, WORKDIR)

    # Same leakage check across every other balotario PDF - not required
    # by Task 1's original acceptance bar (only CLASE_A_I.pdf has ground
    # truth), but this class of bug (page-crossing / trailing-section
    # content leaking into the last question) was only found by checking
    # beyond just the one file with ground truth, so it stays covered here
    # going forward.
    for pdf_path in sorted(PDF_DIR.glob("*.pdf")):
        if pdf_path == PDF:
            continue
        _leakage_check(pdf_path, Path(f"/tmp/mtc_extractor_test_{pdf_path.stem}"))

    # Fresh, explicit check of the exact bug the second review round
    # found: q200's title on each of the 4 affected PDFs must be just the
    # real question text, with no trailing "SEGUNDA PARTE..." appended -
    # and (third review round) not truncated short of the real content
    # either, which a section-break heuristic that's too eager can do just
    # as easily as one that's not eager enough.
    for name in _SECTION_BREAK_PDFS:
        pdf_path = PDF_DIR / name
        xml_path = run_pdftohtml(pdf_path, Path(f"/tmp/mtc_extractor_test_{pdf_path.stem}"))
        p_texts, _ = parse_xml(xml_path)
        p_columns = detect_columns(p_texts)
        p_rows = detect_question_rows(p_texts, p_columns)
        p_last_page = max(t.page for t in p_texts)
        p_header_rows = detect_header_rows(p_texts)
        p_bands = build_row_bands(p_rows, p_last_page, p_header_rows, p_texts, p_columns)
        q200_band = next(b for b in p_bands if b[0] == 200)
        q200_title = text_in_band(p_texts, q200_band, p_columns["descripcion"])
        assert "SEGUNDA PARTE" not in q200_title, (
            f"{name}: q200 title still leaks section title: {q200_title!r}"
        )
        # tema/alt1/alt2 aren't affected by the pre-existing, unrelated
        # alt3/alt4 range-estimation issue on CLASE_A_IIIC.pdf (see
        # _A_IIIC_Q200_EXPECTED's docstring above), so require them
        # non-empty on every affected PDF, not just A_IIIC.
        for key in ("tema", "alt1", "alt2"):
            text = text_in_band(p_texts, q200_band, p_columns[key])
            assert text, f"{name}: q200 column {key!r} is empty: band={q200_band}"
        if name == "CLASE_A_IIIC.pdf":
            for key, expected_text in _A_IIIC_Q200_EXPECTED.items():
                got = text_in_band(p_texts, q200_band, p_columns[key])
                assert got == expected_text, (
                    f"CLASE_A_IIIC.pdf q200 column {key!r} mismatch:\n"
                    f"  got:      {got!r}\n  expected: {expected_text!r}"
                )

    # CLASE_A_I.pdf's last question doesn't cross a section break at all
    # (its band runs to the document end, see the round-1 report), so this
    # exercises the plain fallback path for the same class of check.
    _assert_last_question_nonempty(PDF, WORKDIR, ("tema", "descripcion", "alt1", "alt2", "respuesta"))

    expected = json.loads(JSON.read_text())["data"]
    for band in bands[:5]:
        qnum = band[0]
        exp = next(q for q in expected if q["id"] == qnum)
        # .strip() on the expected side only: text_in_band always returns an
        # already-stripped string, but a1_questions.json has a stray
        # trailing "\n" on at least one title (a pre-existing artifact of
        # whatever pipeline produced that ground-truth file, not meaningful
        # content), so a bare `==` would fail on whitespace alone.
        title = text_in_band(texts, band, columns["descripcion"])
        answer = text_in_band(texts, band, columns["respuesta"])
        exp_title = exp["title"].strip()
        exp_answer = exp["answer"].strip()
        assert title == exp_title, f"q{qnum} title mismatch:\n  got:      {title!r}\n  expected: {exp_title!r}"
        assert answer == exp_answer, f"q{qnum} answer mismatch: {answer!r} != {exp_answer!r}"
        for i, key in enumerate(("alt1", "alt2", "alt3", "alt4")):
            opt = text_in_band(texts, band, columns[key])
            exp_opt = exp["options"][i].strip()
            assert opt == exp_opt, f"q{qnum} option {key} mismatch:\n  got:      {opt!r}\n  expected: {exp_opt!r}"

    print("OK - pdf_layout matches a1_questions.json for questions 1-5")


if __name__ == "__main__":
    main()
