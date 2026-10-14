package com.kolodkingal.enhanced.boss;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;

/**
 * What keeps a boss arena as it was: while a fight lasts, the blocks of its cylinder cannot be broken, placed, flooded,
 * pushed, blown up or eaten by a mob, nothing spawns naturally in it, and a fighter cannot leave it by a Chorus Fruit
 * or a portal. Everything asks {@link BossFights#protects} / {@link BossArena#allowsTeleport}, so an arena with {@code
 * protectBlocks} off (Thanos's) is untouched. The mod's own block-changing abilities and teleports ask the same
 * questions where they act (Tommy's acceleration, Loki's ice, the Hex gate); block telekinesis only borrows an arena's
 * blocks, which the arena gets back ({@link ArenaLoans}).
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class BossArenaEvents {

    private static boolean protects(LevelAccessor level, BlockPos pos) {
        return level instanceof ServerLevel server && BossFights.protects(server, pos);
    }

    private static void refuse(Entity who) {
        if (who instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.enhanced.boss.protected"), true);
        }
    }

    // ---- blocks ----

    /**
     * Nobody breaks a block of the arena. The one cell a loan of block telekinesis is asking about ({@link
     * BossFights#lending}) is let through, so that the claim mods that listen after the arena decide it.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (BossFights.isLending(event.getPos())) {
            return;
        }
        if (protects(event.getLevel(), event.getPos())) {
            refuse(event.getPlayer());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (protects(event.getLevel(), event.getPos())) {
            refuse(event.getEntity());
            event.setCanceled(true);
        }
    }

    /** Water and lava turning into stone or obsidian where they meet. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (protects(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /** A bucket empties or fills where the player points; there is no bucket event, so the use is stopped. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBucketOnBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isBucket(event.getItemStack()) && bucketTouches(event.getEntity(), event.getHitVec())) {
            refuse(event.getEntity());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBucketInTheAir(PlayerInteractEvent.RightClickItem event) {
        if (!isBucket(event.getItemStack()) || !(event.getLevel() instanceof ServerLevel)) {
            return;
        }
        Player player = event.getEntity();
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, true);
        if (hit instanceof BlockHitResult block && bucketTouches(player, block)) {
            refuse(player);
            event.setCanceled(true);
        }
    }

    private static boolean isBucket(ItemStack stack) {
        return stack.getItem() instanceof BucketItem || stack.getItem() instanceof SolidBucketItem;
    }

    /** Whether the block a bucket would change, or the one next to it where it would pour, is protected. */
    private static boolean bucketTouches(Player player, BlockHitResult hit) {
        return player.level() instanceof ServerLevel level && hit.getType() == HitResult.Type.BLOCK
                && (BossFights.protects(level, hit.getBlockPos()) || BossFights.protects(level, hit.getBlockPos().relative(hit.getDirection())));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPiston(PistonEvent.Pre event) {
        if (protects(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /**
     * A block that leans on a borrowed block's open hole (a torch on it, a flower, a lantern under it) and that its
     * neighbours break meanwhile drops nothing: the mend puts it back ({@link ArenaLoans}).
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDrops(BlockDropsEvent event) {
        if (BossFights.leansOnALoan(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /** Explosions hurt and break nothing. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level && !event.getAffectedBlocks().isEmpty()) {
            event.getAffectedBlocks().removeIf(pos -> BossFights.protects(level, pos));
        }
    }

    // ---- mobs ----

    /** Withers, Ravagers and zombies at the door break nothing. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDestroy(LivingDestroyBlockEvent event) {
        if (protects(event.getEntity().level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /** Endermen carry no blocks away, Silverfish eat none, Creepers' craters stay. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity() != null && protects(event.getEntity().level(), event.getEntity().blockPosition())) {
            event.setCanGrief(false);
        }
    }

    /** Nothing spawns in the arena by itself (a spawner, a patrol, the night). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onSpawn(MobSpawnEvent.PositionCheck event) {
        if (event.getLevel() instanceof ServerLevel level && BossFights.protects(level, BlockPos.containing(event.getX(), event.getY(), event.getZ()))) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    // ---- leaving ----

    /**
     * A Chorus Fruit cannot take a fighter out of the arena. (Ender Pearls, the Bifrost, Vanish, the Tesseract and the
     * Witches' Road are judged where the Hex judges them; an operator's {@code /tp} is never stopped.)
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChorusFruit(EntityTeleportEvent.ChorusFruit event) {
        Entity entity = event.getEntity();
        if (entity.level() instanceof ServerLevel level && !BossFights.allowsTeleport(level, entity, event.getTarget())) {
            BossFights.tellHeld(entity);
            event.setCanceled(true);
        }
    }

    /** A portal does not take a fighter out of the arena either. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onTravel(EntityTravelToDimensionEvent event) {
        Entity entity = event.getEntity();
        if (entity.level() instanceof ServerLevel level && level.getServer() != null) {
            for (BossFight fight : BossFights.all(level.getServer())) {
                if (fight.arena().gates() && fight.isMember(entity) && fight.arena().contains(entity.position())) {
                    BossFights.tellHeld(entity);
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }

    private BossArenaEvents() {
    }
}
