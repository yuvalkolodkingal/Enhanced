package com.kolodkingal.enhanced.client.render;

/**
 * The small calculations of the hero's client that need no Minecraft class, so that the game tests can check them: how
 * long the storm has left, which cooldown was the Entomb's, when the eyes glow, how fast the shield's raise is played.
 */
public final class ApocalypseHeroMath {
    /** The ticks the keys of the shield's raise are made for. */
    public static final int RAISE_KEYED = GestureRoutes.SHIELD_KEYED_WIND_UP;

    /**
     * The ticks the Sandstorm still has to rage, told from the cooldown of its key: both start in the same tick, so the
     * storm has run for as long as the cooldown has been counting down.
     *
     * @param cooldownLeft  ticks left of the cooldown of N
     * @param stormTicks    {@code [apocalypse].stormTicks}
     * @param stormCooldown {@code [apocalypse].stormCooldown}
     */
    public static long stormLeft(long cooldownLeft, int stormTicks, int stormCooldown) {
        long ran = stormCooldown - cooldownLeft;
        return Math.max(0L, Math.min(stormTicks, stormTicks - ran));
    }

    /**
     * Whether the cooldown that just began on the first key was the Entomb's, not the Wave's: the Entomb's is the longer
     * one, so a cooldown above the Wave's is the Entomb's (a Wave's is the Wave's, or a tick under it by the time the data
     * arrives). With the same cooldown for both they cannot be told apart and it is the Wave.
     */
    public static boolean isEntomb(long cooldown, int waveCooldown, int entombCooldown) {
        return entombCooldown > waveCooldown && cooldown > waveCooldown;
    }

    /** Whether the eyes glow: while the shield is up or the storm rages. */
    public static boolean eyesGlow(boolean shield, boolean storm) {
        return shield || storm;
    }

    /**
     * How fast the keys of the shield's raise are played so that the arms are open at the very tick the sphere is complete:
     * 1.5 for the wind-up of ten ticks the server asks by default, 1 for fifteen, 0.5 for thirty.
     */
    public static float raiseRate(int windUp) {
        return GestureRoutes.shieldRate(windUp);
    }

    /** The lean (degrees) the two stances add, for their weights. */
    public static float lean(float shield, float storm) {
        return StancePoses.APOC_SHIELD_LEAN * shield + StancePoses.APOC_STORM_LEAN * storm;
    }

    private ApocalypseHeroMath() {
    }
}
