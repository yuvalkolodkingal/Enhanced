package com.kolodkingal.enhanced.road;

import com.kolodkingal.enhanced.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The look of the Road, and the pieces every trial builds with: dark oak and amethyst, hanging roots and glow lichen under
 * everything, soul lanterns for the blue-green light. All coordinates are offsets from the cell's origin
 * ({@link RoadCanvas#origin()}); {@code y} is the height of the walkable top.
 */
public final class RoadPalette {
    public static final BlockState PLANKS = Blocks.DARK_OAK_PLANKS.defaultBlockState();
    public static final BlockState TRIM = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState();
    public static final BlockState LOG = Blocks.DARK_OAK_LOG.defaultBlockState();
    public static final BlockState AMETHYST = Blocks.AMETHYST_BLOCK.defaultBlockState();
    public static final BlockState FENCE = Blocks.DARK_OAK_FENCE.defaultBlockState();
    public static final BlockState LANTERN = Blocks.SOUL_LANTERN.defaultBlockState();
    public static final BlockState HANGING_LANTERN = Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
    public static final BlockState ROOTS = Blocks.HANGING_ROOTS.defaultBlockState();
    public static final BlockState LICHEN = Blocks.GLOW_LICHEN.defaultBlockState().setValue(BlockStateProperties.DOWN, true);
    public static final BlockState GLASS = Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
    public static final BlockState AIR = Blocks.AIR.defaultBlockState();

    // ------------------------------------------------------------------------------------------------------------
    // Stones

    /**
     * A stepping stone: the rectangle {@code x1..x2, z1..z2} at height {@code y}, one block thick, with a root-stalk and a
     * soul lantern under it. Ones with a side of three or more blocks get a trim all round.
     */
    public static void pad(RoadCanvas canvas, int x1, int z1, int x2, int z2, int y) {
        boolean trimmed = x2 - x1 >= 2 && z2 - z1 >= 2;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                canvas.set(x, y, z, trimmed && edge ? TRIM : PLANKS);
            }
        }
        int cx = (x1 + x2) / 2;
        int cz = (z1 + z2) / 2;
        canvas.set(cx, y - 1, cz, LOG);
        canvas.set(cx, y - 2, cz, LOG);
        canvas.set(cx, y - 3, cz, HANGING_LANTERN);
        // Roots and lichen hang from the underside, a different few for every stone.
        int hash = Math.floorMod(x1 * 73856093 ^ z1 * 19349663 ^ y * 83492791, 1 << 20);
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                if (x == cx && z == cz) {
                    continue;
                }
                int h = Math.floorMod(hash + x * 31 + z * 17, 7);
                if (h == 0) {
                    canvas.set(x, y - 1, z, ROOTS);
                } else if (h == 1) {
                    canvas.set(x, y - 1, z, LICHEN);
                }
            }
        }
    }

    /** A platform of {@code half} blocks each way from its middle, trimmed, with a floor of {@code core} in the middle. */
    private static void platform(RoadCanvas canvas, int cx, int y, int cz, int half, BlockState core) {
        for (int x = cx - half; x <= cx + half; x++) {
            for (int z = cz - half; z <= cz + half; z++) {
                boolean edge = Math.abs(x - cx) == half || Math.abs(z - cz) == half;
                boolean inlay = Math.abs(x - cx) <= half - 2 && Math.abs(z - cz) <= half - 2;
                canvas.set(x, y, z, edge ? TRIM : inlay ? core : PLANKS);
                canvas.set(x, y - 1, z, edge ? LOG : PLANKS);
            }
        }
        // Roots and lanterns hang from underneath.
        for (int x = cx - half; x <= cx + half; x += 2) {
            for (int z = cz - half; z <= cz + half; z += 2) {
                int h = Math.floorMod(x * 31 + z * 17 + y, 5);
                canvas.set(x, y - 2, z, h == 0 ? HANGING_LANTERN : h < 3 ? ROOTS : LICHEN);
            }
        }
        // A soul lantern on a post at each corner.
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                int x = cx + sx * half;
                int z = cz + sz * half;
                canvas.set(x, y + 1, z, FENCE);
                canvas.set(x, y + 2, z, FENCE);
                canvas.set(x, y + 3, z, LANTERN);
            }
        }
    }

    /** The platform the player starts on: seven blocks across, an exit door lying flat on the floor behind the start. */
    public static void startPlatform(RoadCanvas canvas, int cx, int y, int cz) {
        platform(canvas, cx, y, cz, 3, PLANKS);
        canvas.set(cx, y + 1, cz + 2, ModBlocks.WITCHES_ROAD_DOOR.get().defaultBlockState());
    }

    /** Where the exit door of {@link #startPlatform} stands, as an offset from the origin. */
    public static BlockPos startExitDoor(RoadCanvas canvas, int cx, int y, int cz) {
        return canvas.at(cx, y + 1, cz + 2);
    }

    /** A checkpoint: five blocks across, the middle in amethyst, a lantern on a post at each corner. */
    public static void checkpointPad(RoadCanvas canvas, int cx, int y, int cz) {
        platform(canvas, cx, y, cz, 2, AMETHYST);
    }

    /** The platform at the end of a trial: nine blocks across, amethyst in the middle. */
    public static void finishPlatform(RoadCanvas canvas, int cx, int y, int cz) {
        platform(canvas, cx, y, cz, 4, AMETHYST);
    }

    // ------------------------------------------------------------------------------------------------------------
    // The gate

    /**
     * Draws a gate standing on {@code (cx, y, cz)} across {@code axis} (the axis it is wide along): two amethyst pillars,
     * a lintel, and purple glass in the opening while it is sealed.
     */
    public static void gate(RoadCanvas canvas, int cx, int y, int cz, Direction.Axis axis) {
        boolean alongX = axis == Direction.Axis.X;
        for (int w = -2; w <= 2; w++) {
            int x = cx + (alongX ? w : 0);
            int z = cz + (alongX ? 0 : w);
            boolean pillar = Math.abs(w) == 2;
            for (int h = 0; h <= 3; h++) {
                if (pillar || h == 3) {
                    canvas.set(x, y + h, z, h == 0 ? TRIM : AMETHYST);
                } else {
                    canvas.set(x, y + h, z, GLASS);
                }
            }
            if (pillar) {
                canvas.set(x, y + 4, z, LANTERN);
            }
        }
    }

    /** The gate opens: the glass in its opening is gone. */
    public static void openGate(RoadCanvas canvas, ServerLevel level, RoadLayout.Gate gate) {
        boolean alongX = gate.axis() == Direction.Axis.X;
        BlockPos base = gate.base();
        for (int w = -1; w <= 1; w++) {
            for (int h = 0; h <= 2; h++) {
                canvas.live(level, base.offset(alongX ? w : 0, h, alongX ? 0 : w), AIR);
            }
        }
    }

    /** Whether the state is a block the Road draws for its look (used by tests to find what a trial left behind). */
    public static boolean isRoadBlock(BlockState state) {
        Block block = state.getBlock();
        return !state.isAir() && (block == Blocks.DARK_OAK_PLANKS || block == Blocks.AMETHYST_BLOCK || block == Blocks.SOUL_LANTERN);
    }

    private RoadPalette() {
    }
}
