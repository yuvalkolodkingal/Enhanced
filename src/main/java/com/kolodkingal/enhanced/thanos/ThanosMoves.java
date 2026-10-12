package com.kolodkingal.enhanced.thanos;

import com.kolodkingal.enhanced.ability.LokiAbilities;
import com.kolodkingal.enhanced.ability.Teleports;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.config.ThanosBossConfig;
import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModEffects;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * What each of the Mad Titan's moves does, tick by tick (DESIGN section 15.5). {@link #choose} is his choice of the next
 * move (the reactive ones by their triggers, the rest by weight, distance and who is near); {@link #begin} starts a move's
 * wind-up; {@link #tick} runs it: it tracks the target until the aim locks, sends the tell, strikes at the end of the
 * wind-up and runs the effect that follows; {@link #finish} and {@link #interrupted} let go of whatever a move held.
 *
 * <p>Every number is the config's, every damage is scaled by {@link ThanosEntity#damageScale()}, and no move changes a
 * block (the single exception is the opt-in crater of {@link StonePowers#craterIfAllowed}). The tells use {@link
 * ThanosEffects}: gold for the sword and the fist, purple for Power, blue for Space, red for Reality, orange for Soul,
 * green for Time.
 */
final class ThanosMoves {
    private static final int GOLD = 0xD9C48A;
    /** How long the Leap flies, about: the landing ring's clock. */
    private static final int LEAP_FLIGHT_TICKS = 20;

    /** A move and the creature it is aimed at. */
    record Choice(ThanosMove move, LivingEntity target) {
    }

    // =========================================================================================================
    // Choosing

    /**
     * His next move, or null if nothing fits (he walks closer). The reactive moves come first, by their triggers; then
     * the Infinity Beam, on its clock; then a weighted choice among what the situation allows.
     */
    @Nullable
    static Choice choose(ThanosEntity boss, LivingEntity target) {
        ServerLevel level = (ServerLevel) boss.level();
        RandomSource random = boss.getRandom();
        boolean locked = boss.gauntletLocked();
        if (!locked) {
            ServerPlayer hiding = boss.hiddenFighter();
            if (hiding != null && boss.hasStone(StoneKind.POWER)) {
                return new Choice(ThanosMove.POWER_PULSE, hiding);
            }
            if (boss.hasStone(StoneKind.TIME) && boss.ready(ThanosMove.REWIND) && boss.rewindSnapshot() != null
                    && boss.damageIn(ThanosBossConfig.REWIND_SECONDS.get() * 20) >= boss.maxPoints() * ThanosBossConfig.REWIND_TRIGGER.get()) {
                return new Choice(ThanosMove.REWIND, target);
            }
            if (boss.hasStone(StoneKind.SPACE) && boss.ready(ThanosMove.SPACE_BARRIER) && !boss.hasBarrier()
                    && (boss.rangedHitsIn(40) >= 4 || boss.ticksSinceRangedHit() < 20 && random.nextFloat() < 0.12F)) {
                return new Choice(ThanosMove.SPACE_BARRIER, target);
            }
            if (boss.hasStone(StoneKind.REALITY) && boss.ready(ThanosMove.REALITY_VEIL) && !boss.hasVeil() && boss.ticksSinceRangedHit() <= 100) {
                return new Choice(ThanosMove.REALITY_VEIL, target);
            }
            if (boss.hasStone(StoneKind.SOUL) && boss.ready(ThanosMove.SOUL_SIGHT) && boss.tricksNear(level, 40.0)) {
                return new Choice(ThanosMove.SOUL_SIGHT, target);
            }
            if (boss.hasStone(StoneKind.TIME) && boss.stones() >= StoneKind.granted().size() && boss.ready(ThanosMove.INFINITY_BEAM)) {
                return new Choice(ThanosMove.INFINITY_BEAM, target);
            }
        }
        List<ServerPlayer> mortals = boss.mortals();
        List<Choice> options = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        double total = 0.0;
        for (ThanosMove move : ThanosMove.available(boss.stones())) {
            if (move.reactive() || move == ThanosMove.INFINITY_BEAM || move.weight() <= 0.0 || !boss.ready(move)) {
                continue;
            }
            if (move.clench() && locked) {
                continue;
            }
            double weight = weight(boss, move, target, mortals);
            if (weight <= 0.0) {
                continue;
            }
            if (boss.usedRecently(move)) {
                weight *= 0.5;
            }
            if (boss.showingOff() && move.stone() == boss.newestStone()) {
                weight *= 2.5;
            }
            options.add(new Choice(move, target));
            weights.add(weight);
            total += weight;
        }
        if (options.isEmpty()) {
            return null;
        }
        double roll = random.nextDouble() * total;
        for (int i = 0; i < options.size(); i++) {
            roll -= weights.get(i);
            if (roll <= 0.0) {
                return options.get(i);
            }
        }
        return options.get(options.size() - 1);
    }

    /** The weight of a move in the choice: 0 if the situation forbids it, else its base weight times what fits. */
    private static double weight(ThanosEntity boss, ThanosMove move, LivingEntity target, List<ServerPlayer> mortals) {
        double d = boss.distanceTo(target);
        boolean sight = boss.hasLineOfSight(target);
        boolean sword = !boss.swordAway();
        double base = move.weight();
        return switch (move) {
            case SWORD_SWEEP -> sword && d <= 5.5 ? base : 0.0;
            case OVERHEAD_SMASH -> sword && d <= 7.0 ? base : 0.0;
            case GAUNTLET_JAB -> d <= 4.0 ? base : 0.0;
            case SWORD_THROW -> sword && d >= 4.0 && d <= 28.0 && sight ? base : 0.0;
            case CHOKE -> d <= 3.2 && target instanceof ServerPlayer player && StonePowers.graspable(player)
                    ? base * (Powers.is(player, Power.LOKI) || Powers.is(player, Power.VISION) ? 3.0 : 1.0) : 0.0;
            case LEAP -> d >= 8.0 && d <= 30.0 ? (d > 15.0 ? 3.0 : base) : 0.0;
            case POWER_BLAST -> d >= 6.0 && d <= 30.0 && sight ? base : 0.0;
            case OVERLOAD -> {
                int seen = 0;
                for (ServerPlayer player : mortals) {
                    if (boss.distanceTo(player) <= 40.0 && boss.hasLineOfSight(player)) {
                        seen++;
                    }
                }
                yield seen >= 1 ? base * (1.0 + 0.3 * (seen - 1)) : 0.0;
            }
            case SHOCKWAVE -> {
                int close = 0;
                boolean flier = false;
                for (ServerPlayer player : mortals) {
                    double distance = boss.distanceTo(player);
                    close += distance <= 8.0 ? 1 : 0;
                    flier |= distance <= 6.0 && !player.onGround() && player.getY() - boss.getY() > 1.5;
                }
                yield close >= 2 || flier ? base * (1.0 + 0.5 * Math.max(0, close - 2)) : 0.0;
            }
            case PORTAL_STEP -> d >= 6.0 && d <= 40.0 && stepSpot(boss, target) != null ? base : 0.0;
            case METEOR_HAIL -> mortals.size() >= 2 || d > 10.0 ? base * (mortals.size() >= 3 ? 1.5 : 1.0) : 0.0;
            case GRAVITY_PULL -> {
                boolean any = false;
                for (ServerPlayer player : mortals) {
                    double distance = boss.distanceTo(player);
                    any |= distance >= 8.0 && distance <= 22.0 && boss.hasLineOfSight(player);
                }
                yield any ? base : 0.0;
            }
            case PHANTOMS -> mortals.size() >= 2 && !boss.hidden() ? base : 0.0;
            case PETRIFY_ORB -> d >= 6.0 && d <= 30.0 && sight && target instanceof ServerPlayer ? base : 0.0;
            case SOUL_DRAIN -> {
                boolean any = false;
                for (ServerPlayer player : mortals) {
                    any |= boss.distanceTo(player) <= 24.0 && boss.hasLineOfSight(player);
                }
                yield any ? base : 0.0;
            }
            case SOULWORLD -> {
                boolean any = false;
                for (ServerPlayer player : mortals) {
                    any |= boss.distanceTo(player) <= 40.0 && boss.hasLineOfSight(player);
                }
                yield any ? base : 0.0;
            }
            case TIME_SNARE -> boss.distanceTo(target) <= 30.0 ? base * (mortals.size() >= 2 ? 1.2 : 1.0) : 0.0;
            default -> 0.0;
        };
    }

    // =========================================================================================================
    // Starting, running and ending a move

    /** The wind-up begins: the clench, and whatever the move marks before it starts. */
    static void begin(ThanosEntity boss, MoveState c) {
        ServerLevel level = (ServerLevel) boss.level();
        if (c.target != null) {
            c.aim = dir(boss.handPosition(), chestOf(c.target));
            boss.setAim(c.aim);
        }
        if (c.move.clench()) {
            ThanosSounds.CLENCH.play(level, boss.position());
            if (c.move.stone() != null) {
                c.move.stone().voice().use(level, boss.position(), 0.5F, 1.0F);
            }
        } else {
            ThanosSounds.SWING.play(level, boss.position(), 0.5F, 1.2F);
        }
        switch (c.move) {
            case OVERLOAD -> c.targets.addAll(pickTargets(boss, Math.min(3, Math.max(1, boss.mortals().size())), 40.0, true));
            case TIME_SNARE -> c.targets.addAll(pickTargets(boss, Math.min(2, Math.max(1, boss.mortals().size())), 30.0, false));
            case SOUL_DRAIN -> c.targets.addAll(pickTargets(boss, Math.min(2, Math.max(1, boss.mortals().size())), 24.0, true));
            case PORTAL_STEP -> {
                Vec3 spot = c.target == null ? null : stepSpot(boss, c.target);
                if (spot != null) {
                    c.spot = spot;
                    ThanosEffects.portal(level, boss.position().add(0, 0.2, 0), 1.6F, c.tell);
                    ThanosEffects.portal(level, spot.add(0, 0.2, 0), 1.6F, c.tell);
                }
            }
            default -> {
            }
        }
    }

    /** One tick of the move; {@code c.t} counts from 1. */
    static void tick(ThanosEntity boss, MoveState c) {
        ServerLevel level = (ServerLevel) boss.level();
        trackAim(boss, c);
        int t = c.t;
        int fire = c.tell;
        switch (c.move) {
            case SWORD_SWEEP -> sweep(boss, c, level, t, fire);
            case OVERHEAD_SMASH -> smash(boss, c, level, t, fire);
            case GAUNTLET_JAB -> jab(boss, c, level, t, fire);
            case SWORD_THROW -> swordThrow(boss, c, level, t, fire);
            case CHOKE -> choke(boss, c, level, t, fire);
            case LEAP -> leap(boss, c, level, t, fire);
            case POWER_BLAST -> powerBlast(boss, c, level, t, fire);
            case OVERLOAD -> overload(boss, c, level, t, fire);
            case SHOCKWAVE -> shockwave(boss, c, level, t, fire);
            case POWER_PULSE -> powerPulse(boss, c, level, t, fire);
            case PORTAL_STEP -> portalStep(boss, c, level, t, fire);
            case METEOR_HAIL -> meteorHail(boss, c, level, t, fire);
            case GRAVITY_PULL -> gravityPull(boss, c, level, t, fire);
            case SPACE_BARRIER -> {
                if (t == fire) {
                    boss.raiseBarrier(ThanosBossConfig.BARRIER_TICKS.get());
                    ThanosEffects.dome(level, boss.position(), 3.0F, ThanosBossConfig.BARRIER_TICKS.get(), StoneKind.SPACE.color());
                    ThanosSounds.BARRIER_UP.play(level, boss.position());
                }
            }
            case REALITY_VEIL -> {
                if (t == 1) {
                    ThanosEffects.wave(level, boss.position(), 12.0F, c.tell, StoneKind.REALITY.color());
                }
                if (t == fire) {
                    boss.raiseVeil(ThanosBossConfig.VEIL_TICKS.get());
                    ThanosEffects.dome(level, boss.position(), 12.0F, ThanosBossConfig.VEIL_TICKS.get(), StoneKind.REALITY.color());
                }
            }
            case PHANTOMS -> phantoms(boss, c, level, t, fire);
            case PETRIFY_ORB -> petrifyOrb(boss, c, level, t, fire);
            case SOUL_SIGHT -> soulSight(boss, c, level, t, fire);
            case SOUL_DRAIN -> soulDrain(boss, c, level, t, fire);
            case SOULWORLD -> soulworld(boss, c, level, t, fire);
            case REWIND -> rewind(boss, c, level, t, fire);
            case TIME_SNARE -> timeSnare(boss, c, level, t, fire);
            case INFINITY_BEAM -> infinityBeam(boss, c, level, t, fire);
        }
    }

    /** The move ended by itself: whatever it held is let go. */
    static void finish(ThanosEntity boss, @Nullable MoveState c) {
        release(boss, c);
    }

    /** The move was broken off: whatever it held is let go. */
    static void interrupted(ThanosEntity boss, MoveState c) {
        release(boss, c);
    }

    private static void release(ThanosEntity boss, @Nullable MoveState c) {
        if (c != null && c.move == ThanosMove.CHOKE && c.grabbed != null) {
            c.grabbed.setDeltaMovement(c.grabbed.getDeltaMovement().multiply(0.2, 0.0, 0.2));
            c.grabbed.hurtMarked = true;
            c.grabbed = null;
        }
    }

    /** Whether Stormbreaker cuts this move short when it is running (the beams). */
    static boolean cutsBeam(ThanosMove move) {
        return move == ThanosMove.POWER_BLAST || move == ThanosMove.INFINITY_BEAM;
    }

    // ---- aiming ----

    /** Tracks the target until the aim locks, then holds it; faces the way he aims. */
    private static void trackAim(ThanosEntity boss, MoveState c) {
        LivingEntity target = c.target;
        int lock = Math.min(c.move.lock(), c.tell);
        if (!c.locked && target != null && target.isAlive() && c.move != ThanosMove.METEOR_HAIL) {
            c.aim = dir(boss.handPosition(), chestOf(target));
            boss.setAim(c.aim);
            boss.faceToward(target.position());
        } else if (c.locked) {
            boss.faceToward(boss.position().add(flat(c.aim)));
        }
        if (!c.locked && lock > 0 && c.t >= lock) {
            c.locked = true;
            boss.setAim(c.aim);
        }
    }

    private static Vec3 dir(Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        return d.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : d.normalize();
    }

    /**
     * The way his body faces the one he aims at (the gauntlet's hand is a step to the side, so the aim from it points a
     * little across): the flat direction from him to the target, or the aim's if there is none.
     */
    private static Vec3 bodyAim(ThanosEntity boss, MoveState c) {
        LivingEntity target = c.target;
        return target != null ? flat(target.position().subtract(boss.position())) : flat(c.aim);
    }

    private static Vec3 flat(Vec3 v) {
        Vec3 f = new Vec3(v.x, 0, v.z);
        return f.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : f.normalize();
    }

    private static Vec3 chestOf(LivingEntity e) {
        return e.position().add(0, e.getBbHeight() * 0.6, 0);
    }

    /** The ground under a point: the first solid block below it, within 40 blocks. */
    private static Vec3 groundBelow(ServerLevel level, LivingEntity from, Vec3 p) {
        BlockHitResult hit = level.clip(new ClipContext(p.add(0, 0.5, 0), p.add(0, -40, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, from));
        return hit.getType() == HitResult.Type.MISS ? p : hit.getLocation();
    }

    /** The creatures a multi-target move picks: players in sight and in range, spread over different ones. */
    private static List<LivingEntity> pickTargets(ThanosEntity boss, int count, double range, boolean needSight) {
        List<ServerPlayer> pool = new ArrayList<>();
        for (ServerPlayer player : boss.mortals()) {
            if (boss.distanceTo(player) <= range && (!needSight || boss.hasLineOfSight(player))) {
                pool.add(player);
            }
        }
        pool.sort(Comparator.comparingDouble(boss::distanceToSqr));
        // The current target first, then the others in turn, so that two moves in a row pick different people.
        LivingEntity current = boss.target();
        List<LivingEntity> picked = new ArrayList<>();
        if (current instanceof ServerPlayer player && pool.remove(player)) {
            picked.add(player);
        }
        while (picked.size() < count && !pool.isEmpty()) {
            picked.add(pool.remove(boss.getRandom().nextInt(pool.size())));
        }
        return picked;
    }

    // =========================================================================================================
    // The sword and the fist

    /** A blow of his lands on {@code victim}: a Chaos Shield stops it (and pays double for the sword), the rest is hurt. */
    static boolean blow(ThanosEntity boss, LivingEntity victim, ResourceKey<DamageType> type, float damage, double push, boolean sword) {
        ServerLevel level = (ServerLevel) boss.level();
        float scaled = damage * boss.damageScale();
        if (victim.hasEffect(ModEffects.TIME_LOCKED)) {
            scaled *= 1.25F;
        }
        if (victim instanceof ServerPlayer wanda && WandaAbilities.hasShieldUp(wanda)) {
            WandaAbilities.blockMelee(wanda, boss, scaled * (sword ? ThanosBossConfig.SWORD_SHIELD_FACTOR.get().floatValue() : 1.0F));
            return false;
        }
        if (sword) {
            StonePowers.disableShield(victim, 100);
        }
        if (victim.isPassenger()) {
            victim.stopRiding();
        }
        boolean hurt = StonePowers.hurt(level, boss, victim, type, scaled, boss);
        if (hurt) {
            StonePowers.shove(victim, boss.position(), push, 0.2);
            ThanosSounds.HIT.play(level, victim.position());
        }
        return hurt;
    }

    /** Everything he may hurt within a cone ahead of him takes the blow. */
    private static int cone(ThanosEntity boss, Vec3 facing, double reach, double halfAngle, float damage, ResourceKey<DamageType> type,
                            double push, boolean sword) {
        ServerLevel level = (ServerLevel) boss.level();
        Vec3 direction = flat(facing);
        double cos = Math.cos(Math.toRadians(halfAngle));
        int hits = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, boss.getBoundingBox().inflate(reach + 1.5),
                e -> e != boss && e.isAlive() && !e.isSpectator() && com.kolodkingal.enhanced.ability.AreaTargets.mayHurt(boss, e, false))) {
            Vec3 to = new Vec3(e.getX() - boss.getX(), 0, e.getZ() - boss.getZ());
            double distance = to.length();
            if (distance - e.getBbWidth() * 0.5 > reach || Math.abs(e.getY() - boss.getY()) > 3.5) {
                continue;
            }
            if (distance > 1.0E-4 && flat(to).dot(direction) < cos && distance > e.getBbWidth()) {
                continue;
            }
            if (blow(boss, e, type, damage, push, sword)) {
                hits++;
            }
        }
        return hits;
    }

    private static void sweep(ThanosEntity boss, MoveState c, ServerLevel level, int t, int fire) {
        if (t == 6) {
            ThanosEffects.ringTell(level, boss.position(), 5.5F, 10, GOLD);
        }
        if (t == 12) {
            level.sendParticles(ParticleTypes.CRIT, boss.swordHandPosition().x, boss.swordHandPosition().y + 0.8, boss.swordHandPosition().z, 6, 0.1, 0.4, 0.1, 0.02);
        }
        if (t == fire) {
            cone(boss, c.aim, 5.5, 80.0, ThanosBossConfig.SWORD_DAMAGE.get().floatValue(), ModDamageTypes.THANOS_SWORD, 1.2, true);
            ThanosSounds.SWING.play(level, boss.position());
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, boss.getX() + flat(c.aim).x * 2.5, boss.getY() + 1.5, boss.getZ() + flat(c.aim).z * 2.5, 1, 0, 0, 0, 0);
        }
    }

    private static void smash(ThanosEntity boss, MoveState c, ServerLevel level, int t, int fire) {
        Vec3 ahead = c.locked ? flat(c.aim) : bodyAim(boss, c);
        if (t <= 16 && !c.locked) {
            c.spot = boss.position().add(ahead.scale(4.0));
        }
        if (t == 8) {
            ThanosEffects.ringTell(level, c.spot, 3.5F, 16, GOLD);
        }
        if (t == 14) {
            boss.setDeltaMovement(ahead.x * 0.35, 0.5, ahead.z * 0.35);
            boss.hurtMarked = true;
        }
        if (t == fire) {
            ThanosSounds.HIT.play(level, c.spot, 1.4F, 0.7F);
            level.sendParticles(ParticleTypes.EXPLOSION, c.spot.x, c.spot.y + 0.3, c.spot.z, 3, 1.0, 0.2, 1.0, 0.0);
            ThanosEffects.shockRing(level, c.spot, 6.0F, 8, GOLD);
            for (LivingEntity e : StonePowers.around(level, boss, c.spot, 3.5, e -> true)) {
                if (blow(boss, e, ModDamageTypes.THANOS_SWORD, ThanosBossConfig.SMASH_DAMAGE.get().floatValue(), 0.4, true)) {
                    Vec3 motion = e.getDeltaMovement();
                    e.setDeltaMovement(motion.x, 0.6, motion.z);
                    e.hurtMarked = true;
                }
            }
        }
        int ring = t - fire;
        if (ring >= 1 && ring <= 8) {
            double from = 3.5 + (ring - 1) * (2.5 / 8.0);
            double to = 3.5 + ring * (2.5 / 8.0);
            StonePowers.ring(level, boss, c.spot, from, to, ThanosBossConfig.SMASH_RING_DAMAGE.get().floatValue() * boss.damageScale(),
                    ModDamageTypes.STONE_BLAST, 0.6, 0.3, c.hit);
        }
    }

    private static void jab(ThanosEntity boss, MoveState c, ServerLevel level, int t, int fire) {
        if (t == 2) {
            level.sendParticles(new EnergyParticleOptions(StoneKind.POWER.color(), 1.0F), boss.handPosition().x, boss.handPosition().y, boss.handPosition().z, 4, 0.1, 0.1, 0.1, 0.02);
        }
        if (t == fire || t == fire + 8) {
            cone(boss, c.target != null ? dir(boss.position(), c.target.position()) : c.aim, 4.0, 25.0,
                    ThanosBossConfig.JAB_DAMAGE.get().floatValue(), ModDamageTypes.THANOS_BLOW, 0.8, false);
            ThanosSounds.SWING.play(level, boss.position(), 0.8F, 1.3F);
        }
    }

    private static void swordThrow(ThanosEntity boss, MoveState c, ServerLevel level, int t, int fire) {
        if (t == 4) {
            ThanosSounds.SWING.play(level, boss.position(), 0.8F, 0.8F);
        }
        if (t == fire && !boss.swordAway()) {
            ThanosSwordEntity sword = new ThanosSwordEntity(ModEntities.THANOS_SWORD.get(), level);
            // The blade leaves the other hand: it flies at the point of the locked line nearest the target, so it still goes for him.
            Vec3 from = boss.swordHandPosition();
            Vec3 aim = c.aim;
            if (c.target != null) {
                Vec3 hand = boss.handPosition();
                double along = Math.max(4.0, chestOf(c.target).subtract(hand).dot(c.aim));
                aim = dir(from, hand.add(c.aim.scale(along)));
            }
            sword.launch(boss, from, aim);
            boss.fight().spawn(sword);
            boss.swordThrown(sword);
            ThanosSounds.SWING.play(level, boss.position(), 1.2F, 0.7F);
        }
    }

    private static void choke(ThanosEntity boss, MoveState c, ServerLevel level, int t, int fire) {
        int lock = Math.min(ThanosMove.CHOKE.lock(), c.tell);
        if (t == lock) {
            c.grabbed = chokeVictim(boss, c);
        }
        if (t < fire) {
            return;
        }
        int held = t - fire;
        if (held == 0) {
            LivingEntity victim = c.grabbed;
            if (victim == null || !victim.isAlive() || boss.distanceTo(victim) > 4.2 || !(victim instanceof ServerPlayer player) || !StonePowers.graspable(player)) {
                c.grabbed = null;
                boss.endMove();
                return;
            }
            ThanosSounds.HIT.play(level, victim.position(), 1.0F, 0.8F);
            if (Powers.is(player, Power.LOKI)) {
                player.displayClientMessage(Component.translatable("message.enhanced.thanos.loki").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE), true);
            }
        }
        LivingEntity victim = c.grabbed;
        if (victim == null) {
            return;
        }
        if (!victim.isAlive() || c.taken >= 60.0F) {
            // Another fighter hit him hard enough to make him let go.
            c.grabbed = null;
            if (victim.isAlive()) {
                victim.setDeltaMovement(0, -0.2, 0);
                victim.hurtMarked = true;
            }
            if (c.taken >= 60.0F) {
                boss.stagger();
            }
            return;
        }
        if (held <= 40) {
            Vec3 forward = boss.forward();
            Vec3 hold = boss.position().add(forward.scale(1.4)).add(0, 3.2, 0);
            Vec3 steer = hold.subtract(victim.position().add(0, victim.getBbHeight(), 0));
            Vec3 velocity = steer.scale(0.4);
            double speed = velocity.length();
            victim.setDeltaMovement(speed > 1.4 ? velocity.scale(1.4 / speed) : velocity);
            victim.hurtMarked = true;
            victim.fallDistance = 0.0F;
            if (victim instanceof ServerPlayer player) {
                player.connection.aboveGroundTickCount = 0;
            }
            if (held > 0 && held % 10 == 0) {
                blow(boss, victim, ModDamageTypes.THANOS_BLOW, ThanosBossConfig.CHOKE_DAMAGE.get().floatValue() / 4.0F, 0.0, false);
            }
            if (held == 40) {
                Vec3 away = boss.forward();
                victim.setDeltaMovement(away.x * 1.5, 0.5, away.z * 1.5);
                victim.hurtMarked = true;
                ThanosSounds.SWING.play(level, boss.position(), 1.2F, 0.8F);
                c.phase = 1;
            }
        } else if (c.phase == 1 && (victim.onGround() && held > 44 || held >= 48)) {
            c.phase = 2;
            blow(boss, victim, ModDamageTypes.THANOS_BLOW, ThanosBossConfig.CHOKE_DAMAGE.get().floatValue() / 2.0F, 0.0, false);
            c.grabbed = null;
        }
    }

    /** Whom the Choke takes: a player in the cone ahead within 3.2 blocks, a Loki or a Vision before the others. */
    @Nullable
    private static LivingEntity chokeVictim(ThanosEntity boss, MoveState c) {
        Vec3 direction = bodyAim(boss, c);
        double cos = Math.cos(Math.toRadians(20.0));
        ServerPlayer best = null;
        for (ServerPlayer player : boss.mortals()) {
            Vec3 to = new Vec3(player.getX() - boss.getX(), 0, player.getZ() - boss.getZ());
            double distance = to.length();
            if (distance > 3.2 || !StonePowers.graspable(player) || distance > 1.0E-4 && flat(to).dot(direction) < cos) {
                continue;
            }
            boolean preferred = Powers.is(player, Power.LOKI) || Powers.is(player, Power.VISION);
            if (best == null || preferred && !(Powers.is(best, Power.LOKI) || Powers.is(best, Power.VISION))) {
                best = player;
            }
        }
        return best;
    }

    private static void leap(ThanosEntity boss, MoveState c, ServerLevel level, int t, int fire) {
        if (t == 6) {
            ThanosEffects.ringParticles(level, boss.position(), 3.0F, GOLD, 18, 0.1);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, boss.getX(), boss.getY() + 0.2, boss.getZ(), 6, 1.2, 0.05, 1.2, 0.01);
        }
        if (t <= Math.min(ThanosMove.LEAP.lock(), c.tell) && c.target != null) {
            Vec3 to = c.target.position().subtract(boss.position());
            double distance = Math.min(18.0, Math.hypot(to.x, to.z));
            c.spot = boss.position().add(flat(to).scale(distance));
        }
        if (t == Math.min(ThanosMove.LEAP.lock(), c.tell) && c.target != null) {
            // His aim is fixed: the ring of the landing shows from now until he comes down (the flight takes about a
            // second).
            ThanosEffects.ringTell(level, c.spot, 4.0F, c.tell - t + LEAP_FLIGHT_TICKS, GOLD);
        }
        if (t == fire) {
            Vec3 to = c.spot.subtract(boss.position());
            Vec3 horizontal = new Vec3(to.x, 0, to.z);
            double speed = Math.min(1.0, horizontal.length() / 20.0);
            Vec3 velocity = horizontal.lengthSqr() < 1.0E-4 ? Vec3.ZERO : horizontal.normalize().scale(speed);
            c.aim = velocity;
            boss.setDeltaMovement(velocity.x, 0.9, velocity.z);
            boss.hurtMarked = true;
            ThanosSounds.STEP.play(level, boss.position(), 1.4F, 0.7F);
            c.phase = 1;
        }
        if (c.phase == 1 && t > fire) {
            Vec3 motion = boss.getDeltaMovement();
            double remaining = Math.hypot(c.spot.x - boss.getX(), c.spot.z - boss.getZ());
            Vec3 velocity = remaining < 0.6 ? Vec3.ZERO : c.aim;
            boss.setDeltaMovement(velocity.x, motion.y, velocity.z);
            if (boss.onGround() && t - fire >= 4) {
                c.phase = 2;
                ThanosEffects.shockRing(level, boss.position(), 4.0F, 6, GOLD);
                ThanosSounds.HIT.play(level, boss.position(), 1.4F, 0.6F);
                StonePowers.splash(level, boss, boss.position(), 4.0, ThanosBossConfig.LEAP_DAMAGE.get().floatValue() * boss.damageScale(),
                        ModDamageTypes.STONE_BLAST, 0.8, 0.3, boss, true);
            }
        }
    }

    // =========================================================================================================
    // The Power Stone

    private static void powerBlast(ThanosEntity boss, MoveState c, ServerLevel level, int t, int fire) {
        int color = StoneKind.POWER.color();
        Vec3 hand = boss.handPosition();
        if (t == 8) {
            ThanosEffects.lineTell(level, boss, hand, hand.add(c.aim.scale(30.0)), 6, color, false);
        }
        if (t == Math.min(ThanosMove.POWER_BLAST.lock(), c.tell)) {
            ThanosEffects.lineTell(level, boss, hand, hand.add(c.aim.scale(30.0)), fire - t, StoneKind.POWER.core(), false);
        }
        if (t < fire) {
            return;
        }
        int k = t - fire;
        if (k >= 6) {
            return;
        }
        if (k == 0) {
            ThanosSounds.POWER_BLAST.play(level, boss.position());
            StoneKind.POWER.voice().use(level, boss.position(), 0.6F, 1.0F);
        }
        StonePowers.BeamHit beam = StonePowers.beam(level, boss, hand, c.aim, 30.0, 1.2,
                ThanosBossConfig.POWER_BLAST_DAMAGE.get().floatValue() * boss.damageScale(), ModDamageTypes.STONE_POWER, 20.0F, 2.0, c.hit, true);
        c.spot = beam.end();
        c.scalar = beam.shield() ? 1.0 : 0.0;
        ThanosEffects.beam(level, boss, hand, beam.end(), 1.2F, 2, color);
        if (k == 5 && !beam.shield()) {
            // The blast at the end of the beam: the ones it did not already hit.
            for (LivingEntity e : StonePowers.around(level, boss, beam.end(), 3.0, e -> !c.hit.contains(e.getId()))) {
                if (StonePowers.hurt(level, boss, e, ModDamageTypes.STONE_BLAST, ThanosBossConfig.POWER_BLAST_SPLASH.get().floatValue() * boss.damageScale(), boss)) {
                    StonePowers.shove(e, beam.end(), 0.8, 0.3);
                }
            }
            ThanosEffects.shockRing(level, beam.end(), 3.0F, 6, color);
            level.sendParticles(ParticleTypes.EXPLOSION, beam.end().x, beam.end().y, beam.end().z, 1, 0, 0, 0, 0);
        }
    }

    private static void overload(ThanosEntity boss, MoveState c, ServerLevel level, int t, int fire) {
        int lock = Math.min(ThanosMove.OVERLOAD.lock(), c.tell);
        int color = StoneKind.POWER.color();
        if (t < lock && t % 4 == 0) {
            for (LivingEntity target : c.targets) {
                if (target.isAlive()) {
                    ThanosEffects.ringParticles(level, groundBelow(level, boss, target.position()), 3.0F, color, 12, 0.1);
                }
            }
        }
        if (t == lock) {
            c.spots.clear();
            for (LivingEntity target : c.targets) {
                if (target.isAlive()) {
                    Vec3 spot = groundBelow(level, boss, target.position());
                    c.spots.add(spot);
                    ThanosEffects.ringTell(level, spot, 3.0F, fire - lock, color);
                }
            }
        }
        if (t == fire) {
            for (Vec3 spot : c.spots) {
                StonePowers.column(level, boss, spot, 3.0, 1.0, 9.0, ThanosBossConfig.OVERLOAD_DAMAGE.get().floatValue() * boss.damageScale(),
                        ModDamageTypes.STONE_BLAST, 0.6, 0.8, boss);
                ThanosEffects.shockRing(level, spot, 3.0F, 6, color);
                level.sendParticles(ParticleTypes.EXPLOSION, spot.x, spot.y + 0.5, spot.z, 2, 1.0, 0.3, 1.0, 0.0);
                StonePowers.craterIfAllowed(level, boss, spot, 2.0F);
            }
            ThanosSounds.POWER_BLAST.play(level, boss.position(), 0.9F, 0.9F);
        }
    }

    private static void shockwave(ThanosEntity boss, MoveState c, ServerLevel level, int t, int fire) {
        if (t == 8) {
            level.sendParticles(new EnergyParticleOptions(StoneKind.POWER.color(), 1.4F), boss.handPosition().x, boss.handPosition().y + 0.6, boss.handPosition().z, 8, 0.2, 0.2, 0.2, 0.02);
        }
        if (t == fire) {
            ThanosEffects.shockRing(level, boss.position(), 9.0F, 10, StoneKind.POWER.color());
            ThanosSounds.HIT.play(level, boss.position(), 1.4F, 0.6F);
            boss.setDeltaMovement(0, 0, 0);
        }
        int k = t - fire;
        if (k >= 1 && k <= 10) {
            StonePowers.ring(level, boss, boss.position(), (k - 1) * 0.9, k * 0.9, ThanosBossConfig.SHOCKWAVE_DAMAGE.get().floatValue() * boss.damageScale(),
                    ModDamageTypes.STONE_BLAST, 1.0, 0.3, c.hit);
        }
    }

    private static void powerPulse(ThanosEntity boss, MoveState c, ServerLevel level, int t, int fire) {
        LivingEntity target = c.target;
        if (target == null || !target.isAlive()) {
            boss.endMove();
            return;
        }
        if (t % 8 == 1 && t < fire) {
            ThanosEffects.lineTell(level, boss, boss.handPosition(), chestOf(target), 8, StoneKind.POWER.color(), true);
        }
        if (t == fire) {
            Vec3 from = boss.handPosition();
            Vec3 to = chestOf(target);
            WandaAbilities.ShieldStop stop = WandaAbilities.shieldAcross(level, boss, from, to, 1.0F);
            if (stop != null && stop.owner() instanceof ServerPlayer wanda) {
                WandaAbilities.wear(wanda, ThanosBossConfig.PULSE_DAMAGE.get().floatValue());
            } else {
                StonePowers.hurt(level, boss, target, ModDamageTypes.STONE_POWER, ThanosBossConfig.PULSE_DAMAGE.get().floatValue() * boss.damageScale(), boss);
            }
            ThanosEffects.beam(level, boss, from, stop != null ? stop.point() : to, 0.3F, 3, StoneKind.POWER.color());
            ThanosSounds.POWER_BLAST.play(level, boss.position(), 0.6F, 1.4F);
            if (target instanceof ServerPlayer player) {
                boss.pulsed(player);
            }
        }
    }

    // =========================================================================================================
    // The Space Stone

    /** Where a Portal Step would put him: right behind the target, if there is room there and no Hex wall between. */
    @Nullable
    private static Vec3 stepSpot(ThanosEntity boss, LivingEntity target) {
        ServerLevel level = (ServerLevel) boss.level();
        Vec3 spot = Teleports.behind(boss, target);
        if (spot == null) {
            Vec3 toward = flat(boss.position().subtract(target.position()));
            for (double distance : new double[]{2.5, 3.5, 4.5}) {
                Vec3 candidate = target.position().add(toward.scale(distance));
                if (Teleports.fits(boss, candidate)) {
                    spot = candidate;
                    break;
                }
            }
        }
        if (spot == null || Teleports.crossesHex(level, boss, spot)) {
            return null;
        }
        return spot;
    }

    private static void portalStep(ThanosEntity boss, MoveState c, ServerLevel level, int t, int fire) {
        if (t == fire) {
            LivingEntity target = c.target;
            Vec3 spot = target == null ? null : stepSpot(boss, target);
            if (spot == null || !boss.mayTeleportTo(level, spot)) {
                // The wall refuses the step: he walks instead.
                boss.endMove();
                return;
            }
            c.spot = spot;
            Vec3 from = boss.position();
            ThanosEffects.portal(level, from.add(0, 0.2, 0), 1.6F, 20);
            ThanosEffects.portal(level, spot.add(0, 0.2, 0), 1.6F, 20);
            boss.moveTo(spot.x, spot.y, spot.z, boss.getYRot(), boss.getXRot());
            boss.getNavigation().stop();
            boss.faceToward(target.position());
            ThanosSounds.PORTAL.play(level, from);
            ThanosSounds.PORTAL.play(level, spot, 1.0F, 1.2F);
            c.scalar = fire + 6;
        }
        if (c.scalar > 0 && t == (int) c.scalar) {
            // A Jab or a Sweep follows at once, its wind-up cut short.
            c.chain = boss.swordAway() || boss.getRandom().nextBoolean() ? ThanosMove.GAUNTLET_JAB : ThanosMove.SWORD_SWEEP;
            boss.endMove();
        }
    }

    private static void meteorHail(ThanosEntity boss, MoveState c, ServerLevel level, int t, int fire) {
        int lock = Math.min(ThanosMove.METEOR_HAIL.lock(), c.tell);
        if (t == 1) {
            ThanosEffects.portal(level, boss.position().add(0, 6.0, 0), 2.0F, 20);
        }
        if (t == lock) {
            c.spots.clear();
            List<ServerPlayer> mortals = boss.mortals();
            for (ServerPlayer player : mortals) {
                if (c.spots.size() < 10) {
                    c.spots.add(groundBelow(level, boss, player.position()));
                }
            }
            RandomSource random = boss.getRandom();
            int extra = ThanosBossConfig.METEOR_EXTRA.get();
            for (int i = 0; i < extra && c.spots.size() < 10 && !mortals.isEmpty(); i++) {
                ServerPlayer around = mortals.get(random.nextInt(mortals.size()));
                Vec3 near = around.position().add((random.nextDouble() - 0.5) * 12.0, 0, (random.nextDouble() - 0.5) * 12.0);
                c.spots.add(groundBelow(level, boss, near));
            }
            for (Vec3 spot : c.spots) {
                ThanosEffects.ringTell(level, spot, (float) ThanosMeteorEntity.RADIUS, 24, StoneKind.SPACE.color());
                ThanosEffects.portal(level, spot.add(0, ThanosMeteorEntity.HEIGHT, 0), 1.6F, 30);
            }
            ThanosSounds.METEOR_WHISTLE.play(level, boss.position());
        }
        // One meteor every three ticks, each falling for the 24 ticks its ring warns.
        if (t >= lock && !c.spots.isEmpty() && (t - lock) % 3 == 0) {
            int index = (t - lock) / 3;
            if (index < c.spots.size()) {
                ThanosMeteorEntity meteor = new ThanosMeteorEntity(ModEntities.THANOS_METEOR.get(), level);
                meteor.launch(boss, c.spots.get(index));
                boss.fight().spawn(meteor);
                ThanosSounds.METEOR_WHISTLE.play(level, meteor.position(), 0.6F, 1.0F);
            }
        }
    }

    private static void gravityPull(ThanosEntity boss, MoveState c, ServerLevel level, int t, int fire) {
        if (t == 8) {
            ThanosEffects.mandala(level, boss.position().add(boss.forward().scale(1.0)), 2.0F, 16, StoneKind.SPACE.color());
        }
        if (t == fire) {
            c.targets.clear();
            for (ServerPlayer player : boss.mortals()) {
                double distance = boss.distanceTo(player);
                if (distance >= 4.0 && distance <= 22.0 && boss.hasLineOfSight(player) && StonePowers.graspable(player)) {
                    c.targets.add(player);
                }
            }
        }
        int k = t - fire;
        if (k >= 1 && k <= 16) {
            Vec3 center = boss.position().add(0, 1.2, 0);
            for (LivingEntity victim : c.targets) {
                if (!victim.isAlive() || !boss.hasLineOfSight(victim) || !StonePowers.graspable(victim)) {
                    continue;
                }
                Vec3 pull = center.subtract(victim.position().add(0, victim.getBbHeight() * 0.5, 0));
                if (pull.length() < 2.0) {
                    continue;
                }
                victim.setDeltaMovement(pull.normalize().scale(0.55));
                victim.hurtMarked = true;
                victim.fallDistance = 0.0F;
                if (victim instanceof ServerPlayer player) {
                    player.connection.aboveGroundTickCount = 0;
                }
            }
            if (k % 4 == 0) {
                ThanosEffects.ringParticles(level, boss.position().add(0, 1.2, 0), 6.0F - k * 0.25F, StoneKind.SPACE.color(), 12, 0.0);
            }
        }
        if (k == 17) {
            ThanosEffects.shockRing(level, boss.position(), 3.5F, 5, StoneKind.SPACE.color());
            StonePowers.splash(level, boss, boss.position(), 3.5, ThanosBossConfig.PULL_CRUSH_DAMAGE.get().floatValue() * boss.damageScale(),
                    ModDamageTypes.STONE_BLAST, 0.5, 0.4, boss, false);
            ThanosSounds.HIT.play(level, boss.position(), 1.2F, 0.7F);
        }
    }

    // =========================================================================================================
    // The Reality Stone

    private static void phantoms(ThanosEntity boss, MoveState c, ServerLevel level, int t, int fire) {
        if (t == 1) {
            ThanosEffects.wave(level, boss.position(), 12.0F, c.tell, StoneKind.REALITY.color());
        }
        if (t == fire) {
            for (int i = 0; i < 3; i++) {
                double angle = boss.getYRot() * Mth.DEG_TO_RAD + i * (Mth.TWO_PI / 3.0);
                Vec3 at = boss.position().add(Math.cos(angle) * 3.5, 0, Math.sin(angle) * 3.5);
                if (!Teleports.fits(boss, at)) {
                    at = boss.position();
                }
                ThanosPhantomEntity phantom = new ThanosPhantomEntity(ModEntities.THANOS_PHANTOM.get(), level);
                phantom.prepare(boss);
                phantom.moveTo(at.x, at.y, at.z, boss.getYRot(), 0.0F);
                boss.fight().spawn(phantom);
                level.sendParticles(new EnergyParticleOptions(StoneKind.REALITY.color(), 1.4F), at.x, at.y + 1.5, at.z, 14, 0.5, 1.2, 0.5, 0.05);
            }
            boss.hide();
            ThanosSounds.REALITY_POP.play(level, boss.position(), 1.2F, 0.6F);
        }
    }

    private static void petrifyOrb(ThanosEntity boss, MoveState c, ServerLevel level, int t, int fire) {
        Vec3 hand = boss.handPosition();
        if (t % 4 == 2 && t < fire) {
            level.sendParticles(new EnergyParticleOptions(StoneKind.REALITY.color(), 1.4F), hand.x, hand.y + 0.6, hand.z, 3, 0.15, 0.15, 0.15, 0.01);
        }
        if (t == fire) {
            ThanosOrbEntity orb = new ThanosOrbEntity(ModEntities.THANOS_ORB.get(), level);
            orb.launch(boss, hand.add(0, 0.4, 0), c.aim);
            boss.fight().spawn(orb);
            ThanosSounds.SWING.play(level, boss.position(), 1.0F, 1.4F);
        }
    }

    // =========================================================================================================
    // The Soul Stone

    private static void soulSight(ThanosEntity boss, MoveState c, ServerLevel level, int t, int fire) {
        if (t == 1) {
            level.sendParticles(new EnergyParticleOptions(StoneKind.SOUL.color(), 1.4F), boss.getX(), boss.getY() + 2.9, boss.getZ(), 10, 0.3, 0.1, 0.3, 0.02);
        }
        if (t == fire) {
            LokiAbilities.dissolveDuplicates(level, boss.position(), 40.0);
            for (ServerPlayer player : level.players()) {
                if (!player.isAlive() || player.isSpectator() || boss.distanceTo(player) > 40.0) {
                    continue;
                }
                LokiAbilities.unmask(player);
                if (player.isInvisible()) {
                    player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false), boss);
                }
            }
            if (boss.hidden()) {
                boss.reveal();
            }
            ThanosSounds.REALITY_POP.play(level, boss.position(), 1.0F, 0.5F);
            StoneKind.SOUL.voice().use(level, boss.position(), 0.8F, 1.0F);
        }
    }

    private static void soulDrain(ThanosEntity boss, MoveState c, ServerLevel level, int t, int fire) {
        int color = StoneKind.SOUL.color();
        if (t == 6) {
            List<Vec3> chests = new ArrayList<>();
            for (LivingEntity target : c.targets) {
                chests.add(chestOf(target));
            }
            ThanosEffects.chains(level, boss, chests, fire - 6, color);
        }
        int k = t - fire;
        if (k < 1) {
            return;
        }
        if (c.poise >= 60.0F) {
            boss.endMove();
            return;
        }
        if (k % 10 != 0 || k > 80) {
            return;
        }
        List<Vec3> chests = new ArrayList<>();
        float healed = 0.0F;
        for (LivingEntity target : List.copyOf(c.targets)) {
            if (!target.isAlive() || StonePowers.untouchable(target) || boss.distanceTo(target) > 28.0 || !boss.hasLineOfSight(target)) {
                c.targets.remove(target);
                continue;
            }
            WandaAbilities.ShieldStop stop = WandaAbilities.shieldAcross(level, boss, boss.handPosition(), chestOf(target), 1.0F);
            if (stop != null && stop.owner() instanceof ServerPlayer wanda) {
                WandaAbilities.wear(wanda, 12.0F);
                c.targets.remove(target);
                continue;
            }
            float damage = ThanosBossConfig.SOUL_DRAIN_TICK.get().floatValue() * boss.damageScale();
            float before = target.getHealth();
            if (StonePowers.hurt(level, boss, target, ModDamageTypes.STONE_SOUL, damage, boss)) {
                healed += Math.max(0.0F, before - target.getHealth()) * ThanosBossConfig.DRAIN_HEAL_FRACTION.get().floatValue();
            }
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 0, false, false), boss);
            chests.add(chestOf(target));
        }
        if (healed > 0.0F) {
            boss.healPoints(healed);
        }
        if (c.targets.isEmpty()) {
            boss.endMove();
            return;
        }
        ThanosEffects.chains(level, boss, chests, 12, color);
        ThanosSounds.SOUL_DRAIN.play(level, boss.position(), 0.8F, 1.0F);
    }

    private static void soulworld(ThanosEntity boss, MoveState c, ServerLevel level, int t, int fire) {
        int color = StoneKind.SOUL.color();
        if (t == 1) {
            c.target = c.target instanceof ServerPlayer ? c.target : pickTargets(boss, 1, 40.0, true).stream().findFirst().orElse(null);
        }
        if (t <= 10 && c.target != null && c.target.isAlive()) {
            c.spot = groundBelow(level, boss, c.target.position());
        }
        if (t == 10 && c.target != null) {
            ThanosEffects.pillar(level, c.spot, 3.0F, fire - 10, color);
        }
        if (t == fire && c.target != null) {
            int taken = 0;
            for (ServerPlayer player : boss.mortals()) {
                if (Math.hypot(player.getX() - c.spot.x, player.getZ() - c.spot.z) <= 3.0 && StonePowers.graspable(player)
                        && player.addEffect(new MobEffectInstance(ModEffects.SOULWORLD, ThanosBossConfig.SOULWORLD_TICKS.get(), 0, false, false, true), boss)) {
                    taken++;
                    ThanosEffects.soulworld(level, player, ThanosBossConfig.SOULWORLD_TICKS.get());
                }
            }
            if (taken > 0) {
                boss.heal((float) (boss.getMaxHealth() * ThanosBossConfig.SOULWORLD_HEAL.get() * taken));
                ThanosSounds.SOULWORLD.play(level, c.spot);
            }
        }
    }

    // =========================================================================================================
    // The Time Stone

    private static void rewind(ThanosEntity boss, MoveState c, ServerLevel level, int t, int fire) {
        if (t == 1 || t == 15) {
            ThanosEffects.mandala(level, boss.handPosition().add(0, 0.5, 0), 1.2F + t * 0.04F, c.tell, StoneKind.TIME.color());
        }
        if (t != fire) {
            return;
        }
        ThanosEntity.Snapshot past = boss.rewindSnapshot();
        if (past == null) {
            return;
        }
        float cap = (float) (boss.getMaxHealth() * ThanosBossConfig.REWIND_MAX_HEAL.get());
        if (past.health() > boss.getHealth()) {
            boss.setHealth(Math.min(past.health(), boss.getHealth() + cap));
        }
        boss.restorePlates(past.plates());
        if (!past.swordAway() && boss.swordAway()) {
            boss.recallSword();
        }
        Vec3 from = boss.position();
        if (from.distanceToSqr(past.position()) > 1.0 && Teleports.fits(boss, past.position()) && boss.mayTeleportTo(level, past.position())) {
            ThanosEffects.mandala(level, from, 2.4F, 30, StoneKind.TIME.color());
            boss.moveTo(past.position().x, past.position().y, past.position().z, boss.getYRot(), boss.getXRot());
            boss.getNavigation().stop();
            ThanosEffects.mandala(level, past.position(), 2.4F, 30, StoneKind.TIME.color());
        }
        boss.forgetHistory();
        ThanosSounds.REWIND.play(level, boss.position());
        StoneKind.TIME.voice().use(level, boss.position(), 0.9F, 1.0F);
    }

    private static void timeSnare(ThanosEntity boss, MoveState c, ServerLevel level, int t, int fire) {
        int lock = Math.min(ThanosMove.TIME_SNARE.lock(), c.tell);
        int color = StoneKind.TIME.color();
        if (t == lock) {
            c.spots.clear();
            for (LivingEntity target : c.targets) {
                if (target.isAlive()) {
                    Vec3 spot = groundBelow(level, boss, target.position());
                    c.spots.add(spot);
                    ThanosEffects.mandala(level, spot, 2.5F, fire - lock, color);
                }
            }
        }
        if (t == fire) {
            for (Vec3 spot : c.spots) {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(spot, spot).inflate(2.5, 3.0, 2.5),
                        e -> e != boss && e.isAlive() && !e.isSpectator() && Math.hypot(e.getX() - spot.x, e.getZ() - spot.z) <= 2.5
                                && com.kolodkingal.enhanced.ability.AreaTargets.mayHurt(boss, e, false))) {
                    e.addEffect(new MobEffectInstance(ModEffects.TIME_LOCKED, ThanosBossConfig.SNARE_TICKS.get(), 0, false, true, true), boss);
                }
                level.sendParticles(new EnergyParticleOptions(color, 1.4F), spot.x, spot.y + 1.0, spot.z, 18, 1.2, 0.8, 1.2, 0.02);
            }
            ThanosSounds.REWIND.play(level, boss.position(), 0.8F, 1.3F);
        }
    }

    private static void infinityBeam(ThanosEntity boss, MoveState c, ServerLevel level, int t, int fire) {
        Vec3 hand = boss.handPosition();
        if (t >= 1 && t <= 33 && (t - 1) % 8 == 0) {
            StoneKind stone = StoneKind.nth((t - 1) / 8 + 1);
            if (stone != null) {
                stone.voice().use(level, boss.position(), 0.4F, 1.0F);
                ThanosSounds.INFINITY_CHARGE.play(level, boss.position(), 1.0F, 0.8F + 0.1F * ((t - 1) / 8));
                level.sendParticles(new EnergyParticleOptions(stone.color(), 1.6F), hand.x, hand.y + 0.3, hand.z, 8, 0.2, 0.2, 0.2, 0.03);
            }
        }
        int lock = Math.min(ThanosMove.INFINITY_BEAM.lock(), c.tell);
        if (t == 10 || t == lock) {
            Vec3 left = rotate(c.aim, 30.0);
            Vec3 right = rotate(c.aim, -30.0);
            ThanosEffects.lineTell(level, boss, hand, hand.add(left.scale(40.0)), c.tell - t, 0xFFFFFF, false);
            ThanosEffects.lineTell(level, boss, hand, hand.add(right.scale(40.0)), c.tell - t, 0xFFFFFF, false);
        }
        int k = t - fire;
        if (k < 0 || k >= 24) {
            return;
        }
        if (k == 0) {
            ThanosSounds.INFINITY_BEAM.play(level, boss.position());
        }
        // The beam sweeps across the fan, one way or the other turn by turn.
        double start = boss.infinityDirection() ? -30.0 : 30.0;
        Vec3 direction = rotate(c.aim, start * (1.0 - 2.0 * k / 23.0));
        StonePowers.BeamHit beam = StonePowers.beam(level, boss, hand, direction, 40.0, 3.0,
                ThanosBossConfig.INFINITY_BEAM_DAMAGE.get().floatValue() * boss.damageScale(), ModDamageTypes.STONE_POWER, 40.0F, 3.0, c.hit, true);
        ThanosEffects.beam(level, boss, hand, beam.end(), 3.0F, 2, -1);
        if (k == 23) {
            boss.flipInfinityDirection();
        }
    }

    /** A direction turned about the vertical by {@code degrees}. */
    private static Vec3 rotate(Vec3 direction, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(direction.x * cos - direction.z * sin, direction.y, direction.x * sin + direction.z * cos).normalize();
    }

    private ThanosMoves() {
    }
}
