package com.kolodkingal.enhanced.road;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The dimension of the Witches' Road trials, {@code enhanced:witches_road}: an empty void (a data pack dimension, see
 * {@code data/enhanced/dimension/witches_road.json}), where every player's trial is built by code in a private cell of its
 * own ({@link RoadCells}) and taken down again when he leaves.
 */
public final class RoadDimension {
    public static final ResourceKey<Level> KEY = ResourceKey.create(Registries.DIMENSION, Enhanced.id("witches_road"));

    /** The Road's level, or null if the server has none (the data pack dimension is missing). */
    @Nullable
    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(KEY);
    }

    public static boolean is(Level level) {
        return level.dimension() == KEY;
    }

    /** Whether the entity is in the Road dimension. */
    public static boolean in(Entity entity) {
        return is(entity.level());
    }

    private RoadDimension() {
    }
}
