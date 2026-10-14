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
 * <p>In memory only, for now: until the server stops (the disk, the memory caps and {@code /enhanced build forget} are
 * {@code UndoFiles}', D-B 12.4).
 */
public final class UndoHistory {
    private static final Map<UUID, UndoHistory> ALL = new HashMap<>();

    private final UUID owner;
    /** The steps that can be undone (some of them partly), newest last. */
    private final Deque<UndoStep> done = new ArrayDeque<>();
    /** The steps that were undone and can be done again, newest last. */
    private final Deque<UndoStep> undone = new ArrayDeque<>();
    private int nextId = 1;

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

    /** Forgets every history (the server has stopped, or a test ends). */
    public static void clear() {
        ALL.clear();
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
        undone.clear();
        done.addLast(step);
        int forgotten = 0;
        while (maxSteps > 0 && done.size() > maxSteps) {
            done.removeFirst();
            forgotten++;
        }
        return forgotten;
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
        if (step.state() == UndoStep.State.UNDONE && done.remove(step)) {
            undone.addLast(step);
        }
    }

    /** A redo of {@code step} ended: it can be undone again. */
    public void redid(UndoStep step) {
        if (undone.remove(step)) {
            done.addLast(step);
        }
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
        done.clear();
        undone.clear();
    }
}
