package com.kolodkingal.enhanced.thanos;

import com.kolodkingal.enhanced.config.ThanosBossConfig;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * The moves of the Mad Titan: the table of DESIGN section 15.5, one row each. A move has a stone (or none: the sword
 * and the fist), a wind-up (the tell everyone sees), a tick at which his aim freezes, the ticks that follow the wind-up
 * (the effect and the animation's tail), a base weight in his choice of the next move and its own cooldown.
 *
 * <p>Every move of a stone is a <em>clench</em>: the Gauntlet closes, and enough poise damage in the wind-up breaks
 * it ({@code staggerPoise}). The sword and fist moves are not. There is no move of the Mind Stone: he never has it.
 * The reactive moves are chosen by their triggers, before the weighted choice; the Infinity Beam by its clock.
 *
 * <p>The ticks are those of the pose table ({@code ThanosModel}): {@link #length} is how long the animation plays from
 * the move's first tick.
 */
public enum ThanosMove {
    // ---- the sword and the fist ----
    SWORD_SWEEP(null, 16, 12, 14, 5.0, () -> ThanosBossConfig.SWORD_COOLDOWN.get()),
    OVERHEAD_SMASH(null, 24, 16, 16, 3.0, () -> ThanosBossConfig.SMASH_COOLDOWN.get()),
    GAUNTLET_JAB(null, 8, 0, 14, 4.0, () -> ThanosBossConfig.JAB_COOLDOWN.get()),
    SWORD_THROW(null, 14, 14, 10, 2.0, () -> ThanosBossConfig.SWORD_THROW_COOLDOWN.get()),
    CHOKE(null, 14, 8, 50, 1.5, () -> ThanosBossConfig.CHOKE_COOLDOWN.get()),
    LEAP(null, 18, 12, 26, 2.0, () -> ThanosBossConfig.LEAP_COOLDOWN.get()),

    // ---- the Power Stone ----
    POWER_BLAST(StoneKind.POWER, 22, 14, 8, 3.0, () -> ThanosBossConfig.POWER_BLAST_COOLDOWN.get()),
    OVERLOAD(StoneKind.POWER, 30, 12, 8, 2.5, () -> ThanosBossConfig.OVERLOAD_COOLDOWN.get()),
    SHOCKWAVE(StoneKind.POWER, 20, 0, 16, 2.0, () -> ThanosBossConfig.SHOCKWAVE_COOLDOWN.get()),
    POWER_PULSE(StoneKind.POWER, 40, 0, 8, 0.0, () -> ThanosBossConfig.PULSE_COOLDOWN.get()),

    // ---- the Space Stone ----
    PORTAL_STEP(StoneKind.SPACE, 16, 0, 14, 3.0, () -> ThanosBossConfig.STEP_COOLDOWN.get()),
    METEOR_HAIL(StoneKind.SPACE, 30, 6, 40, 2.5, () -> ThanosBossConfig.METEOR_COOLDOWN.get()),
    GRAVITY_PULL(StoneKind.SPACE, 24, 0, 26, 2.0, () -> ThanosBossConfig.PULL_COOLDOWN.get()),
    SPACE_BARRIER(StoneKind.SPACE, 10, 0, 6, 0.0, () -> ThanosBossConfig.BARRIER_COOLDOWN.get()),

    // ---- the Reality Stone ----
    REALITY_VEIL(StoneKind.REALITY, 12, 0, 6, 0.0, () -> ThanosBossConfig.VEIL_COOLDOWN.get()),
    PHANTOMS(StoneKind.REALITY, 24, 0, 10, 1.5, () -> ThanosBossConfig.PHANTOMS_COOLDOWN.get()),
    PETRIFY_ORB(StoneKind.REALITY, 14, 14, 14, 2.5, () -> ThanosBossConfig.ORB_COOLDOWN.get()),

    // ---- the Soul Stone ----
    SOUL_SIGHT(StoneKind.SOUL, 10, 0, 6, 0.0, () -> ThanosBossConfig.SIGHT_COOLDOWN.get()),
    SOUL_DRAIN(StoneKind.SOUL, 26, 0, 86, 3.0, () -> ThanosBossConfig.DRAIN_COOLDOWN.get()),
    SOULWORLD(StoneKind.SOUL, 30, 0, 8, 1.0, () -> ThanosBossConfig.SOULWORLD_COOLDOWN.get()),

    // ---- the Time Stone ----
    REWIND(StoneKind.TIME, 30, 0, 8, 0.0, () -> ThanosBossConfig.REWIND_COOLDOWN.get()),
    TIME_SNARE(StoneKind.TIME, 24, 10, 8, 2.5, () -> ThanosBossConfig.SNARE_COOLDOWN.get()),
    INFINITY_BEAM(StoneKind.TIME, 50, 40, 36, 0.0, () -> ThanosBossConfig.INFINITY_BEAM_COOLDOWN.get());

    private final StoneKind stone;
    private final int tell;
    private final int lock;
    private final int after;
    private final double weight;
    private final IntSupplier cooldown;

    ThanosMove(@Nullable StoneKind stone, int tell, int lock, int after, double weight, IntSupplier cooldown) {
        this.stone = stone;
        this.tell = tell;
        this.lock = lock;
        this.after = after;
        this.weight = weight;
        this.cooldown = cooldown;
    }

    /** The stone this move draws on, or null for the sword and the fist. */
    @Nullable
    public StoneKind stone() {
        return stone;
    }

    /** Whether the move needs the Gauntlet: it is a clench that poise breaks and a locked Gauntlet forbids. */
    public boolean clench() {
        return stone != null;
    }

    /** The wind-up, in ticks: the tell. The effect begins when it ends. */
    public int tell() {
        return tell;
    }

    /** The tick of the wind-up at which his aim freezes (0: it never tracks; a player who sidesteps dodges after it). */
    public int lock() {
        return lock;
    }

    /** The ticks after the wind-up: the effect running, then the animation's tail. */
    public int after() {
        return after;
    }

    /** The whole move, wind-up included. */
    public int length() {
        return tell + after;
    }

    /** The base weight of the move in his choice (0 for the moves that are chosen by their trigger). */
    public double weight() {
        return weight;
    }

    /** The move's own cooldown, in ticks. */
    public int cooldown() {
        return cooldown.getAsInt();
    }

    /** Whether the move is chosen by a trigger rather than by weight: the Barrier, the Veil, the Sight, the Rewind, the Pulse. */
    public boolean reactive() {
        return this == SPACE_BARRIER || this == REALITY_VEIL || this == SOUL_SIGHT || this == REWIND || this == POWER_PULSE;
    }

    /** The moves he has with {@code stones} stones, in the order of this enum. */
    public static List<ThanosMove> available(int stones) {
        List<ThanosMove> moves = new ArrayList<>();
        for (ThanosMove move : values()) {
            if (move.stone == null || move.stone.ordinal() < stones) {
                moves.add(move);
            }
        }
        return moves;
    }
}
