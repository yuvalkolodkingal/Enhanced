package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.build.UndoStep.Image;
import com.kolodkingal.enhanced.build.UndoStep.SectionImage;
import com.kolodkingal.enhanced.build.UndoStep.SideEffectImage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.Util;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.CRC32;

/**
 * The undo history on disk (D-B 12.4, spec 5.6, C13): off the main thread, crash-safe, and written ahead.
 *
 * <p><b>Where</b>: per player, in {@code <world>/data/enhanced/build/<uuid>/}: {@code history.dat} (the index: gzip NBT,
 * rewritten atomically), {@code step-<file>.bin} (a step) and {@code pocket.bin} (the pocket, D-B Appendix B). A file of
 * a step is a run of blocks, each {@code int length, int crc, bytes} (a gzip NBT): while the spell works it holds
 * <i>records</i> (the cells the spell is about to change, with what they hold now and what the spell will put there),
 * appended before those cells are written; once the step is sealed it is replaced, atomically, by the step as it is
 * (a block a section, then one for the side effects, the frames a Move carried and Billy's cost). A file that is cut
 * short (a crash during an append) is read up to its last whole block; one that cannot be read at all, or an index
 * that cannot, is set aside as {@code .corrupt} and the history starts without it, the world untouched on its account.
 * A block a world no longer has (a mod removed) reads as "keep": its cell is never touched.
 *
 * <p><b>Written ahead (C13, R52)</b>: a {@link Journal} is what a working spell writes its records into. The job reads
 * the old states of the cells ahead of it ({@link WriteAhead}), hands them over in batches (compressed on
 * {@code Util.backgroundExecutor()}, appended on {@code Util.ioPool()}, in order), and writes a cell only once the batch
 * that holds it has been appended; while a batch is on its way the job yields its slice (the spell slows, never the
 * server). A world save ({@link #onSave}) writes the index (and forces the files), so the saved world never holds a
 * change of a spell its saved record lacks: every cell it holds was recorded before it was written. After a crash the
 * history is read as it was, the cut spell's step {@linkplain UndoStep.State#INTERRUPTED interrupted}, and Ctrl + Z
 * undoes what the saved world holds of it.
 *
 * <p><b>Read back lazily</b>: the index at login (a step is only listed, with its counts); a step's cells when it is
 * undone or redone ({@link #load}). With {@code undoSurvivesRestart} off, or on a server where no root is set (a
 * GameTest server, until a test sets one), nothing is written and nothing waits: the history lives in memory.
 */
public final class UndoFiles {
    private static final int MAGIC = 0x424D5531;
    private static final int FORMAT = 1;
    private static final String INDEX = "history.dat";
    private static final String POCKET = "pocket.bin";
    private static final int MAX_BLOCK = 1 << 28;
    /** The cells the first batch of a spell holds (a spell starts as soon as the first record is down). */
    static final int FIRST_BATCH = 256;
    /** The most cells a batch holds. */
    static final int BATCH_CELLS = 4096;
    /** How long a world save waits for the index (seconds). */
    private static final int SAVE_WAIT = 60;

    private static @Nullable Path root;
    /** The writer's gate: appends wait while it is closed (a test hook, {@link #holdWriter}). */
    private static volatile CompletableFuture<Void> gate = CompletableFuture.completedFuture(null);
    private static final Set<CompletableFuture<?>> IN_FLIGHT = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Chain> CHAINS = new HashMap<>();
    private static long lastFile;
    /** The players whose history was asked for from the disk this run (it is read once a run). */
    private static final Set<UUID> LOADED = new HashSet<>();

    private UndoFiles() {
    }

    // ------------------------------------------------------------------------------------------------------------
    // Switching it on

    /** Makes {@code folder} the root of every history on disk; the history is written from now on. */
    public static void open(Path folder) {
        reset();
        root = folder;
    }

    /** Whether the history is written to disk. */
    public static boolean active() {
        return root != null;
    }

    /** The folder the histories are in, or null. */
    public static @Nullable Path root() {
        return root;
    }

    /**
     * Writes nothing any more and forgets what is in flight: the server stopped, or a test ended. What was queued is
     * finished first; a held writer is let go.
     */
    public static void reset() {
        holdWriter(false);
        drain();
        root = null;
        CHAINS.clear();
        LOADED.clear();
    }

    /** A test hook: while {@code held}, no record is appended (a job waits for its record, and yields). */
    public static void holdWriter(boolean held) {
        CompletableFuture<Void> was = gate;
        if (held) {
            if (was.isDone()) {
                gate = new CompletableFuture<>();
            }
        } else {
            was.complete(null);
        }
    }

    /** Whether the writer is held back. */
    public static boolean held() {
        return !gate.isDone();
    }

    /** Waits until everything queued has been written (nothing is waited for while the writer is held). */
    public static void drain() {
        for (int guard = 0; guard < 1000 && gate.isDone(); guard++) {
            List<CompletableFuture<?>> flying = new ArrayList<>(IN_FLIGHT);
            if (flying.isEmpty()) {
                return;
            }
            try {
                CompletableFuture.allOf(flying.toArray(new CompletableFuture<?>[0])).get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                return;
            }
        }
    }

    private static Path dir(UUID owner) {
        return root.resolve(owner.toString());
    }

    private static Path stepPath(UUID owner, long file) {
        return dir(owner).resolve(String.format("step-%016x.bin", file));
    }

    /** A number for the file of a new step: never the same twice, and in the order they were made. */
    static synchronized long newFile() {
        lastFile = Math.max(lastFile + 1, System.currentTimeMillis() << 8);
        return lastFile;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Queues

    /** The writes of one owner's index and pocket, in order. */
    private static final class Chain {
        CompletableFuture<Void> tail = CompletableFuture.completedFuture(null);
    }

    private static Chain chainOf(UUID owner) {
        return CHAINS.computeIfAbsent(owner, id -> new Chain());
    }

    /** Runs {@code io} on the IO pool after what {@code chain} holds; the next waits for it. */
    private static CompletableFuture<Void> run(Chain chain, Runnable io) {
        CompletableFuture<Void> next = chain.tail.handle((v, e) -> (Void) null).thenRunAsync(() -> {
            try {
                io.run();
            } catch (RuntimeException e) {
                Enhanced.LOGGER.error("Build Magic: a write of the undo history failed", e);
            }
        }, Util.ioPool());
        chain.tail = next;
        track(next);
        return next;
    }

    private static void track(CompletableFuture<?> future) {
        IN_FLIGHT.add(future);
        future.whenComplete((r, e) -> {
            IN_FLIGHT.remove(future);
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // The files of steps and the journals of working spells

    /** One step's file: its appends and its replacement happen in order, one at a time. */
    static final class StepFile {
        final Path path;
        CompletableFuture<Void> tail = CompletableFuture.completedFuture(null);
        volatile boolean failed;
        volatile long durable;
        private @Nullable FileChannel channel;

        StepFile(Path path) {
            this.path = path;
        }

        /** Appends the framed block {@code framed} (when it is ready and the writer is not held); then {@code done}. */
        CompletableFuture<Void> append(CompletableFuture<byte[]> framed, Runnable done) {
            CompletableFuture<Void> next = framed.handle((b, e) -> b)
                    .thenCombine(tail.handle((v, e) -> (Void) null), (b, v) -> b)
                    .thenCombine(gate, (b, g) -> b)
                    .thenApplyAsync(bytes -> {
                        try {
                            if (bytes == null) {
                                failed = true;
                            } else if (!failed) {
                                write(bytes);
                            }
                        } catch (IOException | RuntimeException e) {
                            fail(e);
                        } finally {
                            done.run();
                        }
                        return (Void) null;
                    }, Util.ioPool());
            tail = next;
            track(next);
            return next;
        }

        /** Appends (io thread). */
        private void write(byte[] bytes) throws IOException {
            if (channel == null) {
                Files.createDirectories(path.getParent());
                channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
                if (channel.size() == 0) {
                    ByteBuffer head = ByteBuffer.allocate(8);
                    head.putInt(MAGIC).putInt(FORMAT).flip();
                    while (head.hasRemaining()) {
                        channel.write(head);
                    }
                }
            }
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            durable = channel.size();
        }

        /** Replaces the file by {@code blocks} (io thread): a temporary file, forced, then moved over it. */
        private void replace(List<byte[]> blocks) throws IOException {
            closeChannel();
            Files.createDirectories(path.getParent());
            Path temp = path.resolveSibling(path.getFileName() + ".tmp");
            long size = 8;
            try (FileChannel out = FileChannel.open(temp, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer head = ByteBuffer.allocate(8);
                head.putInt(MAGIC).putInt(FORMAT).flip();
                while (head.hasRemaining()) {
                    out.write(head);
                }
                for (byte[] block : blocks) {
                    ByteBuffer buffer = ByteBuffer.wrap(block);
                    while (buffer.hasRemaining()) {
                        out.write(buffer);
                    }
                    size += block.length;
                }
                out.force(true);
            }
            move(temp, path);
            durable = size;
            failed = false;
        }

        void force() {
            FileChannel open = channel;
            if (open != null && open.isOpen()) {
                try {
                    open.force(false);
                } catch (IOException e) {
                    // (the next save tries again)
                }
            }
        }

        private void closeChannel() {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                    // (nothing to do)
                }
                channel = null;
            }
        }

        private void fail(Exception e) {
            if (!failed) {
                failed = true;
                Enhanced.LOGGER.error("Build Magic: a spell's undo record could not be written; it goes on without one ({})", path, e);
            }
        }

        /** Runs {@code io} after the appends before it, on the IO pool. */
        CompletableFuture<Void> then(Runnable io) {
            CompletableFuture<Void> next = tail.handle((v, e) -> (Void) null).thenRunAsync(() -> {
                try {
                    io.run();
                } catch (RuntimeException e) {
                    Enhanced.LOGGER.error("Build Magic: a write of the undo history failed", e);
                }
            }, Util.ioPool());
            tail = next;
            track(next);
            return next;
        }
    }

    /** The file of {@code step} (made, with its name, when it has none). */
    private static StepFile fileOf(UUID owner, UndoStep step) {
        StepFile file = step.io;
        if (file == null) {
            if (step.file() == 0L) {
                step.setFile(newFile());
            }
            file = new StepFile(stepPath(owner, step.file()));
            step.io = file;
        }
        return file;
    }

    /**
     * A batch of records on its way to the disk: the cells a spell is about to change, in the order they were handed
     * over. Read by the writer's threads once flushed, and never changed after.
     */
    static final class Batch {
        final LongArrayList positions = new LongArrayList();
        final IntArrayList olds = new IntArrayList();
        final IntArrayList news = new IntArrayList();
        final Int2ObjectOpenHashMap<CompoundTag> oldData = new Int2ObjectOpenHashMap<>();
        final Int2ObjectOpenHashMap<CompoundTag> newData = new Int2ObjectOpenHashMap<>();
        volatile boolean done;

        int size() {
            return positions.size();
        }
    }

    /** What a journal holds of a cell: the batch and the place in it. */
    static final class Entry {
        final Batch batch;
        final int index;

        Entry(Batch batch, int index) {
            this.batch = batch;
            this.index = index;
        }

        /** What the cell held when it was read ahead. */
        BlockState old() {
            return Block.stateById(batch.olds.getInt(index));
        }

        /** The block-entity data it held then (null for none). */
        @Nullable CompoundTag oldData() {
            return batch.oldData.get(index);
        }
    }

    /**
     * What a working spell writes its records into (C13): cells are {@link #add added} with what they hold and what the
     * spell will put there; {@link #flush} hands the batch to the writer; {@link #entry} says whether a cell's record
     * is down ({@link #durable}) and what the cell held. A spell writes a cell only when its record is down.
     */
    public static final class Journal {
        private final UndoHistory history;
        private final UndoStep step;
        private final StepFile file;
        private @Nullable Batch open;
        private final Long2ObjectOpenHashMap<Entry> entries = new Long2ObjectOpenHashMap<>();
        private int goal = FIRST_BATCH;
        private boolean closed;

        Journal(UndoHistory history, UndoStep step, StepFile file) {
            this.history = history;
            this.step = step;
            this.file = file;
        }

        /** Whether its records cannot be written (the disk failed): the spell goes on without. */
        public boolean failed() {
            return file.failed || closed;
        }

        /**
         * Records that the spell will change {@code pos} from {@code old} (holding {@code oldData}) to {@code now} (with
         * {@code newData}). A cell recorded before is recorded again (its first record keeps what it held).
         */
        public void add(long pos, BlockState old, @Nullable CompoundTag oldData, BlockState now, @Nullable CompoundTag newData) {
            if (failed()) {
                return;
            }
            Batch batch = open;
            if (batch == null) {
                batch = new Batch();
                open = batch;
            }
            int at = batch.size();
            batch.positions.add(pos);
            batch.olds.add(Block.getId(old));
            batch.news.add(Block.getId(now));
            if (oldData != null) {
                batch.oldData.put(at, oldData);
            }
            if (newData != null) {
                batch.newData.put(at, newData);
            }
            if (!entries.containsKey(pos)) {
                entries.put(pos, new Entry(batch, at));
            }
            if (at + 1 >= goal) {
                flush();
                goal = Math.min(BATCH_CELLS, goal * 2);
            }
        }

        /** Hands what has been added since to the writer. */
        public void flush() {
            Batch batch = open;
            if (batch == null || batch.size() == 0) {
                return;
            }
            open = null;
            if (failed()) {
                batch.done = true;
                return;
            }
            history.opened(step);
            index(history);
            CompletableFuture<byte[]> framed = CompletableFuture.supplyAsync(() -> frame(encodeRecords(batch)), Util.backgroundExecutor());
            file.append(framed, () -> batch.done = true);
        }

        /** The record of {@code pos} that has not been written over yet, or null. */
        @Nullable Entry entry(long pos) {
            return entries.get(pos);
        }

        /** Whether the record of {@code entry} is on disk (or the journal can write none). */
        boolean durable(Entry entry) {
            return entry.batch.done || failed();
        }

        /** The cell was written: its record is no longer asked for. */
        void release(long pos) {
            entries.remove(pos);
        }

        /** How many bytes of records are on disk. */
        long bytes() {
            return file.durable;
        }

        /** The spell's job is over: nothing more is added, the records not yet handed over are dropped. */
        void close() {
            open = null;
            entries.clear();
        }
    }

    /** The journal of {@code step} in {@code history} (made when it has none), or null when nothing is written. */
    static @Nullable Journal journal(UndoHistory history, UndoStep step) {
        if (root == null) {
            return null;
        }
        Journal journal = step.journal;
        if (journal == null) {
            journal = new Journal(history, step, fileOf(history.owner(), step));
            step.journal = journal;
        }
        return journal;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Sealing, replacing and forgetting

    /**
     * A step was sealed into {@code history} (its spell is over, or an undo or a redo of it): its file is replaced by
     * the step as it is, off the main thread, and the index follows.
     */
    static void sealed(UndoHistory history, UndoStep step) {
        if (root == null) {
            return;
        }
        rewrite(history, step);
        index(history);
    }

    /** Writes the step as it is over its file (done after a seal, an undo, a redo). A step not in memory has nothing to write. */
    static void rewrite(UndoHistory history, UndoStep step) {
        if (root == null || !step.loaded()) {
            return;
        }
        UUID owner = history.owner();
        StepFile file = fileOf(owner, step);
        Journal journal = step.journal;
        if (journal != null) {
            journal.close();
            step.journal = null;
        }
        Image image = step.image();
        int mods = step.unstore();
        CompletableFuture<List<byte[]>> blocks = CompletableFuture.supplyAsync(() -> encodeImage(image), Util.backgroundExecutor());
        CompletableFuture<Void> next = blocks.handle((b, e) -> b).thenCombine(file.tail.handle((v, e) -> (Void) null), (b, v) -> b)
                .thenApplyAsync(list -> {
                    try {
                        if (list == null) {
                            throw new IOException("the step could not be encoded");
                        }
                        file.replace(list);
                        step.setBytes(file.durable);
                        step.markStored(mods);
                    } catch (IOException | RuntimeException e) {
                        Enhanced.LOGGER.error("Build Magic: an undo step could not be written ({})", file.path, e);
                    }
                    return (Void) null;
                }, Util.ioPool());
        file.tail = next;
        track(next);
    }

    /** The spell ended without a step to keep: its journal and its file go. */
    static void discard(UndoHistory history, UndoStep step) {
        Journal journal = step.journal;
        if (journal == null) {
            return;
        }
        journal.close();
        step.journal = null;
        history.closed(step);
        if (root != null) {
            StepFile file = step.io;
            step.io = null;
            if (file != null) {
                file.then(() -> {
                    file.closeChannel();
                    delete(file.path);
                });
            }
            index(history);
        }
    }

    /** The files of the steps {@code history} has let go of (forgotten, a redo list emptied) are deleted. */
    static void deleteGone(UndoHistory history) {
        List<UndoStep> gone = history.takeGone();
        if (root == null) {
            return;
        }
        for (UndoStep step : gone) {
            if (step.file() == 0L) {
                continue;
            }
            StepFile file = fileOf(history.owner(), step);
            step.io = null;
            file.then(() -> {
                file.closeChannel();
                delete(file.path);
            });
        }
    }

    /** {@code /enhanced build forget}: the history and the pocket are gone, and so is their folder. */
    static void forgetFolder(UUID owner) {
        if (root == null) {
            return;
        }
        LOADED.add(owner);
        Path folder = dir(owner);
        run(chainOf(owner), () -> deleteTree(folder));
    }

    // ------------------------------------------------------------------------------------------------------------
    // The index

    /** Writes the index of {@code history} (when it changed since the last), off the main thread. */
    static void index(UndoHistory history) {
        if (root == null) {
            return;
        }
        deleteGone(history);
        if (history.indexed == history.version()) {
            return;
        }
        history.indexed = history.version();
        CompoundTag tag = indexTag(history);
        UUID owner = history.owner();
        Path folder = dir(owner);
        run(chainOf(owner), () -> {
            try {
                writeCompressed(tag, folder.resolve(INDEX));
            } catch (IOException e) {
                Enhanced.LOGGER.error("Build Magic: the undo history of {} could not be written", owner, e);
            }
        });
    }

    private static CompoundTag indexTag(UndoHistory history) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("version", FORMAT);
        tag.put("done", stepTags(history.steps()));
        tag.put("redo", stepTags(history.undoneSteps()));
        tag.put("open", stepTags(history.open()));
        return tag;
    }

    private static ListTag stepTags(List<UndoStep> steps) {
        ListTag list = new ListTag();
        for (UndoStep step : steps) {
            if (step.file() == 0L) {
                continue; // (nothing of it was ever written)
            }
            CompoundTag tag = new CompoundTag();
            tag.putLong("file", step.file());
            tag.putString("spell", step.spell().name());
            tag.putString("dimension", step.dimension().location().toString());
            tag.putLong("time", step.time());
            tag.putLong("changed", step.changed());
            tag.putLong("applied", step.applied());
            tag.putLong("undone", step.undone());
            tag.putInt("state", step.state().ordinal());
            tag.putInt("cost", step.cost());
            tag.putLong("practice", step.practice());
            tag.putBoolean("mom", step.mom());
            tag.putBoolean("paid", step.paid());
            tag.putBoolean("tell", step.tell());
            Journal journal = step.journal;
            tag.putLong("bytes", journal != null ? journal.bytes() : step.diskBytes());
            BoundingBox box = step.bounds();
            if (box != null) {
                tag.putIntArray("box", new int[]{box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()});
            }
            list.add(tag);
        }
        return list;
    }

    // ------------------------------------------------------------------------------------------------------------
    // World saves, logouts, the stop

    /**
     * {@code LevelEvent.Save}: the files of the spells at work are forced to disk and the indexes of the histories that
     * changed are written, before the save completes (the records themselves were written ahead, C13). Waits for the
     * writes of the indexes, not for records a held writer has not appended (no cell of theirs was written).
     */
    public static void onSave() {
        if (root == null) {
            return;
        }
        List<CompletableFuture<?>> waits = new ArrayList<>();
        for (UndoHistory history : UndoHistory.all()) {
            for (UndoStep step : history.open()) {
                Journal journal = step.journal;
                if (journal != null) {
                    journal.file.force();
                }
            }
            if (!history.open().isEmpty()) {
                history.touched(); // (the bytes of their records moved on)
            }
            index(history);
            Chain chain = CHAINS.get(history.owner());
            if (chain != null) {
                waits.add(chain.tail);
            }
        }
        await(waits);
    }

    private static void await(List<CompletableFuture<?>> waits) {
        for (CompletableFuture<?> future : waits) {
            try {
                future.get(SAVE_WAIT, TimeUnit.SECONDS);
            } catch (Exception e) {
                Enhanced.LOGGER.error("Build Magic: the undo history did not finish writing", e);
            }
        }
    }

    /** A player leaves: his history's index is written. */
    public static void onLogout(UUID owner) {
        UndoHistory history = UndoHistory.existing(owner);
        if (root != null && history != null) {
            index(history);
        }
    }

    /** The server stops: every index is written and everything queued finishes. */
    public static void shutdown() {
        if (root == null) {
            return;
        }
        for (UndoHistory history : UndoHistory.all()) {
            index(history);
        }
        holdWriter(false);
        drain();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Reading

    /** A history and a pocket as the disk holds them. */
    record Loaded(UndoHistory history, @Nullable Pocket pocket) {
    }

    /**
     * Reads the history of {@code owner} in the background and installs it when it is read (main thread): at his login.
     * Does nothing when nothing is written, or he was read already.
     */
    public static void loadLater(MinecraftServer server, UUID owner) {
        if (root == null || !LOADED.add(owner)) {
            return;
        }
        CompletableFuture<Loaded> reading = CompletableFuture.supplyAsync(() -> read(owner), Util.ioPool());
        track(reading);
        reading.thenAcceptAsync(loaded -> {
            if (loaded != null && root != null) {
                install(server, owner, loaded);
            }
        }, server).exceptionally(e -> {
            Enhanced.LOGGER.error("Build Magic: the undo history of {} could not be read", owner, e);
            return null;
        });
    }

    /** A player logs in: his history is read from the disk, once a run. */
    public static void onLogin(MinecraftServer server, UUID owner) {
        loadLater(server, owner);
    }

    /** Reads the history of {@code owner} now, on this thread, and installs it (a test, a command for a player who is away). */
    public static void loadNow(MinecraftServer server, UUID owner) {
        if (root == null) {
            return;
        }
        drain();
        install(server, owner, read(owner));
    }

    private static void install(MinecraftServer server, UUID owner, @Nullable Loaded loaded) {
        if (loaded == null) {
            return;
        }
        UndoHistory history = loaded.history;
        for (UndoStep step : history.allSteps()) {
            if (server.getLevel(step.dimension()) == null) {
                history.drop(step); // (its dimension no longer exists)
            }
        }
        UndoHistory now = UndoHistory.install(history);
        if (loaded.pocket != null && Pocket.held(owner) == null) {
            Pocket.holdLoaded(owner, loaded.pocket);
        }
        index(now);
        ServerPlayer player = server.getPlayerList().getPlayer(owner);
        if (player != null) {
            BuildMagic.sync(server, owner);
        }
    }

    /** Reads a history and a pocket from the folder of {@code owner}: files that cannot be read are set aside. */
    private static @Nullable Loaded read(UUID owner) {
        Path folder = dir(owner);
        if (!Files.isDirectory(folder)) {
            return null;
        }
        UndoHistory history = UndoHistory.detached(owner);
        Set<String> kept = new HashSet<>();
        Path index = folder.resolve(INDEX);
        if (Files.exists(index)) {
            CompoundTag tag = null;
            try {
                tag = NbtIo.readCompressed(index, NbtAccounter.unlimitedHeap());
                if (tag.getInt("version") != FORMAT) {
                    throw new IOException("version " + tag.getInt("version"));
                }
            } catch (IOException | RuntimeException e) {
                Enhanced.LOGGER.error("Build Magic: the undo history index {} cannot be read; it is set aside", index, e);
                setAside(index);
                tag = null;
                try (Stream<Path> files = Files.list(folder)) {
                    for (Path file : files.toList()) {
                        String name = file.getFileName().toString();
                        if (name.startsWith("step-") && name.endsWith(".bin")) {
                            setAside(file);
                        }
                    }
                } catch (IOException ignored) {
                    // (nothing more to set aside)
                }
            }
            if (tag != null) {
                readSteps(folder, history, tag.getList("done", Tag.TAG_COMPOUND), false, false, kept);
                readSteps(folder, history, tag.getList("redo", Tag.TAG_COMPOUND), true, false, kept);
                readSteps(folder, history, tag.getList("open", Tag.TAG_COMPOUND), false, true, kept);
            }
        }
        // A step file the index does not list is the leftover of a step that was forgotten: it goes.
        try (Stream<Path> files = Files.list(folder)) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (name.startsWith("step-") && (name.endsWith(".bin") && !kept.contains(name) || name.endsWith(".tmp"))) {
                    delete(file);
                }
            }
        } catch (IOException ignored) {
            // (they stay for the next time)
        }
        Pocket pocket = null;
        Path pocketFile = folder.resolve(POCKET);
        if (Files.exists(pocketFile)) {
            try {
                pocket = Pocket.fromTag(NbtIo.readCompressed(pocketFile, NbtAccounter.unlimitedHeap()));
                if (pocket == null) {
                    throw new IOException("not a whole pocket");
                }
            } catch (IOException | RuntimeException e) {
                Enhanced.LOGGER.error("Build Magic: the pocket file {} cannot be read; it is set aside", pocketFile, e);
                setAside(pocketFile);
            }
        }
        return new Loaded(history, pocket);
    }

    private static void readSteps(Path folder, UndoHistory history, ListTag list, boolean redo, boolean working, Set<String> kept) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            long file = tag.getLong("file");
            Path path = folder.resolve(String.format("step-%016x.bin", file));
            Spell spell;
            ResourceKey<Level> dimension;
            try {
                spell = Spell.valueOf(tag.getString("spell"));
                dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("dimension")));
            } catch (IllegalArgumentException | net.minecraft.ResourceLocationException e) {
                continue;
            }
            if (!Files.exists(path)) {
                continue;
            }
            kept.add(path.getFileName().toString());
            long time = tag.getLong("time");
            UndoStep step;
            if (working) {
                // The spell was at work when the history was last written (a crash, a stop): what its records hold is
                // a step that can be undone.
                UndoStep body = readStep(path, spell, dimension, time);
                if (body == null || body.changed() == 0) {
                    kept.remove(path.getFileName().toString());
                    delete(path);
                    continue;
                }
                body.setState(UndoStep.State.INTERRUPTED);
                body.setTell(true);
                step = body;
            } else {
                int[] box = tag.getIntArray("box");
                step = UndoStep.listed(spell, dimension, time, tag.getLong("changed"), tag.getLong("applied"), tag.getLong("undone"),
                        box.length == 6 ? new BoundingBox(box[0], box[1], box[2], box[3], box[4], box[5]) : null);
                int state = tag.getInt("state");
                UndoStep.State[] states = UndoStep.State.values();
                step.setState(states[Math.floorMod(state, states.length)]);
                if (step.state() == UndoStep.State.WORKING) {
                    step.setState(UndoStep.State.INTERRUPTED);
                }
                step.setBilly(tag.getInt("cost"), tag.getLong("practice"), tag.getBoolean("mom"));
                step.setPaid(tag.getBoolean("paid"));
                step.setTell(tag.getBoolean("tell"));
            }
            step.setFile(file);
            try {
                step.setBytes(Files.size(path));
            } catch (IOException e) {
                step.setBytes(tag.getLong("bytes"));
            }
            history.restore(step, redo);
        }
    }

    // ---- A step's cells ----

    /**
     * Reads the cells of {@code step} back from its file (it was only listed), on this thread: for a test and for the
     * undo that needs them. False when its file cannot be read: the step is dropped from its history.
     */
    public static boolean load(UndoHistory history, UndoStep step) {
        if (step.loaded()) {
            return true;
        }
        if (root == null) {
            history.drop(step);
            return false;
        }
        drain();
        return adoptBody(history, step, readStep(stepPath(history.owner(), step.file()), step.spell(), step.dimension(), step.time()));
    }

    /**
     * Reads the cells of {@code step} in the background and calls {@code then} on the main thread once they are in
     * memory ({@code then} gets false when its file cannot be read).
     */
    public static void loadStepLater(MinecraftServer server, UndoHistory history, UndoStep step, Consumer<Boolean> then) {
        if (step.loaded()) {
            then.accept(true);
            return;
        }
        if (root == null) {
            history.drop(step);
            then.accept(false);
            return;
        }
        StepFile file = fileOf(history.owner(), step);
        CompletableFuture<UndoStep> reading = file.tail.handle((v, e) -> (Void) null)
                .thenApplyAsync(v -> readStep(file.path, step.spell(), step.dimension(), step.time()), Util.ioPool());
        track(reading);
        reading.handleAsync((body, e) -> {
            then.accept(e == null && adoptBody(history, step, body));
            return null;
        }, server);
    }

    private static boolean adoptBody(UndoHistory history, UndoStep step, @Nullable UndoStep body) {
        if (step.loaded()) {
            return true;
        }
        if (body == null) {
            history.drop(step);
            return false;
        }
        step.adopt(body);
        return true;
    }

    /** Reads a step's file as it is; null when it cannot be read (it is then set aside). */
    static @Nullable UndoStep readStep(Path path, Spell spell, ResourceKey<Level> dimension, long time) {
        UndoStep step = UndoStep.read(spell, dimension, time);
        boolean[] ended = {false};
        boolean[] images = {false};
        Result result = forEachBlock(path, tag -> {
            String kind = tag.getString("k");
            switch (kind) {
                case "rec" -> decodeRecords(tag, step);
                case "sec" -> {
                    images[0] = true;
                    decodeSection(tag, step);
                }
                case "end" -> {
                    ended[0] = true;
                    decodeEnd(tag, step);
                }
                default -> {
                }
            }
        });
        if (!result.readable() || images[0] && !ended[0]) {
            Enhanced.LOGGER.error("Build Magic: the undo step file {} cannot be read; it is set aside", path);
            setAside(path);
            return null;
        }
        if (!ended[0]) {
            step.setState(UndoStep.State.INTERRUPTED);
        }
        return step;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The pocket

    /** The pocket of {@code owner} is now {@code pocket}: its file is written, off the main thread. */
    static void writePocket(UUID owner, Pocket pocket) {
        if (root == null) {
            return;
        }
        Path file = dir(owner).resolve(POCKET);
        run(chainOf(owner), () -> {
            try {
                writeCompressed(pocket.toTag(), file);
            } catch (IOException e) {
                Enhanced.LOGGER.error("Build Magic: the pocket of {} could not be written", owner, e);
            }
        });
    }

    // ------------------------------------------------------------------------------------------------------------
    // Block states

    /** A block state as the files keep it: its name and properties. */
    public static CompoundTag stateTag(BlockState state) {
        return NbtUtils.writeBlockState(state);
    }

    /** The block state a file keeps, or null when its block no longer exists ("keep"). */
    public static @Nullable BlockState readState(CompoundTag tag) {
        ResourceLocation name = ResourceLocation.tryParse(tag.getString("Name"));
        if (name == null || !BuiltInRegistries.BLOCK.containsKey(name)) {
            return null;
        }
        return NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag);
    }

    /** A palette of the block states one block of a file uses. */
    private static final class Palette {
        private final Object2IntOpenHashMap<BlockState> ids = new Object2IntOpenHashMap<>();
        private final ListTag tags = new ListTag();

        Palette() {
            ids.defaultReturnValue(-1);
        }

        int of(BlockState state) {
            int id = ids.getInt(state);
            if (id < 0) {
                id = tags.size();
                ids.put(state, id);
                tags.add(stateTag(state));
            }
            return id;
        }

        ListTag tags() {
            return tags;
        }
    }

    /** The block states of a palette as read; a state that no longer exists is air with {@code known} false. */
    private static final class Read {
        final BlockState[] states;
        final boolean[] known;

        Read(ListTag palette) {
            states = new BlockState[palette.size()];
            known = new boolean[palette.size()];
            for (int i = 0; i < states.length; i++) {
                BlockState state = readState(palette.getCompound(i));
                known[i] = state != null;
                states[i] = state != null ? state : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            }
        }

        BlockState state(int index) {
            return index >= 0 && index < states.length ? states[index] : states.length > 0 ? states[0] : net.minecraft.world.level.block.Blocks.AIR
                    .defaultBlockState();
        }

        boolean known(int index) {
            return index >= 0 && index < known.length && known[index];
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Encoding

    /** The records of a batch as one block (a gzip NBT): sections in the order they were first touched. */
    private static byte[] encodeRecords(Batch batch) {
        Palette palette = new Palette();
        Long2ObjectLinkedOpenHashMap<SectionRecords> sections = new Long2ObjectLinkedOpenHashMap<>();
        for (int i = 0; i < batch.size(); i++) {
            long pos = batch.positions.getLong(i);
            int x = BlockPos.getX(pos);
            int y = BlockPos.getY(pos);
            int z = BlockPos.getZ(pos);
            long key = SectionPos.asLong(x >> 4, y >> 4, z >> 4);
            SectionRecords section = sections.get(key);
            if (section == null) {
                section = new SectionRecords();
                sections.put(key, section);
            }
            int at = section.cells.size();
            section.cells.add(((y & 15) << 8) | ((z & 15) << 4) | (x & 15));
            section.olds.add(palette.of(Block.stateById(batch.olds.getInt(i))));
            section.news.add(palette.of(Block.stateById(batch.news.getInt(i))));
            CompoundTag oldData = batch.oldData.get(i);
            if (oldData != null) {
                section.oldData.add(dataTag(at, oldData));
            }
            CompoundTag newData = batch.newData.get(i);
            if (newData != null) {
                section.newData.add(dataTag(at, newData));
            }
        }
        CompoundTag root = new CompoundTag();
        root.putString("k", "rec");
        root.put("palette", palette.tags());
        ListTag list = new ListTag();
        for (var entry : sections.long2ObjectEntrySet()) {
            SectionRecords section = entry.getValue();
            CompoundTag tag = new CompoundTag();
            tag.putLong("key", entry.getLongKey());
            tag.put("cells", new IntArrayTag(section.cells.toIntArray()));
            tag.put("olds", new IntArrayTag(section.olds.toIntArray()));
            tag.put("news", new IntArrayTag(section.news.toIntArray()));
            tag.put("od", section.oldData);
            tag.put("nd", section.newData);
            list.add(tag);
        }
        root.put("s", list);
        return compress(root);
    }

    private static final class SectionRecords {
        final IntArrayList cells = new IntArrayList();
        final IntArrayList olds = new IntArrayList();
        final IntArrayList news = new IntArrayList();
        final ListTag oldData = new ListTag();
        final ListTag newData = new ListTag();
    }

    private static CompoundTag dataTag(int index, CompoundTag data) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("i", index);
        tag.put("d", data);
        return tag;
    }

    private static void decodeRecords(CompoundTag tag, UndoStep step) {
        Read palette = new Read(tag.getList("palette", Tag.TAG_COMPOUND));
        ListTag sections = tag.getList("s", Tag.TAG_COMPOUND);
        for (int s = 0; s < sections.size(); s++) {
            CompoundTag section = sections.getCompound(s);
            long key = section.getLong("key");
            int[] cells = section.getIntArray("cells");
            int[] olds = section.getIntArray("olds");
            int[] news = section.getIntArray("news");
            Map<Integer, CompoundTag> oldData = dataMap(section.getList("od", Tag.TAG_COMPOUND));
            Map<Integer, CompoundTag> newData = dataMap(section.getList("nd", Tag.TAG_COMPOUND));
            for (int i = 0; i < cells.length && i < olds.length && i < news.length; i++) {
                boolean known = palette.known(olds[i]) && palette.known(news[i]);
                step.recover(key, (short) cells[i], Block.getId(palette.state(olds[i])), oldData.get(i), Block.getId(palette.state(news[i])),
                        newData.get(i), 0L, known);
            }
        }
    }

    private static Map<Integer, CompoundTag> dataMap(ListTag list) {
        Map<Integer, CompoundTag> map = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            map.put(tag.getInt("i"), tag.getCompound("d"));
        }
        return map;
    }

    /** A step as it is, as blocks: one a section, then one for the rest. */
    private static @Nullable List<byte[]> encodeImage(Image image) {
        List<byte[]> blocks = new ArrayList<>();
        for (SectionImage section : image.sections()) {
            Palette palette = new Palette();
            int n = section.cells().length;
            int[] cells = new int[n];
            int[] olds = new int[n];
            int[] news = new int[n];
            long[] hashes = new long[n];
            ListTag oldData = new ListTag();
            ListTag newData = new ListTag();
            for (int i = 0; i < n; i++) {
                short cell = section.cells()[i];
                cells[i] = cell & 0xFFFF;
                olds[i] = palette.of(Block.stateById(section.olds()[i]));
                news[i] = palette.of(Block.stateById(section.news()[i]));
                hashes[i] = section.hashes().get(cell);
                CompoundTag od = section.oldData().get(cell);
                if (od != null) {
                    oldData.add(dataTag(i, od));
                }
                CompoundTag nd = section.newData().get(cell);
                if (nd != null) {
                    newData.add(dataTag(i, nd));
                }
            }
            CompoundTag tag = new CompoundTag();
            tag.putString("k", "sec");
            tag.putLong("key", section.key());
            tag.put("palette", palette.tags());
            tag.put("cells", new IntArrayTag(cells));
            tag.put("olds", new IntArrayTag(olds));
            tag.put("news", new IntArrayTag(news));
            tag.putByteArray("status", section.status());
            tag.put("hashes", new net.minecraft.nbt.LongArrayTag(hashes));
            tag.put("od", oldData);
            tag.put("nd", newData);
            blocks.add(frame(compress(tag)));
        }
        Palette palette = new Palette();
        CompoundTag end = new CompoundTag();
        end.putString("k", "end");
        end.putInt("state", image.state().ordinal());
        end.putInt("cost", image.cost());
        end.putLong("practice", image.practice());
        end.putBoolean("mom", image.mom());
        end.putBoolean("paid", image.paid());
        end.putBoolean("tell", image.tell());
        end.putBoolean("recovered", image.recovered());
        ListTag effects = new ListTag();
        for (SideEffectImage effect : image.sideEffects()) {
            CompoundTag tag = new CompoundTag();
            tag.putLong("pos", effect.pos());
            tag.putInt("old", palette.of(Block.stateById(effect.old())));
            tag.putInt("now", palette.of(Block.stateById(effect.now())));
            tag.putByte("status", effect.status());
            effects.add(tag);
        }
        end.put("palette", palette.tags());
        end.put("effects", effects);
        ListTag carried = new ListTag();
        for (UndoStep.Carried thing : image.carried()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("id", thing.id());
            tag.putLong("from", thing.from().asLong());
            tag.putInt("fromFacing", thing.fromFacing().get3DDataValue());
            tag.putLong("to", thing.to().asLong());
            tag.putInt("toFacing", thing.toFacing().get3DDataValue());
            carried.add(tag);
        }
        end.put("carried", carried);
        blocks.add(frame(compress(end)));
        return blocks;
    }

    private static void decodeSection(CompoundTag tag, UndoStep step) {
        Read palette = new Read(tag.getList("palette", Tag.TAG_COMPOUND));
        long key = tag.getLong("key");
        int[] cells = tag.getIntArray("cells");
        int[] olds = tag.getIntArray("olds");
        int[] news = tag.getIntArray("news");
        byte[] status = tag.getByteArray("status");
        long[] hashes = tag.getLongArray("hashes");
        Map<Integer, CompoundTag> oldData = dataMap(tag.getList("od", Tag.TAG_COMPOUND));
        Map<Integer, CompoundTag> newData = dataMap(tag.getList("nd", Tag.TAG_COMPOUND));
        for (int i = 0; i < cells.length && i < olds.length && i < news.length && i < status.length; i++) {
            boolean known = palette.known(olds[i]) && palette.known(news[i]);
            step.restore(key, (short) cells[i], Block.getId(palette.state(olds[i])), oldData.get(i), Block.getId(palette.state(news[i])), newData.get(i),
                    i < hashes.length ? hashes[i] : 0L, known ? status[i] : UndoStep.DROPPED);
        }
    }

    private static void decodeEnd(CompoundTag tag, UndoStep step) {
        UndoStep.State[] states = UndoStep.State.values();
        step.setState(states[Math.floorMod(tag.getInt("state"), states.length)]);
        step.setBilly(tag.getInt("cost"), tag.getLong("practice"), tag.getBoolean("mom"));
        step.setPaid(tag.getBoolean("paid"));
        step.setTell(tag.getBoolean("tell"));
        step.markRecovered(tag.getBoolean("recovered"));
        Read palette = new Read(tag.getList("palette", Tag.TAG_COMPOUND));
        ListTag effects = tag.getList("effects", Tag.TAG_COMPOUND);
        for (int i = 0; i < effects.size(); i++) {
            CompoundTag effect = effects.getCompound(i);
            int old = effect.getInt("old");
            int now = effect.getInt("now");
            if (palette.known(old) && palette.known(now)) {
                step.restoreSideEffect(effect.getLong("pos"), palette.state(old), palette.state(now), effect.getByte("status"));
            }
        }
        ListTag carried = tag.getList("carried", Tag.TAG_COMPOUND);
        for (int i = 0; i < carried.size(); i++) {
            CompoundTag thing = carried.getCompound(i);
            step.carried(thing.getUUID("id"), BlockPos.of(thing.getLong("from")), Direction.from3DDataValue(thing.getInt("fromFacing")),
                    BlockPos.of(thing.getLong("to")), Direction.from3DDataValue(thing.getInt("toFacing")));
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Blocks and files

    private static byte[] compress(CompoundTag tag) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            NbtIo.writeCompressed(tag, out);
        } catch (IOException e) {
            throw new IllegalStateException(e); // (an in-memory stream never fails)
        }
        return out.toByteArray();
    }

    /** {@code int length, int crc, bytes}. */
    private static byte[] frame(byte[] payload) {
        CRC32 crc = new CRC32();
        crc.update(payload);
        ByteBuffer buffer = ByteBuffer.allocate(8 + payload.length);
        buffer.putInt(payload.length).putInt((int) crc.getValue()).put(payload);
        return buffer.array();
    }

    /** What reading a file's blocks found. */
    private record Result(boolean readable, boolean whole) {
    }

    /**
     * Reads the blocks of a step file in order. A tail that is cut short, or whose checksum fails, ends the blocks
     * (a crash during an append); a file without the right header is unreadable.
     */
    private static Result forEachBlock(Path path, Consumer<CompoundTag> sink) {
        try (InputStream raw = Files.newInputStream(path); DataInputStream in = new DataInputStream(new BufferedInputStream(raw, 1 << 16))) {
            if (in.readInt() != MAGIC || in.readInt() != FORMAT) {
                return new Result(false, false);
            }
            while (true) {
                int length;
                try {
                    length = in.readInt();
                } catch (EOFException e) {
                    return new Result(true, true);
                }
                try {
                    int crcWas = in.readInt();
                    if (length < 0 || length > MAX_BLOCK) {
                        return new Result(true, false);
                    }
                    byte[] payload = new byte[length];
                    in.readFully(payload);
                    CRC32 crc = new CRC32();
                    crc.update(payload);
                    if ((int) crc.getValue() != crcWas) {
                        return new Result(true, false);
                    }
                    CompoundTag tag = NbtIo.readCompressed(new java.io.ByteArrayInputStream(payload), NbtAccounter.unlimitedHeap());
                    sink.accept(tag);
                } catch (EOFException e) {
                    return new Result(true, false);
                }
            }
        } catch (IOException | RuntimeException e) {
            return new Result(false, false);
        }
    }

    private static void writeCompressed(CompoundTag tag, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (FileChannel out = FileChannel.open(temp, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            ByteBuffer buffer = ByteBuffer.wrap(compress(tag));
            while (buffer.hasRemaining()) {
                out.write(buffer);
            }
            out.force(true);
        }
        move(temp, file);
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Sets a file that cannot be read aside, as {@code <name>.corrupt}. */
    private static void setAside(Path file) {
        try {
            Files.move(file, file.resolveSibling(file.getFileName() + ".corrupt"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Enhanced.LOGGER.error("Build Magic: {} could not be set aside", file, e);
        }
    }

    private static void delete(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            Enhanced.LOGGER.warn("Build Magic: {} could not be deleted", file, e);
        }
    }

    private static void deleteTree(Path folder) {
        if (!Files.isDirectory(folder)) {
            return;
        }
        try (Stream<Path> files = Files.walk(folder)) {
            for (Path file : files.sorted(java.util.Comparator.reverseOrder()).toList()) {
                delete(file);
            }
        } catch (IOException e) {
            Enhanced.LOGGER.warn("Build Magic: {} could not be deleted", folder, e);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // For tests

    /**
     * Rewrites every block of a step file with {@code edit} applied to its tag (a test makes a block that no longer
     * exists, or a file that is cut): waits for what is queued first.
     */
    public static void edit(UUID owner, UndoStep step, Consumer<CompoundTag> edit) {
        drain();
        Path path = stepPath(owner, step.file());
        List<byte[]> blocks = new ArrayList<>();
        forEachBlock(path, tag -> {
            edit.accept(tag);
            blocks.add(frame(compress(tag)));
        });
        try {
            StepFile file = new StepFile(path);
            file.replace(blocks);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The path of a step's file. */
    public static Path pathOf(UUID owner, UndoStep step) {
        return stepPath(owner, step.file());
    }

    /** The folder of a player's history. */
    public static Path folderOf(UUID owner) {
        return dir(owner);
    }
}
