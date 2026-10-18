package com.kolodkingal.enhanced.world;

import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

/** What the structures of 3.3 ask of the ground before they are built: its height, and whether it is dry land. */
final class SiteGround {

    /** The height of the top block of the ground (plants and trees not counted: they do not exist yet). */
    static int surface(Structure.GenerationContext context, int x, int z) {
        return context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(),
                context.randomState());
    }

    /** Whether the top block at {@code (x, z)}, at height {@code top}, is land at or above the sea, not water. */
    static boolean dryLand(Structure.GenerationContext context, int x, int z, int top) {
        ChunkGenerator generator = context.chunkGenerator();
        if (top < generator.getSeaLevel()) {
            return false;
        }
        NoiseColumn column = generator.getBaseColumn(x, z, context.heightAccessor(), context.randomState());
        return column.getBlock(top).getFluidState().isEmpty();
    }

    private SiteGround() {
    }
}
