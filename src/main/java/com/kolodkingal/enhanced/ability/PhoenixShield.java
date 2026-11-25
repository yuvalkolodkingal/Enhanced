package com.kolodkingal.enhanced.ability;

import com.kolodkingal.enhanced.particle.EnergyParticleOptions;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.PowerData;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * (3.19.2) Jean Grey's psionic force field, on Sneak + G: a sphere of orange psionic force round her, for defence. While
 * it is up the projectiles that come at her are thrown back, the hostile creatures that come too close are pushed out,
 * and every blow that still reaches her is cut to {@link #DAMAGE_TAKEN} of itself. It costs a little hunger while it
 * stands. The clients draw it from {@link Live#PHOENIX_SHIELD}.
 */
public final class PhoenixShield {
    /** The radius of the field round the middle of her body. */
    public static final double RADIUS = 1.8;
    /** What of a blow still gets through the field. */
    public static final float DAMAGE_TAKEN = 0.3F;
    /** How far out projectiles are caught, and how close a hostile may come. */
    private static final double CATCH = 4.0;
    private static final double PUSH = 2.6;
    private static final float EXHAUSTION_PER_TICK = 0.012F;
    private static final Set<UUID> UP = new HashSet<>();

    private PhoenixShield() {
    }

    public static boolean isUp(Entity e) {
        return e instanceof ServerPlayer && UP.contains(e.getUUID());
    }

    /** Sneak + G: the field goes up, or down. */
    public static void toggle(ServerPlayer player, PowerData data) {
        boolean on = UP.add(player.getUUID());
        if (!on) {
            UP.remove(player.getUUID());
        }
        data.set(Live.PHOENIX_SHIELD, on);
        player.displayClientMessage(Component.translatable(on ? "message.enhanced.phoenix_shield.on" : "message.enhanced.phoenix_shield.off"), true);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                on ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7F, 1.4F);
    }

    /** Her tick: the field holds (projectiles back, hostiles out) and costs its hunger. */
    public static void tick(ServerPlayer player, PowerData data) {
        boolean up = UP.contains(player.getUUID());
        data.set(Live.PHOENIX_SHIELD, up);
        if (!up) {
            return;
        }
        if (!player.isAlive() || player.isSpectator()) {
            stop(player, data);
            return;
        }
        player.causeFoodExhaustion(EXHAUSTION_PER_TICK);
        ServerLevel level = player.serverLevel();
        Vec3 centre = player.position().add(0, player.getBbHeight() * 0.5, 0);
        for (Projectile shot : level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(CATCH),
                p -> p.getOwner() != player && p.isAlive())) {
            Vec3 motion = shot.getDeltaMovement();
            Vec3 toHer = centre.subtract(shot.position());
            if (motion.lengthSqr() < 1.0E-4 || motion.dot(toHer) <= 0.0) {
                continue; // already going away
            }
            shot.setDeltaMovement(motion.scale(-0.6));
            shot.hurtMarked = true;
            level.sendParticles(new EnergyParticleOptions(PhoenixAbilities.TK_GLOW, 1.0F), shot.getX(), shot.getY(), shot.getZ(),
                    4, 0.1, 0.1, 0.1, 0.02);
        }
        if (player.tickCount % 4 == 0) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(PUSH),
                    e -> e != player && e.isAlive() && pushes(player, e))) {
                Vec3 away = e.position().subtract(player.position());
                Vec3 flat = new Vec3(away.x, 0, away.z);
                if (flat.lengthSqr() < 1.0E-4) {
                    flat = new Vec3(1, 0, 0);
                }
                flat = flat.normalize();
                e.push(flat.x * 0.6, 0.15, flat.z * 0.6);
                e.hurtMarked = true;
            }
        }
    }

    /** Who the field pushes out: hostile creatures, and creatures after her, never hers, nor a boss. */
    private static boolean pushes(ServerPlayer player, LivingEntity e) {
        if (PhoenixAbilities.isHers(player, e) || e.getType().is(ModTags.BOSSES) || e.getType().is(ModTags.PIN_IMMUNE)) {
            return false;
        }
        if (e instanceof Player) {
            return false; // other players walk up to her freely; their blows are still cut
        }
        return e instanceof Enemy || e instanceof Mob mob && mob.getTarget() == player;
    }

    /** What of a blow gets through the field (the whole blow if it is down or the blow cannot be stopped). */
    public static float filter(LivingEntity target, DamageSource source, float amount) {
        // (a fall, drowning, hunger, a wall she is stuck in, or what nothing stops: those are not blows from outside)
        if (!isUp(target) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.IS_FALL)
                || source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypes.STARVE) || source.is(DamageTypes.IN_WALL)) {
            return amount;
        }
        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(new EnergyParticleOptions(PhoenixAbilities.TK_GLOW, 1.2F), target.getX(),
                    target.getY() + target.getBbHeight() * 0.5, target.getZ(), 8, 0.6, 0.6, 0.6, 0.05);
        }
        return amount * DAMAGE_TAKEN;
    }

    public static void stop(ServerPlayer player, PowerData data) {
        UP.remove(player.getUUID());
        data.set(Live.PHOENIX_SHIELD, false);
    }

    public static void clear() {
        UP.clear();
    }
}
