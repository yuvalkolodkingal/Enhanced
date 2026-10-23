package com.kolodkingal.enhanced.platform.event;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** An item's tooltip is being built (client side; the player may be null): add lines to getToolTip(). */
public class ItemTooltipEvent extends PlayerEvent {
    private final ItemStack itemStack;
    private final List<Component> toolTip;
    private final TooltipFlag flags;
    private final Item.TooltipContext context;

    public ItemTooltipEvent(
            ItemStack itemStack, Player entity, List<Component> toolTip, TooltipFlag flags,
            Item.TooltipContext context) {
        super(entity);
        this.itemStack = itemStack;
        this.toolTip = toolTip;
        this.flags = flags;
        this.context = context;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public List<Component> getToolTip() {
        return toolTip;
    }

    public TooltipFlag getFlags() {
        return flags;
    }

    public Item.TooltipContext getContext() {
        return context;
    }
}
