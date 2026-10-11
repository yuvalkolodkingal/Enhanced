package com.kolodkingal.enhanced.apocalypse;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Apocalypse, the boss: En Sabah Nur, the first mutant. A stub: the body, the brain and the attacks are built by the
 * Apocalypse lane (DESIGN section 17); until then it is only registered (with its attributes) and cannot be summoned.
 */
public class ApocalypseEntity extends Monster {
    public ApocalypseEntity(EntityType<? extends ApocalypseEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 900.0);
    }
}
