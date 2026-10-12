package com.kolodkingal.enhanced.thanos;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * What the move that Thanos is making remembers while it runs (server only): how far it is, whom it is aimed at, where
 * its aim froze, the spots it marked, whom it has already hit. {@link ThanosMoves} reads and writes it; {@link
 * ThanosEntity} owns one at a time.
 */
final class MoveState {
    final ThanosMove move;
    /** Ticks since the move began (the wind-up included). */
    int t;
    /** The wind-up of this run: the move's own, or a shorter one when it follows a Portal Step. */
    final int tell;
    @Nullable
    LivingEntity target;
    /** The aim: a unit direction for a blow or a beam; set to the target's position by moves that need a point. */
    Vec3 aim = Vec3.ZERO;
    boolean locked;
    /** A spot the move marked (the Smash's impact, the Leap's landing, the Step's arrival). */
    Vec3 spot = Vec3.ZERO;
    final List<Vec3> spots = new ArrayList<>();
    final List<LivingEntity> targets = new ArrayList<>();
    final IntSet hit = new IntOpenHashSet();
    /** The creature the Choke holds. */
    @Nullable
    LivingEntity grabbed;
    /** The damage the boss took since the move began (a Choke is dropped by 60). */
    float taken;
    /** The poise damage taken while the move runs its effect (a Soul Drain is broken by 60). */
    float poise;
    /** What follows at once (a Portal Step chains a Jab or a Sweep). */
    @Nullable
    ThanosMove chain;
    /** Set when the effect has begun, so a move that fires once does so once. */
    boolean fired;
    /** Free counters for the move's own use. */
    int phase;
    double scalar;

    MoveState(ThanosMove move, @Nullable LivingEntity target, int tell) {
        this.move = move;
        this.target = target;
        this.tell = tell;
    }

    /** The tick of the effect: the wind-up's last. */
    int fireTick() {
        return tell;
    }

    /** Ticks since the effect began (negative during the wind-up). */
    int sinceFire() {
        return t - tell;
    }
}
