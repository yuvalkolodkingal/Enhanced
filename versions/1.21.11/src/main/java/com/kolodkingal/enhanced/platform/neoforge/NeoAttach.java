package com.kolodkingal.enhanced.platform.neoforge;
//#norenames

import com.kolodkingal.enhanced.Enhanced;
import com.kolodkingal.enhanced.platform.attach.Attach;
import com.kolodkingal.enhanced.platform.attach.AttachKey;
import com.kolodkingal.enhanced.platform.attach.AttachService;
import com.kolodkingal.enhanced.platform.attach.SyncTo;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Attachments on NeoForge: each {@link Attach.Spec} becomes an {@link AttachmentType} with the same id, built with
 * the same builder calls the mod made before the port, in one {@code DeferredRegister} handed to the mod bus by
 * {@link NeoSetup}.
 */
public final class NeoAttach implements AttachService {
    final DeferredRegister<AttachmentType<?>> register =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Enhanced.MODID);

    @Override
    public <T> AttachKey.Handle<T> register(Attach.Spec<T> spec) {
        Handle<T> handle = new Handle<>(register.register(spec.id().getPath(), () -> type(spec)));
        //#if >=1.21.4
        if (spec.syncCodec() != null) {
            com.kolodkingal.enhanced.compat.AttachSync.add(handle::type, spec.syncCodec(), spec.syncTo() == SyncTo.SELF);
        }
        //#endif
        return handle;
    }

    @Override
    public AttachKey.Handle<CompoundTag> persistentData(Attach.Spec<CompoundTag> spec) {
        Supplier<AttachmentType<CompoundTag>> type = register.register(spec.id().getPath(), () ->
                AttachmentType.builder(spec.defaultValue())
                        .serialize(serializer(spec.codec(), spec.shouldSerialize()))
                        .copyHandler(NeoPersistentData::copyOnClone)
                        .build());
        return new NeoPersistentData(new Handle<>(type));
    }

    private static <T> AttachmentType<T> type(Attach.Spec<T> spec) {
        AttachmentType.Builder<T> builder = AttachmentType.builder(spec.defaultValue());
        if (spec.codec() != null) {
            builder.serialize(serializer(spec.codec(), spec.shouldSerialize()));
        }
        if (spec.copyOnDeath()) {
            builder.copyOnDeath();
        }
        //#if <1.21.4
        //$$ if (spec.syncCodec() != null) {
            //$$ if (spec.syncTo() == SyncTo.SELF) {
                //$$ builder.sync((holder, to) -> holder == to, spec.syncCodec());
            //$$ } else {
                //$$ builder.sync(spec.syncCodec());
            //$$ }
        //$$ }
        //#endif
        return builder.build();
    }

    /**
     * (1.21.11) An attachment is written to a value output (a compound of its own): what the codec makes is that compound
     * when it is a compound (as 1.21.1 stored it), else it is kept under {@code "value"}.
     */
    private static <T> net.neoforged.neoforge.attachment.IAttachmentSerializer<T> serializer(
            com.mojang.serialization.Codec<T> codec, @org.jetbrains.annotations.Nullable java.util.function.Predicate<? super T> shouldSerialize) {
        return new net.neoforged.neoforge.attachment.IAttachmentSerializer<>() {
            @Override
            public T read(net.neoforged.neoforge.attachment.IAttachmentHolder holder, net.minecraft.world.level.storage.ValueInput input) {
                CompoundTag tag = com.kolodkingal.enhanced.compat.ValueBridge.tag(input);
                var ops = input.lookup().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
                net.minecraft.nbt.Tag stored = tag.size() == 1 && tag.contains("value") && !codec.parse(ops, tag).isSuccess()
                        ? tag.get("value") : tag;
                return codec.parse(ops, stored).getOrThrow(m -> new IllegalStateException("Unable to read attachment: " + m));
            }

            @Override
            public boolean write(T attachment, net.minecraft.world.level.storage.ValueOutput output) {
                if (shouldSerialize != null && !shouldSerialize.test(attachment)) {
                    return false;
                }
                var ops = com.kolodkingal.enhanced.compat.ValueBridge.registries()
                        .createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
                net.minecraft.nbt.Tag made = codec.encodeStart(ops, attachment)
                        .getOrThrow(m -> new IllegalStateException("Unable to write attachment: " + m));
                CompoundTag into = com.kolodkingal.enhanced.compat.ValueBridge.tag(output);
                if (made instanceof CompoundTag compound) {
                    into.merge(compound);
                } else {
                    into.put("value", made);
                }
                return true;
            }
        };
    }

    /** A key's NeoForge attachment type. */
    static final class Handle<T> implements AttachKey.Handle<T> {
        private final Supplier<AttachmentType<T>> type;

        Handle(Supplier<AttachmentType<T>> type) {
            this.type = type;
        }

        /** The attachment type (registered). */
        AttachmentType<T> type() {
            return type.get();
        }

        private static IAttachmentHolder holder(Object holder) {
            return (IAttachmentHolder) holder;
        }

        @Override
        public T get(Object holder) {
            return holder(holder).getData(type.get());
        }

        @Override
        public @Nullable T set(Object holder, T value) {
            return holder(holder).setData(type.get(), value);
        }

        @Override
        public boolean has(Object holder) {
            return holder(holder).hasData(type.get());
        }

        @Override
        public @Nullable T remove(Object holder) {
            return holder(holder).removeData(type.get());
        }

        @Override
        public Optional<T> getExisting(Object holder) {
            return holder(holder).getExistingData(type.get());
        }

        @Override
        public void sync(Object holder) {
            //#if >=1.21.4
            com.kolodkingal.enhanced.compat.AttachSync.send(holder, type.get());
            //#else
            //$$ holder(holder).syncData(type.get());
            //#endif
        }
    }

    /** The NeoForge attachment type behind a key (for the loader's own glue and its GameTest hooks). */
    public static AttachmentType<?> typeOf(AttachKey<?> key) {
        AttachKey.Handle<?> handle = key.handle();
        if (handle instanceof NeoPersistentData data) {
            handle = data.attachment();
        }
        return ((Handle<?>) handle).type();
    }
}
