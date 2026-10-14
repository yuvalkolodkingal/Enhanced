package com.kolodkingal.enhanced.build;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A builder's undo history (D-B 12.2, 12.5): the steps of her spells, newest last, and the steps she undid that Ctrl + Y
 * can do again. One per player, by UUID, so it outlives a logout and a death, and an undo works from any dimension (its
 * job runs in the step's).
 *
 * <ul>
 *     <li><b>Ctrl + Z</b> takes the newest step that still has something to put back, a {@linkplain UndoStep.State#PARTLY
 *     partly undone} one first; once it is wholly undone it moves to the redo list.</li>
 *     <li><b>Ctrl + Y</b> takes the newest undone step back.</li>
 *     <li><b>A new spell</b> empties the redo list.</li>
 *     <li><b>Steps</b>: Billy keeps {@code build_magic_billy.undoSteps} (+2 a rank); the Scarlet Witch as many as
 *     {@code build_magic_wanda.undoSteps}, or with 0 only what the undo memory and disk hold (R39). Past the count, the
 *     oldest step is forgotten.</li>
 * </ul>
 *
 * <p>On disk ({@link UndoFiles}, D-B 12.4): the history keeps the steps whose spells still work ({@link #opened}: their
 * records are being written ahead) for its index, and the steps it lets go of ({@link #takeGone}: forgotten, or an
 * emptied redo list) until their files are deleted; {@link #version} counts its changes, so a world save rewrites only
 * the indexes that changed. A history read back from the disk is {@linkplain #install installed}, or merged into one
 * its player began while it was being read.
 */
public final class UndoHistory {
    private static final Map<UUID, UndoHistory> ALL = new HashMap<>();

    private final UUID owner;
    /** The steps that can be undone (some of them partly), newest last. */
    private final Deque<UndoStep> done = new ArrayDeque<>();
    /** The steps that were undone and can be done again, newest last. */
    private final Deque<UndoStep> undone = new ArrayDeque<>();
    /** The steps whose spells still work and whose records are on disk (C13), oldest first. */
    private final List<UndoStep> open = new ArrayList<>();
    /** The steps it let go of, whose files are still to be deleted. */
    private final List<UndoStep> gone = new ArrayList<>();
    private int nextId = 1;
    private long leftAt = -1;
    private long version;
    /** The version its index on disk was last written at ({@link UndoFiles}); -1 before. */
    long indexed = -1;
    /** How many steps were sealed into it since it was made (a history read from disk drops its redo list if any). */
    private int pushes;

    private UndoHistory(UUID owner) {
        this.owner = owner;
    }

    /** The history of the player {@code owner} (made the first time it is asked for). */
    public static UndoHistory of(UUID owner) {
        return ALL.computeIfAbsent(owner, UndoHistory::new);
    }

    /** The history of {@code owner} if he has one. */
    public static @Nullable UndoHistory existing(UUID owner) {
        return ALL.get(owner);
    }

    /** Every history in memory. */
    public static List<UndoHistory> all() {
        return List.copyOf(ALL.values());
    }

    /** A history that belongs to no player yet (one read from disk, before it is installed). */
    static UndoHistory detached(UUID owner) {
        return new UndoHistory(owner);
    }

    /**
     * Makes {@code loaded} (read from disk) the history of its player; if he began one while it was being read, its
     * steps are older than his new ones and go before them, and its redo list is dropped if he has cast since (a new
     * spell empties it). Returns the history the player now has.
     */
    static UndoHistory install(UndoHistory loaded) {
        UndoHistory now = ALL.get(loaded.owner);
        if (now == null) {
            ALL.put(loaded.owner, loaded);
            loaded.version++;
            return loaded;
        }
        List<UndoStep> older = new ArrayList<>(loaded.done);
        for (int i = older.size() - 1; i >= 0; i--) {
            UndoStep step = older.get(i);
            step.renumber(now.nextId++);
            now.done.addFirst(step);
        }
        if (now.pushes == 0 && now.undone.isEmpty()) {
            for (UndoStep step : loaded.undone) {
                step.renumber(now.nextId++);
                now.undone.addLast(step);
            }
        } else {
            now.gone.addAll(loaded.undone);
        }
        now.gone.addAll(loaded.gone);
        now.version++;
        return now;
    }

    /** Forgets every history (the server has stopped, or a test ends). */
    public static void clear() {
        ALL.clear();
    }

    /** Forgets the history of {@code owner} (nobody was on for 30 minutes, and none is written to disk). */
    static void remove(UUID owner) {
        ALL.remove(owner);
    }

    public UUID owner() {
        return owner;
    }

    /** A number for a new step of this player. */
    public int nextId() {
        return nextId++;
    }

    /**
     * Seals a spell's step into the history: the redo list empties, and past {@code maxSteps} (0: no count) the oldest
     * steps are forgotten.
     *
     * @return how many steps were forgotten to make room
     */
    public int push(UndoStep step, int maxSteps) {
        gone.addAll(undone);
        undone.clear();
        open.remove(step);
        done.addLast(step);
        pushes++;
        version++;
        int forgotten = 0;
        while (maxSteps > 0 && done.size() > maxSteps) {
            gone.add(done.removeFirst());
            forgotten++;
        }
        UndoFiles.sealed(this, step);
        return forgotten;
    }

    /** A step read from disk goes in as it was: undone ({@code redo}) or one Ctrl + Z can work on, after the others. */
    void restore(UndoStep step, boolean redo) {
        step.renumber(nextId++);
        (redo ? undone : done).addLast(step);
    }

    /** The spell of {@code step} began writing its records ahead (C13): its history's index lists it as working. */
    void opened(UndoStep step) {
        if (!open.contains(step)) {
            open.add(step);
            version++;
        }
    }

    /** The spell of {@code step} ended without a step to keep (it changed nothing): it is no longer listed. */
    void closed(UndoStep step) {
        if (open.remove(step)) {
            version++;
        }
    }

    /** The steps whose spells still work and whose records are on disk, oldest first. */
    public List<UndoStep> open() {
        return List.copyOf(open);
    }

    /** The steps it let go of since the last call (forgotten, an emptied redo list): their files can go. */
    List<UndoStep> takeGone() {
        List<UndoStep> taken = List.copyOf(gone);
        gone.clear();
        return taken;
    }

    /** A number that changes whenever the history does (a world save rewrites only the indexes that changed). */
    public long version() {
        return version;
    }

    /** Something the index keeps changed (a step's state, a line said). */
    void touched() {
        version++;
    }

    /** What a login said of the spells cut short is not said again: the steps' "tell" marks are cleared. */
    void told() {
        for (UndoStep step : allSteps()) {
            if (step.tell()) {
                step.setTell(false);
                version++;
            }
        }
    }

    /** The steps that were undone and can be done again, oldest first. */
    public List<UndoStep> undoneSteps() {
        return new ArrayList<>(undone);
    }

    /** The step Ctrl + Z works on: a partly undone step first, else the newest that can be undone; null if none. */
    public @Nullable UndoStep nextToUndo() {
        for (Iterator<UndoStep> it = done.descendingIterator(); it.hasNext(); ) {
            UndoStep step = it.next();
            if (step.state() == UndoStep.State.PARTLY && step.undoable()) {
                return step;
            }
        }
        for (Iterator<UndoStep> it = done.descendingIterator(); it.hasNext(); ) {
            UndoStep step = it.next();
            if (step.undoable()) {
                return step;
            }
        }
        return null;
    }

    /** The step Ctrl + Y works on: the newest undone one, or null. */
    public @Nullable UndoStep nextToRedo() {
        return undone.peekLast();
    }

    /**
     * An undo of {@code step} ended: a wholly undone step moves to the redo list (in the order of undoing), a partly
     * undone one stays where it is for the next Ctrl + Z.
     */
    public void undid(UndoStep step) {
        version++;
        if (step.state() == UndoStep.State.UNDONE && done.remove(step)) {
            undone.addLast(step);
        }
        UndoFiles.sealed(this, step);
    }

    /** A redo of {@code step} ended: it can be undone again. */
    public void redid(UndoStep step) {
        version++;
        if (undone.remove(step)) {
            done.addLast(step);
        }
        UndoFiles.sealed(this, step);
    }

    /** Lets go of {@code step} (its file could not be read): it is in the history no more. */
    void drop(UndoStep step) {
        if (done.remove(step) | undone.remove(step)) {
            gone.add(step);
            version++;
        }
    }

    /** The server tick its player left at, or -1 while he is on (or never was). */
    long leftAt() {
        return leftAt;
    }

    void left(long tick) {
        this.leftAt = tick;
    }

    /** How many steps it holds (the ones Ctrl + Z works on and the ones Ctrl + Y does again). */
    int stepCount() {
        return done.size() + undone.size();
    }

    /** The bytes the files of its steps hold. */
    long diskBytes() {
        long bytes = 0;
        for (UndoStep step : done) {
            bytes += step.diskBytes();
        }
        for (UndoStep step : undone) {
            bytes += step.diskBytes();
        }
        return bytes;
    }

    /** What the cells of its steps weigh in memory, about. */
    long memoryBytes() {
        long bytes = 0;
        for (UndoStep step : done) {
            bytes += step.memoryBytes();
        }
        for (UndoStep step : undone) {
            bytes += step.memoryBytes();
        }
        return bytes;
    }

    /** Forgets the oldest step (done or undone), its file with it; null when it has none. */
    @Nullable UndoStep forgetOldest() {
        UndoStep first = done.peekFirst();
        UndoStep other = undone.peekFirst();
        UndoStep oldest = first == null ? other : other == null ? first : first.time() <= other.time() ? first : other;
        if (oldest == null) {
            return null;
        }
        if (!done.remove(oldest)) {
            undone.remove(oldest);
        }
        gone.add(oldest);
        version++;
        return oldest;
    }

    /** Every step of it (the ones Ctrl + Z works on, then the ones Ctrl + Y does again), oldest first. */
    List<UndoStep> allSteps() {
        List<UndoStep> all = new ArrayList<>(done);
        all.addAll(undone);
        return all;
    }

    /** The steps that can be undone, oldest first. */
    public List<UndoStep> steps() {
        return new ArrayList<>(done);
    }

    /** How many steps Ctrl + Z can still work on. */
    public int undoCount() {
        int count = 0;
        for (UndoStep step : done) {
            if (step.undoable()) {
                count++;
            }
        }
        return count;
    }

    /** How many steps Ctrl + Y can do again. */
    public int redoCount() {
        return undone.size();
    }

    /** Forgets every step of this player. */
    public void forget() {
        gone.addAll(done);
        gone.addAll(undone);
        done.clear();
        undone.clear();
        version++;
    }
}
