package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** A player attacks an entity; cancelling stops the attack. */
public class AttackEntityEvent extends PlayerEvent implements Cancellable {
    private final Entity target;
    private boolean canceled;

    public AttackEntityEvent(Player entity, Entity target) {
        super(entity);
        this.target = target;
    }

    public Entity getTarget() {
        return target;
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
