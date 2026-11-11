package com.kolodkingal.enhanced.compat;

import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;

import java.util.UUID;

/** 1.21.5: an owned entity knows its owner by an entity reference; the mod asks for the owner's UUID. */
public final class Owners {
    private Owners() {
    }

    public static UUID uuid(OwnableEntity pet) {
        EntityReference<LivingEntity> ref = pet.getOwnerReference();
        return ref == null ? null : ref.getUUID();
    }
}
