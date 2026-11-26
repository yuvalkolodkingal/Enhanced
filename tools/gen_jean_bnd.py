"""Jean Grey's "Brand New Day" look (3.19.2): her clothes redrawn after the film's street outfit.

The face and hair of the old skin are kept (they were right). What changes: a yellow hoodie with its hood up (the hat
layer), a dark purple jacket worn open over it (the jacket layer), camo trousers and black combat boots. Slim (3 px) arms.

Run from the repository root: python3 tools/gen_jean_bnd.py
"""
import random
from PIL import Image

PATH = 'src/main/resources/assets/enhanced/textures/entity/suit/phoenix_brand_new_day.png'

YELLOW = [(0xF2, 0xC2, 0x3A), (0xE3, 0xAE, 0x2A), (0xC9, 0x93, 0x1E), (0xA8, 0x76, 0x16)]
# (3.19.3) Yuval chose option A: the olive field jacket of the set photos, and the hood the same olive
GREEN = [(0x6B, 0x6A, 0x3A), (0x5C, 0x5B, 0x31), (0x4C, 0x4B, 0x28), (0x3A, 0x3A, 0x1F)]
CAMO = [(0x7A, 0x74, 0x4E), (0x5B, 0x5A, 0x38), (0x8E, 0x7C, 0x55), (0x3F, 0x42, 0x2C)]
BOOT = [(0x26, 0x24, 0x22), (0x1A, 0x19, 0x18), (0x36, 0x33, 0x2F)]
SKIN = (0xF0, 0xC3, 0xA6)
CORD = (0xF7, 0xEE, 0xD8)
ZIP = (0x2E, 0x2E, 0x18)

rnd = random.Random(19)


def fill(im, box, colors, noise=True):
    x0, y0, x1, y1 = box
    for y in range(y0, y1):
        for x in range(x0, x1):
            c = colors[0]
            if noise and rnd.random() < 0.18:
                c = colors[1]
            im.putpixel((x, y), c + (255,))


def shade_bottom(im, box, color):
    x0, y0, x1, y1 = box
    for x in range(x0, x1):
        im.putpixel((x, y1 - 1), color + (255,))


def clear(im, box):
    x0, y0, x1, y1 = box
    for y in range(y0, y1):
        for x in range(x0, x1):
            im.putpixel((x, y), (0, 0, 0, 0))


def camo(im, box):
    x0, y0, x1, y1 = box
    for y in range(y0, y1):
        for x in range(x0, x1):
            im.putpixel((x, y), CAMO[0] + (255,))
    # blotches
    for _ in range(max(2, (x1 - x0) * (y1 - y0) // 9)):
        cx, cy = rnd.randrange(x0, x1), rnd.randrange(y0, y1)
        col = rnd.choice(CAMO[1:])
        for dx, dy in [(0, 0), (1, 0), (0, 1), (-1, 0), (1, 1)]:
            if x0 <= cx + dx < x1 and y0 <= cy + dy < y1 and rnd.random() < 0.85:
                im.putpixel((cx + dx, cy + dy), col + (255,))


def leg(im, ox, oy):
    """A leg at (ox, oy) of the base layer: camo trousers, black combat boots (the bottom 4 rows)."""
    camo(im, (ox + 4, oy, ox + 12, oy + 4))          # top / bottom
    for i in range(4):
        camo(im, (ox + 4 * i, oy + 4, ox + 4 * i + 4, oy + 12))
        # the boots
        fill(im, (ox + 4 * i, oy + 12, ox + 4 * i + 4, oy + 16), BOOT)
        im.putpixel((ox + 4 * i + 1, oy + 12), BOOT[2] + (255,))
    # laces on the front of the boot
    for y in (oy + 12, oy + 14):
        im.putpixel((ox + 5, y), (0x8A, 0x84, 0x7A, 255))
        im.putpixel((ox + 6, y), (0x8A, 0x84, 0x7A, 255))
    # the cuff of the trousers over the boot
    for x in range(ox, ox + 16):
        im.putpixel((x, oy + 11), CAMO[3] + (255,))
    fill(im, (ox + 8, oy, ox + 12, oy + 4), BOOT)    # the sole (bottom face)


def body_base(im):
    """The yellow hoodie under the jacket."""
    fill(im, (20, 16, 36, 20), YELLOW)
    for i, (a, b) in enumerate([(16, 20), (20, 28), (28, 32), (32, 40)]):
        fill(im, (a, 20, b, 32), YELLOW)
    # the hoodie's pocket and drawstrings on the front (20..28)
    for x in range(21, 27):
        im.putpixel((x, 27), YELLOW[2] + (255,))
    im.putpixel((22, 21), CORD + (255,))
    im.putpixel((22, 22), CORD + (255,))
    im.putpixel((25, 21), CORD + (255,))
    im.putpixel((25, 22), CORD + (255,))
    # camo waistband showing under the hem
    for x in range(16, 40):
        im.putpixel((x, 31), CAMO[3] + (255,))


def arm_base(im, ox, oy):
    """A slim arm of the base layer: hoodie yellow, the hand bare."""
    fill(im, (ox + 4, oy, ox + 10, oy + 4), YELLOW)
    faces = [(ox, ox + 4), (ox + 4, ox + 7), (ox + 7, ox + 11), (ox + 11, ox + 14)]
    for a, b in faces:
        fill(im, (a, oy + 4, b, oy + 16), YELLOW)
        fill(im, (a, oy + 14, b, oy + 16), [SKIN, SKIN], noise=False)
    fill(im, (ox + 7, oy, ox + 10, oy + 4), [SKIN, SKIN], noise=False)  # the bottom face: the hand


def jacket(im):
    """The open dark green jacket on the jacket layer (16,32)-(40,48)."""
    ox, oy = 16, 32
    fill(im, (ox + 4, oy, ox + 20, oy + 4), GREEN)  # top and bottom
    fill(im, (ox, oy + 4, ox + 4, oy + 16), GREEN)
    fill(im, (ox + 12, oy + 4, ox + 16, oy + 16), GREEN)
    fill(im, (ox + 16, oy + 4, ox + 24, oy + 16), GREEN)
    # the front: two panels with the yellow hoodie showing between (x 23..24 of the front, 8 px wide at 20..28)
    front = (ox + 4, oy + 4, ox + 12, oy + 16)
    fill(im, front, GREEN)
    for y in range(oy + 4, oy + 16):
        for x in (ox + 7, ox + 8):
            im.putpixel((x, y), (0, 0, 0, 0))
        im.putpixel((ox + 6, y), ZIP + (255,))
        im.putpixel((ox + 9, y), ZIP + (255,))
    # collar and hem a shade darker, chest pockets
    for x in range(ox, ox + 24):
        im.putpixel((x, oy + 15), GREEN[3] + (255,))
    for x in (ox + 4, ox + 5, ox + 10, ox + 11):
        im.putpixel((x, oy + 4), GREEN[2] + (255,))
    for x in (ox + 4, ox + 5):
        im.putpixel((x, oy + 7), GREEN[3] + (255,))
    for x in (ox + 10, ox + 11):
        im.putpixel((x, oy + 7), GREEN[3] + (255,))
    # the hood hanging down the back, the jacket's olive (3.19.3), over the jacket's back (16+16..16+24)
    for y in range(oy + 4, oy + 8):
        for x in range(ox + 18, ox + 22):
            im.putpixel((x, y), GREEN[1 if y < oy + 7 else 2] + (255,))


def sleeve(im, ox, oy):
    """A slim jacket sleeve on an overlay layer; the hoodie's yellow cuff and the hand below it are left bare."""
    fill(im, (ox + 4, oy, ox + 7, oy + 4), GREEN)
    faces = [(ox, ox + 4), (ox + 4, ox + 7), (ox + 7, ox + 11), (ox + 11, ox + 14)]
    for a, b in faces:
        fill(im, (a, oy + 4, b, oy + 13), GREEN)
        for x in range(a, b):
            im.putpixel((x, oy + 12), GREEN[3] + (255,))


def hood(im):
    """The hat layer: the hood, pulled up round her face, in the jacket's olive (3.19.3: it was yellow in 3.19.2)."""
    for y in range(0, 16):
        for x in range(32, 64):
            r, g, b, a = im.getpixel((x, y))
            if a == 0:
                continue
            if (r, g, b) in YELLOW:
                # the 3.19.2 yellow hood: each of its shades to the olive of the same depth
                im.putpixel((x, y), GREEN[YELLOW.index((r, g, b))] + (255,))
                continue
            lum = (r * 3 + g * 6 + b) / 10.0
            if r > 200 and g > 150 and b < 120 and lum > 150:
                # the old gold trim round the face: the hood's light inner edge
                im.putpixel((x, y), GREEN[0] + (255,))
                continue
            if lum > 120:
                c = GREEN[0]
            elif lum > 95:
                c = GREEN[1]
            elif lum > 70:
                c = GREEN[2]
            else:
                c = GREEN[3]
            im.putpixel((x, y), c + (255,))


def hood_shade(im):
    """(3.19.3) Depth in the olive hood: its folds a shade darker, its lowest rows (the back of the neck) darker still."""
    r = random.Random(7)
    for y in range(0, 16):
        for x in range(32, 64):
            if im.getpixel((x, y))[:3] != GREEN[0] or im.getpixel((x, y))[3] == 0:
                continue
            face_row = y % 8
            c = GREEN[2] if face_row == 7 else GREEN[1] if r.random() < 0.3 else GREEN[0]
            im.putpixel((x, y), c + (255,))


def main():
    im = Image.open(PATH).convert('RGBA')
    hood(im)
    hood_shade(im)
    # base layers
    clear(im, (0, 16, 56, 32))
    leg(im, 0, 16)
    body_base(im)
    arm_base(im, 40, 16)
    clear(im, (16, 48, 48, 64))
    leg(im, 16, 48)
    arm_base(im, 32, 48)
    # overlays: no trouser overlay, the jacket and its sleeves
    clear(im, (0, 32, 56, 48))
    clear(im, (0, 48, 16, 64))
    clear(im, (48, 48, 64, 64))
    jacket(im)
    sleeve(im, 40, 32)
    sleeve(im, 48, 48)
    gloves(im)
    im.save(PATH)


def gloves(im):
    """(3.19.3) Black fingerless gloves over the hands: the fingertips show."""
    black = [(0x1E, 0x1C, 0x1B), (0x16, 0x15, 0x14)]
    for ox, oy in ((40, 16), (32, 48)):
        for a, b in [(ox, ox + 4), (ox + 4, ox + 7), (ox + 7, ox + 11), (ox + 11, ox + 14)]:
            fill(im, (a, oy + 14, b, oy + 16), black, noise=False)
            for x in range(a, b, 2):
                im.putpixel((x, oy + 15), SKIN + (255,))
        fill(im, (ox + 7, oy, ox + 10, oy + 4), black, noise=False)


if __name__ == '__main__':
    main()
