package com.kolodkingal.enhanced.compat;
//#norenames

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * 1.21.5: saved data is a {@link SavedDataType} with a codec instead of a factory of load and save; the mod's saved
 * data keep their tag-based {@code load} and {@code save}, and this makes the codec of them.
 */
public final class SavedBridge {
    @FunctionalInterface
    public interface Saver<T> {
        CompoundTag save(T data, CompoundTag tag, HolderLookup.Provider registries);
    }

    private SavedBridge() {
    }

    public static <T extends SavedData> SavedDataType<T> type(String id, Supplier<T> fresh,
                                                               BiFunction<CompoundTag, HolderLookup.Provider, T> load,
                                                               Saver<T> save) {
        return new SavedDataType<>(id, ctx -> fresh.get(), ctx -> {
            HolderLookup.Provider registries = ctx != null ? ctx.registryAccess() : ValueBridge.registries();
            return CompoundTag.CODEC.xmap(tag -> load.apply(tag, registries), data -> save.save(data, new CompoundTag(), registries));
        });
    }
}
