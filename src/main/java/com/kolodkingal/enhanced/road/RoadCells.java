package com.kolodkingal.enhanced.road;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

/**
 * The grid of private cells in the Road dimension. Cell {@code i} is centred on x = (i mod 64) * 2048, z = (i / 64) * 2048,
 * and a course may use at most {@link #HALF_EXTENT} blocks round that centre ({@link RoadCanvas} refuses to draw beyond it),
 * so two cells are always more than a thousand blocks apart and nobody sees anybody else's trial.
 */
public final class RoadCells {
    /** Distance between the centres of two neighbouring cells. */
    public static final int SPACING = 2048;
    /** Cells in a row of the grid. */
    public static final int COLUMNS = 64;
    /** How far from the cell's centre a course may reach, in x and z. */
    public static final int HALF_EXTENT = 450;
    /** The height of a cell's origin: the ground level of a trial. */
    public static final int ORIGIN_Y = 96;
    /** The most cells there can be (the config limits the players well below it). */
    public static final int MAX_CELLS = COLUMNS * 64;

    /** The centre of cell {@code index}, at {@link #ORIGIN_Y}. */
    public static BlockPos origin(int index) {
        if (index < 0 || index >= MAX_CELLS) {
            throw new IllegalArgumentException("No such cell: " + index);
        }
        return new BlockPos((index % COLUMNS) * SPACING, ORIGIN_Y, (index / COLUMNS) * SPACING);
    }

    /** The cell a position belongs to (the nearest centre), or -1 if it is beyond the grid. */
    public static int indexAt(BlockPos pos) {
        int column = Mth.floor((pos.getX() + SPACING / 2) / (double) SPACING);
        int row = Mth.floor((pos.getZ() + SPACING / 2) / (double) SPACING);
        if (column < 0 || column >= COLUMNS || row < 0 || row >= MAX_CELLS / COLUMNS) {
            return -1;
        }
        return row * COLUMNS + column;
    }

    /** Whether the position is inside the area cell {@code index} may use. */
    public static boolean contains(int index, BlockPos pos) {
        BlockPos origin = origin(index);
        return Math.abs(pos.getX() - origin.getX()) <= HALF_EXTENT && Math.abs(pos.getZ() - origin.getZ()) <= HALF_EXTENT;
    }

    private RoadCells() {
    }
}
