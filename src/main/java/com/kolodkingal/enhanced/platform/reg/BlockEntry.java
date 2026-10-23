package com.kolodkingal.enhanced.platform.reg;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

/** A registered block of the mod (NeoForge's {@code DeferredBlock}). */
public interface BlockEntry<B extends Block> extends RegEntry<Block, B>, ItemLike {
    /** A stack of one of its item. */
    ItemStack toStack();

    ItemStack toStack(int count);
}
