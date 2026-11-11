#!/usr/bin/env python3
"""The symbols of the heroes' abilities (3.10): one round badge per ability, in its hero's colours, with a motif that
says what it does (a bolt, a shield, a fist...). Which motif an ability gets is in tools/symbols.json
("ability.enhanced.<hero>.<n>": "<motif>"). Drawn at 128 px and shrunk to 32 px:
src/main/resources/assets/enhanced/textures/gui/ability/<hero>_<n>.png. Run from the repository root.
The pen and the motifs are the ones of Enlightened's tools/symbols.py."""
import json
import math
import os
import re

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/assets/enhanced/textures/gui/ability")
S = 128


def P(x, y):
    return (x * S / 100.0, y * S / 100.0)


class Pen:
    def __init__(self, d, light, accent, dark):
        self.d, self.light, self.accent, self.dark = d, light, accent, dark

    def poly(self, pts, c=None):
        self.d.polygon([P(*p) for p in pts], fill=c or self.light)

    def line(self, pts, w=6, c=None):
        self.d.line([P(*p) for p in pts], fill=c or self.light, width=int(w * S / 100), joint="curve")
        r = w / 2
        for p in (pts[0], pts[-1]):
            self.disc(p[0], p[1], r, c)

    def disc(self, x, y, r, c=None):
        self.d.ellipse([P(x - r, y - r), P(x + r, y + r)], fill=c or self.light)

    def ring(self, x, y, r, w=5, c=None):
        self.d.ellipse([P(x - r, y - r), P(x + r, y + r)], outline=c or self.light, width=int(w * S / 100))

    def arc(self, x, y, r, a0, a1, w=6, c=None):
        self.d.arc([P(x - r, y - r), P(x + r, y + r)], a0, a1, fill=c or self.light, width=int(w * S / 100))

    def rect(self, x0, y0, x1, y1, c=None):
        self.d.rectangle([P(x0, y0), P(x1, y1)], fill=c or self.light)

    def wave(self, x0, x1, y, amp, waves=1.5, w=6, c=None, phase=0.0):
        pts = [(x0 + (x1 - x0) * i / 30.0, y + amp * math.sin(phase + i / 30.0 * waves * 2 * math.pi)) for i in range(31)]
        self.line(pts, w, c)

    def spiral(self, x, y, r, turns=2.0, w=5, c=None):
        pts = []
        for i in range(61):
            t = i / 60.0
            a = t * turns * 2 * math.pi
            pts.append((x + math.cos(a) * r * t, y + math.sin(a) * r * t))
        self.line(pts, w, c)

    def flame(self, x, y, h, c=None, inner=None):
        w = h * 0.55
        pts = [(x, y - h)]
        for i in range(1, 20):
            t = i / 20.0
            pts.append((x + w * math.sin(t * math.pi) * (0.6 + 0.4 * t), y - h + h * t))
        pts.append((x, y))
        for i in range(19, 0, -1):
            t = i / 20.0
            pts.append((x - w * math.sin(t * math.pi) * (0.6 + 0.4 * t), y - h + h * t))
        self.poly(pts, c or self.accent)
        if inner is not False:
            self.flame_inner(x, y, h * 0.55, inner or self.light)

    def flame_inner(self, x, y, h, c):
        w = h * 0.5
        pts = [(x, y - h)] + [(x + w * math.sin(i / 12.0 * math.pi), y - h + h * i / 12.0) for i in range(1, 12)] + [(x, y)] + \
              [(x - w * math.sin(i / 12.0 * math.pi), y - h + h * i / 12.0) for i in range(11, 0, -1)]
        self.poly(pts, c)

    def drop(self, x, y, h, c=None):
        r = h * 0.32
        pts = [(x, y - h / 2)]
        for i in range(0, 181, 10):
            a = math.radians(i)
            pts.append((x + r * math.cos(a - math.pi / 2 + math.pi / 2) if False else x + r * math.sin(a), y + h / 2 - r + r * -math.cos(a)))
        self.poly([(x, y - h / 2), (x + r, y + h / 2 - r)] + [(x + r * math.cos(math.radians(a)), y + h / 2 - r + r * math.sin(math.radians(a))) for a in range(0, 181, 10)] + [(x - r, y + h / 2 - r)], c)

    def bolt(self, x, y, h, c=None):
        s = h / 60.0
        pts = [(x + 6 * s, y - 30 * s), (x - 14 * s, y + 4 * s), (x - 1 * s, y + 4 * s), (x - 8 * s, y + 30 * s), (x + 14 * s, y - 6 * s), (x + 1 * s, y - 6 * s)]
        self.poly(pts, c)

    def arrow(self, x0, y0, x1, y1, w=6, head=10, c=None):
        self.line([(x0, y0), (x1, y1)], w, c)
        a = math.atan2(y1 - y0, x1 - x0)
        self.poly([(x1 + math.cos(a) * head * 0.6, y1 + math.sin(a) * head * 0.6),
                   (x1 + math.cos(a + 2.4) * head, y1 + math.sin(a + 2.4) * head),
                   (x1 + math.cos(a - 2.4) * head, y1 + math.sin(a - 2.4) * head)], c)

    def star(self, x, y, r, n=4, inner=0.35, c=None, rot=0.0):
        pts = []
        for i in range(n * 2):
            a = rot + i * math.pi / n
            rr = r if i % 2 == 0 else r * inner
            pts.append((x + math.cos(a) * rr, y + math.sin(a) * rr))
        self.poly(pts, c)

    def hand(self, x, y, s=1.0, c=None):
        self.rect(x - 9 * s, y - 2 * s, x + 9 * s, y + 14 * s, c)
        for i, h in enumerate((14, 18, 17, 13)):
            fx = x - 7.5 * s + i * 5 * s
            self.line([(fx, y), (fx, y - h * s)], 4 * s, c)
        self.line([(x - 9 * s, y + 6 * s), (x - 16 * s, y - 2 * s)], 4.5 * s, c)

    def foot(self, x, y, s=1.0, c=None):
        self.d.ellipse([P(x - 6 * s, y - 8 * s), P(x + 6 * s, y + 12 * s)], fill=c or self.light)
        for i in range(4):
            self.disc(x - 6 * s + i * 4 * s, y - 13 * s + abs(i - 1) * 1.5 * s, 2.2 * s, c)

    def bricks(self, x0, y0, x1, y1, rows=3, c=None):
        h = (y1 - y0) / rows
        for r in range(rows):
            off = 0 if r % 2 == 0 else (x1 - x0) / 4
            n = 2
            bw = (x1 - x0) / n
            for b in range(-1, n + 1):
                bx0 = x0 + b * bw + off + 1.5
                bx1 = bx0 + bw - 3
                bx0, bx1 = max(bx0, x0), min(bx1, x1)
                if bx1 - bx0 > 2:
                    self.rect(bx0, y0 + r * h + 1.5, bx1, y0 + (r + 1) * h - 1.5, c)

    def rock(self, x, y, r, c=None):
        pts = [(x + math.cos(a) * r * k, y + math.sin(a) * r * k) for a, k in
               zip([i * math.pi / 4 + 0.3 for i in range(8)], (1, 0.85, 1.05, 0.9, 1, 0.8, 1.05, 0.9))]
        self.poly(pts, c)

    def figure(self, x, y, s=1.0, c=None):
        self.disc(x, y - 22 * s, 6 * s, c)
        self.line([(x, y - 15 * s), (x, y + 4 * s)], 6 * s, c)
        self.line([(x, y + 4 * s), (x - 7 * s, y + 20 * s)], 5 * s, c)
        self.line([(x, y + 4 * s), (x + 7 * s, y + 20 * s)], 5 * s, c)


def m_whip(p):
    p.line([(26, 72), (34, 64)], 8, p.accent)
    p.wave(34, 78, 50, 14, 1.25, 6, phase=math.pi)


def m_heal(p):
    p.drop(50, 50, 52)
    p.rect(46, 44, 54, 66, p.d_base)
    p.rect(39, 51, 61, 59, p.d_base)


def m_octopus(p):
    p.disc(50, 40, 13)
    for i in range(6):
        x = 30 + i * 8
        p.wave(x, x + 2, 52, 3, 1, 4)
        p.line([(x + 2, 52), (x - 4 + (i % 2) * 8, 78)], 4)


def m_spout(p, c=None):
    p.poly([(42, 80), (58, 80), (54, 40), (46, 40)], c)
    p.d.ellipse([P(30, 28), P(70, 44)], outline=c or p.light, width=int(5 * S / 100))
    p.figure(50, 30, 0.45, p.accent)


def m_arms(p):
    p.figure(50, 52, 0.7, p.accent)
    p.wave(22, 40, 40, 6, 1, 6)
    p.wave(60, 78, 40, 6, 1, 6, phase=math.pi)


def m_leaf(p):
    p.d.chord([P(28, 26), P(84, 82)], 110, 290, fill=p.light)
    p.line([(30, 76), (70, 36)], 4, p.d_base)


def m_knives(p):
    for dx, rot in ((-14, -0.35), (0, 0), (14, 0.35)):
        x = 50 + dx
        p.poly([(x + math.sin(rot) * 26, 50 - 26 * math.cos(rot)), (x + 5, 60), (x - 5, 60)])
        p.rect(x - 2, 60, x + 2, 72, p.accent)


def m_snowflake(p, c=None):
    for i in range(3):
        a = i * math.pi / 3
        dx, dy = math.cos(a) * 26, math.sin(a) * 26
        p.line([(50 - dx, 50 - dy), (50 + dx, 50 + dy)], 5, c)
        for sgn in (1, -1):
            bx, by = 50 + sgn * dx * 0.6, 50 + sgn * dy * 0.6
            for b in (0.6, -0.6):
                p.line([(bx, by), (bx + sgn * math.cos(a + b) * 9, by + sgn * math.sin(a + b) * 9)], 3.5, c)


def m_snowball(p):
    p.disc(46, 54, 18)
    p.disc(41, 49, 6, p.accent)
    p.arrow(66, 36, 80, 24, 4, 9)


def m_wall(p, c=None):
    p.bricks(24, 30, 76, 76, 3, c)


def m_ice_wall(p):
    for i, h in enumerate((30, 44, 36, 48, 32)):
        x = 26 + i * 12
        p.poly([(x - 5, 78), (x + 5, 78), (x + 3, 78 - h), (x, 74 - h - 6), (x - 3, 78 - h)])


def m_blood(p):
    p.hand(50, 52, 1.4)
    for i in range(3):
        p.line([(36 + i * 14, 24), (36 + i * 14, 14)], 2.5, p.accent)


def m_suppress(p):
    p.figure(50, 54, 0.8)
    p.arrow(28, 20, 28, 38, 4, 8, p.accent)
    p.arrow(72, 20, 72, 38, 4, 8, p.accent)


def m_shield(p):
    p.arc(50, 64, 30, 180, 360, 7)
    p.figure(50, 62, 0.6, p.accent)


def m_bubble(p):
    p.ring(50, 50, 26, 5)
    p.disc(41, 40, 5)
    p.figure(50, 56, 0.55, p.accent)


def m_phase(p):
    p.drop(38, 50, 44)
    p.star(64, 50, 16, 6, 0.4, p.accent)


def m_breath(p):
    for i in range(3):
        p.wave(28, 78, 34 + i * 16, 4, 1.5, 4.5, phase=i)
    m_snowflake_small(p, 74, 74)


def m_snowflake_small(p, x, y):
    for i in range(3):
        a = i * math.pi / 3
        p.line([(x - math.cos(a) * 8, y - math.sin(a) * 8), (x + math.cos(a) * 8, y + math.sin(a) * 8)], 3, p.accent)


def m_tether(p):
    p.wave(22, 78, 50, 10, 1.5, 5)
    p.d.chord([P(60, 22), P(80, 46)], 110, 290, fill=p.accent)
    p.d.chord([P(22, 56), P(42, 80)], 290, 110, fill=p.accent)


def m_vortex(p):
    p.spiral(50, 50, 30, 2.4, 5)


def m_swim(p):
    p.poly([(26, 50), (40, 38), (66, 42), (76, 50), (66, 58), (40, 62)])
    p.poly([(26, 50), (16, 38), (18, 62)])
    p.disc(64, 47, 2.5, p.d_base)
    p.wave(24, 78, 76, 3, 2, 3.5, p.accent)


def m_step(p):
    p.foot(40, 60, 1.0)
    p.foot(62, 36, 1.0, p.accent)


def m_drop(p):
    p.drop(50, 50, 56)


def m_fireball(p):
    p.disc(56, 46, 16, p.accent)
    p.disc(56, 46, 9)
    for i in range(3):
        p.line([(22 + i * 3, 64 + i * 6 - 6), (40, 54 + i * 4 - 4)], 4, p.accent)


def m_fist(p):
    p.rect(34, 42, 62, 70)
    for i in range(4):
        p.disc(37 + i * 8, 42, 4.5)
    p.flame(68, 46, 26, p.accent, False)


def m_ring(p):
    p.ring(50, 54, 26, 7, p.accent)
    for i in range(6):
        a = i * math.pi / 3
        p.flame(50 + math.cos(a) * 26, 54 + math.sin(a) * 26 + 4, 14, p.accent, False)
    p.figure(50, 58, 0.5)


def m_bolt(p):
    p.bolt(50, 50, 64)


def m_redirect(p):
    p.bolt(34, 44, 40)
    p.arc(52, 52, 24, 200, 350, 5, p.accent)
    p.arrow(70, 46, 76, 60, 5, 9, p.accent)


def m_jet(p):
    p.figure(50, 36, 0.6)
    p.flame(42, 82, -26, p.accent, False)
    p.flame(58, 82, -26, p.accent, False)


def m_flame(p):
    p.flame(50, 80, 56)


def m_fire_wall(p):
    for i in range(4):
        p.flame(28 + i * 14.5, 78, 38 + (i % 2) * 10, p.accent, False)
    p.rect(22, 74, 78, 79, p.accent)


def m_blaze(p):
    for i in range(5):
        a = math.radians(-60 + i * 30)
        x, y = 50 + math.sin(a) * 30, 74 - math.cos(a) * 30
        p.flame(x, y + 8, 18, p.accent, False)
    p.hand(50, 70, 0.9)


def m_heat(p):
    p.rect(46, 24, 54, 64)
    p.disc(50, 68, 10)
    p.rect(48.5, 40, 51.5, 66, p.accent)
    p.disc(50, 68, 6, p.accent)
    for i in range(3):
        p.line([(58, 30 + i * 10), (64, 30 + i * 10)], 3)


def m_sun(p):
    p.disc(50, 50, 14)
    for i in range(8):
        a = i * math.pi / 4
        p.line([(50 + math.cos(a) * 21, 50 + math.sin(a) * 21), (50 + math.cos(a) * 29, 50 + math.sin(a) * 29)], 5)


def m_palm(p):
    p.hand(50, 62, 1.1)
    p.flame(50, 42, 22, p.accent, False)


def m_combustion(p):
    p.disc(50, 56, 22)
    p.disc(50, 56, 8, p.accent)
    p.d.ellipse([P(38, 24), P(62, 34)], fill=p.accent)
    p.disc(50, 29, 3, p.d_base)


def m_avatar_state(p):
    p.poly([(50, 18), (60, 44), (50, 40), (40, 44)])
    p.poly([(50, 82), (60, 56), (50, 60), (40, 56)])
    p.ring(50, 50, 30, 4, p.accent)


def m_energy(p):
    p.spiral(50, 50, 28, 2.0, 4)
    p.star(50, 50, 9, 4, 0.4, p.accent)


def m_spirit(p):
    p.poly([(36, 76), (36, 44), (50, 28), (64, 44), (64, 76), (57, 70), (50, 76), (43, 70)])
    p.disc(44, 46, 3.5, p.d_base)
    p.disc(56, 46, 3.5, p.d_base)


def m_sphere(p):
    cols = [(210, 240, 255), (226, 205, 150), (255, 140, 40), (235, 248, 255)]
    for i, c in enumerate([(60, 140, 230), (110, 160, 60), (230, 80, 30), (240, 220, 160)]):
        p.d.pieslice([P(22, 22), P(78, 78)], i * 90, i * 90 + 90, fill=c)
    p.ring(50, 50, 28, 4)


def m_restore(p):
    m_energy(p)
    p.rect(68, 22, 72, 36, p.accent)
    p.rect(63, 27, 77, 31, p.accent)


def m_rock(p):
    p.rock(50, 46, 20)
    p.rock(46, 42, 8, p.accent)
    p.arrow(28, 76, 40, 64, 4, 8)


def m_lift(p):
    p.rock(50, 38, 16)
    p.hand(50, 72, 0.9, p.accent)


def m_armor(p, c=None):
    p.poly([(30, 30), (42, 26), (50, 32), (58, 26), (70, 30), (68, 50), (64, 76), (36, 76), (32, 50)], c)
    p.line([(50, 36), (50, 72)], 3, p.d_base)


def m_pillar(p):
    p.rect(40, 30, 60, 78)
    p.rect(36, 74, 64, 80, p.accent)
    p.arrow(50, 26, 50, 16, 4, 9, p.accent)


def m_seismic(p):
    p.foot(50, 66, 0.9)
    for r in (16, 24, 32):
        p.arc(50, 68, r, 200, 340, 3.5, p.accent)


def m_dunes(p, c=None):
    p.d.pieslice([P(14, 50), P(66, 100)], 180, 360, fill=c or p.light)
    p.d.pieslice([P(40, 42), P(92, 96)], 180, 360, fill=p.accent)


def m_sand_lift(p):
    m_dunes(p)
    p.arrow(50, 46, 50, 18, 5, 10)


def m_quicksand(p):
    p.spiral(50, 56, 26, 2.2, 5)
    p.figure(50, 34, 0.45, p.accent)


def m_lava_path(p):
    p.poly([(30, 80), (70, 80), (58, 30), (42, 30)], p.accent)
    p.wave(40, 60, 44, 2, 1, 3)
    p.wave(36, 64, 62, 2, 1, 3)


def m_shuriken(p):
    p.star(50, 50, 26, 4, 0.3, p.accent, math.pi / 4)
    p.disc(50, 50, 5, p.d_base)


def m_lava_skin(p):
    m_armor(p, p.accent)
    p.flame(50, 64, 20, p.light, False)


def m_blades(p):
    for i in range(3):
        x = 32 + i * 18
        p.poly([(x, 24), (x + 6, 60), (x, 76), (x - 6, 60)])


def m_magnet(p):
    p.arc(50, 46, 20, 180, 360, 10)
    p.rect(30, 46, 40, 66)
    p.rect(60, 46, 70, 66)
    p.rect(30, 66, 40, 74, p.accent)
    p.rect(60, 66, 70, 74, p.accent)


def m_wreck(p):
    p.bricks(24, 40, 76, 78, 3)
    p.line([(48, 40), (56, 54), (46, 62), (54, 78)], 4, p.d_base)
    p.disc(60, 26, 9, p.accent)


def m_shockwave(p):
    for r, w in ((10, 7), (20, 5), (30, 4)):
        p.arc(50, 64, r, 180, 360, w)
    p.rect(20, 64, 80, 70, p.accent)


def m_catapult(p):
    p.rect(26, 70, 74, 78, p.accent)
    p.figure(50, 46, 0.6)
    p.arrow(50, 22, 50, 10, 4, 8)


def m_grab(p):
    p.hand(50, 34, 1.0)
    p.rock(50, 70, 13, p.accent)


def m_tunnel(p):
    p.disc(50, 56, 26, p.accent)
    p.disc(50, 56, 16, p.d_base)
    p.rect(20, 76, 80, 82, p.accent)


def m_density(p):
    p.poly([(36, 42), (64, 42), (74, 78), (26, 78)])
    p.ring(50, 34, 8, 4)


def m_extract(p):
    p.poly([(50, 24), (66, 40), (50, 76), (34, 40)], p.accent)
    p.poly([(50, 24), (58, 40), (50, 76), (42, 40)])


def m_gust(p, n=3):
    for i in range(n):
        y = 34 + i * 16
        p.line([(24, y), (64 - i * 6, y)], 5)
        p.arc(64 - i * 6, y - 7, 7, 270, 90 + 180, 5)


def m_air_blast(p):
    m_gust(p, 3)


def m_air_jump(p):
    p.figure(50, 40, 0.6)
    p.arc(50, 78, 14, 200, 340, 4, p.accent)
    p.arc(50, 82, 22, 210, 330, 4, p.accent)


def m_air_blades(p):
    for i in range(3):
        p.arc(40 + i * 10, 50, 22 - i * 4, 300, 60, 5)


def m_projection(p):
    m_spirit(p)


def m_scooter(p):
    p.ring(50, 64, 16, 5)
    p.arc(50, 64, 10, 30, 210, 4, p.accent)
    p.figure(50, 30, 0.5, p.accent)


def m_tornado(p):
    for i in range(6):
        w = 30 - i * 4.5
        p.line([(50 - w, 24 + i * 10), (50 + w, 24 + i * 10)], 5)


def m_flight(p):
    p.figure(50, 54, 0.55, p.accent)
    for sgn in (1, -1):
        p.poly([(50 + sgn * 4, 40), (50 + sgn * 34, 26), (50 + sgn * 30, 38), (50 + sgn * 36, 42), (50 + sgn * 28, 50), (50 + sgn * 8, 50)])


def m_suffocate(p):
    p.disc(50, 50, 13, p.accent)
    p.ring(50, 50, 24, 5)
    p.spiral(50, 50, 22, 1.0, 3)


def m_suction(p):
    for a in range(4):
        ang = a * math.pi / 2 + math.pi / 4
        p.arrow(50 + math.cos(ang) * 30, 50 + math.sin(ang) * 30, 50 + math.cos(ang) * 12, 50 + math.sin(ang) * 12, 4, 8)


def m_burst(p):
    p.disc(50, 50, 8, p.accent)
    for a in range(8):
        ang = a * math.pi / 4
        p.arrow(50 + math.cos(ang) * 14, 50 + math.sin(ang) * 14, 50 + math.cos(ang) * 30, 50 + math.sin(ang) * 30, 3.5, 7)


def m_air_spout(p):
    p.poly([(44, 80), (56, 80), (60, 44), (40, 44)])
    p.figure(50, 34, 0.45, p.accent)


def m_feather(p):
    p.d.chord([P(30, 20), P(70, 84)], 60, 300, fill=p.light)
    p.line([(56, 80), (46, 26)], 3, p.d_base)


def m_air_bubble(p):
    p.ring(50, 50, 26, 5)
    p.wave(34, 66, 50, 4, 1, 4, p.accent)


def m_mud(p, c=(120, 80, 40)):
    p.d.pieslice([P(18, 40), P(82, 104)], 180, 360, fill=c)
    p.wave(22, 78, 52, 6, 1.5, 6, (170, 120, 70))


def m_mud_throw(p):
    p.disc(56, 44, 15, (120, 80, 40))
    p.disc(52, 40, 5, (170, 120, 70))
    p.arrow(26, 74, 40, 60, 4, 8)


def m_giant(p):
    p.figure(50, 54, 1.25, (90, 160, 70))
    p.d.chord([P(30, 14), P(46, 32)], 110, 290, fill=(140, 200, 90))
    p.d.chord([P(54, 14), P(70, 32)], 250, 70, fill=(140, 200, 90))


def m_great_lift(p):
    p.rect(20, 34, 80, 48, p.accent)
    p.figure(38, 30, 0.35)
    p.rock(62, 28, 6)
    p.arrow(50, 76, 50, 56, 6, 12)


def m_frost_walk(p):
    p.foot(50, 46, 1.1)
    p.rect(22, 70, 78, 78, p.accent)
    m_snowflake_small(p, 72, 30)


MOTIFS = {name[2:]: fn for name, fn in list(globals().items()) if name.startswith("m_") and callable(fn)}


def rgb(value):
    return ((value >> 16) & 255, (value >> 8) & 255, value & 255)


def heroes():
    """Each hero's two colours, from Power.java: NAME(0xMAIN, 0xLIGHT)."""
    src = open(os.path.join(ROOT, "src/main/java/com/kolodkingal/enhanced/power/Power.java")).read()
    return {name.lower(): (rgb(int(main, 16)), rgb(int(light, 16)))
            for name, main, light in re.findall(r"^    ([A-Z_]+)\(0x([0-9A-Fa-f]{6}), 0x([0-9A-Fa-f]{6})\)", src, re.M)}


def main():
    os.makedirs(OUT, exist_ok=True)
    mapping = json.load(open(os.path.join(ROOT, "tools", "symbols.json")))
    colours = heroes()
    names = []
    for key, motif_name in sorted(mapping.items()):
        parts = key.split(".")[2:]
        hero = parts[0]
        main_c, light = colours[hero]
        # a badge darker than the hero's colour, so that the light motif reads on every hero
        base = tuple(int(c * 0.55) for c in main_c)
        dark = tuple(int(c * 0.25) for c in main_c)
        accent = tuple(min(255, int(c * 1.15) + 20) for c in main_c)
        img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        d.ellipse([P(3, 3), P(97, 97)], fill=dark + (255,))
        d.ellipse([P(6, 6), P(94, 94)], fill=base + (255,))
        d.ellipse([P(10, 12), P(90, 92)], fill=tuple(int(c * 0.82) for c in base) + (255,))
        d.ellipse([P(6, 6), P(94, 94)], outline=light + (255,), width=int(3 * S / 100))
        pen = Pen(d, light, accent, dark)
        pen.d_base = tuple(int(c * 0.82) for c in base)
        MOTIFS[motif_name](pen)
        name = "_".join(parts)
        img.resize((32, 32), Image.LANCZOS).save(os.path.join(OUT, name + ".png"))
        names.append(name)
    os.makedirs(os.path.join(ROOT, "build"), exist_ok=True)
    sheet = Image.new("RGBA", (10 * 36, ((len(names) + 9) // 10) * 36), (30, 30, 40, 255))
    for i, name in enumerate(names):
        sheet.paste(Image.open(os.path.join(OUT, name + ".png")), ((i % 10) * 36 + 2, (i // 10) * 36 + 2))
    sheet.resize((sheet.width * 3, sheet.height * 3), Image.NEAREST).save(os.path.join(ROOT, "build", "symbols_sheet.png"))
    print("drew", len(names), "symbols")


if __name__ == "__main__":
    main()
