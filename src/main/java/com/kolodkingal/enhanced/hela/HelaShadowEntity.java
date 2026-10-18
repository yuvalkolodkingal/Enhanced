package com.kolodkingal.enhanced.hela;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * A Shadow of Hela (3.5): a small wisp of black-green smoke that hides in the city during a player's Guardian's Watch
 * (lane L2 spawns and judges them; one hit of the watcher's strikes it). The 3.5 skeleton registers the entity (so that lane
 * L2 can spawn it before lane L4 lands); lane L4 owns this class and its renderer (smoke only).
 */
public class HelaShadowEntity extends PathfinderMob {
    public HelaShadowEntity(EntityType<? extends HelaShadowEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1.0).add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
