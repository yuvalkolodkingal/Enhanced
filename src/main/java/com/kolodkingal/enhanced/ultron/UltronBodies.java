package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.config.UltronBossConfig;

/**
 * The bodies Ultron moves through, as numbers: Prime (body 0), the Hybrid (body 1) and the Sentry-Ultrons (body 2 and
 * after), each weaker and smaller than the last. Pure functions of the body's index and of how many fighters there are,
 * read by the entity (its attributes and hitbox), the fight (the boss bar, the budget) and the tests.
 *
 * <p>The health of body {@code k} is {@code H0 x max(bodyHealthFloor, bodyHealthDecay^k)}, where {@code H0 =
 * bodyHealth x (1 + healthPerExtraPlayer x (n - 1))} and {@code n} is the number of participants (at most {@link
 * #MAX_COUNTED}). For one player that is 600, 480, 384, 307, 246, 197, 157 and then 150 for every body after.
 */
public final class UltronBodies {
    /** Participants beyond this many do not make him any stronger. */
    public static final int MAX_COUNTED = 6;
    /** The scale of Prime, which the entity type's size (1.3 x 3.4) is that of. */
    public static final float PRIME_SCALE = 1.9F;

    /** What one body is: its form, its size, and how hard it is to hurt. */
    public record Body(int index, float scale, double armor, double toughness) {
        /** 0 = Prime, 1 = the Hybrid (a Sentry's right forearm, a cracked chest), 2 = a Sentry-Ultron. */
        public int form() {
            return Math.min(index, 2);
        }

        /** The body's hitbox is the entity type's, scaled by this. */
        public float hitboxScale() {
            return scale / PRIME_SCALE;
        }
    }

    /** The body with this index (0 or more). */
    public static Body of(int index) {
        int k = Math.max(0, index);
        float scale;
        double armor;
        double toughness;
        if (k == 0) {
            scale = PRIME_SCALE;
            armor = UltronBossConfig.BODY_ARMOR.get();
            toughness = UltronBossConfig.BODY_TOUGHNESS.get();
        } else if (k == 1) {
            scale = 1.7F;
            armor = Math.max(2.0, UltronBossConfig.BODY_ARMOR.get() - 1.0);
            toughness = UltronBossConfig.BODY_TOUGHNESS.get();
        } else {
            scale = Math.max(1.1F, 1.5F - 0.1F * (k - 2));
            armor = Math.max(2.0, UltronBossConfig.BODY_ARMOR.get() - k);
            toughness = Math.min(2.0, UltronBossConfig.BODY_TOUGHNESS.get());
        }
        return new Body(k, scale, armor, toughness);
    }

    /** How many of the participants make him stronger: all of them, up to {@link #MAX_COUNTED}, and at least one. */
    public static int counted(int participants) {
        return Math.max(1, Math.min(MAX_COUNTED, participants));
    }

    /** The health of the first body for this many participants. */
    public static float firstBodyHealth(int participants) {
        double extra = UltronBossConfig.HEALTH_PER_EXTRA_PLAYER.get() * (counted(participants) - 1);
        return (float) (UltronBossConfig.BODY_HEALTH.get() * (1.0 + extra));
    }

    /** The maximum health of body {@code index} for this many participants. */
    public static float health(int index, int participants) {
        double fraction = Math.max(UltronBossConfig.BODY_HEALTH_FLOOR.get(), Math.pow(UltronBossConfig.BODY_HEALTH_DECAY.get(), Math.max(0, index)));
        return (float) (firstBodyHealth(participants) * fraction);
    }

    private UltronBodies() {
    }
}
