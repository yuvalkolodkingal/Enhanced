package com.kolodkingal.enhanced.platform.attach;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * A piece of data the mod keeps on an entity or a level (port P0, PLAN2 3.3): {@code ModAttachments.POWER.get(player)}
 * instead of NeoForge's {@code player.getData(ModAttachments.POWER)}. Made by {@link Attach#builder}. On NeoForge it is
 * an {@code AttachmentType} with the same id, so existing worlds keep their data.
 */
public final class AttachKey<T> {
    /** The loader's attachment behind a key. The holder is an {@link Entity} or a {@link Level}. */
    public interface Handle<T> {
        T get(Object holder);

        @Nullable
        T set(Object holder, T value);

        boolean has(Object holder);

        @Nullable
        T remove(Object holder);

        Optional<T> getExisting(Object holder);

        void sync(Object holder);
    }

    private final ResourceLocation id;
    private final Handle<T> handle;

    AttachKey(ResourceLocation id, Handle<T> handle) {
        this.id = id;
        this.handle = handle;
    }

    public ResourceLocation id() {
        return id;
    }

    /** The loader's handle (for the loader's own glue, such as the GameTests' sync checks). */
    public Handle<T> handle() {
        return handle;
    }

    /** The holder's value, made from the default (and kept) if it has none yet. */
    public T get(Entity holder) {
        return handle.get(holder);
    }

    public T get(Level holder) {
        return handle.get(holder);
    }

    /** Sets the holder's value (synced, if the attachment is); returns the previous one, or null. */
    @Nullable
    public T set(Entity holder, T value) {
        return handle.set(holder, value);
    }

    @Nullable
    public T set(Level holder, T value) {
        return handle.set(holder, value);
    }

    /** Whether the holder has a value (without making one). */
    public boolean has(Entity holder) {
        return handle.has(holder);
    }

    public boolean has(Level holder) {
        return handle.has(holder);
    }

    /** Removes the holder's value; returns it, or null. */
    @Nullable
    public T remove(Entity holder) {
        return handle.remove(holder);
    }

    @Nullable
    public T remove(Level holder) {
        return handle.remove(holder);
    }

    /** The holder's value if it has one (without making one). */
    public Optional<T> getExisting(Entity holder) {
        return handle.getExisting(holder);
    }

    public Optional<T> getExisting(Level holder) {
        return handle.getExisting(holder);
    }

    /** Sends the holder's value again (after a change made inside a mutable value). */
    public void sync(Entity holder) {
        handle.sync(holder);
    }

    public void sync(Level holder) {
        handle.sync(holder);
    }

    @Override
    public String toString() {
        return "AttachKey{" + id + "}";
    }
}
