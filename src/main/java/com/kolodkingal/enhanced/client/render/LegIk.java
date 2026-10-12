package com.kolodkingal.enhanced.client.render;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The inverse kinematics of one of the Iron Spider's legs: where the three rigid segments have to be so that the tip
 * touches a point (the wall he climbs, the ground, a creature he strikes). Pure arithmetic on vectors (no model, no
 * client), so the game tests can check it.
 *
 * <p>A leg is a root (on his back) and three segments of lengths {@code l1}, {@code l2}, {@code l3}. The solve is the
 * classic two-bone one, by the law of cosines, in the plane of the root, the target and a hint for which way the knee
 * bulges; the second and third segments are taken as one bone of fixed shape, because the third is held at
 * {@link #TIP_ANGLE} to the second, curling the way the knee bends (the tip of a spider's leg points down).
 */
public final class LegIk {
    /** The angle the third segment is held at to the second, in radians (30 degrees). */
    public static final double TIP_ANGLE = Math.toRadians(30.0);

    /** A solved leg: where the knee, the ankle and the tip are (the root is the one it was solved from). */
    public record Pose(Vec3 knee, Vec3 ankle, Vec3 tip) {
    }

    /**
     * Solves a leg.
     *
     * @param root   where the first segment starts
     * @param target the point the tip should touch
     * @param bend the way the knee bulges (any vector not along the way to the target; its part across that way is
     *               used)
     * @return the joints; the tip is on {@code target} when the leg can reach it, and short of it, along the way to it
     *         and with the leg stretched, when it cannot. A target nearer than the leg can fold to is reached by a
     *         folded leg that does not get any nearer.
     */
    public static Pose solve(Vec3 root, Vec3 target, Vec3 bend, double l1, double l2, double l3) {
        Vec3 toTarget = target.subtract(root);
        double distance = toTarget.length();
        Vec3 u = distance < 1.0E-9 ? new Vec3(0, 1, 0) : toTarget.scale(1.0 / distance);
        Vec3 v = bend.subtract(u.scale(bend.dot(u)));
        if (v.lengthSqr() < 1.0E-12) {
            v = perpendicular(u);
        }
        v = v.normalize();

        // The second and the third segment as one bone: its length, and how far the second sits from it.
        double cos = Math.cos(TIP_ANGLE);
        double sin = Math.sin(TIP_ANGLE);
        double l23 = Math.sqrt(l2 * l2 + l3 * l3 + 2.0 * l2 * l3 * cos);
        double delta = Math.atan2(l3 * sin, l2 + l3 * cos);

        double reach = l1 + l23;
        double fold = Math.abs(l1 - l23) + 1.0E-6;
        double d = Mth.clamp(distance, Math.min(fold, reach), reach);
        double cosAlpha = (l1 * l1 + d * d - l23 * l23) / (2.0 * l1 * d);
        double alpha = Math.acos(Mth.clamp(cosAlpha, -1.0, 1.0));
        Vec3 knee = root.add(direction(u, v, alpha).scale(l1));

        // The direction of the effective bone, from the knee to where the tip ends up.
        double effective = Math.atan2(-l1 * Math.sin(alpha), d - l1 * Math.cos(alpha));
        double second = effective + delta;
        double third = second - TIP_ANGLE;
        Vec3 ankle = knee.add(direction(u, v, second).scale(l2));
        Vec3 tip = ankle.add(direction(u, v, third).scale(l3));
        return new Pose(knee, ankle, tip);
    }

    /** The unit vector at {@code angle} from {@code u} towards {@code v} (both unit, at right angles). */
    private static Vec3 direction(Vec3 u, Vec3 v, double angle) {
        return u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle)));
    }

    /** Any unit vector at a right angle to the unit vector {@code u}. */
    private static Vec3 perpendicular(Vec3 u) {
        Vec3 axis = Math.abs(u.x) <= Math.abs(u.y) && Math.abs(u.x) <= Math.abs(u.z) ? new Vec3(1, 0, 0)
                : Math.abs(u.y) <= Math.abs(u.z) ? new Vec3(0, 1, 0) : new Vec3(0, 0, 1);
        return u.cross(axis).normalize();
    }

    private LegIk() {
    }
}
