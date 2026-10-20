package com.kolodkingal.enhanced.tower;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The Avengers Towers the refit has been to (3.8, PLAN 9.6), saved with the Overworld ({@code
 * data/enhanced_tower_refits.dat}): keyed by the chunk of each tower's structure start, its version ({@link
 * TowerRefit#VERSION} once refitted, {@link TowerRefit#OLD_DESIGN} for a 3.6.0 tower left as it was) and what the
 * last refit did. A tower with a record is never refitted again by itself (only by the command).
 */
public final class TowerRefits extends SavedData {
    public static final String NAME = "enhanced_tower_refits";

    /** One tower's record. */
    public record Entry(int version, TowerRefit.Result result, long gameTime) {
        CompoundTag write(long key) {
            CompoundTag t = new CompoundTag();
            t.putLong("start", key);
            t.putInt("version", version);
            t.putInt("changed", result.changed());
            t.putInt("already", result.already());
            t.putInt("skipped", result.skipped());
            t.putInt("removed", result.removed());
            t.putInt("kept", result.kept());
            t.putLong("time", gameTime);
            return t;
        }

        static Entry read(CompoundTag t) {
            return new Entry(t.getInt("version"), new TowerRefit.Result(t.getInt("changed"), t.getInt("already"),
                    t.getInt("skipped"), t.getInt("removed"), t.getInt("kept"),
                    t.getInt("version") == TowerRefit.OLD_DESIGN), t.getLong("time"));
        }
    }

    private final Map<Long, Entry> towers = new LinkedHashMap<>();

    public static SavedData.Factory<TowerRefits> factory() {
        return new SavedData.Factory<>(TowerRefits::new, TowerRefits::load, null);
    }

    /** The records of this level (the Overworld's), made empty if there are none yet. */
    public static TowerRefits get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), NAME);
    }

    private static TowerRefits load(CompoundTag tag, HolderLookup.Provider registries) {
        TowerRefits store = new TowerRefits();
        for (Tag t : tag.getList("towers", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            store.towers.put(c.getLong("start"), Entry.read(c));
        }
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        towers.forEach((key, entry) -> list.add(entry.write(key)));
        tag.put("towers", list);
        return tag;
    }

    /** The record of the tower whose start is in chunk {@code key}, or null if the refit has not been to it. */
    @Nullable
    public Entry get(long key) {
        return towers.get(key);
    }

    /** Notes what a refit did to the tower of {@code key}. */
    public void record(long key, Entry entry) {
        towers.put(key, entry);
        setDirty();
    }

    /** Forgets a tower (a test's, when it ends). */
    public void forget(long key) {
        if (towers.remove(key) != null) {
            setDirty();
        }
    }
}
