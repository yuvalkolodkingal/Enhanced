package com.kolodkingal.enhanced.platform;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Multipart entities: an entity that is one part of a bigger one (the Ender Dragon's head, another mod's boss). On
 * NeoForge a part is a {@code PartEntity}; on Fabric the dragon's {@code EnderDragonPart}.
 */
public interface Parts {
    /** Whether {@code entity} is a part of another entity. */
    boolean isPart(Entity entity);

    /** The entity {@code entity} is a part of, or null if it is no part. */
    @Nullable
    Entity parentOf(Entity entity);

    /** The whole entity: the parent of a part, or the entity itself. */
    default Entity root(Entity entity) {
        Entity parent = parentOf(entity);
        return parent != null ? parent : entity;
    }
}
