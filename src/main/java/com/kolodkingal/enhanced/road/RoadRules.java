package com.kolodkingal.enhanced.road;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Set;

/**
 * What cannot be done in the Witches' Road, in one place (the lead's rule list, 3.2):
 * <ul>
 *     <li>Nothing hurts the player. A fall into the void, a command that kills, anything lethal: he is put back at his
 *     checkpoint ({@code LivingIncomingDamageEvent} first, {@code LivingDeathEvent} as the net, so he never dies in the Road
 *     and never respawns in the world).</li>
 *     <li>No blocks are placed or broken, except where the trial allows it ({@link RoadChallenge#mayPlace}/{@code mayBreak}).</li>
 *     <li>No teleports: Ender Pearls, Chorus Fruit, portals, the mod's own leaps (see {@link RoadTrials#allowsTeleport}); no
 *     gliding; no Levitation, Slow Falling or Jump Boost; no firework rockets or wind charges. An operator's {@code /tp} is
 *     free.</li>
 *     <li>The Scarlet Witch Gene cannot be dropped, so it is there to be spent at the end.</li>
 *     <li>The player is a Survivor inside (his game mode comes back when he leaves).</li>
 * </ul>
 * Powers need no rule of their own: only a player without powers walks the Road, and one who is given a power inside is taken
 * out ({@link RoadTrials#serverTick}).
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class RoadRules {
    /** Items that cannot be used in the Road: they would carry the player over a trial. */
    private static final Set<Item> BANNED_ITEMS = Set.of(Items.ENDER_PEARL, Items.CHORUS_FRUIT, Items.WIND_CHARGE, Items.FIREWORK_ROCKET);

    /** Whether the item cannot be used in the Road. */
    public static boolean isBanned(ItemStack stack) {
        return BANNED_ITEMS.contains(stack.getItem());
    }

    /** Every tick of a player in a trial. */
    static void enforce(ServerPlayer player) {
        if (player.isFallFlying()) {
            player.stopFallFlying();
        }
        if (player.hasEffect(MobEffects.LEVITATION)) {
            player.removeEffect(MobEffects.LEVITATION);
        }
        if (player.hasEffect(MobEffects.SLOW_FALLING)) {
            player.removeEffect(MobEffects.SLOW_FALLING);
        }
        if (player.hasEffect(MobEffects.JUMP)) {
            player.removeEffect(MobEffects.JUMP);
        }
        if (!player.gameMode.getGameModeForPlayer().equals(GameType.SURVIVAL)) {
            player.setGameMode(GameType.SURVIVAL);
        }
        if (player.getAbilities().mayfly || player.getAbilities().flying) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        if (player.tickCount % 20 == 0) {
            player.getFoodData().setFoodLevel(20);
            player.clearFire();
            player.setAirSupply(player.getMaxAirSupply());
        }
    }

    private static RoadTrial trialOf(Object entity) {
        return entity instanceof ServerPlayer player ? RoadTrials.trialOf(player.getUUID()) : null;
    }

    private static boolean inRoad(Object entity) {
        return entity instanceof ServerPlayer player && (RoadTrials.isInside(player) || RoadDimension.in(player));
    }

    // ---- damage and death ----

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !RoadTrials.isInside(player)) {
            return;
        }
        event.setCanceled(true);
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            RoadTrial trial = RoadTrials.trialOf(player.getUUID());
            if (trial != null) {
                trial.putBack(player, true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        RoadTrial trial = trialOf(event.getEntity());
        if (trial != null) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            event.setCanceled(true);
            player.setHealth(player.getMaxHealth());
            trial.putBack(player, true);
        }
    }

    // ---- blocks ----

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !RoadDimension.is(level)) {
            return;
        }
        RoadTrial trial = trialOf(event.getPlayer());
        if (trial == null || !trial.challenge().mayBreak(trial, (ServerPlayer) event.getPlayer(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !RoadDimension.is(level)) {
            return;
        }
        RoadTrial trial = trialOf(event.getEntity());
        if (trial == null || !trial.challenge().mayPlace(trial, (ServerPlayer) event.getEntity(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !RoadDimension.in(player)) {
            return;
        }
        RoadTrial trial = RoadTrials.trialOf(player.getUUID());
        if (trial == null || !trial.challenge().mayBreak(trial, player, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !inRoad(player)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (isBanned(stack)) {
            event.setUseItem(TriState.FALSE);
            return;
        }
        if (stack.getItem() instanceof BlockItem || stack.getItem() instanceof BucketItem || stack.is(Items.FLINT_AND_STEEL)
                || stack.is(Items.FIRE_CHARGE)) {
            RoadTrial trial = RoadTrials.trialOf(player.getUUID());
            BlockPos target = event.getPos().relative(event.getFace());
            if (trial == null || !trial.challenge().mayPlace(trial, player, target)) {
                event.setUseItem(TriState.FALSE);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && inRoad(player) && isBanned(event.getItemStack())) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("message.enhanced.road.no_item").withStyle(ChatFormatting.GRAY), true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseItem(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer player && inRoad(player) && isBanned(event.getItem())) {
            event.setCanceled(true);
        }
    }

    // ---- teleports ----

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEnderPearl(EntityTeleportEvent.EnderPearl event) {
        if (RoadTrials.isInside(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onChorusFruit(EntityTeleportEvent.ChorusFruit event) {
        if (event.getEntityLiving() instanceof ServerPlayer player && RoadTrials.isInside(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTravel(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && RoadTrials.isInside(player) && !RoadTrials.isMoving(player)
                && !player.hasPermissions(2)) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("message.enhanced.road.no_teleport").withStyle(ChatFormatting.GRAY), true);
        }
    }

    // ---- the Gene ----

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onToss(ItemTossEvent event) {
        if (inRoad(event.getPlayer()) && event.getEntity().getItem().is(ModItems.SCARLET_WITCH_GENE.get())) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.translatable("message.enhanced.road.keep_gene").withStyle(ChatFormatting.GRAY), true);
        }
    }

    private RoadRules() {
    }
}
