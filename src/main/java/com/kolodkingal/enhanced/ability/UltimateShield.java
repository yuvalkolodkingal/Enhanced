package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * (3.19) Ultimate Wanda's Chaos Shield lets nothing in: while it is up (and while it guards her body as her mind is in another
 * one) no creature or player can be inside it (whoever is, or comes, is put back outside it), no projectile of anyone else's
 * stays in it, no harm of anyone else's reaches her (the damage hook, {@link #stops}), no spell or poison of another's holds
 * on her, and it is never worn down or shattered. Only what bypasses everything (the void, /kill) gets through.
 */
public final class UltimateShield {
    private UltimateShield() {
    }

    /** Whether her impenetrable shield is up now. */
    public static boolean up(Player player) {
        return UltimateWanda.is(player) && (WandaAbilities.hasShieldUp(player) || UltimateWanda.shielded(player));
    }

    /** Whether her shield stops this harm (anything not her own). */
    public static boolean stops(Player player, net.minecraft.world.damagesource.DamageSource source) {
        return up(player) && source.getEntity() != player;
    }

    /** Every tick of hers: whoever is inside is put out, a stray projectile is gone, another's harmful effects fall away. */
    public static void tick(ServerPlayer player) {
        if (!up(player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        double radius = WandaAbilities.SHIELD_RADIUS * UltimateWanda.SHIELD_SCALE;
        Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);
        for (Entity e : level.getEntities(player, new AABB(center, center).inflate(radius),
                e -> e.isAlive() && !e.isSpectator() && !(e instanceof ItemEntity) && e != player.getVehicle()
                        && !player.hasPassenger(e))) {
            if (e instanceof Projectile projectile) {
                if (projectile.getOwner() != player && e.getBoundingBox().getCenter().distanceToSqr(center) < radius * radius) {
                    level.sendParticles(new EnergyParticleOptions(Power.SCARLET_WITCH.glow, 1.0F), e.getX(), e.getY(), e.getZ(), 6,
                            0.1, 0.1, 0.1, 0.05);
                    projectile.discard();
                }
                continue;
            }
            if (e instanceof Player p && p.isCreative()) {
                continue;
            }
            Vec3 at = e.getBoundingBox().getCenter();
            Vec3 away = at.subtract(center);
            double distance = away.length();
            double reach = radius + e.getBbWidth() / 2.0;
            if (distance >= reach) {
                continue;
            }
            double flat = Math.hypot(away.x, away.z);
            Vec3 out = flat < 1.0E-3 ? Vec3.directionFromRotation(0, player.getYRot()) : new Vec3(away.x / flat, 0, away.z / flat);
            Vec3 to = new Vec3(center.x, e.getY(), center.z).add(out.scale(reach + 0.4));
            if (e instanceof ServerPlayer other) {
                other.connection.teleport(to.x, to.y, to.z, other.getYRot(), other.getXRot());
            } else {
                e.teleportTo(to.x, to.y, to.z);
            }
            e.setDeltaMovement(out.x * 0.6, 0.2, out.z * 0.6);
            e.hurtMarked = true;
            Vec3 spark = center.add(out.scale(radius));
            level.sendParticles(new EnergyParticleOptions(Power.SCARLET_WITCH.glow, 1.3F), spark.x, at.y, spark.z, 8, 0.2, 0.3, 0.2, 0.08);
            level.playSound(null, spark.x, at.y, spark.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.7F, 1.9F);
        }
        List<MobEffectInstance> harmful = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                harmful.add(effect);
            }
        }
        for (MobEffectInstance effect : harmful) {
            player.removeEffect(effect.getEffect());
        }
    }
}
