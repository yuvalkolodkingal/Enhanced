package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.registry.ModTags;
import com.kolodkingal.enhanced.ultron.SokoviaShape.Spec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/**
 * The island as a list of blocks, kept in memory (nothing of it is written to disk): the skin, which is the ground as
 * it is when the fight lifts it, and the cone of made-up rock under it ({@link SokoviaShape}). The server pastes it in
 * the sky and clears it again; each client reads the same list from its own world to draw it rising.
 *
 * <p>The skin is copied from the ground column by column. The <em>ground</em> of a column is its highest full block
 * that the island carries and that is not a log or leaves (so a tree, a flower, the snow on a hill, or a wall somebody
 * built is not mistaken for the ground); from there the cells
 * {@code skin} deep down to {@code cover} high up are copied if they are blocks the island carries (the tag {@code
 * enhanced:sokovia_liftable}: stone, soil, plants, trees). A block that is not (a chest, a door, a torch, a wall of
 * somebody's house, water) is not copied: the island has its own rock where the ground was, and air above it. So
 * nothing with an inventory or a hidden state is ever moved, and nothing that somebody built is touched, because
 * nothing of the real ground is edited at all.
 *
 * <p>The cells are kept bottom-up, which is the order the island is built in (so nothing is ever unsupported); only the
 * blocks are listed, never air. All coordinates are those of the ground, where the island was copied from.
 */
public final class SokoviaTemplate {
    /** The ground of a column that has none within reach. */
    public static final int NO_GROUND = Integer.MIN_VALUE;
    /** How far below the middle (and above its cover) a column's ground is looked for. */
    private static final int LOOK_DOWN = 64;

    private final Spec spec;
    private final int cx;
    private final int cy;
    private final int cz;
    /** The ground y of each column of the square round the middle ({@link #NO_GROUND} outside the island). */
    private final int[] ground;
    private final int[] cellX;
    private final int[] cellY;
    private final int[] cellZ;
    private final BlockState[] cellState;
    private final int minY;
    private final int maxY;

    private SokoviaTemplate(Spec spec, BlockPos centre, int[] ground, int[] x, int[] y, int[] z, BlockState[] state) {
        this.spec = spec;
        this.cx = centre.getX();
        this.cy = centre.getY();
        this.cz = centre.getZ();
        this.ground = ground;
        // Bottom-up: sort the cells by height (a stable sort keeps the columns in order within a layer).
        int n = y.length;
        long[] keys = new long[n];
        for (int i = 0; i < n; i++) {
            keys[i] = ((long) y[i] << 32) | i;
        }
        Arrays.sort(keys);
        this.cellX = new int[n];
        this.cellY = new int[n];
        this.cellZ = new int[n];
        this.cellState = new BlockState[n];
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            int from = (int) keys[i];
            cellX[i] = x[from];
            cellY[i] = y[from];
            cellZ[i] = z[from];
            cellState[i] = state[from];
            low = Math.min(low, y[from]);
            high = Math.max(high, y[from]);
        }
        this.minY = n == 0 ? cy : low;
        this.maxY = n == 0 ? cy : high;
    }

    // ---- making one ----

    /** The island as it would be if it were lifted from the ground round {@code centre} now. */
    public static SokoviaTemplate capture(BlockGetter level, Spec spec, BlockPos centre) {
        int extent = spec.extent();
        int side = extent * 2 + 1;
        int[] ground = new int[side * side];
        Arrays.fill(ground, NO_GROUND);
        Cells cells = new Cells();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dz = -extent; dz <= extent; dz++) {
            for (int dx = -extent; dx <= extent; dx++) {
                if (!SokoviaShape.inside(spec, dx, dz)) {
                    continue;
                }
                int x = centre.getX() + dx;
                int z = centre.getZ() + dz;
                int g = groundHeight(level, x, z, centre.getY(), spec);
                if (g == NO_GROUND) {
                    g = centre.getY() - 1;
                }
                ground[(dz + extent) * side + dx + extent] = g;
                // The skin: the ground and what is under it (a cave stays a cave; what is not carried is the island's
                // rock).
                for (int y = g - spec.skin() + 1; y <= g; y++) {
                    BlockState state = level.getBlockState(pos.set(x, y, z));
                    if (state.isAir()) {
                        continue;
                    }
                    cells.add(x, y, z, carried(state) ? state : Blocks.STONE.defaultBlockState());
                }
                // The cover: grass, flowers, snow, saplings, trees. What is not carried is left behind.
                for (int y = g + 1; y <= g + spec.cover(); y++) {
                    BlockState state = level.getBlockState(pos.set(x, y, z));
                    if (!state.isAir() && carried(state)) {
                        cells.add(x, y, z, state);
                    }
                }
                // The cone: rock made up from the seed, hanging under the skin.
                int depth = SokoviaShape.coneDepth(spec, dx, dz);
                for (int k = 0; k < depth; k++) {
                    cells.add(x, g - spec.skin() - k, z, SokoviaShape.coneBlock(spec, dx, dz, k, depth));
                }
            }
        }
        return new SokoviaTemplate(spec, centre, ground, cells.x(), cells.y(), cells.z(), cells.state());
    }

    /**
     * The island as it stands in the sky, {@code dy} blocks above the ground it came from: every block there is in the
     * columns of its shape, whatever it is (what a client draws as the island comes down, and what is cleared). The
     * coordinates are those of the sky.
     */
    public static SokoviaTemplate read(BlockGetter level, Spec spec, BlockPos centre, int dy) {
        int extent = spec.extent();
        int side = extent * 2 + 1;
        int[] ground = new int[side * side];
        Arrays.fill(ground, NO_GROUND);
        Cells cells = new Cells();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int low = centre.getY() + dy - spec.skin() - spec.cone() - 16;
        int high = centre.getY() + dy + spec.cover() + 16;
        low = Math.max(low, level.getMinBuildHeight());
        high = Math.min(high, level.getMaxBuildHeight() - 1);
        for (int dz = -extent; dz <= extent; dz++) {
            for (int dx = -extent; dx <= extent; dx++) {
                if (!SokoviaShape.inside(spec, dx, dz)) {
                    continue;
                }
                int x = centre.getX() + dx;
                int z = centre.getZ() + dz;
                int top = NO_GROUND;
                for (int y = low; y <= high; y++) {
                    BlockState state = level.getBlockState(pos.set(x, y, z));
                    if (!state.isAir()) {
                        cells.add(x, y, z, state);
                        if (state.isCollisionShapeFullBlock(level, pos) && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS)) {
                            top = y;
                        }
                    }
                }
                ground[(dz + extent) * side + dx + extent] = top;
            }
        }
        return new SokoviaTemplate(spec, centre.above(dy), ground, cells.x(), cells.y(), cells.z(), cells.state());
    }

    // ---- what the ground is ----

    /**
     * The y of the ground of the column {@code (x, z)} of a fight centred at height {@code middleY}: its highest full
     * block that is not a log or leaves, looked for downward from above the highest cover the island can carry; or
     * {@link #NO_GROUND}.
     */
    public static int groundHeight(BlockGetter level, int x, int z, int middleY, Spec spec) {
        int top = Math.min(level.getMaxBuildHeight() - 1, middleY + spec.cover() + 8);
        int bottom = Math.max(level.getMinBuildHeight(), middleY - LOOK_DOWN);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
        for (int y = top; y >= bottom; y--) {
            pos.setY(y);
            if (isGround(level, pos, level.getBlockState(pos))) {
                return y;
            }
        }
        return NO_GROUND;
    }

    /**
     * Whether the block is the ground a plant or a tree stands on: a full block that the island carries and that is
     * neither a log nor leaves. (A full block that is not carried, a wall or a floor of planks, is not: the ground is
     * what is under it, and the island has the natural ground where somebody built.)
     */
    public static boolean isGround(BlockGetter level, BlockPos pos, BlockState state) {
        return !state.isAir() && carried(state) && !state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)
                && state.isCollisionShapeFullBlock(level, pos);
    }

    /** Whether the island carries this block (and not water, which is not in the tag either). */
    public static boolean carried(BlockState state) {
        return state.is(ModTags.SOKOVIA_LIFTABLE) && state.getFluidState().isEmpty();
    }

    // ---- what it is ----

    public Spec spec() {
        return spec;
    }

    /** The middle of the island (the block the fight is centred on), at the height of the cells' frame. */
    public BlockPos centre() {
        return new BlockPos(cx, cy, cz);
    }

    /** How many blocks the island has. */
    public int size() {
        return cellX.length;
    }

    public int x(int i) {
        return cellX[i];
    }

    public int y(int i) {
        return cellY[i];
    }

    public int z(int i) {
        return cellZ[i];
    }

    public BlockState state(int i) {
        return cellState[i];
    }

    /** The lowest and highest y of any block. */
    public int minY() {
        return minY;
    }

    public int maxY() {
        return maxY;
    }

    /**
     * The highest y the island's room reaches: the highest ground with the cover (grass, trees) on top of it.
     * Everything from {@link #minY} to here is cleared when the island goes, whatever stands in it.
     */
    public int roomTop() {
        int top = highestGround();
        return Math.max(maxY, top == NO_GROUND ? maxY : top + spec.cover());
    }

    /**
     * The y of the ground of the column {@code dx, dz} blocks from the middle ({@link #NO_GROUND} outside the island):
     * the highest full block of the column for an island read in the sky.
     */
    public int groundAt(int dx, int dz) {
        int extent = spec.extent();
        if (Math.abs(dx) > extent || Math.abs(dz) > extent) {
            return NO_GROUND;
        }
        return ground[(dz + extent) * (extent * 2 + 1) + dx + extent];
    }

    /**
     * The highest and the lowest ground of the island (for the range of heights it spans); both {@link #NO_GROUND} if
     * none.
     */
    public int highestGround() {
        int best = NO_GROUND;
        for (int g : ground) {
            best = Math.max(best, g);
        }
        return best;
    }

    public int lowestGround() {
        int best = Integer.MAX_VALUE;
        for (int g : ground) {
            if (g != NO_GROUND) {
                best = Math.min(best, g);
            }
        }
        return best == Integer.MAX_VALUE ? NO_GROUND : best;
    }

    /** The number of blocks in the skin and the cover, which came from the ground (the rest is cone). */
    public int groundCells() {
        int n = 0;
        for (int i = 0; i < size(); i++) {
            int dx = cellX[i] - cx;
            int dz = cellZ[i] - cz;
            int g = groundAt(dx, dz);
            if (g != NO_GROUND && cellY[i] > g - spec.skin()) {
                n++;
            }
        }
        return n;
    }

    // ---- building ----

    /** Growable parallel arrays of cells, before they are sorted. */
    private static final class Cells {
        private int[] x = new int[4096];
        private int[] y = new int[4096];
        private int[] z = new int[4096];
        private BlockState[] state = new BlockState[4096];
        private int size;

        void add(int cellX, int cellY, int cellZ, BlockState cellState) {
            if (size == x.length) {
                int grown = size * 2;
                x = Arrays.copyOf(x, grown);
                y = Arrays.copyOf(y, grown);
                z = Arrays.copyOf(z, grown);
                state = Arrays.copyOf(state, grown);
            }
            x[size] = cellX;
            y[size] = cellY;
            z[size] = cellZ;
            state[size] = cellState;
            size++;
        }

        int[] x() {
            return Arrays.copyOf(x, size);
        }

        int[] y() {
            return Arrays.copyOf(y, size);
        }

        int[] z() {
            return Arrays.copyOf(z, size);
        }

        BlockState[] state() {
            return Arrays.copyOf(state, size);
        }
    }
}
