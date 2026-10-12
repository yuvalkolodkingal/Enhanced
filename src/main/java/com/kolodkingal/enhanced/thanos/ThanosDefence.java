package com.kolodkingal.enhanced.thanos;

import com.kolodkingal.enhanced.config.ThanosBossConfig;

/**
 * The damage Thanos takes, as pure rules (no world, no entity): what a hit is worth after his plates, his state, the
 * Barrier, the caps, and how much poise it adds to a clench. {@code ThanosEntity.hurt} classifies the hit and asks
 * here, so the order of DESIGN section 15.6 is written down once and tested without a boss.
 *
 * <p>The order: (0) nothing in ARRIVING or DYING, unless the damage cannot be stopped; (1) a creature that is not a
 * player does only a quarter; (2) the Space Barrier turns away ranged damage that is not born of an Infinity Stone;
 * (3) each intact plate takes a tenth off; (4) the surge, a stagger and a locked Gauntlet open him up; (5) Stormbreaker
 * does two and a half times; (6) no one hit takes more than 6 percent of his health and no second more than 15.
 */
public final class ThanosDefence {
    /** The four plates of his armour, as bits of the synced mask. */
    public static final int HELMET = 1;
    public static final int R_PAULDRON = 2;
    public static final int L_PAULDRON = 4;
    public static final int BREASTPLATE = 8;
    public static final int ALL_PLATES = HELMET | R_PAULDRON | L_PAULDRON | BREASTPLATE;

    /** The health fraction below which each plate breaks: right pauldron 75, left pauldron 55, breastplate 35, helmet 20. */
    private static final int[] PLATE_ORDER = {R_PAULDRON, L_PAULDRON, BREASTPLATE, HELMET};
    private static final float[] PLATE_THRESHOLD = {0.75F, 0.55F, 0.35F, 0.20F};

    /**
     * What kind of hit it is.
     *
     * @param bypassesInvulnerability damage that cannot be stopped ({@code /kill}, the void)
     * @param fromMob                 struck by a creature that is neither a player nor a hero (a golem, a wolf, a skeleton)
     * @param ranged                  a projectile or a beam
     * @param stoneBorn               born of an Infinity Stone (the tag {@code enhanced:stone_born}): the Chaos Beam, the
     *                                Mind Stone's beam, Stormbreaker
     * @param stormbreaker            Stormbreaker, thrown or in the hand
     */
    public record Hit(boolean bypassesInvulnerability, boolean fromMob, boolean ranged, boolean stoneBorn, boolean stormbreaker) {
    }

    /**
     * How he stands when the hit lands.
     *
     * @param state           his state
     * @param barrier         whether the Space Barrier is up
     * @param plates          the mask of his intact plates
     * @param gauntletLocked  whether the Gauntlet is locked (a web, a stagger)
     * @param maxHealth       his maximum health
     * @param takenThisSecond what he has already taken in the last second
     */
    public record Stance(ThanosState state, boolean barrier, int plates, boolean gauntletLocked, float maxHealth, float takenThisSecond) {
    }

    /** The number of intact plates in a mask. */
    public static int intact(int plates) {
        return Integer.bitCount(plates & ALL_PLATES);
    }

    /**
     * The damage he takes from a hit of {@code amount}, the steps of the class comment in order.
     */
    public static float damageTaken(Hit hit, Stance stance, float amount) {
        if (hit.bypassesInvulnerability()) {
            return amount;
        }
        if (stance.state() == ThanosState.ARRIVING || stance.state() == ThanosState.DYING) {
            return 0.0F;
        }
        float taken = amount;
        if (hit.fromMob()) {
            taken *= ThanosBossConfig.MOB_DAMAGE_FACTOR.get().floatValue();
        }
        if (stance.barrier() && hit.ranged() && !hit.stoneBorn()) {
            return 0.0F;
        }
        taken *= 1.0F - ThanosBossConfig.PLATE_REDUCTION.get().floatValue() * intact(stance.plates());
        if (stance.state() == ThanosState.SURGE) {
            taken *= ThanosBossConfig.SURGE_DAMAGE_FACTOR.get().floatValue();
        } else if (stance.state() == ThanosState.STAGGERED || stance.gauntletLocked()) {
            taken *= 1.25F;
        }
        if (hit.stormbreaker()) {
            taken *= ThanosBossConfig.STORMBREAKER_FACTOR.get().floatValue();
        }
        float perHit = stance.maxHealth() * ThanosBossConfig.MAX_HIT_FRACTION.get().floatValue();
        float perSecond = stance.maxHealth() * ThanosBossConfig.MAX_DAMAGE_PER_SECOND.get().floatValue() - stance.takenThisSecond();
        return Math.max(0.0F, Math.min(taken, Math.min(perHit, perSecond)));
    }

    /**
     * The poise a hit that was taken adds to a clench: {@code taken} times {@code poiseHit}, three times that for damage
     * born of an Infinity Stone.
     */
    public static float poise(float taken, boolean stoneBorn) {
        return taken * ThanosBossConfig.POISE_HIT.get().floatValue() * (stoneBorn ? 3.0F : 1.0F);
    }

    /**
     * The plates that break when his health falls from {@code before} to {@code after} (fractions of the maximum): those
     * whose threshold the fall crosses. A plate that is already broken, or whose threshold was crossed before, is not
     * asked about again by the caller.
     */
    public static int crossed(float before, float after) {
        int broken = 0;
        for (int i = 0; i < PLATE_ORDER.length; i++) {
            if (before >= PLATE_THRESHOLD[i] && after < PLATE_THRESHOLD[i]) {
                broken |= PLATE_ORDER[i];
            }
        }
        return broken;
    }

    /** The plate that breaks first of those that are still intact ({@code mask}), in the order of a stone-born tear: 0 if none. */
    public static int firstIntact(int mask, int pick) {
        int found = 0;
        int count = intact(mask);
        if (count == 0) {
            return 0;
        }
        int wanted = Math.floorMod(pick, count);
        for (int plate : PLATE_ORDER) {
            if ((mask & plate) != 0) {
                if (wanted-- == 0) {
                    found = plate;
                    break;
                }
            }
        }
        return found;
    }

    private ThanosDefence() {
    }
}
