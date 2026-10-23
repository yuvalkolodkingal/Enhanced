package com.kolodkingal.enhanced.platform.reg;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * A registered object of the mod (port P0, PLAN2 3.3), what NeoForge's {@code DeferredHolder<R, T>} was: a
 * {@link Holder} of the registry's type {@code R} (so it goes wherever a holder does, such as a
 * {@code MobEffectInstance}) and a {@link Supplier} of the object's own type {@code T}. On NeoForge it is a
 * {@code DeferredHolder} (the same object as before), so it behaves exactly as one.
 */
public interface RegEntry<R, T extends R> extends Holder<R>, Supplier<T> {
    /** The object (the registry must have been filled). */
    @Override
    T get();

    /** Its id, such as {@code enhanced:power}. */
    ResourceLocation getId();

    /** Its key in the registry. */
    ResourceKey<R> getKey();

    /** This entry as a plain holder. */
    default Holder<R> holder() {
        return this;
    }
}
