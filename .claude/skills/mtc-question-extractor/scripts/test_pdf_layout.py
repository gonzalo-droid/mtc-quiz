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

REPO = Path(__file__).resolve().parents[4]
PDF = REPO / "app/src/main/assets/pdf/CLASE_A_I.pdf"
JSON = REPO / "app/src/main/assets/json/a1_questions.json"
WORKDIR = Path("/tmp/mtc_extractor_test_a1")


def main() -> None:
    xml_path = run_pdftohtml(PDF, WORKDIR)
    texts, images = parse_xml(xml_path)
    columns = detect_columns(texts)
    for required in ("numero", "tema", "descripcion", "alt1", "respuesta"):
        assert required in columns, f"missing column {required}: {columns}"

    rows = detect_question_rows(texts, columns)
    assert len(rows) == 200, f"expected 200 questions, got {len(rows)}"

    last_page = max(t.page for t in texts)
    header_rows = detect_header_rows(texts)
    bands = build_row_bands(rows, last_page, header_rows)

    # Full-document header-leakage check (all 200 questions, not just the
    # first 5): a page-crossing question's band overlapping the reprinted
    # header on its new page - or stealing/duplicating the next question's
    # content there - shows up as one of _HEADER_WORDS appearing somewhere
    # it shouldn't. 20 of these 200 questions cross a page boundary in
    # CLASE_A_I.pdf, so this exercises that path thoroughly.
    col_keys = [k for k in ("tema", "descripcion", "alt1", "alt2", "alt3", "alt4", "respuesta") if k in columns]
    for band in bands:
        qnum = band[0]
        for key in col_keys:
            text = text_in_band(texts, band, columns[key])
            for word in _HEADER_WORDS:
                assert word not in text, (
                    f"q{qnum} column {key!r} leaked header word {word!r}: {text!r}"
                )

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
