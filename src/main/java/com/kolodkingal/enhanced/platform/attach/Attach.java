package com.kolodkingal.enhanced.platform.attach;

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.Platform;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Declares the mod's attachments (port P0, PLAN2 3.3):
 * {@code Attach.builder("power", PowerData::new).serialize(PowerData.CODEC).copyOnDeath().sync(PowerData.STREAM_CODEC)
 * .build()}. The builder copies NeoForge's {@code AttachmentType.Builder}.
 */
public final class Attach {
    /** Everything a loader needs to make an attachment type. */
    public record Spec<T>(ResourceLocation id, Supplier<T> defaultValue, @Nullable Codec<T> codec,
                          @Nullable Predicate<? super T> shouldSerialize, boolean copyOnDeath,
                          @Nullable StreamCodec<? super RegistryFriendlyByteBuf, T> syncCodec, SyncTo syncTo) {
        public boolean serialized() {
            return codec != null;
        }

        public boolean synced() {
            return syncCodec != null;
        }
    }

    /** An attachment of the mod named {@code name} ({@code enhanced:<name>}) whose value starts as {@code defaultValue}. */
    public static <T> Builder<T> builder(String name, Supplier<T> defaultValue) {
        return new Builder<>(Enhanced.id(name), defaultValue);
    }

    /**
     * {@code enhanced:persistent_data}: a compound kept with an entity's save, what NeoForge's {@code getPersistentData()}
     * was (copied only as that was: whole when the entity changes dimension, its {@code PlayerPersisted} part when a
     * player respawns). On NeoForge the old {@code NeoForgeData} keys of the mod move into it once.
     */
    public static AttachKey<CompoundTag> persistentData() {
        Spec<CompoundTag> spec = new Spec<>(Enhanced.id("persistent_data"), CompoundTag::new, CompoundTag.CODEC,
                tag -> !tag.isEmpty(), false, null, SyncTo.ALL);
        return new AttachKey<>(spec.id(), Platform.get().attachments().persistentData(spec));
    }

    public static final class Builder<T> {
        private final ResourceLocation id;
        private final Supplier<T> defaultValue;
        @Nullable
        private Codec<T> codec;
        @Nullable
        private Predicate<? super T> shouldSerialize;
        private boolean copyOnDeath;
        @Nullable
        private StreamCodec<? super RegistryFriendlyByteBuf, T> syncCodec;
        private SyncTo syncTo = SyncTo.ALL;

        private Builder(ResourceLocation id, Supplier<T> defaultValue) {
            this.id = id;
            this.defaultValue = defaultValue;
        }

        /** Saves the value with its holder. */
        public Builder<T> serialize(Codec<T> codec) {
            this.codec = codec;
            this.shouldSerialize = null;
            return this;
        }

        /** Saves the value with its holder when {@code shouldSerialize} says so. */
        public Builder<T> serialize(Codec<T> codec, Predicate<? super T> shouldSerialize) {
            this.codec = codec;
            this.shouldSerialize = shouldSerialize;
            return this;
        }

        /** Keeps the value when a player respawns (it is always kept on other clones, such as leaving the End). */
        public Builder<T> copyOnDeath() {
            this.copyOnDeath = true;
            return this;
        }

        /** Sends the value to everyone who can see the holder. */
        public Builder<T> sync(StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
            return sync(SyncTo.ALL, codec);
        }

        /** Sends the value to {@code to}. */
        public Builder<T> sync(SyncTo to, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
            this.syncCodec = codec;
            this.syncTo = to;
            return this;
        }

        public AttachKey<T> build() {
            Spec<T> spec = new Spec<>(id, defaultValue, codec, shouldSerialize, copyOnDeath, syncCodec, syncTo);
            return new AttachKey<>(id, Platform.get().attachments().register(spec));
        }
    }

    private Attach() {
    }
}
