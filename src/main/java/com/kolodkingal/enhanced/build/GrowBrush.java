package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.build.LandBrush.Column;
import com.kolodkingal.enhanced.registry.ModTags;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Grow brush (D-B 4.11, phase 2): a pulse every 4 ticks while the right button is held, inside the sphere of the
 * brush, in one of three styles ({@link GrowStyle}). A pulse is planned against the world as it is (nothing cascades
 * inside one pulse: a rock swells one layer a pulse) into one {@link Column} for each cell or plant it changes, which the
 * job writes whole or not at all; the same pure code plans the preview and the pulse.
 *
 * <ul>
 *     <li><b>Swell</b>: every air or replaceable cell of the sphere that touches a solid block of the sphere becomes that
 *     block (the most common solid face neighbour; ties go to the one below).</li>
 *     <li><b>Shrink</b>: every solid cell of the sphere that touches air on two or more faces vanishes.</li>
 *     <li><b>Bloom</b> (Westview's garden): on the top surfaces of the sphere dirt, coarse dirt and rooted dirt become
 *     grass; on grass and moss a quarter of the cells sprout short grass, one in eight a flower, one in thirty-two a tall
 *     flower; netherrack becomes crimson nylium (warped in a warped forest) with roots; mycelium sprouts mushrooms.
 *     Each cell's choice is a hash of the stroke's seed and its position ({@link SpellMath#hash}), so the server grows
 *     what the client showed.</li>
 * </ul>
 *
 * <p>Solid means a full block without a block entity that magic may touch; a block entity is never swelled from or
 * shrunk. Cells in chunks that are not loaded are never read and count as closed.
 */
public final class GrowBrush {
    /** Ticks between two pulses (as Land's). */
    public static final int PULSE_TICKS = LandBrush.PULSE_TICKS;

    private static final Map<Integer, int[]> BALLS = new ConcurrentHashMap<>();
    private static @Nullable List<Block> smallFlowers;
    private static @Nullable List<Block> tallFlowers;

    private GrowBrush() {
    }

    /** The cells of the brush's sphere (offsets {@code dx, dy, dz}) as one number each, nearest the middle first. */
    static int[] ball(int radius) {
        return BALLS.computeIfAbsent(radius, r -> {
            int limit = limit(r);
            List<int[]> cells = new ArrayList<>();
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    for (int dx = -r; dx <= r; dx++) {
                        if (dx * dx + dy * dy + dz * dz <= limit) {
                            cells.add(new int[]{dx, dy, dz});
                        }
                    }
                }
            }
            cells.sort(Comparator.<int[]>comparingInt(c -> c[0] * c[0] + c[1] * c[1] + c[2] * c[2]).thenComparingInt(c -> c[1]).thenComparingInt(c -> c[2])
                    .thenComparingInt(c -> c[0]));
            int[] packed = new int[cells.size()];
            for (int i = 0; i < packed.length; i++) {
                int[] c = cells.get(i);
                packed[i] = c[0] + 64 | c[1] + 64 << 8 | c[2] + 64 << 16;
            }
            return packed;
        });
    }

    /** The squared distance a cell of a sphere of this radius may be at (the same sphere as Paint's dab). */
    static int limit(int radius) {
        return radius * radius + radius;
    }

    /** Whether the offset lies inside the sphere of this radius. */
    public static boolean inSphere(int dx, int dy, int dz, int radius) {
        return dx * dx + dy * dy + dz * dz <= limit(Math.max(0, radius));
    }

    /** The box a pulse may read or write: the sphere, one more for what it touches, and two above for a tall flower. */
    public static BoundingBox reach(BlockPos middle, int radius, GrowStyle style) {
        int r = Math.max(0, radius) + 1;
        return new BoundingBox(middle.getX() - r, middle.getY() - r, middle.getZ() - r, middle.getX() + r, middle.getY() + r + 2, middle.getZ() + r);
    }

    /** Whether the block is one Grow swells from and shrinks: a full block without a block entity that magic may touch. */
    static boolean solid(BlockGetter level, BlockState state, BlockPos pos) {
        return !SpellMath.isEmpty(state) && !SpellMath.isOpen(state) && !state.hasBlockEntity() && !SpellMath.untouchable(state, level, pos)
                && !state.is(ModTags.BUILD_UNCONJURABLE) && Block.isShapeFullBlock(state.getCollisionShape(level, pos));
    }

    private static List<Block> tag(TagKey<Block> tag) {
        List<Block> blocks = new ArrayList<>();
        BuiltInRegistries.BLOCK.getTag(tag).ifPresent(set -> {
            for (Holder<Block> holder : set) {
                blocks.add(holder.value());
            }
        });
        blocks.sort(Comparator.comparing(block -> BuiltInRegistries.BLOCK.getKey(block).toString()));
        return blocks;
    }

    private static synchronized List<Block> smallFlowers() {
        if (smallFlowers == null) {
            smallFlowers = tag(BlockTags.SMALL_FLOWERS);
        }
        return smallFlowers;
    }

    private static synchronized List<Block> tallFlowers() {
        if (tallFlowers == null) {
            tallFlowers = tag(BlockTags.TALL_FLOWERS);
        }
        return tallFlowers;
    }

    /**
     * One pulse at {@code middle} with the brush of {@code radius}: planned a few reads at a time ({@link #step}), its
     * {@link Column}s the cells (a tall flower: two) it changes, in the order of the sphere (nearest the middle first).
     * {@code seed} is the stroke's, for Bloom's choices.
     */
    public static final class Pulse implements BrushPulse {
        private final BlockGetter level;
        private final GrowStyle style;
        private final BlockPos middle;
        private final int radius;
        private final long seed;
        private final int[] ball;
        private final Long2ObjectOpenHashMap<BlockState> cache = new Long2ObjectOpenHashMap<>();
        private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        private final BlockPos.MutableBlockPos near = new BlockPos.MutableBlockPos();
        private long reads;
        private int planned;
        private final List<Column> columns = new ArrayList<>();
        private long changes;
        private int minX = Integer.MAX_VALUE;
        private int minY = Integer.MAX_VALUE;
        private int minZ = Integer.MAX_VALUE;
        private int maxX = Integer.MIN_VALUE;
        private int maxY = Integer.MIN_VALUE;
        private int maxZ = Integer.MIN_VALUE;

        public Pulse(BlockGetter level, GrowStyle style, BlockPos middle, int radius, long seed) {
            this.level = level;
            this.style = style;
            this.middle = middle.immutable();
            this.radius = Math.max(0, Math.min(radius, 32));
            this.seed = seed;
            this.ball = ball(this.radius);
        }

        @Override
        public boolean step(int reads) {
            long until = this.reads + Math.max(1, reads);
            while (planned < ball.length && this.reads < until) {
                int packed = ball[planned++];
                plan(middle.getX() + (packed & 0xFF) - 64, middle.getY() + (packed >> 8 & 0xFF) - 64, middle.getZ() + (packed >> 16 & 0xFF) - 64);
            }
            return done();
        }

        @Override
        public boolean done() {
            return planned >= ball.length;
        }

        @Override
        public long reads() {
            return reads;
        }

        @Override
        public List<Column> columns() {
            return Collections.unmodifiableList(columns);
        }

        @Override
        public long changes() {
            return changes;
        }

        @Override
        public long cells() {
            long n = 0;
            for (Column column : columns) {
                n += column.size();
            }
            return n;
        }

        @Override
        public @Nullable BoundingBox bounds() {
            return columns.isEmpty() ? null : new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
        }

        public GrowStyle style() {
            return style;
        }

        /** What the cell holds; null in a chunk that is not loaded, which is never read. */
        private @Nullable BlockState read(int x, int y, int z) {
            long key = BlockPos.asLong(x, y, z);
            BlockState state = cache.get(key);
            if (state == null && !cache.containsKey(key)) {
                reads++;
                pos.set(x, y, z);
                state = level.isOutsideBuildHeight(y) || !SpellMath.loaded(level, pos) ? null : level.getBlockState(pos);
                cache.put(key, state);
            }
            return state;
        }

        private boolean empty(@Nullable BlockState state) {
            return state != null && SpellMath.isEmpty(state);
        }

        private boolean solid(@Nullable BlockState state, int x, int y, int z) {
            return state != null && GrowBrush.solid(level, state, near.set(x, y, z));
        }

        private void plan(int x, int y, int z) {
            BlockState state = read(x, y, z);
            if (state == null) {
                return;
            }
            switch (style) {
                case SWELL -> swell(x, y, z, state);
                case SHRINK -> shrink(x, y, z, state);
                case BLOOM -> bloom(x, y, z, state);
            }
        }

        private void swell(int x, int y, int z, BlockState state) {
            if (!SpellMath.isEmpty(state) || state.hasBlockEntity()) {
                return;
            }
            BlockState best = null;
            int bestCount = 0;
            BlockState[] found = new BlockState[6];
            int[] counts = new int[6];
            int n = 0;
            Direction[] order = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
            for (Direction direction : order) {
                int nx = x + direction.getStepX();
                int ny = y + direction.getStepY();
                int nz = z + direction.getStepZ();
                if (!inSphere(nx - middle.getX(), ny - middle.getY(), nz - middle.getZ(), radius)) {
                    continue;
                }
                BlockState neighbour = read(nx, ny, nz);
                if (!solid(neighbour, nx, ny, nz)) {
                    continue;
                }
                BlockState block = neighbour.getBlock().defaultBlockState();
                int at = -1;
                for (int i = 0; i < n; i++) {
                    if (found[i].equals(block)) {
                        at = i;
                    }
                }
                if (at < 0) {
                    at = n++;
                    found[at] = block;
                }
                counts[at]++;
                if (counts[at] > bestCount) {
                    bestCount = counts[at];
                    best = found[at]; // (the order puts the one below first: it wins a tie)
                }
            }
            if (best != null) {
                add(x, y, z, state, SpellMath.placedInto(best, state));
            }
        }

        private void shrink(int x, int y, int z, BlockState state) {
            if (!solid(state, x, y, z)) {
                return;
            }
            int air = 0;
            for (Direction direction : Direction.values()) {
                if (empty(read(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ()))) {
                    air++;
                }
            }
            if (air >= 2) {
                add(x, y, z, state, SpellMath.vanished(state));
            }
        }

        private void bloom(int x, int y, int z, BlockState state) {
            BlockState above = read(x, y + 1, z);
            if (!empty(above)) {
                return;
            }
            long h = SpellMath.hash(seed, x, y, z);
            int r = (int) (h >>> 1 & 31);
            long other = h >>> 24;
            if (state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)) {
                add(x, y, z, state, Blocks.GRASS_BLOCK.defaultBlockState());
            } else if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MOSS_BLOCK)) {
                if (!above.isAir()) {
                    return;
                }
                if (r == 0) {
                    List<Block> tall = tallFlowers();
                    BlockState over = read(x, y + 2, z);
                    if (!tall.isEmpty() && over != null && over.isAir()) {
                        Block flower = tall.get((int) Long.remainderUnsigned(other, tall.size()));
                        BlockState lower = flower.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
                        BlockState upper = flower.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
                        addColumn(x, y + 1, z, new BlockState[]{above, over}, new BlockState[]{lower, upper});
                    }
                } else if (r <= 4) {
                    List<Block> small = smallFlowers();
                    if (!small.isEmpty()) {
                        sprout(x, y + 1, z, above, small.get((int) Long.remainderUnsigned(other, small.size())).defaultBlockState());
                    }
                } else if (r <= 12) {
                    sprout(x, y + 1, z, above, Blocks.SHORT_GRASS.defaultBlockState());
                }
            } else if (state.is(Blocks.NETHERRACK)) {
                boolean warped = level instanceof LevelReader reader && reader.getBiome(near.set(x, y, z)).is(Biomes.WARPED_FOREST);
                add(x, y, z, state, (warped ? Blocks.WARPED_NYLIUM : Blocks.CRIMSON_NYLIUM).defaultBlockState());
                if (r >= 5 && r <= 12 && above.isAir()) {
                    sprout(x, y + 1, z, above, (warped ? Blocks.WARPED_ROOTS : Blocks.CRIMSON_ROOTS).defaultBlockState());
                }
            } else if (state.is(Blocks.MYCELIUM) && above.isAir() && r >= 5 && r <= 12) {
                sprout(x, y + 1, z, above, ((other & 1) == 0 ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM).defaultBlockState());
            }
        }

        private void sprout(int x, int y, int z, BlockState old, BlockState plant) {
            add(x, y, z, old, plant);
        }

        private void add(int x, int y, int z, BlockState old, BlockState now) {
            if (!old.equals(now)) {
                addColumn(x, y, z, new BlockState[]{old}, new BlockState[]{now});
            }
        }

        private void addColumn(int x, int y, int z, BlockState[] olds, BlockState[] news) {
            Column column = new Column(x, y, z, olds, news, 0);
            columns.add(column);
            changes += column.changes();
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y + olds.length - 1);
            maxZ = Math.max(maxZ, z);
        }
    }

    /** Whether a pulse of {@code style} that planned nothing says "Nothing grows here" (Bloom only). */
    public static boolean saysNothingGrows(GrowStyle style) {
        return style == GrowStyle.BLOOM;
    }
}
