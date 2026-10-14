package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.EnhancedConfig;
import com.kolodkingal.enhanced.config.BuildConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * How much undo history is kept (D-B 12.5, spec 5.6, C14): in memory and on disk, per player and on the whole server.
 *
 * <table>
 *     <caption>The caps and what happens past them</caption>
 *     <tr><td>memory, per player ({@code undoMemoryMb})</td><td>the oldest steps leave memory: they stay on disk and are
 *     read back when undone (with nothing written to disk, the oldest are forgotten)</td></tr>
 *     <tr><td>memory, whole server ({@code serverUndoMemoryMb})</td><td>the steps of the player whose history takes the
 *     most leave memory first, oldest first (then the oldest are forgotten, with nothing on disk)</td></tr>
 *     <tr><td>disk, per player ({@code undoDiskMb})</td><td>that player's oldest steps are forgotten</td></tr>
 *     <tr><td>disk, whole server ({@code serverUndoDiskMb})</td><td>the oldest step of the player with the most is
 *     forgotten</td></tr>
 * </table>
 *
 * <p>A history never loses its newest step to a disk cap (a spell too big to remember is asked to be held for, D-B 12.5, and
 * runs without a step). A step can leave memory only once its file holds it as it is, and not while an undo or redo of it
 * works; one that cannot yet is tried again the next time ({@link #enforceAll}, once a second). Without a disk (the history
 * is not written) nothing can leave memory: the oldest steps are forgotten instead.
 */
public final class UndoCaps {
    private static final long MIB = 1024L * 1024L;
    /** Every how many ticks the caps are looked at. */
    public static final int EVERY_TICKS = 20;
    /** A history nobody is on for this long is dropped when it is not written to disk (D-B 12.4): 30 minutes. */
    public static final long ABSENT_TICKS = 30L * 60L * 20L;

    private static long memory = -1;
    private static long disk = -1;
    private static long serverMemory = -1;
    private static long serverDisk = -1;

    private UndoCaps() {
    }

    /** A test's caps, in bytes (-1: the configured one); until {@link #reset}. */
    public static void use(long playerMemory, long playerDisk, long wholeMemory, long wholeDisk) {
        memory = playerMemory;
        disk = playerDisk;
        serverMemory = wholeMemory;
        serverDisk = wholeDisk;
    }

    /** The configured caps again. */
    public static void reset() {
        use(-1, -1, -1, -1);
    }

    private static long memoryCap() {
        return memory >= 0 ? memory : configured(BuildConfig.UNDO_MEMORY_MB.get());
    }

    private static long diskCap() {
        return disk >= 0 ? disk : configured(BuildConfig.UNDO_DISK_MB.get());
    }

    private static long serverMemoryCap() {
        return serverMemory >= 0 ? serverMemory : configured(BuildConfig.SERVER_UNDO_MEMORY_MB.get());
    }

    private static long serverDiskCap() {
        return serverDisk >= 0 ? serverDisk : configured(BuildConfig.SERVER_UNDO_DISK_MB.get());
    }

    private static long configured(int megabytes) {
        return megabytes * MIB;
    }

    /** Whether the configuration can be read (a test that opened no server has none). */
    private static boolean configured() {
        return EnhancedConfig.SPEC.isLoaded() || memory >= 0 && disk >= 0 && serverMemory >= 0 && serverDisk >= 0;
    }

    /** What a record of {@code cells} cells in {@code box} costs, about: 0.6 byte a cell and 700 bytes a section touched (D-B 12.5). */
    public static long estimate(long cells, @Nullable BoundingBox box) {
        long sections = 1;
        if (box != null) {
            sections = (long) ((box.maxX() >> 4) - (box.minX() >> 4) + 1) * ((box.maxY() >> 4) - (box.minY() >> 4) + 1)
                    * ((box.maxZ() >> 4) - (box.minZ() >> 4) + 1);
            sections = Math.min(sections, Math.max(1, cells));
        }
        return cells * 6 / 10 + sections * 700;
    }

    /** What a step of {@code cells} cells in {@code box} weighs in memory while its spell works, about: 14 bytes a cell and 12 KiB a section. */
    public static long memoryEstimate(long cells, @Nullable BoundingBox box) {
        return cells * 14 + sectionsOf(cells, box) * 12288L;
    }

    private static long sectionsOf(long cells, @Nullable BoundingBox box) {
        long sections = 1;
        if (box != null) {
            sections = (long) ((box.maxX() >> 4) - (box.minX() >> 4) + 1) * ((box.maxY() >> 4) - (box.minY() >> 4) + 1)
                    * ((box.maxZ() >> 4) - (box.minZ() >> 4) + 1);
            sections = Math.min(sections, Math.max(1, cells));
        }
        return sections;
    }

    /** How a spell's step is kept (D-B 12.5). */
    public enum Room {
        /** Whole in memory while the spell works. */
        FULL,
        /** Too big for memory: only the cells it changed are remembered (bits), and its records on disk are its step. */
        LITE,
        /** Neither fits: the spell asks for the long hold and then runs without a step. */
        NONE
    }

    /**
     * Where the step of a spell of {@code cells} cells in {@code box} can be kept. Whole in memory while the spell works if
     * that weighs no more than half of what the server may spend on undo ({@code serverUndoMemoryMb}) nor one player's share
     * ({@code undoMemoryMb}), counting the steps other spells are working on; else, when the history is written to disk and
     * the record fits in {@code undoDiskMb}, as bits and records (a Move needs its cells whole: it is refused); else not at
     * all. A history that does not fit on disk is {@link Room#NONE} whatever memory says.
     */
    static Room room(Spell spell, long cells, @Nullable BoundingBox box) {
        if (!configured()) {
            return Room.FULL;
        }
        boolean disk = UndoFiles.active();
        boolean diskFits = !disk || estimate(cells, box) <= diskCap();
        long weight = memoryEstimate(cells, box);
        long budget = Math.min(memoryCap(), serverMemoryCap() / 2);
        boolean wholeFits = weight <= budget && weight + workingBytes() <= serverMemoryCap();
        if (wholeFits && diskFits) {
            return Room.FULL;
        }
        if (disk && diskFits && spell != Spell.MOVE) {
            return Room.LITE;
        }
        return Room.NONE;
    }

    /** What the steps of the spells at work weigh in memory now. */
    static long workingBytes() {
        long bytes = 0;
        for (BuildJob job : BuildScheduler.all()) {
            if (job.step().state() == UndoStep.State.WORKING) {
                bytes += job.step().memoryBytes();
            }
        }
        return bytes;
    }

    /**
     * After a step was sealed into {@code history}: its player's caps. Returns how many steps were forgotten (their
     * files go with the next index).
     */
    static int enforce(UndoHistory history) {
        if (!configured()) {
            return 0;
        }
        int forgotten = enforcePlayer(history, running());
        if (forgotten > 0) {
            UndoFiles.index(history);
        }
        return forgotten;
    }

    /**
     * Looks at every history's caps and the server's (once a second, and at a test's wish); a player who is online
     * hears that his oldest spell can no longer be undone when one is forgotten.
     */
    public static void enforceAll(@Nullable MinecraftServer server) {
        if (!configured()) {
            return;
        }
        Set<UndoStep> running = running();
        for (UndoHistory history : UndoHistory.all()) {
            int forgotten = enforcePlayer(history, running);
            tell(server, history, forgotten);
        }
        // The server's disk: the oldest step of the player with the most goes.
        long cap = serverDiskCap();
        while (true) {
            UndoHistory heaviest = null;
            long total = 0;
            for (UndoHistory history : UndoHistory.all()) {
                long bytes = history.diskBytes();
                total += bytes;
                if (history.stepCount() > 1 && (heaviest == null || bytes > heaviest.diskBytes())) {
                    heaviest = history;
                }
            }
            if (total <= cap || heaviest == null) {
                break;
            }
            heaviest.forgetOldest();
            tell(server, heaviest, 1);
        }
        // The server's memory: the steps of the player using the most leave memory, oldest first.
        long memoryCap = serverMemoryCap();
        while (true) {
            List<UndoHistory> heaviest = new ArrayList<>(UndoHistory.all());
            heaviest.sort(Comparator.comparingLong(UndoHistory::memoryBytes).reversed());
            long total = 0;
            for (UndoHistory history : heaviest) {
                total += history.memoryBytes();
            }
            if (total <= memoryCap) {
                break;
            }
            boolean moved = false;
            for (UndoHistory history : heaviest) {
                if (leaveMemory(history, running)) {
                    moved = true;
                    break;
                }
            }
            if (!moved && !UndoFiles.active()) {
                // Nothing is on disk: the oldest steps of the heaviest history are forgotten instead.
                for (UndoHistory history : heaviest) {
                    if (history.stepCount() > 1) {
                        history.forgetOldest();
                        tell(server, history, 1);
                        moved = true;
                        break;
                    }
                }
            }
            if (!moved) {
                break; // (what is left cannot leave yet: the next look tries again)
            }
        }
        for (UndoHistory history : UndoHistory.all()) {
            UndoFiles.index(history);
        }
    }

    /** A history nobody has been on for 30 minutes goes when it is not written to disk. */
    public static void dropAbsent(MinecraftServer server) {
        if (UndoFiles.active()) {
            return;
        }
        long now = server.getTickCount();
        for (UndoHistory history : UndoHistory.all()) {
            long left = history.leftAt();
            ServerPlayer player = server.getPlayerList().getPlayer(history.owner());
            if (left >= 0 && player == null && now - left >= ABSENT_TICKS) {
                UndoHistory.remove(history.owner());
            }
        }
    }

    private static int enforcePlayer(UndoHistory history, Set<UndoStep> running) {
        int forgotten = 0;
        if (UndoFiles.active()) {
            long cap = diskCap();
            while (history.diskBytes() > cap && history.stepCount() > 1) {
                history.forgetOldest();
                forgotten++;
            }
        }
        long cap = memoryCap();
        while (history.memoryBytes() > cap) {
            if (leaveMemory(history, running)) {
                continue;
            }
            if (!UndoFiles.active() && history.stepCount() > 1) {
                history.forgetOldest();
                forgotten++;
                continue;
            }
            break;
        }
        return forgotten;
    }

    /** The oldest step of {@code history} that can leave memory leaves it; false when none can. */
    private static boolean leaveMemory(UndoHistory history, Set<UndoStep> running) {
        for (UndoStep step : history.allSteps()) {
            if (step.loaded() && step.stored() && !running.contains(step) && step.evict()) {
                return true;
            }
        }
        return false;
    }

    /** The steps a job is undoing, redoing or writing now: their cells stay in memory. */
    private static Set<UndoStep> running() {
        Set<UndoStep> steps = Collections.newSetFromMap(new IdentityHashMap<>());
        List<BuildJob> jobs = BuildScheduler.all();
        for (BuildJob job : jobs) {
            steps.add(job.step());
        }
        return steps;
    }

    private static void tell(@Nullable MinecraftServer server, UndoHistory history, int forgotten) {
        if (forgotten <= 0 || server == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(history.owner());
        if (player != null && !player.hasDisconnected()) {
            player.displayClientMessage(BillyMagic.forgotten(), true);
        }
    }
}
