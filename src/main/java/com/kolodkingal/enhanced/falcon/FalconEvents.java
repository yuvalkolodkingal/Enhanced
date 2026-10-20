package com.kolodkingal.enhanced.falcon;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.network.FalconFxPayload;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

/**
 * Falcon's event hooks (3.8, PLAN 8.3-8.4), at the priorities of {@code docs/HEROES-AND-BOSSES.md}:
 * <ul>
 * <li>{@code LivingIncomingDamageEvent}, HIGH: the wings spare him a fall and a wall he glides into (Falcon, or his
 * body's card, with wings); his dive and climb spare him their falls.</li>
 * <li>{@code LivingIncomingDamageEvent}, NORMAL: behind the Wing Shield a blow from the front loses {@code
 * shieldMeleeReduction}, an explosion from the front {@code shieldExplosionReduction}; a projectile that got through
 * from the front does nothing.</li>
 * <li>{@code ProjectileImpactEvent}, NORMAL: a projectile at the front of the Wing Shield bounces back the way it came,
 * at full speed, his now.</li> </ul>
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class FalconEvents {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFallOrWall(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DamageSource source = event.getSource();
        boolean fall = source.is(DamageTypeTags.IS_FALL);
        if (!fall && !source.is(DamageTypes.FLY_INTO_WALL)) {
            return;
        }
        PowerData data = Powers.get(player);
        if (FalconWings.saves(player, data) || fall && Dive.fallGuarded(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onShieldedBlow(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PowerData data = Powers.get(player);
        if (!WingShield.up(player, data)) {
            return;
        }
        DamageSource source = event.getSource();
        Vec3 from = source.getSourcePosition();
        if (from == null || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !WingShield.fromFront(player, from)) {
            return;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            event.setAmount(event.getAmount() * (float) (1.0 - FalconAbilities.cfg(
                    com.kolodkingal.enhanced.config.FalconConfig.SHIELD_EXPLOSION_REDUCTION, 0.5)));
        } else if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            event.setCanceled(true);
        } else if (source.getEntity() != null) {
            event.setAmount(event.getAmount() * (float) (1.0 - FalconAbilities.cfg(
                    com.kolodkingal.enhanced.config.FalconConfig.SHIELD_MELEE_REDUCTION, 0.7)));
        } else {
            return;
        }
        clang(player, from);
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (projectile.level().isClientSide || !(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof ServerPlayer player) || projectile.getOwner() == player) {
            return;
        }
        PowerData data = Powers.get(player);
        if (!WingShield.up(player, data) || !WingShield.fromFront(player, projectile.position())) {
            return;
        }
        event.setCanceled(true);
        // Back the way it came, at full speed, and his now (it may hit its shooter).
        Vec3 motion = projectile.getDeltaMovement();
        Vec3 back = motion.lengthSqr() < 1.0E-6 ? player.getLookAngle() : motion.scale(-1.0);
        projectile.setDeltaMovement(back);
        projectile.setOwner(player);
        projectile.setPos(projectile.position().add(back.normalize().scale(0.3)));
        float yaw = (float) (Math.atan2(back.x, back.z) * (180.0 / Math.PI));
        float pitch = (float) (Math.atan2(back.y, back.horizontalDistance()) * (180.0 / Math.PI));
        projectile.setYRot(yaw);
        projectile.setXRot(pitch);
        projectile.yRotO = yaw;
        projectile.xRotO = pitch;
        projectile.hasImpulse = true;
        projectile.hurtMarked = true;
        clang(player, projectile.position());
    }

    /** The ring of metal and the sparks of a hit on the shield. */
    private static void clang(ServerPlayer player, Vec3 at) {
        FalconAbilities.sound(player.serverLevel(), at, ModSounds.WING_SHIELD_DEFLECT.get(), 1.0F,
                0.9F + player.getRandom().nextFloat() * 0.2F);
        FalconAbilities.send(player.serverLevel(), player, FalconFxPayload.DEFLECT, List.of(at), 0);
    }

    private FalconEvents() {
    }
}
