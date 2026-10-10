package com.kolodkingal.enhanced.event;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.ability.MagnetoAbilities;
import com.kolodkingal.enhanced.ability.PhotonAbilities;
import com.kolodkingal.enhanced.ability.PowerTicker;
import com.kolodkingal.enhanced.ability.QuicksilverAbilities;
import com.kolodkingal.enhanced.ability.SlowMotion;
import com.kolodkingal.enhanced.ability.Telekinesis;
import com.kolodkingal.enhanced.ability.VisionAbilities;
import com.kolodkingal.enhanced.ability.WandaAbilities;
import com.kolodkingal.enhanced.command.EnhancedCommand;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModDamageTypes;
import com.kolodkingal.enhanced.registry.ModTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Game-bus events shared by the server and singleplayer. */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class CommonEvents {

    // ---- ticking ----

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PowerTicker.tick(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Telekinesis.tickFlights(event.getServer());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            HexManager.tickLevel(level);
            SlowMotion.serverTick(level);
            MagnetoAbilities.tickHeld(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Telekinesis.clear();
        HexManager.clearMemory();
        SlowMotion.clear();
        MagnetoAbilities.clearHeld();
    }

    // ---- Quicksilver Time slows the world around him (runs on both sides) ----

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (SlowMotion.skipTick(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityTickDone(EntityTickEvent.Post event) {
        SlowMotion.afterTick(event.getEntity());
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SlowMotion.onStartTracking(event.getTarget(), player);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide) {
            SlowMotion.onJoin(event.getEntity());
            MagnetoAbilities.onJoin(event.getEntity());
        }
    }

    // ---- lifecycle ----

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Telekinesis.onLogout(player);
            Powers.get(player).setBeamActive(false);
            Powers.get(player).setChaosShieldActive(false);
            Powers.get(player).setShrapnelActive(false);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Telekinesis.release(player, false);
            HexManager.forget(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        HexManager.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        HexManager.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Telekinesis.release(player, false);
            if (Powers.is(player, Power.SCARLET_WITCH)) {
                HexManager.onOwnerDeath(player);
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        EnhancedCommand.register(event.getDispatcher());
    }

    // ---- Magneto's mental immunity ----

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().is(MobEffects.CONFUSION) && MagnetoAbilities.wearsHelmet(event.getEntity())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    // ---- defences ----

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        DamageSource source = event.getSource();
        boolean unstoppable = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);

        if ((PhotonAbilities.isPhased(player) || VisionAbilities.isPhased(player)) && !unstoppable) {
            event.setCanceled(true);
            return;
        }
        if (WandaAbilities.hasShieldUp(player) && !unstoppable) {
            if (source.is(DamageTypeTags.IS_PROJECTILE) && source.getDirectEntity() != null && source.getEntity() != player) {
                event.setCanceled(true);
                return;
            }
            if (source.is(DamageTypeTags.IS_EXPLOSION)) {
                event.setAmount(event.getAmount() * EnhancedConfig.CHAOS_SHIELD_EXPLOSION_FACTOR.get().floatValue());
            }
        }
        if (MagnetoAbilities.wearsHelmet(player) && isWandasAttack(source)) {
            // The helmet only blocks Nausea: Magneto takes Wanda's attacks at full strength.
            event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> 0F);
            event.addReductionModifier(DamageContainer.Reduction.ENCHANTMENTS, (container, reduction) -> 0F);
        }
        if (source.is(DamageTypeTags.IS_FALL) && QuicksilverAbilities.isRunning(player)) {
            event.setCanceled(true);
            return;
        }
        if (MagnetoAbilities.hasShieldUp(player) && MagnetoAbilities.isMetalProjectile(source.getDirectEntity())
                && source.getEntity() != player) {
            event.setCanceled(true);
            return;
        }
        if (Powers.is(player, Power.PHOTON) && source.is(ModTags.ABSORBABLE_ENERGY) && source.getEntity() != player) {
            event.setAmount(PhotonAbilities.absorb(player, event.getAmount()));
        }
    }

    /** The Chaos Beam, or anything a Scarlet Witch hurled with telekinesis. */
    private static boolean isWandasAttack(DamageSource source) {
        return source.is(ModDamageTypes.CHAOS_BEAM)
                || (source.is(ModDamageTypes.TELEKINETIC_SLAM) && source.getEntity() instanceof Player thrower && Powers.is(thrower, Power.SCARLET_WITCH));
    }

    /** A slowed projectile gets its full speed back the moment it hits someone, so it hits as hard as it would have. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSlowedProjectileImpact(ProjectileImpactEvent event) {
        if (!event.getProjectile().level().isClientSide && event.getRayTraceResult() instanceof EntityHitResult) {
            SlowMotion.unslow(event.getProjectile());
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (projectile.level().isClientSide || !(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof ServerPlayer player) || projectile.getOwner() == player) {
            return;
        }
        if (PhotonAbilities.isPhased(player) || VisionAbilities.isPhased(player)) {
            event.setCanceled(true); // passes straight through
            return;
        }
        if (WandaAbilities.hasShieldUp(player)) {
            event.setCanceled(true);
            WandaAbilities.disintegrate(player.serverLevel(), projectile);
            return;
        }
        if (MagnetoAbilities.hasShieldUp(player) && MagnetoAbilities.isMetalProjectile(projectile)) {
            event.setCanceled(true);
            if (!projectile.getPersistentData().getBoolean(MagnetoAbilities.DEMAGNETIZED_TAG)) {
                MagnetoAbilities.repel(player, projectile);
            }
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (PhotonAbilities.isPhased(event.getEntity()) || VisionAbilities.isPhased(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    // ---- the Hex wall also stops teleports ----

    @SubscribeEvent
    public static void onEnderPearl(EntityTeleportEvent.EnderPearl event) {
        checkHexTeleport(event);
    }

    @SubscribeEvent
    public static void onChorusFruit(EntityTeleportEvent.ChorusFruit event) {
        if (!event.getEntityLiving().level().isClientSide) {
            HexManager.rememberTeleportStart(event.getEntityLiving());
        }
    }

    @SubscribeEvent
    public static void onEnderTeleport(EntityTeleportEvent.EnderEntity event) {
        if (!event.getEntityLiving().level().isClientSide) {
            HexManager.rememberTeleportStart(event.getEntityLiving());
        }
    }

    private static void checkHexTeleport(EntityTeleportEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity && entity.level() instanceof ServerLevel level
                && !HexManager.onTeleport(level, entity, event.getTarget())) {
            event.setCanceled(true);
        }
    }

    private CommonEvents() {
    }
}
