package com.kolodkingal.enhanced.compat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * 1.21.4 only: an item's or block's properties must carry its id before it is made. The registrars make each entry
 * inside {@link #item} or {@link #block}, and the properties made meanwhile take that id
 * ({@code mixin.ItemPropertiesMixin}, {@code mixin.BlockPropertiesMixin}), so the mod's factories stay as they were.
 */
public final class Ids {
    private static final ThreadLocal<ResourceKey<Item>> ITEM = new ThreadLocal<>();
    private static final ThreadLocal<ResourceKey<Block>> BLOCK = new ThreadLocal<>();

    private Ids() {
    }

    public static <T> T item(String namespace, String name, Supplier<T> factory) {
        ResourceKey<Item> before = ITEM.get();
        ITEM.set(ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, name)));
        try {
            return factory.get();
        } finally {
            ITEM.set(before);
        }
    }

    public static <T> T block(String namespace, String name, Supplier<T> factory) {
        ResourceKey<Block> before = BLOCK.get();
        BLOCK.set(ResourceKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(namespace, name)));
        try {
            return factory.get();
        } finally {
            BLOCK.set(before);
        }
    }

    public static @Nullable ResourceKey<Item> currentItem() {
        return ITEM.get();
    }

    public static @Nullable ResourceKey<Block> currentBlock() {
        return BLOCK.get();
    }
}
