package com.kolodkingal.enhanced.client.render;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.ThanosConfig;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Toggle;
import org.jetbrains.annotations.Nullable;

import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.E;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.I;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.O;
import static com.kolodkingal.enhanced.client.render.AnimMath.Ease.S;
import static com.kolodkingal.enhanced.client.render.Gestures.Target.LOOK;
import static com.kolodkingal.enhanced.client.render.Gestures.define;

/**
 * The keys of the gestures of the playing Thanos (DESIGN section 15.12): ten ways of using the Gauntlet, one for each
 * stone and each of its two uses, plus the Veil closing and opening. Every one is the Gauntlet arm's: the Gauntlet is on
 * the off arm ({@code O}), so the main arm ({@code M}) only balances, or joins in for the two-handed ones (the Veil,
 * the Time Lock). The Power Stone thrusts and slams, the Space Stone draws circles and drags, the Reality Stone flicks,
 * the Soul Stone lashes and looks, the Time Stone presses down and holds up a palm; a glance at the arm tells the stone.
 *
 * <p>The poses the gestures hand over to are here too ({@link #VEIL_O}, {@link #DRAIN_M}, ...), because the stance rows
 * ({@link ThanosPoses}) and the last keys of the gestures that hand over to them must be the same numbers. No Minecraft
 * client imports: the tables, the routes told by the length of a cooldown, and {@link #predicted} are checked by the
 * game tests.
 */
public final class ThanosGestures {
    /**
     * The Veil: the Gauntlet hand held a little out and up, as if it held a thin film. The main arm only spreads
     * ({@link #VEIL_M_Z} is added to whatever the arm is doing, so he can walk with it); the Gauntlet arm is held.
     */
    public static final float[] VEIL_O = {-30, 0, -22};
    public static final float VEIL_M_Z = 10;
    public static final float VEIL_HEAD = -2;
    /** The Soul Drain: the main arm braced back and out, the head down a little, the body leaning back. */
    public static final float[] DRAIN_M = {-12, 0, 8};
    public static final float DRAIN_HEAD = -3;
    public static final float DRAIN_LEAN = -2;
    /** The legs braced in the Drain: x and z of the right leg (the left is the mirror). */
    public static final float[] DRAIN_LEG = {-6, 4};

    /** The speed (blocks per tick) from which a landing is a Titan's: a fall of more than three blocks (0.65 after nine ticks). */
    public static final float HARD_LANDING = 0.60F;

    /** The cooldowns the config falls back on before it has loaded (the defaults), to tell the two uses of a key apart. */
    private static final int[] DEFAULTS = {70, 160, 100, 220, 900, 600};

    static void register() {
        power();
        space();
        reality();
        soul();
        time();
    }

    // ---- The Power Stone: the thrust and the slam ----

    private static void power() {
        // The Gauntlet is thrust at the aim and holds, trembling; the other arm balances. The chest follows the arm
        // that goes forward (a left arm forward turns the chest to the right, which is positive).
        define(GestureId.THANOS_BLAST)
                .key(0, S).o(-40, -6, -8).m(-5, 0, 8).lean(0).twist(0)
                .key(2, O).oAim(LOOK, -2, 0, 0).m(5, 0, 12).lean(3).twist(8)
                .key(4, S).oAim(LOOK, -2, 0, 0).oTremble(1.0F, 2.0F)
                .key(10, S).o(-30, 0, -10).m(-8, 0, 8).lean(0).twist(0);
        // The Gauntlet raised overhead, then driven into the ground: the whole body goes down with it.
        define(GestureId.THANOS_SLAM)
                .key(0, S).m(-8, 0, 8).o(-8, 0, -8).lean(0).drop(0).legs(0, 0)
                .key(6, E).o(-175, 0, -12).m(-60, 0, 12).lean(-6).drop(0).legs(0, 0)
                .key(10, S).o(-175, 0, -12).m(-60, 0, 12).lean(-6).drop(0).legs(0, 0)
                .key(12, I).o(-4, 0, -10).m(-20, 0, 8).lean(12).drop(0.24F).legs(-26, -26)
                .key(14, S).o(-4, 0, -10).m(-20, 0, 8).lean(12).drop(0.24F).legs(-26, -26)
                .key(22, S).o(-8, 0, -8).m(-8, 0, 8).lean(0).drop(0).legs(0, 0);
    }

    // ---- The Space Stone: the circle and the drag ----

    private static void space() {
        // The Gauntlet draws a circle in the air: a ring.
        define(GestureId.THANOS_STEP)
                .key(0, S).o(-30, 10, -10).lean(0)
                .key(3, E).o(-80, -25, -30)
                .key(6, E).o(-60, -45, -10).lean(2)
                .key(9, E).o(-30, -20, -5)
                .key(12, S).o(-8, 0, -8).lean(0);
        // The palm forward, then dragged back, and he leans away from the pull.
        define(GestureId.THANOS_PULL)
                .key(0, S).o(-90, 0, -10).m(-10, 0, 8).lean(0).head(0, 0, 0)
                .key(5, O).o(-95, 0, -12).lean(-2)
                .key(10, E).o(-40, 0, -10).lean(-6).head(-4, 0, 0)
                .key(16, S).o(-8, 0, -8).lean(0).head(0, 0, 0);
    }

    // ---- The Reality Stone: the flick and the veil ----

    private static void reality() {
        // A flick toward the creature, fingers spread.
        define(GestureId.THANOS_WARP)
                .key(0, S).o(-20, 0, -8)
                .key(2, O).oAim(LOOK, -4, 0, 0).oTremble(1.2F, 2.4F)
                .key(6, S).oAim(LOOK, 0, 0, 0).oTremble(1.2F, 2.4F)
                .key(14, S).o(-12, 0, -8);
        // Both arms out, then closing round him into the pose of the stance that takes over (the main arm's spread is
        // the stance's, added to whatever the arm does).
        define(GestureId.THANOS_VEIL_ON)
                .key(0, S).m(-8, 0, 8).o(-8, 0, -8).head(0, 0, 0)
                .key(5, O).m(-20, 0, 45).o(-20, 0, -45).head(-6, 0, 0)
                .key(12, E).m(0, 0, VEIL_M_Z).o(VEIL_O).head(VEIL_HEAD, 0, 0)
                .key(16, S).m(0, 0, VEIL_M_Z).o(VEIL_O).head(VEIL_HEAD, 0, 0);
        // The film is let go: the hand opens, the arms drop.
        define(GestureId.THANOS_VEIL_OFF)
                .key(0, S).m(0, 0, VEIL_M_Z).o(VEIL_O).head(VEIL_HEAD, 0, 0)
                .key(4, O).m(-6, 0, 30).o(-6, 0, -30).head(0, 0, 0)
                .key(10, S).m(-8, 0, 8).o(-8, 0, -8);
    }

    // ---- The Soul Stone: the chain and the sight ----

    private static void soul() {
        // The chain lashes out and the gesture hands over to the stance of the drain: the Gauntlet arm along the look,
        // the body leaning back a little (the stance's lean is its own, so this one ends at 0).
        define(GestureId.THANOS_DRAIN_CAST)
                .key(0, S).o(-30, 0, -8).lean(0)
                .key(2, O).oAim(LOOK, -4, 0, 0).lean(4)
                .key(12, S).oAim(LOOK, 0, 0, 0).oTremble(1.4F, 1.9F).lean(0);
        // The Gauntlet to the eye, then swept across.
        define(GestureId.THANOS_SIGHT)
                .key(0, S).o(-8, 0, -8).head(0, 0, 0)
                .key(4, O).o(-150, -30, 0).head(-6, 0, 0)
                .key(9, E).o(-110, 40, -10).head(-6, 10, 0)
                .key(14, S).o(-8, 0, -8).head(0, 0, 0);
    }

    // ---- The Time Stone: the held palm and the stamp ----

    private static void time() {
        // The palm up, trembling; the arm drawn back, and he leans away.
        define(GestureId.THANOS_REWIND)
                .key(0, S).o(-8, 0, -8).head(0, 0, 0).lean(0)
                .key(4, O).o(-120, 0, -40).oTremble(2.0F, 1.8F).head(-8, 0, 0)
                .key(12, S).o(-120, 0, -40).oTremble(2.0F, 1.8F).head(-8, 0, 0).lean(0)
                .key(16, E).o(-60, 0, -20).lean(-5)
                .key(22, S).o(-8, 0, -8).lean(0).head(0, 0, 0);
        // Both palms pressing down: a stamp.
        define(GestureId.THANOS_TIMELOCK)
                .key(0, S).m(-8, 0, 8).o(-8, 0, -8).drop(0).lean(0)
                .key(6, O).m(-80, 20, 20).o(-80, -20, -20).drop(0.04F)
                .key(10, I).m(-30, 0, 12).o(-30, 0, -12).drop(0.12F).lean(6)
                .key(20, S).m(-8, 0, 8).o(-8, 0, -8).drop(0).lean(0);
    }

    // ---- Which stances are on ----

    /** Switches his two stances on from his data: the Veil while it is up, the Drain while it holds. */
    public static void stances(PowerData data, boolean[] on) {
        on[Stance.THANOS_VEIL.ordinal()] = data.toggle(Toggle.REALITY_VEIL);
        on[Stance.THANOS_DRAIN.ordinal()] = data.is(Live.SOUL_DRAINING);
    }

    // ---- Which gesture a cooldown stands for ----

    /**
     * The gesture the cooldown of an ability starting stands for, or null. The two uses of a key start cooldowns of
     * different lengths (the Blast's 70 ticks and the Slam's 160, the Step's 100 and the Pull's 220, the Rewind's 900 and
     * the Lock's 600), and the ticks that are left when the data arrives tell them apart: the use whose configured
     * cooldown is nearer to it plays. The Transmute is the only use of V that has a cooldown (the Veil is a toggle), and
     * the Soul Stone's two uses are told by their own states ({@link Live#SOUL_DRAINING}, {@link Live#SOUL_SIGHT}): the
     * cooldown that ends a drain, or begins a Sight, is nothing the hand acts twice.
     */
    @Nullable
    public static GestureId cooldown(int ability, long left) {
        boolean loaded = EnhancedConfig.SPEC.isLoaded();
        return cooldown(ability, left,
                loaded ? ThanosConfig.POWER_BLAST_COOLDOWN.get() : DEFAULTS[0], loaded ? ThanosConfig.SLAM_COOLDOWN.get() : DEFAULTS[1],
                loaded ? ThanosConfig.SPACE_STEP_COOLDOWN.get() : DEFAULTS[2], loaded ? ThanosConfig.PULL_COOLDOWN.get() : DEFAULTS[3],
                loaded ? ThanosConfig.REWIND_COOLDOWN.get() : DEFAULTS[4], loaded ? ThanosConfig.TIME_LOCK_COOLDOWN.get() : DEFAULTS[5]);
    }

    /** As {@link #cooldown(int, long)}, with the six cooldowns given (ticks): the Blast, the Slam, the Step, the Pull, the Rewind, the Lock. */
    @Nullable
    public static GestureId cooldown(int ability, long left, int blast, int slam, int step, int pull, int rewind, int lock) {
        return switch (ability) {
            case 1 -> nearer(left, blast, GestureId.THANOS_BLAST, slam, GestureId.THANOS_SLAM);
            case 2 -> nearer(left, step, GestureId.THANOS_STEP, pull, GestureId.THANOS_PULL);
            case 3 -> GestureId.THANOS_WARP;
            case 5 -> nearer(left, rewind, GestureId.THANOS_REWIND, lock, GestureId.THANOS_TIMELOCK);
            default -> null;
        };
    }

    /** The use whose cooldown the ticks left are nearer to (the first, in a tie: nothing tells them apart). */
    private static GestureId nearer(long left, int a, GestureId first, int b, GestureId second) {
        return Math.abs(left - a) <= Math.abs(left - b) ? first : second;
    }

    /** Whether the gesture is one of the two uses of a key that only the length of a cooldown tells from the other. */
    public static boolean toldByCooldown(GestureId id) {
        return id == GestureId.THANOS_SLAM || id == GestureId.THANOS_PULL || id == GestureId.THANOS_REWIND;
    }

    // ---- What the key presses start, before the server has answered ----

    /**
     * The gesture the local Thanos's own key press starts at once, or null: only where the client can tell from its
     * own copy of the data and of the world that the stone will answer. The guards only a client can see are told by
     * the caller: {@code able} (he is not held, starving or a spectator), {@code creature} (a living creature stands in
     * his sight, within the range of the stone), {@code anything} (a block or a creature does, within the range of
     * the Step) and {@code history} (the Rewind has two seconds to go back into). A refusal the client cannot see (a
     * Hex wall, no room to stand, PvP) still lets the gesture play out, and it costs nothing.
     */
    @Nullable
    public static GestureId predicted(PowerData data, int ability, long now, boolean sneaking, boolean able, boolean creature, boolean anything,
                                      boolean history) {
        if (!able || ability < 1 || ability > PowerData.ABILITIES) {
            return null;
        }
        boolean ready = data.isReady(ability, now);
        return switch (ability) {
            // The Blast fires at the sky as readily as at a creature; the Slam needs only the ground.
            case 1 -> ready ? (sneaking ? GestureId.THANOS_SLAM : GestureId.THANOS_BLAST) : null;
            case 2 -> !ready ? null : sneaking ? (creature ? GestureId.THANOS_PULL : null) : (anything ? GestureId.THANOS_STEP : null);
            // The Veil switches at once whatever the Transmute is doing; the Transmute needs a creature.
            case 3 -> sneaking ? (data.toggle(Toggle.REALITY_VEIL) ? GestureId.THANOS_VEIL_OFF : GestureId.THANOS_VEIL_ON)
                    : ready && creature ? GestureId.THANOS_WARP : null;
            case 4 -> !ready ? null : sneaking ? GestureId.THANOS_SIGHT
                    : creature && !data.is(Live.SOUL_DRAINING) ? GestureId.THANOS_DRAIN_CAST : null;
            case 5 -> !ready ? null : sneaking ? GestureId.THANOS_TIMELOCK : history ? GestureId.THANOS_REWIND : null;
            default -> null;
        };
    }

    private ThanosGestures() {
    }
}
