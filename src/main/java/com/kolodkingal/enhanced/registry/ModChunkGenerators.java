package com.kolodkingal.enhanced.registry;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.asgard.AsgardChunkGenerator;
import com.kolodkingal.enhanced.platform.reg.RegEntry;
import com.kolodkingal.enhanced.platform.reg.Registrar;
import com.kolodkingal.enhanced.platform.reg.Registrars;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** The mod's chunk generators (3.5): Asgard's, {@code enhanced:asgard}, named by {@code data/enhanced/dimension/asgard.json}. */
public final class ModChunkGenerators {
    public static final Registrar<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            Registrars.create(Registries.CHUNK_GENERATOR, Enhanced.MODID);

    public static final RegEntry<MapCodec<? extends ChunkGenerator>, MapCodec<AsgardChunkGenerator>> ASGARD =
            CHUNK_GENERATORS.register("asgard", () -> AsgardChunkGenerator.CODEC);

    private ModChunkGenerators() {
    }
}
