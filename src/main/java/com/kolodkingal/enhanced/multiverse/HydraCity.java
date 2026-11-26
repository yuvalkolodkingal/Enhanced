package com.kolodkingal.enhanced.multiverse;

import com.kolodkingal.enhanced.hydra.HydraBossEntity;
import com.kolodkingal.enhanced.hydra.HydraSoldierEntity;
import com.kolodkingal.enhanced.registry.ModEntities;
import com.kolodkingal.enhanced.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import com.kolodkingal.enhanced.asgard.npc.AsgardianEntity;
import com.kolodkingal.enhanced.asgard.npc.Role;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The life of What If... Hydra Won? (3.19.1), in the city that {@link CityChunkGenerator} builds:
 * <ul>
 *     <li><b>The people</b>: citizens (villagers) walk the streets round every player, and tell him, when he speaks to
 *     them, how they suffer under Hydra; Hydra's loudspeakers say their slogans now and then.</li>
 *     <li><b>Hydra</b>: soldiers patrol round every player and fight whoever is not Hydra.</li>
 *     <li><b>The rulers</b>: Red Skull and Captain Hydra ({@link HydraBossEntity}) come out of the Citadel at the
 *     origin when a player comes near it, each until he falls.</li>
 *     <li><b>The liberation</b>: when both have fallen, every Hydra soldier is gone and no more come, the citizens are
 *     happy (and say so), and the city turns back into an ordinary one, chunk by chunk round the players: Hydra's red and
 *     black and iron bars become white, glass and green yards ({@link CityChunkGenerator#FREED}), the roads stay
 *     roads; every chunk in sight of a player turns, and chunks made later are made free.</li>
 * </ul>
 * Everything is capped round each player, and the turning of the city does at most 24 chunks a second per player.
 */
public final class HydraCity {
    /** The tags of the city's citizens and of its patrols. */
    public static final String CITIZEN = "enhanced_citizen";
    public static final String PATROL = "enhanced_city_hydra";
    /** How many citizens and soldiers walk round a player, how far they are counted, and where new ones come. */
    static final int CITIZENS = 8;
    static final int SOLDIERS = 5;
    static final int COUNT_RANGE = 56;
    static final int COME_MIN = 20;
    static final int COME_MAX = 44;
    /** How near the Citadel's door the rulers come out, and how many lines each kind of speech has. */
    static final int CITADEL_RANGE = 64;
    static final int SUFFER_LINES = 12;
    static final int HAPPY_LINES = 8;
    static final int BROADCAST_LINES = 6;
    /** How far round a player (chunks) the free city spreads, and how many chunks a second. */
    /** How many chunks are turned round each player a second, out to the server's view distance (the whole world in sight). */
    static final int FREE_PER_SECOND = 24;

    private static boolean started;

    private HydraCity() {
    }

    // ---- what is saved ----

    /** Who has fallen, whether the city is free, and which chunks have been turned. Saved with the Overworld. */
    public static final class State extends SavedData {
        public static final String NAME = "enhanced_hydra_city";
        boolean redSkull;
        boolean captainHydra;
        boolean freed;
        final Set<Long> turned = new HashSet<>();

        public static SavedData.Factory<State> factory() {
            return new SavedData.Factory<>(State::new, State::load, null);
        }

        private static State load(CompoundTag tag, HolderLookup.Provider registries) {
            State state = new State();
            state.redSkull = tag.getBoolean("RedSkull");
            state.captainHydra = tag.getBoolean("CaptainHydra");
            state.freed = tag.getBoolean("Freed");
            // (3.19.1: "Turned" of before the roads were mended is forgotten: every chunk is turned again, its roads made whole)
            for (long chunk : tag.getLongArray("Freed2")) {
                state.turned.add(chunk);
            }
            return state;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putBoolean("RedSkull", redSkull);
            tag.putBoolean("CaptainHydra", captainHydra);
            tag.putBoolean("Freed", freed);
            tag.putLongArray("Freed2", turned.stream().mapToLong(Long::longValue).toArray());
            return tag;
        }

        /** Hydra rules again: both rulers up, the city not free, no chunk turned (tests, and a fresh start). */
        public void reset() {
            redSkull = false;
            captainHydra = false;
            freed = false;
            turned.clear();
            setDirty();
            CityChunkGenerator.hydraFreed = false;
        }

        static State get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(factory(), NAME);
        }

        public boolean freed() {
            return freed;
        }

        public boolean down(int kind) {
            return kind == HydraBossEntity.RED_SKULL ? redSkull : captainHydra;
        }
    }

    public static State state(MinecraftServer server) {
        return State.get(server);
    }

    // ---- every tick of the server, and every second of a player in the city ----

    static void serverTick(MinecraftServer server) {
        if (!started) {
            started = true;
            CityChunkGenerator.hydraFreed = state(server).freed;
        }
    }

    static void clear() {
        started = false;
        CityChunkGenerator.hydraFreed = false;
    }

    static void tick(ServerPlayer player) {
        if (player.isSpectator()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        State state = state(player.server);
        if (player.tickCount % 40 == 0) {
            people(level, player, state.freed);
        }
        if (player.tickCount % 100 == 0) {
            com.kolodkingal.enhanced.platform.net.Net.sendToPlayer(player, new com.kolodkingal.enhanced.network.HydraSkyPayload(state.freed));
        }
        if (state.freed) {
            turn(level, player, state);
            if (player.getRandom().nextInt(4) == 0) {
                for (AsgardianEntity citizen : citizens(level, player)) {
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, citizen.getX(), citizen.getY() + 2.1, citizen.getZ(), 2, 0.3, 0.2, 0.3, 0.0);
                }
            }
            return;
        }
        if (player.getRandom().nextInt(45) == 0) {
            player.displayClientMessage(Component.translatable("hydra.enhanced.broadcast",
                    Component.translatable("hydra.enhanced.broadcast." + player.getRandom().nextInt(BROADCAST_LINES)))
                    .withStyle(ChatFormatting.DARK_RED), true);
        }
        if (player.blockPosition().closerThan(CityChunkGenerator.CITADEL_DOOR, CITADEL_RANGE) && player.tickCount % 40 == 0) {
            rulers(level, state);
        }
    }

    private static List<AsgardianEntity> citizens(ServerLevel level, ServerPlayer player) {
        return level.getEntitiesOfClass(AsgardianEntity.class, player.getBoundingBox().inflate(COUNT_RANGE),
                v -> v.getTags().contains(CITIZEN));
    }

    /** New citizens and soldiers come round the player until there are as many as the city keeps. */
    private static void people(ServerLevel level, ServerPlayer player, boolean freed) {
        int citizens = citizens(level, player).size();
        if (citizens < CITIZENS) {
            come(level, player, null, CITIZEN);
        }
        if (!freed) {
            int soldiers = level.getEntitiesOfClass(HydraSoldierEntity.class, player.getBoundingBox().inflate(COUNT_RANGE),
                    s -> s.getTags().contains(PATROL)).size();
            if (soldiers < SOLDIERS) {
                come(level, player, ModEntities.HYDRA_SOLDIER.get(), PATROL);
            }
        }
    }

    /** A citizen ({@code type} null: one of the city's people, {@link WhatIfPeople}) or a soldier comes round the player. */
    private static void come(ServerLevel level, ServerPlayer player, @org.jetbrains.annotations.Nullable EntityType<? extends net.minecraft.world.entity.Mob> type, String tag) {
        for (int attempt = 0; attempt < 4; attempt++) {
            double angle = player.getRandom().nextDouble() * Mth.TWO_PI;
            double distance = COME_MIN + player.getRandom().nextDouble() * (COME_MAX - COME_MIN);
            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            if (!level.isLoaded(new BlockPos(x, 0, z))) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            // The streets and the yards only: not the roofs.
            if (y != CityChunkGenerator.GROUND + 1) {
                continue;
            }
            BlockPos feet = new BlockPos(x, y, z);
            if (level.getNearestPlayer(x, y, z, COME_MIN - 4, false) != null) {
                continue;
            }
            if (type == null) {
                level.addFreshEntity(WhatIfPeople.person(level, Role.HYDRA_CITIZEN, x + 0.5, y, z + 0.5, tag));
                return;
            }
            net.minecraft.world.entity.Mob mob = type.create(level);
            if (mob == null) {
                return;
            }
            mob.moveTo(x + 0.5, y, z + 0.5, player.getRandom().nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(feet), MobSpawnType.NATURAL, null);
            mob.addTag(tag);
            if (mob instanceof HydraSoldierEntity soldier) {
                soldier.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(soldier, 0.7));
            }
            level.addFreshEntity(mob);
            return;
        }
    }

    /** Red Skull and Captain Hydra come out of the Citadel, each while he has not fallen and is not already out. */
    private static void rulers(ServerLevel level, State state) {
        for (int kind : new int[]{HydraBossEntity.RED_SKULL, HydraBossEntity.CAPTAIN_HYDRA}) {
            if (state.down(kind) || out(level, kind)) {
                continue;
            }
            BlockPos door = CityChunkGenerator.CITADEL_DOOR.offset(kind == HydraBossEntity.RED_SKULL ? -3 : 3, 0, 0);
            if (!level.isLoaded(door)) {
                continue;
            }
            HydraBossEntity boss = HydraBossEntity.make(level, kind);
            boss.moveTo(door.getX() + 0.5, door.getY(), door.getZ() + 0.5, 180.0F, 0.0F);
            level.addFreshEntity(boss);
            level.playSound(null, door, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.6F, 1.3F);
            Component line = Component.translatable("hydra.enhanced.ruler_comes", boss.getDisplayName()).withStyle(ChatFormatting.DARK_RED);
            for (ServerPlayer player : level.players()) {
                player.sendSystemMessage(line);
            }
        }
    }

    private static boolean out(ServerLevel level, int kind) {
        AABB round = new AABB(CityChunkGenerator.CITADEL_DOOR).inflate(160.0, 128.0, 160.0);
        return !level.getEntitiesOfClass(HydraBossEntity.class, round, b -> b.isAlive() && b.kind() == kind).isEmpty();
    }

    // ---- the rulers fall ----

    /** A ruler fell: he drops what made him, and if he was the last the city is free. */
    public static void bossDown(ServerLevel level, HydraBossEntity boss) {
        ItemStack token = new ItemStack(boss.isRedSkull() ? ModItems.RED_SKULL_SERUM.get() : ModItems.KOBIK_SHARD.get());
        ItemEntity drop = new ItemEntity(level, boss.getX(), boss.getY() + 0.5, boss.getZ(), token);
        drop.setGlowingTag(true);
        level.addFreshEntity(drop);
        if (level.dimension() != Multiverse.HYDRA_KEY) {
            return;
        }
        State state = state(level.getServer());
        if (boss.isRedSkull()) {
            state.redSkull = true;
        } else {
            state.captainHydra = true;
        }
        state.setDirty();
        Component fell = Component.translatable("hydra.enhanced.ruler_falls", boss.getDisplayName()).withStyle(ChatFormatting.GOLD);
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(fell);
        }
        if (state.redSkull && state.captainHydra && !state.freed) {
            free(level, state);
        }
    }

    /** Both rulers have fallen: Hydra is gone, the people rejoice, and the city starts to turn. */
    static void free(ServerLevel level, State state) {
        state.freed = true;
        state.setDirty();
        CityChunkGenerator.hydraFreed = true;
        List<HydraSoldierEntity> soldiers = new java.util.ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof HydraSoldierEntity soldier && !(entity instanceof HydraBossEntity)) {
                soldiers.add(soldier);
            }
        }
        for (HydraSoldierEntity soldier : soldiers) {
            {
                level.sendParticles(ParticleTypes.POOF, soldier.getX(), soldier.getY() + 1.0, soldier.getZ(), 10, 0.3, 0.6, 0.3, 0.02);
                soldier.discard();
            }
        }
        Component free = Component.translatable("hydra.enhanced.freed").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
        for (ServerPlayer player : level.players()) {
            com.kolodkingal.enhanced.platform.net.Net.sendToPlayer(player, new com.kolodkingal.enhanced.network.HydraSkyPayload(true));
            player.sendSystemMessage(free);
            player.displayClientMessage(free, true);
            level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    /** A Hydra soldier that comes into a free city is not let in; nor a second ruler, nor one who has fallen. */
    static boolean onJoin(Entity entity, boolean loadedFromDisk) {
        if (entity instanceof Villager && entity.getTags().contains(CITIZEN)) {
            return true; // a citizen of before 3.19.1's talk: the people are the mod's own now
        }
        if (!(entity instanceof HydraSoldierEntity) || entity.level().getServer() == null) {
            return false;
        }
        State state = state(entity.level().getServer());
        if (entity instanceof HydraBossEntity boss) {
            return state.down(boss.kind()) || state.freed;
        }
        return state.freed;
    }

    // ---- the city turns ----

    /**
     * Turns up to {@link #FREE_PER_SECOND} of the chunks round the player into a free city's, the nearest first, out to
     * the server's view distance: the whole world he can see is free, not only the streets round him.
     */
    private static void turn(ServerLevel level, ServerPlayer player, State state) {
        int cx = player.getBlockX() >> 4;
        int cz = player.getBlockZ() >> 4;
        int radius = Math.max(4, player.server.getPlayerList().getViewDistance() + 1);
        int done = 0;
        for (int r = 0; r <= radius && done < FREE_PER_SECOND; r++) {
            for (int dx = -r; dx <= r && done < FREE_PER_SECOND; dx++) {
                for (int dz = -r; dz <= r && done < FREE_PER_SECOND; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    int x = cx + dx;
                    int z = cz + dz;
                    long key = ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);
                    if (state.turned.contains(key) || !level.hasChunk(x, z)) {
                        continue;
                    }
                    turnChunk(level, level.getChunk(x, z));
                    state.turned.add(key);
                    state.setDirty();
                    done++;
                }
            }
        }
    }

    /** Hydra's colours in the chunk become a free city's, and its roads are mended (grass of before 3.19.1's fix). */
    public static void turnChunk(ServerLevel level, LevelChunk chunk) {
        Set<Block> hydra = CityChunkGenerator.FREED.keySet();
        mendRoads(level, chunk);
        int minY = CityChunkGenerator.GROUND;
        int maxY = CityChunkGenerator.GROUND + CityChunkGenerator.CITADEL_HEIGHT + 2;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int sy = level.getSectionIndex(minY); sy <= level.getSectionIndex(maxY); sy++) {
            if (sy < 0 || sy >= chunk.getSections().length) {
                continue;
            }
            LevelChunkSection section = chunk.getSection(sy);
            if (section.hasOnlyAir() || !section.maybeHas(state -> hydra.contains(state.getBlock()))) {
                continue;
            }
            int baseY = level.getSectionYFromSectionIndex(sy) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        int worldY = baseY + y;
                        BlockState to = CityChunkGenerator.free(state, worldY);
                        if (to == null) {
                            continue;
                        }
                        pos.set(chunk.getPos().getMinBlockX() + x, worldY, chunk.getPos().getMinBlockZ() + z);
                        level.setBlock(pos, to, 2);
                    }
                }
            }
        }
    }

    /** The roads of the chunk are roads again where a turning of before 3.19.1 made them grass (or grey). */
    static void mendRoads(ServerLevel level, LevelChunk chunk) {
        if (!(level.getChunkSource().getGenerator() instanceof CityChunkGenerator city)) {
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                pos.set(chunk.getPos().getMinBlockX() + x, CityChunkGenerator.GROUND, chunk.getPos().getMinBlockZ() + z);
                BlockState road = city.road(pos.getX(), pos.getZ());
                BlockState now = chunk.getBlockState(pos);
                if (road != null && (now.is(Blocks.GRASS_BLOCK) || now.is(Blocks.GRAY_CONCRETE))) {
                    level.setBlock(pos, road, 2);
                }
            }
        }
    }
}
