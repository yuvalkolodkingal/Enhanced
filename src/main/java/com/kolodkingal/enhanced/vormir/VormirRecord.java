package com.kolodkingal.enhanced.vormir;

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The world's memory of the Soul Stone for {@code [soul_stone] onePer = world} (3.6, the plan's 6.4): who was given the
 * one stone of the world, if anyone (saved on the Overworld as {@code enhanced_vormir}). After the first sacrifice
 * every other cliff's keeper says "The stone has been taken." {@link #release} gives the stone back to the cliffs (a
 * lost Soul Stone, an operator).
 */
public final class VormirRecord extends SavedData {
    private static final String NAME = Enhanced.MODID + "_vormir";

    @Nullable
    private UUID holder;

    private static SavedData.Factory<VormirRecord> factory() {
        return new SavedData.Factory<>(VormirRecord::new, VormirRecord::load, null);
    }

    /** The world's record. */
    public static VormirRecord get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), NAME);
    }

    private static VormirRecord load(CompoundTag tag, HolderLookup.Provider registries) {
        VormirRecord record = new VormirRecord();
        if (tag.hasUUID("holder")) {
            record.holder = tag.getUUID("holder");
        }
        return record;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (holder != null) {
            tag.putUUID("holder", holder);
        }
        return tag;
    }

    /** Whether the world's Soul Stone has been given. */
    public boolean taken() {
        return holder != null;
    }

    /** Who was given it, or null. */
    @Nullable
    public UUID holder() {
        return holder;
    }

    /** The world's Soul Stone has been given to {@code player}. */
    public void take(UUID player) {
        holder = player;
        setDirty();
    }

    /** The world's Soul Stone is back at the cliffs. */
    public static void release(MinecraftServer server) {
        VormirRecord record = get(server);
        if (record.holder != null) {
            record.holder = null;
            record.setDirty();
        }
    }
}
