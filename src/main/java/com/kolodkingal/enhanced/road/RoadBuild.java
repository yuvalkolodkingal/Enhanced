package com.kolodkingal.enhanced.road;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * What a {@link RoadChallenge} gets to build its course: the canvas to draw on, and the seed that makes the course the
 * same every time for the same player (use it to start a {@code java.util.SplittableRandom}, never the world's random
 * or the clock). The course must depend on nothing else: it is built again, identically, when the player comes back.
 */
public record RoadBuild(ServerLevel level, long seed, RoadCanvas canvas) {

    /** The centre of the cell: the ground of the trial, where the course starts. */
    public BlockPos origin() {
        return canvas.origin();
    }
}
