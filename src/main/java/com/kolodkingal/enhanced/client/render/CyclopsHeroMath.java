package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.ability.CyclopsGlasses;

import java.util.function.BiPredicate;

/**
 * The pure arithmetic and the rules behind what Cyclops's client shows (3.3): which of the glasses' two gestures a cooldown
 * of N stands for, how bright the visor burns, how the giant beam breathes. No Minecraft client classes, so the game tests
 * can check it.
 */
public final class CyclopsHeroMath {
    /** A cooldown of N longer than this is the glasses coming off ({@link CyclopsGlasses#OFF_TICKS}), else going on. */
    public static final int GLASSES_SPLIT = (CyclopsGlasses.OFF_TICKS + CyclopsGlasses.ON_TICKS) / 2;

    /**
     * Whether the local hero's key will work, so far as the client can tell (his glasses, his ground): installed by the client
     * once it ticks; until then every key is believed.
     */
    private static BiPredicate<Integer, Boolean> castCheck = (ability, sneaking) -> true;

    static void installCastCheck(BiPredicate<Integer, Boolean> check) {
        castCheck = check;
    }

    static boolean works(int ability, boolean sneaking) {
        return castCheck.test(ability, sneaking);
    }

    /** Whether a cooldown of N of {@code left} ticks is the glasses coming off (not going on). */
    public static boolean glassesOff(long left) {
        return left > GLASSES_SPLIT;
    }

    /**
     * How brightly the visor's lens burns (0 to 1) while the Optic Blast is held, {@code ticks} after it began: it flares in
     * over three ticks and then pulses gently (never below 0.8).
     */
    public static float visorGlow(float ticks) {
        if (ticks <= 0.0F) {
            return 0.0F;
        }
        float rise = Math.min(1.0F, ticks / 3.0F);
        return rise * (0.9F + 0.1F * (float) Math.sin(ticks * 0.9F));
    }

    /**
     * The width (blocks) of the giant beam at {@code along} blocks from his eyes, at time {@code t} (ticks): it swells from a
     * quarter of its width at the eyes to the whole of it within two blocks, and breathes by a tenth.
     */
    public static float unleashedWidth(float full, double along, float t) {
        float grow = (float) Math.min(1.0, 0.25 + along / 2.0 * 0.75);
        return full * grow * (0.92F + 0.08F * (float) Math.sin(t * 1.7F + along * 0.6));
    }

    private CyclopsHeroMath() {
    }
}
