package com.kolodkingal.enhanced.asgard;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Asgard, the realm eternal (3.5): the dimension {@code enhanced:asgard} (a data pack dimension, see
 * {@code data/enhanced/dimension/asgard.json}), a floating land over a starry void made by its own chunk generator
 * ({@link AsgardChunkGenerator}). Reached only by the Bifrost: {@link AsgardGate} refuses every other way in or out.
 */
public final class AsgardDimension {
    public static final ResourceKey<Level> KEY = ResourceKey.create(Registries.DIMENSION, Enhanced.id("asgard"));

    /** Asgard's level, or null if the server has none (the data pack dimension is missing). */
    @Nullable
    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(KEY);
    }

    public static boolean is(Level level) {
        return level.dimension() == KEY;
    }

    /** Whether the entity is in Asgard. */
    public static boolean in(Entity entity) {
        return is(entity.level());
    }

    private AsgardDimension() {
    }
}
