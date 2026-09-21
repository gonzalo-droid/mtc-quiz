"""Row assignment for extracted pictures: a line of pictures belongs to the question row that
holds most of its height, not the one its top edge happens to fall in.

Regression for gonzalo-droid/mtc-quiz#21: in every A-class balotario the lane diagram of
question 93 starts 2px above the midpoint between rows 92 and 93, so assigning by top edge put it
on 92 (a text-only row) and left 93 — whose title says "que se muestran en la figura" — with
nothing. audit_images.py already uses the majority-of-height rule; this keeps the extractor from
reintroducing the bug on the next re-extraction.

Run: python3 test_extract_images.py
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from extract_images import assign_images_to_questions  # noqa: E402
from pdf_layout import ImageEl  # noqa: E402


def img(page, top, height, left=100, width=120):
    return ImageEl(page=page, top=top, left=left, width=width, height=height, src=Path("x.png"))


def assigned(images, bands):
    return {q: sorted(im.top for im in ims) for q, ims in assign_images_to_questions(images, bands).items()}


def test_tall_figure_whose_top_pokes_into_the_row_above():
    # bands: (qnum, start page, start top, end page, end top); the split is the digit midpoint
    bands = [(92, 5, 100, 5, 316), (93, 5, 316, 5, 600)]
    figure = img(page=5, top=314, height=200)  # 2px in 92's band, 198px in 93's
    assert assigned([figure], bands) == {93: [314]}, assigned([figure], bands)


def test_line_of_icons_straddling_the_midpoint_stays_together():
    # the CLASE_A_IIIC.pdf page-24 case the old majority vote was written for: one icon 3px on
    # 229's side of the 544px split, two at 547 on 230's side, all one printed row
    bands = [(229, 24, 400, 24, 544), (230, 24, 544, 24, 700)]
    row = [img(24, 541, 40, left=100), img(24, 547, 40, left=260), img(24, 547, 40, left=420)]
    assert assigned(row, bands) == {230: [541, 547, 547]}, assigned(row, bands)


def test_picture_wholly_inside_a_row_stays_there():
    bands = [(10, 2, 100, 2, 300), (11, 2, 300, 2, 500)]
    assert assigned([img(2, 150, 80)], bands) == {10: [150]}


def test_rows_on_different_pages_do_not_interfere():
    bands = [(40, 3, 600, 4, 120), (41, 4, 120, 4, 400)]  # 40 starts on page 3, ends on page 4
    assert assigned([img(4, 60, 50), img(4, 200, 90)], bands) == {40: [60], 41: [200]}


def main():
    tests = [v for k, v in sorted(globals().items()) if k.startswith("test_")]
    for t in tests:
        t()
    print(f"OK - {len(tests)} row-assignment tests")


if __name__ == "__main__":
    main()
