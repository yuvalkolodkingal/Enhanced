package com.kolodkingal.enhanced.client.render;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The small formulas of the sand effects that the client draws from one payload and must agree with the server about: where
 * a boulder is in the orbit he tears them into, how high the crest of a wave stands behind its front, how a ring of spikes
 * rises and settles. Pure (no Minecraft client classes), so the game tests can check them.
 */
public final class SandMath {
    /** The ticks of the Boulder Barrage's tell: the orbit ends exactly where the server throws the boulders from. */
    public static final int BOULDER_TELL = 30;
    /** The ticks the boulders take to rise from the ground. */
    public static final int BOULDER_RISE = 12;
    /** The radius of the orbit where the server starts the boulders, and the height above his head. */
    public static final double ORBIT_RADIUS = 1.2;
    public static final double ORBIT_OVER_HEAD = 0.8;
    /** How long a ridge of sand is behind its crest, in blocks. */
    public static final double RIDGE_LENGTH = 6.0;

    /**
     * Where the {@code i}th of the three boulders is {@code time} ticks into the tell, for a boss whose feet are at {@code feet}
     * and who is {@code height} tall: they rise out of the ground on a wide ring, and orbit over his head faster and faster, so
     * that at the end of the tell each is where the server throws it from ({@code feet + (cos a, height + 0.8, sin a) 1.2 }
     * with {@code a = 2 pi i / 3}, in the world's axes).
     */
    public static Vec3 boulder(Vec3 feet, double height, float time, int i) {
        float rise = AnimMath.ss(0.0F, BOULDER_RISE, time);
        double radius = Mth.lerp(rise, 2.2, ORBIT_RADIUS);
        double y = Mth.lerp(rise, 0.2, height + ORBIT_OVER_HEAD);
        float t = Math.min(time, BOULDER_TELL) / BOULDER_TELL;
        double angle = Mth.TWO_PI * i / 3.0 + 4.0 * Math.PI * t * t;
        return feet.add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
    }

    /**
     * The height of a ridge of sand at {@code behind} blocks behind its crest (0 at the crest, {@link #RIDGE_LENGTH} at its
     * tail): the crest stands {@code crest} tall, and the back of the ridge slopes away to the ground.
     */
    public static double ridge(double behind, double crest) {
        double u = Mth.clamp(behind / RIDGE_LENGTH, 0.0, 1.0);
        return crest * Math.pow(1.0 - u, 1.6);
    }

    /**
     * How tall the spikes of a ring stand {@code age} ticks after they erupted, as a fraction of the peak: up in four ticks,
     * settling to 0.6 by the tenth, sinking away by the twentieth.
     */
    public static float spike(float age) {
        if (age < 0.0F) {
            return 0.0F;
        }
        float up = AnimMath.ss(0.0F, 4.0F, age);
        float settle = 1.0F - 0.4F * AnimMath.ss(4.0F, 10.0F, age);
        float gone = 1.0F - AnimMath.ss(12.0F, 20.0F, age);
        return up * settle * gone;
    }

    /** The heights of the six cones of a ring of spikes, as fractions of the peak: the middle one is the tallest. */
    public static float coneScale(int index) {
        return index == 0 ? 1.0F : 0.55F + 0.1F * (index % 3);
    }

    /** Where cone {@code index} of a ring of spikes stands, from the middle: the first in the middle, five round it. */
    public static Vec3 cone(Vec3 centre, double radius, int index) {
        if (index == 0) {
            return centre;
        }
        double angle = Mth.TWO_PI * (index - 1) / 5.0 + 0.4;
        return centre.add(Math.cos(angle) * radius * 0.62, 0.0, Math.sin(angle) * radius * 0.62);
    }

    /** The radius of the Mind Crush's ring {@code age} ticks into a tell of {@code ticks}: from the range to two blocks. */
    public static double contracting(float age, int ticks, double range) {
        return Mth.lerp(Mth.clamp(age / Math.max(1, ticks), 0.0F, 1.0F), range, 2.0);
    }

    /** How far the stripe of the Entomb has crawled toward its victim, 0 to 1, {@code age} ticks into the tell (it homes for 20). */
    public static float crawl(float age) {
        return AnimMath.ss(0.0F, 20.0F, age);
    }

    private SandMath() {
    }
}
