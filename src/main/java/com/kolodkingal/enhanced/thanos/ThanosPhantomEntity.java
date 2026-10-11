package com.kolodkingal.enhanced.thanos;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * An illusion of Thanos that his Reality Stone throws up. A stub: the body, the brain and the attacks are built by the
 * Thanos lane (DESIGN section 15); until then it is only registered (with its attributes) and cannot be summoned.
 */
public class ThanosPhantomEntity extends Monster {
    public ThanosPhantomEntity(EntityType<? extends ThanosPhantomEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0);
    }
}
