package com.kolodkingal.enhanced.apocalypse;

import com.kolodkingal.enhanced.config.ApocalypseBossConfig;
import com.kolodkingal.enhanced.config.ApocalypseShieldConfig;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The boss's golden shield as a state machine, not a wall (the heart of the fight, spec 3.5). It has <em>strength</em>;
 * only mind power wears it down (everything else does exactly nothing to it, which {@link ApocalypseEntity#hurt}
 * decides before it gets here); at zero it breaks and leaves him vulnerable for a window; it re-forms stronger.
 *
 * <pre>
 * FORMING (40 ticks, invulnerable) -&gt; UP -&gt; [strength 0] -&gt; BROKEN (a window) -&gt; REFORMING (40 ticks) -&gt; UP (again)
 *                                                       \-&gt; (the third shield) GONE, for good: the armour falls
 * </pre>
 *
 * <p>This class knows nothing of the world: it counts ticks and strength. The entity asks it, the fight drives its phases
 * ({@link #form}, {@link #reform}) and is told when a window ends by time. All numbers are in {@code [apocalypse_boss]}
 * and {@code [apocalypse_shield]}.
 */
public final class ApocalypseShield {
    /** How long a shield takes to knit (and to knit again), in ticks. */
    public static final int FORM_TICKS = 40;
    /** The radius of the boss's sphere, in blocks (the hero's is {@code [apocalypse].shieldRadius}, 2.4). */
    public static final double RADIUS = 2.6;

    public enum State {
        /** Knitting up from the ground: invulnerable, the strength grows to its full value. */
        FORMING,
        /** Up: non-mind damage does exactly nothing, mind power wears it. */
        UP,
        /** Broken: a window in which everything hurts him. */
        BROKEN,
        /** Knitting again after a window that ended by time. */
        REFORMING,
        /** Gone for good (before it forms the first time, and after the third break). */
        GONE
    }

    /** What a mind-power hit did to the shield. */
    public enum Result {
        /** Not counted: the shield is not up, or the same attacker hit again within the shield's own cooldown. */
        IGNORED,
        /** Counted: the shield lost strength and is still up. */
        WORN,
        /** Counted: the shield lost its last strength and broke. */
        BROKEN
    }

    private State state = State.GONE;
    private float strength;
    private float max = 1.0F;
    private int stateTicks;
    private int windowLeft;
    private long lastMindHit = Long.MIN_VALUE / 2;
    private boolean stripped;
    private final Map<UUID, Long> lastCounted = new HashMap<>();

    public State state() {
        return state;
    }

    /** The strength it has now (while it knits, the part that has grown). */
    public float strength() {
        return strength;
    }

    /** The strength of the full shield. */
    public float max() {
        return max;
    }

    /** The part of its full strength it has, 0 to 1. */
    public float fraction() {
        return max <= 0.0F ? 0.0F : Math.min(1.0F, strength / max);
    }

    public boolean isUp() {
        return state == State.UP;
    }

    /** Whether everything hurts him: in a window, or with the shield gone for good. */
    public boolean isVulnerable() {
        return state == State.BROKEN || state == State.GONE;
    }

    /** The ticks left of the window (0 outside one). */
    public int windowLeft() {
        return state == State.BROKEN ? windowLeft : 0;
    }

    /** Ticks since the shield entered its state. */
    public int stateTicks() {
        return stateTicks;
    }

    /** The game time at which mind power last wore the shield. */
    public long lastMindHit() {
        return lastMindHit;
    }

    // ---- driving it (the fight) ----

    /** A new shield of {@code max} strength starts to knit; nothing can touch him meanwhile. */
    public void form(float max) {
        this.max = Math.max(1.0F, max);
        this.strength = 0.0F;
        enter(State.FORMING);
    }

    /** The shield knits again at full strength after a window (its strength may change with the fighters). */
    public void reform(float max) {
        this.max = Math.max(1.0F, max);
        this.strength = 0.0F;
        enter(State.REFORMING);
    }

    /** The shield is up at once at full strength (a test, or a fight resumed in a later phase). */
    public void raiseNow(float max) {
        this.max = Math.max(1.0F, max);
        this.strength = this.max;
        enter(State.UP);
    }

    /** Takes the shield away for good. */
    public void remove() {
        strength = 0.0F;
        enter(State.GONE);
    }

    /** Breaks the shield now, opening a window of {@code windowTicks} (0 = gone for good). */
    public void shatter(int windowTicks) {
        strength = 0.0F;
        lastCounted.clear();
        if (windowTicks > 0) {
            windowLeft = windowTicks;
            stripped = false;
            enter(State.BROKEN);
        } else {
            enter(State.GONE);
        }
    }

    /** Lengthens the window that is open (Magneto's strip); once a window. */
    public boolean extendWindow(int ticks) {
        if (state != State.BROKEN || stripped) {
            return false;
        }
        stripped = true;
        windowLeft += ticks;
        return true;
    }

    /** Whether Magneto has already stripped the armour in this window. */
    public boolean isStripped() {
        return stripped;
    }

    /** Ends a window early (the floor was reached): nothing left to wait for. */
    public void closeWindow() {
        if (state == State.BROKEN) {
            windowLeft = 0;
        }
    }

    // ---- being hit ----

    /**
     * Mind power reaches the shield. The attacker's hits are spaced by {@code mindHitCooldownTicks}: a shield that
     * returns without hurting leaves no invulnerability frames, and the Chaos Beam hits every tick, so it keeps its own.
     *
     * @param attacker the player whose mind it is (the cooldown is theirs)
     * @param amount   the strength it removes (the damage times its factor)
     * @param now      the game time
     * @param window   the ticks of the window if this hit breaks it (0: the shield is gone for good)
     */
    public Result absorb(UUID attacker, float amount, long now, int window) {
        if (state != State.UP || amount <= 0.0F) {
            return Result.IGNORED;
        }
        Long last = lastCounted.get(attacker);
        if (last != null && now - last < ApocalypseShieldConfig.MIND_HIT_COOLDOWN_TICKS.get()) {
            return Result.IGNORED;
        }
        lastCounted.put(attacker, now);
        return wear(amount, now, window);
    }

    /** A strain on the shield that is not damage (a Phoenix's Mind Control or Psionic Field): no cooldown. */
    public Result strain(float amount, long now, int window) {
        if (state != State.UP || amount <= 0.0F) {
            return Result.IGNORED;
        }
        return wear(amount, now, window);
    }

    private Result wear(float amount, long now, int window) {
        lastMindHit = now;
        strength -= amount;
        if (strength <= 1.0E-4F) {
            shatter(window);
            return Result.BROKEN;
        }
        return Result.WORN;
    }

    // ---- time ----

    /**
     * One tick: the shield grows while it knits, regains strength once it has been left alone, and a window runs out.
     *
     * @return true when a window ended <em>by time</em> this tick (the fight then re-forms the shield)
     */
    public boolean tick(long now) {
        stateTicks++;
        switch (state) {
            case FORMING, REFORMING -> {
                strength = max * Math.min(1.0F, (float) stateTicks / FORM_TICKS);
                if (stateTicks >= FORM_TICKS) {
                    strength = max;
                    enter(State.UP);
                }
            }
            case UP -> {
                if (strength < max && now - lastMindHit >= ApocalypseShieldConfig.SHIELD_REGEN_DELAY_TICKS.get()) {
                    strength = Math.min(max, strength + max * ApocalypseShieldConfig.SHIELD_REGEN_FRACTION.get().floatValue() / 20.0F);
                }
                if (lastCounted.size() > 8 && stateTicks % 100 == 0) {
                    Iterator<Long> iterator = lastCounted.values().iterator();
                    while (iterator.hasNext()) {
                        if (now - iterator.next() > 200) {
                            iterator.remove();
                        }
                    }
                }
            }
            case BROKEN -> {
                if (windowLeft > 0 && --windowLeft == 0) {
                    return true;
                }
            }
            case GONE -> {
            }
        }
        return false;
    }

    private void enter(State next) {
        state = next;
        stateTicks = 0;
    }

    // ---- the numbers ----

    /**
     * The strength of shield {@code phase} (1 to 3) for {@code fighters} fighters: {@code shieldStrength x shieldGrowth
     * ^ (phase - 1) x (1 + shieldPerExtraPlayer x (n - 1))}: 150, 188 and 234 for one fighter, 255, 319 and 398 for
     * three.
     */
    public static float strengthFor(int phase, int fighters) {
        double base = ApocalypseBossConfig.SHIELD_STRENGTH.get() * Math.pow(ApocalypseBossConfig.SHIELD_GROWTH.get(), Math.max(0, phase - 1));
        return (float) (base * (1.0 + ApocalypseBossConfig.SHIELD_PER_EXTRA_PLAYER.get() * (Math.max(1, fighters) - 1)));
    }
}
