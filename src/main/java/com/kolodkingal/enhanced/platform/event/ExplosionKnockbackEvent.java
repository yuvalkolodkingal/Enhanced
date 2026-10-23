package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** An explosion throws an entity: change how hard. */
public class ExplosionKnockbackEvent extends ExplosionEvent {
    private final Entity affectedEntity;
    private Vec3 knockbackVelocity;

    public ExplosionKnockbackEvent(Level level, Explosion explosion, Entity affectedEntity, Vec3 knockbackVelocity) {
        super(level, explosion);
        this.affectedEntity = affectedEntity;
        this.knockbackVelocity = knockbackVelocity;
    }

    public Entity getAffectedEntity() {
        return affectedEntity;
    }

    public Vec3 getKnockbackVelocity() {
        return knockbackVelocity;
    }

    public void setKnockbackVelocity(Vec3 knockbackVelocity) {
        this.knockbackVelocity = knockbackVelocity;
    }
}
