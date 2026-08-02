# MTC Question & Image Extraction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a reusable skill that extracts MTC exam questions and their
associated images from the balotario PDFs into the app's JSON/asset format,
run it to complete the two fully-pending exams (b2b, b2c) and associate
images across all 9 exams, then wire the app to actually use that data.

**Architecture:** A Python-based extraction skill (`.claude/skills/mtc-question-extractor/`)
parses `pdftohtml -xml` output (text + embedded images, all with pixel
bounding boxes in one coordinate space) to reconstruct table rows and
associate images to the question row they overlap. Kotlin changes are
additive: one domain field rename, one new JVM test, and wiring an existing-
but-unused Coil dependency into the shared `CardQuestion` composable.

**Tech Stack:** Python 3 stdlib (`xml.etree`, `subprocess`), poppler-utils
(`pdftohtml`, `pdfinfo` — already installed at `/opt/homebrew/bin`), `cwebp`
(already installed) for WebP conversion, Kotlin/Compose/Coil3/kotlinx.serialization
(already in the project).

## Global Constraints

- JSON schema for questions lives in `core/domain/.../Question.kt`; it is
  decoded with `Json.Default` (no `ignoreUnknownKeys`), so every field added
  to the JSON must exist on `Question` or the app crashes loading that file.
- `category` in each question JSON is the **fixed exam-level code** (see
  mapping table below), not the PDF's per-row "Clase/Categoría" text (which
  is often "Todas").
- Image asset naming: `q{questionId}_{letter}_{examId}.webp`, letter
  increments `a, b, c...` per image within the same question, no leading
  zeros, extension always `.webp`, stored in `app/src/main/assets/images/`.
- `imagens` field: `List<String>` of asset names **without extension**,
  e.g. `["q1_a_a1", "q1_b_a1"]`. Empty/absent when a question has no image.
- PDF ↔ JSON ↔ examId ↔ category mapping (fixed, do not re-derive):

  | PDF | JSON | examId | category |
  |---|---|---|---|
  | CLASE_A_I | a1_questions.json | a1 | AI |
  | CLASE_A_IIA | a2a_questions.json | a2a | AIIA |
  | CLASE_A_IIB | a2b_questions.json | a2b | AIIB |
  | CLASE_A_IIIA | a3a_questions.json | a3a | AIIIA |
  | CLASE_A_IIIB | a3b_questions.json | a3b | AIIIB |
  | CLASE_A_IIIC | a3c_questions.json | a3c | AIIIC |
  | CLASE_B_IIA | b2a_questions.json | b2a | BIIA |
  | CLASE_B_IIB | b2b_questions.json | b2b | BIIB |
  | CLASE_B_IIC | b2c_questions.json | b2c | BIIC |

- `app/src/main/assets/json/a1_questions_test.json` is dead weight: the
  only test that references its filename (`QuizRepositoryTest.kt`) mocks
  `AssetManager` with an inline JSON string and never reads the real file
  from disk. Delete it, don't move it — nothing depends on its contents.

---

### Task 1: Shared PDF layout parser (`pdf_layout.py`)

**Files:**
- Create: `.claude/skills/mtc-question-extractor/scripts/pdf_layout.py`
- Create: `.claude/skills/mtc-question-extractor/scripts/test_pdf_layout.py`

**Interfaces:**
- Produces (used by Task 2 and Task 3):
  - `run_pdftohtml(pdf_path: Path, workdir: Path) -> Path` — runs
    `pdftohtml -xml -q`, returns path to the generated `.xml` file (image
    files land alongside it in `workdir`).
  - `class TextEl(NamedTuple): page: int; top: int; left: int; width: int; height: int; font: str; text: str`
  - `class ImageEl(NamedTuple): page: int; top: int; left: int; width: int; height: int; src: Path`
  - `parse_xml(xml_path: Path) -> tuple[list[TextEl], list[ImageEl]]`
  - `detect_columns(texts: list[TextEl]) -> dict[str, tuple[int, int]]` —
    maps column name (`"numero"`, `"tema"`, `"descripcion"`, `"alt1"`,
    `"alt2"`, `"alt3"`, `"alt4"`, `"respuesta"`, `"fundamento"`) to
    `(left_start, left_end)` in PDF pixel space. `"fundamento"` is only
    present in the returned dict when the PDF has that column (B-license
    exams).
  - `detect_question_rows(texts: list[TextEl], columns: dict) -> list[tuple[int, int, int]]` —
    list of `(question_number, page, top)` sorted by document order, one
    per detected question start.
  - `build_row_bands(rows: list[tuple[int,int,int]], last_page: int) -> list[tuple[int, int, int, int, int]]` —
    for each row, `(question_number, start_page, start_top, end_page, end_top)`
    marking the vertical span (across a possible page break) that belongs
    to that question, ending where the next question's row starts (or the
    document end for the last one).

- [ ] **Step 1: Write `pdf_layout.py` with the functions above**

```python
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


def detect_columns(texts: list[TextEl]) -> dict[str, tuple[int, int]]:
    page1 = [t for t in texts if t.page == 1]
    # Header row texts are bold, small-ish and appear before the first
    # question row; find them by matching known labels directly rather than
    # relying on a fixed y-range, since page geometry differs per PDF.
    found: dict[str, int] = {}
    for t in page1:
        norm = _norm(t.text)
        for key, labels in _HEADER_LABELS.items():
            if key in found:
                continue
            for label in labels:
                if key == "numero":
                    if norm == "N" or norm == "NO" or norm.startswith("N "):
                        found[key] = min(found.get(key, t.left), t.left)
                elif label in norm:
                    found[key] = min(found.get(key, t.left), t.left)
    ordered = sorted(found.items(), key=lambda kv: kv[1])
    columns: dict[str, tuple[int, int]] = {}
    for i, (key, left) in enumerate(ordered):
        right = ordered[i + 1][1] if i + 1 < len(ordered) else 10**6
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
    cleaned: list[tuple[int, int, int]] = []
    expected = 1
    for qnum, page, top in rows:
        if qnum == expected:
            cleaned.append((qnum, page, top))
            expected += 1
    return cleaned


def build_row_bands(
    rows: list[tuple[int, int, int]], last_page: int
) -> list[tuple[int, int, int, int]]:
    bands = []
    for i, (qnum, page, top) in enumerate(rows):
        if i + 1 < len(rows):
            _, end_page, end_top = rows[i + 1]
        else:
            end_page, end_top = last_page, 10**6
        bands.append((qnum, page, top, end_page, end_top))
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
```

- [ ] **Step 2: Write a regression test against the already-completed `a1` exam**

```python
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
        title = text_in_band(texts, band, columns["descripcion"])
        answer = text_in_band(texts, band, columns["respuesta"])
        assert title == exp["title"], f"q{qnum} title mismatch:\n  got:      {title!r}\n  expected: {exp['title']!r}"
        assert answer == exp["answer"], f"q{qnum} answer mismatch: {answer!r} != {exp['answer']!r}"
        for i, key in enumerate(("alt1", "alt2", "alt3", "alt4")):
            opt = text_in_band(texts, band, columns[key])
            assert opt == exp["options"][i], f"q{qnum} option {key} mismatch:\n  got:      {opt!r}\n  expected: {exp['options'][i]!r}"

    print("OK - pdf_layout matches a1_questions.json for questions 1-5")


if __name__ == "__main__":
    main()
```

- [ ] **Step 3: Run it and iterate until it passes**

Run: `python3 .claude/skills/mtc-question-extractor/scripts/test_pdf_layout.py`
Expected: `OK - pdf_layout matches a1_questions.json for questions 1-5`

If it fails, the mismatch is almost always one of: column boundary off by a
few px (widen the tolerance in `text_in_band`'s `lo - 5`), or a stray digit
elsewhere in the Nº column x-range breaking `detect_question_rows`'s
strictly-increasing check. Print `columns` and the failing band's raw
`texts` to debug — do not hardcode pixel values as a fix, the same script
runs against 9 different page geometries (A-license PDFs are ~1262px wide
with 2-line headers, B-license PDFs are ~1263px wide with 1-line headers
and an extra `FUNDAMENTO` column).

- [ ] **Step 4: Commit**

```bash
git add .claude/skills/mtc-question-extractor/scripts/pdf_layout.py .claude/skills/mtc-question-extractor/scripts/test_pdf_layout.py
git commit -m "feat: add pdf_layout parser for MTC balotario extraction"
```

---

### Task 2: Question text extraction script (`parse_questions.py`)

**Files:**
- Create: `.claude/skills/mtc-question-extractor/scripts/parse_questions.py`

**Interfaces:**
- Consumes: everything from Task 1 (`pdf_layout.py`).
- Produces: a CLI script; writing behavior documented below, no other task
  imports this one.

- [ ] **Step 1: Write the script**

```python
"""Extract questions from a balotario PDF into the app's JSON schema.

Usage:
  python3 parse_questions.py <examId>

Writes/overwrites app/src/main/assets/json/<examId>_questions.json. Only
touches question text fields (id, section, category, topic, title, answer,
options, argument) - never the `imagens` field, that's extract_images.py's
job and running this script again must not clobber image associations.
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


def extract_topic(texts, band, columns) -> str:
    lo = columns["tema"][0]
    hi = columns["descripcion"][0]
    return text_in_band(texts, band, (lo, hi))


def main() -> None:
    exam_id = sys.argv[1]
    pdf_stem, category = EXAM_TO_PDF[exam_id]
    pdf_path = PDF_DIR / f"{pdf_stem}.pdf"
    json_path = JSON_DIR / f"{exam_id}_questions.json"
    workdir = Path(f"/tmp/mtc_extractor_{exam_id}")

    xml_path = run_pdftohtml(pdf_path, workdir)
    texts, images = parse_xml(xml_path)
    columns = detect_columns(texts)
    rows = detect_question_rows(texts, columns)
    last_page = max(t.page for t in texts)
    bands = build_row_bands(rows, last_page)

    questions = []
    for band in bands:
        qnum = band[0]
        title = text_in_band(texts, band, columns["descripcion"])
        options = [text_in_band(texts, band, columns[k]) for k in ("alt1", "alt2", "alt3", "alt4")]
        answer_raw = text_in_band(texts, band, columns["respuesta"]).strip().lower()
        answer = re.sub(r"[^a-d]", "", answer_raw)[:1] or "a"
        entry = {
            "id": qnum,
            "section": detect_section_for_band(texts, band, columns),
            "category": category,
            "topic": extract_topic(texts, band, columns),
            "title": title,
            "answer": answer,
            "options": options,
        }
        if "fundamento" in columns:
            fundamento = text_in_band(texts, band, columns["fundamento"])
            if fundamento:
                entry["argument"] = fundamento
        questions.append(entry)

    json_path.write_text(
        json.dumps({"data": questions}, ensure_ascii=False, indent=4), encoding="utf-8"
    )
    print(f"Wrote {len(questions)} questions to {json_path}")


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: Dry-run against `a1` (already has ground truth) without overwriting it**

Run:
```bash
cd /tmp && cp -r "$(python3 -c "print('$OLDPWD')")" /dev/null 2>/dev/null; \
python3 - <<'PY'
import sys, json
sys.path.insert(0, ".claude/skills/mtc-question-extractor/scripts")
import parse_questions as pq
pq.JSON_DIR = __import__("pathlib").Path("/tmp/mtc_dry_run")
pq.JSON_DIR.mkdir(exist_ok=True)
sys.argv = ["parse_questions.py", "a1"]
pq.main()
generated = json.load(open("/tmp/mtc_dry_run/a1_questions.json"))["data"]
existing = json.load(open("app/src/main/assets/json/a1_questions.json"))["data"]
mismatches = [g["id"] for g, e in zip(generated, existing) if g["title"] != e["title"] or g["answer"] != e["answer"]]
print(f"{len(generated)} generated, {len(existing)} existing, {len(mismatches)} title/answer mismatches")
print("first mismatches:", mismatches[:10])
PY
```
Expected: `200 generated, 200 existing, 0 title/answer mismatches` (a handful
of mismatches from whitespace-only differences are acceptable — inspect
them manually; anything else means the column/row detection needs fixing
before trusting it on b2b/b2c, which have no ground truth to compare
against).

- [ ] **Step 3: Run for real on the two pending exams**

Run:
```bash
python3 .claude/skills/mtc-question-extractor/scripts/parse_questions.py b2b
python3 .claude/skills/mtc-question-extractor/scripts/parse_questions.py b2c
```
Expected: `Wrote <N> questions to .../b2b_questions.json` and same for b2c,
with N matching `pdfinfo`'s implied question count (cross-check: last
detected row number should equal the total; the script already asserts
strictly-increasing numbering starting at 1, so a short list means the
PDF's Nº column stopped matching partway through — inspect that PDF's
`detect_columns` output before accepting the result).

- [ ] **Step 4: Spot-check b2b and b2c against the rendered PDF**

Render a handful of pages to compare visually:
```bash
mkdir -p /tmp/mtc_check && pdftoppm -png -r 100 -f 1 -l 3 app/src/main/assets/pdf/CLASE_B_IIB.pdf /tmp/mtc_check/b2b
```
Read a couple of the generated PNGs and compare their question text/options/
answer letter against the corresponding entries in the freshly written
`b2b_questions.json`. Repeat for `CLASE_B_IIC.pdf` / `b2c_questions.json`.
Fix `parse_questions.py` and re-run if anything is off — do not hand-edit
the generated JSON, the script must be the source of truth so it stays
reusable.

- [ ] **Step 5: Commit**

```bash
git add .claude/skills/mtc-question-extractor/scripts/parse_questions.py app/src/main/assets/json/b2b_questions.json app/src/main/assets/json/b2c_questions.json
git commit -m "feat: extract b2b and b2c questions from their balotario PDFs"
```

---

### Task 3: Image extraction & association script (`extract_images.py`)

**Files:**
- Create: `.claude/skills/mtc-question-extractor/scripts/extract_images.py`

**Interfaces:**
- Consumes: `pdf_layout.py` from Task 1, `EXAM_TO_PDF` mapping (duplicated
  here rather than imported, to keep this script runnable standalone — same
  literal values as in Task 2).
- Produces: files in `app/src/main/assets/images/`, updates `imagens` field
  in `app/src/main/assets/json/<examId>_questions.json` for every exam.

- [ ] **Step 1: Write the script**

```python
"""Extract embedded images from a balotario PDF, associate each one to the
question row it visually belongs to, convert to WebP, and record the
association in that question's `imagens` field.

Usage:
  python3 extract_images.py <examId>

Safe to re-run: recomputes `imagens` for every question in the exam from
scratch each time (clears then rebuilds), so it never accumulates stale
entries if images move between runs.
"""
from __future__ import annotations

import json
import subprocess
import sys
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from pdf_layout import (  # noqa: E402
    ImageEl,
    build_row_bands,
    detect_columns,
    detect_question_rows,
    parse_xml,
    run_pdftohtml,
)

REPO = Path(__file__).resolve().parents[4]
PDF_DIR = REPO / "app/src/main/assets/pdf"
JSON_DIR = REPO / "app/src/main/assets/json"
IMAGES_DIR = REPO / "app/src/main/assets/images"

EXAM_TO_PDF = {
    "a1": "CLASE_A_I",
    "a2a": "CLASE_A_IIA",
    "a2b": "CLASE_A_IIB",
    "a3a": "CLASE_A_IIIA",
    "a3b": "CLASE_A_IIIB",
    "a3c": "CLASE_A_IIIC",
    "b2a": "CLASE_B_IIA",
    "b2b": "CLASE_B_IIB",
    "b2c": "CLASE_B_IIC",
}


def filter_logo_images(images: list[ImageEl], page_count: int) -> list[ImageEl]:
    """Drop images that repeat at the same size/position across many pages
    (headers/logos), keeping only images that appear on few pages - actual
    question illustrations are unique per page."""
    key = lambda im: (im.width, im.height, round(im.left / 5), round(im.top / 5))
    counts = Counter(key(im) for im in images)
    threshold = max(3, page_count // 4)
    return [im for im in images if counts[key(im)] < threshold]


def band_for_image(bands, image: ImageEl):
    for band in bands:
        qnum, sp, st, ep, et = band
        if (image.page, image.top) >= (sp, st) and (image.page, image.top) < (ep, et):
            return qnum
    return None


def convert_to_webp(src: Path, dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(["cwebp", "-quiet", str(src), "-o", str(dest)], check=True)


def main() -> None:
    exam_id = sys.argv[1]
    pdf_path = PDF_DIR / f"{EXAM_TO_PDF[exam_id]}.pdf"
    json_path = JSON_DIR / f"{exam_id}_questions.json"
    workdir = Path(f"/tmp/mtc_extractor_images_{exam_id}")

    xml_path = run_pdftohtml(pdf_path, workdir)
    texts, images = parse_xml(xml_path)
    columns = detect_columns(texts)
    rows = detect_question_rows(texts, columns)
    last_page = max(t.page for t in texts)
    bands = build_row_bands(rows, last_page)
    page_count = last_page

    content_images = filter_logo_images(images, page_count)

    per_question: dict[int, list[ImageEl]] = {}
    for im in content_images:
        qnum = band_for_image(bands, im)
        if qnum is None:
            continue
        per_question.setdefault(qnum, []).append(im)

    data = json.loads(json_path.read_text())
    by_id = {q["id"]: q for q in data["data"]}

    letters = "abcdefghij"
    for qnum, qimages in per_question.items():
        if qnum not in by_id:
            continue
        names = []
        for i, im in enumerate(sorted(qimages, key=lambda x: (x.page, x.top, x.left))):
            name = f"q{qnum}_{letters[i]}_{exam_id}"
            convert_to_webp(im.src, IMAGES_DIR / f"{name}.webp")
            names.append(name)
        by_id[qnum]["imagens"] = names

    json_path.write_text(
        json.dumps({"data": list(by_id.values())}, ensure_ascii=False, indent=4),
        encoding="utf-8",
    )
    print(f"{exam_id}: associated images for {len(per_question)} questions")


if __name__ == "__main__":
    main()
```

- [ ] **Step 2: Run against `a1` first and manually verify a sample**

Run: `python3 .claude/skills/mtc-question-extractor/scripts/extract_images.py a1`

Then read 3-4 of the newly created `app/src/main/assets/images/q*_a1.webp`
files (Read tool supports images) side by side with the corresponding
question's `title`/`options` in `a1_questions.json` to confirm the image
actually matches that question (e.g. a "prohibido voltear" sign question
should get a matching prohibition-sign image, not an unrelated one from a
neighboring row). If associations look wrong, check `filter_logo_images`'s
`threshold` first (a real content image wrongly on every page would be
mis-classified as a logo) before touching band-matching logic.

- [ ] **Step 3: Run for the remaining 8 exams**

Run:
```bash
for exam in a2a a2b a3a a3b a3c b2a b2b b2c; do
  python3 .claude/skills/mtc-question-extractor/scripts/extract_images.py "$exam"
done
```
Expected: one summary line per exam, no tracebacks. Spot-check 2-3 images
per exam the same way as Step 2, prioritizing exams with the most images.

- [ ] **Step 4: Commit**

```bash
git add .claude/skills/mtc-question-extractor/scripts/extract_images.py app/src/main/assets/images app/src/main/assets/json/*.json
git commit -m "feat: extract and associate question images for all 9 exams"
```

---

### Task 4: SKILL.md

**Files:**
- Create: `.claude/skills/mtc-question-extractor/SKILL.md`

- [ ] **Step 1: Write the skill file**

Follow `superpowers:writing-skills` conventions for frontmatter and
structure. Content must cover: the PDF↔JSON↔examId↔category mapping table,
how to check what's pending (compare each `<examId>_questions.json`'s
question count / max id against `pdfinfo`-derived page count and whether
`imagens` is populated — no separate state file), how to run
`parse_questions.py` and `extract_images.py`, the naming convention for
`imagens`/asset files, and the mandatory manual spot-check step (visual
comparison against rendered PDF pages) before trusting output for an exam
with no prior ground truth.

- [ ] **Step 2: Commit**

```bash
git add .claude/skills/mtc-question-extractor/SKILL.md
git commit -m "docs: add SKILL.md for mtc-question-extractor"
```

---

### Task 5: `Question.kt` — replace dead `image` field with `imagens`

**Files:**
- Modify: `core/domain/src/main/java/com/gondroid/core/domain/model/Question.kt:1-33`
- Test: `app/src/test/java/com/gondroid/mtcquiz/domain/models/QuestionTest.kt`

**Interfaces:**
- Produces: `Question.imagens: List<String>` (default `emptyList()`),
  consumed by Task 7 (`CardQuestion`).

- [ ] **Step 1: Read the existing test file to match its style**

Read `app/src/test/java/com/gondroid/mtcquiz/domain/models/QuestionTest.kt`
before editing so the new test matches existing conventions (Truth
assertions, no JUnit `assertEquals`).

- [ ] **Step 2: Write the failing test**

Add to `QuestionTest.kt`:
```kotlin
@Test
fun `decodes imagens array from json`() {
    val json = """
        {
            "id": 1,
            "title": "t",
            "answer": "a",
            "options": ["a", "b", "c", "d"],
            "imagens": ["q1_a_a1", "q1_b_a1"]
        }
    """.trimIndent()
    val question = Json.decodeFromString<Question>(json)
    Truth.assertThat(question.imagens).containsExactly("q1_a_a1", "q1_b_a1").inOrder()
}

@Test
fun `imagens defaults to empty list when absent`() {
    val json = """{"id": 1, "title": "t", "answer": "a", "options": ["a","b","c","d"]}"""
    val question = Json.decodeFromString<Question>(json)
    Truth.assertThat(question.imagens).isEmpty()
}
```
(Add the `kotlinx.serialization.json.Json` and `kotlinx.serialization.decodeFromString`
imports if not already present in the file.)

- [ ] **Step 3: Run the test to confirm it fails**

Run: `./gradlew :app:test --tests "com.gondroid.mtcquiz.domain.models.QuestionTest"`
Expected: FAIL — `Unknown key 'imagens'` (SerializationException) or
unresolved reference `imagens`, since the field doesn't exist yet.

- [ ] **Step 4: Update `Question.kt`**

Replace:
```kotlin
    val options: List<String> = listOf(),
    val image: String? = null,
) {
```
with:
```kotlin
    val options: List<String> = listOf(),
    val imagens: List<String> = emptyList(),
) {
```

- [ ] **Step 5: Run the test to confirm it passes**

Run: `./gradlew :app:test --tests "com.gondroid.mtcquiz.domain.models.QuestionTest"`
Expected: PASS (2 new tests, plus all pre-existing `QuestionTest` cases
still green — none of them reference `image`, confirmed by Explore earlier
in this project).

- [ ] **Step 6: Run the full domain/data test suite to catch any other `image` reference**

Run: `./gradlew :app:test :core:domain:test :core:data:test`
Expected: BUILD SUCCESSFUL. If anything references the old `image` field,
fix that call site now (per Explore's findings during design, nothing else
in the codebase reads it, but re-verify since this is a compile-breaking
change if wrong).

- [ ] **Step 7: Commit**

```bash
git add core/domain/src/main/java/com/gondroid/core/domain/model/Question.kt app/src/test/java/com/gondroid/mtcquiz/domain/models/QuestionTest.kt
git commit -m "refactor: replace unused Question.image with Question.imagens list"
```

---

### Task 6: JSON schema validation test for all `assets/json/*.json`

**Files:**
- Create: `app/src/test/java/com/gondroid/mtcquiz/data/local/QuestionAssetsSchemaTest.kt`

**Interfaces:**
- Consumes: `Question`, `QuestionResponse` from `core:domain` (already
  produced), `Question.imagens` from Task 5.

- [ ] **Step 1: Write the test**

```kotlin
package com.gondroid.mtcquiz.data.local

import com.gondroid.core.domain.model.QuestionResponse
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File

class QuestionAssetsSchemaTest {

    private val jsonDir = File("src/main/assets/json")
    private val validLetters = setOf("a", "b", "c", "d")

    private fun questionFiles(): List<File> =
        jsonDir.listFiles { f -> f.name.endsWith("_questions.json") && f.name != "a1_questions_test.json" }
            ?.sortedBy { it.name }
            ?: error("assets/json directory not found at ${jsonDir.absolutePath}")

    @Test
    fun `every question file parses and has 4 options with a valid answer`() {
        val errors = mutableListOf<String>()
        for (file in questionFiles()) {
            val response = Json.decodeFromString<QuestionResponse>(file.readText())
            for (q in response.data) {
                if (q.options.size != 4) {
                    errors += "${file.name}#${q.id}: expected 4 options, got ${q.options.size}"
                }
                if (q.answer.lowercase() !in validLetters) {
                    errors += "${file.name}#${q.id}: answer '${q.answer}' not in a-d"
                }
                if (q.title.isBlank()) {
                    errors += "${file.name}#${q.id}: blank title"
                }
            }
        }
        assertThat(errors).isEmpty()
    }

    @Test
    fun `question ids within each file are unique and contiguous from 1`() {
        val errors = mutableListOf<String>()
        for (file in questionFiles()) {
            val response = Json.decodeFromString<QuestionResponse>(file.readText())
            val ids = response.data.map { it.id }
            if (ids.toSet().size != ids.size) {
                errors += "${file.name}: duplicate ids found"
            }
            val expected = (1..ids.size).toList()
            if (ids.sorted() != expected) {
                errors += "${file.name}: ids not contiguous 1..${ids.size}, got range ${ids.min()}..${ids.max()}"
            }
        }
        assertThat(errors).isEmpty()
    }

    @Test
    fun `imagens entries follow the q-id_letter_examId naming convention`() {
        val pattern = Regex("""^q\d+_[a-z]_[a-z0-9]+$""")
        val errors = mutableListOf<String>()
        for (file in questionFiles()) {
            val response = Json.decodeFromString<QuestionResponse>(file.readText())
            for (q in response.data) {
                for (name in q.imagens) {
                    if (!pattern.matches(name)) {
                        errors += "${file.name}#${q.id}: imagens entry '$name' doesn't match naming convention"
                    }
                }
            }
        }
        assertThat(errors).isEmpty()
    }
}
```

- [ ] **Step 2: Run it**

Run: `./gradlew :app:test --tests "com.gondroid.mtcquiz.data.local.QuestionAssetsSchemaTest"`
Expected: PASS once Tasks 2 and 3 have run (b2b/b2c populated, `imagens`
present where applicable). If it fails before those tasks are run, that's
expected for `b2b_questions.json`/`b2c_questions.json` being empty at that
point — re-run after Task 3 completes.

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/gondroid/mtcquiz/data/local/QuestionAssetsSchemaTest.kt
git commit -m "test: validate assets/json question schema (options, answer, ids, imagens naming)"
```

---

### Task 7: Wire `imagens` into the UI (`CardQuestion`, evaluation & review screens)

**Files:**
- Modify: `core/presentation/designsystem/build.gradle.kts`
- Modify: `gradle/libs.versions.toml` (no change needed — `coil-compose`
  alias already exists at line 116, reused here)
- Modify: `core/presentation/designsystem/src/main/java/com/gondroid/core/presentation/designsystem/components/CardQuestion.kt`
- Modify: `evaluation/presentation/src/main/java/com/gondroid/evaluation/presentation/EvaluationScreen.kt:226-230`
- Modify: `questionreview/presentation/src/main/java/com/gondroid/questionreview/presentation/QuestionsScreen.kt:213-217`

**Interfaces:**
- Consumes: `Question.imagens: List<String>` from Task 5.
- Produces: `CardQuestion(modifier, title, image: Painter, questionImages: List<String> = emptyList())`.

- [ ] **Step 1: Add Coil to the designsystem module**

In `core/presentation/designsystem/build.gradle.kts`, add inside `dependencies { }`:
```kotlin
    implementation(libs.coil.compose)
```

- [ ] **Step 2: Update `CardQuestion.kt` to render question images from assets**

Replace the file's `Image(...)` block and function signature:
```kotlin
package com.gondroid.core.presentation.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.spacedBy
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.gondroid.core.presentation.designsystem.MTCQuizTheme
import com.gondroid.core.presentation.designsystem.R

@Composable
fun CardQuestion(
    modifier: Modifier,
    title: String,
    image: Painter,
    questionImages: List<String> = emptyList(),
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                modifier = Modifier,
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            if (questionImages.isEmpty()) {
                Image(
                    painter = image,
                    contentDescription = "card_background",
                    modifier = Modifier
                        .height(150.dp)
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center
                )
            } else {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(questionImages) { name ->
                        AsyncImage(
                            model = "file:///android_asset/images/$name.webp",
                            contentDescription = name,
                            modifier = Modifier.height(150.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
)
@Composable
fun PreviewCardQuestion() {
    MTCQuizTheme {
        CardQuestion(
            modifier = Modifier.fillMaxWidth(),
            title = "1.  Respecto de los 100 de control o regulación del tránsito.",
            image = painterResource(id = R.drawable.card_background)
        )
    }
}
```
Note: `LazyRow`'s `items(...)` needs `import androidx.compose.foundation.lazy.items`
and `Arrangement` needs `import androidx.compose.foundation.layout.Arrangement` —
add both alongside the imports above (the block above already lists
`spacedBy` from `Arrangement` but double-check the compiler's exact unused-
import complaints when building, since Compose's `items` extension has a
generic overload that sometimes needs an explicit import).

- [ ] **Step 3: Update `EvaluationScreen.kt` call site**

At `EvaluationScreen.kt:226-230`, replace:
```kotlin
                    CardQuestion(
                        modifier = Modifier.fillMaxWidth(),
                        title = "${state.question.id}.- ${state.question.title}",
                        image = painterResource(id = R.drawable.card_background),
                    )
```
with:
```kotlin
                    CardQuestion(
                        modifier = Modifier.fillMaxWidth(),
                        title = "${state.question.id}.- ${state.question.title}",
                        image = painterResource(id = R.drawable.card_background),
                        questionImages = state.question.imagens,
                    )
```

- [ ] **Step 4: Update `QuestionsScreen.kt` call site**

At `QuestionsScreen.kt:213-217`, replace:
```kotlin
                        CardQuestion(
                            modifier = Modifier.fillMaxWidth(),
                            title = "${question.id}.- ${question.title}",
                            image = painterResource(id = R.drawable.card_background),
                        )
```
with:
```kotlin
                        CardQuestion(
                            modifier = Modifier.fillMaxWidth(),
                            title = "${question.id}.- ${question.title}",
                            image = painterResource(id = R.drawable.card_background),
                            questionImages = question.imagens,
                        )
```

- [ ] **Step 5: Build and fix any compile errors**

Run: `./gradlew :core:presentation:designsystem:compileDebugKotlin :evaluation:presentation:compileDebugKotlin :questionreview:presentation:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Manual verification in the app**

Use the `run` skill (or `./gradlew installDebug` + launch) to open a
category from an exam that now has `imagens` (e.g. `a1`, category AI, after
Task 3 ran) and confirm the question card shows the associated traffic-sign
image instead of the static background for questions that have one, and
still shows the static background for questions that don't.

- [ ] **Step 7: Commit**

```bash
git add core/presentation/designsystem/build.gradle.kts core/presentation/designsystem/src/main/java/com/gondroid/core/presentation/designsystem/components/CardQuestion.kt evaluation/presentation/src/main/java/com/gondroid/evaluation/presentation/EvaluationScreen.kt questionreview/presentation/src/main/java/com/gondroid/questionreview/presentation/QuestionsScreen.kt
git commit -m "feat: render question images in CardQuestion via Coil"
```

---

### Task 8: Remove dead `a1_questions_test.json` asset

**Files:**
- Delete: `app/src/main/assets/json/a1_questions_test.json`

- [ ] **Step 1: Confirm nothing reads the file from disk**

Run: `grep -rn "a1_questions_test" --include="*.kt" app core | grep -v build/`
Expected: only the mock-based reference in `QuizRepositoryTest.kt` (which
supplies its own inline JSON string and never opens the real file — verify
this by re-reading that file's `setUp()` if in doubt).

- [ ] **Step 2: Delete the file**

```bash
git rm app/src/main/assets/json/a1_questions_test.json
```

- [ ] **Step 3: Run the full test suite to confirm nothing broke**

Run: `./gradlew :app:test`
Expected: BUILD SUCCESSFUL — `QuizRepositoryTest` passes because it never
touched the real asset file.

- [ ] **Step 4: Commit**

```bash
git commit -m "chore: remove unused a1_questions_test.json asset fixture"
```

---

## Self-Review Notes

- **Spec coverage:** text extraction (Task 2), image extraction/association
  (Task 3), skill packaging (Task 1, 4), mandatory Kotlin model change
  (Task 5), schema validation recommendation (Task 6), UI wiring
  recommendation (Task 7), dead test-fixture removal recommendation
  (Task 8), WebP recommendation (folded into Task 3's `convert_to_webp`,
  no separate task needed since it's part of the image script from the
  start rather than a later conversion pass).
- **Ordering:** Task 5 (Kotlin field) must land before Task 3's output is
  loaded by the app, but Task 3 (image association) can run before or after
  Task 5 since it only edits JSON files on disk. Tasks are ordered 1→8 as
  the simplest correct sequence; Task 6 depends on Tasks 2+3 having run for
  its assertions to be meaningful, noted inline in that task.
- **Risk concentrated in Task 1/2:** the column/row detection heuristics are
  the one part of this plan without a pre-existing implementation to copy
  from. Both include explicit regression checks against the one exam with
  human-verified ground truth (`a1`) before being trusted on PDFs with no
  ground truth (`b2b`, `b2c`).
