package com.kolodkingal.enhanced.platform.event;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

/** A player picks up an item entity. */
public abstract class ItemEntityPickupEvent {
    private final Player player;
    private final ItemEntity itemEntity;

    protected ItemEntityPickupEvent(Player player, ItemEntity itemEntity) {
        this.player = player;
        this.itemEntity = itemEntity;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemEntity getItemEntity() {
        return itemEntity;
    }

    /** A player touches an item on the ground: setCanPickup(FALSE) leaves it there. */
    public static class Pre extends ItemEntityPickupEvent {
        private TriState canPickup;

        public Pre(Player player, ItemEntity itemEntity) {
            super(player, itemEntity);
            this.canPickup = TriState.DEFAULT;
        }

        public TriState canPickup() {
            return canPickup;
        }

        public void setCanPickup(TriState canPickup) {
            this.canPickup = canPickup;
        }
    }
}
