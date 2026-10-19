package com.kolodkingal.enhanced.vormir;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * The Stonekeeper of the Vormir Cliff (3.6): the hooded keeper of the Soul Stone, hovering at his place on the summit;
 * invulnerable, never pushed, never leaving. Registered by the 3.6 skeleton as an inert creature (drawn as nothing);
 * lane L5 owns the class, its model, renderer and lines.
 */
public class StonekeeperEntity extends PathfinderMob {
    public StonekeeperEntity(EntityType<? extends StonekeeperEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        // (He looks at the nearest player and bobs: lane L5's)
    }
}
