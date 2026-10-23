package com.kolodkingal.enhanced.platform.neoforge.test.bridge;

import com.kolodkingal.enhanced.gametest.TestKit;
import com.kolodkingal.enhanced.platform.neoforge.test.bridge.BridgeTestSupport.Bridged;
import com.kolodkingal.enhanced.platform.event.AttackEntityEvent;
import com.kolodkingal.enhanced.platform.event.CanPlayerSleepEvent;
import com.kolodkingal.enhanced.platform.event.ItemEntityPickupEvent;
import com.kolodkingal.enhanced.platform.event.ItemTooltipEvent;
import com.kolodkingal.enhanced.platform.event.ItemTossEvent;
import com.kolodkingal.enhanced.platform.event.PlayerEvent;
import com.kolodkingal.enhanced.platform.event.PlayerInteractEvent;
import com.kolodkingal.enhanced.platform.event.PlayerRespawnPositionEvent;
import com.kolodkingal.enhanced.platform.event.PlayerSetSpawnEvent;
import com.kolodkingal.enhanced.platform.event.PlayerTickEvent;
import com.kolodkingal.enhanced.platform.event.TriState;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

import static com.kolodkingal.enhanced.platform.neoforge.test.bridge.BridgeTestSupport.BATCH;
import static com.kolodkingal.enhanced.platform.neoforge.test.bridge.BridgeTestSupport.bridge;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * BridgeTests of the player events (port P0, S-A): each NeoForge event reaches the listeners of its common event with
 * the same data, in priority order, cancellable as NeoForge's, and what a listener changes reaches NeoForge
 * ({@link BridgeTestSupport}).
 */
public final class BridgePlayerTests {
    private BridgePlayerTests() {
    }

    private static ServerPlayer player(GameTestHelper helper) {
        return TestKit.fixture(helper).player(5.5, FLOOR, 5.5);
    }

    private static Zombie zombie(GameTestHelper helper) {
        return TestKit.spawnWithNoFreeWill(helper, EntityType.ZOMBIE, new Vec3(8.5, FLOOR, 8.5));
    }

    private static BlockPos floor(GameTestHelper helper) {
        return helper.absolutePos(new BlockPos(5, 1, 5));
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void playerLoggedIn(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        bridge(PlayerEvent.PlayerLoggedInEvent.class,
                () -> new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(p));
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void playerLoggedOut(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        bridge(PlayerEvent.PlayerLoggedOutEvent.class,
                () -> new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void playerChangedDimension(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        bridge(PlayerEvent.PlayerChangedDimensionEvent.class,
                () -> new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(p,
                        Level.OVERWORLD, Level.NETHER));
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void playerRespawn(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        bridge(PlayerEvent.PlayerRespawnEvent.class,
                () -> new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent(p, true));
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void startTracking(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        Zombie zombie = zombie(helper);
        bridge(PlayerEvent.StartTracking.class,
                () -> new net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking(p, zombie));
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void clone(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.fixture(helper);
        ServerPlayer old = f.player(4.5, FLOOR, 4.5);
        ServerPlayer reborn = f.player(6.5, FLOOR, 6.5);
        bridge(PlayerEvent.Clone.class,
                () -> new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(reborn, old, true));
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void breakSpeed(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        BlockPos pos = floor(helper);
        Bridged<PlayerEvent.BreakSpeed, net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed> b =
                bridge(PlayerEvent.BreakSpeed.class,
                        () -> new net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed(p,
                                Blocks.STONE.defaultBlockState(), 1.5F, pos));
        b.common().setNewSpeed(4.0F);
        check(b.neo().getNewSpeed() == 4.0F, "setNewSpeed reaches NeoForge");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void itemCrafted(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        ItemStack sticks = new ItemStack(Items.STICK, 4);
        bridge(PlayerEvent.ItemCraftedEvent.class,
                () -> new net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent(p, sticks,
                        p.inventoryMenu.getCraftSlots()));
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void playerChangeGameMode(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        Bridged<PlayerEvent.PlayerChangeGameModeEvent,
                net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangeGameModeEvent> b =
                bridge(PlayerEvent.PlayerChangeGameModeEvent.class,
                        () -> new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangeGameModeEvent(p,
                                GameType.SURVIVAL, GameType.CREATIVE));
        b.common().setNewGameMode(GameType.ADVENTURE);
        check(b.neo().getNewGameMode() == GameType.ADVENTURE, "setNewGameMode reaches NeoForge");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void entityInteractSpecific(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        Zombie zombie = zombie(helper);
        Bridged<PlayerInteractEvent.EntityInteractSpecific,
                net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific> b =
                bridge(PlayerInteractEvent.EntityInteractSpecific.class,
                        () -> new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific(
                                p, InteractionHand.MAIN_HAND, zombie, new Vec3(0.1, 1.0, 0.1)));
        b.common().setCancellationResult(InteractionResult.SUCCESS);
        check(b.neo().getCancellationResult() == InteractionResult.SUCCESS, "setCancellationResult reaches NeoForge");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void entityInteract(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        Zombie zombie = zombie(helper);
        Bridged<PlayerInteractEvent.EntityInteract,
                net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract> b =
                bridge(PlayerInteractEvent.EntityInteract.class,
                        () -> new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract(p,
                                InteractionHand.OFF_HAND, zombie));
        b.common().setCancellationResult(InteractionResult.CONSUME);
        check(b.neo().getCancellationResult() == InteractionResult.CONSUME, "setCancellationResult reaches NeoForge");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void rightClickBlock(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        BlockPos pos = floor(helper);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        Bridged<PlayerInteractEvent.RightClickBlock,
                net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock> b =
                bridge(PlayerInteractEvent.RightClickBlock.class,
                        () -> new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p,
                                InteractionHand.MAIN_HAND, pos, hit));
        b.common().setUseItem(TriState.FALSE);
        b.common().setUseBlock(TriState.TRUE);
        b.common().setCancellationResult(InteractionResult.FAIL);
        check(b.neo().getUseItem() == net.neoforged.neoforge.common.util.TriState.FALSE, "setUseItem reaches NeoForge");
        check(b.neo().getUseBlock() == net.neoforged.neoforge.common.util.TriState.TRUE, "setUseBlock too");
        check(b.common().getUseItem() == TriState.FALSE && b.common().getUseBlock() == TriState.TRUE, "and reads back");
        check(b.neo().getCancellationResult() == InteractionResult.FAIL, "setCancellationResult reaches NeoForge");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void rightClickItem(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        Bridged<PlayerInteractEvent.RightClickItem,
                net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem> b =
                bridge(PlayerInteractEvent.RightClickItem.class,
                        () -> new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem(p,
                                InteractionHand.MAIN_HAND));
        check(b.common().getItemStack().is(Items.STICK), "the item in the hand");
        b.common().setCancellationResult(InteractionResult.SUCCESS);
        check(b.neo().getCancellationResult() == InteractionResult.SUCCESS, "setCancellationResult reaches NeoForge");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void leftClickBlock(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        BlockPos pos = floor(helper);
        Bridged<PlayerInteractEvent.LeftClickBlock,
                net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock> b =
                bridge(PlayerInteractEvent.LeftClickBlock.class,
                        () -> new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(p, pos,
                                Direction.UP, net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
                                .LeftClickBlock.Action.START));
        check(b.common().getAction() == PlayerInteractEvent.LeftClickBlock.Action.START, "the action");
        b.common().setUseItem(TriState.FALSE);
        check(b.neo().getUseItem() == net.neoforged.neoforge.common.util.TriState.FALSE, "setUseItem reaches NeoForge");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void attackEntity(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        Zombie zombie = zombie(helper);
        bridge(AttackEntityEvent.class, () -> new net.neoforged.neoforge.event.entity.player.AttackEntityEvent(p,
                zombie));
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void canPlayerSleep(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        BlockPos pos = floor(helper);
        Bridged<CanPlayerSleepEvent, net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent> b =
                bridge(CanPlayerSleepEvent.class,
                        () -> new net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent(p, pos, null));
        b.common().setProblem(Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
        check(b.neo().getProblem() == Player.BedSleepingProblem.NOT_POSSIBLE_NOW, "setProblem reaches NeoForge");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void playerRespawnPosition(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        DimensionTransition home = new DimensionTransition(helper.getLevel(), new Vec3(1, 2, 3), Vec3.ZERO, 0.0F,
                0.0F, DimensionTransition.DO_NOTHING);
        DimensionTransition elsewhere = new DimensionTransition(helper.getLevel(), new Vec3(4, 5, 6), Vec3.ZERO,
                0.0F, 0.0F, DimensionTransition.DO_NOTHING);
        Bridged<PlayerRespawnPositionEvent, net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent> b =
                bridge(PlayerRespawnPositionEvent.class,
                        () -> new net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent(p, home,
                                false));
        b.common().setDimensionTransition(elsewhere);
        b.common().setCopyOriginalSpawnPosition(false);
        check(b.neo().getDimensionTransition() == elsewhere, "setDimensionTransition reaches NeoForge");
        check(!b.neo().copyOriginalSpawnPosition(), "setCopyOriginalSpawnPosition too");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void playerSetSpawn(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        BlockPos pos = floor(helper);
        bridge(PlayerSetSpawnEvent.class, () -> new net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent(p,
                Level.OVERWORLD, pos, false));
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void itemTooltip(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        ItemStack stick = new ItemStack(Items.STICK);
        Bridged<ItemTooltipEvent, net.neoforged.neoforge.event.entity.player.ItemTooltipEvent> b =
                bridge(ItemTooltipEvent.class,
                        () -> new net.neoforged.neoforge.event.entity.player.ItemTooltipEvent(stick, p,
                                new ArrayList<>(), TooltipFlag.NORMAL, Item.TooltipContext.EMPTY));
        b.common().getToolTip().add(net.minecraft.network.chat.Component.literal("line"));
        check(b.neo().getToolTip().size() == 1, "a line added to the common tooltip is NeoForge's");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void itemEntityPickup(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        ItemEntity item = new ItemEntity(helper.getLevel(), p.getX(), p.getY(), p.getZ(), new ItemStack(Items.STICK));
        Bridged<ItemEntityPickupEvent.Pre, net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre> b =
                bridge(ItemEntityPickupEvent.Pre.class,
                        () -> new net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre(p, item));
        b.common().setCanPickup(TriState.FALSE);
        check(b.neo().canPickup() == net.neoforged.neoforge.common.util.TriState.FALSE,
                "setCanPickup reaches NeoForge");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void itemToss(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        ItemEntity item = new ItemEntity(helper.getLevel(), p.getX(), p.getY(), p.getZ(), new ItemStack(Items.STICK));
        bridge(ItemTossEvent.class, () -> new net.neoforged.neoforge.event.entity.item.ItemTossEvent(item, p));
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void playerTickPre(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        bridge(PlayerTickEvent.Pre.class, () -> new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(p));
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void playerTickPost(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        bridge(PlayerTickEvent.Post.class, () -> new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
        helper.succeed();
    }
}
