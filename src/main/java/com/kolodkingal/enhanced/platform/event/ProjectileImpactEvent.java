package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;

/** A projectile hits something; cancelling lets it fly on. */
public class ProjectileImpactEvent extends EntityEvent implements Cancellable {
    private final HitResult rayTraceResult;
    private boolean canceled;

    public ProjectileImpactEvent(Projectile entity, HitResult rayTraceResult) {
        super(entity);
        this.rayTraceResult = rayTraceResult;
    }

    @Override
    public Projectile getEntity() {
        return (Projectile) super.getEntity();
    }

    public HitResult getRayTraceResult() {
        return rayTraceResult;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public Projectile getProjectile() {
        return getEntity();
    }
}
