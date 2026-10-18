package com.kolodkingal.enhanced.thanos;

import com.kolodkingal.enhanced.network.ThanosFxPayload;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * What the Mad Titan shows: every tell, beam, portal and ring he draws is sent to the players nearby as a {@link
 * ThanosFxPayload} (which the client's {@code ThanosFx} draws for a few ticks). {@link #PARTICLES} brings back, as a
 * little server-side energy with every effect, the stand-in that showed the fight before the client drew anything; it
 * is off, since the client draws everything.
 */
public final class ThanosEffects {
    /**
     * Whether the server also sends stand-in particles with every effect (off: the client draws the payloads itself).
     */
    public static boolean PARTICLES = false;

    /** How far an effect is sent: past it nobody could see it. */
    private static final double RANGE = 128.0;
    /** The segments of a ring or a beam drawn with particles. */
    private static final int SEGMENTS = 28;

    /** Sends an effect to everyone within {@link #RANGE} of {@code at}. */
    public static void send(ServerLevel level, Vec3 at, ThanosFxPayload payload) {
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, RANGE, payload);
    }

    private static ThanosFxPayload fx(int kind, int color, int entityId, int ticks, float radius, List<Vec3> points) {
        return new ThanosFxPayload(kind, color, entityId, ticks, radius, points.size() > 16 ? points.subList(0, 16) : points);
    }

    // ---- the tells ----

    /** A ring on the ground that fills over {@code ticks}: where something will land. */
    public static void ringTell(ServerLevel level, Vec3 center, float radius, int ticks, int color) {
        send(level, center, fx(ThanosFxPayload.RING_TELL, color, -1, ticks, radius, List.of(center)));
        if (PARTICLES) {
            ringParticles(level, center, radius, color, SEGMENTS, 0.1);
        }
    }

    /** A thin line from his hand to the aim (through the walls if {@code throughWalls}). */
    public static void lineTell(ServerLevel level, Entity caster, Vec3 from, Vec3 to, int ticks, int color, boolean throughWalls) {
        send(level, from, fx(ThanosFxPayload.LINE_TELL, color, caster.getId(), ticks, throughWalls ? 1.0F : 0.0F, List.of(from, to)));
        if (PARTICLES) {
            lineParticles(level, from, to, color, 14, 0.6F);
        }
    }

    /** A thick beam for {@code ticks}. */
    public static void beam(ServerLevel level, Entity caster, Vec3 from, Vec3 to, float width, int ticks, int color) {
        send(level, from, fx(ThanosFxPayload.BEAM, color, caster.getId(), ticks, width, List.of(from, to)));
        if (PARTICLES) {
            lineParticles(level, from, to, color == -1 ? StoneKind.POWER.core() : color, SEGMENTS * 2, 1.4F);
        }
    }

    /** A ring portal. */
    public static void portal(ServerLevel level, Vec3 at, float radius, int ticks) {
        portal(level, at, radius, ticks, StoneKind.SPACE.color());
    }

    /** 3.5: a portal in another colour than the Space Stone's blue (a custom hero's, in the colour his pick chose). */
    public static void portal(ServerLevel level, Vec3 at, float radius, int ticks, int color) {
        send(level, at, fx(ThanosFxPayload.PORTAL, color, -1, ticks, radius, List.of(at)));
        if (PARTICLES) {
            ringParticles(level, at.add(0, radius, 0), radius, color, SEGMENTS, 0.2);
        }
    }

    /** The big portal in the sky over the arrival, and the column of light down to the ground. */
    public static void portalSky(ServerLevel level, Vec3 ground, int ticks) {
        send(level, ground, fx(ThanosFxPayload.PORTAL_SKY, StoneKind.SPACE.color(), -1, ticks, 5.0F, List.of(ground)));
        if (PARTICLES) {
            ringParticles(level, ground.add(0, 30, 0), 5.0F, StoneKind.SPACE.color(), SEGMENTS, 0.1);
            lineParticles(level, ground.add(0, 30, 0), ground, StoneKind.SPACE.core(), 30, 0.8F);
            ringParticles(level, ground, 3.0F, StoneKind.SPACE.color(), SEGMENTS, 0.05);
        }
    }

    /** A falling star in a stone's colour, from the sky to the Gauntlet. */
    public static void star(ServerLevel level, Vec3 from, Vec3 to, int ticks, int color) {
        send(level, to, fx(ThanosFxPayload.STAR, color, -1, ticks, 1.0F, List.of(from, to)));
        if (PARTICLES) {
            lineParticles(level, from, to, color, 24, 1.2F);
        }
    }

    /** A ring that spreads out from the centre to {@code radius}. */
    public static void shockRing(ServerLevel level, Vec3 center, float radius, int ticks, int color) {
        send(level, center, fx(ThanosFxPayload.SHOCK_RING, color, -1, ticks, radius, List.of(center)));
        if (PARTICLES) {
            ringParticles(level, center, radius, color, SEGMENTS * 2, 0.2);
        }
    }

    /** A mandala on the ground. */
    public static void mandala(ServerLevel level, Vec3 center, float radius, int ticks, int color) {
        send(level, center, fx(ThanosFxPayload.MANDALA, color, -1, ticks, radius, List.of(center)));
        if (PARTICLES) {
            ringParticles(level, center, radius, color, SEGMENTS, 0.05);
            ringParticles(level, center, radius * 0.5F, StoneKind.TIME.core(), SEGMENTS / 2, 0.05);
        }
    }

    /** A translucent dome. */
    public static void dome(ServerLevel level, Vec3 center, float radius, int ticks, int color) {
        send(level, center, fx(ThanosFxPayload.DOME, color, -1, ticks, radius, List.of(center)));
        if (PARTICLES) {
            ringParticles(level, center.add(0, 1.0, 0), radius, color, SEGMENTS, 0.3);
        }
    }

    /** A red wave running out along the ground. */
    public static void wave(ServerLevel level, Vec3 center, float radius, int ticks, int color) {
        send(level, center, fx(ThanosFxPayload.WAVE, color, -1, ticks, radius, List.of(center)));
        if (PARTICLES) {
            ringParticles(level, center, radius, color, SEGMENTS * 2, 0.2);
        }
    }

    /** Chains from the caster to every target's chest. */
    public static void chains(ServerLevel level, Entity caster, List<Vec3> targets, int ticks, int color) {
        send(level, caster.position(), fx(ThanosFxPayload.CHAINS, color, caster.getId(), ticks, 0.0F, targets));
        if (PARTICLES) {
            Vec3 hand = caster.position().add(0, caster.getBbHeight() * 0.6, 0);
            for (Vec3 target : targets) {
                lineParticles(level, hand, target, color, 16, 0.9F);
            }
        }
    }

    /** A column of light. */
    public static void pillar(ServerLevel level, Vec3 center, float radius, int ticks, int color) {
        send(level, center, fx(ThanosFxPayload.PILLAR, color, -1, ticks, radius, List.of(center)));
        if (PARTICLES) {
            ringParticles(level, center, radius, color, SEGMENTS, 0.5);
            level.sendParticles(new EnergyParticleOptions(color, 1.6F), center.x, center.y + 2.0, center.z, 14, radius * 0.4, 2.0, radius * 0.4, 0.05);
        }
    }

    /** The orange sky of the Soul World, on the victim's screen. */
    public static void soulworld(ServerLevel level, Entity victim, int ticks) {
        send(level, victim.position(), fx(ThanosFxPayload.SOULWORLD, StoneKind.SOUL.color(), victim.getId(), ticks, 0.0F, List.of()));
    }

    /** False pillars of stone round a petrified target. */
    public static void illusion(ServerLevel level, Vec3 center, float radius, int ticks) {
        send(level, center, fx(ThanosFxPayload.ILLUSION, StoneKind.REALITY.color(), -1, ticks, radius, List.of(center)));
        if (PARTICLES) {
            ringParticles(level, center, radius, StoneKind.REALITY.core(), 8, 0.2);
        }
    }

    /** A white flash of the screen for everyone near. */
    public static void flash(ServerLevel level, Vec3 center, int ticks) {
        send(level, center, fx(ThanosFxPayload.FLASH, 0xFFFFFF, -1, ticks, 0.0F, List.of()));
    }

    // ---- the stand-in particles ----

    /** A ring of energy particles on the ground. */
    public static void ringParticles(ServerLevel level, Vec3 center, float radius, int color, int count, double lift) {
        EnergyParticleOptions particle = new EnergyParticleOptions(color, 1.0F);
        for (int i = 0; i < count; i++) {
            double angle = i * Mth.TWO_PI / count;
            level.sendParticles(particle, center.x + Math.cos(angle) * radius, center.y + lift, center.z + Math.sin(angle) * radius,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** A line of energy particles from one point to another. */
    public static void lineParticles(ServerLevel level, Vec3 from, Vec3 to, int color, int count, float size) {
        EnergyParticleOptions particle = new EnergyParticleOptions(color, size);
        Vec3 step = to.subtract(from).scale(1.0 / Math.max(1, count));
        for (int i = 0; i <= count; i++) {
            Vec3 at = from.add(step.scale(i));
            level.sendParticles(particle, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** The points of a ring, for a caller that needs them (the sigils of Overload). */
    public static List<Vec3> ringPoints(Vec3 center, double radius, int count) {
        List<Vec3> points = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double angle = i * Mth.TWO_PI / count;
            points.add(center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius));
        }
        return points;
    }

    private ThanosEffects() {
    }
}
