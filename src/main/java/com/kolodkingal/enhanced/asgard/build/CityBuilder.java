package com.kolodkingal.enhanced.asgard.build;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

/**
 * One of the procedural builders of Asgard's city (3.5): a pure function from a block position to the block it puts
 * there (or nothing), inside its {@link #box}. Each chunk asks only the builders whose box it meets, and each builder
 * draws only its own part of that chunk ({@link #place}); a builder may draw faster than block by block.
 */
public interface CityBuilder {
    /** Everything the builder can touch. */
    BoundingBox box();

    /** What the builder puts at (x, y, z), or null (leave the block as it is). */
    @Nullable
    BlockState at(int x, int y, int z);

    /** Draws the builder's part of the canvas's chunk. By default: every block of the box inside the chunk. */
    default void place(CityCanvas canvas) {
        BoundingBox b = box();
        BoundingBox c = canvas.box();
        if (!b.intersects(c)) {
            return;
        }
        int x0 = Math.max(b.minX(), c.minX()), x1 = Math.min(b.maxX(), c.maxX());
        int z0 = Math.max(b.minZ(), c.minZ()), z1 = Math.min(b.maxZ(), c.maxZ());
        int y0 = Math.max(b.minY(), c.minY()), y1 = Math.min(b.maxY(), c.maxY());
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    BlockState state = at(x, y, z);
                    if (state != null) {
                        canvas.set(x, y, z, state);
                    }
                }
            }
        }
    }
}
