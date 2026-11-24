#!/usr/bin/env python3
"""
Draws the Scarlet Witch's Sorceress Supreme look (Russell Dauterman's design for Sorcerer Supreme, 2025) on her normal
suit skin: our own pixel drawing of that costume, on the slim model.

    python3 tools/gen_sorcerer_supreme.py

Reads   textures/entity/suit/scarlet_witch.png   (the hair and the shape of the face are kept)
Writes  textures/entity/suit/scarlet_witch_sorcerer_supreme.png        ginger hair, as on the normal skin
        textures/entity/suit/scarlet_witch_sorcerer_supreme_dark.png   the same with the comic's dark auburn hair
        textures/entity/suit/scarlet_witch_sorcerer_supreme_glow.png   what is made of magic and always glows
        textures/entity/suit/scarlet_witch_sorcerer_cloak.png          the Cloak of Levitation at her waist
        textures/entity/suit/scarlet_witch_sorcerer_tiara.png          the tiara of red chaos energy
        textures/entity/suit/scarlet_witch_sorcerer_rune.png           the rune of her gloves, of chaos energy

The costume: a periwinkle top over the shoulders with a red gradient at the throat, a black and plum bodysuit with a
sweetheart neckline, the Eye of Agamotto on her chest between two crescent moons of light, dark sleeves that turn red
at the hands, red blades on the hips, plum boots with gold heels. The tiara, the Eye's light and the rune on the
outside of each hand are magic, not metal or paint: they are drawn full-bright, the runes only while she uses her magic. The Cloak of Levitation is folded and worn at the waist
(SorcererCloakLayer): its collar's two points stand at her hips and its train floats behind her.
"""
import math
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
SUIT = os.path.join(ROOT, "src/main/resources/assets/enhanced/textures/entity/suit")


def rgb(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


CLEAR = (0, 0, 0, 0)
# Whether the crown painted on the normal skin stays under the tiara of energy.
KEEP_CROWN = os.environ.get("KEEP_CROWN", "1") == "1"
P = {
    "..": CLEAR,
    # the periwinkle top
    "V1": rgb("b6b2f4"), "V2": rgb("9b96e4"), "V3": rgb("807acd"), "V4": rgb("665fb3"),
    # the red gradient on the chest's fabric, from the throat down into the periwinkle
    "RG": rgb("e83448"), "MG": rgb("c659a6"), "M2": rgb("aa78cc"),
    # the bodysuit: black with a plum sheen
    "K1": rgb("4d1c43"), "K2": rgb("35112e"), "K3": rgb("210b1d"), "K4": rgb("130610"),
    # its dark red parts
    "N1": rgb("8c1731"), "N2": rgb("671127"), "N3": rgb("470c1d"),
    # scarlet: the hands, the hip blades, the cloak
    "S1": rgb("ff5c42"), "S2": rgb("ee3029"), "S3": rgb("cf1d2b"), "S4": rgb("a51329"), "S5": rgb("780d22"),
    # gold
    "G1": rgb("fbe9ad"), "G2": rgb("efc968"), "G3": rgb("cc9a34"), "G4": rgb("91621a"),
    # light
    "WW": rgb("ffffff"), "W2": rgb("ffdcdc"), "W3": rgb("ff9ea6"),
    "RE": rgb("ff3042"),
    # her eyes
    "EG": rgb("4fc46e"), "ED": rgb("2c7a46"),
}

# The ginger shades of the normal skin's hair, and the darker reds that shade it (on the head and down the back).
GINGER = {rgb(c) for c in ("863332", "cf6d3b", "a44333", "df8f4a", "b75435", "c95532", "a64231", "df8e4a", "d46c3c",
                           "d77f43", "e8a355", "7b2a23", "d46d3c", "d77e43")}
HAIR_SHADE = {rgb(c) for c in ("682224", "77232c", "45201d", "502025", "572424")}
# The skin of the normal skin's face, and the red it is tinted toward (the comic's red gradient on her skin, from the
# hairline down to under her eyes), row by row from her brow (y 10) to her chin (y 15).
FACE_SKIN = {rgb(c) for c in ("db8e86", "e6a89a", "eebfab", "c29694", "f4cbb0", "fbe5ce", "f6d4bd")}
FACE_PINK = (232, 78, 92)
FACE_PINK_BY_ROW = (0.66, 0.6, 0.48, 0.34, 0.2, 0.08)
# The comic's hair: dark brown, auburn where the light catches it.
DARK_RAMP = [rgb(c) for c in ("1d1013", "30181a", "462121", "5e2c27", "773a30", "8e4c3a")]


def put(im, x0, y0, rows):
    """Paints rows of two-letter colour names from (x0, y0); '..' clears, '--' leaves the pixel as it is."""
    for j, row in enumerate(rows):
        for i, key in enumerate(row.split()):
            if key != "--":
                im.putpixel((x0 + i, y0 + j), P[key])


def fill(im, x0, y0, x1, y1, key):
    for y in range(y0, y1):
        for x in range(x0, x1):
            im.putpixel((x, y), P[key])


def mirror_rows(rows):
    return [" ".join(reversed(r.split())) for r in rows]


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


# ---------------------------------------------------------------------------------------------------------------- suit

def body(im, base):
    # front (20..27, 20..31)
    put(im, 20, 20, [
        "V3 V2 MG RG RG MG V2 V3",   # the red at her throat fading into the periwinkle
        "V2 V1 M2 G1 G1 M2 V1 V2",   # the Eye of Agamotto: a gold disc ...
        "V3 WW G2 RE RE G2 WW V3",   # ... its eye open, a moon of light on each side
        "K2 K1 V2 G3 G3 V2 K1 K2",   # the bodysuit's sweetheart neckline rises at both sides
        "K3 K1 K2 V3 V3 K2 K1 K3",
        "K4 N2 K2 V4 V4 K2 N2 K4",
        "K4 N2 K3 K2 K2 K3 N2 K4",
        "K4 N3 K4 K3 K3 K4 N3 K4",
        "S3 S2 N2 K3 K3 N2 S2 S3",   # the cloak's folded top at the waist
        "S4 S3 S2 N1 N1 S2 S3 S4",   # and its clasp, a V pointing down
        "K3 S4 S3 S2 S2 S3 S4 K3",
        "K4 K3 N3 S4 S4 N3 K3 K4",
    ])
    # top: the shoulders (20..27, 16..19), and the underside (28..35, 16..19)
    put(im, 20, 16, [
        "V3 V2 V2 MG MG V2 V2 V3",
        "V2 V1 MG RG RG MG V1 V2",
        "V2 V1 MG RG RG MG V1 V2",
        "V3 V2 V2 MG MG V2 V2 V3",
    ])
    fill(im, 28, 16, 36, 20, "K4")
    side = [
        "V3 V2 V2 V3",
        "V3 V2 V2 V3",
        "V4 V3 V3 V4",
        "K3 K2 K2 K3",
        "K4 K3 K3 K4",
        "K4 N3 N3 K4",
        "K4 N3 N3 K4",
        "K4 K3 K3 K4",
        "S4 S3 S3 S4",
        "S5 S4 S4 S5",
        "K4 K3 K3 K4",
        "K4 K4 K4 K4",
    ]
    put(im, 16, 20, side)
    put(im, 28, 20, side)
    # back (32..39): the periwinkle yoke, the black bodice, the fold of the cloak (her hair hangs over most of it)
    put(im, 32, 20, [
        "V3 V2 MG RG RG MG V2 V3",
        "V2 V1 V2 M2 M2 V2 V1 V2",
        "V3 V2 V2 V3 V3 V2 V2 V3",
        "K3 K2 V3 V4 V4 V3 K2 K3",
        "K4 K2 K3 K3 K3 K3 K2 K4",
        "K4 N3 K3 K4 K4 K3 N3 K4",
        "K4 N3 K4 K4 K4 K4 N3 K4",
        "K4 K3 K4 N2 N2 K4 K3 K4",
        "S3 S2 S2 S3 S3 S2 S2 S3",
        "S4 S3 S3 S4 S4 S3 S3 S4",
        "K4 K3 K3 K4 K4 K3 K3 K4",
        "K4 K4 K3 K4 K4 K3 K4 K4",
    ])
    fill(im, 16, 32, 40, 48, "..")
    # her hair over the suit: the lock on her shoulder, the hair down her back, the strands of the second layer
    for y in range(20, 32):
        for x in range(16, 40):
            c = base.getpixel((x, y))
            back = 32 <= x < 40 and y < 28
            if c in GINGER or (back and c in HAIR_SHADE):
                im.putpixel((x, y), c)
    for y in range(32, 48):
        for x in range(16, 40):
            c = base.getpixel((x, y))
            if c in GINGER:
                im.putpixel((x, y), c)
    # the second layer, over her hair too: the Eye and its moons stand a little off the chest
    put(im, 20, 37, [
        "-- -- -- G1 G2 -- -- --",
        "-- WW G2 RE RE G3 WW --",
        "-- -- -- G3 G4 -- -- --",
    ])


def arm(im, outer_x, front_x, inner_x, back_x, top_x, bottom_x, y0, over_x, flip):
    """One slim arm: the 4 wide outer and inner faces, the 3 wide front and back, rows y0..y0+11."""
    outer = [
        "V2 V1 V1 V2",   # the top's shoulder
        "V3 V2 V2 V3",
        "K2 V3 V3 K2",   # it ends in a point on the outside of the arm
        "K2 K1 V4 K3",
        "K3 K1 K2 K3",   # the dark sleeve
        "K3 K2 K2 K4",
        "N3 K2 K3 K4",
        "N2 N3 N3 K3",   # it turns red toward the hand
        "S4 S3 S3 S5",   # the red hand (its rune is light, not paint: SorcererGlowLayer draws it)
        "S3 S2 S3 S4",
        "S2 S3 S2 S3",
        "W3 W2 W3 W2",   # the fingertips fade pale
    ]
    front = [
        "V2 V1 V2",
        "V3 V2 V3",
        "K2 K1 K3",
        "K2 K1 K3",
        "K3 K1 K3",
        "K3 K2 K4",
        "N3 K2 K4",
        "N2 N2 N3",
        "S3 S2 S4",
        "S2 S2 S3",
        "S2 S1 S3",
        "W2 W3 W2",
    ]
    inner = [
        "V3 V3 V3 V4",
        "K3 V4 V4 K3",
        "K3 K3 K3 K4",
        "K4 K3 K3 K4",
        "K4 K3 K4 K4",
        "K4 K3 K4 K4",
        "K4 N3 K4 K4",
        "N3 N3 N3 K4",
        "S5 S4 S4 S5",
        "S5 S4 S5 S5",
        "S4 S4 S5 S5",
        "W3 W3 W3 W3",
    ]
    back = [
        "V3 V2 V3",
        "V4 V3 V4",
        "K3 K2 K4",
        "K3 K2 K4",
        "K4 K2 K4",
        "K4 K3 K4",
        "K4 N3 K4",
        "N3 N2 N3",
        "S4 S3 S5",
        "S3 S3 S4",
        "S3 S2 S4",
        "W3 W2 W3",
    ]
    if flip:
        outer, front, inner, back = (mirror_rows(r) for r in (outer, front, inner, back))
    put(im, outer_x, y0, outer)
    put(im, front_x, y0, front)
    put(im, inner_x, y0, inner)
    put(im, back_x, y0, back)
    put(im, top_x, y0 - 4, ["V2 V1 V2", "V1 V1 V2", "V2 V1 V2", "V3 V2 V3"])
    fill(im, bottom_x, y0 - 4, bottom_x + 3, y0, "W2")
    # nothing on the arm's second layer: the sleeve is skin-tight (her right arm's is below the arm, her left's beside it)
    over_y = y0 + 12 if y0 == 20 else y0 - 4
    fill(im, over_x, over_y, over_x + 16, over_y + 16, "..")


def leg(im, outer_x, front_x, inner_x, back_x, top_x, bottom_x, y0, over_x, over_y, flip):
    outer = [
        "K3 S3 S2 K3",   # the red blades on her hip
        "S3 S2 S3 K4",
        "K3 S3 S2 K4",
        "S4 S3 K3 K4",
        "K3 S4 K3 K4",
        "K2 K3 K4 K4",
        "K1 K2 K3 K4",   # the boot, plum on black
        "K2 K1 K3 K4",
        "K2 K2 K3 K4",
        "K1 K2 K3 K4",
        "K2 K2 K3 K4",
        "K3 K3 K4 G3",
    ]
    front = [
        "S4 K2 K2 K3",
        "S3 K1 K2 K3",
        "S4 K1 K2 K4",
        "K2 K1 K3 K4",
        "K2 K1 K2 K4",
        "K3 K2 K3 K4",
        "K1 K1 K2 K3",   # the boot's top comes to a point
        "K2 K1 K2 K4",
        "K2 K1 K3 K4",
        "K2 K1 K2 K4",
        "K2 K1 K2 K4",
        "K2 K2 K3 K4",
    ]
    inner = [
        "K3 K3 K3 K4",
        "K3 K3 K4 K4",
        "K3 K2 K3 K4",
        "K3 K3 K4 K4",
        "K4 K3 K4 K4",
        "K3 K3 K4 K4",
        "K3 K2 K3 K4",
        "K4 K3 K4 K4",
        "K3 K3 K4 K4",
        "K3 K3 K4 K4",
        "K4 K3 K4 K4",
        "K4 K4 K4 K4",
    ]
    back = [
        "K4 K3 K3 S4",
        "K4 K3 K2 S3",
        "K4 K3 K2 S4",
        "K4 K3 K2 K3",
        "K4 K3 K2 K3",
        "K4 K4 K3 K3",
        "K4 K3 K2 K2",
        "K4 K3 K2 K3",
        "K4 K3 K2 K3",
        "K4 K3 K2 K3",
        "K4 G2 G1 K3",   # the gold heel
        "K4 G3 G2 G4",
    ]
    if flip:
        outer, front, inner, back = (mirror_rows(r) for r in (outer, front, inner, back))
    put(im, outer_x, y0, outer)
    put(im, front_x, y0, front)
    put(im, inner_x, y0, inner)
    put(im, back_x, y0, back)
    fill(im, top_x, y0 - 4, top_x + 4, y0, "K3")
    put(im, bottom_x, y0 - 4, ["K3 K3 K3 K3", "K4 K4 K4 K4", "K4 K4 K4 K4", "K4 G3 G3 K4"])
    fill(im, over_x, over_y - 4, over_x + 16, over_y + 12, "..")


def head(im, keep_crown):
    # her face is the normal skin's own (8..15, 8..15): only her eyes change, from red to green
    put(im, 8, 13, [
        "-- -- ED -- -- ED -- --",
        "-- -- EG -- -- EG -- --",
    ])
    # and her skin has the comic's red gradient: red at her brow, fading down to her own skin tone at her chin
    for y in range(10, 16):
        for x in range(8, 16):
            c = im.getpixel((x, y))
            if c in FACE_SKIN:
                t = FACE_PINK_BY_ROW[y - 10]
                im.putpixel((x, y), tuple(round(c[n] + (FACE_PINK[n] - c[n]) * t) for n in range(3)) + (255,))
    if not keep_crown:
        # the hat layer's front (40..47, 8..15): the painted crown goes; the tiara is energy now (SorcererGlowLayer)
        fill(im, 40, 8, 48, 16, "..")
    # the cuff earrings, at the front edge of both sides of the hat layer (her right: x 39, her left: x 48)
    put(im, 39, 13, ["G2", "G3"])
    put(im, 48, 13, ["G2", "G3"])


def dark_hair(im):
    """The same picture with the ginger hair turned the comic's dark auburn, shade for shade."""
    lo = min(lum(c) for c in GINGER | HAIR_SHADE)
    hi = max(lum(c) for c in GINGER)

    def dark(c):
        t = max(0.0, min(1.0, (lum(c) - lo) / (hi - lo))) ** 1.1
        f = t * (len(DARK_RAMP) - 1)
        i = min(len(DARK_RAMP) - 2, int(f))
        a, b, k = DARK_RAMP[i], DARK_RAMP[i + 1], f - i
        return tuple(round(a[n] + (b[n] - a[n]) * k) for n in range(3)) + (255,)

    out = im.copy()
    for y in range(64):
        for x in range(64):
            c = im.getpixel((x, y))
            if c[3] == 0:
                continue
            on_head = y < 16
            back_hair = 32 <= x < 40 and 20 <= y < 28
            crown = 40 <= x < 48 and 8 <= y < 16 and c not in GINGER  # the painted crown stays red
            if c in GINGER or ((on_head or back_hair) and c in HAIR_SHADE and not crown):
                out.putpixel((x, y), dark(c))
    return out


def glow(im):
    """What is made of magic and always glows on the skin itself: the Eye of Agamotto's open eye and its two moons."""
    out = Image.new("RGBA", (64, 64), CLEAR)
    bright = {P["WW"], P["RE"], P["W2"], P["W3"]}
    for y in range(16, 64):
        for x in range(64):
            c = im.getpixel((x, y))
            fingertips = (y in (31, 63)) or (16 <= y < 20 and 47 <= x < 50) or (48 <= y < 52 and 39 <= x < 42)
            if c in bright and not fingertips:
                out.putpixel((x, y), c)
    return out


# --------------------------------------------------------------------------------------------------------------- cloak

SCARLET = [rgb(c) for c in ("ff6146", "f33b2d", "de2529", "c11929", "9d1227", "790d23")]
LINING = [rgb(c) for c in ("c9222e", "ab182a", "8e112a", "720e2b", "5a0d30", "47113c")]
GOLD_PALE, GOLD_MID, GOLD_DARK, GOLD_LINE = rgb("fbe9ad"), rgb("efc968"), rgb("c8922e"), rgb("91621a")
# The border's curling pattern (dark lines on pale gold), tiled along it.
CURLS = ["#..#.##.",
         ".#.#...#",
         "##..##.."]


def border(along, across, width):
    """A border pixel: `across` 0 at the cloth's edge, up to `width` - 1 inside; a dark line on both its sides."""
    if across == 0 or across == width - 1:
        return GOLD_DARK if across == 0 else GOLD_LINE
    row = CURLS[(across - 1) % len(CURLS)]
    return GOLD_DARK if row[along % len(row)] == "#" else GOLD_PALE if (along + across) % 5 else GOLD_MID


def cloth_shade(x, y, rows, ramp, lift=0.0):
    f = (0.5 + 0.5 * math.sin(x * 1.31 + 0.6) * math.cos(x * 0.43 + 1.1)
         + 0.16 * math.sin(y * 0.37 + x * 0.9) + 0.22 * y / rows - 0.1 - lift)
    return ramp[max(0, min(len(ramp) - 1, int(0.9 + f * 3.4)))]


def cloak():
    """
    64 x 64. The train: outside x 0-31, y 0-39, lining x 32-63 (across: once round her from the edge at her right hip,
    behind her, to the edge at her left hip). The folded top: outside x 0-31, y 40-49, inside x 32-63. One point of
    the collar: outside x 0-15, y 50-63 (its tip at the top), inside x 16-31.
    """
    im = Image.new("RGBA", (64, 64), CLEAR)
    # the train
    for x in range(32):
        edge = min(x, 31 - x)
        for y in range(40):
            from_hem = 39 - y
            if edge + from_hem < 2:
                continue  # its two front corners are rounded
            c_out = cloth_shade(x, y, 40, SCARLET)
            c_in = cloth_shade(x, y, 40, LINING, 0.1 - 0.6 * (1 - y / 39.0))  # darker, violet, up under the fold
            if edge < 5 and from_hem >= 5:
                c_out = border(y, edge, 5)
                c_in = GOLD_DARK if edge == 0 else c_in
            elif from_hem < 5:
                c_out = border(x, from_hem, 5) if edge >= 5 or from_hem <= edge else border(y, edge, 5)
                c_in = GOLD_DARK if from_hem == 0 or edge == 0 else c_in
            im.putpixel((x, y), c_out)
            im.putpixel((32 + x, y), c_in)
    # the folded top: the cloth turned over at her waist, its gold edge hanging lowest
    for x in range(32):
        edge = min(x, 31 - x)
        for y in range(10):
            from_hem = 9 - y
            if edge + from_hem < 1:
                continue
            c_out = cloth_shade(x * 2 + 3, y, 10, SCARLET, 0.15)
            c_in = cloth_shade(x * 2 + 3, y, 10, LINING)
            if from_hem < 3 or edge < 2:
                c_out = border(x if from_hem < 3 else y, from_hem if from_hem < 3 else edge, 3)
            im.putpixel((x, 40 + y), c_out)
            im.putpixel((32 + x, 40 + y), c_in)
    # one point of the collar (the layer shapes it): lit along its upper edge, darker toward the waist
    for r in range(14):
        for x in range(16):
            k = (0 if x < 2 else 1 if x < 6 else 2 if x < 11 else 3) + (1 if r > 9 else 0)
            im.putpixel((x, 50 + r), SCARLET[min(5, k)])
            im.putpixel((16 + x, 50 + r), LINING[min(5, 1 + k)])
    return im


TIARA = [
    "X..............X",
    "XX............XX",
    "XX............XX",
    ".XX..........XX.",
    ".XX..........XX.",
    ".XXX........XXX.",
    ".XXX........XXX.",
    ".XXXX......XXXX.",
    "..XXXX....XXXX..",
    "..XXXXX..XXXXX..",
    "...XXXXXXXXXX...",
    "....XXXXXXXX....",
    ".....XXXXXX.....",
    "......XXXX......",
    ".......XX.......",
    "................",
]


def tiara():
    """16 x 16: the tiara of red chaos energy: a solid sheet of see-through neon red, its rim a shade lighter."""
    im = Image.new("RGBA", (16, 16), CLEAR)

    def on(x, y):
        return 0 <= x < 16 and 0 <= y < 16 and TIARA[y][x] == "X"

    for y in range(16):
        for x in range(16):
            if not on(x, y):
                continue
            rim = not (on(x - 1, y) and on(x + 1, y) and on(x, y - 1) and on(x, y + 1))
            # (solid, see-through neon: filled all through, its rim a shade lighter; not white-hot, and the layer draws it
            # full-bright, so it glows in the dark)
            im.putpixel((x, y), rgb("ff7d98", 205) if y < 2 else rgb("ff4f74", 195) if rim else rgb("ff2450", 150))
    return im


def rune():
    """
    16 x 16: the rune of her gloves, made of chaos energy (see-through neon red, a pale core): an open eye under three
    moons, rays below it, on a faint round haze. One on the outside of each hand; the top of the picture is toward her
    fingertips.
    """
    im = Image.new("RGBA", (16, 16), CLEAR)
    line, core, haze = rgb("ff3c66", 235), rgb("ffc4d0", 245), rgb("ff2450", 46)

    def dots(pts, colour):
        for x, y in pts:
            im.putpixel((x, y), colour)

    for y in range(16):
        for x in range(16):
            if (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 7.6 ** 2:
                im.putpixel((x, y), haze)
    dots([(4, 2), (5, 1), (7, 1), (8, 1), (10, 1), (11, 2)], line)                 # the moons
    dots([(7, 0), (8, 0)], core)
    lid = [(2, 8), (3, 7), (4, 6), (5, 5), (6, 5), (7, 4), (8, 4), (9, 5), (10, 5), (11, 6), (12, 7), (13, 8)]
    dots(lid, line)
    dots([(x, 16 - y) for x, y in lid if 16 - y < 12], line)                       # its lower lid
    dots([(6, 7), (9, 7), (6, 8), (9, 8), (7, 6), (8, 6), (7, 9), (8, 9)], line)   # the iris
    dots([(7, 7), (8, 7), (7, 8), (8, 8)], core)                                   # the pupil
    dots([(4, 12), (4, 13), (7, 12), (7, 13), (7, 14), (8, 12), (8, 13), (8, 14), (11, 12), (11, 13)], line)  # the rays
    return im


# ---------------------------------------------------------------------------------------------------------------- main

def main():
    base = Image.open(os.path.join(SUIT, "scarlet_witch.png")).convert("RGBA")
    im = base.copy()
    body(im, base)
    # her right arm (40..53, 20..31; second layer 40..55, 32..47) and her left (32..45, 52..63; 48..63, 48..63)
    arm(im, 40, 44, 47, 51, 44, 47, 20, 40, False)
    arm(im, 39, 36, 32, 43, 36, 39, 52, 48, True)
    # her right leg (0..15, 20..31; second layer 0..15, 32..47) and her left (16..31, 52..63; 0..15, 48..63)
    leg(im, 0, 4, 8, 12, 4, 8, 20, 0, 36, False)
    leg(im, 24, 20, 16, 28, 20, 24, 52, 0, 52, True)
    head(im, keep_crown=KEEP_CROWN)
    im.save(os.path.join(SUIT, "scarlet_witch_sorcerer_supreme.png"))
    dark_hair(im).save(os.path.join(SUIT, "scarlet_witch_sorcerer_supreme_dark.png"))
    glow(im).save(os.path.join(SUIT, "scarlet_witch_sorcerer_supreme_glow.png"))
    cloak().save(os.path.join(SUIT, "scarlet_witch_sorcerer_cloak.png"))
    tiara().save(os.path.join(SUIT, "scarlet_witch_sorcerer_tiara.png"))
    rune().save(os.path.join(SUIT, "scarlet_witch_sorcerer_rune.png"))
    print("wrote 6 textures to", os.path.normpath(SUIT))


if __name__ == "__main__":
    main()
