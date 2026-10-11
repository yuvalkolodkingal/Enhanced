package com.kolodkingal.enhanced.client.render;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Where Thor's weapon sits in his fist, as pure JOML maths (no Minecraft imports, so a game test checks it against the
 * item models' own display transforms).
 *
 * <p>All positions are in pixels of the <em>hand frame H</em>: the frame vanilla's {@code ItemInHandLayer} builds before
 * it applies an item's display transform. Its origin is the centre of the bottom-front edge of the arm, {@code +x} is the
 * player's right (inward for the left hand), {@code +y} points forward out of the arm's front face and {@code +z} up the
 * arm towards the shoulder. The middle of the fist is {@link #FIST}.
 *
 * <p>The weapon is placed with {@code v_H = p + s * R * (v - q)} for an item point {@code v} (item pixels): {@code s}
 * is the weapon's {@linkplain Kind#scale size in the hand} (Mjolnir 0.85, Stormbreaker 1.0: its head about as long as
 * Thor's head is wide), {@code q} the point of the item that is held, {@code p} where that point is in H, {@code R} the
 * rotation. At rest {@code R} turns the handle across the fist, head forward and up ({@code beta = 30}); with the arm
 * raised the weapon is thrust ahead ({@code beta = -78}, the lead); and while it whirls, the weapon revolves on its
 * strap about the arm's own axis.
 */
public final class GripMath {
    /** The middle of the fist in H (pixels): the lowest 3 px of the arm. */
    public static final Vector3f FIST = new Vector3f(0.0F, -2.0F, 1.5F);
    /** The centre of the whirl: a little beyond the end of the arm. */
    public static final Vector3f WHIRL = new Vector3f(0.0F, -2.0F, -1.0F);
    /** The whirl's disc is tilted this many degrees about the forward axis so it never touches the head. */
    public static final float TILT = -30.0F;
    /** The handle's angle across the fist at rest and in the lead (degrees). */
    public static final float BETA_REST = 30.0F;
    public static final float BETA_LEAD = -78.0F;
    private static final float RAD = (float) (Math.PI / 180.0);

    /**
     * The two weapons: the size in the hand, the held point, the end of the strap it whirls on, the gap of the strap
     * and the head's centre.
     */
    public enum Kind {
        MJOLNIR(0.85F, new Vector3f(8.0F, 2.5F, 8.0F), new Vector3f(8.0F, -4.0F, 8.0F), 3.0F, new Vector3f(8.0F, 13.25F, 8.0F), 15.5F, 19.8F),
        STORMBREAKER(1.0F, new Vector3f(8.0F, -1.0F, 8.0F), new Vector3f(8.0F, -1.0F, 8.0F), 0.0F, new Vector3f(13.0F, 13.5F, 8.0F), 11.0F, 21.0F);

        /** How big the weapon is drawn in the hand: pixels of the hand frame per pixel of the item model. */
        public final float scale;
        public final Vector3f grip;
        public final Vector3f strap;
        public final float gap;
        public final Vector3f head;
        /** Inner and outer radius (pixels) of the swoosh a whirling weapon leaves behind it. */
        public final float swooshInner;
        public final float swooshOuter;

        Kind(float scale, Vector3f grip, Vector3f strap, float gap, Vector3f head, float swooshInner, float swooshOuter) {
            this.scale = scale;
            this.grip = grip;
            this.strap = strap;
            this.gap = gap;
            this.head = head;
            this.swooshInner = swooshInner;
            this.swooshOuter = swooshOuter;
        }
    }

    /** Where one frame of the weapon is: held item point {@code held}, at {@code position} in H, turned by {@code rotation}. */
    public static final class Placement {
        /** The size of the weapon in the hand ({@link Kind#scale}). */
        public float scale;
        public final Vector3f position = new Vector3f();
        public final Quaternionf rotation = new Quaternionf();
        public final Vector3f held = new Vector3f();
        /** Where the strap is fastened: the centre of the whirl, or the fist (the held point is a strap's length from it). */
        public final Vector3f pivot = new Vector3f();

        /** The matrix that takes an item point in blocks (item pixels over 16) to H in blocks (pixels over 16). */
        public Matrix4f matrix() {
            return new Matrix4f().translate(position.x / 16.0F, position.y / 16.0F, position.z / 16.0F).rotate(rotation)
                    .scale(scale).translate(-held.x / 16.0F, -held.y / 16.0F, -held.z / 16.0F);
        }

        /** An item point (item pixels) in H (pixels). */
        public Vector3f toHand(Vector3f item) {
            Vector3f v = new Vector3f(item).sub(held).mul(scale);
            rotation.transform(v);
            return v.add(position);
        }
    }

    /**
     * The resting and leading rotation: the handle across the fist at {@code betaDeg} (the left hand turns the weapon half
     * way round its handle so the blade still faces outward).
     */
    public static Quaternionf holdRotation(float betaDeg, boolean left) {
        Quaternionf r = new Quaternionf().rotationX(betaDeg * RAD);
        if (left) {
            r.rotateY((float) Math.PI);
        }
        return r;
    }

    /** The whirl's rotation: the item's +y (towards the head) swings round the arm's axis in the tilted disc. */
    public static Quaternionf whirlRotation(boolean left, float spinDeg) {
        float m = left ? -1.0F : 1.0F;
        return new Quaternionf().rotationY(m * TILT * RAD).rotateZ(-m * spinDeg * RAD);
    }

    /** How far the lead has turned the weapon, 0 (resting) to 1: from the arm's raise, or forced by a gesture. */
    public static float leadWeight(float armElevationDeg, float leadForce) {
        return Math.max(AnimMath.ss(45.0F, 95.0F, armElevationDeg), leadForce);
    }

    /** The angle across the fist for a lead weight. */
    public static float beta(float leadWeight) {
        return BETA_REST + (BETA_LEAD - BETA_REST) * leadWeight;
    }

    /**
     * The placement of a weapon.
     *
     * @param left     the left hand holds it
     * @param slim     a slim (3 px) arm: the fist is half a pixel further in
     * @param betaDeg  the handle's angle across the fist ({@link #beta})
     * @param whirl    0 resting or leading, 1 whirling on its strap (blended: the weapon swings round and is pulled in line)
     * @param spinDeg  the angle of the whirl (degrees)
     */
    public static Placement placement(Kind kind, boolean left, boolean slim, float betaDeg, float whirl, float spinDeg) {
        Placement out = new Placement();
        out.scale = kind.scale;
        float m = left ? -1.0F : 1.0F;
        Quaternionf hold = holdRotation(betaDeg, left);
        if (whirl <= 0.001F) {
            out.rotation.set(hold);
        } else {
            out.rotation.set(hold).nlerp(whirlRotation(left, spinDeg), whirl).normalize();
        }
        float w = Math.max(0.0F, Math.min(1.0F, whirl));
        out.held.set(kind.grip).lerp(kind.strap, w);
        Vector3f fist = new Vector3f(slim ? -0.5F * m : 0.0F, FIST.y, FIST.z);
        out.position.set(fist).lerp(WHIRL, w);
        out.pivot.set(out.position);
        if (w > 0.0F && kind.gap != 0.0F) {
            Vector3f out2 = new Vector3f(0.0F, 1.0F, 0.0F);
            out.rotation.transform(out2);
            out.position.add(out2.mul(kind.gap * w));
        }
        return out;
    }

    private GripMath() {
    }
}
