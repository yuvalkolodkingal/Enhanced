package com.kolodkingal.enhanced.ironman.nano;

import com.kolodkingal.enhanced.ability.AreaTargets;
import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.config.IronManConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.ironman.IronMissileEntity;
import com.kolodkingal.enhanced.ironman.MissileGuidance;
import com.kolodkingal.enhanced.ironman.SuitEnergy;
import com.kolodkingal.enhanced.network.IronFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * N on the Mark 85: the <b>Homing Mini-Missiles</b> (3.8, lane L4, PLAN 6.2; the user's "tiny missiles that home onto
 * the enemy for a limited time (10 seconds) and make a small boom"). {@code missileCount} 6 micro-missiles fan out of
 * the shoulders and lock on to up to six different hostiles or attackers within {@code missileLockRange} 24 (those in a
 * 90° cone before him first, then the nearest), each turning {@code missileTurnDegrees} 12° a tick towards its own at
 * {@link #SPEED} for up to {@code missileHomingTicks} 200; each hits for {@code missileDamage} 4 and a small boom of
 * {@code missilePower} 0.6 (no block broken). A missile whose target dies picks the nearest other; with fewer targets
 * than missiles they share them (decision: so six missiles at one boss all fly at it); with no lock at all they fly
 * 32 blocks straight and pop. 15 energy, cooldown 120. The flight is {@code IronMissileEntity}'s (lane L3), the turning
 * {@link MissileGuidance}'s.
 */
public final class HomingMissiles {
    /** The missiles' speed (blocks a tick). */
    public static final double SPEED = 1.2;
    /** The cone in which locks are taken first (degrees either side of his look). */
    public static final double LOCK_CONE = 45.0;

    /**
     * Fires the salvo.
     *
     * @return the missiles launched (empty when refused)
     */
    public static List<Entity> fire(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.has(HeroAbility.IRON_MAN_MISSILES) || !data.isReady(HeroAbility.IRON_MAN_MISSILES, now)
                || !SuitEnergy.spend(player, NanoTech.cfg(IronManConfig.MARK85_MISSILE_ENERGY, 15.0))) {
            return List.of();
        }
        data.startCooldown(HeroAbility.IRON_MAN_MISSILES, now, NanoTech.cfg(IronManConfig.MARK85_MISSILE_COOLDOWN,
                120));
        int count = NanoTech.cfg(IronManConfig.MARK85_MISSILE_COUNT, 6);
        List<LivingEntity> locks = locks(player, NanoTech.cfg(IronManConfig.MARK85_MISSILE_LOCK_RANGE, 24.0), count);
        int homingTicks = NanoTech.cfg(IronManConfig.MARK85_MISSILE_HOMING_TICKS, 200);
        double turn = NanoTech.cfg(IronManConfig.MARK85_MISSILE_TURN_DEGREES, 12.0);
        float damage = (float) NanoTech.cfg(IronManConfig.MARK85_MISSILE_DAMAGE, 4.0);
        float power = (float) NanoTech.cfg(IronManConfig.MARK85_MISSILE_POWER, 0.6);
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        List<Entity> missiles = new ArrayList<>();
        List<Vec3> points = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            boolean right = i % 2 == 0;
            // They fan out of the shoulder pods: up and a little to each side, then turn to their targets.
            Vec3 side = Vec3.directionFromRotation(0.0F, player.getYRot() + (right ? 90.0F : -90.0F));
            double spread = 0.25 + 0.12 * (i / 2);
            Vec3 from = player.getEyePosition().add(0.0, -0.15 * player.getScale(), 0.0)
                    .add(side.scale(0.4 * player.getScale())).add(look.scale(0.3));
            Vec3 motion = look.add(side.scale(spread)).add(0.0, 0.35, 0.0).normalize().scale(SPEED);
            LivingEntity target = locks.isEmpty() ? null : locks.get(i % locks.size());
            // Its life: the homing time with a lock, 32 blocks straight without (then it pops with its small boom).
            double range = target == null ? MissileGuidance.UNLOCKED_RANGE : SPEED * (homingTicks - 0.5);
            IronMissileEntity missile = IronMissileEntity.create(level, player, IronMissileEntity.HOMING, from, motion,
                    SPEED, damage, power, range);
            MissileGuidance.guide(missile, player, target, homingTicks, turn, SPEED);
            level.addFreshEntity(missile);
            missiles.add(missile);
            points.add(from);
        }
        for (LivingEntity target : locks) {
            NanoTech.send(level, player, IronFxPayload.LOCK, List.of(target.position()), target.getId());
        }
        NanoTech.send(level, player, IronFxPayload.MISSILES, points, missiles.size());
        NanoTech.sound(level, player.position(), ModSounds.MINI_MISSILE_LAUNCH.get(), 1.0F, 1.2F);
        if (!locks.isEmpty()) {
            NanoTech.sound(level, player.position(), ModSounds.HOMING_LOCK.get(), 0.9F, 1.0F);
        }
        return missiles;
    }

    /**
     * The creatures the salvo locks on to: up to {@code max} different hostiles or attackers within {@code range} he
     * can see, those in the cone before him first (the nearest first), then the nearest others.
     */
    public static List<LivingEntity> locks(ServerPlayer player, double range, int max) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double cos = Math.cos(Math.toRadians(LOCK_CONE));
        List<LivingEntity> cone = new ArrayList<>();
        List<LivingEntity> rest = new ArrayList<>();
        for (LivingEntity e : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range), e -> hostile(player, e))) {
            Vec3 middle = e.position().add(0.0, e.getBbHeight() * 0.5, 0.0);
            if (middle.distanceTo(eye) > range || !Raycasts.inSight(player, middle)) {
                continue;
            }
            (middle.subtract(eye).normalize().dot(look) >= cos ? cone : rest).add(e);
        }
        Comparator<LivingEntity> nearest = Comparator.comparingDouble(player::distanceToSqr);
        cone.sort(nearest);
        rest.sort(nearest);
        List<LivingEntity> locks = new ArrayList<>(cone);
        locks.addAll(rest);
        return locks.size() > max ? new ArrayList<>(locks.subList(0, max)) : locks;
    }

    /** A hostile or an attacker of his he may hurt (never a passive creature, a pet, a player without PvP). */
    static boolean hostile(ServerPlayer player, LivingEntity e) {
        return e != player && e.isAlive() && !e.isSpectator() && AreaTargets.mayHurt(player, e, false);
    }

    /** A guided missile's target is gone: the nearest other hostile within the lock range of the missile, or null. */
    @Nullable
    public static LivingEntity retarget(ServerPlayer owner, Entity missile) {
        double range = NanoTech.cfg(IronManConfig.MARK85_MISSILE_LOCK_RANGE, 24.0);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : missile.level().getEntitiesOfClass(LivingEntity.class,
                missile.getBoundingBox().inflate(range), e -> hostile(owner, e))) {
            double dist = e.distanceToSqr(missile);
            if (dist <= range * range && dist < bestDist) {
                best = e;
                bestDist = dist;
            }
        }
        return best;
    }

    private HomingMissiles() {
    }
}
