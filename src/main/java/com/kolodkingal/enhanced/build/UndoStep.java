package com.kolodkingal.enhanced.build;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.Short2LongOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * One undo step (D-B 12.1): everything one spell changed, so that Ctrl + Z can put it back and Ctrl + Y do it again. One
 * spell is one step (a Paint stroke from press to release, a Repeat row, a Move with both its ends).
 *
 * <p>It holds the spell, its dimension, the box of the cells it wrote, the game time, Billy's cost and practice and
 * whether Mom helped, a {@link State}, and for every 16 x 16 x 16 section it touched, in the order it touched them: the
 * cells it changed (in the order it changed them), the old and new state of each (as ids of
 * {@code Block.BLOCK_STATE_REGISTRY}), the old and new block-entity data of the cells that have it, and a 64-bit hash of
 * each new block entity's data as it stood once written. Then the settle pass's side effects (a neighbour outside the
 * spell that changed because of it: a torch that dropped off a vanished wall). In memory only, for now; the history on
 * disk ({@code UndoFiles}) writes the same sections ahead of the cells (C13).
 *
 * <p>Each recorded cell has a status: still as the spell left it ({@link #APPLIED}), put back by an undo
 * ({@link #UNDONE}), or dropped because someone changed it since ({@link #DROPPED}, D-B 12.3: undo never destroys anyone's
 * later work, and a dropped cell is never touched again). A cell a protected place refused to undo stays
 * {@link #APPLIED}, and the step stays on top, {@link State#PARTLY partly undone}, for the next Ctrl + Z.
 */
public final class UndoStep {
    /** Where a step stands. The ordinal is what the history saves: append only. */
    public enum State {
        /** Its spell is still at work. */
        WORKING,
        /** Done, everything as the spell left it. */
        DONE,
        /** Undone: every cell is back (or was changed since and left as it was). */
        UNDONE,
        /** An undo left some cells (a protected place): the next Ctrl + Z tries them again. */
        PARTLY,
        /** Its spell was stopped when the world closed: what it did can be undone. */
        INTERRUPTED
    }

    /** The cell holds what the spell put there. */
    public static final byte APPLIED = 0;
    /** An undo put the cell back. */
    public static final byte UNDONE = 1;
    /** Someone changed the cell since: it is never touched again. */
    public static final byte DROPPED = 2;

    private final int id;
    private final Spell spell;
    private final ResourceKey<Level> dimension;
    private final long time;
    private State state = State.WORKING;
    private int cost;
    private long practice;
    private boolean mom;

    private final Long2ObjectLinkedOpenHashMap<Section> sections = new Long2ObjectLinkedOpenHashMap<>();
    private final List<SideEffect> sideEffects = new ArrayList<>();
    private long cells;
    private long applied;
    private int minX = Integer.MAX_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int minZ = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int maxY = Integer.MIN_VALUE;
    private int maxZ = Integer.MIN_VALUE;

    public UndoStep(int id, Spell spell, ResourceKey<Level> dimension, long time) {
        this.id = id;
        this.spell = spell;
        this.dimension = dimension;
        this.time = time;
    }

    // ------------------------------------------------------------------------------------------------------------
    // What the step is

    /** Its number in its player's history. */
    public int id() {
        return id;
    }

    public Spell spell() {
        return spell;
    }

    /** The dimension it changed: an undo runs there, wherever its player is. */
    public ResourceKey<Level> dimension() {
        return dimension;
    }

    /** The game time it was cast. */
    public long time() {
        return time;
    }

    public State state() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    /** Whether Ctrl + Z can still put something of it back. */
    public boolean undoable() {
        return (state == State.DONE || state == State.PARTLY || state == State.INTERRUPTED) && applied > 0;
    }

    /** What it cost Billy (what an undo refunds in part). */
    public int cost() {
        return cost;
    }

    /** The blocks of practice it taught Billy. */
    public long practice() {
        return practice;
    }

    /** Whether a Scarlet Witch helped him. */
    public boolean mom() {
        return mom;
    }

    public void setBilly(int cost, long practice, boolean mom) {
        this.cost = cost;
        this.practice = practice;
        this.mom = mom;
    }

    /** How many cells it changed. */
    public long changed() {
        return cells;
    }

    /** How many of them still hold what it put there ({@link #APPLIED}). */
    public long applied() {
        return applied;
    }

    /** The box of every cell it changed, or null when it changed none. */
    public @Nullable BoundingBox bounds() {
        return cells == 0 ? null : new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** The sections it touched, in the order it touched them ({@link SectionPos#asLong}). */
    public long[] sectionKeys() {
        return sections.keySet().toLongArray();
    }

    /** The side effects of its settle pass, in the order they happened. */
    public List<SideEffect> sideEffects() {
        return sideEffects;
    }

    // ------------------------------------------------------------------------------------------------------------
    // Recording

    /**
     * Records that the spell changed {@code pos} from {@code old} (with its block entity's data {@code oldData}) to
     * {@code now} ({@code newData}: the data it put there, a stamp's or a Move's; {@code newHash}: the hash of the new
     * block entity's data as it stands once written, or 0 for a block with none). A cell recorded before (a Paint
     * stroke over the same cell twice) keeps its first old state and takes the new one.
     */
    public void record(BlockPos pos, BlockState old, @Nullable CompoundTag oldData, BlockState now, @Nullable CompoundTag newData, long newHash) {
        long key = SectionPos.asLong(pos);
        Section section = sections.get(key);
        if (section == null) {
            section = new Section(key);
            sections.put(key, section);
        }
        short cell = Section.cell(pos);
        int at = section.indexOf(cell);
        if (at < 0) {
            at = section.add(cell, Block.getId(old), oldData);
            cells++;
            applied++;
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        } else if (section.status[at] != APPLIED) {
            section.status[at] = APPLIED;
            applied++;
        }
        section.news[at] = Block.getId(now);
        section.setNew(cell, newData, newHash);
    }

    /** Whether the spell changed {@code pos}. */
    public boolean has(BlockPos pos) {
        Section section = sections.get(SectionPos.asLong(pos));
        return section != null && section.indexOf(Section.cell(pos)) >= 0;
    }

    /** What the spell put at {@code pos} (null when it did not change it). */
    public @Nullable BlockState newState(BlockPos pos) {
        Section section = sections.get(SectionPos.asLong(pos));
        int at = section == null ? -1 : section.indexOf(Section.cell(pos));
        return at < 0 ? null : Block.stateById(section.news[at]);
    }

    /** The hash of the data the spell left at {@code pos} (0 for none). */
    public long newHash(BlockPos pos) {
        Section section = sections.get(SectionPos.asLong(pos));
        return section == null ? 0L : section.hash(Section.cell(pos));
    }

    /**
     * The settle pass changed a cell the spell had written ({@code Block.updateFromNeighbourShapes}: a fence joining its
     * neighbour): the step now remembers that as what the spell put there, so an undo still finds it as the spell left it.
     */
    public void settled(BlockPos pos, BlockState now) {
        Section section = sections.get(SectionPos.asLong(pos));
        int at = section == null ? -1 : section.indexOf(Section.cell(pos));
        if (at >= 0) {
            section.news[at] = Block.getId(now);
        }
    }

    /** The settle pass changed a neighbour outside the spell from {@code old} to {@code now}. */
    public void sideEffect(BlockPos pos, BlockState old, BlockState now) {
        sideEffects.add(new SideEffect(pos.asLong(), old, now));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Walking it (an undo backwards, a redo forwards, the settle pass forwards)

    /** A place in the step's cells, moved one cell at a time; an undo goes from the last cell to the first. */
    public Walk walk(boolean backwards) {
        return new Walk(backwards);
    }

    /** A walk over a step's cells in the order they were written (or the reverse), one cell at a time. */
    public final class Walk {
        private final boolean backwards;
        private final long[] keys = sectionKeys();
        private int sectionIndex;
        private int cellIndex;
        private @Nullable Section section;
        private int at = -1;
        private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        private Walk(boolean backwards) {
            this.backwards = backwards;
            this.sectionIndex = backwards ? keys.length - 1 : 0;
        }

        /** Moves to the next cell; false when every cell has been walked. */
        public boolean next() {
            while (true) {
                if (section == null) {
                    if (sectionIndex < 0 || sectionIndex >= keys.length) {
                        at = -1;
                        return false;
                    }
                    section = sections.get(keys[sectionIndex]);
                    cellIndex = backwards ? section.size : -1;
                }
                cellIndex += backwards ? -1 : 1;
                if (cellIndex >= 0 && cellIndex < section.size) {
                    at = cellIndex;
                    section.pos(at, pos);
                    return true;
                }
                section = null;
                sectionIndex += backwards ? -1 : 1;
            }
        }

        /** The cell the walk is on (valid until the next move). */
        public BlockPos pos() {
            return pos;
        }

        public BlockState oldState() {
            return Block.stateById(section.olds[at]);
        }

        public BlockState newState() {
            return Block.stateById(section.news[at]);
        }

        public @Nullable CompoundTag oldData() {
            return section.oldData == null ? null : section.oldData.get(section.cells[at]);
        }

        public @Nullable CompoundTag newData() {
            return section.newData == null ? null : section.newData.get(section.cells[at]);
        }

        public long newHash() {
            return section.hash(section.cells[at]);
        }

        /** The new hash of the cell after a redo wrote it again. */
        public void rehash(long hash) {
            section.setHash(section.cells[at], hash);
        }

        public byte status() {
            return section.status[at];
        }

        /** Sets the cell's status ({@link #APPLIED}, {@link #UNDONE}, {@link #DROPPED}). */
        public void setStatus(byte status) {
            byte was = section.status[at];
            if (was == status) {
                return;
            }
            if (was == APPLIED) {
                applied--;
            } else if (status == APPLIED) {
                applied++;
            }
            section.status[at] = status;
        }

        /** The section the walk is in ({@link SectionPos#asLong}). */
        public long sectionKey() {
            return section == null ? Long.MIN_VALUE : section.key;
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Data

    /** A neighbour outside the spell that its settle pass changed (a torch that dropped off a vanished wall). */
    public static final class SideEffect {
        private final long pos;
        private final BlockState old;
        private final BlockState now;
        private byte status = APPLIED;

        SideEffect(long pos, BlockState old, BlockState now) {
            this.pos = pos;
            this.old = old;
            this.now = now;
        }

        public BlockPos pos() {
            return BlockPos.of(pos);
        }

        public BlockState oldState() {
            return old;
        }

        public BlockState newState() {
            return now;
        }

        public byte status() {
            return status;
        }

        public void setStatus(byte status) {
            this.status = status;
        }
    }

    /**
     * A 64-bit hash of block-entity data (D-B 12.1): what tells an undo that a chest it would take back has been
     * filled since. Computed over the tag's binary form (FNV-1a), so equal tags hash alike on any run.
     */
    public static long hash(@Nullable CompoundTag tag) {
        if (tag == null) {
            return 0L;
        }
        Fnv out = new Fnv();
        try (DataOutputStream data = new DataOutputStream(out)) {
            NbtIo.write(tag, data);
        } catch (IOException e) {
            throw new IllegalStateException(e); // (an in-memory stream never fails)
        }
        return out.hash == 0L ? 1L : out.hash;
    }

    /** FNV-1a over the bytes written to it. */
    private static final class Fnv extends OutputStream {
        private long hash = 0xcbf29ce484222325L;

        @Override
        public void write(int b) {
            hash ^= b & 0xFF;
            hash *= 0x100000001b3L;
        }
    }

    /** The cells of one 16 x 16 x 16 section a step changed, in the order it changed them. */
    private static final class Section {
        final long key;
        /** For each of the 4,096 cells, its place in the lists below, or -1. */
        final short[] index = new short[4096];
        int size;
        short[] cells = new short[16];
        int[] olds = new int[16];
        int[] news = new int[16];
        byte[] status = new byte[16];
        @Nullable Short2ObjectOpenHashMap<CompoundTag> oldData;
        @Nullable Short2ObjectOpenHashMap<CompoundTag> newData;
        @Nullable Short2LongOpenHashMap hashes;

        Section(long key) {
            this.key = key;
            Arrays.fill(index, (short) -1);
        }

        /** The cell's number in its section: y, then z, then x. */
        static short cell(BlockPos pos) {
            return (short) (((pos.getY() & 15) << 8) | ((pos.getZ() & 15) << 4) | (pos.getX() & 15));
        }

        int indexOf(short cell) {
            return index[cell];
        }

        int add(short cell, int old, @Nullable CompoundTag data) {
            if (size == cells.length) {
                int grown = Math.min(4096, size * 2);
                cells = Arrays.copyOf(cells, grown);
                olds = Arrays.copyOf(olds, grown);
                news = Arrays.copyOf(news, grown);
                status = Arrays.copyOf(status, grown);
            }
            int at = size++;
            cells[at] = cell;
            olds[at] = old;
            status[at] = APPLIED;
            index[cell] = (short) at;
            if (data != null) {
                if (oldData == null) {
                    oldData = new Short2ObjectOpenHashMap<>();
                }
                oldData.put(cell, data);
            }
            return at;
        }

        void setNew(short cell, @Nullable CompoundTag data, long hash) {
            if (data != null) {
                if (newData == null) {
                    newData = new Short2ObjectOpenHashMap<>();
                }
                newData.put(cell, data);
            } else if (newData != null) {
                newData.remove(cell);
            }
            setHash(cell, hash);
        }

        void setHash(short cell, long hash) {
            if (hash != 0L) {
                if (hashes == null) {
                    hashes = new Short2LongOpenHashMap();
                }
                hashes.put(cell, hash);
            } else if (hashes != null) {
                hashes.remove(cell);
            }
        }

        long hash(short cell) {
            return hashes == null ? 0L : hashes.get(cell);
        }

        void pos(int at, BlockPos.MutableBlockPos pos) {
            short cell = cells[at];
            pos.set(SectionPos.sectionToBlockCoord(SectionPos.x(key)) + (cell & 15),
                    SectionPos.sectionToBlockCoord(SectionPos.y(key)) + ((cell >> 8) & 15),
                    SectionPos.sectionToBlockCoord(SectionPos.z(key)) + ((cell >> 4) & 15));
        }
    }

    /** The chunks (as {@code ChunkPos.toLong}) of every section the step touched: what an undo holds loaded. */
    public LongSet chunks() {
        LongSet chunks = new LongOpenHashSet();
        for (long key : sections.keySet()) {
            chunks.add(ChunkPos.asLong(SectionPos.x(key), SectionPos.z(key)));
        }
        return chunks;
    }
}
