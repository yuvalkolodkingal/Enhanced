package com.kolodkingal.enhanced.compat;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.Util;

import java.util.UUID;

/** 1.21.5: a tag has no UUID helpers of its own; the UUID is stored with its codec (the same int array as before). */
public final class Tags {
    private Tags() {
    }

    public static void putUUID(CompoundTag tag, String key, UUID id) {
        if (id != null) {
            tag.store(key, UUIDUtil.CODEC, id);
        }
    }

    public static UUID getUUID(CompoundTag tag, String key) {
        return tag.read(key, UUIDUtil.CODEC).orElse(Util.NIL_UUID);
    }

    public static boolean hasUUID(CompoundTag tag, String key) {
        return tag.read(key, UUIDUtil.CODEC).isPresent();
    }
}
