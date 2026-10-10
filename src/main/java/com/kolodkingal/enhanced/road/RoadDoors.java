package com.kolodkingal.enhanced.road;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Every Witches' Road door in one dimension, in the order they were placed: walking the Road takes you to the next one.
 * Stored as a level attachment, so it is saved with the world.
 */
public record RoadDoors(List<BlockPos> doors) {
    public static final RoadDoors EMPTY = new RoadDoors(List.of());

    public static final Codec<RoadDoors> CODEC = BlockPos.CODEC.listOf().xmap(RoadDoors::new, RoadDoors::doors);

    public RoadDoors {
        doors = doors.stream().map(BlockPos::immutable).toList();
    }

    /** The list with this door added at the end (unchanged if it is already listed). */
    public RoadDoors with(BlockPos pos) {
        if (doors.contains(pos)) {
            return this;
        }
        List<BlockPos> list = new ArrayList<>(doors);
        list.add(pos);
        return new RoadDoors(list);
    }

    public RoadDoors without(BlockPos pos) {
        List<BlockPos> list = new ArrayList<>(doors);
        list.remove(pos);
        return new RoadDoors(list);
    }
}
