package com.kolodkingal.enhanced.road;

import com.kolodkingal.enhanced.platform.event.BlockEvent;
import com.kolodkingal.enhanced.platform.event.EntityTeleportEvent;
import com.kolodkingal.enhanced.platform.event.EntityTravelToDimensionEvent;
import com.kolodkingal.enhanced.platform.event.ItemTossEvent;
import com.kolodkingal.enhanced.platform.event.LivingDeathEvent;
import com.kolodkingal.enhanced.platform.event.LivingEntityUseItemEvent;
import com.kolodkingal.enhanced.platform.event.LivingIncomingDamageEvent;
import com.kolodkingal.enhanced.platform.event.PlayerInteractEvent;
import com.kolodkingal.enhanced.platform.event.Priority;
import com.kolodkingal.enhanced.platform.event.Subscribe;
import com.kolodkingal.enhanced.platform.event.Subscriber;
import com.kolodkingal.enhanced.platform.event.TriState;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.GameType;

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
@Subscriber
public final class RoadRules {
    /** Items that cannot be used in the Road: they would carry the player over a trial. */
    private static final Set<Item> BANNED_ITEMS = Set.of(Items.ENDER_PEARL, Items.CHORUS_FRUIT, Items.WIND_CHARGE, Items.FIREWORK_ROCKET);

    /** Whether the item cannot be used in the Road: the ones that carry him over a trial, and the ones that put an entity in his cell. */
    public static boolean isBanned(ItemStack stack) {
        Item item = stack.getItem();
        return BANNED_ITEMS.contains(item) || item instanceof ArmorStandItem || item instanceof BoatItem || item instanceof MinecartItem
                || item instanceof HangingEntityItem || item instanceof SpawnEggItem;
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

    /** Whether the rules of the Road apply to this entity: a player in a trial, or in the Road at all (but an operator who is not in a trial is free). */
    private static boolean inRoad(Object entity) {
        return entity instanceof ServerPlayer player && (RoadTrials.isInside(player) || RoadDimension.in(player) && !player.hasPermissions(2));
    }

    // ---- damage and death ----

    @Subscribe(priority = Priority.HIGHEST)
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

    @Subscribe(priority = Priority.HIGHEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && RoadTrials.isInside(player)) {
            // (A player whose trial is still being built has no trial yet: he is only saved.)
            event.setCanceled(true);
            player.setHealth(player.getMaxHealth());
            RoadTrial trial = RoadTrials.trialOf(player.getUUID());
            if (trial != null) {
                trial.putBack(player, true);
            }
        }
    }

    // ---- blocks ----

    @Subscribe(priority = Priority.HIGHEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !RoadDimension.is(level) || !inRoad(event.getPlayer())) {
            return;
        }
        RoadTrial trial = trialOf(event.getPlayer());
        if (trial == null || !trial.challenge().mayBreak(trial, (ServerPlayer) event.getPlayer(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @Subscribe(priority = Priority.HIGHEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !RoadDimension.is(level) || event.getEntity() instanceof ServerPlayer player && !inRoad(player)) {
            return;
        }
        RoadTrial trial = trialOf(event.getEntity());
        if (trial == null || !trial.challenge().mayPlace(trial, (ServerPlayer) event.getEntity(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @Subscribe(priority = Priority.HIGHEST)
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !inRoad(player)) {
            return;
        }
        RoadTrial trial = RoadTrials.trialOf(player.getUUID());
        if (trial == null || !trial.challenge().mayBreak(trial, player, event.getPos())) {
            event.setCanceled(true);
        }
    }

    @Subscribe(priority = Priority.HIGHEST)
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

    @Subscribe(priority = Priority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && inRoad(player) && isBanned(event.getItemStack())) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("message.enhanced.road.no_item").withStyle(ChatFormatting.GRAY), true);
        }
    }

    @Subscribe(priority = Priority.HIGHEST)
    public static void onUseItem(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer player && inRoad(player) && isBanned(event.getItem())) {
            event.setCanceled(true);
        }
    }

    // ---- teleports ----

    @Subscribe(priority = Priority.HIGHEST)
    public static void onEnderPearl(EntityTeleportEvent.EnderPearl event) {
        if (RoadTrials.isInside(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @Subscribe(priority = Priority.HIGHEST)
    public static void onChorusFruit(EntityTeleportEvent.ChorusFruit event) {
        if (event.getEntityLiving() instanceof ServerPlayer player && RoadTrials.isInside(player)) {
            event.setCanceled(true);
        }
    }

    @Subscribe(priority = Priority.HIGHEST)
    public static void onTravel(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && (RoadTrials.isInside(player) || event.getDimension().equals(RoadDimension.KEY))
                && !RoadTrials.isMoving(player) && !player.hasPermissions(2)) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("message.enhanced.road.no_teleport").withStyle(ChatFormatting.GRAY), true);
        }
    }

    // ---- the Gene ----

    @Subscribe(priority = Priority.HIGHEST)
    public static void onToss(ItemTossEvent event) {
        if (inRoad(event.getPlayer()) && event.getEntity().getItem().is(ModItems.SCARLET_WITCH_GENE.get())) {
            event.setCanceled(true);
            // Cancelling only stops the item entity from being made: the game has taken the stack out of the inventory already (Q, Ctrl+Q,
            // or a click outside the inventory), so it must be given back or the Gene is gone for good.
            giveBack(event.getPlayer(), event.getEntity().getItem());
            event.getPlayer().displayClientMessage(Component.translatable("message.enhanced.road.keep_gene").withStyle(ChatFormatting.GRAY), true);
        }
    }

    /** Puts a stack the game took out of the player's hands back: into his inventory, or failing that on the cursor, and tells his client. */
    static void giveBack(Player player, ItemStack stack) {
        ItemStack rest = stack.copy();
        if (!player.getInventory().add(rest) && !rest.isEmpty()) {
            if (player.containerMenu.getCarried().isEmpty()) {
                player.containerMenu.setCarried(rest);
            } else {
                // (Cannot happen: what he threw left a slot or the cursor, and that is free again.)
                player.getInventory().placeItemBackInInventory(rest);
            }
        }
        player.containerMenu.broadcastChanges();
        player.containerMenu.sendAllDataToRemote();
        player.inventoryMenu.broadcastChanges();
    }

    private RoadRules() {
    }
}
