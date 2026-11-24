"""Draws Ultimate Wanda's skins (3.19): the Scarlet Witch's own skin (scarlet_witch.png: her face, its proportions, its
style) in a Sorcerer Supreme outfit. An M-shaped headdress in pale rose-white whose points sweep up and out like wings, green
eyes, deep purple lips, small gold earrings; a lavender bodice over the shoulders with a starry shimmer at the collar,
narrowing into a deep V; a crimson bustier with dark red side panels and pointed peplum tabs at the waist; a pearl string
holding a gold eye medallion between two white crescent moons; long crimson opera gloves with pale claw tips and white eye
sigils on the backs of the hands; dark crimson leggings with black shadows and pointed maroon boots with a curl at the heel;
a crimson cape that wraps the hips like an open skirt, lined in violet with a wide gold filigree border.

Two hairs: the films' red curls (scarlet_witch_ultimate.png) and dark-brown curls (scarlet_witch_ultimate_comics.png);
the cloak is scarlet_witch_ultimate_cape.png. Slim arms. Run: python3 tools/ultimate_wanda_skin.py"""
import random
import sys
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/enhanced/textures/entity/suit"
BASE = OUT / "scarlet_witch.png"

LAV = [(176, 170, 232), (160, 156, 222), (194, 190, 240)]
LAV_DARK = (128, 122, 196)
STAR = (250, 250, 255)
CRIMSON = [(196, 20, 52), (214, 34, 66), (170, 14, 44)]
DARK_RED = [(118, 10, 32), (132, 14, 38), (100, 8, 26)]
LEG = [(78, 12, 28), (88, 16, 32), (66, 10, 22)]
SHADOW = (24, 6, 12)
MAROON = [(104, 22, 42), (118, 28, 50), (88, 16, 34)]
GOLD = [(228, 182, 68), (246, 210, 104), (176, 128, 36)]
PEARL = (246, 244, 236)
MOON = (255, 255, 250)
VIOLET = [(116, 62, 176), (132, 80, 192), (98, 50, 156)]
CLAW = (238, 226, 232)
ROSE = (242, 218, 226)
ROSE_LINE = (190, 146, 166)
ROSE_HI = (255, 246, 250)
GEM = (255, 70, 100)
EYE_GREEN = (64, 196, 104)
LIPS = (92, 30, 88)
SKIN_LIGHT = (246, 212, 189)

# Dark reds of the old tiara and of the face's paint: never hair.
NOT_HAIR = {(104, 34, 36), (119, 35, 44), (136, 37, 53), (87, 36, 36), (69, 32, 29), (132, 47, 60), (255, 82, 56)}

rnd = random.Random(19)


def pick(pal):
    if not isinstance(pal, list):
        return pal
    r = rnd.random()
    return pal[0] if r < 0.6 else pal[1] if r < 0.8 else pal[2]


def is_hair(c):
    r, g, b, a = c
    if a == 0 or (r, g, b) in NOT_HAIR:
        return False
    if r > 225 and g > 170:  # skin and the face's highlights
        return False
    return r - g > 45 and g >= b - 3


def to_brown(c):
    r, g, b, a = c
    lum = (0.3 * r + 0.59 * g + 0.11 * b) / 255.0
    v = 0.25 + lum * 0.9
    return (int(92 * v), int(56 * v), int(36 * v), a)


class Skin:
    def __init__(self, w=64, h=64):
        self.im = Image.new("RGBA", (w, h), (0, 0, 0, 0))

    def px(self, x, y, c):
        c = pick(c)
        self.im.putpixel((x, y), c + (255,) if len(c) == 3 else c)

    def clear(self, x, y):
        self.im.putpixel((x, y), (0, 0, 0, 0))

    def fill(self, x0, y0, x1, y1, pal):
        for y in range(y0, y1):
            for x in range(x0, x1):
                self.px(x, y, pal)


# ---- the head: hers, with green eyes, purple lips, earrings and the M headdress ----

def head(s, brown):
    base = Image.open(BASE).convert("RGBA")
    for y in range(0, 16):
        for x in range(0, 64):
            c = base.getpixel((x, y))
            if brown and is_hair(c):
                c = to_brown(c)
            s.im.putpixel((x, y), c)
    # the eyes green, the lips purple
    for x in range(8, 16):
        for y in range(8, 16):
            if base.getpixel((x, y))[:3] == (255, 82, 56):
                s.px(x, y, EYE_GREEN)
    s.px(11, 15, LIPS)
    s.px(12, 15, LIPS)
    # small gold earrings at the bottom of each side, by the cheek
    s.px(6, 15, GOLD[1])
    s.px(17, 15, GOLD[1])
    # the old tiara goes: the hat's front and top, and its dark bits on the hat's sides
    for y in range(8, 16):
        for x in range(40, 48):
            s.clear(x, y)
    for y in range(0, 8):
        for x in range(40, 48):
            s.clear(x, y)
    for y in range(8, 16):
        for x in list(range(32, 40)) + list(range(48, 56)):
            if base.getpixel((x, y))[:3] in {(0x88, 0x25, 0x35), (0x77, 0x23, 0x2C), (0x86, 0x33, 0x32), (0xA4, 0x43, 0x33)}:
                s.clear(x, y)
    # the M headdress across the brow (the hat's front): two tall outer points, a dip, a point in the middle
    m = [
        "R..HH..R",
        "RRlRRlRR",
        ".l.GG.l.",
    ]
    for dy, row in enumerate(m):
        for dx, ch in enumerate(row):
            c = {"R": ROSE, "l": ROSE_LINE, "H": ROSE_HI, "G": GEM}.get(ch)
            if c:
                s.px(40 + dx, 8 + dy, c)
    # its wings sweep up and out: over the top's front edge and along the sides' front edges
    for x, y, c in [(40, 7, ROSE), (40, 6, ROSE_HI), (41, 7, ROSE_LINE), (47, 7, ROSE), (47, 6, ROSE_HI), (46, 7, ROSE_LINE),
                    (43, 7, ROSE_HI), (44, 7, ROSE_HI), (40, 5, ROSE), (47, 5, ROSE)]:
        s.px(x, y, c)
    for x, y, c in [(39, 8, ROSE), (38, 8, ROSE_LINE), (37, 8, ROSE), (39, 9, ROSE), (38, 9, ROSE_LINE),
                    (48, 8, ROSE), (49, 8, ROSE_LINE), (50, 8, ROSE), (48, 9, ROSE), (49, 9, ROSE_LINE)]:
        s.px(x, y, c)


def hair_strands(s, brown):
    """The hair that falls over her body in her own skin, kept over the new outfit."""
    base = Image.open(BASE).convert("RGBA")
    for y in range(16, 48):
        for x in range(16, 40):
            c = base.getpixel((x, y))
            if is_hair(c):
                s.im.putpixel((x, y), to_brown(c) if brown else c)


# ---- the body ----

def body(s):
    # top (20..28,16..20) lavender shoulders; bottom (28..36,16..20) crimson
    s.fill(20, 16, 28, 20, LAV)
    s.fill(28, 16, 36, 20, DARK_RED)
    # front (20..28, 20..32), 8 x 12
    front = [
        "LsLLLLsL",   # the collar's starry shimmer
        "LLLLLLLL",
        "LPPmmPPL",   # the pearls and the moons' row
        "dCCmmCCd",   # the cups rise, the medallion between them
        "dCCVVCCd",
        "dCCVVCCd",
        "dDCVVCDd",
        "bbbbbbbb",   # the waist
        "TtT..TtT",   # the peplum's pointed tabs
        "KgllllgK",   # the cape wraps the hips, open at the front
        "KglllxgK",
        "KgxlllgK",
    ]
    pal = {"L": LAV, "s": STAR, "P": PEARL, "m": GOLD[0], "d": DARK_RED, "C": CRIMSON, "V": LAV_DARK, "D": DARK_RED[2],
           "b": CRIMSON[2], "T": CRIMSON[1], "t": DARK_RED[0], ".": LEG, "K": CRIMSON, "g": GOLD, "l": LEG, "x": SHADOW}
    for dy, row in enumerate(front):
        for dx, ch in enumerate(row):
            s.px(20 + dx, 20 + dy, pal[ch])
    # the medallion: gold round an eye, a white crescent moon on each side
    s.px(23, 22, GOLD[1])
    s.px(24, 22, GOLD[1])
    s.px(23, 23, GOLD[0])
    s.px(24, 23, (30, 120, 70))  # the eye at its centre
    s.px(22, 22, MOON)
    s.px(25, 22, MOON)
    # sides (16..20 and 28..32) and back (32..40)
    for x0, w in ((16, 4), (28, 4), (32, 8)):
        for y in range(20, 32):
            r = y - 20
            for x in range(x0, x0 + w):
                if r < 2:
                    c = LAV
                elif r < 7:
                    c = DARK_RED if x0 != 32 else CRIMSON
                elif r == 7:
                    c = CRIMSON[2]
                elif r == 11:
                    c = GOLD
                else:
                    c = CRIMSON
                s.px(x, y, c)
    # jacket layer: the peplum flares out, and the cape's skirt round the hips (violet inside, gold at the hem)
    for x in (20, 22, 25, 27):
        s.px(x, 44, CRIMSON[1])
    for y in range(45, 48):
        for x in (20, 21, 26, 27):
            s.px(x, y, CRIMSON)
        s.px(22, y, GOLD)
        s.px(25, y, GOLD)
    for x in range(16, 20):
        for y in range(44, 48):
            s.px(x, y, GOLD if y == 47 else CRIMSON)
    for x in range(28, 32):
        for y in range(44, 48):
            s.px(x, y, GOLD if y == 47 else CRIMSON)
    for x in range(32, 40):
        for y in range(44, 48):
            s.px(x, y, GOLD if y == 47 else CRIMSON)


def arm(s, x0, y0, back_x):
    """A slim arm at (x0, y0): top and bottom, then the sides 14 wide (right 4, front 3, left 4, back 3)."""
    s.fill(x0 + 4, y0, x0 + 7, y0 + 4, LAV)        # the shoulder's top, under the bodice
    s.fill(x0 + 7, y0, x0 + 10, y0 + 4, CLAW)      # the hand's underside: pale claws
    for y in range(y0 + 4, y0 + 16):
        r = y - (y0 + 4)
        for x in range(x0, x0 + 14):
            c = LAV if r < 2 else DARK_RED[0] if r == 2 else CRIMSON
            s.px(x, y, c)
    # the gloves' top edge, and pale claw tips at the fingers
    for x in range(x0, x0 + 14):
        s.px(x, y0 + 6, DARK_RED[2])
        s.px(x, y0 + 15, CLAW if (x - x0) % 2 == 0 else CRIMSON[2])
    # a white eye sigil on the back of the hand
    s.px(back_x, y0 + 13, STAR)
    s.px(back_x + 1, y0 + 13, (40, 40, 60))
    s.px(back_x + 2, y0 + 13, STAR)
    s.px(back_x + 1, y0 + 12, STAR)
    s.px(back_x + 1, y0 + 14, STAR)


def leg(s, x0, y0, ox, oy, outer_left):
    s.fill(x0 + 4, y0, x0 + 8, y0 + 4, LEG)
    s.fill(x0 + 8, y0, x0 + 12, y0 + 4, MAROON)
    for y in range(y0 + 4, y0 + 16):
        r = y - (y0 + 4)
        for x in range(x0, x0 + 16):
            c = LEG if r < 7 else MAROON
            s.px(x, y, c)
    # heavy black shadows on the inner side of the leggings
    inner = x0 + 8 if outer_left else x0
    for y in range(y0 + 4, y0 + 11):
        s.px(inner + 1, y, SHADOW)
        s.px(inner + 2, y, SHADOW if y % 3 else LEG[2])
    front_inner = x0 + 7 if outer_left else x0 + 4
    for y in range(y0 + 5, y0 + 11):
        s.px(front_inner, y, SHADOW)
    # the pointed boots: a dark toe, and a gold curl at the heel (the back face)
    s.px(x0 + 5, y0 + 15, MAROON[2])
    s.px(x0 + 6, y0 + 15, MAROON[2])
    s.px(x0 + 13, y0 + 14, GOLD[0])
    s.px(x0 + 14, y0 + 13, GOLD[2])
    # overlay: the cape's skirt over the outer side and the outer edge of the front, violet showing at its edge
    outer = range(ox, ox + 4) if outer_left else range(ox + 8, ox + 12)
    for y in range(oy + 4, oy + 11):
        for x in outer:
            s.px(x, y, GOLD if y == oy + 10 else CRIMSON)
        s.px(ox + (4 if outer_left else 7), y, GOLD if y == oy + 10 else VIOLET)
    # and over the back
    for y in range(oy + 4, oy + 11):
        for x in range(ox + 12, ox + 16):
            s.px(x, y, GOLD if y == oy + 10 else CRIMSON)


def skin(brown):
    s = Skin()
    head(s, brown)
    body(s)
    arm(s, 40, 16, 51)       # right arm: base (40,16), its back face at 51..54
    arm(s, 32, 48, 43)       # left arm: base (32,48), its back face at 43..46
    leg(s, 0, 16, 0, 32, True)
    leg(s, 16, 48, 0, 48, False)
    hair_strands(s, brown)
    return s.im


def cape():
    s = Skin(64, 32)
    s.fill(1, 0, 11, 1, GOLD)
    s.fill(11, 0, 21, 1, VIOLET)
    for y in range(1, 17):
        for x in range(1, 11):
            # crimson with folds, a wide gold border of filigree at the sides and the hem
            if y >= 14 or x <= 1 or x >= 10:
                c = GOLD[1] if (x + y) % 3 == 0 else GOLD[0] if (x * y) % 2 == 0 else GOLD[2]
            else:
                c = DARK_RED[0] if x % 3 == 0 else CRIMSON
            s.px(x, y, c)
        for x in range(12, 22):
            s.px(x, y, VIOLET)
        s.px(0, y, GOLD[2])
        s.px(11, y, GOLD[2])
    s.px(5, 1, GOLD[1])
    s.px(6, 1, GOLD[1])
    for y in range(0, 22):
        for x in range(22, 46):
            s.px(x, y, CRIMSON if x < 34 else VIOLET)
    return s.im


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    skin(False).save(OUT / "scarlet_witch_ultimate.png")
    rnd.seed(19)
    skin(True).save(OUT / "scarlet_witch_ultimate_comics.png")
    cape().save(OUT / "scarlet_witch_ultimate_cape.png")
    print("wrote", OUT)


if __name__ == "__main__":
    sys.exit(main())
