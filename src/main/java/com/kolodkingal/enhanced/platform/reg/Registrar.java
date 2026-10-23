package com.kolodkingal.enhanced.platform.reg;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registers the mod's objects of one registry, in the order the code declares them (NeoForge's {@code DeferredRegister},
 * whose method names it keeps). Made by {@link Registrars}.
 */
public interface Registrar<T> {
    <I extends T> RegEntry<T, I> register(String name, Supplier<? extends I> factory);

    <I extends T> RegEntry<T, I> register(String name, Function<ResourceLocation, ? extends I> factory);

    /** Every entry, in the order they were declared. */
    Collection<RegEntry<T, ? extends T>> getEntries();

    ResourceKey<? extends Registry<T>> getRegistryKey();

    String getNamespace();
}
