#!/usr/bin/env python3
"""Check every question image against the picture printed in its PDF row.

Two things can be wrong and neither is visible by reading the JSON: a file can be
associated with the wrong question, or it can simply not be the picture that row prints.
So the images are pulled out of the PDF with their coordinates, assigned to a row by the
same band logic the text audit uses, and compared pixel by pixel with the .webp the app
ships.

What it compares is the *set* per row, not the order: the letter in q4_a_a1 is a sequence
index, not the option it illustrates (see SKILL.md), so what has to hold is that every
image shown for a question is one the PDF prints in that row, and that none is missing.

The B-class PDFs draw their signs as stacks of hairline strips rather than one embedded
picture, so those rows have nothing to compare against and are reported as such; check
them by eye with --render, which writes a PNG of the row above its images.

Usage:
    python3 audit_images.py [examId ...]
    python3 audit_images.py --render <examId> <id> [id ...]   # PNG for a visual check
"""
import json
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path
from statistics import median

sys.path.insert(0, str(Path(__file__).resolve().parent))
from audit_questions import (EXAMS, IMG_DIR, JSON_DIR, NUM, PDF_DIR,  # noqa: E402
                             _centre, _clusters, _pages)

SAME = 0.06          # mean absolute pixel difference below which two pictures are the same
MIN_SIDE = 20        # smaller than this is a rule or a hairline, not a sign


def pdf_images(pdf: Path, outdir: Path):
    """[(page, top, left, w, h, file)] for every picture embedded in the document."""
    subprocess.run(["pdftohtml", "-xml", "-nodrm", "-q", str(pdf), str(outdir / "x")],
                   check=True, capture_output=True)
    root = ET.fromstring((outdir / "x.xml").read_text(encoding="utf-8", errors="replace"))
    out = []
    for page in root.iter("page"):
        pno = int(page.get("number"))
        for im in page.iter("image"):
            w, h = int(im.get("width")), int(im.get("height"))
            if w >= MIN_SIDE and h >= MIN_SIDE:
                out.append((pno, int(im.get("top")), int(im.get("left")), w, h,
                            Path(im.get("src"))))
    return out


def row_bands(pdf: Path):
    """[(page, Nº, lo, hi)] in document order."""
    bands = []
    for pno, frags in _pages(pdf):
        numcl = _clusters([f for f in frags if NUM.match(f["text"])])
        if not numcl:
            continue
        widest = max(len(c) for c in numcl)
        numc = min((c for c in numcl if len(c) == widest), key=lambda c: _centre(c[0]))
        nums = sorted((f["top"], int(f["text"])) for f in numc)
        tops = [t for t, _ in nums]
        gaps = [b - a for a, b in zip(tops, tops[1:])]
        cap = (median(gaps) if gaps else 40) * 2.5
        for i, (top, num) in enumerate(nums):
            # a page's letterhead sits above the table, so the first row does not reach
            # the top of the page the way its text band does
            lo = (tops[i - 1] + top) / 2 if i else max(0, top - 60)
            hi = (top + tops[i + 1]) / 2 if i + 1 < len(tops) else top + cap
            bands.append((pno, num, lo, hi))
    return bands


def by_row(exam: str, tmp: Path):
    """{document position: [(top, left, file)]} — the pictures the PDF prints per row."""
    pdf = PDF_DIR / EXAMS[exam][0]
    bands = row_bands(pdf)
    per_row: dict[int, list] = {}
    orphans = []
    # pictures printed side by side belong to one cell, so a whole line of them goes to a
    # single row: a band drawn between two Nº positions can otherwise cut a line in half
    lines: dict[tuple, list] = {}
    for pno, top, left, _w, _h, src in sorted(pdf_images(pdf, tmp)):
        key = next((k for k in lines if k[0] == pno and abs(k[1] - top) <= 10), (pno, top))
        lines.setdefault(key, []).append((top, left, src))
    for (pno, _t), members in lines.items():
        mid = sorted(m[0] for m in members)[len(members) // 2]
        hit = [k for k, (p, _n, lo, hi) in enumerate(bands) if p == pno and lo <= mid < hi]
        if hit:
            per_row.setdefault(hit[0], []).extend(members)
        else:
            orphans += members
    for k in per_row:
        per_row[k].sort(key=lambda m: m[1])
    return bands, per_row, orphans


def diff(a: Path, b: Path) -> float:
    """Mean absolute difference of two pictures: 0 identical, 1 opposite."""
    from PIL import Image
    ia = Image.open(a).convert("RGB")
    ib = Image.open(b).convert("RGB")
    if ia.size != ib.size:
        ib = ib.resize(ia.size)
    pa, pb = ia.tobytes(), ib.tobytes()
    return sum(abs(x - y) for x, y in zip(pa, pb)) / (len(pa) * 255)


def audit(exam: str):
    data = json.loads((JSON_DIR / f"{exam}_questions.json").read_text())["data"]
    problems, checked, unreadable = [], 0, 0
    with tempfile.TemporaryDirectory() as td:
        _bands, per_row, _orph = by_row(exam, Path(td))
        for i, q in enumerate(data):
            pool = list(per_row.get(i, []))
            names = q.get("imagens") or []
            if names and len(pool) < len(names):
                # the B-class PDFs draw their signs as stacks of hairline strips, so the
                # row has no embedded picture to compare against: check it with --render
                unreadable += len(names)
                continue
            for name in names:
                checked += 1
                webp = IMG_DIR / f"{name}.webp"
                if not webp.exists():
                    problems.append(f"id{q['id']}@{i+1}: {name}.webp is missing")
                    continue
                best = min(((diff(p[2], webp), k) for k, p in enumerate(pool)), default=(9, -1))
                if best[0] > SAME:
                    problems.append(f"id{q['id']}@{i+1}: {name} is not printed in this row")
                else:
                    pool.pop(best[1])
            for leftover in pool:
                problems.append(f"id{q['id']}@{i+1}: the PDF prints a picture the JSON omits")
    return checked, unreadable, problems


def render(exam: str, ids, out: Path):
    """A PNG of each row above the images the JSON gives it, for an eyeball check."""
    from PIL import Image
    pdf = PDF_DIR / EXAMS[exam][0]
    bands = row_bands(pdf)
    data = json.loads((JSON_DIR / f"{exam}_questions.json").read_text())["data"]
    tiles = []
    with tempfile.TemporaryDirectory() as td:
        subprocess.run(["pdftohtml", "-xml", "-i", "-nodrm", "-q", str(pdf), f"{td}/x"],
                       check=True, capture_output=True)
        root = ET.fromstring(Path(f"{td}/x.xml").read_text(errors="replace"))
        widths = {int(p.get("number")): float(p.get("width")) for p in root.iter("page")}
        for qid in ids:
            pos = next(i for i, q in enumerate(data) if q["id"] == qid)
            page, _num, lo, hi = bands[pos]
            subprocess.run(["pdftoppm", "-r", "150", "-f", str(page), "-l", str(page),
                            "-png", str(pdf), f"{td}/pg{page}"], check=True, capture_output=True)
            img = Image.open(sorted(Path(td).glob(f"pg{page}-*.png"))[0])
            scale = img.width / widths[page]
            crop = img.crop((0, int(lo * scale), img.width, int(hi * scale)))
            shots = [Image.open(IMG_DIR / f"{n}.webp").convert("RGB")
                     for n in data[pos].get("imagens") or []]
            h = 110
            shots = [s.resize((int(s.width * h / s.height), h)) for s in shots]
            tile = Image.new("RGB", (max(crop.width, sum(s.width + 12 for s in shots) + 12),
                                     crop.height + h + 16), "white")
            tile.paste(crop, (0, 0))
            x = 0
            for s in shots:
                tile.paste(s, (x, crop.height + 8))
                x += s.width + 12
            tiles.append(tile)
    sheet = Image.new("RGB", (max(t.width for t in tiles),
                              sum(t.height + 10 for t in tiles)), "white")
    y = 0
    for t in tiles:
        sheet.paste(t, (0, y))
        y += t.height + 10
    sheet.save(out)
    return out


def main():
    args = sys.argv[1:]
    if args and args[0] == "--render":
        out = render(args[1], [int(a) for a in args[2:]], Path(f"{args[1]}_rows.png"))
        print(f"wrote {out}")
        return
    print(f"{'exam':5}{'checked':>9}{'problems':>10}{'not embedded':>14}")
    for exam in args or list(EXAMS):
        checked, unreadable, problems = audit(exam)
        print(f"{exam:5}{checked:>9}{len(problems):>10}{unreadable:>14}")
        for p in problems[:10]:
            print("   ", p)


if __name__ == "__main__":
    main()
