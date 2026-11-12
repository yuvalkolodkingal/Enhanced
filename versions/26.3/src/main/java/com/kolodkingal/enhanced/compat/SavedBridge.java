package com.kolodkingal.enhanced.compat;
//#norenames

import com.kolodkingal.enhanced.Enhanced;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * 1.21.5: saved data is a {@link SavedDataType} with a codec instead of a factory of load and save; the mod's saved
 * data keep their tag-based {@code load} and {@code save}, and this makes the codec of them. 26.3: a saved data's id is
 * an {@link Identifier} (the file is {@code data/enhanced/<id>.dat} of its dimension's folder), and 26.3 moves only its
 * own files when it upgrades a world: data the mod saved before (the file {@code <id>.dat} of the dimension's data
 * folder of 1.21.11's layout) is read from there when there is no file of 26.3's yet.
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
        return new SavedDataType<>(Identifier.fromNamespaceAndPath("enhanced", id), level -> {
            T legacy = level == null ? null : legacy(level, id, load);
            return legacy != null ? legacy : fresh.get();
        }, level -> {
            HolderLookup.Provider registries = level != null ? level.registryAccess() : ValueBridge.registries();
            return CompoundTag.CODEC.xmap(tag -> load.apply(tag, registries), data -> save.save(data, new CompoundTag(), registries));
        });
    }

    /** The data saved by an earlier version (1.21.11's layout), or null. */
    private static <T extends SavedData> T legacy(ServerLevel level, String id,
                                                  BiFunction<CompoundTag, HolderLookup.Provider, T> load) {
        Path root = level.getServer().getWorldPath(LevelResource.ROOT);
        Path folder;
        if (level.dimension() == Level.OVERWORLD) {
            folder = root.resolve("data");
        } else if (level.dimension() == Level.NETHER) {
            folder = root.resolve("DIM-1").resolve("data");
        } else if (level.dimension() == Level.END) {
            folder = root.resolve("DIM1").resolve("data");
        } else {
            Identifier dim = level.dimension().identifier();
            folder = root.resolve("dimensions").resolve(dim.getNamespace()).resolve(dim.getPath()).resolve("data");
        }
        Path file = folder.resolve(id + ".dat");
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            T data = load.apply(tag.getCompoundOrEmpty("data"), level.registryAccess());
            data.setDirty();
            Enhanced.LOGGER.info("Read the mod's saved data {} of an earlier version from {}", id, file);
            return data;
        } catch (Exception e) {
            Enhanced.LOGGER.warn("Could not read the mod's saved data {} of an earlier version from {}", id, file, e);
            return null;
        }
    }
}
