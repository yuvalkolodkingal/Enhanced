package com.kolodkingal.enhanced.client.render;

import java.util.ArrayList;
import java.util.List;

/**
 * The cubes of En Sabah Nur's body (DESIGN section 17, 6.8): the one table that the model, the texture painter and the
 * game tests all read, so the three can never drift apart. His body is a custom model of 24 cubes (ten of them also
 * drawn mirrored on the left, so 34 boxes are drawn) on a 128 by 64 texture (one texel per model pixel; the renderer
 * draws it at 1.35 times its size, so a texel is 0.084 blocks): a biped skeleton (the head, the body, two arms, two
 * legs) with the armour built on it as cubes of their own, which is what a player model cannot do (the cap's dome, the
 * cables, the collar's flare, the pauldrons).
 *
 * <p>A cube's box is relative to the pivot of the part it hangs on. The cubes that exist on both sides (the arm, the
 * pauldrons, the cables) are listed once, for the right side; the left is the same cube mirrored about the pivot, with
 * the same texels read the other way, so both sides share one region of the texture. No Minecraft classes, so the game
 * tests can check that no two regions of the texture overlap.
 */
public final class ApocalypseCubes {
    /** The size of the texture. */
    public static final int WIDTH = 128;
    public static final int HEIGHT = 64;
    /** How big the armoured body is drawn, and the frail one: 1 model pixel is this many sixteenths of a block. */
    public static final float SCALE_ARMOURED = 1.35F;
    public static final float SCALE_TRUE = 1.2F;

    /** The parts a cube can hang on. */
    public enum Pivot {
        HEAD(0.0F, 0.0F, 0.0F),
        BODY(0.0F, 0.0F, 0.0F),
        RIGHT_ARM(-5.0F, 2.0F, 0.0F),
        LEFT_ARM(5.0F, 2.0F, 0.0F),
        RIGHT_LEG(-1.9F, 12.0F, 0.0F),
        LEFT_LEG(1.9F, 12.0F, 0.0F);

        public final float x;
        public final float y;
        public final float z;

        Pivot(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /** What a cube is for: whether the true form (the armour fallen) still has it. */
    public enum Kind {
        /** The body that is under the armour, and what stays on it: the head, the body, the arms, the legs, the cables. */
        BODY,
        /** The cap, which stays but cracked. */
        CAP,
        /** The torn cloth of the belt: all that is left of the clothes. */
        CLOTH,
        /** Armour: it falls away in the true form. */
        ARMOUR
    }

    /**
     * One cube: its box relative to the pivot, and where its texels start (the left of its region of the texture).
     *
     * @param mirrored true for a cube that is also drawn on the left side (mirrored about the pivot, same texels)
     */
    public record Cube(String name, Pivot pivot, Kind kind, float x, float y, float z, int w, int h, int d, int u, int v, boolean mirrored) {
        /** The width of the cube's region of the texture: its left, front, right and back faces side by side. */
        public int regionWidth() {
            return 2 * d + 2 * w;
        }

        /** The height of the region: the top and bottom faces above the four sides. */
        public int regionHeight() {
            return d + h;
        }
    }

    /**
     * Every cube, in the order the model builds them. The cables are three boxes a side and the cloak three panels; each
     * box is a cube of its own here, and the ones that repeat share one region of the texture.
     */
    public static final List<Cube> ALL = new ArrayList<>();

    private static void add(String name, Pivot pivot, Kind kind, float x, float y, float z, int w, int h, int d, int u, int v, boolean mirrored) {
        ALL.add(new Cube(name, pivot, kind, x, y, z, w, h, d, u, v, mirrored));
    }

    static {
        add("head", Pivot.HEAD, Kind.BODY, -4, -8, -4, 8, 8, 8, 0, 0, false);
        add("cap", Pivot.HEAD, Kind.CAP, -4.5F, -10.5F, -4.5F, 9, 4, 9, 0, 16, false);
        add("crest", Pivot.HEAD, Kind.ARMOUR, -1, -12.5F, -3.5F, 2, 2, 7, 52, 29, false);
        add("cable_1", Pivot.HEAD, Kind.BODY, -6, -5, -1, 2, 4, 2, 68, 39, true);
        add("cable_2", Pivot.HEAD, Kind.BODY, -6.2F, -1, -1, 2, 4, 2, 68, 39, true);
        add("cable_3", Pivot.HEAD, Kind.BODY, -6.4F, 3, -1, 2, 4, 2, 68, 39, true);
        add("body", Pivot.BODY, Kind.BODY, -4, 0, -2, 8, 12, 4, 32, 0, false);
        add("chest_plate", Pivot.BODY, Kind.ARMOUR, -3.5F, 1.5F, -2.6F, 7, 9, 1, 0, 29, false);
        add("glyph_plate", Pivot.BODY, Kind.ARMOUR, -1.5F, 2.5F, -3.1F, 3, 3, 1, 88, 39, false);
        add("belt", Pivot.BODY, Kind.ARMOUR, -4.5F, 10, -2.5F, 9, 2, 5, 94, 29, false);
        add("tasset", Pivot.BODY, Kind.CLOTH, -2.5F, 11.5F, -2.8F, 5, 5, 1, 76, 39, false);
        add("collar_low", Pivot.BODY, Kind.ARMOUR, -6, -1, -5, 12, 1, 10, 56, 16, false);
        // These two collar cubes are nudged in z (the spec's -4 and 2..4): a face coplanar with the head's z-fights with it,
        // and the collar's front covered the mouth. Now the head's face is in front of the collar at the front, and at the
        // back the collar's face lies just behind the head's.
        add("collar_high", Pivot.BODY, Kind.ARMOUR, -5, -3, -3.5F, 10, 2, 8, 16, 29, false);
        add("collar_back", Pivot.BODY, Kind.ARMOUR, -6, -6, 2.7F, 12, 5, 2, 0, 39, false);
        add("cloak_1", Pivot.BODY, Kind.ARMOUR, -6, 0, 2.2F, 4, 13, 1, 88, 0, false);
        add("cloak_2", Pivot.BODY, Kind.ARMOUR, -2, 0, 2.4F, 4, 13, 1, 88, 0, false);
        add("cloak_3", Pivot.BODY, Kind.ARMOUR, 2, 0, 2.2F, 4, 13, 1, 88, 0, false);
        add("arm", Pivot.RIGHT_ARM, Kind.BODY, -3, -2, -2, 4, 12, 4, 56, 0, true);
        add("pauldron_a", Pivot.RIGHT_ARM, Kind.ARMOUR, -6.5F, -4.5F, -3, 6, 2, 6, 70, 29, true);
        add("pauldron_b", Pivot.RIGHT_ARM, Kind.ARMOUR, -6, -3, -2.5F, 5, 2, 5, 28, 39, true);
        add("gauntlet", Pivot.RIGHT_ARM, Kind.ARMOUR, -3.5F, 5, -2.5F, 5, 6, 5, 100, 16, true);
        add("leg", Pivot.RIGHT_LEG, Kind.BODY, -2, 0, -2, 4, 12, 4, 72, 0, true);
        add("greave", Pivot.RIGHT_LEG, Kind.ARMOUR, -2.5F, 5, -2.5F, 5, 7, 5, 36, 16, true);
        add("knee", Pivot.RIGHT_LEG, Kind.ARMOUR, -2.5F, 3, -2.5F, 5, 2, 5, 48, 39, true);
    }

    /** The mirror of a box about the pivot: the x extent of a cube whose left edge is at {@code x} and which is {@code w} wide. */
    public static float mirroredX(float x, int w) {
        return -(x + w);
    }

    /** Whether two cubes read the same region of the texture: the repeated ones (three cables, three cloak panels) do. */
    public static boolean sharesRegion(Cube a, Cube b) {
        return a.u() == b.u() && a.v() == b.v();
    }

    private ApocalypseCubes() {
    }
}
