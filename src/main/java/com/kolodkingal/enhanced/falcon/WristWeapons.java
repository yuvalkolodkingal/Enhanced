package com.kolodkingal.enhanced.falcon;

import com.kolodkingal.enhanced.config.FalconConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.FalconFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * R: the Wrist Blasters, and Sneak + R: the Wrist Rocket (3.8, PLAN 8.4). A press fires a pair of bright red energy
 * darts ({@link FalconDartEntity}: {@code dartDamage} 4 each, {@code dartSpeed} 3.0, no gravity, {@code dartRange}
 * 40), the left wrist's at once and the right's {@link #PAIR_GAP} ticks later, both aimed at what his crosshair is on;
 * a new pair waits {@code dartGap} 6 ticks (they are energy, not bullets: no magazine). The Wrist Rocket is one small
 * rocket ({@link FalconRocketEntity}, the class of Iron Man's missiles: {@code rocketDamage} 8 and a small boom that
 * never breaks a block), on R's slot ({@code rocketCooldown} 80). Neither needs the wings.
 */
public final class WristWeapons {
    /** The right wrist fires this many ticks after the left. */
    public static final int PAIR_GAP = 3;
    /** The wrists, sideways from the middle of his chest, and below his eyes (blocks). */
    static final double WRIST_SIDE = 0.38;
    static final double WRIST_DROP = 0.45;
    /** The rocket's speed (blocks a tick). */
    public static final double ROCKET_SPEED = 1.6;

    /** R: a pair of darts (the left now, the right {@link #PAIR_GAP} ticks later). */
    static boolean fire(ServerPlayer player, PowerData data) {
        if (!data.has(HeroAbility.FALCON_BLASTERS)) {
            return false;
        }
        FalconAbilities.State state = FalconAbilities.state(player.getUUID());
        long now = player.level().getGameTime();
        long gap = FalconAbilities.scaled(data, HeroAbility.FALCON_BLASTERS,
                FalconAbilities.cfg(FalconConfig.DART_GAP, 6));
        if (now - state.lastPair < gap) {
            return false;
        }
        state.lastPair = now;
        dart(player, false);
        state.rightDartAt = now + PAIR_GAP;
        return true;
    }

    /** Sneak + R: the Wrist Rocket, from the right wrist. */
    static boolean rocket(ServerPlayer player, PowerData data) {
        HeroAbility row = HeroAbility.FALCON_WRIST_ROCKET;
        long now = player.level().getGameTime();
        if (!FalconAbilities.hasVariant(data, row)) {
            return false;
        }
        HeroAbility slot = FalconAbilities.slotRow(data, row);
        if (!data.isReady(slot, now)) {
            return false;
        }
        data.startCooldown(slot, now, FalconAbilities.cfg(FalconConfig.ROCKET_COOLDOWN, 80));
        ServerLevel level = player.serverLevel();
        Vec3 from = wrist(player, true);
        Vec3 dir = aim(player, from, 64.0);
        FalconRocketEntity rocket = FalconRocketEntity.create(level, player, from, dir, ROCKET_SPEED,
                (float) FalconAbilities.cfg(FalconConfig.ROCKET_DAMAGE, 8.0),
                (float) FalconAbilities.cfg(FalconConfig.ROCKET_POWER, 1.0));
        level.addFreshEntity(rocket);
        FalconAbilities.sound(level, from, ModSounds.FALCON_ROCKET.get(), 1.0F, 1.0F);
        FalconAbilities.send(level, player, FalconFxPayload.ROCKET, List.of(from, from.add(dir)), 0);
        return true;
    }

    /** Every tick: the right-hand dart of a pair, when its moment comes. */
    static void tick(ServerPlayer player, PowerData data, FalconAbilities.State state, long now) {
        if (state.rightDartAt >= 0 && now >= state.rightDartAt) {
            state.rightDartAt = -1L;
            dart(player, true);
        }
    }

    /** One dart from a wrist, aimed where his crosshair is. */
    static void dart(ServerPlayer player, boolean right) {
        ServerLevel level = player.serverLevel();
        double range = FalconAbilities.cfg(FalconConfig.DART_RANGE, 40);
        double speed = FalconAbilities.cfg(FalconConfig.DART_SPEED, 3.0);
        Vec3 from = wrist(player, right);
        Vec3 dir = aim(player, from, range);
        FalconDartEntity dart = FalconDartEntity.create(level, player, from, dir, speed,
                (float) FalconAbilities.cfg(FalconConfig.DART_DAMAGE, 4.0), range);
        level.addFreshEntity(dart);
        FalconAbilities.sound(level, from, ModSounds.FALCON_DART.get(), 0.8F, right ? 1.1F : 1.0F);
        FalconAbilities.send(level, player, FalconFxPayload.BLAST, List.of(from, from.add(dir.scale(range))),
                right ? 1 : 0);
    }

    /** Where a wrist is: before his chest, a little to the side (his right is his main arm's). */
    static Vec3 wrist(ServerPlayer player, boolean right) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 side = new Vec3(-flat.z, 0, flat.x); // (his right)
        return player.getEyePosition().add(0, -WRIST_DROP, 0).add(flat.scale(0.45))
                .add(side.scale(right ? WRIST_SIDE : -WRIST_SIDE));
    }

    /**
     * The way from {@code from} to what his crosshair is on within {@code range} (a creature, a block, or that far).
     */
    static Vec3 aim(ServerPlayer player, Vec3 from, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        BlockHitResult block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        Vec3 point = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        EntityHitResult hit = com.kolodkingal.enhanced.ability.Raycasts.entityBetween(player.level(), player, eye,
                point, 0.3, e -> e instanceof net.minecraft.world.entity.LivingEntity living && living.isAlive()
                        && !(e instanceof RedwingEntity));
        if (hit != null) {
            point = hit.getEntity().getBoundingBox().getCenter();
        }
        Vec3 dir = point.subtract(from);
        return dir.lengthSqr() < 1.0E-4 ? look : dir.normalize();
    }

    private WristWeapons() {
    }
}
