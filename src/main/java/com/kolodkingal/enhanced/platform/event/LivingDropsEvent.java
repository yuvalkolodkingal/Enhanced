package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.Collection;

/** What a dead living entity drops (a live collection); cancelling drops nothing. */
public class LivingDropsEvent extends LivingEvent implements Cancellable {
    private final DamageSource source;
    private final Collection<ItemEntity> drops;
    private final boolean recentlyHit;
    private boolean canceled;

    public LivingDropsEvent(
            LivingEntity entity, DamageSource source, Collection<ItemEntity> drops, boolean recentlyHit) {
        super(entity);
        this.source = source;
        this.drops = drops;
        this.recentlyHit = recentlyHit;
    }

    public DamageSource getSource() {
        return source;
    }

    public Collection<ItemEntity> getDrops() {
        return drops;
    }

    public boolean isRecentlyHit() {
        return recentlyHit;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}
