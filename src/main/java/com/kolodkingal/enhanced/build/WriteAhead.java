package com.kolodkingal.enhanced.build;

import com.kolodkingal.enhanced.build.SpellPlanner.Plan;
import com.kolodkingal.enhanced.config.BuildConfig;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;

/**
 * The read-ahead of a spell that writes its undo records ahead (C13, R52): a second cursor of the spell's plan runs in
 * front of the one that writes, reads what each cell holds and what the spell will put there, and hands the cells to the
 * spell's {@link UndoFiles.Journal} in batches. The job writes a cell only when its record is down, and yields while it
 * is not.
 *
 * <p>It reads only cells of chunks the job may touch and that are loaded (never loads one), at most {@link #WINDOW}
 * cells ahead of the writing cursor. What it finds can differ from what the writing cursor finds a few ticks later (a
 * neighbour the spell itself changed, someone else's change): the record's new state is then only what the spell was
 * going to put there; the record's old state is what the writing cursor checks the cell against before it writes
 * ({@code kept_changed}), and a cell the read-ahead never saw is recorded when the writing cursor reaches it.
 */
final class WriteAhead {
    /** The most cells it reads ahead of the writing cursor. */
    static final int WINDOW = 32_768;
    /** The cells it reads in one slice, at least. */
    static final int PER_SLICE = 2048;

    private final ServerLevel level;
    private final Plan plan;
    private final SpellPlanner.Cursor cursor;
    private final UndoFiles.Journal journal;
    private final BiPredicate<Integer, Integer> loaded;
    private final LongSet chunks;
    private final boolean moving;
    private long scanned;
    private boolean exhausted;

    WriteAhead(ServerLevel level, Plan plan, UndoFiles.Journal journal, BiPredicate<Integer, Integer> loaded, LongSet chunks, boolean moving) {
        this.level = level;
        this.plan = plan;
        this.cursor = plan.cursor();
        this.journal = journal;
        this.loaded = loaded;
        this.chunks = chunks;
        this.moving = moving;
    }

    /** How many cells it has read. */
    long scanned() {
        return scanned;
    }

    /** Whether the whole plan has been read. */
    boolean exhausted() {
        return exhausted;
    }

    /**
     * Reads up to {@code max} more cells, while it is less than {@link #WINDOW} cells ahead of {@code walked} (the
     * cells the writing cursor has passed); hands the last batch over when the plan is exhausted.
     *
     * @return how many cells it read
     */
    int scan(long walked, int max) {
        int read = 0;
        while (!exhausted && read < max && scanned < walked + WINDOW) {
            if (!cursor.next()) {
                exhausted = true;
                journal.flush();
                break;
            }
            scanned++;
            read++;
            offer();
        }
        return read;
    }

    /** Reads the cell the cursor is on, and what its source holds for a Move: hands over what will change. */
    private void offer() {
        BlockPos pos = cursor.pos();
        if (!ready(pos)) {
            return;
        }
        BlockState old = level.getBlockState(pos);
        BlockState now = cursor.resolve(level);
        if (now == null) {
            return;
        }
        CompoundTag data = cursor.data();
        BlockPos from = cursor.from();
        if (!now.equals(old) || data != null) {
            journal.add(pos.asLong(), old, oldData(pos, old), now, data == null ? null : data.copy());
        }
        if (moving && from != null && !plan.covers(from.getX(), from.getY(), from.getZ()) && ready(from)) {
            BlockState source = level.getBlockState(from);
            BlockState emptied = SpellMath.vanished(source);
            if (!source.equals(emptied)) {
                journal.add(from.asLong(), source, oldData(from, source), emptied, null);
            }
        }
    }

    /** Whether the cell is one the job may change now: inside the world, in a chunk of its set that is loaded. */
    private boolean ready(BlockPos pos) {
        int cx = SectionPos.blockToSectionCoord(pos.getX());
        int cz = SectionPos.blockToSectionCoord(pos.getZ());
        return chunks.contains(ChunkPos.asLong(cx, cz)) && loaded.test(cx, cz) && SpellMath.loaded(level, pos) && !SpellMath.outside(level, pos);
    }

    /**
     * What the block entity at {@code pos} holds, for the record: nothing of what a container spills (the items lie on
     * the ground when the cell is written, so an undo brings the container back empty, D-B 12.6).
     */
    static @Nullable CompoundTag oldData(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.hasBlockEntity()) {
            return null;
        }
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null) {
            return null;
        }
        CompoundTag tag = entity.saveWithoutMetadata(level.registryAccess());
        if (BuildConfig.FULL_CONTAINERS.get() == BuildConfig.FullContainers.SPILL) {
            tag.remove("Items");
        }
        return tag;
    }

    private @Nullable CompoundTag oldData(BlockPos pos, BlockState state) {
        return oldData(level, pos, state);
    }
}
