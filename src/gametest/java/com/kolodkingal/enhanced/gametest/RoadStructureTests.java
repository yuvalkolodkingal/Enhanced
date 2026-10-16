package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.WitchesRoadConfig;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.road.RoadCanvas;
import com.kolodkingal.enhanced.road.RoadChallenge;
import com.kolodkingal.enhanced.road.RoadChallenges;
import com.kolodkingal.enhanced.road.RoadDimension;
import com.kolodkingal.enhanced.road.RoadSave;
import com.kolodkingal.enhanced.road.RoadSweeper;
import com.kolodkingal.enhanced.road.RoadTrial;
import com.kolodkingal.enhanced.road.RoadTrials;
import com.kolodkingal.enhanced.road.RoadVoice;
import com.kolodkingal.enhanced.road.copy.CopyGeometry;
import com.kolodkingal.enhanced.road.copy.StructureCompare;
import com.kolodkingal.enhanced.road.copy.StructureCopyChallenge;
import com.kolodkingal.enhanced.road.copy.StructureCopyEvents;
import com.kolodkingal.enhanced.road.copy.StructureLibrary;
import com.kolodkingal.enhanced.road.copy.StructureModel;
import com.kolodkingal.enhanced.road.copy.StructureSupply;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * Trial 2, Copy the Structure, in a Road level: the room is built and the blocks are given, building is allowed on the plot and
 * nowhere else, Check counts and marks, Hint shows ghosts, a perfect copy wins and opens the gate, and whatever way the trial ends the
 * player has his things back and the room is gone. The tests that change an option have a batch of their own
 * ({@link RoadStructureOptionTests}).
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class RoadStructureTests {
    static final String BATCH = "road_structure";

    static int index() {
        return RoadChallenges.indexOf(RoadChallenges.byId(StructureCopyChallenge.ID));
    }

    /** The player walks into this trial (an operator's way in: no door, no Gene needed). */
    static void enter(ServerPlayer player) {
        check(RoadTrials.enter(player, index()), "he walks into the trial");
    }

    /** Puts the model's blocks on the plot (what a player does by hand), except the cells given as x, y, z triples. */
    static void build(ServerLevel level, RoadTrial trial, int... except) {
        StructureModel model = StructureCopyChallenge.model(trial);
        BlockPos corner = StructureCopyChallenge.plotCorner(trial);
        for (int y = 0; y < model.sizeY(); y++) {
            for (int z = 0; z < model.sizeZ(); z++) {
                for (int x = 0; x < model.sizeX(); x++) {
                    BlockState state = model.at(x, y, z);
                    if (!state.isAir() && !skip(except, x, y, z)) {
                        level.setBlock(corner.offset(x, y, z), state, 2);
                    }
                }
            }
        }
    }

    private static boolean skip(int[] except, int x, int y, int z) {
        for (int i = 0; i + 2 < except.length; i += 3) {
            if (except[i] == x && except[i + 1] == y && except[i + 2] == z) {
                return true;
            }
        }
        return false;
    }

    /** The cell of the model that holds a block the test likes (after skipping some). */
    static int[] cell(StructureModel model, Predicate<BlockState> test, int skip) {
        for (int y = 0; y < model.sizeY(); y++) {
            for (int z = 0; z < model.sizeZ(); z++) {
                for (int x = 0; x < model.sizeX(); x++) {
                    if (test.test(model.at(x, y, z)) && skip-- <= 0) {
                        return new int[]{x, y, z};
                    }
                }
            }
        }
        throw new IllegalStateException("no such cell");
    }

    static boolean plain(BlockState s) {
        return !s.isAir() && !(s.getBlock() instanceof StairBlock);
    }

    /** A block that has a front, a half, a kind or a way it lies. */
    static boolean hasFront(BlockState s) {
        return s.getBlock() instanceof StairBlock || s.getBlock() instanceof SlabBlock || s.hasProperty(RotatedPillarBlock.AXIS);
    }

    /** The same block the wrong way round. */
    static BlockState spoil(BlockState state) {
        if (state.getBlock() instanceof StairBlock) {
            return state.setValue(StairBlock.FACING, state.getValue(StairBlock.FACING).getOpposite());
        }
        if (state.getBlock() instanceof SlabBlock) {
            return state.setValue(SlabBlock.TYPE, state.getValue(SlabBlock.TYPE) == SlabType.TOP ? SlabType.BOTTOM : SlabType.TOP);
        }
        return state.setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
    }

    /** How many of an item (the trial's marked blocks) the player holds. */
    static int held(ServerPlayer player, Item item) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item) && StructureSupply.isSupply(stack)) {
                n += stack.getCount();
            }
        }
        return n;
    }

    static int supplyStacks(ServerPlayer player) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (StructureSupply.isSupply(player.getInventory().getItem(i))) {
                n++;
            }
        }
        return n;
    }

    static ChestBlockEntity chest(GameTestHelper helper, RoadTrial trial) {
        return (ChestBlockEntity) RoadTestSupport.road(helper).getBlockEntity(StructureCopyChallenge.supplyChest(trial));
    }

    // ---- the room and the supply ----

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void theTrialIsTheSecondAndItsRoomIsBuiltWithTheModelAndAnEmptyPlot(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        check(index() == 1 && RoadChallenges.get(0).id().equals("parkour"), "it is trial 2, after the parkour: " + index());
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        enter(player);
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    ServerLevel road = RoadTestSupport.road(helper);
                    StructureModel model = StructureCopyChallenge.model(trial);
                    BlockPos mc = StructureCopyChallenge.modelCorner(trial);
                    BlockPos pc = StructureCopyChallenge.plotCorner(trial);
                    check(trial.challenge().id().equals("structure_copy") && model.tier() == 2, "the medium model: " + model.tier());
                    check(pc.getX() - (mc.getX() + model.sizeX()) == 3 && pc.getY() == mc.getY() && pc.getZ() == mc.getZ(), "the plot is three blocks from the model, the same way round");
                    for (int y = 0; y < model.sizeY(); y++) {
                        for (int z = 0; z < model.sizeZ(); z++) {
                            for (int x = 0; x < model.sizeX(); x++) {
                                BlockState want = model.at(x, y, z);
                                BlockState have = road.getBlockState(mc.offset(x, y, z));
                                check(want.isAir() ? have.isAir() : StructureCompare.same(want, have), "the model stands as it is at " + x + "," + y + "," + z + ": " + have);
                            }
                        }
                    }
                    AABB box = StructureCopyChallenge.plotBox(trial);
                    for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX - 1, box.maxY - 1, box.maxZ - 1))) {
                        check(road.getBlockState(pos).isAir(), "the plot is empty at " + pos.toShortString());
                    }
                    check(!road.getBlockState(pc.below()).isAir(), "with a floor under it");
                    check(road.getBlockState(StructureCopyChallenge.checkButton(trial)).is(Blocks.WARPED_BUTTON) && road.getBlockState(StructureCopyChallenge.hintButton(trial)).is(Blocks.CHERRY_BUTTON)
                            && road.getBlockState(StructureCopyChallenge.supplyChest(trial)).is(Blocks.CHEST), "the buttons and the chest stand where the room says");
                    StructureCompare.Result none = StructureCopyChallenge.compare(trial);
                    check(none.missing().size() == model.solidCount() && none.total() == model.solidCount() && none.correct() == 0, "nothing of the copy is there yet");
                    check(player.position().distanceTo(trial.layout().start()) < 0.01 && RoadTrials.isInside(player), "he stands on the start platform");
                    check(player.getInventory().countItem(ModItems.SCARLET_WITCH_GENE.get()) == 1, "with his Gene");
                    check(trial.tracked().size() >= 5, "the labels float over the room: " + trial.tracked().size());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void everyThemeStandsInTheRoadAsDrawnWithTheCornersAndJointsTheGameGives(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel road = RoadDimension.level(server);
        RoadSave save = RoadSave.get(server);
        UUID owner = UUID.randomUUID();
        List<Integer> cells = new ArrayList<>();
        f.onClose(() -> {
            for (int cell : cells) {
                RoadSweeper.queue(server, cell, true);
            }
            RoadSweeper.drain(server);
        });
        int walls = 0;
        for (int tier = 1; tier <= 3; tier++) {
            for (int theme = 0; theme < StructureLibrary.THEMES.size(); theme++) {
                StructureModel model = StructureLibrary.generate(77L + theme, tier, theme);
                int cell = save.allocate(owner, "test", 64);
                cells.add(cell);
                RoadCanvas canvas = new RoadCanvas(cell, road.getMinBuildHeight(), road.getMaxBuildHeight());
                StructureCopyChallenge.draw(canvas, model, 12);
                canvas.seal();
                save.plan(cell, canvas.chunks(), canvas.minY(), canvas.maxY());
                RoadSweeper.hold(road, owner, canvas.chunks());
                while (!canvas.placeSome(road, 100000)) {
                    // placed in slices
                }
                BlockPos corner = canvas.origin().offset(CopyGeometry.of(model).modelX0(), 1, 0);
                StructureCopyChallenge.settle(road, canvas, corner, model);
                for (int y = 0; y < model.sizeY(); y++) {
                    for (int z = 0; z < model.sizeZ(); z++) {
                        for (int x = 0; x < model.sizeX(); x++) {
                            BlockState want = model.at(x, y, z);
                            BlockState have = road.getBlockState(corner.offset(x, y, z));
                            String what = model.theme() + " tier " + tier + " at " + x + "," + y + "," + z + ": " + have;
                            check(want.isAir() ? have.isAir() : StructureCompare.same(want, have), "as drawn, " + what);
                            if (have.getBlock() instanceof StairBlock) {
                                check(have.getValue(StairBlock.SHAPE) == StairsShape.STRAIGHT, "a straight stair, " + what);
                            }
                            if (have.getBlock() instanceof WallBlock && (have.getValue(WallBlock.EAST_WALL) != WallSide.NONE || have.getValue(WallBlock.WEST_WALL) != WallSide.NONE)) {
                                walls++;
                            }
                        }
                    }
                }
            }
        }
        check(walls > 10, "the walls of the bridges are joined up: " + walls);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void thePlayerIsGivenExactlyTheKindsOfBlockTheModelNeedsInGenerousQuantity(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        player.getInventory().setItem(5, new ItemStack(Items.DIAMOND, 3));
        player.getInventory().setItem(20, new ItemStack(Items.STONE, 64));
        player.getInventory().setItem(39, new ItemStack(Items.IRON_HELMET));
        enter(player);
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    StructureModel model = StructureCopyChallenge.model(trial);
                    Map<Item, Integer> needs = model.itemsNeeded();
                    check(needs.size() >= 3, "several kinds of block: " + needs.size());
                    for (Map.Entry<Item, Integer> need : needs.entrySet()) {
                        check(held(player, need.getKey()) >= need.getValue() * 3 / 2 && held(player, need.getKey()) >= StructureSupply.MINIMUM,
                                need.getKey() + ": " + held(player, need.getKey()) + " held, " + need.getValue() + " needed");
                    }
                    int others = 0;
                    for (int i = 0; i < 36; i++) {
                        ItemStack stack = player.getInventory().getItem(i);
                        if (!stack.isEmpty() && !StructureSupply.isSupply(stack) && !stack.is(ModItems.SCARLET_WITCH_GENE.get())) {
                            others++;
                        }
                        if (StructureSupply.isSupply(stack)) {
                            check(needs.containsKey(stack.getItem()), "only the model's own kinds: " + stack);
                        }
                    }
                    check(others == 0 && player.getInventory().countItem(Items.DIAMOND) == 0 && player.getInventory().countItem(Items.STONE) == 0, "his own things are put aside");
                    check(player.getInventory().getItem(39).is(Items.IRON_HELMET), "but not what he wears");
                    Item most = needs.keySet().iterator().next();
                    boolean hotbar = false;
                    for (int i = 0; i < 9; i++) {
                        hotbar |= player.getInventory().getItem(i).is(most) && StructureSupply.isSupply(player.getInventory().getItem(i));
                    }
                    check(hotbar, "the most needed kind is on the hotbar");
                    check(StructureSupply.hasStash(player, StructureCopyChallenge.ID), "and what was put aside is saved");
                    ChestBlockEntity chest = chest(helper, trial);
                    check(chest != null, "there is a supply chest");
                    int stacks = 0;
                    for (int i = 0; i < chest.getContainerSize(); i++) {
                        ItemStack stack = chest.getItem(i);
                        if (!stack.isEmpty()) {
                            stacks++;
                            check(StructureSupply.isSupply(stack) && stack.getCount() == stack.getMaxStackSize() && needs.containsKey(stack.getItem()), "a full, marked stack of a kind: " + stack);
                        }
                    }
                    check(stacks == needs.size() * StructureSupply.CHEST_STACKS, "three stacks of each kind: " + stacks);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 500)
    public static void theSupplyChestFillsItselfUpAgain(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        enter(player);
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    ChestBlockEntity chest = chest(helper, RoadTestSupport.trial(player));
                    chest.setItem(0, ItemStack.EMPTY);
                    chest.getItem(1).shrink(40);
                    chest.setItem(2, new ItemStack(Items.DIAMOND, 5));
                })
                .thenWaitUntil(() -> {
                    ChestBlockEntity chest = chest(helper, RoadTestSupport.trial(player));
                    for (int slot = 0; slot < 3; slot++) {
                        ItemStack stack = chest.getItem(slot);
                        check(StructureSupply.isSupply(stack) && stack.getCount() == stack.getMaxStackSize(), "slot " + slot + " is full again: " + stack);
                    }
                })
                .thenSucceed();
    }

    // ---- building ----

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 500)
    public static void blocksCanBePlacedAndBrokenOnThePlotAndNowhereElse(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        enter(player);
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    ServerLevel road = RoadTestSupport.road(helper);
                    StructureModel model = StructureCopyChallenge.model(trial);
                    BlockPos pc = StructureCopyChallenge.plotCorner(trial);
                    BlockPos mc = StructureCopyChallenge.modelCorner(trial);
                    BlockState stone = Blocks.STONE.defaultBlockState();
                    BlockPos inside = pc.offset(model.sizeX() - 1, model.sizeY() + 1, model.sizeZ() - 1);
                    check(!NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(road.dimension(), road, pc), stone, player)).isCanceled(), "he places on the plot's first cell");
                    check(!NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(road.dimension(), road, inside), stone, player)).isCanceled(),
                            "and on its last, two layers above the model");
                    check(!NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(road, pc, stone, player)).isCanceled(), "he breaks there");
                    // Not under it, not beside it, not above it, not on the model, not on the platform, not the console.
                    for (BlockPos no : new BlockPos[]{pc.below(), pc.west(), pc.east(model.sizeX()), pc.north(), pc.south(model.sizeZ()), inside.above(), mc, mc.above(2),
                            mc.offset(model.sizeX() - 1, 0, model.sizeZ() - 1), pc.offset(-3, -1, 8), trial.layout().exitDoor(), StructureCopyChallenge.checkButton(trial),
                            StructureCopyChallenge.supplyChest(trial)}) {
                        check(NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(road.dimension(), road, no), stone, player)).isCanceled(),
                                "he cannot place at " + no.toShortString());
                        check(NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(road, no, road.getBlockState(no), player)).isCanceled(), "or break at " + no.toShortString());
                    }
                    // By the real way: right-clicking with a block of his supply in his hand.
                    int slot = 0;
                    while (!StructureSupply.isSupply(player.getInventory().getItem(slot))) {
                        slot++;
                    }
                    player.getInventory().selected = slot;
                    int before = player.getInventory().getItem(slot).getCount();
                    BlockPos floor = pc.below();
                    InteractionResult onPlot = player.gameMode.useItemOn(player, road, player.getInventory().getItem(slot), InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false));
                    check(onPlot.consumesAction() && !road.getBlockState(pc).isAir() && player.getInventory().getItem(slot).getCount() == before - 1,
                            "a block put on the plot's floor is placed and used up: " + onPlot + " " + road.getBlockState(pc));
                    BlockPos platform = mc.below().offset(model.sizeX() + 1, 0, 0);
                    player.gameMode.useItemOn(player, road, player.getInventory().getItem(slot), InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(platform).add(0, 0.5, 0), Direction.UP, platform, false));
                    check(road.getBlockState(platform.above()).isAir() && player.getInventory().getItem(slot).getCount() == before - 1, "but not on the platform beside the plot");
                    BlockPos modelFloor = mc.below().west();
                    player.gameMode.useItemOn(player, road, player.getInventory().getItem(slot), InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(modelFloor).add(0, 0.5, 0), Direction.UP, modelFloor, false));
                    check(road.getBlockState(modelFloor.above()).isAir() && player.getInventory().getItem(slot).getCount() == before - 1, "nor beside the model");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 500)
    public static void breakingOnThePlotIsInstantAndGivesTheBlockBack(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        ServerPlayer outside = RoadTestSupport.walker(f, 6.5, 6.5);
        enter(player);
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    ServerLevel road = RoadTestSupport.road(helper);
                    StructureModel model = StructureCopyChallenge.model(trial);
                    BlockPos pc = StructureCopyChallenge.plotCorner(trial);
                    int[] cell = cell(model, RoadStructureTests::plain, 0);
                    BlockState state = model.at(cell[0], cell[1], cell[2]);
                    Item item = state.getBlock().asItem();
                    BlockPos pos = pc.offset(cell[0], cell[1], cell[2]);
                    road.setBlock(pos, state, 2);
                    int before = held(player, item);
                    check(player.gameMode.destroyBlock(pos), "he breaks his block");
                    check(road.getBlockState(pos).isAir(), "it is gone");
                    check(held(player, item) == before + 1, "and he has it back, marked as the trial's: " + before + " -> " + held(player, item));
                    check(road.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(5.0)).isEmpty(), "nothing dropped on the floor");
                    // Whatever the block would drop: glass drops nothing, a double slab is two slabs.
                    BlockPos glass = pc.offset(0, 0, 0);
                    road.setBlock(glass, Blocks.GLASS.defaultBlockState(), 2);
                    int glasses = held(player, Items.GLASS);
                    check(player.gameMode.destroyBlock(glass) && held(player, Items.GLASS) == glasses + 1, "a block that drops nothing (glass) comes back all the same");
                    road.setBlock(glass, Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE), 2);
                    int slabs = held(player, Items.OAK_SLAB);
                    check(player.gameMode.destroyBlock(glass) && held(player, Items.OAK_SLAB) == slabs + 2, "and a double slab gives two slabs");
                    BlockPos modelBlock = StructureCopyChallenge.modelCorner(trial).offset(cell[0], cell[1], cell[2]);
                    check(!player.gameMode.destroyBlock(modelBlock) && !road.getBlockState(modelBlock).isAir(), "the model stays");
                    check(!player.gameMode.destroyBlock(pc.below()) && !road.getBlockState(pc.below()).isAir(), "so does the floor");
                    PlayerEvent.BreakSpeed speed = NeoForge.EVENT_BUS.post(new PlayerEvent.BreakSpeed(player, Blocks.OBSIDIAN.defaultBlockState(), 1.0F, pos));
                    check(speed.getNewSpeed() >= 1000.0F, "breaking is instant in the Road: " + speed.getNewSpeed());
                    PlayerEvent.BreakSpeed normal = NeoForge.EVENT_BUS.post(new PlayerEvent.BreakSpeed(outside, Blocks.OBSIDIAN.defaultBlockState(), 1.0F, BlockPos.ZERO));
                    check(normal.getNewSpeed() == 1.0F, "outside the Road a block breaks as slowly as ever: " + normal.getNewSpeed());
                })
                .thenSucceed();
    }

    // ---- Check ----

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 500)
    public static void checkCountsWhatIsRightAndMarksWhatIsMissingWrongOrExtra(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        enter(player);
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    ServerLevel road = RoadTestSupport.road(helper);
                    StructureModel model = StructureCopyChallenge.model(trial);
                    BlockPos pc = StructureCopyChallenge.plotCorner(trial);
                    int[] missing = cell(model, RoadStructureTests::plain, 3);
                    int[] wrong = cell(model, RoadStructureTests::plain, 10);
                    build(road, trial, missing[0], missing[1], missing[2], wrong[0], wrong[1], wrong[2]);
                    road.setBlock(pc.offset(wrong[0], wrong[1], wrong[2]), Blocks.TNT.defaultBlockState(), 2);
                    int[] air = cell(model, BlockState::isAir, 0);
                    road.setBlock(pc.offset(air[0], air[1], air[2]), Blocks.DIRT.defaultBlockState(), 2);
                    StructureCompare.Result now = StructureCopyChallenge.compare(trial);
                    check(now.missing().size() == 1 && now.wrong().size() == 1 && now.extra().size() == 1 && now.correct() == now.total() - 2, "one of each: " + now);
                    check(StructureCopyChallenge.use(trial, player, StructureCopyChallenge.checkButton(trial)), "the Check button is a button of the room");
                    check(trial.phase() == RoadTrial.Phase.ACTIVE, "he has not won");
                    check(!StructureCopyChallenge.use(trial, player, StructureCopyChallenge.checkButton(trial).above(3)), "a block that is no button does nothing");
                    int[] marks = StructureCopyChallenge.markCounts(trial);
                    check(marks[0] == 1 && marks[1] == 1 && marks[2] == 1, "a missing, a wrong and an extra place are marked: " + java.util.Arrays.toString(marks));
                })
                .thenSucceed();
    }

    // ---- winning ----

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 500)
    public static void aPerfectCopyWinsOnItsOwnAndOpensTheGate(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        enter(player);
        int[][] last = new int[1][];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    last[0] = cell(StructureCopyChallenge.model(trial), RoadStructureTests::plain, 2);
                    check(RoadTestSupport.road(helper).getBlockState(trial.layout().gate().base().above()).is(Blocks.PURPLE_STAINED_GLASS), "the gate is sealed");
                    build(RoadTestSupport.road(helper), trial, last[0]);
                })
                .thenExecuteAfter(45, () -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(trial.phase() == RoadTrial.Phase.ACTIVE && !StructureCopyChallenge.compare(trial).perfect(), "one block short is not enough");
                    StructureModel model = StructureCopyChallenge.model(trial);
                    RoadTestSupport.road(helper).setBlock(StructureCopyChallenge.plotCorner(trial).offset(last[0][0], last[0][1], last[0][2]), model.at(last[0][0], last[0][1], last[0][2]), 2);
                    check(StructureCopyChallenge.compare(trial).perfect(), "now it is exact");
                })
                .thenWaitUntil(() -> check(RoadTestSupport.trial(player).phase() == RoadTrial.Phase.WON, "the Road sees it and the trial is won"))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(RoadSave.get(server).progress(player.getUUID()).completed() == index() + 1, "the next trial is unlocked");
                    check(RoadTestSupport.road(helper).getBlockState(trial.layout().gate().base().above()).isAir(), "the gate is open");
                    check(StructureCopyChallenge.compare(trial).perfect(), "and the copy stands");
                    BlockPos pc = StructureCopyChallenge.plotCorner(trial);
                    check(NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(RoadTestSupport.road(helper).dimension(), RoadTestSupport.road(helper), pc.above(30)),
                            Blocks.STONE.defaultBlockState(), player)).isCanceled(), "nothing more can be built");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 500)
    public static void aBlockTheWrongWayRoundIsNotACopy(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        enter(player);
        int[][] at = new int[1][];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    StructureModel model = StructureCopyChallenge.model(trial);
                    at[0] = cell(model, RoadStructureTests::hasFront, 0);
                    build(RoadTestSupport.road(helper), trial);
                    RoadTestSupport.road(helper).setBlock(StructureCopyChallenge.plotCorner(trial).offset(at[0][0], at[0][1], at[0][2]),
                            spoil(model.at(at[0][0], at[0][1], at[0][2])), 2);
                    StructureCompare.Result result = StructureCopyChallenge.compare(trial);
                    check(result.wrong().size() == 1 && result.missing().isEmpty() && result.extra().isEmpty() && result.percent() < 100, "only the one block is wrong: " + result);
                })
                .thenExecuteAfter(45, () -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(trial.phase() == RoadTrial.Phase.ACTIVE, "one block the wrong way round and the trial is not won");
                    StructureModel model = StructureCopyChallenge.model(trial);
                    RoadTestSupport.road(helper).setBlock(StructureCopyChallenge.plotCorner(trial).offset(at[0][0], at[0][1], at[0][2]), model.at(at[0][0], at[0][1], at[0][2]), 2);
                })
                .thenWaitUntil(() -> check(RoadTestSupport.trial(player).phase() == RoadTrial.Phase.WON, "turned right, it is won"))
                .thenSucceed();
    }

    // ---- the end of the trial, whatever way ----

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 500)
    public static void leavingGivesBackHisThingsAndTakesTheRoomDown(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        player.getInventory().setItem(5, new ItemStack(Items.DIAMOND, 3));
        player.getInventory().setItem(20, new ItemStack(Items.STONE, 64));
        player.getInventory().setItem(30, new ItemStack(Items.APPLE, 7));
        enter(player);
        long[][] chunks = new long[1][];
        BlockPos[] origin = new BlockPos[1];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    chunks[0] = trial.chunks();
                    origin[0] = trial.canvas().origin();
                    check(supplyStacks(player) > 0 && player.getInventory().countItem(Items.DIAMOND) == 0, "he holds the supply, not his things");
                    build(RoadTestSupport.road(helper), trial);
                    RoadTrials.leave(player, RoadChallenge.End.LEFT);
                    check(!RoadDimension.in(player) && supplyStacks(player) == 0, "back in the world with none of the supply");
                    check(player.getInventory().getItem(5).is(Items.DIAMOND) && player.getInventory().getItem(5).getCount() == 3 && player.getInventory().getItem(20).is(Items.STONE)
                            && player.getInventory().getItem(20).getCount() == 64 && player.getInventory().getItem(30).is(Items.APPLE) && player.getInventory().getItem(30).getCount() == 7,
                            "his things are in the slots they were in");
                    check(player.getInventory().countItem(ModItems.SCARLET_WITCH_GENE.get()) == 1, "and the Gene");
                    check(!StructureSupply.hasStash(player, StructureCopyChallenge.ID), "nothing is left put aside");
                    RoadSweeper.drain(server);
                    check(RoadTestSupport.blocksIn(RoadTestSupport.road(helper), chunks[0]) == 0, "the room, the model and the copy are gone");
                    check(RoadTestSupport.road(helper).getEntitiesOfClass(ItemEntity.class, new AABB(origin[0]).inflate(80.0)).isEmpty(),
                            "and the supply chest's blocks did not spill on the floor of the next room built here");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void loggingOutInTheTrialReturnsHisThingsAndLoggingInStartsTheRoomAgain(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        player.getInventory().setItem(5, new ItemStack(Items.DIAMOND, 3));
        // He has won the parkour: this is the trial he is on, so it is the one he is put into when he logs in.
        RoadTrials.setCompleted(server, player.getUUID(), index());
        enter(player);
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    build(RoadTestSupport.road(helper), trial);
                    check(!trial.challenge().resumable(), "what he built is not kept");
                    RoadTrials.onLogout(player);
                    check(supplyStacks(player) == 0 && player.getInventory().getItem(5).is(Items.DIAMOND) && player.getInventory().getItem(5).getCount() == 3,
                            "when he logs out his things are his again (that is what is saved)");
                    RoadSweeper.drain(server);
                    RoadTrials.reconcile(player);
                    check(RoadTrials.isInside(player), "he logs in inside the Road: the room is built again");
                })
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(StructureCopyChallenge.compare(trial).correct() == 0, "a new, empty plot");
                    check(supplyStacks(player) > 0 && player.getInventory().countItem(Items.DIAMOND) == 0 && StructureSupply.hasStash(player, StructureCopyChallenge.ID),
                            "with the supply again, and his things put aside again");
                    RoadTrials.leave(player, RoadChallenge.End.LEFT);
                    check(player.getInventory().getItem(5).getCount() == 3 && supplyStacks(player) == 0, "and given back when he leaves");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void aCrashInTheTrialCannotCostHimHisThings(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        player.getInventory().setItem(5, new ItemStack(Items.DIAMOND, 3));
        player.getInventory().setItem(21, new ItemStack(Items.GOLD_INGOT, 9));
        StructureModel model = StructureLibrary.generate(5, 2);
        // The state a crash leaves: the supply in the backpack, his things in the save, and no trial running.
        StructureSupply.give(player, model, StructureCopyChallenge.ID);
        check(supplyStacks(player) > 0 && player.getInventory().countItem(Items.DIAMOND) == 0 && StructureSupply.hasStash(player, StructureCopyChallenge.ID), "he is mid-trial");
        check(!RoadTrials.isInside(player), "(no trial of his is running)");
        StructureCopyEvents.onLogin(new PlayerEvent.PlayerLoggedInEvent(player));
        check(supplyStacks(player) == 0 && !StructureSupply.hasStash(player, StructureCopyChallenge.ID), "when he logs in the supply is gone and nothing is put aside");
        check(player.getInventory().getItem(5).is(Items.DIAMOND) && player.getInventory().getItem(5).getCount() == 3 && player.getInventory().getItem(21).is(Items.GOLD_INGOT)
                && player.getInventory().getItem(21).getCount() == 9 && player.getInventory().countItem(ModItems.SCARLET_WITCH_GENE.get()) == 1, "he has his things back");
        // Supply that is only in the backpack (the save of his things was lost) is taken away all the same.
        player.getInventory().setItem(8, StructureSupply.stack(Items.OAK_PLANKS, 20));
        StructureCopyEvents.onLogin(new PlayerEvent.PlayerLoggedInEvent(player));
        check(supplyStacks(player) == 0, "blocks of the trial never stay in a player's hands");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 500)
    public static void fallingOffThePlatformPutsHimBackAndTheBuildStays(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        enter(player);
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    build(RoadTestSupport.road(helper), trial, 0, 0, 0);
                    check(StructureCopyChallenge.compare(trial).correct() > 0, "he has built");
                    RoadTestSupport.put(player, RoadTestSupport.road(helper), trial.layout().start().add(0.0, -40.0, 0.0));
                })
                .thenExecuteAfter(3, () -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(player.position().distanceTo(trial.layout().start()) < 0.01, "put back on the start platform");
                    check(StructureCopyChallenge.compare(trial).correct() > 0 && supplyStacks(player) > 0, "his build and his blocks are as he left them");
                    check(RoadTrials.isInside(player) && trial.phase() == RoadTrial.Phase.ACTIVE, "and he is still in the trial");
                })
                .thenSucceed();
    }

    /** This test swaps the Road's trials for a Road of its own, which nothing running beside it may see: a batch of its own. */
    @GameTest(template = ARENA, batch = "road_structure_swap", timeoutTicks = 700)
    public static void winningAndSteppingThroughTheGateMovesOnAndGivesBackHisThings(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        RoadChallenge next = new RoadTestSupport.ShortParkour("test_after_copy");
        f.onClose(RoadChallenges.swap(List.of(RoadChallenges.byId(StructureCopyChallenge.ID), next)));
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        player.getInventory().setItem(5, new ItemStack(Items.DIAMOND, 3));
        check(index() == 0, "(a Road of two trials for this test)");
        enter(player);
        RoadTrial[] first = new RoadTrial[1];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    first[0] = RoadTestSupport.trial(player);
                    build(RoadTestSupport.road(helper), first[0]);
                })
                .thenWaitUntil(() -> check(first[0].phase() == RoadTrial.Phase.WON, "won"))
                .thenExecute(() -> {
                    check(RoadSave.get(server).progress(player.getUUID()).completed() == 1, "one won");
                    RoadTestSupport.put(player, RoadTestSupport.road(helper), RoadTestSupport.inside(first[0].layout().gate().zone()));
                })
                .thenWaitUntil(() -> {
                    RoadTrial now = RoadTrials.trialOf(player.getUUID());
                    check(now != null && now != first[0] && now.phase() == RoadTrial.Phase.ACTIVE, "the next trial is ready");
                    player.hasChangedDimension();
                })
                .thenExecute(() -> {
                    check(RoadTestSupport.trial(player).challenge() == next, "he is in the next trial");
                    check(supplyStacks(player) == 0 && player.getInventory().getItem(5).is(Items.DIAMOND) && player.getInventory().getItem(5).getCount() == 3,
                            "the blocks of the last trial are gone and his things are back");
                    check(!StructureSupply.hasStash(player, StructureCopyChallenge.ID), "nothing is put aside");
                })
                .thenSucceed();
    }

    // ---- the seeds and the words ----

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void eachPlayerHasHisOwnModelAndKeepsIt(GameTestHelper helper) {
        int differ = 0;
        UUID base = UUID.randomUUID();
        for (int i = 0; i < 40; i++) {
            UUID a = new UUID(base.getMostSignificantBits() + i, base.getLeastSignificantBits());
            StructureModel one = StructureCopyChallenge.modelFor(RoadTrials.seed(a, StructureCopyChallenge.ID, 0L));
            StructureModel again = StructureCopyChallenge.modelFor(RoadTrials.seed(a, StructureCopyChallenge.ID, 0L));
            check(one.theme().equals(again.theme()) && one.solidCount() == again.solidCount() && one.at(0, 0, 0) == again.at(0, 0, 0), "the same player gets the same model");
            StructureModel other = StructureCopyChallenge.modelFor(RoadTrials.seed(new UUID(a.getMostSignificantBits(), a.getLeastSignificantBits() + 1), StructureCopyChallenge.ID, 0L));
            if (!other.theme().equals(one.theme()) || other.solidCount() != one.solidCount()) {
                differ++;
            }
            check(StructureCopyChallenge.modelFor(RoadTrials.seed(a, StructureCopyChallenge.ID, 1L)).tier() == one.tier(), "a new course keeps the size");
        }
        check(differ >= 20, "another player usually gets another model: " + differ + " of 40");
        helper.succeed();
    }

    private static JsonObject lang(String name) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(RoadStructureTests.class.getResourceAsStream("/assets/enhanced/lang/" + name + ".json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void everythingTheTrialSaysIsSaidInEnglishAndHebrew(GameTestHelper helper) throws IOException {
        JsonObject en = lang("en_us");
        JsonObject he = lang("he_il");
        List<String> keys = new ArrayList<>(List.of("road.enhanced.challenge.structure_copy", "road.enhanced.challenge.structure_copy.goal"));
        for (String key : new String[]{"intro", "model_title", "plot_title", "plot_hint", "check_label", "hint_label", "supply_label", "result", "missing", "wrong", "extra", "marks",
                "perfect", "perfect_short", "hint", "hint_none", "hint_extra", "hints_off", "outside", "time_up"}) {
            keys.add("message.enhanced.road.copy." + key);
        }
        for (String event : new String[]{StructureCopyChallenge.VOICE_START, StructureCopyChallenge.VOICE_TRY, StructureCopyChallenge.VOICE_CLOSE, StructureCopyChallenge.VOICE_HINT}) {
            for (int variant = 0; variant < RoadVoice.VARIANTS; variant++) {
                keys.add(RoadVoice.key(event, variant));
            }
        }
        for (String key : keys) {
            check(en.has(key) && !en.get(key).getAsString().isBlank(), "English: " + key);
            check(he.has(key) && !he.get(key).getAsString().isBlank(), "Hebrew: " + key);
            check(en.get(key).getAsString().split("%s", -1).length == he.get(key).getAsString().split("%s", -1).length, key + " takes the same arguments in both languages");
        }
        check(en.get("road.enhanced.challenge.structure_copy").getAsString().equals("Copy the Structure"), "the English name");
        check(en.get("road.enhanced.challenge.structure_copy.goal").getAsString().length() <= 32 && he.get("road.enhanced.challenge.structure_copy.goal").getAsString().length() <= 32,
                "the goal fits under a title");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theOptionsOfTheTrialAreInTheServerConfigWithTheirDefaults(GameTestHelper helper) {
        check(WitchesRoadConfig.STRUCTURE_COPY_TIER.get() == 2 && WitchesRoadConfig.STRUCTURE_COPY_TIME_LIMIT_SECONDS.get() == 5400
                && WitchesRoadConfig.STRUCTURE_COPY_SUPPLY_PERCENT.get() == 150, "size, the long time limit, the supply");
        check(WitchesRoadConfig.STRUCTURE_COPY_AUTO_CHECK.get() && WitchesRoadConfig.STRUCTURE_COPY_HINTS.get() && WitchesRoadConfig.STRUCTURE_COPY_HINT_SECONDS.get() == 8
                && WitchesRoadConfig.STRUCTURE_COPY_HINT_BLOCKS.get() == 40 && WitchesRoadConfig.STRUCTURE_COPY_MARKS.get() == 40, "checking, hints, marks");
        check(StructureCopyChallenge.tierFor(12345L) == 2, "the default size is medium");
        helper.succeed();
    }

    private RoadStructureTests() {
    }
}
