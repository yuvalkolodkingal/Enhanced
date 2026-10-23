package com.kolodkingal.enhanced.platform.reg;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;
import java.util.function.Supplier;

/** The mod's blocks (NeoForge's {@code DeferredRegister.Blocks}). */
public interface BlockRegistrar extends Registrar<Block> {
    @Override
    <B extends Block> BlockEntry<B> register(String name, Supplier<? extends B> factory);

    @Override
    <B extends Block> BlockEntry<B> register(String name, Function<ResourceLocation, ? extends B> factory);

    <B extends Block> BlockEntry<B> registerBlock(String name, Function<BlockBehaviour.Properties, ? extends B> factory,
                                                  BlockBehaviour.Properties properties);

    <B extends Block> BlockEntry<B> registerBlock(String name, Function<BlockBehaviour.Properties, ? extends B> factory);

    BlockEntry<Block> registerSimpleBlock(String name, BlockBehaviour.Properties properties);

    BlockEntry<Block> registerSimpleBlock(String name);
}
