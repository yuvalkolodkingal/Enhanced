package com.kolodkingal.enhanced.platform.reg;
//#norenames

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * A registered object of the mod (port P0, PLAN2 3.3), what NeoForge's {@code DeferredHolder<R, T>} was: a holder of
 * the registry's type {@code R} and a {@link Supplier} of the object's own type {@code T}. 26.3: {@link Holder} is a
 * sealed interface, so this is no longer one itself; on NeoForge it is still a {@code DeferredHolder} (the same object
 * as before), which {@link #holder()} gives as the holder, where a holder is wanted.
 */
public interface RegEntry<R, T extends R> extends Supplier<T> {
    /** The object (the registry must have been filled). */
    @Override
    T get();

    /** Its id, such as {@code enhanced:power}. */
    Identifier getId();

    /** Its key in the registry. */
    ResourceKey<R> getKey();

    /** The registry's object (the holder's value). */
    R value();

    Optional<ResourceKey<R>> unwrapKey();

    boolean is(TagKey<R> tag);

    boolean is(ResourceKey<R> key);

    /** This entry as a plain holder. */
    @SuppressWarnings("unchecked")
    default Holder<R> holder() {
        return (Holder<R>) (Object) this;
    }
}
