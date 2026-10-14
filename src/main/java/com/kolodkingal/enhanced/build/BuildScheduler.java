package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.BuildConfig;
import com.kolodkingal.enhanced.ultron.SokoviaJob;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.LongSupplier;

/**
 * Every Build Magic spell at work or waiting on the server (D-B 11.7): the builders' queues, the budget all spells share
 * in each server tick, the turns they take, and what happens to them when the server stops.
 *
 * <ul>
 *     <li><b>Where</b>: each level's jobs run in its {@code LevelTickEvent.Post} ({@link #tickLevel}), inside the level
 *     tick, as the Sokovia recovery does; the budget belongs to the server tick and is set at
 *     {@code ServerTickEvent.Pre} ({@link #startTick}), so all levels together stay within it.</li>
 *     <li><b>The budget</b> ({@link Budget}): {@code millisPerTick} (8 ms), {@code blocksPerTick} (20,000 writes; a read
 *     costs a quarter of a write, a cell that changes nothing an eighth) and {@code sectionsPerTick} (16 sections changed
 *     this tick); whichever runs out first ends the tick's work. All halved while the average tick is over 45 ms, with a
 *     floor of 1 ms and 256 writes.</li>
 *     <li><b>Fair shares</b>: each builder's queue runs its first job only, so a builder's spells never run side by side
 *     and undo goes in the order of casting. The running jobs take turns, starting with a different one each tick; each
 *     gets the budget that is left divided by the jobs still to go (a Scarlet Witch's spell inside her own Hex counts
 *     {@code hexBoost} times), and what one does not use passes to the next.</li>
 *     <li><b>The server's stop</b> (R55, {@link #onServerStopping}): the spells left are finished while their blocks
 *     left add up to at most {@code finishOnStopBlocks}, the largest stopped first (and undoable).</li>
 * </ul>
 *
 * <p>Tests drive it without the server's ticks: {@link #manual} keeps a player's jobs out of the ticks (with a "loaded"
 * predicate of the test's own), and {@link #step} runs them with a budget of the test's.
 */
public final class BuildScheduler {
    /** The least time a slowed tick keeps (D-B 11.7), so a spell always moves. */
    public static final long FLOOR_NANOS = 1_000_000L;
    /** The fewest writes a slowed tick keeps. */
    public static final int FLOOR_WRITES = 256;
    /** The most particles all spells together show in a tick (D-B 7.2). */
    public static final int SPARKS = 64;

    private static final Map<UUID, Deque<BuildJob>> QUEUES = new LinkedHashMap<>();
    private static final Set<UUID> MANUAL = new HashSet<>();
    private static final Map<UUID, BiPredicate<Integer, Integer>> LOADED = new HashMap<>();
    /** The jobs that still take a turn in this server tick, in turn order. */
    private static final List<BuildJob> TURNS = new ArrayList<>();
    private static @Nullable Tick tick;
    private static double weightLeft;
    private static int rotation;

    // ------------------------------------------------------------------------------------------------------------
    // The budget

    /**
     * What all spells together may use in a tick.
     *
     * @param nanos         time
     * @param writes        block writes (a read is a quarter, a cell that changes nothing an eighth)
     * @param sections      sections changed this tick
     * @param minSpellTicks the fewest ticks a spell takes ({@code build_magic.minSpellTicks}: its pace)
     * @param clock         now, in nanoseconds
     * @param averageTick   the server's average tick, in nanoseconds (over 45 ms halves everything)
     * @param floorNanos    the least time a slowed tick keeps
     * @param floorWrites   the fewest writes a slowed tick keeps
     */
    public record Budget(long nanos, int writes, int sections, int minSpellTicks, LongSupplier clock, LongSupplier averageTick, long floorNanos,
                         int floorWrites) {
        /** The server's budget from {@code [build_magic]}, with its clock and floors. */
        public static Budget of(MinecraftServer server) {
            return new Budget(BuildConfig.MILLIS_PER_TICK.get() * 1_000_000L, BuildConfig.BLOCKS_PER_TICK.get(), BuildConfig.SECTIONS_PER_TICK.get(),
                    BuildConfig.MIN_SPELL_TICKS.get(), System::nanoTime, server::getAverageTickTimeNanos, FLOOR_NANOS, FLOOR_WRITES);
        }

        /**
         * A test's budget: {@code writes} a tick, {@code sections} sections, the pace of {@code minSpellTicks}, time that
         * never runs out, a server that is never slow, and no floors.
         */
        public static Budget of(int writes, int sections, int minSpellTicks) {
            return new Budget(60_000_000_000L, writes, sections, minSpellTicks, System::nanoTime, () -> 0L, 0L, 0);
        }

        /** The same budget with this clock and this average tick. */
        public Budget withClock(LongSupplier clock, LongSupplier averageTick) {
            return new Budget(nanos, writes, sections, minSpellTicks, clock, averageTick, floorNanos, floorWrites);
        }

        /** A tick's worth of it, halved while the server is slow (never below its floors, never above itself). */
        public Tick start() {
            boolean slow = averageTick.getAsLong() > SokoviaJob.SLOW_TICK_NANOS;
            long time = slow ? Math.min(nanos, Math.max(floorNanos, nanos / 2)) : nanos;
            long units = (slow ? Math.min(writes, Math.max(floorWrites, writes / 2)) : writes) * (long) BuildJob.WRITE;
            int newSections = slow ? Math.max(1, sections / 2) : sections;
            return new Tick(clock, clock.getAsLong() + time, units, newSections, minSpellTicks);
        }
    }

    /** One server tick's budget, as the jobs use it up. */
    public static final class Tick {
        private final LongSupplier clock;
        private final long deadline;
        private long units;
        private int sections;
        private int sparks = SPARKS;
        private final int minSpellTicks;
        private final Map<ServerLevel, LongOpenHashSet> changed = new IdentityHashMap<>();

        Tick(LongSupplier clock, long deadline, long units, int sections, int minSpellTicks) {
            this.clock = clock;
            this.deadline = deadline;
            this.units = units;
            this.sections = sections;
            this.minSpellTicks = minSpellTicks;
        }

        /** Whether the tick's time is up. */
        public boolean timeUp() {
            return clock.getAsLong() >= deadline;
        }

        /** The units left (a write is {@link BuildJob#WRITE}). */
        public long units() {
            return units;
        }

        /** The fewest ticks a spell takes. */
        public int minSpellTicks() {
            return minSpellTicks;
        }

        /**
         * Whether a cell of the section {@code key} may change this tick: a section already changed this tick is free,
         * a new one takes one of the tick's sections (false when none is left).
         */
        public boolean section(ServerLevel level, long key) {
            LongOpenHashSet seen = changed.computeIfAbsent(level, l -> new LongOpenHashSet());
            if (seen.contains(key)) {
                return true;
            }
            if (sections <= 0) {
                return false;
            }
            sections--;
            seen.add(key);
            return true;
        }

        /** Takes one of the tick's particles for all spells (at most 64 a tick, D-B 7.2); false when none is left. */
        public boolean spark() {
            if (sparks <= 0) {
                return false;
            }
            sparks--;
            return true;
        }

        /** How many sections were changed this tick, in every level. */
        public int sectionsChanged() {
            int changed = 0;
            for (LongOpenHashSet seen : this.changed.values()) {
                changed += seen.size();
            }
            return changed;
        }

        void use(long used) {
            units = Math.max(0, units - used);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // The queues

    /** Queues a job behind the builder's others ({@code first}: before them, as the undo of a stopped spell). */
    public static void queue(BuildJob job, boolean first) {
        Deque<BuildJob> queue = QUEUES.computeIfAbsent(job.caster(), id -> new ArrayDeque<>());
        if (first) {
            queue.addFirst(job);
        } else {
            queue.addLast(job);
        }
    }

    /** The job at work for {@code player}, or null. */
    public static @Nullable BuildJob working(UUID player) {
        Deque<BuildJob> queue = QUEUES.get(player);
        return queue == null ? null : queue.peekFirst();
    }

    /** How many of the player's jobs wait behind the one at work. */
    public static int waiting(UUID player) {
        Deque<BuildJob> queue = QUEUES.get(player);
        return queue == null ? 0 : Math.max(0, queue.size() - 1);
    }

    /** The player's jobs, the one at work first. */
    public static List<BuildJob> jobs(UUID player) {
        Deque<BuildJob> queue = QUEUES.get(player);
        return queue == null ? List.of() : List.copyOf(queue);
    }

    /** Every job on the server, the builders in the order they first cast, each one's at work first. */
    public static List<BuildJob> all() {
        List<BuildJob> all = new ArrayList<>();
        for (Deque<BuildJob> queue : QUEUES.values()) {
            all.addAll(queue);
        }
        return all;
    }

    /** How many jobs work or wait on the server ({@code maxSpellsTotal}, C11). */
    public static int total() {
        int total = 0;
        for (Deque<BuildJob> queue : QUEUES.values()) {
            total += queue.size();
        }
        return total;
    }

    /** Drops the newest waiting job of the player (Ctrl + Z, D-B 12.2): nothing of it ever happened. Null if none. */
    public static @Nullable BuildJob dropNewestWaiting(UUID player) {
        Deque<BuildJob> queue = QUEUES.get(player);
        if (queue == null || queue.size() < 2) {
            return null;
        }
        BuildJob newest = queue.pollLast();
        newest.drop();
        return newest;
    }

    /** Drops one waiting job (a dab of a stroke that Ctrl + Z ends): nothing of it ever happens. */
    public static void drop(BuildJob job) {
        Deque<BuildJob> queue = QUEUES.get(job.caster());
        if (queue != null && queue.peekFirst() != job && queue.remove(job)) {
            job.drop();
        }
    }

    /** Drops every waiting job of the player (his power changed): the one at work goes on. How many were dropped. */
    public static int dropWaiting(UUID player) {
        int dropped = 0;
        while (dropNewestWaiting(player) != null) {
            dropped++;
        }
        return dropped;
    }

    /** Takes a finished job off its queue. */
    static void remove(BuildJob job) {
        Deque<BuildJob> queue = QUEUES.get(job.caster());
        if (queue != null) {
            queue.remove(job);
            if (queue.isEmpty()) {
                QUEUES.remove(job.caster());
            }
        }
        BuildMagic.sync(job.level().getServer(), job.caster());
    }

    // ------------------------------------------------------------------------------------------------------------
    // The ticks

    /** {@code ServerTickEvent.Pre}: the tick's budget, and the jobs that take turns in it. */
    public static void startTick(MinecraftServer server) {
        TURNS.clear();
        if (QUEUES.isEmpty() || !EnhancedConfig.SPEC.isLoaded()) {
            tick = null;
            return;
        }
        tick = Budget.of(server).start();
        List<BuildJob> turns = new ArrayList<>();
        for (Map.Entry<UUID, Deque<BuildJob>> entry : QUEUES.entrySet()) {
            if (!MANUAL.contains(entry.getKey()) && !entry.getValue().isEmpty()) {
                turns.add(entry.getValue().peekFirst());
            }
        }
        if (!turns.isEmpty()) {
            int start = Math.floorMod(rotation++, turns.size());
            TURNS.addAll(turns.subList(start, turns.size()));
            TURNS.addAll(turns.subList(0, start));
        }
        weightLeft = 0.0;
        for (BuildJob job : TURNS) {
            weightLeft += job.weight();
        }
    }

    /** {@code LevelTickEvent.Post}: the turns of this level's jobs. */
    public static void tickLevel(ServerLevel level) {
        if (tick == null || TURNS.isEmpty()) {
            return;
        }
        for (Iterator<BuildJob> it = TURNS.iterator(); it.hasNext(); ) {
            BuildJob job = it.next();
            if (job.level() != level) {
                continue;
            }
            it.remove();
            weightLeft = turn(job, tick, weightLeft);
        }
    }

    /**
     * One turn of {@code job}: its share of what is left of the tick ({@code weights}: the weights of the jobs still to
     * go, its own included). Returns the weights still to go after it.
     */
    private static double turn(BuildJob job, Tick tick, double weights) {
        double weight = job.weight();
        if (!job.finished()) {
            long share = weights <= weight ? tick.units() : (long) Math.floor(tick.units() * weight / weights);
            try {
                tick.use(job.step(tick, share));
            } catch (RuntimeException e) {
                // One job's bad cell (a corrupt pocket, a claim mod's listener, a modded block) must not crash the tick: the
                // job stops where it is, what it did stays one undoable step, and its caster is told.
                job.fail(e);
            }
        }
        if (job.finished()) {
            remove(job);
        }
        return Math.max(0.0, weights - weight);
    }

    // ------------------------------------------------------------------------------------------------------------
    // Tests

    /**
     * Keeps the jobs of {@code player} out of the server's ticks (a test steps them with {@link #step}), and, when
     * {@code loaded} is not null, makes it what his jobs think is loaded (x, z of a chunk), at the cast and after.
     */
    public static void manual(UUID player, @Nullable BiPredicate<Integer, Integer> loaded) {
        MANUAL.add(player);
        if (loaded != null) {
            LOADED.put(player, loaded);
        } else {
            LOADED.remove(player);
        }
    }

    /** What the jobs of {@code player} think is loaded: a test's predicate, or the level's chunks. */
    public static BiPredicate<Integer, Integer> loaded(UUID player, ServerLevel level) {
        BiPredicate<Integer, Integer> loaded = LOADED.get(player);
        return loaded != null ? loaded : level::hasChunk;
    }

    /**
     * One tick of the jobs at work of {@code players} (their turns in that order), with {@code budget}: what the
     * server's tick does, for a test. Returns the tick, to see what is left of it.
     */
    public static Tick step(Collection<UUID> players, Budget budget) {
        Tick tick = budget.start();
        List<BuildJob> jobs = new ArrayList<>();
        for (UUID player : players) {
            BuildJob job = working(player);
            if (job != null) {
                jobs.add(job);
            }
        }
        double weights = 0.0;
        for (BuildJob job : jobs) {
            weights += job.weight();
        }
        for (BuildJob job : jobs) {
            weights = turn(job, tick, weights);
        }
        return tick;
    }

    /**
     * Finishes one job at once, loading the chunks of its set as needed (the dab at work when Ctrl + Z ends a stroke).
     */
    public static void finishNow(BuildJob job) {
        Budget endless = new Budget(Long.MAX_VALUE / 4, Integer.MAX_VALUE / BuildJob.WRITE, Integer.MAX_VALUE, 1, System::nanoTime, () -> 0L, 0L, 0);
        job.force();
        for (int guard = 0; guard < 100_000 && !job.finished(); guard++) {
            turn(job, endless.start(), job.weight());
        }
    }

    /** Finishes every job at once, loading the chunks of its set as needed (the server's stop, a test, an operator). */
    public static void finishAllNow() {
        Budget endless = new Budget(Long.MAX_VALUE / 4, Integer.MAX_VALUE / BuildJob.WRITE, Integer.MAX_VALUE, 1, System::nanoTime, () -> 0L, 0L, 0);
        for (int guard = 0; guard < 100_000 && !QUEUES.isEmpty(); guard++) {
            for (BuildJob job : all()) {
                if (working(job.caster()) == job) {
                    job.force();
                    turn(job, endless.start(), job.weight());
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Stopping

    /**
     * The server is stopping ({@code BuildEvents.onServerStopping}, LOW, R20): the spells left are finished at once
     * while their cells left add up to at most {@code cap} ({@code build_magic.finishOnStopBlocks}, R55); the largest
     * are stopped first, their steps sealed as interrupted (undoable), their players told at the next login. A spell
     * that waits and is not finished never started: it is dropped.
     *
     * @return how many jobs were interrupted or dropped
     */
    public static int onServerStopping(long cap) {
        List<BuildJob> left = all();
        left.sort(Comparator.comparingLong(BuildJob::remaining).reversed());
        long total = 0;
        for (BuildJob job : left) {
            total += job.remaining();
        }
        int stopped = 0;
        for (BuildJob job : left) {
            if (total <= cap) {
                break;
            }
            total -= job.remaining();
            if (working(job.caster()) == job) {
                job.interrupt("message.enhanced.build.interrupted");
            } else {
                job.drop();
            }
            remove(job);
            stopped++;
        }
        finishAllNow();
        return stopped;
    }

    /** Stops every job of {@code player} where it is (what it did stays, undoable) and drops the waiting ones. */
    public static int stop(UUID player) {
        List<BuildJob> jobs = jobs(player);
        for (BuildJob job : jobs) {
            if (working(player) == job) {
                job.interrupt(null);
            } else {
                job.drop();
            }
            remove(job);
        }
        return jobs.size();
    }

    /** Drops every spell, working or waiting, and lets go of their chunks: for the server's stop and for tests. */
    public static void clear() {
        for (BuildJob job : all()) {
            job.releaseTickets();
        }
        QUEUES.clear();
        MANUAL.clear();
        LOADED.clear();
        TURNS.clear();
        tick = null;
    }

    private BuildScheduler() {
    }
}
