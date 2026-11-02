#!/usr/bin/env python3
"""Draws the Nytheria Compat branding as true pixel art in the Nytheria "Moonlit" style.

The emblem is a round plate split into two halves that are held together by a brass butterfly
key (the bow-tie shaped inlay a carpenter uses to join two boards): two pieces made to work
together inside Nytheria. The halves use the warm sunset ramp of the Nytheria "N"; they sit on a
dithered night sky with a few stars inside the gold pixel corner frame of the Nytheria pack icon.

Every image is drawn on its own native grid from hand-placed string maps and bitmap fonts defined
below, then enlarged by an integer factor with nearest neighbour only. Colours come only from
PAL (hex values copied from the Nytheria brand kit / Wayfarer palette). No randomness, no external
files: every run writes byte-identical PNGs. After writing, a self-check re-reads each file and
exits non-zero if a size, the integer upscale or a colour is wrong.

Usage:  uv run --with pillow tools/icon/make_logo.py [--preview DIR]
Writes: branding/logo-400.png   400x400   native 100x100 x4 (CurseForge avatar)
        branding/logo.png       512x512   native 128x128 x4
        branding/icon-128.png   128x128   native 32x32 x4 (also the mod icon below)
        src/main/resources/assets/nytheria_compat/icon.png
        branding/banner.png     1600x400  native 400x100 x4
        DIR/contact-sheet.png, DIR/icon-32.png   optional previews (keep DIR outside the repo)
"""
import argparse
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "branding"
MOD_ICON = ROOT / "src/main/resources/assets/nytheria_compat/icon.png"

# ---- palette (Wayfarer / Moonlit keys) ----------------------------------------------------
PAL = {
    # night sky: Wayfarer navy, darkest -> lightest
    'LO': '#090B1A', 'L0': '#11152E', 'L1': '#1A2042', 'L2': '#232A55', 'L3': '#2A3261',
    # stars
    'I3': '#5C70C0', 'LAV': '#BBCEF3', 'SH': '#FFF4D8',
    # brass frame and key
    'BO': '#3E1E0C', 'BD': '#92501F', 'BB': '#E0A324', 'BH': '#FFD852', 'BW': '#FFF0B0',
    # sunset ramp (emblem halves, wordmark), catch light, plum outline / extrusion
    'E5': '#FFF2B8', 'E4': '#FFD852', 'E3': '#FF9A3C', 'E2': '#E2622B', 'E1': '#B33A1E',
    'PW': '#FFF6DE', 'PLd': '#7B3148', 'PLo': '#3A1424',
}
RGBA = {k: tuple(int(v[i:i + 2], 16) for i in (1, 3, 5)) + (255,) for k, v in PAL.items()}
SUNSET = ('E5', 'E4', 'E3', 'E2', 'E1', 'PLd')
LETTERS = ('E5', 'E4', 'E3', 'E2', 'E1')
BRASS = ('BH', 'BH', 'BB', 'BB', 'BD')
BAYER4 = ((0, 8, 2, 10), (12, 4, 14, 6), (3, 11, 1, 9), (15, 7, 13, 5))

# ---- emblem maps: L / R = the two halves, k = brass butterfly key, . = empty ----------------
DISC_24 = [  # 32x32 mod icon, drawn x1
    "........LLL..RRR........",
    "......LLLLL..RRRRR......",
    "....LLLLLLL..RRRRRRR....",
    "...LLLLLLLL..RRRRRRRR...",
    "..LLLLLLLLL..RRRRRRRRR..",
    "..LLLLLLLLL..RRRRRRRRR..",
    ".LLLLLLLLLL..RRRRRRRRRR.",
    ".LLLLLLLLLL..RRRRRRRRRR.",
    "LLLLLLkkkLL..RRkkkRRRRRR",
    "LLLLLLkkkkk..kkkkkRRRRRR",
    "LLLLLLkkkkkkkkkkkkRRRRRR",
    "LLLLLLkkkkkkkkkkkkRRRRRR",
    "LLLLLLkkkkkkkkkkkkRRRRRR",
    "LLLLLLkkkkkkkkkkkkRRRRRR",
    "LLLLLLkkkkk..kkkkkRRRRRR",
    "LLLLLLkkkLL..RRkkkRRRRRR",
    ".LLLLLLLLLL..RRRRRRRRRR.",
    ".LLLLLLLLLL..RRRRRRRRRR.",
    "..LLLLLLLLL..RRRRRRRRR..",
    "..LLLLLLLLL..RRRRRRRRR..",
    "...LLLLLLLL..RRRRRRRR...",
    "....LLLLLLL..RRRRRRR....",
    "......LLLLL..RRRRR......",
    "........LLL..RRR........",
]
DISC_30 = [  # 100x100 avatar and banner, drawn x2
    "...........LLL..RRR...........",
    "........LLLLLL..RRRRRR........",
    "......LLLLLLLL..RRRRRRRR......",
    ".....LLLLLLLLL..RRRRRRRRR.....",
    "....LLLLLLLLLL..RRRRRRRRRR....",
    "...LLLLLLLLLLL..RRRRRRRRRRR...",
    "...LLLLLLLLLLL..RRRRRRRRRRR...",
    "..LLLLLLLLLLLL..RRRRRRRRRRRR..",
    "..LLLLLLLLLLLL..RRRRRRRRRRRR..",
    ".LLLLLLLLLLLLL..RRRRRRRRRRRRR.",
    ".LLLLLLkkLLLLL..RRRRRkkRRRRRR.",
    ".LLLLLLkkkkLLL..RRRkkkkRRRRRR.",
    "LLLLLLLkkkkkkL..RkkkkkkRRRRRRR",
    "LLLLLLLkkkkkkkkkkkkkkkkRRRRRRR",
    "LLLLLLLkkkkkkkkkkkkkkkkRRRRRRR",
    "LLLLLLLkkkkkkkkkkkkkkkkRRRRRRR",
    "LLLLLLLkkkkkkkkkkkkkkkkRRRRRRR",
    "LLLLLLLkkkkkkL..RkkkkkkRRRRRRR",
    ".LLLLLLkkkkLLL..RRRkkkkRRRRRR.",
    ".LLLLLLkkLLLLL..RRRRRkkRRRRRR.",
    ".LLLLLLLLLLLLL..RRRRRRRRRRRRR.",
    "..LLLLLLLLLLLL..RRRRRRRRRRRR..",
    "..LLLLLLLLLLLL..RRRRRRRRRRRR..",
    "...LLLLLLLLLLL..RRRRRRRRRRR...",
    "...LLLLLLLLLLL..RRRRRRRRRRR...",
    "....LLLLLLLLLL..RRRRRRRRRR....",
    ".....LLLLLLLLL..RRRRRRRRR.....",
    "......LLLLLLLL..RRRRRRRR......",
    "........LLLLLL..RRRRRR........",
    "...........LLL..RRR...........",
]
DISC_40 = [  # 128x128 logo, drawn x2
    "...............LLLL..RRRR...............",
    "............LLLLLLL..RRRRRRR............",
    "..........LLLLLLLLL..RRRRRRRRR..........",
    "........LLLLLLLLLLL..RRRRRRRRRRR........",
    ".......LLLLLLLLLLLL..RRRRRRRRRRRR.......",
    "......LLLLLLLLLLLLL..RRRRRRRRRRRRR......",
    ".....LLLLLLLLLLLLLL..RRRRRRRRRRRRRR.....",
    "....LLLLLLLLLLLLLLL..RRRRRRRRRRRRRRR....",
    "....LLLLLLLLLLLLLLL..RRRRRRRRRRRRRRR....",
    "...LLLLLLLLLLLLLLLL..RRRRRRRRRRRRRRRR...",
    "...LLLLLLLLLLLLLLLL..RRRRRRRRRRRRRRRR...",
    "..LLLLLLLLLLLLLLLLL..RRRRRRRRRRRRRRRRR..",
    "..LLLLLLLLLLLLLLLLL..RRRRRRRRRRRRRRRRR..",
    "..LLLLLLLLLLLLLLLLL..RRRRRRRRRRRRRRRRR..",
    ".LLLLLLLLLkkLLLLLLL..RRRRRRRkkRRRRRRRRR.",
    ".LLLLLLLLLkkkkLLLLL..RRRRRkkkkRRRRRRRRR.",
    ".LLLLLLLLLkkkkkkLLL..RRRkkkkkkRRRRRRRRR.",
    ".LLLLLLLLLkkkkkkkkL..RkkkkkkkkRRRRRRRRR.",
    "LLLLLLLLLLkkkkkkkkkkkkkkkkkkkkRRRRRRRRRR",
    "LLLLLLLLLLkkkkkkkkkkkkkkkkkkkkRRRRRRRRRR",
    "LLLLLLLLLLkkkkkkkkkkkkkkkkkkkkRRRRRRRRRR",
    "LLLLLLLLLLkkkkkkkkkkkkkkkkkkkkRRRRRRRRRR",
    ".LLLLLLLLLkkkkkkkkL..RkkkkkkkkRRRRRRRRR.",
    ".LLLLLLLLLkkkkkkLLL..RRRkkkkkkRRRRRRRRR.",
    ".LLLLLLLLLkkkkLLLLL..RRRRRkkkkRRRRRRRRR.",
    ".LLLLLLLLLkkLLLLLLL..RRRRRRRkkRRRRRRRRR.",
    "..LLLLLLLLLLLLLLLLL..RRRRRRRRRRRRRRRRR..",
    "..LLLLLLLLLLLLLLLLL..RRRRRRRRRRRRRRRRR..",
    "..LLLLLLLLLLLLLLLLL..RRRRRRRRRRRRRRRRR..",
    "...LLLLLLLLLLLLLLLL..RRRRRRRRRRRRRRRR...",
    "...LLLLLLLLLLLLLLLL..RRRRRRRRRRRRRRRR...",
    "....LLLLLLLLLLLLLLL..RRRRRRRRRRRRRRR....",
    "....LLLLLLLLLLLLLLL..RRRRRRRRRRRRRRR....",
    ".....LLLLLLLLLLLLLL..RRRRRRRRRRRRRR.....",
    "......LLLLLLLLLLLLL..RRRRRRRRRRRRR......",
    ".......LLLLLLLLLLLL..RRRRRRRRRRRR.......",
    "........LLLLLLLLLLL..RRRRRRRRRRR........",
    "..........LLLLLLLLL..RRRRRRRRR..........",
    "............LLLLLLL..RRRRRRR............",
    "...............LLLL..RRRR...............",
]

# ---- fonts (glyph bitmaps copied from the Nytheria brand kit) -------------------------------
# plate: 2 px stroke display font, cap height 12, descenders to row 14; only the glyphs used here
PLATE_H, PLATE_CAP, PLATE_SPACE, PLATE_GAP = 15, 12, 4, 2
PLATE = {
    'N': ["####..##", "####..##", "####..##", "##.##.##", "##.##.##", "##.##.##",
          "##..####", "##..####", "##..####", "##...###", "##...###", "##...###"],
    'C': [".######.", "########", "##......", "##......", "##......", "##......",
          "##......", "##......", "##......", "##......", "########", ".######."],
    'a': ["........", "........", "........", ".######.", ".#######", "......##",
          ".#######", "########", "##....##", "##....##", "########", ".#######"],
    'e': ["........", "........", "........", ".######.", "########", "##....##",
          "########", "########", "##......", "##......", "########", ".#######"],
    'h': ["##......", "##......", "##......", "#######.", "########", "##....##",
          "##....##", "##....##", "##....##", "##....##", "##....##", "##....##"],
    'i': ["##.", "##.", "...", "##.", "##.", "##.", "##.", "##.", "##.", "##.", "###", ".##"],
    'm': ["............", "............", "............", "############", "############",
          "##...##...##", "##...##...##", "##...##...##", "##...##...##", "##...##...##",
          "##...##...##", "##...##...##"],
    'o': ["........", "........", "........", ".######.", "########", "##....##",
          "##....##", "##....##", "##....##", "##....##", "########", ".######."],
    'p': ["........", "........", "........", "#######.", "########", "##....##",
          "##....##", "##....##", "##....##", "##....##", "########", "#######.",
          "##......", "##......", "##......"],
    'r': ["......", "......", "......", "##.###", "######", "###...",
          "##....", "##....", "##....", "##....", "##....", "##...."],
    't': [".##...", ".##...", ".##...", "######", "######", ".##...",
          ".##...", ".##...", ".##...", ".##...", ".#####", "..####"],
    'y': ["........", "........", "........", "##....##", "##....##", "##....##",
          "##....##", "##....##", "##....##", "##....##", "########", ".#######",
          "......##", "########", "#######."],
}
# ledger: 5x7 text font, cell height 8 (1-row descender)
LEDGER_H, LEDGER_SPACE, LEDGER_GAP = 8, 3, 1
LEDGER = {
    'N': ["#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#", "#...#"],
    'a': [".....", ".....", ".###.", "....#", ".####", "#...#", ".####"],
    'c': [".....", ".....", ".###.", "#...#", "#....", "#...#", ".###."],
    'd': ["....#", "....#", ".##.#", "#..##", "#...#", "#...#", ".####"],
    'e': [".....", ".....", ".###.", "#...#", "#####", "#....", ".####"],
    'f': ["..##", ".#..", "####", ".#..", ".#..", ".#..", ".#.."],
    'h': ["#....", "#....", "#.##.", "##..#", "#...#", "#...#", "#...#"],
    'i': ["#", ".", "#", "#", "#", "#", "#"],
    'k': ["#...", "#...", "#..#", "#.#.", "##..", "#.#.", "#..#"],
    'm': [".....", ".....", "##.#.", "#.#.#", "#.#.#", "#...#", "#...#"],
    'o': [".....", ".....", ".###.", "#...#", "#...#", "#...#", ".###."],
    'p': [".....", ".....", "#.##.", "##..#", "#...#", "####.", "#....", "#...."],
    'r': [".....", ".....", "#.##.", "##..#", "#....", "#....", "#...."],
    't': [".#.", ".#.", "###", ".#.", ".#.", ".#.", "..#"],
    'y': [".....", ".....", "#...#", "#...#", "#...#", ".####", "....#", "####."],
}


def text_map(s, font, height, space, gap):
    """String -> map rows ('#' = ink) for a bitmap font."""
    cols = []
    for i, ch in enumerate(s):
        glyph = ['.' * space] * height if ch == ' ' else font[ch]
        glyph = glyph + ['.' * len(glyph[0])] * (height - len(glyph))
        cols.append(glyph)
        if i < len(s) - 1:
            cols.append(['.' * gap] * height)
    return [''.join(c[y] for c in cols) for y in range(height)]


# ---- native grid helpers -------------------------------------------------------------------
def grid(w, h, fill=None):
    return [[fill] * w for _ in range(h)]


def paste(g, src, ox, oy):
    for y, row in enumerate(src):
        for x, c in enumerate(row):
            if c is not None:
                g[oy + y][ox + x] = c


def to_image(g):
    im = Image.new('RGBA', (len(g[0]), len(g)))
    im.putdata([RGBA[c] for row in g for c in row])
    return im


def upscale(im, f):
    return im.resize((im.width * f, im.height * f), Image.NEAREST)


# ---- lettering / emblem renderer ------------------------------------------------------------
def render(m, s, bands, band_rows=None, extrude=0, shadow=True, seam=2):
    """Map rows enlarged x`s` (shape only) -> native grid. Shading is done on the native grid:
    stepped bands top -> bottom with `seam`-row checker seams, catch light on top edges, brass key with
    its own bands, 1 px PLo seams between parts and a 1 px PLo outline, optional PLd extrusion and a
    1 px LO drop shadow. Returns a grid with a 1 px margin left/top for the outline."""
    sm = [''.join(c * s for c in row) for row in m for _ in range(s)]
    h, w = len(sm), len(sm[0])
    W, H = w + 2, h + extrude + 3
    body = grid(W, H, '.')
    for y in range(h):
        for x in range(w):
            body[y + 1][x + 1] = sm[y][x]

    def at(x, y):
        return body[y][x] if 0 <= x < W and 0 <= y < H else '.'

    def band(y, y0, rows, ramp, x):
        n = len(ramp)
        i, rest = divmod((y - y0) * n, rows)
        if 0 < i < n and rest < seam * n and (x + y) % 2 == 0:  # first rows of a band: checker seam
            i -= 1
        return ramp[min(i, n - 1)]

    def top(x, y, part):  # top edge of a part (its upper neighbour is not that part)
        inside = at(x, y) == part if part == 'k' else at(x, y) not in '.k'
        above = at(x, y - 1) == 'k' if part == 'k' else at(x, y - 1) != '.'
        return inside and not above

    def catch_light(x, y, part):  # only inside top-edge runs of 3+ px, so stair steps stay clean
        return top(x - 1, y, part) and top(x, y, part) and top(x + 1, y, part)

    g = grid(W, H)
    ink = [y for y in range(H) if any(c not in '.k' for c in body[y])]
    y0 = ink[0]
    rows = band_rows or (ink[-1] - y0 + 1)
    key = [y for y in range(H) if 'k' in body[y]]
    for y in range(H):
        for x in range(W):
            c = body[y][x]
            if c == '.':
                continue
            if c == 'k':
                col = 'BW' if catch_light(x, y, 'k') else band(y, key[0], len(key), BRASS, x)
            else:
                col = 'PW' if y < y0 + rows and catch_light(x, y, c) else band(y, y0, rows, bands, x)
                # seam: 1 px PLo on the half's side where it touches the key or the other half
                if any(at(x + dx, y + dy) not in ('.', c) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    col = 'PLo'
            g[y][x] = col
    solid = [[body[y][x] != '.' for x in range(W)] for y in range(H)]
    for d in range(1, extrude + 1):
        for y in range(H - 1, d - 1, -1):
            for x in range(W):
                if body[y - d][x] != '.' and not solid[y][x] and g[y][x] is None:
                    g[y][x] = 'PLd'
    filled = [[g[y][x] is not None for x in range(W)] for y in range(H)]
    for y in range(H):
        for x in range(W):
            if not filled[y][x] and any(0 <= x + dx < W and 0 <= y + dy < H and filled[y + dy][x + dx]
                                        for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
                g[y][x] = 'PLo'
    if shadow:
        for y in range(H - 1, 0, -1):
            for x in range(W):
                if g[y][x] is None and g[y - 1][x] == 'PLo':
                    g[y][x] = 'LO'
    return g


# ---- backdrop: night sky, stars, hills, gold frame ------------------------------------------
def sky(w, h, stops):
    """Vertical night gradient: (row, key) stops, Bayer 4x4 ordered dither between them."""
    g = grid(w, h)
    for y in range(h):
        for (ya, ka), (yb, kb) in zip(stops, stops[1:]):
            if y < yb:
                for x in range(w):  # threshold (b + 0.5) / 16 < (y - ya) / (yb - ya), in integers
                    g[y][x] = kb if (2 * BAYER4[y % 4][x % 4] + 1) * (yb - ya) < 32 * (y - ya) else ka
                break
        else:
            g[y] = [stops[-1][1]] * w
    return g


def stars(g, plus=(), dots=()):
    for x, y in plus:
        g[y][x] = 'SH'
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            g[y + dy][x + dx] = 'I3'
    for x, y in dots:
        g[y][x] = 'LAV'


def hill(g, x, y, runs, body, rim):
    """Silhouette from (x, y): each (length, dy) run keeps its height, then the top moves dy rows.
    `rim` is a 1 px line just above the silhouette."""
    h = len(g)
    for length, dy in runs:
        for xx in range(x, min(x + length, len(g[0]))):
            for yy in range(y, h):
                g[yy][xx] = body
            g[y - 1][xx] = rim
        x += length
        y += dy


# frame rings from the outside in: (top/left colour, bottom/right colour)
RINGS = (('BO', 'BO'), ('BH', 'BB'), ('BB', 'BD'), ('BO', 'BO'))
WEDGE = ('wwhbbbdo', 'whbbbdo', 'hbbbdo', 'bbbdo', 'bbdo', 'bdo', 'do', 'o')
RINGS_S = (('BO', 'BO'), ('BH', 'BB'))
WEDGE_S = ('hbo', 'bo', 'o')
WEDGE_KEY = {'w': 'BW', 'h': 'BH', 'b': 'BB', 'd': 'BD', 'o': 'BO'}


def frame(g, rings, wedge):
    h, w = len(g), len(g[0])
    for i, (tl, br) in enumerate(rings):
        for x in range(i, w - i):
            g[h - 1 - i][x] = br
        for y in range(i, h - i):
            g[y][w - 1 - i] = br
        for x in range(i, w - 1 - i):
            g[i][x] = tl
        for y in range(i, h - 1 - i):
            g[y][i] = tl
    n = len(rings)
    for r, row in enumerate(wedge):
        for c, ch in enumerate(row):
            k = WEDGE_KEY[ch]
            g[n + r][n + c] = g[n + r][w - 1 - n - c] = k
            g[h - 1 - n - r][n + c] = g[h - 1 - n - r][w - 1 - n - c] = k
    return g


# ---- the four native images -------------------------------------------------------------------
def avatar_100():
    g = sky(100, 100, [(8, 'LO'), (24, 'L0'), (44, 'L1'), (62, 'L2'), (82, 'L3')])
    stars(g, plus=[(57, 10), (14, 33), (88, 55)], dots=[(41, 12), (26, 19), (78, 21), (10, 52), (19, 70), (91, 33)])
    hill(g, 4, 93, [(14, -1), (9, -1), (6, -1), (5, -1), (4, -1), (24, 1), (6, 1), (8, 1), (10, 1), (10, 0)], 'LO', 'L2')
    e = render(DISC_30, 2, SUNSET)
    paste(g, e, (100 - len(e[0])) // 2 + 1, 16)
    return frame(g, RINGS, WEDGE)


def logo_128():
    g = sky(128, 128, [(8, 'LO'), (30, 'L0'), (56, 'L1'), (80, 'L2'), (104, 'L3')])
    stars(g, plus=[(72, 10), (16, 40), (113, 71), (22, 96)],
          dots=[(50, 13), (32, 22), (101, 26), (12, 64), (117, 42), (106, 98), (60, 115)])
    hill(g, 4, 120, [(18, -1), (12, -1), (8, -1), (6, -1), (5, -1), (30, 1), (7, 1), (9, 1), (12, 1), (20, 0)], 'LO', 'L2')
    e = render(DISC_40, 2, SUNSET)
    paste(g, e, (128 - len(e[0])) // 2 + 1, 20)
    return frame(g, RINGS, WEDGE)


def icon_32():
    g = sky(32, 32, [(4, 'LO'), (16, 'L0'), (30, 'L1')])
    stars(g, dots=[(28, 6), (4, 25)])
    e = render(DISC_24, 1, SUNSET, shadow=False, seam=1)
    paste(g, e, (32 - len(e[0])) // 2 + 1, 3)
    return frame(g, RINGS_S, WEDGE_S)


def banner_400():
    g = sky(400, 100, [(6, 'LO'), (26, 'L0'), (50, 'L1'), (72, 'L2'), (92, 'L3')])
    stars(g, plus=[(118, 12), (232, 9), (361, 17), (69, 14), (310, 78)],
          dots=[(30, 22), (96, 31), (150, 8), (183, 20), (205, 11), (262, 16), (290, 9), (336, 10),
                (385, 31), (12, 60), (388, 66), (160, 88), (250, 84)])
    hill(g, 4, 92, [(40, -1), (30, -1), (20, -1), (14, -1), (60, 1), (16, 1), (24, 1), (36, 0)], 'L1', 'L2')
    hill(g, 150, 97, [(36, -1), (26, -1), (18, -1), (12, -1), (8, -1), (6, -1), (54, 1), (12, 1), (16, 1), (22, 1),
                      (30, 1), (20, 0)], 'LO', 'L2')
    e = render(DISC_30, 2, SUNSET)
    paste(g, e, 20, 18)
    word = render(text_map('Nytheria Compat', PLATE, PLATE_H, PLATE_SPACE, PLATE_GAP), 2, LETTERS,
                  band_rows=PLATE_CAP * 2, extrude=2)
    wx = 20 + len(e[0]) + 12
    paste(g, word, wx, 24)
    tag = text_map('for the Nytheria modpack', LEDGER, LEDGER_H, LEDGER_SPACE, LEDGER_GAP)
    tx, ty = wx + 3, 66
    for d, col in ((1, 'LO'), (0, 'LAV')):  # hard LO shadow 1 px down-right, then the text
        for y, row in enumerate(tag):
            for x, c in enumerate(row):
                if c == '#':
                    g[ty + y + d][tx + x + d] = col
    return frame(g, RINGS, WEDGE)


# ---- outputs + self-check --------------------------------------------------------------------
def outputs():
    """[(path, native grid, factor, final size)]"""
    icon = icon_32()
    return [
        (OUT / 'logo-400.png', avatar_100(), 4, (400, 400)),
        (OUT / 'logo.png', logo_128(), 4, (512, 512)),
        (OUT / 'icon-128.png', icon, 4, (128, 128)),
        (MOD_ICON, icon, 4, (128, 128)),
        (OUT / 'banner.png', banner_400(), 4, (1600, 400)),
    ]


def check(path, native, f, size):
    """Problems with a written file: size, exact integer upscale of `native`, palette colours."""
    errs = []
    im = Image.open(path).convert('RGBA')
    if im.size != size:
        errs.append(f'size {im.size} != {size}')
        return errs
    nat = to_image(native)
    if nat.size != (size[0] // f, size[1] // f):
        errs.append(f'native {nat.size} x{f} != {size}')
    px = im.load()
    small = Image.new('RGBA', (size[0] // f, size[1] // f))
    small.putdata([px[x * f, y * f] for y in range(small.height) for x in range(small.width)])
    if upscale(small, f).tobytes() != im.tobytes():
        errs.append(f'not uniform {f}x{f} blocks (not an exact integer upscale)')
    if small.tobytes() != nat.tobytes():
        errs.append('file differs from its native image')
    allowed = set(RGBA.values())
    bad = {c for _, c in im.getcolors(1 << 16) if c not in allowed}
    if bad:
        errs.append(f'{len(bad)} colours outside the palette, e.g. {sorted(bad)[:3]}')
    return errs


def preview(dirpath, outs):
    """Contact sheet: every output at final size, its native image at 1x and 2x, the icon at 32 px."""
    dirpath.mkdir(parents=True, exist_ok=True)
    nat = {p.name: to_image(g) for p, g, _, _ in outs}
    sheet = Image.new('RGBA', (1680, 1420), (40, 40, 44, 255))
    sheet.alpha_composite(upscale(nat['logo-400.png'], 4), (20, 20))
    sheet.alpha_composite(upscale(nat['logo.png'], 4), (440, 20))
    x = 980
    for name in ('logo-400.png', 'logo.png', 'icon-128.png'):
        for s in (1, 2):
            im = upscale(nat[name], s)
            sheet.alpha_composite(im, (x, 20 if s == 1 else 160))
        x += 2 * nat[name].width + 20
    sheet.alpha_composite(upscale(nat['icon-128.png'], 4), (980, 420))
    sheet.alpha_composite(nat['icon-128.png'], (1130, 420))
    light = Image.new('RGBA', (120, 72), (236, 236, 232, 255))
    light.alpha_composite(nat['icon-128.png'], (16, 20))
    light.alpha_composite(upscale(nat['icon-128.png'], 2) .resize((32, 32), Image.NEAREST), (64, 20))
    sheet.alpha_composite(light, (1180, 400))
    sheet.alpha_composite(upscale(nat['banner.png'], 4), (40, 560))
    sheet.alpha_composite(nat['banner.png'], (40, 1000))
    sheet.alpha_composite(upscale(nat['banner.png'], 2), (460, 1000))
    sheet.save(dirpath / 'contact-sheet.png')
    nat['icon-128.png'].save(dirpath / 'icon-32.png')


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument('--preview', type=Path, help='also write preview sheets into this directory (outside the repo)')
    args = ap.parse_args()
    outs = outputs()
    for path, native, f, _ in outs:
        path.parent.mkdir(parents=True, exist_ok=True)
        upscale(to_image(native), f).save(path, optimize=True)
    failed = False
    for path, native, f, size in outs:
        errs = check(path, native, f, size)
        print(f"{'FAIL' if errs else 'ok  '} {path.relative_to(ROOT).as_posix()} {size[0]}x{size[1]} "
              f"= native {size[0] // f}x{size[1] // f} x{f}" + ''.join(f'\n     {e}' for e in errs))
        failed |= bool(errs)
    if args.preview:
        preview(args.preview, outs)
    return 1 if failed else 0


if __name__ == '__main__':
    sys.exit(main())
