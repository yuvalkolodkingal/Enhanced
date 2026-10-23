package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.asgard.AsgardDimension;
import com.kolodkingal.enhanced.platform.event.Events;
import com.kolodkingal.enhanced.platform.event.LevelEvent;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Helpers for the tests of Asgard (3.5, written by the skeleton for every lane): the realm's level in the GameTest server.
 */
final class AsgardTestSupport {

    /**
     * The GameTest server builds its world from the flat preset and has only the three vanilla dimensions, not the data
     * pack's Asgard. So the first test that needs it makes the real thing, as {@code RoadTestSupport.ensureRoad} does for
     * the Witches' Road: a level for {@code enhanced:asgard} from the data pack's own dimension file (its own generator),
     * added to the server the way the server adds the dimensions it loads. It stays until the server stops.
     */
    static ServerLevel ensureAsgard(MinecraftServer server) {
        ServerLevel existing = server.getLevel(AsgardDimension.KEY);
        if (existing != null) {
            return existing;
        }
        return makeLevel(server, AsgardDimension.KEY, "/data/enhanced/dimension/asgard.json");
    }

    /** A fixture, and Asgard's level exists. */
    static TestKit.Fixture fixture(GameTestHelper helper) {
        ensureAsgard(helper.getLevel().getServer());
        return TestKit.fixture(helper);
    }

    /** Asgard's level with the chunks round the Observatory's landing generated. */
    static ServerLevel withArrival(MinecraftServer server) {
        ServerLevel asgard = ensureAsgard(server);
        ChunkPos at = new ChunkPos(net.minecraft.core.BlockPos.containing(com.kolodkingal.enhanced.asgard.CityPlan.ARRIVAL));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                asgard.getChunk(at.x + dx, at.z + dz);
            }
        }
        return asgard;
    }

    static ServerLevel makeLevel(MinecraftServer server, ResourceKey<Level> key, String file) {
        try {
            RegistryAccess access = server.registryAccess();
            LevelStem stem;
            try (InputStreamReader reader = new InputStreamReader(AsgardTestSupport.class.getResourceAsStream(file), StandardCharsets.UTF_8)) {
                stem = LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, access), JsonParser.parseReader(reader)).getOrThrow();
            }
            Map<ResourceKey<Level>, ServerLevel> levels = field(server, "levels");
            Executor executor = field(server, "executor");
            LevelStorageSource.LevelStorageAccess storage = field(server, "storageSource");
            ServerLevel level = new ServerLevel(server, executor, storage, new DerivedLevelData(server.getWorldData(), server.getWorldData().overworldData()),
                    key, stem, new ChunkProgressListener() {
                @Override
                public void updateSpawnPos(ChunkPos center) {
                }

                @Override
                public void onStatusChange(ChunkPos chunkPos, @Nullable ChunkStatus chunkStatus) {
                }

                @Override
                public void start() {
                }

                @Override
                public void stop() {
                }
            }, false, BiomeManager.obfuscateSeed(server.getWorldData().worldGenOptions().seed()), List.of(), false, server.overworld().getRandomSequences());
            levels.put(key, level);
            Events.post(new LevelEvent.Load(level));
            return level;
        } catch (IOException | ReflectiveOperationException e) {
            throw new IllegalStateException("The level " + key.location() + " could not be made", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(MinecraftServer server, String name) throws ReflectiveOperationException {
        Field field = MinecraftServer.class.getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(server);
    }

    private AsgardTestSupport() {
    }
}
