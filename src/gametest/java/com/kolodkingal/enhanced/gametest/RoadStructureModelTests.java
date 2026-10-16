package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.road.RoadCanvas;
import com.kolodkingal.enhanced.road.RoadLayout;
import com.kolodkingal.enhanced.road.copy.CopyGeometry;
import com.kolodkingal.enhanced.road.copy.StructureCompare;
import com.kolodkingal.enhanced.road.copy.StructureCopyChallenge;
import com.kolodkingal.enhanced.road.copy.StructureLibrary;
import com.kolodkingal.enhanced.road.copy.StructureModel;
import com.kolodkingal.enhanced.road.copy.StructureSupply;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Trial 2, Copy the Structure, as code that knows no world: the library of models (every model of every theme, tier and seed can be
 * built by hand, in the sizes and with the kinds of block it promises), the comparison (what counts and what does not), the supply
 * (enough of everything) and the room's plan (model and plot alike and three blocks apart, the console, the way from the start).
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class RoadStructureModelTests {
    static final String BATCH = "road_structure_model";

    private static boolean sameCells(StructureModel a, StructureModel b) {
        if (a.sizeX() != b.sizeX() || a.sizeY() != b.sizeY() || a.sizeZ() != b.sizeZ()) {
            return false;
        }
        for (int y = 0; y < a.sizeY(); y++) {
            for (int z = 0; z < a.sizeZ(); z++) {
                for (int x = 0; x < a.sizeX(); x++) {
                    if (a.at(x, y, z) != b.at(x, y, z)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** A number for everything the model holds. */
    private static int contentHash(StructureModel model) {
        int hash = model.sizeX() * 31 + model.sizeY() * 17 + model.sizeZ();
        for (int y = 0; y < model.sizeY(); y++) {
            for (int z = 0; z < model.sizeZ(); z++) {
                for (int x = 0; x < model.sizeX(); x++) {
                    hash = hash * 31 + System.identityHashCode(model.at(x, y, z));
                }
            }
        }
        return hash;
    }

    /** The game's rule, written out again: a stair with a stair of another direction at its front or back (same half) is rounded into a corner. */
    private static boolean makesCorner(StructureModel m, int x, int y, int z) {
        BlockState s = m.at(x, y, z);
        Direction f = s.getValue(StairBlock.FACING);
        for (int sign = -1; sign <= 1; sign += 2) {
            BlockState other = m.at(x + sign * f.getStepX(), y, z + sign * f.getStepZ());
            if (other.getBlock() instanceof StairBlock && other.getValue(StairBlock.HALF) == s.getValue(StairBlock.HALF)
                    && other.getValue(StairBlock.FACING).getAxis() != f.getAxis()) {
                return true;
            }
        }
        return false;
    }

    // ---- the library ----

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void everyModelOfTheLibraryCanBeBuiltByHand(GameTestHelper helper) {
        Map<String, Set<Integer>> distinct = new HashMap<>();
        double[] blocks = new double[4];
        int[] models = new int[4];
        int[] biggest = new int[4];
        for (int tier = 1; tier <= 3; tier++) {
            int[] box = StructureLibrary.box(tier);
            for (int theme = 0; theme < StructureLibrary.THEMES.size(); theme++) {
                for (int seed = 0; seed < 150; seed++) {
                    String what = StructureLibrary.THEMES.get(theme) + " tier " + tier + " seed " + seed;
                    StructureModel model = StructureLibrary.generate(seed, tier, theme);
                    check(model.problems().isEmpty(), what + ": " + model.problems());
                    check(model.theme().equals(StructureLibrary.THEMES.get(theme)) && model.tier() == tier, what + " is what it says it is");
                    check(model.sizeX() <= box[0] && model.sizeZ() <= box[1] && model.sizeY() <= box[2],
                            what + " is " + model.sizeX() + "x" + model.sizeZ() + "x" + model.sizeY() + ", more than " + box[0] + "x" + box[1] + "x" + box[2]);
                    if (tier == 1) {
                        check(model.sizeX() >= 4 && model.sizeY() >= 3, what + " is not a speck: " + model.sizeX() + "x" + model.sizeY());
                    } else {
                        check(Math.max(model.sizeX(), model.sizeZ()) >= (tier == 2 ? 7 : 9) && model.sizeY() >= (tier == 2 ? 5 : 7),
                                what + " is as big as its tier: " + model.sizeX() + "x" + model.sizeZ() + "x" + model.sizeY());
                    }
                    Map<Item, Integer> items = model.itemsNeeded();
                    check(items.size() >= 3 && items.size() <= StructureLibrary.MAX_KINDS, what + " takes " + items.size() + " kinds of block");
                    int total = items.values().stream().mapToInt(Integer::intValue).sum();
                    check(total >= model.solidCount(), what + ": items cover the blocks");
                    check(StructureSupply.stacksFor(StructureSupply.amounts(model, 150)) <= 30, what + " fits the backpack with its supply");
                    check(sameCells(model, StructureLibrary.generate(seed, tier, theme)), what + " is the same every time");
                    for (int y = 0; y < model.sizeY(); y++) {
                        for (int z = 0; z < model.sizeZ(); z++) {
                            for (int x = 0; x < model.sizeX(); x++) {
                                BlockState state = model.at(x, y, z);
                                if (state.getBlock() instanceof StairBlock) {
                                    check(!makesCorner(model, x, y, z), what + ": no stairs round a corner at " + x + "," + y + "," + z);
                                }
                            }
                        }
                    }
                    distinct.computeIfAbsent(theme + ":" + tier, k -> new HashSet<>()).add(contentHash(model));
                    blocks[tier] += model.solidCount();
                    models[tier]++;
                    biggest[tier] = Math.max(biggest[tier], model.solidCount());
                }
            }
        }
        for (Map.Entry<String, Set<Integer>> entry : distinct.entrySet()) {
            check(entry.getValue().size() >= 6, "the seed makes a difference for " + entry.getKey() + ": " + entry.getValue().size() + " kinds in 150");
        }
        double a1 = blocks[1] / models[1];
        double a2 = blocks[2] / models[2];
        double a3 = blocks[3] / models[3];
        check(a1 > 25 && a1 < a2 && a2 < a3 && a3 > 150, "it gets harder: " + a1 + " < " + a2 + " < " + a3 + " blocks on average");
        Enhanced.LOGGER.info("Copy the Structure library: {} / {} / {} blocks on average, {} / {} / {} at the most (tiers 1, 2, 3)", Math.round(a1), Math.round(a2), Math.round(a3),
                biggest[1], biggest[2], biggest[3]);
        check(biggest[1] <= 150 && biggest[3] <= 900, "and not impossible: " + biggest[1] + " and " + biggest[3] + " blocks at the most");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void theSeedPicksTheThemeAndAllFiveThemesAppear(GameTestHelper helper) {
        Set<String> themes = new HashSet<>();
        for (int seed = 0; seed < 200; seed++) {
            themes.add(StructureLibrary.generate(seed * 7919L, 2).theme());
            check(sameCells(StructureLibrary.generate(seed * 7919L, 2), StructureLibrary.generate(seed * 7919L, 2)), "the seed alone decides");
        }
        check(themes.size() == 5 && themes.containsAll(StructureLibrary.THEMES), "all the themes appear: " + themes);
        check(StructureLibrary.generate(1, 0).tier() == 1 && StructureLibrary.generate(1, 9).tier() == 3, "a tier outside 1..3 is brought into it");
        helper.succeed();
    }

    // ---- the comparison ----

    /** A plot that can be changed: starts as a copy of the model. */
    private static final class Plot implements StructureCompare.Plot {
        final StructureModel model;
        final BlockState[][][] cells;

        Plot(StructureModel model) {
            this.model = model;
            cells = new BlockState[model.sizeX()][model.sizeY() + StructureCompare.EXTRA_HEIGHT + 3][model.sizeZ()];
            for (int x = 0; x < cells.length; x++) {
                for (int y = 0; y < cells[0].length; y++) {
                    for (int z = 0; z < cells[0][0].length; z++) {
                        cells[x][y][z] = model.at(x, y, z);
                    }
                }
            }
        }

        @Override
        public BlockState at(int x, int y, int z) {
            return cells[x][y][z];
        }

        StructureCompare.Result compare() {
            return StructureCompare.compare(model, this);
        }

        /** The first cell of the model for which the test is true. */
        int[] find(java.util.function.Predicate<BlockState> test) {
            for (int y = 0; y < model.sizeY(); y++) {
                for (int z = 0; z < model.sizeZ(); z++) {
                    for (int x = 0; x < model.sizeX(); x++) {
                        if (test.test(model.at(x, y, z))) {
                            return new int[]{x, y, z};
                        }
                    }
                }
            }
            throw new IllegalStateException("not in the model");
        }

        void set(int[] at, BlockState state) {
            cells[at[0]][at[1]][at[2]] = state;
        }
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theComparisonSeesWrongOrientationsMissingAndExtraBlocks(GameTestHelper helper) {
        StructureModel hut = StructureLibrary.generate(11, 2, 0);
        Plot plot = new Plot(hut);
        StructureCompare.Result perfect = plot.compare();
        check(perfect.perfect() && perfect.correct() == perfect.total() && perfect.total() == hut.solidCount() && perfect.percent() == 100 && perfect.problems() == 0,
                "a copy is perfect: " + perfect);

        // A stair that faces the other way, or lies upside down, is wrong; one that only differs in the shape the game gives it is not.
        int[] stair = plot.find(s -> s.getBlock() instanceof StairBlock);
        BlockState stairState = hut.at(stair[0], stair[1], stair[2]);
        plot.set(stair, stairState.setValue(StairBlock.FACING, stairState.getValue(StairBlock.FACING).getOpposite()));
        StructureCompare.Result flipped = plot.compare();
        check(!flipped.perfect() && flipped.wrong().size() == 1 && flipped.correct() == flipped.total() - 1 && flipped.percent() < 100,
                "a stair facing the wrong way is wrong: " + flipped);
        check(flipped.wrong().get(0).equals(new StructureCompare.Cell(stair[0], stair[1], stair[2])), "and it says which");
        plot.set(stair, stairState.setValue(StairBlock.HALF, Half.TOP));
        check(plot.compare().wrong().size() == 1, "an upside-down stair is wrong");
        plot.set(stair, stairState.setValue(StairBlock.SHAPE, StairsShape.INNER_LEFT).setValue(StairBlock.WATERLOGGED, true));
        check(plot.compare().perfect(), "the shape a stair takes and the water in it make no difference");
        plot.set(stair, stairState);

        // Logs and pillars: the way they lie.
        int[] log = plot.find(s -> s.hasProperty(RotatedPillarBlock.AXIS));
        BlockState logState = hut.at(log[0], log[1], log[2]);
        plot.set(log, logState.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        check(plot.compare().wrong().size() == 1, "a log lying the wrong way is wrong");
        plot.set(log, logState);

        // A different block is wrong, none is missing, one where none belongs is extra.
        int[] planks = plot.find(s -> !s.isAir() && !(s.getBlock() instanceof StairBlock) && !s.hasProperty(RotatedPillarBlock.AXIS));
        BlockState planksState = hut.at(planks[0], planks[1], planks[2]);
        plot.set(planks, Blocks.STONE.defaultBlockState());
        check(plot.compare().wrong().size() == 1 && plot.compare().percent() < 100, "another block is wrong");
        plot.set(planks, Blocks.AIR.defaultBlockState());
        StructureCompare.Result missing = plot.compare();
        check(missing.missing().size() == 1 && missing.wrong().isEmpty() && missing.correct() == missing.total() - 1, "a gap is missing: " + missing);
        plot.set(planks, planksState);
        int[] air = {0, 0, 0};
        outer:
        for (int y = 0; y < hut.sizeY(); y++) {
            for (int z = 0; z < hut.sizeZ(); z++) {
                for (int x = 0; x < hut.sizeX(); x++) {
                    if (hut.at(x, y, z).isAir()) {
                        air = new int[]{x, y, z};
                        break outer;
                    }
                }
            }
        }
        plot.set(air, Blocks.DIRT.defaultBlockState());
        StructureCompare.Result extra = plot.compare();
        check(extra.extra().size() == 1 && !extra.perfect() && extra.correct() == extra.total(), "a block where none belongs is extra: " + extra);
        // Extras count up to two layers above the model, and not beyond (the plot ends there).
        plot.set(air, Blocks.AIR.defaultBlockState());
        plot.set(new int[]{0, hut.sizeY() + StructureCompare.EXTRA_HEIGHT - 1, 0}, Blocks.DIRT.defaultBlockState());
        check(plot.compare().extra().size() == 1, "a block on the top layer of the plot is extra");
        plot.set(new int[]{0, hut.sizeY() + StructureCompare.EXTRA_HEIGHT - 1, 0}, Blocks.AIR.defaultBlockState());
        plot.set(new int[]{0, hut.sizeY() + StructureCompare.EXTRA_HEIGHT, 0}, Blocks.DIRT.defaultBlockState());
        check(plot.compare().perfect(), "above the plot nothing is looked at");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void slabsLanternsAndFencesAreComparedByWhatYouSee(GameTestHelper helper) {
        // The temple has slabs on its roof; the altar hangs lanterns from its beams; the bridge has walls.
        StructureModel temple = StructureLibrary.generate(3, 2, 3);
        Plot plot = new Plot(temple);
        int[] slab = plot.find(s -> s.getBlock() instanceof SlabBlock);
        BlockState slabState = temple.at(slab[0], slab[1], slab[2]);
        check(slabState.getValue(SlabBlock.TYPE) == SlabType.BOTTOM, "the roof is bottom slabs");
        plot.set(slab, slabState.setValue(SlabBlock.TYPE, SlabType.TOP));
        check(plot.compare().wrong().size() == 1, "a slab in the top half is wrong");
        plot.set(slab, slabState.setValue(SlabBlock.TYPE, SlabType.DOUBLE));
        check(plot.compare().wrong().size() == 1, "a double slab is wrong");
        plot.set(slab, slabState);
        check(plot.compare().perfect(), "and right again");

        StructureModel altar = StructureLibrary.generate(3, 2, 4);
        plot = new Plot(altar);
        int[] lantern = plot.find(s -> s.getBlock() instanceof LanternBlock && s.getValue(LanternBlock.HANGING));
        BlockState lanternState = altar.at(lantern[0], lantern[1], lantern[2]);
        plot.set(lantern, lanternState.setValue(LanternBlock.HANGING, false));
        check(plot.compare().wrong().size() == 1, "a lantern standing where one hangs is wrong");
        plot.set(lantern, lanternState.setValue(LanternBlock.WATERLOGGED, true));
        check(plot.compare().perfect(), "but water in it is not");

        StructureModel bridge = StructureLibrary.generate(3, 3, 2);
        plot = new Plot(bridge);
        int[] wall = plot.find(s -> s.getBlock() instanceof WallBlock);
        BlockState wallState = bridge.at(wall[0], wall[1], wall[2]);
        plot.set(wall, wallState.setValue(WallBlock.UP, !wallState.getValue(WallBlock.UP)).setValue(BlockStateProperties.WATERLOGGED, true));
        check(plot.compare().perfect(), "how a wall joins its neighbours does not matter");
        helper.succeed();
    }

    // ---- the supply ----

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void theSupplyIsMoreThanTheModelNeedsOfEveryBlock(GameTestHelper helper) {
        for (int tier = 1; tier <= 3; tier++) {
            for (int seed = 0; seed < 60; seed++) {
                StructureModel model = StructureLibrary.generate(seed, tier);
                for (int percent : new int[]{100, 150, 400}) {
                    Map<Item, Integer> amounts = StructureSupply.amounts(model, percent);
                    check(amounts.keySet().equals(model.itemsNeeded().keySet()), "exactly the kinds the model is made of");
                    for (Map.Entry<Item, Integer> need : model.itemsNeeded().entrySet()) {
                        check(amounts.get(need.getKey()) >= need.getValue() && amounts.get(need.getKey()) >= StructureSupply.MINIMUM,
                                need.getKey() + ": " + amounts.get(need.getKey()) + " given, " + need.getValue() + " needed");
                        if (percent >= 150) {
                            check(amounts.get(need.getKey()) >= need.getValue() * 3 / 2, "a good deal more");
                        }
                    }
                }
                // The chest (three stacks of a kind) holds a kind-full for the biggest model.
                check(model.itemsNeeded().size() * StructureSupply.CHEST_STACKS <= 27, "the supply chest has room for every kind");
            }
        }
        helper.succeed();
    }

    // ---- the room ----

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void theRoomPutsTheModelAndThePlotSideBySideThreeBlocksApart(GameTestHelper helper) {
        for (int tier = 1; tier <= 3; tier++) {
            for (int theme = 0; theme < StructureLibrary.THEMES.size(); theme++) {
                StructureModel model = StructureLibrary.generate(tier * 100L + theme, tier, theme);
                CopyGeometry g = CopyGeometry.of(model);
                String what = model.theme() + " tier " + tier;
                check(g.plotX0() - (g.modelX0() + g.w()) == 3, what + ": the model and the plot are three blocks apart");
                check(g.w() == model.sizeX() && g.d() == model.sizeZ() && g.h() == model.sizeY(), what + ": the plot is as big as the model");
                check(g.inPlot(g.plotX0(), 1, 0) && g.inPlot(g.plotX0() + g.w() - 1, g.plotHeight(), g.d() - 1), what + ": the plot's corners are in it");
                check(!g.inPlot(g.plotX0() - 1, 1, 0) && !g.inPlot(g.plotX0() + g.w(), 1, 0) && !g.inPlot(g.plotX0(), 0, 0) && !g.inPlot(g.plotX0(), g.plotHeight() + 1, 0)
                        && !g.inPlot(g.plotX0(), 1, -1) && !g.inPlot(g.plotX0(), 1, g.d()), what + ": and nothing round it is");
                check(!g.inPlot(g.modelX0(), 1, 0), what + ": the model is not the plot");

                RoadCanvas canvas = new RoadCanvas(0, 0, 320);
                RoadLayout layout = StructureCopyChallenge.draw(canvas, model, 12);
                BlockPos o = canvas.origin();
                // The model is drawn exactly.
                for (int y = 0; y < model.sizeY(); y++) {
                    for (int z = 0; z < model.sizeZ(); z++) {
                        for (int x = 0; x < model.sizeX(); x++) {
                            BlockState want = model.at(x, y, z);
                            BlockState drawn = canvas.drawn(o.offset(g.modelX0() + x, 1 + y, z));
                            check(want.isAir() ? drawn == null || drawn.isAir() : want == drawn, what + ": the model is drawn as it is at " + x + "," + y + "," + z);
                            // and the plot is empty, with the floor under it.
                            BlockPos plotCell = o.offset(g.plotX0() + x, 1 + y, z);
                            check(canvas.drawn(plotCell) == null, what + ": nothing is drawn on the plot");
                        }
                    }
                }
                for (int x = 0; x < g.w(); x++) {
                    for (int z = 0; z < g.d(); z++) {
                        BlockState floor = canvas.drawn(o.offset(g.plotX0() + x, 0, z));
                        BlockState modelFloor = canvas.drawn(o.offset(g.modelX0() + x, 0, z));
                        check(floor != null && (floor.is(Blocks.POLISHED_BLACKSTONE) || floor.is(Blocks.POLISHED_BLACKSTONE_BRICKS)), what + ": a chequered floor under the plot");
                        check(floor == modelFloor, what + ": the same floor under the model");
                    }
                }
                check(canvas.drawn(o.offset(g.plotX0() - 1, 0, -1)).is(Blocks.SEA_LANTERN) && canvas.drawn(o.offset(g.plotX0() + g.w(), 0, g.d())).is(Blocks.SEA_LANTERN),
                        what + ": the plot is outlined in light");
                // The console: the chest between the buttons, all facing the way the player comes from.
                BlockState chest = canvas.drawn(o.offset(0, 1, g.consoleZ()));
                check(chest.is(Blocks.CHEST) && chest.getValue(ChestBlock.FACING) == Direction.SOUTH, what + ": the supply chest");
                BlockState check = canvas.drawn(o.offset(-1, 1, g.consoleZ() + 1));
                BlockState hint = canvas.drawn(o.offset(1, 1, g.consoleZ() + 1));
                check(check.is(Blocks.WARPED_BUTTON) && hint.is(Blocks.CHERRY_BUTTON) && check.getValue(ButtonBlock.FACING) == Direction.SOUTH, what + ": the buttons");
                // The start platform is on the south side, looking north at the room, and the way to the console is open.
                check(layout.start().z - o.getZ() > g.consoleZ() + 2 && Math.abs(layout.start().x - o.getX() - 0.5) < 1.0E-9, what + ": the player starts in front of the console");
                for (int z = g.consoleZ() + 2; z < g.startZ(); z++) {
                    for (int x = -1; x <= 1; x++) {
                        check(canvas.drawn(o.offset(x, 0, z)) != null && canvas.drawn(o.offset(x, 1, z)) == null && canvas.drawn(o.offset(x, 2, z)) == null,
                                what + ": the way from the start to the console is a clear floor at " + x + "," + z);
                    }
                }
                check(layout.gate() != null && layout.gate().base().equals(o.offset(0, 1, g.gateZ())) && layout.exitDoor() != null && layout.finish() == null
                        && layout.timeLimitTicks() == 0 && layout.checkpoints().size() == 1, what + ": a gate at the end of the aisle, an exit door, no clock");
                check(layout.checkpoints().get(0).failY() < o.getY() - 2, what + ": falling is below the roots under the platform");
                check(canvas.chunks().length <= 36, what + ": the room is small: " + canvas.chunks().length + " chunks");
                // The plot is reserved, so it is cleared with the room.
                check(canvas.minY() <= o.getY() - 2 && canvas.maxY() >= o.getY() + g.plotHeight(), what + ": the heights are covered");
            }
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theBlockOfTheModelIsAlwaysSomethingHeldByHand(GameTestHelper helper) {
        Set<Block> blocks = new HashSet<>();
        for (int tier = 1; tier <= 3; tier++) {
            for (int theme = 0; theme < StructureLibrary.THEMES.size(); theme++) {
                for (int seed = 0; seed < 40; seed++) {
                    StructureModel model = StructureLibrary.generate(seed, tier, theme);
                    for (int y = 0; y < model.sizeY(); y++) {
                        for (int z = 0; z < model.sizeZ(); z++) {
                            for (int x = 0; x < model.sizeX(); x++) {
                                BlockState state = model.at(x, y, z);
                                if (!state.isAir()) {
                                    blocks.add(state.getBlock());
                                    check(state.getBlock().getStateDefinition().getPossibleStates().contains(state), "a real state of " + state.getBlock());
                                }
                            }
                        }
                    }
                }
            }
        }
        check(blocks.size() >= 40, "a good range of blocks in the whole library: " + blocks.size());
        check(blocks.stream().anyMatch(b -> b instanceof StairBlock) && blocks.stream().anyMatch(b -> b instanceof SlabBlock)
                && blocks.stream().anyMatch(b -> b instanceof RotatedPillarBlock) && blocks.contains(Blocks.CAULDRON), "stairs, slabs, logs and the cauldron");
        helper.succeed();
    }

    private RoadStructureModelTests() {
    }
}
