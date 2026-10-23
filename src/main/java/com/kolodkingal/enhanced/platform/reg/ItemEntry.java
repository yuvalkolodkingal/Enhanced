package com.kolodkingal.enhanced.platform.reg;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/** A registered item of the mod (NeoForge's {@code DeferredItem}). */
public interface ItemEntry<I extends Item> extends RegEntry<Item, I>, ItemLike {
    /** A stack of one. */
    ItemStack toStack();

    ItemStack toStack(int count);
}
