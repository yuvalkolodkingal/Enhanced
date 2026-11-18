"""Paints 3.17's Ultimate Mode suits (run from anywhere; writes into the mod's textures):
the Nexus Being (Wanda: her film face and crown untouched, a sleek red suit with black), the Demiurge at rest and
powered (Billy: his Teen face; the body of stars), the Nexus cape and the Demiurge's cloth cloak."""
import math, random, sys
from PIL import Image

import os
SUIT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'enhanced', 'textures', 'entity', 'suit', '')

def hx(h): return ((h >> 16) & 255, (h >> 8) & 255, h & 255)
def mix(a, b, t): return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))
def mul(c, k): return tuple(max(0, min(255, int(round(v * k)))) for v in c)
def add(c, d): return tuple(max(0, min(255, c[i] + d[i])) for i in range(3))

def boxes(x, y, w, h, d):
    """The six faces of a part whose texture starts at (x, y): w across, h tall, d deep."""
    return dict(top=(x + d, y, x + d + w, y + d), bottom=(x + d + w, y, x + d + 2 * w, y + d),
                right=(x, y + d, x + d, y + d + h), front=(x + d, y + d, x + d + w, y + d + h),
                left=(x + d + w, y + d, x + 2 * d + w, y + d + h), back=(x + 2 * d + w, y + d, x + 2 * d + 2 * w, y + d + h))

def parts(slim):
    aw = 3 if slim else 4
    return dict(head=boxes(0, 0, 8, 8, 8), hat=boxes(32, 0, 8, 8, 8),
                body=boxes(16, 16, 8, 12, 4), jacket=boxes(16, 32, 8, 12, 4),
                rarm=boxes(40, 16, aw, 12, 4), rsleeve=boxes(40, 32, aw, 12, 4),
                larm=boxes(32, 48, aw, 12, 4), lsleeve=boxes(48, 48, aw, 12, 4),
                rleg=boxes(0, 16, 4, 12, 4), rpant=boxes(0, 32, 4, 12, 4),
                lleg=boxes(16, 48, 4, 12, 4), lpant=boxes(0, 48, 4, 12, 4))

FACE_LIGHT = dict(front=1.0, right=0.88, left=0.88, back=0.84, top=1.06, bottom=0.66)

def paint(im, faces, fn, which=('front', 'back', 'left', 'right', 'top', 'bottom')):
    """fn(face, x, y, w, h) -> (r, g, b) or (r, g, b, a) or None (leave)."""
    for name in which:
        x0, y0, x1, y1 = faces[name]
        w, h = x1 - x0, y1 - y0
        for y in range(h):
            for x in range(w):
                c = fn(name, x, y, w, h)
                if c is None:
                    continue
                if len(c) == 3:
                    c = c + (255,)
                im.putpixel((x0 + x, y0 + y), c)

def rows(im, box, lines, pal, light=1.0):
    """Paints a face from text rows: one char per pixel, looked up in pal ('.' leaves the pixel)."""
    x0, y0, x1, y1 = box
    for y, line in enumerate(lines):
        cells = line.split()
        assert len(cells) == x1 - x0, (line, x1 - x0)
        for x, ch in enumerate(cells):
            if ch == '.':
                continue
            if ch == '_':
                im.putpixel((x0 + x, y0 + y), (0, 0, 0, 0))
                continue
            im.putpixel((x0 + x, y0 + y), mul(pal[ch], light) + (255,))

def round_shade(x, w, strength=0.16):
    """Darker toward the left and right edges of a face (the limb is round), a touch brighter left of the middle."""
    t = (x + 0.5) / w * 2 - 1
    return 1.0 - strength * t * t + 0.05 * max(0.0, 1 - abs(t + 0.35) * 2.2)

def vnoise(seed):
    r = random.Random(seed)
    g = [[r.random() for _ in range(40)] for _ in range(40)]
    def f(x, y):
        xi, yi = int(math.floor(x)) % 39, int(math.floor(y)) % 39
        fx, fy = x - math.floor(x), y - math.floor(y)
        fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
        a = g[yi][xi] * (1 - fx) + g[yi][xi + 1] * fx
        b = g[yi + 1][xi] * (1 - fx) + g[yi + 1][xi + 1] * fx
        return a * (1 - fy) + b * fy
    return f

# =====================================================================================================================
# The Nexus Being
def nexus(crown):
    src = Image.open(SUIT + 'scarlet_witch.png').convert('RGBA')
    im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    P = parts(True)
    # her head, both layers, exactly as the film Wanda's (the face, the hair, her own crown)
    im.paste(src.crop((0, 0, 64, 16)), (0, 0))
    if not crown:
        # (she does not wear her crown: its strokes over the brow give way to her hair and skin)
        HAIR = [hx(0xA64231), hx(0xB75435), hx(0x863332), hx(0xC95532)]
        fx0, fy0 = 8, 8
        face_rows = ['a b a c c a b a', 'a c b a a b c a', 'b a c a a c a b']
        hair = dict(a=HAIR[0], b=HAIR[2], c=HAIR[1])
        for y, line in enumerate(face_rows):
            for x, ch in enumerate(line.split()):
                im.putpixel((fx0 + x, fy0 + y), hair[ch] + (255,))
        im.putpixel((8, 11), hx(0xA64231) + (255,)); im.putpixel((15, 11), hx(0xA64231) + (255,))
        im.putpixel((8, 12), hx(0xA64231) + (255,)); im.putpixel((15, 12), hx(0x863332) + (255,))
        im.putpixel((8, 13), hx(0x863332) + (255,)); im.putpixel((15, 13), hx(0x863332) + (255,))
        # the hat layer's front: the crown's horns and band go, a little hair stays
        for y in range(8, 16):
            for x in range(40, 48):
                im.putpixel((x, y), (0, 0, 0, 0))
        for x, c in ((40, 0xA64231), (42, 0xB75435), (45, 0xB75435), (47, 0xA64231)):
            im.putpixel((x, 8), hx(c) + (255,))
    pal = dict(H=hx(0xFF5A52), R=hx(0xE0222E), r=hx(0xBE1823), d=hx(0x94101B), D=hx(0x690B14),
               K=hx(0x15121A), k=hx(0x2A2531), g=hx(0x413A4A), S=hx(0xF6D4BD), s=hx(0xE9BBA6), w=hx(0xFFB0A8))
    # ---- the body: a sleek red suit, an hourglass of red between black flanks, a deep neckline
    rows(im, P['body']['front'], [
        'R R S S S S R R',
        'R H R S S R H R',
        'K R R d d R R K',
        'K r H R R H r K',
        'K K r H R r K K',
        'K K d H R d K K',
        'K K K r r K K K',
        'K K d R R d K K',
        'K d R R R R d K',
        'd R R H H R R d',
        'r R R R R R R r',
        'd r R d d R r d'], pal)
    rows(im, P['body']['back'], [
        'R R R R R R R R',
        'R H R R R R H R',
        'K R R r r R R K',
        'K r R r r R r K',
        'K K r R R r K K',
        'K K d r r d K K',
        'K K K d d K K K',
        'K K d r r d K K',
        'K d R r r R d K',
        'd R R R R R R d',
        'r R H R R H R r',
        'd r R d d R r d'], pal, 0.9)
    for side in ('right', 'left'):
        rows(im, P['body'][side], [
            'r R R r', 'r R R r', 'K r r K', 'K K K K', 'K K K K', 'K K K K',
            'K K K K', 'K K K K', 'K K K K', 'd K K d', 'r d d r', 'd r r d'], pal, 0.88)
    paint(im, P['body'], lambda f, x, y, w, h: mul(pal['R'], 0.98 if f == 'top' else 0.6), ('top', 'bottom'))
    for x in (3, 4):  # the neck's skin on top of the body
        for y in range(1, 3):
            im.putpixel((P['body']['top'][0] + x, P['body']['top'][1] + y), pal['S'] + (255,))
    # her long hair down her back and over her shoulder, as on the film suit
    hairset = {hx(c) for c in (0xA64231, 0xC95532, 0xDF8F4A, 0xE8A355, 0xCF6D3B, 0xB75435, 0x863332, 0xA44333, 0xD46D3C, 0xD77E43)}
    def keep_hair(box, cols, rws):
        x0, y0, x1, y1 = box
        for y in rws:
            for x in cols:
                p = src.getpixel((x0 + x, y0 + y))
                if p[3] and p[:3] in hairset:
                    im.putpixel((x0 + x, y0 + y), p)
    keep_hair(P['body']['back'], range(8), range(0, 8))
    keep_hair(P['body']['front'], range(0, 2), range(0, 7))
    keep_hair(P['body']['right'], range(0, 1), range(0, 7))
    keep_hair(P['body']['left'], range(3, 4), range(0, 7))
    for f in ('front', 'back'):
        keep_hair(P['jacket'][f], range(8), range(0, 9))
    # ---- the arms: red sleeves from a black band under the shoulder, long pointed gauntlets of darker red
    def arm(face, x, y, w, h, mirror):
        if face in ('top',):
            return mul(pal['R'], 1.04)
        if face == 'bottom':
            return mul(pal['D'], 0.9)
        xx = (w - 1 - x) if mirror else x
        k = FACE_LIGHT[face] * round_shade(x, w, 0.2)
        if y <= 1:
            c = pal['R']
            if face == 'front' and y == 0 and xx == 1:
                c = pal['H']
        elif y == 2:
            c = pal['K']
        else:
            # the long gauntlet of darker red: its top edge comes to a point on the outside of the arm
            point = 6 if (face in ('front', 'back') and xx == 0) or face == ('right' if not mirror else 'left') else 7
            if y < point:
                c = pal['R'] if xx == 1 else pal['r']
                if face == 'front' and xx == 1 and y in (3, 4):
                    c = pal['H']
            elif y == point:
                c = pal['D']
            else:
                c = pal['d']
                if face == 'front' and xx == 1 and y in (point + 1, point + 2):
                    c = pal['r']
                if y == 11:
                    c = pal['D']
        return mul(c, k)
    paint(im, P['rarm'], lambda f, x, y, w, h: arm(f, x, y, w, h, False))
    paint(im, P['larm'], lambda f, x, y, w, h: arm(f, x, y, w, h, True))
    # ---- the legs: black leggings, red boots to over the knee with a pointed top
    def leg(face, x, y, w, h, mirror):
        if face == 'top':
            return mul(pal['R'], 0.9)
        if face == 'bottom':
            return mul(pal['D'], 0.7)
        xx = (w - 1 - x) if mirror else x
        k = FACE_LIGHT[face] * round_shade(x, w, 0.2)
        outer = face == 'right' if not mirror else face == 'left'
        top = 4 + (0 if (face == 'front' and xx in (1, 2)) else 1)   # the boot's point up the front of the thigh
        if face == 'front' and xx == 0 and not mirror or face == 'front' and xx == 0 and mirror:
            top = 5
        if y == 0:
            c = pal['d'] if face in ('front', 'back') else pal['K']
        elif y < top:
            c = pal['K'] if not (face == 'front' and xx == 1 and y == 2) else pal['k']
            if outer and x in (1, 2) and y >= 1:
                c = pal['d']                                           # a red flash down the outside of the hip
        elif y == top:
            c = pal['H'] if face == 'front' and xx in (1, 2) else pal['R']
        else:
            c = pal['R'] if xx in (1,) else pal['r']
            if face == 'front' and xx == 1 and y in (top + 1, top + 2, 9):
                c = pal['H']
            if y == 7:
                c = pal['d']                                           # the knee's crease
            if y >= 10:
                c = pal['d'] if y == 10 else pal['D']
        return mul(c, k)
    paint(im, P['rleg'], lambda f, x, y, w, h: leg(f, x, y, w, h, False))
    paint(im, P['lleg'], lambda f, x, y, w, h: leg(f, x, y, w, h, True))
    # ---- the second layer: the cape's clasped collar round her shoulders
    col = dict(C=hx(0xC81A26), c=hx(0x9A111C), h=hx(0xF04A48), G=hx(0x15121A))
    def put_rows(part, face, lines, light=1.0):
        rows(im, P[part][face], lines, col, light)
    def merge_rows(box, lines):
        x0, y0, _, _ = box
        for y, line in enumerate(lines):
            for x, ch in enumerate(line.split()):
                if ch in col:
                    im.putpixel((x0 + x, y0 + y), col[ch] + (255,))
    merge_rows(P['jacket']['front'], ['C h . . . . h C', 'c C . . . . C c', '. c G . . G c .'])
    merge_rows(P['jacket']['back'], ['. . . . . . . .'])
    merge_rows(P['jacket']['right'], ['c C C c', '. c c .'])
    merge_rows(P['jacket']['left'], ['c C C c', '. c c .'])
    return im

def nexus_cape():
    """64x32, the game's cape layout: the outside at (1,1)-(11,17), the lining at (12,1)-(22,17)."""
    im = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    n = vnoise(11)
    out_top, out_bot = hx(0xD21C28), hx(0x8E0F1A)
    for y in range(0, 17):
        for x in range(0, 22):
            t = max(0, y - 1) / 15.0
            fold = 0.5 + 0.5 * math.sin((x % 11) * 1.25 + 0.6 + t * 1.3)
            c = mix(out_top, out_bot, t)
            c = mul(c, 0.8 + 0.3 * fold + 0.08 * (n(x * 0.9, y * 0.6) - 0.5))
            if x >= 12:                      # the lining: darker, almost black at the folds
                c = mix(mul(c, 0.45), hx(0x15121A), 0.35)
            if y == 16 and x % 2:
                c = mul(c, 0.75)
            im.putpixel((x, y), c + (255,))
    for x in range(1, 11):                   # a bright hem at the shoulders
        im.putpixel((x, 1), hx(0xF04A48) + (255,))
    return im

# =====================================================================================================================
# The Demiurge
NEB = vnoise(317)
NEB2 = vnoise(99)
def nebula(u, v, deep=False):
    """The cosmic body at a point of its unwrapped surface: deep blue to violet to magenta clouds."""
    a = NEB(u * 0.22, v * 0.2)
    b = NEB2(u * 0.45 + 7, v * 0.4 + 3)
    t = 0.6 * a + 0.4 * b
    if deep:
        c = mix(hx(0x04061A), hx(0x10143C), t)
        return mix(c, hx(0x1B1A5A), max(0.0, b - 0.62) * 1.6)
    c = mix(hx(0x0E0C3E), hx(0x35157A), min(1.0, t * 1.6))
    c = mix(c, hx(0x9A2A8E), max(0.0, t - 0.52) * 2.2)
    return mix(c, hx(0x1A3FB0), max(0.0, 0.40 - t) * 2.0)

def stars(seed, w, h, count, big):
    """Star places on a w x h unwrapped surface: [(x, y, brightness)], spaced so none touch."""
    r = random.Random(seed)
    out = []
    tries = 0
    while len(out) < count and tries < 4000:
        tries += 1
        x, y = r.randrange(w), r.randrange(h)
        if any(abs(x - a) <= 1 and abs(y - b) <= 1 for a, b, _ in out):
            continue
        out.append((x, y, 1.0 if len(out) < big else r.choice((0.35, 0.5, 0.7))))
    return out

def unwrap(faces):
    """The four side faces of a part as one strip round it: [(face, x offset)], in the order right, front, left, back."""
    off = 0
    out = {}
    for f in ('right', 'front', 'left', 'back'):
        out[f] = off
        off += faces[f][2] - faces[f][0]
    return out, off

def cosmic(im, faces, seed, deep, count, big, shade=0.2, rim=None):
    offs, total = unwrap(faces)
    h = faces['front'][3] - faces['front'][1]
    st = {(x, y): b for x, y, b in stars(seed, total, h, count, big)}
    def fn(face, x, y, w, hh):
        if face in ('top', 'bottom'):
            return mul(nebula(x + seed, y + seed * 3, deep), 1.05 if face == 'top' else 0.6)
        u = offs[face] + x
        c = nebula(u + seed * 5, y + seed * 2, deep)
        c = mul(c, FACE_LIGHT[face] * round_shade(x, w, shade))
        b = st.get((u, y))
        if b:
            star = hx(0xFFFFFF) if b >= 1.0 else (hx(0xBFD8FF) if (u + y) % 3 else hx(0xFFD8F4))
            c = mix(c, star, min(1.0, b + 0.2))
        else:
            # the soft glow beside a bright star
            for (sx, sy), sb in st.items():
                if sb >= 1.0 and abs(sx - u) + abs(sy - y) == 1:
                    c = mix(c, hx(0x8FB8FF), 0.38)
        if rim is not None and face in ('front', 'back') and x == (rim[1] if face == 'front' else w - 1 - rim[1]):
            c = mix(c, rim[0], 0.42)
        return c
    paint(im, faces, fn)

def demiurge_idle():
    src = Image.open(SUIT + 'billy_teen.png').convert('RGBA')
    im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    P = parts(False)
    im.paste(src.crop((0, 0, 64, 16)), (0, 0))     # the Teen's face and hair, untouched
    # his eyes hold a little of the blue light
    im.putpixel((9, 12), hx(0xDCEFFF) + (255,)); im.putpixel((14, 12), hx(0xDCEFFF) + (255,))
    im.putpixel((10, 12), hx(0x3F8FE0) + (255,)); im.putpixel((13, 12), hx(0x3F8FE0) + (255,))
    cosmic(im, P['body'], 3, False, 26, 6)
    cosmic(im, P['rarm'], 5, False, 15, 3)
    cosmic(im, P['larm'], 8, False, 15, 3)
    cosmic(im, P['rleg'], 13, False, 15, 3)
    cosmic(im, P['lleg'], 21, False, 15, 3)
    pal = dict(R=hx(0xD8242C), r=hx(0xA8161F), d=hx(0x7A0E16), H=hx(0xFF6A4A), O=hx(0xF07A2A), o=hx(0xC8541A),
               Y=hx(0xF6C64A), y=hx(0xC8962E), W=hx(0xF4F8FF), w=hx(0xB8C8E8), B=hx(0x8FD8FF))
    def merge(box, lines, light=1.0):
        x0, y0, _, _ = box
        for y, line in enumerate(lines):
            for x, ch in enumerate(line.split()):
                if ch in pal:
                    im.putpixel((x0 + x, y0 + y), mul(pal[ch], light) + (255,))
    # the tunic's red panels: a yoke under the scarf, piping down the chest to the belt, the belt and its silver clasp,
    # two red tails below it
    merge(P['body']['front'], [
        'R R R R R R R R',
        'r R H R R R R r',
        '. r R R R R r .',
        '. Y . . . . Y .',
        '. y Y . . Y y .',
        '. . y Y Y y . .',
        '. . . y y . . .',
        'r R R W w R R r',
        'd r R w w R r d',
        '. R r . . r R .',
        '. R r . . r R .',
        '. r d . . d r .'])
    merge(P['body']['back'], [
        'R R R R R R R R',
        'r R R R R R R r',
        '. r r R R r r .',
        '. . . . . . . .',
        '. . . . . . . .',
        '. . . . . . . .',
        '. . . . . . . .',
        'r R R R R R R r',
        'd r r r r r r d',
        '. . . . . . . .',
        '. . . . . . . .',
        '. . . . . . . .'], 0.88)
    for side in ('right', 'left'):
        merge(P['body'][side], ['r R R r', 'd r r d', '. . . .', '. . . .', '. . . .', '. . . .', '. . . .', 'r R R r', 'd r r d'], 0.88)
    paint(im, P['body'], lambda f, x, y, w, h: mul(pal['R'], 1.0), ('top',))
    # the shoulders: orange-red plates (the second layer gives them their edge); the hands: the light gathering in them
    def arm_extra(part, sleeve, mirror):
        faces = P[part]
        for f in ('front', 'back', 'left', 'right'):
            x0, y0, x1, y1 = faces[f]
            for x in range(x1 - x0):
                k = FACE_LIGHT[f] * round_shade(x, x1 - x0, 0.2)
                im.putpixel((x0 + x, y0), mul(pal['O'] if f == 'front' and x in (1, 2) else pal['o'], k) + (255,))
                im.putpixel((x0 + x, y0 + 1), mul(pal['R'], k) + (255,))
                im.putpixel((x0 + x, y0 + 2), mul(pal['d'], k) + (255,))
                # the forearm's stars thicken into light: pale blue, then white at the fingers
                for yy, (c, t) in zip((8, 9, 10, 11), ((pal['B'], 0.3), (pal['B'], 0.6), (pal['W'], 0.8), (pal['W'], 1.0))):
                    p = im.getpixel((x0 + x, y0 + yy))[:3]
                    if yy == 8 and (x + (0 if f in ('front', 'left') else 1)) % 2:
                        continue
                    im.putpixel((x0 + x, y0 + yy), mix(p, mul(c, k if yy < 11 else 0.9 + 0.1 * k), t) + (255,))
        paint(im, faces, lambda f, x, y, w, h: mul(pal['O'], 1.05), ('top',))
        paint(im, faces, lambda f, x, y, w, h: pal['W'], ('bottom',))
        # the plate's rim on the second layer: it stands out from the arm
        s = P[sleeve]
        paint(im, s, lambda f, x, y, w, h: mul(pal['O'], 1.08), ('top',))
        for f in ('front', 'back', 'left', 'right'):
            x0, y0, x1, y1 = s[f]
            outer = 'right' if not mirror else 'left'
            for x in range(x1 - x0):
                k = FACE_LIGHT[f] * round_shade(x, x1 - x0, 0.15)
                im.putpixel((x0 + x, y0), mul(pal['H'] if f == 'front' else pal['O'], k) + (255,))
                if f == outer or (f in ('front', 'back') and (x == 0) != mirror and x in (0, x1 - x0 - 1)):
                    im.putpixel((x0 + x, y0 + 1), mul(pal['o'], k) + (255,))
    arm_extra('rarm', 'rsleeve', False)
    arm_extra('larm', 'lsleeve', True)
    # the legs: gold piping down the outside, red boots with a pointed top
    def leg_extra(part, mirror):
        faces = P[part]
        outer = 'right' if not mirror else 'left'
        for f in ('front', 'back', 'left', 'right'):
            x0, y0, x1, y1 = faces[f]
            w = x1 - x0
            for x in range(w):
                xx = (w - 1 - x) if mirror else x
                k = FACE_LIGHT[f] * round_shade(x, w, 0.2)
                top = 8 if (f == 'front' and xx in (1, 2)) else 9
                for y in range(12):
                    if f == outer and x in (1, 2) and y < top:
                        im.putpixel((x0 + x, y0 + y), mul(pal['Y'] if x == 1 else pal['y'], k) + (255,))
                    if y == top:
                        im.putpixel((x0 + x, y0 + y), mul(pal['H'] if f == 'front' else pal['R'], k) + (255,))
                    elif y > top:
                        c = pal['R'] if (f == 'front' and xx == 1) else pal['r']
                        if y == 11:
                            c = pal['d']
                        im.putpixel((x0 + x, y0 + y), mul(c, k) + (255,))
        paint(im, faces, lambda f, x, y, w, h: mul(pal['d'], 0.7), ('bottom',))
    leg_extra('rleg', False)
    leg_extra('lleg', True)
    return im

def demiurge_powered():
    src = Image.open(SUIT + 'billy_teen.png').convert('RGBA')
    im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    P = parts(False)
    rim = hx(0x6FD0FF)   # a thin line of light along the outside of each limb: the body's outline glows
    cosmic(im, P['body'], 31, True, 40, 8, 0.1)
    cosmic(im, P['rarm'], 37, True, 20, 4, 0.1, (rim, 0))
    cosmic(im, P['larm'], 41, True, 20, 4, 0.1, (rim, 3))
    cosmic(im, P['rleg'], 43, True, 20, 4, 0.1, (rim, 0))
    cosmic(im, P['lleg'], 47, True, 20, 4, 0.1, (rim, 3))
    W, B, b2 = hx(0xF6FBFF), hx(0xBFE6FF), hx(0x6FC0FF)
    # the light breaks through at the hands and the feet: speckles, then all light
    def fade(part, start):
        faces = P[part]
        r = random.Random(hash(part) & 0xFFFF)
        for f in ('front', 'back', 'left', 'right'):
            x0, y0, x1, y1 = faces[f]
            for x in range(x1 - x0):
                for y in range(start, 12):
                    t = (y - start + 1) / (12 - start)
                    p = im.getpixel((x0 + x, y0 + y))[:3]
                    if t < 0.5:
                        if r.random() < t * 1.5:
                            im.putpixel((x0 + x, y0 + y), (B if r.random() < 0.5 else W) + (255,))
                    else:
                        c = mix(B, W, (t - 0.5) * 2)
                        if x in (0, x1 - x0 - 1):
                            c = mix(c, b2, 0.35)
                        im.putpixel((x0 + x, y0 + y), mix(p, c, min(1.0, 0.55 + t * 0.6)) + (255,))
        paint(im, faces, lambda f, x, y, w, h: W, ('bottom',))
    for part in ('rarm', 'larm'):
        fade(part, 6)
    for part in ('rleg', 'lleg'):
        fade(part, 5)
    # the muscles' lines of light on the chest
    x0, y0, _, _ = P['body']['front']
    for (x, y) in ((3, 2), (4, 2), (2, 3), (5, 3), (3, 6), (4, 6), (3, 8), (4, 8)):
        p = im.getpixel((x0 + x, y0 + y))[:3]
        im.putpixel((x0 + x, y0 + y), mix(p, b2, 0.35) + (255,))
    # the head: a face of pale light in the hood's shadow, blank white eyes, white hair
    pal = dict(F=hx(0xD4E6FF), f=hx(0xB4CEF4), s=hx(0x8FAEE0), n=hx(0x7493CC), E=hx(0xFFFFFF), e=hx(0x8FDCFF),
               A=hx(0xFFFFFF), a=hx(0xDCE8FA), g=hx(0xB6C6E2), m=hx(0x5E7FB8), k=hx(0x08102E))
    rows(im, P['head']['front'], [
        'a A a A A a A a',
        'g a A a a A a g',
        'g a F a a F F g',
        'F n n F F n n f',
        'F E e F F e E f',
        'F F F f s f f s',
        'f F F m m f f s',
        's f F f f f s n'], pal)
    def hair(face, x, y, w, h):
        k = FACE_LIGHT[face]
        if face == 'top':
            return mul(pal['A'] if (x * 3 + y * 5) % 4 else pal['a'], 1.0)
        if face == 'bottom':
            return pal['s']
        if face == 'back':
            return mul(pal['a'] if (x + y) % 3 else pal['g'], k) if y < 6 else mul(pal['f'] if y == 6 else pal['s'], k)
        # the sides: hair above, the cheek and jaw below
        front_x = x >= w - 3 if face == 'right' else x < 3
        if y < 3 or (y < 5 and not front_x):
            return mul(pal['a'] if (x + y) % 3 else pal['g'], k)
        return mul(pal['f'] if y < 6 else pal['s'], k)
    paint(im, P['head'], hair, ('top', 'bottom', 'back', 'left', 'right'))
    # the second layer of the head: white hair standing a little off it
    hat = P['hat']
    paint(im, hat, lambda f, x, y, w, h: (pal['A'] if (x * 5 + y * 3) % 3 else pal['a']) if f == 'top' else None, ('top',))
    rows(im, hat['front'], ['A a A A a A a A', 'a A a . . a A a', 'g . . . . . . g'], pal)
    for f in ('left', 'right', 'back'):
        x0, y0, x1, y1 = hat[f]
        for x in range(x1 - x0):
            for y in range(0, 3 if f != 'back' else 5):
                if y >= 2 and (x + y) % 2:
                    continue
                im.putpixel((x0 + x, y0 + y), mul(pal['a'] if (x + y) % 3 else pal['g'], FACE_LIGHT[f]) + (255,))
    return im

def cloak():
    """64x64: the outside x 0-31, y 0-47 (its lower third torn into long tongues), the lining x 32-63, the scarf and
    hood's outside x 0-31, y 48-63, their inside x 32-63."""
    im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    n = vnoise(5)
    r = random.Random(77)
    hi, mid, lo, deep = hx(0xF02A30), hx(0xC8161E), hx(0x8C0C14), hx(0x4E060C)
    # the tongues of the hem: each column's cloth ends at its own length; neighbours form the long flames
    ends = []
    tips = [44, 48, 45, 48, 43, 47]              # six tongues across, each tapering to its own point
    for x in range(32):
        i = min(5, int(x / 32.0 * 6))
        p = abs(((x + 0.5) / 32.0 * 6 - i) * 2 - 1)   # 0 at the tongue's middle, 1 at the gap between two
        ends.append(int(round(tips[i] - 19.0 * p ** 1.25)))
    for half in (0, 1):
        for y in range(48):
            for x in range(32):
                if y >= ends[x] and y >= 25:
                    continue
                v = y / 47.0
                # the folds: stripes of dark and bright red running down the cloth, wandering a little
                fold = math.sin(x * 0.95 + 1.1 * math.sin(v * 5.0 + x * 0.21) + 0.4)
                fold2 = math.sin(x * 2.3 + v * 3.0)
                k = 0.5 + 0.38 * fold + 0.12 * fold2
                c = mix(lo, hi, max(0.0, min(1.0, k)))
                if fold < -0.55:
                    c = mix(c, deep, 0.6)
                c = mul(c, 1.0 - 0.18 * v + 0.07 * (n(x * 0.7, y * 0.5) - 0.5))
                if y >= ends[x] - 1 and y >= 24:
                    c = mul(c, 1.18)                              # the bright torn edge
                if half == 1:
                    # the lining: night-dark red, stars caught in it
                    c = mix(mul(c, 0.3), hx(0x100818), 0.45)
                    if r.random() < 0.035:
                        c = mix(c, hx(0xFFF0F0), 0.85)
                    elif r.random() < 0.05:
                        c = mix(c, hx(0xFF9AA0), 0.5)
                im.putpixel((half * 32 + x, y), c + (255,))
    # the scarf and the hood: cloth in soft folds running round, darker in its creases
    for half in (0, 1):
        for y in range(48, 64):
            for x in range(32):
                v = (y - 48) / 15.0
                fold = math.sin(v * 9.0 + 0.9 * math.sin(x * 0.5))
                c = mix(mid, hi, 0.5 + 0.5 * fold)
                if fold < -0.6:
                    c = mix(c, deep, 0.55)
                c = mul(c, 0.96 + 0.1 * (n(x * 0.8 + 9, y * 0.8) - 0.5))
                if half == 1:
                    c = mix(mul(c, 0.32), hx(0x12060C), 0.4)       # the hood's inside: its shadow
                im.putpixel((half * 32 + x, y), c + (255,))
    return im

# =====================================================================================================================
# Ultimate Source Code Vision, burning: the molten body and the cracks of fire over it
def veins(seed, total, h, count):
    """Cracks on a part's unwrapped surface (total x h): each a wandering line of cells [(x, y)] with its own id."""
    r = random.Random(seed)
    out = []
    for i in range(count):
        x, y = r.randrange(total), r.randrange(h)
        dx, dy = r.choice(((1, 1), (-1, 1), (1, -1), (0, 1), (1, 0), (-1, 0)))
        cells = []
        for _ in range(r.randrange(4, 9)):
            if 0 <= y < h:
                cells.append((x % total, y))
            if r.random() < 0.35:
                dx, dy = r.choice(((1, 1), (-1, 1), (0, 1), (1, 0), (-1, 0), (1, -1)))
            x += dx; y += dy
        out.append((i, cells))
    return out

VEIN_PARTS = (('head', 3, 9), ('body', 5, 11), ('rarm', 7, 7), ('larm', 11, 7), ('rleg', 13, 7), ('lleg', 17, 7))

def molten():
    im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    P = parts(False)
    n = vnoise(23)
    hot, red, deep, shadow = hx(0xE8412A), hx(0xB81E22), hx(0x7A0F24), hx(0x4A0A2E)
    for name, seed, count in VEIN_PARTS:
        faces = P[name]
        offs, total = unwrap(faces)
        h = faces['front'][3] - faces['front'][1]
        cracked = {c for _, cells in veins(seed, total, h, count) for c in cells}
        def fn(face, x, y, w, hh, offs=offs, cracked=cracked, name=name):
            if face in ('top', 'bottom'):
                return mul(red if face == 'top' else deep, 1.0)
            u = offs[face] + x
            t = 0.5 * n(u * 0.5 + 3, y * 0.45) + 0.5 * n(u * 0.23, y * 0.3 + 9)
            c = mix(deep, red, min(1.0, t * 1.5))
            c = mix(c, hot, max(0.0, t - 0.55) * 1.6)
            c = mix(c, shadow, max(0.0, 0.36 - t) * 1.8)        # the purple of its shadows
            c = mul(c, FACE_LIGHT[face] * round_shade(x, w, 0.3))
            if (u, y) in cracked:
                c = mix(c, hx(0xFFB03A), 0.75)
            elif any((u + a, y + b) in cracked for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                c = mix(c, hx(0xF0502A), 0.45)
            return c
        paint(im, faces, fn)
    # the face: no features but two slits of light and the brow's shadow, as a figure of fire has
    x0, y0, _, _ = P['head']['front']
    for x in (1, 2, 5, 6):
        im.putpixel((x0 + x, y0 + 4), hx(0xFFE9A0) + (255,))
        im.putpixel((x0 + x, y0 + 3), hx(0x5A0A22) + (255,))
    for x in (3, 4):
        im.putpixel((x0 + x, y0 + 6), hx(0x6A0C22) + (255,))
    return im

def cracks(frame):
    """The cracks alone, on black (they are added to the body, full-bright): each vein waxes and wanes in its own time."""
    im = Image.new('RGBA', (64, 64), (0, 0, 0, 255))
    P = parts(False)
    for name, seed, count in VEIN_PARTS:
        faces = P[name]
        offs, total = unwrap(faces)
        h = faces['front'][3] - faces['front'][1]
        vs = veins(seed, total, h, count)
        glow = {}
        for i, cells in vs:
            k = 0.5 + 0.5 * math.sin(frame * math.pi / 2 + i * 1.9 + seed)
            k = 0.3 + 0.7 * k
            for j, c in enumerate(cells):
                run = 0.75 + 0.25 * math.sin(frame * math.pi / 2 - j * 1.3 + i)   # the light runs along the crack
                glow[c] = max(glow.get(c, 0.0), k * run)
        def fn(face, x, y, w, hh, offs=offs, glow=glow):
            if face in ('top', 'bottom'):
                return (10, 3, 0)
            u = offs[face] + x
            g = glow.get((u, y))
            if g:
                return mix(hx(0xFF8A1E), hx(0xFFF6C8), min(1.0, g * 1.15)) if g > 0.45 else mul(hx(0xFF8A1E), g * 2.0)
            near = max([glow.get((u + a, y + b), 0.0) for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))] or [0.0])
            # the whole body glows a little of itself, more beside a crack, and at its edges (the fire's rim)
            edge = 0.16 if x in (0, w - 1) else 0.0
            return mul(hx(0xFF6A1A), 0.02 + 0.3 * near + edge * 0.5)
        paint(im, faces, fn)
    x0, y0, _, _ = P['head']['front']
    for x in (1, 2, 5, 6):
        im.putpixel((x0 + x, y0 + 4), (255, 250, 220, 255))
    return im

# =====================================================================================================================
# Cosmic Spider-Man (Captain Universe) and the Godkiller armour
def cosmic_spider_64():
    im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    P = parts(False)
    W, w2, w3 = hx(0xF4F7FC), hx(0xCFD6E4), hx(0x9AA4BA)
    blue, red, redd = hx(0x3FA8F0), hx(0xD8242C), hx(0x8E1018)
    def white(face, x, y, w, h):
        k = FACE_LIGHT[face] * round_shade(x, w, 0.22)
        c = mix(W, w2, min(1.0, y / max(1.0, h - 1.0) * 0.8))
        return mul(c, k)
    # the starfield of the Uni-Power over the head, shoulders, chest and arms; white from the ribs down
    for part, seed, n in (('head', 51, 16), ('body', 53, 14), ('rarm', 57, 9), ('larm', 59, 9)):
        cosmic(im, P[part], seed, True, n, 3, 0.12)
    for part in ('rleg', 'lleg'):
        paint(im, P[part], white)
    def lower(part, start, edge):
        faces = P[part]
        for f in ('front', 'back', 'left', 'right'):
            x0, y0, x1, y1 = faces[f]
            wdt = x1 - x0
            for x in range(wdt):
                # the white rises to a point in the middle of the chest, as on the suit
                rise = 0
                if part == 'body' and f in ('front', 'back'):
                    rise = 2 - min(2, abs(int(x - (wdt - 1) / 2.0)))
                for y in range(start - rise, y1 - y0):
                    c = white(f, x, y, wdt, y1 - y0)
                    if y == start - rise:
                        c = mul(blue, FACE_LIGHT[f])
                    im.putpixel((x0 + x, y0 + y), c + (255,))
        paint(im, faces, lambda f, x, y, w, h: mul(w3, 0.9), ('bottom',))
    lower('body', 6, True)
    lower('rarm', 7, True)
    lower('larm', 7, True)
    # the constellation: white stars joined by lines across the chest and shoulders
    x0, y0, _, _ = P['body']['front']
    for (x, y) in ((1, 1), (2, 2), (3, 2), (4, 1), (5, 2), (6, 1), (3, 3), (4, 4)):
        im.putpixel((x0 + x, y0 + y), (255, 255, 255, 255) if (x, y) in ((1, 1), (4, 1), (6, 1), (4, 4)) else hx(0xAFC4E8) + (255,))
    # the mask: the great white eyes rimmed in blue, the red web over the mouth and chin
    rows(im, P['head']['front'], [
        '. . . . . . . .',
        '. . . . . . . .',
        'B B . . . . B B',
        'B W B . . B W B',
        'B W W B B W W B',
        '. B B R R B B .',
        '. . R r r R . .',
        '. . r R R r . .'], dict(B=blue, W=W, R=red, r=redd))
    return im

def godkiller_64():
    im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    P = parts(False)
    n = vnoise(61)
    G, g, d, D, K = hx(0xF0B43A), hx(0xC98A26), hx(0x8E5E1E), hx(0x54381A), hx(0x16110C)
    V = hx(0x6E5A78)       # the violet of its shadowed plates
    Lg, R = hx(0xFFF6C0), hx(0xFF3020)
    def plate(face, x, y, w, h, seams):
        k = FACE_LIGHT[face] * round_shade(x, w, 0.3)
        t = 0.55 + 0.45 * (n(x * 0.6 + face.__hash__() % 7, y * 0.5) - 0.5)
        c = mix(g, G, min(1.0, max(0.0, t + 0.25 - y / (h * 2.5))))
        if y in seams or x in ((0, w - 1) if w > 3 else ()):
            c = d
        if y - 1 in seams:
            c = mix(c, Lg, 0.25)                    # the lit edge under a seam
        if face == 'back':
            c = mix(c, V, 0.22)
        return mul(c, k)
    for part, seams in (('head', (0, 7)), ('body', (0, 5, 8, 11)), ('rarm', (4, 7, 11)), ('larm', (4, 7, 11)), ('rleg', (0, 5, 9)), ('lleg', (0, 5, 9))):
        paint(im, P[part], lambda f, x, y, w, h, seams=seams: mul(G, 1.02) if f == 'top' else (mul(D, 0.8) if f == 'bottom' else plate(f, x, y, w, h, seams)))
    pal = dict(G=G, g=g, d=d, D=D, K=K, L=Lg, R=R, V=V, l=hx(0xFFE27A))
    def merge(box, lines):
        x0, y0, _, _ = box
        for y, line in enumerate(lines):
            for x, ch in enumerate(line.split()):
                if ch in pal:
                    im.putpixel((x0 + x, y0 + y), pal[ch] + (255,))
    # the helm: a blunt faceplate, two red slits, the grille of the mouth
    merge(P['head']['front'], [
        'd G G G G G G d',
        'G G G g g G G G',
        'G g d d d d g G',
        'G K K d d K K G',
        'G R R d d R R G',
        'g G d g g d G g',
        'd g K d d K g d',
        'D d K K K K d D'])
    # the chest: the two reactors side by side, the black vents on the shoulders, the plated belly
    merge(P['body']['front'], [
        'K K d G G d K K',
        'K K d G G d K K',
        'G d l l d l l G',
        'G l L l d L l G',
        'G l L l d l L G',
        'd d l l d l l d',
        'V g G d d G g V',
        'V g G d d G g V',
        'd d d d d d d d',
        'D g G g g G g D',
        'D g G g g G g D',
        'D d d K K d d D'])
    # the back: the great plates of the shoulders, the spine's ridge, the thrusters' two vents
    merge(P['body']['back'], [
        'K K d G G d K K',
        'G G d G G d G G',
        'G g G d d G g G',
        'g G G d d G G g',
        'g G g K K g G g',
        'd d d d d d d d',
        'V g l d d l g V',
        'V g l d d l g V',
        'd d d d d d d d',
        'D g G g g G g D',
        'D g G g g G g D',
        'D d d K K d d D'])
    merge(P['head']['back'], [
        'd G G G G G G d',
        'G G g g g g G G',
        'G g G G G G g G',
        'G g G d d G g G',
        'G g G d d G g G',
        'g G g g g g G g',
        'd g d d d d g d',
        'D d K K K K d D'])
    for part in ('rarm', 'larm'):
        x0, y0, _, _ = P[part]['front']
        for x in (1, 2):
            im.putpixel((x0 + x, y0 + 9), pal['l'] + (255,)); im.putpixel((x0 + x, y0 + 10), pal['L'] + (255,))   # the palm cannon's glow
        tx0, ty0, _, _ = P[part]['top']
        for x in range(1, 3):
            for y in range(1, 3):
                im.putpixel((tx0 + x, ty0 + y), K + (255,))   # the shoulder's vent
    return im

# =====================================================================================================================
# Round three: suits painted four times as fine (256x256: the model's picture is read by its proportions, so a finer
# one fits the same body), for the forms that are seen large or close: the Godkiller, Cosmic Spider-Man, Ultimate Tommy
from PIL import ImageDraw
HD = 4

def hd_parts(slim=False):
    return {name: {f: tuple(v * HD for v in box) for f, box in faces.items()} for name, faces in parts(slim).items()}

class Face:
    """One face of a part at the fine scale: draws in its own pixels (0,0 its top left), clipped to it."""
    def __init__(self, im, box, light=1.0):
        self.im, self.box, self.light = im, box, light
        self.w, self.h = box[2] - box[0], box[3] - box[1]
        self.layer = Image.new('RGBA', (self.w, self.h), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.layer)
    def c(self, col, k=1.0):
        return mul(col, self.light * k) + (255,)
    def fill(self, col, k=1.0):
        self.d.rectangle((0, 0, self.w, self.h), fill=self.c(col, k))
    def rect(self, x0, y0, x1, y1, col, k=1.0):
        self.d.rectangle((x0, y0, x1, y1), fill=self.c(col, k))
    def poly(self, pts, col, k=1.0):
        self.d.polygon(pts, fill=self.c(col, k))
    def oval(self, x0, y0, x1, y1, col, k=1.0):
        self.d.ellipse((x0, y0, x1, y1), fill=self.c(col, k))
    def line(self, pts, col, width=1, k=1.0):
        self.d.line(pts, fill=self.c(col, k), width=width)
    def px(self, x, y, col, k=1.0):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.layer.putpixel((x, y), self.c(col, k))
    def shade(self, strength=0.22):
        """The roundness of the limb: darker toward the face's left and right edges."""
        px = self.layer.load()
        for x in range(self.w):
            k = round_shade(x, self.w, strength)
            for y in range(self.h):
                r, g, b, a = px[x, y]
                px[x, y] = (min(255, int(r * k)), min(255, int(g * k)), min(255, int(b * k)), a)
    def done(self):
        self.im.alpha_composite(self.layer, (self.box[0], self.box[1]))

def faces_of(im, part, which=('front', 'back', 'left', 'right', 'top', 'bottom')):
    return {f: Face(im, part[f], FACE_LIGHT[f]) for f in which}

def godkiller():
    im = Image.new('RGBA', (64 * HD, 64 * HD), (0, 0, 0, 0))
    P = hd_parts()
    G, Lt, g, d, D, K = hx(0xE2A22C), hx(0xFFDC78), hx(0xB67C22), hx(0x7E5418), hx(0x3E2A12), hx(0x120E0A)
    V, Lg, R, Rg = hx(0x6A5878), hx(0xFFF8D0), hx(0xFF2A1A), hx(0xFFB090)
    n = vnoise(61)
    def metal(F, seed):
        """Brushed gold with a soft sheen from the top left."""
        px = F.layer.load()
        for y in range(F.h):
            for x in range(F.w):
                t = 0.5 + 0.5 * (n(x * 0.11 + seed, y * 0.05 + seed) - 0.5) + 0.25 * (1 - y / F.h) - 0.1 * (x / F.w)
                c = mix(g, G, max(0.0, min(1.0, t)))
                if t > 0.8:
                    c = mix(c, Lt, (t - 0.8) * 2.5)
                px[x, y] = mul(c, F.light) + (255,)
    def plate(F, x0, y0, x1, y1, inset=False):
        """A plate's edges: lit along its top and left, dark along its bottom and right (or the other way: a recess)."""
        hi, lo = (D, Lt) if inset else (Lt, D)
        F.line([(x0, y0), (x1, y0)], hi); F.line([(x0, y0), (x0, y1)], hi, k=0.9)
        F.line([(x0, y1), (x1, y1)], lo); F.line([(x1, y0), (x1, y1)], lo)
    def rivets(F, pts):
        for x, y in pts:
            F.px(x, y, Lt); F.px(x + 1, y + 1, D)
    def base(part, seed, seams, shade=0.26):
        fs = faces_of(im, part)
        for i, (f, F) in enumerate(fs.items()):
            metal(F, seed + i * 3)
            if f in ('top', 'bottom'):
                if f == 'bottom':
                    F.fill(D)
                continue
            last = 0
            for sy in list(seams) + [F.h]:
                plate(F, 1, last + 1, F.w - 2, sy - 2)
                rivets(F, [(3, last + 3), (F.w - 5, last + 3)])
                F.rect(0, sy - 1, F.w, sy, d)
                last = sy
        return fs
    # ---- the helm: a blunt dome, a heavy brow, two slits of red light, a barred mouth-grille (no face of a man)
    fs = base(P['head'], 1, (9, 26))
    F = fs['front']
    F.poly([(0, 9), (32, 9), (32, 13), (20, 15), (16, 13), (12, 15), (0, 13)], d)          # the brow, drawn down to the nose
    F.line([(0, 9), (32, 9)], Lt)
    F.poly([(3, 15), (13, 17), (13, 19), (4, 18)], K); F.poly([(29, 15), (19, 17), (19, 19), (28, 18)], K)   # the eyes' sockets
    F.poly([(5, 16), (12, 17.5), (12, 18.5), (5, 17.5)], R); F.poly([(27, 16), (20, 17.5), (20, 18.5), (27, 17.5)], R)
    F.px(8, 17, Rg); F.px(23, 17, Rg)
    F.rect(15, 13, 16, 22, d); F.line([(14, 13), (14, 22)], Lt, k=0.9)                      # the ridge of the nose
    F.poly([(8, 22), (24, 22), (22, 31), (10, 31)], D)                                      # the jaw's plate
    for x in range(10, 23, 3):
        F.rect(x, 24, x + 1, 30, K)                                                         # the grille's bars
    F.line([(8, 22), (24, 22)], Lt, k=0.8)
    F.poly([(0, 14), (3, 20), (3, 31), (0, 31)], g); F.poly([(32, 14), (29, 20), (29, 31), (32, 31)], g)   # the cheek guards
    F.line([(3, 20), (3, 31)], D); F.line([(28, 20), (28, 31)], D)
    for f in ('left', 'right'):
        F2 = fs[f]
        F2.oval(11, 12, 21, 22, d); F2.oval(13, 14, 19, 20, D); F2.oval(15, 16, 17, 18, Lt)   # the ear's hub
        F2.rect(4, 26, 28, 27, D)
    F2 = fs['back']
    for x in (8, 16, 24):
        F2.rect(x - 1, 12, x, 24, D)                                                        # the vents of the neck
    F2.rect(4, 27, 28, 28, D)
    fs['top'].rect(13, 4, 18, 28, g); fs['top'].line([(13, 4), (13, 28)], Lt); fs['top'].line([(19, 4), (19, 28)], D)   # the crest
    for F in fs.values():
        F.shade(0.2); F.done()
    # ---- the chest: two round reactors, black vents in the shoulders, a keel down the breast, a plated belly
    fs = base(P['body'], 9, (23, 34), 0.2)
    F = fs['front']
    F.poly([(0, 0), (9, 0), (7, 6), (0, 8)], K); F.poly([(32, 0), (23, 0), (25, 6), (32, 8)], K)    # the shoulder vents
    F.line([(0, 8), (7, 6), (9, 0)], Lt, k=0.8); F.line([(32, 8), (25, 6), (23, 0)], Lt, k=0.8)
    for cx in (9, 23):
        F.oval(cx - 7, 9, cx + 7, 23, D); F.oval(cx - 6, 10, cx + 6, 22, d)
        F.oval(cx - 5, 11, cx + 5, 21, hx(0xFFC850)); F.oval(cx - 4, 12, cx + 4, 20, hx(0xFFE9A0)); F.oval(cx - 2, 14, cx + 2, 18, Lg)
    F.rect(15, 0, 16, 23, d); F.line([(14, 0), (14, 23)], Lt, k=0.9)
    for y in (26, 30):
        F.line([(4, y), (28, y)], D); F.line([(4, y + 1), (28, y + 1)], Lt, k=0.85)
    F.poly([(0, 24), (4, 24), (6, 34), (0, 34)], V); F.poly([(32, 24), (28, 24), (26, 34), (32, 34)], V)   # the waist's shadowed plates
    F.rect(12, 36, 20, 40, D); F.rect(13, 37, 19, 39, g); F.rect(15, 41, 16, 47, D)         # the belt's block, the groin's split
    F2 = fs['back']
    F2.poly([(4, 2), (14, 2), (13, 20), (6, 18)], g); F2.poly([(28, 2), (18, 2), (19, 20), (26, 18)], g)   # the shoulder blades
    F2.line([(4, 2), (14, 2)], Lt); F2.line([(18, 2), (28, 2)], Lt); F2.line([(6, 18), (13, 20)], D); F2.line([(26, 18), (19, 20)], D)
    F2.rect(15, 0, 16, 34, D)
    for cx in (9, 23):
        F2.oval(cx - 4, 24, cx + 4, 32, D); F2.oval(cx - 3, 25, cx + 3, 31, K); F2.oval(cx - 1, 27, cx + 1, 29, hx(0xFFB040))   # the thrusters
    for F in fs.values():
        F.shade(0.16); F.done()
    # ---- the arms: a great shoulder with its black slot, the elbow's joint, the gauntlet and its palm cannon
    for name, seed, mirror in (('rarm', 17, False), ('larm', 23, True)):
        fs = base(P[name], seed, (17, 26))
        outer = fs['right' if not mirror else 'left']
        outer.poly([(3, 3), (13, 3), (12, 12), (4, 12)], K); outer.line([(3, 13), (13, 13)], Lt, k=0.8)     # the shoulder's slot
        for f in ('front', 'back', 'left', 'right'):
            F = fs[f]
            F.rect(0, 18, F.w, 24, d); F.rect(0, 20, F.w, 22, D)                           # the elbow
            F.rect(0, 40, F.w, 47, D); F.line([(0, 40), (F.w, 40)], Lt, k=0.7)              # the hand
            for x in range(2, F.w, 4):
                F.rect(x, 42, x + 1, 46, d)
        F = fs['front']
        F.oval(4, 30, 12, 38, D); F.oval(5, 31, 11, 37, hx(0xFFD060)); F.oval(7, 33, 9, 35, Lg)   # the repulsor of the forearm
        fs['bottom'].oval(3, 3, 13, 13, hx(0xFFE9A0)); fs['bottom'].oval(6, 6, 10, 10, Lg)
        for F in fs.values():
            F.shade(0.28); F.done()
    # ---- the legs: the thigh's plate, the knee's cap, the shin, the heavy boot
    for name, seed in (('rleg', 31), ('lleg', 37)):
        fs = base(P[name], seed, (18, 26, 40))
        for f in ('front', 'back', 'left', 'right'):
            F = fs[f]
            F.rect(0, 40, F.w, 47, d); F.rect(0, 45, F.w, 47, D)
        F = fs['front']
        F.oval(3, 17, 13, 27, d); F.oval(4, 18, 12, 26, G); F.oval(6, 20, 9, 23, Lt)        # the knee
        F.poly([(5, 28), (11, 28), (10, 39), (6, 39)], g); F.line([(5, 28), (6, 39)], Lt, k=0.9); F.line([(11, 28), (10, 39)], D)
        F.poly([(4, 2), (12, 2), (11, 15), (5, 15)], G); F.line([(4, 2), (5, 15)], Lt); F.line([(12, 2), (11, 15)], D)
        for F in fs.values():
            F.shade(0.28); F.done()
    # ---- the second layer, standing off the body: the pauldrons, the collar round the helm, the chest's rim
    for name, mirror in (('rsleeve', False), ('lsleeve', True)):
        fs = faces_of(im, P[name])
        fs['top'].fill(G); fs['top'].rect(2, 2, 13, 13, Lt, 0.92); fs['top'].rect(5, 5, 10, 10, K)
        for f in ('front', 'back', 'left', 'right'):
            F = fs[f]
            F.rect(0, 0, F.w, 13, G); F.line([(0, 0), (F.w, 0)], Lt); F.rect(0, 12, F.w, 14, d)
            F.shade(0.3)
        outer = fs['right' if not mirror else 'left']
        outer.poly([(3, 3), (13, 3), (12, 10), (4, 10)], K)
        for f in ('front', 'back', 'left', 'right', 'top'):
            fs[f].done()
    fs = faces_of(im, P['jacket'], ('front', 'back', 'left', 'right'))
    for f, F in fs.items():
        F.rect(0, 0, F.w, 3, g); F.line([(0, 0), (F.w, 0)], Lt); F.line([(0, 4), (F.w, 4)], D)
        F.done()
    return im

def cosmic_spider():
    im = Image.new('RGBA', (64 * HD, 64 * HD), (0, 0, 0, 0))
    P = hd_parts()
    r = random.Random(170)
    N0, N1 = hx(0x04050E), hx(0x10143A)
    W, w2, w3, w4 = hx(0xFBFCFF), hx(0xDDE3EE), hx(0xB4BED0), hx(0x8490A8)
    blue, blued, red, redd = hx(0x3AA8F4), hx(0x1E6EC8), hx(0xE02830), hx(0x7A0E16)
    def night(F, seed, stars):
        px = F.layer.load()
        for y in range(F.h):
            for x in range(F.w):
                t = NEB(x * 0.06 + seed, y * 0.06 + seed * 2)
                px[x, y] = mul(mix(N0, N1, t), F.light) + (255,)
        for _ in range(stars):
            x, y = r.randrange(F.w), r.randrange(F.h)
            b = r.choice((0.5, 0.7, 1.0))
            F.px(x, y, mix(N1, hx(0xFFFFFF), b))
            if b == 1.0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    F.px(x + dx, y + dy, mix(N1, hx(0xBFD8FF), 0.55))
    def white(F, x0, y0, x1, y1):
        """The white of the Uni-Power's suit: bright, with soft grey modelling."""
        for y in range(max(0, y0), min(F.h, y1)):
            for x in range(max(0, x0), min(F.w, x1)):
                t = (y - y0) / max(1, y1 - y0)
                F.px(x, y, mix(W, w2, 0.55 * t))
    def constellation(F, pts, links):
        for a, b in links:
            F.line([pts[a], pts[b]], hx(0xDCE8FF), 1)
        for x, y in pts:
            F.oval(x - 1, y - 1, x + 1, y + 1, hx(0xFFFFFF))
    # ---- the head: night and stars, the great white eyes rimmed in blue, the red web over the mouth
    fs = faces_of(im, P['head'])
    for i, F in enumerate(fs.values()):
        night(F, 3 + i, 26)
    F = fs['front']
    for sgn in (1, -1):
        def mx(x):
            return x if sgn == 1 else 31 - x
        eye = [(mx(2), 11), (mx(6), 10), (mx(12), 15), (mx(12), 18), (mx(8), 19), (mx(4), 17)]
        rim = [(mx(1), 9), (mx(7), 8), (mx(14), 14), (mx(14), 19), (mx(8), 21), (mx(2), 18)]
        F.poly(rim, blue); F.poly(eye, W)
        F.line([eye[0], eye[1]], w2)
    F.poly([(12, 21), (20, 21), (22, 26), (19, 31), (13, 31), (10, 26)], red)                 # the web over the mouth and chin
    for x in (13, 16, 19):
        F.line([(16, 21), (x - 1 if x < 16 else x + 1 if x > 16 else 16, 31)], redd)
    for y, half in ((24, 5), (28, 4)):
        F.line([(16 - half, y), (16, y + 1), (16 + half, y)], redd)
    F.line([(12, 21), (20, 21), (22, 26), (19, 31)], blued); F.line([(12, 21), (10, 26), (13, 31)], blued)
    for f in ('front', 'left', 'right', 'back'):                                             # the blue line round the head's edge
        fs[f].line([(0, 0), (fs[f].w - 1, 0)], blue, 1)
    constellation(fs['top'], [(8, 8), (16, 5), (24, 9), (20, 18), (11, 20)], [(0, 1), (1, 2), (2, 3), (3, 4), (4, 0)])
    for F in fs.values():
        F.shade(0.14); F.done()
    # ---- the body: night over the shoulders and breast; from the ribs down, white, rising to a point in the middle
    fs = faces_of(im, P['body'])
    for i, F in enumerate(fs.values()):
        night(F, 11 + i, 30)
    for f in ('front', 'back'):
        F = fs[f]
        edge = [(0, 22), (8, 20), (16, 14), (24, 20), (32, 22)]
        F.poly(edge + [(32, 48), (0, 48)], W)
        px = F.layer.load()
        for y in range(14, 48):
            for x in range(32):
                if px[x, y][:3] == mul(W, F.light):
                    t = (y - 14) / 34.0
                    c = mix(W, w2, 0.5 * t)
                    if x in (15, 16) and y > 26:
                        c = w3                                                               # the line down the middle of the belly
                    if abs(x - 16) in (7, 8) and 24 < y < 40:
                        c = mix(c, w3, 0.5)                                                  # the ribs' shadow
                    px[x, y] = mul(c, F.light) + (255,)
        F.line(edge, blue, 2)
        F.line([(4, 44), (16, 47), (28, 44)], w3)
    for f in ('left', 'right'):
        F = fs[f]
        white(F, 0, 22, F.w, 48); F.line([(0, 22), (F.w, 22)], blue, 2)
    constellation(fs['front'], [(4, 4), (11, 8), (16, 4), (21, 8), (28, 4), (16, 11)], [(0, 1), (1, 2), (2, 3), (3, 4), (1, 5), (3, 5)])
    constellation(fs['back'], [(5, 5), (13, 9), (20, 5), (27, 10)], [(0, 1), (1, 2), (2, 3)])
    fs['bottom'].fill(w3)
    for F in fs.values():
        F.shade(0.14); F.done()
    # ---- the arms: night to below the elbow, then the white gloves, a blue line between
    for name, seed in (('rarm', 23), ('larm', 29)):
        fs = faces_of(im, P[name])
        for i, F in enumerate(fs.values()):
            night(F, seed + i, 14)
        for f in ('front', 'back', 'left', 'right'):
            F = fs[f]
            white(F, 0, 27, F.w, 48); F.line([(0, 27), (F.w, 27)], blue, 2)
            F.line([(0, 41), (F.w, 41)], w3)
            for x in range(3, F.w, 4):
                F.line([(x, 43), (x, 47)], w3)                                               # the fingers
        constellation(fs['front'], [(4, 5), (11, 11), (6, 19)], [(0, 1), (1, 2)])
        fs['bottom'].fill(w2)
        for F in fs.values():
            F.shade(0.24); F.done()
    # ---- the legs: white all the way, the muscles' soft lines, the boots' soles
    for name in ('rleg', 'lleg'):
        fs = faces_of(im, P[name])
        for f, F in fs.items():
            white(F, 0, 0, F.w, F.h)
            if f in ('front', 'back'):
                F.line([(7, 4), (8, 20)], w3); F.line([(4, 22), (11, 22)], w3); F.line([(8, 26), (8, 40)], w3)
            if f not in ('top', 'bottom'):
                F.rect(0, 44, F.w, 47, w3); F.line([(0, 44), (F.w, 44)], w4)
        fs['bottom'].fill(w4)
        for F in fs.values():
            F.shade(0.26); F.done()
    return im

def tommy_ultimate():
    """The comics' Speed: a dark green suit with the lighter green bolt on its chest and pale sleeves, spiky white hair,
    silver goggles with yellow lenses."""
    im = Image.new('RGBA', (64 * HD, 64 * HD), (0, 0, 0, 0))
    P = hd_parts()
    Gd, Gm, Gl, Gp = hx(0x103A2A), hx(0x1C5A40), hx(0x4FC890), hx(0xBDF2D6)
    Gk = hx(0x08241A)
    skin, skin2, skin3 = hx(0xF2CDB0), hx(0xDDB092), hx(0xBE8A6E)
    hair, hair2, hair3 = hx(0xFAFCFF), hx(0xD8E2EE), hx(0xA8B6C8)
    silver, silver2, lens, lens2 = hx(0xD8DEE8), hx(0x8E98A8), hx(0xF2E85A), hx(0xFFFAB0)
    # ---- the head: the face, the goggles, white hair swept up in spikes
    fs = faces_of(im, P['head'])
    for f, F in fs.items():
        F.fill(hair if f != 'bottom' else skin2)
    F = fs['front']
    F.rect(0, 9, 32, 32, skin)
    F.poly([(0, 0), (32, 0), (32, 10), (28, 7), (25, 12), (21, 6), (17, 11), (13, 5), (9, 11), (5, 7), (0, 12)], hair)   # the fringe's spikes
    for x in (5, 13, 21, 28):
        F.line([(x, 2), (x - 1, 8)], hair2)
    F.rect(1, 13, 31, 20, silver2)                                                           # the goggles' strap and frame
    for x0 in (3, 18):
        F.poly([(x0, 13), (x0 + 11, 13), (x0 + 11, 19), (x0 + 9, 21), (x0 + 2, 21), (x0, 19)], silver)
        F.poly([(x0 + 1, 14), (x0 + 10, 14), (x0 + 10, 18), (x0 + 8, 20), (x0 + 3, 20), (x0 + 1, 18)], lens)
        F.line([(x0 + 2, 15), (x0 + 6, 15)], lens2, 1); F.px(x0 + 6, 17, hx(0x5A4A20)); F.px(x0 + 5, 17, hx(0x5A4A20))
    F.rect(14, 15, 17, 17, silver)
    F.line([(15, 21), (15, 25), (17, 25)], skin3)                                            # the nose
    F.rect(11, 27, 21, 29, hx(0xFFFFFF)); F.line([(11, 28), (21, 28)], skin3); F.line([(11, 27), (21, 27)], skin3)   # the gritted teeth
    for x in (14, 17):
        F.line([(x, 27), (x, 29)], skin3)
    F.line([(0, 22), (0, 31)], skin2); F.line([(31, 22), (31, 31)], skin2); F.line([(4, 31), (27, 31)], skin2)
    for f in ('left', 'right'):
        F2 = fs[f]
        front_x = (20, 32) if f == 'right' else (0, 12)
        F2.rect(front_x[0], 12, front_x[1], 32, skin)
        F2.rect(0, 14, 32, 18, silver2); F2.line([(0, 14), (32, 14)], silver)
        ex = 22 if f == 'right' else 6
        F2.oval(ex, 19, ex + 5, 25, skin2)                                                   # the ear
        for x in range(2, 30, 6):
            F2.line([(x, 2), (x + 2, 11)], hair2)
    F2 = fs['back']
    F2.rect(0, 14, 32, 18, silver2)
    for x in range(3, 30, 5):
        F2.line([(x, 2), (x + 1, 26)], hair2)
    F2.rect(0, 27, 32, 32, hair3)
    for x in range(3, 30, 6):
        fs['top'].line([(x, 3), (x + 3, 28)], hair2)
    for F in fs.values():
        F.shade(0.12); F.done()
    # (the second layer: the hair standing up in spikes off the head)
    hs = faces_of(im, P['hat'], ('front', 'back', 'left', 'right', 'top'))
    hs['top'].fill(hair)
    for x in range(2, 30, 5):
        hs['top'].line([(x, 4), (x + 3, 27)], hair2)
    for f in ('front', 'back', 'left', 'right'):
        F = hs[f]
        pts = [(0, 0), (F.w, 0)]
        tips = [(F.w, 6 if f != 'front' else 4)]
        for i in range(6, -1, -1):
            x = int(i * F.w / 6)
            tips.append((x, (9 if f != 'front' else 6) if i % 2 else 3))
        F.poly(pts + tips, hair)
        for i in range(1, 6, 2):
            x = int(i * F.w / 6)
            F.line([(x, 0), (x, 5)], hair2)
        F.done()
    hs['top'].done()
    # ---- the body: dark green, the lighter bolt on the chest
    fs = faces_of(im, P['body'])
    for f, F in fs.items():
        F.fill(Gd if f != 'bottom' else Gk)
    F = fs['front']
    F.poly([(10, 0), (22, 0), (20, 6), (16, 9), (12, 6)], skin2)                              # the open collar's throat
    F.poly([(4, 0), (10, 0), (12, 6), (16, 9), (20, 6), (22, 0), (28, 0), (30, 4), (21, 14), (25, 14), (14, 34), (17, 20), (11, 20)], Gl)   # the bolt
    F.line([(4, 0), (10, 0), (12, 6), (16, 9), (20, 6), (22, 0), (28, 0)], Gp)
    F.line([(30, 4), (21, 14), (25, 14), (14, 34)], Gm); F.line([(11, 20), (17, 20)], Gp)
    F.line([(8, 30), (8, 46)], Gm); F.line([(24, 30), (24, 46)], Gm); F.rect(0, 38, 32, 40, Gk); F.rect(13, 37, 19, 41, Gm)   # the seams, the belt
    F2 = fs['back']
    F2.line([(16, 2), (16, 36)], Gk); F2.poly([(6, 0), (26, 0), (20, 8), (12, 8)], Gm); F2.rect(0, 38, 32, 40, Gk)
    for f in ('left', 'right'):
        fs[f].rect(0, 38, fs[f].w, 40, Gk); fs[f].line([(8, 4), (8, 36)], Gm)
    fs['top'].fill(Gm)
    for F in fs.values():
        F.shade(0.18); F.done()
    # ---- the arms: the pale sleeves of the picture, darker at the shoulder, the gloves' cuffs
    for name in ('rarm', 'larm'):
        fs = faces_of(im, P[name])
        for f, F in fs.items():
            F.fill(Gl)
            if f in ('top', 'bottom'):
                F.fill(Gd if f == 'top' else Gp)
                continue
            F.rect(0, 0, F.w, 7, Gd); F.poly([(0, 7), (F.w, 7), (F.w // 2, 13)], Gd)          # the shoulder's point
            F.line([(0, 7), (F.w // 2, 13), (F.w, 7)], Gm)
            px = F.layer.load()
            for y in range(14, 48):
                for x in range(F.w):
                    px[x, y] = mul(mix(Gl, Gp, (y - 14) / 34.0), F.light) + (255,)
            F.rect(0, 32, F.w, 34, Gm); F.line([(0, 31), (F.w, 31)], Gp)                      # the glove's cuff
            for x in range(3, F.w, 4):
                F.line([(x, 43), (x, 47)], Gl)
        for F in fs.values():
            F.shade(0.26); F.done()
    # ---- the legs: dark green, a lighter flash down the outside, pale boots
    for name, mirror in (('rleg', False), ('lleg', True)):
        fs = faces_of(im, P[name])
        for f, F in fs.items():
            F.fill(Gd if f != 'bottom' else Gp)
            if f in ('top', 'bottom'):
                continue
            F.rect(0, 34, F.w, 48, Gl); F.poly([(0, 34), (F.w, 34), (F.w // 2, 29)], Gl); F.line([(0, 34), (F.w // 2, 29), (F.w, 34)], Gp)
            F.rect(0, 45, F.w, 47, Gm)
            if f in ('front', 'back'):
                F.line([(8, 2), (8, 26)], Gm)
        outer = fs['right' if not mirror else 'left']
        outer.poly([(6, 0), (10, 0), (9, 28), (7, 28)], Gl)
        for F in fs.values():
            F.shade(0.26); F.done()
    return im

def tommy_visionquest():
    """Tommy as the VisionQuest picture shows him from the chest up: short dark brown hair, a worn grey-green denim jacket
    with its collar, a cream T-shirt. (The picture ends at the chest: the dark jeans and shoes are plain guesses.)"""
    im = Image.new('RGBA', (64 * HD, 64 * HD), (0, 0, 0, 0))
    P = hd_parts()
    n = vnoise(88)
    skin, skin2, skin3 = hx(0xE8C0A2), hx(0xCFA284), hx(0xA87A62)
    hair, hair2, hair3 = hx(0x33241A), hx(0x4A3626), hx(0x21160F)
    J, J2, J3, Jl = hx(0x55605A), hx(0x424C47), hx(0x2E3632), hx(0x76827A)
    tee, tee2 = hx(0xE6DFCC), hx(0xC8C0AA)
    jean, jean2, jean3 = hx(0x232C3C), hx(0x18202C), hx(0x34405A)
    shoe, sole = hx(0x1A1816), hx(0x3A3632)
    def cloth(F, a, b, seed, x0=0, y0=0, x1=None, y1=None):
        px = F.layer.load()
        for y in range(y0, F.h if y1 is None else y1):
            for x in range(x0, F.w if x1 is None else x1):
                t = 0.5 + 0.9 * (n(x * 0.35 + seed, y * 0.18 + seed) - 0.5)
                px[x, y] = mul(mix(b, a, max(0.0, min(1.0, t))), F.light) + (255,)
    # ---- the head
    fs = faces_of(im, P['head'])
    for f, F in fs.items():
        F.fill(hair if f != 'bottom' else skin2)
    F = fs['front']
    F.rect(0, 8, 32, 32, skin)
    F.poly([(0, 0), (32, 0), (32, 12), (29, 9), (24, 8), (18, 9), (12, 7), (6, 8), (2, 10), (0, 13)], hair)        # the short fringe, brushed up
    for x in (4, 10, 17, 24):
        F.line([(x, 1), (x + 2, 7)], hair2)
    for x0 in (5, 19):
        F.line([(x0, 14), (x0 + 8, 14)], hair3, 2)                                            # the brows
        F.rect(x0 + 1, 17, x0 + 7, 19, hx(0xF4F1EC)); F.rect(x0 + 3, 17, x0 + 5, 19, hx(0x4A6A7A)); F.px(x0 + 4, 18, hx(0x101418))
        F.line([(x0 + 1, 16), (x0 + 7, 16)], skin3)
    F.line([(15, 18), (15, 23), (17, 24)], skin3); F.line([(14, 24), (17, 24)], skin2)        # the nose
    F.line([(12, 27), (20, 27)], hx(0x9A5A52)); F.line([(13, 28), (19, 28)], skin2)           # the mouth
    F.line([(0, 14), (0, 31)], skin2); F.line([(31, 14), (31, 31)], skin2); F.line([(6, 31), (25, 31)], skin2)
    for x in range(4, 28, 3):
        F.px(x, 29, skin2)                                                                    # a little stubble
    for f in ('left', 'right'):
        F2 = fs[f]
        front_x = (18, 32) if f == 'right' else (0, 14)
        F2.rect(front_x[0], 13, front_x[1], 32, skin)
        ex = 20 if f == 'right' else 7
        F2.oval(ex, 16, ex + 5, 23, skin2)
        for x in range(2, 30, 5):
            F2.line([(x, 1), (x + 1, 10)], hair2)
    for x in range(3, 30, 5):
        fs['back'].line([(x, 1), (x, 24)], hair2)
    fs['back'].rect(8, 28, 24, 32, skin2)
    for x in range(3, 30, 5):
        fs['top'].line([(x, 3), (x + 2, 28)], hair2)
    for F in fs.values():
        F.shade(0.12); F.done()
    hs = faces_of(im, P['hat'], ('front', 'top'))
    hs['top'].fill(hair)
    for x in range(3, 30, 5):
        hs['top'].line([(x, 4), (x + 2, 27)], hair2)
    hs['front'].poly([(0, 0), (32, 0), (32, 7), (26, 5), (19, 7), (12, 4), (5, 6), (0, 8)], hair)
    hs['front'].done(); hs['top'].done()
    # ---- the body: the cream T-shirt between the jacket's open fronts, the collar, the pockets
    fs = faces_of(im, P['body'])
    for i, (f, F) in enumerate(fs.items()):
        cloth(F, J, J2, 5 + i)
    F = fs['front']
    F.poly([(10, 0), (22, 0), (20, 40), (12, 40)], tee)
    F.poly([(12, 0), (20, 0), (16, 5)], skin2)                                                # the throat
    F.line([(12, 0), (16, 5), (20, 0)], tee2, 1)
    for y in (16, 28):
        F.line([(13, y), (19, y + 1)], tee2)                                                  # the shirt's folds
    F.poly([(4, 0), (11, 0), (12, 12), (8, 9)], Jl); F.poly([(28, 0), (21, 0), (20, 12), (24, 9)], Jl)   # the collar's points
    F.line([(4, 0), (8, 9), (12, 12)], J3); F.line([(28, 0), (24, 9), (20, 12)], J3)
    F.line([(11, 12), (12, 40)], J3, 1); F.line([(21, 12), (20, 40)], J3, 1)                  # the open fronts' edges
    for x0 in (2, 22):
        F.rect(x0, 16, x0 + 7, 24, J2); F.line([(x0, 16), (x0 + 7, 16)], Jl); F.poly([(x0, 16), (x0 + 7, 16), (x0 + 3, 20)], J3)   # the chest pockets
        F.px(x0 + 3, 18, hx(0xB0A890))
    for x in (9, 23):
        for y in range(14, 40, 6):
            F.px(x, y, hx(0xB0A890))                                                          # the buttons
    F.rect(0, 40, 32, 42, J3); F.rect(0, 42, 32, 48, jean)
    F2 = fs['back']
    F2.line([(4, 10), (28, 10)], J3); F2.line([(16, 10), (16, 40)], J3); F2.rect(4, 0, 28, 3, Jl); F2.rect(0, 40, 32, 42, J3); F2.rect(0, 42, 32, 48, jean)
    for f in ('left', 'right'):
        fs[f].rect(0, 40, fs[f].w, 42, J3); fs[f].rect(0, 42, fs[f].w, 48, jean); fs[f].line([(8, 6), (8, 40)], J3)
    fs['top'].fill(J)
    for F in fs.values():
        F.shade(0.18); F.done()
    # ---- the arms: the jacket's sleeves, their seams and cuffs, the hands
    for k, name in enumerate(('rarm', 'larm')):
        fs = faces_of(im, P[name])
        for i, (f, F) in enumerate(fs.items()):
            cloth(F, J, J2, 21 + k * 7 + i)
            if f == 'bottom':
                F.fill(skin2)
            if f in ('top', 'bottom'):
                continue
            F.line([(0, 6), (F.w, 6)], J3)
            for y in (18, 27):
                F.line([(1, y), (F.w - 2, y + 1)], J2)                                        # the elbow's creases
            F.rect(0, 36, F.w, 39, J3); F.line([(0, 36), (F.w, 36)], Jl)
            F.rect(0, 40, F.w, 48, skin)
            for x in range(3, F.w, 4):
                F.line([(x, 44), (x, 47)], skin2)
        for F in fs.values():
            F.shade(0.26); F.done()
    # ---- the legs (not in the picture): plain dark jeans, dark shoes
    for k, name in enumerate(('rleg', 'lleg')):
        fs = faces_of(im, P[name])
        for i, (f, F) in enumerate(fs.items()):
            cloth(F, jean3, jean2, 41 + k * 5 + i)
            if f == 'bottom':
                F.fill(sole)
            if f in ('top', 'bottom'):
                continue
            F.line([(8, 0), (8, 40)], jean2)
            F.line([(2, 22), (13, 23)], jean2)
            F.rect(0, 40, F.w, 48, shoe); F.rect(0, 46, F.w, 48, sole); F.line([(0, 40), (F.w, 40)], jean2)
        for F in fs.values():
            F.shade(0.26); F.done()
    return im

if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else SUIT
    nexus(True).save(out + 'scarlet_witch_nexus.png')
    nexus(False).save(out + 'scarlet_witch_nexus_plain.png')
    nexus_cape().save(out + 'scarlet_witch_nexus_cape.png')
    demiurge_idle().save(out + 'billy_demiurge.png')
    demiurge_powered().save(out + 'billy_demiurge_powered.png')
    cloak().save(out + 'billy_demiurge_cloak.png')
    cosmic_spider().save(out + 'spider_man_cosmic.png')
    godkiller().save(out + 'iron_man_godkiller.png')
    tommy_ultimate().save(out + 'tommy_ultimate.png')
    # (a look of the ordinary game is an ordinary 64x64 skin: painted fine, then brought down to it)
    vq = tommy_visionquest().resize((64, 64), Image.BOX)
    vq.putdata([(r, g, b, 255 if a >= 128 else 0) if a >= 128 else (0, 0, 0, 0) for r, g, b, a in vq.getdata()])
    vq.save(out + 'tommy_visionquest.png')
    molten().save(out + 'vision_molten.png')
    for i in range(4):
        cracks(i).save(out + 'vision_molten_cracks_%d.png' % i)
