---
name: mtc-question-extractor
description: Use when extracting MTC balotario PDF questions into JSON and webp image assets for the MTCQuiz app
---

# MTC Question Extractor

Extract Peru's MTC traffic-exam questions and associated illustrations from balotario PDF files into structured JSON and webp assets, integrated with the app's Room database schema.

## Quick Reference: PDF ↔ JSON ↔ Exam Mapping

| PDF File | JSON File | examId | Category Code |
|---|---|---|---|
| CLASE_A_I.pdf | a1_questions.json | a1 | AI |
| CLASE_A_IIA.pdf | a2a_questions.json | a2a | AIIA |
| CLASE_A_IIB.pdf | a2b_questions.json | a2b | AIIB |
| CLASE_A_IIIA.pdf | a3a_questions.json | a3a | AIIIA |
| CLASE_A_IIIB.pdf | a3b_questions.json | a3b | AIIIB |
| CLASE_A_IIIC.pdf | a3c_questions.json | a3c | AIIIC |
| CLASE_B_IIA.pdf | b2a_questions.json | b2a | BIIA |
| CLASE_B_IIB.pdf | b2b_questions.json | b2b | BIIB |
| CLASE_B_IIC.pdf | b2c_questions.json | b2c | BIIC |

## How to Run

Extract question text from a balotario PDF:
```bash
python3 .claude/skills/mtc-question-extractor/scripts/parse_questions.py <examId>
```
Writes/updates `app/src/main/assets/json/<examId>_questions.json`. Preserves any non-text fields (like `imagens`) already present for each question.

Extract and associate embedded images, convert to WebP, record in `imagens`:
```bash
python3 .claude/skills/mtc-question-extractor/scripts/extract_images.py <examId>
```
Recomputes `imagens` from scratch each run (clears then rebuilds), so re-running never accumulates stale image references.

## Checking What's Pending

**No separate state file is maintained** — status is always derived fresh from actual file state.

To check if an exam needs text extraction:
1. Run `python3 ... parse_questions.py <examId>`
2. Check the script's `detect_question_rows` output length (the actual row count, not PDF page count — pages ≠ rows)
3. Compare against the JSON's actual question count

To check if an exam needs image extraction:
- Open `app/src/main/assets/json/<examId>_questions.json` and scan whether `imagens` fields are populated
- If most questions lack `imagens`, images have not been extracted yet

## Image Naming & Asset Files

Images live in `app/src/main/assets/images/` as `.webp` files.

**Naming convention:** `q{questionId}_{letter}_{examId}.webp`
- `{letter}` = a/b/c/d (matching option position in question's `options` array)
- Example: `q1_a_a1.webp` = exam a1, question 1, option a

**Critical exception for a3b and a3c:**
These PDFs each contain *two separate numbered tables* that restart numbering. The JSON preserves the PDF's literal (duplicate) `id` values, but filenames use the record's *document position* (1-indexed, continuing across the restart) to avoid collisions.
- Example: a3c's second table, row 28 (id=28 in the PDF, position 228 in the document) → `q228_a_a3c.webp`, not `q28_a_a3c.webp`
- The `imagens` array keeps filenames consistent with actual disk files

**Why this matters:** When you see `q{id}` in a filename and it doesn't match the JSON's `id` field, you're likely looking at a3b or a3c.

## Mandatory Verification Before Trusting New Output

### Text extraction (parse_questions.py)

**For an exam with no prior ground-truth validation:**
1. Dry-run against a1 first (the one exam with clean, pre-existing human-verified ground truth)
2. Compare output questions against expected text: 0 title/answer mismatches (whitespace-normalized) required before trusting the script on any other exam
3. If mismatches occur, check the script's adaptive leak-threshold logic and PDF-specific column calibration

### Image extraction (extract_images.py)

**Visual spot-checks are mandatory:**
1. Read actual rendered `.webp` files using the Read tool (supports images)
2. Cross-reference image content against the specific **option text and answer letter** it should correspond to — not just "does this look like a plausible sign"
3. Spot-check at least 3-5 images per exam before trusting output
4. Verify lettering order (a/b/c/d left-to-right in printed row order) matches visual appearance

## Known Data-Quality Issues (Pre-Existing, Not This Script's Scope)

**Category field inconsistency (a2a, a2b, a3a, a3b, a3c):**
Some questions incorrectly have `"category": "Todas"` instead of the exam's correct fixed code. This predates the extraction skill and the scripts never touch `category`, so the issue persists unchanged. Plan: defer to a separate manual cleanup pass.

**Text/image mismatches (b2a, b2b, b2c questions 13-15):**
Pre-existing hand-curated option text for these questions doesn't accurately describe the actual PDF sign artwork (e.g., b2a q14 option c's text says "animales en la vía" but the PDF shows a cyclist-warning icon). The extracted images are correct; the source-PDF's own text descriptions are what's wrong. Flagged for future content-review pass.

**Fundamento field extraction imperfections (some B-license exams):**
Rare residual issues in `fundamento` (the `argument` Kotlin field) extraction, including occasional cross-question leaks. Not currently displayed in the app, lower priority. Known limitation of the per-PDF row-splitting heuristics.

## Script Dependencies

- `pdf_layout.py` — Shared PDF table parser, used by both text and image extraction
  - `parse_xml()` — XML parsing from pdftohtml output
  - `detect_question_rows()` — Row detection via Nº column
  - `build_row_bands()` — Pixel-band clustering for multi-line cells
  - `detect_columns()` — Column boundary detection
  - Special handling: a3b/a3c duplicate `id` detection (index-based fallback in `extract_images.py`)

See the actual script files for algorithm details:
- Text field extraction: `.claude/skills/mtc-question-extractor/scripts/parse_questions.py`
- Image association & conversion: `.claude/skills/mtc-question-extractor/scripts/extract_images.py`
- Shared PDF layout logic: `.claude/skills/mtc-question-extractor/scripts/pdf_layout.py`

## Implementation Details Worth Knowing

**pdftohtml dependency:** All scripts use `pdftohtml -xml` to extract text and images with pixel coordinates. Must be installed on the system.

**Adaptive thresholds:** Both scripts compute PDF-specific parameters (row-gap thresholds, column boundaries) from the actual document data rather than using fixed magic numbers. This makes them robust to PDF layout variations.

**Safe re-running:** `parse_questions.py` preserves non-text fields on re-run (e.g., existing `imagens`). `extract_images.py` rebuilds `imagens` from scratch, so re-running is safe and idempotent.

**Multi-table PDFs:** a3b/a3c are structurally unique in the corpus (two numbered tables per PDF with restart-at-1). The scripts detect duplicate `id` values and use document-position indexing instead of `id`-keyed lookups for these cases — fully automatic, no manual intervention needed.
