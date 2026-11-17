package com.kolodkingal.enhanced.magic;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.platform.net.Net;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The light of 3.17's spells and ultimate moves, from the server: the shapes every client draws ({@link MagicFxPayload},
 * drawn by {@code client.magic.MagicFxRenderer} with the other energy effects) and the particles that go with them.
 */
public final class MagicFx {
    /** A shockwave running out along the ground to {@code size} blocks. */
    public static final int RING = 0;
    /** Bolts from the sky onto each point. */
    public static final int BOLTS = 1;
    /** Beams from {@code origin} to each point. */
    public static final int BEAMS = 2;
    /** Streams from each point into {@code origin} (a drain). */
    public static final int DRAIN = 3;
    /** Bands of light round each point (something bound), {@code size} tall. */
    public static final int BIND = 4;
    /** A funnel turning in on {@code origin}, {@code size} wide. */
    public static final int VORTEX = 5;
    /** A circle of runes on the ground ({@code size}: its radius), for as long as it lasts. */
    public static final int CIRCLE = 6;
    /** A dome ({@code size}: its radius). */
    public static final int DOME = 7;
    /** A standing portal facing {@code points[0]} ({@code size}: its radius). */
    public static final int PORTAL = 8;
    /** A trail of light along the points. */
    public static final int TRAIL = 9;
    /** An awakening: a column of light and rings rising round {@code entity}. */
    public static final int AWAKEN = 10;
    /** A giant hand of light from {@code origin} along {@code points[0]} ({@code size}: its length). */
    public static final int HAND = 11;
    /** Disks of light whirling round {@code entity} ({@code size}: how far out). */
    public static final int DISKS = 12;
    /** An endless corridor of frames from {@code origin} toward {@code entity}. */
    public static final int HALLWAY = 13;
    /** A field of gravity: rings rising ({@code color2} 1) or slamming down (0). */
    public static final int GRAVITY = 14;
    /** A turning sigil on the ground. */
    public static final int SIGIL = 15;
    /** Reality cracking like glass round {@code origin}. */
    public static final int SHATTER = 16;
    /** Rays bursting out of {@code origin}. */
    public static final int STARBURST = 17;
    /** Take down the lasting shape {@code id}. */
    public static final int CANCEL = 18;
    /** A broom of light under {@code entity} (drawn from its state; this is its appearing). */
    public static final int SPARKLE = 19;
    /**
     * The Nexus Being's Chaos Burst: all the ground and all the sky within {@code size} blocks of {@code origin} throw red
     * energy in every direction ({@code points}: those it strikes).
     */
    public static final int CHAOS_BURST = 20;

    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private MagicFx() {
    }

    /** A new id for a lasting shape. */
    public static int nextId() {
        return NEXT_ID.getAndIncrement();
    }

    /** Sends a shape to everyone near {@code at} (96 blocks). */
    public static void send(ServerLevel level, Vec3 at, MagicFxPayload payload) {
        Net.sendToPlayersNear(level, null, at.x, at.y, at.z, 96.0, payload);
    }

    public static void send(ServerLevel level, int kind, Vec3 origin, List<Vec3> points, int color, int color2, float size,
                            int duration) {
        send(level, origin, new MagicFxPayload(kind, 0, -1, origin, points, color, color2, size, duration));
    }

    /** A lasting shape with an id (to be cancelled later with {@link #cancel}). */
    public static int sendLasting(ServerLevel level, int kind, Vec3 origin, List<Vec3> points, int color, int color2,
                                  float size, int duration) {
        int id = nextId();
        send(level, origin, new MagicFxPayload(kind, id, -1, origin, points, color, color2, size, duration));
        return id;
    }

    /** A shape that follows an entity. */
    public static void sendOn(ServerLevel level, int kind, Entity entity, List<Vec3> points, int color, int color2,
                              float size, int duration) {
        Vec3 at = entity.position();
        send(level, at, new MagicFxPayload(kind, 0, entity.getId(), at, points, color, color2, size, duration));
    }

    /** Takes down a lasting shape. */
    public static void cancel(ServerLevel level, Vec3 at, int id) {
        if (id > 0) {
            send(level, at, new MagicFxPayload(CANCEL, id, -1, at, List.of(), 0, 0, 0.0F, 0));
        }
    }

    // ---- particles ----

    /** A puff of motes of the spell's colour round {@code at}. */
    public static void burst(ServerLevel level, Vec3 at, int color, int count, double spread, float size) {
        level.sendParticles(new EnergyParticleOptions(color, size), at.x, at.y, at.z, count, spread, spread, spread, 0.04);
    }

    /** A ring of motes round {@code at} (radius {@code r}). */
    public static void ring(ServerLevel level, Vec3 at, double r, int color, int count, float size) {
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2 * i / count;
            level.sendParticles(new EnergyParticleOptions(color, size), at.x + Math.cos(a) * r, at.y, at.z + Math.sin(a) * r,
                    1, 0, 0.02, 0, 0.0);
        }
    }

    /** A line of motes from {@code a} to {@code b}. */
    public static void line(ServerLevel level, Vec3 a, Vec3 b, int color, float size) {
        Vec3 d = b.subtract(a);
        int steps = (int) Math.min(60, d.length() * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = a.add(d.scale(i / (double) Math.max(1, steps)));
            level.sendParticles(new EnergyParticleOptions(color, size), p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    /** Motes spiralling up round {@code at} (an entity's spell landing on it). */
    public static void spiral(ServerLevel level, Vec3 at, double r, double height, int color, float size) {
        RandomSource random = level.getRandom();
        double turn = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < 24; i++) {
            double t = i / 24.0;
            double a = turn + t * Math.PI * 4;
            level.sendParticles(new EnergyParticleOptions(color, size), at.x + Math.cos(a) * r, at.y + t * height,
                    at.z + Math.sin(a) * r, 1, 0, 0, 0, 0.0);
        }
    }
}
