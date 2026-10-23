package com.kolodkingal.enhanced.asgard;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.asgard.build.CityTemplates;
import com.kolodkingal.enhanced.bifrost.Bifrost;
import com.kolodkingal.enhanced.block.asgard.BifrostMechanismBlockEntity;
import com.kolodkingal.enhanced.config.AsgardConfig;
import com.kolodkingal.enhanced.network.ThorFxPayload;
import com.kolodkingal.enhanced.platform.net.Net;
import com.kolodkingal.enhanced.registry.ModAttachments;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The rules of Asgard (3.5): safe to explore. The city is protected ({@link #protectsCity}: breaking, placing, buckets,
 * fire, explosions, pistons, creatures' griefing; the wild land and the sea are free; operators in Creative may edit);
 * no monster is born there by nature; a fall into the void is caught by Heimdall (the player is set down in the
 * Observatory, unhurt); a soft edge pushes fliers back; a bed lets one sleep but never sets the spawn; who dies there
 * keeps his inventory ({@code asgard.keepInventory}; with it off, still the keys of the Bifrost and Hela's Headdress); who
 * logs in there while Asgard is closed is sent home. Also: the progress of those who come ({@code VISITED}, where they
 * came from), and the Observatory's mechanism, which spins when the Bifrost touches down or leaves near it.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class AsgardRules {
    /** How far from the Observatory's centre the Bifrost spins the mechanism. */
    public static final double MECHANISM_REACH = 24.0;
    /** The ways monsters are born by nature (refused in Asgard); eggs, spawners, commands and the mod's own pass. */
    private static final EnumSet<MobSpawnType> NATURE = EnumSet.of(MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION,
            MobSpawnType.STRUCTURE, MobSpawnType.PATROL, MobSpawnType.REINFORCEMENT, MobSpawnType.EVENT);

    /** What a player who died in Asgard keeps until he comes back (server thread). */
    private record Kept(@Nullable ListTag inventory, int level, float progress, int total, List<ItemStack> keys) {
    }

    private static final Map<UUID, Kept> KEPT = new HashMap<>();
    /** Fires that broke out in the protected city, put out at the end of the tick. */
    private static final Map<ServerLevel, Set<BlockPos>> FIRES = new HashMap<>();

    static {
        Bifrost.ARRIVED.add((level, at, traveller) -> spinNear(level, at));
        Bifrost.DEPARTED.add((level, at, traveller) -> spinNear(level, at));
    }

    // ---- the protected city ----

    private static boolean protectOn() {
        return !EnhancedConfig.SPEC.isLoaded() || AsgardConfig.PROTECT_CITY.get();
    }

    /**
     * Whether the block at {@code pos} belongs to the protected city of Asgard ({@code asgard.protectCity}) and may not be
     * changed by {@code editor} (null: no player, an explosion or a creature). Operators in Creative may edit.
     */
    public static boolean protectsCity(@Nullable Player editor, ServerLevel level, BlockPos pos) {
        if (!AsgardDimension.is(level) || !protectOn() || !CityPlan.isProtected(pos)) {
            return false;
        }
        return editor == null || !(editor.isCreative() && editor.hasPermissions(2));
    }

    private static boolean refuses(@Nullable Entity who, Level level, BlockPos pos) {
        return level instanceof ServerLevel server && protectsCity(who instanceof Player p ? p : null, server, pos);
    }

    private static void tell(@Nullable Entity who) {
        if (who instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.enhanced.asgard.protected").withStyle(ChatFormatting.GOLD), true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level && refuses(event.getPlayer(), level, event.getPos())) {
            event.setCanceled(true);
            tell(event.getPlayer());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level) || !AsgardDimension.is(level)) {
            return;
        }
        if (event.getPlacedBlock().is(Blocks.RESPAWN_ANCHOR) && !(event.getEntity() instanceof Player p && p.hasPermissions(2) && p.isCreative())) {
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("message.enhanced.asgard.no_spawn").withStyle(ChatFormatting.GOLD), true);
            }
            return;
        }
        if (refuses(event.getEntity(), level, event.getPos())) {
            event.setCanceled(true);
            tell(event.getEntity());
        }
    }

    /** Buckets, flint and steel and fire charges used on the city. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onUseOnBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof BucketItem || stack.getItem() instanceof FlintAndSteelItem || stack.getItem() instanceof FireChargeItem)) {
            return;
        }
        BlockPos pos = event.getPos();
        Direction face = event.getFace() == null ? Direction.UP : event.getFace();
        if (refuses(event.getEntity(), event.getLevel(), pos) || refuses(event.getEntity(), event.getLevel(), pos.relative(face))) {
            event.setCanceled(true);
            tell(event.getEntity());
        }
    }

    /** A bucket emptied or filled toward the city (it aims by itself). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getItemStack().getItem() instanceof BucketItem) || !AsgardDimension.is(event.getLevel())) {
            return;
        }
        Player player = event.getEntity();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(player.blockInteractionRange()));
        BlockHitResult hit = event.getLevel().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
        if (hit.getType() == HitResult.Type.BLOCK && (refuses(player, event.getLevel(), hit.getBlockPos())
                || refuses(player, event.getLevel(), hit.getBlockPos().relative(hit.getDirection())))) {
            event.setCanceled(true);
            tell(player);
        }
    }

    @SubscribeEvent
    public static void onToolUse(BlockEvent.BlockToolModificationEvent event) {
        if (event.getContext().getLevel() instanceof Level level && refuses(event.getPlayer(), level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getLevel() instanceof Level level && refuses(null, level, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (event.getLevel() instanceof Level level && refuses(null, level, event.getPos())) {
            event.setNewState(event.getOriginalState());
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level && AsgardDimension.is(level)) {
            event.getAffectedBlocks().removeIf(pos -> protectsCity(null, level, pos));
        }
    }

    @SubscribeEvent
    public static void onPiston(PistonEvent.Pre event) {
        if (event.getLevel() instanceof Level level && (refuses(null, level, event.getPos()) || refuses(null, level, event.getFaceOffsetPos()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onGrief(EntityMobGriefingEvent event) {
        Entity e = event.getEntity();
        if (e != null && e.level() instanceof ServerLevel level && AsgardDimension.is(level) && protectOn()
                && CityPlan.isProtected(e.blockPosition())) {
            event.setCanGrief(false);
        }
    }

    @SubscribeEvent
    public static void onDestroy(LivingDestroyBlockEvent event) {
        if (refuses(null, event.getEntity().level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /** Fire that appears in the city (from lightning or anything that sets it without asking) goes out at once. */
    @SubscribeEvent
    public static void onNeighbours(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel() instanceof ServerLevel level && AsgardDimension.is(level) && event.getState().getBlock() instanceof BaseFireBlock
                && protectsCity(null, level, event.getPos())) {
            FIRES.computeIfAbsent(level, k -> new HashSet<>()).add(event.getPos().immutable());
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel asgard && AsgardDimension.is(asgard)) {
            FeastHall.tick(asgard); // 3.7.2: mends once a Hall of Feasts built before
        }
        if (event.getLevel() instanceof ServerLevel level && !FIRES.isEmpty()) {
            Set<BlockPos> fires = FIRES.remove(level);
            if (fires != null) {
                for (BlockPos pos : fires) {
                    if (level.getBlockState(pos).getBlock() instanceof BaseFireBlock) {
                        level.removeBlock(pos, false);
                    }
                }
            }
        }
    }

    // ---- no monsters by nature ----

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (AsgardDimension.is(event.getLevel().getLevel()) && event.getEntity().getType().getCategory() == MobCategory.MONSTER
                && NATURE.contains(event.getSpawnType())) {
            event.setSpawnCancelled(true);
        }
    }

    // ---- the void, the edge ----

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !AsgardDimension.in(player) || player.isSpectator()) {
            return;
        }
        int catchY = EnhancedConfig.SPEC.isLoaded() ? AsgardConfig.VOID_CATCH_Y.get() : -56;
        if (player.getY() < catchY) {
            catchFall(player);
            return;
        }
        if (player.tickCount % 20 == 0) {
            pushFromEdge(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onVoidDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && AsgardDimension.in(player)
                && event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD) && !player.isSpectator()) {
            event.setCanceled(true);
            catchFall(player);
        }
    }

    /** Heimdall catches a player who fell into the void: the Bifrost sets him down in the Observatory, unhurt. */
    public static void catchFall(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 at = CityPlan.ARRIVAL;
        player.stopRiding();
        player.teleportTo(level, at.x, at.y, at.z, CityPlan.ARRIVAL_YAW, 0.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.hurtMarked = true;
        player.displayClientMessage(Component.translatable("message.enhanced.asgard.rescued").withStyle(ChatFormatting.GOLD), true);
        level.playSound(null, at.x, at.y, at.z, ModSounds.BIFROST_ARRIVE.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
        Net.sendToPlayersNear(level, null, at.x, at.y, at.z, 128,
                new ThorFxPayload(ThorFxPayload.BIFROST_ARRIVAL, player.getId(), List.of(player.position())));
        Bifrost.mark(level, player.position());
        spinNear(level, player.position());
    }

    /** Beyond {@code asgard.edgeRadius}, a player is pushed back toward the city. Returns whether he was. */
    public static boolean pushFromEdge(ServerPlayer player) {
        double edge = EnhancedConfig.SPEC.isLoaded() ? AsgardConfig.EDGE_RADIUS.get() : 640;
        double r = Math.hypot(player.getX(), player.getZ());
        if (r <= edge) {
            return false;
        }
        Vec3 in = new Vec3(-player.getX() / r, 0, -player.getZ() / r);
        player.setDeltaMovement(in.scale(1.6).add(0, 0.3, 0));
        player.hurtMarked = true;
        player.displayClientMessage(Component.translatable("message.enhanced.asgard.edge").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    // ---- no spawn point ----

    @SubscribeEvent
    public static void onSetSpawn(PlayerSetSpawnEvent event) {
        if (event.getSpawnLevel() == AsgardDimension.KEY && event.getNewSpawn() != null && !event.isForced()) {
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("message.enhanced.asgard.no_spawn").withStyle(ChatFormatting.GOLD), true);
            }
        }
    }

    // ---- death ----

    private static boolean keepAll() {
        return !EnhancedConfig.SPEC.isLoaded() || AsgardConfig.KEEP_INVENTORY.get();
    }

    /** The items never lost in Asgard: the keys of the Bifrost and Hela's Headdress. */
    public static boolean alwaysKept(ItemStack stack) {
        return stack.is(ModItems.MJOLNIR.get()) || stack.is(ModItems.STORMBREAKER.get()) || stack.is(ModItems.HOFUND.get())
                || stack.is(ModItems.GUNGNIR.get()) || stack.is(ModItems.HELA_HEADDRESS.get());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player) || !AsgardDimension.in(player)
                || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        if (keepAll()) {
            KEPT.put(player.getUUID(), new Kept(player.getInventory().save(new ListTag()), player.experienceLevel,
                    player.experienceProgress, player.totalExperience, new ArrayList<>()));
        } else {
            KEPT.put(player.getUUID(), new Kept(null, 0, 0, 0, new ArrayList<>()));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Kept kept = KEPT.get(player.getUUID());
        if (kept == null) {
            return;
        }
        if (kept.inventory() != null) {
            event.setCanceled(true);
            return;
        }
        event.getDrops().removeIf(drop -> {
            if (alwaysKept(drop.getItem())) {
                kept.keys().add(drop.getItem().copy());
                return true;
            }
            return false;
        });
    }

    @SubscribeEvent
    public static void onXpDrop(LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && KEPT.get(player.getUUID()) instanceof Kept kept && kept.inventory() != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        Kept kept = KEPT.remove(event.getOriginal().getUUID());
        if (kept == null) {
            return;
        }
        Player player = event.getEntity();
        if (kept.inventory() != null) {
            player.getInventory().load(kept.inventory());
            player.experienceLevel = kept.level();
            player.experienceProgress = kept.progress();
            player.totalExperience = kept.total();
        }
        for (ItemStack key : kept.keys()) {
            if (!player.getInventory().add(key)) {
                player.drop(key, false);
            }
        }
    }

    // ---- who comes and goes ----

    /** Coming to Asgard (not from it): he has visited, and is remembered where he came from (Heimdall can send him back). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTravel(EntityTravelToDimensionEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player) || event.getDimension() != AsgardDimension.KEY
                || AsgardDimension.in(player)) {
            return;
        }
        AsgardProgress progress = ModAttachments.ASGARD.get(player);
        ModAttachments.ASGARD.set(player, progress.with(AsgardProgress.VISITED)
                .withCameFrom(GlobalPos.of(player.level().dimension(), player.blockPosition())));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && AsgardDimension.in(player) && !Bifrost.asgardEnabled()
                && !player.hasPermissions(2)) {
            player.displayClientMessage(Component.translatable("message.enhanced.asgard.closed").withStyle(ChatFormatting.GOLD), false);
            Bifrost.open(player, Bifrost.Destination.HOME, Bifrost.Source.RESCUE);
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        CityTemplates.clear();
        if (AsgardDimension.level(event.getServer()) != null) {
            AsgardState state = AsgardState.get(event.getServer());
            if (state.planVersion() == 0) {
                state.setPlanVersion(CityPlan.VERSION);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        CityTemplates.clear();
        KEPT.clear();
        FIRES.clear();
    }

    // ---- the mechanism ----

    /** The Bifrost touched down or left at {@code at}: the Observatory's mechanism spins up if it is near. */
    public static boolean spinNear(ServerLevel level, Vec3 at) {
        if (!AsgardDimension.is(level) || at.distanceTo(Vec3.atCenterOf(CityPlan.OBSERVATORY_CENTER)) > MECHANISM_REACH
                || !level.isLoaded(CityPlan.MECHANISM)
                || !(level.getBlockEntity(CityPlan.MECHANISM) instanceof BifrostMechanismBlockEntity mechanism)) {
            return false;
        }
        mechanism.spin(level.getGameTime());
        level.playSound(null, CityPlan.MECHANISM, ModSounds.BIFROST_MECHANISM_SPIN.get(), SoundSource.BLOCKS, 1.2F, 1.0F);
        return true;
    }

    private AsgardRules() {
    }
}
