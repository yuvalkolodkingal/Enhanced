package com.kolodkingal.enhanced.platform.reg;

import net.minecraft.core.component.DataComponentType;

import java.util.function.UnaryOperator;

/** The mod's data components (NeoForge's {@code DeferredRegister.DataComponents}). */
public interface ComponentRegistrar extends Registrar<DataComponentType<?>> {
    <D> RegEntry<DataComponentType<?>, DataComponentType<D>> registerComponentType(
            String name, UnaryOperator<DataComponentType.Builder<D>> builder);
}
