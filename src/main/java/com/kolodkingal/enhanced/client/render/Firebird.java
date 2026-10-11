package com.kolodkingal.enhanced.client.render;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The firebird of the Phoenix: a bird made of ribbons of flame, built from a skeleton and drawn the same at every size
 * ({@code scale} is the only thing that changes). It forms behind and above a charging Phoenix, and on the Burst it
 * is flung open and flies out along her facing. This class only builds the strands as points in the world; {@link
 * PhoenixFx} draws them.
 *
 * <p>The skeleton, for a bird of scale 1 whose chest is at the origin: a spine curving up from the tail 3 blocks behind
 * to the head 2.2 ahead and 1 above; a head ring of radius 0.35 with a two-ribbon beak and three crest ribbons
 * sweeping back; two wings of seven primary feathers (3.0 to 6.0 long, the longest at the tip) over a second layer of
 * five shorter ones; and a tail of five long streamers that wave.
 */
public final class Firebird {
    /** What a strand is, so that the drawing can give each its own width. */
    public enum Part {
        SPINE, HEAD, BEAK, CREST, PRIMARY, COVERT, TAIL
    }

    /**
     * One ribbon of the bird: points from its root to its tip (world positions), the width it is drawn at, and what
     * part it is.
     */
    public record Strand(Part part, Vec3[] points, float width) {
    }

    /** How many primary and second-layer feathers one wing has, and how many streamers the tail. */
    public static final int PRIMARIES = 7;
    public static final int COVERTS = 5;
    public static final int STREAMERS = 5;
    public static final int CRESTS = 3;
    /** The segments of a feather, a streamer and the spine. */
    private static final int SEGMENTS = 8;

    /** The length of primary feather {@code i} (0 to 6) of a bird of scale 1: 3.0, 3.5, ... 6.0. */
    public static float primaryLength(int i) {
        return 3.0F + 0.5F * i;
    }

    /** The length of tail streamer {@code i} (0 to 4) of a bird of scale 1: 2.5, 2.9, ... 4.1. */
    public static float streamerLength(int i) {
        return 2.5F + 0.4F * i;
    }

    /**
     * Builds the bird.
     *
     * @param chest   where its chest is
     * @param forward the way it faces (horizontal or not, unit)
     * @param up      its up (unit, at right angles to {@code forward})
     * @param scale   its size
     * @param flap    the angle of the wings above level, in degrees (50 up, -20 flung down)
     * @param time    ticks, for the waving of the tail and the crest
     * @param wings   whether the wings are out (a bird that has only just begun to form is a body and a head)
     */
    public static List<Strand> build(Vec3 chest, Vec3 forward, Vec3 up, float scale, float flap, float time, boolean wings) {
        Vec3 f = forward.normalize();
        Vec3 u = up.normalize();
        Vec3 r = f.cross(u).normalize();
        // The frame: r is to its right when looking along f (f x u points to the right of a viewer facing f).
        List<Strand> strands = new ArrayList<>();

        // ---- the spine, tail to head ----
        Vec3 tail = chest.subtract(f.scale(3.0F * scale)).subtract(u.scale(0.3F * scale));
        Vec3 control = chest.add(f.scale(0.6F * scale)).subtract(u.scale(0.1F * scale));
        Vec3 head = chest.add(f.scale(2.2F * scale)).add(u.scale(1.0F * scale));
        strands.add(new Strand(Part.SPINE, bezier(tail, control, head, SEGMENTS), 0.20F * scale));

        // ---- the head: a ring facing forward and one facing sideways, a beak, a crest ----
        float headRadius = 0.35F * scale;
        strands.add(new Strand(Part.HEAD, circle(head, r, u, headRadius, 12), 0.07F * scale));
        strands.add(new Strand(Part.HEAD, circle(head, f, u, headRadius, 12), 0.07F * scale));
        strands.add(new Strand(Part.BEAK, line(head.add(f.scale(0.3F * scale)).add(u.scale(0.04F * scale)), head.add(f.scale(0.85F * scale)).subtract(u.scale(0.05F * scale)), 2), 0.09F * scale));
        strands.add(new Strand(Part.BEAK, line(head.add(f.scale(0.3F * scale)).subtract(u.scale(0.06F * scale)), head.add(f.scale(0.75F * scale)).subtract(u.scale(0.2F * scale)), 2), 0.06F * scale));
        for (int i = 0; i < CRESTS; i++) {
            float sway = Mth.sin(time * 0.5F + i * 1.7F) * 0.15F;
            Vec3 root = head.add(u.scale(0.28F * scale));
            Vec3 tip = root.subtract(f.scale((1.0F + 0.35F * i) * scale)).add(u.scale((0.55F - 0.15F * i) * scale))
                    .add(r.scale((i - 1) * 0.32F * scale + sway * scale));
            Vec3 mid = root.subtract(f.scale(0.4F * scale)).add(u.scale(0.5F * scale));
            strands.add(new Strand(Part.CREST, bezier(root, mid, tip, 5), 0.05F * scale));
        }

        // ---- the wings ----
        if (wings) {
            Vec3 shoulder = bezierAt(tail, control, head, 0.62F);
            for (int side = -1; side <= 1; side += 2) {
                wing(strands, shoulder, f, u, r.scale(side), flap, scale);
            }
        }

        // ---- the tail: five streamers that wave ----
        Vec3 root = tail.add(f.scale(0.2F * scale));
        for (int i = 0; i < STREAMERS; i++) {
            float spread = (i - 2) * 9.0F * Mth.DEG_TO_RAD;
            Vec3 dir = f.scale(-Mth.cos(spread)).add(r.scale(Mth.sin(spread))).subtract(u.scale(0.05F * Math.abs(i - 2))).normalize();
            float length = streamerLength(i) * scale;
            Vec3[] points = new Vec3[SEGMENTS + 1];
            for (int k = 0; k <= SEGMENTS; k++) {
                float t = k / (float) SEGMENTS;
                // A sine across the streamer that grows toward its tip, with a period of seven ticks.
                float wave = Mth.sin(time * Mth.TWO_PI / 7.0F - t * 3.2F + i * 0.9F) * 0.28F * scale * t;
                points[k] = root.add(dir.scale(length * t)).add(r.scale(wave)).subtract(u.scale(0.12F * scale * t * t));
            }
            strands.add(new Strand(Part.TAIL, points, 0.09F * scale));
        }
        return strands;
    }

    /**
     * One wing: seven primary feathers fanning from the shoulder, the longest at the tip (straight out along the wing)
     * and the shortest swept back along the body, each bending back and up along a curve; and five shorter feathers
     * behind them. {@code out} is the unit vector from the body to this wing's side.
     */
    private static void wing(List<Strand> strands, Vec3 shoulder, Vec3 f, Vec3 u, Vec3 out, float flap, float scale) {
        float angle = flap * Mth.DEG_TO_RAD;
        Vec3 axis = out.scale(Mth.cos(angle)).add(u.scale(Mth.sin(angle)));
        // At right angles to the wing, on its upper side: "higher" for a feather's curve.
        Vec3 lift = u.scale(Mth.cos(angle)).subtract(out.scale(Mth.sin(angle)));
        Vec3 back = f.scale(-1.0F);
        for (int i = 0; i < PRIMARIES; i++) {
            float sweep = (PRIMARIES - 1 - i) * 15.0F * Mth.DEG_TO_RAD;
            feather(strands, Firebird.Part.PRIMARY, shoulder, axis, back, lift, sweep, primaryLength(i) * scale, 0.10F * scale);
        }
        Vec3 under = shoulder.subtract(lift.scale(0.18F * scale)).add(back.scale(0.25F * scale));
        for (int j = 0; j < COVERTS; j++) {
            float sweep = (10.0F + 18.0F * j) * Mth.DEG_TO_RAD;
            feather(strands, Firebird.Part.COVERT, under, axis, back, lift, sweep, (2.2F + 0.45F * (COVERTS - 1 - j)) * scale, 0.08F * scale);
        }
    }

    /**
     * A feather from {@code from}: along the wing turned back by {@code sweep}, bent back and up by a quadratic curve.
     */
    private static void feather(List<Strand> strands, Part part, Vec3 from, Vec3 axis, Vec3 back, Vec3 lift, float sweep, float length, float width) {
        Vec3 dir = axis.scale(Mth.cos(sweep)).add(back.scale(Mth.sin(sweep))).normalize();
        Vec3 control = from.add(dir.scale(0.4F * length)).add(lift.scale(0.3F * length));
        Vec3 end = from.add(dir.scale(length)).add(back.scale(0.12F * length)).subtract(lift.scale(0.05F * length));
        strands.add(new Strand(part, bezier(from, control, end, SEGMENTS), width));
    }

    // ---- curves ----

    private static Vec3 bezierAt(Vec3 a, Vec3 b, Vec3 c, float t) {
        float s = 1.0F - t;
        return a.scale(s * s).add(b.scale(2.0F * s * t)).add(c.scale(t * t));
    }

    private static Vec3[] bezier(Vec3 a, Vec3 b, Vec3 c, int segments) {
        Vec3[] points = new Vec3[segments + 1];
        for (int i = 0; i <= segments; i++) {
            points[i] = bezierAt(a, b, c, i / (float) segments);
        }
        return points;
    }

    private static Vec3[] line(Vec3 a, Vec3 b, int segments) {
        Vec3[] points = new Vec3[segments + 1];
        for (int i = 0; i <= segments; i++) {
            points[i] = a.lerp(b, i / (double) segments);
        }
        return points;
    }

    private static Vec3[] circle(Vec3 centre, Vec3 a, Vec3 b, float radius, int segments) {
        Vec3[] points = new Vec3[segments + 1];
        for (int i = 0; i <= segments; i++) {
            float angle = Mth.TWO_PI * i / segments;
            points[i] = centre.add(a.scale(Mth.cos(angle) * radius)).add(b.scale(Mth.sin(angle) * radius));
        }
        return points;
    }

    private Firebird() {
    }
}
