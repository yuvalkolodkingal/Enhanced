package com.kolodkingal.enhanced.ultron;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Ultron, the boss: Prime and the bodies he is uploaded into. A stub: the body, the brain and the attacks are built by
 * the Ultron lane (DESIGN section 16); until then it is only registered (with its attributes) and cannot be summoned.
 */
public class UltronEntity extends Monster {
    public UltronEntity(EntityType<? extends UltronEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 600.0);
    }
}
