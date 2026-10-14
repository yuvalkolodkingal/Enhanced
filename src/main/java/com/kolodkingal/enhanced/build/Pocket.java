package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.build.SpellMath.Volume;
import com.kolodkingal.enhanced.registry.ModTags;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
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

    /** The pocket each builder holds (her last Copy's take), by player. In memory only, for now ({@code UndoFiles}). */
    private static final Map<UUID, Pocket> HELD = new HashMap<>();
    private static int lastId;

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
        private @Nullable Pocket pocket;

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
            if (entity != null) {
                CompoundTag tag = kept(entity);
                if (tag != null) {
                    long at = ((long) (wy - box.minY()) * box.getZSpan() + (wz - box.minZ())) * box.getXSpan() + (wx - box.minX());
                    data.put(at, tag);
                    hashes.put(at, UndoStep.hash(tag));
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
