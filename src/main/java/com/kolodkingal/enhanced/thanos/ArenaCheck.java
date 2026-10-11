package com.kolodkingal.enhanced.thanos;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Whether the summoner's ground is a place the Mad Titan can be called to: the pure verdict of DESIGN section 15.3. He
 * wants solid ground under the summoner, the open sky above and a wide space round, all of it inside the world border.
 * The check reads the world through {@link World}, so the game tests can feed it a fake one (the test arena has a
 * barrier roof and could never pass).
 *
 * <p>The order of the verdicts: the border, the ground, the sky, then 24 sample spots (8 on each ring of radius 8, 16
 * and 24 round the summoner) of which {@code summonOpenFraction} must have standing room (solid ground within three
 * blocks up or down, three free blocks above it, no liquid).
 */
public final class ArenaCheck {
    /** Why a place is not good enough. */
    public enum Verdict {
        OK, NO_GROUND, NO_SKY, TOO_CLOSED, BORDER
    }

    /** The radii of the sample rings and how many spots each has. */
    private static final int[] RINGS = {8, 16, 24};
    private static final int SPOTS_PER_RING = 8;

    /** The world as the check sees it. */
    public interface World {
        /** Whether the block can be stood on: solid on its top face and not a liquid. */
        boolean solidGround(int x, int y, int z);

        /** Whether there is room in the block: nothing to collide with, no liquid. */
        boolean free(int x, int y, int z);

        /** Whether the block is a liquid. */
        boolean liquid(int x, int y, int z);

        /** Whether the sky is open above the block (nothing between it and the top of the world). */
        boolean seesSky(int x, int y, int z);

        /** Whether the column is inside the world border. */
        boolean inBorder(double x, double z);

        int minY();

        int maxY();
    }

    /**
     * The verdict for a summoner standing at {@code feet}.
     *
     * @param skyBlocks    the clear air needed above the summoner
     * @param openFraction the fraction of the sample spots that need standing room
     * @param radius       the radius of the arena: all of it must be inside the world border
     */
    public static Verdict verdict(World world, BlockPos feet, int skyBlocks, double openFraction, int radius) {
        double x = feet.getX() + 0.5;
        double z = feet.getZ() + 0.5;
        if (!world.inBorder(x, z) || !world.inBorder(x + radius, z) || !world.inBorder(x - radius, z)
                || !world.inBorder(x, z + radius) || !world.inBorder(x, z - radius)) {
            return Verdict.BORDER;
        }
        if (!world.solidGround(feet.getX(), feet.getY() - 1, feet.getZ()) || world.liquid(feet.getX(), feet.getY(), feet.getZ())) {
            return Verdict.NO_GROUND;
        }
        if (!world.seesSky(feet.getX(), feet.getY() + 2, feet.getZ())) {
            return Verdict.NO_SKY;
        }
        for (int dy = 2; dy <= skyBlocks + 1; dy++) {
            int y = feet.getY() + dy;
            if (y >= world.maxY()) {
                break;
            }
            if (!world.free(feet.getX(), y, feet.getZ())) {
                return Verdict.NO_SKY;
            }
        }
        int open = 0;
        int total = 0;
        for (int ring : RINGS) {
            for (int k = 0; k < SPOTS_PER_RING; k++) {
                double angle = k * (Math.PI * 2.0 / SPOTS_PER_RING);
                int sx = feet.getX() + (int) Math.round(Math.cos(angle) * ring);
                int sz = feet.getZ() + (int) Math.round(Math.sin(angle) * ring);
                total++;
                if (standingRoom(world, sx, feet.getY(), sz) != Integer.MIN_VALUE) {
                    open++;
                }
            }
        }
        return open >= Math.ceil(total * openFraction - 1.0E-9) ? Verdict.OK : Verdict.TOO_CLOSED;
    }

    /**
     * The height of the feet of someone standing in the column {@code (x, z)} within three blocks of {@code y}, or
     * {@link Integer#MIN_VALUE} if there is no standing room there: solid ground, three free blocks above it, no liquid.
     */
    public static int standingRoom(World world, int x, int y, int z) {
        for (int step = 0; step <= 3; step++) {
            for (int sign : step == 0 ? new int[]{0} : new int[]{1, -1}) {
                int feet = y + step * sign;
                if (feet <= world.minY() || feet + 2 >= world.maxY()) {
                    continue;
                }
                if (world.solidGround(x, feet - 1, z) && world.free(x, feet, z) && world.free(x, feet + 1, z)
                        && world.free(x, feet + 2, z) && !world.liquid(x, feet, z)) {
                    return feet;
                }
            }
        }
        return Integer.MIN_VALUE;
    }

    /**
     * Where he arrives: a spot with standing room 14 to 20 blocks from the summoner, nearest first, or null if there is
     * none (the caller then uses the summoner's own ground).
     */
    @Nullable
    public static BlockPos arrivalSpot(World world, BlockPos summoner) {
        for (int distance = 14; distance <= 20; distance++) {
            for (int k = 0; k < 16; k++) {
                double angle = k * (Math.PI * 2.0 / 16.0) + distance * 0.37;
                int x = summoner.getX() + (int) Math.round(Math.cos(angle) * distance);
                int z = summoner.getZ() + (int) Math.round(Math.sin(angle) * distance);
                if (!world.inBorder(x + 0.5, z + 0.5) || Math.hypot(x - summoner.getX(), z - summoner.getZ()) < 14.0) {
                    // (Rounding to a block can bring a spot a little closer than the ring it was taken on.)
                    continue;
                }
                int feet = standingRoom(world, x, summoner.getY(), z);
                if (feet != Integer.MIN_VALUE) {
                    return new BlockPos(x, feet, z);
                }
            }
        }
        return null;
    }

    /** The real world as the check sees it. */
    public static World of(ServerLevel level) {
        return new World() {
            @Override
            public boolean solidGround(int x, int y, int z) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(pos);
                return state.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP) && state.getFluidState().isEmpty();
            }

            @Override
            public boolean free(int x, int y, int z) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(pos);
                return state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty();
            }

            @Override
            public boolean liquid(int x, int y, int z) {
                return !level.getFluidState(new BlockPos(x, y, z)).isEmpty();
            }

            @Override
            public boolean seesSky(int x, int y, int z) {
                return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) <= y;
            }

            @Override
            public boolean inBorder(double x, double z) {
                WorldBorder border = level.getWorldBorder();
                return border.isWithinBounds(x, z);
            }

            @Override
            public int minY() {
                return level.getMinBuildHeight();
            }

            @Override
            public int maxY() {
                return level.getMaxBuildHeight();
            }
        };
    }

    /** The point on the ground of {@code spot} where something stands. */
    public static Vec3 standAt(BlockPos spot) {
        return Vec3.atBottomCenterOf(spot);
    }

    private ArenaCheck() {
    }
}
