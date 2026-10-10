package com.kolodkingal.enhanced.client.render;

import org.joml.Vector3f;

/**
 * Aiming an arm or the head in three dimensions on a body that is pitched, rolled and turned by the renderer. Pure
 * (JOML only), so the game tests can rebuild the arm from the result and check where it points.
 *
 * <p>The renderer's chain is {@code M = [entity yaw] * YP(-yawAdd) * T(pivot) ZP(-roll) XP(-pitch) T(-pivot) * S(-1, -1, 1)}
 * followed by the model part's own {@code rotationZYX}. A direction in the yaw-aligned world frame (x = the player's
 * right, y = up, z = forward) becomes a direction in model space (+x left, +y down, +z back) by undoing that chain.
 */
public final class AimMath {
    private static final float RAD = (float) (Math.PI / 180.0);
    private static final float COS_60 = 0.5F;

    /** The direction in model space (+x left, +y down, +z back) of a world direction {@code w}, for this body. */
    public static Vector3f toModel(Vector3f w, float pitchDeg, float rollDeg, float yawDeg) {
        // Entity-local: z points back.
        Vector3f l = new Vector3f(w.x, w.y, -w.z);
        // Undo the extra turn YP(-yawAdd), then ZP(-roll) XP(-pitch).
        l.rotateY(yawDeg * RAD);
        l.rotateZ(rollDeg * RAD).rotateX(pitchDeg * RAD);
        return new Vector3f(-l.x, -l.y, l.z);
    }

    /**
     * The {@code (xRot, yRot)} (radians) that make an arm's tip point along {@code w} (a unit direction in the yaw-aligned
     * world frame) on a body pitched {@code pitchDeg} forward, rolled {@code rollDeg} (right side down) and turned
     * {@code yawDeg} (to the right) by the renderer. A direction behind the body is clamped to the nearest one the arm
     * reaches: the shoulder plane, or a back swing of up to 60 degrees.
     */
    public static float[] arm(Vector3f w, float pitchDeg, float rollDeg, float yawDeg) {
        Vector3f d = toModel(w, pitchDeg, rollDeg, yawDeg);
        if (d.z > 0.0F && d.y < COS_60) {
            // Behind the shoulder plane and not a plain back swing: clamp to the plane.
            d.z = 0.0F;
            float len = d.length();
            if (len < 1.0E-6F) {
                d.set(0.0F, 1.0F, 0.0F);
            } else {
                d.div(len);
            }
        }
        float acos = (float) Math.acos(Math.max(-1.0F, Math.min(1.0F, d.y)));
        if (d.z <= 0.0F) {
            return new float[]{-acos, (float) Math.atan2(-d.x, -d.z)};
        }
        return new float[]{acos, (float) Math.atan2(d.x, d.z)};
    }

    /**
     * The head's {@code (xRot, yRot)} (radians) to look along {@code w}: {@code xRot} positive looks down, {@code yRot}
     * positive turns to the player's right.
     */
    public static float[] head(Vector3f w, float pitchDeg, float rollDeg, float yawDeg) {
        Vector3f d = toModel(w, pitchDeg, rollDeg, yawDeg);
        float len = d.length();
        if (len < 1.0E-6F) {
            return new float[]{0.0F, 0.0F};
        }
        d.div(len);
        float x = (float) Math.asin(Math.max(-1.0F, Math.min(1.0F, d.y)));
        float y = (float) Math.atan2(-d.x, -d.z);
        return new float[]{x, y};
    }

    /**
     * The direction a head looking along the vanilla head angles (pitch down positive, yaw to the right positive, both
     * relative to the body's facing) points in the yaw-aligned world frame: x = right, y = up, z = forward.
     */
    public static Vector3f lookDirection(float headPitchRad, float headYawRad) {
        float cp = (float) Math.cos(headPitchRad);
        return new Vector3f((float) Math.sin(headYawRad) * cp, -(float) Math.sin(headPitchRad), (float) Math.cos(headYawRad) * cp);
    }

    private AimMath() {
    }
}
