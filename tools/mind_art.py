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

def source_code_vision():
    """White Vision, his seams turned to gold circuitry and the stone gold: Vision as his source code."""
    im = load("white_vision")
    px = im.load()
    for y in range(64):
        for x in range(64):
            c = px[x, y]
            if c[3] == 0:
                continue
            h, s, v = hsv(c)
            if s < 0.25 and v < 0.78:
                px[x, y] = rgb(0.12, 0.70, min(1.0, v + 0.35), c[3])  # the seams: gold
            elif s < 0.25:
                px[x, y] = rgb(0.13, 0.08, min(1.0, v + 0.04), c[3])  # the shell: warm pearl
            else:
                px[x, y] = rgb(0.14, 0.85, 1.0, c[3])  # the stone and the eyes: bright gold
    save(im, "entity/suit/vision_source_code.png")
    cape = load("white_vision_cape")
    cp = cape.load()
    for y in range(cape.height):
        for x in range(cape.width):
            c = cp[x, y]
            if c[3]:
                h, s, v = hsv(c)
                cp[x, y] = rgb(0.12, 0.35, v, c[3])
    save(cape, "entity/suit/vision_source_code_cape.png")


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
    """The cinema's title card (64x32: a 4x2 painting): a TV's screen, two letters and a heart, drawn here."""
    im = Image.new("RGBA", (64, 32), (24, 18, 30, 255))
    d = ImageDraw.Draw(im)
    d.rectangle([2, 2, 61, 29], outline=(230, 200, 120, 255))
    d.rectangle([4, 4, 59, 27], fill=(52, 40, 70, 255))
    # a W of two Vs, a V, in gold and red
    w = [(10, 9), (14, 22), (18, 13), (22, 22), (26, 9)]
    d.line(w, fill=(232, 52, 64, 255), width=2)
    d.line([(36, 9), (42, 22), (48, 9)], fill=(240, 210, 90, 255), width=2)
    d.polygon([(52, 17), (54, 15), (56, 17), (54, 20)], fill=(232, 52, 64, 255))
    for x in range(6, 58, 3):
        d.point((x, 25), fill=(120, 100, 150, 255))
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


def main():
    source_code_vision()
    nano_ultron()
    human_torch()
    horton()
    sentry_bot()
    ai_orb("edith", 0.55)
    ai_orb("friday", 0.93)
    dum_e()
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
