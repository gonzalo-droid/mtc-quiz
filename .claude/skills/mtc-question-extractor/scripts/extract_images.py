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

import binascii
import json
import struct
import subprocess
import sys
import zlib
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from pdf_layout import (  # noqa: E402
    ImageEl,
    build_row_bands,
    detect_columns,
    detect_header_rows,
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

# Minimum pixel height for something to count as a real picture rather
# than a band-boundary leak stub (see the comment above its use in
# `main`). Every genuine image/composite observed across all 9 exams is
# >=33px tall; every confirmed leak fragment is <=6px - a wide margin
# either side of this value.
_MIN_CONTENT_HEIGHT = 15


def filter_logo_images(images: list[ImageEl], page_count: int) -> list[ImageEl]:
    """Drop images that repeat at the same size/position across many pages
    (headers/logos), keeping only images that appear on few pages - actual
    question illustrations are unique per page.

    Keys on (width, height, rounded position) rather than image byte
    content - this only tells "same size and place, repeated across most
    of this single PDF" apart from "appears once or twice", which is what
    actually distinguishes a reprinted header/logo from a real per-question
    illustration *within one exam's own PDF*. It does NOT compare pixel
    content, so two different exams that happen to reuse the same sign
    illustration (a real content image) are never confused with each other
    here in the first place: `main` calls this once per exam, on that
    exam's own `images` list only, so there's no cross-exam data in `images`
    to accidentally collide against. The only false-positive risk is
    entirely intra-document: a real content image that coincidentally
    repeats at the same rounded size/position on enough pages of the *same*
    PDF to cross `threshold` - see Step 2/3's manual spot-check, which
    verifies by band position (does the image land in the right question's
    row) rather than trusting this heuristic blindly.
    """
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


# The 3 CLASE_B_II*.pdf files (b2a/b2b/b2c) don't embed each picture as one
# `pdftohtml`-exported image the way the other 6 PDFs do. Instead, every
# real picture (e.g. a candidate traffic-sign icon for one alt column) is
# split into dozens-to-hundreds of separate, individually-tiny <image>
# elements - each one exactly 1px tall, same `left`/`width` as its
# neighbors, stacked with consecutive `top`s - that only add up to the
# real, legible picture when reassembled. Confirmed via `file`/struct on
# the raw PNGs (all 8-bit RGB, no alpha, no palette, non-interlaced): a
# single icon like the ones in q13/q15's "which sign is X" options is
# physically ~340 individual 1x1... 1x59px row slices, not one image.
# Treating each raw slice as its own "image" (the naive reading of
# `ImageEl`) produces near-blank 1px-tall WebP files - confirmed by reading
# a handful back with the Read tool during Step 2/3 manual verification;
# this isn't a hypothetical, it's the actual failure mode this exists to
# fix. `_read_png_rgb8`/`_write_png_rgb8`/`stitch_sliced_images` below
# reassemble these into one real composite image per logical picture,
# entirely in the stdlib (per this script's own "Python 3 stdlib only"
# constraint - no Pillow, no ImageMagick) by manually decoding/re-encoding
# the PNG scanlines.
#
# This is applied unconditionally (not gated on exam_id) because it's a
# safe no-op for the other 6 PDFs: `main` only calls it *after* each image
# has already been assigned to its owning question via `band_for_image`,
# and groups purely by (left, width) *within* one question's own image
# list. A real single-picture PDF's per-question image list only ever has
# one image at any given (left, width) to begin with, so every group there
# has exactly 1 member and passes through `stitch_sliced_images` unchanged
# - verified: re-running against a1 after adding this produced byte-
# identical `imagens` associations to the pre-fix run.


def _png_chunks(data: bytes):
    pos = 8
    while pos < len(data):
        length = struct.unpack(">I", data[pos : pos + 4])[0]
        ctype = data[pos + 4 : pos + 8]
        cdata = data[pos + 8 : pos + 8 + length]
        yield ctype, cdata
        pos += 12 + length
        if ctype == b"IEND":
            break


def _paeth(a: int, b: int, c: int) -> int:
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    if pb <= pc:
        return b
    return c


def _unfilter_row(ftype: int, line: bytearray, prev: bytes, bpp: int) -> None:
    n = len(line)
    if ftype == 0:
        return
    if ftype == 1:  # Sub
        for i in range(n):
            a = line[i - bpp] if i >= bpp else 0
            line[i] = (line[i] + a) & 0xFF
    elif ftype == 2:  # Up
        for i in range(n):
            line[i] = (line[i] + prev[i]) & 0xFF
    elif ftype == 3:  # Average
        for i in range(n):
            a = line[i - bpp] if i >= bpp else 0
            b = prev[i]
            line[i] = (line[i] + (a + b) // 2) & 0xFF
    elif ftype == 4:  # Paeth
        for i in range(n):
            a = line[i - bpp] if i >= bpp else 0
            b = prev[i]
            c = prev[i - bpp] if i >= bpp else 0
            line[i] = (line[i] + _paeth(a, b, c)) & 0xFF
    else:
        raise ValueError(f"unsupported PNG filter type {ftype}")


def _read_png_rgb8(path: Path) -> tuple[int, list[bytes]] | None:
    """Decode a PNG into `(width, [unfiltered scanline bytes, ...])`.

    Only supports what every sliced-image PNG in these PDFs actually is
    (confirmed via struct-level inspection of every b2a/b2b/b2c source
    file): 8-bit depth, color type 2 (RGB, no alpha/palette), no
    interlacing. Returns None for anything else so the caller can fall
    back gracefully instead of producing a corrupt composite.
    """
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    width = height = bit_depth = color_type = interlace = None
    idat = bytearray()
    for ctype, cdata in _png_chunks(data):
        if ctype == b"IHDR":
            width, height, bit_depth, color_type, _comp, _filt, interlace = struct.unpack(
                ">IIBBBBB", cdata
            )
        elif ctype == b"IDAT":
            idat.extend(cdata)
    if width is None or bit_depth != 8 or color_type != 2 or interlace != 0:
        return None
    bpp = 3
    stride = width * bpp
    raw = zlib.decompress(bytes(idat))
    if len(raw) < height * (stride + 1):
        return None
    rows: list[bytes] = []
    prev = bytes(stride)
    idx = 0
    for _ in range(height):
        ftype = raw[idx]
        idx += 1
        line = bytearray(raw[idx : idx + stride])
        idx += stride
        _unfilter_row(ftype, line, prev, bpp)
        rows.append(bytes(line))
        prev = rows[-1]
    return width, rows


def _write_png_rgb8(path: Path, width: int, rows: list[bytes]) -> None:
    """Encode `rows` (each `width*3` raw RGB8 bytes) as a plain, unfiltered
    (filter type 0 per scanline - simplest correct choice, no need to
    optimize for size at icon resolutions) 8-bit RGB PNG."""

    def chunk(ctype: bytes, cdata: bytes) -> bytes:
        body = ctype + cdata
        return struct.pack(">I", len(cdata)) + body + struct.pack(">I", binascii.crc32(body) & 0xFFFFFFFF)

    height = len(rows)
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)
    raw = bytearray()
    for row in rows:
        raw.append(0)
        raw.extend(row)
    idat = zlib.compress(bytes(raw), 9)
    out = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", idat) + chunk(b"IEND", b"")
    path.write_bytes(out)


def stitch_sliced_images(qimages: list[ImageEl], workdir: Path, qnum: int) -> list[ImageEl]:
    """Reassemble row-sliced pictures (see module comment above) within one
    question's own already-assigned image list. Grouping is deliberately
    scoped to *within* a single question (`qimages` is always one
    question's list, called from `main` after `band_for_image`) rather
    than across the whole document: two unrelated icons for two different
    questions can otherwise sit at the exact same (left, width) with zero
    visible top gap between them (confirmed on CLASE_B_IIA.pdf - one
    left=555 run of 1px slices continues unbroken for 80+ rows spanning
    what turned out to be several different questions' own icons back to
    back), so a purely position-based "same left/width, contiguous top"
    grouping pass over the whole document would silently merge two
    different questions' pictures into one. Scoping to one question's own
    `qimages` (whose membership already came from the correct, band-based
    per-question assignment) sidesteps that entirely - within one
    question's own image list, everything sharing the same (left, width)
    is safe to assume is the same logical picture, since a single table
    cell has at most one picture in it.
    """
    groups: dict[tuple[int, int], list[ImageEl]] = {}
    for im in qimages:
        groups.setdefault((im.left, im.width), []).append(im)

    result: list[ImageEl] = []
    for (left, width), members in groups.items():
        if len(members) == 1:
            result.append(members[0])
            continue
        members = sorted(members, key=lambda m: m.top)
        rows: list[bytes] = []
        widths: set[int] = set()
        ok = True
        for m in members:
            parsed = _read_png_rgb8(m.src)
            if parsed is None:
                ok = False
                break
            w, r = parsed
            widths.add(w)
            rows.extend(r)
        if not ok or len(widths) != 1:
            # Can't safely reassemble (unexpected PNG format, or a member
            # whose decoded width doesn't match its neighbors) - fall back
            # to the single tallest raw slice rather than silently
            # dropping the question's only picture entirely.
            result.append(max(members, key=lambda m: m.height))
            continue
        out_dir = workdir / "stitched"
        out_dir.mkdir(parents=True, exist_ok=True)
        out_path = out_dir / f"q{qnum}_{left}_{width}_{len(members)}.png"
        _write_png_rgb8(out_path, widths.pop(), rows)
        result.append(
            ImageEl(
                page=members[0].page,
                top=min(m.top for m in members),
                left=left,
                width=width,
                height=sum(m.height for m in members),
                src=out_path,
            )
        )
    return result


def convert_to_webp(src: Path, dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(["cwebp", "-quiet", str(src), "-o", str(dest)], check=True)


def detect_rows_allowing_resets(texts, columns):
    """Like `pdf_layout.detect_question_rows`, but tolerant of the Nº
    column restarting from 1 partway through the document.

    CLASE_A_IIIB.pdf and CLASE_A_IIIC.pdf (exams a3b/a3c) each print more
    than one numbered table, and every table after the first restarts its
    own Nº column at 1 (confirmed directly: CLASE_A_IIIB.pdf's Nº column
    reads "...199, 200, 1, 2, 3..." starting on page 21, with the "1"
    aligning exactly with a3b_questions.json's own topic change at array
    index 200 - i.e. a genuine second table, not noise). Their committed
    JSON's own `id` field mirrors that same restart (ids 1-71 each appear
    twice in a3b_questions.json, 1-139 twice in a3c_questions.json), so
    `id` is not an exam-wide-unique key for these two exams the way it is
    for the other 7 (see `main`, which only calls this function when
    `data["data"]` has been found to contain duplicate ids).

    `detect_question_rows`'s strict `qnum == expected` check (requiring
    the Nº column to count continuously 1..N for the *whole* document,
    documented as intentional there - it's what rejects accidental digit-
    shaped noise landing in that column's x-range on the other 7, well-
    behaved PDFs) has no way to distinguish "the table legitimately
    restarted" from "row detection lost sync" and simply stops accepting
    anything once the count no longer matches - which is exactly the
    truncation bug this function exists to avoid (an earlier run of this
    script silently wrote a3b/a3c_questions.json truncated to 200
    questions this way, permanently losing ids 201+ until a human noticed
    and restored from git). This function accepts a restart back to 1 as
    the start of a new table instead of rejecting it, and returns rows
    renumbered with a single document-global, always-unique, always-
    increasing-by-1 position (1..N) rather than the PDF's own repeating Nº
    digit - `main` uses that position to index directly into
    `data["data"]` by array position instead of via an `id` lookup,
    sidestepping the id-collision problem entirely. Verified empirically
    for both a3b and a3c: raw digit-candidate count in the Nº column
    (271/339) exactly matches each file's own total question count, with
    exactly one restart each, so no additional noise-filtering beyond the
    restart tolerance is needed for these two documents.
    """
    lo, hi = columns["numero"]
    raw = []
    for t in texts:
        if not (lo - 5 <= t.left < hi):
            continue
        if not t.text.strip().isdigit():
            continue
        raw.append((int(t.text.strip()), t.page, t.top))
    cleaned_positions: list[tuple[int, int]] = []
    expected = 1
    for qnum, page, top in raw:
        if qnum == expected:
            cleaned_positions.append((page, top))
            expected += 1
        elif qnum == 1 and expected != 1:
            # A new table begins: accept the restart and keep counting.
            cleaned_positions.append((page, top))
            expected = 2
    return [(i + 1, page, top) for i, (page, top) in enumerate(cleaned_positions)]


def main() -> None:
    exam_id = sys.argv[1]
    pdf_path = PDF_DIR / f"{EXAM_TO_PDF[exam_id]}.pdf"
    json_path = JSON_DIR / f"{exam_id}_questions.json"
    workdir = Path(f"/tmp/mtc_extractor_images_{exam_id}")

    data = json.loads(json_path.read_text())
    original_count = len(data["data"])
    ids = [q["id"] for q in data["data"]]
    # a3b/a3c print more than one numbered table in the same PDF, and
    # every table after the first restarts its own Nº column at 1 (see
    # detect_rows_allowing_resets's docstring) - their committed JSON
    # mirrors that with genuinely repeating `id` values, unlike the other
    # 7 exams where `id` is exam-wide unique. Detect that here (rather
    # than hardcoding exam_id) so this stays correct if a future exam's
    # PDF turns out to have the same multi-table structure.
    has_duplicate_ids = len(set(ids)) != len(ids)

    xml_path = run_pdftohtml(pdf_path, workdir)
    texts, images = parse_xml(xml_path)
    columns = detect_columns(texts)
    if has_duplicate_ids:
        # detect_question_rows's strict global continuity check would
        # silently stop at the first table's last row (200 for both a3b
        # and a3c) instead of recognizing the second table's own restart
        # at 1 as legitimate - see detect_rows_allowing_resets's
        # docstring. This was the actual root cause of a previous run
        # silently truncating a3b/a3c_questions.json to 200 questions.
        rows = detect_rows_allowing_resets(texts, columns)
    else:
        rows = detect_question_rows(texts, columns)
    header_rows = detect_header_rows(texts)
    last_page = max(t.page for t in texts)
    # build_row_bands needs header_rows/texts/columns (not just rows/
    # last_page) to correctly stop a page-starting row's band after that
    # page's reprinted header, and to cap the final question's band at a
    # section-title line or later table's header rather than sweeping in
    # unrelated content - see pdf_layout.build_row_bands's own docstring.
    # Images living in that unrelated trailing content (e.g. a
    # supplementary section's own illustrations) would otherwise get
    # misattributed to the last real question.
    bands = build_row_bands(rows, last_page, header_rows, texts, columns)
    page_count = last_page

    # Band-match every *raw* image first, before any logo filtering. For
    # the 3 row-sliced PDFs (b2a/b2b/b2c - see comment above
    # stitch_sliced_images) a single real picture is 40-300+ individual
    # 1px-tall slices, each at its own slightly different `top`; row N (out
    # of ~150) of one question's icon and row N of a *different* question's
    # same-shaped icon round to a similar/identical `key()` bucket in
    # filter_logo_images, so running that filter on raw slices - as an
    # earlier version of this script did - wrongly treats most of a real
    # icon's own rows as "repeated across many pages" and discards them:
    # confirmed regression, CLASE_B_IIC.pdf q13's first icon kept only 3 of
    # its 42 real rows this way, producing a near-blank composite. Grouping
    # into per-question bands *before* filtering avoids this entirely -
    # `filter_logo_images` never sees individual scanlines, only whole
    # pictures (see below).
    per_question: dict[int, list[ImageEl]] = {}
    for im in images:
        qnum = band_for_image(bands, im)
        if qnum is None:
            continue
        per_question.setdefault(qnum, []).append(im)

    # Reassemble row-sliced pictures into single composite images, scoped
    # within each question's own already-assigned image list (see
    # stitch_sliced_images's docstring for why it must be scoped this way
    # rather than done globally before band assignment).
    for qnum in per_question:
        per_question[qnum] = stitch_sliced_images(per_question[qnum], workdir, qnum)

    # A single physical picture in the row-sliced PDFs can straddle a
    # band's own boundary the same way a run of *text* lines can (see
    # pdf_layout.build_row_bands's docstring on why bands split at the
    # midpoint between two Nº digits, not at guaranteed content edges): the
    # picture's own last 1-2 rows can land fractionally past that midpoint
    # and get attributed to the *next* question instead of the one the
    # picture actually belongs to. Confirmed real case: CLASE_B_IIC.pdf
    # q16 ("...tienen, por lo general, la siguiente forma: a) Rectangular
    # b) Romboidal c) Triangulo tipo banderola" - a plain text/shape-name
    # question with no picture content of its own at all) picked up a
    # bare 2-row, ~2px-tall stub - the tail end of q15's own candidate-
    # sign icon - as its sole "image", rendering as an essentially blank
    # WebP. Every genuine picture across all 9 exams (verified directly:
    # the shortest real one is 33px tall in the row-sliced PDFs, 39px in
    # the others) is far taller than a stray 1-3-row fragment like this,
    # so dropping anything under a generous `_MIN_CONTENT_HEIGHT` here
    # removes this whole class of boundary leak without any real risk of
    # discarding genuine content.
    per_question = {
        qnum: [im for im in qimages if im.height >= _MIN_CONTENT_HEIGHT]
        for qnum, qimages in per_question.items()
    }

    # Only now - operating on whole reassembled pictures, one per question,
    # never on individual scanlines - does the logo/header heuristic mean
    # what its own docstring says: "appears at the same size/position
    # across many pages". Must run on the *flattened, whole-document* set
    # of final images (not scoped per question like the band-matching /
    # stitching steps above): repetition across pages is only detectable
    # by comparing across the whole document at once - scoping it to one
    # question's own 1-3 images at a time would make every image trivially
    # "unique" and defeat the filter entirely. Any question whose only
    # picture(s) get dropped here simply ends up with no `imagens` entries.
    all_final_images = [im for qimages in per_question.values() for im in qimages]
    content_set = set(filter_logo_images(all_final_images, page_count))
    per_question = {
        qnum: [im for im in qimages if im in content_set]
        for qnum, qimages in per_question.items()
    }
    # Drop questions left with zero images after filtering, so they don't
    # show up as a hollow `"imagens": []` entry or get counted in the
    # summary line below - matches the original invariant (before this
    # filter step existed) that `per_question` only ever holds questions
    # that actually have at least one image.
    per_question = {qnum: ims for qnum, ims in per_question.items() if ims}

    # `records` (never reassigned/rebuilt - always the same list object
    # `data["data"]` already is) is the single source of truth for what
    # gets written back, for *both* branches below. This is deliberate:
    # the previous version of this script instead reconstructed its
    # output from `list(by_id.values())`, a dict keyed by `q["id"]` - for
    # a3b/a3c, where `id` isn't exam-wide unique (see `has_duplicate_ids`
    # above), that dict comprehension silently collapsed 271/339 records
    # down to 200 unique keys, and the script happily wrote that
    # truncated list back out, permanently losing ids 201+ until a human
    # noticed and restored from git HEAD. Writing from `records` directly
    # makes that whole failure mode structurally impossible: its length
    # is fixed at `original_count` for the entire function.
    records = data["data"]

    # Clear every question's `imagens` first so a question that had images
    # on a previous run but has none now (e.g. because a logo-filtering
    # threshold change reclassified its only image) doesn't keep a stale
    # association - matches this script's own "clears then rebuilds"
    # contract (see module docstring), rather than only ever overwriting
    # entries that happen to get a fresh match this run.
    for q in records:
        q.pop("imagens", None)

    if has_duplicate_ids:
        # `id` doesn't uniquely address a record (see above), but `qnum`
        # here is the document-global sequential position produced by
        # detect_rows_allowing_resets (1..N, always unique, always ==
        # array position + 1) - verified empirically to align exactly
        # with `records`'s own array order for both a3b and a3c (the
        # digit-column restart lands on exactly the same index as the
        # JSON's own topic-change boundary in both files).
        def record_for(qnum: int):
            idx = qnum - 1
            return records[idx] if 0 <= idx < len(records) else None
    else:
        by_id = {q["id"]: q for q in records}

        def record_for(qnum: int):
            return by_id.get(qnum)

    # 26 letters comfortably covers every case observed across all 9 exams
    # (max is b2c's q13 at a handful of stitched composites, well under
    # 10) with headroom to spare; falls back to a numeric suffix instead
    # of raising if some future PDF ever has more than 26 images on one
    # question, rather than crashing the whole exam's run over one row.
    letters = "abcdefghijklmnopqrstuvwxyz"
    for qnum, qimages in per_question.items():
        record = record_for(qnum)
        if record is None:
            continue
        names = []
        for i, im in enumerate(sorted(qimages, key=lambda x: (x.page, x.top, x.left))):
            suffix = letters[i] if i < len(letters) else str(i)
            name = f"q{qnum}_{suffix}_{exam_id}"
            convert_to_webp(im.src, IMAGES_DIR / f"{name}.webp")
            names.append(name)
        record["imagens"] = names

    # Hard safety check: this script must be a strict no-op on the set of
    # questions and every field except `imagens` - it only annotates
    # existing entries, never adds/drops/replaces one. Refuse to write
    # anything if the output question count would ever drift from the
    # input count, rather than trusting that the rest of this script got
    # it right (see the comment on `records` above for the concrete bug
    # this guards against).
    if len(records) != original_count:
        raise RuntimeError(
            f"{exam_id}: refusing to write {json_path} - question count "
            f"would change from {original_count} to {len(records)}. "
            "This script must never add/drop questions, only annotate "
            "`imagens` on existing ones."
        )

    json_path.write_text(
        json.dumps({"data": records}, ensure_ascii=False, indent=4),
        encoding="utf-8",
    )
    print(f"{exam_id}: associated images for {len(per_question)} questions")


if __name__ == "__main__":
    main()
