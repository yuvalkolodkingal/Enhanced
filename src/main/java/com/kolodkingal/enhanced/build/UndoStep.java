package com.kolodkingal.enhanced.build;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.Short2LongOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import java.util.UUID;

/**
 * One undo step (D-B 12.1): everything one spell changed, so that Ctrl + Z can put it back and Ctrl + Y do it again. One
 * spell is one step (a Paint stroke from press to release, a Repeat row, a Move with both its ends).
 *
 * <p>It holds the spell, its dimension, the box of the cells it wrote, the game time, Billy's cost and practice and
 * whether Mom helped, whether its blocks were paid with items ({@code requireMaterials}), a {@link State}, and for
 * every 16 x 16 x 16 section it touched, in the order it touched them: the cells it changed (in the order it changed
 * them), the old and new state of each (as ids of {@code Block.BLOCK_STATE_REGISTRY}), the old and new block-entity
 * data of the cells that have it, and a 64-bit hash of each new block entity's data as it stood once written. Then the
 * settle pass's side effects (a neighbour outside the spell that changed because of it: a torch that dropped off a
 * vanished wall), and the frames and paintings a Move carried (where each hung before and after). On disk
 * ({@link UndoFiles}) its cells are recorded before they change (C13), and once it is sealed the whole step is kept in
 * its file ({@link #image}); a step of a history read back from the disk may be only listed, its cells read when it is
 * undone or redone ({@link #loaded}, {@link #adopt}).
 *
 * <p>A step read back from the records of a spell that a crash cut short ({@link #recovered}) knows what each cell held
 * before and what the spell was to put there, not how its settle pass shaped it or which cells it reached: its undo puts
 * a cell back when it holds the block the spell was to put there (any of them, for a cell recorded more than once).
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

    private int id;
    private final Spell spell;
    private final ResourceKey<Level> dimension;
    private final long time;
    private State state = State.WORKING;
    private int cost;
    private long practice;
    private boolean mom;
    private boolean paid;

    /** The order the spell's ripple swept (a plan's order); set when its job starts. */
    void setFxOrder(int order) {
        this.fxOrder = order;
    }

    /**
     * The order the spell's ripple swept ({@code BuildFxPayload}: rise, melt, shells, columns): the plan's, else worked out
     * from the spell and the box for a step read from disk (a Vanish melts, a shape swells in shells, a valley-sized spell goes
     * by columns).
     */
    public int fxOrder() {
        if (fxOrder >= 0) {
            return fxOrder;
        }
        BoundingBox box = bounds();
        if (box != null && (box.getXSpan() > 64 || box.getZSpan() > 64)) {
            return com.kolodkingal.enhanced.network.BuildFxPayload.COLUMNS;
        }
        return switch (spell) {
            case VANISH -> com.kolodkingal.enhanced.network.BuildFxPayload.MELT;
            case SHAPE -> com.kolodkingal.enhanced.network.BuildFxPayload.SHELLS;
            default -> com.kolodkingal.enhanced.network.BuildFxPayload.RISE;
        };
    }

    /** Its file's name on disk ({@code step-<file>.bin}), or 0 before it has one. */
    private long file;
    /** Its records being written ahead (C13), while its spell works. */
    UndoFiles.@Nullable Journal journal;
    /** Its file's writer (made with its name). */
    UndoFiles.@Nullable StepFile io;
    /** How many bytes its file holds. */
    private volatile long bytes;
    /** Its file holds the step as it is now (written after its last change, so its cells can leave memory). */
    private volatile boolean stored;
    private volatile int mods;
    /** How the spell's ripple swept ({@code BuildFxPayload} order), or -1 when it is not known (read from disk). */
    private int fxOrder = -1;
    /** What {@link #memoryBytes} found, until the step changes (-1: not known). */
    private long memory = -1;
    /** Read back from a crash's records: its cells are what the spell was to do (see the class comment). */
    private boolean recovered;
    /** For a recovered step: the other new states (and their data's hashes) recorded for a cell, by position. */
    private @Nullable Long2ObjectOpenHashMap<long[]> otherNews;
    /** Its "stopped when the world closed" line is still to be said, at its player's next login (D-B 11.9). */
    private boolean tell;
    /** Only listed (read from the history's index): its cells are on disk until {@link #adopt}. */
    private boolean listed;

    private final Long2ObjectLinkedOpenHashMap<Section> sections = new Long2ObjectLinkedOpenHashMap<>();
    /**
     * A lite step (too big to hold in memory while it works, or one that nothing will be able to undo): it remembers only
     * which cells it changed (a bit each) and the hashes of the block entities it left, not their states. A lite step
     * whose records are on disk (the journal, C13) is sealed as a listed step that reads back from them; one with no
     * journal is dropped when its spell ends.
     */
    private boolean lite;
    private final Long2ObjectLinkedOpenHashMap<LiteSection> lites = new Long2ObjectLinkedOpenHashMap<>();
    /** A Move's cells that carried block-entity data: source cell to target cell, and back. */
    private @Nullable Long2LongOpenHashMap links;
    private @Nullable Long2LongOpenHashMap backLinks;
    private final List<SideEffect> sideEffects = new ArrayList<>();
    private final List<Carried> carried = new ArrayList<>();
    /** The carried things by the block they hung on before the Move, and after it. */
    private final Long2ObjectOpenHashMap<List<Carried>> carriedBefore = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<List<Carried>> carriedAfter = new Long2ObjectOpenHashMap<>();
    private long cells;
    private long applied;
    private long undone;
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
        unstore();
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

    /**
     * Whether its blocks were paid with items from its builder's inventory ({@code build_magic.requireMaterials}, D-B
     * 5.6): its undo gives the placed blocks back and takes the vanished ones back, and its redo pays again.
     */
    public boolean paid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    /** How many cells it changed. */
    public long changed() {
        return cells;
    }

    /** How many of them still hold what it put there ({@link #APPLIED}). */
    public long applied() {
        return applied;
    }

    /** How many of them an undo put back ({@link #UNDONE}): what a redo would do again, and what Billy pays for it. */
    public long undone() {
        return undone;
    }

    /** The box of every cell it changed, or null when it changed none. */
    public @Nullable BoundingBox bounds() {
        return cells == 0 ? null : new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** The sections it touched, in the order it touched them ({@link SectionPos#asLong}). */
    public long[] sectionKeys() {
        return lite ? lites.keySet().toLongArray() : sections.keySet().toLongArray();
    }

    /**
     * A Move took the block entity of {@code source} (with what it holds) to {@code target}: an undo gives the source its
     * contents back only if the target was undone, and a redo writes the target's only if the source was cleared, so
     * what a container holds is never in two places (D-B 12.3).
     */
    public void link(BlockPos source, BlockPos target) {
        if (links == null) {
            links = new Long2LongOpenHashMap();
            links.defaultReturnValue(Long.MIN_VALUE);
            backLinks = new Long2LongOpenHashMap();
            backLinks.defaultReturnValue(Long.MIN_VALUE);
        }
        links.put(source.asLong(), target.asLong());
        backLinks.put(target.asLong(), source.asLong());
    }

    /** Where the Move took the contents of {@code source}, or null. */
    public @Nullable BlockPos linkedTarget(BlockPos source) {
        long target = links == null ? Long.MIN_VALUE : links.get(source.asLong());
        return target == Long.MIN_VALUE ? null : BlockPos.of(target);
    }

    /** Where the Move took the contents that {@code target} holds from, or null. */
    public @Nullable BlockPos linkedSource(BlockPos target) {
        long source = backLinks == null ? Long.MIN_VALUE : backLinks.get(target.asLong());
        return source == Long.MIN_VALUE ? null : BlockPos.of(source);
    }

    private long[] linkArray() {
        if (links == null) {
            return new long[0];
        }
        long[] pairs = new long[links.size() * 2];
        int i = 0;
        for (var entry : links.long2LongEntrySet()) {
            pairs[i++] = entry.getLongKey();
            pairs[i++] = entry.getLongValue();
        }
        return pairs;
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
        if (lite) {
            recordLite(pos, newHash);
            return;
        }
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
            if (section.status[at] == UNDONE) {
                undone--;
            }
            section.status[at] = APPLIED;
            applied++;
        }
        section.news[at] = Block.getId(now);
        section.setNew(cell, newData, newHash);
    }

    /** Makes the step lite (before it records anything): see {@link #lite}. */
    void makeLite() {
        if (cells == 0) {
            lite = true;
        }
    }

    /** Whether the step is lite. */
    public boolean isLite() {
        return lite;
    }

    private void recordLite(BlockPos pos, long newHash) {
        long key = SectionPos.asLong(pos);
        LiteSection section = lites.get(key);
        if (section == null) {
            section = new LiteSection(key);
            lites.put(key, section);
        }
        short cell = Section.cell(pos);
        if (!section.get(cell)) {
            section.set(cell);
            cells++;
            applied++;
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        section.setHash(cell, newHash);
    }

    /** The hashes of the block entities a lite step left, as (position, hash) pairs. */
    long[] liteHashes() {
        LongArrayList pairs = new LongArrayList();
        for (LiteSection section : lites.values()) {
            if (section.hashes != null) {
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                for (var entry : section.hashes.short2LongEntrySet()) {
                    section.pos(entry.getShortKey(), pos);
                    pairs.add(pos.asLong());
                    pairs.add(entry.getLongValue());
                }
            }
        }
        return pairs.toLongArray();
    }

    /** A lite step whose records are on disk lets go of its bits: it is listed, read back from its file when undone. */
    void evictLite() {
        lites.clear();
        lite = false;
        listed = true;
        memory = -1;
        stored = true;
    }

    /** Restores the hash of a block entity a lite step left, from its file. */
    void restoreNewHash(BlockPos pos, long hash) {
        Section section = sections.get(SectionPos.asLong(pos));
        short cell = Section.cell(pos);
        if (section != null && section.indexOf(cell) >= 0) {
            section.setHash(cell, hash);
        }
    }

    /** Whether the spell changed {@code pos}. */
    public boolean has(BlockPos pos) {
        if (lite) {
            LiteSection bits = lites.get(SectionPos.asLong(pos));
            return bits != null && bits.get(Section.cell(pos));
        }
        Section section = sections.get(SectionPos.asLong(pos));
        return section != null && section.indexOf(Section.cell(pos)) >= 0;
    }

    /** What the spell put at {@code pos} (null when it did not change it). */
    public @Nullable BlockState newState(BlockPos pos) {
        if (lite) {
            return null;
        }
        Section section = sections.get(SectionPos.asLong(pos));
        int at = section == null ? -1 : section.indexOf(Section.cell(pos));
        return at < 0 ? null : Block.stateById(section.news[at]);
    }

    /** The hash of the data the spell left at {@code pos} (0 for none). */
    public long newHash(BlockPos pos) {
        if (lite) {
            LiteSection bits = lites.get(SectionPos.asLong(pos));
            return bits == null ? 0L : bits.hash(Section.cell(pos));
        }
        Section section = sections.get(SectionPos.asLong(pos));
        return section == null ? 0L : section.hash(Section.cell(pos));
    }

    /** What {@code pos} held before the spell changed it (null when it did not change it). */
    public @Nullable BlockState oldState(BlockPos pos) {
        if (lite) {
            return null;
        }
        Section section = sections.get(SectionPos.asLong(pos));
        int at = section == null ? -1 : section.indexOf(Section.cell(pos));
        return at < 0 ? null : Block.stateById(section.olds[at]);
    }

    /** The block-entity data {@code pos} had before the spell changed it (null for none). */
    public @Nullable CompoundTag oldData(BlockPos pos) {
        if (lite) {
            return null;
        }
        Section section = sections.get(SectionPos.asLong(pos));
        return section == null || section.oldData == null ? null : section.oldData.get(Section.cell(pos));
    }

    /** The status of the cell {@code pos} ({@link #APPLIED}, {@link #UNDONE}, {@link #DROPPED}), or -1 when not recorded. */
    public byte status(BlockPos pos) {
        if (lite) {
            return has(pos) ? APPLIED : -1;
        }
        Section section = sections.get(SectionPos.asLong(pos));
        int at = section == null ? -1 : section.indexOf(Section.cell(pos));
        return at < 0 ? -1 : section.status[at];
    }

    /**
     * The settle pass changed a cell the spell had written ({@code Block.updateFromNeighbourShapes}: a fence joining its
     * neighbour): the step now remembers that as what the spell put there, so an undo still finds it as the spell left it.
     */
    public void settled(BlockPos pos, BlockState now) {
        if (lite) {
            return;
        }
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

    /**
     * A Move carried a frame or a painting (by its UUID) from the cell {@code from}, facing {@code fromFacing}, to
     * {@code to}, facing {@code toFacing} (D-B 12.1): an undo hangs it back where it was if it still hangs where the
     * spell put it, and a redo carries it again.
     */
    public void carried(UUID id, BlockPos from, Direction fromFacing, BlockPos to, Direction toFacing) {
        Carried thing = new Carried(id, from.immutable(), fromFacing, to.immutable(), toFacing);
        carried.add(thing);
        carriedBefore.computeIfAbsent(thing.fromSupport().asLong(), key -> new ArrayList<>()).add(thing);
        carriedAfter.computeIfAbsent(thing.toSupport().asLong(), key -> new ArrayList<>()).add(thing);
    }

    /** The frames and paintings a Move carried, in the order it carried them. */
    public List<Carried> carried() {
        return carried;
    }

    /**
     * The frames and paintings a Move carried that hang on {@code pos}: on their old place's block ({@code before},
     * what an undo hangs back once that block stands again) or on their new place's (a redo's).
     */
    public List<Carried> carriedOn(BlockPos pos, boolean before) {
        if (carried.isEmpty()) {
            return List.of();
        }
        return (before ? carriedBefore : carriedAfter).getOrDefault(pos.asLong(), List.of());
    }

    // ------------------------------------------------------------------------------------------------------------
    // On disk (UndoFiles)

    /** Its file's name on disk ({@code step-<file>.bin}, as 16 hex digits), or 0 before it has one. */
    public long file() {
        return file;
    }

    void setFile(long file) {
        this.file = file;
    }

    /** How many bytes its file holds (what the disk caps count). */
    public long diskBytes() {
        return bytes;
    }

    void setBytes(long bytes) {
        this.bytes = bytes;
    }

    /** Its file now holds the step as it was when {@code mods} counted its changes. */
    void markStored(int mods) {
        if (this.mods == mods) {
            stored = true;
        }
    }

    /** Its file no longer holds the step as it is (it changed, or is being written again); the count of its changes. */
    int unstore() {
        stored = false;
        memory = -1;
        return ++mods;
    }

    /** Whether its file holds the step as it is (its cells may leave memory). */
    public boolean stored() {
        return stored;
    }

    void markRecovered(boolean recovered) {
        this.recovered = recovered;
    }

    /**
     * What its cells weigh in memory, about: a section's index and its cells' lists, and the block-entity data (a few
     * hundred bytes each); a step only listed weighs nothing.
     */
    public long memoryBytes() {
        if (listed) {
            return 256L;
        }
        if (lite) {
            return 512L + lites.size() * 1024L;
        }
        if (memory >= 0 && state != State.WORKING) {
            return memory;
        }
        long total = 512L;
        for (Section section : sections.values()) {
            total += 8192L + 4096L + (long) section.cells.length * 11L;
            total += (section.oldData == null ? 0 : section.oldData.size()) * 256L + (section.newData == null ? 0 : section.newData.size()) * 256L;
        }
        total += sideEffects.size() * 48L + carried.size() * 96L;
        memory = total;
        return total;
    }

    /**
     * Lets its cells go from memory once its file holds them ({@link #stored}); it is listed again, and is read back when
     * it is undone or redone. False when its file does not hold the step as it is, or its spell still works.
     */
    boolean evict() {
        if (listed || !stored || state == State.WORKING || journal != null) {
            return false;
        }
        sections.clear();
        sideEffects.clear();
        carried.clear();
        carriedBefore.clear();
        carriedAfter.clear();
        otherNews = null;
        links = null;
        backLinks = null;
        listed = true;
        memory = -1;
        return true;
    }

    /** Gives the step a new number in the history it is read into. */
    void renumber(int id) {
        this.id = id;
    }

    /** Whether its cells are in memory (false: only listed, read from disk when it is undone or redone). */
    public boolean loaded() {
        return !listed;
    }

    /** Whether it was read back from the records of a spell a crash cut short (see the class comment). */
    public boolean recovered() {
        return recovered;
    }

    /** Whether its "stopped when the world closed" line is still to be said at its player's next login. */
    public boolean tell() {
        return tell;
    }

    public void setTell(boolean tell) {
        this.tell = tell;
    }

    /**
     * A step of a history read from disk that is only listed: what its index says of it (how many cells it changed,
     * how many still hold what it put there, how many an undo put back, its box); its cells come with {@link #adopt}.
     */
    static UndoStep listed(Spell spell, ResourceKey<Level> dimension, long time, long changed, long applied, long undone,
                           @Nullable BoundingBox box) {
        UndoStep step = new UndoStep(0, spell, dimension, time);
        step.listed = true;
        step.cells = changed;
        step.applied = applied;
        step.undone = undone;
        if (box != null) {
            step.minX = box.minX();
            step.minY = box.minY();
            step.minZ = box.minZ();
            step.maxX = box.maxX();
            step.maxY = box.maxY();
            step.maxZ = box.maxZ();
        }
        return step;
    }

    /** A step read from disk, before its cells are {@linkplain #restore restored}. */
    static UndoStep read(Spell spell, ResourceKey<Level> dimension, long time) {
        return new UndoStep(0, spell, dimension, time);
    }

    /** The cells, side effects and carried things of {@code body} (the same step read from disk) become this step's. */
    void adopt(UndoStep body) {
        sections.clear();
        sections.putAll(body.sections);
        sideEffects.clear();
        sideEffects.addAll(body.sideEffects);
        carried.clear();
        carriedBefore.clear();
        carriedAfter.clear();
        for (Carried thing : body.carried) {
            carried(thing.id(), thing.from(), thing.fromFacing(), thing.to(), thing.toFacing());
        }
        links = body.links;
        backLinks = body.backLinks;
        cells = body.cells;
        applied = body.applied;
        undone = body.undone;
        minX = body.minX;
        minY = body.minY;
        minZ = body.minZ;
        maxX = body.maxX;
        maxY = body.maxY;
        maxZ = body.maxZ;
        recovered = body.recovered;
        otherNews = body.otherNews;
        listed = false;
        memory = -1;
        stored = !body.recovered; // (its file holds it as it is, unless that file is the records of a cut spell)
    }

    /**
     * Restores a cell of a step read from disk, in the order the step wrote them: what it held, what the spell put
     * there, their data, the hash of the new data, and its status ({@link #DROPPED} for a cell whose block no longer
     * exists: "keep").
     */
    void restore(long key, short cell, int old, @Nullable CompoundTag oldData, int now, @Nullable CompoundTag newData, long hash, byte status) {
        Section section = sections.get(key);
        if (section == null) {
            section = new Section(key);
            sections.put(key, section);
        }
        if (section.indexOf(cell) >= 0) {
            return; // (each cell is recorded once)
        }
        int at = section.add(cell, old, oldData);
        section.news[at] = now;
        section.setNew(cell, newData, hash);
        section.status[at] = status;
        cells++;
        if (status == APPLIED) {
            applied++;
        } else if (status == UNDONE) {
            undone++;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        section.pos(at, pos);
        minX = Math.min(minX, pos.getX());
        minY = Math.min(minY, pos.getY());
        minZ = Math.min(minZ, pos.getZ());
        maxX = Math.max(maxX, pos.getX());
        maxY = Math.max(maxY, pos.getY());
        maxZ = Math.max(maxZ, pos.getZ());
    }

    /**
     * A cell of the records of a spell a crash cut short (C13), in the order they were written: its first record says
     * what it held before; its last, what the spell was to put there, the earlier ones are kept as other new states it
     * may hold. {@code known} false: a block of it no longer exists, and the cell is kept as it is.
     */
    void recover(long key, short cell, int old, @Nullable CompoundTag oldData, int now, @Nullable CompoundTag newData, long hash, boolean known) {
        recovered = true;
        Section section = sections.get(key);
        int at = section == null ? -1 : section.indexOf(cell);
        if (at < 0) {
            restore(key, cell, old, oldData, now, newData, hash, known ? APPLIED : DROPPED);
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        section.pos(at, pos);
        if (otherNews == null) {
            otherNews = new Long2ObjectOpenHashMap<>();
        }
        long[] others = otherNews.get(pos.asLong());
        long[] more = others == null ? new long[2] : Arrays.copyOf(others, others.length + 2);
        more[more.length - 2] = section.news[at];
        more[more.length - 1] = section.hash(cell);
        otherNews.put(pos.asLong(), more);
        section.news[at] = now;
        section.setNew(cell, newData, hash);
        if (!known && section.status[at] == APPLIED) {
            section.status[at] = DROPPED;
            applied--;
        }
    }

    /**
     * For a {@linkplain #recovered recovered} step: whether {@code here} (with the hash {@code hereHash} of its data) is
     * what the spell was to put at {@code pos}, as far as its records tell: the block of one of the new states
     * recorded for the cell (whatever its settle pass made of its shape), with the same data where there is some.
     */
    public boolean recoveredHolds(BlockPos pos, BlockState here, long hereHash) {
        Section section = sections.get(SectionPos.asLong(pos));
        int at = section == null ? -1 : section.indexOf(Section.cell(pos));
        if (at < 0) {
            return false;
        }
        long hash = section.hash(section.cells[at]);
        if (Block.stateById(section.news[at]).is(here.getBlock()) && (!here.hasBlockEntity() || hash == 0L || hash == hereHash)) {
            return true;
        }
        long[] others = otherNews == null ? null : otherNews.get(pos.asLong());
        for (int i = 0; others != null && i < others.length; i += 2) {
            if (Block.stateById((int) others[i]).is(here.getBlock()) && (!here.hasBlockEntity() || others[i + 1] == 0L || others[i + 1] == hereHash)) {
                return true;
            }
        }
        return false;
    }

    /** Restores the hash of the data an undo put back in a cell of a step read from disk. */
    void restoreRestored(long key, short cell, long hash) {
        Section section = sections.get(key);
        if (section != null && section.indexOf(cell) >= 0) {
            section.setRestored(cell, hash);
        }
    }

    /** Restores a side effect of a step read from disk, with its status. */
    void restoreSideEffect(long pos, BlockState old, BlockState now, byte status) {
        SideEffect effect = new SideEffect(pos, old, now);
        effect.setStatus(status);
        sideEffects.add(effect);
    }

    /**
     * A redo of the step was at work when the server stopped without saving it (a crash): the cells it had put back
     * may hold what the spell put there again, so the step is one Ctrl + Z can undo, its undone cells (and side effects)
     * as the spell left them (the undo's check skips those the redo never reached).
     */
    void redoCut() {
        for (Section section : sections.values()) {
            for (int at = 0; at < section.size; at++) {
                if (section.status[at] == UNDONE) {
                    section.status[at] = APPLIED;
                    undone--;
                    applied++;
                }
            }
        }
        for (SideEffect effect : sideEffects) {
            if (effect.status() == UNDONE) {
                effect.setStatus(APPLIED);
            }
        }
        state = State.INTERRUPTED;
    }

    /**
     * A copy of the step as it is now, for writing it to disk off the server thread ({@link UndoFiles}): every section's
     * cells in order with their old and new states, statuses, data and hashes, the side effects and what a Move carried.
     * The data tags are shared, not copied: the step never changes a tag it holds.
     */
    public Image image() {
        List<SectionImage> images = new ArrayList<>(sections.size());
        for (Section section : sections.values()) {
            images.add(new SectionImage(section.key, Arrays.copyOf(section.cells, section.size), Arrays.copyOf(section.olds, section.size),
                    Arrays.copyOf(section.news, section.size), Arrays.copyOf(section.status, section.size),
                    section.oldData == null ? new Short2ObjectOpenHashMap<>() : new Short2ObjectOpenHashMap<>(section.oldData),
                    section.newData == null ? new Short2ObjectOpenHashMap<>() : new Short2ObjectOpenHashMap<>(section.newData),
                    section.hashes == null ? new Short2LongOpenHashMap() : new Short2LongOpenHashMap(section.hashes),
                    section.restored == null ? new Short2LongOpenHashMap() : new Short2LongOpenHashMap(section.restored)));
        }
        List<SideEffectImage> effects = new ArrayList<>(sideEffects.size());
        for (SideEffect effect : sideEffects) {
            effects.add(new SideEffectImage(effect.pos, Block.getId(effect.old), Block.getId(effect.now), effect.status));
        }
        return new Image(spell, dimension, time, state, cost, practice, mom, paid, tell, recovered || lite, images, effects, List.copyOf(carried), linkArray(),
                liteHashes());
    }

    /** A step as it was when copied ({@link #image}), to be written to disk. */
    public record Image(Spell spell, ResourceKey<Level> dimension, long time, State state, int cost, long practice, boolean mom, boolean paid,
                        boolean tell, boolean recovered, List<SectionImage> sections, List<SideEffectImage> sideEffects, List<Carried> carried,
                        long[] links, long[] hashes) {
    }

    /** A section of an {@link Image}: its cells in the order they were written, with their states (ids) and data. */
    public record SectionImage(long key, short[] cells, int[] olds, int[] news, byte[] status, Short2ObjectOpenHashMap<CompoundTag> oldData,
                               Short2ObjectOpenHashMap<CompoundTag> newData, Short2LongOpenHashMap hashes, Short2LongOpenHashMap restored) {
    }

    /** A side effect of an {@link Image}: its cell, its old and new state (ids) and its status. */
    public record SideEffectImage(long pos, int old, int now, byte status) {
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

        private final boolean liteWalk = lite;
        private @Nullable LiteSection liteSection;
        private int liteBit = -1;

        private Walk(boolean backwards) {
            this.backwards = backwards;
            this.sectionIndex = backwards ? keys.length - 1 : 0;
        }

        /** A lite step's cells: the section's bits in order (the settle pass wants no states). */
        private boolean nextLite() {
            while (true) {
                if (liteSection == null) {
                    if (sectionIndex < 0 || sectionIndex >= keys.length) {
                        return false;
                    }
                    liteSection = lites.get(keys[sectionIndex]);
                    liteBit = backwards ? 4096 : -1;
                }
                liteBit += backwards ? -1 : 1;
                while (liteBit >= 0 && liteBit < 4096 && !liteSection.get((short) liteBit)) {
                    liteBit += backwards ? -1 : 1;
                }
                if (liteBit >= 0 && liteBit < 4096) {
                    liteSection.pos((short) liteBit, pos);
                    return true;
                }
                liteSection = null;
                sectionIndex += backwards ? -1 : 1;
            }
        }

        /** Moves to the next cell; false when every cell has been walked. */
        public boolean next() {
            if (liteWalk) {
                return nextLite();
            }
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

        public @Nullable BlockState oldState() {
            return liteWalk ? null : Block.stateById(section.olds[at]);
        }

        public @Nullable BlockState newState() {
            return liteWalk ? null : Block.stateById(section.news[at]);
        }

        public @Nullable CompoundTag oldData() {
            return liteWalk ? null : section.oldData == null ? null : section.oldData.get(section.cells[at]);
        }

        public @Nullable CompoundTag newData() {
            return liteWalk ? null : section.newData == null ? null : section.newData.get(section.cells[at]);
        }

        public long newHash() {
            if (liteWalk) {
                return liteSection.hash((short) liteBit);
            }
            return section.hash(section.cells[at]);
        }

        /** The hash of the data an undo put back in the cell (0 for none): what a redo expects to find. */
        public long restoredHash() {
            return section.restored(section.cells[at]);
        }

        public void setRestoredHash(long hash) {
            section.setRestored(section.cells[at], hash);
            unstore();
        }

        /** The new hash of the cell after a redo wrote it again. */
        public void rehash(long hash) {
            section.setHash(section.cells[at], hash);
        }

        public byte status() {
            return liteWalk ? APPLIED : section.status[at];
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
            if (was == UNDONE) {
                undone--;
            } else if (status == UNDONE) {
                undone++;
            }
            section.status[at] = status;
            unstore();
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

    /** A frame or a painting a Move carried, where it hung before and after (D-B 12.1). */
    public static final class Carried {
        private final UUID id;
        private final BlockPos from;
        private final Direction fromFacing;
        private final BlockPos to;
        private final Direction toFacing;

        Carried(UUID id, BlockPos from, Direction fromFacing, BlockPos to, Direction toFacing) {
            this.id = id;
            this.from = from;
            this.fromFacing = fromFacing;
            this.to = to;
            this.toFacing = toFacing;
        }

        public UUID id() {
            return id;
        }

        /** The cell it hung in before the Move. */
        public BlockPos from() {
            return from;
        }

        public Direction fromFacing() {
            return fromFacing;
        }

        /** The cell the Move hung it in. */
        public BlockPos to() {
            return to;
        }

        public Direction toFacing() {
            return toFacing;
        }

        /** The block it hung on before the Move. */
        public BlockPos fromSupport() {
            return from.relative(fromFacing.getOpposite());
        }

        /** The block it hangs on after the Move. */
        public BlockPos toSupport() {
            return to.relative(toFacing.getOpposite());
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

    /** The cells of one section a lite step changed, one bit each, and the hashes of the block entities among them. */
    private static final class LiteSection {
        final long key;
        final long[] bits = new long[64];
        @Nullable Short2LongOpenHashMap hashes;

        LiteSection(long key) {
            this.key = key;
        }

        boolean get(short cell) {
            return (bits[cell >> 6 & 63] >>> (cell & 63) & 1L) != 0L;
        }

        void set(short cell) {
            bits[cell >> 6 & 63] |= 1L << (cell & 63);
        }

        void setHash(short cell, long hash) {
            if (hash != 0L) {
                if (hashes == null) {
                    hashes = new Short2LongOpenHashMap();
                }
                hashes.put(cell, hash);
            }
        }

        long hash(short cell) {
            return hashes == null ? 0L : hashes.get(cell);
        }

        void pos(short cell, BlockPos.MutableBlockPos pos) {
            pos.set(SectionPos.sectionToBlockCoord(SectionPos.x(key)) + (cell & 15), SectionPos.sectionToBlockCoord(SectionPos.y(key)) + ((cell >> 8) & 15),
                    SectionPos.sectionToBlockCoord(SectionPos.z(key)) + ((cell >> 4) & 15));
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
        /** The hash of the block-entity data an undo put back in a cell (0: none), for the redo's content check. */
        @Nullable Short2LongOpenHashMap restored;

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

        void setRestored(short cell, long hash) {
            if (hash != 0L) {
                if (restored == null) {
                    restored = new Short2LongOpenHashMap();
                }
                restored.put(cell, hash);
            } else if (restored != null) {
                restored.remove(cell);
            }
        }

        long restored(short cell) {
            return restored == null ? 0L : restored.get(cell);
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
        for (long key : lites.keySet()) {
            chunks.add(ChunkPos.asLong(SectionPos.x(key), SectionPos.z(key)));
        }
        return chunks;
    }
}
