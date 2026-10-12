package com.kolodkingal.enhanced.ultron;

import com.kolodkingal.enhanced.ability.ThorAbilities;
import com.kolodkingal.enhanced.block.MjolnirBlock;
import com.kolodkingal.enhanced.config.UltronBossConfig;
import com.kolodkingal.enhanced.ultron.SokoviaShape.Spec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.function.BiPredicate;

/**
 * One batch of the lift's block edits, done a slice at a time: the island is placed in the sky ({@link #paste}) or
 * taken down again ({@link #clear}), at most {@code editsPerTick} blocks a tick (half of that while the server is
 * struggling to keep its tick), inside the level tick, so a slice never races a chunk save of its own tick.
 *
 * <ul>
 *     <li>Every edit is {@code setBlock} with {@code UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE | UPDATE_SUPPRESS_DROPS}: the
 *     clients are told, no neighbour is bothered and nothing is dropped. The island has no fluid, no block entity and
 *     nothing redstone, so a block placed this way has nothing to do.</li>
 *     <li>Placing goes bottom-up, so nothing is ever unsupported. Taking down goes top-down.</li>
 *     <li>The job never touches a chunk that is not loaded: a slice that meets one stops there and waits for the next
 *     tick.</li>
 *     <li>Taking down clears <em>every</em> block inside the island's shape and the heights it spans, whatever it is,
 *     but what is not the island's own (a block a hero's ability put there, which the arena's protection cannot stop)
 *     is first turned into items: Mjolnir lying as a block is lifted with its owner, anything else drops what it
 *     would drop. Nothing of the players' is lost; the items fall with the island.</li>
 * </ul>
 */
public final class SokoviaJob {
    /** Clients told, neighbours left alone, nothing dropped: 2 | 16 | 32. */
    public static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    /** A tick longer than this (nanoseconds) on average makes the slices half as big. */
    public static final long SLOW_TICK_NANOS = 45_000_000L;

    private final ServerLevel level;
    private final boolean pasting;
    private final BiPredicate<Integer, Integer> loaded;

    // Pasting.
    private final SokoviaTemplate template;
    private final int lift;

    // Clearing.
    private final int[] columnX;
    private final int[] columnZ;
    private final int topY;
    private final int bottomY;

    private int cursor;
    private int edits;
    private int mostInATick;
    private int ticks;
    private boolean waited;
    private boolean done;
    private boolean force;

    private SokoviaJob(ServerLevel level, boolean pasting, SokoviaTemplate template, int lift, int[] columnX, int[] columnZ, int topY, int bottomY,
                       BiPredicate<Integer, Integer> loaded) {
        this.level = level;
        this.pasting = pasting;
        this.template = template;
        this.lift = lift;
        this.columnX = columnX;
        this.columnZ = columnZ;
        this.topY = topY;
        this.bottomY = bottomY;
        this.loaded = loaded;
    }

    /** Places the island {@code lift} blocks above the ground it was copied from. */
    public static SokoviaJob paste(ServerLevel level, SokoviaTemplate template, int lift) {
        return paste(level, template, lift, level::hasChunk);
    }

    /** The same, for a world whose idea of a loaded chunk is {@code loaded} (the tests unload chunks this way). */
    public static SokoviaJob paste(ServerLevel level, SokoviaTemplate template, int lift, BiPredicate<Integer, Integer> loaded) {
        return new SokoviaJob(level, true, template, lift, new int[0], new int[0], 0, 0, loaded);
    }

    /**
     * Clears the island in the sky: every block in the columns of its shape ({@code spec}, round {@code centre}), from
     * {@code topY} down to {@code bottomY}.
     */
    public static SokoviaJob clear(ServerLevel level, Spec spec, BlockPos centre, int bottomY, int topY) {
        return clear(level, spec, centre, bottomY, topY, level::hasChunk);
    }

    public static SokoviaJob clear(ServerLevel level, Spec spec, BlockPos centre, int bottomY, int topY, BiPredicate<Integer, Integer> loaded) {
        int extent = spec.extent();
        int[] xs = new int[(extent * 2 + 1) * (extent * 2 + 1)];
        int[] zs = new int[xs.length];
        int n = 0;
        for (int dz = -extent; dz <= extent; dz++) {
            for (int dx = -extent; dx <= extent; dx++) {
                if (SokoviaShape.inside(spec, dx, dz)) {
                    xs[n] = centre.getX() + dx;
                    zs[n] = centre.getZ() + dz;
                    n++;
                }
            }
        }
        int low = Math.max(bottomY, level.getMinBuildHeight());
        int high = Math.min(topY, level.getMaxBuildHeight() - 1);
        return new SokoviaJob(level, false, null, 0, Arrays.copyOf(xs, n), Arrays.copyOf(zs, n), high, low, loaded);
    }

    // ---- running ----

    /** How many edits a tick may take now: {@code editsPerTick}, halved while the server is struggling. */
    public static int budget(ServerLevel level) {
        int budget = UltronBossConfig.EDITS_PER_TICK.get();
        return level.getServer().getAverageTickTimeNanos() > SLOW_TICK_NANOS ? Math.max(50, budget / 2) : budget;
    }

    /** Does this tick's slice. */
    public boolean step() {
        return step(budget(level));
    }

    /**
     * Does a slice of at most {@code budget} edits.
     *
     * @return true when the job is done
     */
    public boolean step(int budget) {
        if (done) {
            return true;
        }
        ticks++;
        waited = false;
        int before = edits;
        done = pasting ? pasteSlice(budget) : clearSlice(budget);
        mostInATick = Math.max(mostInATick, edits - before);
        return done;
    }

    private boolean pasteSlice(int budget) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int made = 0;
        while (cursor < template.size() && made < budget) {
            int x = template.x(cursor);
            int z = template.z(cursor);
            if (!chunkReady(x >> 4, z >> 4)) {
                waited = true;
                return false;
            }
            pos.set(x, template.y(cursor) + lift, z);
            BlockState state = template.state(cursor);
            if (!level.isOutsideBuildHeight(pos) && level.getBlockState(pos) != state) {
                level.setBlock(pos, state, FLAGS);
                made++;
                edits++;
            }
            cursor++;
        }
        return cursor >= template.size();
    }

    private boolean clearSlice(int budget) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int columns = columnX.length;
        if (columns == 0 || topY < bottomY) {
            return true;
        }
        long total = (long) (topY - bottomY + 1) * columns;
        int made = 0;
        while (cursor < total && made < budget) {
            int layer = cursor / columns;
            int column = cursor % columns;
            int x = columnX[column];
            int z = columnZ[column];
            if (!chunkReady(x >> 4, z >> 4)) {
                waited = true;
                return false;
            }
            pos.set(x, topY - layer, z);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                if (!isTheIslands(state)) {
                    dropWhatIsNotTheIslands(state, pos);
                }
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
                made++;
                edits++;
            }
            cursor++;
        }
        return cursor >= total;
    }

    /** Whether the block is of the island (carried from the ground, or the cone's rock). */
    private static boolean isTheIslands(BlockState state) {
        return SokoviaTemplate.carried(state) || SokoviaShape.isConeRock(state);
    }

    /**
     * A block that is not the island's, in the way of the clearing: Mjolnir is lifted with its owner, the rest drops.
     */
    private void dropWhatIsNotTheIslands(BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof MjolnirBlock) {
            ItemStack hammer = MjolnirBlock.lift(level, pos);
            if (!hammer.isEmpty()) {
                ThorAbilities.dropAt(level, Vec3.atCenterOf(pos), hammer);
            }
        } else {
            Block.dropResources(state, level, pos, level.getBlockEntity(pos));
        }
    }

    // ---- what it did ----

    public boolean isDone() {
        return done;
    }

    /** Whether the last slice stopped at a chunk that was not loaded. */
    public boolean isWaiting() {
        return waited;
    }

    /** How many blocks were changed so far. */
    public int edits() {
        return edits;
    }

    /** The most blocks changed in one slice. */
    public int mostInATick() {
        return mostInATick;
    }

    /** How many slices were run. */
    public int ticks() {
        return ticks;
    }

    /** Whether the job places the island (false: it clears it). */
    public boolean isPasting() {
        return pasting;
    }

    /**
     * Does what is left of the job at once (the server is stopping, or an operator cleans up): a chunk that is not
     * loaded is loaded for it, because the world is closing and the work is worth the wait.
     */
    public void finishNow() {
        force = true;
        step(Integer.MAX_VALUE);
    }

    /** Whether the chunk can be edited now (when the job is forced, it is loaded first). */
    private boolean chunkReady(int chunkX, int chunkZ) {
        if (force) {
            level.getChunk(chunkX, chunkZ);
            return true;
        }
        return loaded.test(chunkX, chunkZ);
    }
}
