package com.kolodkingal.enhanced.client.render;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The pure maths of the Magnetic Pull's streams (D-M 8.3), drawn by {@link MagnetStreams}: how long a thing flies to
 * Magneto's hand, the curve it flies along, how large it is drawn on the way, and how the field's pulse grows. No
 * Minecraft client classes and no state, so the game tests can check it (R59).
 */
public final class MagnetStreamMath {
    /** The shortest and the longest flight, in ticks. */
    public static final int MIN_FLIGHT = 8;
    public static final int MAX_FLIGHT = 26;
    /** Ticks between the little icons of one stream. */
    public static final int ICON_GAP = 2;
    /** Degrees an icon turns each tick. */
    public static final float SPIN = 30.0F;
    /** The scale an icon leaves at, and the scale it arrives at. */
    public static final float START_SCALE = 0.5F;
    public static final float END_SCALE = 0.35F;
    /** Ticks the field's pulse takes to grow from {@link #PULSE_START} blocks to the pull's radius, fading as it goes. */
    public static final int PULSE_TICKS = 8;
    public static final float PULSE_START = 1.0F;

    /** How long a thing {@code distance} blocks from his hand flies: round(6 + 0.75 x distance) ticks, 8 to 26. */
    public static int flightTicks(double distance) {
        return Mth.clamp((int) Math.round(6.0 + 0.75 * distance), MIN_FLIGHT, MAX_FLIGHT);
    }

    /**
     * How far the middle of the curve is raised over the straight line: 1 + 0.15 x distance blocks, half of it with reduced
     * motion (the stream still flies: it says where the metal came from).
     */
    public static double arcHeight(double distance, boolean reducedMotion) {
        double height = 1.0 + 0.15 * distance;
        return reducedMotion ? height * 0.5 : height;
    }

    /**
     * How far along the curve a thing is ({@code 0} where it starts, {@code 1} in his hand) at {@code u}, the part of its
     * flight that has passed: it leaves slowly and speeds up as it nears the magnet.
     */
    public static double along(double u) {
        double t = Mth.clamp(u, 0.0, 1.0);
        return t * (0.4 + 0.6 * t);
    }

    /**
     * The point at {@code s} (0 to 1) of the quadratic curve from {@code from} to {@code to} whose control point is the
     * middle of the two raised by {@code height}.
     */
    public static Vec3 point(Vec3 from, Vec3 to, double height, double s) {
        double t = Mth.clamp(s, 0.0, 1.0);
        double a = (1.0 - t) * (1.0 - t);
        double b = 2.0 * (1.0 - t) * t;
        double c = t * t;
        double mx = (from.x + to.x) * 0.5;
        double my = (from.y + to.y) * 0.5 + height;
        double mz = (from.z + to.z) * 0.5;
        return new Vec3(a * from.x + b * mx + c * to.x, a * from.y + b * my + c * to.y, a * from.z + b * mz + c * to.z);
    }

    /** The scale of an icon at {@code s} (0 to 1) along its curve: 0.5, shrinking to 0.35 as it arrives. */
    public static float scale(double s) {
        return (float) Mth.lerp(Mth.clamp(s, 0.0, 1.0), START_SCALE, END_SCALE);
    }

    /** The pulse's radius {@code age} ticks after it started: from 1 block out to {@code radius}, quick at first. */
    public static float pulseRadius(float age, float radius) {
        float u = Mth.clamp(age / PULSE_TICKS, 0.0F, 1.0F);
        float eased = 1.0F - (1.0F - u) * (1.0F - u) * (1.0F - u);
        return PULSE_START + (Math.max(radius, PULSE_START) - PULSE_START) * eased;
    }

    /** How much of the pulse is left {@code age} ticks after it started: 1 at the start, nothing once it reached the radius. */
    public static float pulseFade(float age) {
        float u = Mth.clamp(age / PULSE_TICKS, 0.0F, 1.0F);
        return 1.0F - u * u;
    }

    private MagnetStreamMath() {
    }
}
