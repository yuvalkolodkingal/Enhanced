package com.kolodkingal.enhanced.client.build;

import com.kolodkingal.enhanced.build.SpellPlanner;
import com.kolodkingal.enhanced.network.BuildPocketPayload;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * This client's copy of the builder's pocket (D-B 4.8, 13.1): what the server sent in {@link BuildPocketPayload} parts,
 * put together, for the ghost of a Copy's stamp. It keeps the runs as they came (where each starts, its palette index),
 * never an array of every cell, so a big pocket (sent as its outer surface only) costs what its runs cost; a cell is
 * found by a binary search over the runs. It is a {@link SpellPlanner.Source}, so the planner stamps it as the server
 * stamps its own pocket. No block entity data travels: the ghost draws blocks only.
 */
final class ClientPocket implements SpellPlanner.Source {
    private final int id;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final List<BlockState> palette;
    /** The first cell of each run (cells counted over y, z, x). */
    private final long[] starts;
    /** Each run's palette index (0: no block there). */
    private final int[] indices;
    private final long blocks;

    private ClientPocket(int id, int sizeX, int sizeY, int sizeZ, List<BlockState> palette, long[] starts, int[] indices, long blocks) {
        this.id = id;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.palette = palette;
        this.starts = starts;
        this.indices = indices;
        this.blocks = blocks;
    }

    /** The pocket the parts made, or null when its runs are broken (it is then asked for again). */
    static @Nullable ClientPocket of(BuildPocketPayload.Whole whole) {
        long cells = (long) whole.sx() * whole.sy() * whole.sz();
        if (whole.sx() <= 0 || whole.sy() <= 0 || whole.sz() <= 0) {
            return null;
        }
        LongArrayList starts = new LongArrayList();
        IntArrayList indices = new IntArrayList();
        long[] blocks = {0L};
        boolean read = BuildPocketPayload.readRuns(whole.runs(), cells, whole.palette().size(), (first, length, index) -> {
            starts.add(first);
            indices.add(index);
            if (index != 0) {
                blocks[0] += length;
            }
        });
        if (!read) {
            return null;
        }
        return new ClientPocket(whole.pocket(), whole.sx(), whole.sy(), whole.sz(), whole.palette(), starts.toLongArray(),
                indices.toIntArray(), blocks[0]);
    }

    /** The pocket's id ({@code BuildData.pocket}). */
    int id() {
        return id;
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

    /** How many cells hold a block (a mask copy's thing, or a box copy's every cell). */
    @Override
    public long cells() {
        return blocks;
    }

    @Override
    public @Nullable BlockState state(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= sizeX || y >= sizeY || z >= sizeZ) {
            return null;
        }
        long cell = ((long) y * sizeZ + z) * sizeX + x;
        int low = 0;
        int high = starts.length - 1;
        // The last run that starts at or before the cell holds it (the runs cover every cell, in order).
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (starts[middle] <= cell) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        int index = starts.length == 0 ? 0 : indices[low];
        return index == 0 ? null : palette.get(index - 1);
    }
}
