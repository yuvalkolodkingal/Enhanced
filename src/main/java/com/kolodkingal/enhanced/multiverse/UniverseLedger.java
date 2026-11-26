package com.kolodkingal.enhanced.multiverse;

import com.kolodkingal.enhanced.thanos.StoneKind;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Which stones of the other universes have been found (3.19), saved with the Overworld: for each universe and kind of
 * stone, the copy that is the stone ({@link UniverseStones}). Every other copy of it fades away.
 */
public final class UniverseLedger extends SavedData {
    public static final String NAME = "enhanced_universe_stones";

    /** "universe/kind" -> the copy that was found. */
    private final Map<String, String> found = new HashMap<>();

    public static SavedData.Factory<UniverseLedger> factory() {
        return new SavedData.Factory<>(UniverseLedger::new, UniverseLedger::load, null);
    }

    /** The ledger of the server (kept with the Overworld). */
    public static UniverseLedger get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), NAME);
    }

    private static UniverseLedger load(CompoundTag tag, HolderLookup.Provider registries) {
        UniverseLedger ledger = new UniverseLedger();
        CompoundTag lines = tag.getCompound("found");
        for (String line : lines.getAllKeys()) {
            ledger.found.put(line, lines.getString(line));
        }
        return ledger;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag lines = new CompoundTag();
        found.forEach(lines::putString);
        tag.put("found", lines);
        return tag;
    }

    /** The copy of that universe's stone that was found, or null if nobody has found it yet. */
    @Nullable
    public String finder(int universe, StoneKind kind) {
        return found.get(UniverseStones.key(universe, kind));
    }

    /** Writes the copy as that universe's stone. */
    public void found(int universe, StoneKind kind, String copy) {
        found.put(UniverseStones.key(universe, kind), copy);
        setDirty();
    }

    /** Forgets every stone found (the command {@code /enhanced multiverse reset_stones}). */
    public void reset() {
        found.clear();
        setDirty();
    }
}
