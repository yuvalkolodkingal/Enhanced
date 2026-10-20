package com.kolodkingal.enhanced.falcon;

import com.kolodkingal.enhanced.config.FalconConfig;
import com.kolodkingal.enhanced.custom.HeroAbility;
import com.kolodkingal.enhanced.network.FalconFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * B held: the Wing Shield, and Sneak + B: the Wing Slash (3.8, PLAN 8.4). The Wing Shield wraps both wings round the
 * front of him ({@code Live.FALCON_WING_SHIELD}) for as long as B is held, at most {@code shieldMaxTicks} 100: a
 * projectile from the front bounces back off the metal ({@link FalconEvents}), a blow from the front loses {@code
 * shieldMeleeReduction} 70 %, an explosion from the front {@code shieldExplosionReduction} 50 %; in the air he hovers
 * still (his flight's speed is 0 while it is up), on the ground he shuffles. Lowered, it waits {@code shieldCooldown}
 * 60. The Wing Slash: he spins with the wings out, {@code slashDamage} 7 to every creature within {@code slashRadius}
 * 3 ({@code falcon_dive}), knockback {@link #SLASH_KNOCKBACK}; {@code slashCooldown} 80.
 */
public final class WingShield {
    public static final double SLASH_KNOCKBACK = 1.0;
    /** "From the front": the source within this cosine of his facing (a half-circle and a little). */
    static final double FRONT = -0.1;

    /** B down: the wings wrap in front of him. */
    static boolean raise(ServerPlayer player, PowerData data) {
        HeroAbility row = HeroAbility.FALCON_WING_SHIELD;
        long now = player.level().getGameTime();
        if (!data.has(row) || data.is(Live.FALCON_WING_SHIELD) || !data.isReady(row, now)) {
            return false;
        }
        FalconAbilities.State state = FalconAbilities.state(player.getUUID());
        state.shieldStart = now;
        data.set(Live.FALCON_WING_SHIELD, true);
        FalconWings.endGlide(player, true);
        if (!player.onGround() && data.power() == Power.FALCON && player.mayFly() && !player.getAbilities().flying) {
            // (In the air he hovers behind it: the flight's speed is 0 while it is up.)
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
        ServerLevel level = player.serverLevel();
        FalconAbilities.sound(level, player.position(), ModSounds.WING_SHIELD_UP.get(), 1.0F, 1.0F);
        FalconAbilities.send(level, player, FalconFxPayload.SHIELD_ON, List.of(player.position()), 0);
        return true;
    }

    /** B up: the wings open again. */
    static void lower(ServerPlayer player, PowerData data) {
        if (data.is(Live.FALCON_WING_SHIELD)) {
            end(player, data, true);
        }
    }

    /** The shield comes down (with {@code wait}: its cooldown starts). */
    static void end(ServerPlayer player, PowerData data, boolean wait) {
        FalconAbilities.State state = FalconAbilities.peek(player.getUUID());
        if (state != null) {
            state.shieldStart = -1L;
        }
        if (!data.is(Live.FALCON_WING_SHIELD)) {
            return;
        }
        data.set(Live.FALCON_WING_SHIELD, false);
        if (wait) {
            data.startCooldown(HeroAbility.FALCON_WING_SHIELD, player.level().getGameTime(),
                    FalconAbilities.cfg(FalconConfig.SHIELD_COOLDOWN, 60));
        }
        ServerLevel level = player.serverLevel();
        FalconAbilities.sound(level, player.position(), ModSounds.FALCON_WINGS_OPEN.get(), 0.6F, 1.2F);
        FalconAbilities.send(level, player, FalconFxPayload.SHIELD_OFF, List.of(player.position()), 0);
    }

    static void tick(ServerPlayer player, PowerData data, FalconAbilities.State state, long now) {
        if (!data.is(Live.FALCON_WING_SHIELD)) {
            state.shieldStart = -1L;
            return;
        }
        if (state.shieldStart < 0) {
            state.shieldStart = now;
        }
        int max = FalconAbilities.cfg(FalconConfig.SHIELD_MAX_TICKS, 100);
        if (now - state.shieldStart >= max || !FalconWings.hasWings(player, data)) {
            end(player, data, true);
        }
    }

    /** Whether his Wing Shield is up now. */
    public static boolean up(Player player, PowerData data) {
        return data.is(Live.FALCON_WING_SHIELD) && data.has(HeroAbility.FALCON_WING_SHIELD);
    }

    /** Whether something at {@code from} is before him (his facing, flat). */
    public static boolean fromFront(Player player, Vec3 from) {
        Vec3 to = from.subtract(player.position());
        Vec3 flat = new Vec3(to.x, 0, to.z);
        if (flat.lengthSqr() < 1.0E-6) {
            return true; // (right on top of him: the wings cover him)
        }
        Vec3 facing = Vec3.directionFromRotation(0.0F, player.getYRot());
        return flat.normalize().dot(facing) > FRONT;
    }

    /** Sneak + B: the Wing Slash, a spin with the wings out. */
    static boolean slash(ServerPlayer player, PowerData data) {
        HeroAbility row = HeroAbility.FALCON_WING_SLASH;
        long now = player.level().getGameTime();
        if (!FalconAbilities.hasVariant(data, row)) {
            return false;
        }
        HeroAbility slot = FalconAbilities.slotRow(data, row);
        if (!data.isReady(slot, now) || data.is(Live.FALCON_WING_SHIELD)) {
            return false;
        }
        data.startCooldown(slot, now, FalconAbilities.cfg(FalconConfig.SLASH_COOLDOWN, 80));
        ServerLevel level = player.serverLevel();
        double radius = FalconAbilities.cfg(FalconConfig.SLASH_RADIUS, 3.0);
        double damage = FalconAbilities.cfg(FalconConfig.SLASH_DAMAGE, 7.0);
        Vec3 middle = player.getBoundingBox().getCenter();
        AABB round = player.getBoundingBox().inflate(radius);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, round, e -> e != player
                && FalconAbilities.mayHurt(player, e))) {
            Vec3 nearest = nearest(e.getBoundingBox(), middle);
            if (nearest.distanceTo(middle) <= radius) {
                FalconAbilities.hurt(player, e, ModDamageTypes.FALCON_DIVE, damage, player);
                FalconAbilities.push(player, e, player.position(), SLASH_KNOCKBACK);
            }
        }
        FalconAbilities.sound(level, player.position(), ModSounds.WING_SLASH.get(), 1.0F, 1.0F);
        FalconAbilities.send(level, player, FalconFxPayload.SLASH, List.of(middle), (int) Math.round(radius * 10));
        return true;
    }

    private static Vec3 nearest(AABB box, Vec3 p) {
        return new Vec3(Math.max(box.minX, Math.min(p.x, box.maxX)), Math.max(box.minY, Math.min(p.y, box.maxY)),
                Math.max(box.minZ, Math.min(p.z, box.maxZ)));
    }

    private WingShield() {
    }
}
