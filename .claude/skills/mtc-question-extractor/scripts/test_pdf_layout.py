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
    detect_question_rows,
    parse_xml,
    run_pdftohtml,
    text_in_band,
)

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
    bands = build_row_bands(rows, last_page)

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
