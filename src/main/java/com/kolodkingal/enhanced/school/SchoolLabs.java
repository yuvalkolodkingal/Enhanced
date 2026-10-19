package com.kolodkingal.enhanced.school;

import com.kolodkingal.enhanced.world.SchoolLabPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The X-Men lower labs of the Overworld's Mutant Schools (3.7), saved with the world ({@code
 * data/enhanced_school_labs.dat} of the Overworld): for each school, keyed by the chunk of its structure start, how far
 * its lab is built ({@link State}, the step a crash resumes at), the template's version and why it is blocked. The 3.7
 * skeleton writes the record shell; lane L4 ({@code LabBuilder}) fills and reads it.
 */
public final class SchoolLabs extends SavedData {
    public static final String NAME = "enhanced_school_labs";

    /** How far a school's lab is. */
    public enum State {
        /** Not begun. */
        NONE,
        /** Being built: {@link Lab#step} is the next step (each step is idempotent). */
        BUILDING,
        /** Built (the entrance may still be blocked: {@link Lab#reason}). */
        DONE,
        /** Cannot be built here ({@link Lab#reason}: {@code too_deep}, {@code entrance}...). */
        BLOCKED
    }

    /**
     * One school's lab.
     *
     * @param key     the chunk of the school's structure start ({@code ChunkPos.toLong})
     * @param state   how far it is
     * @param step    the next step while {@link State#BUILDING}
     * @param version the template's version it was built from ({@link SchoolLabPlan#VERSION})
     * @param reason  why it is blocked, or ""
     * @param origin  (lane L4) where the school's template was put: its piece's template position
     * @param turn    (lane L4) how the school was turned (the lab is turned the same way)
     * @param pivot   (lane L4) the school's turning pivot (template-relative)
     * @param cursor  (lane L4) how far the current step is (a face block, a layer; a crash resumes there)
     */
    public record Lab(long key, State state, int step, int version, String reason, BlockPos origin, Rotation turn,
                      BlockPos pivot, int cursor) {
        /** The skeleton's record (no placement yet). */
        public Lab(long key, State state, int step, int version, String reason) {
            this(key, state, step, version, reason, BlockPos.ZERO, Rotation.NONE, BlockPos.ZERO, 0);
        }

        /** Where the school (and so its lab) lies in the world. */
        public LabSite site() {
            return new LabSite(origin, turn, pivot);
        }

        /** The same lab at another state, step and cursor (the reason kept unless a new one is given). */
        public Lab at(State newState, int newStep, int newCursor, String newReason) {
            return new Lab(key, newState, newStep, version, newReason, origin, turn, pivot, newCursor);
        }

        CompoundTag write() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("key", key);
            tag.putString("state", state.name());
            tag.putInt("step", step);
            tag.putInt("version", version);
            tag.putString("reason", reason);
            tag.putIntArray("origin", new int[]{origin.getX(), origin.getY(), origin.getZ()});
            tag.putString("turn", turn.name());
            tag.putIntArray("pivot", new int[]{pivot.getX(), pivot.getY(), pivot.getZ()});
            tag.putInt("cursor", cursor);
            return tag;
        }

        static Lab read(CompoundTag tag) {
            State state;
            try {
                state = State.valueOf(tag.getString("state").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                state = State.NONE;
            }
            Rotation turn;
            try {
                turn = Rotation.valueOf(tag.getString("turn"));
            } catch (IllegalArgumentException e) {
                turn = Rotation.NONE;
            }
            return new Lab(tag.getLong("key"), state, tag.getInt("step"), tag.getInt("version"),
                    tag.getString("reason"), pos(tag.getIntArray("origin")), turn, pos(tag.getIntArray("pivot")),
                    tag.getInt("cursor"));
        }

        private static BlockPos pos(int[] xyz) {
            return xyz.length == 3 ? new BlockPos(xyz[0], xyz[1], xyz[2]) : BlockPos.ZERO;
        }
    }

    private final Map<Long, Lab> labs = new LinkedHashMap<>();

    public static SavedData.Factory<SchoolLabs> factory() {
        return new SavedData.Factory<>(SchoolLabs::new, SchoolLabs::load, null);
    }

    /** The labs of this level (the Overworld), made empty if there are none yet. */
    public static SchoolLabs get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), NAME);
    }

    private static SchoolLabs load(CompoundTag tag, HolderLookup.Provider registries) {
        SchoolLabs store = new SchoolLabs();
        for (Tag t : tag.getList("labs", Tag.TAG_COMPOUND)) {
            Lab lab = Lab.read((CompoundTag) t);
            store.labs.put(lab.key(), lab);
        }
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Lab lab : labs.values()) {
            list.add(lab.write());
        }
        tag.put("labs", list);
        return tag;
    }

    /** The lab of the school whose start is in chunk {@code key}, or null (not seen yet). */
    @Nullable
    public Lab byKey(long key) {
        return labs.get(key);
    }

    /** Records {@code lab} (replacing the school's old record). */
    public void put(Lab lab) {
        labs.put(lab.key(), lab);
        setDirty();
    }

    /** Forgets the lab of the school whose start is in chunk {@code key} (the tests' fake schools). */
    public void remove(long key) {
        if (labs.remove(key) != null) {
            setDirty();
        }
    }

    public Collection<Lab> all() {
        return Collections.unmodifiableCollection(labs.values());
    }
}
