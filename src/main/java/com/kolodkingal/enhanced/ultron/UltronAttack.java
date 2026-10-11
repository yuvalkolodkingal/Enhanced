package com.kolodkingal.enhanced.ultron;

import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * The six attacks of the Ultron boss, with the numbers that decide when each one is chosen: how likely, how near or far
 * the target may be, how long it must rest, and how long its <em>tell</em> lasts (the pose, glow and sound a player sees
 * and hears before anything can hurt; no damage ever comes sooner). {@link #choose} is the pure rule that picks the
 * next one, so that it can be tested without a boss.
 *
 * <p>Which bodies have which attack: Finger Beams only Prime, the Gravity Pull only Prime and the Hybrid; the Volley of
 * the Sentry-Ultrons is one burst, not three.
 */
public enum UltronAttack {
    VOLLEY(UltronState.VOLLEY, 40, 6.0, 30.0, 70, 14),
    BEAMS(UltronState.BEAMS, 25, 8.0, 32.0, 160, 30),
    DIVE(UltronState.DIVE, 20, 5.0, 30.0, 240, 24),
    PULL(UltronState.PULL, 20, 6.0, 26.0, 300, 24),
    CALL(UltronState.CALL, 30, 0.0, Double.MAX_VALUE, 500, 30),
    PUNCH(UltronState.PUNCH, 60, 0.0, 3.2, 25, 8);

    /** The Punch is chosen only within this many blocks. */
    public static final double PUNCH_RANGE = 3.2;
    /** Finger Beams and the Dive are never chosen within this many ticks of each other. */
    public static final int HEAVY_GAP = 80;
    /** The rest between two attacks, in ticks (and 10 more for the bodies after the second). */
    public static final int GLOBAL_GAP = 20;

    private final UltronState state;
    private final int weight;
    private final double minRange;
    private final double maxRange;
    private final int cooldown;
    private final int tell;

    UltronAttack(UltronState state, int weight, double minRange, double maxRange, int cooldown, int tell) {
        this.state = state;
        this.weight = weight;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.cooldown = cooldown;
        this.tell = tell;
    }

    /** The pose the body takes while it does this. */
    public UltronState state() {
        return state;
    }

    /** The ticks of the tell: the first damage comes no sooner. */
    public int tell() {
        return tell;
    }

    /** The ticks before this attack can be chosen again. */
    public int cooldown() {
        return cooldown;
    }

    public double minRange() {
        return minRange;
    }

    public double maxRange() {
        return maxRange;
    }

    /** Whether the body with this index has the attack. */
    public boolean allowedFor(int body) {
        return switch (this) {
            case BEAMS -> body == 0;
            case PULL -> body <= 1;
            default -> true;
        };
    }

    /**
     * What the brain knows when it chooses.
     *
     * @param body          the index of the body
     * @param distance      from the body to its target
     * @param now           the game time
     * @param last          the attack it did last, or null
     * @param lastBeams     the game time Finger Beams were last chosen
     * @param lastDive      the game time the Dive was last chosen
     * @param afterStagger  the body has just recovered from a stagger
     * @param callAllowed   fewer Sentries live than may and the budget is not spent
     * @param callUrgent    a wave is due, or fewer than half the most Sentries live
     * @param readyAt       for each attack (by ordinal) the game time it can be chosen again
     */
    public record Situation(int body, double distance, long now, @Nullable UltronAttack last, long lastBeams, long lastDive,
                            boolean afterStagger, boolean callAllowed, boolean callUrgent, long[] readyAt) {
    }

    /**
     * The next attack, or null if none suits (the body then moves). The rules: the same attack is never chosen twice in
     * a row; Finger Beams and the Dive never within {@link #HEAVY_GAP} ticks of each other; the Punch only within {@link
     * #PUNCH_RANGE}; the Call only when it may; after a stagger the next attack is a Volley; the rest by weight among
     * those that are ready and in range.
     */
    @Nullable
    public static UltronAttack choose(Situation s, RandomSource random) {
        if (s.afterStagger()) {
            return VOLLEY;
        }
        UltronAttack[] all = values();
        double[] weights = new double[all.length];
        double total = 0.0;
        for (UltronAttack attack : all) {
            double w = weightOf(attack, s);
            weights[attack.ordinal()] = w;
            total += w;
        }
        if (total <= 0.0) {
            return null;
        }
        double roll = random.nextDouble() * total;
        for (UltronAttack attack : all) {
            roll -= weights[attack.ordinal()];
            if (roll < 0.0 && weights[attack.ordinal()] > 0.0) {
                return attack;
            }
        }
        for (UltronAttack attack : all) {
            if (weights[attack.ordinal()] > 0.0) {
                return attack;
            }
        }
        return null;
    }

    private static double weightOf(UltronAttack attack, Situation s) {
        if (!attack.allowedFor(s.body()) || s.readyAt()[attack.ordinal()] > s.now() || attack == s.last()) {
            return 0.0;
        }
        if (attack == BEAMS && s.now() - s.lastDive() < HEAVY_GAP) {
            return 0.0;
        }
        if (attack == DIVE && s.now() - s.lastBeams() < HEAVY_GAP) {
            return 0.0;
        }
        if (attack == CALL) {
            return s.callAllowed() ? attack.weight * (s.callUrgent() ? 4.0 : 1.0) : 0.0;
        }
        if (s.distance() < attack.minRange || s.distance() > attack.maxRange) {
            return 0.0;
        }
        return attack.weight;
    }
}
