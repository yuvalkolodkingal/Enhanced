package com.kolodkingal.enhanced.boss;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.ultron.SokoviaJournal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The one thread that writes the arena journals (D-T 5.8): the server thread only builds a snapshot of a fight's ledger
 * ({@link ArenaLoans}) and hands it over here, so that no file is ever created, written or renamed on the server thread (a
 * borrow can come at every key press of three or four heroes, and a file and a rename can take tens of milliseconds on a
 * Windows host with an antivirus).
 *
 * <p>A journal is {@code <world>/data/enhanced/arena/<fight>.nbt}, gzip-compressed NBT, written to a temporary file and moved
 * over the real name in one step (as the Sokovia journal is), but not forced to the disk: a hard stop loses at most what was
 * still waiting here, a fraction of a second. The writes and the deletions run in the order they were handed over; a
 * snapshot that a newer one of the same file has overtaken while it waited is skipped, and a deletion waits behind the
 * writes handed over before it, so an older snapshot can never bring a deleted journal back.
 *
 * <p>The thread starts with the server and is drained when it has stopped, after the flushed save of the stop: everything
 * the journals still kept waiting for a save of the world is then retired, at most ten seconds are given to the queue, and
 * the thread ends.
 */
@EventBusSubscriber(modid = Enhanced.MODID)
public final class ArenaJournalWriter {
    /** The folder of the journals, inside the world. */
    public static final String DIRECTORY = "data/enhanced/arena";
    /** The name of the writing thread. */
    public static final String THREAD = "Enhanced arena journal";
    /** How long the stop waits for the queue, in seconds. */
    private static final long DRAIN_SECONDS = 10L;

    /** The newest job handed over for each file: an older one still waiting is skipped. */
    private static final Map<Path, Long> NEWEST = new ConcurrentHashMap<>();
    private static final AtomicLong JOBS = new AtomicLong();
    @Nullable
    private static ExecutorService executor;
    @Nullable
    private static volatile String lastWriter;

    // ---------------------------------------------------------------------------------------------------------
    // The server's life

    /** The thread starts with the server (HIGH, before the journals of a crash are read). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onServerStarting(ServerStartingEvent event) {
        start();
    }

    /**
     * The server has stopped, after the flushed save of the stop (R48): every journal entry, every journal kept for a save
     * and every island note retiring are retired at once, then the queue is drained and the thread ends.
     */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        retireAll(event.getServer());
        drain();
        ArenaLoans.clear();
        ArenaLoansRecovery.clear();
    }

    /** Starts the writing thread, if it is not running. */
    public static synchronized void start() {
        if (executor == null) {
            executor = Executors.newSingleThreadExecutor(task -> {
                Thread thread = new Thread(task, THREAD);
                thread.setDaemon(true);
                return thread;
            });
        }
    }

    /**
     * Everything the journals keep only until the world has been saved (R48) is let go now: the entries of the ledgers of
     * finished fights and the journals a crash recovery restored (their files deleted through the queue), and the notes of
     * Ultron's islands that are retiring ({@link SokoviaJournal#retireAll}). What the stop handler does first; a test
     * simulates the stop with it.
     */
    public static void retireAll(MinecraftServer server) {
        ArenaLoans.retireAll(server);
        ArenaLoansRecovery.retireAll(server);
        SokoviaJournal.retireAll(server);
    }

    /** Waits (at most ten seconds) until everything handed over is written, and ends the thread. */
    public static void drain() {
        ExecutorService draining;
        synchronized (ArenaJournalWriter.class) {
            draining = executor;
            executor = null;
        }
        if (draining == null) {
            return;
        }
        draining.shutdown();
        try {
            if (!draining.awaitTermination(DRAIN_SECONDS, TimeUnit.SECONDS)) {
                Enhanced.LOGGER.warn("The arena journal did not finish writing within {} s", DRAIN_SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        NEWEST.clear();
    }

    // ---------------------------------------------------------------------------------------------------------
    // The journals

    /** The folder of the arena journals of this world. */
    public static Path directory(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(DIRECTORY);
    }

    /** The journal file of a fight. */
    public static Path file(MinecraftServer server, UUID fight) {
        return directory(server).resolve(fight + ".nbt");
    }

    /** Hands over a snapshot of a fight's ledger: it replaces the fight's journal, off the server thread. */
    public static void submit(MinecraftServer server, UUID fight, CompoundTag journal) {
        enqueue(file(server, fight), journal);
    }

    /** Hands over the deletion of a fight's journal (it runs after every write handed over before it). */
    public static void delete(MinecraftServer server, UUID fight) {
        enqueue(file(server, fight), null);
    }

    private static void enqueue(Path file, @Nullable CompoundTag journal) {
        long job = JOBS.incrementAndGet();
        NEWEST.put(file, job);
        executor().execute(() -> {
            if (NEWEST.getOrDefault(file, job) != job) {
                return; // a newer snapshot (or the deletion) of the same journal waits behind this one
            }
            try {
                if (journal == null) {
                    Files.deleteIfExists(file);
                    Files.deleteIfExists(temporary(file));
                } else {
                    write(file, journal);
                }
                lastWriter = Thread.currentThread().getName();
            } catch (IOException | RuntimeException e) {
                Enhanced.LOGGER.error("Could not {} the arena journal {}", journal == null ? "delete" : "write", file, e);
            } finally {
                NEWEST.remove(file, job);
            }
        });
    }

    /** Writes the journal to a temporary file and moves it over the real name in one step. */
    private static void write(Path file, CompoundTag journal) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = temporary(file);
        NbtIo.writeCompressed(journal, temporary);
        try {
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static Path temporary(Path file) {
        return file.resolveSibling(file.getFileName() + ".tmp");
    }

    private static synchronized ExecutorService executor() {
        if (executor == null) {
            start(); // (a journal handed over before the server's start, as a test server may)
        }
        return executor;
    }

    // ---------------------------------------------------------------------------------------------------------
    // For tests

    /** Waits (at most ten seconds) until everything handed over so far is on disk. */
    @VisibleForTesting
    public static void flush() {
        try {
            executor().submit(() -> {
            }).get(DRAIN_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("The arena journal did not flush", e);
        }
    }

    /** The name of the thread that wrote (or deleted) a journal last, or null if none was. */
    @VisibleForTesting
    @Nullable
    public static String lastWriter() {
        return lastWriter;
    }

    private ArenaJournalWriter() {
    }
}
