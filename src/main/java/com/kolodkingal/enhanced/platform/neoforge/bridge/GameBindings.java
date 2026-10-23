package com.kolodkingal.enhanced.platform.neoforge.bridge;

import com.kolodkingal.enhanced.platform.event.AttackEntityEvent;
import com.kolodkingal.enhanced.platform.event.BlockDropsEvent;
import com.kolodkingal.enhanced.platform.event.BlockEvent;
import com.kolodkingal.enhanced.platform.event.CanPlayerSleepEvent;
import com.kolodkingal.enhanced.platform.event.EntityJoinLevelEvent;
import com.kolodkingal.enhanced.platform.event.EntityLeaveLevelEvent;
import com.kolodkingal.enhanced.platform.event.EntityMobGriefingEvent;
import com.kolodkingal.enhanced.platform.event.EntityMountEvent;
import com.kolodkingal.enhanced.platform.event.EntityStruckByLightningEvent;
import com.kolodkingal.enhanced.platform.event.EntityTeleportEvent;
import com.kolodkingal.enhanced.platform.event.EntityTickEvent;
import com.kolodkingal.enhanced.platform.event.EntityTravelToDimensionEvent;
import com.kolodkingal.enhanced.platform.event.ExplosionEvent;
import com.kolodkingal.enhanced.platform.event.ExplosionKnockbackEvent;
import com.kolodkingal.enhanced.platform.event.FMLCommonSetupEvent;
import com.kolodkingal.enhanced.platform.event.FinalizeSpawnEvent;
import com.kolodkingal.enhanced.platform.event.ItemAttributeModifierEvent;
import com.kolodkingal.enhanced.platform.event.ItemEntityPickupEvent;
import com.kolodkingal.enhanced.platform.event.ItemTooltipEvent;
import com.kolodkingal.enhanced.platform.event.ItemTossEvent;
import com.kolodkingal.enhanced.platform.event.LevelEvent;
import com.kolodkingal.enhanced.platform.event.LevelTickEvent;
import com.kolodkingal.enhanced.platform.event.LivingChangeTargetEvent;
import com.kolodkingal.enhanced.platform.event.LivingDamageEvent;
import com.kolodkingal.enhanced.platform.event.LivingDeathEvent;
import com.kolodkingal.enhanced.platform.event.LivingDestroyBlockEvent;
import com.kolodkingal.enhanced.platform.event.LivingDropsEvent;
import com.kolodkingal.enhanced.platform.event.LivingEntityUseItemEvent;
import com.kolodkingal.enhanced.platform.event.LivingEquipmentChangeEvent;
import com.kolodkingal.enhanced.platform.event.LivingExperienceDropEvent;
import com.kolodkingal.enhanced.platform.event.LivingFallEvent;
import com.kolodkingal.enhanced.platform.event.LivingIncomingDamageEvent;
import com.kolodkingal.enhanced.platform.event.LivingKnockBackEvent;
import com.kolodkingal.enhanced.platform.event.LivingShieldBlockEvent;
import com.kolodkingal.enhanced.platform.event.MobEffectEvent;
import com.kolodkingal.enhanced.platform.event.MobSpawnEvent;
import com.kolodkingal.enhanced.platform.event.OnDatapackSyncEvent;
import com.kolodkingal.enhanced.platform.event.PistonEvent;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.PlayerInteractEvent;
import com.kolodkingal.enhanced.platform.event.PlayerRespawnPositionEvent;
import com.kolodkingal.enhanced.platform.event.PlayerSetSpawnEvent;
import com.kolodkingal.enhanced.platform.event.PlayerTickEvent;
import com.kolodkingal.enhanced.platform.event.ProjectileImpactEvent;
import com.kolodkingal.enhanced.platform.event.RegisterCommandsEvent;
import com.kolodkingal.enhanced.platform.event.ServerStartedEvent;
import com.kolodkingal.enhanced.platform.event.ServerStartingEvent;
import com.kolodkingal.enhanced.platform.event.ServerStoppedEvent;
import com.kolodkingal.enhanced.platform.event.ServerStoppingEvent;
import com.kolodkingal.enhanced.platform.event.ServerTickEvent;
import com.kolodkingal.enhanced.platform.event.TriState;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Generated from the port's event table (do not edit by hand): how each NeoForge event becomes its
 * common event. An event with something to change (a setter, cancelling) gets an adapter that reads and writes the
 * NeoForge event itself, so NeoForge sees every change at once; any other is copied.
 */
public final class GameBindings {
    private GameBindings() {
    }

    public static void install(Map<Class<?>, Binding<?, ?>> table) {
        Binding.put(table, PlayerEvent.PlayerLoggedInEvent.class,
                net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent.class,
                n -> new PlayerEvent.PlayerLoggedInEvent(n.getEntity()));
        Binding.put(table, PlayerEvent.PlayerLoggedOutEvent.class,
                net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent.class,
                n -> new PlayerEvent.PlayerLoggedOutEvent(n.getEntity()));
        Binding.put(table, PlayerEvent.PlayerChangedDimensionEvent.class,
                net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent.class,
                n -> new PlayerEvent.PlayerChangedDimensionEvent(n.getEntity(), n.getFrom(), n.getTo()));
        Binding.put(table, PlayerEvent.PlayerRespawnEvent.class,
                net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent.class,
                n -> new PlayerEvent.PlayerRespawnEvent(n.getEntity(), n.isEndConquered()));
        Binding.put(table, PlayerEvent.StartTracking.class,
                net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking.class,
                n -> new PlayerEvent.StartTracking(n.getEntity(), n.getTarget()));
        Binding.put(table, PlayerEvent.Clone.class, net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone.class,
                n -> new PlayerEvent.Clone(n.getEntity(), n.getOriginal(), n.isWasDeath()));
        Binding.put(table, PlayerEvent.BreakSpeed.class,
                net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed.class,
                NeoPlayerEventBreakSpeed::new);
        Binding.put(table, PlayerEvent.ItemCraftedEvent.class,
                net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent.class,
                n -> new PlayerEvent.ItemCraftedEvent(n.getEntity(), n.getCrafting(), n.getInventory()));
        Binding.put(table, PlayerEvent.PlayerChangeGameModeEvent.class,
                net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangeGameModeEvent.class,
                NeoPlayerEventPlayerChangeGameModeEvent::new);
        Binding.put(table, PlayerInteractEvent.EntityInteractSpecific.class,
                net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific.class,
                NeoPlayerInteractEventEntityInteractSpecific::new);
        Binding.put(table, PlayerInteractEvent.EntityInteract.class,
                net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract.class,
                NeoPlayerInteractEventEntityInteract::new);
        Binding.put(table, PlayerInteractEvent.RightClickBlock.class,
                net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock.class,
                NeoPlayerInteractEventRightClickBlock::new);
        Binding.put(table, PlayerInteractEvent.RightClickItem.class,
                net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem.class,
                NeoPlayerInteractEventRightClickItem::new);
        Binding.put(table, PlayerInteractEvent.LeftClickBlock.class,
                net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.class,
                NeoPlayerInteractEventLeftClickBlock::new);
        Binding.put(table, AttackEntityEvent.class, net.neoforged.neoforge.event.entity.player.AttackEntityEvent.class,
                NeoAttackEntityEvent::new);
        Binding.put(table, CanPlayerSleepEvent.class,
                net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent.class,
                NeoCanPlayerSleepEvent::new);
        Binding.put(table, PlayerRespawnPositionEvent.class,
                net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent.class,
                NeoPlayerRespawnPositionEvent::new);
        Binding.put(table, PlayerSetSpawnEvent.class,
                net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent.class,
                NeoPlayerSetSpawnEvent::new);
        Binding.put(table, ItemTooltipEvent.class, net.neoforged.neoforge.event.entity.player.ItemTooltipEvent.class,
                n -> new ItemTooltipEvent(n.getItemStack(), n.getEntity(), n.getToolTip(), n.getFlags(),
                        n.getContext()));
        Binding.put(table, ItemEntityPickupEvent.Pre.class,
                net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre.class,
                NeoItemEntityPickupEventPre::new);
        Binding.put(table, ItemTossEvent.class, net.neoforged.neoforge.event.entity.item.ItemTossEvent.class,
                NeoItemTossEvent::new);
        Binding.put(table, LivingIncomingDamageEvent.class,
                net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent.class,
                NeoLivingIncomingDamageEvent::new);
        Binding.put(table, LivingDamageEvent.Pre.class,
                net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre.class,
                NeoLivingDamageEventPre::new);
        Binding.put(table, LivingDamageEvent.Post.class,
                net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post.class,
                n -> new LivingDamageEvent.Post(n.getEntity(), n.getSource(), n.getOriginalDamage(),
                        n.getNewDamage(), n.getBlockedDamage(), n.getShieldDamage()));
        Binding.put(table, LivingDeathEvent.class, net.neoforged.neoforge.event.entity.living.LivingDeathEvent.class,
                NeoLivingDeathEvent::new);
        Binding.put(table, LivingChangeTargetEvent.class,
                net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent.class,
                NeoLivingChangeTargetEvent::new);
        Binding.put(table, LivingDestroyBlockEvent.class,
                net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent.class,
                NeoLivingDestroyBlockEvent::new);
        Binding.put(table, LivingDropsEvent.class, net.neoforged.neoforge.event.entity.living.LivingDropsEvent.class,
                NeoLivingDropsEvent::new);
        Binding.put(table, LivingEntityUseItemEvent.Start.class,
                net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Start.class,
                NeoLivingEntityUseItemEventStart::new);
        Binding.put(table, LivingEntityUseItemEvent.Tick.class,
                net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Tick.class,
                NeoLivingEntityUseItemEventTick::new);
        Binding.put(table, LivingEquipmentChangeEvent.class,
                net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent.class,
                n -> new LivingEquipmentChangeEvent(n.getEntity(), n.getSlot(), n.getFrom(), n.getTo()));
        Binding.put(table, LivingExperienceDropEvent.class,
                net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent.class,
                NeoLivingExperienceDropEvent::new);
        Binding.put(table, LivingFallEvent.class, net.neoforged.neoforge.event.entity.living.LivingFallEvent.class,
                NeoLivingFallEvent::new);
        Binding.put(table, LivingKnockBackEvent.class,
                net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent.class,
                NeoLivingKnockBackEvent::new);
        Binding.put(table, LivingShieldBlockEvent.class,
                net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent.class,
                NeoLivingShieldBlockEvent::new);
        Binding.put(table, MobEffectEvent.Added.class,
                net.neoforged.neoforge.event.entity.living.MobEffectEvent.Added.class,
                n -> new MobEffectEvent.Added(n.getEntity(), n.getEffectInstance(), n.getOldEffectInstance(),
                        n.getEffectSource()));
        Binding.put(table, MobEffectEvent.Applicable.class,
                net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable.class,
                NeoMobEffectEventApplicable::new);
        Binding.put(table, MobEffectEvent.Expired.class,
                net.neoforged.neoforge.event.entity.living.MobEffectEvent.Expired.class,
                NeoMobEffectEventExpired::new);
        Binding.put(table, MobEffectEvent.Remove.class,
                net.neoforged.neoforge.event.entity.living.MobEffectEvent.Remove.class,
                NeoMobEffectEventRemove::new);
        Binding.put(table, MobSpawnEvent.PositionCheck.class,
                net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck.class,
                NeoMobSpawnEventPositionCheck::new);
        Binding.put(table, MobSpawnEvent.SpawnPlacementCheck.class,
                net.neoforged.neoforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck.class,
                NeoMobSpawnEventSpawnPlacementCheck::new);
        Binding.put(table, FinalizeSpawnEvent.class,
                net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent.class,
                NeoFinalizeSpawnEvent::new);
        Binding.put(table, EntityJoinLevelEvent.class, net.neoforged.neoforge.event.entity.EntityJoinLevelEvent.class,
                NeoEntityJoinLevelEvent::new);
        Binding.put(table, EntityLeaveLevelEvent.class, net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent.class,
                n -> new EntityLeaveLevelEvent(n.getEntity(), n.getLevel()));
        Binding.put(table, EntityMobGriefingEvent.class,
                net.neoforged.neoforge.event.entity.EntityMobGriefingEvent.class,
                NeoEntityMobGriefingEvent::new);
        Binding.put(table, EntityMountEvent.class, net.neoforged.neoforge.event.entity.EntityMountEvent.class,
                NeoEntityMountEvent::new);
        Binding.put(table, EntityStruckByLightningEvent.class,
                net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent.class,
                NeoEntityStruckByLightningEvent::new);
        Binding.put(table, EntityTeleportEvent.ChorusFruit.class,
                net.neoforged.neoforge.event.entity.EntityTeleportEvent.ChorusFruit.class,
                NeoEntityTeleportEventChorusFruit::new);
        Binding.put(table, EntityTeleportEvent.EnderEntity.class,
                net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderEntity.class,
                NeoEntityTeleportEventEnderEntity::new);
        Binding.put(table, EntityTeleportEvent.EnderPearl.class,
                net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderPearl.class,
                NeoEntityTeleportEventEnderPearl::new);
        Binding.put(table, EntityTravelToDimensionEvent.class,
                net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent.class,
                NeoEntityTravelToDimensionEvent::new);
        Binding.put(table, ProjectileImpactEvent.class, net.neoforged.neoforge.event.entity.ProjectileImpactEvent.class,
                NeoProjectileImpactEvent::new);
        Binding.put(table, EntityTickEvent.Pre.class, net.neoforged.neoforge.event.tick.EntityTickEvent.Pre.class,
                NeoEntityTickEventPre::new);
        Binding.put(table, EntityTickEvent.Post.class, net.neoforged.neoforge.event.tick.EntityTickEvent.Post.class,
                n -> new EntityTickEvent.Post(n.getEntity()));
        Binding.put(table, PlayerTickEvent.Pre.class, net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre.class,
                n -> new PlayerTickEvent.Pre(n.getEntity()));
        Binding.put(table, PlayerTickEvent.Post.class, net.neoforged.neoforge.event.tick.PlayerTickEvent.Post.class,
                n -> new PlayerTickEvent.Post(n.getEntity()));
        Binding.put(table, LevelTickEvent.Pre.class, net.neoforged.neoforge.event.tick.LevelTickEvent.Pre.class,
                n -> new LevelTickEvent.Pre(n::hasTime, n.getLevel()));
        Binding.put(table, LevelTickEvent.Post.class, net.neoforged.neoforge.event.tick.LevelTickEvent.Post.class,
                n -> new LevelTickEvent.Post(n::hasTime, n.getLevel()));
        Binding.put(table, ServerTickEvent.Pre.class, net.neoforged.neoforge.event.tick.ServerTickEvent.Pre.class,
                n -> new ServerTickEvent.Pre(n::hasTime, n.getServer()));
        Binding.put(table, ServerTickEvent.Post.class, net.neoforged.neoforge.event.tick.ServerTickEvent.Post.class,
                n -> new ServerTickEvent.Post(n::hasTime, n.getServer()));
        Binding.put(table, ServerStartingEvent.class, net.neoforged.neoforge.event.server.ServerStartingEvent.class,
                n -> new ServerStartingEvent(n.getServer()));
        Binding.put(table, ServerStartedEvent.class, net.neoforged.neoforge.event.server.ServerStartedEvent.class,
                n -> new ServerStartedEvent(n.getServer()));
        Binding.put(table, ServerStoppingEvent.class, net.neoforged.neoforge.event.server.ServerStoppingEvent.class,
                n -> new ServerStoppingEvent(n.getServer()));
        Binding.put(table, ServerStoppedEvent.class, net.neoforged.neoforge.event.server.ServerStoppedEvent.class,
                n -> new ServerStoppedEvent(n.getServer()));
        Binding.put(table, LevelEvent.Load.class, net.neoforged.neoforge.event.level.LevelEvent.Load.class,
                n -> new LevelEvent.Load(n.getLevel()));
        Binding.put(table, LevelEvent.Save.class, net.neoforged.neoforge.event.level.LevelEvent.Save.class,
                n -> new LevelEvent.Save(n.getLevel()));
        Binding.put(table, LevelEvent.Unload.class, net.neoforged.neoforge.event.level.LevelEvent.Unload.class,
                n -> new LevelEvent.Unload(n.getLevel()));
        Binding.put(table, BlockEvent.BreakEvent.class, net.neoforged.neoforge.event.level.BlockEvent.BreakEvent.class,
                NeoBlockEventBreakEvent::new);
        Binding.put(table, BlockEvent.EntityPlaceEvent.class,
                net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent.class,
                NeoBlockEventEntityPlaceEvent::new);
        Binding.put(table, BlockEvent.FarmlandTrampleEvent.class,
                net.neoforged.neoforge.event.level.BlockEvent.FarmlandTrampleEvent.class,
                NeoBlockEventFarmlandTrampleEvent::new);
        Binding.put(table, BlockEvent.FluidPlaceBlockEvent.class,
                net.neoforged.neoforge.event.level.BlockEvent.FluidPlaceBlockEvent.class,
                NeoBlockEventFluidPlaceBlockEvent::new);
        Binding.put(table, BlockEvent.NeighborNotifyEvent.class,
                net.neoforged.neoforge.event.level.BlockEvent.NeighborNotifyEvent.class,
                NeoBlockEventNeighborNotifyEvent::new);
        Binding.put(table, BlockEvent.PortalSpawnEvent.class,
                net.neoforged.neoforge.event.level.BlockEvent.PortalSpawnEvent.class,
                NeoBlockEventPortalSpawnEvent::new);
        Binding.put(table, BlockEvent.BlockToolModificationEvent.class,
                net.neoforged.neoforge.event.level.BlockEvent.BlockToolModificationEvent.class,
                NeoBlockEventBlockToolModificationEvent::new);
        Binding.put(table, BlockDropsEvent.class, net.neoforged.neoforge.event.level.BlockDropsEvent.class,
                NeoBlockDropsEvent::new);
        Binding.put(table, PistonEvent.Pre.class, net.neoforged.neoforge.event.level.PistonEvent.Pre.class,
                NeoPistonEventPre::new);
        Binding.put(table, ExplosionEvent.Detonate.class,
                net.neoforged.neoforge.event.level.ExplosionEvent.Detonate.class,
                n -> new ExplosionEvent.Detonate(n.getLevel(), n.getExplosion(), n.getAffectedEntities()));
        Binding.put(table, ExplosionKnockbackEvent.class,
                net.neoforged.neoforge.event.level.ExplosionKnockbackEvent.class,
                NeoExplosionKnockbackEvent::new);
        Binding.put(table, RegisterCommandsEvent.class, net.neoforged.neoforge.event.RegisterCommandsEvent.class,
                n -> new RegisterCommandsEvent(n.getDispatcher(), n.getCommandSelection(), n.getBuildContext()));
        Binding.put(table, OnDatapackSyncEvent.class, net.neoforged.neoforge.event.OnDatapackSyncEvent.class,
                n -> new OnDatapackSyncEvent(n.getPlayerList(), n.getPlayer()));
        Binding.put(table, ItemAttributeModifierEvent.class,
                net.neoforged.neoforge.event.ItemAttributeModifierEvent.class,
                NeoItemAttributeModifierEvent::new);
        Binding.put(table, FMLCommonSetupEvent.class, net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent.class,
                NeoFMLCommonSetupEvent::new);
    }

    private static final class NeoPlayerEventBreakSpeed extends PlayerEvent.BreakSpeed {
        private final net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed n;

        NeoPlayerEventBreakSpeed(net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed n) {
            super(n.getEntity(), n.getState(), n.getOriginalSpeed(), n.getPosition().orElse(null));
            this.n = n;
        }

        @Override
        public float getNewSpeed() {
            return n.getNewSpeed();
        }

        @Override
        public void setNewSpeed(float newSpeed) {
            n.setNewSpeed(newSpeed);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoPlayerEventPlayerChangeGameModeEvent extends PlayerEvent.PlayerChangeGameModeEvent {
        private final net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangeGameModeEvent n;

        NeoPlayerEventPlayerChangeGameModeEvent(
                net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangeGameModeEvent n) {
            super(n.getEntity(), n.getCurrentGameMode(), n.getNewGameMode());
            this.n = n;
        }

        @Override
        public GameType getNewGameMode() {
            return n.getNewGameMode();
        }

        @Override
        public void setNewGameMode(GameType newGameMode) {
            n.setNewGameMode(newGameMode);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoPlayerInteractEventEntityInteractSpecific
            extends PlayerInteractEvent.EntityInteractSpecific {
        private final net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific n;

        NeoPlayerInteractEventEntityInteractSpecific(
                net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific n) {
            super(n.getEntity(), n.getHand(), n.getTarget(), n.getLocalPos());
            this.n = n;
        }

        @Override
        public InteractionResult getCancellationResult() {
            return n.getCancellationResult();
        }

        @Override
        public void setCancellationResult(InteractionResult cancellationResult) {
            n.setCancellationResult(cancellationResult);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoPlayerInteractEventEntityInteract extends PlayerInteractEvent.EntityInteract {
        private final net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract n;

        NeoPlayerInteractEventEntityInteract(
                net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract n) {
            super(n.getEntity(), n.getHand(), n.getTarget());
            this.n = n;
        }

        @Override
        public InteractionResult getCancellationResult() {
            return n.getCancellationResult();
        }

        @Override
        public void setCancellationResult(InteractionResult cancellationResult) {
            n.setCancellationResult(cancellationResult);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoPlayerInteractEventRightClickBlock extends PlayerInteractEvent.RightClickBlock {
        private final net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock n;

        NeoPlayerInteractEventRightClickBlock(
                net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock n) {
            super(n.getEntity(), n.getHand(), n.getPos(), n.getHitVec());
            this.n = n;
        }

        @Override
        public TriState getUseBlock() {
            return TriState.valueOf(n.getUseBlock().name());
        }

        @Override
        public void setUseBlock(TriState useBlock) {
            n.setUseBlock(net.neoforged.neoforge.common.util.TriState.valueOf(useBlock.name()));
        }

        @Override
        public TriState getUseItem() {
            return TriState.valueOf(n.getUseItem().name());
        }

        @Override
        public void setUseItem(TriState useItem) {
            n.setUseItem(net.neoforged.neoforge.common.util.TriState.valueOf(useItem.name()));
        }

        @Override
        public InteractionResult getCancellationResult() {
            return n.getCancellationResult();
        }

        @Override
        public void setCancellationResult(InteractionResult cancellationResult) {
            n.setCancellationResult(cancellationResult);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoPlayerInteractEventRightClickItem extends PlayerInteractEvent.RightClickItem {
        private final net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem n;

        NeoPlayerInteractEventRightClickItem(
                net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem n) {
            super(n.getEntity(), n.getHand());
            this.n = n;
        }

        @Override
        public InteractionResult getCancellationResult() {
            return n.getCancellationResult();
        }

        @Override
        public void setCancellationResult(InteractionResult cancellationResult) {
            n.setCancellationResult(cancellationResult);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoPlayerInteractEventLeftClickBlock extends PlayerInteractEvent.LeftClickBlock {
        private final net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock n;

        NeoPlayerInteractEventLeftClickBlock(
                net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock n) {
            super(n.getEntity(), n.getPos(), n.getFace(), Action.valueOf(n.getAction().name()));
            this.n = n;
        }

        @Override
        public TriState getUseBlock() {
            return TriState.valueOf(n.getUseBlock().name());
        }

        @Override
        public void setUseBlock(TriState useBlock) {
            n.setUseBlock(net.neoforged.neoforge.common.util.TriState.valueOf(useBlock.name()));
        }

        @Override
        public TriState getUseItem() {
            return TriState.valueOf(n.getUseItem().name());
        }

        @Override
        public void setUseItem(TriState useItem) {
            n.setUseItem(net.neoforged.neoforge.common.util.TriState.valueOf(useItem.name()));
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoAttackEntityEvent extends AttackEntityEvent {
        private final net.neoforged.neoforge.event.entity.player.AttackEntityEvent n;

        NeoAttackEntityEvent(net.neoforged.neoforge.event.entity.player.AttackEntityEvent n) {
            super(n.getEntity(), n.getTarget());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoCanPlayerSleepEvent extends CanPlayerSleepEvent {
        private final net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent n;

        NeoCanPlayerSleepEvent(net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent n) {
            super(n.getEntity(), n.getPos(), n.getVanillaProblem());
            this.n = n;
        }

        @Override
        public Player.BedSleepingProblem getProblem() {
            return n.getProblem();
        }

        @Override
        public void setProblem(Player.BedSleepingProblem problem) {
            n.setProblem(problem);
        }
    }

    private static final class NeoPlayerRespawnPositionEvent extends PlayerRespawnPositionEvent {
        private final net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent n;

        NeoPlayerRespawnPositionEvent(net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent n) {
            super(n.getEntity(), n.getOriginalDimensionTransition(), n.isFromEndFight());
            this.n = n;
        }

        @Override
        public DimensionTransition getDimensionTransition() {
            return n.getDimensionTransition();
        }

        @Override
        public void setDimensionTransition(DimensionTransition dimensionTransition) {
            n.setDimensionTransition(dimensionTransition);
        }

        @Override
        public boolean copyOriginalSpawnPosition() {
            return n.copyOriginalSpawnPosition();
        }

        @Override
        public void setCopyOriginalSpawnPosition(boolean copyOriginalSpawnPosition) {
            n.setCopyOriginalSpawnPosition(copyOriginalSpawnPosition);
        }
    }

    private static final class NeoPlayerSetSpawnEvent extends PlayerSetSpawnEvent {
        private final net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent n;

        NeoPlayerSetSpawnEvent(net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent n) {
            super(n.getEntity(), n.getSpawnLevel(), n.getNewSpawn(), n.isForced());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoItemEntityPickupEventPre extends ItemEntityPickupEvent.Pre {
        private final net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre n;

        NeoItemEntityPickupEventPre(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre n) {
            super(n.getPlayer(), n.getItemEntity());
            this.n = n;
        }

        @Override
        public TriState canPickup() {
            return TriState.valueOf(n.canPickup().name());
        }

        @Override
        public void setCanPickup(TriState canPickup) {
            n.setCanPickup(net.neoforged.neoforge.common.util.TriState.valueOf(canPickup.name()));
        }
    }

    private static final class NeoItemTossEvent extends ItemTossEvent {
        private final net.neoforged.neoforge.event.entity.item.ItemTossEvent n;

        NeoItemTossEvent(net.neoforged.neoforge.event.entity.item.ItemTossEvent n) {
            super(n.getEntity(), n.getPlayer());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoLivingIncomingDamageEvent extends LivingIncomingDamageEvent {
        private final net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent n;

        NeoLivingIncomingDamageEvent(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent n) {
            super(n.getEntity(), n.getSource(), n.getAmount());
            this.n = n;
        }

        @Override
        public float getAmount() {
            return n.getAmount();
        }

        @Override
        public void setAmount(float amount) {
            n.setAmount(amount);
        }

        @Override
        public int getInvulnerabilityTicks() {
            return n.getContainer().getPostAttackInvulnerabilityTicks();
        }

        @Override
        public void setInvulnerabilityTicks(int invulnerabilityTicks) {
            n.setInvulnerabilityTicks(invulnerabilityTicks);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }

        @Override
        public void addReductionModifier(Reduction type, ReductionModifier modifier) {
            n.addReductionModifier(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.valueOf(
                    type.name()), (container, reduction) -> modifier.modify(this, reduction));
        }
    }

    private static final class NeoLivingDamageEventPre extends LivingDamageEvent.Pre {
        private final net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre n;

        NeoLivingDamageEventPre(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre n) {
            super(n.getEntity(), n.getSource(), n.getOriginalDamage(), n.getNewDamage());
            this.n = n;
        }

        @Override
        public float getNewDamage() {
            return n.getNewDamage();
        }

        @Override
        public void setNewDamage(float newDamage) {
            n.setNewDamage(newDamage);
        }
    }

    private static final class NeoLivingDeathEvent extends LivingDeathEvent {
        private final net.neoforged.neoforge.event.entity.living.LivingDeathEvent n;

        NeoLivingDeathEvent(net.neoforged.neoforge.event.entity.living.LivingDeathEvent n) {
            super(n.getEntity(), n.getSource());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoLivingChangeTargetEvent extends LivingChangeTargetEvent {
        private final net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent n;

        NeoLivingChangeTargetEvent(net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent n) {
            super(n.getEntity(), n.getOriginalAboutToBeSetTarget());
            this.n = n;
        }

        @Override
        public LivingEntity getNewAboutToBeSetTarget() {
            return n.getNewAboutToBeSetTarget();
        }

        @Override
        public void setNewAboutToBeSetTarget(LivingEntity newAboutToBeSetTarget) {
            n.setNewAboutToBeSetTarget(newAboutToBeSetTarget);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoLivingDestroyBlockEvent extends LivingDestroyBlockEvent {
        private final net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent n;

        NeoLivingDestroyBlockEvent(net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent n) {
            super(n.getEntity(), n.getPos(), n.getState());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoLivingDropsEvent extends LivingDropsEvent {
        private final net.neoforged.neoforge.event.entity.living.LivingDropsEvent n;

        NeoLivingDropsEvent(net.neoforged.neoforge.event.entity.living.LivingDropsEvent n) {
            super(n.getEntity(), n.getSource(), n.getDrops(), n.isRecentlyHit());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoLivingEntityUseItemEventStart extends LivingEntityUseItemEvent.Start {
        private final net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Start n;

        NeoLivingEntityUseItemEventStart(net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Start n) {
            super(n.getEntity(), n.getItem(), n.getHand(), n.getDuration());
            this.n = n;
        }

        @Override
        public int getDuration() {
            return n.getDuration();
        }

        @Override
        public void setDuration(int duration) {
            n.setDuration(duration);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoLivingEntityUseItemEventTick extends LivingEntityUseItemEvent.Tick {
        private final net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Tick n;

        NeoLivingEntityUseItemEventTick(net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Tick n) {
            super(n.getEntity(), n.getItem(), n.getHand(), n.getDuration());
            this.n = n;
        }

        @Override
        public int getDuration() {
            return n.getDuration();
        }

        @Override
        public void setDuration(int duration) {
            n.setDuration(duration);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoLivingExperienceDropEvent extends LivingExperienceDropEvent {
        private final net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent n;

        NeoLivingExperienceDropEvent(net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent n) {
            super(n.getEntity(), n.getAttackingPlayer(), n.getOriginalExperience());
            this.n = n;
        }

        @Override
        public int getDroppedExperience() {
            return n.getDroppedExperience();
        }

        @Override
        public void setDroppedExperience(int droppedExperience) {
            n.setDroppedExperience(droppedExperience);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoLivingFallEvent extends LivingFallEvent {
        private final net.neoforged.neoforge.event.entity.living.LivingFallEvent n;

        NeoLivingFallEvent(net.neoforged.neoforge.event.entity.living.LivingFallEvent n) {
            super(n.getEntity(), n.getDistance(), n.getDamageMultiplier());
            this.n = n;
        }

        @Override
        public float getDistance() {
            return n.getDistance();
        }

        @Override
        public void setDistance(float distance) {
            n.setDistance(distance);
        }

        @Override
        public float getDamageMultiplier() {
            return n.getDamageMultiplier();
        }

        @Override
        public void setDamageMultiplier(float damageMultiplier) {
            n.setDamageMultiplier(damageMultiplier);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoLivingKnockBackEvent extends LivingKnockBackEvent {
        private final net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent n;

        NeoLivingKnockBackEvent(net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent n) {
            super(n.getEntity(), n.getStrength(), n.getRatioX(), n.getRatioZ());
            this.n = n;
        }

        @Override
        public float getStrength() {
            return n.getStrength();
        }

        @Override
        public void setStrength(float strength) {
            n.setStrength(strength);
        }

        @Override
        public double getRatioX() {
            return n.getRatioX();
        }

        @Override
        public void setRatioX(double ratioX) {
            n.setRatioX(ratioX);
        }

        @Override
        public double getRatioZ() {
            return n.getRatioZ();
        }

        @Override
        public void setRatioZ(double ratioZ) {
            n.setRatioZ(ratioZ);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoLivingShieldBlockEvent extends LivingShieldBlockEvent {
        private final net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent n;

        NeoLivingShieldBlockEvent(net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent n) {
            super(n.getEntity(), n.getDamageSource(), n.getOriginalBlock());
            this.n = n;
        }

        @Override
        public boolean getBlocked() {
            return n.getBlocked();
        }

        @Override
        public void setBlocked(boolean blocked) {
            n.setBlocked(blocked);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoMobEffectEventApplicable extends MobEffectEvent.Applicable {
        private final net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable n;

        NeoMobEffectEventApplicable(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable n) {
            super(n.getEntity(), n.getEffectInstance(), n.getEffectSource());
            this.n = n;
        }

        @Override
        public Result getResult() {
            return Result.valueOf(n.getResult().name());
        }

        @Override
        public void setResult(Result result) {
            n.setResult(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable.Result.valueOf(
                    result.name()));
        }
    }

    private static final class NeoMobEffectEventExpired extends MobEffectEvent.Expired {
        private final net.neoforged.neoforge.event.entity.living.MobEffectEvent.Expired n;

        NeoMobEffectEventExpired(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Expired n) {
            super(n.getEntity(), n.getEffectInstance());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoMobEffectEventRemove extends MobEffectEvent.Remove {
        private final net.neoforged.neoforge.event.entity.living.MobEffectEvent.Remove n;

        NeoMobEffectEventRemove(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Remove n) {
            super(n.getEntity(), n.getEffectInstance(), n.getEffect());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoMobSpawnEventPositionCheck extends MobSpawnEvent.PositionCheck {
        private final net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck n;

        NeoMobSpawnEventPositionCheck(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck n) {
            super(n.getEntity(), n.getLevel(), n.getSpawnType(), n.getSpawner());
            this.n = n;
        }

        @Override
        public Result getResult() {
            return Result.valueOf(n.getResult().name());
        }

        @Override
        public void setResult(Result result) {
            n.setResult(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck.Result.valueOf(
                    result.name()));
        }
    }

    private static final class NeoMobSpawnEventSpawnPlacementCheck extends MobSpawnEvent.SpawnPlacementCheck {
        private final net.neoforged.neoforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck n;

        NeoMobSpawnEventSpawnPlacementCheck(
                net.neoforged.neoforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck n) {
            super(n.getEntityType(), n.getLevel(), n.getSpawnType(), n.getPos(), n.getRandom(), n.getDefaultResult());
            this.n = n;
        }

        @Override
        public Result getResult() {
            return Result.valueOf(n.getResult().name());
        }

        @Override
        public void setResult(Result result) {
            n.setResult(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck.Result.valueOf(
                    result.name()));
        }
    }

    private static final class NeoFinalizeSpawnEvent extends FinalizeSpawnEvent {
        private final net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent n;

        NeoFinalizeSpawnEvent(net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent n) {
            super(n.getEntity(), n.getLevel(), n.getX(), n.getY(), n.getZ(), n.getDifficulty(), n.getSpawnType(),
                    n.getSpawnData());
            this.n = n;
        }

        @Override
        public SpawnGroupData getSpawnData() {
            return n.getSpawnData();
        }

        @Override
        public void setSpawnData(SpawnGroupData spawnData) {
            n.setSpawnData(spawnData);
        }

        @Override
        public boolean isSpawnCancelled() {
            return n.isSpawnCancelled();
        }

        @Override
        public void setSpawnCancelled(boolean spawnCancelled) {
            n.setSpawnCancelled(spawnCancelled);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoEntityJoinLevelEvent extends EntityJoinLevelEvent {
        private final net.neoforged.neoforge.event.entity.EntityJoinLevelEvent n;

        NeoEntityJoinLevelEvent(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent n) {
            super(n.getEntity(), n.getLevel(), n.loadedFromDisk());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoEntityMobGriefingEvent extends EntityMobGriefingEvent {
        private final net.neoforged.neoforge.event.entity.EntityMobGriefingEvent n;

        NeoEntityMobGriefingEvent(net.neoforged.neoforge.event.entity.EntityMobGriefingEvent n) {
            super(n.getEntity(), n.getEntity().level());
            this.n = n;
        }

        @Override
        public boolean canGrief() {
            return n.canGrief();
        }

        @Override
        public void setCanGrief(boolean canGrief) {
            n.setCanGrief(canGrief);
        }
    }

    private static final class NeoEntityMountEvent extends EntityMountEvent {
        private final net.neoforged.neoforge.event.entity.EntityMountEvent n;

        NeoEntityMountEvent(net.neoforged.neoforge.event.entity.EntityMountEvent n) {
            super(n.getEntity(), n.getEntityBeingMounted(), n.getLevel(), n.isMounting());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoEntityStruckByLightningEvent extends EntityStruckByLightningEvent {
        private final net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent n;

        NeoEntityStruckByLightningEvent(net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent n) {
            super(n.getEntity(), n.getLightning());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoEntityTeleportEventChorusFruit extends EntityTeleportEvent.ChorusFruit {
        private final net.neoforged.neoforge.event.entity.EntityTeleportEvent.ChorusFruit n;

        NeoEntityTeleportEventChorusFruit(net.neoforged.neoforge.event.entity.EntityTeleportEvent.ChorusFruit n) {
            super(n.getEntityLiving(), n.getTargetX(), n.getTargetY(), n.getTargetZ());
            this.n = n;
        }

        @Override
        public double getTargetX() {
            return n.getTargetX();
        }

        @Override
        public void setTargetX(double targetX) {
            n.setTargetX(targetX);
        }

        @Override
        public double getTargetY() {
            return n.getTargetY();
        }

        @Override
        public void setTargetY(double targetY) {
            n.setTargetY(targetY);
        }

        @Override
        public double getTargetZ() {
            return n.getTargetZ();
        }

        @Override
        public void setTargetZ(double targetZ) {
            n.setTargetZ(targetZ);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoEntityTeleportEventEnderEntity extends EntityTeleportEvent.EnderEntity {
        private final net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderEntity n;

        NeoEntityTeleportEventEnderEntity(net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderEntity n) {
            super(n.getEntityLiving(), n.getTargetX(), n.getTargetY(), n.getTargetZ());
            this.n = n;
        }

        @Override
        public double getTargetX() {
            return n.getTargetX();
        }

        @Override
        public void setTargetX(double targetX) {
            n.setTargetX(targetX);
        }

        @Override
        public double getTargetY() {
            return n.getTargetY();
        }

        @Override
        public void setTargetY(double targetY) {
            n.setTargetY(targetY);
        }

        @Override
        public double getTargetZ() {
            return n.getTargetZ();
        }

        @Override
        public void setTargetZ(double targetZ) {
            n.setTargetZ(targetZ);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoEntityTeleportEventEnderPearl extends EntityTeleportEvent.EnderPearl {
        private final net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderPearl n;

        NeoEntityTeleportEventEnderPearl(net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderPearl n) {
            super(n.getPlayer(), n.getTargetX(), n.getTargetY(), n.getTargetZ(), n.getPearlEntity(),
                    n.getAttackDamage(), n.getHitResult());
            this.n = n;
        }

        @Override
        public double getTargetX() {
            return n.getTargetX();
        }

        @Override
        public void setTargetX(double targetX) {
            n.setTargetX(targetX);
        }

        @Override
        public double getTargetY() {
            return n.getTargetY();
        }

        @Override
        public void setTargetY(double targetY) {
            n.setTargetY(targetY);
        }

        @Override
        public double getTargetZ() {
            return n.getTargetZ();
        }

        @Override
        public void setTargetZ(double targetZ) {
            n.setTargetZ(targetZ);
        }

        @Override
        public float getAttackDamage() {
            return n.getAttackDamage();
        }

        @Override
        public void setAttackDamage(float attackDamage) {
            n.setAttackDamage(attackDamage);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoEntityTravelToDimensionEvent extends EntityTravelToDimensionEvent {
        private final net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent n;

        NeoEntityTravelToDimensionEvent(net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent n) {
            super(n.getEntity(), n.getDimension());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoProjectileImpactEvent extends ProjectileImpactEvent {
        private final net.neoforged.neoforge.event.entity.ProjectileImpactEvent n;

        NeoProjectileImpactEvent(net.neoforged.neoforge.event.entity.ProjectileImpactEvent n) {
            super(n.getProjectile(), n.getRayTraceResult());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoEntityTickEventPre extends EntityTickEvent.Pre {
        private final net.neoforged.neoforge.event.tick.EntityTickEvent.Pre n;

        NeoEntityTickEventPre(net.neoforged.neoforge.event.tick.EntityTickEvent.Pre n) {
            super(n.getEntity());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoBlockEventBreakEvent extends BlockEvent.BreakEvent {
        private final net.neoforged.neoforge.event.level.BlockEvent.BreakEvent n;

        NeoBlockEventBreakEvent(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent n) {
            super(n.getLevel(), n.getPos(), n.getState(), n.getPlayer());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoBlockEventEntityPlaceEvent extends BlockEvent.EntityPlaceEvent {
        private final net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent n;

        NeoBlockEventEntityPlaceEvent(net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent n) {
            super(n.getLevel(), n.getPos(), n.getPlacedBlock(), n.getPlacedAgainst(), n.getEntity());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoBlockEventFarmlandTrampleEvent extends BlockEvent.FarmlandTrampleEvent {
        private final net.neoforged.neoforge.event.level.BlockEvent.FarmlandTrampleEvent n;

        NeoBlockEventFarmlandTrampleEvent(net.neoforged.neoforge.event.level.BlockEvent.FarmlandTrampleEvent n) {
            super(n.getLevel(), n.getPos(), n.getState(), n.getFallDistance(), n.getEntity());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoBlockEventFluidPlaceBlockEvent extends BlockEvent.FluidPlaceBlockEvent {
        private final net.neoforged.neoforge.event.level.BlockEvent.FluidPlaceBlockEvent n;

        NeoBlockEventFluidPlaceBlockEvent(net.neoforged.neoforge.event.level.BlockEvent.FluidPlaceBlockEvent n) {
            super(n.getLevel(), n.getPos(), n.getLiquidPos(), n.getState());
            this.n = n;
        }

        @Override
        public BlockState getNewState() {
            return n.getNewState();
        }

        @Override
        public void setNewState(BlockState newState) {
            n.setNewState(newState);
        }

        @Override
        public BlockState getOriginalState() {
            return n.getOriginalState();
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoBlockEventNeighborNotifyEvent extends BlockEvent.NeighborNotifyEvent {
        private final net.neoforged.neoforge.event.level.BlockEvent.NeighborNotifyEvent n;

        NeoBlockEventNeighborNotifyEvent(net.neoforged.neoforge.event.level.BlockEvent.NeighborNotifyEvent n) {
            super(n.getLevel(), n.getPos(), n.getState(), n.getNotifiedSides(), n.getForceRedstoneUpdate());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoBlockEventPortalSpawnEvent extends BlockEvent.PortalSpawnEvent {
        private final net.neoforged.neoforge.event.level.BlockEvent.PortalSpawnEvent n;

        NeoBlockEventPortalSpawnEvent(net.neoforged.neoforge.event.level.BlockEvent.PortalSpawnEvent n) {
            super(n.getLevel(), n.getPos(), n.getState(), n.getPortalSize());
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoBlockEventBlockToolModificationEvent extends BlockEvent.BlockToolModificationEvent {
        private final net.neoforged.neoforge.event.level.BlockEvent.BlockToolModificationEvent n;

        NeoBlockEventBlockToolModificationEvent(
                net.neoforged.neoforge.event.level.BlockEvent.BlockToolModificationEvent n) {
            super(n.getState(), n.getContext(), n.isSimulated());
            this.n = n;
        }

        @Override
        public BlockState getFinalState() {
            return n.getFinalState();
        }

        @Override
        public void setFinalState(BlockState finalState) {
            n.setFinalState(finalState);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoBlockDropsEvent extends BlockDropsEvent {
        private final net.neoforged.neoforge.event.level.BlockDropsEvent n;

        NeoBlockDropsEvent(net.neoforged.neoforge.event.level.BlockDropsEvent n) {
            super(n.getLevel(), n.getPos(), n.getState(), n.getBlockEntity(), n.getDrops(), n.getBreaker(),
                    n.getTool());
            this.n = n;
        }

        @Override
        public int getDroppedExperience() {
            return n.getDroppedExperience();
        }

        @Override
        public void setDroppedExperience(int droppedExperience) {
            n.setDroppedExperience(droppedExperience);
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoPistonEventPre extends PistonEvent.Pre {
        private final net.neoforged.neoforge.event.level.PistonEvent.Pre n;

        NeoPistonEventPre(net.neoforged.neoforge.event.level.PistonEvent.Pre n) {
            super(n.getLevel(), n.getPos(), n.getDirection(), PistonMoveType.valueOf(n.getPistonMoveType().name()));
            this.n = n;
        }

        @Override
        public boolean isCanceled() {
            return n.isCanceled();
        }

        @Override
        public void setCanceled(boolean canceled) {
            n.setCanceled(canceled);
        }
    }

    private static final class NeoExplosionKnockbackEvent extends ExplosionKnockbackEvent {
        private final net.neoforged.neoforge.event.level.ExplosionKnockbackEvent n;

        NeoExplosionKnockbackEvent(net.neoforged.neoforge.event.level.ExplosionKnockbackEvent n) {
            super(n.getLevel(), n.getExplosion(), n.getAffectedEntity(), n.getKnockbackVelocity());
            this.n = n;
        }

        @Override
        public Vec3 getKnockbackVelocity() {
            return n.getKnockbackVelocity();
        }

        @Override
        public void setKnockbackVelocity(Vec3 knockbackVelocity) {
            n.setKnockbackVelocity(knockbackVelocity);
        }
    }

    private static final class NeoItemAttributeModifierEvent extends ItemAttributeModifierEvent {
        private final net.neoforged.neoforge.event.ItemAttributeModifierEvent n;

        NeoItemAttributeModifierEvent(net.neoforged.neoforge.event.ItemAttributeModifierEvent n) {
            super(n.getItemStack(), n.getDefaultModifiers());
            this.n = n;
        }

        @Override
        public List<ItemAttributeModifiers.Entry> getModifiers() {
            return n.getModifiers();
        }

        @Override
        public void replaceModifier(Holder<Attribute> attribute, AttributeModifier modifier, EquipmentSlotGroup slot) {
            n.replaceModifier(attribute, modifier, slot);
        }

        @Override
        public ItemAttributeModifiers build() {
            return n.build();
        }
    }

    private static final class NeoFMLCommonSetupEvent extends FMLCommonSetupEvent {
        private final net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent n;

        NeoFMLCommonSetupEvent(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent n) {
            super();
            this.n = n;
        }

        @Override
        public CompletableFuture<Void> enqueueWork(Runnable work) {
            return n.enqueueWork(work);
        }
    }
}
