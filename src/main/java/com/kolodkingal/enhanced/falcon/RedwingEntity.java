package com.kolodkingal.enhanced.falcon;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Redwing, Falcon's drone (3.8, PLAN 8.4): it flies over his shoulder, marks monsters and zaps them; shot down, it
 * drops into his pack for repairs. Registered by the 3.8 skeleton with its attributes ({@code [falcon] redwingHealth}'s
 * 12), hovering in place (drawn as a placeholder box); lane L5 owns the class.
 */
public class RedwingEntity extends PathfinderMob {
    public RedwingEntity(EntityType<? extends RedwingEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }
}
