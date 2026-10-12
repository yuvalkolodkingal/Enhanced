package com.kolodkingal.enhanced.client.render;

import net.minecraft.util.Mth;

import static com.kolodkingal.enhanced.client.render.AnimMath.ss;

/**
 * The cycle of Spider-Man's hands and knees as he climbs (DESIGN section 14): one cycle for every 1.1 blocks he moves
 * along the surface, none while he hangs still. Pure arithmetic, so the game tests can check it; the pose it drives is
 * {@link SpiderPoses}.
 */
public final class CrawlMath {
    /** The blocks he climbs for one cycle of his hands and knees. */
    public static final float CYCLE = 1.1F;

    /** The cadence (radians per tick) of the crawl at a speed along the surface (blocks per tick). */
    public static float omega(float speed) {
        return Mth.TWO_PI * speed / CYCLE;
    }

    /** How much of the cycle shows at a speed along the surface: none at rest, all of it from 0.15 blocks a tick. */
    public static float amp(float speed) {
        return ss(0.02F, 0.15F, speed);
    }

    /** The phase after {@code dt} ticks at cadence {@code omega}, kept small (a long climb must not lose precision). */
    public static float advance(float phase, float omega, float dt) {
        float next = phase + omega * dt;
        return next > 1.0E4F ? next - 1.0E4F : next;
    }

    private CrawlMath() {
    }
}
