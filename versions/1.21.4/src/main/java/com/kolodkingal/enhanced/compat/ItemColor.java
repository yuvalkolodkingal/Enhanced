package com.kolodkingal.enhanced.compat;

import net.minecraft.world.item.ItemStack;

/** 1.21.4 only: 1.21.1's item colour (a tint per tint index), drawn through {@link LegacyItems}' tint source. */
@FunctionalInterface
public interface ItemColor {
    int getColor(ItemStack stack, int tintIndex);
}
