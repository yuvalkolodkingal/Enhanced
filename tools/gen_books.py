#!/usr/bin/env python3
"""
(3.19.2) The two books' item textures, 16 x 16, eight frames each (interpolated, 4 ticks a frame):

- darkhold.png: the Darkhold closed, as in Yuval's picture of its cover (the films): dark iron, a raised frame, two
  pairs of ring clasps each held by a little skull, a bar across the middle, black strands melted over it in spikes.
- book_of_the_vishanti.png: the Book of the Vishanti as in his picture: a book of deep blue worked all over in
  silver-blue, its heart shining, in a burst of white light whose rays turn slowly.

Run from the repository root: python3 tools/gen_books.py
"""
import math
import random

from PIL import Image

OUT = "src/main/resources/assets/enhanced/textures/item/"
FRAMES = 8


def hexc(h, a=255):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, a)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(4))


def _unused_book_shape():
    """Which pixels are cover, spine, pages and outline (the same shape as the old books)."""
    cells = {}
    for y in range(16):
        for x in range(16):
            if 1 <= y <= 14 and 2 <= x <= 12:
                cells[(x, y)] = "cover"
            elif 2 <= y <= 14 and x in (13, 14):
                cells[(x, y)] = "pages"
            elif 2 <= y <= 13 and x == 1:
                cells[(x, y)] = "spine"
    shape = dict(cells)
    for (x, y) in list(cells):
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            p = (x + dx, y + dy)
            if p not in cells and 0 <= p[0] < 16 and 0 <= p[1] < 16:
                shape[p] = "outline"
    return shape


def darkhold():
    """Closed, as in the films: a cover of dark iron, a raised frame, two pairs of ring clasps each held by a little
    skull, a bar across the middle in a ring, black strands melted over it in spikes; the skulls' eyes glow a dull red."""
    rnd = random.Random(0xDA4C)
    sheet = Image.new("RGBA", (16, 16 * FRAMES))
    cover = {(x, y) for x in range(3, 13) for y in range(1, 15)}
    pages = {(13, y) for y in range(2, 15)}
    outline = set()
    for (x, y) in cover | pages:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + dx, y + dy)
            if q not in cover and q not in pages and 0 <= q[0] < 16 and 0 <= q[1] < 16:
                outline.add(q)
    noise = {p: rnd.random() for p in cover}
    frame_lit = {(x, 2) for x in range(4, 12)} | {(4, y) for y in range(2, 14)}
    frame_dark = {(x, 13) for x in range(4, 12)} | {(11, y) for y in range(2, 14)}
    rings = {(5, 3), (6, 2), (7, 3), (6, 4), (8, 3), (9, 2), (10, 3), (9, 4),
             (5, 11), (6, 10), (7, 11), (6, 12), (8, 11), (9, 10), (10, 11), (9, 12)}
    skulls = {(7, 4), (8, 4), (7, 5), (8, 5), (7, 9), (8, 9), (7, 10), (8, 10)}
    eyes = [(7, 5), (8, 5), (7, 9), (8, 9)]
    bar = {(x, 7) for x in range(5, 11)}
    halo = {(6, 6), (9, 6), (6, 8), (9, 8)}
    strands = {(9, 0), (10, 0), (9, 1), (10, 1), (10, 2), (11, 1),  # melted over the top
               (12, 6), (11, 6), (12, 7), (13, 7), (12, 8), (11, 8), (13, 9),  # and the side
               (5, 13), (6, 13), (6, 14), (8, 13), (8, 14), (9, 14)}  # and the foot
    spikes = [(5, 15), (6, 15), (9, 15), (14, 7), (11, 0)]
    for f in range(FRAMES):
        g = 0.5 + 0.5 * math.sin(math.pi * 2 * f / FRAMES)
        px = {}
        for p in outline:
            px[p] = hexc(0x0a0a0c)
        for p in cover:
            px[p] = mix(hexc(0x3c3c43), hexc(0x222226), noise[p])
        for p in pages:
            px[p] = hexc(0x2a2a2e) if p[1] % 2 else hexc(0x404046)
        for p in frame_lit:
            px[p] = hexc(0x66666e)
        for p in frame_dark:
            px[p] = hexc(0x18181b)
        for p in halo:
            px[p] = hexc(0x50505a)
        for p in rings:
            px[p] = hexc(0xa4a4ae) if p[1] in (2, 10) else hexc(0x82828c)
        for p in bar:
            px[p] = hexc(0x8a8a94) if p[0] < 8 else hexc(0x6e6e78)
        for p in skulls:
            px[p] = hexc(0x9a948c)
        for p in eyes:
            px[p] = mix(hexc(0x1a0a0a), hexc(0xb01828), g * 0.8)
        for p in strands:
            px[p] = hexc(0x050507) if (p[0] + p[1]) % 2 else hexc(0x34343c)
        for p in spikes:
            px[p] = hexc(0x2e2e34)
        # a glint that runs down the frame
        gy = 2 + (f * 11 // FRAMES)
        px[(4, gy)] = hexc(0xa0a0aa)
        for (x, y), c in px.items():
            sheet.putpixel((x, y + 16 * f), c)
    sheet.save(OUT + "darkhold.png")


def vishanti():
    """A book of deep blue, worked all over in silver-blue, in a burst of white light."""
    rnd = random.Random(0x7155)
    sheet = Image.new("RGBA", (16, 16 * FRAMES))
    book = {(x, y) for x in range(4, 12) for y in range(3, 13)}
    pages = {(12, y) for y in range(4, 13)}
    outline = set()
    for (x, y) in book | pages:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + dx, y + dy)
            if q not in book and q not in pages:
                outline.add(q)
    noise = {p: rnd.random() for p in book}
    work = set()
    for x in range(5, 11):
        work |= {(x, 4), (x, 11)}
    for y in range(4, 12):
        work |= {(5, y), (10, y)}
    work |= {(7, 6), (8, 6), (6, 7), (9, 7), (6, 8), (9, 8), (7, 9), (8, 9), (7, 5), (8, 10)}
    core = {(7, 7), (8, 7), (7, 8), (8, 8)}
    for f in range(FRAMES):
        g = 0.5 + 0.5 * math.sin(math.pi * 2 * f / FRAMES)
        px = {}
        # the burst of light behind it: rays turning slowly
        for y in range(16):
            for x in range(16):
                dx, dy = x - 7.5, y - 7.5
                d = math.hypot(dx, dy)
                ang = math.atan2(dy, dx) + f * math.pi / 32
                ray = max(0.0, math.cos(ang * 8)) ** 3
                a = (0.35 + 0.65 * ray) * max(0.0, 1.0 - d / 8.6) * (0.8 + 0.2 * g)
                if a > 0.06:
                    px[(x, y)] = mix(hexc(0x9fd8ff, 0), hexc(0xf4fbff, 255), min(1.0, a * 1.5))
        for p in outline:
            px[p] = hexc(0x081446)
        for p in book:
            px[p] = mix(hexc(0x2a52d8), hexc(0x14308f), noise[p])
        for p in pages:
            px[p] = mix(hexc(0xdcecff), hexc(0xffffff), g)
        for p in work:
            px[p] = mix(hexc(0x5f95ff), hexc(0xb8dcff), g * (0.6 + 0.4 * ((p[0] + p[1]) % 2)))
        for p in core:
            px[p] = mix(hexc(0xcfe8ff), hexc(0xffffff), g)
        for p in ((4, 3), (11, 3), (4, 12), (11, 12)):
            px[p] = hexc(0xd8e4ff)
        for (x, y), c in px.items():
            sheet.putpixel((x, y + 16 * f), c)
    sheet.save(OUT + "book_of_the_vishanti.png")


if __name__ == "__main__":
    darkhold()
    vishanti()
