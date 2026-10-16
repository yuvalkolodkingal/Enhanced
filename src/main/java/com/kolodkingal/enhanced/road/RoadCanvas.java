package com.kolodkingal.enhanced.road;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The surface a trial is drawn on. A {@link RoadChallenge} writes its blocks here ({@link #set}, {@link #fill}); nothing
 * reaches the world until the framework places the canvas in slices ({@link #placeSome}), and the canvas knows every chunk
 * and the range of heights it touched, which is what makes it possible to take the trial down again, whatever happens
 * (the record is saved before the first block is placed, see {@code RoadSave}). A trial that lets the player build
 * (the plot of the copy challenge) {@link #reserve}s the volume it uses, so that it is cleared too.
 *
 * <p>The canvas refuses to draw outside the cell ({@link RoadCells#HALF_EXTENT}) or the dimension: a bug in a course
 * can never leak into another player's cell.
 */
public final class RoadCanvas {
    /** What placing a block costs against the per-tick budget. */
    private static final int BLOCK_COST = 1;
    /** What touching a chunk for the first time costs (it may have to be generated). */
    private static final int CHUNK_COST = 300;
    /** No neighbour updates (nothing falls, breaks or reacts), no drops, clients told. */
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private final int cell;
    private final BlockPos origin;
    private final int minBuildY;
    private final int maxBuildY;

    private final Long2ObjectOpenHashMap<Long2ObjectOpenHashMap<BlockState>> pending = new Long2ObjectOpenHashMap<>();
    private final LongOpenHashSet chunks = new LongOpenHashSet();
    private int minY = Integer.MAX_VALUE;
    private int maxY = Integer.MIN_VALUE;
    private int blocks;

    // ---- placing ----
    private boolean sealed;
    private long[] order = new long[0];
    private long[][] positions = new long[0][];
    private BlockState[][] states = new BlockState[0][];
    private int chunkCursor;
    private int blockCursor;
    private boolean chunkTouched;
    private int placed;

    /**
     * @param cell      the cell this canvas belongs to
     * @param minBuildY the lowest block the dimension has
     * @param maxBuildY one above the highest block the dimension has
     */
    public RoadCanvas(int cell, int minBuildY, int maxBuildY) {
        this.cell = cell;
        this.origin = RoadCells.origin(cell);
        this.minBuildY = minBuildY;
        this.maxBuildY = maxBuildY;
    }

    public int cell() {
        return cell;
    }

    /** The centre of the cell, at the ground level of the trial. */
    public BlockPos origin() {
        return origin;
    }

    /** The block at an offset from {@link #origin()}. */
    public BlockPos at(int dx, int dy, int dz) {
        return origin.offset(dx, dy, dz);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Drawing

    private void check(BlockPos pos) {
        if (!RoadCells.contains(cell, pos) || pos.getY() < minBuildY || pos.getY() >= maxBuildY) {
            throw new IllegalArgumentException("Outside the cell " + cell + ": " + pos);
        }
    }

    private void touch(BlockPos pos) {
        chunks.add(ChunkPos.asLong(pos));
        minY = Math.min(minY, pos.getY());
        maxY = Math.max(maxY, pos.getY());
    }

    /** Draws a block. */
    public void set(BlockPos pos, BlockState state) {
        if (sealed) {
            throw new IllegalStateException("The canvas is sealed: use live()");
        }
        check(pos);
        touch(pos);
        Long2ObjectOpenHashMap<BlockState> chunk = pending.computeIfAbsent(ChunkPos.asLong(pos), k -> new Long2ObjectOpenHashMap<>());
        if (chunk.put(pos.asLong(), state) == null) {
            blocks++;
        }
    }

    public void set(int dx, int dy, int dz, BlockState state) {
        set(origin.offset(dx, dy, dz), state);
    }

    /** Fills the box between two offsets from the origin (both included). */
    public void fill(int dx1, int dy1, int dz1, int dx2, int dy2, int dz2, BlockState state) {
        for (int x = Math.min(dx1, dx2); x <= Math.max(dx1, dx2); x++) {
            for (int y = Math.min(dy1, dy2); y <= Math.max(dy1, dy2); y++) {
                for (int z = Math.min(dz1, dz2); z <= Math.max(dz1, dz2); z++) {
                    set(origin.offset(x, y, z), state);
                }
            }
        }
    }

    /** Marks the box between two offsets from the origin as used, without drawing in it (a plot the player builds on). */
    public void reserve(int dx1, int dy1, int dz1, int dx2, int dy2, int dz2) {
        if (sealed) {
            throw new IllegalStateException("The canvas is sealed");
        }
        BlockPos a = origin.offset(Math.min(dx1, dx2), Math.min(dy1, dy2), Math.min(dz1, dz2));
        BlockPos b = origin.offset(Math.max(dx1, dx2), Math.max(dy1, dy2), Math.max(dz1, dz2));
        check(a);
        check(b);
        for (int cx = a.getX() >> 4; cx <= b.getX() >> 4; cx++) {
            for (int cz = a.getZ() >> 4; cz <= b.getZ() >> 4; cz++) {
                chunks.add(ChunkPos.asLong(cx, cz));
            }
        }
        minY = Math.min(minY, a.getY());
        maxY = Math.max(maxY, b.getY());
    }

    /** What the canvas holds for a block that is not placed yet, or null. */
    @Nullable
    public BlockState drawn(BlockPos pos) {
        Long2ObjectOpenHashMap<BlockState> chunk = pending.get(ChunkPos.asLong(pos));
        return chunk == null ? null : chunk.get(pos.asLong());
    }

    /** How many blocks have been drawn. */
    public int blockCount() {
        return blocks;
    }

    // ------------------------------------------------------------------------------------------------------------
    // What it touched

    /** Every chunk the canvas touches, as {@link ChunkPos#asLong} values, sorted. */
    public long[] chunks() {
        long[] all = chunks.toLongArray();
        LongArrays.quickSort(all);
        return all;
    }

    public boolean touches(ChunkPos chunk) {
        return chunks.contains(chunk.toLong());
    }

    /** The lowest block drawn or reserved (meaningless while {@link #isEmpty}). */
    public int minY() {
        return minY;
    }

    /** The highest block drawn or reserved. */
    public int maxY() {
        return maxY;
    }

    public boolean isEmpty() {
        return chunks.isEmpty();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Placing

    /** Freezes the drawing; from now on only {@link #placeSome} and {@link #live} act on the world. */
    public void seal() {
        if (sealed) {
            return;
        }
        sealed = true;
        order = pending.keySet().toLongArray();
        LongArrays.quickSort(order);
        positions = new long[order.length][];
        states = new BlockState[order.length][];
        for (int i = 0; i < order.length; i++) {
            Long2ObjectOpenHashMap<BlockState> chunk = pending.get(order[i]);
            positions[i] = new long[chunk.size()];
            states[i] = new BlockState[chunk.size()];
            int n = 0;
            for (Long2ObjectMap.Entry<BlockState> entry : chunk.long2ObjectEntrySet()) {
                positions[i][n] = entry.getLongKey();
                states[i][n] = entry.getValue();
                n++;
            }
        }
        pending.clear();
    }

    public boolean isSealed() {
        return sealed;
    }

    /**
     * Places blocks in the level until the budget is spent.
     *
     * @return whether the whole canvas is placed now
     */
    public boolean placeSome(ServerLevel level, int budget) {
        if (!sealed) {
            seal();
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int spent = 0;
        boolean progressed = false;
        while (chunkCursor < order.length) {
            if (!chunkTouched) {
                // A call always makes some progress, however small its budget.
                if (progressed && spent + CHUNK_COST > budget) {
                    return false;
                }
                spent += CHUNK_COST;
                chunkTouched = true;
            }
            long[] chunkPositions = positions[chunkCursor];
            BlockState[] chunkStates = states[chunkCursor];
            while (blockCursor < chunkPositions.length) {
                if (progressed && spent >= budget) {
                    return false;
                }
                pos.set(chunkPositions[blockCursor]);
                BlockState state = chunkStates[blockCursor];
                blockCursor++;
                if (state.isAir() && level.getBlockState(pos).isAir()) {
                    continue;
                }
                level.setBlock(pos, state, FLAGS);
                placed++;
                spent += BLOCK_COST;
                progressed = true;
            }
            chunkCursor++;
            blockCursor = 0;
            chunkTouched = false;
        }
        return true;
    }

    /** Whether every block has been placed. */
    public boolean isPlaced() {
        return sealed && chunkCursor >= order.length;
    }

    /** How many blocks {@link #placeSome} has changed so far. */
    public int placedCount() {
        return placed;
    }

    /**
     * Changes a block of a placed trial (a gate that opens, a checkpoint that lights). The block must be inside what the
     * canvas touched, so that taking the trial down clears it.
     */
    public void live(ServerLevel level, BlockPos pos, BlockState state) {
        if (!chunks.contains(ChunkPos.asLong(pos)) || pos.getY() < minY || pos.getY() > maxY) {
            throw new IllegalArgumentException("Not inside the canvas: " + pos);
        }
        level.setBlock(pos, state, FLAGS);
    }
}
