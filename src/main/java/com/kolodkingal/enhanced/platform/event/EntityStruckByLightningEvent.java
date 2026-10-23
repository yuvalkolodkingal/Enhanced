package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;

/** An entity is about to be struck by lightning; cancelling spares it. */
public class EntityStruckByLightningEvent extends EntityEvent implements Cancellable {
    private final LightningBolt lightning;
    private boolean canceled;

    public EntityStruckByLightningEvent(Entity entity, LightningBolt lightning) {
        super(entity);
        this.lightning = lightning;
    }

    public LightningBolt getLightning() {
        return lightning;
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
