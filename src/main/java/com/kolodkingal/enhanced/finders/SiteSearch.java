package com.kolodkingal.enhanced.finders;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.LocatorConfig;
import com.kolodkingal.enhanced.config.core.ConfigSpec;
import com.kolodkingal.enhanced.thanos.StoneKind;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.LongPredicate;

/**
 * The search for the Infinity Stones' sites (3.7, lane L5): incremental, budgeted, cached, skipping spent sites ({@link
 * SpentSites}). The Stone Locator and the amplified Cerebro ask it ({@link #ask}, {@link #nearest}).
 *
 * <p><b>How it searches</b> (as vanilla's {@code /locate}): for a structure of a random-spread set, the rings of
 * placement cells round the asker, each cell's one potential chunk; the structure check of the level answers "no start
 * there" (free), "a start is there" (free) or "the chunk's structure starts must be loaded" (the expensive part). For
 * concentric rings (the strongholds) the precomputed ring chunks, nearest first. After the first ring with a find, one
 * more ring is searched and the nearest is the answer; at the radius ({@code searchRadius} 4096, the Nether's 1024) it
 * answers "too far".
 *
 * <p><b>The budget</b>: at most {@code startChecksPerTick} structure-start loads per tick on the whole server, and at
 * most {@link #CELLS_PER_TICK} cells; the searches (one per player and site kind, {@link Job}) take turns. A load's
 * answer is kept for every player ({@link #SHARED}). <b>The cache</b>: an answer is kept for {@code recheckTicks}, or
 * until its asker moves {@code moveRecheck} blocks or its site is spent; a search nobody asked about for {@link
 * #FORGET_TICKS} stops (the Locator searches only while held).
 */
public final class SiteSearch {
    /** The most cells looked at per tick on the whole server (cheap, but not free). */
    public static final int CELLS_PER_TICK = 2048;
    /** The most cells one search looks at before the next takes its turn. */
    static final int CELLS_PER_TURN = 64;
    /** A search whose asker stopped asking this long ago stops. */
    public static final int FORGET_TICKS = 100;

    // ------------------------------------------------------------------------------------------------------------
    // Answers

    /**
     * What a search says: {@code LocatorState.SEARCHING}, {@code FOUND} (then where the site is and the chunk its
     * structure starts in) or {@code TOO_FAR}.
     */
    public record Answer(byte status, Optional<BlockPos> pos, long startChunk) {
        public static final Answer SEARCHING = new Answer(LocatorState.SEARCHING, Optional.empty(), 0L);
        public static final Answer TOO_FAR = new Answer(LocatorState.TOO_FAR, Optional.empty(), 0L);

        public static Answer found(BlockPos pos, long startChunk) {
            return new Answer(LocatorState.FOUND, Optional.of(pos), startChunk);
        }

        public boolean isFound() {
            return status == LocatorState.FOUND;
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Probes: where the starts can be, and the questions asked about a chunk

    /** What the level says about a chunk without loading anything. */
    public enum Check {
        /** No start of the structure there. */
        ABSENT,
        /** A start is there. */
        PRESENT,
        /** Only loading the chunk's structure starts can tell (counted against the budget). */
        LOAD
    }

    /**
     * One structure's placement in one level. A grid ({@link #spacing} &gt; 0: random spread, one potential chunk per
     * cell) or a list of chunks ({@link #candidates}: the strongholds' rings).
     */
    public interface Probe {
        /** The size of a placement cell in chunks; 0 for a list of {@link #candidates}. */
        int spacing();

        /** The one chunk of the cell {@code (cellX, cellZ)} where a start may be. */
        ChunkPos potential(int cellX, int cellZ);

        /** The chunks of a list placement (strongholds); empty for a grid. */
        default List<ChunkPos> candidates() {
            return List.of();
        }

        Check check(ChunkPos chunk);

        /** Loads the chunk's structure starts (the expensive part): whether a start is there. */
        boolean load(ChunkPos chunk);

        /** Where the Locator points for a start in {@code chunk}. */
        BlockPos locate(ChunkPos chunk);

        /** What its loads are kept under for every player ({@link #SHARED}), or null: not kept. */
        @Nullable
        default Object key() {
            return null;
        }
    }

    /** The loads already made, by probe key: chunk → whether a start was there. */
    private static final Map<Object, Long2BooleanOpenHashMap> SHARED = new HashMap<>();

    /**
     * Where the probes of a search come from: the level's own placements ({@link #placements}). The GameTest world
     * (flat) places no stone site, so GameTests put stand-ins here and put this one back when they end.
     */
    private static BiFunction<ServerLevel, TagKey<Structure>, List<Probe>> prober = SiteSearch::placements;

    /** Puts where the probes come from (null: the level's own placements). */
    public static void prober(@Nullable BiFunction<ServerLevel, TagKey<Structure>, List<Probe>> source) {
        prober = source == null ? SiteSearch::placements : source;
    }

    /** The probes of the structures of {@code tag} in {@code level}. */
    public static List<Probe> probes(ServerLevel level, TagKey<Structure> tag) {
        return prober.apply(level, tag);
    }

    /** The probes of the structures of {@code tag} as {@code level} places them (none where it never does). */
    public static List<Probe> placements(ServerLevel level, TagKey<Structure> tag) {
        Optional<HolderSet.Named<Structure>> set = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .getTag(tag);
        if (set.isEmpty()) {
            return List.of();
        }
        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        List<Probe> probes = new ArrayList<>();
        for (Holder<Structure> structure : set.get()) {
            for (StructurePlacement placement : state.getPlacementsForStructure(structure)) {
                if (placement instanceof RandomSpreadStructurePlacement spread) {
                    probes.add(new GridProbe(level, structure, spread, state.getLevelSeed()));
                } else if (placement instanceof ConcentricRingsStructurePlacement rings) {
                    List<ChunkPos> chunks = state.getRingPositionsFor(rings);
                    probes.add(new RingProbe(level, structure, rings, chunks == null ? List.of() : chunks));
                }
            }
        }
        return probes;
    }

    /** A structure of a random-spread set. */
    private record GridProbe(ServerLevel level, Holder<Structure> structure, RandomSpreadStructurePlacement placement,
                             long seed) implements Probe {
        @Override
        public int spacing() {
            return placement.spacing();
        }

        @Override
        public ChunkPos potential(int cellX, int cellZ) {
            return placement.getPotentialStructureChunk(seed, cellX * placement.spacing(),
                    cellZ * placement.spacing());
        }

        @Override
        public Check check(ChunkPos chunk) {
            return SiteSearch.check(level.structureManager(), structure, placement, chunk);
        }

        @Override
        public boolean load(ChunkPos chunk) {
            return SiteSearch.load(level, structure, chunk);
        }

        @Override
        public BlockPos locate(ChunkPos chunk) {
            return placement.getLocatePos(chunk);
        }

        @Override
        public Object key() {
            return List.of(level.dimension(), structure.unwrapKey().map(Object.class::cast).orElse(structure));
        }
    }

    /** A structure of a concentric-rings set (the strongholds). */
    private record RingProbe(ServerLevel level, Holder<Structure> structure,
                             ConcentricRingsStructurePlacement placement, List<ChunkPos> candidates) implements Probe {
        @Override
        public int spacing() {
            return 0;
        }

        @Override
        public ChunkPos potential(int cellX, int cellZ) {
            return new ChunkPos(cellX, cellZ);
        }

        @Override
        public Check check(ChunkPos chunk) {
            return SiteSearch.check(level.structureManager(), structure, placement, chunk);
        }

        @Override
        public boolean load(ChunkPos chunk) {
            return SiteSearch.load(level, structure, chunk);
        }

        @Override
        public BlockPos locate(ChunkPos chunk) {
            return placement.getLocatePos(chunk);
        }

        @Override
        public Object key() {
            return List.of(level.dimension(), structure.unwrapKey().map(Object.class::cast).orElse(structure));
        }
    }

    private static Check check(StructureManager manager, Holder<Structure> structure, StructurePlacement placement,
                               ChunkPos chunk) {
        StructureCheckResult result = manager.checkStructurePresence(chunk, structure.value(), placement, false);
        return switch (result) {
            case START_PRESENT -> Check.PRESENT;
            case CHUNK_LOAD_NEEDED -> Check.LOAD;
            default -> Check.ABSENT;
        };
    }

    private static boolean load(ServerLevel level, Holder<Structure> structure, ChunkPos chunk) {
        ChunkAccess access = level.getChunk(chunk.x, chunk.z, ChunkStatus.STRUCTURE_STARTS);
        StructureStart start = level.structureManager().getStartForStructure(SectionPos.bottomOf(access),
                structure.value(), access);
        return start != null && start.isValid();
    }

    // ------------------------------------------------------------------------------------------------------------
    // A search

    /** One search: from a chunk, within a radius, through one or more probes, skipping some starts (spent sites). */
    public static final class Job {
        private final ChunkPos origin;
        private final long radiusSq;
        private final int radiusChunks;
        private final LongPredicate skip;
        private final List<Cursor> cursors = new ArrayList<>();
        private int loads;
        private boolean cancelled;
        @Nullable
        private Answer answer;

        Job(ChunkPos origin, int radiusBlocks, LongPredicate skip) {
            this.origin = origin;
            this.radiusSq = (long) radiusBlocks * radiusBlocks;
            this.radiusChunks = (radiusBlocks >> 4) + 1;
            this.skip = skip;
        }

        /** Whether it has its answer (or was stopped). */
        public boolean done() {
            return answer != null || cancelled;
        }

        /** Its answer: {@link Answer#SEARCHING} until it is done. */
        public Answer answer() {
            return answer == null ? Answer.SEARCHING : answer;
        }

        /** The structure-start loads it made. */
        public int loads() {
            return loads;
        }

        /** Stops it (its asker went away); it never answers. */
        public void cancel() {
            cancelled = true;
        }

        private long distSq(ChunkPos chunk, Probe probe) {
            BlockPos at = probe.locate(chunk);
            long dx = at.getX() - origin.getMiddleBlockX();
            long dz = at.getZ() - origin.getMiddleBlockZ();
            return dx * dx + dz * dz;
        }

        /** Takes its turn: up to {@link #CELLS_PER_TURN} cells, within what is left of the tick's budget. */
        private void step(Budget budget) {
            int looked = 0;
            while (!done() && looked < CELLS_PER_TURN && budget.cells > 0) {
                boolean allDone = true;
                boolean waiting = false;
                for (Cursor cursor : cursors) {
                    if (cursor.finished) {
                        continue;
                    }
                    allDone = false;
                    Cursor.Step step = cursor.next(this, budget);
                    if (step == Cursor.Step.WAIT) {
                        waiting = true;
                    } else {
                        looked++;
                        budget.cells--;
                    }
                }
                if (allDone) {
                    finish();
                    return;
                }
                if (waiting && budget.loads <= 0) {
                    return;
                }
            }
        }

        private void finish() {
            Cursor best = null;
            for (Cursor cursor : cursors) {
                if (cursor.best != null && (best == null || cursor.bestDist < best.bestDist)) {
                    best = cursor;
                }
            }
            answer = best == null ? Answer.TOO_FAR : Answer.found(best.probe.locate(best.best), best.best.toLong());
        }

        /** Whether a start is in {@code chunk} (free when the level or a shared load knows; else a counted load). */
        @Nullable
        private Boolean present(Probe probe, ChunkPos chunk, Budget budget) {
            Object key = probe.key();
            Long2BooleanOpenHashMap shared = key == null ? null : SHARED.get(key);
            if (shared != null && shared.containsKey(chunk.toLong())) {
                return shared.get(chunk.toLong());
            }
            Check check = probe.check(chunk);
            if (check != Check.LOAD) {
                return check == Check.PRESENT;
            }
            if (budget.loads <= 0) {
                return null;
            }
            budget.loads--;
            loads++;
            boolean there = probe.load(chunk);
            if (key != null) {
                SHARED.computeIfAbsent(key, k -> new Long2BooleanOpenHashMap()).put(chunk.toLong(), there);
            }
            return there;
        }

        /** Where one probe's search stands. */
        private abstract static class Cursor {
            enum Step { LOOKED, WAIT }

            final Probe probe;
            boolean finished;
            @Nullable
            ChunkPos best;
            long bestDist = Long.MAX_VALUE;

            Cursor(Probe probe) {
                this.probe = probe;
            }

            abstract Step next(Job job, Budget budget);

            /** Considers a chunk: null when it must wait for the budget. */
            @Nullable
            Boolean consider(Job job, ChunkPos chunk, Budget budget) {
                long dist = job.distSq(chunk, probe);
                if (dist > job.radiusSq || dist >= bestDist || job.skip.test(chunk.toLong())) {
                    return false;
                }
                Boolean there = job.present(probe, chunk, budget);
                if (there == null) {
                    return null;
                }
                if (there) {
                    best = chunk;
                    bestDist = dist;
                }
                return there;
            }
        }

        /** Rings of cells round the origin's cell; one more ring after the first find. */
        private static final class GridCursor extends Cursor {
            private final int cellX;
            private final int cellZ;
            private final int lastRing;
            private int ring;
            private int index;
            private int foundRing = -1;

            GridCursor(Probe probe, ChunkPos origin, int radiusChunks) {
                super(probe);
                int spacing = probe.spacing();
                cellX = Math.floorDiv(origin.x, spacing);
                cellZ = Math.floorDiv(origin.z, spacing);
                lastRing = radiusChunks / spacing + 2;
            }

            @Override
            Step next(Job job, Budget budget) {
                int dx;
                int dz;
                if (ring == 0) {
                    dx = 0;
                    dz = 0;
                } else {
                    int side = index / (2 * ring);
                    int along = index % (2 * ring);
                    switch (side) {
                        case 0 -> {
                            dx = -ring + along;
                            dz = -ring;
                        }
                        case 1 -> {
                            dx = ring;
                            dz = -ring + along;
                        }
                        case 2 -> {
                            dx = ring - along;
                            dz = ring;
                        }
                        default -> {
                            dx = -ring;
                            dz = ring - along;
                        }
                    }
                }
                Boolean there = consider(job, probe.potential(cellX + dx, cellZ + dz), budget);
                if (there == null) {
                    return Step.WAIT;
                }
                if (there && foundRing < 0) {
                    foundRing = ring;
                }
                index++;
                if (index >= Math.max(1, 8 * ring)) {
                    index = 0;
                    if ((foundRing >= 0 && ring >= foundRing + 1) || ring >= lastRing) {
                        finished = true;
                    }
                    ring++;
                }
                return Step.LOOKED;
            }
        }

        /** A list of chunks, nearest first; the first with a start ends it. */
        private static final class ListCursor extends Cursor {
            private final Iterator<ChunkPos> order;
            @Nullable
            private ChunkPos peeked;

            ListCursor(Probe probe, Job job) {
                super(probe);
                List<ChunkPos> sorted = new ArrayList<>(probe.candidates());
                sorted.sort(Comparator.comparingLong(c -> job.distSq(c, probe)));
                order = sorted.iterator();
            }

            @Override
            Step next(Job job, Budget budget) {
                if (!order.hasNext()) {
                    finished = true;
                    return Step.LOOKED;
                }
                ChunkPos chunk = peeked != null ? peeked : order.next();
                Boolean there = consider(job, chunk, budget);
                if (there == null) {
                    peeked = chunk;
                    return Step.WAIT;
                }
                peeked = null;
                if (there) {
                    finished = true;
                }
                return Step.LOOKED;
            }
        }
    }

    /** What is left of this tick's budget. */
    private static final class Budget {
        int loads;
        int cells;

        Budget(int loads, int cells) {
            this.loads = loads;
            this.cells = cells;
        }
    }

    private static final Deque<Job> QUEUE = new ArrayDeque<>();

    /**
     * Starts a search (GameTests start their own with stand-in probes; players' searches start through {@link #ask}).
     * It runs on the server's ticks ({@link #tick}).
     */
    public static Job start(ChunkPos origin, int radiusBlocks, List<Probe> probes, LongPredicate skip) {
        Job job = new Job(origin, radiusBlocks, skip);
        for (Probe probe : probes) {
            job.cursors.add(probe.spacing() > 0 ? new Job.GridCursor(probe, origin, job.radiusChunks)
                    : new Job.ListCursor(probe, job));
        }
        job.answer = job.cursors.isEmpty() ? Answer.TOO_FAR : null;
        if (!job.done()) {
            QUEUE.addLast(job);
        }
        return job;
    }

    /** One server tick of the searches: the budget shared in turns (a few rounds, while the cells last). */
    public static void tick() {
        Budget budget = new Budget(config(LocatorConfig.START_CHECKS_PER_TICK, 2), CELLS_PER_TICK);
        for (int round = 0; round < 4 && !QUEUE.isEmpty() && budget.cells > 0; round++) {
            int before = budget.cells;
            int jobs = QUEUE.size();
            for (int i = 0; i < jobs && budget.cells > 0; i++) {
                Job job = QUEUE.pollFirst();
                if (job.done()) {
                    continue;
                }
                job.step(budget);
                if (!job.done()) {
                    QUEUE.addLast(job);
                }
            }
            if (budget.cells == before) {
                break; // every search left waits for a load: the next tick
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Players' searches

    /** One player's search for one kind of site in one level. */
    private static final class Asked {
        final ResourceKey<Level> dimension;
        @Nullable
        Job job;
        Answer answer = Answer.SEARCHING;
        BlockPos origin;
        long startedAt;
        long lastAsked;

        Asked(ResourceKey<Level> dimension) {
            this.dimension = dimension;
        }
    }

    /** Per player: the searches by what they look for ("dimension|tag"). */
    private static final Map<UUID, Map<String, Asked>> ASKED = new HashMap<>();

    /**
     * What the search for the structures of {@code tag} round {@code player} says now (it starts or goes on in the
     * background): {@code kind}'s spent sites skipped (null: none are), within {@code radiusBlocks}.
     */
    public static Answer ask(ServerPlayer player, TagKey<Structure> tag, @Nullable StoneKind kind, int radiusBlocks) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        String key = level.dimension().location() + "|" + tag.location();
        Asked asked = ASKED.computeIfAbsent(player.getUUID(), id -> new HashMap<>())
                .computeIfAbsent(key, k -> new Asked(level.dimension()));
        asked.lastAsked = now;
        if (asked.job != null && asked.job.done()) {
            asked.answer = asked.job.answer();
            asked.job = null;
        }
        boolean spent = asked.answer.isFound() && kind != null
                && SpentSites.isSpent(level, kind, asked.answer.startChunk());
        if (spent) {
            asked.answer = Answer.SEARCHING;
        }
        int move = config(LocatorConfig.MOVE_RECHECK, 64);
        boolean stale = asked.origin == null || spent
                || now - asked.startedAt >= config(LocatorConfig.RECHECK_TICKS, 600)
                || asked.origin.distSqr(player.blockPosition()) > (double) move * move;
        if (stale && asked.job == null) {
            asked.origin = player.blockPosition();
            asked.startedAt = now;
            LongPredicate skip = kind == null ? chunk -> false : chunk -> SpentSites.isSpent(level, kind, chunk);
            asked.job = start(new ChunkPos(asked.origin), radiusBlocks, probes(level, tag), skip);
            if (asked.job.done()) {
                asked.answer = asked.job.answer();
                asked.job = null;
            }
        }
        return asked.answer;
    }

    /**
     * The nearest site of {@code kind} found for {@code player} so far in his own level (the search goes on in the
     * background), if any: Cerebro's diamonds. Only the kinds whose sites stand in his level have one.
     */
    public static Optional<GlobalPos> nearest(ServerPlayer player, StoneKind kind) {
        LocatorRoutes.Route route = LocatorRoutes.route(LocatorRoutes.realm(player.level().dimension()), kind, true);
        if (route.type() != LocatorRoutes.Type.SEARCH || route.sites() == null) {
            return Optional.empty();
        }
        Answer answer = ask(player, route.sites(), kind, radius(player.level().dimension()));
        return answer.pos().map(pos -> GlobalPos.of(player.level().dimension(), pos));
    }

    /** How far the search goes in a level (blocks). */
    public static int radius(ResourceKey<Level> dimension) {
        return dimension == Level.NETHER ? config(LocatorConfig.NETHER_SEARCH_RADIUS, 1024)
                : config(LocatorConfig.SEARCH_RADIUS, 4096);
    }

    /** A site became spent: every answer that pointed to it is asked again. */
    static void spent(ServerLevel level, long startChunk) {
        for (Map<String, Asked> map : ASKED.values()) {
            for (Asked asked : map.values()) {
                if (asked.dimension == level.dimension() && asked.answer.isFound()
                        && asked.answer.startChunk() == startChunk) {
                    asked.answer = Answer.SEARCHING;
                    asked.origin = null;
                }
            }
        }
    }

    /** Searches nobody asked about for a while stop. */
    public static void forgetIdle(long now) {
        for (Map<String, Asked> map : ASKED.values()) {
            map.values().removeIf(asked -> {
                if (now - asked.lastAsked > FORGET_TICKS) {
                    if (asked.job != null) {
                        asked.job.cancel();
                    }
                    return true;
                }
                return false;
            });
        }
        ASKED.values().removeIf(Map::isEmpty);
    }

    /** The player left: his searches stop. */
    public static void forget(UUID player) {
        Map<String, Asked> map = ASKED.remove(player);
        if (map != null) {
            for (Asked asked : map.values()) {
                if (asked.job != null) {
                    asked.job.cancel();
                }
            }
        }
    }

    /** Everything forgotten (the server stopped). */
    public static void clear() {
        QUEUE.clear();
        ASKED.clear();
        SHARED.clear();
    }

    /** How many searches are running (GameTests). */
    public static int running() {
        return (int) QUEUE.stream().filter(j -> !j.done()).count();
    }

    private static int config(ConfigSpec.IntValue value, int fallback) {
        return EnhancedConfig.SPEC.isLoaded() ? value.get() : fallback;
    }

    private SiteSearch() {
    }
}
