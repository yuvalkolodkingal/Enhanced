"""Captain Hydra (3.19), drawn from Captain America's own classic suit so the face, the skin and every shape are his:
the suit's blue turned to Hydra's near-black, its white to Hydra red (the stripes, the wings) and its red to black, the
star on his chest replaced by Hydra's skull and tentacles; his face and skin left exactly as they are. Also his shield:
Captain America's shield, its rings in Hydra's black and red, the star replaced by the Hydra emblem.

  textures/entity/suit/captain_hydra.png   (from captain_america_classic.png)
  textures/entity/captain/hydra_shield.png (from captain/shield.png)
Run: python3 tools/gen_captain_hydra.py"""
import colorsys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/enhanced/textures/entity"
RED = (200, 16, 46)
RED_DARK = (128, 8, 26)
BLACK = (24, 24, 30)


def classify(r, g, b):
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    if v > 0.78 and s < 0.18:
        return "white"
    if s > 0.25 and 0.52 < h < 0.75:
        return "blue"
    if s > 0.45 and (h < 0.04 or h > 0.93) and v > 0.35:
        return "red"
    return "other"


def shade(base, v, ref=0.6):
    k = max(0.45, min(1.6, v / ref))
    return tuple(max(0, min(255, int(c * k))) for c in base)


def recolour(img, white_to=RED, blue_to=BLACK, red_to=BLACK, keep=lambda x, y: False):
    out = img.copy()
    px = out.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0 or keep(x, y):
                continue
            kind = classify(r, g, b)
            v = max(r, g, b) / 255
            if kind == "blue":
                px[x, y] = shade(blue_to, v, 0.55) + (a,)
            elif kind == "white":
                px[x, y] = shade(white_to, v, 0.95) + (a,)
            elif kind == "red":
                px[x, y] = shade(red_to, v, 0.75) + (a,)
    return out


# Hydra's emblem, 8 x 8: the skull (R) and its six tentacles (r), on the suit's black (.)
EMBLEM = [
    "..RRRR..",
    ".RR..RR.",
    ".RRRRRR.",
    "..R..R..",
    ".rRRRRr.",
    "r.r..r.r",
    "r.r..r.r",
    "..r..r..",
]


def emblem(px, x0, y0, scale=1):
    for j, row in enumerate(EMBLEM):
        for i, c in enumerate(row):
            if c == ".":
                continue
            colour = RED if c == "R" else RED_DARK
            for dy in range(scale):
                for dx in range(scale):
                    px[x0 + i * scale + dx, y0 + j * scale + dy] = colour + (255,)


def skin():
    cap = Image.open(ROOT / "suit/captain_america_classic.png").convert("RGBA")
    # the face (head front, x 8-15, y 8-15) is his own: only the helmet's colours change round it
    out = recolour(cap, white_to=RED)
    px = out.load()
    # the chest (body front: x 20-27, y 20-31): the star becomes Hydra's emblem
    for y in range(20, 28):
        for x in range(20, 28):
            if classify(*cap.getpixel((x, y))[:3]) in ("white",):
                px[x, y] = shade(BLACK, 0.55, 0.55) + (255,)
    emblem(px, 20, 20)
    return out


def shield():
    cap = Image.open(ROOT / "captain/shield.png").convert("RGBA")
    # rings: red to black, white to Hydra red, the blue centre to black
    out = recolour(cap, white_to=RED, blue_to=(14, 14, 18), red_to=BLACK)
    px = out.load()
    # the star sat in the centre of the face (64 x 64 at the top left): put the emblem there, four times as big
    for y in range(16, 48):
        for x in range(16, 48):
            if classify(*cap.getpixel((x, y))[:3]) == "white" and (x - 32) ** 2 + (y - 32) ** 2 < 14 ** 2:
                px[x, y] = (14, 14, 18, 255)
    emblem(px, 16, 16, 4)
    return out


if __name__ == "__main__":
    skin().save(ROOT / "suit/captain_hydra.png")
    shield().save(ROOT / "captain/hydra_shield.png")
    print("wrote captain_hydra.png and hydra_shield.png")
