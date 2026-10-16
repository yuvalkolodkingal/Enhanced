package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.road.RoadCanvas;
import com.kolodkingal.enhanced.road.RoadCells;
import com.kolodkingal.enhanced.road.RoadChallenge;
import com.kolodkingal.enhanced.road.RoadChallenges;
import com.kolodkingal.enhanced.road.RoadDimension;
import com.kolodkingal.enhanced.road.RoadLayout;
import com.kolodkingal.enhanced.road.RoadProgress;
import com.kolodkingal.enhanced.road.RoadSave;
import com.kolodkingal.enhanced.road.RoadSweeper;
import com.kolodkingal.enhanced.road.RoadTrial;
import com.kolodkingal.enhanced.road.RoadTrials;
import com.kolodkingal.enhanced.road.WitchesRoad;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The framework of the Witches' Road trials (3.2): the dimension, the private cells, drawing and taking down a trial,
 * the saved progress, checkpoints, falls and death, leaving, winning and moving on to the next trial, and the rules inside.
 * They share one batch, {@code road_framework}: every test has players of its own, and so cells of its own.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class RoadFrameworkTests {
    static final String BATCH = "road_framework";

    // ---- the place ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void theRoadIsAVoidDimension(GameTestHelper helper) throws IOException {
        // (The GameTest server has only the vanilla dimensions: this reads the data pack's files the way a real server does.)
        RegistryAccess access = helper.getLevel().getServer().registryAccess();
        DimensionType type = access.registryOrThrow(Registries.DIMENSION_TYPE).get(Enhanced.id("witches_road"));
        check(type != null, "the data pack gives the dimension a type");
        check(!type.hasSkyLight() && !type.bedWorks() && !type.respawnAnchorWorks() && !type.hasRaids(), "dark, without beds, anchors or raids");
        check(type.minY() == 0 && type.height() == 320 && type.logicalHeight() == 320, "320 blocks tall from 0");
        check(type.effectsLocation().equals(net.minecraft.world.level.dimension.BuiltinDimensionTypes.NETHER_EFFECTS), "with the fog and no sky of the Nether");
        Biome biome = access.registryOrThrow(Registries.BIOME).get(Enhanced.id("witches_road"));
        check(biome != null, "and a biome");
        check(biome.getMobSettings().getMobs(MobCategory.MONSTER).isEmpty() && biome.getMobSettings().getMobs(MobCategory.CREATURE).isEmpty()
                && biome.getMobSettings().getMobs(MobCategory.AMBIENT).isEmpty(), "nothing spawns in it");
        check(biome.getFogColor() == 735812 && biome.getSkyColor() == 735812, "its fog is dark blue-green");
        check(biome.getAmbientParticle().isPresent(), "warped spores float in it");
        try (InputStreamReader reader = new InputStreamReader(RoadFrameworkTests.class.getResourceAsStream("/data/enhanced/dimension/witches_road.json"),
                StandardCharsets.UTF_8)) {
            LevelStem stem = LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, access), JsonParser.parseReader(reader)).getOrThrow();
            check(stem.type().is(Enhanced.id("witches_road")), "the dimension uses that type");
            check(stem.generator() instanceof FlatLevelSource, "its generator is the flat one: " + stem.generator());
            FlatLevelGeneratorSettings settings = ((FlatLevelSource) stem.generator()).settings();
            check(settings.getBiome().is(Enhanced.id("witches_road")), "with that biome");
            check(settings.getLayersInfo().size() == 1 && settings.getLayersInfo().get(0).getBlockState().isAir(), "and nothing in it: a void");
            check(((FlatLevelSource) stem.generator()).getTypeNameForDataFixer().isPresent(), "a flat generator");
        }
        check(RoadDimension.KEY.location().equals(Enhanced.id("witches_road")), "the key is enhanced:witches_road");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void cellsAreFarApartOnAGrid(GameTestHelper helper) {
        Set<BlockPos> seen = new HashSet<>();
        for (int i = 0; i < 300; i++) {
            BlockPos origin = RoadCells.origin(i);
            check(seen.add(origin), "cell " + i + " has an origin of its own");
            check(RoadCells.indexAt(origin) == i, "and is found again from it");
            check(RoadCells.contains(i, origin.offset(RoadCells.HALF_EXTENT, 0, -RoadCells.HALF_EXTENT)), "its corner is its own");
            check(!RoadCells.contains(i, origin.offset(RoadCells.HALF_EXTENT + 1, 0, 0)), "past it is not");
            check(origin.getX() % 16 == 0 && origin.getZ() % 16 == 0, "it starts on a chunk");
            if (i > 0) {
                BlockPos before = RoadCells.origin(i - 1);
                int apart = Math.max(Math.abs(origin.getX() - before.getX()), Math.abs(origin.getZ() - before.getZ()));
                check(apart == 0 || apart >= RoadCells.SPACING, "neighbouring cells are " + RoadCells.SPACING + " apart: " + apart);
            }
        }
        check(2 * RoadCells.HALF_EXTENT + 1000 < RoadCells.SPACING, "two courses can never come within sight of each other");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void theCanvasKeepsInsideItsCell(GameTestHelper helper) {
        RoadCanvas canvas = new RoadCanvas(3, 0, 320);
        canvas.set(0, 5, 0, Blocks.STONE.defaultBlockState());
        canvas.set(RoadCells.HALF_EXTENT, 5, -RoadCells.HALF_EXTENT, Blocks.STONE.defaultBlockState());
        for (BlockPos bad : new BlockPos[]{canvas.at(RoadCells.HALF_EXTENT + 1, 0, 0), canvas.at(0, 0, -RoadCells.HALF_EXTENT - 1),
                canvas.at(0, -200, 0), canvas.at(0, 400, 0)}) {
            boolean refused = false;
            try {
                canvas.set(bad, Blocks.STONE.defaultBlockState());
            } catch (IllegalArgumentException e) {
                refused = true;
            }
            check(refused, "the canvas refuses " + bad);
        }
        check(canvas.chunks().length == 2, "it knows the two chunks it touched: " + canvas.chunks().length);
        int base = RoadCells.ORIGIN_Y;
        check(canvas.minY() == base + 5 && canvas.maxY() == base + 5, "and the heights");
        canvas.reserve(0, 0, 0, 20, 30, 20);
        check(canvas.minY() == base && canvas.maxY() == base + 30 && canvas.chunks().length == 5, "a reserved volume counts: " + canvas.chunks().length);
        canvas.seal();
        boolean refused = false;
        try {
            canvas.set(0, 6, 0, Blocks.STONE.defaultBlockState());
        } catch (IllegalStateException e) {
            refused = true;
        }
        check(refused, "a sealed canvas takes no more drawing");
        helper.succeed();
    }

    // ---- drawing and taking down ----

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 300)
    public static void aCanvasIsPlacedInSlicesAndClearedWithNothingLeft(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel road = RoadDimension.level(server);
        RoadSave save = RoadSave.get(server);
        java.util.UUID owner = java.util.UUID.randomUUID();
        int cell = save.allocate(owner, "test", 64);
        f.onClose(() -> {
            RoadSweeper.queue(server, cell, true);
            RoadSweeper.drain(server);
        });
        check(cell >= 0 && save.cell(cell) != null, "a cell is allocated and in the ledger");
        RoadCanvas canvas = new RoadCanvas(cell, road.getMinBuildHeight(), road.getMaxBuildHeight());
        // A plank every 7 blocks over 300 blocks: a dozen chunks.
        for (int x = -150; x <= 150; x += 7) {
            canvas.set(x, 4, 3, Blocks.DARK_OAK_PLANKS.defaultBlockState());
            canvas.set(x, 9, 3, Blocks.SOUL_LANTERN.defaultBlockState());
        }
        canvas.seal();
        save.plan(cell, canvas.chunks(), canvas.minY(), canvas.maxY());
        RoadSweeper.hold(road, owner, canvas.chunks());
        int blocks = canvas.blockCount();
        check(!canvas.placeSome(road, 5), "a small budget places a little");
        check(canvas.placedCount() > 0 && canvas.placedCount() < blocks, "and not everything: " + canvas.placedCount() + " of " + blocks);
        while (!canvas.placeSome(road, 40)) {
            check(canvas.placedCount() <= blocks, "it never places more than it holds");
        }
        check(canvas.isPlaced() && RoadTestSupport.blocksIn(road, canvas.chunks()) == blocks, "all " + blocks + " blocks are there");
        check(save.cell(cell).chunks().length == canvas.chunks().length, "the ledger knows which chunks");
        RoadSweeper.queue(server, cell, true);
        check(RoadSweeper.isClearing(cell), "the cell is queued for clearing");
        RoadSweeper.drain(server);
        check(RoadTestSupport.blocksIn(road, canvas.chunks()) == 0, "nothing is left of it");
        check(save.cell(cell) == null && !RoadSweeper.isClearing(cell), "the ledger is clear");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void aTrialIsBuiltForThePlayerAndTakenDownWhenHeLeaves(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel road = RoadDimension.level(server);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        Vec3 where = player.position();
        long[][] chunks = new long[1][];
        int[] cell = new int[1];
        check(RoadTrials.enter(player), "the Road builds a trial for him");
        check(RoadTrials.isInside(player) && RoadTrials.trialOf(player.getUUID()) == null, "not in it until it is built");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(RoadDimension.in(player), "he is in the Road");
                    check(player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL, "as a Survivor");
                    Vec3 start = trial.layout().start();
                    check(player.position().distanceTo(start) < 0.01, "at the start of the course: " + player.position() + " vs " + start);
                    check(RoadCells.contains(trial.cell(), player.blockPosition()), "inside his cell");
                    RoadProgress progress = RoadSave.get(server).existing(player.getUUID());
                    check(progress.inside() && progress.entry() != null && progress.entry().pos().distanceTo(where) < 0.01, "his way back is saved");
                    check(road.getBlockState(BlockPos.containing(start).below()).isSolid(), "there is ground under him");
                    RoadSave.Cell record = RoadSave.get(server).cell(trial.cell());
                    check(record != null && record.owner().equals(player.getUUID()) && record.chunks().length > 3, "the ledger has his cell with its chunks");
                    check(trial.canvas().isPlaced() && RoadTestSupport.blocksIn(road, trial.chunks()) > 500, "the course is there");
                    check(player.getMainHandItem().getCount() == 1, "he still has his Gene");
                    check(RoadTestSupport.tickets(road, player.getUUID()) == trial.chunks().length, "his chunks are held by tickets: " + RoadTestSupport.tickets(road, player.getUUID())
                            + " for " + trial.chunks().length);
                    chunks[0] = trial.chunks();
                    cell[0] = trial.cell();
                    RoadTrials.leave(player, RoadChallenge.End.LEFT);
                    check(!RoadDimension.in(player) && player.serverLevel() == helper.getLevel(), "he is back in the world");
                    check(player.position().distanceTo(where) < 0.01, "where he stood");
                    check(player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL, "in the game mode he came in");
                    check(player.getMainHandItem().getCount() == 1, "with his Gene");
                    check(!progress.inside() && progress.entry() == null, "nothing says he is inside");
                    check(RoadTrials.trialOf(player.getUUID()) == null && !RoadTrials.isInside(player), "no trial");
                    check(RoadSweeper.isClearing(cell[0]), "his cell is queued for clearing");
                    // (In the same tick: a cell that is free again is the next player's, and the tests run side by side.)
                    RoadSweeper.drain(server);
                    check(RoadTestSupport.blocksIn(road, chunks[0]) == 0, "nothing of the course is left");
                    check(RoadSave.get(server).cell(cell[0]) == null, "the cell is free again");
                    check(RoadTestSupport.tickets(road, player.getUUID()) == 0, "and its chunks are let go: " + RoadTestSupport.tickets(road, player.getUUID()) + " tickets left");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void aCellIsClearedInSlicesWithoutHoldingTheServer(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel road = RoadDimension.level(server);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        long[][] chunks = new long[1][];
        int[] cell = new int[1];
        check(RoadTrials.enter(player), "he walks the Road");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    chunks[0] = trial.chunks();
                    cell[0] = trial.cell();
                    RoadTrials.leave(player, RoadChallenge.End.LEFT);
                    check(RoadTestSupport.blocksIn(road, chunks[0]) > 500, "the course stands when he has just left");
                })
                .thenWaitUntil(() -> check(!RoadSweeper.isClearing(cell[0]), "the cell is cleared by the server's own ticks"))
                .thenExecute(() -> {
                    check(RoadTestSupport.blocksIn(road, chunks[0]) == 0, "and nothing is left");
                    check(RoadSave.get(server).cell(cell[0]) == null, "the ledger is clear");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void theSameCourseEveryTimeForTheSamePlayerAndAnotherForAnother(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer a = RoadTestSupport.walker(f, 8.5, 8.5);
        ServerPlayer b = RoadTestSupport.walker(f, 12.5, 12.5);
        long first = RoadTrials.seed(a.getUUID(), "parkour", 0);
        check(first == RoadTrials.seed(a.getUUID(), "parkour", 0), "the same seed for the same player");
        check(first != RoadTrials.seed(b.getUUID(), "parkour", 0), "another for another player");
        check(first != RoadTrials.seed(a.getUUID(), "parkour", 1), "another when an operator deals a new course");
        check(first != RoadTrials.seed(a.getUUID(), "copy", 0), "another for another trial");
        check(RoadTrials.enter(a) && RoadTrials.enter(b), "both walk the Road");
        long[][] chunksB = new long[1][];
        int[] blocksB = new int[1];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(a))
                .thenWaitUntil(RoadTestSupport.active(b))
                .thenExecute(() -> {
                    RoadTrial ta = RoadTestSupport.trial(a);
                    RoadTrial tb = RoadTestSupport.trial(b);
                    check(ta.cell() != tb.cell(), "each has a cell of his own: " + ta.cell() + " " + tb.cell());
                    check(RoadCells.origin(ta.cell()).distSqr(RoadCells.origin(tb.cell())) >= (double) RoadCells.SPACING * RoadCells.SPACING, "far apart");
                    check(ta.layout().start().subtract(Vec3.atLowerCornerOf(RoadCells.origin(ta.cell())))
                            .distanceTo(tb.layout().start().subtract(Vec3.atLowerCornerOf(RoadCells.origin(tb.cell())))) < 0.01, "both start in the middle of theirs");
                    check(a.level() == b.level() && a.position().distanceTo(b.position()) > 1000, "they never see each other");
                    chunksB[0] = tb.chunks();
                    blocksB[0] = RoadTestSupport.blocksIn(RoadTestSupport.road(helper), chunksB[0]);
                    RoadTrials.leave(a, RoadChallenge.End.LEFT);
                    RoadSweeper.drain(server);
                    check(RoadTestSupport.blocksIn(RoadTestSupport.road(helper), chunksB[0]) == blocksB[0] && blocksB[0] > 0, "when one leaves the other's course stays");
                    check(RoadDimension.in(b) && RoadTrials.trialOf(b.getUUID()) == tb, "and he plays on");
                })
                .thenSucceed();
    }

    // ---- the saved progress ----

    @GameTest(template = ARENA, batch = BATCH)
    public static void progressSurvivesTheWorldBeingSaved(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        RoadSave save = new RoadSave();
        java.util.UUID id = java.util.UUID.randomUUID();
        RoadProgress progress = save.progress(id);
        progress.setCompleted(2);
        progress.setCheckpoint(3);
        progress.setInside(true);
        progress.setSalt(7L);
        progress.addFall();
        progress.addStarted();
        progress.setEntry(new RoadProgress.Entry(net.minecraft.world.level.Level.NETHER, new Vec3(1.5, 70.0, -3.25), 90.0F, 12.5F, 2));
        CompoundTag mine = new CompoundTag();
        mine.putInt("best", 1234);
        progress.putData("boats", mine);
        java.util.UUID other = java.util.UUID.randomUUID();
        int cell = save.allocate(other, "parkour", 8);
        save.plan(cell, new long[]{5L, 9L, -4L}, 90, 130);
        CompoundTag tag = save.save(new CompoundTag(), server.registryAccess());
        RoadSave back = RoadSave.load(tag, server.registryAccess());
        RoadProgress read = back.existing(id);
        check(read != null, "his progress is read back");
        check(read.completed() == 2 && read.checkpoint() == 0 || read.completed() == 2, "the trials he has won");
        check(read.checkpoint() == 3 && read.inside() && read.salt() == 7L && read.falls() == 1 && read.started() == 1, "where he is, and the counters");
        RoadProgress.Entry entry = read.entry();
        check(entry != null && entry.dimension().equals(net.minecraft.world.level.Level.NETHER) && entry.pos().equals(new Vec3(1.5, 70.0, -3.25))
                && entry.yaw() == 90.0F && entry.pitch() == 12.5F && entry.gameMode() == 2, "where he came from");
        check(read.data("boats").getInt("best") == 1234 && read.data("nothing").isEmpty(), "what a trial keeps");
        RoadSave.Cell record = back.cell(cell);
        check(record != null && record.owner().equals(other) && record.challenge().equals("parkour") && java.util.Arrays.equals(record.chunks(), new long[]{5L, 9L, -4L})
                && record.minY() == 90 && record.maxY() == 130, "the ledger of cells too");
        check(back.allocate(id, "x", 8) == cell + 1 && back.allocate(id, "x", 8) == cell + 2, "a cell in use is not given twice");
        check(RoadSave.load(new CompoundTag(), server.registryAccess()).players().isEmpty(), "an empty save reads as an empty Road");
        RoadSave blank = new RoadSave();
        blank.progress(java.util.UUID.randomUUID());
        check(RoadSave.load(blank.save(new CompoundTag(), server.registryAccess()), server.registryAccess()).players().isEmpty(),
                "a player with nothing to remember is not kept");
        helper.succeed();
    }

    // ---- checkpoints, falls and death ----

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void fallingPutsThePlayerBackAtHisLastCheckpoint(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        check(RoadTrials.enter(player), "he walks the Road");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    RoadLayout layout = trial.layout();
                    check(layout.checkpoints().size() >= 4, "the course has checkpoints: " + layout.checkpoints().size());
                    Vec3 start = layout.start();
                    // He falls from the start: back at the start.
                    RoadTestSupport.put(player, RoadTestSupport.road(helper), new Vec3(start.x, layout.checkpoint(0).failY() - 3.0, start.z));
                })
                .thenExecuteAfter(2, () -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    Vec3 start = trial.layout().start();
                    check(player.position().distanceTo(start) < 0.01, "put back at the start: " + player.position());
                    check(trial.checkpoint() == 0 && RoadSave.get(server).progress(player.getUUID()).falls() == 1, "a fall is counted");
                    // He reaches checkpoint 2.
                    RoadTestSupport.put(player, RoadTestSupport.road(helper), RoadTestSupport.inside(trial.layout().checkpoint(2).zone()));
                })
                .thenExecuteAfter(2, () -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(trial.checkpoint() == 2, "checkpoint 2 is his: " + trial.checkpoint());
                    check(RoadSave.get(server).progress(player.getUUID()).checkpoint() == 2, "and saved");
                    Vec3 start = trial.layout().start();
                    RoadTestSupport.put(player, RoadTestSupport.road(helper), new Vec3(start.x, trial.layout().checkpoint(2).failY() - 3.0, start.z));
                })
                .thenExecuteAfter(2, () -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    Vec3 at = trial.layout().checkpoint(2).respawn();
                    check(player.position().distanceTo(at) < 0.01, "put back at checkpoint 2: " + player.position() + " vs " + at);
                    check(trial.checkpoint() == 2 && !player.isDeadOrDying(), "he did not die");
                    // Checkpoints only count forwards: stepping on checkpoint 1 again changes nothing.
                    RoadTestSupport.put(player, RoadTestSupport.road(helper), RoadTestSupport.inside(trial.layout().checkpoint(1).zone()));
                })
                .thenExecuteAfter(2, () -> check(RoadTestSupport.trial(player).checkpoint() == 2, "back on checkpoint 1: still at 2"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void dyingInTheRoadPutsThePlayerBackAtHisCheckpointNotInTheWorld(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        check(RoadTrials.enter(player), "he walks the Road");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    RoadTestSupport.put(player, RoadTestSupport.road(helper), RoadTestSupport.inside(trial.layout().checkpoint(1).zone()));
                })
                .thenExecuteAfter(2, () -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    check(trial.checkpoint() == 1, "at checkpoint 1");
                    RoadTestSupport.put(player, RoadTestSupport.road(helper), trial.layout().start().add(2.0, 0.5, 0.0));
                    // Everything that hurts is turned away; what kills him puts him back.
                    float health = player.getHealth();
                    player.hurt(player.damageSources().generic(), 4.0F);
                    player.hurt(player.damageSources().lava(), 4.0F);
                    player.hurt(player.damageSources().fall(), 4.0F);
                    check(player.getHealth() == health, "nothing hurts him in the Road");
                    check(player.position().distanceTo(trial.layout().start().add(2.0, 0.5, 0.0)) < 0.01, "and nothing moves him");
                    player.kill();
                    check(player.isAlive() && player.getHealth() == player.getMaxHealth(), "he does not die");
                    check(RoadDimension.in(player), "and is still in the Road");
                    Vec3 at = trial.layout().checkpoint(1).respawn();
                    check(player.position().distanceTo(at) < 0.01, "at his last checkpoint: " + player.position() + " vs " + at);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void aPlayerWhoLeavesStartsAgainAtHisCheckpoint(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        check(RoadTrials.enter(player), "he walks the Road");
        Vec3[] second = new Vec3[1];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    second[0] = trial.layout().checkpoint(2).respawn().subtract(Vec3.atLowerCornerOf(RoadCells.origin(trial.cell())));
                    RoadTestSupport.put(player, RoadTestSupport.road(helper), RoadTestSupport.inside(trial.layout().checkpoint(2).zone()));
                })
                .thenExecuteAfter(2, () -> {
                    check(RoadTestSupport.trial(player).checkpoint() == 2, "he reached checkpoint 2");
                    RoadTrials.leave(player, RoadChallenge.End.LEFT);
                    check(RoadSave.get(server).progress(player.getUUID()).checkpoint() == 2 && RoadSave.get(server).progress(player.getUUID()).completed() == 0,
                            "his place is kept");
                    check(RoadTrials.enter(player), "he walks the Road again");
                })
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    Vec3 now = player.position().subtract(Vec3.atLowerCornerOf(RoadCells.origin(trial.cell())));
                    check(trial.checkpoint() == 2 && now.distanceTo(second[0]) < 0.01, "he starts at checkpoint 2: " + now + " vs " + second[0]);
                    check(trial.layout().checkpoint(2).respawn().distanceTo(player.position()) < 0.01, "the same course as before");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void theExitDoorLeavesTheRoadOnTheSecondClick(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        check(RoadTrials.enter(player), "he walks the Road");
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    RoadTrial trial = RoadTestSupport.trial(player);
                    ServerLevel road = RoadTestSupport.road(helper);
                    BlockPos door = trial.layout().exitDoor();
                    check(WitchesRoad.isDoor(road.getBlockState(door)), "an exit door lies on the start platform");
                    check(WitchesRoad.doors(road).doors().isEmpty(), "and it is no door of the Road that heroes walk between");
                    WitchesRoad.use(road, door, player);
                    check(RoadDimension.in(player), "one click only asks");
                    WitchesRoad.use(road, door, player);
                    check(!RoadDimension.in(player), "the second leaves");
                    check(player.getMainHandItem().getCount() == 1, "with the Gene");
                })
                .thenSucceed();
    }

    // ---- winning, and on to the next trial ----

    /** A Road of its own (the registry is swapped): nothing running beside it may see that, so it has a batch of its own. */
    @GameTest(template = ARENA, batch = "road_framework_swap", timeoutTicks = 600)
    public static void winningUnlocksTheNextTrialAndTheGateLeadsToIt(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        RoadChallenge added = new RoadTestSupport.ShortParkour("test_second");
        // A Road of two trials: the parkour and this one (the mod's later trials are not part of this test).
        f.onClose(RoadChallenges.swap(List.of(RoadChallenges.byId("parkour"), added)));
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        check(RoadTrials.enter(player), "he walks the Road");
        RoadTrial[] first = new RoadTrial[1];
        int[] firstCell = new int[1];
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    first[0] = RoadTestSupport.trial(player);
                    firstCell[0] = first[0].cell();
                    RoadProgress progress = RoadSave.get(server).progress(player.getUUID());
                    check(first[0].challenge().id().equals("parkour") && progress.completed() == 0, "trial 1");
                    RoadLayout.Gate gate = first[0].layout().gate();
                    check(RoadTestSupport.road(helper).getBlockState(gate.base().above()).is(Blocks.PURPLE_STAINED_GLASS), "the gate is sealed");
                    // He comes to the finish: the trial is won.
                    RoadTestSupport.put(player, RoadTestSupport.road(helper), RoadTestSupport.inside(first[0].layout().finish()));
                })
                .thenExecuteAfter(3, () -> {
                    RoadProgress progress = RoadSave.get(server).progress(player.getUUID());
                    RoadLayout.Gate gate = first[0].layout().gate();
                    check(first[0].phase() == RoadTrial.Phase.WON, "won: " + first[0].phase());
                    check(progress.completed() == 1, "the next trial is unlocked");
                    check(RoadTestSupport.road(helper).getBlockState(gate.base().above()).isAir(), "the gate is open");
                    // He steps through the gate.
                    RoadTestSupport.put(player, RoadTestSupport.road(helper), RoadTestSupport.inside(gate.zone()));
                })
                .thenWaitUntil(() -> {
                    RoadTrial now = RoadTrials.trialOf(player.getUUID());
                    check(now != null && now != first[0] && now.phase() == RoadTrial.Phase.ACTIVE, "the second trial is ready");
                    player.hasChangedDimension();
                })
                .thenExecute(() -> {
                    RoadTrial second = RoadTestSupport.trial(player);
                    RoadProgress progress = RoadSave.get(server).progress(player.getUUID());
                    check(second.challenge().id().equals("test_second"), "he is in the second trial");
                    check(second.cell() != firstCell[0], "in a new cell");
                    check(RoadCells.contains(second.cell(), player.blockPosition()) && player.position().distanceTo(second.layout().start()) < 0.01, "at its start");
                    check(first[0].phase() == RoadTrial.Phase.CLEARING, "the old one is coming down");
                    RoadSweeper.drain(server);
                    check(RoadSave.get(server).cell(firstCell[0]) == null, "and gone");
                    check(RoadTestSupport.tickets(RoadTestSupport.road(helper), player.getUUID()) == second.chunks().length, "only the new trial's chunks are held");
                    check(progress.completed() == 1 && progress.checkpoint() == 0 && progress.inside(), "his progress: one won, at the start of the next");
                    // He wins that one too: no trial is left, the Road is walked.
                    RoadTestSupport.put(player, RoadTestSupport.road(helper), RoadTestSupport.inside(second.layout().finish()));
                })
                .thenExecuteAfter(3, () -> {
                    RoadTrial second = RoadTestSupport.trial(player);
                    check(RoadSave.get(server).progress(player.getUUID()).completed() == 2, "two won");
                    RoadTestSupport.put(player, RoadTestSupport.road(helper), RoadTestSupport.inside(second.layout().gate().zone()));
                })
                .thenExecuteAfter(3, () -> {
                    check(!RoadDimension.in(player) && !RoadTrials.isInside(player), "the end of the Road takes him back to the world");
                    check(RoadTrials.walkedAll(player) && RoadTrials.doorMode(player) == RoadTrials.DoorMode.CHOOSE, "the door now offers the choice");
                    check(player.getMainHandItem().getCount() == 1, "the Gene is still his");
                    check(!RoadSave.get(server).progress(player.getUUID()).inside(), "he is not inside");
                })
                .thenSucceed();
    }

    // ---- the commands ----

    private static void run(MinecraftServer server, ServerPlayer as, int permission, String command) {
        server.getCommands().performPrefixedCommand(as.createCommandSourceStack().withPermission(permission), command);
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void operatorsCanSetSkipAndResetTheTrialsAndAnyoneCanLeave(GameTestHelper helper) {
        TestKit.Fixture f = RoadTestSupport.fixture(helper);
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = RoadTestSupport.walker(f, 10.5, 10.5);
        String name = player.getGameProfile().getName();
        RoadSave save = RoadSave.get(server);
        // Players are not operators: nothing happens.
        run(server, player, 0, "enhanced road skip " + name);
        check(save.existing(player.getUUID()) == null || save.existing(player.getUUID()).completed() == 0, "a player cannot skip a trial");
        run(server, player, 2, "enhanced road skip " + name);
        check(save.progress(player.getUUID()).completed() == 1, "an operator counts the next trial as won");
        run(server, player, 2, "enhanced road set " + name + " 0");
        check(save.progress(player.getUUID()).completed() == 0, "and sets how many are won");
        run(server, player, 2, "enhanced road set " + name + " 99");
        check(save.progress(player.getUUID()).completed() == RoadChallenges.count(), "(never more than there are)");
        run(server, player, 2, "enhanced road reset " + name);
        check(save.existing(player.getUUID()) == null || save.existing(player.getUUID()).completed() == 0 && save.existing(player.getUUID()).salt() == 0L, "reset forgets it");
        run(server, player, 2, "enhanced road reset " + name + " newcourse");
        check(save.progress(player.getUUID()).salt() == 1L, "and with newcourse deals other courses");
        run(server, player, 2, "enhanced road status " + name);
        run(server, player, 2, "enhanced road cells");
        run(server, player, 2, "enhanced road enter " + name);
        helper.startSequence()
                .thenWaitUntil(RoadTestSupport.active(player))
                .thenExecute(() -> {
                    run(server, player, 2, "enhanced road layout " + name);
                    check(RoadDimension.in(player), "an operator sent him onto the Road without a Gene or a door");
                    run(server, player, 2, "enhanced road skip " + name);
                })
                .thenExecuteAfter(2, () -> {
                    check(save.progress(player.getUUID()).completed() == 1, "skip wins the trial he is in");
                    run(server, player, 0, "road leave");
                    check(!RoadDimension.in(player) && !RoadTrials.isInside(player), "/road leave: anyone can leave the Road");
                    run(server, player, 0, "road leave");
                    run(server, player, 0, "road status");
                    run(server, player, 2, "enhanced road enter " + name + " 1");
                    check(RoadTrials.isInside(player), "an operator can send him into a given trial");
                    run(server, player, 2, "enhanced road leave " + name);
                    check(!RoadTrials.isInside(player), "and take him out");
                })
                .thenSucceed();
    }

    private RoadFrameworkTests() {
    }
}
