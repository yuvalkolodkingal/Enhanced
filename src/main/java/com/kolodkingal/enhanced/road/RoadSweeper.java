package com.kolodkingal.enhanced.road;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.config.WitchesRoadConfig;
import com.kolodkingal.enhanced.registry.ModTicketTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * Takes the trials down. A cell is a job: every chunk its canvas touched is loaded and cleared of blocks (the heights it
 * used only), what stood in it (a boat, a dropped item) is removed, and only then are its chunk tickets let go and the cell
 * struck from the ledger. The work is sliced by {@code witches_road.removeBlocksPerTick}. At the start of a server every cell
 * in the ledger is a job: whatever the last run left behind is cleaned up before anyone can find it.
 */
public final class RoadSweeper {
    /** How far a ticket spreads: 2 keeps the chunk loaded and its entities ticking. */
    public static final int TICKET_DISTANCE = 2;
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    /** What a chunk costs against the budget on top of its blocks (loading it, looking for entities). */
    private static final int CHUNK_COST = 200;

    /** How long after the blocks of a cell are gone the entities in its chunks are removed (the chunks' entities may arrive a little after the chunks: 2 s). */
    private static final int ENTITY_DELAY_TICKS = 40;
    /** How many times a cell whose chunks could not all be cleared is tried again before it is left in the ledger. */
    private static final int ATTEMPTS = 3;

    private static final class Job {
        final int cell;
        final UUID owner;
        final long[] chunks;
        final int minY;
        final int maxY;
        final int attempt;
        /** The chunks of the cell, whole: the tickets are let go of all together, and the entities are removed from all of them. */
        final long[] all;
        int cursor;
        /** The chunks that threw while their blocks were cleared. */
        final java.util.List<Long> failed = new java.util.ArrayList<>();
        /** The game time from which the entities are removed (0: the blocks are not all cleared yet). */
        long entitiesAt;
        int entityCursor;

        Job(RoadSave.Cell cell) {
            this(cell.index(), cell.owner(), cell.chunks(), cell.chunks(), cell.minY(), cell.maxY(), 0);
        }

        Job(int cell, UUID owner, long[] chunks, long[] all, int minY, int maxY, int attempt) {
            this.cell = cell;
            this.owner = owner;
            this.chunks = chunks;
            this.all = all;
            this.minY = minY;
            this.maxY = maxY;
            this.attempt = attempt;
        }
    }

    private static final Deque<Job> JOBS = new ArrayDeque<>();
    /** For the tests: makes the clearing of the chunks it says yes to throw. */
    private static java.util.function.LongPredicate fault;

    /** For the tests: {@code fault} says which chunks throw when they are cleared (null: none does). */
    public static void injectFault(java.util.function.LongPredicate fault) {
        RoadSweeper.fault = fault;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Tickets

    /** Holds the chunks with the Road's ticket (loaded and ticking until {@link #release}). */
    public static void hold(ServerLevel level, UUID owner, long[] chunks) {
        for (long chunk : chunks) {
            level.getChunkSource().addRegionTicket(ModTicketTypes.ENHANCED_ROAD, new ChunkPos(chunk), TICKET_DISTANCE, owner, true);
        }
    }

    /** Lets go of what {@link #hold} holds. */
    public static void release(ServerLevel level, UUID owner, long[] chunks) {
        for (long chunk : chunks) {
            level.getChunkSource().removeRegionTicket(ModTicketTypes.ENHANCED_ROAD, new ChunkPos(chunk), TICKET_DISTANCE, owner, true);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Jobs

    /** Queues the clearing of a cell of the ledger (the tickets of a cell that is not held yet are taken now). */
    public static void queue(MinecraftServer server, int cell, boolean ticketsHeld) {
        for (Job job : JOBS) {
            if (job.cell == cell) {
                return;
            }
        }
        RoadSave.Cell record = RoadSave.get(server).cell(cell);
        if (record == null) {
            return;
        }
        ServerLevel level = RoadDimension.level(server);
        if (level != null && !ticketsHeld) {
            hold(level, record.owner(), record.chunks());
        }
        JOBS.addLast(new Job(record));
    }

    /** Queues every cell of the ledger that is not in use (the server has just started: none is). */
    public static void queueOrphans(MinecraftServer server, java.util.Set<Integer> inUse) {
        int found = 0;
        for (RoadSave.Cell cell : RoadSave.get(server).cells()) {
            if (!inUse.contains(cell.index())) {
                queue(server, cell.index(), false);
                found++;
            }
        }
        if (found > 0) {
            Enhanced.LOGGER.info("Clearing {} cell(s) of the Witches' Road that the last run left behind", found);
        }
    }

    /** Whether any cell is still being cleared. */
    public static boolean busy() {
        return !JOBS.isEmpty();
    }

    public static int pending() {
        return JOBS.size();
    }

    /** Whether a cell is waiting to be cleared or being cleared. */
    public static boolean isClearing(int cell) {
        for (Job job : JOBS) {
            if (job.cell == cell) {
                return true;
            }
        }
        return false;
    }

    /** One tick of work. */
    public static void tick(MinecraftServer server) {
        if (JOBS.isEmpty()) {
            return;
        }
        run(server, WitchesRoadConfig.REMOVE_BLOCKS_PER_TICK.get(), false);
    }

    /** Does all the work now (a stopping server, a test). */
    public static void drain(MinecraftServer server) {
        int guard = 0;
        while (!JOBS.isEmpty() && guard++ < 100_000) {
            run(server, Integer.MAX_VALUE / 2, true);
        }
    }

    /**
     * Works on the first job. The blocks of a cell go first; the entities (a boat, labels, a dropped item) only a moment later, with the
     * chunks still held, because the entities of a chunk that was loaded from disk arrive a little after it (a cell the last run left behind is
     * cleared this way); then the tickets are let go and the cell leaves the ledger. A chunk that cannot be cleared does not stop the rest;
     * the cell is tried again, and if it still cannot be cleared it stays in the ledger (it is not given to anybody, and the next start tries it).
     */
    private static void run(MinecraftServer server, int budget, boolean immediate) {
        ServerLevel level = RoadDimension.level(server);
        while (budget > 0 && !JOBS.isEmpty()) {
            Job job = JOBS.peekFirst();
            if (level == null) {
                // The Road's dimension is not there (its data pack is gone): the cell stays in the ledger, and nothing is cleared that is not there.
                JOBS.removeFirst();
                continue;
            }
            while (budget > 0 && job.cursor < job.chunks.length) {
                long key = job.chunks[job.cursor++];
                try {
                    budget -= clear(level, job, key);
                } catch (RuntimeException e) {
                    Enhanced.LOGGER.error("The Witches' Road could not clear a chunk of cell {}", job.cell, e);
                    job.failed.add(key);
                    budget -= CHUNK_COST;
                }
            }
            if (job.cursor < job.chunks.length) {
                break;
            }
            if (job.entitiesAt == 0L) {
                job.entitiesAt = level.getGameTime() + (immediate ? 0 : ENTITY_DELAY_TICKS);
            }
            if (!immediate && level.getGameTime() < job.entitiesAt) {
                break;
            }
            while (budget > 0 && job.entityCursor < job.all.length) {
                try {
                    removeEntities(level, job.all[job.entityCursor]);
                } catch (RuntimeException e) {
                    Enhanced.LOGGER.error("The Witches' Road could not remove what stood in a chunk of cell {}", job.cell, e);
                }
                job.entityCursor++;
                budget -= CHUNK_COST;
            }
            if (job.entityCursor < job.all.length) {
                break;
            }
            JOBS.removeFirst();
            if (!job.failed.isEmpty() && job.attempt + 1 < ATTEMPTS) {
                long[] again = job.failed.stream().mapToLong(Long::longValue).toArray();
                JOBS.addLast(new Job(job.cell, job.owner, again, job.all, job.minY, job.maxY, job.attempt + 1));
                continue;
            }
            release(level, job.owner, job.all);
            if (job.failed.isEmpty()) {
                RoadSave.get(server).release(job.cell);
            } else {
                Enhanced.LOGGER.error("The Witches' Road could not clear cell {} and keeps it out of use until the next start", job.cell);
            }
        }
    }

    /** Clears one chunk of a job; returns what it cost. */
    private static int clear(ServerLevel level, Job job, long chunkKey) {
        if (fault != null && fault.test(chunkKey)) {
            throw new IllegalStateException("A fault the test made");
        }
        ChunkPos chunkPos = new ChunkPos(chunkKey);
        LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
        int cost = CHUNK_COST;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int low = Math.max(job.minY, level.getMinBuildHeight());
        int high = Math.min(job.maxY, level.getMaxBuildHeight() - 1);
        for (int y = low; y <= high; ) {
            int sectionIndex = chunk.getSectionIndex(y);
            LevelChunkSection section = chunk.getSection(sectionIndex);
            int sectionTop = chunk.getSectionYFromSectionIndex(sectionIndex) * 16 + 15;
            int top = Math.min(high, sectionTop);
            if (!section.hasOnlyAir()) {
                for (int by = y; by <= top; by++) {
                    for (int bx = 0; bx < 16; bx++) {
                        for (int bz = 0; bz < 16; bz++) {
                            if (!section.getBlockState(bx, by & 15, bz).isAir()) {
                                pos.set(chunkPos.getMinBlockX() + bx, by, chunkPos.getMinBlockZ() + bz);
                                level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
                                cost++;
                            }
                        }
                    }
                }
            }
            y = top + 1;
        }
        return cost;
    }

    /** Removes what stands in the chunk (a boat, labels, an item): last, because taking a block away may spill what it held (a chest's contents) as items. */
    private static void removeEntities(ServerLevel level, long chunkKey) {
        ChunkPos chunkPos = new ChunkPos(chunkKey);
        level.getChunk(chunkPos.x, chunkPos.z);
        AABB area = new AABB(chunkPos.getMinBlockX(), level.getMinBuildHeight() - 64, chunkPos.getMinBlockZ(),
                chunkPos.getMaxBlockX() + 1, level.getMaxBuildHeight() + 64, chunkPos.getMaxBlockZ() + 1);
        for (Entity entity : level.getEntities((Entity) null, area, e -> !(e instanceof Player))) {
            entity.discard();
        }
    }

    /** Forgets every job (the server stopped). */
    public static void clear() {
        JOBS.clear();
    }

    private RoadSweeper() {
    }
}
