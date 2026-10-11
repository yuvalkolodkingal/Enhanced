package com.kolodkingal.enhanced.ultron;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * An Ultron Sentry: a flying robot, his army, and the hero's drone. A stub: the body, the brain and the attacks are
 * built by the Ultron lane (DESIGN section 16); until then it is only registered (with its attributes) and cannot be
 * summoned.
 */
public class UltronSentryEntity extends Monster {
    public UltronSentryEntity(EntityType<? extends UltronSentryEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0);
    }
}
