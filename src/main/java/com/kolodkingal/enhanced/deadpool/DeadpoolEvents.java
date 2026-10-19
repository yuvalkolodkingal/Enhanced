package com.kolodkingal.enhanced.deadpool;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.DeadpoolConfig;
import com.kolodkingal.enhanced.network.DeadpoolFxPayload;
import com.kolodkingal.enhanced.power.Live;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

/**
 * Deadpool's event hooks (3.7). Priorities as {@code docs/HEROES-AND-BOSSES.md} lists them:
 * <ul> <li>{@link ProjectileImpactEvent}: while he parries, what flies at him from the front does not hit him: it
 * bounces back the way he looks, his now (arrows, bolts, fireballs; a beam is no projectile);</li>
 * <li>{@link LivingIncomingDamageEvent}, HIGH: no fall damage from his own Drop-In; Dogpool's bite never reaches whom
 * the heroes' rule spares (a player without PvP); NORMAL: while he parries, a blow from the front loses {@code
 * parryMeleeReduction};</li> <li>{@link LivingChangeTargetEvent}, NORMAL: a creature he taunted keeps at him, one his
 * smoke blinded does not find him ({@link Taunt#keepsOff}).</li> </ul> (The healing factor's own rules are lane L1's,
 * in {@code healing/}: a creature that loses a downed healer is let go of by the taunt.)
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class DeadpoolEvents {

    @SubscribeEvent
    public static void onProjectile(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof ServerPlayer
                player)) {
            return;
        }
        Projectile projectile = event.getProjectile();
        if (projectile.getOwner() == player || !Powers.get(player).is(Live.DEADPOOL_PARRYING)) {
            return;
        }
        Vec3 motion = projectile.getDeltaMovement();
        Vec3 look = player.getLookAngle();
        boolean fromFront = motion.lengthSqr() < 1.0E-6 ? Katanas.inFront(player, projectile.position())
                : motion.normalize().dot(look) < 0.0;
        if (!fromFront) {
            return;
        }
        event.setCanceled(true);
        deflect(player, projectile);
    }

    /** The projectile bounces back the way he looks, his now; the swords ting. */
    static void deflect(ServerPlayer player, Projectile projectile) {
        Vec3 look = player.getLookAngle();
        double speed = Math.max(1.2, projectile.getDeltaMovement().length());
        Vec3 from = player.getEyePosition().add(look.scale(1.3)).subtract(0.0, projectile.getBbHeight() * 0.5, 0.0);
        projectile.setPos(from);
        projectile.setDeltaMovement(look.scale(speed));
        if (projectile instanceof AbstractHurtingProjectile hurting) {
            hurting.accelerationPower = Math.max(hurting.accelerationPower, 0.1);
        }
        projectile.setOwner(player);
        projectile.hasImpulse = true;
        projectile.hurtMarked = true;
        ServerLevel level = player.serverLevel();
        DeadpoolAbilities.send(level, player, DeadpoolFxPayload.DEFLECT, List.of(from), 0);
        DeadpoolAbilities.sound(level, from, ModSounds.DEADPOOL_DEFLECT.get(), 1.0F,
                1.1F + level.random.nextFloat() * 0.2F);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onGuarded(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (event.getEntity() instanceof ServerPlayer player && source.is(DamageTypeTags.IS_FALL)
                && TeleportBelt.fallGuarded(player)) {
            event.setCanceled(true);
            return;
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof DogpoolEntity dog && !dog.mayBite(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onParried(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DamageSource source = event.getSource();
        Entity direct = source.getDirectEntity();
        if (direct == null || direct instanceof Projectile || source.is(DamageTypeTags.BYPASSES_ARMOR)
                || !Katanas.parries(player, direct)) {
            return;
        }
        double cut = DeadpoolAbilities.cfg(DeadpoolConfig.PARRY_MELEE_REDUCTION, 0.6);
        event.setAmount((float) (event.getAmount() * (1.0 - cut)));
        ServerLevel level = player.serverLevel();
        Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(0.8));
        DeadpoolAbilities.send(level, player, DeadpoolFxPayload.DEFLECT, List.of(at), 1);
        DeadpoolAbilities.sound(level, at, ModSounds.DEADPOOL_PARRY.get(), 1.0F, 1.0F);
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide) {
            return;
        }
        if (Taunt.keepsOff(mob, event.getNewAboutToBeSetTarget(), mob.level().getGameTime())) {
            event.setCanceled(true);
        }
    }

    private DeadpoolEvents() {
    }
}
