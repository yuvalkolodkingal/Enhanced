package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.ability.AbilityHandler;
import com.kolodkingal.enhanced.hex.HexManager;
import com.kolodkingal.enhanced.platform.event.BlockEvent;
import com.kolodkingal.enhanced.platform.event.Events;
import com.kolodkingal.enhanced.platform.event.ItemTossEvent;
import com.kolodkingal.enhanced.platform.event.PlayerInteractEvent;
import com.kolodkingal.enhanced.platform.event.TriState;
import com.kolodkingal.enhanced.platform.test.EnhancedTest;
import com.kolodkingal.enhanced.power.Power;
import com.kolodkingal.enhanced.power.Powers;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.road.RoadChallenge;
import com.kolodkingal.enhanced.road.RoadChallenges;
import com.kolodkingal.enhanced.road.RoadDimension;
import com.kolodkingal.enhanced.road.RoadRules;
import com.kolodkingal.enhanced.road.RoadSave;
import com.kolodkingal.enhanced.road.RoadSweeper;
import com.kolodkingal.enhanced.road.RoadTrial;
import com.kolodkingal.enhanced.road.RoadTrials;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * What cannot be done in the Witches' Road: nothing hurts, nothing is built or broken (but what a trial opens), nothing teleports, flies
 * or floats, the Gene cannot be dropped, no power works and one who is given a power is taken out. The players of the tests are in
 * a trial; the same actions outside the Road are shown to be untouched.
 */
public final class RoadRulesTests {
    static final String BATCH = "road_rules";

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void nothingIsBuiltOrBrokenInTheRoadExceptWhereATrialOpensAPlot(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        RoadChallenge added = new RoadTestSupport.PlotParkour("test_plot");
        RoadChallenges.register(added);
        f.onClose(() -> RoadChallenges.unregister(added));
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        ServerPlayer other = RoadTestSupport.walker(f, 6.5, 6.5);
        check(RoadTrials.enter(player, RoadChallenges.indexOf(added)), "he walks the trial with a plot");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    ServerLevel road = RoadTestSupport.road(helper);
                    BlockPos floor = BlockPos.containing(trial.layout().start()).below();
                    BlockPos plot = RoadTestSupport.PlotParkour.plot(trial);
                    BlockState floorState = road.getBlockState(floor);
                    check(!floorState.isAir(), "there is a floor");
                    check(Events.post(new BlockEvent.BreakEvent(road, floor, floorState, player)).isCanceled(), "he cannot break the floor");
                    check(Events.post(new PlayerInteractEvent.LeftClickBlock(player, floor, Direction.UP,
                            PlayerInteractEvent.LeftClickBlock.Action.START)).isCanceled(), "nor start to");
                    check(Events.post(new BlockEvent.EntityPlaceEvent(road, floor.above(2), road.getBlockState(floor.above(2)),
                            floorState, player)).isCanceled(), "he cannot place a block");
                    check(!Events.post(new BlockEvent.EntityPlaceEvent(road, plot, road.getBlockState(plot), floorState, player)).isCanceled(),
                            "but he can on the plot the trial opens");
                    check(!Events.post(new BlockEvent.BreakEvent(road, plot, Blocks.STONE.defaultBlockState(), player)).isCanceled(),
                            "and break there");
                    // Items that place blocks are turned away from everywhere else with the right click.
                    player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE));
                    PlayerInteractEvent.RightClickBlock click = Events.post(new PlayerInteractEvent.RightClickBlock(player, InteractionHand.OFF_HAND, floor,
                            new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(floor), Direction.UP, floor, false)));
                    check(click.getUseItem() == TriState.FALSE, "a stone is not put down");
                    player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                })
                .thenSucceed();
        // The player outside the Road builds as ever.
        BlockPos near = helper.absolutePos(new BlockPos(6, 3, 6));
        check(!Events.post(new BlockEvent.BreakEvent(helper.getLevel(), near, Blocks.STONE.defaultBlockState(), other)).isCanceled(),
                "in the world he breaks blocks as ever");
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void teleportsFliersAndFloatersAreStopped(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        ServerPlayer outside = RoadTestSupport.walker(f, 6.5, 6.5);
        check(RoadTrials.enter(player), "he walks the Road");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    ServerLevel road = RoadTestSupport.road(helper);
                    Vec3 far = trial.layout().start().add(500.0, 0.0, 0.0);
                    check(!HexManager.onTeleport(road, player, far), "no teleport takes him out of his place");
                    check(!HexManager.onTeleport(helper.getLevel(), player, helper.absoluteVec(new Vec3(5.5, 2.0, 5.5))), "nor out of the Road");
                    check(HexManager.onTeleport(road, player, trial.layout().start().add(3.0, 0.0, 0.0)), "but a hop inside his cell is allowed");
                    check(HexManager.onTeleport(helper.getLevel(), outside, helper.absoluteVec(new Vec3(5.5, 2.0, 5.5))), "someone outside teleports as ever");
                    // Ender Pearls and the other items that would carry him over a trial cannot be used.
                    for (ItemStack banned : new ItemStack[]{new ItemStack(Items.ENDER_PEARL), new ItemStack(Items.CHORUS_FRUIT), new ItemStack(Items.WIND_CHARGE),
                            new ItemStack(Items.FIREWORK_ROCKET)}) {
                        player.setItemInHand(InteractionHand.MAIN_HAND, banned);
                        check(Events.post(new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND)).isCanceled(),
                                banned.getItem() + " cannot be used");
                    }
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
                    check(!Events.post(new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND)).isCanceled(), "a stick can");
                    outside.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ENDER_PEARL));
                    check(!Events.post(new PlayerInteractEvent.RightClickItem(outside, InteractionHand.MAIN_HAND)).isCanceled(),
                            "an Ender Pearl works in the world");
                    check(RoadRules.isBanned(new ItemStack(Items.ENDER_PEARL)) && !RoadRules.isBanned(new ItemStack(Items.STICK)), "the list of banned items");
                    // Floating and flying.
                    player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 200));
                    player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200));
                    player.addEffect(new MobEffectInstance(MobEffects.JUMP, 200, 3));
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 200));
                    player.startFallFlying();
                    check(player.isFallFlying(), "(he glides, for the test)");
                    player.setGameMode(GameType.CREATIVE);
                })
                .thenExecuteAfter(2, () -> {
                    check(!player.isFallFlying(), "gliding is stopped");
                    check(!player.hasEffect(MobEffects.LEVITATION) && !player.hasEffect(MobEffects.SLOW_FALLING) && !player.hasEffect(MobEffects.JUMP),
                            "Levitation, Slow Falling and Jump Boost are taken off him");
                    check(player.hasEffect(MobEffects.NIGHT_VISION), "what does no harm stays");
                    check(player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && !player.getAbilities().mayfly, "he is a Survivor again, and cannot fly");
                })
                .thenSucceed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void theGeneCannotBeDroppedInTheRoad(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        ServerPlayer outside = RoadTestSupport.walker(f, 6.5, 6.5);
        check(RoadTrials.enter(player), "he walks the Road");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    ServerLevel road = RoadTestSupport.road(helper);
                    ItemEntity gene = new ItemEntity(road, 0, 0, 0, new ItemStack(ModItems.SCARLET_WITCH_GENE.get()));
                    check(Events.post(new ItemTossEvent(gene, player)).isCanceled(), "the Gene cannot be thrown away in the Road");
                    ItemEntity stick = new ItemEntity(road, 0, 0, 0, new ItemStack(Items.STICK));
                    check(!Events.post(new ItemTossEvent(stick, player)).isCanceled(), "a stick can");
                    ItemEntity world = new ItemEntity(helper.getLevel(), 0, 0, 0, new ItemStack(ModItems.SCARLET_WITCH_GENE.get()));
                    check(!Events.post(new ItemTossEvent(world, outside)).isCanceled(), "in the world it can");
                })
                .thenSucceed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void aPowerGivenInsideTakesThePlayerOutWithHisGene(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        check(RoadTrials.enter(player), "he walks the Road");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    // (An operator's /enhanced power set, or any awakening item.) Pressing a key does nothing in the Road either way.
                    Powers.setPower(player, Power.MAGNETO);
                    AbilityHandler.onKey(player, 1, true);
                    AbilityHandler.onKey(player, 1, false);
                })
                .thenExecuteAfter(3, () -> {
                    check(!RoadDimension.in(player) && !RoadTrials.isInside(player), "the Road does not tolerate powers: he is sent back");
                    check(Powers.is(player, Power.MAGNETO), "his new power is his");
                    check(player.getMainHandItem().getCount() == 1 && player.getMainHandItem().is(ModItems.SCARLET_WITCH_GENE.get()), "and he still has his Gene");
                    check(!RoadSave.get(server).progress(player.getUUID()).inside(), "nothing says he is inside");
                    check(RoadTrials.refusal(player) != null, "and he cannot walk the Road any more");
                })
                .thenSucceed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH)
    public static void onlyAMortalWithoutPowersIsLetIn(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        int cells = RoadSave.get(server).cellCount();
        ServerPlayer hero = RoadTestSupport.walker(f, 4.5, 4.5);
        Powers.setPower(hero, Power.THOR);
        ServerPlayer creative = RoadTestSupport.walker(f, 8.5, 4.5);
        creative.setGameMode(GameType.CREATIVE);
        ServerPlayer spectator = RoadTestSupport.walker(f, 12.5, 4.5);
        spectator.setGameMode(GameType.SPECTATOR);
        ServerPlayer mortal = RoadTestSupport.walker(f, 16.5, 4.5);
        check("message.enhanced.road.own_road".equals(RoadTrials.refusal(hero)), "a hero already walks his own road");
        check("message.enhanced.road.mortals".equals(RoadTrials.refusal(creative)), "the Road tests only those who can fall (Creative)");
        check("message.enhanced.road.mortals".equals(RoadTrials.refusal(spectator)), "(Spectator)");
        check(RoadTrials.refusal(mortal) == null, "a mortal is let in");
        check(!RoadTrials.enter(hero) && !RoadTrials.enter(creative) && !RoadTrials.enter(spectator), "the others are not taken");
        check(RoadSave.get(server).cellCount() == cells, "no cell was built for them: " + RoadSave.get(server).cellCount() + " vs " + cells);
        check(RoadTrials.enter(mortal), "the mortal walks");
        check("message.enhanced.road.already".equals(RoadTrials.refusal(mortal)) && !RoadTrials.enter(mortal), "and cannot walk twice");
        helper.succeed();
    }

    @EnhancedTest(template = ARENA, batch = BATCH, timeoutTicks = 500)
    public static void whatATrialSpawnsIsRemovedWithIt(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        RoadChallenge added = new RoadTestSupport.PlotParkour("test_plot_entities");
        RoadChallenges.register(added);
        f.onClose(() -> RoadChallenges.unregister(added));
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        check(RoadTrials.enter(player, RoadChallenges.indexOf(added)), "he walks the trial that spawns a boat and forgets an item");
        Boat[] boat = new Boat[1];
        ItemEntity[] item = new ItemEntity[1];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                // (A few ticks for the chunks to take the new entities in; the player stays a good while in a real trial.)
                .thenExecuteAfter(10, () -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    ServerLevel road = RoadTestSupport.road(helper);
                    boat[0] = (Boat) trial.tracked().get(0);
                    item[0] = RoadTestSupport.PlotParkour.forgotten.get(player.getUUID());
                    check(item[0] != null && road.getEntity(item[0].getUUID()) == item[0], "the item is in the Road");
                    check(boat[0].isAlive() && item[0].isAlive() && trial.tracked().size() == 1, "a boat the trial knows, and an item it does not");
                    RoadTrials.leave(player, RoadChallenge.End.LEFT);
                    check(boat[0].isRemoved(), "the boat is gone the moment the trial ends");
                    RoadSweeper.drain(server);
                    check(item[0].isRemoved(), "and the item the trial forgot goes with its cell");
                })
                .thenSucceed();
    }

    private RoadRulesTests() {
    }
}
