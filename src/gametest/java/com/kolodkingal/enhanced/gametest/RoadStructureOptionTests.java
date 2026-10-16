package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.WitchesRoadConfig;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.road.RoadSave;
import com.kolodkingal.enhanced.road.RoadSweeper;
import com.kolodkingal.enhanced.road.RoadTrial;
import com.kolodkingal.enhanced.road.RoadTrials;
import com.kolodkingal.enhanced.road.copy.StructureCompare;
import com.kolodkingal.enhanced.road.copy.StructureCopyChallenge;
import com.kolodkingal.enhanced.road.copy.StructureModel;
import com.kolodkingal.enhanced.road.copy.StructureSupply;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.kolodkingal.enhanced.gametest.RoadStructureTests.build;
import static com.kolodkingal.enhanced.gametest.RoadStructureTests.cell;
import static com.kolodkingal.enhanced.gametest.RoadStructureTests.enter;
import static com.kolodkingal.enhanced.gametest.RoadStructureTests.index;
import static com.kolodkingal.enhanced.gametest.RoadStructureTests.supplyStacks;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/** The options of the Copy the Structure trial and what each one changes: Check by hand, hints, the time limit, the size. Each test has a batch of its own, as a test that changes the config must. */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class RoadStructureOptionTests {

    @GameTest(template = ARENA, batch = "road_structure_manual", timeoutTicks = 500)
    public static void withAutoCheckOffTheTrialIsWonOnlyWhenHePressesCheck(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        f.config(WitchesRoadConfig.STRUCTURE_COPY_AUTO_CHECK, false);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        enter(player);
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> build(RoadTestSupport.road(helper), RoadTestSupport.trial(player)))
                .thenExecuteAfter(50, () -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(StructureCopyChallenge.compare(trial).perfect() && trial.phase() == RoadTrial.Phase.ACTIVE, "a perfect copy is not noticed until he says so");
                    check(StructureCopyChallenge.use(trial, player, StructureCopyChallenge.checkButton(trial)), "he presses Check");
                    check(trial.phase() == RoadTrial.Phase.WON, "and wins at once");
                    check(RoadSave.get(server).progress(player.getUUID()).completed() == index() + 1, "the next trial is unlocked");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "road_structure_hint", timeoutTicks = 500)
    public static void hintShowsGhostsOfTheLowestMissingBlocksForAFewSecondsAndCostsNothing(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        f.config(WitchesRoadConfig.STRUCTURE_COPY_HINT_SECONDS, 1);
        f.config(WitchesRoadConfig.STRUCTURE_COPY_HINT_BLOCKS, 5);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        enter(player);
        List<Entity> ghosts = new ArrayList<>();
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    StructureModel model = StructureCopyChallenge.model(trial);
                    // He has built the lower half; one block is wrong.
                    int[] wrong = cell(model, RoadStructureTests::plain, 4);
                    build(RoadTestSupport.road(helper), trial);
                    BlockPos pc = StructureCopyChallenge.plotCorner(trial);
                    int half = model.sizeY() / 2;
                    for (int y = half; y < model.sizeY(); y++) {
                        for (int z = 0; z < model.sizeZ(); z++) {
                            for (int x = 0; x < model.sizeX(); x++) {
                                RoadTestSupport.road(helper).setBlock(pc.offset(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                            }
                        }
                    }
                    RoadTestSupport.road(helper).setBlock(pc.offset(wrong[0], wrong[1], wrong[2]), net.minecraft.world.level.block.Blocks.TNT.defaultBlockState(), 2);
                    StructureCompare.Result before = StructureCopyChallenge.compare(trial);
                    check(before.missing().size() > 5, "much is missing: " + before.missing().size());
                    int stacks = supplyStacks(player);
                    StructureCopyChallenge.use(trial, player, StructureCopyChallenge.hintButton(trial));
                    ghosts.addAll(StructureCopyChallenge.ghosts(trial));
                    check(ghosts.size() == 5, "five ghost blocks (the most the option allows): " + ghosts.size());
                    Set<BlockPos> problems = new HashSet<>();
                    before.missing().forEach(c -> problems.add(pc.offset(c.x(), c.y(), c.z())));
                    before.wrong().forEach(c -> problems.add(pc.offset(c.x(), c.y(), c.z())));
                    int highest = Integer.MIN_VALUE;
                    for (Entity ghost : ghosts) {
                        check(ghost instanceof Display.BlockDisplay && ghost.hasGlowingTag() && !ghost.isRemoved(), "a glowing block display");
                        BlockPos at = BlockPos.containing(ghost.position().add(0.001, 0.001, 0.001));
                        check(problems.remove(at), "a ghost stands where a block is missing or wrong: " + at.toShortString());
                        CompoundTag saved = ghost.saveWithoutId(new CompoundTag());
                        BlockState want = StructureCopyChallenge.model(trial).at(at.getX() - pc.getX(), at.getY() - pc.getY(), at.getZ() - pc.getZ());
                        check(saved.getCompound("block_state").getString("Name").equals(BuiltInRegistries.BLOCK.getKey(want.getBlock()).toString()),
                                "and it shows the block that belongs there: " + saved.getCompound("block_state"));
                        highest = Math.max(highest, at.getY());
                    }
                    for (BlockPos left : problems) {
                        check(left.getY() >= highest, "the lowest ones come first: " + left.toShortString() + " is left, a ghost is at " + highest);
                    }
                    check(StructureCopyChallenge.compare(trial).correct() == before.correct() && supplyStacks(player) == stacks && trial.phase() == RoadTrial.Phase.ACTIVE,
                            "a hint costs nothing");
                    StructureCopyChallenge.hint(trial, player);
                    check(StructureCopyChallenge.ghosts(trial).size() == 5, "pressing again at once changes nothing");
                })
                .thenExecuteAfter(40, () -> {
                    check(StructureCopyChallenge.ghosts(RoadTestSupport.trial(player)).isEmpty() && ghosts.stream().allMatch(Entity::isRemoved), "after a second the ghosts are gone");
                    RoadTrial trial = RoadTestSupport.trial(player);
                    StructureCopyChallenge.hint(trial, player);
                    check(StructureCopyChallenge.ghosts(trial).size() == 5, "and another hint can be asked for");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "road_structure_nohint", timeoutTicks = 300)
    public static void withHintsOffTheHintButtonShowsNothing(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        f.config(WitchesRoadConfig.STRUCTURE_COPY_HINTS, false);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        enter(player);
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(StructureCopyChallenge.use(trial, player, StructureCopyChallenge.hintButton(trial)), "the button is pressed");
                    check(StructureCopyChallenge.ghosts(trial).isEmpty(), "and shows no ghost");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "road_structure_time", timeoutTicks = 400)
    public static void whenTheLongTimeRunsOutTheRoomIsTakenDownAndHeIsSentHomeWithEverything(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        f.config(WitchesRoadConfig.STRUCTURE_COPY_TIME_LIMIT_SECONDS, 2);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        player.getInventory().setItem(5, new ItemStack(Items.DIAMOND, 3));
        enter(player);
        long[][] chunks = new long[1][];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    chunks[0] = RoadTestSupport.trial(player).chunks();
                    build(RoadTestSupport.road(helper), RoadTestSupport.trial(player), 0, 0, 0);
                    check(RoadTrials.isInside(player), "he is in the trial");
                })
                .thenWaitUntil(() -> check(!RoadTrials.isInside(player), "after two seconds he is sent home"))
                .thenExecute(() -> {
                    check(!com.kolodkingal.enhanced.road.RoadDimension.in(player) && player.serverLevel() == helper.getLevel(), "back in the world");
                    check(supplyStacks(player) == 0 && player.getInventory().getItem(5).is(Items.DIAMOND) && player.getInventory().getItem(5).getCount() == 3
                            && player.getInventory().countItem(ModItems.SCARLET_WITCH_GENE.get()) == 1, "with his things and his Gene");
                    check(!RoadSave.get(server).progress(player.getUUID()).inside() && RoadSave.get(server).progress(player.getUUID()).completed() == 0, "nothing is lost or won");
                    RoadSweeper.drain(server);
                    check(RoadTestSupport.blocksIn(RoadTestSupport.road(helper), chunks[0]) == 0, "and the room is taken down");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "road_structure_tier1", timeoutTicks = 400)
    public static void theSmallSizeIsAtMostFiveByFiveByFive(GameTestHelper helper) {
        sized(helper, 1, 5, 5, 5);
    }

    @GameTest(template = ARENA, batch = "road_structure_tier3", timeoutTicks = 400)
    public static void theLargeSizeIsAtMostElevenByElevenByNineAndBiggerThanTheSmall(GameTestHelper helper) {
        sized(helper, 3, 11, 11, 9);
    }

    private static void sized(GameTestHelper helper, int tier, int w, int d, int h) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        f.config(WitchesRoadConfig.STRUCTURE_COPY_TIER, tier);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        enter(player);
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    StructureModel model = StructureCopyChallenge.model(trial);
                    check(model.tier() == tier && model.sizeX() <= w && model.sizeZ() <= d && model.sizeY() <= h, "tier " + tier + ": " + model.sizeX() + "x" + model.sizeZ() + "x" + model.sizeY());
                    check(model.solidCount() > (tier == 1 ? 20 : 150), "and it is a real build: " + model.solidCount() + " blocks");
                    check(supplyStacks(player) >= model.itemsNeeded().size() && StructureSupply.hasStash(player, StructureCopyChallenge.ID), "with its blocks in hand");
                    // The most a model can ask of the backpack still fits it.
                    check(StructureSupply.stacksFor(StructureSupply.amounts(model, 150)) <= 30, "the supply fits the backpack");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "road_structure_tier0", timeoutTicks = 300)
    public static void sizeZeroGivesEachPlayerASizeOfHisOwnAndKeepsIt(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        f.config(WitchesRoadConfig.STRUCTURE_COPY_TIER, 0);
        Set<Integer> tiers = new HashSet<>();
        for (long seed = 1; seed < 200; seed++) {
            int tier = StructureCopyChallenge.tierFor(seed * 104729L);
            tiers.add(tier);
            check(tier >= 1 && tier <= 3 && StructureCopyChallenge.tierFor(seed * 104729L) == tier, "a size of 1 to 3 for a seed, the same every time");
        }
        check(tiers.size() == 3, "all three sizes are dealt: " + tiers);
        helper.succeed();
    }

    private RoadStructureOptionTests() {
    }
}
