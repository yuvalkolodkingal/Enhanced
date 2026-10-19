package com.kolodkingal.enhanced.stones;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.SlowMotion;
import com.kolodkingal.enhanced.config.StonesConfig;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * The Time Stone's Slow Time zones (3.6): {@code SlowMotion} reads them beside Quicksilver's and Tommy's. A player
 * whose Time Stone (or Eye of Agamotto, or Doctor Strange's Y) slows time has {@link Live#TIME_SLOWING} (a synced bit,
 * so the clients agree on what is slowed); the zone is centred on him and moves with him, and he is not slowed. Inside
 * a Time Stone's zone (and no speedster's) creatures run {@code [time_stone] slowFactor} times slower; a boss at half
 * that ({@code bossFactorHalf}; the bosses' own rule then applies, as for Quicksilver). Both sides.
 */
public final class StoneZones {

    /** Whether this player's Time Stone slows time round him. */
    public static boolean slows(PowerData data) {
        return data.is(Live.TIME_SLOWING);
    }

    /** The radius of a Time Stone's zone ({@code [time_stone] slowRadius}). */
    public static double radius() {
        return EnhancedConfig.SPEC.isLoaded() ? StonesConfig.TIME_SLOW_RADIUS.get() : 12;
    }

    /** How many times slower a Time Stone's zone makes time ({@code [time_stone] slowFactor}). */
    public static int factor() {
        return EnhancedConfig.SPEC.isLoaded() ? StonesConfig.TIME_SLOW_FACTOR.get() : 3;
    }

    /**
     * The factor of the zone that holds {@code entity}: a speedster's ({@code speedster}, the config's) when one of
     * theirs holds it, else the Time Stone's (halved for a boss with {@code bossFactorHalf}).
     */
    public static int factorFor(Entity entity, int speedster) {
        double stone = radius();
        boolean inStone = false;
        for (Player player : entity.level().players()) {
            if (!player.isAlive()) {
                continue;
            }
            PowerData data = Powers.get(player);
            double d = player.position().distanceToSqr(entity.position());
            if (SlowMotion.slowsTime(data) && d < SlowMotion.radius() * SlowMotion.radius()) {
                return speedster;
            }
            inStone |= slows(data) && d < stone * stone;
        }
        if (!inStone) {
            return speedster;
        }
        int factor = factor();
        boolean half = !EnhancedConfig.SPEC.isLoaded() || StonesConfig.TIME_BOSS_FACTOR_HALF.get();
        if (half && entity.getType().is(com.kolodkingal.enhanced.registry.ModTags.BOSSES)) {
            factor = Math.max(1, factor / 2);
        }
        return factor;
    }

    private StoneZones() {
    }
}
