package com.kolodkingal.enhanced.gametest;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.build.BuildEvents;
import com.kolodkingal.enhanced.build.BuildJob;
import com.kolodkingal.enhanced.build.BuildMagic;
import com.kolodkingal.enhanced.build.BuildRules.Left;
import com.kolodkingal.enhanced.build.BuildScheduler;
import com.kolodkingal.enhanced.build.BuildScheduler.Budget;
import com.kolodkingal.enhanced.build.Pocket;
import com.kolodkingal.enhanced.build.Spell;
import com.kolodkingal.enhanced.build.SpellRecipe;
import com.kolodkingal.enhanced.build.UndoFiles;
import com.kolodkingal.enhanced.build.UndoHistory;
import com.kolodkingal.enhanced.build.UndoStep;
import com.kolodkingal.enhanced.power.Power;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.kolodkingal.enhanced.gametest.BuildTestSupport.abs;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.boxSpell;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.builder;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.cast;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.count;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.finish;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.recipe;
import static com.kolodkingal.enhanced.gametest.BuildTestSupport.working;
import static com.kolodkingal.enhanced.gametest.TestKit.ARENA;
import static com.kolodkingal.enhanced.gametest.TestKit.check;

/**
 * The undo history on disk (D-B 12.4, 19.6, spec 18.4 B77-B81): the files round trip, a record is down before its cell is
 * written, the caps, the pocket and the operator's commands.
 *
 * <p>The history is written under a temporary folder a test opens ({@code UndoFiles.open}); the writes happen on the IO
 * pool, so a test waits for them with {@code UndoFiles.drain}. The files are server-wide state: every test has a batch
 * of its own ({@code build_undo_*}), and {@code TestKit.bossFixture} forgets the builders when it ends.
 */
@GameTestHolder(Enhanced.MODID)
@PrefixGameTestTemplate(false)
public final class BuildUndoTests {

    /** A folder for the history, opened for the test's length and deleted when it ends. */
    static Path folder(TestKit.Fixture f) {
        try {
            Path folder = Files.createTempDirectory("enhanced-undo-");
            UndoFiles.open(folder);
            f.onClose(() -> {
                UndoFiles.reset();
                UndoHistory.clear();
                try (Stream<Path> files = Files.walk(folder)) {
                    files.sorted(Comparator.reverseOrder()).forEach(file -> file.toFile().delete());
                } catch (IOException e) {
                    // (a temporary folder: the system cleans it)
                }
            });
            return folder;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** What a step holds, cell by cell in order, as text: for "equal after the disk". */
    static List<String> cellsOf(UndoStep step) {
        List<String> cells = new ArrayList<>();
        UndoStep.Walk walk = step.walk(false);
        while (walk.next()) {
            cells.add(walk.pos().toShortString() + " " + walk.oldState() + " " + walk.newState() + " " + walk.oldData() + " " + walk.newData() + " "
                    + walk.newHash() + " " + walk.status());
        }
        for (UndoStep.SideEffect effect : step.sideEffects()) {
            cells.add("side " + effect.pos().toShortString() + " " + effect.oldState() + " " + effect.newState() + " " + effect.status());
        }
        for (UndoStep.Carried thing : step.carried()) {
            cells.add("carried " + thing.id() + " " + thing.from() + " " + thing.fromFacing() + " " + thing.to() + " " + thing.toFacing());
        }
        cells.add("meta " + step.spell() + " " + step.dimension().location() + " " + step.time() + " " + step.state() + " " + step.cost() + " "
                + step.practice() + " " + step.mom() + " " + step.paid() + " " + step.changed() + " " + step.applied() + " " + step.undone() + " "
                + step.bounds());
        return cells;
    }

    private static CompoundTag chestData() {
        CompoundTag item = new CompoundTag();
        item.putByte("Slot", (byte) 3);
        item.putString("id", "minecraft:diamond");
        item.putInt("count", 5);
        ListTag items = new ListTag();
        items.add(item);
        CompoundTag data = new CompoundTag();
        data.put("Items", items);
        return data;
    }

    // B77

    /**
     * {@code UndoFiles} writes a history of two steps (one with a chest's data and a side effect, a frame a Move carried and
     * Billy's cost) and reads it back equal; a state name that does not exist reads as "keep" (its cell is never touched);
     * a garbage {@code history.dat} is set aside as {@code .corrupt} and the history starts empty.
     */
    @GameTest(template = ARENA, batch = "build_undo_disk", timeoutTicks = 200)
    public static void theHistoryRoundTripsThroughTheDisk(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        MinecraftServer server = f.level().getServer();
        Path folder = folder(f);
        UUID owner = UUID.randomUUID();
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        UndoHistory history = UndoHistory.of(owner);

        UndoStep first = new UndoStep(history.nextId(), Spell.FILL, Level.OVERWORLD, 1234L);
        for (int i = 0; i < 40; i++) {
            first.record(new BlockPos(100 + i % 5, 70 + i / 20, 200 + i % 7), air, null, stone, null, 0L);
        }
        first.setBilly(40, 40, false);
        first.setState(UndoStep.State.DONE);
        history.push(first, 0);

        UndoStep second = new UndoStep(history.nextId(), Spell.MOVE, Level.OVERWORLD, 5678L);
        BlockPos chest = new BlockPos(-40, 64, 300);
        CompoundTag data = chestData();
        second.record(chest, Blocks.CHEST.defaultBlockState(), data, air, null, 0L);
        second.record(chest.east(), air, null, Blocks.CHEST.defaultBlockState(), data.copy(), UndoStep.hash(data));
        second.record(new BlockPos(-39, 64, 301), Blocks.OAK_STAIRS.defaultBlockState(), null, Blocks.GLASS.defaultBlockState(), null, 0L);
        second.sideEffect(new BlockPos(-39, 65, 301), Blocks.TORCH.defaultBlockState(), air);
        UUID frame = UUID.randomUUID();
        second.carried(frame, new BlockPos(-40, 65, 299), Direction.SOUTH, new BlockPos(-39, 65, 299), Direction.EAST);
        second.setBilly(7, 7, true);
        second.setPaid(true);
        second.setState(UndoStep.State.DONE);
        history.push(second, 0);

        List<String> firstCells = cellsOf(first);
        List<String> secondCells = cellsOf(second);
        UndoFiles.drain();
        check(Files.exists(folder.resolve(owner.toString()).resolve("history.dat")), "the index is on disk");

        UndoHistory.clear();
        UndoFiles.loadNow(server, owner);
        UndoHistory loaded = UndoHistory.existing(owner);
        check(loaded != null && loaded.steps().size() == 2, "two steps come back: " + (loaded == null ? "no history" : loaded.steps().size()));
        UndoStep a = loaded.steps().get(0);
        UndoStep b = loaded.steps().get(1);
        check(!a.loaded() && !b.loaded() && a.changed() == 40 && b.changed() == 3, "they are only listed at first: " + a.changed() + ", " + b.changed());
        check(UndoFiles.load(loaded, a) && UndoFiles.load(loaded, b), "their cells are read back");
        check(cellsOf(a).equals(firstCells), "the first step is equal: " + cellsOf(a).get(0) + " / " + firstCells.get(0));
        check(cellsOf(b).equals(secondCells), "the second step is equal, its chest's data, side effect, frame and Billy's cost with it:\n"
                + cellsOf(b) + "\n" + secondCells);
        check(b.oldData(chest) != null && b.oldData(chest).equals(data), "the chest's data came back");

        // A state name that no longer exists reads as "keep": the cell is never touched.
        UndoFiles.edit(owner, a, tag -> {
            ListTag palette = tag.getList("palette", 10);
            for (int i = 0; i < palette.size(); i++) {
                if (palette.getCompound(i).getString("Name").equals("minecraft:stone")) {
                    palette.getCompound(i).put("Name", StringTag.valueOf("enhanced_test:vanished_block"));
                }
            }
        });
        UndoHistory.clear();
        UndoFiles.loadNow(server, owner);
        UndoHistory again = UndoHistory.existing(owner);
        UndoStep kept = again.steps().get(0);
        check(UndoFiles.load(again, kept), "the edited step is read");
        UndoStep.Walk walk = kept.walk(false);
        int dropped = 0;
        while (walk.next()) {
            dropped += walk.status() == UndoStep.DROPPED ? 1 : 0;
        }
        check(dropped == 40 && kept.applied() == 0, "every cell whose block is gone is 'keep': " + dropped + ", applied " + kept.applied());
        check(UndoFiles.readState(stoneTag("enhanced_test:vanished_block")) == null && UndoFiles.readState(stoneTag("minecraft:stone")) == stone,
                "a name that does not exist reads as nothing");

        // A garbage index is set aside; the history starts empty.
        UndoFiles.drain();
        Path index = folder.resolve(owner.toString()).resolve("history.dat");
        try {
            Files.write(index, new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9});
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        UndoHistory.clear();
        UndoFiles.loadNow(server, owner);
        UndoHistory empty = UndoHistory.existing(owner);
        check(empty != null && empty.steps().isEmpty() && empty.undoneSteps().isEmpty(), "the history starts empty");
        check(Files.exists(folder.resolve(owner.toString()).resolve("history.dat.corrupt")) && !Files.exists(index), "the index is set aside as .corrupt");
        helper.succeed();
    }

    private static CompoundTag stoneTag(String name) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        return tag;
    }

    // B78

    /**
     * With the writer held back (a test hook of {@code UndoFiles}) a running job writes no cell and yields; released, it
     * writes. Someone changes a cell after it was read ahead: the job leaves it as they made it ({@code kept_changed}).
     * The job is then dropped with no save hook and no stop, as a crash would: every cell it changed has its old state
     * in the step's file, and after a reload an undo of the interrupted step restores them all, the changed cell
     * excepted.
     */
    @GameTest(template = ARENA, batch = "build_undo_ahead", timeoutTicks = 400)
    public static void aCellIsNeverWrittenBeforeItsRecord(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        MinecraftServer server = level.getServer();
        Path folder = folder(f);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        UUID id = wanda.getUUID();
        BoundingBox box = new BoundingBox(5, 2, 5, 9, 6, 9);
        UndoFiles.holdWriter(true);
        cast(wanda, boxSpell(f, Spell.FILL, box));
        BuildJob job = working(wanda);
        check(job != null, "the fill is at work");
        Budget budget = Budget.of(30, 4096, 1);
        for (int i = 0; i < 6; i++) {
            BuildScheduler.step(List.of(id), budget);
        }
        check(job.written() == 0 && !job.finished() && count(f, box, Blocks.STONE) == 0,
                "with the writer held back no cell is written: " + job.written() + ", " + count(f, box, Blocks.STONE));

        // Someone changes a cell after it was read ahead.
        BlockPos glass = abs(f, 7, 4, 7);
        level.setBlock(glass, Blocks.GLASS.defaultBlockState(), 3);
        UndoFiles.holdWriter(false);
        UndoFiles.drain();
        for (int i = 0; i < 3; i++) {
            BuildScheduler.step(List.of(id), budget);
        }
        long written = job.written();
        check(written > 0 && written < 124 && !job.finished(), "released, it writes: " + written);
        check(job.left(Left.CHANGED) == 1 && level.getBlockState(glass).is(Blocks.GLASS), "the cell someone changed stays as they made it: "
                + job.left(Left.CHANGED));

        // A crash: the job is dropped with no stop and no save hook; its records are on disk.
        UndoStep running = job.step();
        List<BlockPos> changed = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(abs(f, 5, 2, 5), abs(f, 9, 6, 9))) {
            if (level.getBlockState(pos).is(Blocks.STONE)) {
                changed.add(pos.immutable());
            }
        }
        check(changed.size() == written, "the world holds what the job wrote: " + changed.size() + " of " + written);
        BuildScheduler.clear();
        UndoFiles.drain();
        UndoHistory.clear();
        UndoFiles.loadNow(server, id);
        UndoHistory history = UndoHistory.existing(id);
        check(history != null && history.steps().size() == 1, "the history comes back with one step");
        UndoStep step = history.steps().get(0);
        check(step.state() == UndoStep.State.INTERRUPTED && step.recovered() && step.loaded() && step.undoable(),
                "an interrupted step: " + step.state() + ", recovered " + step.recovered());
        for (BlockPos pos : changed) {
            check(step.has(pos) && step.oldState(pos).isAir() && step.newState(pos).is(Blocks.STONE), "its record has " + pos + ": the old state");
        }
        check(running.changed() == written, "(the step the job kept: " + running.changed() + ")");
        BuildScheduler.manual(id, null);
        BuildMagic.undo(wanda);
        finish(wanda);
        check(count(f, box, Blocks.STONE) == 0 && level.getBlockState(glass).is(Blocks.GLASS), "the undo restores every cell it changed, the glass stays: "
                + count(f, box, Blocks.STONE));
        check(count(f, box, Blocks.AIR) == 124, "all 124 are air again: " + count(f, box, Blocks.AIR));
        helper.succeed();
    }

    /**
     * The world-save hook ({@code BuildEvents.onLevelSave}) writes the index and forces the records of a spell at work:
     * the index lists the working step with its file, which holds the records of the cells the job wrote; once the
     * spell ends, the step is listed as done and its file is the step as it is.
     */
    @GameTest(template = ARENA, batch = "build_undo_save", timeoutTicks = 300)
    public static void aWorldSaveWritesTheIndexOfASpellAtWork(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        Path folder = folder(f);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        UUID id = wanda.getUUID();
        BoundingBox box = new BoundingBox(5, 2, 5, 9, 6, 9);
        cast(wanda, boxSpell(f, Spell.FILL, box));
        BuildJob job = working(wanda);
        Budget budget = Budget.of(30, 4096, 1);
        for (int i = 0; i < 4; i++) {
            UndoFiles.drain();
            BuildScheduler.step(List.of(id), budget);
        }
        check(job.written() > 0 && !job.finished(), "the spell is at work: " + job.written());
        BuildEvents.onLevelSave(new LevelEvent.Save(level));
        Path index = folder.resolve(id.toString()).resolve("history.dat");
        CompoundTag tag = readIndex(index);
        ListTag open = tag.getList("open", 10);
        check(open.size() == 1 && tag.getList("done", 10).isEmpty(), "the index lists the working step: " + tag);
        check(open.getCompound(0).getLong("bytes") > 8 && open.getCompound(0).getLong("file") == job.step().file(), "with its file and the bytes of its records: "
                + open.getCompound(0));
        check(Files.exists(UndoFiles.pathOf(id, job.step())), "its file is there");
        finish(wanda);
        UndoFiles.drain();
        BuildEvents.onLevelSave(new LevelEvent.Save(level));
        tag = readIndex(index);
        check(tag.getList("open", 10).isEmpty() && tag.getList("done", 10).size() == 1, "after the spell the step is listed as done: " + tag);
        check(tag.getList("done", 10).getCompound(0).getLong("changed") == 125, "with its 125 cells");
        UndoHistory.clear();
        UndoFiles.loadNow(level.getServer(), id);
        UndoStep step = UndoHistory.existing(id).steps().get(0);
        check(step.state() == UndoStep.State.DONE && !step.recovered() && UndoFiles.load(UndoHistory.existing(id), step) && step.applied() == 125,
                "read back it is the done step: " + step.state() + ", " + step.applied());
        helper.succeed();
    }

    private static CompoundTag readIndex(Path index) {
        try {
            return net.minecraft.nbt.NbtIo.readCompressed(index, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // B80

    /**
     * The pocket (a Copy's take) is written to {@code pocket.bin} and read back equal, cell by cell and its chest's data
     * with it, after the memory is forgotten; a change of dimension keeps it.
     */
    @GameTest(template = ARENA, batch = "build_undo_pocket", timeoutTicks = 200)
    public static void thePocketSurvivesARestartAndADimensionChange(GameTestHelper helper) {
        TestKit.Fixture f = TestKit.bossFixture(helper);
        ServerLevel level = f.level();
        MinecraftServer server = level.getServer();
        Path folder = folder(f);
        ServerPlayer wanda = builder(f, Power.SCARLET_WITCH, 0.5, 0.5, Blocks.STONE);
        UUID id = wanda.getUUID();
        BoundingBox box = new BoundingBox(3, 2, 3, 5, 4, 5);
        BlockPos chest = abs(f, 4, 3, 4);
        level.setBlock(abs(f, 3, 2, 3), Blocks.OAK_STAIRS.defaultBlockState(), 3);
        level.setBlock(abs(f, 5, 4, 5), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
        ((ChestBlockEntity) level.getBlockEntity(chest)).setItem(2, new ItemStack(Items.DIAMOND, 5));
        cast(wanda, copyTake(f, box));
        finish(wanda);
        Pocket held = Pocket.held(id);
        check(held != null && held.sizeX() == 3 && held.sizeY() == 3 && held.sizeZ() == 3, "the pocket holds the box");
        UndoFiles.drain();
        check(Files.exists(folder.resolve(id.toString()).resolve("pocket.bin")), "pocket.bin is on disk");

        // The server restarts: the memory is forgotten, the pocket is read back at the login.
        Pocket.clearHeld();
        UndoHistory.clear();
        UndoFiles.loadNow(server, id);
        Pocket back = Pocket.held(id);
        check(back != null && back != held, "the pocket is read back");
        check(back.id() == held.id() && back.sizeX() == held.sizeX() && back.sizeY() == held.sizeY() && back.sizeZ() == held.sizeZ()
                && back.origin().equals(held.origin()) && back.cells() == held.cells(), "with its number, size, origin and cell count");
        for (int y = 0; y < 3; y++) {
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 3; x++) {
                    check(java.util.Objects.equals(back.state(x, y, z), held.state(x, y, z)) && back.hash(x, y, z) == held.hash(x, y, z)
                            && java.util.Objects.equals(back.data(x, y, z), held.data(x, y, z)), "cell " + x + "," + y + "," + z + " is equal");
                }
            }
        }
        check(back.data(1, 1, 1) != null, "the chest's data came back");

        // A change of dimension keeps it.
        BuildEvents.onChangeDimension(new PlayerEvent.PlayerChangedDimensionEvent(wanda, Level.OVERWORLD, Level.NETHER));
        check(Pocket.held(id) == back, "the pocket travels with her");
        helper.succeed();
    }

    private static SpellRecipe copyTake(TestKit.Fixture f, BoundingBox box) {
        BoundingBox world = abs(f, box);
        BlockPos low = new BlockPos(world.minX(), world.minY(), world.minZ());
        return recipe(Spell.COPY).op(SpellRecipe.Op.TAKE).box(world).at(low, Direction.UP).build();
    }
}
