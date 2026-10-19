package com.kolodkingal.enhanced.deadpool;

import com.kolodkingal.enhanced.ability.Raycasts;
import com.kolodkingal.enhanced.config.DeadpoolConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.DeadpoolFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * G: the pistols (3.7): two chunky silver cartoon pistols with bright tracer rounds (no real-gun realism, PLAN 5.1). A
 * press fires a pair, left then right {@link #PAIR_GAP} ticks later, each a {@link DeadpoolBulletEntity} ({@code
 * pistolDamage} 4, speed {@link #SPEED}, no gravity, {@code pistolRange} 48); a pair every {@code shotGap} ticks; the
 * magazine ({@code magazine} 16 shots) empties into an automatic reload of {@code reloadTicks} (a twirl and a click).
 * The magazine and the reload are the {@link DeadpoolState} attachment (his HUD's "16/16"); his pistols stay drawn for
 * {@link DeadpoolAbilities#AIM_TICKS} after a shot ({@code Live.DEADPOOL_AIMING}). Sneak + G: the Trick Shot, one round
 * that bounces off the first block it meets toward the nearest creature he may hurt within {@code trickShotSeek} of
 * the bounce (8; it takes no round from the magazine), and G's slot waits {@code trickShotCooldown} (the pairs do not).
 */
public final class Pistols {
    /** Ticks from the left shot to the right. */
    public static final int PAIR_GAP = 3;
    /** Blocks a tick. */
    public static final double SPEED = 3.5;

    /** The magazine's size with the server's numbers. */
    public static int magazine() {
        return DeadpoolAbilities.cfg(DeadpoolConfig.MAGAZINE, 16);
    }

    /** G: a pair of shots, if the pistols are loaded and the last pair's gap is over. */
    static void fire(ServerPlayer player, PowerData data) {
        if (!data.has(HeroAbility.DEADPOOL_PISTOLS)) {
            return;
        }
        long now = player.level().getGameTime();
        DeadpoolState ammo = DeadpoolAbilities.data(player);
        if (ammo.reloadAt() > now) {
            player.displayClientMessage(Component.translatable("message.enhanced.deadpool.reloading")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        DeadpoolAbilities.State state = DeadpoolAbilities.state(player.getUUID());
        if (now - state.lastPair < DeadpoolAbilities.cfg(DeadpoolConfig.SHOT_GAP, 6) || state.rightShotAt >= 0L) {
            return;
        }
        state.lastPair = now;
        if (shoot(player, data, state, true, now) && DeadpoolAbilities.data(player).reloadAt() <= now) {
            state.rightShotAt = now + PAIR_GAP;
        }
    }

    /**
     * One round from one hand, if the magazine has one: the round flies, the magazine counts down, the last round
     * starts the reload. Returns whether a round was fired.
     */
    static boolean shoot(ServerPlayer player, PowerData data, DeadpoolAbilities.State state, boolean left, long now) {
        DeadpoolState ammo = DeadpoolAbilities.data(player);
        int rounds = Math.min(ammo.ammo(), magazine());
        if (rounds <= 0) {
            reload(player, now);
            return false;
        }
        ServerLevel level = player.serverLevel();
        Vec3 from = muzzle(player, left);
        int range = DeadpoolAbilities.cfg(DeadpoolConfig.PISTOL_RANGE, 48);
        Vec3 look = aim(player, from, range);
        DeadpoolBulletEntity bullet = DeadpoolBulletEntity.create(level, player, from, look, SPEED,
                (float) DeadpoolAbilities.cfg(DeadpoolConfig.PISTOL_DAMAGE, 4.0), range, false);
        level.addFreshEntity(bullet);
        state.aimUntil = now + DeadpoolAbilities.AIM_TICKS;
        data.set(Live.DEADPOOL_AIMING, true);
        DeadpoolAbilities.setData(player, ammo.withAmmo(rounds - 1));
        DeadpoolAbilities.send(level, player, DeadpoolFxPayload.SHOT, List.of(from, from.add(look.scale(2.0))),
                left ? 0 : 1);
        DeadpoolAbilities.sound(level, from, ModSounds.DEADPOOL_SHOT.get(), 0.8F, left ? 1.0F : 1.08F);
        if (rounds - 1 <= 0) {
            reload(player, now);
        }
        return true;
    }

    /** The magazine is empty: the twirl and the click, {@code reloadTicks} long. */
    static void reload(ServerPlayer player, long now) {
        DeadpoolState ammo = DeadpoolAbilities.data(player);
        if (ammo.reloadAt() > now) {
            return;
        }
        int ticks = DeadpoolAbilities.cfg(DeadpoolConfig.RELOAD_TICKS, 40);
        DeadpoolAbilities.setData(player, ammo.withAmmo(0).withReloadAt(now + Math.max(1, ticks)));
        ServerLevel level = player.serverLevel();
        DeadpoolAbilities.send(level, player, DeadpoolFxPayload.RELOAD, List.of(player.position()), ticks);
        DeadpoolAbilities.sound(level, player.getEyePosition(), ModSounds.DEADPOOL_RELOAD.get(), 0.8F, 1.0F);
    }

    /** Sneak + G: the Trick Shot (G's slot rested). */
    static void trickShot(ServerPlayer player, PowerData data) {
        if (!DeadpoolAbilities.hasVariant(data, HeroAbility.DEADPOOL_TRICK_SHOT)) {
            return;
        }
        long now = player.level().getGameTime();
        HeroAbility slot = DeadpoolAbilities.slotRow(data, HeroAbility.DEADPOOL_TRICK_SHOT);
        if (!data.isReady(slot, now)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 from = muzzle(player, false);
        int range = DeadpoolAbilities.cfg(DeadpoolConfig.PISTOL_RANGE, 48);
        Vec3 look = aim(player, from, range);
        DeadpoolBulletEntity bullet = DeadpoolBulletEntity.create(level, player, from, look, SPEED,
                (float) DeadpoolAbilities.cfg(DeadpoolConfig.TRICK_SHOT_DAMAGE, 8.0), range, true);
        level.addFreshEntity(bullet);
        DeadpoolAbilities.State state = DeadpoolAbilities.state(player.getUUID());
        state.aimUntil = now + DeadpoolAbilities.AIM_TICKS;
        data.set(Live.DEADPOOL_AIMING, true);
        data.startCooldown(slot, now,
                DeadpoolAbilities.cfg(DeadpoolConfig.TRICK_SHOT_COOLDOWN, 80));
        DeadpoolAbilities.send(level, player, DeadpoolFxPayload.SHOT, List.of(from, from.add(look.scale(2.0))), 2);
        DeadpoolAbilities.sound(level, from, ModSounds.DEADPOOL_SHOT.get(), 0.9F, 0.85F);
    }

    static void tick(ServerPlayer player, PowerData data, DeadpoolAbilities.State state, long now) {
        if (state.rightShotAt >= 0L && now >= state.rightShotAt) {
            state.rightShotAt = -1L;
            shoot(player, data, state, false, now);
        }
        DeadpoolState ammo = DeadpoolAbilities.data(player);
        if (ammo.reloadAt() > 0L && now >= ammo.reloadAt()) {
            DeadpoolAbilities.setData(player, ammo.withAmmo(magazine()).withReloadAt(0L));
        } else if (ammo.reloadAt() == 0L && ammo.ammo() > magazine()) {
            DeadpoolAbilities.setData(player, ammo.withAmmo(magazine())); // (the server's magazine got smaller)
        }
        data.set(Live.DEADPOOL_AIMING, now < state.aimUntil);
    }

    /**
     * The way a round from {@code muzzle} flies: to what his crosshair is on (the first creature or block along his
     * look within {@code range}), so both pistols' rounds meet where he aims.
     */
    static Vec3 aim(ServerPlayer player, Vec3 muzzle, double range) {
        HitResult hit = Raycasts.any(player, range, e -> e instanceof LivingEntity && e != player && e.isPickable(),
                1.0F);
        Vec3 at = hit.getLocation();
        Vec3 way = at.subtract(muzzle);
        if (way.lengthSqr() < 1.0 || way.normalize().dot(player.getLookAngle()) < 0.5) {
            return player.getLookAngle();
        }
        return way.normalize();
    }

    /** Where a round leaves his left or right pistol: a little below and to the side of his eyes, ahead of him. */
    static Vec3 muzzle(ServerPlayer player, boolean left) {
        Vec3 look = player.getLookAngle();
        Vec3 side = new Vec3(-look.z, 0.0, look.x);
        if (side.lengthSqr() < 1.0E-6) {
            side = new Vec3(1.0, 0.0, 0.0);
        }
        side = side.normalize().scale(left ? -0.32 : 0.32);
        return player.getEyePosition().add(look.scale(0.6)).add(side).add(0.0, -0.28, 0.0);
    }

    private Pistols() {
    }
}
