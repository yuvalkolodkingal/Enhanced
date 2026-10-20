package com.kolodkingal.enhanced.captain;

import com.kolodkingal.enhanced.config.CaptainConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.CaptainFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * G: the Shield Slam, and with Sneak the Super-Soldier Leap (3.8, PLAN 3.4). The slam is a short hop and the shield
 * driven down: when he lands (or after {@link #MAX_AIR} ticks) a shockwave hurts every creature standing on the ground
 * within {@code slamRadius} and knocks it up; without a shield in hand it is a fist slam at half the damage. The leap
 * is a long running bound with no fall damage at its landing.
 */
public final class ShieldSlam {
    /** The hop of the slam (up), and the leap's push (forward, up). */
    static final double HOP = 0.6;
    static final double LEAP_FORWARD = 1.8;
    static final double LEAP_UP = 0.8;
    /** The slam's shockwave waits this long at least (the hop), and comes at the latest after {@link #MAX_AIR}. */
    static final int MIN_AIR = 4;
    static final int MAX_AIR = 24;
    /** How high the shockwave knocks them. */
    static final double KNOCK_UP = 0.5;
    /** How long the leap spares him the fall's damage at most. */
    static final int LEAP_GUARD = 60;

    /** The slam begins: the hop. Returns whether it began. */
    public static boolean slam(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        CaptainAbilities.State state = CaptainAbilities.state(player.getUUID());
        if (!data.has(HeroAbility.CAPTAIN_SHIELD_SLAM) || !data.isReady(HeroAbility.CAPTAIN_SHIELD_SLAM, now)
                || state.slamStart >= 0) {
            return false;
        }
        state.slamStart = now;
        state.slamShield = ShieldRules.shieldHand(player) != null;
        data.startCooldown(HeroAbility.CAPTAIN_SHIELD_SLAM, now, CaptainAbilities.cfg(CaptainConfig.SLAM_COOLDOWN,
                100));
        Vec3 m = player.getDeltaMovement();
        player.setDeltaMovement(m.x * 0.3, HOP, m.z * 0.3);
        player.hurtMarked = true;
        CaptainAbilities.guardFall(player, MAX_AIR + 10);
        data.set(Live.CAP_SLAMMING, true);
        CaptainAbilities.send(player.serverLevel(), player, CaptainFxPayload.SLAM, List.of(player.position()), 0);
        return true;
    }

    /** The leap: a long bound forward. Returns whether he leapt. */
    public static boolean leap(ServerPlayer player, PowerData data) {
        long now = player.level().getGameTime();
        if (!data.has(HeroAbility.CAPTAIN_SUPER_LEAP) || !data.isReady(HeroAbility.CAPTAIN_SUPER_LEAP, now)) {
            return false;
        }
        CaptainAbilities.State state = CaptainAbilities.state(player.getUUID());
        state.leapStart = now;
        data.startCooldown(HeroAbility.CAPTAIN_SUPER_LEAP, now, CaptainAbilities.cfg(CaptainConfig.LEAP_COOLDOWN, 40));
        Vec3 dir = CaptainAbilities.flat(player.getLookAngle());
        player.setDeltaMovement(dir.x * LEAP_FORWARD, LEAP_UP, dir.z * LEAP_FORWARD);
        player.hurtMarked = true;
        CaptainAbilities.guardFall(player, LEAP_GUARD);
        CaptainAbilities.sound(player.serverLevel(), player.position(), ModSounds.SHIELD_CHARGE.get(), 0.6F, 1.4F);
        CaptainAbilities.send(player.serverLevel(), player, CaptainFxPayload.LEAP, List.of(player.position()), 0);
        return true;
    }

    static void tick(ServerPlayer player, PowerData data, CaptainAbilities.State state, long now) {
        if (state.leapStart >= 0) {
            long t = now - state.leapStart;
            if (t > 2 && (player.onGround() || player.isInWater()) || t > LEAP_GUARD) {
                state.leapStart = -1;
                CaptainAbilities.guardFall(player, 2);
            }
        }
        if (state.slamStart < 0) {
            return;
        }
        long t = now - state.slamStart;
        if (t >= MIN_AIR && (player.onGround() || player.isInWater() || player.getAbilities().flying)
                || t >= MAX_AIR) {
            shockwave(player, data, state);
        } else if (t >= MIN_AIR - 1) {
            // Down hard with the shield.
            Vec3 m = player.getDeltaMovement();
            player.setDeltaMovement(m.x, Math.min(m.y, -0.9), m.z);
            player.hurtMarked = true;
        }
    }

    /** He lands: the shockwave round him. */
    static void shockwave(ServerPlayer player, PowerData data, CaptainAbilities.State state) {
        state.slamStart = -1;
        ServerLevel level = player.serverLevel();
        CaptainAbilities.guardFall(player, 2);
        double radius = CaptainAbilities.cfg(CaptainConfig.SLAM_RADIUS, 4.0);
        float damage = (float) CaptainAbilities.cfg(CaptainConfig.SLAM_DAMAGE, 6.0) * (state.slamShield ? 1.0F
                : 0.5F);
        Vec3 at = player.position();
        List<Vec3> points = new ArrayList<>();
        points.add(at);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius, 2.0,
                radius), e -> CaptainAbilities.mayHurt(player, e) && grounded(e)
                && Math.hypot(e.getX() - at.x, e.getZ() - at.z) <= radius)) {
            if (CaptainAbilities.hurt(player, e, ModDamageTypes.CAP_SLAM, damage, null)) {
                CaptainAbilities.push(e, at, 0.3, KNOCK_UP, player);
                points.add(e.position());
            }
        }
        CaptainAbilities.sound(level, at, ModSounds.SHIELD_SLAM.get(), 1.2F, state.slamShield ? 1.0F : 1.2F);
        CaptainAbilities.send(level, player, CaptainFxPayload.SLAM, points, state.slamShield ? 2 : 1);
    }

    /** Whether the creature stands on the ground (a creature without a mind of its own included). */
    static boolean grounded(LivingEntity e) {
        return e.onGround() || !e.level().noCollision(e, e.getBoundingBox().move(0.0, -0.25, 0.0));
    }

    private ShieldSlam() {
    }
}
