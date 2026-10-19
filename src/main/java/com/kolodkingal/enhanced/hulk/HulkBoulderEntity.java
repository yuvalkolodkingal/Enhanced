package com.kolodkingal.enhanced.hulk;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

/**
 * The boulder the Hulk throws (3.6, B): it looks like the block he stood on (no block is taken) and shatters where it
 * hits. Registered by the 3.6 skeleton as an inert projectile (drawn as nothing); lane L7 owns the class and its
 * renderer.
 */
public class HulkBoulderEntity extends Projectile {
    public HulkBoulderEntity(EntityType<? extends HulkBoulderEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }
}
