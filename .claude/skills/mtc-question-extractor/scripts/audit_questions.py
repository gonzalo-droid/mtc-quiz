#!/usr/bin/env python3
"""Audit <examId>_questions.json against its source balotario PDF.

Three layers, none of which reuse parse_questions.py or pdf_layout.py — an extractor bug
must not be able to hide itself inside its own audit:

  1. Structural — ids, option shape, answer letter, category, image assets. JSON only.
  2. Text       — every title and option must appear in the PDF. These are justified
                  tables, so pdftotext interleaves a wrapped cell with its neighbours
                  ("a) Ambas son una parte parte de la via de"); exact substring matching
                  reports ~190/204 mismatches that are artifacts of the reader. Matching is
                  therefore an in-order token overlap over a local window anchored on the
                  target's rarest token.
  3. Answers    — the one field no text check can reach, since it lives in its own table
                  column. Both the Nº and RESPUESTA columns are located by clustering the
                  page's own fragments, then read row by row.

Usage:  python3 audit_questions.py [examId ...]        # default: all nine
"""
import json
import re
import subprocess
import sys
import tempfile
import unicodedata
import xml.etree.ElementTree as ET
from collections import Counter
from difflib import SequenceMatcher
from pathlib import Path
from statistics import median

REPO = next(p for p in Path(__file__).resolve().parents
            if (p / "app/src/main/assets/json").is_dir())
JSON_DIR = REPO / "app/src/main/assets/json"
PDF_DIR = REPO / "app/src/main/assets/pdf"
IMG_DIR = REPO / "app/src/main/assets/images"

EXAMS = {
    "a1": ("CLASE_A_I.pdf", "AI"), "a2a": ("CLASE_A_IIA.pdf", "AIIA"),
    "a2b": ("CLASE_A_IIB.pdf", "AIIB"), "a3a": ("CLASE_A_IIIA.pdf", "AIIIA"),
    "a3b": ("CLASE_A_IIIB.pdf", "AIIIB"), "a3c": ("CLASE_A_IIIC.pdf", "AIIIC"),
    "b2a": ("CLASE_B_IIA.pdf", "BIIA"), "b2b": ("CLASE_B_IIB.pdf", "BIIB"),
    "b2c": ("CLASE_B_IIC.pdf", "BIIC"),
}

TEXT_THRESHOLD = 0.90        # in-order token coverage below this gets reported
OPT_PREFIX = re.compile(r"^\s*([a-d])\s*[)\.]\s*", re.I)
NUM = re.compile(r"^\d{1,3}$")
# the answer cell is written "a", "a)", "(a)" or "a)." depending on the page
LET = re.compile(r"^\(?([a-dA-D])[)\.\s]*$")


def toks(s: str) -> list[str]:
    s = unicodedata.normalize("NFKD", s)
    s = "".join(c for c in s if not unicodedata.combining(c))
    return re.sub(r"[^a-z0-9]+", " ", s.lower()).split()


# ---------------------------------------------------------------- layer 2: text
class PdfText:
    def __init__(self, pdf: Path):
        out = subprocess.run(["pdftotext", "-enc", "UTF-8", str(pdf), "-"],
                             capture_output=True, text=True, check=True).stdout
        self.t = toks(out)
        self.pos: dict[str, list[int]] = {}
        for i, w in enumerate(self.t):
            self.pos.setdefault(w, []).append(i)
        self.freq = Counter(self.t)

    def score(self, text: str) -> float:
        """Best in-order coverage of `text` by any local window of the PDF, 0..1."""
        tgt = toks(text)
        if not tgt:
            return 1.0
        present = [w for w in tgt if w in self.pos]
        if not present:
            return 0.0
        anchor = min(present, key=lambda w: self.freq[w])
        span, best = 3 * len(tgt) + 30, 0.0
        for i in self.pos[anchor][:400]:
            win = self.t[max(0, i - span):i + span]
            cov = sum(b.size for b in SequenceMatcher(None, tgt, win, autojunk=False)
                      .get_matching_blocks()) / len(tgt)
            if cov > best:
                best = cov
                if best > 0.999:
                    break
        return best


# ------------------------------------------------------------- layer 3: answers
def _pages(pdf: Path):
    with tempfile.TemporaryDirectory() as td:
        out = Path(td) / "o"
        subprocess.run(["pdftohtml", "-xml", "-i", "-nodrm", "-q", str(pdf), str(out)],
                       check=True, capture_output=True)
        xml = out.with_suffix(".xml").read_text(encoding="utf-8", errors="replace")
    for page in ET.fromstring(xml).iter("page"):
        frags = []
        for t in page.iter("text"):
            txt = "".join(t.itertext()).strip()
            if txt:
                frags.append({"left": int(t.get("left")), "top": int(t.get("top")),
                              "width": int(t.get("width") or 0), "text": txt})
        yield int(page.get("number")), frags


def _centre(f):
    return f["left"] + f["width"] / 2


def _clusters(frags, tol=6):
    """Group fragments into columns by x-centre, densest (then rightmost) first.

    The header row cannot be used for this: most B-class pages carry no header at all,
    and on the A-class second tables the repeated header sits at different x than the
    body it heads.
    """
    out = []
    for f in sorted(frags, key=_centre):
        if out and _centre(f) - _centre(out[-1][-1]) <= tol:
            out[-1].append(f)
        else:
            out.append([f])
    return sorted(out, key=lambda c: (-len(c), -_centre(c[0])))


def _row_bands(tops, cap):
    """Row boundaries at the midpoints between consecutive Nº positions.

    Splitting at the numbers themselves loses answers: the Nº is centred in its cell, so
    a tall row's answer letter can sit above its own number.
    """
    return [((tops[i - 1] + t) / 2 if i else t - 20,
             (t + tops[i + 1]) / 2 if i + 1 < len(tops) else t + cap)
            for i, t in enumerate(tops)]


def pdf_rows(pdf: Path):
    """[(page, Nº, [letters], {tokens}) ...] in document order.

    An empty letter list means the row's RESPUESTA cell could not be read. The token set
    is everything printed in the row band, used to line JSON records up against rows.
    """
    out = []
    for pno, frags in _pages(pdf):
        numcl = _clusters([f for f in frags if NUM.match(f["text"])])
        if not numcl:
            continue
        widest = max(len(c) for c in numcl)
        numc = min((c for c in numcl if len(c) == widest), key=lambda c: _centre(c[0]))
        nums = sorted((f["top"], int(f["text"])) for f in numc)

        # a page that ends one table and starts another carries two answer columns at
        # different x, so clusters are tried densest-first and a row falls through
        letcls = [sorted((f["top"], LET.match(f["text"]).group(1).lower()) for f in c)
                  for c in _clusters([f for f in frags if LET.match(f["text"])])]
        gaps = [b[0] - a[0] for a, b in zip(nums, nums[1:])]
        cap = (median(gaps) if gaps else 40) * 2.5

        for (lo, hi), (_t, num) in zip(_row_bands([t for t, _ in nums], cap), nums):
            found = []
            for cl in letcls:
                found = [l for t, l in cl if lo <= t < hi]
                if len(found) == 1:
                    break
            words = {w for f in frags if lo <= f["top"] < hi for w in toks(f["text"])}
            out.append((pno, num, found, words))
    return out


def align(data, rows):
    """Pair JSON records with PDF rows, tolerating a row that has no record (or vice versa).

    b2c's JSON skips one source row (its RESPUESTA and 4th option are blank in the PDF),
    and comparing by position after such a skip turns every later row into a false
    mismatch. A row is matched on how much of the record's own wording it contains.
    """
    def score(rec, row):
        want = set(toks(rec.get("title") or ""))
        for o in rec.get("options") or []:
            want |= set(toks(OPT_PREFIX.sub("", o)))
        return len(want & row[3]) / len(want) if want else 1.0

    def pair_score(k, m, depth=2):
        """How well records k.. line up with rows m.. over the next few steps."""
        vals = [score(data[k + n], rows[m + n])
                for n in range(depth) if k + n < len(data) and m + n < len(rows)]
        return sum(vals) / len(vals) if vals else 0.0

    # Staying in step wins ties by a wide margin: neighbouring rows share a lot of
    # vocabulary, so a single row whose wording is only partly readable (options printed
    # as images, say) must not drag the walk out of sync. A skip is only taken when the
    # two rows after it also line up better.
    MARGIN = 0.15
    pairs, i, j = [], 0, 0
    while i < len(data) and j < len(rows):
        in_step = pair_score(i, j)
        if max(pair_score(i, j + 1), pair_score(i + 1, j)) < in_step + MARGIN:
            pairs.append((i, j))
            i, j = i + 1, j + 1
        elif pair_score(i, j + 1) >= pair_score(i + 1, j):
            j += 1                      # this PDF row has no record
        else:
            pairs.append((i, None))     # this record is in no PDF row
            i += 1
    pairs += [(k, None) for k in range(i, len(data))]
    return pairs


# ----------------------------------------------------------------------- audit
def audit(exam: str) -> dict:
    pdf_name, cat = EXAMS[exam]
    pdf = PDF_DIR / pdf_name
    data = json.loads((JSON_DIR / f"{exam}_questions.json").read_text())["data"]
    txt = PdfText(pdf)
    r = {"exam": exam, "n": len(data), "rows": 0, "struct": [], "text": [],
         "answers": [], "unresolved": [], "images": []}

    seen: dict[int, list[int]] = {}
    for pos, q in enumerate(data, 1):
        tag = f"id{q.get('id')}@{pos}"
        seen.setdefault(q.get("id"), []).append(pos)
        opts = q.get("options") or []
        if len(opts) != 4:
            r["struct"].append(f"{tag}: {len(opts)} options")
        letters = [m.group(1).lower() for o in opts if (m := OPT_PREFIX.match(o or ""))]
        if len(letters) != len(opts):
            r["struct"].append(f"{tag}: option without an a)-d) prefix")
        if (q.get("answer") or "").strip().lower() not in letters:
            r["struct"].append(f"{tag}: answer {q.get('answer')!r} not among its options")
        if q.get("category") != cat:
            r["struct"].append(f"{tag}: category {q.get('category')!r} != {cat!r}")
        if not (q.get("title") or "").strip():
            r["struct"].append(f"{tag}: empty title")

        if (s := txt.score(q.get("title") or "")) < TEXT_THRESHOLD:
            r["text"].append((tag, "title", round(s, 2), (q.get("title") or "")[:70]))
        for o in opts:
            body = OPT_PREFIX.sub("", o or "")
            if (s := txt.score(body)) < TEXT_THRESHOLD:
                r["text"].append((tag, "option", round(s, 2), body[:70]))

        for img in q.get("imagens") or []:
            if not (IMG_DIR / f"{img}.webp").exists():
                r["images"].append(f"{tag}: missing {img}.webp")

    for qid, positions in seen.items():
        if len(positions) > 1:
            r["struct"].append(f"id{qid}: duplicated at positions {positions}")

    rows = pdf_rows(pdf)
    r["rows"] = len(rows)
    if len(rows) != len(data):
        r["struct"].append(f"PDF has {len(rows)} rows, JSON has {len(data)} questions "
                           f"({abs(len(rows) - len(data))} unaccounted for)")
    for i, j in align(data, rows):
        tag = f"id{data[i].get('id')}@{i+1}"
        if j is None:
            r["unresolved"].append(f"{tag}: no matching row in the PDF")
        elif not rows[j][2]:
            r["unresolved"].append(f"{tag}: no answer letter in the PDF (page {rows[j][0]})")
        elif (data[i].get("answer") or "").strip().lower() != rows[j][2][0]:
            r["answers"].append(f"{tag}: JSON {data[i].get('answer')} vs PDF {rows[j][2][0]}")
    return r


def main():
    exams = sys.argv[1:] or list(EXAMS)
    results = [audit(e) for e in exams]

    print(f"{'exam':5}{'json':>6}{'rows':>6}{'struct':>8}{'text':>6}{'answer':>8}{'unres':>7}{'img':>5}")
    print("-" * 51)
    for r in results:
        print(f"{r['exam']:5}{r['n']:>6}{r['rows']:>6}{len(r['struct']):>8}{len(r['text']):>6}"
              f"{len(r['answers']):>8}{len(r['unresolved']):>7}{len(r['images']):>5}")

    if set(exams) == set(EXAMS):
        on_disk = {p.stem for p in IMG_DIR.glob("*.webp")}
        used = set()
        for e in exams:
            for q in json.loads((JSON_DIR / f"{e}_questions.json").read_text())["data"]:
                used.update(q.get("imagens") or [])
        print(f"\nimages: {len(on_disk)} on disk / {len(used)} referenced / "
              f"{len(on_disk - used)} orphan / {len(used - on_disk)} broken")

    for r in results:
        if not any((r["struct"], r["text"], r["answers"], r["unresolved"], r["images"])):
            continue
        print(f"\n===== {r['exam']} =====")
        for key, label, limit in (("answers", "ANSWER disagrees with PDF", 40),
                                  ("unresolved", "ANSWER unreadable in PDF", 10),
                                  ("text", "TEXT not found in PDF", 15),
                                  ("struct", "STRUCTURE", 8),
                                  ("images", "IMAGES", 8)):
            if items := r[key]:
                print(f"-- {label} ({len(items)})")
                for it in items[:limit]:
                    print("   ", it)
                if len(items) > limit:
                    print(f"    ... {len(items) - limit} more")


if __name__ == "__main__":
    main()
