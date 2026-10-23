package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

/** A player throws an item away; cancelling gives it back. */
public class ItemTossEvent extends EntityEvent implements Cancellable {
    private final Player player;
    private boolean canceled;

    public ItemTossEvent(ItemEntity entity, Player player) {
        super(entity);
        this.player = player;
    }

    @Override
    public ItemEntity getEntity() {
        return (ItemEntity) super.getEntity();
    }

    public Player getPlayer() {
        return player;
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
