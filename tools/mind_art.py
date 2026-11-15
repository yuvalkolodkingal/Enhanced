#!/usr/bin/env python3
"""The art of Vision's mind (3.12): skins, item and block textures, paintings. Run from the repository root; it rewrites
the files it makes. Every picture is drawn here (most from the mod's own skins, recoloured), nothing is copied."""
import colorsys
import math
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src/main/resources/assets/enhanced/textures")
SUIT = os.path.join(TEX, "entity/suit")


def save(img, *path):
    out = os.path.join(TEX, *path)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    img.save(out)


def load(name):
    return Image.open(os.path.join(SUIT, name + ".png")).convert("RGBA")


def hsv(c):
    r, g, b = (v / 255.0 for v in c[:3])
    return colorsys.rgb_to_hsv(r, g, b)


def rgb(h, s, v, a=255):
    r, g, b = colorsys.hsv_to_rgb(h % 1.0, max(0, min(1, s)), max(0, min(1, v)))
    return int(r * 255), int(g * 255), int(b * 255), a


def is_skin(c):
    h, s, v = hsv(c)
    return 0.02 < h < 0.12 and 0.2 < s < 0.6 and v > 0.55


# ---- skins ----

def nano_ultron():
    """Ultron Prime in the colours of the nanotech: copper plates, orange light."""
    im = load("ultron_prime")
    px = im.load()
    for y in range(64):
        for x in range(64):
            c = px[x, y]
            if c[3] == 0:
                continue
            h, s, v = hsv(c)
            if s > 0.45 and v > 0.6:
                px[x, y] = rgb(0.06, 0.95, 1.0, c[3])  # the light: orange
            else:
                px[x, y] = rgb(0.055, 0.55 + 0.2 * (1 - v), v * 0.9 + 0.08, c[3])  # copper
    save(im, "entity/suit/ultron_nano.png")
    save(im, "entity/mind/nano_ultron.png")


def human_torch():
    """The first android after the flames: the human Vision's face, slicked dark hair, a 1930s brown suit."""
    im = load("vision_human")
    px = im.load()
    for y in range(64):
        for x in range(64):
            c = px[x, y]
            if c[3] == 0 or is_skin(c):
                continue
            h, s, v = hsv(c)
            if y < 16:
                if not (8 <= x < 16 and 10 <= y < 16):
                    px[x, y] = rgb(0.07, 0.45, v * 0.35, c[3])  # hair: near black
            elif s < 0.35 and 0.35 < v < 0.9 and not (y < 32 and 20 <= x < 28 and v > 0.85):
                px[x, y] = rgb(0.08, 0.45, v * 0.62, c[3])  # suit: brown
    save(im, "entity/mind/human_torch.png")
    # ablaze: the same body glowing orange-red under the flames
    fire = im.copy()
    fp = fire.load()
    for y in range(64):
        for x in range(64):
            c = fp[x, y]
            if c[3]:
                h, s, v = hsv(c)
                fp[x, y] = rgb(0.03 + 0.06 * v, 0.9, 0.55 + 0.45 * v, c[3])
    save(fire, "entity/mind/human_torch_ablaze.png")


def horton():
    """Dr. Phineas Horton: Ultron's human suit and grey hair, a long grey beard and round glasses."""
    im = load("ultron_human")
    px = im.load()
    for y in range(64):
        for x in range(64):
            c = px[x, y]
            if c[3] == 0:
                continue
            h, s, v = hsv(c)
            if s > 0.5 and h < 0.05:
                px[x, y] = rgb(0.08, 0.3, 0.45, c[3])  # no red light
    d = ImageDraw.Draw(im)
    beard = (196, 196, 190, 255)
    beard_dark = (150, 150, 146, 255)
    # the face (front 8..15 x 8..15): beard from the cheeks down, a moustache, glasses
    for x in range(8, 16):
        for y in range(13, 16):
            px[x, y] = beard if (x + y) % 3 else beard_dark
    for x in (8, 9, 14, 15):
        px[x, 12] = beard
    for x in range(10, 14):
        px[x, 12] = beard_dark
    for x in (9, 10, 13, 14):
        px[x, 11] = (40, 36, 30, 255)
    px[11, 11] = (40, 36, 30, 255)
    px[12, 11] = (40, 36, 30, 255)
    # the beard over the chest's top
    for x in range(21, 27):
        for y in range(20, 23):
            px[x, y] = beard if (x * y) % 4 else beard_dark
    # the sides of the head
    for x in list(range(0, 8)) + list(range(16, 24)):
        for y in range(13, 16):
            px[x, y] = beard
    save(im, "entity/mind/horton.png")


def ai_human(name, hair, suit, trousers, shirt=None, glasses=False, long_hair=False, stripe=None):
    """An AI of the mind in her (or his) human form, as the AIs are played in the series: a person drawn here on the
    human body of the mod's own skins, hair, clothes and touches of their own."""
    im = load("ultron_human")
    px = im.load()

    def shade(c, v):
        return tuple(max(0, min(255, int(k * (0.7 + 0.5 * v)))) for k in c[:3]) + (255,)

    for y in range(64):
        for x in range(64):
            c = px[x, y]
            if c[3] == 0:
                continue
            second = (y >= 32 and y < 48) or (y >= 48 and (x < 16 or x >= 48))
            if second:
                px[x, y] = (0, 0, 0, 0)
                continue
            h, s, v = hsv(c)
            if y < 16:
                if is_skin(c) and not (y < 10 and 8 <= x < 16) and not (x < 8 or x >= 16):
                    continue
                if 8 <= x < 16 and 8 <= y < 16 and is_skin(c):
                    continue
                px[x, y] = shade(hair, v)
                continue
            if is_skin(c):
                continue
            legs = (y >= 16 and y < 32 and x < 16) or (y >= 48 and 16 <= x < 32)
            px[x, y] = shade(trousers if legs else suit, v)
            if legs and (y % 16) >= 13:
                px[x, y] = shade((34, 30, 28), v)  # shoes
    # the face, painted whole: skin, brows, eyes, a mouth
    skin = (226, 184, 152, 255)
    for x in range(8, 16):
        for y in range(10, 16):
            px[x, y] = skin
    for x in (8, 15):
        px[x, 10] = shade(hair, 0.7)
        px[x, 11] = shade(hair, 0.7) if long_hair else skin
    for x in (9, 10, 13, 14):
        px[x, 11] = shade(hair, 0.5)
    px[11, 14] = (170, 104, 96, 255)
    px[12, 14] = (170, 104, 96, 255)
    px[11, 13] = (206, 164, 134, 255)
    px[12, 13] = (206, 164, 134, 255)
    # the eyes plain, no red light
    for x in (9, 10, 13, 14):
        px[x, 12] = (250, 250, 250, 255)
    px[10, 12] = (60, 70, 90, 255)
    px[13, 12] = (60, 70, 90, 255)
    # the top of the head and its sides, all hair
    for x in range(8, 16):
        for y in range(0, 8):
            px[x, y] = shade(hair, 0.6 + 0.2 * ((x + y) % 2))
        px[x, 8] = shade(hair, 0.7)
    for x in list(range(0, 8)) + list(range(16, 32)):
        for y in range(8, 12 if not long_hair else 16):
            px[x, y] = shade(hair, 0.6 + 0.2 * ((x * y) % 2))
    if long_hair:
        # hair down the back, over the shoulders (the hat layer of the head's back and sides)
        for x in range(32, 64):
            for y in range(8, 16):
                if x >= 56 or x < 40 or y >= 14:
                    px[x, y] = shade(hair, 0.65 + 0.2 * ((x + y) % 2))
        for x in range(24, 32):
            for y in range(20, 24):
                px[x + 8, y] = shade(hair, 0.7)
    if shirt:
        for x in range(23, 25):
            for y in range(20, 26):
                px[x, y] = shirt + (255,)
    if stripe:
        for x in range(44, 48):
            px[x, 20] = stripe + (255,)
            px[x, 21] = stripe + (255,)
        for x in range(36, 40):
            px[x, 52] = stripe + (255,)
            px[x, 53] = stripe + (255,)
        for x in range(20, 28):
            px[x, 27] = stripe + (255,)
    if glasses:
        for x in (8, 11, 12, 15):
            px[x, 12] = (30, 30, 34, 255)
        for x in range(8, 16):
            px[x, 11] = (30, 30, 34, 255)
    save(im, "entity/mind/" + name + ".png")


def nano_overlay(w, h, name, seed):
    """The nanotech's threads over what it took (3.12.2): thin lines of orange and pale blue light, laid over a
    villager's, a golem's or a zombie's own look (drawn full-bright over their texture)."""
    rnd = random.Random(seed)
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = im.load()
    colours = [(255, 138, 31), (255, 170, 60), (95, 227, 255), (150, 240, 255)]
    for _ in range(w * h // 40):
        x, y = rnd.randrange(w), rnd.randrange(h)
        c = rnd.choice(colours)
        horizontal = rnd.random() < 0.5
        for i in range(rnd.randint(2, 6)):
            xx, yy = (x + i, y) if horizontal else (x, y + i)
            if 0 <= xx < w and 0 <= yy < h:
                px[xx, yy] = c + (rnd.randint(170, 230),)
        if rnd.random() < 0.3:
            px[x, y] = (255, 255, 255, 240)
    save(im, "entity/mind/" + name + ".png")


def sentry_bot():
    """The riddle robots: a Sentry's frame in workshop grey."""
    im = load("ultron_sentry")
    px = im.load()
    for y in range(64):
        for x in range(64):
            c = px[x, y]
            if c[3]:
                h, s, v = hsv(c)
                px[x, y] = rgb(0.58, 0.12 if s < 0.4 else 0.6, v, c[3])
    save(im, "entity/mind/riddle_bot.png")


def ai_orb(name, hue):
    """E.D.I.T.H.'s and F.R.I.D.A.Y.'s holograms: a glowing orb with rings (32x32, a 6-sided box of 8x8 faces)."""
    im = Image.new("RGBA", (32, 16), (0, 0, 0, 0))
    px = im.load()
    for y in range(16):
        for x in range(32):
            fx, fy = (x % 8) - 3.5, (y % 8) - 3.5
            r = math.hypot(fx, fy)
            a = max(0, 1 - r / 5.2)
            ring = 1.0 if abs(r - 2.6) < 0.6 else 0.0
            v = 0.55 + 0.45 * max(a, ring)
            px[x, y] = rgb(hue, 0.55 - 0.3 * ring, v, int(120 + 135 * max(a, ring)))
    save(im, "entity/mind/" + name + ".png")


def dum_e():
    """Dum-E: a workshop arm, worn yellow and grey (64x32)."""
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    rnd = random.Random(7)
    px = im.load()
    for y in range(32):
        for x in range(64):
            base = (214, 170, 38) if x < 40 else (92, 96, 104)
            n = rnd.randint(-14, 10)
            px[x, y] = tuple(max(0, min(255, c + n)) for c in base) + (255,)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 28, 63, 31], fill=(60, 60, 64, 255))  # the base's rim
    d.rectangle([44, 0, 47, 3], fill=(150, 210, 255, 255))  # the claw's lamp
    save(im, "entity/mind/dum_e.png")


def nano_infected_skin():
    """A person the nanotech has taken: plain clothes, grey skin, orange threads all over (64x64, player layout)."""
    im = load("vision_human")
    px = im.load()
    rnd = random.Random(13)
    for y in range(64):
        for x in range(64):
            c = px[x, y]
            if c[3] == 0:
                continue
            h, s, v = hsv(c)
            if is_skin(c):
                px[x, y] = rgb(0.08, 0.12, v * 0.8, c[3])
            elif y >= 16:
                px[x, y] = rgb(0.6 if (x // 4) % 2 else 0.1, 0.25, v * 0.75, c[3])
            if rnd.random() < 0.07:
                px[x, y] = (255, 120 + rnd.randint(0, 60), 30, c[3])
    save(im, "entity/mind/nano_infected.png")


def nano_infected_overlay():
    """The orange threads of the nanotech over a villager (64x64, the villager layout)."""
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    rnd = random.Random(11)
    d = ImageDraw.Draw(im)
    for _ in range(90):
        x, y = rnd.randrange(64), rnd.randrange(64)
        length = rnd.randint(2, 6)
        dx, dy = rnd.choice([(1, 0), (0, 1), (1, 1), (1, -1)])
        d.line([x, y, x + dx * length, y + dy * length], fill=(255, 128 + rnd.randint(0, 60), 30, 230))
    save(im, "entity/mind/nano_infected.png")


# ---- items and blocks ----

def source_code_item():
    """The Source Code: the gem of Vision's brow, gold, with circuit lines."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.polygon([(8, 1), (14, 8), (8, 15), (2, 8)], fill=(200, 150, 30, 255), outline=(110, 74, 10, 255))
    d.polygon([(8, 3), (12, 8), (8, 13), (4, 8)], fill=(255, 214, 80, 255))
    for x, y in [(8, 5), (7, 7), (9, 7), (8, 9), (6, 8), (10, 8), (8, 11)]:
        im.putpixel((x, y), (255, 250, 210, 255))
    d.line([(5, 8), (7, 8)], fill=(150, 104, 20, 255))
    d.line([(9, 8), (11, 8)], fill=(150, 104, 20, 255))
    save(im, "item/source_code.png")


def popcorn_item():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.polygon([(3, 6), (13, 6), (12, 15), (4, 15)], fill=(220, 40, 40, 255))
    for x in (5, 8, 11):
        d.line([(x, 6), (x - (1 if x < 8 else -1 if x > 8 else 0), 15)], fill=(245, 245, 240, 255))
    rnd = random.Random(3)
    for _ in range(26):
        x, y = rnd.randint(3, 12), rnd.randint(1, 6)
        im.putpixel((x, y), (255, 244, 200, 255) if rnd.random() < 0.7 else (240, 200, 90, 255))
    save(im, "item/popcorn.png")


def ultron_hat():
    """Ultron's gift: a grey fedora with a dark band. The item (16x16) and the worn hat's texture (32x16)."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.ellipse([1, 9, 15, 13], fill=(96, 96, 100, 255))
    d.rectangle([4, 4, 12, 10], fill=(120, 120, 126, 255))
    d.rectangle([4, 8, 12, 9], fill=(40, 36, 40, 255))
    d.line([(6, 4), (10, 4)], fill=(150, 150, 156, 255))
    save(im, "item/ultron_hat.png")
    tex = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    p = tex.load()
    rnd = random.Random(5)
    for y in range(32):
        for x in range(64):
            g = 112 + rnd.randint(-8, 8)
            p[x, y] = (g, g, g + 6, 255)
    d = ImageDraw.Draw(tex)
    d.rectangle([0, 10, 40, 12], fill=(40, 36, 40, 255))  # the band, around the crown
    save(tex.resize((32, 32)), "item/ultron_hat_model.png")


def block(name, draw):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    draw(im, ImageDraw.Draw(im))
    save(im, "block/" + name + ".png")


def elevator(im, d):
    d.rectangle([0, 0, 15, 15], fill=(58, 60, 66, 255))
    d.rectangle([1, 1, 14, 14], outline=(170, 150, 90, 255))
    d.polygon([(8, 3), (11, 6), (5, 6)], fill=(240, 220, 150, 255))
    d.polygon([(8, 12), (11, 9), (5, 9)], fill=(240, 220, 150, 255))


def elevator_side(im, d):
    d.rectangle([0, 0, 15, 15], fill=(58, 60, 66, 255))
    d.rectangle([1, 1, 14, 14], outline=(110, 100, 70, 255))


def projector(im, d):
    d.rectangle([0, 0, 15, 15], fill=(36, 34, 38, 255))
    d.ellipse([4, 4, 11, 11], fill=(30, 30, 30, 255), outline=(180, 180, 190, 255))
    d.ellipse([6, 6, 9, 9], fill=(250, 240, 200, 255))


def projector_side(im, d):
    d.rectangle([0, 0, 15, 15], fill=(36, 34, 38, 255))
    for y in (4, 7, 10):
        d.line([(2, y), (13, y)], fill=(70, 68, 74, 255))


def nano_storage_top(im, d):
    """The nanotech storage seen from above: a ring of spokes around a core, as the reference's console."""
    d.rectangle([0, 0, 15, 15], fill=(34, 36, 40, 255))
    for i in range(16):
        a = i * math.pi / 8
        d.line([(7.5, 7.5), (7.5 + 6.5 * math.cos(a), 7.5 + 6.5 * math.sin(a))], fill=(120, 124, 130, 255))
    d.ellipse([5, 5, 10, 10], fill=(150, 154, 160, 255))
    d.ellipse([7, 7, 8, 8], fill=(255, 150, 40, 255))


def nano_storage_side(im, d):
    d.rectangle([0, 0, 15, 15], fill=(40, 42, 46, 255))
    d.rectangle([3, 5, 12, 10], fill=(22, 24, 26, 255), outline=(90, 92, 98, 255))
    d.rectangle([6, 7, 9, 8], fill=(80, 230, 90, 255))
    d.line([(0, 2), (15, 2)], fill=(200, 200, 210, 255))


def painting_logo():
    """The cinema's picture (3.15: 112x64, a 7x4 painting filling the screen): WandaVision as its first episodes were,
    in black and white: the title, a heart, and Wanda and Vision side by side in their 1950s living room, on an old
    television's rounded screen with its scanlines and grain. Drawn here."""
    W, H = 112, 64
    im = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    d = ImageDraw.Draw(im)

    def g(v):
        v = max(0, min(255, int(v)))
        return (v, v, v, 255)

    # the room: a wall in soft light, a skirting board, a floor; a window with curtains, a lamp
    for y in range(H):
        for x in range(W):
            light = 150 - abs(x - W / 2) * 0.9 - abs(y - 26) * 0.6
            im.putpixel((x, y), g(light if y < 46 else light * 0.55))
    d.rectangle([0, 45, W, 46], fill=g(70))
    d.rectangle([12, 10, 30, 32], fill=g(190))
    d.line([(21, 10), (21, 32)], fill=g(120))
    d.line([(12, 21), (30, 21)], fill=g(120))
    d.rectangle([9, 8, 13, 36], fill=g(95))
    d.rectangle([29, 8, 33, 36], fill=g(95))
    d.polygon([(88, 22), (98, 22), (95, 14), (91, 14)], fill=g(200))
    d.line([(93, 22), (93, 44)], fill=g(60))
    d.rectangle([90, 44, 96, 45], fill=g(60))
    # the title, and a heart
    title = "WandaVision"
    font = None
    try:
        from PIL import ImageFont
        font = ImageFont.load_default()
    except Exception:
        pass
    tw = d.textlength(title, font=font) if font else 6 * len(title)
    d.text(((W - tw) / 2 + 1, 4), title, fill=g(30), font=font)
    d.text(((W - tw) / 2, 3), title, fill=g(240), font=font)
    hx, hy = 56, 24
    d.polygon([(hx - 3, hy - 1), (hx - 2, hy - 3), (hx, hy - 2), (hx + 2, hy - 3), (hx + 3, hy - 1), (hx, hy + 3)], fill=g(225))

    # Wanda (left of the heart): long dark hair, a pale face, a dress with a wide skirt
    wx, top = 44, 18
    d.ellipse([wx - 4, top, wx + 4, top + 9], fill=g(35))  # hair
    d.rectangle([wx - 5, top + 4, wx + 5, top + 15], fill=g(35))
    d.ellipse([wx - 3, top + 2, wx + 3, top + 9], fill=g(205))  # face
    d.point((wx - 1, top + 5), fill=g(40))
    d.point((wx + 1, top + 5), fill=g(40))
    d.line([(wx - 1, top + 7), (wx + 1, top + 7)], fill=g(90))
    d.rectangle([wx - 1, top + 10, wx + 1, top + 11], fill=g(200))  # neck
    d.polygon([(wx - 4, top + 12), (wx + 4, top + 12), (wx + 3, top + 19), (wx - 3, top + 19)], fill=g(125))  # bodice
    d.polygon([(wx - 3, top + 19), (wx + 3, top + 19), (wx + 8, top + 30), (wx - 8, top + 30)], fill=g(145))  # skirt
    d.line([(wx - 3, top + 19), (wx + 3, top + 19)], fill=g(60))  # her belt
    d.line([(wx - 4, top + 13), (wx - 6, top + 22)], fill=g(200), width=2)  # arm
    d.line([(wx + 4, top + 13), (wx + 7, top + 19)], fill=g(200), width=2)  # the arm in his
    d.rectangle([wx - 3, top + 30, wx - 2, top + 33], fill=g(195))  # legs
    d.rectangle([wx + 2, top + 30, wx + 3, top + 33], fill=g(195))
    d.rectangle([wx - 4, top + 33, wx - 1, top + 34], fill=g(25))
    d.rectangle([wx + 1, top + 33, wx + 4, top + 34], fill=g(25))

    # Vision (right of the heart): tall, his bald head (mid grey: red in colour), the Mind Stone pale on his brow,
    # a dark suit, a white shirt and a tie
    vx, top = 68, 15
    d.ellipse([vx - 4, top, vx + 4, top + 10], fill=g(120))  # head
    d.polygon([(vx, top + 2), (vx + 1, top + 3), (vx, top + 4), (vx - 1, top + 3)], fill=g(245))  # the stone
    d.point((vx - 2, top + 5), fill=g(250))
    d.point((vx + 2, top + 5), fill=g(250))
    d.line([(vx - 1, top + 8), (vx + 1, top + 8)], fill=g(70))
    d.line([(vx - 4, top + 3), (vx - 4, top + 8)], fill=g(95))  # the lines of his head
    d.line([(vx + 4, top + 3), (vx + 4, top + 8)], fill=g(95))
    d.rectangle([vx - 1, top + 11, vx + 1, top + 12], fill=g(115))  # neck
    d.polygon([(vx - 6, top + 13), (vx + 6, top + 13), (vx + 5, top + 28), (vx - 5, top + 28)], fill=g(45))  # jacket
    d.polygon([(vx - 2, top + 13), (vx + 2, top + 13), (vx, top + 19)], fill=g(235))  # shirt
    d.line([(vx, top + 14), (vx, top + 20)], fill=g(20))  # tie
    d.line([(vx - 6, top + 14), (vx - 8, top + 23)], fill=g(45), width=2)  # the arm to her
    d.line([(vx + 6, top + 14), (vx + 7, top + 27)], fill=g(45), width=2)
    d.rectangle([vx - 4, top + 28, vx - 1, top + 36], fill=g(40))  # trousers
    d.rectangle([vx + 1, top + 28, vx + 4, top + 36], fill=g(40))
    d.rectangle([vx - 5, top + 36, vx - 1, top + 37], fill=g(15))
    d.rectangle([vx + 1, top + 36, vx + 5, top + 37], fill=g(15))

    # the old television: scanlines, grain, a vignette and the screen's rounded black edge
    rnd = random.Random(1950)
    px = im.load()
    for y in range(H):
        for x in range(W):
            v = px[x, y][0]
            if y % 2:
                v *= 0.86
            v += rnd.randint(-10, 10)
            dx, dy = (x - W / 2) / (W / 2), (y - H / 2) / (H / 2)
            v *= max(0.0, 1.0 - 0.45 * (dx * dx + dy * dy))
            r = 7
            cx = r if x < r else (W - 1 - r if x > W - 1 - r else x)
            cy = r if y < r else (H - 1 - r if y > H - 1 - r else y)
            if (x - cx) ** 2 + (y - cy) ** 2 > r * r or x < 1 or y < 1 or x > W - 2 or y > H - 2:
                v = 8
            px[x, y] = g(v)
    save(im, "painting/mind_cinema.png")
def painting_torch():
    """The workshop's painting of the first android (64x96: a 2x3 painting at twice the pixels), after the one in
    VisionQuest: a dark figure standing in a column of fire that bursts out round his feet, red, pink and gold on a
    dark violet ground with glints of neon, in a gilt frame."""
    import math
    W, H = 64, 96
    rnd = random.Random(1939)
    px = [[(0, 0, 0)] * W for _ in range(H)]
    # the ground: dark violet, darker at the top, a little teal neon at the sides
    for y in range(H):
        for x in range(W):
            t = y / H
            base = (int(28 + 20 * t), int(16 + 8 * t), int(44 + 16 * t))
            px[y][x] = base
    for (x0, y0, x1, y1) in [(8, 30, 15, 31), (8, 30, 8, 40), (50, 58, 57, 59), (57, 50, 57, 59), (46, 22, 54, 23)]:
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                px[y][x] = (40, 190, 210)

    def add(x, y, c, a):
        if 0 <= x < W and 0 <= y < H:
            r, g, b = px[y][x]
            px[y][x] = (min(255, int(r + c[0] * a)), min(255, int(g + c[1] * a)), min(255, int(b + c[2] * a)))

    fire = [(235, 25, 35), (250, 45, 80), (255, 95, 30), (245, 170, 60), (200, 20, 45)]
    cx = 32
    # the column of fire round the figure, rising to a point above his head
    for _ in range(3400):
        y = rnd.uniform(4, 88)
        u = (y - 4) / 84.0
        spread = 4 + 14 * u ** 1.6 + 3 * math.sin(y * 0.35)
        x = cx + rnd.gauss(0, spread / 2.2)
        c = rnd.choice(fire)
        add(int(x), int(y), c, rnd.uniform(0.1, 0.32))
    # the burst at his feet, spreading out to both sides
    for _ in range(2600):
        ang = rnd.uniform(math.pi * 0.05, math.pi * 0.95)
        r = abs(rnd.gauss(0, 18))
        x = cx + math.cos(ang) * r * 1.6 * rnd.choice([-1, 1])
        y = 84 - math.sin(ang) * r * 0.55 + rnd.gauss(0, 2)
        add(int(x), int(y), rnd.choice(fire), rnd.uniform(0.12, 0.38))
    # embers
    for _ in range(90):
        x = int(rnd.uniform(4, 60))
        y = int(rnd.uniform(4, 90))
        add(x, y, rnd.choice([(255, 210, 120), (255, 120, 140)]), 0.9)
    # the figure: a dark silhouette, his outline lit by the fire
    fig = set()
    for y in range(26, 33):
        for x in range(cx - 3, cx + 3):
            if (x - cx + 0.5) ** 2 / 9 + (y - 29.5) ** 2 / 12 <= 1:
                fig.add((x, y))
    for y in range(33, 35):
        for x in range(cx - 1, cx + 1):
            fig.add((x, y))
    for y in range(35, 58):
        w = 6 if y < 40 else 5 if y < 52 else 4
        for x in range(cx - w, cx + w):
            fig.add((x, y))
    for y in range(36, 56):  # arms, a little out from the body
        for x in (cx - 8 + (y - 36) // 10, cx - 7 + (y - 36) // 10, cx + 6 - (y - 36) // 10, cx + 7 - (y - 36) // 10):
            fig.add((x, y))
    for y in range(58, 84):  # legs
        for x in range(cx - 4, cx - 1):
            fig.add((x, y))
        for x in range(cx + 1, cx + 4):
            fig.add((x, y))
    for (x, y) in fig:
        px[y][x] = (18, 10, 16)
    for (x, y) in fig:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in fig and 0 <= x + dx < W and 0 <= y + dy < H:
                add(x + dx, y + dy, (255, 150, 70), 0.55)
    im = Image.new("RGBA", (W, H))
    for y in range(H):
        for x in range(W):
            im.putpixel((x, y), px[y][x] + (255,))
    # the gilt frame
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, W - 1, H - 1], outline=(120, 86, 30, 255))
    d.rectangle([1, 1, W - 2, H - 2], outline=(214, 172, 74, 255))
    d.rectangle([2, 2, W - 3, H - 3], outline=(160, 120, 44, 255))
    save(im, "painting/human_torch.png")



# ---- the AIs as the series plays them (3.15): people painted whole, from their pictures ----

# (u, v, w, h, d) of each box of a 64x64 skin, and of its second layer
BOXES = {"head": (0, 0, 8, 8, 8), "body": (16, 16, 8, 12, 4), "rarm": (40, 16, 4, 12, 4), "larm": (32, 48, 4, 12, 4),
         "rleg": (0, 16, 4, 12, 4), "lleg": (16, 48, 4, 12, 4)}
LAYER2 = {"head": (32, 0), "body": (16, 32), "rarm": (40, 32), "larm": (48, 48), "rleg": (0, 32), "lleg": (0, 48)}


def faces(box, second=False):
    """The faces of a box: name -> (x0, y0, width, height) in the skin."""
    u, v, w, h, d = BOXES[box]
    if second:
        u, v = LAYER2[box]
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h)}


def cloth(c, x, y, grain=0.06, seed=0):
    """A colour with the weave of cloth in it (a little light and shade, the same every run)."""
    r = random.Random(x * 131 + y * 71 + seed * 7)
    f = 1.0 + (r.random() - 0.5) * 2 * grain
    return tuple(max(0, min(255, int(k * f))) for k in c[:3]) + (255,)


def paint(px, box, fn, second=False, which=None):
    """Paints a box: fn(face, x, y, w, h) -> a colour or None, for each pixel of each face (x, y from its top left)."""
    for name, (x0, y0, w, h) in faces(box, second).items():
        if which and name not in which:
            continue
        for y in range(h):
            for x in range(w):
                c = fn(name, x, y, w, h)
                if c is not None:
                    px[x0 + x, y0 + y] = c


def face_of(px, skin, hair, eyes, lips, brows=None, female=False, beard=None, fringe=1):
    """The face (head front 8x8): hair over the forehead (fringe rows), brows, eyes, nose, mouth, a beard."""
    x0, y0 = 8, 8
    brows = brows or tuple(int(k * 0.7) for k in hair[:3])
    for y in range(8):
        for x in range(8):
            px[x0 + x, y0 + y] = cloth(skin, x, y, 0.03, 3)
    for y in range(fringe):
        for x in range(8):
            px[x0 + x, y0 + y] = cloth(hair, x, y, 0.1, 5)
    for x in (0, 7):
        for y in range(fringe, 3 if female else 2):
            px[x0 + x, y0 + y] = cloth(hair, x, y, 0.1, 5)
    for x in (1, 2, 5, 6):
        px[x0 + x, y0 + 3] = brows + (255,)
    for x, iris in ((1, False), (2, True), (5, True), (6, False)):
        px[x0 + x, y0 + 4] = (eyes + (255,)) if iris else (245, 243, 238, 255)
    if female:
        px[x0 + 1, y0 + 4] = (60, 40, 36, 255)
        px[x0 + 6, y0 + 4] = (60, 40, 36, 255)
    shade = tuple(int(k * 0.88) for k in skin[:3]) + (255,)
    px[x0 + 3, y0 + 5] = shade
    px[x0 + 4, y0 + 5] = shade
    px[x0 + 3, y0 + 6] = lips + (255,)
    px[x0 + 4, y0 + 6] = lips + (255,)
    if beard:
        for y in range(5, 8):
            for x in range(8):
                if y == 5 and 2 <= x <= 5:
                    continue
                if y == 6 and 3 <= x <= 4:
                    continue
                px[x0 + x, y0 + y] = cloth(beard, x, y, 0.12, 9)
        for x in range(2, 6):
            px[x0 + x, y0 + 5] = cloth(beard, x, 5, 0.12, 9)
        px[x0 + 3, y0 + 6] = lips + (255,)
        px[x0 + 4, y0 + 6] = lips + (255,)


def hair_of(px, hair, style, skin):
    """The rest of the head: its top, sides and back, by style (short, slick, curly, long, pulled)."""
    def fn(face, x, y, w, h):
        if face in ("top",):
            return cloth(hair, x, y, 0.12 if style == "curly" else 0.08, 11)
        if face == "bottom":
            return cloth(skin, x, y, 0.03, 3)
        if face == "front":
            return None
        low = {"short": 3, "slick": 3, "curly": 4, "long": 8, "pulled": 3}[style]
        if face == "back":
            low = {"short": 6, "slick": 6, "curly": 6, "long": 8, "pulled": 8}[style]
        if y < low:
            g = 0.18 if style == "curly" else 0.09
            c = cloth(hair, x, y, g, 13)
            if style == "curly" and (x + y) % 3 == 0:
                c = tuple(min(255, int(k * 1.25)) for k in c[:3]) + (255,)
            return c
        return cloth(skin, x, y, 0.03, 3)
    paint(px, "head", fn)
    # the second layer: volume for curls and long hair, and a knot of pulled-back hair at the back
    def layer(face, x, y, w, h):
        if style == "curly" and face in ("top", "left", "right", "back") and (face == "top" or y < 4):
            return cloth(hair, x, y, 0.2, 17) if (x * 3 + y) % 4 else None
        if style == "curly" and face == "front" and y < 1:
            return cloth(hair, x, y, 0.2, 17) if x % 2 else None
        if style == "long" and face in ("back", "left", "right") and y >= 2:
            return cloth(hair, x, y, 0.1, 19)
        if style == "pulled" and face == "back" and 2 <= x <= 5 and 3 <= y <= 6:
            return cloth(tuple(int(k * 0.85) for k in hair[:3]), x, y, 0.1, 19)
        return None
    paint(px, "head", layer, second=True)


def person(name, skin, hair, style, eyes, lips, body, arms, legs, female=False, beard=None, fringe=1, extra=None):
    """A person of Vision's mind: body(face, x, y, w, h), arms(...), legs(...) give the clothes' colours; extra(px) adds touches."""
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = im.load()
    hair_of(px, hair, style, skin)
    face_of(px, skin, hair, eyes, lips, female=female, beard=beard, fringe=fringe)
    paint(px, "body", body)
    paint(px, "rarm", arms)
    paint(px, "larm", lambda f, x, y, w, h: arms(f, w - 1 - x if f in ("front", "back") else x, y, w, h))
    paint(px, "rleg", legs)
    paint(px, "lleg", legs)
    if extra:
        extra(px)
    save(im, "entity/mind/" + name + ".png")


def friday_person():
    """F.R.I.D.A.Y. (VisionQuest): auburn hair loose to the shoulders, a long brown work dress with its sleeves rolled
    to the elbow, a belt with a pouch, boots."""
    skin, hair = (232, 192, 166), (146, 62, 36)
    dress, dark, belt, shirt, boots = (112, 80, 54), (86, 60, 40), (58, 38, 24), (196, 176, 146), (52, 36, 26)

    def body(f, x, y, w, h):
        if f == "front" and y < 3 and 2 <= x <= 5:
            return cloth(shirt if abs(x - 3.5) < 3 - y else dress, x, y, 0.05, 1)
        if y in (7, 8):
            return cloth(belt, x, y, 0.05, 2) if not (f == "front" and x == 5 and y == 7) else (196, 160, 72, 255)
        if f == "front" and x in (1, 6) and y < 7:
            return cloth(dark, x, y, 0.06, 1)
        return cloth(dress, x, y, 0.06, 1)

    def arms(f, x, y, w, h):
        if y < 5:
            return cloth(dress, x, y, 0.06, 4)
        if y == 5:
            return cloth(shirt, x, y, 0.05, 4)  # the rolled sleeve
        return cloth(skin, x, y, 0.03, 3)

    def legs(f, x, y, w, h):
        if y >= 10:
            return cloth(boots, x, y, 0.05, 6)
        return cloth(dress if y < 9 else dark, x, y, 0.06, 5)

    def extra(px):
        # the pouch on her right hip
        for x in range(4, 8):
            for y in range(20, 23):
                px[x, y] = cloth((70, 46, 30), x, y, 0.06, 7)
    person("friday_human", skin, hair, "long", (70, 96, 80), (176, 102, 92), body, arms, legs, female=True, extra=extra)


def jarvis_person():
    """J.A.R.V.I.S. (VisionQuest): dark hair combed back, a grey-brown tweed suit, a white shirt and a gold tie."""
    skin, hair = (226, 190, 160), (40, 32, 28)
    tweed, lapel, shirt, tie, shoes = (126, 116, 98), (102, 94, 80), (236, 236, 230), (204, 162, 52), (44, 32, 26)

    def body(f, x, y, w, h):
        if f == "front":
            if y < 7 and 3 <= x <= 4:
                return cloth(tie, x, y, 0.08, 1) if y > 0 else cloth(shirt, x, y, 0.02, 1)
            if y < 5 and x in (2, 5):
                return cloth(shirt, x, y, 0.02, 1)
            if y < 7 and x in (1, 6):
                return cloth(lapel, x, y, 0.1, 1)
            if y >= 7 and x in (3, 4):
                return cloth(lapel, x, y, 0.1, 1)  # the jacket's front closed, its edge
        return cloth(tweed, x, y, 0.14, 1)

    def arms(f, x, y, w, h):
        if y == 11:
            return cloth(skin, x, y, 0.03, 3)
        if y == 10:
            return cloth(shirt, x, y, 0.02, 4)  # the cuff
        return cloth(tweed, x, y, 0.14, 4)

    def legs(f, x, y, w, h):
        if y >= 11:
            return cloth(shoes, x, y, 0.05, 6)
        return cloth(tweed, x, y, 0.14, 5)
    person("jarvis_human", skin, hair, "slick", (70, 60, 50), (170, 112, 100), body, arms, legs)


def jumpsuit(teal, patch, boots):
    """A mechanic's jumpsuit (Dum-E's and U's): a collar, a zip, a name patch on the chest, boots."""
    collar = tuple(int(k * 0.8) for k in teal)

    def body(f, x, y, w, h):
        if f == "front":
            if y == 0 and x in (2, 5):
                return cloth(collar, x, y, 0.05, 1)
            if x in (3, 4) and y < 8 and (x == 4):
                return cloth(collar, x, y, 0.05, 1)  # the zip
            if 5 <= x <= 6 and 2 <= y <= 3:
                return cloth(patch, x, y, 0.03, 1)
        if y == 8:
            return cloth(collar, x, y, 0.05, 1)  # the waist seam
        return cloth(teal, x, y, 0.07, 1)

    def arms(f, x, y, w, h):
        return cloth(teal, x, y, 0.07, 4) if y < 11 else cloth((224, 182, 152), x, y, 0.03, 3)

    def legs(f, x, y, w, h):
        return cloth(boots, x, y, 0.05, 6) if y >= 10 else cloth(teal, x, y, 0.07, 5)
    return body, arms, legs


def dum_e_person():
    """Dum-E (VisionQuest): broad, brown hair and a full beard, a dark teal jumpsuit with his name on it."""
    body, arms, legs = jumpsuit((50, 76, 82), (214, 216, 210), (30, 28, 26))
    person("dum_e_human", (222, 178, 148), (96, 70, 50), "short", (70, 60, 50), (166, 104, 92), body, arms, legs,
           beard=(92, 66, 46))


def u_person():
    """U (VisionQuest): young, dark curly hair, clean-shaven, the same jumpsuit as Dum-E."""
    body, arms, legs = jumpsuit((58, 86, 92), (214, 216, 210), (30, 28, 26))
    person("u_human", (230, 190, 160), (44, 34, 30), "curly", (70, 56, 46), (178, 108, 96), body, arms, legs, fringe=2)


def edith_person():
    """E.D.I.T.H. (VisionQuest): dark hair pulled back, a navy sweater with a red neckerchief and red at the cuffs, a
    grey skirt and dark tights."""
    skin, hair = (232, 196, 172), (58, 40, 32)
    navy, red, skirt, tights, shoes = (34, 44, 80), (186, 40, 40), (112, 112, 118), (42, 40, 50), (24, 22, 24)

    def body(f, x, y, w, h):
        if f == "front" and y < 3 and abs(x - 3.5) < 3.5 - y:
            return cloth(red, x, y, 0.06, 1)  # the neckerchief's knot and points
        if f == "back" and y < 2 and 2 <= x <= 5:
            return cloth(red, x, y, 0.06, 1)
        if y >= 10:
            return cloth(skirt, x, y, 0.06, 1)
        return cloth(navy, x, y, 0.08, 1)

    def arms(f, x, y, w, h):
        if y in (9, 10):
            return cloth(red if y == 9 else navy, x, y, 0.06, 4)
        if y == 11:
            return cloth(skin, x, y, 0.03, 3)
        return cloth(navy, x, y, 0.08, 4)

    def legs(f, x, y, w, h):
        if y < 4:
            return cloth(skirt, x, y, 0.06, 5)  # the pleated skirt to the knee
        if y >= 11:
            return cloth(shoes, x, y, 0.04, 6)
        return cloth(tights, x, y, 0.05, 5)
    person("edith_human", skin, hair, "pulled", (60, 48, 40), (170, 96, 92), body, arms, legs, female=True)

def main():
    nano_ultron()
    human_torch()
    horton()
    sentry_bot()
    ai_orb("edith", 0.55)
    friday_person()
    edith_person()
    dum_e_person()
    jarvis_person()
    u_person()
    ai_orb("friday", 0.93)
    dum_e()
    nano_overlay(64, 64, "nano_overlay", 12)
    nano_overlay(128, 128, "nano_overlay_golem", 13)
    nano_infected_skin()
    source_code_item()
    popcorn_item()
    ultron_hat()
    block("mind_elevator", elevator)
    block("mind_elevator_side", elevator_side)
    block("cinema_projector", projector)
    block("cinema_projector_side", projector_side)
    block("nano_storage_top", nano_storage_top)
    block("nano_storage_side", nano_storage_side)
    painting_logo()
    painting_torch()
    print("mind art written")


if __name__ == "__main__":
    main()
