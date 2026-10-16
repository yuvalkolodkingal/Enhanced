package com.kolodkingal.enhanced.vishanti;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Every ward of one dimension, saved with the world ({@code data/enhanced_wards.dat} of the dimension): wards persist
 * across restarts and work whenever the chunks round their base are loaded, whoever is online. There are few of them (a
 * handful per player), so a list is all the lookup needs.
 */
public final class WardStore extends SavedData {
    public static final String NAME = "enhanced_wards";

    private final List<Ward> wards = new ArrayList<>();

    public static SavedData.Factory<WardStore> factory() {
        return new SavedData.Factory<>(WardStore::new, WardStore::load, null);
    }

    /** The store of this level, made empty if there is none yet. */
    public static WardStore get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(factory(), NAME);
    }

    /** The store of this level if it has one (nothing is made: what every tick asks first), else null. */
    @Nullable
    public static WardStore peek(ServerLevel level) {
        return level.getDataStorage().get(factory(), NAME);
    }

    private static WardStore load(CompoundTag tag, HolderLookup.Provider registries) {
        WardStore store = new WardStore();
        Tag list = tag.get("wards");
        if (list != null) {
            Ward.CODEC.listOf().parse(NbtOps.INSTANCE, list).resultOrPartial(error -> { }).ifPresent(store.wards::addAll);
        }
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("wards", Ward.CODEC.listOf().encodeStart(NbtOps.INSTANCE, wards).getOrThrow());
        return tag;
    }

    public List<Ward> wards() {
        return Collections.unmodifiableList(wards);
    }

    public boolean isEmpty() {
        return wards.isEmpty();
    }

    /** Adds the ward, or replaces the ward of that owner's base of the same name; true if the store changed. */
    public boolean put(Ward ward) {
        for (int i = 0; i < wards.size(); i++) {
            if (wards.get(i).is(ward.owner(), ward.name())) {
                if (wards.get(i).equals(ward)) {
                    return false;
                }
                wards.set(i, ward);
                setDirty();
                return true;
            }
        }
        wards.add(ward);
        setDirty();
        return true;
    }

    /** Removes the ward of that owner's base; true if there was one. */
    public boolean remove(UUID owner, String name) {
        boolean removed = wards.removeIf(ward -> ward.is(owner, name));
        if (removed) {
            setDirty();
        }
        return removed;
    }

    /** Removes every ward of the owner for which {@code keep} is false; returns how many went. */
    public int retain(UUID owner, Predicate<Ward> keep) {
        int before = wards.size();
        wards.removeIf(ward -> ward.owner().equals(owner) && !keep.test(ward));
        int removed = before - wards.size();
        if (removed > 0) {
            setDirty();
        }
        return removed;
    }

    /** The ward that protects this point, or null. */
    @Nullable
    public Ward at(Vec3 point) {
        for (Ward ward : wards) {
            if (ward.contains(point)) {
                return ward;
            }
        }
        return null;
    }
}
