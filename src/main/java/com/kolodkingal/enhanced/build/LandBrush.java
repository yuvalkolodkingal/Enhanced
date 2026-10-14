package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.registry.ModTags;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Land brush (D-B 4.11, R46): hold the right button and sweep, and the ground under the brush rises, sinks or
 * smooths out, a pulse every {@value #PULSE_TICKS} ticks. Pure and common, like the planner: nothing here writes a
 * block, reads a config or names a client class. The server's job plans each pulse with it ({@link Pulse}) and writes
 * it through the rules, a column at a time; a client draws the brush's disc on the ground with {@link #surface}.
 *
 * <p><b>The disc.</b> The columns within the brush's radius r of its middle (the block looked at), nearest the middle
 * first (Appendix A.3). Each column moves by a falloff {@code (1 - (d/r)^2)^2} of one block a pulse ({@link #falloff}:
 * one at the middle, nothing at the rim); the fraction is kept per column for the whole stroke ({@link Memory}), so a
 * hill rises smoothly, and no column moves more than one block a pulse.
 *
 * <p><b>The surface</b> of a column is its highest ground block ({@code enhanced:build_ground}: dirt, grass, stone,
 * sand, gravel...) within r + {@value #REACH} of the aimed height whose upper neighbour lies loose on it
 * ({@link #loose}: air, a fluid, a plant, a snow layer, leaves or a log). A column without one is left alone: a house,
 * a road or a roof is never the ground, and a barrier or a cave's ceiling above the ground does not hide it.
 *
 * <ul>
 *     <li><b>Raise</b>: the surface block moves up one and the cell it left takes the block that was under it (dirt
 *     under grass; else the surface block itself); the small plants and the snow on top ride up.</li>
 *     <li><b>Lower</b>: the surface block moves down one, replacing the block under it (ground, or nothing at all:
 *     never a floor or a chest); the plants ride down, and the cell at the top is left empty (water under water).</li>
 *     <li><b>Smooth</b>: each column's target is the average height of the surfaces round it within max(1, r / 3) (a
 *     square, the column itself left out), all measured before the pulse moves any; a pulse moves the column one block
 *     toward it when it is more than half a block away.</li>
 * </ul>
 *
 * <p><b>What it never moves.</b> A column moves only whole: its surface block, what rides on it, and the open cell
 * above (air or a fluid). Anything else on top holds it where it is (a log, leaves, a block, a container), so trees and
 * houses stand as they stood; nothing magic never touches, and no block with a block entity (a suspicious sand), moves
 * or is replaced; and a rising column never copies an ore (the cell it leaves takes the surface block itself, and an
 * ore on an ore stays). The rules of every cell (claims, arenas, bodies...) are the job's, and a column one of them
 * refuses stays whole too.
 */
public final class LandBrush {
    /** A brush pulses every 4 ticks while the right button is held (D-B 14.7). */
    public static final int PULSE_TICKS = 4;
    /** How far above and below the aimed height, beyond the radius, a column's surface is looked for (D-B 4.11). */
    public static final int REACH = 8;
    /** The most plants (a stack of sugar cane, bamboo) that ride on a column; a taller stack holds it. */
    public static final int MAX_RIDERS = 16;
    /** A column without a surface. */
    public static final int NO_SURFACE = Integer.MIN_VALUE;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    /** The disc's columns per radius, nearest the middle first (each packed {@code dx << 16 | dz & 0xFFFF}). */
    private static final Map<Integer, int[]> DISCS = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------------------------------------------------
    // The disc and the surface

    /**
     * How much of a block a column {@code dx, dz} from the brush's middle moves a pulse: {@code (1 - (d/r)^2)^2}, one
     * at the middle, nothing at the rim and beyond. A brush of radius 0 moves its middle column only.
     */
    public static double falloff(int dx, int dz, int radius) {
        if (radius <= 0) {
            return dx == 0 && dz == 0 ? 1.0 : 0.0;
        }
        double t = 1.0 - (double) (dx * dx + dz * dz) / ((double) radius * radius);
        return t <= 0.0 ? 0.0 : t * t;
    }

    /** Whether the column {@code dx, dz} from the brush's middle is in its disc (within its radius). */
    public static boolean inDisc(int dx, int dz, int radius) {
        return (long) dx * dx + (long) dz * dz <= (long) radius * radius;
    }

    /** Smooth's reach round a column, max(1, r / 3) (D-B 4.11). */
    public static int smoothReach(int radius) {
        return Math.max(1, radius / 3);
    }

    /**
     * The cells a pulse of this brush may read or write, for the chunks it may touch and the island it must not touch:
     * its disc (and Smooth's reach round it) from below the lowest surface it looks for to above the plants on the
     * highest.
     */
    public static BoundingBox reach(BlockPos middle, int radius, LandStyle style) {
        int r = Math.max(0, radius);
        int side = r + (style == LandStyle.SMOOTH ? smoothReach(r) : 0);
        return new BoundingBox(middle.getX() - side, middle.getY() - r - REACH - 1, middle.getZ() - side, middle.getX() + side,
                middle.getY() + r + REACH + MAX_RIDERS + 1, middle.getZ() + side);
    }

    /**
     * The surface of the column {@code x, z} for a brush of {@code radius} aimed at the height {@code aimY}: the
     * highest ground block within r + {@value #REACH} of it whose upper neighbour lies loose on it, or
     * {@link #NO_SURFACE}. A column in a chunk that is not loaded has none (it is never read).
     */
    public static int surface(BlockGetter level, int x, int z, int aimY, int radius) {
        return new Reader(level).surface(x, z, aimY, radius);
    }

    /**
     * Whether a block lies loose on the ground rather than being part of it: air, a fluid, a plant (a log and leaves
     * too), a snow layer, or anything a placed block replaces. A ground block under one is a surface.
     */
    public static boolean loose(BlockState state) {
        return SpellMath.isEmpty(state) || state.is(ModTags.BUILD_PLANTS) || state.is(Blocks.SNOW) || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS);
    }

    /**
     * Whether a block rides on a moving column: a small plant or a snow layer (no log, no leaves, nothing with a block
     * entity, nothing magic never makes, no fire).
     */
    public static boolean rides(BlockState state) {
        if (state.isAir() || SpellMath.isOpen(state) || state.hasBlockEntity() || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)
                || state.is(ModTags.BUILD_UNCONJURABLE) || state.is(ModTags.BUILD_VOLATILE)) {
            return false;
        }
        return state.is(ModTags.BUILD_PLANTS) || state.is(Blocks.SNOW) || state.canBeReplaced();
    }

    /** The disc of {@code radius}: its columns as packed offsets, nearest the middle first, then by z, then by x. */
    static int[] disc(int radius) {
        return DISCS.computeIfAbsent(Math.max(0, radius), r -> {
            List<int[]> all = new ArrayList<>();
            for (int dz = -r; dz <= r; dz++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (inDisc(dx, dz, r)) {
                        all.add(new int[]{dx, dz});
                    }
                }
            }
            all.sort(Comparator.<int[]>comparingInt(c -> c[0] * c[0] + c[1] * c[1]).thenComparingInt(c -> c[1]).thenComparingInt(c -> c[0]));
            int[] packed = new int[all.size()];
            for (int i = 0; i < packed.length; i++) {
                packed[i] = all.get(i)[0] << 16 | all.get(i)[1] & 0xFFFF;
            }
            return packed;
        });
    }

    private static int dx(int packed) {
        return packed >> 16;
    }

    private static int dz(int packed) {
        return (short) packed;
    }

    // ------------------------------------------------------------------------------------------------------------
    // A stroke and its pulses

    /**
     * What a Land stroke remembers from pulse to pulse: how far (a fraction of a block) each column has moved but not
     * yet shown. One per stroke; the server keeps it with the stroke.
     */
    public static final class Memory {
        private final Long2DoubleOpenHashMap moved = new Long2DoubleOpenHashMap();

        /**
         * The column {@code x, z} moves {@code fraction} of a block more: true when that makes a whole block, which it
         * then owes no more.
         */
        boolean move(int x, int z, double fraction) {
            long key = (long) x << 32 | z & 0xFFFFFFFFL;
            double now = moved.get(key) + fraction;
            boolean whole = now >= 1.0;
            moved.put(key, whole ? now - 1.0 : now);
            return whole;
        }

        /** How many columns it remembers. */
        public int columns() {
            return moved.size();
        }
    }

    /**
     * One column's move in a pulse: the cells {@code y} to {@code y + size() - 1} of the column {@code x, z}, what each
     * holds now and what it gets, and which of them gets a block from magic rather than from the column itself (the
     * cell a rising surface leaves; -1 for none). The arrays are read, never changed.
     */
    public record Column(int x, int y, int z, BlockState[] olds, BlockState[] news, int conjured) {
        /** How many cells it spans. */
        public int size() {
            return olds.length;
        }

        public BlockPos pos(int i) {
            return new BlockPos(x, y + i, z);
        }

        /** Whether the cell {@code i} changes. */
        public boolean changes(int i) {
            return !olds[i].equals(news[i]);
        }

        /** How many of its cells change. */
        public int changes() {
            int n = 0;
            for (int i = 0; i < olds.length; i++) {
                if (changes(i)) {
                    n++;
                }
            }
            return n;
        }
    }

    /**
     * One pulse of a stroke at its middle and radius, planned against the world as it is, a few reads at a time
     * ({@link #step}): for Smooth the surfaces round the disc are measured first; then each column of the disc, nearest
     * the middle first, takes its share of a block from the stroke's {@link Memory} and, when that makes a whole one,
     * plans its move ({@link Column}). Planning changes nothing in the world (only the memory); the job writes the
     * columns in their order.
     */
    public static final class Pulse implements BrushPulse {
        private final Reader reader;
        private final LandStyle style;
        private final BlockPos middle;
        private final int radius;
        private final Memory memory;
        private final int[] disc;
        private final int reach;
        /** Smooth: the half side of the measured square (the disc and its reach round it), its surfaces, and sums. */
        private final int span;
        private int @Nullable [] heights;
        private long @Nullable [] sums;
        private int @Nullable [] counts;
        private int measured;
        private int planned;
        private final List<Column> columns = new ArrayList<>();
        private long changes;
        private int minX = Integer.MAX_VALUE;
        private int minY = Integer.MAX_VALUE;
        private int minZ = Integer.MAX_VALUE;
        private int maxX = Integer.MIN_VALUE;
        private int maxY = Integer.MIN_VALUE;
        private int maxZ = Integer.MIN_VALUE;

        public Pulse(BlockGetter level, LandStyle style, BlockPos middle, int radius, Memory memory) {
            this.reader = new Reader(level);
            this.style = style;
            this.middle = middle.immutable();
            this.radius = Math.max(0, radius);
            this.memory = memory;
            this.disc = disc(this.radius);
            this.reach = smoothReach(this.radius);
            this.span = this.radius + reach;
            if (style == LandStyle.SMOOTH) {
                int side = 2 * span + 1;
                heights = new int[side * side];
            }
        }

        /** Plans on with about {@code reads} block reads (at least a column); true once the whole pulse is planned. */
        public boolean step(int reads) {
            long until = reader.reads + Math.max(1, reads);
            while (reader.reads < until) {
                if (heights != null && measured < heights.length) {
                    measure();
                    continue;
                }
                if (planned >= disc.length) {
                    return true;
                }
                plan(disc[planned++]);
            }
            return done();
        }

        /** Whether every column of the disc is planned. */
        public boolean done() {
            return planned >= disc.length && (heights == null || measured >= heights.length);
        }

        /** The block reads it has made so far. */
        public long reads() {
            return reader.reads;
        }

        /** The columns that move, in their order (nearest the middle first); complete once {@link #done}. */
        public List<Column> columns() {
            return Collections.unmodifiableList(columns);
        }

        /** How many cells its columns change. */
        public long changes() {
            return changes;
        }

        /** How many cells its columns span (changed or not). */
        public long cells() {
            long n = 0;
            for (Column column : columns) {
                n += column.size();
            }
            return n;
        }

        /** The box of the cells its columns span, or null when none moves. */
        public @Nullable BoundingBox bounds() {
            return columns.isEmpty() ? null : new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
        }

        public BlockPos middle() {
            return middle;
        }

        public int radius() {
            return radius;
        }

        public LandStyle style() {
            return style;
        }

        /** Smooth: the surface of the next column of the measured square. */
        private void measure() {
            int side = 2 * span + 1;
            int i = measured++;
            int x = middle.getX() + i % side - span;
            int z = middle.getZ() + i / side - span;
            heights[i] = reader.surface(x, z, middle.getY(), radius);
            if (measured == heights.length) {
                sums();
            }
        }

        /** Smooth: the sums of the measured surfaces (and how many there are) over every rectangle from the corner. */
        private void sums() {
            int side = 2 * span + 1;
            sums = new long[(side + 1) * (side + 1)];
            counts = new int[(side + 1) * (side + 1)];
            for (int z = 0; z < side; z++) {
                for (int x = 0; x < side; x++) {
                    int h = heights[z * side + x];
                    int at = (z + 1) * (side + 1) + x + 1;
                    int left = at - 1;
                    int up = at - (side + 1);
                    int corner = up - 1;
                    sums[at] = sums[left] + sums[up] - sums[corner] + (h == NO_SURFACE ? 0 : h);
                    counts[at] = counts[left] + counts[up] - counts[corner] + (h == NO_SURFACE ? 0 : 1);
                }
            }
        }

        /** The column {@code packed} (an offset from the middle) takes its share and, when it makes a block, moves. */
        private void plan(int packed) {
            int dx = dx(packed);
            int dz = dz(packed);
            int x = middle.getX() + dx;
            int z = middle.getZ() + dz;
            if (!memory.move(x, z, falloff(dx, dz, radius))) {
                return;
            }
            Column column = switch (style) {
                case RAISE, LOWER -> {
                    int y = reader.surface(x, z, middle.getY(), radius);
                    yield y == NO_SURFACE ? null : reader.move(x, y, z, style == LandStyle.RAISE);
                }
                case SMOOTH -> smooth(dx, dz, x, z);
            };
            if (column == null || column.changes() == 0) {
                return;
            }
            columns.add(column);
            changes += column.changes();
            minX = Math.min(minX, x);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxZ = Math.max(maxZ, z);
            minY = Math.min(minY, column.y());
            maxY = Math.max(maxY, column.y() + column.size() - 1);
        }

        /** Smooth: the column's move one block toward the average of the surfaces round it, or null. */
        private @Nullable Column smooth(int dx, int dz, int x, int z) {
            int side = 2 * span + 1;
            int cx = dx + span;
            int cz = dz + span;
            int h = heights[cz * side + cx];
            if (h == NO_SURFACE) {
                return null;
            }
            int x0 = Math.max(0, cx - reach);
            int z0 = Math.max(0, cz - reach);
            int x1 = Math.min(side - 1, cx + reach);
            int z1 = Math.min(side - 1, cz + reach);
            long sum = rect(sums, x0, z0, x1, z1) - h;
            long count = rect(counts, x0, z0, x1, z1) - 1;
            if (count <= 0) {
                return null;
            }
            double target = (double) sum / count;
            if (target > h + 0.5) {
                return reader.move(x, h, z, true);
            }
            if (target < h - 0.5) {
                return reader.move(x, h, z, false);
            }
            return null;
        }

        private long rect(long[] table, int x0, int z0, int x1, int z1) {
            int row = 2 * span + 2;
            return table[(z1 + 1) * row + x1 + 1] - table[z0 * row + x1 + 1] - table[(z1 + 1) * row + x0] + table[z0 * row + x0];
        }

        private long rect(int[] table, int x0, int z0, int x1, int z1) {
            int row = 2 * span + 2;
            return (long) table[(z1 + 1) * row + x1 + 1] - table[z0 * row + x1 + 1] - table[(z1 + 1) * row + x0] + table[z0 * row + x0];
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Reading a column

    /** Reads a world's columns and counts its reads. */
    private static final class Reader {
        private final BlockGetter level;
        private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        long reads;

        Reader(BlockGetter level) {
            this.level = level;
        }

        private BlockState read(int x, int y, int z) {
            reads++;
            return level.getBlockState(pos.set(x, y, z));
        }

        int surface(int x, int z, int aimY, int radius) {
            int top = Math.min(aimY + radius + REACH, level.getMaxBuildHeight() - 1);
            int bottom = Math.max(aimY - radius - REACH, level.getMinBuildHeight());
            if (top < bottom || !SpellMath.loaded(level, pos.set(x, top, z))) {
                reads++;
                return NO_SURFACE;
            }
            BlockState above = top + 1 < level.getMaxBuildHeight() ? read(x, top + 1, z) : AIR;
            for (int y = top; y >= bottom; y--) {
                BlockState state = read(x, y, z);
                if (MagicSelect.isGround(state) && loose(above)) {
                    return y;
                }
                above = state;
            }
            return NO_SURFACE;
        }

        /**
         * The move of the column {@code x, z} whose surface is at {@code y}, one block up or down, or null when it
         * cannot move whole: something that does not ride holds it, its surface (or for Lower the block under it) is
         * not to be moved or replaced, or it would leave the world.
         */
        @Nullable Column move(int x, int y, int z, boolean up) {
            BlockState surface = read(x, y, z);
            if (surface.hasBlockEntity() || SpellMath.untouchable(surface, level, pos.set(x, y, z))) {
                return null;
            }
            List<BlockState> riders = new ArrayList<>();
            int top = y + 1;
            if (top >= level.getMaxBuildHeight()) {
                return null;
            }
            BlockState open = read(x, top, z);
            while (rides(open)) {
                if (riders.size() == MAX_RIDERS || top + 1 >= level.getMaxBuildHeight()) {
                    return null;
                }
                riders.add(open);
                open = read(x, ++top, z);
            }
            if (!SpellMath.isOpen(open)) {
                return null; // (a log, leaves, a block on top: the column holds)
            }
            BlockState under = y - 1 >= level.getMinBuildHeight() ? read(x, y - 1, z) : null;
            int n = riders.size() + 2;
            BlockState[] olds = new BlockState[n];
            BlockState[] news = new BlockState[n];
            if (up) {
                // y .. top: [surface, riders, open] -> [filler, surface, riders]
                BlockState filler = under != null && plain(under, x, y - 1, z) ? under : plain(surface, x, y, z) ? surface : null;
                if (filler == null) {
                    return null;
                }
                olds[0] = surface;
                news[0] = filler;
                olds[1] = riders.isEmpty() ? open : riders.get(0);
                news[1] = surface;
                for (int i = 0; i < riders.size(); i++) {
                    olds[i + 2] = i + 1 < riders.size() ? riders.get(i + 1) : open;
                    news[i + 2] = riders.get(i);
                }
                news[n - 1] = SpellMath.placedInto(news[n - 1], olds[n - 1]);
                return new Column(x, y, z, olds, news, 0);
            }
            // y - 1 .. top - 1: [under, surface, riders] -> [surface, riders, emptied]
            if (under == null || !(MagicSelect.isGround(under) || SpellMath.isEmpty(under)) || under.hasBlockEntity()
                    || SpellMath.untouchable(under, level, pos.set(x, y - 1, z))) {
                return null;
            }
            olds[0] = under;
            news[0] = surface;
            olds[1] = surface;
            for (int i = 0; i < riders.size(); i++) {
                news[i + 1] = riders.get(i);
                olds[i + 2] = riders.get(i);
            }
            BlockState last = riders.isEmpty() ? surface : riders.get(riders.size() - 1);
            news[n - 1] = open.getFluidState().is(FluidTags.WATER) && open.getFluidState().isSource() ? WATER : SpellMath.vanished(last);
            return new Column(x, y - 1, z, olds, news, -1);
        }

        /**
         * Whether a rising column may fill the cell it leaves with {@code state}: plain ground, which magic may make,
         * and not an ore (a column never copies one).
         */
        private boolean plain(BlockState state, int x, int y, int z) {
            return MagicSelect.isGround(state) && !state.is(Tags.Blocks.ORES) && !state.is(ModTags.BUILD_UNCONJURABLE) && !state.hasBlockEntity()
                    && !SpellMath.untouchable(state, level, pos.set(x, y, z));
        }
    }

    private LandBrush() {
    }
}
