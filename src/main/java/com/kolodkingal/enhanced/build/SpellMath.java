package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.registry.ModTags;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * The pure geometry of Build Magic (D-B 4, 11.1, Appendix A): the same numbers on the server, which builds a spell, and
 * on every client, which draws its ghost and counts it, so that the ghost is the result. Nothing here reads a config,
 * writes a block or names a client class; the few rules that look at blocks read them through a {@link BlockGetter}.
 *
 * <ul>
 *     <li><b>{@link Volume}s</b>: sets of cells that are walked row by row, a row being the cells of one {@code y} and
 *     one {@code z} as runs along {@code x}: the box and its Fill styles ({@link #fill}, {@link #inside}), the eight
 *     {@link Shape}s, a set of cells ({@link #cells}), and {@linkplain #shifted any of them moved}. Rows are computed,
 *     not listed, so the walls of a valley-sized box cost their walls, not their volume.</li>
 *     <li><b>The shapes</b> of Appendix A.1, solid and hollow, in whole blocks and integer arithmetic wherever it can
 *     be, so a client and the server never disagree on a cell (Appendix A.2 pins their counts).</li>
 *     <li><b>Turn and flip</b> ({@link Placement}): where a pocket's cell lands and what its block becomes, and
 *     back.</li>
 *     <li><b>Where a ghost stands</b> ({@link #stand}): on a floor, against a wall, under a ceiling, or in the
 *     air.</li>
 *     <li><b>The mix</b> ({@link Mix}): a block per cell from the hotbar, weighted by stack size, by a hash of the cell
 *     and the cast's seed.</li>
 *     <li><b>The family rule</b> ({@link #swap}) of Change and Paint: oak stairs become spruce stairs.</li>
 *     <li><b>One cell's rules</b>: what vanishing leaves ({@link #vanished}), the material in water
 *     ({@link #placedInto}), what Change may touch ({@link #canChange}, {@link #touchesOpen}), the other half of a door
 *     or a bed ({@link #partner}), where the world ends ({@link #world}, {@link #outside}).</li>
 *     <li><b>The Pull surface</b> ({@link PullSurface}): the face Pull &amp; Push works on, flooded a few cells at a
 *     time.</li>
 * </ul>
 */
public final class SpellMath {
    /** A row with no cell. */
    public static final int[] NO_RUNS = new int[0];
    /** How many shape counts and heart planes are remembered before they are forgotten all at once. */
    private static final int REMEMBERED = 256;
    private static final Map<Shape, Long> COUNTS = new ConcurrentHashMap<>();
    private static final Map<Integer, int[][]> HEARTS = new ConcurrentHashMap<>();

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    // ------------------------------------------------------------------------------------------------------------
    // Volumes

    /**
     * A set of cells, walked row by row. A row is the cells of one {@code y} and one {@code z}; its cells are given as
     * runs along {@code x}: pairs {@code from, to} (both included), ascending, never touching each other. A volume
     * never lists its cells, so a plan of a hundred million cells holds only the numbers that describe it.
     */
    public interface Volume {
        /** The smallest box that holds every cell. */
        BoundingBox bounds();

        /** Whether the cell is in the volume. */
        boolean contains(int x, int y, int z);

        /**
         * The cells of row {@code y, z} between {@code x0} and {@code x1} (both included), as runs. The default asks
         * {@link #contains} for every {@code x}; the volumes of this class compute their rows at once.
         */
        default int[] runs(int y, int z, int x0, int x1) {
            BoundingBox b = bounds();
            if (y < b.minY() || y > b.maxY() || z < b.minZ() || z > b.maxZ()) {
                return NO_RUNS;
            }
            Runs out = new Runs();
            int to = Math.min(x1, b.maxX());
            for (int x = Math.max(x0, b.minX()); x <= to; x++) {
                if (contains(x, y, z)) {
                    out.add(x, x);
                }
            }
            return out.toArray();
        }

        /** How many cells the volume holds, row by row. */
        default long count() {
            BoundingBox b = bounds();
            long n = 0;
            for (int y = b.minY(); y <= b.maxY(); y++) {
                for (int z = b.minZ(); z <= b.maxZ(); z++) {
                    n += length(runs(y, z, b.minX(), b.maxX()));
                }
            }
            return n;
        }
    }

    /** The box with these two corners. */
    public static BoundingBox box(BlockPos a, BlockPos b) {
        return BoundingBox.fromCorners(a, b);
    }

    /** Every cell of the box. */
    public static Volume solid(BoundingBox box) {
        return new BoxCells(box, BoxCells.SOLID);
    }

    /**
     * The cells of the box that Fill turns into the material in this style (D-B 4.2): every cell for Solid and Only the
     * empty (which then keeps what is not empty, a rule of the cell, not of the box), the four sides for Walls, the
     * walls, floor and roof for Room (whose inside, {@link #inside}, is emptied), the bottom layer for Floor, and the
     * twelve edges for Frame.
     */
    public static Volume fill(FillStyle style, BoundingBox box) {
        return new BoxCells(box, switch (style) {
            case WALLS -> BoxCells.WALLS;
            case ROOM -> BoxCells.SHELL;
            case FLOOR -> BoxCells.FLOOR;
            case FRAME -> BoxCells.FRAME;
            default -> BoxCells.SOLID;
        });
    }

    /** The cells of the box that are not on its outer layer: what Room empties and Vanish's Hollow out takes. */
    public static Volume inside(BoundingBox box) {
        return new BoxCells(box, BoxCells.INSIDE);
    }

    /**
     * The cells of a volume whose six neighbours are all in it: the inside of a selected thing for Hollow out (for a
     * box, the same as {@link #inside(BoundingBox)}).
     */
    public static Volume inside(Volume volume) {
        if (volume instanceof BoxCells cells && cells.kind == BoxCells.SOLID) {
            return inside(cells.box);
        }
        return new Interior(volume);
    }

    /** The volume moved by {@code dx, dy, dz}: a shape's offsets put where it stands. */
    public static Volume shifted(Volume volume, int dx, int dy, int dz) {
        return new Shifted(volume, dx, dy, dz, volume.bounds().moved(dx, dy, dz));
    }

    /** The cells of a set of positions ({@link BlockPos#asLong}), which must not change while the volume is used. */
    public static Volume cells(LongSet positions) {
        if (positions.isEmpty()) {
            return new CellSet(positions, new BoundingBox(0, 0, 0, 0, 0, 0));
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (LongIterator it = positions.iterator(); it.hasNext(); ) {
            long p = it.nextLong();
            int x = BlockPos.getX(p);
            int y = BlockPos.getY(p);
            int z = BlockPos.getZ(p);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }
        return new CellSet(positions, new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ));
    }

    /** A box and one of its Fill styles, every row computed from the box alone. */
    private record BoxCells(BoundingBox box, int kind) implements Volume {
        static final int SOLID = 0;
        static final int WALLS = 1;
        static final int SHELL = 2;
        static final int FLOOR = 3;
        static final int FRAME = 4;
        static final int INSIDE = 5;

        @Override
        public BoundingBox bounds() {
            return box;
        }

        @Override
        public boolean contains(int x, int y, int z) {
            if (!box.isInside(x, y, z)) {
                return false;
            }
            int edges = (x == box.minX() || x == box.maxX() ? 1 : 0) + (y == box.minY() || y == box.maxY() ? 1 : 0)
                    + (z == box.minZ() || z == box.maxZ() ? 1 : 0);
            boolean side = x == box.minX() || x == box.maxX() || z == box.minZ() || z == box.maxZ();
            return switch (kind) {
                case WALLS -> side;
                case SHELL -> edges > 0;
                case FLOOR -> y == box.minY();
                case FRAME -> edges >= 2;
                case INSIDE -> edges == 0;
                default -> true;
            };
        }

        @Override
        public int[] runs(int y, int z, int x0, int x1) {
            if (y < box.minY() || y > box.maxY() || z < box.minZ() || z > box.maxZ()) {
                return NO_RUNS;
            }
            int edgeRows = (y == box.minY() || y == box.maxY() ? 1 : 0) + (z == box.minZ() || z == box.maxZ() ? 1 : 0);
            boolean sideRow = z == box.minZ() || z == box.maxZ();
            int[] row = switch (kind) {
                case WALLS -> sideRow ? whole() : ends();
                case SHELL -> edgeRows > 0 ? whole() : ends();
                case FLOOR -> y == box.minY() ? whole() : NO_RUNS;
                case FRAME -> edgeRows >= 2 ? whole() : edgeRows == 1 ? ends() : NO_RUNS;
                case INSIDE -> edgeRows == 0 ? run(box.minX() + 1, box.maxX() - 1) : NO_RUNS;
                default -> whole();
            };
            return clip(row, x0, x1);
        }

        @Override
        public long count() {
            long sx = box.getXSpan();
            long sy = box.getYSpan();
            long sz = box.getZSpan();
            return switch (kind) {
                case SOLID -> sx * sy * sz;
                case INSIDE -> Math.max(0, sx - 2) * Math.max(0, sy - 2) * Math.max(0, sz - 2);
                case SHELL -> sx * sy * sz - Math.max(0, sx - 2) * Math.max(0, sy - 2) * Math.max(0, sz - 2);
                case WALLS -> sy * (sx * sz - Math.max(0, sx - 2) * Math.max(0, sz - 2));
                case FLOOR -> sx * sz;
                default -> {
                    // A cell of the frame has two or three of its coordinates on the box's sides.
                    long bx = Math.min(sx, 2);
                    long by = Math.min(sy, 2);
                    long bz = Math.min(sz, 2);
                    yield bx * by * bz + bx * by * (sz - bz) + bx * (sy - by) * bz + (sx - bx) * by * bz;
                }
            };
        }

        private int[] whole() {
            return run(box.minX(), box.maxX());
        }

        private int[] ends() {
            return box.minX() == box.maxX() ? run(box.minX(), box.minX()) : twoRuns(box.minX(), box.minX(), box.maxX(), box.maxX());
        }
    }

    /** The cells of a volume whose six neighbours are all in it. */
    private record Interior(Volume volume) implements Volume {
        @Override
        public BoundingBox bounds() {
            return volume.bounds();
        }

        @Override
        public boolean contains(int x, int y, int z) {
            return volume.contains(x, y, z) && volume.contains(x + 1, y, z) && volume.contains(x - 1, y, z)
                    && volume.contains(x, y + 1, z) && volume.contains(x, y - 1, z) && volume.contains(x, y, z + 1)
                    && volume.contains(x, y, z - 1);
        }

        @Override
        public int[] runs(int y, int z, int x0, int x1) {
            int[] inner = shrink(volume.runs(y, z, x0 - 1, x1 + 1));
            inner = intersect(inner, volume.runs(y + 1, z, x0, x1));
            inner = intersect(inner, volume.runs(y - 1, z, x0, x1));
            inner = intersect(inner, volume.runs(y, z + 1, x0, x1));
            inner = intersect(inner, volume.runs(y, z - 1, x0, x1));
            return clip(inner, x0, x1);
        }
    }

    private record Shifted(Volume volume, int dx, int dy, int dz, BoundingBox bounds) implements Volume {
        @Override
        public boolean contains(int x, int y, int z) {
            return bounds.isInside(x, y, z) && volume.contains(x - dx, y - dy, z - dz);
        }

        @Override
        public int[] runs(int y, int z, int x0, int x1) {
            if (y < bounds.minY() || y > bounds.maxY() || z < bounds.minZ() || z > bounds.maxZ() || x1 < bounds.minX() || x0 > bounds.maxX()) {
                return NO_RUNS;
            }
            int[] local = volume.runs(y - dy, z - dz, Math.max(x0, bounds.minX()) - dx, Math.min(x1, bounds.maxX()) - dx);
            int[] out = new int[local.length];
            for (int i = 0; i < local.length; i++) {
                out[i] = local[i] + dx;
            }
            return out;
        }

        @Override
        public long count() {
            return volume.count();
        }
    }

    private record CellSet(LongSet positions, BoundingBox bounds) implements Volume {
        @Override
        public boolean contains(int x, int y, int z) {
            return bounds.isInside(x, y, z) && positions.contains(BlockPos.asLong(x, y, z));
        }

        @Override
        public long count() {
            return positions.size();
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Runs: the cells of one row, as {from, to} pairs

    /** Builds the runs of a row from ascending cells or runs, joining those that touch. */
    static final class Runs {
        private int[] buffer = new int[8];
        private int size;

        void add(int from, int to) {
            if (from > to) {
                return;
            }
            if (size > 0 && from <= buffer[size - 1] + 1) {
                buffer[size - 1] = Math.max(buffer[size - 1], to);
                return;
            }
            if (size == buffer.length) {
                buffer = Arrays.copyOf(buffer, size * 2);
            }
            buffer[size++] = from;
            buffer[size++] = to;
        }

        int[] toArray() {
            return size == 0 ? NO_RUNS : Arrays.copyOf(buffer, size);
        }
    }

    /** One run, or none if {@code from > to}. */
    static int[] run(int from, int to) {
        return from > to ? NO_RUNS : new int[]{from, to};
    }

    /** Two runs, the first left of the second; an empty one is left out. */
    static int[] twoRuns(int from1, int to1, int from2, int to2) {
        Runs out = new Runs();
        out.add(from1, to1);
        out.add(from2, to2);
        return out.toArray();
    }

    /** The cells of a row, counted. */
    static long length(int[] runs) {
        long n = 0;
        for (int i = 0; i < runs.length; i += 2) {
            n += runs[i + 1] - runs[i] + 1L;
        }
        return n;
    }

    /** The runs cut to {@code x0..x1}. */
    static int[] clip(int[] runs, int x0, int x1) {
        if (runs.length == 0 || (runs[0] >= x0 && runs[runs.length - 1] <= x1)) {
            return runs;
        }
        Runs out = new Runs();
        for (int i = 0; i < runs.length; i += 2) {
            out.add(Math.max(runs[i], x0), Math.min(runs[i + 1], x1));
        }
        return out.toArray();
    }

    /** The cells in both rows. */
    static int[] intersect(int[] a, int[] b) {
        if (a.length == 0 || b.length == 0) {
            return NO_RUNS;
        }
        Runs out = new Runs();
        int i = 0;
        int j = 0;
        while (i < a.length && j < b.length) {
            out.add(Math.max(a[i], b[j]), Math.min(a[i + 1], b[j + 1]));
            if (a[i + 1] < b[j + 1]) {
                i += 2;
            } else {
                j += 2;
            }
        }
        return out.toArray();
    }

    /** The cells of {@code a} that are not in {@code b}. */
    static int[] subtract(int[] a, int[] b) {
        if (a.length == 0 || b.length == 0) {
            return a;
        }
        Runs out = new Runs();
        int j = 0;
        for (int i = 0; i < a.length; i += 2) {
            int from = a[i];
            int to = a[i + 1];
            while (j < b.length && b[j + 1] < from) {
                j += 2;
            }
            int k = j;
            while (from <= to && k < b.length && b[k] <= to) {
                out.add(from, b[k] - 1);
                from = Math.max(from, b[k + 1] + 1);
                k += 2;
            }
            out.add(from, to);
        }
        return out.toArray();
    }

    /** The cells of the row whose two neighbours along {@code x} are in it too. */
    static int[] shrink(int[] runs) {
        Runs out = new Runs();
        for (int i = 0; i < runs.length; i += 2) {
            out.add(runs[i] + 1, runs[i + 1] - 1);
        }
        return out.toArray();
    }

    /** The row seen in a mirror at {@code x = 0}. */
    static int[] mirror(int[] runs) {
        int[] out = new int[runs.length];
        for (int i = 0; i < runs.length; i += 2) {
            out[runs.length - 2 - i] = -runs[i + 1];
            out[runs.length - 1 - i] = -runs[i];
        }
        return out;
    }

    /** The largest whole number whose square is at most {@code q} ({@code q >= 0}). */
    static int isqrt(long q) {
        long a = (long) Math.sqrt((double) q);
        while (a * a > q) {
            a--;
        }
        while ((a + 1) * (a + 1) <= q) {
            a++;
        }
        return (int) a;
    }

    /** The smallest whole number whose square is at least {@code q} ({@code q >= 0}). */
    static int ceilSqrt(long q) {
        int a = isqrt(q);
        return (long) a * a == q ? a : a + 1;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The eight shapes (Appendix A.1)

    /**
     * One of the eight shapes, in offsets from its base centre: {@code dy} up from the surface it stands on, and for
     * the Arch and the Heart, which stand upright facing the builder, {@code u} to his right, {@code v} up and
     * {@code w} away from him. Every cell test is Appendix A.1's, in whole numbers but for the Cone's and the Heart's
     * (doubles, the same on every machine).
     *
     * <p><b>Hollow</b> keeps a one-block shell: a Ball the cells with one of their six neighbours outside it; a Dome,
     * a Tower, a Cone and a Pyramid those with a neighbour outside among {@code +x, -x, +z, -z, +y} (no floor: the
     * ground is the floor); a Heart, in its own plane, those with one of their four neighbours there outside (an
     * outline). A Ring and an Arch are one block thick already, so they ignore it.
     *
     * @param kind   which shape
     * @param radius its radius, at least 0
     * @param height a Tower's, a Ring's, a Cone's or a Pyramid's height (at least 1); 0 for the others
     * @param depth  an Arch's or a Heart's depth along {@code w} (at least 1; {@link #of} gives {@code 1 + r / 4}); 1
     *               for the others
     * @param hollow whether only the shell is kept
     * @param turn   for the Arch and the Heart, which way the builder looks: 0 north, 1 east, 2 south, 3 west
     *               ({@link #turnOf}); 0 for the others, which every turn leaves the same
     */
    public record Shape(ShapeKind kind, int radius, int height, int depth, boolean hollow, int turn) implements Volume {
        public Shape {
            radius = Math.max(0, radius);
            height = kind.hasHeight() ? Math.max(1, height) : 0;
            depth = kind.upright() ? Math.max(1, depth) : 1;
            hollow = hollow && kind != ShapeKind.RING && kind != ShapeKind.ARCH;
            turn = kind.upright() ? Math.floorMod(turn, 4) : 0;
        }

        /**
         * The shape of a cast: {@code height} exactly as the recipe carries it (at least 1 for a shape with a height,
         * as {@link SpellRecipe#clamped} makes it), the depth {@code 1 + r / 4} of Appendix A.1. A recipe never means
         * "the default height" by 0: a client sends {@link ShapeKind#defaultHeight} until the shape is planted, so the
         * server, which clamps first, and a client planning its ghost from the recipe it sends plan the same shape.
         */
        public static Shape of(ShapeKind kind, int radius, int height, boolean hollow, int turn) {
            int r = Math.max(0, radius);
            return new Shape(kind, r, height, 1 + r / 4, hollow, turn);
        }

        /** The shapes that grow shell by shell from their heart; the others rise layer by layer (Appendix A.3). */
        public boolean round() {
            return kind == ShapeKind.BALL || kind == ShapeKind.DOME || kind == ShapeKind.RING || kind == ShapeKind.ARCH
                    || kind == ShapeKind.HEART;
        }

        /**
         * Where it swells from: a Ball's centre, a Dome's and a Ring's base centre, the middle of an Arch's and a
         * Heart's own plane (half its height up, half its depth in).
         */
        public BlockPos heart() {
            return switch (kind) {
                case BALL -> new BlockPos(0, radius, 0);
                case ARCH -> fromPlane(0, radius / 2, (depth - 1) / 2);
                case HEART -> fromPlane(0, radius, (depth - 1) / 2);
                default -> BlockPos.ZERO;
            };
        }

        @Override
        public BoundingBox bounds() {
            int r = radius;
            return switch (kind) {
                case BALL -> new BoundingBox(-r, 0, -r, r, 2 * r, r);
                case DOME -> new BoundingBox(-r, 0, -r, r, r, r);
                case TOWER, RING, CONE, PYRAMID -> new BoundingBox(-r, 0, -r, r, height - 1, r);
                case ARCH, HEART -> {
                    int top = kind == ShapeKind.ARCH ? r : 2 * r;
                    int d = depth - 1;
                    yield switch (turn) {
                        case 1 -> new BoundingBox(0, 0, -r, d, top, r);
                        case 2 -> new BoundingBox(-r, 0, 0, r, top, d);
                        case 3 -> new BoundingBox(-d, 0, -r, 0, top, r);
                        default -> new BoundingBox(-r, 0, -d, r, top, 0);
                    };
                }
            };
        }

        @Override
        public boolean contains(int x, int y, int z) {
            if (!bounds().isInside(x, y, z) || !solid(x, y, z)) {
                return false;
            }
            if (!hollow) {
                return true;
            }
            boolean byX = kind != ShapeKind.HEART || (turn & 1) == 0;
            boolean byZ = kind != ShapeKind.HEART || (turn & 1) == 1;
            boolean down = kind == ShapeKind.BALL || kind == ShapeKind.HEART;
            return (byX && (!solid(x + 1, y, z) || !solid(x - 1, y, z))) || (byZ && (!solid(x, y, z + 1) || !solid(x, y, z - 1)))
                    || !solid(x, y + 1, z) || (down && !solid(x, y - 1, z));
        }

        @Override
        public int[] runs(int y, int z, int x0, int x1) {
            BoundingBox b = bounds();
            if (y < b.minY() || y > b.maxY() || z < b.minZ() || z > b.maxZ() || x1 < b.minX() || x0 > b.maxX()) {
                return NO_RUNS;
            }
            int from = Math.max(x0, b.minX());
            int to = Math.min(x1, b.maxX());
            if (convex()) {
                // One run per row, the same both sides of x = 0: the hollow row is its two ends, as far in as a
                // neighbour row (or the row itself, along x) reaches the outside.
                int a = extent(y, z);
                if (a < 0 || a < from || -a > to) {
                    return NO_RUNS;
                }
                if (!hollow) {
                    return clip(run(-a, a), from, to);
                }
                int m = Math.min(a - 1, Math.min(extent(y + 1, z), Math.min(extent(y, z + 1), extent(y, z - 1))));
                if (kind == ShapeKind.BALL) {
                    m = Math.min(m, extent(y - 1, z));
                }
                return clip(m < 0 ? run(-a, a) : twoRuns(-a, -m - 1, m + 1, a), from, to);
            }
            if (!hollow) {
                return clip(solidRuns(y, z, from, to), from, to);
            }
            boolean byX = kind != ShapeKind.HEART || (turn & 1) == 0;
            boolean byZ = kind != ShapeKind.HEART || (turn & 1) == 1;
            boolean down = kind == ShapeKind.BALL || kind == ShapeKind.HEART;
            int[] all = solidRuns(y, z, from - 1, to + 1);
            if (all.length == 0) {
                return NO_RUNS;
            }
            int[] inner = byX ? shrink(all) : all;
            inner = intersect(inner, solidRuns(y + 1, z, from, to));
            if (down) {
                inner = intersect(inner, solidRuns(y - 1, z, from, to));
            }
            if (byZ) {
                inner = intersect(inner, solidRuns(y, z + 1, from, to));
                inner = intersect(inner, solidRuns(y, z - 1, from, to));
            }
            return clip(subtract(all, inner), from, to);
        }

        /**
         * How many cells it has: counted row by row in whole numbers, once per shape and remembered (a preflight asks
         * for the size of the same plan a few times a second, and a radius-512 ball has a million rows).
         */
        @Override
        public long count() {
            Long known = COUNTS.get(this);
            if (known != null) {
                return known;
            }
            long n = countRows();
            if (COUNTS.size() >= REMEMBERED) {
                COUNTS.clear();
            }
            COUNTS.put(this, n);
            return n;
        }

        private long countRows() {
            int r = radius;
            if (kind.upright()) {
                // Every layer along w is the same plane.
                long plane = 0;
                for (int v = 0; v <= 2 * r; v++) {
                    int[] row = planeRuns(v, -r - 2, r + 2);
                    if (hollow) {
                        int[] inner = intersect(intersect(shrink(row), planeRuns(v + 1, -r - 2, r + 2)), planeRuns(v - 1, -r - 2, r + 2));
                        row = subtract(row, inner);
                    }
                    plane += length(row);
                }
                return plane * depth;
            }
            if (!convex()) {
                // A Ring: every layer is the same annulus.
                long layer = 0;
                for (int z = -r; z <= r; z++) {
                    layer += length(solidRuns(0, z, -r, r));
                }
                return layer * height;
            }
            // Three layers of row extents at a time, each row's neighbours read from them.
            BoundingBox b = bounds();
            int rows = b.getZSpan() + 2;
            int[] below = extents(b.minY() - 1, b.minZ() - 1, rows);
            int[] here = extents(b.minY(), b.minZ() - 1, rows);
            long n = 0;
            for (int y = b.minY(); y <= b.maxY(); y++) {
                int[] above = extents(y + 1, b.minZ() - 1, rows);
                for (int i = 1; i < rows - 1; i++) {
                    int a = here[i];
                    if (a < 0) {
                        continue;
                    }
                    if (!hollow) {
                        n += 2L * a + 1;
                        continue;
                    }
                    int m = Math.min(a - 1, Math.min(above[i], Math.min(here[i + 1], here[i - 1])));
                    if (kind == ShapeKind.BALL) {
                        m = Math.min(m, below[i]);
                    }
                    n += m < 0 ? 2L * a + 1 : 2L * (a - m);
                }
                below = here;
                here = above;
            }
            return n;
        }

        private int[] extents(int y, int z0, int rows) {
            int[] out = new int[rows];
            for (int i = 0; i < rows; i++) {
                out[i] = extent(y, z0 + i);
            }
            return out;
        }

        /** The shapes whose every row is one run, the same both sides of {@code x = 0}. */
        private boolean convex() {
            return kind == ShapeKind.BALL || kind == ShapeKind.DOME || kind == ShapeKind.TOWER || kind == ShapeKind.CONE
                    || kind == ShapeKind.PYRAMID;
        }

        /** For a {@link #convex} shape, how far row {@code y, z} reaches either side of {@code x = 0}; -1 for none. */
        private int extent(int y, int z) {
            int r = radius;
            int big = r * r + r;
            if (Math.abs(z) > 2 * r + 1) {
                return -1;
            }
            switch (kind) {
                case BALL:
                    return y < 0 || y > 2 * r ? -1 : root(big - (y - r) * (y - r) - z * z);
                case DOME:
                    return y < 0 || y > r ? -1 : root(big - y * y - z * z);
                case TOWER:
                    return y < 0 || y >= height ? -1 : root(big - z * z);
                case CONE: {
                    if (y < 0 || y >= height || !inCone(z * z, y)) {
                        return -1;
                    }
                    double p = (double) (r * (height - y)) / height;
                    int a = (int) Math.sqrt(Math.max(0.0, p * p + p - z * z));
                    while (inCone((a + 1) * (a + 1) + z * z, y)) {
                        a++;
                    }
                    while (a > 0 && !inCone(a * a + z * z, y)) {
                        a--;
                    }
                    return a;
                }
                case PYRAMID: {
                    if (y < 0 || y >= height) {
                        return -1;
                    }
                    int m = r * (height - y) / height;
                    return Math.abs(z) <= m ? m : -1;
                }
                default:
                    throw new IllegalStateException(kind + " is not one run a row");
            }
        }

        private static int root(int q) {
            return q < 0 ? -1 : isqrt(q);
        }

        /** Whether the solid shape holds the cell (the hollow one keeps those of its cells next to the outside). */
        public boolean solid(int x, int y, int z) {
            int r = radius;
            int big = r * r + r;
            if (Math.abs(x) > 2 * r + depth + 1 || Math.abs(z) > 2 * r + depth + 1) {
                return false;
            }
            return switch (kind) {
                case BALL -> y >= 0 && y <= 2 * r && x * x + (y - r) * (y - r) + z * z <= big;
                case DOME -> y >= 0 && y <= r && x * x + y * y + z * z <= big;
                case TOWER -> y >= 0 && y < height && x * x + z * z <= big;
                case RING -> {
                    int q = x * x + z * z;
                    yield y >= 0 && y < height && q > r * r - r && q <= big;
                }
                case CONE -> y >= 0 && y < height && inCone(x * x + z * z, y);
                case PYRAMID -> y >= 0 && y < height && Math.max(Math.abs(x), Math.abs(z)) <= r * (height - y) / height;
                case ARCH, HEART -> {
                    int w = w(x, z);
                    yield w >= 0 && w < depth && inPlane(u(x, z), y);
                }
            };
        }

        /**
         * The runs of row {@code y, z} of the solid shape, exact between {@code x0} and {@code x1} (a row of a Heart is
         * looked at only there; the others give their whole row).
         */
        private int[] solidRuns(int y, int z, int x0, int x1) {
            int r = radius;
            if (Math.abs(z) > 2 * r + depth + 1) {
                return NO_RUNS;
            }
            switch (kind) {
                case BALL, DOME, TOWER, CONE, PYRAMID: {
                    int a = extent(y, z);
                    return a < 0 ? NO_RUNS : run(-a, a);
                }
                case RING:
                    return y < 0 || y >= height ? NO_RUNS : annulus(r * r + r - z * z, r * r - r - z * z);
                default: {
                    if ((turn & 1) == 0) {
                        int w = turn == 0 ? -z : z;
                        if (w < 0 || w >= depth) {
                            return NO_RUNS;
                        }
                        // x is u (looking north) or -u (looking south): the plane's row, mirrored for the south.
                        return turn == 0 ? planeRuns(y, x0, x1) : mirror(planeRuns(y, -x1, -x0));
                    }
                    if (!inPlane(turn == 1 ? z : -z, y)) {
                        return NO_RUNS;
                    }
                    return turn == 1 ? run(0, depth - 1) : run(-(depth - 1), 0);
                }
            }
        }

        /** The {@code u} runs of the Arch's or the Heart's plane at height {@code v}, exact between u0 and u1. */
        private int[] planeRuns(int v, int u0, int u1) {
            int r = radius;
            if (kind == ShapeKind.ARCH) {
                int t = Math.max(1, r / 4);
                return v < 0 ? NO_RUNS : annulus(r * r + r - v * v, (r - t) * (r - t) + (r - t) - v * v);
            }
            if (v < 0 || v > 2 * r) {
                return NO_RUNS;
            }
            int[][] plane = HEARTS.get(r);
            if (plane == null) {
                plane = heartPlane(r);
                if (HEARTS.size() >= REMEMBERED) {
                    HEARTS.clear();
                }
                HEARTS.put(r, plane);
            }
            return clip(plane[v], u0, u1);
        }

        /** Every row of the plane of a heart of radius {@code r}, found once (it is the same both sides of u = 0). */
        private static int[][] heartPlane(int r) {
            int[][] plane = new int[2 * r + 1][];
            for (int v = 0; v <= 2 * r; v++) {
                Runs right = new Runs();
                for (int u = 0; u <= r + 1; u++) {
                    if (inHeart(r, u, v)) {
                        right.add(u, u);
                    }
                }
                int[] half = right.toArray();
                int[] left = mirror(half);
                Runs row = new Runs();
                for (int i = 0; i < left.length; i += 2) {
                    row.add(left[i], left[i + 1]);
                }
                for (int i = 0; i < half.length; i += 2) {
                    row.add(half[i], half[i + 1]);
                }
                plane[v] = row.toArray();
            }
            return plane;
        }

        /** The cells with {@code inner < x^2 <= outer} (all of {@code x^2 <= outer} when {@code inner < 0}). */
        private static int[] annulus(int outer, int inner) {
            if (outer < 0) {
                return NO_RUNS;
            }
            int a = isqrt(outer);
            if (inner < 0) {
                return run(-a, a);
            }
            int b = isqrt(inner);
            return b >= a ? NO_RUNS : twoRuns(-a, -b - 1, b + 1, a);
        }

        private static int[] centred(int q) {
            if (q < 0) {
                return NO_RUNS;
            }
            int a = isqrt(q);
            return run(-a, a);
        }

        private boolean inCone(int q, int y) {
            double p = (double) (radius * (height - y)) / height;
            return q <= p * p + p;
        }

        private boolean inPlane(int u, int v) {
            int r = radius;
            if (kind == ShapeKind.ARCH) {
                int t = Math.max(1, r / 4);
                int q = u * u + v * v;
                return v >= 0 && q > (r - t) * (r - t) + (r - t) && q <= r * r + r;
            }
            return inHeart(r, u, v);
        }

        private static boolean inHeart(int r, int u, int v) {
            if (v < 0 || v > 2 * r) {
                return false;
            }
            double x = u / (r + 0.5) * 1.14;
            double y = (v + 0.5) / (2 * r + 1) * 2.25 - 1.0;
            double a = x * x + y * y - 1.0;
            return a * a * a - x * x * (y * y * y) <= 0.0;
        }

        /** The builder's right, {@code u}, of the cell {@code x, z}. */
        private int u(int x, int z) {
            return switch (turn) {
                case 1 -> z;
                case 2 -> -x;
                case 3 -> -z;
                default -> x;
            };
        }

        /** Away from the builder, {@code w}, of the cell {@code x, z}. */
        private int w(int x, int z) {
            return switch (turn) {
                case 1 -> x;
                case 2 -> z;
                case 3 -> -x;
                default -> -z;
            };
        }

        private BlockPos fromPlane(int u, int v, int w) {
            return switch (turn) {
                case 1 -> new BlockPos(w, v, u);
                case 2 -> new BlockPos(-u, v, w);
                case 3 -> new BlockPos(-w, v, -u);
                default -> new BlockPos(u, v, -w);
            };
        }
    }

    /**
     * The turn of an upright shape (the Arch, the Heart) whose builder looks {@code facing}: 0 north, 1 east, 2 south,
     * 3 west, which is also the number of quarter turns clockwise from north.
     */
    public static int turnOf(Direction facing) {
        return Math.floorMod(facing.get2DDataValue() - 2, 4);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Turn and flip

    /**
     * How a pocket of {@code sizeX x sizeY x sizeZ} cells is turned and flipped when it is stamped (D-B 4.8): first
     * {@code turn} quarter turns clockwise seen from above (as vanilla's {@link Rotation}), then the flip in the
     * world's axes ({@link SpellRecipe#FLIP_X}: east and west swap, {@link SpellRecipe#FLIP_Z}: north and south,
     * {@link SpellRecipe#UPSIDE_DOWN}). Positions are offsets in the source box and in the target box; blocks turn and
     * flip with vanilla's {@code rotate} and {@code mirror}, and upside down swaps tops and bottoms
     * ({@link #upsideDown}).
     */
    public record Placement(int sizeX, int sizeY, int sizeZ, int turn, int flip) {
        public Placement {
            turn = Math.floorMod(turn, 4);
            flip = Math.floorMod(flip, 4);
        }

        /** The target box's size along x. */
        public int targetSizeX() {
            return (turn & 1) == 0 ? sizeX : sizeZ;
        }

        /** The target box's size along z. */
        public int targetSizeZ() {
            return (turn & 1) == 0 ? sizeZ : sizeX;
        }

        public Rotation rotation() {
            return Rotation.values()[turn];
        }

        /** Where the source cell {@code x, y, z} lands in the target box. */
        public BlockPos target(int x, int y, int z) {
            int tx;
            int tz;
            switch (turn) {
                case 1 -> {
                    tx = sizeZ - 1 - z;
                    tz = x;
                }
                case 2 -> {
                    tx = sizeX - 1 - x;
                    tz = sizeZ - 1 - z;
                }
                case 3 -> {
                    tx = z;
                    tz = sizeX - 1 - x;
                }
                default -> {
                    tx = x;
                    tz = z;
                }
            }
            int ty = y;
            switch (flip) {
                case SpellRecipe.FLIP_X -> tx = targetSizeX() - 1 - tx;
                case SpellRecipe.FLIP_Z -> tz = targetSizeZ() - 1 - tz;
                case SpellRecipe.UPSIDE_DOWN -> ty = sizeY - 1 - ty;
                default -> {
                }
            }
            return new BlockPos(tx, ty, tz);
        }

        /** The source cell that lands on the target cell {@code tx, ty, tz}, or null outside the target box. */
        public @Nullable BlockPos source(int tx, int ty, int tz) {
            if (tx < 0 || ty < 0 || tz < 0 || tx >= targetSizeX() || ty >= sizeY || tz >= targetSizeZ()) {
                return null;
            }
            switch (flip) {
                case SpellRecipe.FLIP_X -> tx = targetSizeX() - 1 - tx;
                case SpellRecipe.FLIP_Z -> tz = targetSizeZ() - 1 - tz;
                case SpellRecipe.UPSIDE_DOWN -> ty = sizeY - 1 - ty;
                default -> {
                }
            }
            return switch (turn) {
                case 1 -> new BlockPos(tz, ty, sizeZ - 1 - tx);
                case 2 -> new BlockPos(sizeX - 1 - tx, ty, sizeZ - 1 - tz);
                case 3 -> new BlockPos(sizeX - 1 - tz, ty, tx);
                default -> new BlockPos(tx, ty, tz);
            };
        }

        /**
         * The block as it stands in the target: turned, then flipped. (A pocket's cell has no level and no position, so
         * vanilla's {@code rotate(Rotation)} is used, as the client's ghost must too.)
         */
        @SuppressWarnings("deprecation")
        public BlockState state(BlockState state) {
            BlockState out = turn == 0 ? state : state.rotate(rotation());
            return switch (flip) {
                case SpellRecipe.FLIP_X -> out.mirror(Mirror.FRONT_BACK);
                case SpellRecipe.FLIP_Z -> out.mirror(Mirror.LEFT_RIGHT);
                case SpellRecipe.UPSIDE_DOWN -> upsideDown(out);
                default -> out;
            };
        }
    }

    /**
     * The block upside down: tops and bottoms swap ({@code half} of stairs, trapdoors and doors, a slab's
     * {@code type}), {@code facing} up and down, floor and ceiling (buttons, levers, bells), a lantern hangs or stands,
     * and the {@code up} and {@code down} sides of a block that has both swap. Twice gives the block back.
     */
    public static BlockState upsideDown(BlockState state) {
        BlockState out = state;
        for (Property<?> property : state.getProperties()) {
            out = flipped(out, property);
        }
        if (state.hasProperty(BlockStateProperties.UP) && state.hasProperty(BlockStateProperties.DOWN)) {
            out = out.setValue(BlockStateProperties.UP, state.getValue(BlockStateProperties.DOWN))
                    .setValue(BlockStateProperties.DOWN, state.getValue(BlockStateProperties.UP));
        }
        if (state.hasProperty(BlockStateProperties.HANGING)) {
            out = out.cycle(BlockStateProperties.HANGING);
        }
        return out;
    }

    private static <T extends Comparable<T>> BlockState flipped(BlockState state, Property<T> property) {
        Object value = state.getValue(property);
        Object other = switch (value) {
            case Half half -> half == Half.TOP ? Half.BOTTOM : Half.TOP;
            case DoubleBlockHalf half -> half == DoubleBlockHalf.UPPER ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER;
            case SlabType type -> type == SlabType.TOP ? SlabType.BOTTOM : type == SlabType.BOTTOM ? SlabType.TOP : type;
            case AttachFace face -> face == AttachFace.FLOOR ? AttachFace.CEILING : face == AttachFace.CEILING ? AttachFace.FLOOR : face;
            case BellAttachType bell -> bell == BellAttachType.FLOOR ? BellAttachType.CEILING
                    : bell == BellAttachType.CEILING ? BellAttachType.FLOOR : bell;
            case Direction direction -> direction.getAxis() == Direction.Axis.Y ? direction.getOpposite() : direction;
            default -> value;
        };
        if (other != value && property.getValueClass().isInstance(other) && property.getPossibleValues().contains(other)) {
            return state.setValue(property, property.getValueClass().cast(other));
        }
        return state;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Where a ghost stands

    /**
     * The low corner of a box of {@code sizeX x sizeY x sizeZ} cells set on the face {@code face} of the block
     * {@code looked} (D-B 4.7, 4.8), like a block about to be placed: on a floor it stands on the looked block,
     * centred; against a wall its near side touches the wall and its bottom is level with the looked block; under a
     * ceiling it hangs, its top just below; with no face (the floating aim cell in the air) its bottom is the looked
     * cell, centred. {@code lift} moves it that many blocks off the surface (up, for no face; below 0 into it).
     */
    public static BlockPos stand(int sizeX, int sizeY, int sizeZ, BlockPos looked, @Nullable Direction face, int lift) {
        int x = looked.getX() - (sizeX - 1) / 2;
        int y = looked.getY();
        int z = looked.getZ() - (sizeZ - 1) / 2;
        if (face == null) {
            return new BlockPos(x, y + lift, z);
        }
        return switch (face) {
            case UP -> new BlockPos(x, y + 1 + lift, z);
            case DOWN -> new BlockPos(x, y - sizeY - lift, z);
            case EAST -> new BlockPos(looked.getX() + 1 + lift, y, z);
            case WEST -> new BlockPos(looked.getX() - sizeX - lift, y, z);
            case SOUTH -> new BlockPos(x, y, looked.getZ() + 1 + lift);
            case NORTH -> new BlockPos(x, y, looked.getZ() - sizeZ - lift);
        };
    }

    // ------------------------------------------------------------------------------------------------------------
    // The mix

    /** A 64-bit mix of a seed and a cell (SplitMix64): the same on every machine. */
    public static long hash(long seed, int x, int y, int z) {
        long h = mix64(seed ^ 0x9E3779B97F4A7C15L);
        h = mix64(h + x);
        h = mix64(h + y * 0x632BE59BD9B4E019L);
        return mix64(h + z * 0xC2B2AE3D27D4EB4FL);
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /**
     * The hotbar's mix (D-B 5.3): every cell gets one of the blocks, weighted by its stack size (32 stone and 8
     * cobblestone are 80/20), chosen by {@link #hash} of the cell and the cast's seed, so the ghost shows exactly the
     * mix that will be built. A mix depends only on which blocks it has and their weights, never on the order or the
     * stacks they came in: equal blocks are merged (their weights added) and the parts kept in the order of their
     * block state's id ({@link Block#getId}, which the server and every client share, as chunks travel with it). The
     * server and a client make it the same way with {@link #ofHotbar}.
     *
     * @param blocks the blocks with their weights, at least one with a weight above 0 (the others are left out)
     */
    public record Mix(List<Part> blocks) {
        /** One block of the mix and its weight (its stack size). */
        public record Part(BlockState state, int weight) {
        }

        public Mix {
            Map<BlockState, Integer> weights = new HashMap<>();
            for (Part part : blocks) {
                if (part.weight > 0) {
                    weights.merge(part.state, part.weight, Integer::sum);
                }
            }
            if (weights.isEmpty()) {
                throw new IllegalArgumentException("a mix needs a block");
            }
            blocks = weights.entrySet().stream().map(entry -> new Part(entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparingInt(part -> Block.getId(part.state))).toList();
        }

        /**
         * The mix of a hotbar, the one way the server (at the cast) and a client (for its ghost) make it: every block
         * item of {@code slots} weighted by its stack size, its block as {@code state} gives it (the state it would be
         * placed in; null leaves the item out); empty slots and items that are not blocks are left out, and the item's
         * data is never looked at. Null when no block is left.
         */
        public static @Nullable Mix ofHotbar(List<ItemStack> slots, Function<ItemStack, BlockState> state) {
            List<Part> parts = new ArrayList<>();
            for (ItemStack stack : slots) {
                if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
                    continue;
                }
                BlockState block = state.apply(stack);
                if (block != null) {
                    parts.add(new Part(block, stack.getCount()));
                }
            }
            return parts.isEmpty() ? null : new Mix(parts);
        }

        /** The block of the cell {@code x, y, z} for this seed. */
        public BlockState pick(int x, int y, int z, long seed) {
            long total = 0;
            for (Part part : blocks) {
                total += part.weight;
            }
            long roll = Long.remainderUnsigned(hash(seed, x, y, z), total);
            for (Part part : blocks) {
                roll -= part.weight;
                if (roll < 0) {
                    return part.state;
                }
            }
            return blocks.getLast().state;
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The family rule

    /**
     * What Change and Paint make of {@code old} with {@code material} in hand (D-B 4.4), or null to leave it as it is.
     * Every block belongs to a family of vanilla's {@link BlockFamilies} (oak planks with their stairs, slab, fence,
     * door ...), or is a family of its own. A block that is a family's variant (stairs, a slab, a wall, a door ...)
     * becomes the same variant of the material's family, and is left alone when that family has none: oak stairs
     * become spruce stairs, and stay oak stairs with glass in hand. A family's base, or a block of no family, becomes
     * the base of the material's family (holding spruce planks or spruce stairs alike gives spruce planks). The new
     * block keeps every property of {@code old} it has by name ({@code facing}, {@code half}, {@code type},
     * {@code waterlogged} ...), on top of the material's own state when it is the material's block.
     */
    public static @Nullable BlockState swap(BlockState old, BlockState material) {
        Block block = familyMember(old.getBlock(), material.getBlock());
        if (block == null) {
            return null;
        }
        return withPropertiesOf(block == material.getBlock() ? material : block.defaultBlockState(), old);
    }

    private static @Nullable Block familyMember(Block from, Block material) {
        Slot at = Families.SLOTS.get(from);
        Slot with = Families.SLOTS.get(material);
        if (at != null && at.variant != null) {
            return with == null ? null : with.family.getVariants().get(at.variant);
        }
        return with == null ? material : with.family.getBaseBlock();
    }

    /** {@code state} with every property of {@code from} it has by name, where the value is one it allows. */
    public static BlockState withPropertiesOf(BlockState state, BlockState from) {
        BlockState out = state;
        for (Property<?> property : state.getProperties()) {
            Property<?> source = from.getBlock().getStateDefinition().getProperty(property.getName());
            if (source != null) {
                out = copied(out, property, from, source);
            }
        }
        return out;
    }

    private static <T extends Comparable<T>, S extends Comparable<S>> BlockState copied(BlockState into, Property<T> property, BlockState from,
                                                                                       Property<S> source) {
        Optional<T> value = property.getValue(source.getName(from.getValue(source)));
        return value.isPresent() ? into.setValue(property, value.get()) : into;
    }

    /** A block's place in vanilla's families: its family, and its variant there (null for the base). */
    private record Slot(BlockFamily family, @Nullable BlockFamily.Variant variant) {
    }

    /** Vanilla's families, mapped once at first use, in the order of their base block's id (the same on every side). */
    private static final class Families {
        static final Map<Block, Slot> SLOTS = build();

        private static Map<Block, Slot> build() {
            List<BlockFamily> families = new ArrayList<>(BlockFamilies.getAllFamilies().toList());
            families.sort(Comparator.comparing(family -> BuiltInRegistries.BLOCK.getKey(family.getBaseBlock()).toString()));
            Map<Block, Slot> slots = new HashMap<>();
            // A block that is some family's base is a base, even where another family lists it as a variant
            // ("polished").
            for (BlockFamily family : families) {
                slots.putIfAbsent(family.getBaseBlock(), new Slot(family, null));
            }
            for (BlockFamily family : families) {
                family.getVariants().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> slots.putIfAbsent(entry.getValue(), new Slot(family, entry.getKey())));
            }
            return Map.copyOf(slots);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // One cell's rules

    /** Whether a cell is open space to the spells: air, or a fluid on its own (water, lava). */
    public static boolean isOpen(BlockState state) {
        return state.isAir() || state.getBlock() instanceof LiquidBlock;
    }

    /** Whether Only the empty and Pull fill the cell: air, or something a placed block replaces (grass, water). */
    public static boolean isEmpty(BlockState state) {
        return state.isAir() || state.canBeReplaced();
    }

    /**
     * What a block leaves when it vanishes (D-B 4.3): its water when it held some (a waterlogged block, kelp,
     * seagrass), as tearing a block out does; else air. Water and lava themselves vanish to air.
     */
    public static BlockState vanished(BlockState old) {
        if (old.isAir() || old.getBlock() instanceof LiquidBlock) {
            return AIR;
        }
        return old.getFluidState().createLegacyBlock();
    }

    /** The material as it goes into a cell holding {@code old}: waterlogged exactly when the cell holds still water. */
    public static BlockState placedInto(BlockState material, BlockState old) {
        if (material.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return material.setValue(BlockStateProperties.WATERLOGGED, old.getFluidState().is(Fluids.WATER));
        }
        return material;
    }

    /** Whether Change and Paint may turn the block into another: never air or a fluid (Change never fills). */
    public static boolean canChange(BlockState state) {
        return !isOpen(state);
    }

    /**
     * Whether the cell touches open space ({@link #isOpen}) on one of its six faces: the skin of a hill or a house,
     * what Change's Only the outside and Paint change. A neighbour in a chunk that is not loaded counts as closed.
     */
    public static boolean touchesOpen(BlockGetter level, BlockPos pos) {
        BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            next.setWithOffset(pos, direction);
            if (loaded(level, next) && isOpen(level.getBlockState(next))) {
                return true;
            }
        }
        return false;
    }

    /**
     * The cells of a world a spell may change (D-B 10.3 P2): inside its build height and, for a level, inside its world
     * border (a cell whose corner lies inside, as {@link WorldBorder#isWithinBounds(BlockPos)} reads it). A getter that
     * is not a level ends where every world ends, {@link Level#MAX_LEVEL_SIZE} blocks from its middle.
     */
    public static BoundingBox world(BlockGetter level) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;
        if (!(level instanceof LevelReader reader)) {
            return new BoundingBox(-Level.MAX_LEVEL_SIZE, minY, -Level.MAX_LEVEL_SIZE, Level.MAX_LEVEL_SIZE - 1, maxY, Level.MAX_LEVEL_SIZE - 1);
        }
        WorldBorder border = reader.getWorldBorder();
        int minX = Mth.ceil(border.getMinX());
        int minZ = Mth.ceil(border.getMinZ());
        int maxX = Math.max(minX, Mth.ceil(border.getMaxX()) - 1);
        int maxZ = Math.max(minZ, Mth.ceil(border.getMaxZ()) - 1);
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Whether the cell is outside the build height or (for a level) the world border, where no spell ever writes
     * (D-B 10.3 P2): above the top a level reads void air, which a count would otherwise take for a cell to fill.
     */
    public static boolean outside(BlockGetter level, BlockPos pos) {
        return level.isOutsideBuildHeight(pos) || level instanceof LevelReader reader && !reader.getWorldBorder().isWithinBounds(pos);
    }

    /** Whether the cell's chunk can be read without loading it (any getter that is not a level always can). */
    @SuppressWarnings("deprecation") // hasChunkAt only asks whether the chunk is there, which is all this wants
    public static boolean loaded(BlockGetter level, BlockPos pos) {
        return !(level instanceof LevelReader reader) || reader.hasChunkAt(pos);
    }

    /**
     * The other cell of a two-cell block (the other half of a door or a tall plant, the other part of a bed), or null
     * for a block of one cell. A spell never cuts one in half (D-B 11.5).
     */
    public static @Nullable BlockPos partner(BlockState state, BlockPos pos) {
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        }
        if (state.hasProperty(BlockStateProperties.BED_PART) && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            return pos.relative(state.getValue(BlockStateProperties.BED_PART) == BedPart.FOOT ? facing : facing.getOpposite());
        }
        return null;
    }

    /** Whether magic never touches the block (D-B 5.7): {@code enhanced:build_untouchable}, or hardness below 0. */
    public static boolean untouchable(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModTags.BUILD_UNTOUCHABLE) || state.getDestroySpeed(level, pos) < 0.0F;
    }

    // ------------------------------------------------------------------------------------------------------------
    // The Pull surface

    /**
     * The face Pull &amp; Push works on (D-B 4.6): the block looked at, and every block of the same block connected to
     * it in the plane of the looked face (four neighbours in that plane) whose neighbour in front of the face is air or
     * replaceable. It is flooded a few reads at a time ({@link #step}), breadth first, so it is the same on both sides
     * for the same world, and it stops at {@code cap} cells (the magic-select cap) and inside a window of {@code span}
     * blocks round the looked block (Billy's side). A cell in a chunk that is not loaded never joins.
     */
    public static final class PullSurface {
        private final BlockGetter level;
        private final Direction face;
        private final int cap;
        private final BlockPos low;
        private final BlockPos high;
        private final Block block;
        private final LongSet seen = new LongOpenHashSet();
        private final LongSet cells = new LongOpenHashSet();
        private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        private boolean done;

        public PullSurface(BlockGetter level, BlockPos anchor, Direction face, int cap, int span) {
            this.level = level;
            this.face = face;
            this.cap = Math.max(1, cap);
            int below = span <= 0 ? Integer.MAX_VALUE / 2 : (span - 1) / 2;
            int above = span <= 0 ? Integer.MAX_VALUE / 2 : span / 2;
            Direction.Axis axis = face.getAxis();
            this.low = new BlockPos(axis == Direction.Axis.X ? anchor.getX() : anchor.getX() - below,
                    axis == Direction.Axis.Y ? anchor.getY() : anchor.getY() - below, axis == Direction.Axis.Z ? anchor.getZ() : anchor.getZ() - below);
            this.high = new BlockPos(axis == Direction.Axis.X ? anchor.getX() : anchor.getX() + above,
                    axis == Direction.Axis.Y ? anchor.getY() : anchor.getY() + above, axis == Direction.Axis.Z ? anchor.getZ() : anchor.getZ() + above);
            BlockState state = loaded(level, anchor) ? level.getBlockState(anchor) : AIR;
            this.block = state.getBlock();
            if (isOpen(state)) {
                done = true;
            } else {
                long start = anchor.asLong();
                seen.add(start);
                cells.add(start);
                queue.enqueue(start);
                done = cells.size() >= this.cap;
            }
        }

        /** Floods on for about {@code reads} block reads; true once the surface is complete. */
        public boolean step(int reads) {
            BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
            BlockPos.MutableBlockPos front = new BlockPos.MutableBlockPos();
            int left = reads;
            while (!done && left > 0) {
                if (queue.isEmpty()) {
                    done = true;
                    break;
                }
                long at = queue.dequeueLong();
                for (Direction direction : Direction.values()) {
                    if (direction.getAxis() == face.getAxis()) {
                        continue;
                    }
                    next.set(at).move(direction);
                    if (!inWindow(next) || !seen.add(next.asLong()) || !loaded(level, next)) {
                        continue;
                    }
                    left--;
                    if (!level.getBlockState(next).is(block)) {
                        continue;
                    }
                    front.setWithOffset(next, face);
                    left--;
                    if (loaded(level, front) && isEmpty(level.getBlockState(front))) {
                        cells.add(next.asLong());
                        queue.enqueue(next.asLong());
                        if (cells.size() >= cap) {
                            done = true;
                            break;
                        }
                    }
                }
            }
            return done;
        }

        public boolean done() {
            return done;
        }

        /** The face looked at. */
        public Direction face() {
            return face;
        }

        /** How many cells the surface has so far. */
        public int size() {
            return cells.size();
        }

        /** The surface's cells (complete once {@link #done}). */
        public Volume cells() {
            return SpellMath.cells(cells);
        }

        private boolean inWindow(BlockPos pos) {
            return pos.getX() >= low.getX() && pos.getX() <= high.getX() && pos.getY() >= low.getY() && pos.getY() <= high.getY()
                    && pos.getZ() >= low.getZ() && pos.getZ() <= high.getZ();
        }
    }

    private SpellMath() {
    }
}
