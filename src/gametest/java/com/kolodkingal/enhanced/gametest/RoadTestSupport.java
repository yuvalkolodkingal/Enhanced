package com.kolodkingal.enhanced.gametest;

import com.google.gson.JsonParser;
import com.kolodkingal.enhanced.registry.ModItems;
import com.kolodkingal.enhanced.registry.ModTicketTypes;
import com.kolodkingal.enhanced.road.RoadChallenge;
import com.kolodkingal.enhanced.road.RoadBuild;
import com.kolodkingal.enhanced.road.RoadDimension;
import com.kolodkingal.enhanced.road.RoadLayout;
import com.kolodkingal.enhanced.road.RoadSave;
import com.kolodkingal.enhanced.road.RoadSweeper;
import com.kolodkingal.enhanced.road.RoadTrial;
import com.kolodkingal.enhanced.road.RoadTrials;
import com.kolodkingal.enhanced.road.parkour.ParkourChallenge;
import com.kolodkingal.enhanced.road.parkour.ParkourGenerator;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.jetbrains.annotations.Nullable;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.Ticket;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static com.kolodkingal.enhanced.gametest.TestKit.FLOOR;

/**
 * Helpers for the tests of the Witches' Road trials: a player who may walk the Road, waiting until the trial built for him is
 * ready, putting him in the places a trial looks at, and counting what a trial leaves in the Road dimension.
 */
final class RoadTestSupport {
    /** A small trial of the Road that is not the real thing: three jumps, so a test of the framework can have a second trial. */
    static class ShortParkour implements RoadChallenge {
        private final String id;

        ShortParkour(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public RoadLayout build(RoadBuild build) {
            ParkourGenerator.Plan plan = ParkourGenerator.generate(build.seed(), new ParkourGenerator.Params(3, 1, 2));
            return ParkourChallenge.draw(build.canvas(), plan, 12, 0);
        }
    }

    /**
     * The GameTest server builds its world from the flat preset and has only the three vanilla dimensions, not the data pack's
     * Witches' Road. So the first test that needs it makes the real thing: a level for {@code enhanced:witches_road} from the data
     * pack's own dimension file (the same flat void), added to the server the way the server adds the dimensions it loads. It
     * stays until the server stops.
     */
    static ServerLevel ensureRoad(MinecraftServer server) {
        ServerLevel existing = server.getLevel(RoadDimension.KEY);
        if (existing != null) {
            return existing;
        }
        try {
            RegistryAccess access = server.registryAccess();
            LevelStem stem;
            try (InputStreamReader reader = new InputStreamReader(RoadTestSupport.class.getResourceAsStream("/data/enhanced/dimension/witches_road.json"),
                    StandardCharsets.UTF_8)) {
                stem = LevelStem.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, access), JsonParser.parseReader(reader)).getOrThrow();
            }
            Map<ResourceKey<Level>, ServerLevel> levels = field(server, "levels");
            Executor executor = field(server, "executor");
            LevelStorageSource.LevelStorageAccess storage = field(server, "storageSource");
            ServerLevel level = new ServerLevel(server, executor, storage, new DerivedLevelData(server.getWorldData(), server.getWorldData().overworldData()),
                    RoadDimension.KEY, stem, new ChunkProgressListener() {
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
            levels.put(RoadDimension.KEY, level);
            NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));
            return level;
        } catch (IOException | ReflectiveOperationException e) {
            throw new IllegalStateException("The Road's level could not be made", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(MinecraftServer server, String name) throws ReflectiveOperationException {
        Field field = MinecraftServer.class.getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(server);
    }

    /** A fixture, and the Road's level exists. */
    static TestKit.Fixture fixture(GameTestHelper helper) {
        ensureRoad(helper.getLevel().getServer());
        return TestKit.fixture(helper);
    }

    /**
     * A small trial that lets the player place a block one block above the start platform's middle and break the one under it, and
     * that spawns a boat of its own (tracked) when it begins, next to an item it forgets (not tracked).
     */
    static final class PlotParkour extends ShortParkour {
        PlotParkour(String id) {
            super(id);
        }

        static BlockPos plot(RoadTrial trial) {
            return BlockPos.containing(trial.layout().start()).above(3);
        }

        @Override
        public boolean mayPlace(RoadTrial trial, ServerPlayer player, BlockPos pos) {
            return pos.equals(plot(trial));
        }

        @Override
        public boolean mayBreak(RoadTrial trial, ServerPlayer player, BlockPos pos) {
            return pos.equals(plot(trial));
        }

        @Override
        public void onStart(RoadTrial trial, ServerPlayer player) {
            ServerLevel level = trial.level();
            Boat boat = EntityType.BOAT.create(level);
            boat.moveTo(trial.layout().start().add(2.0, 0.0, 0.0));
            level.addFreshEntity(trial.track(boat));
            ItemEntity item = new ItemEntity(level, trial.layout().start().x + 2.5, trial.layout().start().y + 1.0, trial.layout().start().z, new ItemStack(Items.STICK));
            level.addFreshEntity(item);
            forgotten.put(trial.owner(), item);
        }

        /** The item each player's trial spawned and forgot. */
        static final Map<java.util.UUID, ItemEntity> forgotten = new java.util.concurrent.ConcurrentHashMap<>();
    }

    /** A Survival player without powers holding a Scarlet Witch Gene, in the arena. */
    static ServerPlayer walker(TestKit.Fixture f, double x, double z) {
        ServerPlayer player = f.player(x, FLOOR, z);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SCARLET_WITCH_GENE.get(), 1));
        // Whatever happens, the test leaves nothing of him in the Road.
        f.onClose(() -> forget(f.level().getServer(), player));
        return player;
    }

    /** Takes the player out of the Road, forgets his progress and clears every cell that is waiting to be cleared. */
    static void forget(MinecraftServer server, ServerPlayer player) {
        if (RoadTrials.isInside(player) || RoadDimension.in(player)) {
            RoadTrials.leave(player, RoadChallenge.End.LEFT);
        }
        RoadSave.get(server).forget(player.getUUID());
        RoadSweeper.drain(server);
    }

    /**
     * For a sequence's {@code thenWaitUntil}: passes in the tick the player's trial is active. The fake client of a test player never
     * answers the server's change of dimension, and a player the server thinks is still changing it cannot be hurt; this answers
     * for it.
     */
    static Runnable active(ServerPlayer player) {
        return () -> {
            RoadTrial trial = RoadTrials.trialOf(player.getUUID());
            TestKit.check(trial != null && trial.phase() == RoadTrial.Phase.ACTIVE, "the trial of " + player.getGameProfile().getName() + " is ready");
            player.hasChangedDimension();
        };
    }

    /** The player's trial (the test fails if he has none). */
    static RoadTrial trial(ServerPlayer player) {
        RoadTrial trial = RoadTrials.trialOf(player.getUUID());
        TestKit.check(trial != null, "he has a trial");
        return trial;
    }

    /** How many chunk tickets of the Road the level holds for this player (read from the chunk system, which keeps no public count). */
    @SuppressWarnings("unchecked")
    static int tickets(ServerLevel level, java.util.UUID owner) {
        try {
            Field manager = ServerChunkCache.class.getDeclaredField("distanceManager");
            manager.setAccessible(true);
            Object distanceManager = manager.get(level.getChunkSource());
            Field ticketsField = DistanceManager.class.getDeclaredField("tickets");
            ticketsField.setAccessible(true);
            Field keyField = Ticket.class.getDeclaredField("key");
            keyField.setAccessible(true);
            int count = 0;
            for (SortedArraySet<Ticket<?>> set : ((Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>>) ticketsField.get(distanceManager)).values()) {
                for (Ticket<?> ticket : set) {
                    if (ticket.getType() == ModTicketTypes.ENHANCED_ROAD && owner.equals(keyField.get(ticket))) {
                        count++;
                    }
                }
            }
            return count;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static ServerLevel road(GameTestHelper helper) {
        return RoadDimension.level(helper.getLevel().getServer());
    }

    /** Moves the player to a place in the Road (feet), the way an operator's /tp would. */
    static void put(ServerPlayer player, ServerLevel road, Vec3 at) {
        // (The way an operator's /tp would: the rules of the Road do not stop it.)
        RoadTrials.moving(player, () -> player.teleportTo(road, at.x, at.y, at.z, player.getYRot(), 0.0F));
    }

    /** The middle of a zone, at its bottom. */
    static Vec3 inside(AABB zone) {
        Vec3 c = zone.getCenter();
        return new Vec3(c.x, zone.minY + 0.2, c.z);
    }

    /** How many blocks that are not air there are in the chunks (all heights). */
    static int blocksIn(ServerLevel level, long[] chunks) {
        int count = 0;
        for (long key : chunks) {
            ChunkPos pos = new ChunkPos(key);
            LevelChunk chunk = level.getChunk(pos.x, pos.z);
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                if (chunk.getSection(chunk.getSectionIndex(y)).hasOnlyAir()) {
                    y |= 15;
                    continue;
                }
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        at.set(pos.getMinBlockX() + x, y, pos.getMinBlockZ() + z);
                        if (!chunk.getBlockState(at).isAir()) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    private RoadTestSupport() {
    }
}
