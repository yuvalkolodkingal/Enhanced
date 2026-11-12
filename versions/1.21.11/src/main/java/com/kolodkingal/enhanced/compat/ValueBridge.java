package com.kolodkingal.enhanced.compat;
//#norenames

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.lang.reflect.Field;

/**
 * 1.21.11 only: from 1.21.6 an entity and a block entity are saved through a {@code ValueOutput} and loaded from a
 * {@code ValueInput}, not a {@code CompoundTag}. Both are the tag's (the game's only implementations are
 * {@code TagValueOutput} and {@code TagValueInput}, which write into and read from one compound), so the mod's
 * CompoundTag code reads and writes that compound itself: the same keys in the same place as before.
 */
public final class ValueBridge {
    private static final Field OUTPUT = field(TagValueOutput.class);
    private static final Field INPUT = field(TagValueInput.class);

    private ValueBridge() {
    }

    private static Field field(Class<?> owner) {
        for (Field f : owner.getDeclaredFields()) {
            if (f.getType() == CompoundTag.class && !java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                f.setAccessible(true);
                return f;
            }
        }
        throw new IllegalStateException("no compound in " + owner);
    }

    /** The compound a value output writes into. */
    public static CompoundTag tag(ValueOutput output) {
        if (output instanceof TagValueOutput) {
            try {
                return (CompoundTag) OUTPUT.get(output);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        return new CompoundTag(); // (no other kind exists in 1.21.11; what is written here is not kept)
    }

    /** The compound a value input reads from. */
    public static CompoundTag tag(ValueInput input) {
        if (input instanceof TagValueInput) {
            try {
                return (CompoundTag) INPUT.get(input);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        return new CompoundTag();
    }

    /** The registries a block entity is saved with (1.21.1's {@code saveAdditional} was given them). */
    public static HolderLookup.Provider registries(BlockEntity be) {
        if (be.getLevel() != null) {
            return be.getLevel().registryAccess();
        }
        return registries();
    }

    /** The registries of the running server (or, failing that, the built-in ones). */
    public static HolderLookup.Provider registries() {
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return server.registryAccess();
        }
        return RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
    }

    /** A value output that writes into {@code tag}'s own new compound (read it back with {@link #tag(ValueOutput)}). */
    public static TagValueOutput output(HolderLookup.Provider registries) {
        return TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
    }

    /** A value input that reads {@code tag}. */
    public static ValueInput input(HolderLookup.Provider registries, CompoundTag tag) {
        return TagValueInput.create(ProblemReporter.DISCARDING, registries, tag);
    }

    /** 1.21.1's {@code entity.saveWithoutId(tag)}: the entity's data written into {@code tag}, which is returned. */
    public static CompoundTag saveWithoutId(Entity entity, CompoundTag tag) {
        TagValueOutput out = output(entity.registryAccess());
        entity.saveWithoutId(out);
        tag.merge(out.buildResult());
        return tag;
    }

    /** 1.21.1's {@code entity.load(tag)}. */
    public static void load(Entity entity, CompoundTag tag) {
        entity.load(input(entity.registryAccess(), tag));
    }
}
