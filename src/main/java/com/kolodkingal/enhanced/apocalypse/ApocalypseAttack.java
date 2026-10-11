package com.kolodkingal.enhanced.apocalypse;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The nine attacks the controller chooses between (the Cairo Collapse and the Escape are set pieces of the fight, not
 * chosen): what each is called, how long its tell lasts, when its aim locks, how far it reaches, how often it comes and
 * how likely it is to be chosen. The numbers are the table of the spec (3.6); damage and the rest of the balance are in
 * {@code [apocalypse_boss]}. Every attack has a tell, and a fixed moment after which its aim does not track, so it can
 * be dodged.
 */
public enum ApocalypseAttack {
    /** Five sand lances in a flat fan. */
    LANCES(ApocalypseState.TELL_LANCE, 1, 40, 60, 6.0, 32.0, 8),
    /** A rolling wave of sand along a lane of the ground. */
    WAVE(ApocalypseState.TELL_WAVE, 1, 30, 100, 0.0, 28.0, 12),
    /** Spikes erupt under up to four players. */
    SPIKES(ApocalypseState.TELL_SPIKES, 1, 30, 120, 4.0, 30.0, 8),
    /** Three boulders tear out of the ground and are hurled. */
    BOULDERS(ApocalypseState.TELL_BOULDERS, 1, 25, 160, 8.0, 36.0, 22),
    /** The grasp that swallows one player in a cocoon of sand. */
    ENTOMB(ApocalypseState.TELL_ENTOMB, 2, 20, 240, 6.0, 30.0, 20),
    /** A contracting ring of mind: nausea, slowness and blindness for everyone in range. */
    MIND_CRUSH(ApocalypseState.TELL_MIND_CRUSH, 2, 15, 300, 0.0, 28.0, -1),
    /** He steps through a portal and appears behind the player farthest from him. */
    PORTAL_STEP(ApocalypseState.TELL_PORTAL, 1, 20, 160, 0.0, Double.MAX_VALUE, -1),
    /** The arm drawn across the chest: his fist. */
    BACKHAND(ApocalypseState.BACKHAND, 1, 60, 25, 0.0, 3.2, -1),
    /** Tornadoes: one in the second phase, three in the third. */
    TORNADO(ApocalypseState.TELL_TORNADO, 2, 15, 400, 0.0, Double.MAX_VALUE, -1);

    /** Entomb and Mind Crush are never chosen within this many ticks of each other. */
    public static final int STRONG_GAP = 100;

    private final ApocalypseState state;
    private final int fromPhase;
    private final int weight;
    private final int cooldown;
    private final double minRange;
    private final double maxRange;
    private final int lockTick;

    ApocalypseAttack(ApocalypseState state, int fromPhase, int weight, int cooldown, double minRange, double maxRange, int lockTick) {
        this.state = state;
        this.fromPhase = fromPhase;
        this.weight = weight;
        this.cooldown = cooldown;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.lockTick = lockTick;
    }

    /** The state his body is in while the tell lasts. */
    public ApocalypseState state() {
        return state;
    }

    /** The first phase (1 to 3) in which it is chosen. */
    public int fromPhase() {
        return fromPhase;
    }

    public int weight() {
        return weight;
    }

    /** The ticks before it can be chosen again. */
    public int cooldown() {
        return cooldown;
    }

    public double minRange() {
        return minRange;
    }

    public double maxRange() {
        return maxRange;
    }

    /** The tick of the tell at which the aim locks (it does not track after), or -1 if there is no aim. */
    public int lockTick() {
        return lockTick;
    }

    /** The ticks of the tell. */
    public int tellTicks() {
        return state.tellTicks();
    }

    /** The attack of that state, or null. */
    public static ApocalypseAttack of(ApocalypseState state) {
        for (ApocalypseAttack attack : values()) {
            if (attack.state == state) {
                return attack;
            }
        }
        return null;
    }

    // ---- choosing ----

    /**
     * What the controller knows when it chooses.
     *
     * @param phase         1 to 3 (the true form is the third)
     * @param nearest       the distance to the nearest participant
     * @param inWindow      the shield is broken and he attacks at half the rate: only Lances, Backhand and Spikes
     * @param afterStagger  the stagger of a window has just ended: the first attack is the Lances
     * @param last          the attack he chose last, or null
     * @param now           the game time
     * @param lastEntomb    the game time of the last Entomb
     * @param lastMindCrush the game time of the last Mind Crush
     * @param sight         whether a player is in his sight (the Mind Crush needs one)
     * @param portalWanted  whether the conditions for a Portal Step hold (nobody within 10 blocks for 80 ticks, four
     *                      attacks since the last, or stuck for 60 ticks) and an exit exists
     * @param ready         whether the attack's own cooldown has passed
     */
    public record Choice(int phase, double nearest, boolean inWindow, boolean afterStagger, ApocalypseAttack last, long now,
                         long lastEntomb, long lastMindCrush, boolean sight, boolean portalWanted, Predicate<ApocalypseAttack> ready) {
    }

    /** Whether the attack may be chosen now. */
    public boolean allowed(Choice choice) {
        if (choice.phase() < fromPhase || this == choice.last() || !choice.ready().test(this)) {
            return false;
        }
        if (choice.inWindow() && this != LANCES && this != BACKHAND && this != SPIKES) {
            return false;
        }
        return switch (this) {
            case BACKHAND -> choice.nearest() <= maxRange;
            case PORTAL_STEP -> choice.portalWanted();
            case MIND_CRUSH -> choice.sight() && choice.nearest() <= maxRange + 4.0
                    && choice.now() - choice.lastEntomb() >= STRONG_GAP;
            case ENTOMB -> choice.nearest() >= minRange && choice.nearest() <= maxRange
                    && choice.now() - choice.lastMindCrush() >= STRONG_GAP;
            case TORNADO -> true;
            default -> choice.nearest() >= minRange && choice.nearest() <= maxRange;
        };
    }

    /**
     * Picks the next attack by weight among those that are ready and in range, or null if none is. The same attack is
     * never chosen twice in a row; Entomb and Mind Crush never within {@value #STRONG_GAP} ticks of each other; the first
     * attack after a stagger is the Lances; a Portal Step comes only when {@link Choice#portalWanted}.
     */
    public static ApocalypseAttack choose(Choice choice, RandomSource random) {
        if (choice.afterStagger() && LANCES.allowed(choice)) {
            return LANCES;
        }
        List<ApocalypseAttack> candidates = new ArrayList<>();
        int total = 0;
        for (ApocalypseAttack attack : values()) {
            if (attack.allowed(choice)) {
                candidates.add(attack);
                total += attack.weight;
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        int pick = random.nextInt(total);
        for (ApocalypseAttack attack : candidates) {
            pick -= attack.weight;
            if (pick < 0) {
                return attack;
            }
        }
        return candidates.get(candidates.size() - 1);
    }
}
