package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.build.SpellMath.Volume;
import com.kolodkingal.enhanced.network.BuildPocketPayload;
import com.kolodkingal.enhanced.registry.ModTags;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiPredicate;

/**
 * Copied cells (D-B 4.8): a Copy's pocket, which a builder keeps and stamps again and again, and the source a Move or a
 * Repeat reads into a pocket of its own when it starts, so that a source and a target that overlap come out exact
 * (B29) and a Move can tell what changed under it since (C12). It is what the planner stamps
 * ({@link SpellPlanner.Source}).
 *
 * <p>The cells are kept as runs of one block along {@code x}, row by row ({@code y}, then {@code z}), as the history
 * and the pocket's file keep them (D-B Appendix B), so a box full of air or of one stone costs a few numbers a row.
 * A cell the copy does not have (outside a selected thing, a block magic never touches, half of a door whose other
 * half is outside, a cell in a chunk that was not loaded) is absent: a stamp leaves the target cell alone.
 *
 * <p>What a copy keeps of a block entity (D-B 11.6, {@link Contents}): a Move carries everything; a Copy or a Repeat
 * keeps the data of the types {@code enhanced:build_copies_data} names, with what they hold only for a hero whose
 * {@code copiesContents} is on (the Scarlet Witch); any other block entity comes out as the bare block.
 */
public final class Pocket implements SpellPlanner.Source {
    /** What a copy keeps of a block entity. */
    public enum Contents {
        /** Everything (a Move: it moves, it does not copy). */
        ALL,
        /** The data of {@code build_copies_data} types, items included ({@code copiesContents} on). */
        COPIES,
        /** The data of {@code build_copies_data} types that hold no items; containers come out empty. */
        COPIES_EMPTY;

        /** What {@code limits}' hero keeps in a copy. */
        public static Contents of(Limits limits) {
            return limits.copiesContents() ? COPIES : COPIES_EMPTY;
        }
    }

    /**
     * The pocket each builder holds (her last Copy's take), by player; kept on disk too ({@code pocket.bin},
     * {@link UndoFiles}), so it survives a restart, and read back at her next login.
     */
    private static final Map<UUID, Pocket> HELD = new HashMap<>();
    private static int lastId;
    /** What a run of the pocket is reckoned to cost to keep: its block's index and its length, as two numbers. */
    private static final int RUN_BYTES = 6;
    /** What a block of its palette is reckoned to cost. */
    private static final int STATE_BYTES = 8;

    private final int id;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final BlockPos origin;
    /** The blocks of its runs; index 0 (null) is an absent cell. */
    private final List<BlockState> palette;
    /** For each row ({@code y * sizeZ + z}), where its runs start; one more for the end. */
    private final int[] rowStart;
    /** Each run's block ({@link #palette} index; 0 = absent). */
    private final int[] runState;
    /** Each run's end along {@code x} (excluded). */
    private final int[] runEnd;
    private final Long2ObjectOpenHashMap<CompoundTag> data;
    private final Long2LongOpenHashMap hashes;
    private final long cells;

    private Pocket(int id, int sizeX, int sizeY, int sizeZ, BlockPos origin, List<BlockState> palette, int[] rowStart, int[] runState,
                   int[] runEnd, Long2ObjectOpenHashMap<CompoundTag> data, Long2LongOpenHashMap hashes, long cells) {
        this.id = id;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.origin = origin;
        this.palette = palette;
        this.rowStart = rowStart;
        this.runState = runState;
        this.runEnd = runEnd;
        this.data = data;
        this.hashes = hashes;
        this.cells = cells;
    }

    /** A number for a new pocket (never 0, which is "the pocket is empty"). */
    public static int nextId() {
        lastId = lastId == Integer.MAX_VALUE ? 1 : lastId + 1;
        return lastId;
    }

    /** The pocket {@code player} holds, or null (she copied nothing yet). */
    public static @Nullable Pocket held(UUID player) {
        return HELD.get(player);
    }

    /** {@code player} now holds {@code pocket} (a Copy's take; it travels with her to any dimension). */
    public static void hold(UUID player, Pocket pocket) {
        HELD.put(player, pocket);
        UndoFiles.writePocket(player, pocket);
    }

    /** The pocket read back from {@code pocket.bin} at {@code player}'s login: held, not written again. */
    static void holdLoaded(UUID player, Pocket pocket) {
        HELD.put(player, pocket);
    }

    /** {@code player}'s pocket is gone ({@code /enhanced build forget}). */
    static void drop(UUID player) {
        HELD.remove(player);
    }

    /** Forgets every builder's pocket (the server's stop, the end of a test). */
    public static void clearHeld() {
        HELD.clear();
    }

    /** Its number (what a client that lacks it asks for). */
    public int id() {
        return id;
    }

    /** Where its low corner was in the world when it was read. */
    public BlockPos origin() {
        return origin;
    }

    @Override
    public int sizeX() {
        return sizeX;
    }

    @Override
    public int sizeY() {
        return sizeY;
    }

    @Override
    public int sizeZ() {
        return sizeZ;
    }

    /** How many cells it has (absent ones not counted). */
    @Override
    public long cells() {
        return cells;
    }

    @Override
    public @Nullable BlockState state(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= sizeX || y >= sizeY || z >= sizeZ) {
            return null;
        }
        int row = y * sizeZ + z;
        int from = rowStart[row];
        int to = rowStart[row + 1];
        // The first run of the row that ends after x.
        int low = from;
        int high = to - 1;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (runEnd[mid] <= x) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        int index = runState[low];
        return index == 0 ? null : palette.get(index);
    }

    @Override
    public @Nullable CompoundTag data(int x, int y, int z) {
        return data.get(index(x, y, z));
    }

    /** The hash ({@link UndoStep#hash}) of the cell's block-entity data as it was read, or 0 for none. */
    public long hash(int x, int y, int z) {
        return hashes.get(index(x, y, z));
    }

    private long index(int x, int y, int z) {
        return ((long) y * sizeZ + z) * sizeX + x;
    }

    /**
     * About how many bytes it takes to keep (its runs, its palette and its data), what {@code pocketMaxMb} bounds (D-B
     * 4.8: "That is too big to keep in your pocket"). It is counted before compression, so it never lets a pocket
     * through that the compressed file would refuse.
     */
    public long bytes() {
        return estimate(runState.length, palette.size(), data);
    }

    private static long estimate(long runs, int states, Long2ObjectOpenHashMap<CompoundTag> data) {
        long bytes = runs * RUN_BYTES + (long) states * STATE_BYTES;
        for (CompoundTag tag : data.values()) {
            bytes += tag.sizeInBytes();
        }
        return bytes;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Its file ({@code pocket.bin}, D-B Appendix B)

    /**
     * The pocket as its file keeps it (D-B Appendix B): its {@code size}, its {@code palette} (block names, index 1 on;
     * 0 is an absent cell), its {@code cells} as runs of a palette index and a length over y, then z, then x, the
     * {@code data} of its block entities by cell, its {@code id} and the {@code origin} it was read at. A pocket never
     * changes once read, so this is made off the server thread.
     */
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("size", new int[]{sizeX, sizeY, sizeZ});
        tag.putIntArray("origin", new int[]{origin.getX(), origin.getY(), origin.getZ()});
        tag.putInt("id", id);
        ListTag states = new ListTag();
        for (int i = 1; i < palette.size(); i++) {
            states.add(UndoFiles.stateTag(palette.get(i)));
        }
        tag.put("palette", states);
        IntArrayList runs = new IntArrayList();
        for (int row = 0; row + 1 < rowStart.length; row++) {
            int start = 0;
            for (int i = rowStart[row]; i < rowStart[row + 1]; i++) {
                int length = runEnd[i] - start;
                start = runEnd[i];
                if (!runs.isEmpty() && runs.getInt(runs.size() - 2) == runState[i]) {
                    runs.set(runs.size() - 1, runs.getInt(runs.size() - 1) + length); // (a run goes on into the next row)
                } else {
                    runs.add(runState[i]);
                    runs.add(length);
                }
            }
        }
        tag.putIntArray("cells", runs.toIntArray());
        ListTag entities = new ListTag();
        for (var entry : data.long2ObjectEntrySet()) {
            CompoundTag cell = new CompoundTag();
            cell.putLong("i", entry.getLongKey());
            cell.put("d", entry.getValue());
            entities.add(cell);
        }
        tag.put("data", entities);
        return tag;
    }

    /**
     * The pocket a file keeps ({@link #toTag}), or null when the tag is not a whole one. A block whose name no longer
     * exists is an absent cell ("keep": a stamp leaves the target alone), and its data is dropped.
     */
    public static @Nullable Pocket fromTag(CompoundTag tag) {
        int[] size = tag.getIntArray("size");
        if (size.length != 3 || size[0] <= 0 || size[1] <= 0 || size[2] <= 0 || (long) size[1] * size[2] >= Integer.MAX_VALUE) {
            return null;
        }
        int[] at = tag.getIntArray("origin");
        BlockPos origin = at.length == 3 ? new BlockPos(at[0], at[1], at[2]) : BlockPos.ZERO;
        List<BlockState> palette = new ArrayList<>();
        palette.add(null);
        ListTag states = tag.getList("palette", Tag.TAG_COMPOUND);
        int[] remap = new int[states.size() + 1];
        for (int i = 0; i < states.size(); i++) {
            BlockState state = UndoFiles.readState(states.getCompound(i));
            if (state != null) {
                remap[i + 1] = palette.size();
                palette.add(state);
            } // (else 0: a block that no longer exists is read as absent)
        }
        int rows = size[1] * size[2];
        int[] rowStart = new int[rows + 1];
        IntArrayList runState = new IntArrayList();
        IntArrayList runEnd = new IntArrayList();
        int[] runs = tag.getIntArray("cells");
        int row = 0;
        int x = 0;
        long cells = 0;
        for (int i = 0; i + 1 < runs.length; i += 2) {
            int index = runs[i];
            long left = runs[i + 1];
            if (index < 0 || index >= remap.length || left <= 0) {
                return null;
            }
            index = remap[index];
            while (left > 0) {
                if (row >= rows) {
                    return null; // (more cells than the pocket has)
                }
                int take = (int) Math.min(left, size[0] - x);
                if (x == 0) {
                    rowStart[row] = runState.size();
                }
                if (x > 0 && runState.getInt(runState.size() - 1) == index) {
                    runEnd.set(runEnd.size() - 1, x + take);
                } else {
                    runState.add(index);
                    runEnd.add(x + take);
                }
                if (index != 0) {
                    cells += take;
                }
                x += take;
                left -= take;
                if (x == size[0]) {
                    x = 0;
                    row++;
                }
            }
        }
        if (row != rows || x != 0) {
            return null; // (fewer cells than the pocket has)
        }
        rowStart[rows] = runState.size();
        Pocket shape = new Pocket(0, size[0], size[1], size[2], origin, palette, rowStart, runState.toIntArray(), runEnd.toIntArray(),
                new Long2ObjectOpenHashMap<>(), new Long2LongOpenHashMap(), cells);
        Long2ObjectOpenHashMap<CompoundTag> data = new Long2ObjectOpenHashMap<>();
        Long2LongOpenHashMap hashes = new Long2LongOpenHashMap();
        ListTag entities = tag.getList("data", Tag.TAG_COMPOUND);
        for (int i = 0; i < entities.size(); i++) {
            CompoundTag cell = entities.getCompound(i);
            long index = cell.getLong("i");
            int cx = (int) (index % size[0]);
            long rest = index / size[0];
            if (index < 0 || shape.state(cx, (int) (rest / size[2]), (int) (rest % size[2])) == null) {
                continue; // (an absent cell has no data)
            }
            CompoundTag value = cell.getCompound("d");
            data.put(index, value);
            hashes.put(index, UndoStep.hash(value));
        }
        int id = tag.getInt("id");
        if (id > 0) {
            lastId = Math.max(lastId, id); // (a pocket read back keeps its number; new ones never take it)
        }
        return new Pocket(id, size[0], size[1], size[2], origin, palette, rowStart, runState.toIntArray(), runEnd.toIntArray(), data, hashes, cells);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Sending one to its owner's client

    /**
     * The most cells a pocket may have to travel whole to its owner's client (D-B 13.1): a bigger one goes as its outer
     * surface only, and when even that has more cells, as its size alone (the ghost is then a frame). It is what a
     * ghost draws at most ({@code ghostDetail}).
     */
    public static final int SENT_WHOLE = 262_144;

    /** How many of its runs a stream reads a tick while it gets a big pocket ready to send. */
    private static final int STREAM_RUNS = 65_536;

    /** The pocket as its owner's client will get it ({@code BuildPocketPayload}), got ready a slice a tick. */
    public Stream stream() {
        return new Stream(SENT_WHOLE);
    }

    /** The same with another number in place of {@link #SENT_WHOLE} (a test's small pocket sent as its surface). */
    public Stream stream(int whole) {
        return new Stream(whole);
    }

    /**
     * A pocket on its way to its owner's client: its cells as runs over {@code y}, then {@code z}, then {@code x} (the
     * client's own order), made a slice a tick ({@link #step}), then cut into parts ({@code BuildPocketPayload#split}),
     * of which the server sends at most 4 a tick. A pocket of at most {@link #SENT_WHOLE} cells goes whole, its air
     * included (a box copy's air carves the ghost too); a bigger one as its outer surface only, the blocks with an
     * absent, air or outside neighbour; and if that surface still has more than {@link #SENT_WHOLE} cells, as its size
     * alone.
     */
    public final class Stream {
        private final int most;
        private final boolean whole;
        private final ByteBuf runs = Unpooled.buffer();
        private int row;
        private int pendingIndex = -1;
        private long pendingLength;
        private long surface;
        private boolean sizeOnly;
        private @Nullable List<BuildPocketPayload> parts;
        private int sent;

        private Stream(int most) {
            this.most = most;
            this.whole = cells <= most;
        }

        /** The pocket it sends. */
        public Pocket pocket() {
            return Pocket.this;
        }

        /** Gets on with it for one tick; true once every part is ready ({@link #next}). */
        public boolean step() {
            if (parts != null) {
                return true;
            }
            int rows = rowStart.length - 1;
            int budget = STREAM_RUNS;
            while (row < rows && budget > 0 && !sizeOnly) {
                budget -= whole ? wholeRow(row) : surfaceRow(row);
                row++;
            }
            if (row < rows && !sizeOnly) {
                return false;
            }
            List<BlockState> states = palette.subList(1, palette.size());
            if (sizeOnly) {
                runs.clear();
                pendingIndex = -1;
                pendingLength = 0;
                add(0, (long) sizeX * sizeY * sizeZ);
                states = List.of();
            }
            flush();
            byte[] bytes = new byte[runs.readableBytes()];
            runs.readBytes(bytes);
            runs.release();
            parts = BuildPocketPayload.split(id, sizeX, sizeY, sizeZ, states, bytes);
            return true;
        }

        /** Whether it is sent as its outer surface (a pocket of more than {@link #SENT_WHOLE} cells). */
        public boolean surfaceOnly() {
            return !whole && !sizeOnly;
        }

        /** Whether it is sent as its size alone (even its surface has more than {@link #SENT_WHOLE} cells). */
        public boolean sizeOnly() {
            return sizeOnly;
        }

        /** The next parts to send, at most {@code max} of them (none until it is ready). */
        public List<BuildPocketPayload> next(int max) {
            if (parts == null || sent >= parts.size()) {
                return List.of();
            }
            List<BuildPocketPayload> next = parts.subList(sent, Math.min(parts.size(), sent + max));
            sent += next.size();
            return next;
        }

        /** Whether every part has been handed out. */
        public boolean done() {
            return parts != null && sent >= parts.size();
        }

        /** The row as it is. Returns how many runs it read. */
        private int wholeRow(int r) {
            int start = 0;
            for (int i = rowStart[r]; i < rowStart[r + 1]; i++) {
                add(runState[i], runEnd[i] - start);
                start = runEnd[i];
            }
            return Math.max(1, rowStart[r + 1] - rowStart[r]);
        }

        /** The row's blocks on the outside of the pocket; every other cell absent. Returns how many runs it read. */
        private int surfaceRow(int r) {
            int y = r / sizeZ;
            int z = r % sizeZ;
            int[] solid = solid(y, z);
            // A cell is inside when both its neighbours along x and the four of the rows beside it are blocks too.
            int[] inside = erode(solid);
            inside = meet(inside, solid(y - 1, z));
            inside = meet(inside, solid(y + 1, z));
            inside = meet(inside, solid(y, z - 1));
            inside = meet(inside, solid(y, z + 1));
            int start = 0;
            int k = 0;
            for (int i = rowStart[r]; i < rowStart[r + 1]; i++) {
                int end = runEnd[i];
                int index = runState[i];
                if (index == 0 || palette.get(index).isAir()) {
                    add(0, end - start);
                    start = end;
                    continue;
                }
                int at = start;
                while (at < end) {
                    while (k < inside.length && inside[k + 1] <= at) {
                        k += 2;
                    }
                    if (k < inside.length && inside[k] <= at) {
                        int to = Math.min(end, inside[k + 1]);
                        add(0, to - at);
                        at = to;
                    } else {
                        int to = k < inside.length ? Math.min(end, inside[k]) : end;
                        add(index, to - at);
                        surface += to - at;
                        at = to;
                    }
                }
                start = end;
            }
            if (surface > most) {
                sizeOnly = true;
            }
            return Math.max(1, (rowStart[r + 1] - rowStart[r]) * 6);
        }

        /** The stretches along x of row {@code y, z} that hold a block (not absent, not air), as from, to pairs. */
        private int[] solid(int y, int z) {
            if (y < 0 || z < 0 || y >= sizeY || z >= sizeZ) {
                return NO_STRETCHES;
            }
            int r = y * sizeZ + z;
            IntArrayList out = new IntArrayList();
            int start = 0;
            for (int i = rowStart[r]; i < rowStart[r + 1]; i++) {
                int index = runState[i];
                if (index != 0 && !palette.get(index).isAir()) {
                    if (!out.isEmpty() && out.getInt(out.size() - 1) == start) {
                        out.set(out.size() - 1, runEnd[i]);
                    } else {
                        out.add(start);
                        out.add(runEnd[i]);
                    }
                }
                start = runEnd[i];
            }
            return out.toIntArray();
        }

        /** Appends {@code length} cells of {@code index}, joined with the run before when it is the same. */
        private void add(int index, long length) {
            if (length <= 0) {
                return;
            }
            if (index == pendingIndex) {
                pendingLength += length;
            } else {
                flush();
                pendingIndex = index;
                pendingLength = length;
            }
        }

        private void flush() {
            while (pendingIndex >= 0 && pendingLength > 0) {
                int length = (int) Math.min(Integer.MAX_VALUE, pendingLength);
                BuildPocketPayload.writeRun(runs, pendingIndex, length);
                pendingLength -= length;
            }
            pendingIndex = -1;
        }
    }

    private static final int[] NO_STRETCHES = new int[0];

    /** Stretches (from, to pairs) less one cell at each end: the cells whose two neighbours along x are in them too. */
    private static int[] erode(int[] stretches) {
        IntArrayList out = new IntArrayList();
        for (int i = 0; i < stretches.length; i += 2) {
            if (stretches[i] + 1 < stretches[i + 1] - 1) {
                out.add(stretches[i] + 1);
                out.add(stretches[i + 1] - 1);
            }
        }
        return out.toIntArray();
    }

    /** The cells two lists of stretches (from, to pairs, in order) share. */
    private static int[] meet(int[] a, int[] b) {
        IntArrayList out = new IntArrayList();
        int i = 0;
        int j = 0;
        while (i < a.length && j < b.length) {
            int from = Math.max(a[i], b[j]);
            int to = Math.min(a[i + 1], b[j + 1]);
            if (from < to) {
                out.add(from);
                out.add(to);
            }
            if (a[i + 1] < b[j + 1]) {
                i += 2;
            } else {
                j += 2;
            }
        }
        return out.toIntArray();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Reading one

    /**
     * Reads the cells of {@code region} out of {@code level} into a pocket, a slice at a time ({@link Taker#step}): a
     * Copy's take, a Move's or a Repeat's source when it starts. {@code loaded} says which chunks may be read (x, z of
     * a chunk); a cell of any other is absent and never loads it.
     */
    public static Taker take(ServerLevel level, Volume region, Contents contents, int id, BiPredicate<Integer, Integer> loaded) {
        return new Taker(level, region, contents, id, loaded);
    }

    /** A pocket being read, a slice at a time. */
    public static final class Taker {
        private final ServerLevel level;
        private final Volume region;
        private final Contents contents;
        private final int id;
        private final BiPredicate<Integer, Integer> loaded;
        private final BoundingBox box;
        private final List<BlockState> palette = new ArrayList<>();
        private final Object2IntOpenHashMap<BlockState> indices = new Object2IntOpenHashMap<>();
        private final int[] rowStart;
        private final IntArrayList runState = new IntArrayList();
        private final IntArrayList runEnd = new IntArrayList();
        private final Long2ObjectOpenHashMap<CompoundTag> data = new Long2ObjectOpenHashMap<>();
        private final Long2LongOpenHashMap hashes = new Long2LongOpenHashMap();
        private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        private int row;
        private int x;
        private int current = -1;
        private long cells;
        private long read;
        private long dataBytes;
        private @Nullable Pocket pocket;
        private @Nullable BiPredicate<BlockPos, BlockState> mayKeep;

        private Taker(ServerLevel level, Volume region, Contents contents, int id, BiPredicate<Integer, Integer> loaded) {
            this.level = level;
            this.region = region;
            this.contents = contents;
            this.id = id;
            this.loaded = loaded;
            this.box = region.bounds();
            this.rowStart = new int[box.getYSpan() * box.getZSpan() + 1];
            palette.add(null); // index 0: absent
        }

        /**
         * The pocket keeps what a block entity holds only where {@code mayKeep} says so (a claim's vault is copied bare); the
         * predicate gets the cell and its block.
         */
        public void reading(BiPredicate<BlockPos, BlockState> mayKeep) {
            this.mayKeep = mayKeep;
        }

        /** Reads on for at most {@code reads} cells; true once the whole box is read ({@link #pocket}). */
        public boolean step(int reads) {
            int rows = rowStart.length - 1;
            for (int i = 0; i < reads && pocket == null; i++) {
                if (x == 0) {
                    rowStart[row] = runState.size();
                    current = -1;
                }
                int y = row / box.getZSpan();
                int z = row % box.getZSpan();
                add(cell(box.minX() + x, box.minY() + y, box.minZ() + z));
                read++;
                if (++x == box.getXSpan()) {
                    x = 0;
                    if (++row == rows) {
                        finish();
                    }
                }
            }
            return pocket != null;
        }

        /** How many cells it has read. */
        public long read() {
            return read;
        }

        /** About how many bytes what it has read so far takes to keep ({@link Pocket#bytes}). */
        public long bytes() {
            return (long) runState.size() * RUN_BYTES + (long) palette.size() * STATE_BYTES + dataBytes;
        }

        /** The pocket, once read; else null. */
        public @Nullable Pocket pocket() {
            return pocket;
        }

        private void add(int index) {
            if (index == current) {
                runEnd.set(runEnd.size() - 1, x + 1);
                return;
            }
            runState.add(index);
            runEnd.add(x + 1);
            current = index;
        }

        /** The palette index of the cell (0 for absent), its data kept as the contents rule says. */
        private int cell(int wx, int wy, int wz) {
            if (!region.contains(wx, wy, wz) || !loaded.test(wx >> 4, wz >> 4)) {
                return 0;
            }
            pos.set(wx, wy, wz);
            BlockState state = level.getBlockState(pos);
            if (SpellMath.untouchable(state, level, pos)) {
                return 0; // never copied: a stamp leaves the target cell alone
            }
            BlockPos partner = SpellMath.partner(state, pos);
            if (partner != null && !region.contains(partner.getX(), partner.getY(), partner.getZ())) {
                return 0; // never half a door
            }
            BlockEntity entity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
            if (entity != null && (mayKeep == null || mayKeep.test(pos, state))) {
                CompoundTag tag = kept(entity);
                if (tag != null) {
                    long at = ((long) (wy - box.minY()) * box.getZSpan() + (wz - box.minZ())) * box.getXSpan() + (wx - box.minX());
                    data.put(at, tag);
                    hashes.put(at, UndoStep.hash(tag));
                    dataBytes += tag.sizeInBytes();
                }
            }
            cells++;
            int index = indices.getOrDefault(state, -1);
            if (index < 0) {
                index = palette.size();
                palette.add(state);
                indices.put(state, index);
            }
            return index;
        }

        /** What the copy keeps of a block entity (null: the bare block). */
        private @Nullable CompoundTag kept(BlockEntity entity) {
            CompoundTag all = entity.saveWithoutMetadata(level.registryAccess());
            if (contents == Contents.ALL) {
                return all;
            }
            if (!BuiltInRegistries.BLOCK_ENTITY_TYPE.wrapAsHolder(entity.getType()).is(ModTags.BUILD_COPIES_DATA)) {
                return null;
            }
            if (contents == Contents.COPIES_EMPTY && BuiltInRegistries.BLOCK_ENTITY_TYPE.wrapAsHolder(entity.getType()).is(ModTags.BUILD_HOLDS_ITEMS)) {
                return null; // Billy's chests come out empty
            }
            return all;
        }

        private void finish() {
            rowStart[rowStart.length - 1] = runState.size();
            pocket = new Pocket(id, box.getXSpan(), box.getYSpan(), box.getZSpan(), new BlockPos(box.minX(), box.minY(), box.minZ()),
                    new ArrayList<>(palette), rowStart, runState.toIntArray(), runEnd.toIntArray(), data,
                    hashes, cells);
        }
    }
}
