"""Paints 3.17's Ultimate Mode suits (run from anywhere; writes into the mod's textures):
the Nexus Being (Wanda: her film face and crown untouched, a sleek red suit with black), the Demiurge at rest and
powered (Billy: his Teen face; the body of stars), the Nexus cape and the Demiurge's cloth cloak."""
import math, random, sys
from PIL import Image

SUIT = '/home/user/Enhanced/src/main/resources/assets/enhanced/textures/entity/suit/'

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
                return (40, 10, 0)
            u = offs[face] + x
            g = glow.get((u, y))
            if g:
                return mix(hx(0xFF8A1E), hx(0xFFF6C8), min(1.0, g * 1.15)) if g > 0.45 else mul(hx(0xFF8A1E), g * 2.0)
            near = max([glow.get((u + a, y + b), 0.0) for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))] or [0.0])
            # the whole body glows a little of itself, more beside a crack, and at its edges (the fire's rim)
            edge = 0.16 if x in (0, w - 1) else 0.0
            return mul(hx(0xFF6A1A), 0.14 + 0.4 * near + edge)
        paint(im, faces, fn)
    x0, y0, _, _ = P['head']['front']
    for x in (1, 2, 5, 6):
        im.putpixel((x0 + x, y0 + 4), (255, 250, 220, 255))
    return im

if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else SUIT
    nexus(True).save(out + 'scarlet_witch_nexus.png')
    nexus(False).save(out + 'scarlet_witch_nexus_plain.png')
    nexus_cape().save(out + 'scarlet_witch_nexus_cape.png')
    demiurge_idle().save(out + 'billy_demiurge.png')
    demiurge_powered().save(out + 'billy_demiurge_powered.png')
    cloak().save(out + 'billy_demiurge_cloak.png')
    molten().save(out + 'vision_molten.png')
    for i in range(4):
        cracks(i).save(out + 'vision_molten_cracks_%d.png' % i)
