package com.kolodkingal.enhanced.hydra;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * A Hydra soldier (3.6) of the Hydra outposts: a rifleman, an enforcer or a sniper (its rank, lane L8's), in black
 * tactical gear. A monster (Peaceful removes it, the Snap counts it). Registered by the 3.6 skeleton with the
 * rifleman's attributes and no mind of its own; lane L8 owns the class (ranks, weapons, AI, the garrison, the alarm).
 */
public class HydraSoldierEntity extends Monster {
    public HydraSoldierEntity(EntityType<? extends HydraSoldierEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.ARMOR, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    @Override
    protected void registerGoals() {
        // (Its goals are lane L8's (HydraGoals))
    }
}
