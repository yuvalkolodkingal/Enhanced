package com.kolodkingal.enhanced.platform.reg;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.Function;
import java.util.function.Supplier;

/** The mod's items (NeoForge's {@code DeferredRegister.Items}). */
public interface ItemRegistrar extends Registrar<Item> {
    @Override
    <I extends Item> ItemEntry<I> register(String name, Supplier<? extends I> factory);

    @Override
    <I extends Item> ItemEntry<I> register(String name, Function<ResourceLocation, ? extends I> factory);

    ItemEntry<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block, Item.Properties properties);

    ItemEntry<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block);

    ItemEntry<BlockItem> registerSimpleBlockItem(Holder<Block> block, Item.Properties properties);

    ItemEntry<BlockItem> registerSimpleBlockItem(Holder<Block> block);

    <I extends Item> ItemEntry<I> registerItem(String name, Function<Item.Properties, ? extends I> factory,
                                               Item.Properties properties);

    <I extends Item> ItemEntry<I> registerItem(String name, Function<Item.Properties, ? extends I> factory);

    ItemEntry<Item> registerSimpleItem(String name, Item.Properties properties);

    ItemEntry<Item> registerSimpleItem(String name);
}
