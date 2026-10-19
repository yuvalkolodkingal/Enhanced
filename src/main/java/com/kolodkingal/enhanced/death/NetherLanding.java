package com.kolodkingal.enhanced.death;

import com.kolodkingal.enhanced.stones.SpaceRealms;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Where a player who chose "Start again in the Nether" stands up (3.7, lane L8): a safe floor near his spawn point
 * divided by 8 (solid ground, two blocks of air, no lava or fire within one block, below the bedrock roof), searched in
 * square rings out from the centre within a budget of columns; and when there is none, a small platform built for him
 * (an obsidian floor in a ring of netherrack, an air pocket over it, the lava and fire round it turned to netherrack).
 * Server only; pure on the level it is given (the tests use a fake Nether in their own arena).
 */
public final class NetherLanding {
    /** How far round the centre the floors are looked for (blocks, each way). */
    public static final int RADIUS = 16;
    /** The most columns looked at (the whole square of {@link #RADIUS}). */
    public static final int BUDGET = (2 * RADIUS + 1) * (2 * RADIUS + 1);
    /** The height the search prefers (a floor nearer to it wins in a column), and the platform's floor. */
    public static final int PREFERRED_Y = 64;
    /** Blocks kept clear under the roof of a level with a ceiling (the Nether's bedrock is at 123..127). */
    static final int UNDER_ROOF = 6;

    private NetherLanding() {
    }

    /**
     * The centre of the search in {@code nether}: the place the player would have started again ({@code from}, in
     * {@code fromType}), by the two levels' scale (the Overworld's x/8, z/8), at the preferred height, inside the
     * border.
     */
    public static BlockPos centre(ServerLevel nether, DimensionType fromType, Vec3 from) {
        double scale = DimensionType.getTeleportationScale(fromType, nether.dimensionType());
        return nether.getWorldBorder().clampToBounds(from.x * scale, PREFERRED_Y, from.z * scale);
    }

    /** The feet of a safe spot near {@code centre}, or of the platform built at {@code centre} when there is none. */
    public static BlockPos land(ServerLevel nether, BlockPos centre) {
        int min = nether.getMinBuildHeight() + 1;
        int max = top(nether);
        return find(nether, centre, RADIUS, min, max).orElseGet(() -> platform(nether, centre, min, max));
    }

    /** The highest feet height allowed in {@code level}: under the roof of a level with a ceiling. */
    public static int top(ServerLevel level) {
        return level.dimensionType().hasCeiling()
                ? level.getMinBuildHeight() + level.dimensionType().logicalHeight() - UNDER_ROOF
                : level.getMaxBuildHeight() - 2;
    }

    /**
     * The nearest safe feet to {@code centre}: column by column in square rings out to {@code radius} (at most {@link
     * #BUDGET} columns), and in a column the safe feet between {@code minY} and {@code maxY} nearest the centre's
     * height.
     */
    public static Optional<BlockPos> find(ServerLevel level, BlockPos centre, int radius, int minY, int maxY) {
        int columns = 0;
        BlockPos.MutableBlockPos feet = new BlockPos.MutableBlockPos();
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    if (++columns > BUDGET) {
                        return Optional.empty();
                    }
                    int x = centre.getX() + dx;
                    int z = centre.getZ() + dz;
                    if (!level.getWorldBorder().isWithinBounds(x, z)) {
                        continue;
                    }
                    level.getChunk(x >> 4, z >> 4);
                    BlockPos best = null;
                    for (int y = minY; y <= maxY; y++) {
                        feet.set(x, y, z);
                        if (safe(level, feet, maxY) && (best == null
                                || Math.abs(y - centre.getY()) < Math.abs(best.getY() - centre.getY()))) {
                            best = feet.immutable();
                        }
                    }
                    if (best != null) {
                        return Optional.of(best);
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Whether a player may stand up at {@code feet}: {@link SpaceRealms#safe} (solid, harmless ground that is not
     * bedrock; air at the feet and the head), no higher than {@code maxY} (never on or in the roof), and no lava,
     * fire or magma within one block of the body.
     */
    public static boolean safe(ServerLevel level, BlockPos feet, int maxY) {
        if (feet.getY() > maxY || !SpaceRealms.safe(level, feet)) {
            return false;
        }
        BlockPos.MutableBlockPos near = new BlockPos.MutableBlockPos();
        for (int dy = -1; dy <= 2; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    near.set(feet.getX() + dx, feet.getY() + dy, feet.getZ() + dz);
                    if (hot(level.getBlockState(near))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean hot(BlockState state) {
        return state.getFluidState().is(FluidTags.LAVA) || state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK)
                || state.is(BlockTags.CAMPFIRES);
    }

    /**
     * Builds the platform at {@code centre} (its floor at the centre's height, kept between {@code minY} and {@code
     * maxY}): a 3 x 3 obsidian floor in a 5 x 5 ring of netherrack, three blocks of air over all of it, and round that
     * pocket (one block further out, below and above) every fluid, fire or falling block turned to netherrack, so no
     * lava flows in and nothing falls on him. Returns the feet (the middle of the floor).
     */
    public static BlockPos platform(ServerLevel level, BlockPos centre, int minY, int maxY) {
        int y = Mth.clamp(centre.getY(), minY + 2, Math.max(minY + 2, maxY - 3));
        BlockPos feet = new BlockPos(centre.getX(), y, centre.getZ());
        BlockState netherrack = Blocks.NETHERRACK.defaultBlockState();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -2; dy <= 3; dy++) {
                    at.set(feet.getX() + dx, feet.getY() + dy, feet.getZ() + dz);
                    boolean inner = Math.abs(dx) <= 2 && Math.abs(dz) <= 2;
                    if (inner && dy >= 0 && dy <= 2) {
                        level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
                    } else if (inner && dy == -1) {
                        boolean middle = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
                        level.setBlock(at, middle ? Blocks.OBSIDIAN.defaultBlockState() : netherrack, 3);
                    } else {
                        BlockState state = level.getBlockState(at);
                        if (!state.getFluidState().isEmpty() || hot(state)
                                || state.getBlock() instanceof FallingBlock) {
                            level.setBlock(at, netherrack, 3);
                        }
                    }
                }
            }
        }
        return feet;
    }
}
