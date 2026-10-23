package com.kolodkingal.enhanced.platform.attach;

/** The loader's side of {@link Attach} (SPI: gameplay code uses {@link AttachKey}). */
public interface AttachService {
    /** Registers the attachment type {@code spec} describes (now or at the loader's registration time). */
    <T> AttachKey.Handle<T> register(Attach.Spec<T> spec);

    /** The handle of {@code PERSISTENT_DATA}: the compound that replaces NeoForge's {@code getPersistentData()}. */
    AttachKey.Handle<net.minecraft.nbt.CompoundTag> persistentData(Attach.Spec<net.minecraft.nbt.CompoundTag> spec);
}
