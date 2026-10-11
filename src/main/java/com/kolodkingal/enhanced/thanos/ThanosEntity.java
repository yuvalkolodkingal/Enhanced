package com.kolodkingal.enhanced.thanos;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Thanos, the Mad Titan, the boss of the Infinity Stones. A stub: the body, the brain and the attacks are built by the
 * Thanos lane (DESIGN section 15); until then it is only registered (with its attributes) and cannot be summoned.
 */
public class ThanosEntity extends Monster {
    public ThanosEntity(EntityType<? extends ThanosEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 3000.0);
    }
}
