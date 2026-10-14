package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.network.BuildVerdictPayload;
import com.kolodkingal.enhanced.registry.ModTags;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Magic select (D-B 3.4): a double click takes the whole connected thing, as Axiom's Magic Select does, with nothing to
 * set. The same flood on both sides: the client floods it to draw its glow (some 20,000 reads a client tick), and the server
 * floods it again from the same seeds in the job's SELECT phase when a spell is cast, so the spell takes exactly the
 * cells the client showed whenever the two worlds agree.
 *
 * <ul>
 *     <li>From a <b>built block</b> (not in {@code enhanced:build_ground}): every block that is not empty
 *     ({@link SpellMath#isEmpty}: air, a fluid, or a plant a placed block replaces, such as grass tufts) and not
 *     ground, connected through any of its <b>26</b> neighbours, so a tree comes whole, leaves touching only at their
 *     corners included. A house on a meadow stops at the meadow; a tree stops at its dirt.</li>
 *     <li>From a <b>ground block</b>: the cells of that same block connected through their <b>6</b> faces: a sand dune,
 *     a granite boulder in stone.</li>
 *     <li>Up to {@link SpellRecipe#MAX_SEEDS} seeds: the union of their things (a seed already selected adds
 *     nothing).</li>
 * </ul>
 *
 * <p>A thing is never selected in part, which would puzzle a child: past the cap (Wanda's
 * {@code build_magic_wanda.magicSelectMax}, Billy's meter), wider than Billy's side, reaching beyond the hero's reach
 * from his eyes, or with a bounding box of more than {@link #MAX_BOUNDS} cells, <b>nothing</b> is selected, and
 * {@link #refusal} says why ({@code TOO_BIG_TO_SELECT}, {@code TOO_WIDE}, {@code TOO_FAR}). Only a chunk that is not
 * loaded cuts a thing: the flood never reads one (for a client or the server the world ends there).
 *
 * <p>The flood is breadth first from each seed in turn, its neighbours in a fixed order, so the same world gives the
 * same cells and the same reason. It runs a few reads at a time ({@link #step}); a cell read but not taken is not
 * remembered (it is read again when another cell touches it), so it holds no more than the cells it selects.
 */
public final class MagicSelect {
    /** The biggest bounding box of a selection (D-B 10.2 C7): the planner walks a selection's box row by row. */
    public static final long MAX_BOUNDS = 16_777_216L;

    /** The 26 neighbours of a cell, faces first. */
    private static final int[][] AROUND = around();
    /** The 6 face neighbours (the first six of {@link #AROUND}). */
    private static final int FACES = 6;

    private final BlockGetter level;
    private final List<BlockPos> seeds;
    private final int cap;
    private final int side;
    private final @Nullable Reach reach;
    private final LongOpenHashSet cells = new LongOpenHashSet();
    private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
    private int next;
    private boolean ground;
    private @Nullable Block block;
    private int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
    private int refusal = SpellPlanner.NONE;
    private int limit;
    private boolean done;

    /**
     * Where the hero's eyes are and how far magic select reaches from them: a cell of the thing farther than
     * {@code blocks + slack} from the eyes (to its centre) refuses the selection. A client floods with no slack; the
     * server, whose player may have moved since the double click, with {@link Limits#REACH_SLACK}.
     *
     * @param eyes   the eyes when the selection is flooded
     * @param blocks the hero's reach, as the refusal names it
     * @param slack  how far beyond it a cell still counts as within reach
     */
    public record Reach(Vec3 eyes, int blocks, double slack) {
        boolean within(int x, int y, int z) {
            double radius = blocks + slack;
            return eyes.distanceToSqr(x + 0.5, y + 0.5, z + 0.5) <= radius * radius;
        }
    }

    /** A selection of at most {@code cap} cells and of any size otherwise (the tests, a pure flood). */
    public MagicSelect(BlockGetter level, Collection<BlockPos> seeds, int cap) {
        this(level, seeds, cap, 0, null);
    }

    /**
     * A selection from {@code seeds} (at most {@link SpellRecipe#MAX_SEEDS}; the first ones are kept), refused past
     * {@code cap} cells, past {@code side} blocks on a side (0: no side limit) and beyond {@code reach} (null:
     * anywhere). {@link Limits#select} makes one with the hero's numbers.
     */
    public MagicSelect(BlockGetter level, Collection<BlockPos> seeds, int cap, int side, @Nullable Reach reach) {
        this.level = level;
        this.seeds = seeds.stream().limit(SpellRecipe.MAX_SEEDS).map(BlockPos::immutable).toList();
        this.cap = Math.max(1, cap);
        this.side = Math.max(0, side);
        this.reach = reach;
    }

    /** The whole selection at once (a test, or a flood small enough to need no slices). */
    public MagicSelect run() {
        while (!step(Integer.MAX_VALUE)) {
            // Each step floods on until it is done.
        }
        return this;
    }

    /**
     * Floods on for about {@code reads} block reads; true once the selection is complete (or refused). A selection
     * with no cell at the end (every seed on air, a fluid or a chunk not loaded) is refused as
     * {@link BuildVerdictPayload#NO_BOX}.
     */
    public boolean step(int reads) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int left = reads;
        while (!done && left > 0) {
            if (queue.isEmpty()) {
                if (!startNext()) {
                    finish();
                    break;
                }
                left--;
                continue;
            }
            long at = queue.dequeueLong();
            int x = BlockPos.getX(at);
            int y = BlockPos.getY(at);
            int z = BlockPos.getZ(at);
            int around = ground ? FACES : AROUND.length;
            for (int i = 0; i < around && !done; i++) {
                int[] d = AROUND[i];
                pos.set(x + d[0], y + d[1], z + d[2]);
                if (cells.contains(pos.asLong()) || !SpellMath.loaded(level, pos)) {
                    continue;
                }
                left--;
                if (joins(level.getBlockState(pos))) {
                    take(pos);
                }
            }
        }
        return done;
    }

    /** Starts the flood of the next seed that is not selected yet; false when no seed is left. */
    private boolean startNext() {
        while (next < seeds.size()) {
            BlockPos seed = seeds.get(next++);
            if (cells.contains(seed.asLong()) || !SpellMath.loaded(level, seed)) {
                continue;
            }
            BlockState state = level.getBlockState(seed);
            if (SpellMath.isEmpty(state)) {
                continue;
            }
            ground = isGround(state);
            block = state.getBlock();
            take(seed);
            return true;
        }
        return false;
    }

    /** Whether a neighbour joins the thing of the seed being flooded. */
    private boolean joins(BlockState state) {
        return ground ? state.is(block) : !SpellMath.isEmpty(state) && !isGround(state);
    }

    /** Selects the cell, unless it breaks a limit (then nothing is selected). */
    private void take(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (reach != null && !reach.within(x, y, z)) {
            refuse(BuildVerdictPayload.TOO_FAR, reach.blocks());
            return;
        }
        long packed = pos.asLong();
        cells.add(packed);
        if (cells.size() > cap) {
            refuse(BuildVerdictPayload.TOO_BIG_TO_SELECT, cap);
            return;
        }
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        minZ = Math.min(minZ, z);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
        maxZ = Math.max(maxZ, z);
        long spanX = (long) maxX - minX + 1;
        long spanY = (long) maxY - minY + 1;
        long spanZ = (long) maxZ - minZ + 1;
        if (side > 0 && (spanX > side || spanY > side || spanZ > side)) {
            refuse(BuildVerdictPayload.TOO_WIDE, side);
            return;
        }
        if (spanX * spanY * spanZ > MAX_BOUNDS) {
            // Its box holds more than that many blocks, however few the thing has: the number said is the box's.
            refuse(BuildVerdictPayload.TOO_BIG_TO_SELECT, (int) MAX_BOUNDS);
            return;
        }
        queue.enqueue(packed);
    }

    private void refuse(int code, int number) {
        refusal = code;
        limit = number;
        cells.clear();
        cells.trim();
        queue.clear();
        done = true;
    }

    private void finish() {
        if (cells.isEmpty()) {
            refuse(BuildVerdictPayload.NO_BOX, 0);
            return;
        }
        done = true;
    }

    /** Whether the selection is complete (or refused). */
    public boolean done() {
        return done;
    }

    /**
     * Why nothing is selected: {@link SpellPlanner#NONE} (the selection holds its thing), or
     * {@link BuildVerdictPayload#TOO_BIG_TO_SELECT}, {@link BuildVerdictPayload#TOO_WIDE},
     * {@link BuildVerdictPayload#TOO_FAR}, {@link BuildVerdictPayload#NO_BOX} (no seed was on a block).
     */
    public int refusal() {
        return refusal;
    }

    /**
     * The number the refusal names: the cap (or {@link #MAX_BOUNDS} for a thing whose box holds more blocks than that),
     * Billy's side, the reach (0 for none).
     */
    public int limit() {
        return limit;
    }

    /** How many cells are selected so far. */
    public int size() {
        return cells.size();
    }

    /**
     * The cells found so far ({@link BlockPos#asLong}), for the client's glow as it spreads: a view that must not be
     * changed, empty once the selection is refused.
     */
    public LongSet found() {
        return LongSets.unmodifiable(cells);
    }

    /** The cells found so far, with the thing's box: the bounds the flood has reached. */
    public @Nullable BoundingBox bounds() {
        return cells.isEmpty() ? null : new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * The selected thing, as the planner's mask ({@link SpellPlanner.Setup#withMask}): its cells once the selection is
     * complete, or null while it floods or when it is refused.
     */
    public @Nullable SpellMath.Volume mask() {
        return done && refusal == SpellPlanner.NONE ? SpellMath.cells(cells) : null;
    }

    /** The seeds it floods from (at most {@link SpellRecipe#MAX_SEEDS}). */
    public List<BlockPos> seeds() {
        return seeds;
    }

    /**
     * What the hero is told when nothing is selected: "Too big to hold in mind (more than %s blocks)", and for the
     * Scarlet Witch (whose two corners hold any size) also "Mark it with two corners instead"; the side and the reach
     * as every other spell says them ({@link Limits#tooWide}, {@link Limits#tooFar}); null when something is selected.
     */
    public @Nullable Component message(boolean wanda) {
        return switch (refusal) {
            case BuildVerdictPayload.TOO_BIG_TO_SELECT -> {
                MutableComponent line = Component.translatable("message.enhanced.build.too_big_to_select", limit);
                yield wanda ? line.append(". ").append(Component.translatable("message.enhanced.build.mark_corners")) : line;
            }
            case BuildVerdictPayload.TOO_WIDE -> Limits.tooWide(limit);
            case BuildVerdictPayload.TOO_FAR -> Limits.tooFar(limit);
            case BuildVerdictPayload.NO_BOX -> Component.translatable("message.enhanced.build.no_box");
            default -> null;
        };
    }

    /** Whether a block is the ground nature made ({@code enhanced:build_ground}), which a built thing stops at. */
    public static boolean isGround(BlockState state) {
        return state.is(ModTags.BUILD_GROUND);
    }

    private static int[][] around() {
        List<int[]> all = new ArrayList<>();
        all.add(new int[]{0, -1, 0});
        all.add(new int[]{0, 1, 0});
        all.add(new int[]{0, 0, -1});
        all.add(new int[]{0, 0, 1});
        all.add(new int[]{-1, 0, 0});
        all.add(new int[]{1, 0, 0});
        for (int dy = -1; dy <= 1; dy++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 1) {
                        all.add(new int[]{dx, dy, dz});
                    }
                }
            }
        }
        return all.toArray(new int[0][]);
    }
}
