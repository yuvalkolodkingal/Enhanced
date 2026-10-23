package com.kolodkingal.enhanced.ironman.nano;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Where the Mark 85's nanotech <b>wing blades</b> fly (3.8.3, the Endgame look): each {@link NanoDroneEntity} is one
 * side's wing unit of {@link #BLADES} curved crescent blades, so the two drones make four blades, two beside each arm.
 * The inner blade hangs just past his hand at shoulder height, the outer one further out, a little lower and further
 * back; each bobs and sways on its own. Pure maths in his own frame, shared by the server (the lasers leave from a
 * blade's {@link #muzzle}: the middle of its inner edge) and the client (the renderer draws each blade there).
 *
 * <p>The frame: {@code x} to his left, {@code y} up, {@code z} forward, in blocks at scale 1, turned with his body's
 * yaw. A drone's slot: {@code slot % 2} is its side (0 right, 1 left); {@code slot / 2} pushes extra drones (more than
 * two in the config) further out.
 */
public final class WingFormation {
    /** Blades on one drone: inner and outer. */
    public static final int BLADES = 2;
    /** Ticks an inner blade takes to grow out of his arm; the outer one starts {@link #OUTER_DELAY} later. */
    public static final int GROW_TICKS = 16;
    public static final int OUTER_DELAY = 6;
    /** Ticks the blades take to fold back into his arms (drawn by the clients after the drones go). */
    public static final int RETRACT_TICKS = 12;

    /** The inner blade's distance out from his middle, and the step out per rank. */
    static final double INNER_OUT = 1.2;
    static final double RANK_OUT = 0.62;
    /** The inner blade's height (its inner edge's middle) and how much lower the outer ones hang. */
    static final double INNER_UP = 1.28;
    static final double RANK_DOWN = 0.14;
    /** How far back the inner blade is, and the step back per rank (the arc sweeps back). */
    static final double INNER_BACK = 0.08;
    static final double RANK_BACK = 0.28;
    /** The bob (up and down) and the drift (in and out), in blocks. */
    public static final double BOB = 0.06;
    public static final double DRIFT = 0.04;

    /** Which side a drone flies on: 0 his right, 1 his left. */
    public static int side(int slot) {
        return Math.floorMod(slot, 2);
    }

    /** The rank (0 inner, 1 outer, 2... further out) of a drone's blade. */
    public static int rank(int slot, int blade) {
        return Math.max(0, slot) / 2 * BLADES + blade;
    }

    /** -1 on his right (the frame's x is his left), +1 on his left. */
    public static double sign(int side) {
        return side == 0 ? -1.0 : 1.0;
    }

    /** Each blade's own beat, so the four never bob together. */
    static double phase(int side, int rank) {
        return side * 1.7 + rank * 2.3;
    }

    /**
     * A blade's place in his frame (its inner edge's middle) at {@code time} (ticks), bob and drift included.
     */
    public static Vec3 local(int side, int rank, double time) {
        double phase = phase(side, rank);
        double out = INNER_OUT + RANK_OUT * rank + DRIFT * Math.sin(time * 0.083 + phase);
        double up = INNER_UP - RANK_DOWN * Math.min(rank, 1) - 0.06 * Math.max(0, rank - 1)
                + BOB * Math.sin(time * 0.12 + phase);
        double back = INNER_BACK + RANK_BACK * rank;
        return new Vec3(sign(side) * out, up, -back);
    }

    /** Where his arm is in his frame (the blades grow out of it): beside the shoulder, a little lower. */
    public static Vec3 arm(int side) {
        return new Vec3(sign(side) * 0.42, 1.2, 0.0);
    }

    /** A point of his frame in the world: from {@code base} (his feet), turned with {@code yaw}, at {@code scale}. */
    public static Vec3 world(Vec3 base, float yaw, float scale, Vec3 local) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, yaw);
        Vec3 left = Vec3.directionFromRotation(0.0F, yaw - 90.0F);
        return base.add(left.scale(local.x * scale)).add(0.0, local.y * scale, 0.0).add(forward.scale(local.z * scale));
    }

    /** Where a blade of a drone of his is now (its inner edge's middle, where its laser leaves). */
    public static Vec3 muzzle(Player owner, int slot, int blade, double time) {
        return world(owner.position(), owner.yBodyRot, owner.getScale(),
                local(side(slot), rank(slot, blade), time));
    }

    /** The middle of a drone's blades now (the drone entity's own place). */
    public static Vec3 centre(Player owner, int slot, double time) {
        Vec3 sum = Vec3.ZERO;
        for (int b = 0; b < BLADES; b++) {
            sum = sum.add(muzzle(owner, slot, b, time));
        }
        return sum.scale(1.0 / BLADES);
    }

    /** How far a blade has grown (0 nothing … 1 whole) {@code age} ticks after its drone formed. */
    public static float grown(int rank, float age) {
        float start = rank % BLADES == 0 ? 0.0F : OUTER_DELAY;
        return Mth.clamp((age - start) / GROW_TICKS, 0.0F, 1.0F);
    }

    /** How much of a blade is left (1 whole … 0 gone) {@code age} ticks after the drones were recalled. */
    public static float left(int rank, float age) {
        // The outer blade folds into the inner one first, then the inner one into his arm.
        float t = Mth.clamp(age / RETRACT_TICKS, 0.0F, 1.0F);
        return rank % BLADES == 0 ? 1.0F - Mth.clamp((t - 0.25F) / 0.75F, 0.0F, 1.0F)
                : 1.0F - Mth.clamp(t / 0.6F, 0.0F, 1.0F);
    }

    private WingFormation() {
    }
}
