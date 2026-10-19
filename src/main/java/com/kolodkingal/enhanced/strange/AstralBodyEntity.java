package com.kolodkingal.enhanced.strange;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Doctor Strange's body while his spirit is out (3.6, the Astral Projection): a copy of his look, kneeling, eyes
 * closed, with 20 health of its own; damage to it calls him back. Never saved. Registered by the 3.6 skeleton as an
 * inert creature (drawn as nothing); lane L3 owns the class and its renderer.
 */
public class AstralBodyEntity extends PathfinderMob {
    public AstralBodyEntity(EntityType<? extends AstralBodyEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        // (It does not move: lane L3's)
    }
}
