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

    private static final class Job {
        final int cell;
        final UUID owner;
        final long[] chunks;
        final int minY;
        final int maxY;
        int cursor;

        Job(RoadSave.Cell cell) {
            this.cell = cell.index();
            this.owner = cell.owner();
            this.chunks = cell.chunks();
            this.minY = cell.minY();
            this.maxY = cell.maxY();
        }
    }

    private static final Deque<Job> JOBS = new ArrayDeque<>();

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
        run(server, WitchesRoadConfig.REMOVE_BLOCKS_PER_TICK.get());
    }

    /** Does all the work now (a stopping server, a test). */
    public static void drain(MinecraftServer server) {
        while (!JOBS.isEmpty()) {
            run(server, Integer.MAX_VALUE / 2);
        }
    }

    private static void run(MinecraftServer server, int budget) {
        ServerLevel level = RoadDimension.level(server);
        while (budget > 0 && !JOBS.isEmpty()) {
            Job job = JOBS.peekFirst();
            if (level != null) {
                try {
                    while (budget > 0 && job.cursor < job.chunks.length) {
                        budget -= clear(level, job, job.chunks[job.cursor++]);
                    }
                } catch (RuntimeException e) {
                    Enhanced.LOGGER.error("The Witches' Road could not clear cell {}", job.cell, e);
                    job.cursor = job.chunks.length;
                }
            } else {
                job.cursor = job.chunks.length;
            }
            if (job.cursor >= job.chunks.length) {
                JOBS.removeFirst();
                if (level != null) {
                    release(level, job.owner, job.chunks);
                }
                RoadSave.get(server).release(job.cell);
            }
        }
    }

    /** Clears one chunk of a job; returns what it cost. */
    private static int clear(ServerLevel level, Job job, long chunkKey) {
        ChunkPos chunkPos = new ChunkPos(chunkKey);
        LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
        int cost = CHUNK_COST;
        AABB area = new AABB(chunkPos.getMinBlockX(), level.getMinBuildHeight() - 64, chunkPos.getMinBlockZ(),
                chunkPos.getMaxBlockX() + 1, level.getMaxBuildHeight() + 64, chunkPos.getMaxBlockZ() + 1);
        for (Entity entity : level.getEntities((Entity) null, area, e -> !(e instanceof Player))) {
            entity.discard();
        }
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

    /** Forgets every job (the server stopped). */
    public static void clear() {
        JOBS.clear();
    }

    private RoadSweeper() {
    }
}
