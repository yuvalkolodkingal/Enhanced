"""Draws Ultimate Wanda's rune textures (3.19): white on clear, tinted red in the game.

  textures/fx/wanda_rune_glyphs.png  8 glyphs of 64 x 64 in a row (512 x 64): the runes that ride the sky's rings and the shield
  textures/fx/wanda_rune_circle.png  512 x 512: the great sigil (rings of glyphs, a heptagram, the eye of chaos)
Run: python3 tools/gen_wanda_runes.py"""
import math
import random
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/enhanced/textures/fx"
S = 4  # drawn 4x larger, then shrunk (smooth lines)


def glow(img, radius):
    """The lines with a soft halo of their own light round them."""
    halo = img.filter(ImageFilter.GaussianBlur(radius))
    a = Image.new("RGBA", img.size)
    a.alpha_composite(halo)
    a.alpha_composite(halo)
    a.alpha_composite(img)
    return a


# Each glyph: strokes on a 0..1 square (polylines), in the spirit of chaos-magic sigils: a stave, crossing bars, hooks, eyes.
GLYPHS = [
    [[(0.5, 0.1), (0.5, 0.9)], [(0.2, 0.3), (0.5, 0.5), (0.8, 0.3)], [(0.3, 0.75), (0.7, 0.75)]],
    [[(0.25, 0.1), (0.25, 0.9)], [(0.25, 0.2), (0.75, 0.45), (0.25, 0.65)], [(0.55, 0.9), (0.8, 0.7)]],
    [[(0.2, 0.85), (0.5, 0.12), (0.8, 0.85)], [(0.32, 0.55), (0.68, 0.55)], [(0.5, 0.12), (0.5, 0.4)]],
    [[(0.5, 0.08), (0.5, 0.92)], [(0.2, 0.25), (0.8, 0.75)], [(0.8, 0.25), (0.2, 0.75)]],
    [[(0.3, 0.1), (0.3, 0.9), (0.75, 0.9)], [(0.3, 0.45), (0.7, 0.15)], [(0.3, 0.45), (0.7, 0.62)]],
    [[(0.5, 0.1), (0.85, 0.5), (0.5, 0.9), (0.15, 0.5), (0.5, 0.1)], [(0.5, 0.3), (0.5, 0.7)]],
    [[(0.2, 0.15), (0.8, 0.15)], [(0.5, 0.15), (0.5, 0.88)], [(0.25, 0.6), (0.5, 0.88), (0.75, 0.6)]],
    [[(0.28, 0.1), (0.28, 0.9)], [(0.72, 0.1), (0.72, 0.9)], [(0.28, 0.35), (0.72, 0.65)], [(0.15, 0.9), (0.85, 0.9)]],
]
EYES = {5: (0.5, 0.5, 0.06), 6: (0.5, 0.42, 0.05)}


def glyph(draw, i, x0, y0, size, width):
    for line in GLYPHS[i]:
        pts = [(x0 + px * size, y0 + py * size) for px, py in line]
        draw.line(pts, fill=(255, 255, 255, 255), width=width, joint="curve")
    if i in EYES:
        cx, cy, r = EYES[i]
        draw.ellipse([x0 + (cx - r) * size, y0 + (cy - r) * size, x0 + (cx + r) * size, y0 + (cy + r) * size], fill=(255, 255, 255, 255))


def glyph_sheet():
    n, cell = len(GLYPHS), 64 * S
    img = Image.new("RGBA", (n * cell, cell))
    d = ImageDraw.Draw(img)
    for i in range(n):
        glyph(d, i, i * cell + cell * 0.12, cell * 0.12, cell * 0.76, 5 * S)
    img = glow(img, 3 * S)
    return img.resize((n * 64, 64), Image.LANCZOS)


def ring_of_glyphs(d, c, r, size, count, width, rng):
    for k in range(count):
        a = 2 * math.pi * k / count
        x, y = c + math.cos(a) * r, c + math.sin(a) * r
        g = Image.new("RGBA", (int(size), int(size)))
        glyph(ImageDraw.Draw(g), rng.randrange(len(GLYPHS)), 0, 0, size, width)
        g = g.rotate(-math.degrees(a) - 90, resample=Image.BICUBIC)
        d.alpha_composite(g, (int(x - size / 2), int(y - size / 2)))


def circle():
    size = 512 * S
    c = size / 2
    rng = random.Random(616)
    img = Image.new("RGBA", (size, size))
    d = ImageDraw.Draw(img)
    w = 3 * S
    for r in (0.49, 0.46, 0.355, 0.33):
        d.ellipse([c - r * size, c - r * size, c + r * size, c + r * size], outline=(255, 255, 255, 255), width=w)
    ring_of_glyphs(img, c, 0.405 * size, 0.07 * size, 28, 3 * S, rng)
    # the heptagram
    pts = [(c + math.cos(-math.pi / 2 + 2 * math.pi * k / 7) * 0.33 * size, c + math.sin(-math.pi / 2 + 2 * math.pi * k / 7) * 0.33 * size)
           for k in range(7)]
    order = [pts[(k * 3) % 7] for k in range(8)]
    d.line(order, fill=(255, 255, 255, 255), width=w, joint="curve")
    for r in (0.15, 0.13):
        d.ellipse([c - r * size, c - r * size, c + r * size, c + r * size], outline=(255, 255, 255, 255), width=w)
    ring_of_glyphs(img, c, 0.21 * size, 0.045 * size, 14, 2 * S, rng)
    # the eye of chaos
    e = 0.09 * size
    d.chord([c - e, c - e * 0.55, c + e, c + e * 0.55], 0, 360, outline=(255, 255, 255, 255), width=w)
    d.ellipse([c - e * 0.32, c - e * 0.32, c + e * 0.32, c + e * 0.32], fill=(255, 255, 255, 255))
    for k in range(48):  # the outer ticks
        a = 2 * math.pi * k / 48
        r0, r1 = 0.46 * size, (0.49 if k % 4 == 0 else 0.475) * size
        d.line([(c + math.cos(a) * r0, c + math.sin(a) * r0), (c + math.cos(a) * r1, c + math.sin(a) * r1)], fill=(255, 255, 255, 255), width=w)
    img = glow(img, 4 * S)
    return img.resize((512, 512), Image.LANCZOS)


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    glyph_sheet().save(OUT / "wanda_rune_glyphs.png")
    circle().save(OUT / "wanda_rune_circle.png")
    print("wrote", OUT / "wanda_rune_glyphs.png", OUT / "wanda_rune_circle.png")
