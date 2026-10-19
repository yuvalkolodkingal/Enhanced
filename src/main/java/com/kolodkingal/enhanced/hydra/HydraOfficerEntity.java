package com.kolodkingal.enhanced.hydra;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * A Hydra officer (3.6): he commands an outpost from its command room by day and its radio room by night, raises the
 * alarm the moment he sees an intruder, and fires an energy pistol. Registered by the 3.6 skeleton with his attributes
 * and no mind of his own; lane L8 owns the class.
 */
public class HydraOfficerEntity extends Monster {
    public HydraOfficerEntity(EntityType<? extends HydraOfficerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 36.0).add(Attributes.ARMOR, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    protected void registerGoals() {
        // (His goals are lane L8's (HydraGoals))
    }
}
