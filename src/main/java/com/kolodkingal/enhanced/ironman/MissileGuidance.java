package com.kolodkingal.enhanced.ironman;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * How a mini-missile steers (3.8, PLAN 6.3): {@code IronMissileEntity} asks it every tick for its next motion. The 3.8
 * skeleton's missiles fly straight (the motion they have); lane L4 writes the Mark 85's homing (lock, turn 12 degrees a
 * tick, retarget, pop at 200 ticks) and keeps the signature.
 */
public final class MissileGuidance {
    /** The missile's next motion. Skeleton: straight on. */
    public static Vec3 steer(Entity missile) {
        return missile.getDeltaMovement();
    }

    private MissileGuidance() {
    }
}
